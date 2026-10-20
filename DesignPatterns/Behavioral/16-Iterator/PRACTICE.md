# 16-Iterator — Session Plan

Goal is not "wrap an ArrayList and call its iterator" — the course's V3 already did
that. It's **write a real iterator over storage nobody else can see, and honour the
`Iterator` contract**. Different domain from the book example on purpose.

---

## Layout

```
16-Iterator/
  iterator/problem/Track.java                 title, seconds, explicit
  iterator/problem/Playlist.java              array-backed, getTracks() leaks the array
  iterator/problem/IteratorProblemMain.java
  iterator/solution/Track.java
  iterator/solution/Playlist.java             linked nodes + inner TrackIterator, implements Iterable
  iterator/solution/IteratorSolutionMain.java
```

```bash
javac -Xlint:all -d out $(find . -name '*.java') && java -ea -cp out iterator.solution.IteratorSolutionMain
```

## Rules for `solution/`

- no getter that returns the storage — callers see only `Iterator<Track>`
- storage is **not** a JDK collection (linked nodes), so you can't delegate
- `Playlist implements Iterable<Track>` — for-each must work
- a second traversal (`cleanOnly()`) without a new loop in any caller
- `next()` past the end → `NoSuchElementException`
- adding during iteration → `ConcurrentModificationException`
- prove it with `assert`, not prints

---

## 1. Read — 15 min

`NOTES.md` top to bottom.

## 2. Build — ~45 min

| Step | Build | Must prove |
|---|---|---|
| **problem** | array-backed playlist, index loops in the driver | it works — then corrupt it through `getTracks()` |
| **solution** | linked storage, inner iterator, `Iterable` | same total and clean count as `problem/` |
| **contract** | exhaustion, `remove()`, two cursors at once | each asserted |
| **fail-fast** | `modCount` | `add` inside a for-each throws |

## 3. Break it on purpose

- Move the cursor onto `Playlist` itself (`private Node cursor`). Run the
  two-iterators assert. Explain in a comment why it fails. Revert.
- Make `TrackIterator` a `static` nested class. Read the compiler error, then
  explain what an inner class captures. Revert.

## 4. Self-check — 10 min

1. What does `Iterable` give you that `createIterator()` doesn't?
2. Why does each `iterator()` call need its own cursor?
3. What is `modCount` and what is it *not* (hint: threads)?
4. External vs internal iteration — which is `Stream`?
5. When should you write your own iterator instead of delegating?

## 5. Commit

```bash
git add DesignPatterns/16-Iterator && git commit -m "iterator: <step>"
```

---

## Done when

- [x] Watched the course video, did the course exercise
- [ ] Read `NOTES.md`
- [x] problem — written, runs, corrupted from outside
- [x] solution — same numbers as problem, for-each works
- [x] contract + fail-fast asserted
- [ ] broken once (shared cursor), reverted
- [ ] All 5 self-check questions answered without looking
- [ ] Committed

Then tick `16 — Iterator` in `../README.md`.

**Stuck?** Ask me for a nudge on the pattern — not the code.
