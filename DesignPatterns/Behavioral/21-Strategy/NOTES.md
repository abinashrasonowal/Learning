# Strategy

> Define a family of algorithms, put each one behind a common interface, and let the
> caller pick one at runtime.

You already wrote this — `../../01-SOLID/ocp/solution` *is* Strategy. OCP is the principle,
Strategy is the shape it most often takes.

---

## The shape

```java
interface PaymentStrategy { void pay(double amount); }   // the varying part
class UPI implements PaymentStrategy { ... }              // concrete strategies
class Checkout {                                          // the context
    private final PaymentStrategy strategy;
    Checkout(PaymentStrategy s) { this.strategy = s; }
    void complete(double amt) { strategy.pay(amt); }
}
```

Three roles:

| Role | Job | Knows about |
|---|---|---|
| **Strategy** | interface for the algorithm | nothing |
| **Concrete strategy** | one way of doing it | its own data only |
| **Context** | uses *a* strategy, owns the workflow around it | the interface, never a concrete class |

The context decides **when** the algorithm runs. The strategy decides **how**.

---

## Smell it replaces

```java
if (method.equals("UPI")) { ... } else if (method.equals("Card")) { ... } else ...
```

A conditional on a *type* that grows with the business. Every new branch edits, and
re-tests, a class that already worked.

---

## The `if/else` doesn't vanish — it moves

Someone still turns the string `"UPI"` from a request into `new UPI(...)`. Strategy
pushes that choice **to the edge of the program** (the `main`, a config, a
`Map<String, PaymentStrategy>` registry, or a Factory — pattern 03). The context is
clean; selection lives in one place instead of being smeared through business code.

If the selection logic and the algorithm live in the same class, you haven't applied
Strategy, you've just renamed the if/else.

---

## Constructor vs setter injection

The reference example uses `setPaymentStrategy(...)`, then `pay()`. Call `pay()` first
and you get an NPE — the object was constructible in an invalid state.

- **Constructor** (default) — context is always valid. Use when one strategy per
  context-lifetime is enough.
- **Setter** — only when the strategy genuinely changes mid-life (a game AI switching
  difficulty). Then guard it: non-null default, or `Objects.requireNonNull`.
- **Method parameter** — `checkout.complete(amt, strategy)`. Best when every call can
  differ; the context holds no strategy at all.

---

## Lambdas

A one-method strategy interface is a functional interface. In Java 8+:

```java
Checkout c = new Checkout(amt -> System.out.println("cash " + amt));
```

and `java.util.Comparator` is the strategy you've used a hundred times without the name.

**Keep a class when** the strategy carries state (card number, UPI handle, a discount
percent) or is long enough to deserve its own test. **Use a lambda when** it's a
one-liner with no state.

---

## Strategy vs neighbours

| | Looks like | Difference |
|---|---|---|
| **State** | same class diagram | states swap *themselves* (`ctx.setState(next)`); strategies are chosen from outside and don't know each other |
| **Command** | interface + `execute()` | command is a *request* to store/queue/undo; strategy is an *algorithm* the context calls as part of its own work |
| **Template Method** | varying algorithm | inheritance (override a step) vs composition (swap the whole object) |

---

## Pitfalls

- **Leaky interface** — `pay(double amount, String cardNo, String upiId)` because each
  strategy needed a different input. Put per-strategy data in the strategy's
  constructor; the interface takes only what *every* strategy needs.
- **One strategy** — an interface with a single implementation is ceremony. Wait for
  the second one.
- **Context peeking** — `if (strategy instanceof UPI)` inside the context. The whole
  point is the context doesn't know.

---

## Cheat sheet

- Trigger: `switch`/`if` on a type that keeps growing
- Fix: interface for the algorithm, inject it, choose at the edge
- Interview line: *"Strategy is OCP made concrete — composition over inheritance for
  swapping behaviour at runtime. `Comparator` is the JDK example."*
