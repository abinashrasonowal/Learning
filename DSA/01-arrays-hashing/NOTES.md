# 01 — Arrays & Hashing

> Spend memory to stop re-scanning. Every technique here is the same trade: an O(n²)
> loop that keeps asking "have I seen this?" becomes O(n) once something remembers.

The whole topic is one question — **what do I need to remember, and keyed by what?**
Get the key right and the problem collapses.

---

## The four techniques

### 1. Complement lookup — "does its partner exist?"

**Trigger:** "two elements that sum to / differ by / pair with…"

Store what you've *passed*, then ask about what you *need*. The insight is that for
`a + b == target`, the moment you hold `a` you already know `b` — so you never need a
second loop, just a record of everything behind you.

```
for each x:   if (seen contains target - x) -> answer
              seen.put(x, index)            // put AFTER the check, or x pairs with itself
```

**Classic bug:** putting before checking. `[3, 3]`, target 6 works by luck;
`[3, 2]`, target 6 wrongly matches 3 with itself. Check first, then insert.

### 2. Frequency counting — "how many of each?"

**Trigger:** anagram, duplicate, majority, "k most frequent", "appears more than once".

Two ways to count, and the choice is not cosmetic:

```java
int[] count = new int[26];        // lowercase letters ONLY — fastest, no boxing
count[c - 'a']++;

Map<Character,Integer> m ...      // any alphabet, unicode, arbitrary keys
m.merge(c, 1, Integer::sum);      // shorter than getOrDefault + put
```

Reach for `int[26]` when the problem says "lowercase English letters" — it's a hash
map with a perfect hash and no collisions. Reach for `HashMap` the moment that
guarantee isn't in the constraints.

**Anagram grouping:** the key *is* the trick. Either the sorted string
(`O(k log k)` per word) or a length-26 count signature (`O(k)`, but you must turn it
into a `String`/`List` to be usable as a key — arrays hash by identity, see gotchas).

### 3. Prefix sums — "range totals, many queries"

**Trigger:** "sum of subarray", "equal-sum split", "subarray summing to k".

`prefix[i]` = sum of everything before `i`. Then `sum(i..j) = prefix[j+1] - prefix[i]` in
O(1) after O(n) setup. Combine with a hashmap of *seen prefix sums* and "count subarrays
summing to k" becomes one pass: you're back to technique 1, complement on prefix sums.

**Invariant:** `prefix[0] == 0`. That entry isn't padding — it's the empty prefix, and
omitting it is why subarrays starting at index 0 get missed. Seed the map with
`{0: 1}` for the same reason.

**Watch:** `int` prefix sums overflow at ~2·10⁹. If the constraint says
`n ≤ 10⁵` and `|a[i]| ≤ 10⁴`, the max sum is 10⁹ — fits, barely. Bump to `long` the
moment it doesn't; silently wrong beats loudly wrong never.

### 4. Set membership — "is it there?" and in-place marking

**Trigger:** "contains duplicate", "longest consecutive sequence", "missing number".

A `HashSet` turns membership into O(1). The upgrade, when values are bounded by the
array length (`1..n`): **use the array itself as the hash table.** Negate `a[|x|-1]` to
mark "seen", or swap each value to its home index. O(1) extra space, and it's the
answer to "can you do it without extra memory?"

**Longest consecutive sequence** is the one worth internalising: dump everything in a
set, then only start counting upward from `x` when `x-1` is absent. Each element is
visited at most twice, so it's O(n) — not the O(n log n) sort you'd reach for first.

---

## Java gotchas that cost real submissions

| Gotcha | What bites | Do this |
|---|---|---|
| `map.get(k)` returns `null` | unboxing `null` to `int` → **NPE** | `getOrDefault(k, 0)`, or `merge` |
| `Integer == Integer` | true only in the **−128..127** cache; fails silently above | `.equals()`, or compare `int` |
| `int[]` as a map key | arrays use identity hash — two equal arrays are different keys | `Arrays.toString`, or `List<Integer>` |
| `Arrays.sort(int[])` | dual-pivot quicksort → **O(n²)** on adversarial input | `Integer[]` (TimSort) if it's a hostile test |
| `Arrays.asList(intArray)` | gives a `List<int[]>` of size 1 | `Arrays.stream(a).boxed().toList()` |
| `(lo + hi) / 2` | overflows past 2³¹ | `lo + (hi - lo) / 2` |
| string `+=` in a loop | O(n²) copying | `StringBuilder` |
| `HashMap` iteration order | unspecified and *not* insertion order | `LinkedHashMap` when order matters |

`char` arithmetic is your friend: `c - 'a'` gives 0..25, `(char)('a' + i)` goes back.
No map needed for a fixed alphabet.

---

## Complexity

| Technique | Time | Space | What the space holds |
|---|---|---|---|
| Complement lookup | O(n) | O(n) | every value seen so far |
| Frequency count | O(n) | O(k) | k = alphabet size, often 26 — that's O(1) |
| Prefix sums | O(n) build, O(1) query | O(n) | one running total per index |
| Set membership | O(n) | O(n) | the values, deduplicated |
| In-place marking | O(n) | **O(1)** | nothing — the input is the table |

Saying "O(n) space" when it's a 26-element array is a tell. Bounded alphabet is O(1).

---

## Build these (see `../PRACTICE.md` for the rules)

| File | What it must prove |
|---|---|
| `MyHashMap.java` | your own separate-chaining map: `put`/`get`/`remove`, resize at load factor 0.75. Assert a collision (two keys, same bucket) is still retrievable. |
| `TwoSum.java` | one pass. Assert on `[3,3]` and on "no answer exists". |
| `GroupAnagrams.java` | both keys — sorted-string and count-signature. Assert they agree. |
| `SubarraySumK.java` | prefix map. Assert the `{0:1}` seed matters by removing it and watching a case fail. |
| `LongestConsecutive.java` | O(n) set version. Assert on duplicates and on `[]`. |
| `MissingNumber.java` | in-place / XOR, **O(1) space**. Assert the missing element is `0` and is `n`. |

Write `MyHashMap` first. Everything else in this repo uses `java.util.HashMap`
afterwards, guilt-free — but once you've handled a collision by hand, "amortised O(1)"
stops being a phrase you repeat and becomes a thing you've seen.

---

## Self-check

1. Why must the insert come *after* the complement check?
2. When is `int[26]` strictly better than `HashMap`, and what in the constraints tells you?
3. Why does the prefix-sum map start at `{0: 1}`?
4. `HashMap` is O(1) amortised — what's the worst case, and what does Java do at 8
   entries in one bucket?
5. Which problem here is O(1) space, and what makes that possible?
