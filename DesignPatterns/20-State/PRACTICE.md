# 20-State — Session Plan

Goal is not "swap an object from outside" — the course's transport-mode example does
that, and it's really Strategy. It's **let the states choose the transitions, and make
illegal transitions fail by default**. Different domain on purpose.

---

## Layout

```
20-State/
  state/problem/OrderStatus.java          CREATED, PAID, SHIPPED, DELIVERED, CANCELLED
  state/problem/Order.java                switch in every action, one case forgotten
  state/problem/StateProblemMain.java
  state/solution/OrderState.java          actions return next state, throw by default
  state/solution/Created.java  Paid.java  Shipped.java  Delivered.java  Cancelled.java
  state/solution/Order.java               the context — forwards, no switch
  state/solution/StateSolutionMain.java
```

```bash
javac -Xlint:all -d out $(find . -name '*.java') && java -ea -cp out state.solution.StateSolutionMain
```

## Rules for `solution/`

- no `switch` / `if` on the state anywhere
- no public `setState` — the next state comes back from the current one
- every action throws by default; a state overrides only what's legal from it
- the refund rule lives in the one state where it applies (`Paid`)
- a rejected action leaves the state unchanged
- prove it with `assert`, not prints

---

## 1. Read — 15 min

`NOTES.md` top to bottom.

## 2. Build — ~45 min

| Step | Build | Must prove |
|---|---|---|
| **problem** | enum + 4 switches, `CANCELLED` missing from `deliver()` | happy path works, and the silent bug |
| **solution** | interface + 5 states + context | same scenarios as `problem/`, bug now throws |
| **side effect** | refund only from `Paid` | cancel-after-pay refunds, cancel-before-pay doesn't |

## 3. Break it on purpose

- Add `RETURNED` to `problem/` — count the switches you edit. Then add a `Returned`
  state + `returnItem()` to `solution/` — count again.
- Rewrite `problem/Order.deliver()` as a switch **expression**. Watch the compiler
  catch the missing `CANCELLED`. Note in a comment when that's good enough.

## 4. Self-check — 10 min

1. State vs Strategy — same diagram, what's different?
2. Why is the course's transport example Strategy?
3. Return-next-state vs `ctx.setState` — trade-off?
4. Why are actions illegal by default?
5. When is an enum + switch expression enough?

## 5. Commit

```bash
git add DesignPatterns/20-State && git commit -m "state: <step>"
```

---

## Done when

- [x] Watched the course video, did the course exercise
- [ ] Read `NOTES.md`
- [x] problem — written, runs, silent bug shown
- [x] solution — same scenarios, illegal moves throw
- [x] refund lives in `Paid` only
- [ ] broken once (add RETURNED both ways), compared
- [ ] All 5 self-check questions answered without looking
- [ ] Committed

Then tick `20 — State` in `../README.md`.

**Stuck?** Ask me for a nudge on the pattern — not the code.
