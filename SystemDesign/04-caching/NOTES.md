# 04 — Caching

> Source: [System Design Primer](https://github.com/donnemartin/system-design-primer):
> "Content delivery network" (push/pull), "Cache" (client, CDN, web server, database
> and application caching; cache-aside, write-through, write-behind, refresh-ahead).

01's latency table is the whole argument: memory is ~100 ns, a datacenter round trip
to a database is ~500 µs plus query time, and a cross-world trip is ~150 ms. Most
systems are read-heavy (the photo app in 01 was 500:1), and a small set of hot items
gets most of the reads. **Caching keeps that hot set closer and in faster storage.**

The price is that **the cache can be wrong**. Every section below is either "where to
put the copy" or "how to stop the copy lying for too long".

---

## 1. The numbers that matter

```
Effective latency = hit_ratio × cache_latency + miss_ratio × (cache_latency + db_latency)

cache 1 ms, DB 20 ms:
  90% hit  →  0.9×1 + 0.1×21 =  3.0 ms
  99% hit  →  0.99×1 + 0.01×21 = 1.2 ms
```

Going from 90% to 99% hits cuts **DB load by 10×** (10% of reads reach it vs 1%).
**Hit ratio** is the metric to watch, and small changes near the top have a big
effect.

---

## 2. Where caches live

Closest to the user first:

| Layer | What's cached | Controlled by |
|---|---|---|
| **Client (browser, mobile app)** | static assets, API responses | HTTP headers (`Cache-Control`, `ETag`), app code |
| **CDN** | static files, media, sometimes whole pages or API responses | headers + CDN config |
| **Web server / reverse proxy** | full responses (Nginx, Varnish) | proxy config |
| **Application cache** | objects, computed results, sessions (Redis, Memcached) | your code |
| **Database** | pages / rows in memory (buffer pool) | the DB, mostly automatic |

A request served by an earlier layer never touches the later ones. **The cheapest
request is the one that never leaves the browser.**

### HTTP caching headers (client, CDN, proxy)

| Header | Meaning |
|---|---|
| `Cache-Control: max-age=3600` | fresh for 1 hour, no need to ask the server |
| `Cache-Control: s-maxage=…` | max-age for **shared** caches (CDN, proxy) only |
| `Cache-Control: private` | browser may cache, CDN must not (per-user data) |
| `Cache-Control: no-cache` | may store, but **must revalidate** before every use |
| `Cache-Control: no-store` | don't store at all (sensitive data) |
| `ETag` + `If-None-Match` | revalidation: "still version `abc`?" → **304 Not Modified**, no body sent |
| `stale-while-revalidate=60` | serve the stale copy instantly while refreshing in the background |

**Cache busting for static assets:** put a content hash in the filename
(`app.3f9a1c.js`) and cache it for a year (`max-age=31536000, immutable`). A new
deploy means a new filename, so there's never a stale asset and never a need to
purge.

> `no-cache` does **not** mean "don't cache". That's `no-store`. A classic mix-up.

---

## 3. CDN

A network of edge servers around the world that serve content from close to the user
(cutting the ~150 ms cross-world round trip from 01).

| | **Push CDN** | **Pull CDN** |
|---|---|---|
| How | **you upload** content to the CDN whenever it changes | CDN **fetches from your origin** on the first request, then caches it for the TTL |
| Expiry | you control it | TTL from headers |
| Good for | low traffic, or content that rarely changes | **high traffic**: popular content stays cached |
| Weakness | you must push every change; uses storage for content nobody requests | first request per edge is slow (a miss); a short TTL means repeat origin fetches |

**Costs (from the Primer):** CDN bills can be significant at high traffic, content
can be stale if it changes before the TTL expires, and URLs must point at the CDN.

Beyond the Primer:
- **Origin shield:** a middle cache tier between the edges and your origin, so 200
  edges missing at once become one origin request, not 200.
- **Cache key design:** by default the key is the URL. Varying it on unnecessary
  query strings, cookies or headers fragments the cache and lowers the hit ratio.
- **Purge / invalidation APIs** exist but are slow-ish and rate-limited. Prefer
  versioned URLs for anything you control.
- **Dynamic content** can still benefit: a CDN keeps warm connections to your origin
  and terminates TLS at the edge, which cuts round trips (see 02) even when nothing
  is cached.

---

## 4. What to cache (application level)

Recap from 01: **cache objects, not query results.** One changed row invalidates
every query result that included it, but only one object key.

Good candidates:
- Data that's **read often and changes rarely**: user profiles, product details,
  config.
- **Expensive to compute**: rendered pages, aggregates, recommendations.
- **Session data**, which needs fast access and is easy to recreate if lost.

Don't cache:
- Data that changes on almost every read (the hit ratio will be terrible).
- Data that must never be stale (account balances, inventory at checkout). Read
  those from the source of truth.
- Very large values that would push lots of small hot items out of memory.

---

## 5. Update strategies

How the cache and the DB stay in sync. These are the Primer's four, plus write-around.

### Cache-aside (lazy loading), the default

```
read:   value = cache.get(k)
        if miss: value = db.read(k); cache.set(k, value, ttl)
write:  db.write(k, v); cache.delete(k)
```

- ✅ Only data that's actually requested gets cached. A cache outage degrades
  performance but doesn't break anything (reads fall back to the DB).
- ❌ A miss costs 3 trips (cache, DB, cache). Data can be stale if the DB changes
  through another path (TTL limits how long). A new or restarted cache node starts
  empty, so latency spikes.

### Write-through

```
write:  cache.set(k, v)  →  cache (or app) synchronously writes db
read:   always from cache
```

- ✅ The cache is never stale for data written through it, and reads are fast.
- ❌ Every write is slower (two writes). Most data written may never be read (use a
  TTL). A new node has nothing until the data is written again, so it's usually
  combined with cache-aside for reads.

### Write-behind (write-back)

```
write:  cache.set(k, v); enqueue(k, v)   → worker writes to db asynchronously
```

- ✅ Very fast writes, and many writes to the same key can be batched into one.
- ❌ **Data loss** if the cache dies before flushing. More complex. The DB lags behind
  the cache.

### Refresh-ahead

The cache **reloads recently accessed entries before they expire**.

- ✅ Popular keys never miss, so there are no latency spikes.
- ❌ Only helps if you predict well; refreshing items nobody will request wastes
  load.

### Write-around

Writes go straight to the DB without touching the cache. Reads use cache-aside.

- ✅ Write-heavy data that's rarely read never pollutes the cache.
- ❌ The first read after a write is always a miss.

### Summary

| Strategy | Write speed | Staleness | Risk |
|---|---|---|---|
| Cache-aside | normal | until TTL / delete | stale-set race (section 6) |
| Write-through | slower | minimal | cache full of unread data |
| Write-behind | fastest | DB lags | **data loss** |
| Refresh-ahead | — | minimal for hot keys | wasted refreshes |
| Write-around | normal | until next read | first read misses |

---

## 6. Invalidation

> "There are only two hard things in computer science: cache invalidation and naming
> things." — Phil Karlton

### The options

- **TTL:** every entry expires after N seconds. It's a safety net: stale data can't
  outlive the TTL, whatever else goes wrong. **Always set one.**
- **Explicit delete on write:** the writer deletes the key after updating the DB.
- **Versioned keys:** `user:42:v7`. A write bumps the version, so old entries just
  stop being read and age out.
- **Change data capture (CDC):** a process tails the DB's change log (e.g. Debezium)
  and deletes cache keys, so invalidation happens even for writes from other
  services or scripts.

### Delete, don't update

On a write, **delete** the cache key rather than writing the new value into it. Two
concurrent writers updating the cache can finish in the opposite order to their DB
writes, leaving the *older* value in the cache permanently. A delete just causes one
extra miss.

### Order: update the DB, *then* delete the cache

If you delete first, a reader can miss between the delete and the DB write, read the
**old** value from the DB, and put it back in the cache.

### Even then, a race remains

```
1. Reader: cache miss, reads OLD value from DB
2. Writer: updates DB to NEW, deletes cache key
3. Reader: sets cache = OLD                 ← stale until TTL
```

It's rare (the reader must be slower than an entire write), but at scale rare things
happen daily. Mitigations: **short TTLs** to bound the damage; a **delayed second
delete** (delete again ~1s after the write); or **leases**, where the cache hands out a
token on a miss and rejects a set from a reader whose lease was invalidated by a
write (Facebook's memcache paper).

---

## 7. Eviction policies

When memory is full, something has to go.

| Policy | Evicts | Good for |
|---|---|---|
| **LRU** (least recently used) | the item untouched the longest | the default; recent use predicts future use |
| **LFU** (least frequently used) | the item used least often | stable popularity; resists one-off scans pushing out hot items |
| **FIFO** | the oldest inserted | simple, rarely the best |
| **TTL-based** | the item expiring soonest | when TTLs reflect value |
| **Random** | anything | cheap; surprisingly decent |

Redis is configured via `maxmemory-policy`: `allkeys-lru`, `allkeys-lfu`, or the
`volatile-*` variants that only evict keys with a TTL. **Sizing:** the cache needs to
hold the *working set* (what's actively requested), not the whole dataset. Below
that, the hit ratio collapses.

---

## 8. Failure modes

| Problem | What happens | Fixes |
|---|---|---|
| **Stampede (thundering herd)** | a **hot key expires**, and thousands of concurrent requests miss at once and all hit the DB | **request coalescing / single-flight** (one request recomputes, the rest wait); a lock on recompute; **serve stale while revalidating**; probabilistic early refresh |
| **Penetration** | requests for keys that **don't exist** always miss and always hit the DB (often malicious) | **cache the "not found" result** with a short TTL; a **Bloom filter** of existing keys to reject impossible lookups |
| **Avalanche** | **many keys expire at the same moment** (all set by one batch job), or the cache cluster goes down | **add random jitter to TTLs**; replicated or highly available cache; rate-limit or circuit-break DB access so it isn't flattened |
| **Hot key** | one key gets so much traffic its single cache shard melts (a celebrity's profile) | small **in-process local cache** in front; copy the key to N shards (`key#1…key#N`) and read a random copy |
| **Cold start** | a new or restarted cache is empty, so the DB takes the full read load | warm the cache before switching traffic over; bring capacity up gradually |

**The deeper issue:** once your DB is sized for a 99% hit ratio, the cache is no longer
optional. If it goes down, the DB gets 100× its normal load and falls over. Know
whether your DB can survive a cold cache, and if not, protect it (rate limits, load
shedding).

---

## 9. Redis vs Memcached

| | **Redis** | **Memcached** |
|---|---|---|
| Data types | strings, hashes, lists, sets, sorted sets, streams, … | strings (blobs) only |
| Persistence | optional (snapshots, append-only log) | none |
| Replication / HA | built in (replicas, Sentinel, Cluster) | none, client-side sharding |
| Threading | commands run on a single thread (I/O threaded since v6) | multi-threaded |
| Extras | pub/sub, Lua scripts, transactions, TTL per key | — |
| Pick when | you need data structures, persistence or HA: the usual default | plain, huge, simple key-value caching with many cores |

Redis also gets used for leaderboards (sorted sets), rate limiters, distributed locks
and queues. That's handy, but don't let your cache quietly become the system of
record for data that needs durability.

---

## 10. Local vs distributed caches

| | **Local (in-process)** | **Distributed (Redis cluster)** |
|---|---|---|
| Speed | nanoseconds, no network | ~0.5–1 ms round trip |
| Shared across instances | ❌ each server has its own copy | ✅ |
| Consistency | instances can disagree | one copy |
| Capacity | limited by app memory | scales out (sharded with consistent hashing, see 06) |

A common setup is **both levels**: a small local cache with a short TTL for the
hottest keys, then the distributed cache, then the DB. The local level absorbs hot
keys, and its short TTL limits how long servers disagree.

---

## Gotchas

- **Always set a TTL.** It's the safety net for every invalidation bug you haven't
  found yet.
- **On write: update the DB, then delete the cache.** Don't update the cache, and
  don't delete before writing.
- **`no-cache` ≠ don't cache.** `no-cache` revalidates; `no-store` doesn't store.
- **Never cache per-user responses on a shared CDN** without `private` or the right
  cache key. One user's page served to everyone is a data leak.
- **Jitter your TTLs.** Keys set together expire together, and so does your DB.
- **Coalesce misses on hot keys.** One expiry shouldn't become 10,000 DB queries.
- **Cache negative results**, or missing keys will skip the cache every time.
- **A sized-for-99%-hits DB can't survive a cold cache.** Plan warm-up, and protect
  the DB with load shedding.
- **Write-behind trades durability for speed.** Use it only where losing recent
  writes is acceptable (counters, analytics), never for payments.
- **Fragmented cache keys** (varying on cookies, tracking query strings) quietly
  destroy the CDN hit ratio. Normalise them.
