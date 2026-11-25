# How `ArrayList` works

> An `ArrayList` is an `Object[]` with a `size` counter and a growth policy. Appending
> is cheap because the array is usually bigger than the list; everything else follows
> from "it's an array".

Companion to [NOTES.md](NOTES.md) and [EQUALITY-AND-HASHING.md](EQUALITY-AND-HASHING.md).

1. Structure — array + size
2. Creation — lazy allocation
3. `add` at the end
4. `grow()` — 1.5× and copy
5. Insert / remove in the middle — shifting
6. `get` / `set` — random access
7. Fail-fast iteration — `modCount`
8. Complexity
9. Gotchas
10. `ArrayList` vs the alternatives

Then: Summary · Self-check

---

## 1. Structure — array + size

```java
public class ArrayList<E> extends AbstractList<E>
        implements List<E>, RandomAccess, Cloneable, Serializable {
    transient Object[] elementData;   // backing array; its length is the capacity
    private int size;                 // slots actually in use
}
```

```
elementData (capacity 10)
 [ "a" | "b" | "c" | "d" | null | null | null | null | null | null ]
                          ↑
                       size = 4        slots 4..9 are spare
```

- **Capacity ≠ size** — same distinction as `HashMap`. `size()` is the element count;
  capacity is `elementData.length`, with no public getter.
- **`Object[]`, not `E[]`** — generics are erased, so it stores references and casts on
  `get`. A `List<Integer>` holds pointers to boxed `Integer` objects, not ints (§9).

## 2. Creation — lazy allocation

| Constructor | What happens |
|---|---|
| `new ArrayList<>()` | points at a shared empty array; a real `Object[10]` appears on the first `add` |
| `new ArrayList<>(n)` | allocates `Object[n]` immediately |
| `new ArrayList<>(0)` | a *different* shared empty array — grows from 1: `1, 2, 3, 4, 6, 9…` |
| `new ArrayList<>(coll)` | copies `coll.toArray()` |

Two distinct empty sentinels exist only so the no-arg list knows to jump straight to 10.

## 3. `add` at the end

```
if size == elementData.length  ->  grow()      // full: make room (§4)
elementData[size++] = e
modCount++                                      // §7
```

O(1) when there's spare room — which is almost always.

## 4. `grow()` — 1.5× and copy

```java
int newCapacity = oldCapacity + (oldCapacity >> 1);       // ×1.5
elementData = Arrays.copyOf(elementData, newCapacity);    // new array + System.arraycopy
```

Starting from the default: **10 → 15 → 22 → 33 → 49 → 73 → 109 → …**

**One grow is O(n)** — every reference is copied; old and new arrays are alive at the
same moment, then the old one is garbage.

**But `add` is O(1) amortised.** Grows are geometric, so the total copying over n adds
is `n + n/1.5 + n/1.5² + … ≤ 3n` — a constant per add. Growing by a fixed +10 would be
n/10 grows × O(n) each = **O(n²)**. Same argument as `HashMap` resize (doubling there).

| Growth factor | Spare capacity (worst) | Copies per add (amortised) | Used by |
|---|---|---|---|
| ×1.5 | ≤ 33% | ~3 | `ArrayList` |
| ×2 | ≤ 50% | ~2 | `Vector`, `HashMap`, C++ `std::vector` (GCC) |

×1.5 trades a few more copies for less wasted memory.

**Ceiling:** it won't ask for more than about `Integer.MAX_VALUE - 8` slots (a "soft
max" leaving room for VM array headers); beyond that → `OutOfMemoryError`.

## 5. Insert / remove in the middle — shifting

```java
list.add(2, "X");   // System.arraycopy(elementData, 2, elementData, 3, size - 2)
```

```
before: [ a | b | c | d | _ ]
after:  [ a | b | X | c | d ]      c, d each moved one slot right
```

```java
list.remove(1);     // shift left, then elementData[--size] = null
```

```
before: [ a | b | c | d ]
after:  [ a | c | d | null ]       null the vacated slot so GC can reclaim the object
```

