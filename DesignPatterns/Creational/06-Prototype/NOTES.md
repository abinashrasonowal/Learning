# Prototype

> Create new objects by copying an existing, already-configured one — and let the
> object do its own copying.

Useful when an object is expensive or fiddly to set up, and you want many slight
variations of a known-good one.

---

## The shape

```java
interface Prototype<T> { T copy(); }
class ServerConfig implements Prototype<ServerConfig> {
    private ServerConfig(ServerConfig other) { ...deep copy every field... }   // copy constructor
    public ServerConfig copy() { return new ServerConfig(this); }
}
ServerConfig web2 = template.copy().rename("web-2").addPort(8443);
```

| Role | Job | Knows about |
|---|---|---|
| **Prototype** | copies itself, deeply enough | its own fields |
| **Client** | asks for a copy, customises it | the `copy()` method only |
| **Registry** (optional) | named templates, hands out copies | prototypes by key |

---

## Smell it replaces

```java
new ServerConfig("web-2", t.cpus(), t.ports(), t.disks());   // copy by hand
```

- the **client** must know every field — add `tags` later and every hand-copy misses it
- it copies **references**: the copy's port list and disks *are* the template's;
  customising the copy edits the template
- private state can't be copied from outside at all

---

## Shallow vs deep — the whole pattern lives here

| Field type | Copy how | Why |
|---|---|---|
| primitive, `String`, `Integer`, records of those | share it | immutable — sharing is safe |
| mutable object (`Disk`) | `disk.copy()` | otherwise both configs resize the same disk |
| collection of immutables (`List<Integer>`) | new list, same elements | the list is mutable, the elements aren't |
| collection of mutables (`List<Disk>`) | new list, copy each element | both levels are mutable |

Rule: **copy until you hit something immutable.** Get one level wrong and the bug is a
"template that changes by itself" — far from the line that caused it.

---

## Why not `Cloneable` / `Object.clone()`

- `Object.clone()` is a **shallow** field-by-field copy — the bug above, by default
- `Cloneable` is a marker with no methods; forget it → `CloneNotSupportedException`
- `clone()` can't reassign `final` fields, so deep-copying a `final List` is impossible
- *Effective Java* item 13: prefer a **copy constructor** or **copy factory**

The JDK has the trap too: `ArrayList.clone()` and `array.clone()` are shallow.
`new ArrayList<>(other)` is a copy constructor — also shallow in its elements.

---

## The reference example: game board

Deep enough: `GameBoard.clone()` builds a new board and clones each `GamePiece` —
right, because pieces have setters. Two things to notice:

- the interface method is named `clone()`, which **overrides `Object.clone()`**
  (widening `protected` → `public`, covariant return). It works, but readers will
  assume `Cloneable` semantics. `copy()` says what it means.
- `getPieces()` returns the live list, so a caller can still share pieces by hand —
  the pattern only protects you if the internals aren't handed out.

---

## Prototype registry

Keep named, pre-configured prototypes and hand out copies:

```java
registry.register("web", webTemplate);    // store a COPY
ServerConfig s = registry.create("web");   // return a COPY
```

Copy on the way **in** too — otherwise the caller who registered it can still edit
the stored template.

---

## Prototype vs neighbours

| | Difference |
|---|---|
| **Factory** | picks a class and calls `new`; prototype copies an instance, so the configuration comes along |
| **Builder `toBuilder()`** | same idea — start from an existing object — for immutable objects |
| **Memento** | snapshot to *restore* later; prototype copies to *use* now |
| **Singleton** | the opposite: exactly one instance vs as many copies as you like |

---

## Pitfalls

- **Shallow copy of a mutable field** — the template changes when a copy does
- **Getters returning live collections** — callers share internals by hand
- **Registry storing the caller's object** — no copy on the way in
- **Cycles** (A → B → A) — naive deep copy loops forever; needs an identity map

---

## Cheat sheet

- Trigger: clients copying objects field by field, or templates "changing by themselves"
- Fix: `copy()` via a private copy constructor that deep-copies every mutable field
- Interview line: *"Prototype lets an object copy itself, so the client doesn't need
  to know its fields. Prefer a copy constructor over `Cloneable` — `Object.clone()` is
  shallow and fights `final`."*
