# System Design

High-level design (HLD): how services, storage and networks fit together at scale.
The class-level counterpart, LLD, lives in [DesignPatterns](../DesignPatterns/), so
patterns stay there and boxes-and-arrows go here.

Mostly notes, not code. Each topic's `NOTES.md` follows the repo rule: **the problem →
why the naive version breaks → the fix → the trade-off it costs.** Every building block
here is a trade, so if a note doesn't name what you give up, it isn't finished.

Two halves:

- **Per-topic folders**: one building block each (`NOTES.md`, plus a diagram when it
  helps). A folder gets created when its notes get written.
- **[case-studies/](case-studies/)**: full designs that put the blocks together. Each
  one follows the same template so they can be compared side by side.

## Topics

| # | Topic | Core idea | Notes |
|---|---|---|---|
| 01 | [fundamentals](01-fundamentals/) | estimate first: QPS, storage, bandwidth | ✅ |
| 02 | [networking-apis](02-networking-apis/) | DNS, HTTP, REST vs gRPC, WebSockets | ✅ |
| 03 | [load-balancing](03-load-balancing/) | L4 vs L7, algorithms, health checks | ✅ |
| 04 | [caching](04-caching/) | cache-aside vs write-through, eviction, CDN | ✅ |
| 05 | [databases](05-databases/) | SQL vs NoSQL, indexes, normalisation | ✅ |
| 06 | [replication-sharding](06-replication-sharding/) | leader/follower, partition keys, consistent hashing | ✅ |
| 07 | [consistency](07-consistency/) | CAP / PACELC, quorums, eventual consistency | ✅ |
| 08 | messaging | queues vs pub/sub, Kafka, delivery guarantees | ⬜ |
| 09 | reliability | rate limiting, retries, idempotency, circuit breakers | ⬜ |
| 10 | storage-search | blob storage, inverted indexes, search engines | ⬜ |
| 11 | observability | metrics, logs, traces, SLOs | ⬜ |

Order matters: 06 assumes 05, 07 assumes 06, 09 assumes 08.

## Case studies

| Design | Stresses | Status | LLD pair |
|---|---|---|---|
| URL shortener | ID generation, read-heavy caching | ⬜ | — |
| Rate limiter | distributed counters, algorithms | ⬜ | — |
| Chat / messaging | WebSockets, fan-out, ordering | ⬜ | — |
| News feed | fan-out on write vs read | ⬜ | — |
| Notification system | queues, retries, multi-channel | ⬜ | — |
| Ride sharing | geo-indexing, real-time matching | ⬜ | [DesignPatterns #24](../DesignPatterns/README.md#apply) |

### Case-study template

1. **Requirements**: functional, non-functional, explicitly out of scope
2. **Estimates**: users, QPS (read/write), storage over 5 years, bandwidth
3. **API**: the endpoints and their payloads
4. **Data model**: entities, store choice, and *why that store*
5. **High-level design**: the diagram
6. **Deep dives**: the 2–3 hardest parts
7. **Bottlenecks & trade-offs**: what breaks first at 10×, and what you chose not to do

## Progress

- Topics: 7 of 11 have notes
- Case studies: 0 of 6
