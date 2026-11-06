# 02 — Networking & APIs

> Source: [System Design Primer](https://github.com/donnemartin/system-design-primer):
> "Domain name system", "Communication" (TCP, UDP, HTTP, RPC, REST). The Primer's
> CDN section sits between them, but belongs with 04-caching.

Before anything scales, a request has to *arrive*. This topic is the path a request
takes (DNS → connection → protocol → API) and what each hop costs. Most "the app is
slow" problems from far away are really **round-trip problems**, and every choice
here is about how many round trips you pay and how much state you hold.

---

## 1. What happens when you open `https://example.com`

```
1. DNS lookup        example.com → 93.184.216.34         0 RTT if cached, else 1+
2. TCP handshake     SYN → SYN-ACK → ACK                  1 RTT
3. TLS handshake     agree keys, verify certificate       1 RTT (TLS 1.3) / 2 (TLS 1.2)
4. HTTP request      GET / → response                     1 RTT
```

A **cold** HTTPS request costs **3–4 round trips** before the first byte of the page.
Across the world (~150 ms RTT) that's **~500 ms** spent on setup alone.

That one fact explains most of this topic:
- **Reuse connections** (keep-alive, connection pools) so you pay setup once.
- **Cache DNS** so you skip step 1.
- **Move servers closer** (CDN, edge, multi-region) because you can't cut the RTT itself.
- **HTTP/3** merges steps 2 and 3 to save a round trip.

---

## 2. DNS

**Problem:** humans and configs use names, but the network routes by IP address.

### How a lookup works

```
client → OS cache → resolver (ISP or 8.8.8.8)
                       ├→ root server       "ask the .com servers"
                       ├→ TLD server (.com)  "ask example.com's nameservers"
                       └→ authoritative NS   "93.184.216.34, TTL 300"
```

Every layer **caches the answer for its TTL**, so most lookups never leave the
resolver.

### Record types

| Record | Maps | Use |
|---|---|---|
| **A / AAAA** | name → IPv4 / IPv6 | the actual address |
| **CNAME** | name → another name | `www` → `example.com`, or → `myapp.cdn.net` |
| **NS** | domain → its nameservers | who is authoritative |
| **MX** | domain → mail servers | email routing |
| **TXT** | domain → text | domain ownership proofs, SPF/DKIM |

### DNS as a traffic router

Managed DNS (Route 53, Cloudflare) can answer differently per request:
- **Weighted round robin:** split traffic across servers (or 5% to a canary).
- **Latency-based:** send the user to the region with the lowest latency.
- **Geolocation:** send by country (data residency, localised content).
- **Health-checked failover:** stop returning a dead endpoint's IP.

**Costs:** DNS routing is **coarse and slow to change**. Clients and resolvers cache
answers for the TTL (and some ignore it and cache longer), so a failover can't take
effect faster than the TTL. A low TTL means faster failover but more DNS queries. DNS
is also a juicy DDoS target: the 2016 attack on Dyn took Twitter, GitHub and others
offline because their DNS couldn't answer.

---

## 3. TCP vs UDP

| | **TCP** | **UDP** |
|---|---|---|
| Connection | yes (handshake, then a stream) | no (fire individual datagrams) |
| Delivery | guaranteed, **in order**, no duplicates | may drop, reorder or duplicate |
| Control | flow + congestion control | none, app decides |
| Cost | handshake, retransmits, per-connection memory | minimal |
| Used by | HTTP/1.1 & 2, databases, SSH, most APIs | DNS, video/voice calls, games, **QUIC (HTTP/3)** |

**Choose TCP** when every byte must arrive intact (an API response, a file, a DB
query). **Choose UDP** when *fresh* matters more than *complete*: in a video call, a
packet that arrives late is useless, so retransmitting it only adds lag.

**TCP's weakness, head-of-line blocking:** bytes are delivered in order, so one lost
packet stalls everything behind it until it's retransmitted, even data that arrived
fine.

**Each TCP connection holds memory on the server**, so connections are a resource.
Use **connection pools** (to the DB, between services) rather than opening one per
request.

---

## 4. HTTP

A **stateless request/response** protocol: each request carries everything the server
needs (auth token, parameters). Statelessness is what makes app servers easy to clone
(see 01).

### Methods: safe and idempotent

| Method | Purpose | Safe (no side effects) | Idempotent (repeat = same result) |
|---|---|---|---|
| GET | read | ✅ | ✅ |
| PUT | replace / create at a known URL | ❌ | ✅ |
| DELETE | remove | ❌ | ✅ |
| POST | create / trigger an action | ❌ | ❌ |
| PATCH | partial update | ❌ | not guaranteed |

**Why idempotency matters:** networks fail mid-request, and the client can't tell
whether the server processed it. Idempotent requests can be **retried safely**. A
retried POST can charge a card twice; see *Idempotency keys* in section 7.

### Status codes worth knowing

| Code | Meaning | Note |
|---|---|---|
| 200 / 201 / 204 | OK / Created / No content | |
| 301 / 302 | moved permanently / temporarily | 301 gets cached by browsers, so it's hard to undo |
| 304 | not modified | cache revalidation, no body sent |
| 400 | bad request | client's fault, don't retry unchanged |
| 401 / 403 | not authenticated / not allowed | 401 = "who are you?", 403 = "I know, and no" |
| 404 / 409 | not found / conflict | 409: version mismatch, duplicate |
| 429 | too many requests | rate limited, honour `Retry-After` |
| 500 | server error | a bug |
| 502 / 504 | bad gateway / gateway timeout | the **proxy/LB** couldn't get a good answer from the backend |
| 503 | service unavailable | overloaded or in maintenance, retry later |

Rule: **4xx = don't retry as-is; 5xx and 429 = retry with backoff.**

### Versions

| | Key change | Remaining problem |
|---|---|---|
| **HTTP/1.1** | keep-alive (reuse the connection) | one request at a time per connection, so browsers open ~6 connections per host |
| **HTTP/2** | **multiplexing:** many requests over one connection as interleaved streams; binary; compressed headers | still on TCP, so one lost packet stalls *all* streams |
| **HTTP/3** | runs on **QUIC (over UDP)**: streams are independent, transport + TLS handshake in 1 RTT (0 RTT on resume), connection survives a network switch (Wi-Fi → mobile) | UDP is sometimes blocked by networks, so clients fall back to HTTP/2 |

---

## 5. API styles: REST vs RPC vs GraphQL

### REST

Model the system as **resources** (nouns) and act on them with HTTP methods (verbs):
`GET /users/42`, `POST /users/42/orders`.

- **Stateless** and **uniform**, so any client can use it with no special library.
- **Cacheable:** GET responses work with HTTP caches and CDNs for free.
- **Weaknesses:** not every action fits a resource (`POST /orders/7/cancel` is RPC in
  disguise). And a fixed response shape means **over-fetching** (getting fields you
  don't need) or **under-fetching** (needing several calls to build one screen).

### RPC (gRPC, Thrift)

Call a **remote function** as if it were local: `getUser(42)`. A generated client stub
serialises the call and sends it over the network.

- **gRPC:** Protobuf (compact binary, typed schema) over HTTP/2, with streaming in
  both directions. Fast and strongly typed.
- **Good for:** internal service-to-service calls, where performance matters and you
  control both ends.
- **Weaknesses:** tightly coupled (a schema change means regenerating clients). Not
  human-readable. Browsers can't call gRPC directly (they need a gRPC-Web proxy).
  Hides the network, which tempts people to treat remote calls as cheap. They aren't
  (see the latency table in 01).

### GraphQL

One endpoint, and **the client says exactly which fields it wants**.

- Solves over- and under-fetching: one request returns exactly one screen's data.
  Good for mobile and for many differently-shaped clients.
- **Weaknesses:** HTTP caching mostly stops working (queries are usually POSTs to one
  URL). The server can hit **N+1 queries** internally (fixed with batching, e.g.
  DataLoader). One expensive query can overload the server, so you need depth and
  cost limits.

### Which to choose

| Situation | Choose |
|---|---|
| Public API, third-party developers | **REST** |
| Internal microservices, high volume | **gRPC** |
| Many client types (web, iOS, Android) needing different shapes of the same data | **GraphQL** (often as a gateway in front of REST/gRPC services) |

---

## 6. Real-time: getting data *to* the client

HTTP is client-initiated. When the server has news, there are four options:

| Technique | How | Good for | Cost |
|---|---|---|---|
| **Short polling** | client asks every N seconds | rare updates, simple setups | mostly empty responses; delay up to N |
| **Long polling** | server holds the request open until it has data or times out, then the client reconnects | moderate updates, works everywhere | one held request per client, reconnect overhead |
| **Server-Sent Events (SSE)** | one long-lived HTTP response streaming events | **server → client only**: feeds, notifications, live scores, LLM token streaming | one direction only; auto-reconnects |
| **WebSockets** | HTTP connection upgraded to a **full-duplex** channel | **both directions, low latency**: chat, multiplayer, collaborative editing | **stateful** connections (see below) |

**The real cost of WebSockets is state.** Each user is attached to *one specific
server*, which breaks the stateless-clone model from 01:
- To message user B, you must find **which server holds B's connection**. That needs
  a pub/sub layer (Redis pub/sub, Kafka) or a connection registry.
- Deploys and scale-downs **drop connections**, so clients need reconnect-with-backoff
  logic and servers need to drain connections gracefully.
- A server holding 100k idle connections is limited by memory and file descriptors,
  not CPU.

For **server → server** notifications, use **webhooks**: you give the other service a
URL, and it POSTs to you when something happens. Webhooks must be retried and
verified (signed), and the receiver must be idempotent because duplicates will
happen.

---

## 7. API design essentials

### Idempotency keys

The client generates a unique key per *logical* operation and sends it as a header
(`Idempotency-Key: 8f3a…`). The server stores key → result. On a retry with the same
key, it returns the stored result instead of acting again. This is how payment APIs
(Stripe) make POST safe to retry.

### Pagination

| | How | Problem |
|---|---|---|
| **Offset** | `?limit=20&offset=1000` | DB still scans and discards 1000 rows, so deep pages are slow. Inserts during paging cause skipped or repeated items. |
| **Cursor** | `?limit=20&after=<last_id>` → `WHERE id > last_id` | uses an index, constant speed, stable under inserts. Can't jump to "page 50". |

Use **cursor pagination** for feeds and large or changing data. Offset is fine for
small admin tables.

### Versioning

`/v1/users` (in the URL: explicit, easy to route and cache) or a header
(`Accept: application/vnd.app.v2+json`, which keeps URLs clean). Either way: **add
fields freely, never remove or rename them** within a version. Clients break on
removals, not on additions.

### Timeouts and rate limits

- **Every network call needs a timeout.** Without one, a slow dependency holds your
  threads until you run out and go down too.
- Rate-limit clients and return **429 with `Retry-After`** (algorithms in
  09-reliability).

---

## Gotchas

- **Setup round trips dominate at distance.** A cold HTTPS request is 3–4 RTTs. Reuse
  connections, and put servers near users.
- **DNS failover is only as fast as the TTL**, and some clients ignore the TTL. Don't
  rely on DNS alone for quick failover; put a load balancer behind it.
- **POST isn't safe to retry** without an idempotency key. Timeouts plus automatic
  retries plus non-idempotent writes equals double charges.
- **HTTP/2 doesn't remove head-of-line blocking**, it moves it down to TCP. On lossy
  networks (mobile), HTTP/3 is the real fix.
- **RPC makes remote calls look local**, and they're ~5,000× slower than a memory read.
  Watch for N+1 calls hidden behind innocent-looking methods.
- **WebSockets make servers stateful.** Plan the pub/sub routing and reconnect logic
  before choosing them; if traffic is server → client only, SSE is simpler.
- **Offset pagination degrades with depth** and skips items when data changes. Use
  cursors for anything user-facing and large.
- **Never remove a field from a live API version.** Add a new field and deprecate the
  old one.
