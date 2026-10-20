# DSA — How to practise

The failure mode isn't solving too few problems. It's solving 300 and retaining 40
because nothing was ever revisited and no pattern was ever named. This file exists to
stop that.

---

## The session (60–75 min)

| Phase | Time | Rule |
|---|---|---|
| **Warm-up** | 5 min | one problem from `PROBLEMS.md` that's due for revisit. From memory. |
| **Build** | 25 min | the current topic's structure/technique, from scratch, with `assert`s. |
| **Solve** | 30 min | 1–2 new problems in that topic's pattern. |
| **Log** | 5 min | write the *insight* into `PROBLEMS.md`. Non-negotiable. |

Skip the Log phase and the session was entertainment.

## The 25-minute rule

Cold attempt, timer on:

1. **0–5 min** — restate the problem in one sentence. State the brute force and its
   complexity **out loud before writing anything**. If you can't, you've misread it.
2. **5–20 min** — solve. Brute force first if the optimal isn't obvious; a working
   O(n²) beats a broken O(n).
3. **20–25 min** — stuck? Write down *exactly what you don't know*, then look at the
   solution.
4. **Looked it up? It is not solved.** Log it as `hinted`, close everything, and
   rewrite it from empty the next day. Only then does it count.

Reading a solution and nodding is the single most common way to waste a month.

## Revision cadence

Every logged problem gets a revisit date: **+1 day, +7 days, +30 days.**

- Solved cold in under 10 min at a checkpoint → advance to the next interval
- Struggled or peeked → reset to +1 day
- Cleared +30 → mark `retained`, stop tracking it

Three passes is enough for almost everything. If a problem fails +30 twice, the gap
isn't that problem — it's the topic's `NOTES.md`. Go back and reread it.

## Rules for every implementation

- **Build the structure yourself in the topic folder; use the library everywhere else.**
  Write your own `HashMap` in 01, then use `java.util.HashMap` for the rest of your life.
- **State the complexity in a comment above the method** — time *and* space. Wrong
  guesses caught here are free; caught in an interview they're not.
- Every file has a `demo()` or `main` with `assert`s covering: the normal case, **empty
  input**, a single element, and the case the problem's constraints call out (duplicates,
  negatives, overflow). That's four asserts, not a test suite.
- Assert on **values**, never on a println. A method that prints "ok" proves nothing.
- No `static` scratch fields shared between methods. Pass state; return values.
- Name the variable after its meaning (`left`, `windowSum`), not its type (`i2`, `tmp`).
  You'll reread this in a month.

```bash
javac -Xlint:all -d ../out *.java && java -ea -cp ../out TwoSum
```

## What goes in a topic's `NOTES.md`

Same discipline as the rest of this repo — revision material, not a textbook:

- **The trigger** — the sentence in a problem statement that means "use this". This is
  the most valuable line in the file.
- **The invariant** — what stays true on every iteration. If you can't name it, you're
  pattern-matching instead of understanding.
- **The Java gotcha** — boxing, overflow, sort stability, iterator invalidation.
- **The complexity**, and what the space actually holds.
- **The classic bug** — off-by-one, the un-advanced pointer, the un-reset window.

## Self-check per topic

Before ticking a topic in `README.md`, answer without looking:

1. What in a problem statement tells me to reach for this?
2. What's the invariant, and where in my loop is it maintained?
3. Where does the naive version spend its time, and what did I trade to fix it?
4. What's the edge case that breaks a careless implementation?

Can't answer #1 → you have code but no pattern. That's the one that transfers.

## Commit

```bash
git add Learning/DSA && git commit -m "dsa: <topic> — <what>"
```

One commit per topic or per few problems. The diff is revision material.
