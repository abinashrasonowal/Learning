# Composite

> Build trees out of objects, and let clients treat one item and a whole group of
> items the same way.

A `Bundle` contains products *or other bundles*, and `bundle.price()` works exactly
like `product.price()`. The client never asks "is this one or many?".

---

## The shape

```java
interface Item { int price(); }                                  // component
record Product(String name, int price) implements Item { }        // leaf
class Bundle implements Item {                                    // composite
    private final List<Item> children = new ArrayList<>();
    Bundle add(Item i) { children.add(i); return this; }
    public int price() { int s = 0; for (Item c : children) s += c.price(); return discounted(s); }
}
```

| Role | Job | Knows about |
|---|---|---|
| **Component** | operations every node supports | nothing |
| **Leaf** | answers directly | its own data |
| **Composite** | asks its children, combines, applies its own rule | children as `Item`s only |
| **Client** | calls the operation on the root | `Item` |

---

## Smell it replaces

```java
for (Object item : items) {
    if (item instanceof Product p) ... else if (item instanceof Bundle b) ...
}
```

- an `instanceof` chain in **every** operation (total, count, describe)
- a new kind (`GiftCard`) compiles, goes in the list, and falls through **silently**
- a group can't contain a group, because the group's type lists its children's
  concrete type (`List<Product>`)

---

## The reference example: file system

Classic and correct: `Folder` holds `FileSystemComponent`s, so folders nest. But its
only operation is `showDetails()`, which prints — no value comes back up the tree, and
there's no indentation, so you can't see the nesting in the output. Composite earns
its keep with an operation that **aggregates**: total size, total price, file count.
The recursion is where the pattern lives.

---

## Safe vs transparent

Where does `add()` go?

| | `add()` on | Pro | Con |
|---|---|---|---|
| **Safe** (here) | composite only | can't add to a leaf — compile error | client must know it holds a `Bundle` to add |
| **Transparent** (GoF) | component | client treats every node identically | leaves must throw on `add()` — runtime error |

Java code usually goes safe. The course example is safe too (`addComponent` on
`Folder` only).

---

## Composite operations

- **aggregate up** — `price()` sums children, then the bundle applies its discount:
  nested discounts fall out naturally
- **push down** — `describe(out, indent)` passes context (indent) to children
- **a new operation** = one method on `Item` + one per class; no `instanceof` anywhere.
  Lots of new operations on a stable tree? That's Visitor (pattern 23).

---

## In the JDK

- `java.awt.Container` extends `Component` — a panel holds buttons *or other panels*
- `javax.swing.JComponent` trees, the DOM's `Node`
- `java.nio.file` — `Files.walk(dir)` treats files and directories uniformly

---

## Composite vs neighbours

| | Difference |
|---|---|
| **Decorator** | also wraps an object of the same interface — but **one** child, to add behaviour |
| **Iterator** | how you *walk* a composite (DFS/BFS iterator) |
| **Visitor** | adds operations to a composite tree without editing node classes |

---

## Pitfalls

- **Cycles** — `bundle.add(bundle)` → `price()` recurses forever. Guard if trees can be
  user-built.
- **Leaf-only methods on the component** — `Item.sku()` that bundles can't answer.
  Keep the component to what *every* node can do.
- **Exposing children** — `getChildren()` returning the live list lets callers bypass
  the composite's rules.

---

## Cheat sheet

- Trigger: `instanceof` chains over "single vs group", or groups that can't nest
- Fix: a component interface, leaves answer directly, composites recurse over children
- Interview line: *"Composite lets clients treat individual objects and compositions
  uniformly — part-whole trees. AWT's `Container` is a `Component` that holds
  `Component`s."*
