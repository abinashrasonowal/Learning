# 19-Observer — Session Plan

Goal is not "loop over a list and call `update`". It's **make the subject survive
observers that misbehave** — unsubscribe mid-notify, throw, or subscribe twice — and
pick push vs pull deliberately. You write all the code.

---

## Layout

```
19-Observer/
  observer/problem/StockTicker.java          holds PriceBoard + AlertService + Logger fields, calls each
  observer/problem/PriceBoard.java  AlertService.java  TradeLogger.java
  observer/problem/ObserverProblemMain.java
  observer/solution/PriceEvent.java          record(String symbol, double oldPrice, double newPrice)
  observer/solution/PriceListener.java       the observer interface
  observer/solution/StockTicker.java         the subject
  observer/solution/PriceBoard.java          shows latest price per symbol (state: a Map)
  observer/solution/PriceAlert.java          fires once when price crosses a threshold, then detaches itself
  observer/solution/TradeLogger.java         records every event in a List (your test double)
  observer/solution/ObserverSolutionMain.java
```

```bash
javac -Xlint:all -d out $(find . -name '*.java') && java -ea -cp out observer.solution.ObserverSolutionMain
```

## Rules for `solution/`

- `StockTicker` has **no field of a concrete listener type** — only the interface
- push an immutable `PriceEvent` record, not a bare `double`
- notify only when the price actually changed
- `PriceAlert` detaches **itself** from inside its own callback — and nothing crashes
- attaching the same listener twice does **not** double-notify
- one listener throwing does **not** stop the others from being notified
- prove with `assert` on `TradeLogger`'s recorded list, not by eyeballing prints

---

## 1. Read — 15 min

`NOTES.md`, especially "The bugs nobody shows in the demo".

## 2. Build — ~60 min

| Step | Build | Must prove |
|---|---|---|
| **problem** | ticker calling three concrete classes | works — now add a 4th listener and count the edits to `StockTicker` |
| **solution, naive** | `ArrayList`, plain for-each | basic notify works; `assert logger.events().size() == 3` |
| **self-detach** | add `PriceAlert` that detaches in `update()` | **first watch it throw** `ConcurrentModificationException`. Then fix. |
| **duplicates** | attach `logger` twice | assert it recorded each event once |
| **throwing listener** | a lambda listener that always throws | the logger after it still receives the event |
| **subscription** | `attach` returns `AutoCloseable`; use try-with-resources | listener stops getting events after the block |

## 3. Break it on purpose

- Change `PriceEvent` back to `update(double price)`, then add a `volume` field.
  Count how many files you had to touch. Revert.
- Make `TradeLogger.update` call `ticker.setPrice(...)`. Watch the recursion. Write a
  one-line comment on how you'd guard it.

## 4. Self-check — 10 min

1. Push vs pull — which did you pick and what does it cost you?
2. Why does detaching inside `update()` crash with a plain `ArrayList`? Two fixes?
3. What's a lapsed listener and how does your `AutoCloseable` prevent it?
4. Observer vs Mediator in one sentence.
5. Why was `java.util.Observable` deprecated?

## 5. Commit

```bash
git add DesignPatterns/19-Observer && git commit -m "observer: <step>"
```

---

## Done when

- [ ] Read `NOTES.md`
- [x] problem — written, runs
- [x] solution — naive version runs
- [x] self-detach — saw the CME, then fixed it
- [x] duplicate attach + throwing listener handled, asserted
- [x] subscription via `AutoCloseable`
- [ ] All 5 self-check questions answered without looking
- [ ] Committed

Then tick `19 — Observer` in `../README.md`.

**Stuck?** Ask me for a nudge on the pattern — not the code.
