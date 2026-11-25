# How `TreeMap` works

> A `TreeMap` is a **red-black tree**: a binary search tree that recolours and rotates
> itself after every insert and delete so its height stays ≤ 2·log₂(n+1). Sorted
> order, `floor`/`ceiling` and O(log n) worst case all fall out of that one fact.

Companion to [EQUALITY-AND-HASHING.md](EQUALITY-AND-HASHING.md): `HashMap` finds keys
with `hashCode` + `equals`; `TreeMap` uses **neither**, only `compare`.

**Part I — structure**
1. Structure — a tree of `Entry` nodes
2. Ordering — `compare` is the only question asked

**Part II — operations** (in the order you meet them)

3. `get` — walk down
4. `put` — BST insert, then fix the colours
5. Rotations
6. The red-black rules, and why they bound the height
7. `remove` — successor swap, then fix
8. Iteration — successor via parent pointers
9. Navigation — `floor`, `ceiling`, `first`, `poll`
10. Views — `headMap`, `subMap`, `descendingMap`

**Part III — reference**

11. Complexity · 12. Gotchas · 13. DSA patterns · 14. `TreeSet` and the alternatives

Then: Summary · Self-check

---

# Part I — Structure

## 1. Structure — a tree of `Entry` nodes

```java
public class TreeMap<K,V> extends AbstractMap<K,V> implements NavigableMap<K,V> {
    private final Comparator<? super K> comparator;   // null → natural order (Comparable)
    private transient Entry<K,V> root;
    private transient int size;
    private transient int modCount;                    // fail-fast iterators

    static final class Entry<K,V> implements Map.Entry<K,V> {
        K key;
        V value;
        Entry<K,V> left, right;
        Entry<K,V> parent;            // makes iteration stackless (§8)
        boolean color = BLACK;
    }
}
```

**No array, no buckets, no capacity, no load factor, no resize.** Each `put` allocates
exactly one node and links it in. The tree is the whole data structure.

**BST invariant:** for every node, everything in `left` is smaller and everything in
`right` is larger. In-order traversal (left → node → right) therefore visits keys
**min to max**. That's the entire reason a `TreeMap` iterates sorted.

**Memory:** an `Entry` is ~40 bytes (header + 5 references + a boolean, compressed
oops) against ~32 for a `HashMap.Node`, but there's no backing table, so per entry
they come out roughly even.

## 2. Ordering — `compare` is the only question asked

```java
final int compare(Object k1, Object k2) {
    return comparator == null ? ((Comparable<? super K>) k1).compareTo((K) k2)
                              : comparator.compare((K) k1, (K) k2);
}
```

Every operation reduces to: `compare(key, node.key)` is `< 0` → go left, `> 0` → go
right, **`== 0` → this is the same key.**

So `compare` replaces **both** `hashCode` and `equals`:

| Question | `HashMap` asks | `TreeMap` asks |
|---|---|---|
| Where does it go? | `hashCode()` → bucket | `compare` → left / right |
| Is it the same key? | `equals()` | `compare(...) == 0` |

Consequences (all verified on Java 17):

```java
TreeMap<String,Integer> m = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
m.put("Apple", 1);
m.put("APPLE", 2);
m;            // {Apple=2} — one entry: compare said "same key"
              // value replaced, the ORIGINAL key object kept
```

- **No `Comparable` and no comparator** → the first `put` throws `ClassCastException`.
- **`null` key with natural ordering** → `NullPointerException`, even on an empty map
  (`put` calls `compare(key, key)` on the first insert precisely to fail early).
  `get(null)` throws too. **`null` values are fine.**
- **"Consistent with equals":** if `compare(a, b) == 0` but `!a.equals(b)`, the map
  still obeys `compare`, which breaks the general `Map` contract. `BigDecimal` is the
  classic case: `new BigDecimal("1.0")` and `new BigDecimal("1.00")` are different keys
  in a `HashMap` and the **same** key in a `TreeMap`.

---

# Part II — Operations

## 3. `get` — walk down

