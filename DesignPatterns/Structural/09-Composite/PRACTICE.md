# 09-Composite — Session Plan

Goal is not "a folder that prints its files" — the course example does that, with no
value coming back up the tree. It's **an aggregating operation that recurses, nested
groups with their own rules, and no `instanceof` anywhere**. Different domain on
purpose.

---

## Layout

```
09-Composite/
  composite/problem/Product.java  Bundle.java  GiftCard.java
  composite/problem/Cart.java                 List<Object> + instanceof chain
  composite/problem/CompositeProblemMain.java gift card silently ignored
  composite/solution/Item.java                the component
  composite/solution/Product.java  GiftCard.java   leaves
  composite/solution/Bundle.java              composite: children + discount
  composite/solution/CompositeSolutionMain.java
```

```bash
javac -Xlint:all -d out $(find . -name '*.java') && java -ea -cp out composite.solution.CompositeSolutionMain
```

## Rules for `solution/`

- no `instanceof` anywhere
- the cart is itself a `Bundle` (0% discount) — no separate `Cart` class
- `Bundle` holds `Item`s, so bundles nest; discounts apply at each level
- `add()` only on `Bundle` (safe composite)
- `describe()` shows the nesting with indentation; assert the exact text
- prove it with `assert`, not prints

---

## 1. Read — 15 min

`NOTES.md` top to bottom.

## 2. Build — ~45 min

| Step | Build | Must prove |
|---|---|---|
| **problem** | cart with `instanceof` total | right total — then a gift card ignored |
| **solution** | `Item`, leaves, `Bundle` | same total; gift card counted |
| **nesting** | bundle inside bundle | nested discounts in the total |
| **new operation** | `productCount()` | one method per class, no type checks |

## 3. Break it on purpose

- Move `add()` onto `Item` (transparent). Make `Product.add()` throw. Write the
  call that now compiles but fails. Explain the trade-off in a comment. Revert.
- `cart.add(cart)` then `cart.price()`. Read the stack trace. Add a guard.

## 4. Self-check — 10 min

1. What does the client never have to ask?
2. Safe vs transparent — where does `add()` go, and what does each cost?
3. Why is `showDetails()` (print only) a weak demo of Composite?
4. Composite vs Decorator — both wrap the same interface; difference?
5. Name a composite in AWT/Swing.

## 5. Commit

```bash
git add DesignPatterns/09-Composite && git commit -m "composite: <step>"
```

---

## Done when

- [x] Watched the course video, did the course exercise
- [ ] Read `NOTES.md`
- [x] problem — written, runs, silent fall-through shown
- [x] solution — same total, gift card counted, no `instanceof`
- [x] nested bundles + `productCount()` asserted
- [ ] broken once (transparent `add()`), reverted
- [ ] All 5 self-check questions answered without looking
- [ ] Committed

Then tick `09 — Composite` in `../README.md`.

**Stuck?** Ask me for a nudge on the pattern — not the code.
