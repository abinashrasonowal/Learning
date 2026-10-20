# DSA

Data structures and algorithms kept for revision, not for streak-count. Java 17,
no build tool — same conventions as [DesignPatterns](../DesignPatterns/).

Two halves, and both are load-bearing:

- **Per-topic folders** — the structure or technique built from scratch, with `assert`s.
  Writing a hash map is how you stop treating it as magic.
- **[PROBLEMS.md](PROBLEMS.md)** — every problem solved, filed under the *pattern* it
  taught, with a revisit date. Notes are what you reread; the log is the proof you can
  apply them cold.

Which problems? **[SHEET.md](SHEET.md)** — the Love Babbar sheet's 429 problems
deduplicated to ~152, one per distinct concept, sorted into the 12 topics below.

Read [PRACTICE.md](PRACTICE.md) before the first session — it sets the timebox rules
and the revision cadence. Those two are the whole difference between practice and
grinding.

## Running

```bash
cd DSA/01-arrays-hashing

java TwoSum.java                                   # single file, has its own demo()

javac -Xlint:all -d ../out *.java                  # whole topic
java -ea -cp ../out TwoSum
```

`-ea` matters — every file's checks are `assert`s and are silently skipped without it.

## Topics

| # | Topic | Core idea | Notes | Code |
|---|---|---|---|---|
| 01 | [arrays-hashing](01-arrays-hashing/) | trade space for O(1) lookup | ✅ | ⬜ |
| 02 | two-pointers | sorted input ⇒ move from both ends | ⬜ | ⬜ |
| 03 | sliding-window | contiguous subarray, expand/contract | ⬜ | ⬜ |
| 04 | stack-queue | remember what's still unresolved | ⬜ | ⬜ |
| 05 | binary-search | halve a *monotonic* search space | ⬜ | ⬜ |
| 06 | linked-list | pointer surgery, fast/slow | ⬜ | ⬜ |
| 07 | trees-bst | recursion is the traversal | ⬜ | ⬜ |
| 08 | heaps-intervals | k-th anything, sort-by-start | ⬜ | ⬜ |
| 09 | graphs | BFS = shortest hops, DFS = reachability | ⬜ | ⬜ |
| 10 | dp | define the state, then the transition | ⬜ | ⬜ |
| 11 | backtracking | choose → recurse → un-choose | ⬜ | ⬜ |
| 12 | greedy-bits | prove the exchange argument first | ⬜ | ⬜ |

Order matters — 03 assumes 02, 09 assumes 04 and 07, 10 assumes 11. Don't jump to DP.

## Complexity floor — know these cold

| Operation | Cost | Watch out |
|---|---|---|
| `HashMap` get/put | O(1) amortised | O(n) worst case on collisions; treeifies at 8 per bucket |
| `Arrays.sort(int[])` | O(n log n) avg | **O(n²) adversarial** — dual-pivot quicksort, not stable |
| `Arrays.sort(T[])` | O(n log n) worst | TimSort, stable — box your primitives if you need the guarantee |
| `ArrayList` get / add | O(1) / O(1) amortised | `add(0, x)` is O(n) |
| `LinkedList` get | O(n) | almost never the right choice; use `ArrayDeque` |
| `TreeMap` get/put | O(log n) | when you need *ordered* keys or `floorKey`/`ceilingKey` |
| `PriorityQueue` peek/poll | O(1) / O(log n) | building from a collection is O(n), not O(n log n) |
| `String` concat in a loop | **O(n²)** | `StringBuilder` |
| Recursion depth | ~10⁴ frames | JVM default stack ≈ 512KB–1MB; deep DFS on 10⁵ nodes overflows |

## Progress

- Topics: 1 of 12 have notes, 0 have code
- Problems logged: 0 of ~152 curated ([SHEET.md](SHEET.md)) — log in [PROBLEMS.md](PROBLEMS.md)
