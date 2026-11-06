# 01 — Fundamentals

> Source: [System Design Primer](https://github.com/donnemartin/system-design-primer):
> "start here" (CS75 lecture, *Scalability for Dummies*), performance vs scalability,
> latency vs throughput, availability patterns, appendix tables.

Every later topic answers one question: **what breaks first as load grows, and what do
I pay to fix it?** This topic gives you the vocabulary (latency, throughput,
availability) and the arithmetic (estimation) to answer it with numbers, not vibes.

---

## 1. How a system grows (*Scalability for Dummies*)

Each step exists because the previous one broke. Learn them as a chain, not a list.

### Step 1: Clones (stateless app servers)

**Breaks:** one server can't take the traffic, so you add more behind a load balancer.
But if user state (sessions, uploads, local cache) lives *on* a server, the next request
can land on a different server and the user is suddenly logged out.

**Fix:** every app server is an identical clone holding **no user-specific state**.
Sessions go to an external store (Redis) or the DB, and files go to shared storage.
Every server is built from the same image, so any request can hit any server.

**Costs:** one more network hop per request, and a session store that must itself be
available.

> Sticky sessions (the LB pins a user to one server) look like a shortcut. They aren't:
> the user still loses state when that server dies, and load spreads unevenly.

### Step 2: Database

**Breaks:** app servers now scale horizontally, so the single DB becomes the
bottleneck.

**Two paths:**
1. **Keep the relational DB and scale it:** read replicas (master–slave), bigger
   machines, then sharding, then denormalising, then tuning queries. Each step works,
   but it gets more expensive and needs more DBA effort each time.
2. **Design for scale early:** denormalise from the start, avoid joins in the DB
   (join in the app), or use a NoSQL store that scales out natively.

**Costs:** either path eventually needs a **cache**, because the DB is still slow.

### Step 3: Cache

**Breaks:** the DB can't keep up with reads, and most reads are repeats.

**Fix:** an in-memory store (Redis, Memcached) between the app and the DB. Always read
the cache first and fall back to the DB on a miss. Two styles:

| Style | How | Problem |
|---|---|---|
| **Cache query results** | key = hash of the SQL, value = the result set | **Invalidation is miserable:** one changed row affects every cached query that touched it |
| **Cache objects** | assemble the domain object (User with profile, friends…), cache it whole | Easy to invalidate (delete one key), and the object can be built asynchronously |

Prefer **object caching**. Good candidates: sessions, fully rendered pages, activity
streams, user↔friend relationships.

**Costs:** stale data, and the work of deciding *when* to invalidate (the subject of
04-caching).

### Step 4: Asynchronism

**Breaks:** some work is too slow to do while the user waits.

**Two flavours:**
1. **Do it ahead of time:** precompute expensive output (e.g. render pages to static
   HTML nightly and serve them from a CDN).
2. **Do it after the request:** put the job on a **queue**, have workers process it,
   reply "processing…" immediately, and notify the user when it's done.

**Costs:** the result isn't immediate, jobs can fail or run twice (so workers must be
**idempotent**), and there's another system (the queue) to run. Covered in 08-messaging.

---

## 2. Performance vs scalability

| | Symptom | Example |
|---|---|---|
| **Performance problem** | slow **for a single user** | a page takes 3s with one user on the system |
| **Scalability problem** | fast for one user, slow **under load** | 100ms at 10 RPS, 5s at 10,000 RPS |

**Scalable** means adding resources gives a *proportional* increase in capacity. If
doubling servers gives you 1.2× throughput, something shared (DB, lock, single queue) is
the real bottleneck.

> Fix performance first. Scaling out a slow system just gives you many slow servers,
> and a bigger bill.

---

## 3. Latency vs throughput

- **Latency:** time to complete one operation (ms).
- **Throughput:** operations completed per unit of time (RPS, MB/s).

**Goal:** maximise throughput *while keeping latency acceptable*.

They often trade against each other. **Batching** is the classic case: grouping writes
(Kafka producer batching, DB group commit) raises throughput because there's less
per-operation overhead, but each item waits for its batch, so latency goes up.

