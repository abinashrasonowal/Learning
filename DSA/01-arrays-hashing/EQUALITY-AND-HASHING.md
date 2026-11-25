# `==`, `equals`, `hashCode` and how `HashMap` uses them

> `hashCode()` picks the bucket, `equals()` picks the entry inside it. Break the
> contract between them and the map doesn't throw — it just stops finding things.

Companion to [NOTES.md](NOTES.md). Read this before writing `MyHashMap.java`.

**Part I — the contract** (what *you* must get right)
1. `==` — same object?
2. `equals()` — logically the same?
3. `hashCode()` — which bucket?
4. Bugs from breaking the contract

**Part II — inside `HashMap`** (what Java does with it, in the order a `put` meets it)

5. Structure — an array of linked lists
6. `put` / `get` / `remove`
7. Collisions
8. Treeification — `TreeNode`
9. Load factor and resize
10. Is resizing costly?

**Part III — reference**

11. Complexity · 12. Defaults · 13. Odds and ends · 14. `HashSet` — a `HashMap` in disguise

Then: Summary · Self-check

---

# Part I — The contract

## 1. `==` — same object?

| Operands | `==` compares |
|---|---|
| Primitives (`int`, `char`, …) | values |
| References | *identity* — do both point at the same object? |

```java
String a = new String("hi"), b = new String("hi");
a == b;          // false — two objects
a.equals(b);     // true  — same content
"hi" == "hi";    // true  — literals are interned in the string pool
```

**The cache trap.** `Integer` boxes **−128..127** from a cache, so `==` works there by
accident and fails silently at 128. Same for `Long`, `Short`, `Byte`, `Character ≤ 127`.

```java
Integer x = 127, y = 127;  x == y;   // true  — same cached object
Integer p = 128, q = 128;  p == q;   // false — two new objects
```

**Rule:** `==` for primitives and for "is this literally the same instance".
`.equals()` for everything else.

## 2. `equals()` — logically the same?

`Object.equals` *is* `==`. A class has value semantics only if it overrides it —
`String`, the boxed types, collections and records all do.

```java
@Override
public boolean equals(Object o) {
    if (this == o) return true;                           // fast path
    if (!(o instanceof Person p)) return false;           // also handles null
    return age == p.age && Objects.equals(name, p.name);  // null-safe field compare
}
```

The contract — break one and collections misbehave in ways no test of your own class
catches:

| Property | Meaning |
|---|---|
| Reflexive | `x.equals(x)` |
| Symmetric | `x.equals(y)` ⇔ `y.equals(x)` |
| Transitive | `x = y` and `y = z` ⇒ `x = z` |
| Consistent | same answer on repeat calls, if nothing changed |
| Non-null | `x.equals(null)` is `false`, never an NPE |

Symmetry is the one that breaks in practice: `instanceof` with subclasses, or a
`CaseInsensitiveString` that equals a plain `String` but not the other way round.

## 3. `hashCode()` — which bucket?

One hard rule, one non-rule:

1. **`a.equals(b)` ⇒ `a.hashCode() == b.hashCode()`** — mandatory.
2. Equal hashes do **not** imply equal objects. That's a *collision* (§7), and it's legal.

```java
@Override
public int hashCode() {
    return Objects.hash(name, age);   // exactly the fields equals() uses
}
```

- **Override both or neither.**
- **Hash only fields `equals` uses.** Hashing a field `equals` ignores lets two equal
  objects hash apart.
- **A good hash is** deterministic, uniform, and mixes every field. `x + y` is bad —
  `(1,2)` and `(2,1)` collide. `31 * x + y` makes order matter (31: odd prime, the JIT
  turns it into `(i << 5) - i`).
- `return 1;` is *legal* — every key collides and the map degrades to one bucket.
  Correct, but O(n), or O(log n) once treeified (§8).

## 4. Bugs from breaking the contract

None of these throw. The map just quietly stops finding things.

**`equals` without `hashCode`:**

```java
class Person {
    String name;
    Person(String n) { name = n; }
    @Override public boolean equals(Object o) {
        return o instanceof Person p && name.equals(p.name);
    }
    // no hashCode — inherits identity hash
}

map.put(new Person("Ana"), 1);
map.get(new Person("Ana"));   // null — equal objects, different buckets, equals never runs
```

**Mutating a key after `put`:**

```java
List<Integer> key = new ArrayList<>(List.of(1, 2));
map.put(key, "x");
key.add(3);               // hashCode changed
map.get(key);             // null — looks in the new bucket, entry sits in the old one
map.containsValue("x");   // true — still in there, unreachable by key
```

