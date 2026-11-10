# 08 — Messaging

> Source: [System Design Primer](https://github.com/donnemartin/system-design-primer):
> "Asynchronism": message queues, task queues, back pressure.
> Depth: DDIA ch. 11 (stream processing); *Enterprise Integration Patterns* for the
> vocabulary.

01's fourth scaling step was **asynchronism**: don't make the user wait for work that
can happen later. This topic is the machinery for that: queues and logs between
services. They **decouple** producers from consumers (in time, speed and failure), and
in exchange the system becomes eventually consistent (07) and **duplicates become
normal**.

---

## 1. Why put a queue in between

| Benefit | Example |
|---|---|
| **Take slow work off the request path** | upload a video → respond "processing…" → transcode in the background |
| **Absorb spikes** | a flash sale makes 50k orders/s, the warehouse system handles 5k/s; the queue holds the difference |
| **Decouple failure** | the email service is down, so signups still succeed and emails are sent when it recovers |
| **Fan-out** | one "order placed" event → billing, shipping, analytics and email all react independently |
| **Scale independently** | add consumers when the queue grows, without touching producers |

**Costs:** results arrive later, failures become invisible to the user (they happen
after the response), there's another system to run and monitor, and debugging means
following a message across services.

**When not to (from the Primer):** cheap calculations and real-time workflows are
better done synchronously. A queue adds delay and complexity. If the user needs the
answer *now* (is this password correct?), don't queue it.

---

## 2. Queues and task queues (from the Primer)

**Message queue:** receives, holds and delivers messages.

```
1. App publishes a job to the queue, tells the user "in progress"
2. A worker takes the job, processes it
3. Worker signals completion (updates the DB, notifies the user)
```

**Task queue:** a queue plus a framework that runs the tasks: receives tasks and their
data, runs them, returns results, supports scheduling. For computationally heavy
background jobs. Celery (Python), Sidekiq (Ruby), BullMQ (Node).

The Primer's broker notes:
- **Redis** works as a simple broker, but **messages can be lost**.
- **RabbitMQ** is popular, but you run it yourself (it speaks the AMQP protocol).
- **Amazon SQS** is hosted, but can have higher latency, and **messages can be
  delivered twice**.

---

## 3. Point-to-point vs pub/sub

| | **Point-to-point (work queue)** | **Publish/subscribe** |
|---|---|---|
| Each message goes to | **one** consumer | **every** subscriber (each subscribing group gets its own copy) |
| Pattern | **competing consumers** share the work | **fan-out**: many independent reactions to one event |
| Use for | jobs: resize this image, send this email | events: "order placed", "user signed up" |
| Adding a consumer | splits the work further | adds a new feature, with no producer change |

Most real systems combine them: a topic fans out to several **groups**, and *within*
each group, consumers compete for the work.

---

## 4. Two kinds of broker

| | **Traditional queue** (RabbitMQ, SQS, ActiveMQ) | **Log-based** (Kafka, Kinesis, Pulsar, Redpanda) |
|---|---|---|
| Storage | message **deleted once acknowledged** | append-only log, **kept for a retention period** (days, or forever) |
| Consumer position | broker tracks each message | consumer tracks an **offset** (a position in the log) |
| Replay | no, once it's gone it's gone | **yes**: rewind the offset and reprocess |
| Ordering | weak once consumers compete | **strict within a partition** |
| Parallelism | add consumers freely | capped by the **number of partitions** |
| Per-message features | priorities, delays, per-message routing, per-message retry | few; everything happens at the partition level |
| Throughput | thousands to tens of thousands/s | **millions/s** (sequential disk writes, see 01) |
| Pick for | **task distribution**: jobs, per-message retries | **event streams**: many consumers, replay, ordering, high volume, change data capture |

---

## 5. Kafka essentials

```
topic "orders"
 ├─ partition 0: [m0][m1][m2][m3] ...  ← consumer A (group "billing")
 ├─ partition 1: [m0][m1][m2] ...      ← consumer B (group "billing")
 └─ partition 2: [m0][m1] ...          ← consumer B (group "billing")
                                          (group "analytics" reads all 3 on its own)
```

- **Topic** = a named stream. **Partition** = one ordered log. **Offset** = a
  message's position in its partition.
- **Message key → partition** (by hashing the key). Messages with the same key go to
  the same partition, so **they're processed in order**. Key by `order_id` and an
  order's events never get reordered.
- **Consumer group:** each partition is read by exactly **one** consumer in the group.
  So **more consumers than partitions means idle consumers**: the partition count is
  your maximum parallelism. Choose it with growth in mind.
- **Replication:** each partition has a leader and followers (06). Producers set
  `acks=all` so a write isn't confirmed until the in-sync followers have it; otherwise
  a leader failure can lose recent messages.
- **Retention vs compaction:** delete messages after a time limit, or **compact**:
  keep only the latest message per key (the topic becomes a changelog of current
  state).
- **Watch out for:** **hot partitions** (one busy key overloads one partition, 06's hot
  spot again), and **consumer-group rebalances**, which pause consumption whenever a
  consumer joins, leaves or stalls.

---

## 6. Delivery guarantees

| Guarantee | How | Failure |
|---|---|---|
| **At-most-once** | acknowledge, *then* process | crash after the ack → **message lost** |
| **At-least-once** | process, *then* acknowledge | crash after processing, before the ack → **redelivered, processed twice** |
| **Exactly-once** | — | not achievable over a network on its own |

**At-least-once is the practical default.** "Exactly-once" in practice means
**at-least-once delivery + idempotent processing**, giving *effectively* once.
(Kafka's exactly-once transactions cover read-from-Kafka → write-to-Kafka. Once
the consumer has side effects outside Kafka, like a DB write or an email, you're back
to needing idempotency.)

### Where duplicates come from

- A consumer crashes after doing the work but before acknowledging.
- A producer's send times out and it retries, but the first send had succeeded.
- The message's **visibility timeout** (SQS) or ack deadline expires while the
  consumer is still working, so the broker hands it to another consumer.
- A consumer-group rebalance happens before offsets were committed.

**Design every consumer as if every message will arrive twice.**

---

## 7. Idempotent consumers

| Technique | How |
|---|---|
| **Naturally idempotent operations** | `SET status = 'shipped'` is safe to repeat; `balance = balance − 10` isn't |
| **Dedup table** | store processed message IDs, and record the ID **in the same DB transaction as the effect**; skip IDs already seen |
| **Upsert / conditional write** | `INSERT … ON CONFLICT DO NOTHING`; write only if the version matches (05's optimistic concurrency) |
| **Idempotency key to downstream APIs** | pass the message ID as the payment provider's idempotency key (02) |

The dedup record and the side effect **must commit together**. If they're separate,
a crash between them brings the problem back.

---

## 8. Ordering

- **Global ordering kills parallelism.** One ordered stream means one consumer.
- **Order per key** is usually what's actually needed: all events for one order or
  one user in sequence, different keys in parallel. Kafka: key by entity. SQS FIFO:
  set the message group ID.
- **Retries break ordering.** If message 5 fails and is retried later, 6 and 7 get
  processed first. When order matters, either block the key until 5 succeeds, or make
  consumers tolerate out-of-order events (e.g. ignore events with an older version
  number than the one already applied).
- **Two producers can't guarantee order between them.** Order is only defined within
  one partition.

---

## 9. Failure handling

- **Retry with exponential backoff and jitter:** 1s, 2s, 4s, 8s… plus randomness so
  failed messages don't all retry at the same moment (09).
- **Poison messages:** a message that *always* fails (bad data, a bug) gets retried
  forever and blocks its partition. After N attempts, move it to a **dead-letter queue
  (DLQ)**.
- **Watch the DLQ.** Alert on it, and have a way to inspect, fix and replay messages.
  An unwatched DLQ is just a slower way to lose data.
- **Set the visibility timeout / ack deadline above your worst-case processing time**,
  or extend it with a heartbeat for long jobs. Otherwise a slow job gets handed to a
  second worker and runs twice in parallel.
- **Separate errors worth retrying from ones that aren't.** A timeout, a 503 or a lock
  conflict is worth retrying. A validation failure or a 400 goes straight to the DLQ.

---

## 10. Back pressure

**Problem:** a queue absorbs a spike, but if producers are *consistently* faster than
consumers, it grows without limit. The Primer's warning: once the queue no longer fits
in memory, the broker reads from disk, everything slows down, and it gets worse.

Fixes:
- **Bound the queue.** When it's full, push back on producers: return **503 / 429**
  and let clients retry with backoff (the Primer's recommendation).
- **Scale consumers on lag:** autoscale on queue depth or **age of the oldest
  message**, not CPU.
- **Expire useless messages:** a notification that's 3 hours late is worse than none.
  Give messages a TTL.
- **Shed low-priority work first.** Separate queues for critical and best-effort work,
  so analytics can't starve payments.

**Alert on consumer lag** (messages behind, and the age of the oldest). A growing
queue is a silent outage: every request "succeeds", but nothing actually happens.

---

## 11. The dual-write problem and the transactional outbox

**Problem:** a service must update its database *and* publish an event.

```
db.save(order)          ✅
broker.publish(event)   ❌ crash / timeout
→ order exists, but nobody downstream ever hears about it
```

Reversing the order is equally broken (an event for an order that was never saved).
A database transaction and a broker publish **can't be made atomic together**.

**Fix: the transactional outbox**

```
BEGIN
  INSERT INTO orders ...
  INSERT INTO outbox (event_id, type, payload) ...    ← same transaction
COMMIT

relay: reads new outbox rows → publishes → marks them sent
```

- The relay works either by **polling** the outbox table or by **change data capture**
  (Debezium tailing the DB's log).
- The relay can crash after publishing but before marking the row sent, so events can
  be published **twice**. That's at-least-once again, and consumers must be idempotent
  (section 7).
- The same idea lets CDC feed caches (04), search indexes and warehouses (05) from
  the database without dual writes.

---

## 12. Sagas: transactions across services

**Problem:** "place order" spans the order, inventory, payment and shipping services,
each with its own database. 2PC across them (06) is slow and blocks if the
coordinator fails, and most brokers and services don't support it anyway.

**Saga:** a sequence of **local transactions**, each with a **compensating action**
that undoes it semantically.

```
1. Create order (PENDING)     ↩ cancel order
2. Reserve inventory          ↩ release inventory
3. Charge payment   ❌ fails
   → run compensations in reverse: release inventory, cancel order
```

| | **Choreography** | **Orchestration** |
|---|---|---|
| How | each service listens for events and reacts ("inventory reserved" → payment charges) | a central **orchestrator** tells each service what to do next and tracks state |
| ✅ | loose coupling, no central component | the flow lives in **one place**; easy to see, change and monitor |
| ❌ | the flow is spread across services; hard to follow, cycles creep in | the orchestrator is another component with business logic in it |
| Fits | short flows, 2–4 steps | longer or more complex flows (tools: Temporal, AWS Step Functions) |

Saga rules:
- **No isolation:** other requests can see intermediate states. Model them
  explicitly (`PENDING`, `RESERVED`) rather than pretending they don't exist.
- **Compensations must be idempotent and retryable.** They're messages too.
- **Not everything can be undone.** You can't unsend an email, only send a correction.
  Put irreversible steps **last**.

---

## 13. Event-driven styles

| Style | Event carries | Trade-off |
|---|---|---|
| **Event notification** | "order 42 changed", just an ID | consumers call back for details, which couples them again and adds load |
| **Event-carried state transfer** | the full new state | consumers keep their own copy and need no callbacks; data is duplicated and eventually consistent |
| **Event sourcing** | events **are** the source of truth; current state is rebuilt by replaying them | full audit trail, time travel, replay. But complex: event schemas live forever, and you need snapshots for speed. Often paired with **CQRS** (separate read models built from the events). |

Default to **event-carried state transfer** for integrating services. Use event
sourcing only where the audit trail *is* the product (ledgers, compliance), not as a
general architecture.

---

## Gotchas

- **At-least-once is the real-world guarantee.** Every consumer must be idempotent,
  with dedup recorded in the same transaction as the effect.
- **"Exactly-once" ends at the broker boundary.** External side effects still need
  idempotency.
- **Partition count caps consumer parallelism.** Extra consumers in a group sit idle.
- **Order is per partition, per key.** Retries and multiple producers break it unless
  you design for it.
- **Poison messages block partitions.** Use a DLQ after N attempts, and actually watch
  it.
- **Visibility timeout < processing time = parallel duplicate processing.**
- **A growing queue is a silent outage.** Alert on lag and on the oldest message's
  age, not just on errors.
- **Bound your queues** and push back (503/429) instead of buffering forever.
- **Writing to the DB and publishing to the broker can't be atomic.** Use the
  transactional outbox (or CDC).
- **Sagas have no isolation, and some steps can't be undone.** Model pending states,
  and put irreversible steps last.
- **Don't queue what the user needs to know right now.** Async is for work, not
  answers.
