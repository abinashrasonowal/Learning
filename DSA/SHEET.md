# Curated Love Babbar sheet — one problem per concept

Source: [DSA Sheet by Love Babbar](https://www.geeksforgeeks.org/dsa/dsa-sheet-by-love-babbar/)
(429 problems, 15 sections).

**What this file is.** The sheet is a coverage list, not a curriculum — it repeats the
same concept up to six times and files problems by *data structure* rather than by
*technique*. This is the same sheet with the repeats collapsed: **~130 problems, one per
distinct idea**, re-sorted into the 12 topic folders.

**How to read a row.** `Solve` is the one you actually write. `Also covers` lists the
sheet problems that are the same idea in different clothes — you can skip those, and if
you ever fail a revisit on the kept problem, they're your extra reps.

> A folded problem isn't a *worse* problem. It's the same lesson. Solving all six
> sort-and-pair-extremes greedy problems doesn't teach you six things.

Progress convention: tick a row only when it's `solved` cold in `PROBLEMS.md`.

---

## 01 — Arrays & hashing

| Concept | Solve | Also covers |
|---|---|---|
| Kadane / running best | Kadane's Algo | Find Largest sum contiguous Subarray · LargestSum Contiguous Subarray (DP) · Smallest sum contiguous subarray |
| Dutch national flag | Sort array of 0s, 1s and 2s without a sorting algo | Move all negative elements to one side · Partitioning arrays with many repeated entries · Sort a LL of 0's 1's 2's |
| Floyd cycle on values | Find duplicate in an array of N+1 integers | Find the repeating and the missing (adds the XOR/math variant) |
| Prefix × suffix | Product array Puzzle | — |
| Boyer–Moore voting | Find majority element | — |
| Interval merge | Merge Intervals | Merge Overlapping Intervals (Stacks §16) |
| Lexicographic next | Next Permutation | Find next greater number with same set of digits |
| Gap method, no extra space | Merge 2 sorted arrays without extra space | Merge 2 sorted arrays |
| Cycle decomposition | Minimum no. of swaps required to sort the array | Minimum swaps to convert Binary tree into BST |
| Boundary simulation | Spiral traversal on a Matrix | — |
| In-place transpose+reverse | Rotate matrix by 90 degrees | — |
| Staircase search | Search an element in a Matrix | Print elements in sorted order using row-column sorted matrix · Find a specific pair in matrix |
| Row-wise binary search | Find row with maximum no. of 1's | Common elements in all rows of a matrix (hashing variant) |

**Dropped:** cyclically rotate an array by one, reverse an array, find max and min
(one-liners — write them once in warm-up, don't log them).

## 02 — Two pointers

| Concept | Solve | Also covers |
|---|---|---|
| Converge from both ends | Check whether a String is Palindrome or not | Reverse a String |
| Sorted pair search | Find a pair with a given difference | Find a pair with given sum (2-sum sorted) |
| k-sum generalisation | Find four elements that sum to a given value | Count triplet with sum smaller than a given value (the *counting* twist) |
| Merge-style scan | Find the Union and Intersection of two sorted arrays | Intersection of two sorted Linked Lists |
| Sort + squeeze | Chocolate Distribution Problem | Minimize the maximum difference between heights |

## 03 — Sliding window

| Concept | Solve | Also covers |
|---|---|---|
| Monotonic deque, fixed k | Maximum of all subarrays of size k | First negative integer in every window of size k · Sum of min and max of all subarrays of size k |
| Variable window + counts | Smallest window in a string containing all characters of another string | Smallest window that contains all characters of the string itself |
| Window + frequency cap | Minimum sum of squares of character counts after removing k characters | Longest substring with at most k distinct (not in sheet — add it) |
| Stream + queue | Queue-based first non-repeating character in a stream | Function to find number of customers who could not get a computer |

## 04 — Stack & queue

| Concept | Solve | Also covers |
|---|---|---|
| Array-backed, from scratch | Implement Stack from Scratch | Implement Queue from Scratch · Reverse a String using Stack |
| Circular buffer | Implement a Circular queue | Implement n queues in an array |
| Space partitioning | Implement N stacks in an array | Implement 2 stacks in an array |
| Auxiliary invariant | Design a Stack with getMin() in O(1) time **and O(1) space** | Find the middle element of a stack |
| Monotonic stack | Find the next Greater element | Next Smaller Element · Delete nodes which have a greater value on right |
| Monotonic stack → area | Largest rectangular Area in Histogram | — |
| …composed into 2D | Maximum size rectangle (binary matrix) | Largest area rectangular sub-matrix with equal 1s and 0s |
| Matching + index stack | Check balanced parenthesis | Expression contains redundant bracket · Minimum bracket reversals · Minimum swaps for bracket balancing |
| Longest valid run | Length of the Longest Valid Substring | — |
| Postfix machine | Evaluation of Postfix expression | Arithmetic Expression evaluation |
| Amortised conversion | Implement Queue using Stack | Implement Stack using Queue · Implement Stack using Deque · Stack Permutations |
| Recursion as the only storage | Sort a Stack using recursion | Reverse a stack using recursion · Insert at bottom without another DS · Reverse a Queue using recursion · Reverse first k elements of a queue · Interleave the halves of a queue |
| Hash + DLL | LRU Cache Implementation | LRU Page Replacement algorithm (Greedy §22) |
| Multi-source BFS on grid | Minimum time required to rot all oranges | Distance of nearest cell having 1 in a binary matrix |
| Elimination in O(n) | The Celebrity Problem | — |
| Circular prefix feasibility | First circular tour that visits all petrol pumps | — |

## 05 — Binary search & divide-and-conquer

| Concept | Solve | Also covers |
|---|---|---|
| Lower/upper bound | Find first and last positions of an element in a sorted array | Find a Fixed Point · Missing Number in AP |
| Broken monotonicity | Search in a rotated sorted array | Find pivot element in a sorted array |
| **Binary search on the answer** | Aggressive cows | Book Allocation · Painters Partition · ROTI-Prata · EKOSPOJ · Bishu and Soldiers · Smallest number with n trailing zeroes in factorial · Optimum location of point |
| BS on a numeric range | Square root of an integer | Calculate square of a number without `*` `/` `pow` |
| Partition two arrays | K-th Element of Two Sorted Arrays | — |
| BS on the value space | Kth smallest element in a row-column wise sorted matrix | Find median in a row-wise sorted matrix |
| Merge sort as a counter | Find the inversion count | Implement Merge-sort in-place |
| Pairing / minimal comparisons | Maximum and minimum of an array using minimum comparisons | Find the Kth max and min element of an array |

`Aggressive cows` is the single highest-value row in this file: **seven** sheet problems,
one technique — "if I can check a candidate answer in O(n), I can binary search the
answer space". Learn the check-function shape, not the cows.

## 06 — Linked list

| Concept | Solve | Also covers |
|---|---|---|
| Pointer reversal | Reverse a Linked List (iterative **and** recursive) | Reverse a Doubly Linked list |
| Reversal in blocks | Reverse a Linked List in groups of given size | Rotate a DLL in groups of given size |
| Floyd's tortoise & hare | Find the starting point of the loop | Detect loop · Delete loop · Find the middle element · n'th node from the end · Check if circular |
| Merge sort on lists | Merge Sort for Linked Lists | Quicksort for Linked Lists (and *why* mergesort wins here — see self-check) |
| Reverse-half compose | Check whether a Singly Linked List is a palindrome | — |
| Length-difference align | Intersection Point of two Linked Lists | — |
| Digit-wise carry | Add two numbers represented by linked lists | Add 1 to a number represented as a LL · Multiply 2 numbers represented by LL |
| Interleave-then-split | Clone a linked list with next and random pointer | — |
| Multi-level merge | Flatten a Linked List | — |
| Set-based dedup | Remove duplicates in an unsorted Linked List | Remove duplicates in a sorted Linked List |
| Two pointers need `prev` | Find pairs with a given sum in a DLL | Count triplets in a sorted DLL summing to X · Rotate DLL by N nodes |
| Circular splice | Split a Circular Linked List into two halves | Deletion from a Circular Linked List · Move last element to front |
| Heap + list | Merge K sorted Linked Lists | *(also in 08 — solve it once, here)* |

**Dropped:** segregate even/odd nodes, sort a k-sorted DLL (that's just heap + list).
**Keep as theory, not code:** "Can we reverse a LL in less than O(n)?", "Why quicksort
for arrays and mergesort for lists?" — those are self-check questions.

## 07 — Trees & BST

### Binary tree

| Concept | Solve | Also covers |
|---|---|---|
| Iterative traversal | Inorder traversal, recursive **and** iterative | Preorder iterative · Postorder iterative (two stacks — the awkward one) |
| BFS by level | Level order traversal | Reverse level order · Zig-zag traversal (deque variant) · Check if all leaves are at same level |
| Post-order returning info | Diameter of a tree | Height of a tree · Check if balanced · Find largest subtree sum · Sum of nodes on longest root-to-leaf path |
| **(level, horizontal-dist) bookkeeping** | Top View of a tree | Left View · Right View · Bottom View · Diagonal traversal · Boundary traversal |
| Structural recursion on pairs | Check if 2 trees are mirror or not | Mirror of a tree · Tree Isomorphism Problem |
| Index-map reconstruction | Construct Binary Tree from Inorder and Preorder | Construct Binary Tree from String with bracket representation |
| In-place rewiring | Convert Binary Tree into Doubly Linked List | — |
| Bottom-up rewrite | Convert Binary Tree into Sum tree | Check if Binary tree is a Sum tree |
| Root-to-node paths | Find LCA in a Binary tree | Find distance between 2 nodes · Kth ancestor of a node |
| Prefix-sum on a path | Print all K-sum paths in a Binary tree | — |
| Tree DP (include/exclude) | Maximum sum of nodes such that no two are adjacent | Largest Independent Set Problem (DP §39) |
| Serialise + hash subtrees | Check if a tree contains duplicate subtrees of size ≥ 2 | Find all duplicate subtrees |

### BST

| Concept | Solve | Also covers |
|---|---|---|
| Range recursion | Check if a tree is a BST or not | Find a value in a BST · Find min and max in a BST |
| Successor splice | Deletion of a node in a BST | — |
| Ordered neighbours | Find inorder successor and predecessor in a BST | Populate inorder successor of all nodes |
| O(h) via ordering | Find LCA of 2 nodes in a BST | *(contrast with the O(n) binary-tree version above)* |
| Inorder = sorted | Find Kth smallest element in a BST | Find Kth largest · Count BST nodes in a given range · Flatten BST to sorted list |
| **Morris traversal, O(1) space** | Find the median of a BST in O(n) time and O(1) space | — |
| Bounds recursion | Construct BST from preorder traversal | Check whether a given preorder is valid |
| Rebuild balanced | Convert a normal BST into a Balanced BST | Convert Binary tree into BST |
| Two inorder streams | Merge two BSTs | Count pairs from 2 BSTs whose sum equals X |
| Post-order returning info | Largest BST in a Binary Tree | Check whether BST contains a dead end |
| BST built right-to-left | Replace every element with the least greater element on its right | — |
| Augmented / interval BST | Given n appointments, find the conflicting appointments | — |

## 08 — Heaps & intervals

| Concept | Solve | Also covers |
|---|---|---|
| Sift up / sift down | Implement a MaxHeap/MinHeap using arrays and recursion | Convert min heap to max heap · Merge 2 binary max heaps · Check if a Binary Tree is a heap · Convert BST to Min Heap |
| Heapify + in-place sort | Sort an array using heap (HeapSort) | — |
| Bounded heap of size k | K largest elements in an array | Kth smallest and largest in an unsorted array · Kth smallest number again |
| k-way merge | Merge K sorted arrays | Merge K sorted Linked Lists |
| Two heaps, balanced | Median in a stream of integers | — |
| k pointers + heap | Smallest range in K lists | — |
| Greedy by cheapest pair | Connect n ropes with minimum cost | Minimum Cost of ropes · Minimum Cost to cut a board into squares · Minimum sum of two numbers formed from digits |
| Count-max + spacing | Reorganize strings (no two adjacent same) | Rearrange characters in a string so no two adjacent are same (appears **3×** in the sheet) |
| Prefix sums + heap | Kth largest sum of continuous subarrays | — |
| Sweep line on intervals | Minimum Platforms Problem | Maximum trains for which stoppage can be provided |

## 09 — Graphs

| Concept | Solve | Also covers |
|---|---|---|
| Adjacency list + both walks | Implement BFS algorithm | Implement DFS · Create a graph and print it |
| 3-colour recursion | Detect cycle in a **directed** graph | Detect cycle in an undirected graph (parent-skip variant) · Check whether a given graph is a tree |
| Kahn / DFS ordering | Implement Topological Sort | Find whether it is possible to finish all tasks · Minimum time taken by each job in a DAG · Longest path in a DAG |
| Modelling as topo-sort | Given a sorted dictionary of an alien language, find the character order | — |
| Grid flood | Find the number of Islands | Flood fill algorithm · Search in a Maze · Making wired connections |
| BFS = shortest hops | Minimum Step by Knight | Snake and Ladders · Water Jug problem using BFS |
| Implicit graph | Word Ladder | — |
| 0-1 BFS (deque) | Minimum edges to reverse to make a path from source to destination | — |
| Hash + DFS copy | Clone a graph | — |
| Dijkstra | Dijkstra's algorithm | — |
| State-space Dijkstra | Cheapest Flights Within K Stops | — |
| Negative weights | Implement Bellman-Ford | Detect a negative cycle in a graph |
| All-pairs | Implement Floyd-Warshall | — |
| **DSU** + MST | Implement Kruskal's Algorithm | Implement Prim's Algorithm · Journey to the Moon · Total number of spanning trees |
| Tarjan low-link | Find bridges in a graph | Articulation points |
| Kosaraju | Count strongly connected components | — |
| 2-colouring | Check whether a graph is Bipartite | Two Clique Problem |

**Dropped as competitive-only:** Seven Bridges, Chinese Postman, Vertex Cover, number
of triangles, Oliver and the Game, TSP (kept in DP as the bitmask example), Graph/M
colouring (kept in backtracking), minimise cash flow (kept in greedy).

## 10 — Dynamic programming

The sheet's 60 DP problems are about **12 recurrences** wearing different names.

| Family | Solve | Also covers |
|---|---|---|
| 0/1 knapsack | Knapsack Problem | Subset Sum Problem · Partition problem · Minimum removals to make max−min ≤ K |
| Unbounded knapsack | Unbounded Knapsack (repetition allowed) | Coin Change Problem · Minimum number of coins · Minimum cost to fill a given weight · Maximize the cut segments · Count ways to reach a given score |
| LCS grid | Longest Common Subsequence | Longest Common Substring · Longest Repeated Subsequence · LCS of three strings · Space-optimised LCS · Longest Recurring Subsequence · Transform one string to another · Find if a string is interleaved of two others |
| Edit distance | Edit Distance | Word Wrap Problem |
| Wildcard matching | String matching where one string contains wildcard characters | — |
| LIS (+ O(n log n)) | Longest Increasing Subsequence | Maximum Sum Increasing Subsequence · Maximum Length Chain of Pairs · Longest alternating subsequence · Longest subsequence with adjacent difference one · Weighted Job Scheduling |
| Palindrome DP | Longest Palindromic Subsequence | Longest Palindromic Substring · Count all palindromic subsequences · Minimum characters to add at front (the KMP trick beats DP here) |
| Interval / MCM | Matrix Chain Multiplication | Palindrome Partitioning · Boolean Parenthesization · Optimal Binary Search Tree |
| Worst-case optimisation | Egg Dropping Problem | — |
| Grid DP | Min Cost Path Problem | Gold Mine Problem · Maximum size square sub-matrix with all 1s · Painting the Fence |
| Prefix + Kadane compose | Maximum sum rectangle in a 2D matrix | Largest rectangular sub-matrix whose sum is 0 |
| Tree DP | Largest Independent Set Problem | *(same as the tree row in 07 — solve it once)* |
| Bitmask DP | Travelling Salesman Problem | — |
| Counting DP | Count Balanced Binary Trees of Height h | Friends Pairing Problem · Count Derangements |
| Catalan | Program for nth Catalan Number | — |
| State machine | Maximum profit by buying and selling a share at most k times | Maximum profit … at most twice |
| Game theory DP | Optimal Strategy for a Game | Coin game winner where every player has three choices |
| Reachability DP | Word Break Problem | *(also solvable with a trie — see 12)* |

**Dropped:** binomial/permutation coefficient, assembly line scheduling, mobile numeric
keypad, maximum difference of zeros and ones, count subsequences with product < K,
max subsequence sum with no three consecutive, max sum of pairs with specific
difference, minimum jumps (kept as greedy).

## 11 — Backtracking

| Concept | Solve | Also covers |
|---|---|---|
| choose → recurse → un-choose | Print all permutations of a string | Print all subsequences of a string · Power Set · Find K-th permutation sequence (do it with math instead) |
| Pick/skip with pruning | Combinational Sum | Subset Sum Problem · Tug of War · Partition of a set into K subsets with equal sum |
| Grid backtracking | Rat in a Maze | Knight's tour · Shortest safe route with landmines · Longest route in a matrix with hurdles · Print all paths in an m×n matrix · Search a word in a 2D grid · Find if there is a path of more than k length |
| Column/diagonal constraints | Printing all solutions in N-Queen | M-Colouring Problem · Graph Colouring |
| Constraint propagation | Sudoku Solver | — |
| Partition + palindrome check | Print all palindromic partitions of a string | Remove Invalid Parentheses · Recursively print all sentences from a list of word lists |

## 12 — Greedy, bits & strings

### Greedy — always state the exchange argument before you code

| Concept | Solve | Also covers |
|---|---|---|
| Earliest finish time | Activity Selection Problem | Maximum meetings in one room · Maximum trains for stoppage |
| Deadline slotting | Job Sequencing Problem | Program for Shortest Job First (SJF) |
| Ratio sort | Fractional Knapsack Problem | *(contrast: 0/1 knapsack is **not** greedy — that's the lesson)* |
| Prefix codes | Huffman Coding | — |
| Canonical-coin greedy | Greedy algorithm to find minimum number of coins | *(and where it **fails** — coins {1,3,4}, target 6 — which is why DP exists)* |
| **Sort + pair extremes** | Minimum sum of absolute difference of pairs of two arrays | Maximize sum of arr[i]*i · Maximum sum of absolute difference · Maximize sum of consecutive differences in a circular array · Maximize array sum after K negations · Maximum product subset · Smallest subset with sum greater than the rest · Minimize the maximum difference between heights |
| Reach-frontier greedy | Minimum number of jumps to reach end of an array | *(the DP version is O(n²) — solve both, compare)* |
| Net-balance settlement | Minimise cash flow among friends | Minimise the cashflow (Graphs §42 — same problem, listed twice) |
| Digit greedy | Find the smallest number with given number of digits and sum of digits | — |

**Dropped as SPOJ filler:** DEFKIN, DIEHARD, GERGOVIA, Picking Up Chicks, CHOCOLA,
ARRANGE, Check if it is possible to survive on the Island, K Centers, Buy Maximum
Stocks, Water Connection, min/max to buy all candies. High keystrokes, low concept.

### Bit manipulation

| Concept | Solve | Also covers |
|---|---|---|
| Brian Kernighan | Count set bits in an integer | Sort an array according to count of set bits |
| XOR partition by bit | Find the two non-repeating elements in an array of repeating elements | Count bits to be flipped to convert A to B |
| `n & (n-1)` | Program to find whether a number is a power of two | Find position of the only set bit |
| Counting by bit position | Count total set bits in all numbers from 1 to n | — |
| Subsets as bitmasks | Power Set | *(the bridge to bitmask DP in 10)* |
| Shift-and-subtract | Divide two integers without `*`, `/`, `%` | Calculate square without `*` `/` `pow` |

### Strings & tries

| Concept | Solve | Also covers |
|---|---|---|
| Failure function | KMP Algorithm | Check whether one string is a rotation of another · Minimum characters to add at front to make a palindrome |
| Rolling hash | Rabin-Karp Algorithm | Boyer-Moore Algorithm for Pattern Searching |
| Bijective mapping | Check if two given strings are isomorphic | Find duplicate characters in a string · Find the first repeated word |
| Trie from scratch | Construct a trie from scratch | Implement a Phone Directory · Word Break (trie solution) |
| Trie for prefixes | Find the shortest unique prefix for every word | Longest Common Prefix |
| Trie of bit rows | Print unique rows in a given boolean matrix | — |
| Adjacent cancellation | Recursively remove all adjacent duplicates | Count and Say · Number of flips to make a binary string alternate |
| Constrained enumeration | Generate all valid IP addresses from a string | Split a binary string into two substrings with equal 0s and 1s |
| Roman parsing | Converting Roman Numerals to Decimal | — |

**Theory, not code:** "Why are strings immutable in Java?" — good interview answer
(hashcode caching, string pool, thread safety), zero practice value. Put it in notes.

---

## What the sheet is missing

It's from ~2020 and predates a few now-standard problems. Add these — they're each a
distinct concept with no representative above:

| Missing | Concept |
|---|---|
| Longest substring without repeating characters | the canonical variable-size window |
| Trapping Rain Water | prefix max from both sides / two pointers |
| 3Sum | sort + fix one + two pointers, with duplicate skipping |
| Course Schedule II | topo sort where the *order* is the answer |
| Number of Provinces / Redundant Connection | DSU without MST |
| Range Sum Query (mutable) | segment tree / Fenwick — the sheet has **no** range-structure problem at all |
| Implement `getRandom()` in O(1) | hash + array swap-delete, a real design question |

## Counts

| | Sheet | Kept |
|---|---|---|
| Arrays + Matrix | 25 | 13 |
| Strings | 43 | 9 (spread across topics) |
| Searching & Sorting | 35 | 8 |
| Linked List | 35 | 13 |
| Stacks & Queues | 38 | 16 |
| Binary Trees | 35 | 12 |
| BST | 22 | 12 |
| Heap | 18 | 10 |
| Graphs | 43 | 17 |
| DP | 60 | 18 |
| Backtracking | 19 | 6 |
| Greedy | 35 | 9 |
| Bit Manipulation | 10 | 6 |
| Trie | 6 | 3 |
| **Total** | **~429** | **~152** |

At the session pace in [PRACTICE.md](PRACTICE.md) (1–2 problems/session) that's roughly
three months, with every one of them logged and revisited three times — rather than 429
solved once and forgotten.
