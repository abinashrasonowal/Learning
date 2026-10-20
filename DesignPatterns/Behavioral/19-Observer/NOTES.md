# Observer

> When one object changes, notify every interested object automatically — without the
> changing object knowing who they are.

Also called publish/subscribe (in-process). Every GUI listener, every event bus, every
reactive stream is this pattern.

---

## The shape

```java
interface Observer { void update(WeatherData data); }
class WeatherStation {                               // the subject
    private final List<Observer> observers = new ArrayList<>();
    void attach(Observer o) { observers.add(o); }
    void detach(Observer o) { observers.remove(o); }
    void setReading(...) { ...; notifyObservers(); }
    private void notifyObservers() { for (Observer o : observers) o.update(...); }
}
```

| Role | Job | Knows about |
|---|---|---|
| **Subject** | holds state, keeps a list, notifies on change | only the `Observer` interface |
| **Observer** | reacts to the change | the subject (or the event), nothing about other observers |

---

## Smell it replaces

```java
class WeatherStation {
    private final PhoneDisplay phone;
    private final LcdDisplay lcd;
    void setTemp(float t) { phone.show(t); lcd.render(t); }   // grows per device
}
```

The subject holds concrete fields for each listener. Adding a third display edits the
station. That's an OCP *and* DIP violation — the high-level thing depends on details.

Observer inverts it: the station depends on an interface; listeners register themselves.

---

## Push vs pull — the design decision that matters

| | Signature | Pro | Con |
|---|---|---|---|
| **Push** | `update(float temp)` | observer needs no reference to subject | add humidity → every observer's signature changes |
| **Push event** | `update(WeatherEvent e)` | add a field to the event, no signature change | event class grows |
| **Pull** | `update(WeatherStation s)` then `s.getTemp()` | observer reads only what it needs | observer coupled to subject's getters |

The reference example uses raw push of one `float` — fine for a demo, brittle the day a
second value shows up. Default to a small immutable event object (a `record`).

---

## The bugs nobody shows in the demo

**1. Detach during notify.** An observer that unsubscribes itself inside `update()`:

```java
public void update(...) { if (done) station.detach(this); }
```

The subject is mid-`for-each` over an `ArrayList` → `ConcurrentModificationException`.
Fix: iterate over a copy (`List.copyOf(observers)`) or use `CopyOnWriteArrayList`.

**2. Duplicate attach.** `attach(o)` twice → `o` notified twice per change. Use a
`LinkedHashSet` (keeps order, rejects duplicates) or check first.

**3. One observer throws.** Exception in observer #2 → observers #3..n never hear about
it. Decide: catch-and-log per observer, or let it propagate. Either is valid — not
deciding is the bug.

**4. Lapsed listener (memory leak).** Subject lives long, observer is short-lived and
never detaches → subject's list keeps it reachable forever. Fix: always detach (e.g.
`attach` returns an `AutoCloseable` subscription), or weak references.

**5. Notify storm / re-entrancy.** An observer that calls back into the subject's
setter → notify inside notify → infinite loop or out-of-order updates.

**6. Order.** Never let correctness depend on notification order — the subject doesn't
promise one.

---

## Observer vs neighbours

| | Difference |
|---|---|
| **Mediator** | observers react independently; a mediator coordinates *between* colleagues with logic |
| **Event bus / pub-sub** | Observer with a middleman — publishers and subscribers don't even know the subject |
| **Strategy** | one strategy, called for its result; observers are many, called for side effects |

---

## Pitfalls

- notifying on every setter, even when the value didn't change → noisy; compare first
- passing the mutable subject in a push event → observers mutate it mid-notify
- `java.util.Observable` — deprecated since Java 9 (a class, not an interface, and not
  thread-safe). Don't use it; write your own or use `PropertyChangeSupport`.

---

## Cheat sheet

- Trigger: one object calls N concrete listeners directly, and N keeps growing
- Fix: subject holds `List<Observer>`, notifies through the interface
- Interview line: *"Observer decouples the source of a change from its consumers —
  one-to-many, push or pull. The hard parts are unsubscribe-during-notify, exceptions
  in a listener, and listener leaks."*
