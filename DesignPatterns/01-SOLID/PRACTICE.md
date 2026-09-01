# 01-SOLID — Today's Session Plan

Target: ~2h30m. Goal is not "read SOLID", it's **be able to spot the smell in
unfamiliar code and name the fix without looking**.

You write all the code. Nothing here is solved for you.

---

## 1. Read — 25 min

Read `NOTES.md` top to bottom once. Don't take notes yet, just read.

## 2. Write the five examples — 60 min (~12 min each)

One file per principle: `SRP.java`, `OCP.java`, `LSP.java`, `ISP.java`, `DIP.java`.
No packages, no Maven. Each file: `public class <Name>` first with `main`, then the
supporting classes below it. Run with `java -ea SRP.java`.

Same shape every time — **the bad version and the good version in the same file**,
`main` runs bad first, then good, so the file tells the whole story on its own:

```java
public class XXX {
    public static void main(String[] args) {
        System.out.println("--- BAD ---");
        // ... show the violation. If it throws, catch and print it.
        System.out.println("--- GOOD ---");
        // ... same behaviour, no violation.
    }
}
// ---------- BAD ----------
// ---------- GOOD ----------
```

Domains to use (pick your own if you'd rather — the domain doesn't matter, the
shape does):

| File | Bad version | Good version must |
|---|---|---|
| `SRP.java` | `Invoice` that calculates the total, saves to DB *and* sends email | split so three different actors edit three different classes |
| `OCP.java` | `PaymentProcessor.processPayment(String method, double amt)` with an if/else chain | let you add a 4th payment method as a **new file only** — prove it by adding one |
| `LSP.java` | `ReadOnlyFile extends File`, `write()` throws | make `readOnly.write()` fail to **compile**, not at runtime |
| `ISP.java` | `SimplePrinter implements Machine{print,scan,copy}`, two methods throw | split into role interfaces; no stub throws anywhere |
| `DIP.java` | `NotificationService` doing `new EmailService()` in its constructor | inject an interface — then write a recording test double and `assert` on it |

Rules for every good half:

- no method throws `UnsupportedOperationException`
- no `if`/`switch` on a type string
- no `new` of a concrete dependency inside a class holding business logic

**After each file, break it on purpose** — that's where the lesson lands:

- `SRP.java` — add `printToPdf()`. Which class? Where it *doesn't* go is the point.
- `OCP.java` — add a `Crypto` method to both halves. Count files edited: bad = edit a tested class, good = add one.
- `LSP.java` — call `write()` on the read-only type in the good half. Compiler stops you.
- `ISP.java` — add `fax()` to the fat interface. Every implementor breaks. Now add it to the role interfaces instead.
- `DIP.java` — write a second test double asserting two sends. Impossible in the bad half — no seam.

## 3. Exercise — 45 min

`Exercise.java` is one god class violating all five. **Before reading the hints at
the bottom of that file**, list the five violations yourself. Then refactor into
`Solution.java` from scratch.

Run `java -ea Exercise.java` first to see current behaviour; keep
`java -ea Solution.java` printing the same order totals as you go.
Same three rules as step 2.

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
git add DesignPatterns/01-SOLID && git commit -m "solid: worked through the five principles"
```

---

## Done when

- [ ] Read `NOTES.md`
- [ ] `SRP.java` written, runs, deliberately broken once
- [ ] `OCP.java` written, runs, deliberately broken once
- [ ] `LSP.java` written, runs, deliberately broken once
- [ ] `ISP.java` written, runs, deliberately broken once
- [ ] `DIP.java` written, runs, deliberately broken once
- [ ] Listed all 5 violations in `Exercise.java` before reading its hints
- [ ] `Solution.java` written from scratch, runs, no stubs / throws / type-switches
- [ ] All 6 self-check questions answered without looking
- [ ] Committed

Then tick `01 — SOLID Principles` in `../README.md` and start 02-Singleton.

**Stuck on one?** Ask me for a nudge on that principle — not the file.
