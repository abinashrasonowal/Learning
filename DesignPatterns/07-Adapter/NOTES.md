# Adapter

> Make an existing class fit the interface your code expects, without changing either
> side — a wrapper that translates.

The travel plug of patterns. You reach for it when the thing you need already exists,
but speaks a different dialect (a vendor SDK, a legacy class, another team's API).

---

## The shape

```java
interface ExchangeRates { double rate(Currency from, Currency to); }   // target — what we want
class FxVendorClient { long getQuote(String pair) { ... } }            // adaptee — what we have
class FxVendorAdapter implements ExchangeRates {                        // the adapter
    private final FxVendorClient vendor;
    public double rate(Currency f, Currency t) { ...translate, call vendor, translate back... }
}
```

| Role | Job | Knows about |
|---|---|---|
| **Target** | the interface our code depends on | our domain types |
| **Adaptee** | existing class we can't/won't change | its own world |
| **Adapter** | implements target, holds adaptee, translates | both |
| **Client** | uses the target | target only |

---

## Smell it replaces

Business code calling the vendor directly: building `"USD" + "INR"` strings, dividing
by the vendor's `10_000` scale, checking its `-1` sentinel. Every caller repeats the
dialect, one of them gets it wrong (here: `-1` treated as a rate → negative price), and
switching vendors means rewriting business code.

---

## What an adapter really translates

The course's `SendGridAdapter` only renames: `send(to, subject, body)` →
`sendEmail(to, subject, body)`. That's the minimum. Real adapters translate:

| Mismatch | Example here |
|---|---|
| **names** | `getQuote` → `rate` |
| **types** | `Currency` enum → `"USDINR"` string |
| **units / scale** | `long` × 10 000 → `double` |
| **error style** | `-1` sentinel → `IllegalArgumentException` |
| **missing capability** | vendor quotes only USD pairs → adapter derives inverse and cross rates |

If an adapter is only renaming, ask whether you need it — but you still want it if
it's the seam that keeps the vendor out of your domain.

---

## Object adapter vs class adapter

| | Object adapter (here) | Class adapter |
|---|---|---|
| How | holds the adaptee (composition) | `extends Adaptee implements Target` |
| Can adapt | the adaptee *and* its subclasses, any instance | one class |
| In Java | the normal choice | rare — single inheritance, couples you to the class |

---

## In the JDK

- `Arrays.asList(array)` — array → `List`
- `InputStreamReader` — byte `InputStream` → char `Reader`
- `Collections.enumeration(collection)` / `Collections.list(enumeration)` — old ↔ new iteration APIs

---

## Adapter vs neighbours

| | Interface | Intent |
|---|---|---|
| **Adapter** | **changes** it | make it fit |
| **Decorator** | keeps it the same | add behaviour |
| **Proxy** | keeps it the same | control access |
| **Facade** | new, **simpler** one over many classes | simplify a subsystem |

Adapter wraps **one** thing to change its shape; Facade fronts **many** to reduce them.

---

## Pitfalls

- **Leaky adapter** — `rate()` returns the vendor's `long`, or throws the vendor's
  exception type. The dialect escaped.
- **Business logic in the adapter** — discounts, rounding rules for invoices. It
  translates; it doesn't decide.
- **Adapting your own code** — if you own both sides, change the interface instead.

---

## Cheat sheet

- Trigger: business code speaking a vendor's/legacy class's dialect
- Fix: a target interface in your terms; one adapter class holds the adaptee and
  translates names, types, units and errors
- Interview line: *"Adapter converts one interface into another the client expects.
  `InputStreamReader` adapts bytes to chars. Prefer object adapters — composition."*
