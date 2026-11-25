# Case study 02: Rate limiter

> Think the limits in front of the Stripe or GitHub API. Building blocks used:
> [01 estimation](../01-fundamentals/NOTES.md#6-back-of-the-envelope-method),
> [03 API gateways](../03-load-balancing/NOTES.md#8-reverse-proxy-vs-load-balancer-vs-api-gateway),
> [04 caching](../04-caching/NOTES.md#10-local-vs-distributed-caches),
> [06 sharding](../06-replication-sharding/NOTES.md#hot-spots),
> [07 clocks](../07-consistency/NOTES.md#7-clocks-and-ordering),
> [09 rate limiting](../09-reliability/NOTES.md#6-rate-limiting).

[09 §6](../09-reliability/NOTES.md#6-rate-limiting) covers the *algorithms*. This case
study is about turning one into a **service that sits in front of every request**. That
placement drives everything else. The limiter is in the hot path of the whole API, so
it has to be **faster than anything behind it**, and it **can't be the reason the API is
down**. The hard parts are **doing an atomic check across many gateway nodes in under a
millisecond**, **one huge customer overloading one counter**, and **deciding what
happens when the limiter itself breaks**.

---

## 1. Requirements

**Functional**
- Limit requests **per API key**, with limits set by plan (free: 10/s, pro: 100/s,
  enterprise: custom).
- Limit **per IP** for unauthenticated traffic (login, signup, public endpoints).
- **Per-endpoint rules**: expensive endpoints (search, exports) get lower limits than
  cheap ones.
- **Several limits on one request**: a burst limit (per second) *and* a quota (per day).
  The request has to pass all of them.
- Rejected requests get **429** with `Retry-After` and `RateLimit-*` headers.
- **Rules change without a deploy**: support raises a customer's limit and it takes
  effect within seconds.

**Non-functional**
- **Added latency:** p99 under ~1 ms. Every API request pays it, so it's a tax on the
  whole platform.
- **The limiter's failure must not become the API's failure.** If the limiter is down,
  the API keeps serving (see 6.3).
- **Accuracy: approximate is fine.** Letting 103 through on a limit of 100 is fine.
  Letting 200 through, or blocking a customer at 50, is not. Aim for a few percent.
- **Scale:** the whole API fleet, every request, every region.

**Out of scope (said out loud)**
- **Volumetric DDoS** (millions of packets/s from botnets). That's handled at the
  network edge or CDN, before traffic ever reaches application servers. A Redis-backed
  limiter would fall over first.
- **Billing-grade metering.** Invoices need exact, durable, auditable counts, which is
  a separate pipeline (event log → aggregation). The limiter's counters are allowed to
  be slightly wrong and to reset; billing counts are not.
- **Fairness between users inside one tenant**: that's the customer's problem.

---

## 2. Estimates

Assumptions: the API serves **1M requests/s at peak** across all gateways; **10M active
API keys and IPs** in any hour; each request checks **~3 rules** (key/second,
key/day, endpoint/second).

```
Checks        1M req/s × 3 rules                      = 3M rule checks/s
              (bundled into ~1 Redis call per request, see 6.1)   ≈ 1M scripts/s

Redis shard   ~100k simple ops/s; a Lua script doing ~4 commands
              → budget ~50k scripts/s per shard, leaving headroom
Shards        1M ÷ 50k                                = 20 primaries (+ 20 replicas)

State         per counter: 2 numbers + key + Redis overhead      ≈ 100 B
              10M keys × 3 counters × 100 B           = 3 GB   (fits on one box)

Latency       same-DC round trip ≈ 0.5 ms (01 §5) + ~50 µs script
              → ~0.6 ms p50; the 1 ms p99 budget allows exactly one round trip
```

**Verdict:**
- **Storage is trivial.** 3 GB, and all of it is disposable: counters expire on their
  own, and losing them only means a brief window of extra leniency.
- **Throughput is the problem.** 1M ops/s needs a sharded Redis cluster for *ops*, not
  for memory. This is the opposite of the URL shortener, which sharded for size.
- **The latency budget allows one network hop.** That rules out a separate limiter
  service *plus* a Redis call (two hops), and rules out a call per rule (three hops).
  It shapes the design in 5 and 6.1.

---

## 3. API

There are two interfaces: what the limiter tells **clients**, and how gateways **ask**
the limiter.

### To the client (response headers on every request)

```
200 OK
  RateLimit-Limit:     100
  RateLimit-Remaining: 37
  RateLimit-Reset:     1          (seconds until the bucket is full again)

429 Too Many Requests
  Retry-After: 2
  RateLimit-Limit: 100
  RateLimit-Remaining: 0
  Body: { "error": "rate_limited", "limit": "api_key:per_second", "retry_after": 2 }
```

- Send the headers on **every** response, not just 429s. Well-behaved clients slow down
  *before* hitting the limit.
- **Name the limit that tripped.** "Per second" and "daily quota" need very different
  reactions from the client: wait 2 s, or wait until tomorrow.

### Internal check (a library call inside the gateway)

```
check(descriptors, cost=1) → { allowed, remaining, retry_after, rule_id }

descriptors = [ ("api_key", "k_123"),
                ("api_key+endpoint", "k_123:/v1/search"),
                ("ip", "203.0.113.7") ]
```

- **`cost`** lets an expensive request take several tokens (a bulk export costs 10). It
  costs nothing to add now and it's painful to retrofit.
- **Descriptors are generic**: the limiter doesn't know what an API key is. Rules match
  descriptors. This is the model Envoy's global rate-limit service uses.

### Admin (rules API)

```
PUT /admin/v1/rules/{rule_id}
  { "match": { "descriptor": "api_key", "plan": "pro" },
    "algorithm": "token_bucket", "capacity": 200, "refill_per_sec": 100,
    "mode": "enforce" | "shadow" }

PUT /admin/v1/overrides/{api_key}   { "rule_id": "...", "refill_per_sec": 1000 }
```

**`mode: shadow`** logs what *would* have been rejected without rejecting it. Every new
or tightened rule runs in shadow for a while first. A wrong limit is an outage you
caused yourself (§7).

---

## 4. Data model

```
Rules (source of truth: Postgres; served from memory)
  rule_id, match (descriptor type, plan, endpoint), algorithm,
  capacity, refill_per_sec | limit + window, mode, updated_at
Overrides
  descriptor_value (e.g. api_key) → rule_id → custom numbers

Counters (Redis, all with TTL)
  rl:{k_123}:sec      HASH  tokens=37.4  ts=1764059112.381     token bucket
  rl:{k_123}:day      STRING 48211        EXPIRE at midnight UTC  fixed window
  rl:{ip:203.0.113.7}:min  …
```

**Rules: a relational DB, cached in every gateway's memory.** There are thousands of
rules, they change a few times a day, and they're read a million times a second. Store
them in Postgres (audit history, admin UI, transactions), and **push** them to
gateways: gateways poll a config service or watch a key, and hold the full set in
memory. No request ever reads rules over the network. If the config service is down,
gateways keep the last rules they had.

**Counters: Redis.** Why:
- It needs an **atomic read-modify-write** at 1M/s. Redis is single-threaded per shard,
  so a Lua script runs without interleaving. That's exactly the atomicity needed
  ([09 §6](../09-reliability/NOTES.md#distributed-rate-limiting)), with no locks.
- **TTL is built in**, so idle keys clean themselves up.
- **Durability isn't needed.** Losing counters means briefly allowing a little more
  traffic. Turn off AOF persistence, which also takes fsync latency off the hot path.

A DB with conditional updates (DynamoDB, Postgres) would be correct but ~5–10 ms per
check and expensive at 1M writes/s. That's the wrong side of the latency budget.

**Algorithm per rule, picked by what the limit *means*:**

| Rule | Algorithm | Why |
|---|---|---|
| Per-second / per-minute burst limits | **Token bucket** | Bursts are normal client behaviour; it smooths to the average rate |
| Daily / monthly quota | **Fixed window**, aligned to calendar | The product says "resets at midnight UTC". The boundary burst is the *intended* semantics here |
| Login attempts per IP | **Sliding window counter** | No boundary loophole for an attacker to time |

---

## 5. High-level design

```
  client ──▶ CDN / edge (DDoS, coarse per-IP limits)
                 │
                 ▼
        ┌─────────────────────────────────────────────┐
        │ API gateway (many replicas, per region)      │
        │  1. authenticate → api_key, plan             │
        │  2. match rules in memory → descriptors      │      ┌──────────────────────┐
        │  3. limiter lib: ONE Lua call per entity ───┼─────▶│ Redis Cluster         │
        │       (local token lease for hot keys, 6.2)  │◀─────│ 20 shards, by hash    │
        │  4. allowed? → forward : 429 + headers       │      │ of {entity} tag       │
        │  5. emit decision metric (async)             │      └──────────────────────┘
        └───────────────┬─────────────────────────────┘
                        │ allowed only
                        ▼
                 backend services (each with its own concurrency limit / load shedding)

  Rules: admin UI ─▶ Postgres ─▶ config service ─push/poll─▶ every gateway's memory
```

**Where the limiter runs: a library in the gateway, not a separate service.**

| Option | ✅ | ❌ |
|---|---|---|
| **Library in the gateway** (chosen) | One network hop (gateway → Redis); no extra service to run | Every gateway needs the library; rule logic ships with gateway deploys |
| **Separate rate-limit service** (Envoy's model) | Language-neutral; one team owns it; Redis connections pooled in one place | **Two hops** (gateway → service → Redis), and the service is one more thing that can fail |
| **Per-node only, in memory** | Zero network, zero dependencies | Inaccurate when load is uneven; limits change as the fleet scales |

At a 1 ms budget, the library wins. The separate-service model is the right call when
many teams with different gateways and languages all need limiting and a millisecond
more doesn't matter.

**Limits are not the last line of defence.** Backends still protect themselves with
concurrency limits and load shedding ([09 §7](../09-reliability/NOTES.md#7-load-shedding-and-graceful-degradation)).
Rate limits stop *one client* taking too much. They don't stop *all clients together*
exceeding capacity.

---

## 6. Deep dives

### 6.1 One atomic check, one round trip

**Naive:** `GET` the counter, compare in the gateway, `SET` it back. Two gateways read
`tokens=1` at the same time, both allow, both write `0`. On a hot key that happens
constantly, and the limit becomes a suggestion. This is the lost update from
[05](../05-databases/NOTES.md).

**Fix:** do the whole read-refill-decide-write **inside Redis**, as a Lua script:

```lua
-- KEYS[1] = bucket key   ARGV = capacity, refill_per_sec, cost
local cap, rate, cost = tonumber(ARGV[1]), tonumber(ARGV[2]), tonumber(ARGV[3])
local t   = redis.call('TIME')                  -- Redis's clock, not the gateway's
local now = tonumber(t[1]) + tonumber(t[2]) / 1e6

local b      = redis.call('HMGET', KEYS[1], 'tokens', 'ts')
local tokens = tonumber(b[1]) or cap            -- new key starts full
local ts     = tonumber(b[2]) or now

tokens = math.min(cap, tokens + (now - ts) * rate)   -- refill for elapsed time
local allowed = tokens >= cost
if allowed then tokens = tokens - cost end

redis.call('HSET', KEYS[1], 'tokens', tokens, 'ts', now)
redis.call('EXPIRE', KEYS[1], math.ceil(cap / rate) + 1)  -- gone once it'd be full anyway
return { allowed and 1 or 0, math.floor(tokens) }
```

Details that matter:
- **No timer refills anything.** The bucket stores `tokens` and `ts`, and works out the
  refill from elapsed time when it's next touched. Ten million idle buckets cost
  nothing.
- **Use Redis's clock (`TIME`), not the gateway's.** Gateways' clocks drift apart
  ([07 §7](../07-consistency/NOTES.md#7-clocks-and-ordering)). If gateway A is 2 s ahead
  of B, A's request "refills" 2 s worth of tokens that B then sees as already spent.
  One shard, one clock.
- **The TTL is the refill time.** After `cap / rate` seconds idle the bucket would be
  full anyway, so deleting it changes nothing. A missing key *is* a full bucket.
- **Several rules, one call.** Running the per-second and per-day checks as two scripts
  costs two round trips and blows the budget. Put both counters for an entity in one
  script. In Redis Cluster a script can only touch keys **on one shard**, so use a
  **hash tag**: `rl:{k_123}:sec` and `rl:{k_123}:day` hash on `k_123` only and land
  together. Different entities (API key vs IP) are on different shards; check them in
  **parallel**, so the wall-clock cost is still one round trip.
- **All-or-nothing across rules.** If the per-second check passes and the daily quota
  fails, the per-second token shouldn't be spent. Check all the counters for the
  entity first, then decrement all of them only if every one passes, in the same
  script.

**Trade-off:** logic in Lua is harder to test and deploy than gateway code, and a slow
script blocks the whole shard. Keep scripts short and fixed, load them once with
`SCRIPT LOAD`, and call them by SHA (`EVALSHA`).

### 6.2 Hot keys: one customer, one shard

The busiest enterprise customer sends **50k req/s**. Their counter is one key, so it
lives on one shard. At ~50k scripts/s per shard, **one customer saturates a shard** and
slows every other customer who hashes there. Adding shards doesn't help: you can't
split one key ([06 hot spots](../06-replication-sharding/NOTES.md#hot-spots)).

**Fix: token leasing.** For keys above a threshold, a gateway doesn't ask Redis for
one token per request. It asks for **a batch** (say 100), keeps them in memory, and
spends them locally. The Lua script is the same, with `cost = 100`.

```
50k req/s, 50 gateways, batch of 100:
  Redis calls for this key = 50,000 ÷ 100 = 500/s     (was 50,000/s)
```

**Trade-offs:**
- **Accuracy.** Up to `gateways × batch` tokens can be leased and unspent at once.
  Here that's 50 × 100 = 5,000, which is 10% of their per-second rate. Leased tokens
  that aren't used expire with a short lease (~1 s); they don't go back.
- **Fairness near the limit.** One gateway can hold the last tokens while another
  rejects. Shrink the batch as `remaining` falls, down to 1 near the limit.
- **Only for big keys.** For a customer doing 10/s, a batch of 100 would let one
  gateway burn the whole bucket. Batch size scales with the key's rate: about
  `rate × lease_time ÷ gateways`.

This is the "local + periodic sync" row from
[09 §6](../09-reliability/NOTES.md#distributed-rate-limiting), applied only where it's
needed, so small customers keep exact limits.

### 6.3 When the limiter breaks

Redis shard down, network partition, or a slow script: the gateway's call to Redis
fails or times out. Every API request goes through this path, so this decision matters
more than any algorithm.

**Fail open (allow) or closed (reject)?**

| | Fail open | Fail closed |
|---|---|---|
| Effect | API keeps working, limits stop applying | **The whole API is down** because a counter store is down |
| Risk | abuse during the outage; backends see unlimited traffic | none from clients; total outage from you |
| Right for | fairness and plan limits (almost everything) | limits guarding something irreversible or costly: SMS sends, password guesses, paid third-party calls |

**Choose fail open, with a local safety net.** When Redis is unreachable, the gateway
switches to **in-memory limits of `limit ÷ number of gateways`** per key. These are
inaccurate (uneven load across gateways means some customers get less, some more), but
they keep one client from flooding everything while Redis recovers. Backends'
own load shedding is the second net.

Details that matter:
- **A tight timeout on the Redis call** (~5 ms). Without it, a slow Redis makes every
  API request slow, which is worse than Redis being down. A slow dependency is the
  failure mode to fear ([09 §2](../09-reliability/NOTES.md#2-timeouts)).
- **A circuit breaker** around Redis ([09 §4](../09-reliability/NOTES.md#4-circuit-breakers)).
  Once calls start failing, stop trying for a few seconds and go straight to local
  limits. Otherwise each request waits out the timeout first.
- **Failover resets counts.** Replication is async, so a promoted replica is a few
  writes behind. Some clients get a few extra requests. Acceptable, and why
  "approximate" is in the requirements.
- **Per-rule override.** The login rule is marked `fail_closed`: a Redis outage must
  not become a window for unlimited password guessing.
- **Alert on fail-open.** It's silent by design, so it needs a metric and an alert, or
  the limiter can be off for days without anyone noticing.

---

## 7. Bottlenecks & trade-offs

### What breaks first at 10×

(10M req/s, 100M active keys.)

| Pressure | What happens | Response |
|---|---|---|
| **Redis ops** | 10M scripts/s needs ~200 shards | Lease tokens for more keys (lower threshold); keep fixed-window quotas in local counters synced every few seconds, since daily quotas tolerate seconds of lag |
| **Hot keys** | A customer at 500k req/s | Larger leases; per-region budgets (below) split the load across regions' clusters |
| **Connections** | 1,000s of gateways × 200 shards of connections | Pool and multiplex connections; a sidecar or proxy per host instead of per process |
| **Memory** | 30 GB | Still fine; TTLs keep it to active keys only |
| **Rule matching** | Thousands of rules per request | Index rules by descriptor type in memory; it was a linear scan before |

### Global limits across regions

A customer's limit is 100/s **in total**, and they send traffic to three regions. Each
region has its own Redis. Options:

| Approach | ✅ | ❌ |
|---|---|---|
| **One global Redis** | exact | +100 ms cross-region round trip on every request. Ruled out by the 1 ms budget. |
| **Split the limit by region** (e.g. 60/25/15 by last hour's traffic share) | local, fast, never exceeds the total | wrong when traffic shifts suddenly: one region rejects while another has unused budget |
| **Local limits + async sync** (each region publishes its counts every second; each subtracts the others') | follows traffic shifts | overshoots by up to ~1 s of traffic during spikes |

**Choose split by region, rebalanced every minute** from observed traffic. Most
customers are mostly in one region anyway. This is the same rule as
[07 §8](../07-consistency/NOTES.md#8-choosing-per-piece-of-data): a rate-limit counter
doesn't deserve cross-region consensus.

### What I chose not to do

- **A separate rate-limit service**: cleaner ownership, but a second network hop on
  every request.
- **Exact global counts**: costs a cross-region round trip to prevent overshoot the
  requirements explicitly allow.
- **Durable counters (AOF, a database)**: pays fsync latency and cost to protect data
  that's worthless a minute later.
- **Queueing over-limit requests (leaky bucket style)**: holding requests ties up gateway
  connections and memory, and the client probably times out anyway. Reject fast with
  `Retry-After` and let the client decide.
- **Rate limiting inside each backend instead of at the gateway**: rejected requests
  would already have cost auth, routing and a hop. Backends keep *capacity*
  protection; *per-client* limits belong at the edge.

### Operating it (the part people forget)

- **Shadow mode first.** A new or tightened rule logs would-be rejections for a few
  days. A wrong limit on a big customer is an outage that *you* caused.
- **Measure per rule:** allowed vs rejected rate, Redis latency, fail-open count, top
  limited keys ([11](../11-observability/NOTES.md)). "Why am I getting 429s?" is a top
  support question; support needs to see which rule fired and when.
- **Limits are product decisions.** They're tied to plans and pricing. Keep them in
  config the business can change, never in code.
- **Count rejected requests too, at the edge.** A client hammering away at 429s still
  costs bandwidth and auth. The per-IP edge limit catches clients that ignore
  `Retry-After`.

---

## Gotchas

- **The limiter is on every request's path.** Its latency is added to everything, and
  its outage is everyone's outage unless you fail open.
- **Atomic or it's not a limit.** Read-then-write lets concurrent requests through;
  do the check in one Lua script.
- **Use one clock.** Redis's `TIME`, not each gateway's wall clock.
- **One hot key = one hot shard.** You can't shard a single counter; lease tokens in
  batches instead.
- **Multi-key scripts need hash tags** in Redis Cluster, or they fail with `CROSSSLOT`.
- **Rate limiting isn't overload protection.** It caps clients one by one. Backends
  still need load shedding for when everyone arrives at once.
- **The limiter's counters are not billing.** Approximate and disposable vs exact and
  auditable are different systems.

---

## Practice

Cover the answers and try each first.

1. A free-tier key has `capacity 10, refill 1/s`. It's been idle for an hour, then
   sends 15 requests in the same instant. How many succeed, and when can it send the
   next one?
2. A teammate stores buckets under `rl:k_123:sec` and `rl:k_123:day` and runs one Lua
   script over both on Redis Cluster. It fails in production. Why, and what's the fix?
3. Why is the token lease in 6.2 bad for a key limited to 5/s on a fleet of 50
   gateways?
4. Redis is down and you've failed open. Which rule would you *never* fail open, and
   why?

<details>
<summary>Answers</summary>

1. **10 succeed.** The bucket caps at 10 however long it's idle, and the other 5 get
   429. One token refills per second, so the next request can go **1 s later**
   (`Retry-After: 1`).
2. The two keys hash to different slots, so a script touching both fails with
   `CROSSSLOT`. Use a hash tag: `rl:{k_123}:sec` and `rl:{k_123}:day` hash only the
   part in braces, so both land on the same shard.
3. With 50 gateways, even a batch of 1 per gateway could lease 50 tokens, which is 10 s
   of that key's budget, sitting idle on gateways that may never use them. Meanwhile
   the gateway actually getting the traffic is rejected. Small keys need exact,
   per-request checks; leasing only pays off when a key's rate is much larger than the
   number of gateways.
4. Anything where a burst does **irreversible or costly harm**: login and
   password-reset attempts (unlimited credential guessing), SMS/email sends (real
   money and spam complaints), calls to a paid third-party API. For those, a brief
   "try again later" is cheaper than what slips through.

</details>