> Measure latency at **percentiles (p95, p99)**, not averages. An average of 50ms can
> hide 1% of users waiting 2s, and that 1% is often your heaviest users.

---

## 4. Availability

**Availability** = uptime ÷ total time, quoted in "nines".

### In numbers

| | Per year | Per month | Per week | Per day |
|---|---|---|---|---|
| **99.9%** (three nines) | 8h 46m | 43m 50s | 10m 5s | 1m 26s |
| **99.99%** (four nines) | 52m 36s | 4m 23s | 1m 5s | 8.6s |

Each extra nine cuts allowed downtime by 10×, and usually costs much more than 10×.

**Combining components:**

```
In sequence (request needs A AND B):    A_total = A × B
    99.9% × 99.9%            = 99.8%    (worse than either)

In parallel (request needs A OR B):     A_total = 1 − (1 − A)(1 − B)
    1 − (0.001 × 0.001)      = 99.9999% (far better than either)
```

So **every dependency in the request path lowers availability**, and **redundancy
raises it**. A chain of 10 services at 99.9% each is about 99.0%: roughly 3.6 days of
downtime a year.

### Fail-over

| | How it works | Downside |
|---|---|---|
| **Active–passive** | Heartbeats between active and standby. If heartbeats stop, the passive takes over the active's IP. Only the active serves traffic. | Standby capacity sits idle. Failover isn't instant (a *hot* standby is faster than a *cold* one that has to boot). Risk of **split-brain** if both think they're active. |
| **Active–active** | Both nodes serve traffic, and load is spread across them (via DNS, a load balancer, or app logic). | Concurrent writes can **conflict**. Each node must be sized to carry the **full load** alone, or losing one overloads the other and it fails too (a cascade). |

Both share a risk: **data loss** if the active node dies before its latest writes
reach the other node. Replication itself (master–slave, master–master) is covered in
06-replication-sharding.

---

## 5. Numbers to know cold

### Powers of two

| Power | Exact | ≈ | Unit |
|---|---|---|---|
| 2¹⁰ | 1,024 | 1 thousand | 1 KB |
| 2¹⁶ | 65,536 | 64 thousand | 64 KB (max ports, `short` range) |
| 2²⁰ | 1,048,576 | 1 million | 1 MB |
| 2³⁰ | ~1.07 × 10⁹ | 1 billion | 1 GB |
| 2³² | ~4.3 × 10⁹ | 4 billion | 4 GB (max `int` unsigned, IPv4 space) |
| 2⁴⁰ | ~1.1 × 10¹² | 1 trillion | 1 TB |

### Latency numbers

The exact values drift with hardware. **Learn the orders of magnitude and the gaps
between them.**

| Operation | Time | Feel |
|---|---|---|
| L1 cache reference | 0.5 ns | |
| Main memory reference | 100 ns | 200× L1 |
| Compress 1 KB (fast codec) | 10 µs | |
| Read 4 KB randomly from SSD | 150 µs | |
| Read 1 MB sequentially from memory | 250 µs | |
| Round trip **within a datacenter** | 500 µs | **5,000× a memory read** |
| Read 1 MB sequentially from SSD | 1 ms | 4× memory |
| HDD seek | 10 ms | |
| Read 1 MB sequentially from HDD | 30 ms | 120× memory |
| Packet CA → Netherlands → CA | 150 ms | **300× a datacenter round trip** |

What follows from these:
- **Memory ≫ SSD ≫ network ≫ HDD seek ≫ cross-continent.** Keep hot data in memory.
- **A network call costs about the same as thousands of memory reads.** Calling a
  service or DB in a loop (N+1 queries) is the most common self-inflicted latency.
- **Sequential beats random**, especially on disk. Log-structured storage and Kafka
  are built on this.
- **Cross-region round trips (~150 ms) are physics.** You can't optimise them away,
  only avoid them: replicate data closer to users and put a CDN at the edge.
