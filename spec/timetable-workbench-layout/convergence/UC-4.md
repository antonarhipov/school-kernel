# Convergence: UC-4 - Explain and decide a proposal beside the timetable

## Summary

- Submission: `checkpoints/UC-4.md` at `f519d4a95a8cc1f0df9d061dc546f59930cf775d` (revision base `e2137b1`; initial implementation `fa2323a`)
- Verdict: APPROVE
- Findings: 0 critical, 0 gaps, 0 protocol, 0 drift, 0 cosmetic; prior G-1, G-2, G-3 and K-1 resolved
- Suite: independent focused normative browser passed; independent full reactor passed 195 tests (58 kernel unit, 27 kernel integration, 26 workspace unit, 84 workspace integration including 36 browser), with 0 failures, errors or skips.
- Working tree impact from verification: none tracked; `git status --short` was clean before and after tests. Screenshots remain ignored under `timetable-workspace/target/workbench-layout/`.

## Protocol Gate

1. UC-4 alone was `READY_FOR_CONVERGENCE`; UC-1, UC-2 and required UC-3 were `APPROVED`; UC-5 remained `NOT_STARTED`. No other UC was active.
2. The prior full implementation was committed at `fa2323a`; the revision's CSS, browser assertion, ledger and replacement checkpoint were committed together at `f519d4a`. Both submission boundaries are immutable and the worktree was clean before and after independent checks. `git diff e2137b1..f519d4a` contains only those four attributable revision files, no UC-5-only behavior, route, schema, dependency or persisted field.
3. The replacement checkpoint has rows for six main steps, all eleven extensions, seven guarantees, both postconditions, Requires UC-3, all eleven applicable rules, commands, changed files and related regressions. UC-1 through UC-3 remain approved, and no other UC is active.
4. `git diff --check e2137b1..f519d4a` passed. The focused real-browser journey and full reactor passed independently with Docker-backed isolated PostgreSQL and packaged assets/kernel. Fresh 1600×900 and 1280×800 screenshots were inspected; the browser also measured 1279/701 and read the verified Proposal at 700/390.

## Runtime Reproduction

| Actor | Step or extension | Executor reported | Converge observed |
|---|---|---|---|
| Administrator in packaged Chrome with isolated PostgreSQL | Draft to verified Proposal and two-sided canvas | Real 1,000-lesson UC-2 Draft consumed by UC-3 run; exact old Current and verified result retained | Independent focused and full browser runs passed `WorkspaceBrowserIT.java:2588–2716`: verifier-checked school, packaged solve result, exact stored accepted/Draft/Proposal and 1,001 representations for 1,000 stable lesson IDs. |
| Administrator | Navigate origin/destination, category/grouping, Day and focus | Minimal context adjustment, same selected ID, exact before/after | Fresh browser `:2717–2785` passed category/grouping links, both move targets, period/room/Day adjustments, in-focus retention and out-of-focus return, retained search and exact selected fields. |
| Administrator | Review at desktop/intermediate sizes and collapse/reopen | Wide non-overlay task, decisions reachable, selected change/count and scroll retained | Browser `:2786–2852` passed 1600/1280/1279/701 geometry, no page or task horizontal overflow, a heading and complete row, below-canvas task, reachable decisions and exact horizontal/vertical matrix scroll retention. Fresh desktop screenshots show the side inspector, compact subject/room/protection cue and decisions in the task area. The 701 px test exposed a 10 px task overflow, fixed by stacking before/after panels at `styles.css:423–426`; the rerun and complete reactor passed. |
| Administrator | Revise, discard, stale/failed acceptance and success | Exact old Current on refusal; exact successor only after confirmation | Browser `:1808–1845,2859–2985` and HTTP/JDBC `WorkspaceRepairPlanningIT.java:290–373` passed exact document/version and lifecycle assertions. No unverified result was presented as Current. |
| Administrator | Review-only interactions | Exact stored data and no mutation request | Browser `:2630–2641,2853–2868` recorded zero non-GET/HEAD requests through mode, category/group, side, search/filter, focus, responsive and task actions, compared the exact durable document, then recorded only `POST /api/proposal/accept` after explicit confirmation. |
| Administrator | Narrow verified Proposal | Read-only accepted/proposed agenda at both boundaries | Browser `:2829–2838` selected class 16 at 700 and 390 px, found both sides of lesson 960 with accepted/proposed labels and true lifecycle, no mutation/desktop controls or page overflow, and exact unchanged durable state. |