```java
Entry<K,V> p = root;
while (p != null) {
    int cmp = compare(key, p.key);
    if      (cmp < 0) p = p.left;
    else if (cmp > 0) p = p.right;
    else              return p;
}
return null;
```

One comparison per level, so the cost is the **height**. For a plain BST the height
can be n; §6 is why it's ~log n here.

`containsValue` is the exception: values aren't ordered, so it's a full O(n) scan.

## 4. `put` — BST insert, then fix the colours

```java
// simplified — the JDK splits comparator / Comparable paths, same logic
public V put(K key, V value) {
    Entry<K,V> t = root;
    if (t == null) {
        compare(key, key);                       // type- and null-check the first key
        root = new Entry<>(key, value, null);
        size = 1; modCount++;
        return null;
    }
    Entry<K,V> parent;
    int cmp;
    do {                                         // 1. walk down, exactly like get
        parent = t;
        cmp = compare(key, t.key);
        if      (cmp < 0) t = t.left;
        else if (cmp > 0) t = t.right;
        else    return t.setValue(value);        //    found: replace value, keep old key
    } while (t != null);

    Entry<K,V> e = new Entry<>(key, value, parent);   // 2. hang the new leaf
    if (cmp < 0) parent.left = e; else parent.right = e;

    fixAfterInsertion(e);                        // 3. restore red-black rules
    size++; modCount++;
    return null;
}
```

**Step 3, `fixAfterInsertion(x)`:** the new node is coloured **red** (adding red never
changes black-heights, so only "no red-red" can break). While `x`'s parent is red:

| Case | Uncle is… | Fix | Then |
|---|---|---|---|
| **1. Recolour** | red | parent + uncle → black, grandparent → red | move `x` up to the grandparent, loop |
| **2. Triangle** | black (or null) | `x` is an inner child: rotate at the parent to turn it into case 3 | fall through |
| **3. Line** | black (or null) | rotate at the grandparent the other way, swap parent/grandparent colours | **done** |

Finally `root.color = BLACK`. At most **2 rotations** per insert; recolouring can climb
to the root, so the fix is O(log n).

### Worked example: insert 10, 20, 30, 40, 50

Sorted input — the case that turns a plain BST into a linked list of height 5.

```
insert 10        insert 20          insert 30: red 30 under red 20, uncle null
                                    → case 3 (right-right line): rotateLeft(10)
  10B              10B                   20B
                     \                  /   \
                     20R              10R   30R

insert 40: red 40 under red 30, uncle 10 is red → case 1: recolour, 20 is root → black
     20B
    /   \
  10B   30B
          \
          40R

insert 50: red 50 under red 40, uncle (30.left) null → case 3: rotateLeft(30)
     20B
    /   \
  10B   40B
        /  \
      30R  50R
```

Height 3 instead of 5, and it keeps doing this as n grows.

## 5. Rotations

A rotation changes which node is the parent **without changing in-order order**, so the
BST invariant survives. It's O(1): three pointer moves plus parent links.

```
        rotateLeft(x)  →
    x                    y
   / \                  / \
  a   y        ←       x   c
     / \  rotateRight(y)  / \
    b   c                a   b
```

`a < x < b < y < c` before and after. Only `b` changes parent.

## 6. The red-black rules, and why they bound the height

Full treatment — every insert/delete case, worked deletes, code, the 2-3-4 view:
[RED-BLACK-TREE.md](../07-trees-bst/RED-BLACK-TREE.md).

1. Every node is red or black; the root is black; null leaves count as black.
2. **No red node has a red child.**
3. **Every path from a node down to a null leaf crosses the same number of black
   nodes** (its *black-height*).

**Why that gives O(log n):** let the root's black-height be `b`.
- Rule 3: every root-to-leaf path has exactly `b` black nodes.
- Rule 2: reds can't be adjacent, so a path has at most `b` reds → **longest path ≤ 2b**.
- The all-black top `b` levels are a full tree: `n ≥ 2ᵇ − 1`, so `b ≤ log₂(n+1)`.
- **Height ≤ 2·log₂(n+1).** A million keys → at most ~40 levels.

