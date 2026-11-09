# 07 — Consistency

> Source: [System Design Primer](https://github.com/donnemartin/system-design-primer):
> "Availability vs consistency" (CAP theorem, CP vs AP) and "Consistency patterns"
> (weak, eventual, strong).
> Depth: DDIA ch. 5 (quorums) and ch. 8–9 (clocks, linearizability, consensus).

06 put the data on several machines. This topic asks **what a reader is allowed to
see** while those copies disagree, and what it costs to make them agree. Every answer
is a trade between **correctness, latency and availability**, and the right answer is
usually different for different data in the same product.

> "Consistency" means different things in different places. In **ACID** (05) it means
> *constraints hold*. In **CAP** and in this topic, it means *all copies look like one
> up-to-date copy*. Same word, unrelated ideas.

---

## 1. Consistency patterns (from the Primer)

| Pattern | After a write, reads… | How | Examples |
|---|---|---|---|
| **Weak** | may or may not see it; best effort | no effort to propagate reliably | memcached, video calls, multiplayer games: losing a moment of data is fine |
| **Eventual** | will see it **eventually**, typically within milliseconds | asynchronous replication | DNS, email, Cassandra / DynamoDB by default; highly available systems |
| **Strong** | **always** see it immediately | synchronous replication / consensus | relational DBs on one node, file systems, Spanner; anything with transactions |

"Eventually" comes with **no deadline**. Normally it's milliseconds, but during a
failure or under heavy load it can be minutes, and in the meantime a client can read
any older value.

---

## 2. The spectrum of consistency models

Strongest to weakest. Stronger is easier to reason about, and costs more latency and
availability.

| Model | Guarantee | Cost |
|---|---|---|
| **Linearizable** ("strong") | behaves like **a single copy**: once a write completes, every later read (by anyone) sees it, in real-time order | every operation coordinates with a majority, so it's slow across regions and unavailable during partitions |
| **Sequential** | everyone sees operations in the **same order**, but not necessarily real-time order | still needs global ordering |
| **Causal** | if A **could have caused** B (B's author saw A), everyone sees A before B; unrelated writes can appear in any order | tracking dependencies; still available during partitions |
| **Session guarantees** | per client: **read-your-writes**, **monotonic reads** (never go back in time), **monotonic writes**, **writes-follow-reads** | cheap: sticky routing or version tracking (06) |
| **Eventual** | copies converge if writes stop | none, but the app must cope with stale and out-of-order reads |

**Causal consistency is the sweet spot for a lot of user-facing data.** It's the
strongest model that stays available during a partition, and it rules out the
anomalies people actually notice (a reply appearing before its question, a comment on
a post you can't see yet).

---

## 3. CAP theorem

A distributed data store can provide at most two of three guarantees at the same time:
- **C**onsistency: every read sees the latest write (linearizability)
- **A**vailability: every request to a non-failed node gets a non-error response
- **P**artition tolerance: the system keeps working despite messages between nodes
  being lost

**Networks *will* partition**, so P isn't optional. The real meaning is: **when a
partition happens, choose C or A.**

| | During a partition | Good when (from the Primer) | Examples |
|---|---|---|---|
| **CP** | refuse or time out requests that can't be confirmed by a majority | the business needs **atomic reads and writes**: money, inventory, locks, unique constraints | ZooKeeper, etcd, HBase, Spanner, Consul |
| **AP** | answer with whatever data this side has, possibly stale; reconcile when the partition heals | eventual consistency is acceptable, or the system **must keep working** regardless | Cassandra, DynamoDB (default reads), Riak, CouchDB, DNS |

### Worked scenario

Two datacenters, the link between them is cut, and a user in each tries to buy the
**last ticket**.
- **CP:** at most one side can reach a majority and sell it. The other side returns
  an error ("try again later"). No double sale, but some users see errors.
- **AP:** both sides sell it. Both users get "success". When the link comes back,
  there's a conflict to resolve: refund one, apologise, offer compensation.

Neither is wrong. **It's a business decision:** what costs more, an error or an
apology?

### CAP's limits

- The C and A in CAP are **very strict** definitions. Most real systems are neither
  fully CP nor fully AP; they make finer-grained choices, often configurable per
  request.
- CAP only talks about **partitions**, which are rare. It says nothing about the
  trade-off you pay **all the time**. That's PACELC.

---

## 4. PACELC

> If there's a **P**artition, choose **A** or **C**;
> **E**lse (normal operation), choose **L**atency or **C**onsistency.

The "else" half is the everyday trade-off: **strong consistency needs coordination
between replicas, and coordination costs round trips**, even when nothing has failed.
Within one region that's ~1 ms; across continents it's 100 ms+ per write (01's latency
table).

| System | Partition | Else | Meaning |
|---|---|---|---|
| Cassandra, DynamoDB, Riak | **PA** | **EL** | always favour speed and availability; consistency is tunable per request |
| Spanner, HBase, etcd / ZooKeeper | **PC** | **EC** | always favour consistency; pay the latency |

---

## 5. Quorums, in depth

From 06: N replicas, writes wait for W acknowledgements, reads ask R replicas.

```
W + R > N   →  read and write sets overlap  →  a read reaches ≥1 node with the latest write

N=3:  W=2, R=2  balanced (typical)
      W=3, R=1  fast reads, slow writes, writes fail if ANY node is down
      W=1, R=3  fast writes, slow reads
      W=1, R=1  fastest; W+R ≤ N, so eventual consistency only
```

Cassandra exposes this per query: `ONE`, `QUORUM`, `ALL`, and `LOCAL_QUORUM` (a
majority within the local datacenter, which avoids cross-region latency).

### Why W + R > N still isn't linearizable

The overlap guarantee has holes:
- **Sloppy quorums / hinted handoff:** when "home" nodes are down, writes go to
  *other* nodes to stay available. A later read quorum among the home nodes can miss
  the write.
- **Concurrent writes + last-write-wins:** two overlapping writes are resolved by
  timestamp, and clock skew (section 7) can make the older one win.
- **Partially failed writes:** a write that reached only 1 of the required 2 nodes is
  reported as a *failure*, but isn't rolled back. Later reads may or may not see it.
- **A read during a write:** one reader sees the new value from a quorum, and a later
  reader sees the old value from a different quorum, unless reads repair stale nodes
  *before* answering.
- **Restoring a node** from a stale replica can drop the count of up-to-date copies
  below W.

Quorums give you **very likely** up-to-date reads. For true linearizability you need
**consensus**.

---

## 6. Consensus: getting nodes to agree

**Problem:** electing exactly one leader, or agreeing on the order of writes, while
nodes crash and messages get lost or delayed.

**Raft** (and the older **Paxos**) solve it with **majorities**:
1. Nodes elect a **leader** for a numbered **term**. A candidate needs votes from a
   majority, and each node votes once per term, so there can only be one leader per
   term.
2. The leader appends each write to its log and replicates it.
3. A write is **committed once a majority** has stored it. Any future majority must
   overlap this one, so committed data survives any minority failing.
4. If the leader dies, a new election starts, and only a node with all committed
   entries can win.

### Cluster sizes

To survive **f** failed nodes you need **2f + 1** nodes:

| Nodes | Majority | Survives | |
|---|---|---|---|
| 3 | 2 | 1 failure | common |
| 4 | 3 | **still 1** | an even count adds cost, not tolerance |
| 5 | 3 | 2 failures | common for critical systems |

That's why consensus clusters have an **odd** number of nodes.

### Where consensus is used

You rarely run consensus *for your data*. You use a system that does:
- **etcd** (Kubernetes' entire state), **ZooKeeper** (older Kafka, HBase), **Consul**:
  leader election, configuration, service discovery, distributed locks.
- **Spanner, CockroachDB**, and Kafka's newer KRaft mode use it internally.

**Costs:** every write waits for a round trip to a majority; across regions that's
100 ms+. And the cluster is down whenever it can't reach a majority. **Never implement
Raft yourself** for production. Use etcd/ZooKeeper, or a database that has it built in.

---

## 7. Clocks and ordering

### Wall clocks can't be trusted across machines

- Machine clocks **drift**, and NTP sync corrects them only to within milliseconds (or
  much worse on a bad network).
- A clock can **jump backwards** when NTP corrects it, or across a leap second.
- So "latest timestamp wins" across machines can keep the **older** write. That's the
  last-write-wins problem from 06.

**Within one machine**, use a **monotonic clock** to measure elapsed time
(`System.nanoTime()`, not `currentTimeMillis()`).

### Logical clocks

| | How | Gives you |
|---|---|---|
| **Lamport timestamp** | a counter: increment on each event; on receiving a message, set it to max(own, received) + 1 | a total order **consistent with causality** (if A caused B, then A < B). It can't tell whether two events were concurrent. |
| **Vector clock** | one counter **per node**, compared element by element | detects whether one event happened before another **or they were concurrent**, so true conflicts can be detected rather than silently overwritten |

**Spanner's TrueTime** takes a different route: GPS and atomic clocks give a time with
a *known* error bound, and each commit **waits out that uncertainty** (a few ms)
before finishing, so timestamp order matches real-time order across the world.

### Distributed locks need fencing tokens

A lock with an expiry ("lease") isn't safe on its own:

```
1. Client A gets the lock (expires in 10s)
2. Client A pauses (GC, swap, VM migration) for 15s
3. Lock expires; client B gets it and writes
4. Client A wakes, still believes it holds the lock, and writes  ← two holders
```

**Fix:** the lock service hands out an increasing **fencing token** with each lock
(33, then 34). The storage remembers the highest token it has seen and **rejects
writes with an older token**. A's write with token 33 arrives after B's 34, so it's
refused. A lock based only on timeouts, with no fencing, is only good enough when the
occasional double run is harmless.

---

## 8. Choosing per piece of data

The question isn't "is my system CP or AP?" but **"what does each piece of data
need?"**

| Data | Needs | Why |
|---|---|---|
| Account balance, payments | **strong** (linearizable) + transactions | double spending is unacceptable |
| Inventory at checkout, seat booking | **strong**, or eventual + compensation | a business choice: error vs apology (the ticket scenario) |
| Unique username / email | **strong** | two users can't both claim `@alex` |
| Leader election, locks, config | **consensus** (etcd/ZooKeeper) | exactly one leader, or nothing works |
| Chat messages | **causal** (ordered per conversation) | replies after questions; order between conversations doesn't matter |
| User editing their own profile | **read-your-writes** | they must see their change; others can lag |
| Like counts, view counts | **eventual** | being off by a few for a second harms no one |
| Feeds, recommendations, search results | **eventual** | slightly stale is invisible |

**Eventual consistency plus compensation is how much of the real world works:**
airlines overbook and then compensate; banks let ATMs dispense during network outages
and reconcile later. When an "apology" is cheap and errors are expensive, AP plus a
correction process beats CP.

---

## Gotchas

- **ACID consistency ≠ CAP consistency.** The first is about constraints, the second
  about replicas agreeing.
- **P isn't optional.** CAP really means "during a partition, C or A". Pick per data
  type, not per company.
- **PACELC matters more day-to-day than CAP.** Strong consistency costs latency even
  when nothing is broken, and across regions that's 100 ms+ per write.
- **"Eventual" has no deadline.** Design for minutes of lag during incidents, not the
  usual milliseconds.
- **W + R > N ≠ linearizable.** Sloppy quorums, last-write-wins and partial writes leave
  gaps. Use consensus when you truly need strong consistency.
- **Use odd cluster sizes for consensus.** 4 nodes tolerate no more failures than 3.
- **Never order events across machines by wall clock.** Clocks drift and jump. Use
  logical clocks, versions, or a single leader to assign order.
- **Use a monotonic clock** for timeouts and durations.
- **A lock with an expiry needs fencing tokens.** A paused process can wake up still
  believing it holds the lock.
- **Don't build consensus yourself.** etcd, ZooKeeper, or a database with Raft built in.
- **Causal consistency is often enough** for user-facing data, and it stays available
  during partitions.
