# Facade

> One simple entry point in front of a subsystem of many classes — so callers make one
> call instead of knowing the order, the rules and the undo steps of five.

---

## The shape

```java
class OrderFacade {
    OrderFacade(Inventory i, PaymentService p, ShippingService s, Notifier n) { ... }
    String placeOrder(OrderRequest o) {
        reserve → charge → ship → notify, with release/refund on failure
    }
}
```

| Role | Job | Knows about |
|---|---|---|
| **Facade** | a few high-level operations; orchestrates the subsystem | every subsystem class it uses |
| **Subsystem classes** | do the real work | each other as needed — **not** the facade |
| **Client** | calls the facade | the facade (and may still use subsystems directly) |

---

## Smell it replaces

Two controllers (web, mobile) each calling `reserve → charge → ship → notify` in the
right order, with the right compensation on each failure. The copies drift — mobile
forgot to release stock when the card is declined, so books vanish with no order
behind them. A new step (fraud check) means editing every copy.

The knowledge being duplicated is **how to use the subsystem correctly**. The facade
is the one place that knowledge lives.

---

## The reference example: API gateway

`APIGateway.getFullOrderDetails(...)` calls three services and joins the strings.
Right idea, but the three calls are independent — nothing has to happen in order and
nothing has to be undone, so the facade is only saving three lines. It earns its keep
when the subsystem has **ordering** and **failure rules** that callers keep getting
wrong.

Also: it `new`s its services in its constructor. Inject them, so tests can pass fakes
and the facade doesn't decide which implementations exist.

---

## What a facade is *not*

- **Not a wall.** Clients may still use `Inventory` directly; the facade is a
  convenience, not access control. (Hiding is a module/package decision.)
- **Not new behaviour.** It orchestrates existing operations; business rules stay in
  the subsystem classes.
- **Not one per class.** One facade per use-case area — `OrderFacade`, not
  `InventoryFacade` wrapping one class (that's just indirection).

---

## In the wild

- an **API gateway / BFF** in microservices — the course's framing
- `javax.faces.context.FacesContext`, Spring's `JdbcTemplate` (connection + statement +
  result set + cleanup behind `query(...)`)
- a service-layer class in a layered app is usually a facade over repositories

---

## Facade vs neighbours

| | Difference |
|---|---|
| **Adapter** | wraps **one** class to change its interface; facade fronts **many** to simplify |
| **Mediator** | colleagues know and talk *through* the mediator; subsystem classes don't know the facade exists |
| **Abstract Factory** | can be used *by* a facade to create subsystem objects |

---

## Pitfalls

- **God facade** — every operation in the app on one class. Split by use case.
- **Leaking subsystem types** — `placeOrder` returning `PaymentService.Txn`. The client
  is coupled to the subsystem again.
- **Facade with logic** — tax, discount rules creeping into the facade. Push them back
  into the subsystem.

---

## Cheat sheet

- Trigger: several callers repeating the same multi-step, order-sensitive use of a
  subsystem
- Fix: one facade method per use case; subsystems injected; ordering and compensation
  written once
- Interview line: *"Facade gives a simple interface over a complex subsystem. Unlike
  Mediator, the subsystem doesn't know it exists; unlike Adapter, it simplifies many
  classes rather than converting one."*
