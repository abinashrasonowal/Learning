# 01-SOLID — Session Plan

Goal is not "read SOLID", it's **spot the smell in unfamiliar code and name the fix
without looking**. You write all the code. Nothing here is solved for you.

---

## Layout

One folder per principle, violation and fix in sibling packages:

```
01-SOLID/
  SRP/Violation/Invoice.java          package SRP.Violation;
  SRP/Correct/Invoice.java            package SRP.Correct;
  SRP/Correct/InvoiceRepository.java
  SRP/Correct/EmailService.java
  SRP/Correct/Main.java               <- the driver lives here, not in Invoice
  OCP/Violation/...  OCP/Correct/...
  ...
```

Package name must match the folder path. Two ways to run:

```bash
# single file, quick — works even with a package declaration
java SRP/Violation/Invoice.java

# multi-class Correct/ package — compile all, then run the driver
javac -d out $(find . -name '*.java') && java -ea -cp out SRP.Correct.Main
```

Add `out/` to `.gitignore` if it isn't already.

> **Naming decision to make now, not on topic 24:** `Violation/` is a good name;
> `Correct/` is the weak half — it implies one right answer, which is the exact
> mindset that makes you over-split. `Problem/`+`Solution/` (matches the reference
> repo for the patterns) or `Bad/`+`Good/` (matches it for SOLID) are both better
> pairs. Whatever you pick, use it for all 24 topics.

## Rules for every `Correct/` package

- no method throws `UnsupportedOperationException`
- no `if`/`switch` on a type string
- no `new` of a concrete dependency inside a class holding business logic
- classes have **state and a constructor** — not a pile of `static` methods.
  A class with no fields isn't demonstrating anything about responsibility.
- ask of every method you *keep*: does this change for the same reason as the
  others? Over-splitting is as wrong as under-splitting.

---

## 1. Read — 25 min

`NOTES.md` top to bottom, once. Don't take notes yet, just read.

## 2. Write the five — ~15 min each

| Principle | Violation to build | `Correct/` must prove |
|---|---|---|
| **SRP** ✅ | `Invoice` that prices, saves to DB *and* emails | three actors → three classes; pricing **stays** in Invoice |
| **OCP** | `PaymentProcessor.process(String method, double amt)` with an if/else chain | adding a 4th payment method is a **new file only** — prove it by adding one |
| **LSP** | `ReadOnlyFile extends File`, `write()` throws | `readOnly.write()` fails to **compile**, not at runtime |
| **ISP** | `SimplePrinter implements Machine{print,scan,copy}`, two throw | role interfaces; zero stub throws |
| **DIP** | `NotificationService` doing `new EmailService()` in its constructor | inject an interface, then write a recording test double and `assert` on it |

**After each one, break it on purpose** — that's where the lesson lands:

- **SRP** — add `printToPdf()`. Which class? Where it *doesn't* go is the point.
- **OCP** — add a `Crypto` method to both packages. Count edits: Violation = edit a
  working class, Correct = add one file, touch nothing.
- **LSP** — call `write()` on the read-only type in `Correct/`. Compiler stops you.
- **ISP** — add `fax()` to the fat interface; every implementor breaks. Now add it to
  the role interfaces instead — only the machines that fax change.
- **DIP** — write a second test double asserting two sends. Impossible in the
  Violation package: no seam to inject through.

## 3. Self-check — 20 min

Close everything. Answer in your own words:

1. SRP says "one reason to change" — reason according to *whom*?
2. What does OCP cost you if you pick the wrong axis of variation?
3. Why is `Square extends Rectangle` an LSP violation? Which method proves it?
4. How does an ISP violation *cause* an LSP violation?
5. DIP vs Dependency Injection — what's the difference?
6. Which two principles are about cohesion, which three about coupling?

Can't answer one → reread that section of `NOTES.md`, then move on.

## 4. Commit

```bash
git add DesignPatterns/01-SOLID && git commit -m "solid: <principle>"
```

One commit per principle beats one big one — the diff is your revision material.

---

## Done when

- [x] Read `NOTES.md`
- [ ] SRP — `Violation/` done ✅, `Correct/` filled in, driver runs, broken once
- [ ] OCP — written, runs, broken once
- [ ] LSP — written, runs, broken once
- [ ] ISP — written, runs, broken once
- [ ] DIP — written, runs, broken once
- [ ] All 6 self-check questions answered without looking
- [ ] Folder naming decided and applied consistently
- [ ] Committed

Then tick `01 — SOLID Principles` in `../README.md` and start 02-Singleton.

**Stuck?** Ask me for a nudge on the principle — not the code.
