# State

> An object changes its behaviour when its internal state changes — as if it changed
> class. Each state is its own object, and the states decide the transitions.

---

## The shape

```java
interface OrderState {
    default OrderState pay(Order o)  { throw illegal("pay"); }    // illegal by default
    default OrderState ship(Order o) { throw illegal("ship"); }
}
class Created implements OrderState { public OrderState pay(Order o) { return new Paid(); } }
class Order {                                    // the context
    private OrderState state = new Created();
    void pay() { state = state.pay(this); }      // forward, keep what comes back
}
```

| Role | Job | Knows about |
|---|---|---|
| **Context** | public API, holds current state, forwards every call | the state interface |
| **State** | behaviour for one state, **picks the next state** | the context, and the states it can go to |

---

## Smell it replaces

```java
void ship() {
    switch (status) {
        case PAID -> status = SHIPPED;
        case CREATED -> throw ...;
        ...
    }
}
// ...and the same switch in pay(), deliver(), cancel()
```

- one switch **per action**, each listing **every** status — states × actions cells
- a new status edits every switch
- a missed case in a switch *statement* compiles and silently does nothing

---

## The reference example is Strategy, not State

The course's `DirectionService` swaps `Walking` / `Car` / `Train` from **outside**
(`setTransportationMode(...)`), and the modes never transition to each other. That's
exactly the Strategy class diagram — see `21-Strategy/NOTES.md`.

What makes it **State**:

- the **state** chooses the next state (`Created.pay()` returns `Paid`)
- the client never calls `setState` — there's no public setter at all
- the same call (`cancel()`) does different things depending on where you are

| | Strategy | State |
|---|---|---|
| Who swaps it | client, from outside | the states themselves |
| Do they know each other? | no | yes — each knows its successors |
| Typical | pricing, sorting, routing | lifecycles, protocols, vending machine, TCP connection |

---

## Two ways to transition

| | How | Pro | Con |
|---|---|---|---|
| **Return next state** (here) | `state = state.pay(this)` | no setter on the context; states can be stateless/shared | every method returns something |
| **Context setter** (GoF) | `ctx.setState(new Paid())` inside the state | methods are `void` | the setter must be reachable by states — keep it package-private |

---

## Illegal by default

Put a throwing `default` on every action in the interface. A state overrides only
what's legal from it; terminal states (`Delivered`, `Cancelled`) override nothing.
The silent missed case from the switch becomes a loud `IllegalStateException`.

Adding a **new action** = one interface default + overrides only where it's legal.
Adding a **new state** = one class + edit the states that transition into it.

---

## When an enum is enough

For a small, stable machine, an `enum` + a switch **expression** is fine — switch
expressions must be exhaustive, so a missed case is a compile error:

```java
status = switch (status) { case PAID -> SHIPPED; case CREATED, SHIPPED, ... -> throw ...; };
```

Reach for State classes when states carry **behaviour or data** (the refund lives in
`Paid`) or when the switches start multiplying.

---

## Pitfalls

- **Public `setState`** — any caller can jump `Created → Delivered`; the machine has
  no guarantees left.
- **Transition logic in the context** — `if (state instanceof Paid) state = new Shipped()`.
  That's the switch again.
- **States holding per-order data** — keep data on the context; then states can be
  shared singletons (or enum constants).

---

## Cheat sheet

- Trigger: the same `switch (status)` repeated in every method
- Fix: one class per state, illegal-by-default actions, states return their successor
- Interview line: *"State is Strategy where the strategies swap themselves. Each state
  owns its transitions, so illegal moves are rejected in one place."*
