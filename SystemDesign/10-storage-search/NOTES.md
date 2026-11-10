# 10 — Storage & Search

> Source: the Primer barely covers either topic. Use these instead:
> - the [awesome-system-design-resources](https://github.com/ashishps1/awesome-system-design-resources)
>   articles on object storage, inverted indexes and search
> - DDIA ch. 3 (full-text indexes)
> - the Elasticsearch *Definitive Guide* concepts (shards, analysis, relevance)

The database (05) holds structured records. Two kinds of data don't belong there:
**large binary files** (images, video, backups), which go in **object storage**, and
**text that people search**, which needs a different index shape: an **inverted
index**. Both sit *beside* the database, and both follow one rule: **the database
stays the source of truth for metadata; the other store holds bytes or a searchable
copy.**

---

## 1. Block vs file vs object storage

| | **Block** | **File** | **Object** |
|---|---|---|---|
| What | a raw disk volume | a shared filesystem with folders | flat namespace of key → object (bytes + metadata) |
| Access | attached to **one** machine; random reads and writes | mounted by **many** machines; normal file operations | **HTTP API**: `PUT` / `GET` / `DELETE` of whole objects |
| Edit in place | ✅ | ✅ | ❌ rewrite the whole object |
| Latency | lowest (sub-ms) | low | higher (tens of ms to first byte) |
| Scale | one volume, up to TBs | limited by the filesystem | **practically unlimited** |
| Cost | 💲💲💲 | 💲💲 | 💲 |
| Examples | AWS EBS, local SSD | NFS, AWS EFS | **S3**, GCS, Azure Blob, MinIO |
| Use for | database data directories, OS disks | shared config, legacy apps that need a filesystem | **images, video, backups, logs, data lakes, static assets** |

**Default to object storage** for anything that's a file. Block storage is for
databases themselves. File storage is mainly for software that insists on a
filesystem.

---

## 2. Object storage in practice

### Bytes in the object store, metadata in the DB

```
photos table (Postgres)            S3 bucket
 id | owner | caption | s3_key ──────►  photos/2026/11/9f3a…c1.jpg
```

Queries ("photos by user 42, newest first") hit the DB. Only the actual download
touches the object store, usually through a CDN.

### Uploads: keep app servers out of the data path

**Problem:** routing a 2 GB video through your app servers ties up their bandwidth,
memory and threads for minutes.

**Fix: presigned URLs.**

```
1. Client → App:  "I want to upload a photo"
2. App:           create a metadata row (status = PENDING), generate a presigned PUT URL
                  (valid ~15 min, for this one key only)
3. Client → S3:   uploads the bytes directly
4. S3 → queue:    "object created" event
5. Worker:        validate, generate thumbnails, set status = READY     (08)
```

- **Multipart upload** for large files (above ~100 MB): split into parts, upload them
  in parallel, and retry only the parts that failed. That makes uploads resumable.
- Downloads of private content work the same way: the app hands out a short-lived
  **signed URL** (often a signed CDN URL), and the bytes never pass through the app.

### Durability vs availability

- **Durability:** the chance the data still exists. S3 is designed for **11 nines**
  (99.999999999%) by keeping redundant pieces across multiple zones.
- **Availability:** the chance a request succeeds *right now*, about **99.9–99.99%**.
  Data is almost never lost, but requests do fail occasionally, so clients still need
  retries (09).
- S3 has given **strong read-after-write consistency** since late 2020. Older material
  saying it's eventually consistent is out of date.

### Replication vs erasure coding

How storage systems keep data safe without paying 3× for it:

| | **3× replication** | **Erasure coding** (e.g. 6 data + 3 parity pieces) |
|---|---|---|
| Extra storage | **200%** | **50%** |
| Survives | 2 lost copies | any 3 lost pieces |
| Cost | cheap to read and rebuild | CPU to encode; rebuilding needs reads from many pieces |

At 01's 27 PB (9 PB raw × 3 replicas), erasure coding would need ~13.5 PB instead.
That's why large object stores use it for colder data.

### Storage classes and lifecycle rules

| Class | For | Retrieval |
|---|---|---|
| Standard (hot) | frequently read | instant |
| Infrequent access | read occasionally | instant, but **per-GB retrieval fee** |
| Archive (Glacier, etc.) | compliance, old backups | **minutes to hours**, plus fees |

**Lifecycle rules** move objects automatically (e.g. to infrequent access after 30
days, to archive after a year, delete after 7 years). Remember 01's photo app: old
photos are rarely viewed, so tiering is where most of the storage savings are.

### Key design

- **Never overwrite an object in place.** Write a new key (a content hash or version
  in the name) and update the DB pointer. The same idea as cache busting (04):
  CDN caches can't serve a stale version of a key that never changes.
- **Content-addressed keys** (key = hash of the content) give **deduplication** for
  free: the same file uploaded twice is stored once.

### Large media: video

Serve video as **adaptive bitrate streaming** (HLS / DASH). Transcode each upload into
several quality levels, cut each into ~2–6 second segments, and let the player switch
quality per segment to match the viewer's bandwidth. Every segment is a small,
immutable, CDN-cacheable object.

### Distributed file systems

HDFS and Google's GFS split huge files into large chunks (64–128 MB), replicate them
across machines, and keep metadata on a master node. They powered the Hadoop era of
batch analytics. Today most of that has moved to **object storage plus separate
compute** (data lakes on S3, queried by Spark or a warehouse, see 05).

---

## 3. Why search needs its own index

`WHERE caption LIKE '%sunset%'`:
- **can't use a B-tree index** (a leading wildcard, see 05), so it's a full table scan
- matches `sunsets` but not `Sunset` (case), and not `setting sun` (no notion of word
  forms)
- has **no ranking**: a caption that mentions sunset once and one that's all about
  sunsets count the same
- has no typo tolerance: `sunest` finds nothing

Search engines solve these with a different data structure.

---

## 4. The inverted index

A normal index maps **row → contents**. An inverted index maps **word → the rows
(documents) that contain it**, like the index at the back of a book.

```
doc 1: "Sunset over the beach"
doc 2: "Beach volleyball at sunset"
doc 3: "Mountain sunrise"

term       → postings (doc ID : positions)
beach      → 1:[3], 2:[0]
mountain   → 3:[0]
sunrise    → 3:[1]
sunset     → 1:[0], 2:[3]
volleyball → 2:[1]
```

**Querying:** `sunset AND beach` = intersect the two posting lists → {1, 2}. `OR` =
union. **Phrase queries** ("beach volleyball") use the **positions** to check that the
words are next to each other.

### Analysis: from text to terms

Each document goes through an **analyser** before indexing:

```
"Running at the Beaches!"
  tokenise   → [Running, at, the, Beaches]
  lowercase  → [running, at, the, beaches]
  stop words → [running, beaches]          (drop "at", "the")
  stemming   → [run, beach]
  synonyms   → [run, beach, shore]         (optional)
```

**The same analyser must run on the query.** Otherwise the document indexed `beach`,
the user searched `Beaches`, and nothing matches. Analyser mismatch is the most common
"why doesn't search find it?" bug.

### Relevance: ranking the matches

Matching documents are scored. The classic intuition is **TF-IDF**:
- **Term frequency (TF):** the more often a term appears in a document, the more
  relevant the document.
- **Inverse document frequency (IDF):** rare terms matter more. Matching "volleyball"
  says more than matching "photo".

**BM25** (the default in Lucene and Elasticsearch) refines it: term frequency
**saturates** (the tenth mention adds little), and scores are **normalised by document
length** (one mention in a short caption beats one mention in a long essay).

Real products blend text relevance with **business signals**: popularity, recency,
personalisation, whether the item is in stock.

### Typos and fuzzy matching

**Fuzzy queries** match terms within an **edit distance** (Levenshtein): `sunest` →
`sunset` is 2 edits. Usually limited to 1–2 edits, and only on longer words, or it
matches too much.

---

## 5. Elasticsearch / OpenSearch architecture

Built on **Lucene** (the inverted-index library).

```
index "photos"
 ├─ shard 0 (primary) ─► replica
 ├─ shard 1 (primary) ─► replica
 └─ shard 2 (primary) ─► replica
      each shard = one Lucene index = a set of immutable segments
```

- **Sharding (06):** documents are spread across shards by hashing their ID. **The
  number of primary shards is fixed when the index is created.** Changing it means
  reindexing.
- **Replicas** give failover and extra read capacity.
- **Searches are scatter-gather:** the query goes to every shard, each returns its top
  hits, and a coordinating node merges them. It's only as fast as the slowest shard
  (01's fan-out gotcha).
- **Near-real-time, not real-time:** new documents become searchable after a
  **refresh** (default every **1 second**), when a new segment is written. Segments
  are immutable and get merged in the background, much like an LSM-tree (05).
- **Deep pagination is expensive:** `from=10000&size=20` makes *every* shard gather
  and sort 10,020 results. Use `search_after` (cursor pagination, see 02), and cap how
  deep users can page.

### Search is a secondary store

- **The DB stays the source of truth.** The search index is a **copy built for
  queries**, and it's eventually consistent (07), seconds behind.
- **Keep it in sync** with **change data capture or the outbox pattern (08)**, never
  with dual writes from the app.
- **Be able to rebuild it from scratch.** Mappings and analysers change, and that
  means reindexing. Build the new index alongside the old one, then switch an
  **alias** to it, so there's no downtime.
- **Don't use Elasticsearch as your primary database.** It isn't built for
  transactions or durability guarantees, and it's designed to be rebuilt from the
  source of truth.

---

## 6. Related search problems

### Typeahead / autocomplete

Suggestions as the user types, within **~100 ms**, for every keystroke.
- **A trie (prefix tree)** where each node stores the **top-k completions** for its
  prefix, so a lookup is just walking to the prefix's node. Build it **offline** from
  query logs, and serve it from memory.
- In Elasticsearch: **edge n-grams** (index `sun`, `suns`, `sunse`, `sunset`) or the
  completion suggester.
- Cache heavily. Popular prefixes are a small set (04).
- Make it a case study: it's a classic interview problem.

### Vector (semantic) search

Text or images become **embeddings** (vectors from an ML model), and search finds the
**nearest vectors**, so "beach at dusk" matches "sunset by the sea" with no word in
common. It uses **approximate nearest neighbour (ANN)** indexes (HNSW is the common
one), which trade a little accuracy for big speed gains. Postgres (pgvector),
Elasticsearch and dedicated vector DBs support it. **Hybrid search** (BM25 + vectors,
with the scores combined) usually beats either alone.

### Geo search: "what's near me?"

A B-tree on latitude and longitude can't answer "within 2 km" efficiently.
- **Geohash:** encode the location as a string, where **a shared prefix means nearby**
  (`tdr1y…`). "Nearby" becomes a prefix query, which ordinary indexes handle. Gotcha:
  two points on either side of a cell boundary have different prefixes, so **also
  search the 8 neighbouring cells**.
- **Quadtree:** recursively split the map into 4 cells until each holds ≤ N points.
  Dense cities get small cells, oceans get big ones.
- **Uber's H3:** hexagonal cells, all neighbours equidistant.
- Postgres **PostGIS** and Elasticsearch geo queries do this for you.

This is the core of the **ride-sharing** case study (matching riders to nearby
drivers).

---

## 7. Worked example: the photo app

| Need | Solution |
|---|---|
| Store 27 PB of photos (01) | S3 with lifecycle tiering; erasure-coded cold storage |
| Uploads | presigned URLs + multipart; "object created" event → thumbnail worker (08) |
| Serving | CDN in front of S3, with immutable keys (04) |
| Search by caption and tag | Elasticsearch, fed from Postgres by CDC; BM25 + popularity boost |
| "Photos taken near here" | geohash on the photo's location, indexed in ES or PostGIS |
| Search suggestions | trie of top queries built offline, cached in memory |

---

## Gotchas

- **Don't send uploads through app servers.** Use presigned URLs and multipart.
- **Bytes in object storage, metadata in the DB.** Queries never scan the bucket.
- **Never overwrite objects.** New key per version, so caches and CDNs stay correct.
- **Archive tiers have retrieval delays, fees and minimum storage durations.** Cheap to
  store, expensive to read in a hurry.
- **Durability ≠ availability.** 11 nines of durability still comes with failed
  requests. Retry.
- **The same analyser must run at index and query time.** Most "search can't find it"
  bugs are analyser mismatches.
- **The search index is a secondary, eventually consistent copy.** Sync it with
  CDC/outbox, be able to rebuild it, and never treat it as the source of truth.
- **The number of primary shards is fixed at index creation.** Changing it means a
  reindex behind an alias.
- **Near-real-time means ~1s behind.** Don't write and then search expecting to find
  the document immediately.
- **Deep pagination costs every shard.** Use `search_after` and cap the depth.
- **Geohash neighbours:** points near a cell edge need the 8 surrounding cells searched
  too.
