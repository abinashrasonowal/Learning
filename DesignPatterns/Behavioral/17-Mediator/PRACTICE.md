# 17-Mediator — Session Plan

Goal is not "broadcast a chat message" — that's the course example and its mediator
holds no rule. It's **move a real coordination rule out of every colleague into one
mediator, and make colleagues forget each other exist**. Different domain on purpose.

---

## Layout

```
17-Mediator/
  mediator/problem/Aircraft.java              peer list, checks every peer before landing
  mediator/problem/MediatorProblemMain.java   forgets one link -> collision
  mediator/solution/Tower.java                the mediator interface
  mediator/solution/ControlTower.java         one runway + FIFO holding queue
  mediator/solution/Aircraft.java             knows only its Tower
  mediator/solution/MediatorSolutionMain.java
```

```bash
javac -Xlint:all -d out $(find . -name '*.java') && java -ea -cp out mediator.solution.MediatorSolutionMain
```

## Rules for `solution/`

- `Aircraft` has **no** reference to another `Aircraft`
- the "one on the runway" rule exists in exactly one class
- waiting aircraft are **told** when it's their turn — no polling
- tower callbacks (`clearedToLand`, `hold`) are package-private
- unregistered aircraft / wrong aircraft vacating → fail loudly
- prove it with `assert`, not prints

---

## 1. Read — 15 min

`NOTES.md` top to bottom.

## 2. Build — ~45 min

| Step | Build | Must prove |
|---|---|---|
| **problem** | peer-to-peer aircraft, 3 of them, one link forgotten | the collision — assert both on the runway |
| **solution** | `Tower`, `ControlTower`, `Aircraft` | same 3-aircraft scenario: exactly one on the runway |
| **queue** | FIFO holding, hand-off on vacate | B then C get the runway in order, no polling |
| **4th aircraft** | add one more | one `register` call, zero edits to the others |

## 3. Break it on purpose

- Make `clearedToLand()` public and call it from `main` on a holding aircraft.
  Assert two on the runway. Explain in a comment why visibility is part of the
  pattern. Revert.
- Add a second rule — emergency aircraft jump the queue. Notice it's one edit, in
  `ControlTower`, and none in `Aircraft`.

## 4. Self-check — 10 min

1. Why does the problem need n(n-1)/2 links and the solution n?
2. Mediator vs Observer — who holds the logic?
3. Mediator vs Facade?
4. What's the god-mediator risk and how do you split it?
5. What's wrong with the chat-room example's two-step registration?

## 5. Commit

```bash
git add DesignPatterns/17-Mediator && git commit -m "mediator: <step>"
```

---

## Done when

- [x] Watched the course video, did the course exercise
- [ ] Read `NOTES.md`
- [x] problem — written, runs, collision asserted
- [x] solution — one on the runway, FIFO hand-off asserted
- [x] 4th aircraft with zero edits to the others
- [ ] broken once (public callback), reverted
- [ ] All 5 self-check questions answered without looking
- [ ] Committed

Then tick `17 — Mediator` in `../README.md`.

**Stuck?** Ask me for a nudge on the pattern — not the code.