- Cost is **O(size − index)**: cheap at the end, worst at the front.
- **Nulling the tail slot matters.** Without it, the array still references the removed
  object — a memory leak (Effective Java's classic "obsolete reference" example).
- `System.arraycopy` is a native block move, so shifting is fast in practice — but
  still linear. Using an `ArrayList` as a queue (`remove(0)`) is O(n) per poll; use
  `ArrayDeque`.

## 6. `get` / `set` — random access

```java
public E get(int index) {
    Objects.checkIndex(index, size);   // bounds check against size, not capacity
    return (E) elementData[index];
}
```

O(1). `ArrayList` implements the **`RandomAccess`** marker interface; library code
(e.g. `Collections.binarySearch`, `Collections.shuffle`) checks it to pick index-based
loops over iterators.

Indexing between `size` and capacity throws `IndexOutOfBoundsException` even though
the slot physically exists.

## 7. Fail-fast iteration — `modCount`

Every **structural** change (`add`, `remove`, `clear` — not `set`) bumps `modCount`.
The iterator snapshots it and checks on every `next()`:

```java
for (String s : list)
    if (s.isEmpty()) list.remove(s);   // ConcurrentModificationException
```

Fixes:

```java
list.removeIf(String::isEmpty);        // best — one O(n) compaction pass

Iterator<String> it = list.iterator(); // iterator's own remove keeps modCount in sync
while (it.hasNext()) if (it.next().isEmpty()) it.remove();

for (int i = list.size() - 1; i >= 0; i--)   // backwards — shifts don't skip elements
    if (list.get(i).isEmpty()) list.remove(i);
```

Removing in a loop with `it.remove()` or by index is O(n²) worst case (each removal
shifts); `removeIf` is O(n). Fail-fast is best-effort — a bug detector, not a
concurrency guarantee.

---

## 8. Complexity

| Operation | Cost |
|---|---|
| `get` / `set` | O(1) |
| `add(e)` | O(1) amortised; O(n) on the add that grows |
| `add(i, e)` / `remove(i)` | O(n − i) |
| `contains` / `indexOf` / `remove(Object)` | O(n) — linear scan with `equals` |
| `removeIf` / `removeAll` / `retainAll` | O(n) passes (plus the argument's `contains`) |
| `clear` | O(n) — nulls every slot |
| `sort` | O(n log n) — TimSort, directly on `elementData` |

## 9. Gotchas

- **Never shrinks.** `remove` and `clear()` null slots but keep the capacity. Call
  `trimToSize()` to release it.
- **Pre-size when you know the count:** `new ArrayList<>(n)` or `ensureCapacity(n)`.
  Unlike `HashMap`, the argument *is* the element count — no load factor.
- **`remove(int)` vs `remove(Object)`** on a `List<Integer>`: `list.remove(1)` removes
  **index 1**. To remove the value 1: `list.remove(Integer.valueOf(1))`.
- **Boxing cost.** `List<Integer>` ≈ 4-byte reference + ~16-byte `Integer` per element,
  scattered on the heap — vs 4 contiguous bytes in an `int[]`. For hot numeric loops,
  use `int[]` (`Integer` −128..127 is cached, see EQUALITY-AND-HASHING §1).
- **`subList` is a view**, not a copy. Writes go through to the parent; a structural
  change to the parent makes the sub-list throw `ConcurrentModificationException`.
  `list.subList(a, b).clear()` is the idiomatic range delete.
- **`Arrays.asList(arr)`** is fixed-size and backed by `arr` — `set` writes into the
  array, `add`/`remove` throw `UnsupportedOperationException`. **`List.of(...)`** is
  fully immutable and rejects `null`.
- **`toArray()`** returns a fresh copy, so mutating it doesn't affect the list.
- **Not thread-safe** — concurrent `add`s can lose elements or throw
  `ArrayIndexOutOfBoundsException` mid-grow.

## 10. `ArrayList` vs the alternatives

| | `ArrayList` | `LinkedList` | `ArrayDeque` |
|---|---|---|---|
| Backing | resizable array | doubly linked nodes | circular resizable array |
| `get(i)` | O(1) | O(n) | — (no index access) |
| add/remove at end | O(1) amortised | O(1) | O(1) amortised |
| add/remove at front | O(n) | O(1) | O(1) amortised |
| insert in middle | O(n) shift | O(1) *once at the node* — but finding it is O(n) | — |
| Memory per element | 4-byte ref (+ spare) | ~24-byte node + ref | 4-byte ref (+ spare) |
| Cache locality | good | poor | good |

**Rule of thumb:** `ArrayList` for lists, `ArrayDeque` for stacks and queues.
`LinkedList` almost never wins in practice.

Thread-safe options:

| | How | Good for |
|---|---|---|
| `Collections.synchronizedList(list)` | one lock around every call; iterate inside `synchronized (list)` | low contention |
| `CopyOnWriteArrayList` | every write copies the whole array; iterators read a snapshot | reads ≫ writes (listener lists) |
| `Vector` | legacy, synchronized methods, grows ×2 | nothing new |

---

## Summary — say this in an interview

> `ArrayList` is an `Object[]` plus a `size`. The default constructor allocates lazily —
> capacity 10 on the first `add`. Appending writes into the next spare slot; when full,
> it grows by 1.5× and copies with `System.arraycopy`. One grow is O(n), but geometric
> growth makes `add` O(1) amortised.
>
> `get`/`set` are O(1). Inserting or removing in the middle shifts everything after it,
> so it's O(n), and removal nulls the vacated slot for GC. Structural changes bump
> `modCount`, which makes iterators fail fast. It never shrinks on its own —
> `trimToSize()` does that — and it isn't thread-safe.

## Self-check

1. `new ArrayList<>()` — how big is the backing array before and after the first `add`?
2. Write the capacity sequence for the first five grows from the default.
3. Why is `add` O(1) amortised, and what would growing by +10 each time cost instead?
4. Why does `remove(i)` set `elementData[size]` to `null`? What breaks if it doesn't?
5. `List<Integer> l = new ArrayList<>(List.of(5, 1, 7)); l.remove(1);` — what's left?
6. Why does removing inside a for-each loop throw, and what's the O(n) fix?
7. `new ArrayList<>(1000)` vs `new HashMap<>(1000)` — which one avoids resizing for
   1000 elements, and why the difference?
8. You need a FIFO queue of a million items. Why not `ArrayList`, and what instead?
