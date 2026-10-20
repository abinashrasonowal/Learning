# Builder

> Build a complex object step by step with named calls, then validate and freeze it in
> one `build()`.

The fix for constructors with too many parameters — especially several of the same
type, or many optional ones.

---

## The shape (Bloch's builder — the one you'll write)

```java
public final class HttpRequest {
    private final String method; ...                          // all final
    private HttpRequest(Builder b) { ...copy fields... }       // private: only the builder calls it
    public static Builder builder(String method, String url)   // required params here
    public static final class Builder {
        public Builder header(String k, String v) { ...; return this; }   // optional, chainable
        public Builder timeoutMs(int ms)          { ...; return this; }
        public HttpRequest build() { validate(); return new HttpRequest(this); }
    }
}
HttpRequest r = HttpRequest.builder("POST", url).header("Accept", "json").timeoutMs(2000).build();
```

| Part | Job |
|---|---|
| **Product** | immutable, private constructor |
| **Builder** | mutable scratchpad: defaults, accumulates, validates in `build()` |
| **`builder(required…)`** | required params can't be forgotten |
| **`build()`** | the one place cross-field rules are checked; returns a frozen object |

---

## Smells it replaces

**Telescoping constructors**

```java
new HttpRequest("POST", url, headers, "{}", 3, 2000);   // which int is the timeout?
```

- same-typed parameters side by side → a swapped call compiles
- one constructor per combination → "headers but default timeout" needs a new one
- the call site is unreadable without the IDE's parameter hints

**JavaBeans (`new` + setters)**

- the object exists in a half-set, invalid state between calls
- can't be immutable — setters are the API
- nowhere to validate "GET has no body" — which setter would check it?

---

## The reference example: `House`

Correct shape: mandatory fields in the builder's constructor, optional ones chained.
Look at the **problem** version's constructor though:

```java
this.hasSwimmingPool = false;    // ignores the parameter
```

Three booleans in a row, and one silently dropped — nobody notices, because
`new House(..., true, true, false)` is unreadable anyway. That's the smell, caught in
the wild. Also worth fixing in the solution: fields aren't `final`, and `build()`
doesn't validate anything.

---

## What `build()` is for

Validation that needs **more than one field** can only run once every field is known:
"GET must not have a body", "end after start", "pool max ≥ min". Per-setter checks
can't do it. If `build()` just calls `new`, half the pattern's value is unused.

---

## Immutability details

- product fields `final`, class `final`
- **defensive copy** collections in the product's constructor
  (`unmodifiableMap(new LinkedHashMap<>(b.headers))`) — otherwise changing the builder
  after `build()` changes the "immutable" object
- the builder may be reused; each `build()` is independent
- `toBuilder()` — start a variant from an existing object (pairs with Prototype)

---

## GoF Builder vs Bloch Builder

GoF's version adds a **Director** that runs the same steps on different builders
(`HtmlBuilder`, `PdfBuilder`) to produce different representations. Rare in practice.
When people say "builder" in Java, they almost always mean Bloch's — *Effective Java*
item 2.

---

## In the JDK

- `java.net.http.HttpRequest.newBuilder()` — this exact domain
- `Locale.Builder`, `Stream.builder()`, `ProcessBuilder`
- `StringBuilder` — builder in spirit (accumulate, then `toString()`), no validation

---

## Pitfalls

- **Required fields as setters** — `builder().build()` with no URL. Put required ones
  in `builder(...)`.
- **No defensive copy** — the builder's map leaks into the product.
- **Validation in setters only** — misses every cross-field rule.
- **Builder for 3 fields** — a constructor or a `record` is fine until parameters
  multiply or repeat types.

---

## Cheat sheet

- Trigger: constructor with 4+ params, several optional, or several of the same type
- Fix: private constructor, static nested `Builder`, required params in `builder(...)`,
  validate in `build()`, defensive-copy collections
- Interview line: *"Builder replaces telescoping constructors with named steps and one
  validation point. `java.net.http.HttpRequest.newBuilder()` is the JDK example."*
