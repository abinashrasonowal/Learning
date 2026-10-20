# Command

> Turn a request into an object — so it can be passed around, stored, queued, logged,
> and undone.

The reference example (`Button` + `BoldCommand`) shows the decoupling, but stops before
the reason the pattern exists. Without **undo / history / queue**, Command is just
Strategy for a button.

---

## The shape

```java
interface Command { void execute(); void undo(); }

class BoldCommand implements Command {            // concrete command
    private final Document doc;                   // the receiver
    private final int start, end;                 // the request's parameters, captured
    public void execute() { doc.bold(start, end); }
    public void undo()    { doc.unbold(start, end); }
}

class Editor {                                    // the invoker
    private final Deque<Command> history = new ArrayDeque<>();
    void run(Command c) { c.execute(); history.push(c); }
    void undo()         { if (!history.isEmpty()) history.pop().undo(); }
}
```

| Role | Job |
|---|---|
| **Command** | interface: `execute()` (+ `undo()`) |
| **Concrete command** | binds a receiver + the arguments; knows *which* receiver method to call |
| **Receiver** | does the real work (`Document`, `TextEditor`, `Light`) — knows nothing about commands |
| **Invoker** | triggers commands, and keeps history — `Button`, `Editor`, a job queue |
| **Client** | builds commands and hands them to the invoker (`main`) |

---

## Smell it replaces

```java
class BoldButton   { void click() { editor.bold(); } }
class ItalicButton { void click() { editor.italic(); } }
```

One UI class per action, each welded to the editor. And there's nowhere to record
*what happened*, so undo is impossible.

---

## What making a request an object buys you

| Capability | How |
|---|---|
| **Undo** | each command reverses itself; invoker pops a stack |
| **Redo** | second stack; a *new* command clears it |
| **Macro** | `MacroCommand` holds `List<Command>`, executes in order, undoes in **reverse** |
| **Queue / schedule** | `ExecutorService.submit(runnable)` — a `Runnable` *is* a command |
| **Log / replay** | persist commands; replay them to rebuild state (event sourcing, DB redo logs) |
| **Same action, many triggers** | menu item, toolbar button and Ctrl+B share one command |

---

## Undo is the hard part

- **Capture state at execute time, not construction time.** A `DeleteText` command must
  remember *what* it deleted when it runs — it didn't exist before.
- **Some actions aren't naturally reversible** (`setColor` — what was the old colour?).
  Store the previous value inside the command when `execute()` runs.
- **Alternative: Memento** (pattern 18) — snapshot the receiver before executing; undo
  restores the snapshot. Simpler to get right, heavier on memory.
- **Redo stack is invalidated** by any new command after an undo. Forgetting this is
  the classic bug.

---

## Command vs neighbours

| | Difference |
|---|---|
| **Strategy** | strategy = *how* the context does its own job, called for a result. Command = *a request* the invoker can store, delay, undo. The context owns a strategy; an invoker owns a history of commands. |
| **Memento** | memento stores *state*; command stores *an action*. Often combined for undo. |
| **Chain of Responsibility** | command goes to a known receiver; a chain finds a handler |

---

## Pitfalls

- **Logic in the command** — `BoldCommand.execute()` that does the bolding itself
  instead of calling the receiver. Now commands and receiver duplicate behaviour.
- **Invoker calling the receiver** — `if (cmd instanceof Bold) doc.bold()`. The invoker
  must know only `Command`.
- **Reusing a stateful command instance** — executing the same `DeleteText` object
  twice overwrites the text it saved for undo. One execution, one instance.
- The reference `main` calls `setCommand` twice before `click()` — the first command is
  silently discarded. Fine in a demo, but notice it.

---

## Cheat sheet

- Trigger: you need undo, queueing, logging, or several triggers for one action
- Fix: wrap each request in an object with `execute()`/`undo()`; invoker keeps a stack
- Interview line: *"Command encapsulates a request as an object, decoupling who triggers
  it from who performs it. That's what makes undo/redo, macros and job queues possible.
  `Runnable` is the JDK's command."*
