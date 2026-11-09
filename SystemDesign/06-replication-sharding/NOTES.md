# 06 — Replication & Sharding

> Source: [System Design Primer](https://github.com/donnemartin/system-design-primer):
> "Database": master-slave replication, master-master replication, federation,
> sharding (and its consistent hashing link).
> Depth: DDIA ch. 5 (replication) and ch. 6 (partitioning).

05 was one database on one machine. Eventually that machine runs out: of read
capacity, write capacity, disk, or the luck to never fail. There are two ways to use
more machines, and they solve different problems:

| | **Replication** | **Sharding (partitioning)** |
|---|---|---|
| What | **the same data** copied onto several nodes | **different data** on different nodes |
| Solves | availability (survive a node dying), **read** scaling, latency (a copy near users) | **write** scaling, data too big for one node |
| Doesn't solve | write capacity: every replica still applies every write | availability: lose a shard, lose its data |

Real systems use **both**: the data is split into shards, and each shard is
replicated.

```
            ┌── shard A (users a–h) ── leader ─► follower, follower
router ─────┼── shard B (users i–p) ── leader ─► follower, follower
            └── shard C (users q–z) ── leader ─► follower, follower
```

---

## 1. Leader–follower replication (master–slave)

**All writes go to one leader.** The leader streams its change log to the followers,
which apply it in the same order. Reads can go to any node.

- ✅ Simple, no write conflicts (one place decides order), read capacity grows with
  each follower.
- ❌ (from the Primer) Promoting a follower when the leader dies needs extra logic.
  Followers lag behind the leader. More followers mean more replication load on the
  leader. **Writes can be lost** if the leader dies before they reach a follower.
- Writes are still limited to what **one machine** can handle.

### Synchronous vs asynchronous

| | Leader waits for | Durability | Write latency / availability |
|---|---|---|---|
| **Synchronous** | all (or a chosen) follower to confirm | no loss if the leader dies | slow, and **one stuck follower blocks every write** |
| **Asynchronous** | nobody | **recent writes lost** if the leader dies | fast; followers can fall behind |
| **Semi-synchronous** | **one** follower to confirm, the rest async | at least two copies of every write | the usual compromise |

Most setups are async or semi-sync. Fully synchronous replication to every follower is
rare, because availability then depends on the slowest node.

### Replication lag and its anomalies

With async followers, a read from a follower can return old data. Three visible
problems, and their fixes:

| Problem | What the user sees | Fix |
|---|---|---|
| **Read-your-writes** | edits their profile, refreshes, sees the old one | read the user's *own* data from the leader (or from the leader for N seconds after they write); or track the log position of their last write and only read from a follower that has caught up |
| **Monotonic reads** | refreshes twice and data goes *backwards* (second read hit a laggier follower) | pin each user to **one** follower (e.g. by hashing the user ID) |
| **Consistent prefix** | sees a reply before the question it answers | write causally related data to the same shard, so it's applied in order |

Lag is normally milliseconds, but **under load or after a failure it can grow to
minutes**, and the system has to stay correct then too.

### Failover

When the leader dies:
1. **Detect it.** Usually a timeout: no heartbeat for N seconds.
2. **Choose a new leader.** Ideally the follower with the most recent data, chosen by
   consensus among the nodes or by a controller.
3. **Reconfigure.** Clients send writes to the new leader, and the old leader must
   step down if it comes back.

What goes wrong:
- **Lost writes:** with async replication, writes the old leader accepted but never
  sent are gone. If the old leader returns, those writes get thrown away, which can
  clash with things like IDs already handed to other systems.
- **Split brain:** two nodes both think they're the leader and both accept writes,
  causing conflicting data. Prevented by **fencing**: each leader term gets an
  increasing *epoch* number, and storage rejects writes carrying an old epoch (or the
  old node gets forcibly shut off).
- **The timeout is a trade-off:** too short, and a slow moment or a GC pause triggers
  an unnecessary failover (which adds load and can cascade); too long, and you're
  down while waiting.

Automatic failover is hard to get right. Many teams use managed databases (RDS, Cloud
SQL) or proven tools (Patroni for Postgres) rather than writing their own.

---

## 2. Multi-leader replication (master–master)

**Several nodes accept writes** and replicate to each other.

- ✅ Writes keep working if one leader dies, and each datacenter can have a local
  leader for fast writes.
- ❌ (from the Primer) Writes need a load balancer or app logic to pick a leader. The
  setup is either **loosely consistent** (giving up some ACID guarantees) or has
  slower writes because of synchronisation. **Conflict resolution** gets harder as you
  add write nodes.

**Where it fits:** multiple datacenters (one leader per region); clients that work
offline (each phone is effectively a leader that syncs later); collaborative editing
(Google Docs).

### Conflicts

Two leaders accept different writes to the same record at the same time. Options:

| Strategy | How | Cost |
|---|---|---|
| **Avoid** | route all writes for a given record to one "home" leader (e.g. by user's region) | conflicts only happen if the home changes |
| **Last write wins (LWW)** | keep the write with the latest timestamp | **silently drops** the other write, and clock skew between machines can make the "latest" wrong |
| **Merge** | combine both (union a set, add both counter increments); **CRDTs** are data types designed to merge automatically | only works for data that has a meaningful merge |
| **Ask the app or the user** | store both versions and resolve on next read | complexity is pushed onto the app |

**Prefer avoiding conflicts.** LWW is the easy default, but it loses data and no one
notices.

---

## 3. Leaderless replication (Dynamo-style)

No leader at all. The client (or a coordinator node) writes to **N replicas** and
counts it as successful when **W** confirm. Reads query **R** replicas and take the
newest value.

- **Quorum rule:** if **W + R > N**, every read overlaps at least one node that has
  the latest write. A common choice: N = 3, W = 2, R = 2.
- **Repairing replicas that missed a write:** *read repair* (a read finds a stale node
  and fixes it), *anti-entropy* (a background job compares replicas, using Merkle trees
  to find differences cheaply), *hinted handoff* (another node holds the write for a
  dead node and passes it on when it recovers).
- ✅ No failover step; the system tolerates slow or dead nodes.
- ❌ Conflicts are possible (as with multi-leader), and quorums aren't the same as
  strong consistency in edge cases.
- Used by **Cassandra, ScyllaDB, Riak**, and the original Dynamo design.

What quorums do and don't guarantee is part of 07-consistency.

---

## 4. Federation (functional partitioning)

Split the database **by function**: users in one DB, forums in another, products in a
third.

- ✅ (from the Primer) Less read and write traffic per DB, so less replication lag;
  smaller databases fit more data in memory, so more cache hits; writes go to
  separate DBs in parallel.
- ❌ Doesn't help if one function has a huge table on its own. The app must know which
  DB holds what. **Joins across databases** become app-side work. More hardware and
  more complexity.

Federation often happens naturally when moving to microservices: each service owns
its own database.

---

## 5. Sharding (horizontal partitioning)

Split **the rows of one table** across nodes by a **shard key**: users 1–1M on shard
A, 1M–2M on shard B.

- ✅ (from the Primer) Less traffic and less replication per shard, more cache hits,
  **smaller indexes** so faster queries. If one shard goes down, the others keep
  serving (replicate each shard to avoid losing data).
- ❌ The app or a router needs logic to find the right shard. Data and load can be
  **lopsided** (one shard holds the power users). **Rebalancing** is complex. **Joins
  and transactions across shards** are hard.

*(Vertical partitioning splits **columns** instead: rarely used wide columns such as
`bio` or `settings_json` move to a separate table. That's a schema technique more
than a scaling one.)*

### Sharding strategies

| Strategy | How | ✅ | ❌ |
|---|---|---|---|
| **Range** | key ranges per shard (A–H, I–P, …; or by date) | **range queries** stay on one shard; easy to reason about | **hot spots**: a time-ordered key sends *all* new writes to the newest shard |
| **Hash (mod N)** | `shard = hash(key) % N` | even spread | no range queries; **changing N remaps almost every key** |
| **Consistent hashing** | keys and nodes on a hash ring (below) | adding or removing a node moves only **~1/N** of keys | no range queries; needs virtual nodes for an even spread |
| **Fixed partitions** | create many partitions up front (say 1,000 for 10 nodes) and move whole partitions between nodes | rebalancing = moving partitions, keys never re-hash | must choose the count early (Elasticsearch fixes the primary shard count at index creation) |
| **Directory / lookup** | a lookup service maps key → shard | completely flexible; move any key anywhere | the lookup is an extra hop, and **a critical dependency** (cache it, replicate it) |

### Consistent hashing

**Problem with mod N:** going from 4 to 5 servers changes `hash(key) % N` for about
**80%** of keys. In a cache, that's an instant mass of misses (an avalanche, see 04).
In a database, it means moving most of the data.

**How it works:**
1. Picture the hash space (0 … 2³²−1) as a **ring**.
2. Hash each **node** to a position on the ring.
3. Hash each **key** to a position, then walk **clockwise**: the first node you reach
   owns the key.
4. **Adding a node** takes keys only from its clockwise neighbour, about 1/N of all
   keys. **Removing a node** hands its keys to its neighbour.

**Virtual nodes:** with only a few points on the ring, ranges are uneven and one
node's removal dumps everything onto one neighbour. So each physical node is placed at
**many points** (100–200 virtual nodes):
- the load evens out statistically
- a dead node's keys spread over *many* nodes, not one
- a bigger machine can get more virtual nodes in proportion to its capacity

Used by: Dynamo, Cassandra, Riak, Memcached clients (ketama), CDNs and load balancers
for cache locality (see 03).

### Choosing a shard key

The most important decision, and expensive to change later. A good key:
- has **high cardinality** (many distinct values, so it can spread across many shards)
- spreads load **evenly** (not just data: a key with even data but one hot value is
  still hot)
- **matches the main access pattern**, so common queries hit *one* shard
- isn't **monotonic** when using range sharding (timestamps and auto-increment IDs
  pile all writes onto the last shard)

Examples:
- **Chat:** shard by `conversation_id`, so a conversation's messages are on one shard
  and reading history is a single-shard query.
- **E-commerce orders:** shard by `user_id`, so "my orders" hits one shard. But "all
  orders for merchant X" now has to ask every shard, or needs a secondary index.

**No key serves every query.** Pick the key for the most frequent, latency-sensitive
query, and handle the rest with secondary indexes or a separate copy of the data.

### Hot spots

Even a good key can have one celebrity value (a viral post getting millions of likes).
Spread that key across shards by **adding a random suffix** (`post123#0 …
post123#9`): writes spread across 10 shards, and reads gather from all 10. Only do this
for keys you know are hot. Same idea as the cache hot key in 04.

### Secondary indexes on sharded data

| | **Local index** (per shard) | **Global index** (sharded by the indexed value) |
|---|---|---|
| How | each shard indexes only its own rows | the index itself is partitioned by the indexed term |
| Writes | update one shard | can touch several shards, so the index is often updated **asynchronously** (it lags) |
| Reads by the indexed field | **scatter-gather**: ask *every* shard and merge | ask one index shard |
| Watch out | tail latency: the query is as slow as the **slowest** shard (01's fan-out gotcha) | stale index results |

### Cross-shard joins and transactions

- **Joins:** denormalise (05) so the data needed together lives together, or join in
  the application.
- **Transactions:**
  - **Two-phase commit (2PC):** all shards prepare, then all commit. Gives atomicity,
    but it's slow, and if the coordinator dies mid-way, shards wait with locks held.
  - **Sagas:** a chain of local transactions, each with a compensating undo step.
    Covered with messaging in 08.
  - **Best: choose a shard key so transactions stay within one shard.**

### Unique IDs across shards

Auto-increment IDs break once there are several shards, since each would count from 1
on its own. Options:

| | ✅ | ❌ |
|---|---|---|
| **UUID v4** | no coordination needed | 128 bits; random, so B-tree inserts land all over the index |
| **UUID v7 / ULID** | time-ordered, no coordination | 128 bits |
| **Snowflake-style** (timestamp + machine ID + per-machine sequence, in 64 bits) | compact, **sortable by time**, no coordination per ID | needs machine IDs assigned, and clocks must not go backwards |
| **Ticket server / ranges** | a central service hands out ID *blocks* | a central dependency (blocks reduce calls to it) |

### Rebalancing

Moving data while still serving traffic: copy the data, catch up on changes made in
the meantime, switch reads and writes over, then delete the old copy. Throttle it so
the copying doesn't starve real traffic. Many teams **keep rebalancing manually
triggered**: an automatic rebalance that kicks in during a load spike adds load at
the worst possible moment.

---

## 6. When to shard

**Sharding is a one-way door.** Undoing it is close to impossible, and it makes
everything harder: joins, transactions, schema changes, operations. Try the cheaper
options first, roughly in this order:

1. Fix queries and indexes (05).
2. Cache (04).
3. Add read replicas (section 1), if reads are the bottleneck.
4. Scale up: a bigger machine. Today's large instances handle a lot.
5. Federation: split by function or service (section 4).
6. Archive or partition cold data so the hot set stays small.
7. **Then** shard, choosing the key carefully. Or use a database that shards for you
   (Cassandra, DynamoDB, CockroachDB, Vitess for MySQL, Citus for Postgres).

---

## Gotchas

- **Replicas scale reads, not writes.** Every replica applies every write. Write
  scaling needs sharding.
- **Async replication means a failover can lose data.** Be clear about whether your
  setup acknowledges writes before a second copy exists.
- **Read-your-writes breaks with followers.** Route a user's reads of their own data
  to the leader, or to a follower that has caught up.
- **Split brain is the dangerous failover failure.** Use fencing (epochs), not just a
  timeout.
- **Last-write-wins silently loses data,** and clock skew picks the wrong winner.
  Avoid conflicts by giving each record one home leader.
- **`hash % N` remaps ~everything when N changes.** Use consistent hashing or fixed
  partitions.
- **Monotonic shard keys create one hot shard** under range sharding.
- **No shard key fits every query.** Queries by anything else need scatter-gather or a
  global (lagging) index.
- **Scatter-gather is as slow as the slowest shard,** and it gets worse as you add
  shards.
- **Keep transactions inside one shard.** 2PC across shards is slow and blocks when
  the coordinator fails.
- **Auto-increment IDs don't work across shards.** Use Snowflake-style IDs or
  UUIDv7/ULID.
- **Shard last.** Indexes, caching, replicas and a bigger box come first. Sharding
  can't easily be undone.
