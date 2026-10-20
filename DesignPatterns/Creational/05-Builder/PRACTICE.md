# 05-Builder — Session Plan

Goal is not "chain setters and call `build()`" — the course's `House` does that. It's
**make the swapped-argument bug impossible, validate across fields in `build()`, and
keep the result truly immutable**. Different domain on purpose.

---

## Layout

```
05-Builder/
  builder/problem/HttpRequest.java          telescoping constructors, map stored as given
  builder/problem/BuilderProblemMain.java   swapped ints, leaked header, GET with body
  builder/solution/HttpRequest.java         immutable + static nested Builder + toBuilder()
  builder/solution/BuilderSolutionMain.java
```

```bash
javac -Xlint:all -d out $(find . -name '*.java') && java -ea -cp out builder.solution.BuilderSolutionMain
```

## Rules for `solution/`

- `HttpRequest` constructor is private; fields `final`
- required params (method, url) in `builder(...)`, not setters
- defaults live in the builder, in one place
- `build()` checks cross-field rules (GET/DELETE with body) and ranges
- headers defensively copied and read-only
- changing the builder after `build()` doesn't affect the built request
- prove it with `assert`, not prints

---

## 1. Read — 15 min

`NOTES.md` top to bottom.

## 2. Build — ~45 min

| Step | Build | Must prove |
|---|---|---|
| **problem** | 3 telescoping constructors | swapped ints compile; caller's map leaks in; GET body accepted |
| **solution** | private ctor, nested `Builder`, `build()` validation | named args; each bad input rejected |
| **immutability** | defensive copy, read-only headers | builder change after `build()` doesn't leak |
| **toBuilder** | variant from an existing request | original unchanged |

## 3. Break it on purpose

- Replace the defensive copy with `this.headers = builder.headers`. Run the
  leak assert. Explain in a comment. Revert.
- Move the "GET has no body" check into `body(...)`. Then call `.body("{}")` on a
  POST builder and switch… you can't switch the method. Explain why cross-field
  checks belong in `build()`.

## 4. Self-check — 10 min

1. Two smells Builder replaces — name both.
2. Why do required params go in `builder(...)`?
3. What can `build()` check that a setter can't?
4. What breaks without the defensive copy?
5. GoF Builder vs Bloch Builder — what's the Director?

## 5. Commit

```bash
git add DesignPatterns/05-Builder && git commit -m "builder: <step>"
```

---

## Done when

- [x] Watched the course video, did the course exercise
- [ ] Read `NOTES.md`
- [x] problem — written, runs, three bugs shown
- [x] solution — named, validated, immutable
- [x] `toBuilder()` variant, original unchanged
- [ ] broken once (no defensive copy), reverted
- [ ] All 5 self-check questions answered without looking
- [ ] Committed

Then tick `05 — Builder` in `../README.md`.

**Stuck?** Ask me for a nudge on the pattern — not the code.
