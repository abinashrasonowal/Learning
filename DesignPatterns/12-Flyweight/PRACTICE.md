# 12-Flyweight — Session Plan

Goal is not "share a `String` through a factory" — the course's `BulletType` does that
and saves nothing. It's **share heavy intrinsic state, pass extrinsic state in, and
measure the saving with asserts**. Different domain on purpose.

---

## Layout

```
12-Flyweight/
  flyweight/problem/TextureLoader.java      counts loads; 1 KB per texture
  flyweight/problem/Tree.java               x, y, name, color, own texture
  flyweight/problem/Forest.java
  flyweight/problem/FlyweightProblemMain.java   1000 trees -> 1000 loads
  flyweight/solution/TextureLoader.java
  flyweight/solution/TreeType.java          flyweight: name, color, texture; draw(x, y)
  flyweight/solution/TreeTypeFactory.java   (name, color) -> shared TreeType
  flyweight/solution/Tree.java              record: x, y, type
  flyweight/solution/Forest.java
  flyweight/solution/FlyweightSolutionMain.java
```

```bash
javac -Xlint:all -d out $(find . -name '*.java') && java -ea -cp out flyweight.solution.FlyweightSolutionMain
```

## Rules for `solution/`

- `TreeType` is immutable and never exposes its texture
- position is never stored on `TreeType` — it's passed to `draw(x, y)`
- `TreeTypeFactory` is the only creator, keyed on **all** intrinsic state
- the factory is an instance, not a static
- prove loads, bytes and identity with `assert`, not prints

---

## 1. Read — 15 min

`NOTES.md` top to bottom.

## 2. Build — ~40 min

| Step | Build | Must prove |
|---|---|---|
| **problem** | each tree owns a texture | 1000 loads, ~1 MB |
| **solution** | flyweight + factory + context | 3 loads, 3 KB, same 1000 trees |
| **identity** | same kind → same object | `==` on two oaks; `!=` oak vs pine |
| **key** | same name, different color | a separate flyweight |

## 3. Break it on purpose

- Key the factory on `name` only. Plant a green oak and an autumn-red oak. Assert
  what `draw()` returns for the red one. Explain in a comment. Revert.
- Add a `setColor` to `TreeType`. Call it on one tree's type. Count the trees that
  changed colour. Revert.

## 4. Self-check — 10 min

1. Intrinsic vs extrinsic — the question that decides which is which?
2. Why does the course's `BulletType` save no memory?
3. Why must a flyweight be immutable?
4. Why does `Integer.valueOf(100) == Integer.valueOf(100)` hold but not for 1000?
5. Flyweight vs Singleton?

## 5. Commit

```bash
git add DesignPatterns/12-Flyweight && git commit -m "flyweight: <step>"
```

---

## Done when

- [x] Watched the course video, did the course exercise
- [ ] Read `NOTES.md`
- [x] problem — written, runs, 1000 loads measured
- [x] solution — 3 loads, shared by identity
- [x] full intrinsic key asserted
- [ ] broken once (name-only key), reverted
- [ ] All 5 self-check questions answered without looking
- [ ] Committed

Then tick `12 — Flyweight` in `../README.md`.

**Stuck?** Ask me for a nudge on the pattern — not the code.
