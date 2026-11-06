# 03 — Load Balancing

> Source: [System Design Primer](https://github.com/donnemartin/system-design-primer):
> "Load balancer" (including L4/L7 and horizontal scaling), "Reverse proxy (web
> server)", "Load balancer vs reverse proxy".

01 said: make app servers stateless clones. This topic is the piece that makes clones
useful, the thing in front that **decides which clone gets each request**, and notices
when one dies. It's also where 02's L4 (TCP) vs L7 (HTTP) distinction starts to
matter.

---

## 1. What a load balancer does

**Problem:** one server is both a capacity limit and a single point of failure (SPOF).
Adding servers only helps if something spreads the traffic and routes around dead
ones.

A load balancer (LB):
1. **Distributes** requests across healthy backends.
2. **Health-checks** backends and stops sending traffic to failed ones.
3. **Terminates TLS** (optional), taking the encryption CPU cost off the backends.
4. **Hides the backends:** clients see one address, and servers can be added,
   removed or replaced behind it.

**Costs (from the Primer):** it can become the bottleneck if under-provisioned or
misconfigured, it adds another component to run, and **a single LB is itself a SPOF**
(see section 8).

---

## 2. Where load balancers sit

```
users
  │  DNS / anycast  → picks a region (global load balancing)
  ▼
[ regional LB ]     → picks an app server
  ▼
app servers ── [ internal LB ] ──► service B instances
  │
  └──────────── [ DB proxy ] ───► DB read replicas
```

LBs appear at **every tier** where there's more than one instance, not just at the
front door.

**Server-side vs client-side:**
- **Server-side:** a dedicated LB box or service sits in the path (Nginx, HAProxy,
  AWS ALB/NLB). Simple for clients, but it's an extra hop.
- **Client-side:** the client gets the list of instances (from service discovery)
  and chooses one itself. No extra hop, but every client needs the logic. gRPC
  clients and service meshes (Envoy sidecars) work this way.

---

## 3. L4 vs L7

| | **Layer 4 (transport)** | **Layer 7 (application)** |
|---|---|---|
| Sees | IP addresses, ports (TCP/UDP) | the full HTTP request: path, host, headers, cookies |
| Routes by | connection | **request**, e.g. `/api/*` → API servers, `/static/*` → file servers |
| Mechanism | forwards packets / the connection (via NAT) | **terminates** the client connection, opens its own to the backend |
| Can also | pass TLS through untouched | modify headers, retry, compress, cache, rewrite URLs, auth |
| Cost | very fast, little CPU | more CPU and memory, adds a little latency |
| Examples | AWS NLB, HAProxy (TCP mode), LVS | AWS ALB, Nginx, HAProxy (HTTP mode), Envoy |

**Default to L7** for HTTP services: content routing, header-based canary releases and
per-request balancing are worth the CPU. **Use L4** for non-HTTP traffic (databases,
custom TCP, game servers), extreme throughput, or when TLS must stay end-to-end.

> **The gRPC / HTTP/2 trap:** HTTP/2 multiplexes many requests over **one long-lived
> connection**. An L4 LB balances *connections*, so every request from that client
> lands on the same backend while others sit idle. HTTP/2 and gRPC need **L7**
> (request-level) balancing, or client-side balancing.

---

## 4. Balancing algorithms

| Algorithm | How | Good when | Weakness |
|---|---|---|---|
| **Round robin** | each backend in turn | identical servers, similar requests | ignores how busy each server is |
| **Weighted round robin** | turns proportional to weight | mixed server sizes; canaries (5% to the new version) | weights are static |
| **Least connections** | backend with the fewest open connections | requests of varying length | an open connection isn't a reliable measure of load |
| **Least response time** | fewest connections + fastest recent responses | backends with uneven performance | needs latency tracking; can overload a backend that just got fast |
| **IP hash / consistent hashing** | hash(client IP or key) → backend | the same key must reach the same server (cache locality, sticky sessions) | uneven if a few keys are hot; consistent hashing covered in 06 |
| **Random** | uniformly random | many LBs, no shared state | short-term imbalance |
| **Power of two choices** | pick 2 random backends, send to the less loaded | many LBs with stale load info | — (near-optimal and cheap) |

**Power of two choices** deserves remembering. "Least connections" across many LB
instances fails because they all see the same stale data and **stampede** the same
"least loaded" server. Picking the better of two random servers avoids the herd and
gets nearly the same balance. Envoy and NGINX both offer it.

---

## 5. Health checks

**Active:** the LB probes each backend (e.g. `GET /health` every 5s). It marks a
backend down after N consecutive failures and back up after M successes. These
thresholds stop a backend flapping in and out on a single slow response.

**Passive (outlier detection):** the LB watches real traffic, and a backend returning
lots of 5xx or timeouts gets ejected for a while. This catches problems that a
`/health` endpoint misses.

### Shallow vs deep checks

| | Checks | Risk |
|---|---|---|
| **Shallow (liveness)** | "the process is up and responding" | says healthy while the server can't do real work (e.g. its DB pool is exhausted) |
| **Deep (readiness)** | "I can reach my DB, cache and dependencies" | **cascading failure:** if the DB blips, *every* instance fails its check at once, the LB removes them all, and you go from degraded to fully down |

Rule: **a health check should report what *this instance* can fix.** Check the
process's own state (startup done, not overloaded). Handle a dependency outage by
returning errors or degrading gracefully, not by pulling yourself out of the pool.
And if *all* backends fail, many LBs "fail open" and send traffic anyway; know what
yours does.

