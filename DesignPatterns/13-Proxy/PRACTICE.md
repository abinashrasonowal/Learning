# 13-Proxy — Session Plan

Goal is not "load an image lazily" — that's one kind of proxy, and the course's version
isn't thread-safe. It's **build a virtual and a protection proxy, stack them, and see a
dynamic proxy do it for any interface**. Different domain on purpose.

---

## Layout

```
13-Proxy/
  proxy/problem/User.java
  proxy/problem/RealReportStore.java     connects in the constructor
  proxy/problem/AdminPage.java           checks the role
  proxy/problem/ArchivePage.java         forgets to
  proxy/problem/ProxyProblemMain.java
  proxy/solution/User.java  ReportStore.java  RealReportStore.java
  proxy/solution/LazyReportStore.java      virtual proxy (synchronized)
  proxy/solution/ProtectedReportStore.java protection proxy
  proxy/solution/ArchivePage.java          unchanged logic, given a protected store
  proxy/solution/CallCounter.java          java.lang.reflect.Proxy
  proxy/solution/ProxySolutionMain.java
```

```bash
javac -Xlint:all -d out $(find . -name '*.java') && java -ea -cp out proxy.solution.ProxySolutionMain
```

## Rules for `solution/`

- every proxy implements `ReportStore`; callers never see `RealReportStore`
- no role check in any page — only in `ProtectedReportStore`
- `LazyReportStore` connects at most once, even under concurrency
- the dynamic proxy rethrows the real exception
- prove connections, refusals and call counts with `assert`, not prints

---

## 1. Read — 15 min

`NOTES.md` top to bottom.

## 2. Build — ~45 min

| Step | Build | Must prove |
|---|---|---|
| **problem** | eager store, checks in callers | 1 connection with 0 reads; viewer deletes via `ArchivePage` |
| **virtual** | `LazyReportStore` | 0 connections until first read, then exactly 1 |
| **protection** | `ProtectedReportStore` | viewer refused through the same `ArchivePage`; admin allowed |
| **dynamic** | `CallCounter` | counts per method, real `SecurityException` comes through |

## 3. Break it on purpose

- Remove `synchronized` from `LazyReportStore.real()`. Use `ladder.Race` from
  02-Singleton (copy it) to hit `read()` from 32 threads. Count connections.
- In `CallCounter`, drop the `InvocationTargetException` unwrap. What exception does
  the viewer's delete throw now? Revert.

## 4. Self-check — 10 min

1. Five kinds of proxy — what does each control?
2. Proxy vs Decorator — same diagram, what differs?
3. Why does the protection proxy fix the `ArchivePage` hole without editing it?
4. Why is the course's `ProxyImage` not thread-safe?
5. Why does `@Transactional` not apply on self-invocation?

## 5. Commit

```bash
git add DesignPatterns/13-Proxy && git commit -m "proxy: <step>"
```

---

## Done when

- [x] Watched the course video, did the course exercise
- [ ] Read `NOTES.md`
- [x] problem — written, runs, eager connect + permission hole shown
- [x] virtual + protection proxies, stacked
- [x] dynamic proxy counting calls
- [ ] broken once (unsynchronised lazy init), reverted
- [ ] All 5 self-check questions answered without looking
- [ ] Committed

Then tick `13 — Proxy` in `../README.md`.

**Stuck?** Ask me for a nudge on the pattern — not the code.
