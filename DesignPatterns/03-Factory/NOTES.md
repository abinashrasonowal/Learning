# Factory

> Put the decision "which class do I instantiate?" in one place, and hand callers an
> interface instead of a concrete class.

Three different things all get called "factory". Know which one you mean.

---

## The three factories

| Name | Shape | Example |
|---|---|---|
| **Static factory method** | `static Foo of(...)` on the class itself, instead of a constructor | `List.of`, `Integer.valueOf`, `Optional.empty` |
| **Simple factory** | a separate class with `create(type)` that picks a concrete class | `StorageFactory.create("s3://bucket")`, `DriverManager.getConnection(url)` |
| **Factory Method** (GoF) | an abstract `createX()` in a base class; **subclasses** decide what to build | `Iterable.iterator()` — every collection decides which iterator |

The course's `TransportFactory` is a **simple factory**. It's the one you'll write
most; Factory Method is the one the GoF book means.

---

## Simple factory — the shape

```java
interface Storage { ... }                                  // product
class S3Storage implements Storage { ... }                 // concrete products
class StorageFactory {                                     // the factory
    Storage create(String uri) { ... picks + validates + constructs ... }
}
Storage s = factory.create(config.get("storage.uri"));     // caller names no class
```

| Role | Job | Knows about |
|---|---|---|
| **Product** | the interface callers use | nothing |
| **Concrete product** | one implementation | its own data |
| **Factory** | maps input → concrete class, validates, constructs | every concrete product |
| **Client** | uses the product | the interface and the factory, never `new S3Storage` |

---

## Smell it replaces

```java
if (uri.startsWith("s3://")) storage = new S3Storage(...); else if (...) ...
```

…copied into every class that needs a `Storage`. The copies drift — one validates the
bucket name, one doesn't — and adding `file:` means finding every copy.

The course's problem (`new Car()`, `new Bike()` in `main`) doesn't hurt yet, because
there's only one call site and no creation logic. The pattern pays off when creation
has **logic** (parsing, validation, defaults) and **many callers**.

---

## `switch` vs registry

The course's factory is a `switch` on a string. That's fine — it's one switch, in one
place, which is the whole point. When you need callers or plugins to add types
**without editing the factory** (OCP), switch to a registry:

```java
Map<String, Function<String, Storage>> creators;
factory.register("gcs://", GcsStorage::new);
```

Either way: unknown type → throw with the list of known ones, never return `null`.

---

## Factory Method (GoF)

```java
abstract class ReportJob {
    final void run() { Storage s = createStorage(); ... }   // uses it
    protected abstract Storage createStorage();              // subclass decides
}
class NightlyJob extends ReportJob { protected Storage createStorage() { return new S3Storage("nightly"); } }
```

It's Template Method where the varying step is **creation**. Use it when the class
that *uses* the product is also the thing being specialised.

---

## Factory vs neighbours

| | Difference |
|---|---|
| **Abstract Factory** | factory of a *family* of related products (charger + refunder from the same provider) |
| **Builder** | builds *one* complex object step by step; factory picks *which* class in one call |
| **Prototype** | creates by copying an existing instance instead of choosing a class |
| **Strategy** | a factory often *returns* a strategy — Strategy's "the if/else moves to the edge" is a factory |

---

## Pitfalls

- **Factory returns the concrete type** — `S3Storage create(...)`. Callers start using
  S3-only methods; you're coupled again.
- **Boolean soup** — `create(true, false, true)`. That's a Builder's job.
- **Creation in the factory, validation in the caller** — the whole point is that
  validation can't be skipped.
- **Every class gets a factory** — if there's one implementation and no logic, `new`
  is fine.

---

## Cheat sheet

- Trigger: `if/else` + `new ConcreteX` copied wherever an object is needed
- Fix: one factory maps input → instance, callers depend on the interface
- Interview line: *"Simple factory centralises which class to instantiate; Factory
  Method defers it to a subclass. `DriverManager.getConnection(url)` is a simple
  factory — the URL decides the driver."*