## Evidence Ledger

| Contract element | Executor claim | Evidence obtained | Strength | Verified |
|---|---|---|---|---|
| Main 1 | Accepted/proposed canvas and wide task | Browser `WorkspaceBrowserIT.java:2621–2648,2696–2716`; focused and full PASS | STRONG | yes |
| Main 2 | Authoritative review values | Browser `:2649–2695` compares exact unique, categories, groupings, effects, protection and run facts | STRONG | yes |
| Main 3 | Choose both move targets | Browser `:2747–2778` exercises category, detail and grouping links at both sides | STRONG | yes |
| Main 4 | Stable identity and exact selected fields | Browser `:2717–2749,3186–3190` checks selected ID, target side and old/proposed named values; task detail duplicates them | STRONG | yes |
| Main 5 | Inspect, switch modes/focus and confirm | Browser `:1792–1798,2696–2703,2763–2863` exercises Current/Draft/Proposal, Day/focus and confirmation without write | STRONG | yes |
| Main 6 | Atomic exact successor and retained context | Browser `:2863–2875,3100–3115` compares exact successor and removed Draft/Proposal; outside-Day selection clears with explanation | STRONG | yes |
| Extension 1a | Ineligible candidate never shown | Browser `:2224–2228` and HTTP/JDBC `WorkspaceRepairPlanningIT.java:420–450` return failed/rejected output to exact Draft without Proposal | STRONG | yes |
| Extension 1b | Narrow Proposal read-only | Verified normative Proposal browser `:2829–2838` shows labelled accepted/proposed sides at 700 and 390, absent decisions/matrix, no page overflow and unchanged DB | STRONG | yes |
| Extension 2a | Zero categories/groupings labelled | Browser `:2919–2938` compares every zero label and exact unchanged document | STRONG | yes |
| Extension 3a | Necessary context adjustment only | Browser `:2540–2583,2747–2778` proves both-sided match, Day/period/room changes, in/out-of-focus routing and retained search | STRONG | yes |
| Extension 3b | Combined same-slot tile | Browser `:2509–2513,2733–2746` checks one tile and old/new room values | STRONG | yes |
| Extension 3c | One-sided addition/cancellation | Browser `:2509–2523` checks exact existing side and status | STRONG | yes |
| Extension 4a | Unchanged lesson outside totals | Browser `:2523–2526,2776–2785` checks accepted/unchanged detail and protection | STRONG | yes |
| Extension 5a | Revise to retained Draft | Browser `:2883–2915` consumes verified Proposal and compares exact Draft/Current | STRONG | yes |
| Extension 5b | Discard only Proposal | Browser `:2883–2915,2945–2968` compares exact retained Draft/Current and absence of Proposal | STRONG | yes |
| Extension 6a | Stale identity invalidates Proposal | Browser `:2969–2985` and HTTP/JDBC `WorkspaceRepairPlanningIT.java:306–340` assert refusal and exact Current/Draft | STRONG | yes |
| Extension 6b | Failed persistence keeps Proposal | Browser `:1808–1835` and HTTP/JDBC `WorkspaceRepairPlanningIT.java:343–373` assert byte-exact document/version and explicit retry | STRONG | yes |
| G1 | Stable IDs and distinct authoritative totals | Browser `:2509–2513,2649–2678,2705–2716` checks all 1,000 IDs, two move sides, one combined tile and category/direct/ripple counts | STRONG | yes |
| G2 | Either-side matching and exact values | Browser `:2540–2583,2719–2778`, optional missing room cue `:2511–2513` | STRONG | yes |
| G3 | Task geometry, decisions, collapse/scroll | Browser `:2786–2852` measures 1600/1280/1279/701 layout, no task/page overflow, accessible decisions, exact selected/count and both real matrix scroll offsets across collapse/reopen; CSS `:423–426` removes measured 701 overflow | STRONG | yes |
| G4 | Presentation has no data change or mutation request | Browser `:2630–2641,2853–2868` captures zero non-read-only fetch requests during review actions, exact PostgreSQL document/version and only one confirmed accept POST | STRONG | yes |
| G5 | Non-color cues and distinct availability | Browser `:2696–2701,2769–2782` checks text/structure, accessible protection and accepted/proposed load | STRONG | yes |
| G6 | Only successful acceptance advances Current | Browser `:1808–1845,2859–2875,2969–2985` and HTTP/JDBC `WorkspaceRepairPlanningIT.java:306–373` compare complete documents and lifecycle | STRONG | yes |
| G7 | Normative browser all shapes/decisions/narrow and exact state | Verified 1,000-lesson Proposal in `:2588–2875` plus supplemental shapes `:2486–2583` and refusal tests `:2883–2985`; independent 36/36 browser and 195/195 reactor pass | STRONG | yes |
| Success postcondition | Exact successor sole Current | Browser `:2863–2875,3100–3115` checks exact successor, only Current and complete return | STRONG | yes |
| Minimal guarantee | Old accepted bundle exact on refusal/review | Browser `:1808–1845,2853–2868,2883–2985` and HTTP/JDBC `WorkspaceRepairPlanningIT.java:290–373` compare exact data and version | STRONG | yes |
| Requires UC-3 | Consume actual verified Proposal | Browser `:2612–2648` runs from saved ready Draft through packaged solve and independent verifier before Proposal review | STRONG | yes |

