# Convergence: UC-4 - Explain and decide a proposal beside the timetable

## Summary

- Submission: `checkpoints/UC-4.md` at `fa2323a` (base `7e276394cfc287d0a07d3e50f2642f33b6db8d88`)
- Verdict: REJECT
- Findings: 0 critical, 3 gaps, 0 protocol, 0 drift, 1 cosmetic
- Suite: independent focused normative browser passed; independent full reactor ran 195 tests with 0 failures, 1 error and 0 skips (an unrelated UC-2 CDP timeout); the exact UC-2 case passed immediately in isolation. The earlier executor full reactor passed 195/195, but cannot replace the verifier's failed full run.
- Working tree impact from verification: none tracked; `git status --short` was clean before and after tests. Screenshots remain ignored under `timetable-workspace/target/workbench-layout/`.

## Protocol Gate

1. UC-4 alone was `READY_FOR_CONVERGENCE`; UC-1, UC-2 and required UC-3 were `APPROVED`; UC-5 remained `NOT_STARTED`. No other UC was active.
2. The implementation, test, ledger and checkpoint were committed together at `fa2323a`, with a clean worktree and a valid base diff of seven attributable files. No UC-5-only behavior, route, schema, dependency or persisted field was added.
3. The checkpoint has rows for six main steps, all eleven extensions, seven guarantees, both postconditions, Requires UC-3, all eleven applicable rules, commands, changed files and related regressions. Presence of a row does not establish evidence strength.
4. `git diff --check 7e276394..fa2323a` passed. The focused real-browser journey passed independently with Docker-backed isolated PostgreSQL; fresh 1600×900 and 1280×800 screenshots were inspected. The complete independent suite did not pass cleanly, as detailed under G-3.

## Runtime Reproduction

| Actor | Step or extension | Executor reported | Converge observed |
|---|---|---|---|
| Administrator in packaged Chrome with isolated PostgreSQL | Draft to verified Proposal and two-sided canvas | Real 1,000-lesson UC-2 Draft consumed by UC-3 run; exact old Current and verified result retained | Independent focused `WorkspaceBrowserIT.reviewsIndependentlyVerifiedNormativeRepairInRealBrowser` passed. `WorkspaceBrowserIT.java:2588–2704` asserts the verifier-checked school, packaged solve result, exact stored accepted/Draft/Proposal and 1,001 representations for 1,000 stable lesson IDs. |
| Administrator | Navigate origin/destination, category/grouping, Day and focus | Minimal context adjustment, same selected ID, exact before/after | Fresh browser `:2705–2773` passed category and grouping links, origin/destination, period/room/Day adjustments, in-focus retention and out-of-focus return, retained search and exact selected fields. |
| Administrator | Review at desktop sizes and collapse/reopen | Wide non-overlay task, decisions reachable, selected change/count retained | Fresh browser `:2774–2809` passed 1600/1280 geometry and collapse/reopen. Screenshots show time headings and class row beside inspector, compact subject/room and protection cue, with decisions in the task area. It does not measure Proposal's 1279/701/700 boundary behavior or the matrix scroll offset after collapse/reopen (G-2). |
| Administrator | Revise, discard, stale/failed acceptance and success | Exact old Current on refusal; exact successor only after confirmation | Browser `:1808–1845,2812–2932` and HTTP/JDBC `WorkspaceRepairPlanningIT.java:290–373` passed exact document/version and lifecycle assertions. No unverified result was presented as Current. |
| Administrator | Review-only interactions | Exact stored data unchanged | Browser `:1795–1798,2473,2773` compared durable state, but did not record whether a forbidden mutating request was attempted and refused (G-1). |
| Administrator | Narrow Proposal | 390 px read-only agenda | Browser `:2526–2537` passed a supplemental presentation-only comparison fixture at 390 px. The normative verified Proposal was not exercised at 390, nor Proposal at 701/700 px (G-2). |

## Evidence Ledger

