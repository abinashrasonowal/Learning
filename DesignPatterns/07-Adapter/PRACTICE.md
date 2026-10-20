# 07-Adapter — Session Plan

Goal is not "rename one method" — the course's SendGrid adapter does only that. It's
**keep a vendor's dialect (types, scale, error codes, missing features) out of your
code entirely**. Different domain on purpose.

---

## Layout

```
07-Adapter/
  adapter/problem/FxVendorClient.java       vendor SDK: "USDINR", ×10 000 longs, -1 = unknown
  adapter/problem/PriceConverter.java       calls the vendor directly — negative price bug
  adapter/problem/AdapterProblemMain.java
  adapter/solution/FxVendorClient.java      unchanged
  adapter/solution/Currency.java            our type
  adapter/solution/ExchangeRates.java       the target interface
  adapter/solution/FxVendorAdapter.java     translates everything
  adapter/solution/PriceConverter.java      depends on ExchangeRates only
  adapter/solution/AdapterSolutionMain.java
```

```bash
javac -Xlint:all -d out $(find . -name '*.java') && java -ea -cp out adapter.solution.AdapterSolutionMain
```

## Rules for `solution/`

- `FxVendorClient` is not edited (pretend it's a jar)
- nothing outside the adapter sees a pair string, the 10 000 scale or `-1`
- the adapter fills the vendor's gaps (inverse and cross rates)
- `PriceConverter` can run against a lambda `ExchangeRates` with no vendor
- prove it with `assert`, not prints

---

## 1. Read — 15 min

`NOTES.md` top to bottom.

## 2. Build — ~40 min

| Step | Build | Must prove |
|---|---|---|
| **problem** | converter calling the vendor | USD→INR right, INR→USD negative |
| **solution** | target, adapter, converter | same USD→INR; INR→USD = 10; INR→EUR cross rate |
| **errors** | unquoted currency | exception, not a price |
| **seam** | lambda `ExchangeRates` | converter works with no vendor |

## 3. Break it on purpose

- Make `rate()` return the vendor's raw `long`. Follow the change into
  `PriceConverter`. Explain in a comment what leaked. Revert.
- Write a second adapter for an imaginary vendor that returns `"83.25"` strings.
  Notice `PriceConverter` doesn't change.

## 4. Self-check — 10 min

1. Five kinds of mismatch an adapter can translate.
2. Object adapter vs class adapter — which, and why, in Java?
3. Adapter vs Facade vs Decorator — one line each.
4. Name an adapter in `java.io`.
5. When is an adapter the wrong tool?

## 5. Commit

```bash
git add DesignPatterns/07-Adapter && git commit -m "adapter: <step>"
```

---

## Done when

- [x] Watched the course video, did the course exercise
- [ ] Read `NOTES.md`
- [x] problem — written, runs, negative price shown
- [x] solution — vendor untouched, all translation in the adapter
- [x] errors + lambda seam asserted
- [ ] broken once (leaky return type), reverted
- [ ] All 5 self-check questions answered without looking
- [ ] Committed

Then tick `07 — Adapter` in `../README.md`.

**Stuck?** Ask me for a nudge on the pattern — not the code.