- Throughput rules of thumb: HDD ~30 MB/s, 1 Gbps network ~100 MB/s, SSD ~1 GB/s,
  memory ~4 GB/s. About 2,000 round trips/s inside a datacenter, and about 6–7/s
  across the world.

---

## 6. Back-of-the-envelope method

The aim is the **order of magnitude**: is this one machine, ten, or a thousand?
Round everything aggressively.

```
1. Users         DAU (daily active users)
2. Actions       actions per user per day, split into reads and writes
3. QPS           daily actions ÷ 10⁵            (1 day = 86,400 s ≈ 10⁵ s)
4. Peak          QPS × 2–3                      (traffic isn't flat)
5. Storage       writes/day × size × retention × replication
6. Bandwidth     QPS × payload size             (bytes → bits: × 8)
7. Verdict       which number is scary? That number drives the design.
```

### Exercise: photo-sharing app

- 100M daily active users
- Each user uploads 0.1 photos/day, average 500 KB
- Each user views 50 photos/day
- Keep photos for 5 years, stored with 3× replication

Work out: write QPS, read QPS and the read:write ratio, storage per day and over
5 years, and egress bandwidth.

<details>
<summary>Worked answer (try it first)</summary>

**Write QPS**
- 100M × 0.1 = **10M uploads/day**
- 10M ÷ 10⁵ = **~100 writes/s** average, so **~300/s at peak**

**Read QPS**
- 100M × 50 = **5B views/day**
- 5 × 10⁹ ÷ 10⁵ = **~50,000 reads/s** average, so **~150,000/s at peak**
- Read:write = 5B : 10M = **500 : 1**, which is extremely read-heavy

**Storage**
- 10M × 500 KB = **5 TB/day**
- 5 TB × 365 × 5 ≈ **9 PB** of raw photos
- × 3 replication ≈ **27 PB**
- Metadata (~1 KB per photo row): 10M × 1 KB = 10 GB/day, which is ~18 TB over 5
  years. That's small next to the photos, but too much for one DB node eventually.

**Egress bandwidth**
- 50,000 reads/s × 500 KB = **25 GB/s**, which is **~200 Gbps** average and
  **~600 Gbps** at peak

**Verdict: what drives the design**
- **Writes are trivial.** 300/s fits on one DB primary for metadata.
- **Egress is the scary number.** 200–600 Gbps can't come from your own servers, so
  you need a **CDN**, and you should serve resized thumbnails rather than 500 KB
  originals for feeds.
- **Storage is the second scary number.** 27 PB goes in **object storage** (an S3-style
  blob store), never in the DB, with **tiering**: old photos are rarely viewed, so move
  them to cheaper cold storage.
- **A 500:1 read ratio** means read replicas and caching for metadata, and the photos
  themselves are immutable, which makes them perfect for caching.

</details>

---

## Gotchas

- **Peak ≠ average.** Size for peak (×2–3), plus headroom for spikes like launches,
  sales and retries after an outage. Sizing for the average means falling over every
  evening.
- **1 day ≈ 10⁵ s overestimates QPS by ~15%.** That's fine, because erring high is
  the safe direction.
- **Replication multiplies storage.** So do backups, indexes and derived copies
  (thumbnails). Raw data × 3–5 is a realistic total.
- **Bits vs bytes.** Network speeds are quoted in **bits** (Gbps), and storage in
  **bytes** (GB). 1 GB/s = 8 Gbps. Mixing them up is an 8× error.
- **Parallel redundancy only helps if failures are independent.** Two replicas in the
  same rack, region or deploy fail together. Correlated failure is why the
  1 − (1 − A)² formula is optimistic.
- **Tail latency multiplies with fan-out.** If a request calls 100 servers in parallel
  and each is slow 1% of the time, then 1 − 0.99¹⁰⁰ ≈ **63%** of requests are slow.
  At scale, p99 *is* the typical experience.
- **Statelessness comes before horizontal scaling.** You can't add clones until state
  is out of the app servers.
- **Async moves the problem, it doesn't remove it.** Queued jobs fail and retry, so
  workers must be idempotent, and someone has to watch the dead-letter queue.