---

## 6. Session persistence (sticky sessions)

The LB pins a client to one backend, using a cookie (L7) or IP hash (L4).

**Why to avoid it:** it brings back the problem stateless clones were meant to solve
(see 01). When that server dies the user loses their state, load spreads unevenly
(one server gets a heavy user), and scaling down drops sessions.

**When it's legitimate:**
- **WebSockets / long-lived connections** (see 02). The connection *is* stateful.
- **Cache locality:** routing the same user or key to the same server keeps its local
  cache warm. Use consistent hashing so adding a server only remaps a fraction of keys.

**IP hashing is unreliable:** many users behind one corporate NAT or mobile carrier
share an IP and all land on one server.

---

## 7. TLS termination

| Option | How | Trade-off |
|---|---|---|
| **Terminate at the LB** | LB decrypts, talks plain HTTP to backends | backends save CPU, certificates are managed in one place, and the LB can do L7 routing. But traffic inside the network is unencrypted. |
| **Re-encrypt** | LB decrypts, inspects, then re-encrypts to backends (often with mTLS) | L7 features *and* encryption in transit (zero-trust networks). Costs double the TLS work. |
| **Passthrough** | LB forwards encrypted bytes untouched (L4) | true end-to-end encryption, but the LB can't see paths or headers: L4 balancing only. |

Once the LB terminates the connection, the backend sees the **LB's IP** as the
client. The real client IP arrives in the `X-Forwarded-For` header (L7) or via the
PROXY protocol (L4). Rate limiting and logging must read it from there.

---

## 8. Reverse proxy vs load balancer vs API gateway

| | What it is | Typical jobs |
|---|---|---|
| **Reverse proxy** | a server-side intermediary in front of **one or more** servers | hide backends, TLS termination, compression, caching, serving static files |
| **Load balancer** | a reverse proxy whose main job is spreading traffic across **many** servers | distribution, health checks, failover |
| **API gateway** | a reverse proxy specialised for APIs and microservices | authentication, rate limiting, request routing to services, request/response transformation, API keys |
| *(Forward proxy)* | the opposite: sits in front of **clients** | corporate egress filtering, anonymity |

The Primer's point: **a reverse proxy is useful even with one server** (security,
TLS, static files). A load balancer only makes sense with several. In practice
Nginx, HAProxy and Envoy do all of these; the names describe the *role*, not the
product.

---

## 9. Not making the LB a new SPOF

- **Active–passive pair:** two LBs share a **floating (virtual) IP**. If the active
  one stops sending heartbeats, the passive one claims the IP (VRRP / keepalived).
  Same trade-offs as fail-over in 01.
- **Active–active:** several LBs, with traffic spread over them by DNS (multiple A
  records) or by **ECMP** routing.
- **Anycast:** the same IP is announced from many locations, and internet routing
  (BGP) delivers each user to the nearest one. This is how Cloudflare and Google
  route users to a nearby edge location, and an outage at one location shifts traffic
  automatically.
- **Managed cloud LBs** (ALB, NLB, Google Cloud LB) are already redundant behind the
  scenes, so you mostly pay for this rather than build it.

---

## 10. Horizontal scaling in practice

The LB is what makes it possible to add and remove servers, but scaling is only
smooth if:
- **Servers are stateless.** Sessions live in a central store (Primer). Otherwise
  you're stuck with sticky sessions.
- **Downstream can absorb it.** 10× app servers means 10× DB connections. The
  bottleneck moves down to the DB or cache (a Primer disadvantage); connection
  pooling and DB proxies (PgBouncer) help.
- **Connection draining on removal:** stop sending new requests to an instance, let
  in-flight ones finish, then remove it. Without draining, every deploy drops
  requests.
- **Slow start on addition:** a new instance has cold caches (and a JIT warming up),
  so ramp its traffic up gradually instead of hitting it with a full share at once.
- **Autoscale on the right signal:** CPU, request rate or queue depth, whichever
  actually saturates first. Scale out fast and scale in slowly, to avoid flapping.

---

## Gotchas

- **Deep health checks cause cascades.** One shared dependency failing takes every
  instance out of the pool at once. Health checks report the instance, not its
  dependencies.
- **L4 + HTTP/2 or gRPC = one hot backend.** Long-lived multiplexed connections pin
  all requests. Use L7 or client-side balancing.
- **Idle timeout mismatch causes random 502s.** If the backend's keep-alive timeout is
  *shorter* than the LB's idle timeout, the backend closes a connection just as the LB
  reuses it. Set backend keep-alive > LB idle timeout (e.g. ALB 60s → app 65s+).
- **LB retries + non-idempotent requests = duplicates.** Only let the LB retry safe,
  idempotent methods, or requests that never reached the backend.
- **Many LBs + least connections = stampede.** Stale shared load data sends everyone
  to the same server. Use power of two choices.
- **The client IP is in `X-Forwarded-For`**, not the socket. Rate limits and audit
  logs keyed on the socket IP see only your LB. And only trust the header when it's
  set by your own proxy, since clients can forge it.
- **IP-hash stickiness breaks behind NAT.** Thousands of users can share one IP.
- **Scaling out moves the bottleneck down.** More app servers mean more DB
  connections; plan the pooling before the autoscaler finds the limit for you.
- **Cloud LBs have hidden ramp limits.** A sudden 10× spike can outrun how fast a
  managed LB scales. For planned events, pre-warm the LB or contact your provider.
