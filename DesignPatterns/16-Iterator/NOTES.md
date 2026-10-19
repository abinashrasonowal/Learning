# Iterator

> Walk the elements of a collection one by one without knowing how the collection
> stores them.

You use it every day: `for (T x : list)` is sugar for `list.iterator()` →
`hasNext()` / `next()`. The pattern is about writing that for **your own** collection.

---

## The shape

```java
interface Iterator<T> { boolean hasNext(); T next(); }   // the cursor
interface Iterable<T> { Iterator<T> iterator(); }        // "you can walk me"
class Playlist implements Iterable<Track> {              // the aggregate
    public Iterator<Track> iterator() { return new TrackIterator(); }
    private class TrackIterator implements Iterator<Track> { ... }  // knows the internals
}
```

| Role | Job | Knows about |
|---|---|---|
| **Aggregate** | stores elements, hands out iterators | its own storage |
| **Iterator** | holds a cursor, returns the next element | the aggregate's internals (inner class) |
| **Client** | loops | only `Iterator` / `Iterable` |

The storage detail lives in exactly two places: the aggregate and its iterator. Change
array → linked list and only those two change.

---

## Smell it replaces

```java
public Track[] getTracks() { return tracks; }          // or List<Track>
for (int i = 0; i < p.size(); i++) p.getTracks()[i] ...
```

- callers depend on *how* it's stored (array, index, `size()` vs `length`)
- callers can write into it — `getTracks()[0] = null` corrupts the collection
- every alternative traversal (filtered, reversed) is copy-pasted into every caller

Returning `List.copyOf(...)` fixes the writing, not the coupling to "it's a list".

---

## Reference example's three versions

| Version | What it does | Lesson |
|---|---|---|
| V1 | `getBooks()` + index loop | the smell above |
| V2 | hand-written nested `BookIterator` | the pattern — but `next()` past the end throws `IndexOutOfBoundsException`, not `NoSuchElementException` |
| V3 | `implements Iterable`, delegates to `TreeSet.iterator()` | in real code, **delegate** to the backing collection when you can |

Write your own iterator only when there's no backing collection to delegate to
(linked nodes, a tree, a paged API) or when the traversal is custom (filter, skip).

---

## Implement `Iterable`, not just `createIterator()`

`Iterable<T>` is what unlocks for-each, `forEach(...)`, and `spliterator()`.
A method called `createIterator()` gives you none of that.

To expose a **second** traversal, return an `Iterable` built from a lambda:

```java
public Iterable<Track> cleanOnly() { return () -> new TrackIterator(t -> !t.explicit()); }
for (Track t : playlist.cleanOnly()) ...
```

---

## The contract (what V2 gets wrong)

- `hasNext()` must be **idempotent** — call it twice, nothing moves
- `next()` past the end → `NoSuchElementException`
- `remove()` is optional — the default throws `UnsupportedOperationException`;
  leave it unless you really support it
- each `iterator()` call returns a **fresh** cursor — two loops at once must not
  share position (so don't keep the cursor in the aggregate)

---

## Fail-fast

Modify a collection while iterating it → undefined walk. JDK collections detect this
with a `modCount`: the aggregate bumps it on each structural change, the iterator
remembers the value it started with and throws `ConcurrentModificationException`
on mismatch.

It's a **bug detector**, not a thread-safety guarantee. Need to remove while looping?
`iterator.remove()` or `list.removeIf(...)`.

---

## Iterator vs neighbours

| | Difference |
|---|---|
| **Composite** | Iterator is often how you walk a Composite tree (DFS/BFS iterator) |
| **Visitor** | iterator gives you elements; visitor gives elements an operation (double dispatch) |
| **Stream** | `stream()` is internal iteration — you pass the loop body; `Iterator` is external — you drive the loop |

---

## Pitfalls

- **Cursor stored on the collection** — `playlist.nextTrack()`. Only one traversal
  can exist at a time.
- **Returning the internal list "for iteration"** — the caller can still mutate it.
- **`static` nested iterator** that needs the aggregate's fields — make it an inner
  class or pass the head/array in.
- **Forgetting `NoSuchElementException`** — callers relying on the contract get an
  unrelated exception.

---

## Cheat sheet

- Trigger: a getter that exposes storage so callers can loop
- Fix: `implements Iterable<T>`, return an iterator that hides the storage
- Interview line: *"Iterator separates traversal from storage. In Java, implement
  `Iterable` so for-each works; JDK collections are fail-fast via `modCount`."*
