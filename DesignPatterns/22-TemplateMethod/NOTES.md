# Template Method

> A base class fixes the **order** of an algorithm's steps; subclasses fill in some of
> the steps without changing the order.

"Don't call us, we'll call you" — the base class calls the subclass's steps
(the Hollywood principle), not the other way round.

---

## The shape

```java
abstract class ReportExporter {
    public final String export(List<Sale> s) {       // the template method — final
        validate(s);                                 // fixed step — private
        String out = header();                       // required step — abstract
        for (Sale x : s) out += row(x);
        return out + footer(total);                  // hook — optional, default ""
    }
    protected abstract String header();
    protected abstract String row(Sale s);
    protected String footer(int total) { return ""; }
}
```

Three kinds of step:

| Kind | Declared as | Subclass |
|---|---|---|
| **Fixed** | `private` (or `final`) | can't touch it |
| **Required** | `protected abstract` | must implement |
| **Hook** | `protected` with a default (often empty) | may override |

And the template method itself is **`final`** — that's what keeps the order fixed.

---

## Smell it replaces

Two classes with the same skeleton — validate, loop, total — and only the formatting
lines differ. Copy-paste means the copies **drift**: one gets a bug fix or a
validation, the other doesn't. A third format is a third copy.

---

## The reference example: parsers

`DataParser.parse()` = `openFile()` → `parseData()` → `closeFile()`. Right idea, two
gaps worth knowing:

- `parse()` is `final` ✓, but `openFile()` / `closeFile()` are `protected` and
  non-final, so a subclass can override "fixed" steps. If a step must not change,
  make it `private`.
- the steps take and return nothing — they only print. Real templates **pass data**
  between steps (`row(sale)` returns a string the template appends; `footer(total)`
  receives a value the template computed).

---

## Template Method vs Strategy

| | Template Method | Strategy |
|---|---|---|
| Mechanism | inheritance — override a step | composition — swap the whole object |
| Granularity | varies **part** of an algorithm | varies the **whole** algorithm |
| Chosen | at compile time (which subclass) | at runtime (inject a different one) |
| Cost | one superclass slot used up; subclass coupled to base | an extra interface + object |

Rule of thumb: if the skeleton is stable and only a step or two vary, Template Method.
If you'd want to mix and match steps at runtime, pass them in as strategies (or
lambdas) instead.

---

## In the JDK

- `AbstractList` — implement `get(i)` and `size()`; `iterator()`, `indexOf`,
  `equals`, `hashCode` come free, built on your two steps
- `InputStream.read(byte[], int, int)` — loops over your abstract `read()`
- `HttpServlet.service()` — dispatches to the `doGet` / `doPost` you override

---

## Pitfalls

- **Template method not `final`** — a subclass overrides `export()` and skips
  validation. The pattern is gone.
- **Too many hooks** — ten optional steps and nobody knows which combination works.
  Fewer, well-named hooks.
- **Calling overridable steps from the base constructor** — the subclass's fields
  aren't initialised yet.
- **Deep hierarchies** — `CsvExporter extends BaseExporter extends AbstractExporter`.
  One level of template is usually plenty.

---

## Cheat sheet

- Trigger: sibling classes with the same skeleton and different details
- Fix: `final` template method in an abstract base; `private` fixed steps,
  `abstract` required steps, default-bodied hooks
- Interview line: *"Template Method fixes the algorithm's order in a base class and
  lets subclasses fill in steps — inheritance where Strategy uses composition.
  `AbstractList` is the JDK example."*