**Why not AVL?** AVL trees are more strictly balanced (height ≤ ~1.44·log n), so
lookups are slightly faster, but they rotate more on insert and delete. Red-black
trees do **≤ 2 rotations per insert, ≤ 3 per delete** — a better fit for a
general-purpose map with mixed reads and writes. (Same reason `HashMap`'s treeified
buckets are red-black.)

## 7. `remove` — successor swap, then fix

`deleteEntry(p)`:

1. **Two children?** Find the in-order successor `s` (the leftmost node of `p.right`),
   **copy `s.key` and `s.value` into `p`**, and delete `s` instead. `s` has no left
   child, so the problem is now "delete a node with at most one child".
2. **Splice it out:** its single child (if any) takes its place.
3. **If the removed node was black**, one path just lost a black node — rule 3 is
   broken. `fixAfterDeletion` repairs it by looking at the sibling:

| Sibling | Fix |
|---|---|
| red | rotate so the sibling becomes black, re-examine |
| black, both its children black | recolour sibling red, push the problem up a level |
| black, near child red | rotate at the sibling → becomes the next case |
| black, far child red | rotate at the parent, recolour — **done** |

At most **3 rotations**; recolouring can climb, so O(log n). Removing a red node needs
no fix at all.

**Side effect of step 1:** the `Entry` object that used to hold the successor's key now
holds a different key. That's why `TreeMap`'s iterator has special handling for
`iterator.remove()` on a two-child node, and why you should never keep `Entry`
references across a modification.

## 8. Iteration — successor via parent pointers

```java
static <K,V> Entry<K,V> successor(Entry<K,V> t) {
    if (t == null) return null;
    if (t.right != null) {                    // leftmost node of the right subtree
        Entry<K,V> p = t.right;
        while (p.left != null) p = p.left;
        return p;
    }
    Entry<K,V> p = t.parent, ch = t;          // else climb until we arrive from a left child
    while (p != null && ch == p.right) { ch = p; p = p.parent; }
    return p;
}
```

- Iteration starts at `getFirstEntry()` (keep going left) and calls `successor`
  repeatedly. **No recursion, no stack** — that's what the `parent` field buys.
- A single step can be O(log n), but each edge is walked down once and up once over a
  full traversal, so the **whole pass is O(n)**, **O(1) amortised per step**.
- Fail-fast: modifying the map outside `iterator.remove()` throws
  `ConcurrentModificationException` (same `modCount` idea as `ArrayList` and `HashMap`).
- `entrySet()` iteration hands out the **real** nodes: `entry.setValue(v)` writes
  through to the map.

## 9. Navigation — `floor`, `ceiling`, `first`, `poll`

What `HashMap` can't do at all. All are one walk down, remembering the best candidate.

```java
// ceilingEntry(key): smallest key >= key  (simplified)
Entry<K,V> p = root, best = null;
while (p != null) {
    int cmp = compare(key, p.key);
    if      (cmp < 0) { best = p; p = p.left; }    // p is a candidate; look for smaller
    else if (cmp > 0) p = p.right;                 // too small; go right
    else    return p;                              // exact hit
}
return best;
```

| Method | Returns | Cost |
|---|---|---|
| `firstKey()` / `lastKey()` | min / max key, **throws** if empty | O(log n) |
| `firstEntry()` / `lastEntry()` | min / max entry, `null` if empty | O(log n) |
| `floorKey(k)` / `ceilingKey(k)` | largest ≤ k / smallest ≥ k | O(log n) |
| `lowerKey(k)` / `higherKey(k)` | largest < k / smallest > k | O(log n) |
| `pollFirstEntry()` / `pollLastEntry()` | remove and return min / max | O(log n) |

Note `first*`/`last*` are **O(log n), not O(1)** — the JDK doesn't cache the leftmost
node.

**The `*Entry` methods return immutable snapshots** (`AbstractMap.SimpleImmutableEntry`):
`map.floorEntry(k).setValue(v)` throws `UnsupportedOperationException`. Write back with
`map.put(e.getKey(), v)` instead.

## 10. Views — `headMap`, `subMap`, `descendingMap`

