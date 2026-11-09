# 05 — Databases

> Source: [System Design Primer](https://github.com/donnemartin/system-design-primer):
> "Database": RDBMS and ACID, denormalization, SQL tuning, NoSQL (key-value,
> document, wide column, graph), "SQL or NoSQL". The Primer's replication,
> federation and sharding sections belong to 06.
> Depth: *Designing Data-Intensive Applications* (DDIA) ch. 2–3 and 7.

After 01–04 the app tier is stateless and easy to clone. All the state that's left
lives in the database, which is why **the database is usually the hardest part to
scale and the most expensive to get wrong.** This topic is about a single database:
how it stores data, keeps it correct, and finds it quickly. 06 splits it across
machines.

---

## 1. Relational databases and ACID

A relational DB stores data in **tables of rows**, with a fixed schema, relationships
expressed through foreign keys, and SQL for querying and joining. Its main guarantee
is **transactions**: a group of reads and writes that succeeds or fails as a unit.

| | Means | In practice |
|---|---|---|
| **Atomicity** | all of the transaction happens, or none of it | debit A and credit B: never just one of them |
| **Consistency** | the transaction takes the DB from one valid state to another | constraints (foreign keys, `UNIQUE`, `CHECK`) are never violated. Mostly the *app's* job; the DB enforces what you declare. |
| **Isolation** | concurrent transactions don't see each other's half-done work | how strictly this holds depends on the **isolation level** (below) |
| **Durability** | once committed, it survives a crash | the write-ahead log (WAL) is flushed to disk before "committed" is returned |

### Isolation levels

Full isolation (transactions behaving as if they ran one at a time) is expensive, so
databases offer weaker levels. Each one allows some **anomalies**:

| Level | Prevents | Still allows |
|---|---|---|
| Read uncommitted | — | **dirty reads** (seeing uncommitted data) |
| **Read committed** | dirty reads | **non-repeatable reads** (re-reading a row gives a different value) |
| **Repeatable read / snapshot** | the above | **write skew**, and phantoms in some implementations |
| **Serializable** | everything | — (but slower, and transactions get aborted and must be retried) |

Defaults: **PostgreSQL = read committed**, **MySQL InnoDB = repeatable read**. Most
apps run on the default without knowing what it allows.

### The lost update

The anomaly you'll actually meet:

```
T1: read balance = 100          T2: read balance = 100
T1: write balance = 100 + 50    T2: write balance = 100 − 30
→ final 70; T1's +50 is gone
```

Fixes, best first:
1. **Atomic update:** `UPDATE accounts SET balance = balance + 50 WHERE id = 1`. The
   DB does the read-modify-write safely.
2. **Pessimistic lock:** `SELECT … FOR UPDATE`, so the other transaction waits.
3. **Optimistic concurrency:** add a `version` column and write with
   `UPDATE … SET balance = ?, version = version + 1 WHERE id = 1 AND version = 7`.
   If 0 rows were updated, someone else got there first, so re-read and retry.
   Best when conflicts are rare.

**Write skew** is the subtler one. Two doctors each check "is at least one other
doctor on call?", both see yes, and both go off call. Each transaction is fine on its
own, but together they break the rule. Only serializable isolation (or explicitly
locking the rows the decision depends on) prevents it.

---

## 2. Indexes

**Problem:** without an index, `WHERE email = ?` reads every row: O(n), a full table
scan.

**Fix:** an index is a separate structure, sorted by the indexed column(s), that
points back to the rows.

**Costs:** every insert, update and delete must also update **every index** on that
table, and indexes take disk space and memory. **Indexes speed up reads and slow down
writes.** Add them for the queries you actually run, not "just in case".

### B-tree (the default)

A balanced, sorted tree with very high fan-out, so lookups take O(log n) and only 3–4
levels for millions of rows. Because it's sorted, it supports **equality and range
queries** (`=`, `<`, `>`, `BETWEEN`, `ORDER BY`, prefix `LIKE 'abc%'`).

### Rules for using indexes well

- **Composite index, leftmost prefix:** an index on `(country, city, age)` serves
  queries on `country`, on `country + city`, and on all three, but **not** on `city`
  alone. Order columns by how you filter, with equality conditions before range
  conditions.
- **Covering index:** if the index contains every column the query needs, the DB
  answers from the index alone without visiting the table.
- **Selectivity:** indexing a column with few distinct values (`is_active`, `gender`)
  rarely helps, because the DB decides a scan is cheaper.
- **What stops an index being used:**
  - wrapping the column in a function (`WHERE LOWER(email) = …`; use an expression
    index instead)
  - a leading wildcard (`LIKE '%abc'`)
  - type mismatches (comparing a string column to a number)
- **Check with `EXPLAIN`** (`EXPLAIN ANALYZE` in Postgres). Never guess whether an
  index is used.

### B-tree vs LSM-tree

Two families of storage engine, with opposite trade-offs:

| | **B-tree** | **LSM-tree** (log-structured merge) |
|---|---|---|
| Writes | update pages in place (random I/O) | append to an in-memory table, flush sorted files (SSTables) sequentially, **merge them in the background** (compaction) |
| Reads | one tree lookup | may check memory plus several files (Bloom filters skip files that can't contain the key) |
| Strength | **read-heavy**, predictable reads | **write-heavy**, very high ingest rates |
| Used by | PostgreSQL, MySQL InnoDB, most relational DBs | Cassandra, RocksDB, LevelDB, HBase, ScyllaDB |

This ties back to 01's latency table: **sequential writes are much faster than random
ones**, and LSM-trees are built to only write sequentially.

---

## 3. Normalisation and denormalisation

### Normalisation: store each fact once

| Form | Rule (short version) |
|---|---|
| 1NF | each column holds one value; no repeating groups (`phone1, phone2, phone3`) |
| 2NF | every non-key column depends on the **whole** primary key |
| 3NF | non-key columns depend **only** on the key, not on each other |

**Why:** if a customer's address is stored in every order row, changing it means
updating thousands of rows, and missing one leaves the data contradicting itself (an
*update anomaly*). Normalised data has one place to change, so it can't disagree with
itself.

**Cost:** reads need **joins** to put the data back together.

### Denormalisation: copy data to avoid joins

Store redundant copies (e.g. `author_name` on every post) so reads skip the join.

- ✅ Faster reads. And once data is split across machines (06), **cross-machine joins
  are slow or impossible**, so denormalising often becomes necessary rather than
  optional.
- ❌ Writes must update every copy, and copies can drift out of sync.
- The Primer's rule of thumb: worth it when reads outnumber writes heavily (100:1,
  1000:1).
- **Materialised views** let the DB keep a precomputed, denormalised result for you
  (refreshed on a schedule or on change).

**Start normalised and denormalise specific hot paths** once you've measured that the
join is the problem.

---

## 4. SQL tuning (from the Primer)

1. **Measure first:** benchmark (e.g. `ab`) and profile with the slow query log. Don't
   guess.
2. **Tighten the schema:**
   - right-sized types (`INT` not `BIGINT` where values are small; `DECIMAL` for
     money, **never `FLOAT`**)
   - `NOT NULL` where possible
   - store **large blobs outside the DB** (object storage, see 10) and keep only their
     location in the row
3. **Index** the columns you filter, join and sort on (section 2).
4. **Avoid expensive joins**, and denormalise where measurement says so.
5. **Partition tables:** move cold rows (old orders) to separate partitions or tables,
   so the hot data fits in memory.

The Primer also says to tune the query cache. MySQL removed its query cache in 8.0,
so skip that one and cache in the application instead (see 04).

---

## 5. NoSQL

"Not only SQL": stores that drop some relational features (joins, fixed schema, and
often multi-row transactions) in exchange for easier horizontal scaling or a data
model that fits a particular shape of data.

| Type | Data model | Access | Good for | Weakness | Examples |
|---|---|---|---|---|---|
| **Key-value** | key → opaque value | get/put **by key only**, O(1) | sessions, caches, carts, feature flags | can't query by value; no relationships | Redis, DynamoDB, etcd |
| **Document** | key → JSON-like document, nested | by key, plus queries and indexes on fields | self-contained records read as a whole: profiles, catalogs, CMS content | joins are weak; data duplicated across documents drifts | MongoDB, Couchbase, Firestore |
| **Wide-column** | row key → sorted columns, grouped into column families | by **partition key**, range scans within a partition | **huge write volume**: time series, event logs, messaging, IoT | must design tables around the queries; no ad-hoc queries; no joins | Cassandra, HBase, Bigtable, ScyllaDB |
| **Graph** | nodes + edges, both with properties | traversals ("friends of friends who like X") | relationship-heavy data: social graphs, recommendations, fraud rings | harder to scale out; specialised query language | Neo4j, Neptune |

### BASE

NoSQL systems often trade ACID for **BASE**: **B**asically **A**vailable, **S**oft
state, **E**ventual consistency. The system keeps answering during failures, but
replicas may briefly disagree. What "eventual" means, and the alternatives, is
07-consistency.

### Design NoSQL around queries, not entities

In SQL you model the data and then write any query you like. In Cassandra or DynamoDB
you **list your queries first and design a table for each one**, duplicating data as
needed. A query you didn't plan for can be impossible or require a full scan.
**Changing access patterns later is expensive.**

---

## 6. SQL or NoSQL?

From the Primer, with a modern reality check.

| Choose SQL when | Choose NoSQL when |
|---|---|
| data is structured and relational | data is semi-structured, or the schema changes often |
| you need **transactions** across rows or tables | you don't need complex joins |
| you need flexible, ad-hoc queries and joins | access patterns are known and simple (by key / by partition) |
| integrity constraints matter (money, inventory) | **very high write throughput**, or many TB to PB of data |
| you want mature tooling and well-known scaling patterns | you need to scale out writes across many machines from day one |

Typical NoSQL fits (Primer): fast ingest of clickstream and log data, leaderboards,
temporary data (shopping carts), frequently accessed hot tables, metadata and lookup
tables.

**The modern default is a relational DB (usually Postgres) until a measured need says
otherwise.** A single well-tuned Postgres instance with read replicas handles far more
than most products ever reach. It also supports JSON columns (document-style data),
full-text search, and extensions for time series and vectors. Reach for NoSQL for a
**specific workload**, not as the main store "because it scales".

**NewSQL / distributed SQL** (Google Spanner, CockroachDB, YugabyteDB) gives SQL and
ACID transactions with horizontal scaling, at the cost of higher write latency
(consensus between replicas, see 07) and operational complexity.

**Polyglot persistence** is normal: each kind of data goes in the store that suits it.

---

## 7. Other kinds of store

| Store | For | Notes |
|---|---|---|
| **Object / blob storage** (S3, GCS) | files, images, video, backups | cheap, effectively unlimited, read by key. Keep the files here and their metadata in the DB. See 10. |
| **Search engine** (Elasticsearch, OpenSearch) | full-text search, filtering, ranking | built on inverted indexes; a *secondary* copy fed from the main DB. See 10. |
| **Time-series DB** (TimescaleDB, InfluxDB, Prometheus) | metrics, sensor data | optimised for appends by time and range queries over time windows |
| **Column-oriented warehouse** (BigQuery, Snowflake, Redshift, ClickHouse) | analytics: aggregates over billions of rows | stores data by **column**, so `SUM(amount)` reads one column instead of whole rows |

### OLTP vs OLAP

| | **OLTP** (transactions) | **OLAP** (analytics) |
|---|---|---|
| Queries | many small reads/writes by key | few large scans and aggregates |
| Users | the app, end users | analysts, dashboards, reports |
| Storage | row-oriented | column-oriented |
| Examples | Postgres, MySQL | BigQuery, Snowflake, ClickHouse |

**Don't run heavy analytics on your production OLTP database.** A big report query
can starve real user traffic. Copy data to a warehouse (with ETL or change data
capture) and run the analytics there.

---

## 8. Worked example: the photo app from 01

| Data | Store | Why |
|---|---|---|
| Users, follows, photo metadata | **Postgres** | relational, needs constraints (unique usernames), ~300 writes/s is easy |
| Photo files (27 PB) | **object storage + CDN** | blobs never go in the DB; see 01's estimate |
| Home feed (precomputed list of photo IDs per user) | **Redis or Cassandra** | read on every app open, always by user ID: simple key access at high volume |
| Likes / view counters | **Redis** (counters), flushed to the DB periodically | very hot writes, and approximate counts are acceptable |
| Photo search by caption / tag | **Elasticsearch**, fed from Postgres | full-text search is a different index shape |

One product, five stores, each matching an access pattern. This is the kind of
breakdown case studies expect.

---

## Gotchas

- **Read-modify-write in application code loses updates.** Use atomic `UPDATE … SET
  x = x + 1`, `SELECT … FOR UPDATE`, or a version column.
- **Know your isolation level.** The default (read committed / repeatable read)
  allows anomalies, and write skew gets past both.
- **Every index slows every write.** Index for real queries, check with `EXPLAIN`, and
  drop unused indexes.
- **Composite index order matters.** `(a, b)` doesn't help `WHERE b = ?`.
- **Functions on indexed columns disable the index.** Use an expression index or
  rewrite the query.
- **Never store money as `FLOAT`.** Use `DECIMAL` or integer cents.
- **Don't put large blobs in the DB.** Store them in object storage and keep the
  key/URL in the row.
- **NoSQL is designed around queries.** Adding a new access pattern later can mean a
  new table and backfilling all the data.
- **Denormalised copies drift.** Every copy needs an owner that keeps it updated
  (application code, a trigger, or change data capture).
- **Don't run analytics on the OLTP primary.** Use a replica at minimum, a warehouse
  ideally.
- **"NoSQL scales, SQL doesn't" is outdated.** Postgres plus replicas plus caching
  goes very far, and distributed SQL exists when it doesn't.
