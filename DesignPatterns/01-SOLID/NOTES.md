# SOLID Principles

Five rules for structuring classes so changes stay local. Not patterns — the
*reasons* the patterns exist. Almost every GoF pattern is one of these applied.

Run: `java SRP.java` (and OCP / LSP / ISP / DIP).

---

## S — Single Responsibility Principle

> A class should have one, and only one, reason to change.

**Smell:** a class name with "and" in its description. `Invoice` that calculates
totals *and* writes to the DB *and* sends email.

**Why it hurts:** the DBA changing the schema forces a change to the same file the
accountant's tax-rule change touches. Merge conflicts, wide blast radius, untestable
(you can't unit-test the calculation without a database).

**Fix:** split by *actor* — who requests the change.
`Invoice` (business rules) / `InvoiceRepository` (persistence) / `EmailService` (notification).

**Careful:** srp.srp is not "one method per class". Responsibility = reason to change,
not amount of code.

---

## O — Open/Closed Principle

> Open for extension, closed for modification.

**Smell:** an `if/else if` or `switch` on a type string that grows every time the
business adds an option. `if (method.equals("CreditCard")) ... else if ("Paypal")`.

**Why it hurts:** adding UPI means editing, recompiling and re-testing a class that
already worked. Every edit risks breaking the existing branches.

**Fix:** make the varying part a polymorphic interface. `PaymentMethod` depends on
`PaymentProcessor`; adding UPI = adding a new file, zero edits to tested code.

**This is literally the Strategy pattern.** Also Factory, State, Visitor.

**Careful:** you can't be open to *every* axis of change. Pick the axis that actually
varies in your domain. Guessing wrong = speculative abstraction (worse than the if/else).

---

## L — Liskov Substitution Principle

> A subtype must be usable anywhere its base type is expected, without surprises.

**Smell:** an override that `throw new UnsupportedOperationException(...)`, or that
tightens what the caller is allowed to pass, or silently does nothing.

**Why it hurts:** `ReadOnlyFile extends File` compiles fine, then blows up at runtime
the moment someone calls `write()` through a `File` reference. The type system lied.

**Fix:** the hierarchy was wrong, not the subclass. Don't model "read-only file" as a
crippled file — model the *capabilities*: `CanRead` and `CanWrite` interfaces, and let
each class implement only what it truly supports. The compiler now stops the mistake.

**Rule of thumb:** inheritance is for "is-substitutable-for", not "is-kind-of".
A `Square` is a kind of `Rectangle` in geometry, but not substitutable for one in code
(setWidth on a Square breaks a caller's assumption that height is unchanged).

---

## I — Interface Segregation Principle

> No client should be forced to depend on methods it does not use.

**Smell:** implementing an interface and stubbing half the methods with exceptions.
`SimplePrinter implements Machine` → forced to implement `scan()` and `copy()`.

**Why it hurts:** the fat interface couples `SimplePrinter` to scanning. Add `fax()` to
`Machine` and every implementor must be touched, even ones that will never fax.

**Fix:** split the fat interface into role interfaces — `Printer`, `Scanner`, `Copier`.
A device implements exactly the roles it has. `MultiPurposeMachine` implements all three.

**Relation to LSP:** ISP violations are how you *end up* with LSP violations. Fat
interface → stub throwing methods → subtype not substitutable. Fix ISP, LSP follows.

---

## D — Dependency Inversion Principle

> Depend on abstractions, not concretions. High-level policy must not depend on
> low-level detail — both should depend on an interface.

**Smell:** `new` of a concrete service inside a constructor.
`NotificationService` doing `this.emailService = new EmailService()`.

**Why it hurts:** `NotificationService` is now welded to email. You can't test it
without sending email, can't swap in a push channel, and adding a channel means
editing the high-level class (also an OCP violation).

**Fix:** invert the arrow. Define `NotificationChannel` *owned by the high-level module*,
have `EmailService`/`SMSService` implement it, and inject through the constructor.
The detail now depends on the policy — the dependency direction is inverted.

**Not the same as Dependency Injection.** DI is the mechanism (pass it in); DIP is the
goal (point at an abstraction). You can inject a concrete class and still violate DIP.

---

## Cheat sheet

| | One-line trigger | Fix |
|---|---|---|
| srp.srp | class serves two different actors | split by reason-to-change |
| OCP | `switch` on type that keeps growing | polymorphic interface |
| LSP | override throws / weakens contract | remodel hierarchy by capability |
| ISP | implementor stubs unused methods | split into role interfaces |
| DIP | `new ConcreteThing()` in high-level code | inject an interface |

**Interview framing:** srp.srp and ISP are about *cohesion* (keep related things together,
unrelated things apart). OCP, LSP and DIP are about *coupling* (depend on stable
abstractions so change doesn't propagate).

**Counterweight:** every principle applied without judgement produces a maze of
one-method interfaces. Apply when a second reason to change actually shows up, not
before. Two implementations justify an interface; one does not.
