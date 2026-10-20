# Flyweight

> When you have huge numbers of similar objects, pull the state they all share into a
> small set of shared, immutable objects — and keep only what differs per object.

A memory optimisation, not a design-clarity one. Use it when a profiler says so.

---

## The shape

```java
final class TreeType { name, color, byte[] texture;  String draw(int x, int y) }   // flyweight — shared
record Tree(int x, int y, TreeType type) { }                                       // context — per object
class TreeTypeFactory { TreeType get(String name, String color) { computeIfAbsent... } }
```

| Role | Holds | Shared? |
|---|---|---|
| **Flyweight** (`TreeType`) | **intrinsic** state — same for every tree of a kind | yes, by many contexts |
| **Context** (`Tree`) | **extrinsic** state — position — plus a flyweight reference | no |
| **Factory** | a map from key → flyweight; the only creator | — |

The rule that makes it work: **extrinsic state is passed in** (`type.draw(x, y)`), never
stored on the flyweight.

---

## Smell it replaces

1000 trees, 3 kinds, each tree holding its own 1 KB texture → 1000 loads, ~1 MB. With
flyweights: 3 loads, 3 KB, and each tree is two ints and a reference.

---

## Intrinsic vs extrinsic — how to split

Ask of each field: **is it the same for every object of this kind?**

| Field | Same for every oak? | Goes on |
|---|---|---|
| name, color, texture | yes | `TreeType` (flyweight) |
| x, y | no | `Tree` (context) |

The factory's key is **all** the intrinsic state — `(name, color)`, not just `name`,
or an autumn-red oak would get the green oak's flyweight.

---

## The reference example: bullets

Good framing (bullets share appearance, differ in position/velocity), three things to
fix when you rewrite it:

- `BulletType` holds only a `String color`. A reference to a `BulletType` costs the
  same as a reference to the `String` — **nothing is saved**. The intrinsic state has to
  be *heavy* (a sprite, a mesh, a font glyph) for the pattern to pay.
- the "5 Green bullets" loop passes `"Red"` — so the demo only ever creates one type.
- the factory is a `static HashMap` — global, and not thread-safe. An instance factory
  (injected) is testable; use `ConcurrentHashMap.computeIfAbsent` if shared across threads.

---

## Flyweights must be immutable

One `TreeType` is shared by hundreds of trees. If anything can change it, every tree of
that kind changes. `final` class, `final` fields, no setters, never hand out the
internal array.

---

## In the JDK

- `Integer.valueOf(int)` — caches −128..127; `Integer.valueOf(100) == Integer.valueOf(100)`
  is `true`, for 1000 it's `false` (an interview classic)
- `String` interning / the string pool
- `Boolean.valueOf`, `Character.valueOf`, `enum` constants

---

## Flyweight vs neighbours

| | Difference |
|---|---|
| **Singleton** | one instance total; flyweight is one instance **per key** |
| **Prototype** | many independent copies; flyweight is one shared original |
| **Factory** | the flyweight factory is a factory with a cache |
| **Composite** | leaf nodes are often flyweights (characters in a document tree) |

---

## Pitfalls

- **Light intrinsic state** — sharing a `String` saves nothing (the bullet example)
- **Mutable flyweight** — one change, every context affected
- **Incomplete key** — two kinds collapsed into one flyweight
- **Premature use** — adds a factory and splits a class; only for measured memory problems
- **Identity checks** — fine for flyweights you created; never rely on `==` for `Integer`
  outside the cache range

---

## Cheat sheet

- Trigger: very many objects, mostly identical data, memory pressure
- Fix: immutable flyweight for intrinsic state, factory keyed on it, extrinsic state
  passed in
- Interview line: *"Flyweight shares intrinsic state across many objects to save
  memory. `Integer.valueOf` caches −128 to 127 — that's why `==` works for small
  `Integer`s and not large ones."*
