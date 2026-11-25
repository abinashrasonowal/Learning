# Red-Black Tree

> A binary search tree that paints every node red or black and keeps two colour rules.
> The rules cap the longest path at twice the shortest, so the height is
> ≤ 2·log₂(n+1) and every operation is **O(log n) worst case**. Insert and delete repair
> the rules with recolouring and **at most 2 / 3 rotations**.

How Java uses one — `Entry` layout, `compare`, iteration, `floor`/`ceiling`, views:
**[TREEMAP-INTERNALS.md](../01-arrays-hashing/TREEMAP-INTERNALS.md)**. This file is the
data structure on its own: rules, proofs, every fix-up case, code.

**Part I — why it works**
1. The problem — BSTs degrade
2. The rules
3. The height bound
4. Rotations

**Part II — operations**

5. Search
6. Insert — 3 cases
7. Delete — 4 cases
8. Code — insert and a validator

**Part III — context**

9. The 2-3-4 tree view (and left-leaning RB trees)
10. Red-black vs AVL vs others
11. Where it's used · 12. Complexity · 13. Gotchas

Then: Summary · Self-check

---

# Part I — Why it works

## 1. The problem — BSTs degrade

A plain BST is O(height) per operation, and the height depends on insertion order:

```
insert 10, 20, 30, 40, 50           insert 30, 20, 40, 10, 50

10                                       30
  \                                     /  \
   20                                 20    40
     \                               /        \
      30          height 5 = n      10        50     height 3 ≈ log n
        \
         40
           \
            50
```

Sorted input — common in practice — turns it into a linked list. A **self-balancing**
BST restructures itself after each insert/delete to keep the height O(log n) whatever
the order. Red-black and AVL are the two classic ways to do it.

## 2. The rules

1. Every node is **red** or **black**.
2. The **root is black**.
3. Every **NIL leaf** (the `null` children) is **black**.
4. **Red rule:** a red node's children are both black — **no two reds in a row**.
5. **Black rule:** for each node, **every path down to a NIL leaf crosses the same
   number of black nodes**. That count is the node's **black-height**, `bh(x)`.

```
                 13B                    bh(13) = 3 (counting NIL)
              /       \
           8R           17R
          /   \        /    \
        1B    11B    15B     25B
          \                 /   \
          6R              22R   27R
```

Rules 4 and 5 do all the work. Rules 1–3 just make them well-defined.

Colour is **1 bit per node** — the only space overhead over a plain BST.

## 3. The height bound

**Claim:** a red-black tree with n internal nodes has height **h ≤ 2·log₂(n+1)**.

1. **A subtree rooted at x has ≥ 2^bh(x) − 1 internal nodes.** By induction on height:
   each child has black-height ≥ bh(x) − 1, so x's subtree has at least
   `2·(2^(bh(x)−1) − 1) + 1 = 2^bh(x) − 1` nodes.
2. **bh(root) ≥ h/2.** On the longest root-to-leaf path, reds can't be adjacent
   (rule 4), so at least half its nodes are black.
3. Combine: `n ≥ 2^(h/2) − 1` → **h ≤ 2·log₂(n+1)**.

Intuition: rule 5 makes every path have the same number of blacks; rule 4 lets you
sprinkle at most one red between each pair of blacks. So the longest path is at most
**2× the shortest**.

| n | perfectly balanced | AVL worst (~1.44 log n) | red-black worst (2 log n) |
|---|---|---|---|
| 1,000 | 10 | 14 | 20 |
| 1,000,000 | 20 | 28 | 40 |
| 1,000,000,000 | 30 | 43 | 60 |

## 4. Rotations

The only structural operation. Changes which node is the parent **without changing the
in-order sequence**, so the BST property survives. **O(1)** — a fixed number of pointer
updates.

```
          rotateLeft(x)  →
    x                        y
   / \                      / \
  a   y           ←        x   c
     / \     rotateRight(y)   / \
    b   c                    a   b

in-order before and after:  a  x  b  y  c
```

