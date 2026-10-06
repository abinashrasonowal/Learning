# 02-Singleton — Session Plan

Goal is not "write `getInstance()`" — anyone can. It's to **break your own singleton
three ways and know which fix each break demands**. You write all the code.

---

## Layout

Unlike 01-SOLID there's no problem/solution split — the whole point is a *ladder* of
implementations, each fixing the one below:

```
02-Singleton/
  naive/Counter.java              lazy, unsynchronised — the bug
  naive/RaceMain.java             driver that PROVES two instances exist
  synced/Counter.java             synchronized getInstance()
  dcl/Counter.java                double-checked + volatile
  holder/Counter.java             static nested holder — the default
  enumsingleton/Counter.java      enum INSTANCE
  attack/ReflectionAttack.java    breaks naive/synced/dcl/holder, fails on enum
  attack/SerializationAttack.java same, then fixed with readResolve
  SingletonMain.java              (package-less driver is fine here, or pick a package)
```

Package name must match the folder path.

```bash
javac -Xlint:all -d out $(find . -name '*.java') && java -ea -cp out naive.RaceMain
```

## Rules

- every `Counter` holds **real state** — an `int count` bumped by `increment()`.
  A singleton with no state proves nothing; you can't tell two instances apart.
- prove identity with `System.identityHashCode(...)` or `==`, never with a print
  that says "created" — a lying log is how people convince themselves it worked.
- no `Thread.sleep` as synchronisation. Use `CountDownLatch` to start threads together
  and `ExecutorService` + `Future` to collect results.
- no `resetForTests()`. If you want one, note *why* in a comment — that's the lesson.
- `assert`, don't eyeball. Run with `java -ea`.

---

## 1. Read — 20 min

`NOTES.md` once, top to bottom.

## 2. Build the ladder — ~15 min each

| Step | Build | Must prove |
|---|---|---|
| **naive** | lazy `if (instance == null)` | a **race that actually fires**: N threads → more than one distinct instance. Slow the constructor to widen the window. |
| **synced** | `synchronized getInstance()` | same race now yields exactly 1. Then say what it costs. |
| **dcl** | double-checked + `volatile` | write down what `volatile` prevents — reordering, not staleness. Delete it and note that the test still passes (that's the scary part). |
| **holder** | static nested `Holder` | it's lazy: prove `Counter` isn't initialised until first `getInstance()` (a `println` in a static initialiser shows the order). |
| **enum** | `enum Counter { INSTANCE; }` | survives both attacks below with zero defensive code. |

**Widening the race window:** a constructor doing nothing is atomic in practice.
Give it real work — a loop, or build a small array — so the two threads overlap.
If your race won't fire, the bug isn't gone, your test is too fast.

## 3. Break it on purpose — the actual lesson

- **Reflection** — `Constructor<Counter> c = Counter.class.getDeclaredConstructor();
  c.setAccessible(true);` then `assert c.newInstance() != Counter.getInstance()`.
  Run it against `holder` (breaks) and `enumsingleton` (throws — read the message).
- **Serialization** — make it `Serializable`, write to a `ByteArrayOutputStream`, read
  back, `assert deserialized == Counter.getInstance()`. Watch it fail. Add
  `readResolve()`. Watch it pass. Run the same test on the enum unmodified.
- **Constructor guard** — defend the reflection attack with a flag in the private
  constructor, then explain in one line why that's still weaker than the enum.

## 4. Self-check — 20 min

Close everything:

1. Why is the holder idiom lazy *and* thread-safe with no `synchronized` in your code?
2. DCL without `volatile` — describe the exact failure a caller sees.
3. Which two attacks does `enum` defeat for free, and how?
4. Singleton is which SOLID violation, and why does it make tests order-dependent?
5. "One instance" vs "a class that enforces one instance" — when do you want which?
6. Spring beans are singletons by default. Why is that *not* this pattern's problem?

## 5. Commit

```bash
git add DesignPatterns/02-Singleton && git commit -m "singleton: <step>"
```

---

## Done when

- [ ] Read `NOTES.md`
- [ ] naive — race fires, more than one instance, asserted
- [ ] synced — same race yields 1
- [ ] dcl — written with `volatile`, cost of removing it understood
- [ ] holder — laziness proven by init order
- [ ] enum — survives both attacks
- [ ] reflection attack written, breaks holder, refused by enum
- [ ] serialization attack written, fixed with `readResolve`
- [ ] All 6 self-check questions answered without looking
- [ ] Committed

Then tick `02 — Singleton` in `../README.md` and start 03-Factory.

**Stuck?** Ask me for a nudge on the pattern — not the code. If your race won't
reproduce, ask about that specifically; it's the one step where the environment
fights you.
