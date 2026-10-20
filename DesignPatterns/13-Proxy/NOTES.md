# Proxy

> A stand-in with the same interface as the real object, that controls **access** to
> it — when it's created, who may call it, where it lives.

The client holds a `ReportStore` and never knows whether it's the real one.

---

## The shape

```java
interface ReportStore { String read(String id); void delete(String id); }   // subject
class RealReportStore implements ReportStore { ... }                         // real subject
class ProtectedReportStore implements ReportStore {                          // proxy
    private final ReportStore inner;
    public void delete(String id) { checkAdmin(); inner.delete(id); }
}
```

| Role | Job | Knows about |
|---|---|---|
| **Subject** | the interface | nothing |
| **Real subject** | the actual work | its own job |
| **Proxy** | same interface; decides whether/when/how the real one is reached | the real subject (often creates it) |
| **Client** | uses the subject | the interface only |

---

## Kinds of proxy

| Kind | Controls | Example here / elsewhere |
|---|---|---|
| **Virtual** | *when* the real object is created | `LazyReportStore` — connect on first use (the course's `ProxyImage`) |
| **Protection** | *who* may call what | `ProtectedReportStore` — only admins delete |
| **Remote** | *where* it runs | RMI stubs, gRPC clients — a local object for a remote service |
| **Caching** | *whether* the real call happens | return a stored result |
| **Smart reference / logging** | *what happens around* each call | `CallCounter` — counts calls |

---

## Smells it replaces

- **Eager expensive setup** — connecting at startup even when nothing is read
- **Access rules in callers** — `if (!user.isAdmin())` in `AdminPage`, forgotten in
  `ArchivePage`. Every caller is a potential hole. With a protection proxy, the check
  travels **with the object** the caller is given.

---

## The reference example: lazy image

`ProxyImage` delays `new RealImage(...)` (which loads from disk) until `display()` —
a correct virtual proxy. Two things to add:

- it's **not thread-safe**: two threads calling `display()` at once can both load the
  image. `synchronized` (or a holder) on the lazy getter fixes it.
- it's only one of five kinds. Protection proxies are the ones that show up most in
  real code (security, transactions).

---

## Dynamic proxies — `java.lang.reflect.Proxy`

```java
(T) Proxy.newProxyInstance(loader, new Class<?>[]{iface}, (proxy, method, args) -> {
    before(); Object r = method.invoke(target, args); after(); return r;
});
```

One handler stands in for **any** interface, generated at runtime. This is how Spring
`@Transactional`, Spring Data repositories and Mockito mocks work. Unwrap
`InvocationTargetException` so callers see the real exception.

---

## Proxy vs Decorator

Same class diagram — both implement the interface and hold one. The difference is intent
and who's in charge:

| | Proxy | Decorator |
|---|---|---|
| Intent | **control access** to the subject | **add behaviour** to it |
| Who creates the inner object | often the proxy itself (lazy, remote) | the client, then wraps it |
| Stacking | usually one, chosen by infrastructure | many, chosen freely by the client |
| Client awareness | shouldn't know it's a proxy | knowingly builds the stack |

In practice the line blurs (a caching proxy ≈ a caching decorator). Name it by what it's
*for*.

---

## In the JDK and frameworks

- `java.lang.reflect.Proxy`, RMI stubs
- Hibernate lazy-loaded entities (a subclass proxy that loads on first getter)
- Spring AOP proxies around beans

---

## Pitfalls

- **Lazy proxy without synchronisation** — double initialisation under load
- **Leaking the real subject** — a getter on the proxy returning `inner` defeats a
  protection proxy
- **Self-invocation** — inside the real object, `this.method()` skips the proxy
  (why `@Transactional` on a method called from the same class doesn't apply)
- **Swallowing reflection exceptions** in dynamic proxies — callers see
  `UndeclaredThrowableException` instead of the real error

---

## Cheat sheet

- Trigger: expensive setup done eagerly, or access rules repeated in callers
- Fix: a stand-in with the same interface that delays, guards, or wraps the real calls
- Interview line: *"Proxy controls access to an object through the same interface —
  virtual, protection, remote, caching. Spring's `@Transactional` is a dynamic proxy;
  that's why self-invocation bypasses it."*