Only subtree `b` changes parent (from `y` to `x`). `rotateLeft(x)` lifts `x`'s right
child; `rotateRight(y)` lifts `y`'s left child.

Rotations fix **shape**; recolouring fixes **colour**. Insert and delete use both.

---

# Part II — Operations

## 5. Search

Exactly a BST search — the colours are ignored:

```java
Node n = root;
while (n != null && key != n.key)
    n = key < n.key ? n.left : n.right;
return n;
```

O(h) = **O(log n)**. Same for min (go left), max (go right), floor, ceiling, successor.

## 6. Insert — 3 cases

1. **BST-insert** the key as a new leaf.
2. **Colour it red.** A red node adds no black to any path, so the black rule (5) still
   holds. The only rules that can break: the **root** rule (if the tree was empty) or
   the **red rule** (if the parent is red).
3. **Fix up** while `z` and its parent `p` are both red. The grandparent `g` exists and
   is black (a red `p` can't be the root, and the tree was valid before). Look at the
   **uncle** `u` — `g`'s other child:

### Case 1 — uncle is red → recolour, move up

```
        g B                    g R   ← may now clash with ITS parent: z = g, loop
       /   \                  /   \
     p R   u R      →       p B   u B
     /                      /
   z R                    z R
```

Push the blackness down from `g` into `p` and `u`. Black-heights unchanged. No rotation,
but the problem may move two levels up — loop with `z = g`.

### Case 2 — uncle is black, `z` is an *inner* child (triangle) → rotate the parent

```
        g B                    g B
       /   \                  /   \
     p R   u B   rotateLeft(p)  z R   u B      now a straight line:
       \           →         /                rename z ↔ p, go to case 3
       z R                 p R
```

Doesn't fix anything by itself — it just converts the triangle into case 3's line.

### Case 3 — uncle is black, `z` is an *outer* child (line) → rotate the grandparent

```
          g B                         p B
         /   \     rotateRight(g)    /   \
       p R   u B         →         z R   g R
       /                                   \
     z R                                   u B
```

Rotate `g` the opposite way, then swap the colours of `p` and `g`. The subtree's root is
black again, as before, so nothing above is affected. **Done** — no loop.

Finally: **colour the root black** (handles case 1 bubbling up to the root, and the
first insert).

Mirror everything when `p` is a right child (uncle on the left, rotations reversed).

| | Rotations | Recolours | Loops? |
|---|---|---|---|
| Case 1 | 0 | 3 | yes, 2 levels up |
| Case 2 → 3 | 2 | 2 | no — terminates |
| Case 3 | 1 | 2 | no — terminates |

**At most 2 rotations per insert.** Case 1 can repeat O(log n) times (O(1) amortised).

### Worked example: insert 10, 20, 30, 15, 25, 5

```
+10           +20           +30: red-red, uncle NIL (black)
                            line (right-right) → case 3: rotateLeft(10), swap colours
 10B          10B
                \                20B
                20R             /   \
                              10R   30R

+15: under 10 (red), uncle 30 is red → case 1: recolour; 20 is root → back to black

      20B
     /   \
   10B   30B
     \
     15R

+25: under 30 (black) → nothing to fix

      20B
     /   \
   10B   30B
     \   /
    15R 25R

+5: under 10 (black) → nothing to fix

        20B
       /    \
     10B     30B
     / \     /
   5R  15R 25R
```

Every root-to-NIL path has 3 blacks; no red has a red child. ✅

## 7. Delete — 4 cases

Delete is harder because removing a **black** node shortens some paths by one black.

### Step 1 — reduce to "at most one child"

If `z` has two children, find its in-order **successor** `s` (leftmost node of
`z.right`), **copy `s`'s key into `z`**, and delete `s` instead. `s` has no left child,
so every delete is now of a node with **0 or 1 child**.

### Step 2 — splice it out

Its child `x` (possibly NIL) takes its place.

| Removed node | Child `x` | Result |
|---|---|---|
| **red** | — | nothing breaks. **Done.** (A red node with ≤ 1 child has none.) |
| black | **red** | colour `x` black — it replaces the lost black. **Done.** |
| black | black / NIL | every path through `x` is one black short → `x` is **"double black"**; fix up |

### Step 3 — fix the double black

`x` carries an extra unit of black that must be either **absorbed by a red** node,
**pushed up** to the parent, or **removed at the root**. Look at `x`'s sibling `s`
(shown for `x` as a left child; mirror otherwise). "Near" child = `s`'s child closer to
`x`; "far" = the other.

**Case 1 — sibling is red → rotate so the sibling becomes black**

```
       p B                         s B
      /   \     rotateLeft(p)     /   \
    x ●   s R        →          p R    d
          / \                  /  \
         c   d               x ●   c   ← c is x's new (black) sibling
```

Swap `p` and `s` colours. Nothing fixed yet, but `x` now has a black sibling → go to
case 2, 3 or 4. Because `p` is now red, case 2 will terminate immediately.

**Case 2 — sibling black, both its children black → recolour sibling, move up**

```
       p ?                     p ?   ← gets the extra black: x = p
      /   \                   /   \
    x ●   s B       →       x     s R
          / \                     / \
        c B  d B                c B  d B
```

Take one black off both `x` and `s` (s turns red), give it to `p`. If `p` was red →
colour it black, **done**. If `p` was black → `p` is now double black; loop upward.

**Case 3 — sibling black, near child red, far child black → rotate the sibling**

```
       p ?                       p ?
      /   \    rotateRight(s)   /   \
    x ●   s B        →        x ●   c B
          / \                         \
        c R  d B                      s R
                                        \
                                        d B
```

Swap `s` and `c` colours. Now the sibling's **far** child is red → case 4.

**Case 4 — sibling black, far child red → rotate the parent, done**

```
       p ?                          s ?   ← takes p's old colour
      /   \     rotateLeft(p)      /   \
    x ●   s B         →          p B    d B
          / \                   /  \
         c   d R              x     c
```

`s` takes `p`'s colour, `p` and `d` become black. The extra black is absorbed by `p`
on `x`'s side. **Done.**

At the end, colour `x` black (covers "absorbed by a red" and "reached the root").

| | Rotations | Loops? |
|---|---|---|
| Case 1 | 1 | no — always leads to 2/3/4 |
| Case 2 | 0 | yes, if the parent was black |
| Case 3 → 4 | 2 | no — terminates |
| Case 4 | 1 | no — terminates |

**At most 3 rotations per delete** (case 1 → 3 → 4). Case 2 can repeat O(log n) times.

### Worked examples

Delete **30** from `20B (10B (-, 15R), 30B)` — exercises cases 3 then 4:

```
       20B                  30 is black with no children → its NIL spot is double black
      /   \                 x = NIL (right child of 20), sibling s = 10 (black)
    10B   30B ✗             mirror case: near child = 10.right = 15 (red), far = 10.left = NIL
      \                     → case 3 (mirrored): rotateLeft(10), swap colours of 10 and 15
      15R

       20B                  sibling is now 15 (black), its far child 10 is red
      /   \                 → case 4 (mirrored): rotateRight(20)
    15B    ●                   15 takes 20's colour (B), 20 → B, 10 → B
    /
  10R

       15B                  every path: 2 blacks + NIL ✅
      /   \
    10B   20B
```

Delete **10** from `20B (10B, 30B)` — case 2 ending at the root:

```
    20B             x = NIL (double black), sibling 30 black with NIL children
   /   \            → case 2: 30 → red, extra black moves to 20
 10B ✗  30B         20 is the root → the extra black is simply dropped

    20B
       \
       30R
```

## 8. Code — insert and a validator

Java, `int` keys, `null` as NIL (counted black). Colours match `TreeMap`'s
convention (`BLACK = true`).

```java
class RedBlackTree {
    static final boolean RED = false, BLACK = true;

    static final class Node {
        int key;
        Node left, right, parent;
        boolean color = RED;                       // new nodes are always red
        Node(int key, Node parent) { this.key = key; this.parent = parent; }
    }

    Node root;

    static boolean isRed(Node n) { return n != null && n.color == RED; }   // NIL is black

    void insert(int key) {
        Node parent = null, t = root;
        while (t != null) {                        // 1. plain BST descent
            parent = t;
            if      (key < t.key) t = t.left;
            else if (key > t.key) t = t.right;
            else return;                           //    duplicate — set semantics
        }
        Node z = new Node(key, parent);
        if      (parent == null)   root = z;
        else if (key < parent.key) parent.left = z;
        else                       parent.right = z;
        fixAfterInsert(z);                         // 2. repair colours
    }

    private void fixAfterInsert(Node z) {
        while (z != root && isRed(z.parent)) {
            Node p = z.parent, g = p.parent;       // g exists: a red parent is never the root
            if (p == g.left) {
                Node u = g.right;
                if (isRed(u)) {                    // case 1: recolour, move up
                    p.color = BLACK; u.color = BLACK; g.color = RED;
                    z = g;
                } else {
                    if (z == p.right) {            // case 2: triangle → line
                        rotateLeft(p);
                        z = p; p = z.parent;
                    }
                    p.color = BLACK; g.color = RED; // case 3: line → rotate grandparent
                    rotateRight(g);
                }
            } else {                               // mirror image
                Node u = g.left;
                if (isRed(u)) {
                    p.color = BLACK; u.color = BLACK; g.color = RED;
                    z = g;
                } else {
                    if (z == p.left) {
                        rotateRight(p);
                        z = p; p = z.parent;
                    }
                    p.color = BLACK; g.color = RED;
                    rotateLeft(g);
                }
            }
        }
        root.color = BLACK;
    }

    private void rotateLeft(Node x) {
        Node y = x.right;
        x.right = y.left;
        if (y.left != null) y.left.parent = x;
        y.parent = x.parent;
        if      (x.parent == null)        root = y;
        else if (x == x.parent.left)      x.parent.left = y;
        else                              x.parent.right = y;
        y.left = x;
        x.parent = y;
    }

    private void rotateRight(Node y) {
        Node x = y.left;
        y.left = x.right;
        if (x.right != null) x.right.parent = y;
        x.parent = y.parent;
        if      (y.parent == null)        root = x;
        else if (y == y.parent.right)     y.parent.right = x;
        else                              y.parent.left = x;
        x.right = y;
        y.parent = x;
    }

    /** Black-height of n counting NIL, or -1 if any rule is broken below n. */
    static int blackHeight(Node n) {
        if (n == null) return 1;
        if (isRed(n) && (isRed(n.left) || isRed(n.right))) return -1;   // rule 4
        int l = blackHeight(n.left), r = blackHeight(n.right);
        if (l == -1 || r == -1 || l != r) return -1;                    // rule 5
        return l + (n.color == BLACK ? 1 : 0);
    }

    boolean isValid() { return !isRed(root) && blackHeight(root) != -1; }
}
```

Test it the way the rest of this repo does — `assert` after every operation, run with
`-ea`:

```java
RedBlackTree t = new RedBlackTree();
for (int i = 1; i <= 100_000; i++) { t.insert(i); assert t.isValid(); }   // sorted input
```

**Delete, in outline** (`fixAfterDelete` mirrors the 4 cases in §7):

```
delete(z):
    if z.left != null && z.right != null:          // two children
        s = leftmost(z.right); z.key = s.key; z = s
    x = z.left != null ? z.left : z.right          // the one child, or null
    if x != null:
        replace z with x in z.parent
        if z.color == BLACK: fixAfterDelete(x)
    else if z.parent == null:                      // deleting the only node
        root = null
    else:
        if z.color == BLACK: fixAfterDelete(z)     // fix while z still stands in as "NIL"
        unlink z from z.parent
```

The `else` branch is the JDK's trick for a NIL double black: run the fix-up using the
doomed node itself as the placeholder (it has a parent pointer; `null` doesn't), and
unlink it afterwards.

---

# Part III — Context

## 9. The 2-3-4 tree view (and left-leaning RB trees)

A red-black tree is a **2-3-4 tree** (a B-tree where each node holds 1–3 keys) encoded
as a binary tree. A **red** node is glued to its parent — together they form one B-tree
node:

```
red-black                        2-3-4 tree

        20B                       [      20      ]
       /    \                    /                \
     10B     30B            [ 5  10  15 ]     [ 25  30 ]
     / \     /
   5R  15R 25R
```

Through that lens the rules stop looking arbitrary:

| Red-black | 2-3-4 tree |
|---|---|
| black node + 0, 1 or 2 red children | a 2-, 3- or 4-node |
| no red-red (rule 4) | a node holds at most 3 keys |
| equal black-height (rule 5) | **all leaves at the same depth** — B-trees are perfectly balanced |
| insert case 1 (recolour, move up) | **split** a full 4-node, push the middle key up |
| insert cases 2/3 (rotate) | absorb the new key into a 3-node, re-centred |
| delete case 2 (recolour sibling, move up) | **merge** with a sibling |
| delete cases 3/4 (rotate) | **borrow** a key from a sibling |

This is also why red-black trees are "2× the B-tree height" at worst — each B-tree level
becomes at most 2 binary levels.

**Left-leaning red-black trees** (Sedgewick, 2008) restrict red links to lean left,
which maps them onto 2-3 trees and collapses insert into three local checks on the way
back up a recursive insert:

```java
Node insert(Node h, int key) {
    if (h == null) return new Node(key, RED);
    if      (key < h.key) h.left  = insert(h.left, key);
    else if (key > h.key) h.right = insert(h.right, key);

    if (isRed(h.right) && !isRed(h.left))     h = rotateLeft(h);   // fix right-leaning red
    if (isRed(h.left)  &&  isRed(h.left.left)) h = rotateRight(h); // two reds in a row
    if (isRed(h.left)  &&  isRed(h.right))     flipColors(h);      // split a 4-node
    return h;
}   // caller: root = insert(root, key); root.color = BLACK;
```

Much shorter to write in an interview; delete is still fiddly, and the classic
(CLRS / JDK) version does fewer rotations.

## 10. Red-black vs AVL vs others

| | Red-black | AVL | B-tree / B+ tree | Skip list | Treap / splay |
|---|---|---|---|---|---|
| Balance rule | colours; longest ≤ 2× shortest | subtree heights differ ≤ 1 | all leaves same depth, wide nodes | random levels | random priorities / move-to-root |
| Max height | 2·log n | ~1.44·log n | log_B n (very shallow) | O(log n) **expected** | expected / amortised |
| Rotations per insert | ≤ 2 | ≤ 2 | (splits) | — | expected O(1) / O(log n) amortised |
| Rotations per delete | **≤ 3** | **O(log n)** | (merges) | — | |
| Lookups | good | **best of the binary trees** | best on disk | good | good / great for hot keys |
| Extra per node | 1 bit | height or balance factor | — | ~2 pointers avg | priority / — |
| Typical home | general-purpose in-memory ordered maps | read-heavy indexes | **databases, file systems** | concurrent maps (`ConcurrentSkipListMap`), Redis sorted sets | caches, simple randomised BSTs |

**Why the standard libraries pick red-black:** worst-case O(log n) everything, O(1)
rotations per update (cheap writes, and good for persistent/concurrent variants), and
lookups only slightly slower than AVL.

## 11. Where it's used

- **Java** — `TreeMap`, `TreeSet`, and `HashMap` buckets once they treeify at 8 entries
  ([EQUALITY-AND-HASHING §8](../01-arrays-hashing/EQUALITY-AND-HASHING.md)).
- **C++** — `std::map`, `std::set`, `std::multimap`, `std::multiset` (libstdc++, libc++
  and MSVC all use red-black trees).
- **Linux kernel** — `lib/rbtree.c`: scheduler run queues, timers, epoll, I/O schedulers.
- **Functional languages** — persistent (immutable) red-black trees back ordered maps
  in several standard libraries; insert copies only the O(log n) path.

## 12. Complexity

| Operation | Time | Notes |
|---|---|---|
| search / min / max / floor / ceiling / successor | O(log n) | colours ignored |
| insert | O(log n) | ≤ 2 rotations; O(1) amortised recolouring |
| delete | O(log n) | ≤ 3 rotations; O(1) amortised recolouring |
| in-order traversal | O(n) | sorted output |
| build from sorted input | O(n) | build a balanced tree directly, colour the deepest partial level red |
| space | O(n) | + 1 colour bit per node (+ parent pointer if used) |

## 13. Gotchas

- **New nodes are red, always.** Inserting black would break rule 5 on one path —
  much harder to repair than a red-red clash.
- **NIL is black.** Every uncle/sibling check must treat `null` as black — write an
  `isRed(n)` helper that returns `false` for `null` and use it everywhere.
- **Update parent pointers in rotations.** The most common implementation bug: `y.left`
  moves to `x.right` but its `parent` isn't updated, or the root isn't re-pointed.
- **Mirror cases are not optional.** Every insert and delete case has a left/right
  mirror; forgetting one passes small tests and fails on random input. Test with
  sorted, reverse-sorted and random sequences, validating after every operation.
- **Two-child delete copies the successor's key into the node.** Any outside reference
  to "the node holding key k" is now pointing at a different key
  (TREEMAP-INTERNALS §7).
- **"Balanced" ≠ "perfectly balanced."** Height can be up to 2× optimal. If you need
  the k-th element or rank queries, augment each node with its subtree size
  (order-statistic tree) — `TreeMap` doesn't.

---

## Summary — say this in an interview

> A red-black tree is a self-balancing BST. Every node is red or black; the root and
> NIL leaves are black; no red node has a red child; and every path from a node to its
> leaves has the same number of black nodes. Those last two rules make the longest path
> at most twice the shortest, so the height is ≤ 2·log₂(n+1) and search, insert and
> delete are O(log n) worst case.
>
> Insert adds a red leaf. If its parent is red: a red uncle means recolour and move up;
> a black uncle means one or two rotations and you're done. Delete reduces to removing a
> node with ≤ 1 child; removing a black node leaves a "double black", fixed by four
> sibling cases with at most three rotations.
>
> It's less strictly balanced than AVL, so lookups are slightly slower, but updates do
> O(1) rotations — which is why `TreeMap`, `std::map` and the Linux kernel use it. It's
> a 2-3-4 tree in binary form: recolouring is a node split, rotations re-centre a node.

## Self-check

**Rules and height**

1. State the five rules. Which two actually enforce balance?
2. Prove h ≤ 2·log₂(n+1). Where does each of rules 4 and 5 get used?
3. Can a red-black tree consist of only black nodes? What shape must it be?
4. What's the shortest and longest possible root-to-NIL path if bh(root) = 3?

**Insert**

5. Why is a new node coloured red rather than black?
6. Insert 1 through 7 in order into an empty tree. Draw it after each step and name
   the case used.
7. Why does case 1 loop but case 3 terminate?
8. Why is "at most 2 rotations" true even though the fix-up can climb the whole tree?

**Delete**

9. Why does deleting a red node never need a fix-up?
10. What is a "double black" and what are the three ways to get rid of it?
11. Why does delete case 1 (red sibling) always end within one or two more steps?
12. Delete the root from the final tree of §6's worked example. Which cases run?

**Context**

13. Draw the 2-3-4 tree for the final tree of §6. Which red-black operation is a 4-node
    split?
14. When would you choose AVL over red-black? A B-tree over both?
15. Why do `HashMap` buckets use a red-black tree rather than an AVL tree?
