# Abstract Factory

> An interface for creating a **family** of related objects, so the objects you get
> always belong together — without naming their concrete classes.

Factory answers "which class?". Abstract Factory answers "which **set** of classes?" —
and makes it impossible to mix sets.

---

## The shape

```java
interface Charger  { String charge(int amount); }          // abstract products
interface Refunder { String refund(String chargeId); }
interface PaymentProvider {                                  // the abstract factory
    Charger charger();
    Refunder refunder();
}
class StripeProvider implements PaymentProvider { ... }      // one concrete factory per family
class Checkout {
    Checkout(PaymentProvider p) { charger = p.charger(); refunder = p.refunder(); }
}
```

| Role | Job | Knows about |
|---|---|---|
| **Abstract factory** | one create method per product | the product interfaces |
| **Concrete factory** | builds one family's products | that family's concrete classes |
| **Abstract product** | what the client uses | nothing |
| **Client** | takes *one* factory, pulls every product from it | interfaces only |

---

## Smell it replaces

```java
private final StripeCharger charger = new StripeCharger();
private final RazorpayRefunder refunder = new RazorpayRefunder();   // wrong family
```

Each product picked separately, by hand. Nothing ties them together, so a migration
that changes one and forgets the other compiles and fails at runtime — a Stripe charge
sent to the Razorpay refunder.

The fix isn't "add interfaces" — that alone still lets you pass a Stripe charger and a
Razorpay refunder. The fix is **take one factory, get both from it**.

---

## Making mixing impossible

- client constructor takes the **factory**, not the products
- concrete products are **package-private** — outside the package, the factory is the
  only way to get one
- choose the factory once, at the edge (config, `main`, DI)

---

## The reference example: Windows / Mac UI

Classic and correct: `UIFactory` → `createButton()`, `createScrollBar()`. Note its
`main` builds a `WindowsFactory`, never uses it, and runs the app with `MacOSFactory` —
proof of the pattern's point, actually: switching the whole UI is one argument.

UI themes are the textbook case. Real-world families show up wherever products must
share a backend: payment providers, cloud SDKs (AWS client + AWS signer), database
drivers.

---

## In the JDK

- **`java.sql.Connection`** — `createStatement()`, `prepareStatement()`,
  `createBlob()`: every object comes from the same driver as the connection
- `DocumentBuilderFactory`, `TransformerFactory` — pick an XML implementation family
- `java.awt.Toolkit` — native peers for one platform

---

## The weakness: new products

Adding a **family** (PayPal) = one new factory + its products. Easy, OCP-friendly.

Adding a **product** (`Payouts`) = a new method on the factory interface → **every**
concrete factory must change. The product set is the rigid axis. Pick this pattern when
families grow and the product list is stable.

---

## Abstract Factory vs Factory Method

| | Factory Method | Abstract Factory |
|---|---|---|
| Creates | one product | a family of products |
| Mechanism | subclass overrides `createX()` | client is given a factory object |
| Relationship | — | each method *of* an abstract factory is often a factory method |

---

## Pitfalls

- **Client takes products, not the factory** — mixing is possible again.
- **Public concrete products** — someone writes `new StripeCharger()` and bypasses it.
- **One family** — an abstract factory with a single implementation is ceremony; wait
  for the second.
- **`create(String type)` on the abstract factory** — that's a simple factory hiding
  inside; you've lost the per-product type safety.

---

## Cheat sheet

- Trigger: several objects that must come from the same "family", picked separately
- Fix: one factory interface per family, client takes the factory and pulls all
  products from it
- Interview line: *"Abstract Factory guarantees a consistent family. JDBC's
  `Connection` is one — every `Statement` it creates belongs to the same driver.
  Easy to add families, hard to add products."*
