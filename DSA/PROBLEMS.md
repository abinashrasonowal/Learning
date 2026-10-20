# Problem log

Filed by **pattern**, not by source or difficulty — the pattern is the thing that
transfers to a problem you haven't seen. Newest first within each section.

**One rule:** the `Insight` column holds *why the trick works*, not what it is.
"Used a hashmap" is worthless in a month. "The complement is knowable before the
second number is seen, so one pass suffices" is the thing you'll actually reuse.

Status: `solved` (cold, unaided) · `hinted` (looked something up — rewrite tomorrow) ·
`retained` (cleared +30, stop tracking)

---

## How to add a row

```markdown
| [Two Sum](https://leetcode.com/problems/two-sum/) | 2026-09-08 | hash complement | O(n) / O(n) | Complement is knowable before its partner is seen → one pass, not two | solved | 2026-09-09 |
```

Copy the row, change the fields. Keep the date format `YYYY-MM-DD` so `sort` works.

---

## Arrays & hashing

| Problem | Date | Technique | Time / Space | Insight | Status | Revisit |
|---|---|---|---|---|---|---|
| | | | | | | |

## Two pointers

| Problem | Date | Technique | Time / Space | Insight | Status | Revisit |
|---|---|---|---|---|---|---|
| | | | | | | |

## Sliding window

| Problem | Date | Technique | Time / Space | Insight | Status | Revisit |
|---|---|---|---|---|---|---|
| | | | | | | |

## Stack & queue

| Problem | Date | Technique | Time / Space | Insight | Status | Revisit |
|---|---|---|---|---|---|---|
| | | | | | | |

## Binary search

| Problem | Date | Technique | Time / Space | Insight | Status | Revisit |
|---|---|---|---|---|---|---|
| | | | | | | |

## Linked list

| Problem | Date | Technique | Time / Space | Insight | Status | Revisit |
|---|---|---|---|---|---|---|
| | | | | | | |

## Trees & BST

| Problem | Date | Technique | Time / Space | Insight | Status | Revisit |
|---|---|---|---|---|---|---|
| | | | | | | |

## Heaps & intervals

| Problem | Date | Technique | Time / Space | Insight | Status | Revisit |
|---|---|---|---|---|---|---|
| | | | | | | |

## Graphs

| Problem | Date | Technique | Time / Space | Insight | Status | Revisit |
|---|---|---|---|---|---|---|
| | | | | | | |

## Dynamic programming

| Problem | Date | Technique | Time / Space | Insight | Status | Revisit |
|---|---|---|---|---|---|---|
| | | | | | | |

## Backtracking

| Problem | Date | Technique | Time / Space | Insight | Status | Revisit |
|---|---|---|---|---|---|---|
| | | | | | | |

## Greedy & bits

| Problem | Date | Technique | Time / Space | Insight | Status | Revisit |
|---|---|---|---|---|---|---|
| | | | | | | |

---

## Due for revisit

Cheapest way to find what's due — no tooling, no script:

```bash
grep -oE '\| 20[0-9]{2}-[0-9]{2}-[0-9]{2} \|$' PROBLEMS.md | sort   # revisit dates, oldest first
grep -c 'solved' PROBLEMS.md                                        # count cold solves
grep 'hinted' PROBLEMS.md                                           # owe a rewrite
```

Anything in `hinted` that's older than two days is a lie you're telling yourself.

## Stuck-on list

Problems that beat you twice. Not failures — the syllabus.

| Problem | Pattern | What specifically blocks me | Attempts |
|---|---|---|---|
| | | | |
