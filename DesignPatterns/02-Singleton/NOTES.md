# Singleton

> Guarantee a class has exactly one instance, and give the program a way to reach it.

The simplest pattern to write and the easiest to get wrong. Two separate questions
live inside it, and mixing them up is where the trouble starts:

1. **Should there be one instance?** — usually a fair question (one connection pool)
2. **Should the class enforce that itself, through a static?** — usually not

Classic Singleton answers "yes" to both. That second yes is what makes it the most
argued-about pattern in the book.

---

## The shape

```java
private static Singleton instance;   // the one slot
private Singleton() {}               // nobody else can construct
public static Singleton getInstance()  // the global access point
```

Three moving parts: a private constructor, a static field, a static accessor.
Remove any one and it isn't a Singleton.

---

## Five implementations, in the order people discover them

### 1. Eager

```java
private static final Config INSTANCE = new Config();
```

Thread-safe for free — the JVM initialises statics under a lock, once. Cost: built
even if never used, and a constructor that throws becomes an `ExceptionInInitializerError`
at class-load time, far from the code that caused it.

### 2. Lazy, naive — **broken**

```java
if (instance == null) instance = new Config();
```

Two threads, both see `null`, both construct. You get two singletons. This is not
theoretical — it reproduces reliably with ~50 threads and a slow constructor.

### 3. `synchronized getInstance()`

Correct, and the honest first fix. Cost: every *read* takes the lock forever, to
protect a write that happens once. Fine until it's on a hot path.

### 4. Double-checked locking — **broken without `volatile`**

```java
private static volatile Config instance;      // volatile is not optional
if (instance == null) {
    synchronized (Config.class) {
        if (instance == null) instance = new Config();
    }
}
```

Without `volatile`, a second thread can see a **non-null reference to a
half-constructed object** — the JVM is allowed to publish the reference before the
constructor's writes are visible. The bug is invisible in testing and brutal in
production. `volatile` forbids that reordering (properly, since Java 5's memory model).

### 5. Holder idiom (Bill Pugh) — **the one to reach for**

```java
private static class Holder {
    static final Config INSTANCE = new Config();
}
public static Config getInstance() { return Holder.INSTANCE; }
```

Lazy *and* thread-safe with zero synchronisation in your code: the inner class isn't
initialised until first touched, and the JVM already guarantees class init happens
once. No `volatile` to forget, no lock on the read path.

### 6. `enum` singleton (Bloch, *Effective Java*)

```java
public enum Config { INSTANCE; }
```

The only version immune to **both** reflection and serialization attacks, for free.
Cost: can't extend a class, and eager. Awkward when the instance needs constructor
arguments.

---

## How singletons get broken

| Attack | What happens | Defence |
|---|---|---|
| **Reflection** | `ctor.setAccessible(true); ctor.newInstance()` — private constructor bypassed | guard inside the constructor, or use `enum` |
| **Serialization** | deserializing returns a *new* object; `==` now fails | `private Object readResolve() { return getInstance(); }`, or `enum` |
| **Cloning** | `clone()` copies it | don't implement `Cloneable`; throw if inherited |
| **Two classloaders** | each loader has its own statics → one instance *per loader* | rarely worth fixing; know it exists |

`enum` wins the table without you writing a line of defence. That's the argument for it.

---

## Why it's on every "considered harmful" list

**It's a global variable wearing a design-pattern hat.** Consequences:

- **Hidden dependency.** `Config.getInstance()` buried in a method means the class's
  real dependencies aren't in its constructor. You can't tell what it needs by reading
  its signature — a **DIP violation** with extra steps.
- **Untestable.** No seam. Tests can't inject a fake, and static state leaks between
  tests, so test order starts to matter. Needing a `resetForTests()` is the smell
  confessing itself.
- **Lifecycle you don't control.** Born on first touch, dies with the JVM. No ordering,
  no scoping, no per-request or per-tenant variant later.

**The reframe:** you usually want *one instance*, not *a class that enforces one
instance*. Construct it once in `main` (or let Spring's default singleton scope do it)
and inject it. Same instance count, no global, testable — see `01-SOLID/dip/solution`,
which is exactly this move.

---

## When it's genuinely right

- Truly stateless, or immutable-after-load: config read once, a logger, a metrics registry
- Expensive shared resource with real cardinality of one: thread pool, connection pool
- The JDK's own: `Runtime.getRuntime()`, `Desktop.getDesktop()`, `LogManager`

Litmus test: **would a second instance be a bug, or just wasteful?** If merely
wasteful, you want a shared instance, not a Singleton.

---

## Cheat sheet

| | |
|---|---|
| Intent | one instance + global access point |
| Use when | a second instance would be a *bug*, and injection genuinely can't reach the caller |
| Default implementation | holder idiom; `enum` if you want attack-proof and don't need ctor args |
| Never ship | naive lazy, or DCL without `volatile` |
| Real cost | hidden dependencies, no test seam, JVM-scoped lifecycle |
| Related | Prototype is its opposite; Factory often *returns* one; DI containers replace it |

**Interview framing:** name the holder idiom, say *why* `volatile` matters in DCL
(publication of a partially constructed object, not staleness), then volunteer that
you'd rather inject a single instance than reach for a static. That last sentence is
the one that separates memorised from understood.
