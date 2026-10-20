# 06-Prototype — Session Plan

Goal is not "implement `clone()`" — the course's board does that. It's **know exactly
which fields need deep copying and prove a copy can't reach back into its template**.
Different domain on purpose.

---

## Layout

```
06-Prototype/
  prototype/problem/Disk.java                  mutable: resize()
  prototype/problem/ServerConfig.java          ports, disks, tags (added later)
  prototype/problem/PrototypeProblemMain.java  hand copy: shared lists, dropped tags
  prototype/solution/Prototype.java            copy(), not clone()
  prototype/solution/Disk.java                 copy()
  prototype/solution/ServerConfig.java         private copy constructor, deep where needed
  prototype/solution/PrototypeRegistry.java    copy in, copy out
  prototype/solution/PrototypeSolutionMain.java
```

```bash
javac -Xlint:all -d out $(find . -name '*.java') && java -ea -cp out prototype.solution.PrototypeSolutionMain
```

## Rules for `solution/`

- callers never list `ServerConfig`'s fields to copy it
- no `Cloneable`, no `Object.clone()`
- `Disk` is deep-copied; `Integer` ports and `String` tags are shared (immutable)
- getters don't hand out live collections
- the registry copies on the way in and on the way out
- prove it with `assert`, not prints

---

## 1. Read — 15 min

`NOTES.md` top to bottom.

## 2. Build — ~45 min

| Step | Build | Must prove |
|---|---|---|
| **problem** | hand copy in `main` | template gets the copy's port and disk size; tags dropped |
| **solution** | `copy()` via copy constructor | same customisation, template untouched; tags carried |
| **registry** | named templates | two `create` calls are independent; editing the registered object changes nothing |

## 3. Break it on purpose

- In the copy constructor, write `this.disks.addAll(other.disks)` instead of copying
  each disk. Run the asserts. Which one fails, and why not the ports one? Revert.
- Implement `Cloneable` on `ServerConfig` with `super.clone()`. Try to deep-copy the
  `final` disks list. Write down why you can't. Revert.

## 4. Self-check — 10 min

1. Which fields need deep copying, and what's the stopping rule?
2. Three problems with `Object.clone()`.
3. Why does the registry copy on the way in?
4. Prototype vs Factory — what comes along with a copy that `new` doesn't give you?
5. Is `ArrayList.clone()` deep or shallow?

## 5. Commit

```bash
git add DesignPatterns/06-Prototype && git commit -m "prototype: <step>"
```

---

## Done when

- [x] Watched the course video, did the course exercise
- [ ] Read `NOTES.md`
- [x] problem — written, runs, shared state + dropped field shown
- [x] solution — deep copy, template untouched
- [x] registry — independent copies, copy in and out
- [ ] broken once (shallow disks), reverted
- [ ] All 5 self-check questions answered without looking
- [ ] Committed

Then tick `06 — Prototype` in `../README.md`.

**Stuck?** Ask me for a nudge on the pattern — not the code.
