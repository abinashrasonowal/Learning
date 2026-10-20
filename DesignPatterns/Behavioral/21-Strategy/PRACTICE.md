# 21-Strategy — Session Plan

Goal is not "write an interface with three implementations" — your OCP solution
already did that. It's **choose the strategy without an if/else anywhere in business
code, and know when a lambda beats a class**. You write all the code.

---

## Layout

```
21-Strategy/
  strategy/problem/ShippingCalculator.java    cost(String carrier, Parcel p) — if/else on carrier
  strategy/problem/Parcel.java                weightKg, distanceKm
  strategy/problem/StrategyProblemMain.java
  strategy/solution/ShippingStrategy.java     the interface
  strategy/solution/Standard.java  Express.java  SameDay.java
  strategy/solution/Parcel.java
  strategy/solution/ShippingCalculator.java   the context
  strategy/solution/ShippingRegistry.java     Map<String, ShippingStrategy> — selection lives here
  strategy/solution/StrategySolutionMain.java
```

Different domain from the payment example on purpose — copying it teaches nothing.

```bash
javac -Xlint:all -d out $(find . -name '*.java') && java -ea -cp out strategy.solution.StrategySolutionMain
```

## Rules for `solution/`

- no `if`/`switch` on a carrier string **anywhere except** `ShippingRegistry`
  (and ideally not even there — a `Map` lookup is enough)
- context takes the strategy through its **constructor** — no setter, no NPE path
- the interface method takes only `Parcel`. Per-strategy data (express surcharge,
  same-day cutoff hour) goes in the strategy's own constructor and fields
- at least one strategy has real state that changes its result
- unknown carrier → fail loudly with a clear message, not a silent `0.0`
- prove results with `assert`, not prints: `assert calc.cost(p) == 120.0;`

---

## 1. Read — 15 min

`NOTES.md` top to bottom.

## 2. Build — ~45 min

| Step | Build | Must prove |
|---|---|---|
| **problem** | if/else calculator, 3 carriers, driver | it works — and feel the edit needed to add a 4th |
| **solution** | interface, 3 strategies, context, registry | same numbers as `problem/` for the same parcels (assert them) |
| **lambda** | add a 4th carrier `"Pickup"` (cost 0) as a **lambda** registered in the map | zero new files, zero edits to `ShippingCalculator` |
| **runtime swap** | a `ShippingCalculator` per parcel chosen from user input string | the only place the string is read is the registry |

## 3. Break it on purpose

- Add a `String trackingCode` parameter to the interface method because `Express`
  needs it. Watch every strategy — and the lambda — change. Undo it; move the data
  into `Express`'s constructor instead.
- Write `if (strategy instanceof Express)` inside the context. Explain in a comment
  why this is the original problem coming back.

## 4. Self-check — 10 min

Close everything. Answer in your own words:

1. Where did the if/else go? Why is that better than where it was?
2. Strategy vs State — same diagram, what's different?
3. When is a lambda the wrong choice for a strategy?
4. Name a strategy in the JDK.
5. Why constructor injection over the setter in the reference example?

## 5. Commit

```bash
git add DesignPatterns/21-Strategy && git commit -m "strategy: <step>"
```

---

## Done when

- [ ] Read `NOTES.md`
- [x] problem — written, runs
- [x] solution — written, runs, asserts match problem's numbers
- [x] lambda carrier added with zero edits to the context
- [ ] broken once (leaky interface), reverted
- [ ] All 5 self-check questions answered without looking
- [ ] Committed

Then tick `21 — Strategy` in `../README.md`.

**Stuck?** Ask me for a nudge on the pattern — not the code.