| Contract element | Executor claim | Evidence obtained | Strength | Verified |
|---|---|---|---|---|
| Main 1 | Accepted/proposed canvas and wide task | Browser `WorkspaceBrowserIT.java:2621–2636,2693–2704`; focused PASS | STRONG | yes |
| Main 2 | Authoritative review values | Browser `:2637–2683` compares exact unique, categories, groupings, effects, protection and run facts | STRONG | yes |
| Main 3 | Choose both move targets | Browser `:2735–2762` exercises category, detail and grouping links at both sides | STRONG | yes |
| Main 4 | Stable identity and exact selected fields | Browser `:2705–2737,2757–2759` checks selected ID, target side and old/proposed named values; task detail duplicates them | STRONG | yes |
| Main 5 | Inspect, switch modes/focus and confirm | Browser `:1792–1798,2684–2691,2751–2812` exercises Current/Draft/Proposal, Day/focus and confirmation | STRONG | yes |
| Main 6 | Atomic exact successor and retained context | Browser `:2812–2820,3050–3061` compares exact successor and removed Draft/Proposal; outside-Day selection clears with explanation | STRONG | yes |
| Extension 1a | Ineligible candidate never shown | Browser `:2224–2228` and HTTP/JDBC `WorkspaceRepairPlanningIT.java:420–450` return failed/rejected output to exact Draft without Proposal | STRONG | yes |
| Extension 1b | Narrow Proposal read-only | Supplemental 390 px browser `:2526–2537` passes, but the required 701/700 boundary and normative 390 Proposal are absent | WEAK (G-2) | no |
| Extension 2a | Zero categories/groupings labelled | Browser `:2865–2885` compares every zero label and exact unchanged document | STRONG | yes |
| Extension 3a | Necessary context adjustment only | Browser `:2540–2583,2735–2762` proves both-sided match, Day/period/room changes, in/out-of-focus routing and retained search | STRONG | yes |
| Extension 3b | Combined same-slot tile | Browser `:2509–2513,2721–2734` checks one tile and old/new room values | STRONG | yes |
| Extension 3c | One-sided addition/cancellation | Browser `:2509–2523` checks exact existing side and status | STRONG | yes |
| Extension 4a | Unchanged lesson outside totals | Browser `:2523–2526,2764–2773` checks accepted/unchanged detail and protection | STRONG | yes |
| Extension 5a | Revise to retained Draft | Browser `:2827–2860` consumes verified Proposal and compares exact Draft/Current | STRONG | yes |
| Extension 5b | Discard only Proposal | Browser `:2827–2860,2889–2913` compares exact retained Draft/Current and absence of Proposal | STRONG | yes |
| Extension 6a | Stale identity invalidates Proposal | Browser `:2915–2932` and HTTP/JDBC `WorkspaceRepairPlanningIT.java:306–340` assert refusal and exact Current/Draft | STRONG | yes |
| Extension 6b | Failed persistence keeps Proposal | Browser `:1808–1835` and HTTP/JDBC `WorkspaceRepairPlanningIT.java:343–373` assert byte-exact document/version and explicit retry | STRONG | yes |
| G1 | Stable IDs and distinct authoritative totals | Browser `:2509–2513,2637–2666,2693–2704` checks all 1,000 IDs, two move sides, one combined tile and category/direct/ripple counts | STRONG | yes |
| G2 | Either-side matching and exact values | Browser `:2540–2583,2707–2762`, optional missing room cue `:2511–2513` | STRONG | yes |
| G3 | Task geometry, decisions, collapse/scroll | Desktop geometry and selection/count pass `:2774–2809`; no direct matrix scroll assertion or 1279 Proposal geometry | WEAK (G-2) | no |
| G4 | Presentation has no data change or mutation request | Exact PostgreSQL document passes `:1795–1798,2473,2773`; mutating fetch absence is not observed | WEAK (G-1) | no |
| G5 | Non-color cues and distinct availability | Browser `:2684–2689,2757–2770` checks text/structure, accessible protection and accepted/proposed load | STRONG | yes |
| G6 | Only successful acceptance advances Current | Browser `:1808–1845,2812–2820,2915–2932` and HTTP/JDBC `:306–373` compare complete documents and lifecycle | STRONG | yes |
| G7 | Normative browser all shapes/decisions/narrow and exact state | Main normative journey and supplemental comparison shapes pass; Proposal breakpoint coverage is incomplete and independent full reactor had one CDP error | WEAK (G-2, G-3) | no |
| Success postcondition | Exact successor sole Current | Browser `:2812–2820,3050–3061` checks exact successor, only Current and complete return | STRONG | yes |
| Minimal guarantee | Old accepted bundle exact on refusal/review | Browser `:1808–1845,2827–2932` and HTTP/JDBC `:290–373` compare exact data and version | STRONG | yes |
| Requires UC-3 | Consume actual verified Proposal | Browser `:2612–2636` runs from saved ready Draft through packaged solve and independent verifier before Proposal review | STRONG | yes |

## Rule Conformance

| Rule | Constraint | Evidence | Result |
|---|---|---|---|
| RULE-1 | “MUST extend the existing packaged workspace” and “MUST NOT fork accepted assignments” | Seven-file diff contains one `app.js`/inspection-state presentation path, no route/schema/dependency; IDs agree across modes | PASS |
| RULE-2 | Presentation actions “MUST NOT issue durable mutation requests” | State owner is unchanged except ephemeral target side; exact DB comparison passes, but no UC-4 mutating-request trace | GAP G-1 |
| RULE-3 | Existing services “MUST enforce only” declared transitions | No service diff; `WorkspaceRepairPlanningIT.java:290–373,420–450` proves stale/refused/failed outcomes by value | PASS |
| RULE-5 | Task area “MUST” sit beneath canvas and verify geometry at 1279/1280 | 1600/1280 geometry passes `WorkspaceBrowserIT.java:2774–2809`; no UC-4 1279/701 geometry or collapse scroll measurement | GAP G-2 |
| RULE-6 | Filters “MUST intersect” and Proposal matching “MUST consider either” side | Browser `:2540–2583,2735–2762` checks joined sides, matching label, unique count and necessary adjustments | PASS |
| RULE-7 | Exact authoritative names/IDs and candidate refusal “MUST” hold | Browser `:2707–2734`, verifier-gated proposal `:2612–2636`, optional unavailable-name cue `:2511–2513` | PASS |
| RULE-10 | Stable-ID index “MUST” support both move targets and distinct totals | Browser `:2509–2583,2637–2762` checks shapes, target selection and exact review counts | PASS |
| RULE-11 | Only explicit successful acceptance “MAY advance Current” | Browser `:1808–1845,2812–2932` and HTTP/JDBC `WorkspaceRepairPlanningIT.java:306–373` check exact success and refusals | PASS |
| RULE-12 | At ≤700 px UI “MUST” be read-only; route/access table “MUST remain unchanged” | No server diff and supplemental 390 px passes; Proposal at 700 and 701 is untested | GAP G-2 |
| RULE-13 | New strings “MUST use” catalog and state “MUST” have non-color cues | `messages.js:129–166`; browser `:2684–2689,2757–2770` checks text/structure, native buttons and accessible name | PASS |
| RULE-14 | Verification strategy “MUST be met” with normative viewport data and complete shared browser regression | 1279/701/700 Proposal evidence is missing; 195-test independent run had one CDP error despite exact isolated rerun PASS | GAP G-2, G-3 |

