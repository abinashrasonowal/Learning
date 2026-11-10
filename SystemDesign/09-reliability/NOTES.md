# 09 — Reliability

> Source: the Primer has no dedicated section; its availability patterns are already
> in 01. Use these instead:
> - the [awesome-system-design-resources](https://github.com/ashishps1/awesome-system-design-resources)
>   articles on rate limiting and circuit breakers
> - the AWS Builders' Library: *Timeouts, retries, and backoff with jitter*; *Using
>   load shedding to avoid overload*
> - Google *SRE Book*, ch. 21–22: *Handling Overload*, *Addressing Cascading
>   Failures*

At scale, **something is always broken**: a node, a disk, a dependency, a deploy. The
goal isn't preventing failure but **containing it**. Most big outages aren't one
component dying. They're **cascades**: one slow dependency ties up callers, callers
retry, load grows, and the failure spreads up the stack. Every technique here breaks
one link in that chain.

---

## 1. How things fail

| Failure | Looks like | Why it's dangerous |
|---|---|---|
| **Crash** | process gone, connection refused | the *easy* case: detected fast, traffic routed away (03) |
| **Slow** | responses take 10s instead of 50ms | **worse than dead**: callers wait, holding threads and connections, so the slowness spreads |
| **Partial / gray** | some requests fail, health check passes | hard to detect; the LB keeps sending traffic |
| **Overload** | latency climbs, then errors | the work to recover (retries) adds more load |
| **Correlated** | many nodes fail together | bad deploy, bad config push, shared dependency, a whole zone. Redundancy doesn't help when every copy has the same bug. |

**Most outages are triggered by a change**: a deploy, a config change, a migration.
Safe change processes (section 10) prevent more incidents than any runtime pattern.

---

## 2. Timeouts

**Every network call needs a timeout** (02). Without one, a hung dependency holds
the caller's thread forever.

**Why slow kills you, in numbers:**

```
Server: 200 worker threads
Dependency normally 50 ms  →  capacity ≈ 200 / 0.05 = 4,000 req/s
Dependency stalls to 10 s  →  capacity ≈ 200 / 10   =    20 req/s
```

One slow dependency drops this server's capacity **200×**, even for requests that
don't use that dependency, because they're all queued behind the same threads.

Setting timeouts:
- Base them on the dependency's **p99 or p99.9 latency**, not its average. Too tight,
  and normal slow requests fail; too loose, and you're back to holding threads.
- Use separate **connect** and **request** timeouts. Connecting should take
  milliseconds; if it doesn't, the host is gone.
- **Propagate deadlines.** Pass the remaining time budget downstream (gRPC does this
  natively). If the user's request has 200 ms left, a downstream call shouldn't wait
  2s for an answer nobody is waiting for.
- **Timeouts must get shorter as you go deeper.** If the outer timeout is shorter than
  an inner one, the inner service keeps working on a request the caller has already
  abandoned.

---

## 3. Retries

Retries fix **transient** failures (a dropped packet, a node mid-restart) and make
**overload** worse. Rules:

1. **Retry only what can succeed next time:** timeouts, 503, 429, connection resets.
   Never retry a 400; it will fail identically.
2. **Retry only idempotent operations**, or use idempotency keys (section 8).
3. **Exponential backoff with jitter:**
   ```
   sleep = random(0, min(cap, base × 2^attempt))      // "full jitter"
   ```
   Backoff gives the dependency room to recover. **Jitter** stops thousands of clients
   that failed at the same moment from retrying at the same moment, in synchronised
   waves.
4. **Cap the attempts** (e.g. 3), and **honour `Retry-After`**.
5. **Use a retry budget:** limit retries to a fraction of normal traffic (e.g. ≤10%).
   When the dependency is mostly failing, retrying everything just multiplies the
   load. A budget shuts retries off automatically.

### Retry amplification

```
3 layers (gateway → service A → service B → DB), each doing 1 try + 3 retries
attempts reaching the DB for one user request = 4 × 4 × 4 = 64
```

When the DB is struggling, every layer retrying turns one request into 64. **Retry at
one layer only**, usually the one closest to the user or the one that knows whether
the operation is safe to repeat. Inner layers fail fast and pass the error up.

---

## 4. Circuit breakers

**Problem:** when a dependency is down, calling it anyway wastes threads waiting for
timeouts and adds load to something already struggling.

**Fix:** wrap calls in a breaker that **stops calling** after repeated failures.

```
          failure rate > threshold
 CLOSED  ───────────────────────────►  OPEN
 (calls flow,                          (fail immediately,
  failures counted)                     no calls made)
    ▲                                    │ after a cool-down
    │ trial calls succeed                ▼
    └───────────────────────────── HALF-OPEN
           trial calls fail → back to OPEN   (let a few trial calls through)
```

- **Fail fast:** callers get an instant error instead of a 2s timeout, so threads stay
  free.
- **Give the dependency room to recover** instead of hammering it while it restarts.
- **Trip on error *rate* with a minimum volume** (e.g. >50% failures over ≥20 calls
  in 10s). Otherwise 1 failure out of 2 calls opens the breaker.
- **Pair it with a fallback:**

  | Fallback | Example |
  |---|---|
  | stale cached data | last known product price (04) |
  | a sensible default | generic recommendations instead of personalised ones |
  | degrade the feature | hide the "reviews" section |
  | queue for later | accept the order, charge the card when payment recovers (08) |
  | fail clearly | "payments are unavailable", for when no fallback is correct |

In Java, use **Resilience4j** (Netflix Hystrix is retired). The breaker is the **State
pattern** from [DesignPatterns/Behavioral/20-State](../../DesignPatterns/Behavioral/20-State/):
closed, open and half-open are states with their own behaviour and transitions.

---

## 5. Bulkheads

Named after a ship's watertight compartments: **one flooded section doesn't sink the
ship.**

**Problem:** all calls share one thread pool. If one dependency gets slow, it takes
*all* the threads (section 2's arithmetic), and calls to healthy dependencies starve.

**Fix:** give each dependency its **own limited pool** (threads, connections, or a
concurrency limit). When the recommendations service hangs, its 20 threads fill up,
and the other 180 keep serving checkout.

The same idea at larger scale:
- **Separate pools or queues by priority:** paid users vs free, checkout vs analytics
  (08's priority queues).
- **Cell-based architecture:** split customers across independent full copies of the
  stack (cells). A bad deploy or poison request only takes down one cell's customers.
- **Shuffle sharding:** assign each customer a random small *combination* of servers.
  A noisy customer that breaks their servers rarely shares the same combination with
  anyone else.

---

## 6. Rate limiting

**Why:** protect against abuse and runaway clients, share capacity fairly, control
costs, and keep load under what the system can actually handle.

### Algorithms

| Algorithm | How | ✅ | ❌ |
|---|---|---|---|
| **Token bucket** | a bucket of capacity *B* refills at *r* tokens/s; each request takes a token, and none left means reject | **allows bursts** up to *B* while enforcing average rate *r*; tiny state; **the most common** (AWS, Stripe) | two parameters to tune |
| **Leaky bucket** | requests join a queue that drains at a fixed rate | **smooth, constant output** rate | bursts get queued (adding latency) or dropped |
| **Fixed window counter** | count per clock window (e.g. per minute); reset at the boundary | simplest: one counter | **boundary burst**: 100 at 0:59 and 100 at 1:00 means 200 in 2 seconds |
| **Sliding window log** | keep a timestamp per request; count the ones in the last 60s | exact | memory per request; expensive at high volume |
| **Sliding window counter** | current window's count + previous window's count × the fraction of it still in range | close to exact, cheap (two counters); smooths the boundary problem | approximate |

**Default to token bucket** for API limits (bursts are normal and fine). Use a
**sliding window counter** when you need "N per minute" with no boundary loophole.

### Where to limit

- At the **edge** (API gateway, CDN) for per-client limits, so bad traffic is
  rejected before it costs anything.
- At each **service** to protect itself, regardless of what's upstream.
- **Key** by API key or user ID. IP is the fallback for anonymous traffic, but many
  users can share one IP behind a NAT (03).

### Distributed rate limiting

With 10 gateway instances, a limit of 100/min needs a **shared count**.

| Approach | ✅ | ❌ |
|---|---|---|
| **Central counter in Redis** | accurate | a Redis round trip per request; Redis becomes a dependency |
| **Local limits per node** (limit ÷ N) | no network call | inaccurate when traffic is uneven across nodes |
| **Local + periodic sync** | fast, roughly accurate | briefly overshoots |

- Make the check-and-increment **atomic**: `INCR` + `EXPIRE`, or a **Lua script** for a
  token bucket. "Read the count, then write it back" lets concurrent requests past the
  limit (05's lost update again).
- **Decide in advance whether to fail open or closed** if Redis is down. Failing open
  (allow everything) is usual for fairness limits. Failing closed is right for limits
  protecting something fragile or expensive.

### Responding

Return **429 Too Many Requests** with `Retry-After`, plus limit headers
(`RateLimit-Limit`, `RateLimit-Remaining`, `RateLimit-Reset`) so well-behaved clients
can slow down before they hit the limit.

---

## 7. Load shedding and graceful degradation

Rate limiting caps each *client*. **Load shedding** protects the *server* when total
demand exceeds capacity, whoever it comes from.

**Principle:** when overloaded, it's better to **reject some requests fast** than to
serve all of them slowly. Slow-for-everyone means everyone times out, and you did all
that work for nothing.

- **Reject early and cheaply:** check before doing any expensive work, and return 503
  immediately.
- **Limit concurrency, not just request rate.** Little's law says
  `concurrency = throughput × latency`, so a cap on in-flight requests reacts
  automatically when latency rises. (Adaptive concurrency limiters, like Netflix's
  `concurrency-limits` library, tune that cap themselves.)
- **Drop work that's already doomed:** if a request waited in the queue past its
  deadline, the client has given up. Don't process it.
- **Shed by priority:** health checks and checkout first, analytics and prefetching
  last.

**Graceful degradation** means planning which features can be switched off under load
or failure: recommendations, reviews, live counters, search suggestions. Wire them to
**feature flags** so they can be turned off in seconds during an incident. The core
path (browse, buy) should depend on as few things as possible.

---

## 8. Idempotency keys, in full

Retries (section 3) are only safe if repeating a request does no extra harm. The flow
(introduced in 02, and used by consumers in 08):

```
1. Client generates a key per logical operation, sends it with every retry
2. Server: look up the key
   - not seen    → lock the key, process, store {key, request hash, response}, return the response
   - seen, done  → return the stored response (don't process again)
   - seen, still in progress → 409, retry later
   - same key, different request body → 422 (client bug)
3. Expire keys after a window (e.g. 24h)
```

Store the key **in the same transaction as the effect**, or a crash between them
reopens the duplicate window (08).

---

## 9. Cascading failures: how they unfold

A typical sequence:

```
1. DB gets slow (a bad query, a lost cache, a noisy neighbour)
2. Services wait on the DB → threads fill up → their latency rises
3. Callers time out → retry (×64 with retries at every layer, section 3)
4. Load rises further → instances fail health checks → LB removes them (03)
5. Remaining instances get *more* traffic each → they fall over too
6. Things restart with cold caches (04) → DB load spikes → back to step 1
7. Recovery: every client retries at once (no jitter) → a thundering herd → fall over again
```

What each defence breaks:

| Defence | Stops |
|---|---|
| Timeouts | threads held forever (step 2) |
| Retry budget, single-layer retries, jitter | retry storms (steps 3 and 7) |
| Circuit breakers | calling a dependency that's known to be down |
| Bulkheads | one slow dependency taking all the threads |
| Load shedding | overload turning into total failure (step 5) |
| Shallow health checks (03) | the LB pulling every instance at once (step 4) |
| Capacity headroom (N+2) | losing a node or a zone overloading the rest |
| Cache warm-up, slow start (03, 04) | the restart spike (step 6) |

---

## 10. Redundancy, blast radius and safe change

- **Redundancy:** spread across availability zones at minimum, and across regions for
  critical systems (active-passive vs active-active from 01).
- **Plan capacity for losing a piece:** N+1 survives one failure, N+2 survives one
  failure *during* maintenance. If 3 zones each run at 80%, losing one sends the other
  two to 120%: you were never redundant.
- **Limit blast radius:** cells and shuffle sharding (section 5).
- **Change safely**, since changes cause most outages:
  - **canary:** deploy to 1% of traffic, compare error rates, then widen
  - **progressive rollout** zone by zone or region by region, never everywhere at once
  - **feature flags** to separate deploying code from switching it on
  - **fast, rehearsed rollback**, including for config changes, which take down as
    many systems as code changes do

### Disaster recovery: RPO and RTO

- **RPO (recovery point objective):** how much data you can lose, measured as time.
  RPO = 1h means up to the last hour of writes may be gone.
- **RTO (recovery time objective):** how long recovery may take.

| Strategy | Setup | RPO / RTO | Cost |
|---|---|---|---|
| Backup & restore | periodic backups in another region | hours / hours | 💲 |
| Pilot light | data replicated, minimal infrastructure running | minutes / tens of minutes | 💲💲 |
| Warm standby | scaled-down full copy running | seconds–minutes / minutes | 💲💲💲 |
| Active-active multi-region | full capacity in each region | ~0 / ~0 | 💲💲💲💲 |

**An untested backup is not a backup.** Restore from it regularly.

### Testing failure on purpose

- **Chaos engineering:** inject failures in production, deliberately (Netflix's Chaos
  Monkey kills instances; others add latency or drop a zone), to find weaknesses
  before a real incident does.
- **Game days:** a team rehearses an outage scenario end to end, including the
  people: alerts, runbooks, rollback.

---

## Gotchas

- **Slow is worse than dead.** A dead dependency fails fast; a slow one takes all your
  threads. Timeouts on everything.
- **Timeouts must shrink going inward,** and pass deadlines along so nobody works for a
  caller who's gone.
- **Retries amplify.** 4 attempts at each of 3 layers = 64× load. Retry at one layer,
  with a budget.
- **Backoff without jitter** synchronises clients into retry waves.
- **Don't retry 4xx** (except 429). Don't retry non-idempotent calls without a key.
- **Circuit breakers need a minimum volume,** or two calls can trip them.
- **A shared thread pool is a shared fate.** Bulkhead each dependency.
- **Fixed-window rate limits allow 2× bursts at the boundary.** Token bucket or sliding
  window counter instead.
- **Rate-limit counters must be atomic** (`INCR`, Lua). Read-then-write lets requests
  past the limit.
- **Decide fail-open vs fail-closed** for the rate limiter before Redis goes down.
- **Under overload, reject fast.** Serving everyone slowly serves no one.
- **Redundancy needs headroom.** Three zones at 80% each isn't redundant.
- **Most outages are changes.** Canary, progressive rollout, feature flags, rehearsed
  rollback.
- **Untested backups and untested failovers don't exist.** Rehearse them.
