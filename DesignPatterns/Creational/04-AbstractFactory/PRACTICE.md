# 04-AbstractFactory — Session Plan

Goal is not "two interfaces and two factories" — the course's UI example does that.
It's **make a mismatched family impossible to build, not just unlikely**. Different
domain from Windows/Mac on purpose.

---

## Layout

```
04-AbstractFactory/
  abstractfactory/problem/StripeCharger.java  StripeRefunder.java
  abstractfactory/problem/RazorpayCharger.java  RazorpayRefunder.java
  abstractfactory/problem/Checkout.java                   Stripe charger + Razorpay refunder — mismatch
  abstractfactory/problem/AbstractFactoryProblemMain.java
  abstractfactory/solution/Charger.java  Refunder.java     abstract products
  abstractfactory/solution/PaymentProvider.java           the abstract factory
  abstractfactory/solution/StripeProvider.java  RazorpayProvider.java
  abstractfactory/solution/Stripe*.java  Razorpay*.java   package-private products
  abstractfactory/solution/Checkout.java                  takes a PaymentProvider only
  abstractfactory/solution/AbstractFactorySolutionMain.java
```

```bash
javac -Xlint:all -d out $(find . -name '*.java') && java -ea -cp out abstractfactory.solution.AbstractFactorySolutionMain
```

## Rules for `solution/`

- `Checkout` takes a `PaymentProvider`, never a `Charger` + `Refunder` separately
- concrete products are package-private
- switching provider is one argument at the edge
- a 3rd family can be added without touching `Checkout`
- prove it with `assert`, not prints

---

## 1. Read — 15 min

`NOTES.md` top to bottom.

## 2. Build — ~45 min

| Step | Build | Must prove |
|---|---|---|
| **problem** | hand-picked concrete charger + refunder | the mismatch fails at runtime |
| **solution** | products, provider interface, two providers, checkout | charge → refund works for both families |
| **switch** | provider chosen from a string in `main` | `Checkout` never names a provider |
| **3rd family** | a fake provider from lambdas | zero edits to `Checkout` |

## 3. Break it on purpose

- Add `Checkout(Charger, Refunder)` as a second constructor. Build the mismatched pair
  through it. Explain in a comment why the pattern is only as strong as the
  constructor. Remove it.
- Add a third product, `Payouts`, to `PaymentProvider`. Count the classes you edit.
  That's the pattern's rigid axis — write it down.

## 4. Self-check — 10 min

1. Factory vs Abstract Factory — one line each.
2. Why isn't "add interfaces" enough to stop mixing?
3. Which is easy to add — a family or a product? Why?
4. Name the abstract factory in JDBC.
5. Why make the concrete products package-private?

## 5. Commit

```bash
git add DesignPatterns/04-AbstractFactory && git commit -m "abstract factory: <step>"
```

---

## Done when

- [x] Watched the course video, did the course exercise
- [ ] Read `NOTES.md`
- [x] problem — written, runs, mismatch shown
- [x] solution — both families work end to end
- [x] 3rd family added with no edits to `Checkout`
- [ ] broken once (second constructor), removed
- [ ] All 5 self-check questions answered without looking
- [ ] Committed

Then tick `04 — Abstract Factory` in `../README.md`.

**Stuck?** Ask me for a nudge on the pattern — not the code.
