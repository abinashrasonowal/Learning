# Mediator

> Objects stop talking to each other directly. They talk to one mediator, and the
> mediator decides who hears what.

Many-to-many becomes many-to-one. The coordination rule that was smeared across every
colleague now lives in one class.

---

## The shape

```java
interface Tower { void requestLanding(Aircraft a); void runwayVacated(Aircraft a); }
class ControlTower implements Tower { ... runway state, holding queue ... }   // the mediator
class Aircraft {                                                              // a colleague
    private final Tower tower;
    void requestLanding() { tower.requestLanding(this); }
    void clearedToLand()  { ... }      // called back by the tower
}
```

| Role | Job | Knows about |
|---|---|---|
| **Mediator** | holds the coordination rule, routes requests | every colleague |
| **Colleague** | does its own work, reports to / is told by the mediator | the mediator only — never another colleague |

---

## Smell it replaces

```java
class Aircraft {
    List<Aircraft> peers;                    // everyone knows everyone
    boolean requestLanding() { for (Aircraft p : peers) if (p.onRunway) return false; ... }
}
```

- **n(n-1)/2 links** — 4 aircraft need 6 two-way links; forget one and two land at once
- the rule ("runway must be free") is copied into every colleague
- a change in the rule (two runways, priority for emergencies) edits every colleague
- no one *tells* the waiting aircraft — they poll

---

## The reference example: chat room

`ChatRoom` relays a message to everyone except the sender. Good first picture, but the
mediator holds **no rule** — it's just a broadcast loop, which makes it look like
Observer. The pattern earns its keep when the mediator **decides** something: who
gets the runway, which form field is enabled, who's next in the queue.

Also note its two-step setup: `new ChatUser("Amit", room)` **and** `room.addUser(amit)`.
Forget the second and Amit can send but never receives — silently. If setup has two
steps, make the mediator reject the half-registered case loudly.

---

## Mediator vs Observer

| | Observer | Mediator |
|---|---|---|
| Direction | one subject → many listeners | many ↔ one ↔ many |
| Who has the logic | listeners decide what to do | mediator decides who does what |
| Do colleagues know each other? | listeners don't know each other | colleagues don't know each other |
| Typical | event bus, GUI listener | air-traffic control, dialog box wiring, chat server |

A mediator is often *implemented* with observer-style callbacks — they combine.

---

## Mediator vs Facade

Facade simplifies a subsystem for **outside** callers; the subsystem doesn't know the
facade exists. Mediator coordinates **inside** — colleagues know and call it.

---

## Pitfalls

- **God mediator** — every rule in the system ends up in one class. Split by concern:
  one mediator per dialog / per runway group, not one per app.
- **Colleagues reaching around** — `tower.getAircraft("6E-202").hold()`. Now they
  know each other again.
- **Public callbacks** — if `clearedToLand()` is public, any caller can put a plane on
  the runway. Keep the mediator-only methods package-private.

---

## Cheat sheet

- Trigger: objects holding lists of peers; one rule re-implemented in each of them
- Fix: one mediator owns the rule; colleagues hold only the mediator
- Interview line: *"Mediator turns many-to-many coupling into many-to-one. Air traffic
  control: planes talk to the tower, never to each other."*
