# Case study 01: URL shortener

> Think bit.ly / TinyURL. Building blocks used: [01 estimation](../01-fundamentals/NOTES.md#6-back-of-the-envelope-method),
> [04 caching](../04-caching/NOTES.md), [05 databases](../05-databases/NOTES.md),
> [06 sharding](../06-replication-sharding/NOTES.md#5-sharding-horizontal-partitioning),
> [08 messaging](../08-messaging/NOTES.md), [09 rate limiting](../09-reliability/NOTES.md#6-rate-limiting).

It looks like a toy: one table, two endpoints. What makes it interesting is that it's
**extremely read-heavy with tiny payloads**, and every link you hand out is a promise
to redirect correctly **for years**. So the two hard parts are **generating short codes
that never collide** and **making the redirect fast and always up**.

---

## 1. Requirements

**Functional**
- Given a long URL, return a short one: `https://sho.rt/aZ3kQ9x`.
- Visiting the short URL redirects to the long URL.
- Optional **custom alias** (`sho.rt/my-launch`).
- Optional **expiry**. After it, the link stops working.
- Basic **click counts** per link (not a full analytics product).

**Non-functional**
- **Redirect latency:** p99 under ~50 ms server-side. A redirect is pure overhead
  between the user and the page they actually wanted.
- **Availability of redirects: 99.99%.** A broken short link is broken in every tweet,
  email and printed poster that contains it. Creating links can be less available
  (99.9%), since the user is right there and can retry.
- **Durability:** a link, once created, is never lost or silently repointed.
- **Codes shouldn't be guessable in sequence.** Otherwise anyone can walk `…aaa1`,
  `…aaa2` and scrape every private link.

**Out of scope (said out loud)**
- User accounts and dashboards, link editing, rich analytics (geo, referrer, device),
  QR codes.
- **De-duplicating long URLs** (same long URL → same code). Different users want
  separate links with separate stats and expiries, so each create gets a new code.

---

## 2. Estimates

Assumptions: **10M new links/day**, **100:1 read:write** (a link is created once and
clicked many times), kept **5 years**.

```
Writes      10M/day ÷ 10⁵ s                 ≈ 100/s      peak ≈ 300/s
Reads       1B/day  ÷ 10⁵ s                 ≈ 10,000/s   peak ≈ 30,000/s

Links       10M × 365 × 5                   ≈ 18B, call it 20B
Row size    code 7 B + long URL ~200 B + timestamps, owner, overhead   ≈ 500 B
Storage     20B × 500 B                     = 10 TB      × 3 replicas = 30 TB

Bandwidth   30,000/s × ~500 B response      ≈ 15 MB/s    (≈ 120 Mbps, trivial)

Cache       20% of daily reads × 500 B      = 0.2 × 1B × 500 B = 100 GB (upper bound;
            repeat clicks on the same link make the real hot set smaller)
```

**How long must the code be?** Use base62 (`a–z A–Z 0–9`, URL-safe):

| Length | Codes | vs 20B links |
|---|---|---|
| 6 | 62⁶ ≈ 57B | 35% full after 5 years, so random codes start colliding a lot |
| **7** | 62⁷ ≈ **3.5T** | **0.6% full**, plenty of headroom |
| 8 | 62⁸ ≈ 218T | wasted characters |

**Verdict:**
- **Writes are trivial** (300/s). The only hard thing about them is uniqueness.
- **Reads are the load**: 30k/s of tiny key lookups, so this is a **caching** problem.
  100 GB of hot data fits in a small Redis cluster.
- **20B rows / 30 TB** is too big for one DB node, so the store must **partition**.
- **Bandwidth doesn't matter.** Unlike the photo app in 01, there's no payload worth
  putting on a CDN.

---

## 3. API

```
POST /api/v1/urls
  Headers: Authorization: Bearer <api-key>, Idempotency-Key: <uuid>
  Body:    { "long_url": "https://…", "custom_alias": "my-launch"?, "expires_at": "2027-01-01T00:00:00Z"? }
  201 →    { "code": "aZ3kQ9x", "short_url": "https://sho.rt/aZ3kQ9x", "expires_at": … }
  400      invalid / disallowed URL
  409      custom alias taken
  429      rate limited

GET /{code}
  302 Found, Location: <long_url>     (see 301 vs 302 below)
  404      never existed
  410 Gone expired

GET /api/v1/urls/{code}/stats   → { "clicks": 12345 }   (eventually consistent)
DELETE /api/v1/urls/{code}      → 204                     (owner only)
```

- **Idempotency-Key** on create, because a client that times out and retries would
  otherwise get two different short links for one intent
  ([09 §8](../09-reliability/NOTES.md#8-idempotency-keys-in-full)).
- The redirect lives at the **root path** on a separate short domain, and the API lives
  under `/api/v1`. Keeping them apart lets you scale, rate-limit and deploy the hot
  read path separately from the write path.

### 301 vs 302

| | 301 Moved Permanently | 302 Found (or 307) |
|---|---|---|
| Browser behaviour | caches the redirect, so the next click never reaches you | asks you every time |
| Load on you | lower | every click hits you |
| Click counting | **you miss repeat clicks** | accurate |
| Expiry / takedown | **can't take it back**: browsers keep redirecting | works immediately |

**Choose 302.** Expiry, takedowns of malicious links and click counts all need the
request to reach us. The extra load is exactly what the cache is for. (A shortener
with no analytics and no expiry could use 301 and save the traffic.)

---

## 4. Data model

```
urls
  code        string   PK     "aZ3kQ9x"
  long_url    string          up to 2 KB
  owner_id    string          API key / user
  created_at  timestamp
  expires_at  timestamp NULL

click_counts                  (separate, written asynchronously)
  code        string   PK
  clicks      counter
```

**Store: a partitioned key-value / wide-column store** (DynamoDB, Cassandra), keyed by
`code`. Why:

- **The only query is `get by code`.** No joins, no range scans, no ad-hoc queries.
  That's exactly what a KV store is built for ([05 §6](../05-databases/NOTES.md#6-sql-or-nosql)).
- **20B rows need partitioning**, and KV stores do it natively by hashing the key
  ([06 §5](../06-replication-sharding/NOTES.md#5-sharding-horizontal-partitioning)).
  Random codes spread evenly, so there are no hot partitions from the key itself.
- **It needs a conditional write** ("insert only if this code doesn't exist"), which
  both offer: DynamoDB condition expressions, Cassandra lightweight transactions.

Postgres sharded by `hash(code)` would also work, and is a fine answer if the team
already runs it. What you'd give up is native resharding; you'd have to build that.

**Why counts are a separate table:** incrementing a counter on the `urls` row on every
click turns a read-only hot path into a write-heavy one, and a viral link becomes one
row taking 30k writes/s.

---

## 5. High-level design

```
                                   ┌──────────────────────────────────────────┐
  click  GET /aZ3kQ9x              │             Redirect service             │
 ───────────────────▶ LB ─────────▶│  (stateless, many replicas)              │
                                   │  1. in-process cache (few s, hot keys)   │
                                   │  2. Redis  ──miss──▶ 3. KV store         │
                                   │  4. emit click event (fire-and-forget)   │
                                   └───────────┬──────────────────────────────┘
                                               │ 302 Location: long_url
                                               ▼
                                   ┌──────────────────────┐      ┌────────────────────┐
                                   │ Kafka: click-events  │─────▶│ Click aggregator   │
                                   └──────────────────────┘      │ batches → counts   │
                                                                 └────────────────────┘
  create POST /api/v1/urls
 ───────────────────▶ API gateway ─▶ Shortener service ──▶ KV store (conditional put)
                     (auth, rate        │
                      limit)            └─▶ async URL safety scan (queue)

  Expiry: store-native TTL on expires_at + lazy check on read (410).
```

- **Two services, one store.** Redirect and shortener have very different load
  (100:1) and availability targets, so they deploy and scale separately. A bad deploy
  of the create path can't take down redirects (a bulkhead, [09 §5](../09-reliability/NOTES.md#5-bulkheads)).
- **The redirect path touches only cache and KV store.** Analytics, safety scans and
  everything else are off to the side, so their outages don't break clicks.

---

## 6. Deep dives

### 6.1 Generating the code

The heart of the problem: 300 writes/s across many servers, **no two may ever produce
the same code**, and codes shouldn't be sequential.

| Approach | How | Breaks because |
|---|---|---|
| **Hash the long URL** | MD5/SHA → base62 → first 7 chars | Truncation **collides**, so you still need check-and-retry. Same URL → same code, which is the dedupe we said we didn't want. |
| **Single auto-increment counter** | DB sequence → base62 | **Sequential**, so enumerable. The counter is a **SPOF and bottleneck** for every create. |
| **Counter ranges** | Each server leases a block (e.g. 1M IDs) from a coordinator (ZooKeeper/etcd, or one DB row updated with `UPDATE … RETURNING`), then hands out IDs locally | Still sequential unless you scramble them. Server crash wastes its block (harmless). One more component to run. |
| **Random + conditional put** | Generate 7 random base62 chars, `put if not exists`, retry on conflict | Needs a store with conditional writes. That's it. |

**Choose random + conditional put.**
- No coordinator: any server generates codes independently.
- Not enumerable by construction.
- Collisions are rare and *detected*: at 0.6% fill, a collision happens on about 1 in
  170 attempts, and two in a row about 1 in 30,000. Cap retries at ~3 and alert if
  you hit the cap, because that means the keyspace is filling up (time for 8 chars).
- **Cost:** every create is a conditional write. In Cassandra that's a Paxos round
  (~4× a normal write), but at 300/s nobody cares. Use a **cryptographic** RNG: a
  predictable one makes codes guessable again.

The counter-range approach is the right answer if writes were 100× higher and
conditional writes became the bottleneck. Then you scramble the counter with a
**bijection** over `[0, 62⁷)` (e.g. a small Feistel cipher with a secret key) so IDs
stay unique but don't look sequential.

**Custom aliases** go through the same conditional put, in the same namespace. Extra
rules: allowed characters and length, a blocklist (`api`, `admin`, `login`, brand
names), and 409 on conflict instead of retrying.

### 6.2 The read path: making 30k redirects/s fast

Cache-aside ([04 §5](../04-caching/NOTES.md#5-update-strategies)), with three layers:

1. **In-process LRU, TTL a few seconds.** Absorbs **hot keys**: a link in a viral
   tweet can take thousands of clicks/s, and without this, all of them hit the *one*
   Redis shard that owns that key.
2. **Redis, LRU eviction**, ~100 GB across a few shards. Expected hit ratio well above
   90%, because click traffic is heavily skewed towards recent links.
3. **KV store** on a miss, then populate Redis.

Details that matter:
- **Links are immutable**, which makes caching easy. The only invalidations are
  **delete** and **expiry**: on delete, remove the Redis key; for expiry, set the cache
  TTL to `min(default TTL, expires_at − now)` so the cache can't outlive the link.
- **Negative caching.** Bots and scanners hit random codes, and every 404 is a miss
  that goes all the way to the DB. Cache "not found" for a short TTL (~1 min). Keep it
  short, because a code that 404s now can be created a second later.
- **Stampede protection.** When a hot key expires, thousands of requests miss at once.
  Coalesce them so one request fetches and the rest wait
  ([04 §8](../04-caching/NOTES.md#8-failure-modes)).
- **If Redis is down**, redirects fall through to the KV store. Size the store to
  survive that at reduced latency, or one cache failure becomes a full outage.

### 6.3 Click counting without slowing clicks

**Naive:** `UPDATE click_counts SET clicks = clicks + 1` inside the redirect request.
It adds a write to the latency of every click, and a viral link becomes a single hot
row.

**Fix:** the redirect service emits a click event to Kafka and **does not wait** for it
([08](../08-messaging/NOTES.md)). An aggregator consumes events, sums them in memory per
code for a few seconds, then writes one increment per code per batch. 30k clicks/s
on one viral link becomes one write every few seconds.

**Trade-offs:**
- Counts are **eventually consistent**, seconds behind. Fine for this use.
- Delivery is at-least-once, so counts can be slightly high after a consumer crash.
  Exact counts would need idempotent processing (dedupe by event ID); we don't need
  them.
- If Kafka is down, the redirect still succeeds and we drop or locally buffer the event.
  **Losing a click count is acceptable; failing a redirect is not.**

---

## 7. Bottlenecks & trade-offs

### What breaks first at 10×

(100M creates/day, 300k redirects/s peak, 200B links.)

| Pressure | What happens | Response |
|---|---|---|
| **Hot keys** | One viral link exceeds what a single Redis shard serves | In-process cache already absorbs it; add read replicas per shard; edge caching (short `Cache-Control` on the 302 at a CDN) |
| **Cache size** | ~1 TB hot set | More Redis shards; consistent hashing keeps resharding cheap ([06](../06-replication-sharding/NOTES.md)) |
| **Storage** | 300 TB with replicas | KV store adds partitions; move links with no clicks in 1+ year to cheaper storage tier |
| **Keyspace** | 200B / 3.5T ≈ 6% fill, collisions on ~6% of attempts | Still workable; plan the move to 8 chars (new codes only, old ones stay valid) |
| **Multi-region** | Users far from the region pay ~150 ms per redirect | Replicate the store to several regions, serve reads locally (see below) |

### The multi-region catch: read-your-writes

With async replication across regions, a user can create a link in Europe, paste it
into a chat, and a friend in the US clicks it **before the row has replicated**: 404.
That's a stale-read problem ([07](../07-consistency/NOTES.md)).
**Fix:** on a local miss, don't return 404 straight away; check the home region
(encoded in the code or tracked separately) before giving up. Misses are rare, so the
extra hop is cheap.

### What I chose not to do

- **301 redirects**: they'd cut load but cost us expiry, takedowns and counts.
- **De-duplicating long URLs**: needs a secondary index on `hash(long_url)` and makes
  per-user stats and expiry impossible to separate.
- **Strong consistency across regions**: a cross-region quorum on every create costs
  ~150 ms and availability, to prevent a rare 404 that the fallback already fixes.
- **Reusing expired codes**: a recycled code silently sends old links (in old emails,
  old printouts) to a **new, unrelated destination**. 3.5T codes is enough to never
  reuse one.

### Abuse (the part people forget)

- **Rate limit creates** per API key and per IP ([09 §6](../09-reliability/NOTES.md#6-rate-limiting)).
  A shortener without one becomes a spam and phishing tool in a week.
- **Scan destinations asynchronously** against malware/phishing lists, and disable
  links that fail. That's another reason for 302: a takedown has to take effect.
- **Disallow redirect loops**: a long URL pointing back at the short domain.

---

## Gotchas

- **The estimate tells you what kind of problem it is.** 300 writes/s and 30k reads/s
  of ~500 B each means "cache problem with a uniqueness constraint", not "big data".
- **Truncated hashes collide.** Anything shorter than the full hash needs a
  collision check, so hashing doesn't save you the conditional write.
- **Sequential IDs leak.** They reveal your volume and let anyone enumerate links.
  Randomise or scramble.
- **The redirect path must depend on as little as possible.** Every extra dependency in
  that path multiplies into its availability ([01 §4](../01-fundamentals/NOTES.md#4-availability)).
- **Cache 404s, briefly.** Otherwise scanners get to hammer your database for free.
- **Never reuse a code.** A link is a promise that outlives your expiry policy.

---

## Practice

Cover the answers and try each first.

1. Why does the in-process cache in 6.2 need a TTL of seconds, not minutes?
2. A teammate proposes `code = base62(MD5(long_url))[:7]` "because it's stateless".
   What two problems do you raise?
3. Redis loses all data after a restart. What happens to the KV store, and what
   protects it?

<details>
<summary>Answers</summary>

1. It's per server and nobody invalidates it. A deleted or taken-down link keeps
   redirecting from every server's local cache until the TTL runs out, so the TTL is
   the worst-case takedown delay. Seconds is enough to absorb a hot key and short
   enough to be acceptable.
2. (a) Truncated hashes collide, so you still need a conditional write and a retry
   strategy, which means it isn't really stateless. (b) The same long URL always gives
   the same code, so two users shortening one URL share a link, its stats and its
   expiry. Retrying with a salt to fix collisions breaks even the dedupe benefit.
3. A cold cache: the full 30k/s lands on the store at once (a thundering herd). What
   protects it: the in-process cache still absorbs hot keys, request coalescing
   prevents duplicate fetches per key, the store is sized to take a cache outage, and
   load shedding ([09 §7](../09-reliability/NOTES.md#7-load-shedding-and-graceful-degradation))
   if it can't.

</details>
