# Design Patterns — Notes & Practice

Following the [prateek/design-patterns-java](https://github.com/prateek27/design-patterns-java)
curriculum. Java 17, no build tool.

## Layout

One folder per topic. Inside it, `NOTES.md` for revision plus a `problem/` and a
`solution/` package — the violation and the fix side by side, each with its own
driver class.

```
01-SOLID/
  NOTES.md
  PRACTICE.md
  srp/problem/Invoice.java  srp/problem/SRPProblemMain.java
  srp/solution/Invoice.java  srp/solution/DatabaseRepository.java
  srp/solution/EmailService.java  srp/solution/SRPSolutionMain.java
```

## Running

```bash
java srp/problem/Invoice.java                              # single file

javac -Xlint:all -d out $(find . -name '*.java')           # whole topic
java -ea -cp out srp.solution.SRPSolutionMain
```

## Progress

### Foundations
- [ ] 01 — SOLID Principles — SRP done, OCP/LSP/ISP/DIP to go

### Creational
- [ ] 02 — Singleton
- [ ] 03 — Factory
- [ ] 04 — Abstract Factory
- [ ] 05 — Builder
- [ ] 06 — Prototype

### Structural
- [ ] 07 — Adapter
- [ ] 08 — Bridge
- [ ] 09 — Composite
- [ ] 10 — Decorator
- [ ] 11 — Facade
- [ ] 12 — Flyweight
- [ ] 13 — Proxy

### Behavioural
- [ ] 14 — Chain of Responsibility
- [ ] 15 — Command
- [ ] 16 — Iterator
- [ ] 17 — Mediator
- [ ] 18 — Memento
- [ ] 19 — Observer
- [ ] 20 — State
- [ ] 21 — Strategy
- [ ] 22 — Template Method
- [ ] 23 — Visitor

### Apply
- [ ] 24 — LLD Project: Ride Sharing App
