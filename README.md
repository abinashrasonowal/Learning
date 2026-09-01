# Learning

Notes and code practice I keep for revision. Each top-level folder is one subject:
the source material, my notes, and — where the subject has code — runnable examples.

## Contents

| Folder | Subject | Notes | Practice |
|---|---|---|---|
| [DesignPatterns](DesignPatterns/) | SOLID principles, GoF patterns, LLD | per-topic `NOTES.md` | Java, `problem/` + `solution/` per topic |
| [CleanCode](CleanCode/) | Clean Code — Robert C. Martin | `CleanCode.md` | — |
| [ThePragmaticProgrammer](ThePragmaticProgrammer/) | The Pragmatic Programmer, 20th Anniv. Ed. | — | — |

## Conventions

- **Notes are for revision, not transcription.** Smell → why it hurts → the fix →
  the gotcha. If a note just restates the code, it isn't worth rereading.
- **Every code example runs.** No pseudocode. Non-trivial logic leaves an `assert`
  behind so a broken refactor fails loudly.
- **`problem/` and `solution/` packages** side by side, so the wrong version stays
  visible next to the right one — the contrast is the lesson.
- **One commit per topic.** The diff is the revision material.

## Running the Java examples

Java 17 here; no Maven, no Gradle.

```bash
cd DesignPatterns/01-SOLID

# single file, quick
java srp/problem/Invoice.java

# a whole package — compile, then run the driver
javac -Xlint:all -d out $(find . -name '*.java')
java -ea -cp out srp.solution.SRPSolutionMain
```

`-ea` enables the assertions used as self-checks. `out/` is gitignored.

## Progress

- **Design Patterns** — 1 of 24 topics ([progress](DesignPatterns/README.md#progress))
- **Clean Code** — reading
- **The Pragmatic Programmer** — reading