## Related-UC Regression

| Use case | Relationship/shared surface | Evidence | Result |
|---|---|---|---|
| Workbench-layout UC-1–UC-3 | Accepted canvas, Draft task, run handoff, mode/focus, shared browser state | Independent full run exercised all 36 browser cases and 84 workspace integration cases; `preparesProtectedRepairDraftInRealBrowser` timed out at CDP command `WorkspaceBrowserIT.java:1499`, then passed alone. Earlier executor full reactor passed 195/195. | INCONCLUSIVE full gate (G-3) |
| Existing repair/import/initial planning | Services and local route boundary shared with Proposal | Independent full run: 19/19 import, 9/9 Draft, 10/10 repair planning, 8/8 initial planning, 2/2 Flyway failure; kernel 85/85 | PASS |
| `timetable-ux-polish` UC-5 | Shared browser repair journey | No status change or participant gate claim; independent suite's one CDP timeout prevents claiming a clean shared browser run | INCONCLUSIVE full gate (G-3) |

## Findings

- G-1 GAP — UC-4 G4 states review/mode/filter actions do not mutate state, and RULE-2 says presentation actions “MUST NOT issue durable mutation requests.” `WorkspaceBrowserIT.java:1795–1798,2473,2773` compares the stored document but never intercepts or counts mutating `fetch` calls during UC-4's review actions. A refused request could leave that document exact. Revision outcome: instrument the normative browser journey before review-only actions; assert zero mutating requests after category/group, origin/destination, mode, search, filters, focus and task collapse/reopen, then assert only the explicit confirmed acceptance request occurs.
- G-2 GAP — UC-4 G3 requires collapse/reopen to retain representable canvas scroll, G7 requires normative-scale narrow behavior, RULE-5 requires 1279/1280 geometry, and RULE-14's strategy says to exercise 701/700 boundaries. `WorkspaceBrowserIT.java:2774–2809` measures Proposal only at 1600/1280, `:2526–2537` checks 390 px only with a supplemental presentation fixture, and collapse/reopen checks selection/count but not matrix scroll. Revision outcome: on the verified 1,000-lesson Proposal, assert Proposal layout at 1279 and 701, read-only accepted/proposed agenda at 700 and 390, no page overflow, and scroll retention through task collapse/reopen; restore desktop context before acceptance.
- G-3 GAP — RULE-14 requires the complete shared browser regression and related approved UCs green. `./mvnw -q -pl timetable-workspace -am verify` ran 195 tests with one error: `WorkspaceBrowserIT.preparesProtectedRepairDraftInRealBrowser` timed out in `Cdp.command` at `WorkspaceBrowserIT.java:1499`. The exact UC-2 case passed immediately in isolation and two executor full runs passed, so this is not a reproduced product regression, but the verifier cannot call this full run green. Revision outcome: rerun the full reactor on the revised checkpoint and obtain 0 failures/errors/skips, or diagnose any repeat.
- K-1 COSMETIC — `checkpoints/UC-4.md` Main 4 cites `WorkspaceBrowserIT.java:3120–3123` for the exact-side helper, which is now at `:3132–3135` after added assertions. The same row's `:2705–2737` contains the by-value browser assertions, so this stale pointer does not block independently; correct it in the revised checkpoint.

## Walkthrough

No administrator walkthrough is requested yet because the automated evidence gate is not complete. When the revised UC-4 passes, derive the script from its accepted/proposed review, both move targets, one-sided and combined changes, counts, mode/focus, task disclosure, explicit decision and narrow read-only branches. Fault injection remains automated rather than a manual prerequisite.

## Status Update

UC-4 `READY_FOR_CONVERGENCE` → `NEEDS_REVISION`; UC-5 remains `NOT_STARTED` because it requires approved UC-4. UC-1, UC-2 and UC-3 remain `APPROVED`. No other feature's verdict changes.

## Response to execute

REVISE UC-4: G-1 request-absence proof, G-2 Proposal viewport/scroll proof, and G-3 clean full regression.
