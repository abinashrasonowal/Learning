# Decorator

> Add behaviour to an object by wrapping it in another object with the same
> interface — and stack wrappers in any combination, at runtime.

`new CachingFetcher(new RetryingFetcher(new HttpFetcher(server), 3))` — each layer
is a `Fetcher`, holds a `Fetcher`, and adds one thing.

---

## The shape

```java
interface Fetcher { String fetch(String url); }                // component
class HttpFetcher implements Fetcher { ... }                    // concrete component
class RetryingFetcher implements Fetcher {                      // decorator
    private final Fetcher inner;                                 // IS-A and HAS-A Fetcher
    public String fetch(String url) { ...retry around inner.fetch(url)... }
}
```

| Role | Job | Knows about |
|---|---|---|
| **Component** | the interface everyone implements | nothing |
| **Concrete component** | does the real work | its own job |
| **Decorator** | holds a component, adds behaviour before/after/around delegating | the interface only — never what it wraps |
| **Client** | builds the stack once, then uses it as a plain component | the interface |

---

## Smell it replaces

```java
class RetryingHttpFetcher extends HttpFetcher { ... }
class CachingRetryingHttpFetcher extends RetryingHttpFetcher { ... }
```

Behaviour added by **subclassing**:

- one class per **combination** — retry, cache, log → up to 7 classes, and "cache but
  don't retry" is yet another
- combinations fixed at **compile time**
- settings baked in (the hard-coded `3` attempts)
- retry is welded to HTTP — can't reuse it around another fetcher

Decorator turns *n* features into *n* classes, combinable freely.

---

## Order matters

```java
new LoggingFetcher(new CachingFetcher(http))   // logs every request, hits included
new CachingFetcher(new LoggingFetcher(http))   // logs only misses — real network calls
```

Same pieces, different behaviour. Outer layers see everything; inner layers see only
what outer layers let through. Decide the order deliberately and test it.

---

## The reference example: pizza toppings

`new MushroomDecorator(new OliveDecorator(new CheeseDecorator(basic)))` — the textbook
demo, and it shows the class-explosion problem well (`CheeseOlivePizza extends
CheesePizza`). Two notes:

- every topping just **adds** to cost and description, so order never matters — which
  hides the most important property of real decorators (see above)
- the abstract `PizzaDecorator` base that forwards every method pays off when the
  interface has **many** methods; with one method, each decorator can implement the
  interface directly (or be a lambda)

---

## In the JDK

- `java.io` is built on it: `new BufferedReader(new InputStreamReader(new FileInputStream(f)))`
- `Collections.unmodifiableList(list)`, `Collections.synchronizedMap(map)`
- servlet `HttpServletRequestWrapper`

---

## Decorator vs neighbours

| | Same interface? | Intent | Who builds the stack |
|---|---|---|---|
| **Decorator** | yes | **add** behaviour, stackable | the client, freely |
| **Proxy** | yes | **control access** to the real thing (lazy, security, remote) | usually the proxy owns/creates the subject |
| **Adapter** | **no** — changes it | make it fit | — |
| **Composite** | yes | many children, aggregate | — |
| **Chain of Responsibility** | yes | each link may **stop** the request | — |

---

## Pitfalls

- **Identity** — `decorated != original`; `equals`/`instanceof HttpFetcher` checks
  break. Don't type-check what you've wrapped.
- **Unwrapping** — a caller needing the inner object's extra methods is a sign the
  interface is too thin.
- **Deep stacks** — debugging through 6 layers is painful; build the stack in one
  place (a factory) so it's readable.
- **Stateful decorators shared across threads** — `CachingFetcher`'s `HashMap` isn't
  thread-safe.

---

## Cheat sheet

- Trigger: subclasses for each combination of optional behaviour
- Fix: one decorator per behaviour, each wrapping the interface; compose at runtime
- Interview line: *"Decorator adds responsibilities by wrapping, not inheriting —
  `java.io` streams are the textbook case. Order of wrapping changes behaviour."*