```java
m.headMap(k)              // keys <  k        headMap(k, true)  → keys <= k
m.tailMap(k)              // keys >= k        tailMap(k, false) → keys >  k
m.subMap(lo, hi)          // lo <= key < hi   (inclusive flags available)
m.descendingMap()         // reverse order
m.keySet(), m.values(), m.entrySet(), m.navigableKeySet(), m.descendingKeySet()
```

- **Live views, not copies.** Creating one is O(1). Changes go both ways: a `put` on
  the map appears in an existing `headMap`, and `view.remove(k)` removes from the map.
- A range view **rejects keys outside its range**: `m.headMap(20).put(25, …)` throws
  `IllegalArgumentException`.
- **`view.size()` is O(k)** — it counts by iterating the range. Don't call it in a loop.
  (The full map's `size()` is O(1); it's a field.)

---

# Part III — Reference

## 11. Complexity

| Operation | `TreeMap` | `HashMap` |
|---|---|---|
| `get` / `put` / `remove` / `containsKey` | **O(log n) worst case** | O(1) average, O(log n) worst (treeified bucket) |
| `containsValue` | O(n) | O(n) |
| `firstKey` / `lastKey` | O(log n) | O(n) |
| `floor` / `ceiling` / `higher` / `lower` | O(log n) | — |
| sorted iteration | O(n) | O(n log n) — copy and sort |
| create a range view | O(1) | — |
| `new TreeMap<>(sortedMap)` | **O(n)** (`buildFromSorted`) | — |
| `new TreeMap<>(hashMap)` | O(n log n) — n `put`s | — |

`buildFromSorted` builds a perfectly balanced tree directly from already-sorted input,
colouring only the bottom partial level red. That's why copying a `TreeMap` (or a
`putAll` from a `SortedMap` with the same comparator into an **empty** `TreeMap`) is
linear.

**In practice `HashMap` is several times faster** even where the big-O is close: every
`TreeMap` step is a pointer chase to a node somewhere else in memory (a cache miss),
and every comparison is a method call. Use `TreeMap` because you need **order**, not
for speed.

## 12. Gotchas