Keys should be immutable: `String`, boxed types, records, `List.of(...)`.

**`int[]` as a key:** arrays inherit identity `equals`/`hashCode`, so two equal arrays
are two different keys. Use `Arrays.toString(a)` or a `List<Integer>` — see the
gotchas table in [NOTES.md](NOTES.md).

---

# Part II — Inside `HashMap`

## 5. Structure — an array of linked lists

```java
transient Node<K,V>[] table;          // the buckets; length always a power of 2

static class Node<K,V> {
    final int hash;     // cached — never recomputed, reused on resize
    final K key;
    V value;
    Node<K,V> next;     // hand-rolled singly linked list, not java.util.LinkedList
}
```

```
table (Node[16])
 [0] → null
 [1] → Node(17,"a") → Node(33,"b") → null     ← chain (collision)
 [2] → Node(2,"x")  → null
 [3] → TreeNode ── red-black tree             ← after treeifying (§8)
 ...
```

Each slot is `null`, a `Node` (head of a chain), or a `TreeNode` (root of a tree).

- **Not an array of Sets.** It's the other way round — `HashSet` *wraps* a
  `HashMap<E, PRESENT>`. Uniqueness comes from `put` replacing on an `equals` match.
- **Defaults:** capacity **16**, load factor **0.75**, threshold **12** (§12).
- **Lazy:** `new HashMap<>()` leaves `table == null` until the first `put`.
- **Power of 2:** any capacity you pass is rounded *up* (`10 → 16`, `1000 → 1024`).

## 6. `put` / `get` / `remove`

All three share one path:

```
h     = k.hashCode() ^ (k.hashCode() >>> 16)   // 1. spread high bits into low bits
index = h & (N - 1)                            // 2. == h % N, because N is a power of 2
walk bucket[index]:                            // 3. find the key
    if node.hash == h && (node.key == k || k.equals(node.key)) -> match

put:    match -> replace value;  no match -> append at TAIL;  if ++size > threshold -> resize (§9)
get:    match -> return value;   no match -> null
remove: match -> unlink
```

| Detail | Why |
|---|---|
| Spread `h ^ (h >>> 16)` | `& (N-1)` only reads low bits; without mixing, hashes differing only in high bits all collide |
| `& (N-1)` instead of `%` | one AND instead of a division — the reason N is a power of 2 |
| `hash ==` before `equals` | int compare is cheap; `equals` may not be |
| `node.key == k` first | identity short-circuit — same object, skip `equals` |
| Insert at **tail** (Java 8+) | Java 7 inserted at the head; a concurrent resize could form a cycle → `get()` spun forever |

Step 3 is O(1) only while chains are short. Keeping them short is what §7–§9 are about.

## 7. Collisions

Two keys in the same bucket. Two flavours:

1. **Same `hashCode`** — `"Aa".hashCode() == "BB".hashCode()` (both 2112). Only
   `equals` can separate them.
2. **Different hash, same index** — with N = 16, `17 & 15 == 33 & 15 == 1`. Far more
   common; the cached `hash` field rules these out without calling `equals`.

**Unavoidable.** Pigeonhole: infinitely many keys, 2³² hashes, far fewer buckets.
Birthday paradox: with 16 buckets, P(collision) passes 50% at ~5 keys — in general
~√N. The goal is short chains, not zero collisions.

**Resolution strategies:**

| | Separate chaining | Open addressing |
|---|---|---|
| Collided entry lives | in a per-bucket list/tree | in another slot of the same array (probe) |
| Used by | **Java `HashMap`** | Python `dict`, Rust `HashMap` |
| Delete | unlink | tombstone, or later probes break |
| Comfortable load | up to ~1.0 | ≲ 0.7, then falls off a cliff |
| Cache behaviour | poor (pointer chasing) | good (contiguous) |

Probing flavours: **linear** (`i, i+1, i+2…` — clusters), **quadratic** (`i+1², i+2²…`),
**double hashing** (`i + j·h₂`). Robin Hood and cuckoo hashing are the names worth
recognising.

**When collisions are an attack — hash flooding.** An attacker who controls keys
(request params, JSON fields) sends thousands with one hash — `"Aa"`/`"BB"` combine
into 2ⁿ colliding strings (`"AaAa"`, `"AaBB"`, `"BBAa"`, …). Every insert walks one
huge chain → **O(n²)** total, CPU pinned. CVE-2011-4858. Java's defence is §8; Python,
Rust and Go use a randomised per-process hash seed instead.

