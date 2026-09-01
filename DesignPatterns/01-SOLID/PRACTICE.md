# 01-SOLID — Today's Session Plan

Target: ~2h30m. Goal is not "read SOLID", it's **be able to spot the smell in
unfamiliar code and name the fix without looking**.

---

## 1. Read — 25 min

Read `NOTES.md` top to bottom once. Don't take notes yet, just read.

## 2. Run and break — 40 min

For each of SRP, OCP, LSP, ISP, DIP (~8 min each):

```
java -ea SRP.java     # then OCP, LSP, ISP, DIP
```

Read the BAD half. Say out loud what breaks. Read the GOOD half. Then **break it
on purpose** — the change is the lesson:

| File | Do this | Notice |
|---|---|---|
| `SRP.java` | Add a `printToPdf()` method. Which class? | Where it *doesn't* go is the point |
| `OCP.java` | Add a `Crypto` payment method to both halves | Count files edited: bad=1 edit, good=1 add |
| `LSP.java` | Try calling `write()` on `ReadOnlyFile` in the GOOD half | It won't compile — that's the win |
| `ISP.java` | Add `fax()` to `Machine`, then to the role interfaces | Bad half: every implementor breaks |
| `DIP.java` | Write a second test double asserting two sends | Impossible in the bad half — no seam |

## 3. Exercise — 50 min

`Exercise.java` is one god class violating all five. Refactor it yourself into
`Solution.java` **without looking at the other files.** Rules:

- No `new` of a concrete dependency inside a class that has business logic
- No method that throws `UnsupportedOperationException`
- No `switch`/`if-else` chain on a type string
- `main` must still print the same shipping/notification lines

Run `java -ea Exercise.java` first to see current behaviour, then keep
`java -ea Solution.java` matching it as you refactor.

## 4. Self-check — 20 min

Close everything. Answer in your own words, out loud or in a scratch file:

1. SRP says "one reason to change" — reason according to *whom*?
2. What does OCP cost you if you pick the wrong axis of variation?
3. Why is `Square extends Rectangle` an LSP violation? Which method proves it?
4. How does an ISP violation *cause* an LSP violation?
5. DIP vs Dependency Injection — what's the difference?
6. Which two principles are about cohesion, which three about coupling?

Anything you can't answer → reread that section of `NOTES.md`, then move on.

## 5. Commit — 5 min

```
git add DesignPatterns/01-SOLID && git commit -m "solid: exercise solution"
```

---

## Done when

- [ ] Read `NOTES.md`
- [ ] Ran + deliberately broke all 5 examples
- [ ] `Solution.java` written from scratch, runs, no stubs/throws/type-switches
- [ ] All 6 self-check questions answered without looking
- [ ] Committed

Then tick `01 — SOLID Principles` in `../README.md` and start 02-Singleton.
