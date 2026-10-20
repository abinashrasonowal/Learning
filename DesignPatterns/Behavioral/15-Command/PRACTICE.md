# 15-Command — Session Plan

Goal is not "wrap a method call in a class". It's **build working undo/redo and a macro,
and prove the state is right after every step**. If your solution has no undo, it's
Strategy wearing a different hat. You write all the code.

---

## Layout

```
15-Command/
  command/problem/Document.java              StringBuilder text; append(), delete(n), toUpperCase()
  command/problem/AppendButton.java  DeleteButton.java  UpperButton.java   one class per action
  command/problem/CommandProblemMain.java
  command/solution/Document.java             the receiver — same as problem, knows nothing of commands
  command/solution/Command.java              execute() + undo()
  command/solution/AppendCommand.java
  command/solution/DeleteCommand.java        must remember what it deleted
  command/solution/UpperCaseCommand.java     must remember the text before
  command/solution/MacroCommand.java         List<Command>; undo in reverse
  command/solution/Editor.java               the invoker — undo and redo stacks
  command/solution/CommandSolutionMain.java
```

```bash
javac -Xlint:all -d out $(find . -name '*.java') && java -ea -cp out command.solution.CommandSolutionMain
```

## Rules for `solution/`

- `Document` has **no** undo logic and imports nothing from commands
- `Editor` knows only the `Command` interface — no `instanceof`, no receiver calls
- commands **call** the receiver; they don't reimplement its logic
- undo state is captured in `execute()`, not in the constructor
- a new command after an undo **clears the redo stack**
- undo/redo on an empty stack is a no-op, not an exception
- every step proved with `assert doc.text().equals("...")`

---

## 1. Read — 15 min

`NOTES.md`, especially "Undo is the hard part".

## 2. Build — ~60 min

| Step | Build | Must prove |
|---|---|---|
| **problem** | three button classes, each calling `Document` directly | works — then try to add undo. Notice you can't: nothing remembers what happened. |
| **solution, execute only** | `Command`, three commands, `Editor.run()` | same text as the problem driver |
| **undo** | `undo()` on each command, history stack | `append "hello"`, `append " world"`, `delete 6`, undo ×3 → `""` at each step asserted |
| **redo** | second stack | undo 2, redo 1, then run a new command → redo is empty (assert it) |
| **macro** | `MacroCommand("shout")` = append `"!"` then uppercase | one `undo()` reverts both, in reverse order |
| **Runnable** | submit a command to an `ExecutorService` as `cmd::execute` | you've just used Command as a job queue |

## 3. Break it on purpose

- Capture the deleted text in `DeleteCommand`'s **constructor** instead of `execute()`.
  Run delete → append → undo → undo. Watch the text come out wrong. Revert.
- Run the **same** `DeleteCommand` instance twice, then undo twice. Explain in a
  comment why one instance per execution matters.
- Make `MacroCommand.undo()` undo in forward order. Find a sequence that breaks.

## 4. Self-check — 10 min

1. Command vs Strategy — same shape, what's the difference in intent?
2. Why must undo state be captured in `execute()`?
3. Why does a new command clear the redo stack?
4. Why does a macro undo in reverse?
5. Command + Memento — when would you use Memento for undo instead?
6. Name the command interface in the JDK.

## 5. Commit

```bash
git add DesignPatterns/15-Command && git commit -m "command: <step>"
```

---

## Done when

- [ ] Read `NOTES.md`
- [x] problem — written, runs, tried (and failed) to add undo
- [x] solution — execute works, asserts match problem
- [x] undo + redo, every step asserted
- [x] macro undoes in reverse
- [ ] broken once (constructor capture), reverted
- [ ] All 6 self-check questions answered without looking
- [ ] Committed

Then tick `15 — Command` in `../README.md`.

**Stuck?** Ask me for a nudge on the pattern — not the code.