## Rule Conformance

| Rule | Constraint | Evidence | Result |
|---|---|---|---|
| RULE-1 | “MUST extend the existing packaged workspace” and “MUST NOT fork accepted assignments” | Initial UC-4 diff uses one `app.js`/inspection-state presentation path; revision changes only CSS/browser test, no route/schema/dependency; IDs agree across modes | PASS |
| RULE-2 | Presentation actions “MUST NOT issue durable mutation requests” | Browser `WorkspaceBrowserIT.java:2630–2641,2853–2868` records zero mutating fetches and exact document/version through presentation actions, then one confirmed accept POST | PASS |
| RULE-3 | Existing services “MUST enforce only” declared transitions | No service diff; `WorkspaceRepairPlanningIT.java:290–373,420–450` proves stale/refused/failed outcomes by value | PASS |
| RULE-5 | Task area “MUST” sit beneath canvas and verify geometry at 1279/1280 | Browser `:2786–2852` measures 1600/1280 side-inspector and 1279/701 stacked geometry, ≤35% task height, visible heading/row, reachable decisions, no horizontal task/page clipping and scroll retention; CSS `:423–426` fixes 701 overflow | PASS |
| RULE-6 | Filters “MUST intersect” and Proposal matching “MUST consider either” side | Browser `:2540–2583,2747–2778` checks joined sides, matching label, unique count and necessary adjustments | PASS |
| RULE-7 | Exact authoritative names/IDs and candidate refusal “MUST” hold | Browser `:2719–2746`, verifier-gated proposal `:2612–2648`, optional unavailable-name cue `:2511–2513` | PASS |
| RULE-10 | Stable-ID index “MUST” support both move targets and distinct totals | Browser `:2509–2583,2649–2778` checks shapes, target selection and exact review counts | PASS |
| RULE-11 | Only explicit successful acceptance “MAY advance Current” | Browser `:1808–1845,2859–2985` and HTTP/JDBC `WorkspaceRepairPlanningIT.java:306–373` check exact success and refusals | PASS |
| RULE-12 | At ≤700 px UI “MUST” be read-only; route/access table “MUST remain unchanged” | No server diff; verified Proposal at 700/390 in browser `:2829–2838` shows labelled accepted/proposed agenda with no decisions, matrix, page overflow or durable write; hostile local-access regressions passed | PASS |
| RULE-13 | New strings “MUST use” catalog and state “MUST” have non-color cues | `messages.js:129–166`; browser `:2696–2701,2769–2782` checks text/structure, native buttons and accessible name | PASS |
| RULE-14 | Verification strategy “MUST be met” with normative viewport data and complete shared browser regression | Verified 1,000-lesson browser `:2588–2875` covers 1600/1280/1279/701/700/390, exact JDBC and request/scroll comparisons; independent full reactor 195/195, including browser 36/36 | PASS automation; walkthrough pending |

