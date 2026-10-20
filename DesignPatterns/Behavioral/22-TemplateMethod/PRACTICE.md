# 22-TemplateMethod — Session Plan

Goal is not "three print statements in a fixed order" — the course's parser does that.
It's **remove real duplication between sibling classes, pass data between steps, and
make the skeleton impossible to skip**. Different domain on purpose.

---

## Layout

```
22-TemplateMethod/
  templatemethod/problem/Sale.java                region, amount
  templatemethod/problem/CsvReport.java           full skeleton
  templatemethod/problem/MarkdownReport.java      copied skeleton, validation drifted
  templatemethod/problem/TemplateProblemMain.java
  templatemethod/solution/Sale.java
  templatemethod/solution/ReportExporter.java     final export(), private validate, abstract steps, footer hook
  templatemethod/solution/CsvExporter.java  MarkdownExporter.java
  templatemethod/solution/TemplateSolutionMain.java
```

```bash
javac -Xlint:all -d out $(find . -name '*.java') && java -ea -cp out templatemethod.solution.TemplateSolutionMain
```

## Rules for `solution/`

- `export()` is `final`
- validation is `private` in the base — no subclass can skip it
- `header()` and `row()` are `abstract`; `footer()` is a hook with an empty default
- steps exchange data (`row(sale)` returns text, `footer(total)` receives the total)
- output byte-for-byte identical to `problem/` — assert the exact strings
- prove it with `assert`, not prints

---

## 1. Read — 15 min

`NOTES.md` top to bottom.

## 2. Build — ~45 min

| Step | Build | Must prove |
|---|---|---|
| **problem** | CSV + Markdown, each with its own loop and validation | exact outputs, and the drift on a negative amount |
| **solution** | abstract base + two exporters | same exact strings; both reject the same bad input |
| **hook** | `footer(total)` only in Markdown | CSV has no total line, Markdown does |
| **3rd format** | HTML as an anonymous subclass | only steps written — no loop, no validation |

## 3. Break it on purpose

- Remove `final` from `export()` and override it in `MarkdownExporter` without the
  validation. Watch the negative-amount assert fail. Explain in a comment. Revert.
- Rewrite the same thing as Strategy: `ReportFormat` interface passed into a
  `ReportExporter` constructor. Which version would you keep, and why?

## 4. Self-check — 10 min

1. Fixed step vs required step vs hook — how is each declared?
2. Why must the template method be `final`?
3. Template Method vs Strategy — inheritance vs composition, when each?
4. Name a template method in the JDK.
5. What's wrong with calling an abstract step from the base constructor?

## 5. Commit

```bash
git add DesignPatterns/22-TemplateMethod && git commit -m "template method: <step>"
```

---

## Done when

- [x] Watched the course video, did the course exercise
- [ ] Read `NOTES.md`
- [x] problem — written, runs, drift shown
- [x] solution — exact same outputs, same validation everywhere
- [x] hook + 3rd format with only steps
- [ ] broken once (non-final template), reverted
- [ ] All 5 self-check questions answered without looking
- [ ] Committed

Then tick `22 — Template Method` in `../README.md`.

**Stuck?** Ask me for a nudge on the pattern — not the code.