## 8. Treeification — `TreeNode`

```java
static final int TREEIFY_THRESHOLD    = 8;   // list -> tree
static final int UNTREEIFY_THRESHOLD  = 6;   // tree -> list
static final int MIN_TREEIFY_CAPACITY = 64;  // below this, resize instead
```

**When.** A chain reaching **8** becomes a red-black tree — only if
`table.length ≥ 64`; a smaller table resizes instead, because spreading keys is the
better fix. Only *that* bucket treeifies. It goes back to a list at **≤ 6** (on
`remove`, or when a resize `split()`s it). The 8/6 gap is hysteresis — no flapping
at 7.

**Shape.** `TreeNode` extends `Node`, so the array type never changes. It adds
`parent`, `left`, `right`, `prev`, `red` — it's a tree *and* still a doubly linked
list (used for iteration, splitting and untreeifying).

**Ordering** — keys only promise `hashCode`/`equals`, so Java decides left/right by:

1. compare `hash`
2. tie, and the key is `Comparable` → `compareTo`
3. still tied → `tieBreakOrder` (class name, identity hash) — insertion only, useless
   for lookup

| Collided keys | Lookup in the tree |
|---|---|
| different hashes | O(log n) ✅ |
| same hash, `Comparable` (e.g. `String`) | O(log n) ✅ |
| same hash, **not** `Comparable` | searches both subtrees → O(n) ❌ |

So a key class that may collide should implement `Comparable`.

**What it buys:** worst case drops from O(n) to O(log n) — 1,000 colliding keys cost
~10 comparisons, not 1,000. That defuses hash flooding (`String` is `Comparable`, so
even identical hashes stay O(log n)) and softens a bad user-written `hashCode`.

**Why not always:** a `TreeNode` is ~2× a `Node`, and for 3–4 entries a list walk is
faster than a tree. With a decent hash, P(a bucket reaches 8) ≈ 6 × 10⁻⁸ — trees are a
defence, not the normal path.

## 9. Load factor and resize

**The invariant: `n / N ≤ k`.** `n` = entries, `N` = buckets, `k` = load factor (0.75).
`n/N` is the average chain length, so bounding it is what keeps §6 step 3 O(1). Java
stores `threshold = N × k` and checks `++size > threshold` after each new key.

**`resize()`:** double `N`, double `threshold`, allocate the new array, move every entry.

| Event | n | N | threshold | n/N |
|---|---|---|---|---|
| first `put` | 1 | 16 | 12 | 0.06 |
| 12th `put` | 12 | 16 | 12 | 0.75 |
| 13th `put` → resize | 13 | 32 | 24 | 0.41 |
| 25th `put` → resize | 25 | 64 | 48 | 0.39 |
| 49th `put` → resize | 49 | 128 | 96 | 0.38 |

The ratio saws between ~0.375 and 0.75 and never exceeds k.

**The power-of-2 payoff.** Doubling adds exactly one bit to the mask:

```
oldN = 16  → mask 0b01111
newN = 32  → mask 0b11111
                    ↑ new bit, value = oldN
```

So each entry either stays or moves by exactly `oldN`, decided by one bit of its
*stored* hash — no `hashCode()` call, no modulo:

```java
if ((hash & oldN) == 0)  -> stays at i        // "lo" list
else                     -> moves to i + oldN // "hi" list
```

```
old bucket 1 (N=16):   17 → 33 → 49 → 65
new bucket 1  (N=32):  33 → 65              (hash & 16 == 0)
new bucket 17 (N=32):  17 → 49              (hash & 16 != 0)
```

Each old chain splits into lo/hi in one pass, order preserved. Tree buckets do the
same via `TreeNode.split()`, untreeifying any half that drops to ≤ 6.

**Choosing k:**

| k | Effect |
|---|---|
| smaller (0.5) | shorter chains, more memory, more frequent resizes |
| **0.75** | Java's balance |
| larger (1.0+) | less memory, longer chains |

## 10. Is resizing costly?

**Yes — once.**

| Cost of one resize | |
|---|---|
| Time | O(n) — every entry relinked |
| Memory | old + new array alive at the same moment |
| GC | old array becomes garbage; large arrays can trigger a long collection |
| Latency | the unlucky `put` takes ms on a large map, while others take ns |

**But O(1) amortised.** Resizes happen at 12, 24, 48, …, n. Summed:
`12 + 24 + 48 + … + n ≤ 2n` — about 2 extra moves per insert. Each resize costs twice
the last but happens half as often.

