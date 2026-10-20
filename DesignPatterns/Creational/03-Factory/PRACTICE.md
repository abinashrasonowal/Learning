# 03-Factory — Session Plan

Goal is not "move `new Car()` into a switch" — with one caller and no creation logic
that's ceremony. It's **centralise creation that has real logic, so no caller can
skip the validation, and add a type without editing callers**. Different domain from
the transport example on purpose.

---

## Layout

```
03-Factory/
  factory/problem/Storage.java  MemoryStorage.java  S3Storage.java
  factory/problem/BackupJob.java          parses the URI itself, validates the bucket
  factory/problem/CacheWarmer.java        copy of the same parsing — validation lost
  factory/problem/FactoryProblemMain.java
  factory/solution/Storage.java  MemoryStorage.java  S3Storage.java  FileStorage.java
  factory/solution/StorageFactory.java    registry: scheme -> creator, validation inside
  factory/solution/BackupJob.java  CacheWarmer.java   take a Storage, parse nothing
  factory/solution/FactorySolutionMain.java
```

```bash
javac -Xlint:all -d out $(find . -name '*.java') && java -ea -cp out factory.solution.FactorySolutionMain
```

## Rules for `solution/`

- no `new S3Storage` / `new FileStorage` outside `StorageFactory`
- clients take a `Storage` in their constructor — they never see a URI
- `create()` returns `Storage`, never a concrete type
- unknown scheme → throw with the known schemes listed, never `null`
- a new scheme can be added with **one `register` call**, no edits to the factory
- prove it with `assert`, not prints

---

## 1. Read — 15 min

`NOTES.md` top to bottom.

## 2. Build — ~45 min

| Step | Build | Must prove |
|---|---|---|
| **problem** | two clients, each parsing the URI | same bad bucket: one rejects, one accepts |
| **solution** | `StorageFactory` + clients taking `Storage` | same results; bad bucket rejected everywhere |
| **new scheme** | `file:` inside the factory | no edits to `BackupJob` / `CacheWarmer` |
| **plugin** | `gcs://` registered from `main` | zero edits to any class |

## 3. Break it on purpose

- Change `create()` to return `S3Storage` and call an S3-only method from
  `BackupJob`. Then try to pass it a `file:` URI. Explain in a comment. Revert.
- Rewrite `StorageFactory` as a Factory Method: abstract `Job` with
  `protected abstract Storage createStorage()`. Which version fits this problem?

## 4. Self-check — 10 min

1. Static factory method vs simple factory vs Factory Method — one line each.
2. Why does the course's `new Car()` problem not really hurt yet?
3. Switch vs registry — when is the switch fine?
4. Why must the factory return the interface?
5. Name two factories in the JDK.

## 5. Commit

```bash
git add DesignPatterns/03-Factory && git commit -m "factory: <step>"
```

---

## Done when

- [x] Watched the course video, did the course exercise
- [ ] Read `NOTES.md`
- [x] problem — written, runs, drift shown
- [x] solution — same results, validation in one place
- [x] `file:` and `gcs://` added with no client edits
- [ ] broken once (concrete return type), reverted
- [ ] All 5 self-check questions answered without looking
- [ ] Committed

Then tick `03 — Factory` in `../README.md`.

**Stuck?** Ask me for a nudge on the pattern — not the code.