- **Mutating a key's ordering fields after insertion corrupts the tree.** The node stays
  where it was placed; `get` now walks the wrong branch and misses it, and
  `floor`/`ceiling` answers become wrong for *other* keys too. Remove, mutate, re-insert.
  (Same rule as mutating a `HashMap` key's `hashCode` fields — worse, because a tree's
  damage isn't confined to one bucket.)
- **A comparator that returns 0 for distinct objects silently merges them.** Comparing
  people by `age` alone means one entry per age. Add a tie-breaker:
  `Comparator.comparing(Person::age).thenComparing(Person::id)`.
- **`(a, b) -> a - b` overflows.** `Integer.MIN_VALUE - 1` wraps to positive and the tree
  orders wrongly. Use `Integer.compare(a, b)` or `Comparator.naturalOrder()`.
- **A comparator must be consistent** (transitive, antisymmetric). A broken one doesn't
  throw — `get` just stops finding keys.
- **`firstKey()` on an empty map throws `NoSuchElementException`;** `firstEntry()`
  returns `null`. Pick the one that matches how you want to handle emptiness.
- **`get` with a key of the wrong type throws `ClassCastException`** instead of
  returning `null` like `HashMap` does.
- **Not thread-safe.** `Collections.synchronizedSortedMap` locks everything;
  `ConcurrentSkipListMap` is the concurrent sorted map (skip list, not a tree; O(log n)
  expected, lock-free).

## 13. DSA patterns

**Sorted multiset** — Java has none, so a `TreeMap<Integer, Integer>` of counts is the
standard stand-in (sliding-window min/max with removals, "k-th smallest in a stream"):

```java
TreeMap<Integer, Integer> cnt = new TreeMap<>();
cnt.merge(x, 1, Integer::sum);                                 // add one x
cnt.computeIfPresent(x, (k, v) -> v == 1 ? null : v - 1);      // remove one x (null deletes)
int min = cnt.firstKey(), max = cnt.lastKey();                 // O(log n) each
```

A `PriorityQueue` gives min *or* max and only removes the top cheaply
(`remove(x)` is O(n)); the counted `TreeMap` gives both ends **and** arbitrary removal
in O(log n).

**Intervals keyed by start** — "does `[s, e)` overlap anything booked?"
(My Calendar, merge-on-insert):

```java
TreeMap<Integer, Integer> booked = new TreeMap<>();            // start → end
Map.Entry<Integer,Integer> prev = booked.floorEntry(s);         // last interval starting ≤ s
Integer next = booked.ceilingKey(s);                            // first interval starting ≥ s
boolean free = (prev == null || prev.getValue() <= s) && (next == null || next >= e);
if (free) booked.put(s, e);
```

**Other signals to reach for it:** "nearest value ≤ / ≥ x", "count of keys in a range"
(via a view — but mind §10's O(k) `size()`), "process events in time order while still
inserting new ones", line sweep with a changing set of active heights (skyline).

## 14. `TreeSet` and the alternatives

`TreeSet` is to `TreeMap` what `HashSet` is to `HashMap`
([EQUALITY-AND-HASHING §14](EQUALITY-AND-HASHING.md#14-hashset--a-hashmap-in-disguise)):

```java
public class TreeSet<E> extends AbstractSet<E> implements NavigableSet<E> {
    private transient NavigableMap<E,Object> m;              // a TreeMap
    private static final Object PRESENT = new Object();
    public boolean add(E e) { return m.put(e, PRESENT) == null; }
}
```

Everything above applies unchanged; `floor`, `ceiling`, `first`, `pollFirst` and the
range views (`headSet`, `subSet`) are the same tree walks.

| Need | Use |
|---|---|
| key → value, no order needed | `HashMap` |
| insertion order, or LRU (`accessOrder = true`) | `LinkedHashMap` |
| **sorted order, nearest key, ranges** | **`TreeMap`** |
| only the min (or max), repeatedly | `PriorityQueue` — O(1) peek, smaller and faster |
| sorted *and* concurrent | `ConcurrentSkipListMap` |
| enum keys | `EnumMap` — an array indexed by ordinal |

---

## Summary — say this in an interview

> `TreeMap` is a red-black tree of `Entry` nodes (key, value, left, right, parent,
> colour). It orders and identifies keys **only** through `compareTo` or the
> comparator — `hashCode` and `equals` are never called, so `compare == 0` means "same
> key".
>
> `put` is a BST insert of a red node followed by recolouring and at most two rotations;
> `remove` swaps in the successor and needs at most three. The red-black rules keep the
> height ≤ 2·log₂(n+1), so every operation is **O(log n) worst case**.
>
> In-order traversal via parent pointers gives sorted iteration in O(n), and the same
> walk-down gives `floor`, `ceiling` and range views — which is the reason to choose it
> over `HashMap`, not speed.

## Self-check

**Structure and ordering**

1. Which methods of the key does `TreeMap` call — `hashCode`, `equals`, `compareTo`?
   What does `compare(a, b) == 0` mean to it?
2. Why does `new TreeMap<>().put(null, 1)` throw on an *empty* map?
3. A case-insensitive `TreeMap` gets `put("Apple", 1)` then `put("APPLE", 2)`.
   What's in it?

**Balancing**

4. Why is a new node coloured red rather than black?
5. Insert 1, 2, 3 into an empty red-black tree. Draw it after each step and name the
   case used.
6. Derive the height bound from rules 2 and 3.
7. Why does the JDK use red-black trees instead of AVL?

**Operations**

8. How does `remove` handle a node with two children, and why is that safe?
9. How does iteration avoid recursion? Why is a full pass O(n) when one `successor`
   call can be O(log n)?
10. `map.floorEntry(k).setValue(v)` throws. Why, and what do you write instead?
11. You hold `m.headMap(50)` and then `m.put(10, …)`. Is 10 in the view? What about
    `view.put(70, …)`?

**Choosing**

12. You need the min and max of a sliding window where elements also leave. Why is a
    `TreeMap` of counts better than a `PriorityQueue`?
13. `TreeMap` and `HashMap` both have O(log n) worst-case `get`. Why is `HashMap` still
    much faster in practice?