## Related-UC Regression

| Use case | Relationship/shared surface | Evidence | Result |
|---|---|---|---|
| Workbench-layout UC-1–UC-3 | Accepted canvas, Draft task, run handoff, mode/focus, shared browser state | Independent full reactor passed all 36 browser and 84 workspace integration cases, including the UC-2 case that timed out once in the prior audit; their approval verdicts remain unchanged | PASS |
| Existing repair/import/initial planning | Services and local route boundary shared with Proposal | Independent full run: 19/19 import, 9/9 Draft, 10/10 repair planning, 8/8 initial planning, 2/2 Flyway failure; kernel 85/85 | PASS |
| `timetable-ux-polish` UC-5 | Shared browser repair journey | Independent shared suite passed; no status change or participant-gate claim. That feature's five-administrator gate remains `PENDING_WALKTHROUGH` in its own ledger | PASS automation only |

## Findings

None. Prior G-1 is resolved by the request trace at `WorkspaceBrowserIT.java:2630–2641,2853–2868`; G-2 by the 1279/701/700/390 and scroll assertions at `:2786–2852` plus the 701 px CSS fix; G-3 by this independent 195/195 reactor; K-1 by the corrected helper pointer at `:3186–3190` in the replacement checkpoint. No production, test or documentation defect remains open from this audit.

## Walkthrough

Automated convergence passed. The administrator should perform this UC-4-derived walkthrough on a verified Proposal, preferably the same long-name validation school:

1. In Proposal, identify the accepted Current versus the not-current Proposal. Read the unique changed total, zero categories, grouping counts, direct/ripple effects, protection and safe run evidence in the wide task area while the canvas remains visible.
2. Choose a period move from a category or grouping, visit both labelled accepted origin and proposed destination, and explain the exact old/new weekday, period, teacher and room for one stable lesson. Navigate to a side outside the current Day/filter/focus and confirm the announced, minimal context adjustment without losing search/highlights.
3. Inspect a same-slot room change, an unchanged protected lesson, and the available addition/cancellation demonstration; distinguish each from the unique changed count. Switch Current, Draft and Proposal, use a focused schedule and return, then collapse/reopen the review area and confirm selection/count/canvas context remain understandable.
4. At a narrow width, confirm the Proposal is a read-only accepted/proposed agenda with no revise, discard or accept control. At desktop width, decide deliberately: verify confirmation alone does not accept, then explicitly accept the verified Proposal and identify only the new Current, with no Draft, Proposal or open repair task. The alternate revise/discard paths and stale/storage failures are covered by automation rather than requiring the administrator to force failures.

User result: on 2026-09-25, the user replied `PASS` to the UC-4 walkthrough request. No additional per-step observations were supplied. This confirms UC-4 only; it does not satisfy the separate `timetable-ux-polish` UC-5 five-administrator gate or the later `timetable-workbench-layout` UC-5 feature gate.

## Status Update

UC-4 `READY_FOR_CONVERGENCE` → `PENDING_WALKTHROUGH` after the automated audit, then `PENDING_WALKTHROUGH` → `APPROVED` after explicit administrator confirmation. UC-5 is now eligible but remains `NOT_STARTED`. UC-1, UC-2 and UC-3 remain `APPROVED`. No other feature's verdict changes.

## Response to execute

APPROVED