Doubling is what makes this true. Growing by a fixed +16 would mean n/16 resizes × O(n)
each = **O(n²)**.

> **Amortised O(1) ≠ worst-case O(1).** Fine for throughput (batch jobs); a real spike
> for latency (p99 SLAs, game loops, trading).

**Mitigations:**

- **Pre-size** when you know the count — the usual fix (§12 for the arithmetic).
- **Build once, read many** — resize becomes a one-off setup cost; `Map.copyOf` to freeze.
- **`ConcurrentHashMap`** resizes cooperatively — each writer migrates a chunk, reads
  keep working.
- **Incremental rehash** elsewhere: Redis keeps two tables and moves a few buckets per
  operation; linear hashing splits one bucket at a time.
- **Raise k** (`new HashMap<>(16, 1.0f)`) — fewer resizes, longer chains.

**The map never shrinks.** `remove` doesn't resize down — fill to 1M, clear it, and the
big array stays. Make a new map to reclaim the memory.

---

# Part III — Reference

## 11. Complexity

**`HashMap` / `HashSet`:**

| Operation | Average | Worst (Java 8+) | Worst (≤ Java 7) |
|---|---|---|---|
| `put` / `add` | O(1) **amortised** | O(log n) | O(n) |
| `get` / `remove` / `containsKey` | O(1) | O(log n) | O(n) |
| `containsValue` | O(n) | O(n) | O(n) |
| one `resize` | — | O(n) | O(n) |

Hidden assumption: `hashCode`/`equals` are O(1). A `String` key of length k is O(k) —
its hash is cached after the first call, `equals` is not.

**Add / remove against other collections:**

| Structure | Add | Remove |
|---|---|---|
| `HashMap` / `HashSet` | O(1) avg | O(1) avg |
| `TreeMap` / `TreeSet` | O(log n) | O(log n) |
| `ArrayList` | O(1) amortised at end, O(n) at index | O(n); O(1) only at end |
| `ArrayDeque` | O(1) either end | O(1) either end |
| `PriorityQueue` | O(log n) | O(log n) `poll`, O(n) `remove(obj)` |

## 12. Defaults

```java
static final int   DEFAULT_INITIAL_CAPACITY = 1 << 4;   // 16
static final float DEFAULT_LOAD_FACTOR      = 0.75f;
static final int   MAXIMUM_CAPACITY         = 1 << 30;
```

**Capacity ≠ size.** `size()` is the entry count (0 when new). Capacity is the bucket
count — internal, no public getter.

**The constructor argument is a capacity, not an expected count:**

```java
new HashMap<>(1000);                       // N = 1024, threshold 768 → still resizes
new HashMap<>((int) (1000 / 0.75f) + 1);   // N = 2048, no resize
HashMap.newHashMap(1000);                  // Java 19+ — does the arithmetic for you
```

| Collection | Default capacity | Load factor | Grows |
|---|---|---|---|
| `HashMap` / `LinkedHashMap` / `HashSet` | 16 (lazy) | 0.75 | ×2 |
| `ConcurrentHashMap` | 16 | 0.75 (fixed) | ×2 |
| `Hashtable` (legacy) | **11** | 0.75 | ×2 + 1 — indexes with `%`, so prime-ish sizes |
| `TreeMap` | — (one node per entry) | — | — |
| `ArrayList` | 10 (lazy) | — | ×1.5 |

## 13. Odds and ends

- **`null` key:** `HashMap` allows one (hash 0 → bucket 0). `Hashtable` and
  `ConcurrentHashMap` reject null keys *and* values.
- **Not thread-safe:** concurrent `put` can lose writes. Use `ConcurrentHashMap`, not
  `Collections.synchronizedMap` or `Hashtable`.
- **`HashSet`** is a `HashMap` with a dummy value — identical rules (§14).
- **Iteration order** is unspecified and changes on resize. `LinkedHashMap` keeps
  insertion order; `TreeMap` keeps sorted order (and uses `compareTo`, not `hashCode` —
  so `compareTo` must be consistent with `equals`).
- **Records** generate `equals`/`hashCode`/`toString` from components:
  `record Point(int x, int y) {}` is a correct map key out of the box.

## 14. `HashSet` — a `HashMap` in disguise

There is no separate set algorithm. A `HashSet` holds a `HashMap` and uses only its keys:

```java
public class HashSet<E> extends AbstractSet<E> implements Set<E> {
    private transient HashMap<E, Object> map;
    private static final Object PRESENT = new Object();   // one shared dummy value

    public boolean add(E e)           { return map.put(e, PRESENT) == null; }
    public boolean contains(Object o) { return map.containsKey(o); }
    public boolean remove(Object o)   { return map.remove(o) == PRESENT; }
    public int size()                 { return map.size(); }
    public Iterator<E> iterator()     { return map.keySet().iterator(); }
}
```

