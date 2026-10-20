# 10-Decorator — Session Plan

Goal is not "add toppings" — in the course's pizza every layer just adds, so order
never matters. It's **replace a combinatorial class tree with stackable wrappers, and
prove that wrapping order changes behaviour**. Different domain on purpose.

---

## Layout

```
10-Decorator/
  decorator/problem/FakeServer.java  Fetcher.java  HttpFetcher.java
  decorator/problem/RetryingHttpFetcher.java          extends HttpFetcher, hard-coded 3
  decorator/problem/CachingRetryingHttpFetcher.java   extends that — one class per combo
  decorator/problem/DecoratorProblemMain.java
  decorator/solution/FakeServer.java  Fetcher.java  HttpFetcher.java
  decorator/solution/RetryingFetcher.java  CachingFetcher.java  LoggingFetcher.java
  decorator/solution/DecoratorSolutionMain.java
```

```bash
javac -Xlint:all -d out $(find . -name '*.java') && java -ea -cp out decorator.solution.DecoratorSolutionMain
```

## Rules for `solution/`

- each decorator implements `Fetcher` and wraps a `Fetcher` — never `HttpFetcher`
- no decorator extends another class
- retry attempts are a constructor argument
- prove the call counts on `FakeServer` with `assert`, not prints
- prove two orders of the same decorators behave differently

---

## 1. Read — 15 min

`NOTES.md` top to bottom.

## 2. Build — ~45 min

| Step | Build | Must prove |
|---|---|---|
| **problem** | inheritance chain | cache+retry works; cache-without-retry impossible |
| **solution** | three decorators | same call counts as `problem/` |
| **new combo** | cache, no retry | fails fast: 1 call |
| **order** | log∘cache vs cache∘log | 2 log lines vs 1 |
| **lambda** | a one-line decorator | no new class |

## 3. Break it on purpose

- Make `CachingFetcher`'s field type `HttpFetcher` instead of `Fetcher`. Try to wrap
  a `RetryingFetcher` with it. Explain in a comment. Revert.
- Put retry **outside** cache: `new RetryingFetcher(new CachingFetcher(http), 3)`.
  Does anything change for this server? When would it?

## 4. Self-check — 10 min

1. How many classes for 3 optional behaviours — inheritance vs decorator?
2. Why does wrapping order matter? Give the logging example.
3. When is an abstract forwarding base decorator worth it?
4. Decorator vs Proxy — same structure, different intent?
5. Name the decorator stack in `java.io`.

## 5. Commit

```bash
git add DesignPatterns/10-Decorator && git commit -m "decorator: <step>"
```

---

## Done when

- [x] Watched the course video, did the course exercise
- [ ] Read `NOTES.md`
- [x] problem — written, runs, missing combination shown
- [x] solution — same call counts, composed at runtime
- [x] order dependence asserted
- [ ] broken once (concrete field type), reverted
- [ ] All 5 self-check questions answered without looking
- [ ] Committed

Then tick `10 — Decorator` in `../README.md`.

**Stuck?** Ask me for a nudge on the pattern — not the code.
