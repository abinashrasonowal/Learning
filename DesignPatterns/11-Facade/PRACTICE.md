# 11-Facade — Session Plan

Goal is not "call three services and join the strings" — the course's gateway does
that, with nothing to order or undo. It's **put the order-sensitive, failure-sensitive
use of a subsystem in one place so callers can't get it wrong**. Different domain on
purpose.

---

## Layout

```
11-Facade/
  facade/problem/Inventory.java  PaymentService.java  ShippingService.java  Notifier.java
  facade/problem/WebCheckout.java        orchestrates correctly
  facade/problem/MobileCheckout.java     copy — forgets to release stock on decline
  facade/problem/FacadeProblemMain.java
  facade/solution/Inventory.java  PaymentService.java  ShippingService.java  Notifier.java
  facade/solution/OrderRequest.java      the inputs, as a record
  facade/solution/OrderFacade.java       reserve → charge → ship → notify, with undo
  facade/solution/FacadeSolutionMain.java
```

```bash
javac -Xlint:all -d out $(find . -name '*.java') && java -ea -cp out facade.solution.FacadeSolutionMain
```

## Rules for `solution/`

- subsystem classes are unchanged from `problem/`
- subsystems are injected into the facade, never `new`-ed inside it
- each failure undoes exactly the steps already done
- no email is sent for a failed order
- the subsystems are still usable directly
- prove stock, refunds and emails with `assert`, not prints

---

## 1. Read — 15 min

`NOTES.md` top to bottom.

## 2. Build — ~45 min

| Step | Build | Must prove |
|---|---|---|
| **problem** | two controllers, one with the missing release | declined mobile order leaks stock |
| **solution** | `OrderFacade` | same success path; declined card leaves stock untouched |
| **shipping fails** | refund + release | refund recorded, stock restored, no email |
| **out of stock** | fails first | nothing charged |

## 3. Break it on purpose

- Add a fraud check (reject amounts over 10 000) before charging. Count the classes
  you edit in `solution/` (one) vs `problem/` (two).
- Make `OrderFacade` construct its own subsystems with `new`. Try to assert on the
  `Notifier` from `main`. Explain in a comment why injection matters. Revert.

## 4. Self-check — 10 min

1. What knowledge does a facade stop callers from duplicating?
2. Why is the course's gateway a weak example?
3. Facade vs Adapter vs Mediator — one line each.
4. Is a facade access control? Why not?
5. What's a god facade and how do you avoid it?

## 5. Commit

```bash
git add DesignPatterns/11-Facade && git commit -m "facade: <step>"
```

---

## Done when

- [x] Watched the course video, did the course exercise
- [ ] Read `NOTES.md`
- [x] problem — written, runs, stock leak shown
- [x] solution — every failure path undone correctly
- [x] subsystems injected, still usable directly
- [ ] broken once (fraud check added), compared
- [ ] All 5 self-check questions answered without looking
- [ ] Committed

Then tick `11 — Facade` in `../README.md`.

**Stuck?** Ask me for a nudge on the pattern — not the code.