**How "no duplicates" falls out of `put`:** `put` returns the *previous* value.

| `add(e)` when… | `map.put(e, PRESENT)` | returns | `add` returns |
|---|---|---|---|
| no `equals` match in the bucket | appends a new node | `null` | `true` |
| an equal key is already there | replaces the value (with `PRESENT` again), **keeps the old key** | `PRESENT` | `false` |

So everything in §6–§10 applies unchanged: same hash spreading, same buckets,
same treeify at 8, same resize at `0.75 × N`.

**Consequences worth knowing:**

- **Duplicate `add` keeps the original object.** If two elements are `equals` but
  distinguishable (different `id`, same `name`), the set holds the first one inserted.
- **Memory:** every element costs a full `HashMap.Node` (~32 bytes with compressed
  oops: hash, key, value, next) plus its slot in the table. The `value` field is wasted —
  that's the price of reusing `HashMap`. `PRESENT` itself is shared, so it's one object total.
- **`LinkedHashSet`** is the same trick: a package-private `HashSet` constructor backs it
  with a `LinkedHashMap` instead, which threads a doubly linked list through the nodes
  → insertion-order iteration.
- **Pre-sizing** is identical: `new HashSet<>(1000)` still resizes; use
  `HashSet.newHashSet(1000)` (Java 19+) or `(int) (n / 0.75f) + 1`.
- **`removeAll` trap:** `AbstractSet.removeAll(c)` iterates whichever side is smaller.
  If the set is *not* bigger than `c`, it calls `c.contains(x)` for every element — when
  `c` is a `List`, that's O(n·m). Wrap the argument: `set.removeAll(new HashSet<>(list))`.

**Other sets — not all of them are a `HashMap`:**

| Set | Backed by | Order | `null` | Notes |
|---|---|---|---|---|
| `HashSet` | `HashMap` | none | one allowed | the default |
| `LinkedHashSet` | `LinkedHashMap` | insertion | one allowed | slightly more memory |
| `TreeSet` | `TreeMap` (red-black tree) | sorted | no | O(log n); uses `compareTo`, not `hashCode` |
| `ConcurrentHashMap.newKeySet()` | `ConcurrentHashMap` | none | no | thread-safe |
| `Set.of(...)` | its own array, **open addressing** (linear probing) | randomised per JVM run | no | immutable; throws on duplicate arguments |
| `EnumSet` | a bit vector (`long`) | enum declaration order | no | one bit per constant — fastest by far |

---

## Summary — say this in an interview

> `==` checks identity, `equals` checks logical equality, and `hashCode` must agree
> with `equals` so hash collections can find the right bucket.
>
> `HashMap` is an array of buckets (`Node[] table`, default 16, allocated on first
> `put`). `hashCode` picks the bucket, `equals` picks the key inside it. Collisions are
> chained in a linked list, which becomes a red-black tree at 8 entries (table ≥ 64),
> so the worst case is O(log n) instead of O(n).
>
> It keeps `n/N ≤ 0.75`; past that it doubles and rehashes. Each entry stays at `i` or
> moves to `i + oldN`. One resize is O(n), but doubling makes `put` O(1) amortised —
> pre-size when you know the count, because the occasional spike is real.

## Self-check

**The contract**

1. Why is `Integer a = 128, b = 128; a == b` false but `127` true?
2. You override `equals` only. Which `HashMap` operation breaks, and why does it fail
   silently instead of throwing?
3. What happens to an entry whose key is mutated after insertion? Is it gone?

**Internals**

4. Why does `HashMap` keep its capacity a power of 2? Name two places it pays off.
5. Two keys collide. Name both ways that can happen, and which one `equals` must settle.
6. When does a bucket become a tree, and why isn't that the default?
7. A bucket is a tree, and its keys share a hash but aren't `Comparable`. What's lookup?

**Resize**

8. Why doubling and not +16? What does growing by a constant cost over n inserts?
9. `new HashMap<>(1000)` — how many entries before the first resize? How do you fit 1000?
10. Is `put` O(1)? Give the honest answer, with the word "amortised" in it.

**`HashSet`**

11. How does `HashSet.add` know whether the element was new? Which object stays in the
    set when you add an `equals` duplicate?
12. `set.removeAll(list)` is unexpectedly slow. Why, and what's the one-line fix?
