# Use-Case Checkpoint: UC-4 - Explain and decide a proposal beside the timetable

## Summary

- Status: READY_FOR_CONVERGENCE
- Base commit: `7e276394cfc287d0a07d3e50f2642f33b6db8d88`
- Submission commit: HEAD at convergence
- Relations verified: Requires UC-3; the browser begins with UC-2's saved ready Draft, runs the packaged repair path, independently verifies the feasible Proposal, and consumes that actual Proposal rather than a mocked review.

## Contract Evidence

| Contract element | Test or runtime evidence | Result |
|---|---|---|
| Main 1 | `WorkspaceBrowserIT.java:2621–2636,2693–2704`: exact old Current and verified Proposal share the 1,000-ID canvas, with both sides of the move and explicit not-current label. | PASS |
| Main 2 | `WorkspaceBrowserIT.java:2637–2683`: authoritative unique/category/group/direct/ripple/protection/run values in the wide area. | PASS |
| Main 3 | `WorkspaceBrowserIT.java:2735–2766`: category, detail and grouping origin/destination links; in-focus and out-of-focus navigation. | PASS |
| Main 4 | `WorkspaceBrowserIT.java:2705–2737,3120–3123`: exact before/after subject/class/teacher/weekday/period/room names and IDs, dimensions and effects in inspector and task area. | PASS |
| Main 5 | `WorkspaceBrowserIT.java:1792–1798,2684–2691,2763–2812`: other lessons, mode/range/focus, unchanged accepted Current and explicit decision confirmation. | PASS |
| Main 6 | `WorkspaceBrowserIT.java:2812–2820,3057–3058`: exact successor accepted, only Current remains, representable context retained and unrepresentable Day selection explained. | PASS |
| Extension 1a | `WorkspaceBrowserIT.java:2224–2228`; `WorkspaceRepairPlanningIT.java:420–450`: invalid/rejected candidate returns to Draft with no Proposal or Current change; `WorkspaceImportIT.java:395–411` rejects missing required accepted metadata at the verifier boundary. | PASS |
| Extension 1b | `WorkspaceBrowserIT.java:2526–2537`: 390 px labelled read-only agenda and no desktop decisions; shared browser suite covers 700/701 px. | PASS |
| Extension 2a | `WorkspaceBrowserIT.java:2865–2885`: zero category/group labels and no accepted advance. | PASS |
| Extension 3a | `WorkspaceBrowserIT.java:2540–2583,2735–2766`: one-sided narrowing, both move targets, Day/period/room/focus adjustments, retained search and highlights. | PASS |
| Extension 3b | `WorkspaceBrowserIT.java:2509–2513,2721–2734`: one combined same-slot changed tile with old/proposed room. | PASS |
| Extension 3c | `WorkspaceBrowserIT.java:2509–2523`: addition and cancellation show their sole existing side and explicit status. | PASS |
| Extension 4a | `WorkspaceBrowserIT.java:2523–2526,2764–2773`: accepted unchanged/protected lesson remains outside changed total. | PASS |
| Extension 5a | `WorkspaceBrowserIT.java:2827–2860`: verified Proposal revision opens exact editable Draft, preserves Current. | PASS |
| Extension 5b | `WorkspaceBrowserIT.java:2827–2860,2889–2913`: discard removes only Proposal and preserves exact accepted/Draft documents. | PASS |
| Extension 6a | `WorkspaceBrowserIT.java:2915–2932`; `WorkspaceRepairPlanningIT.java:306–340`: stale identity invalidates Proposal to Draft with exact old Current. | PASS |
| Extension 6b | `WorkspaceBrowserIT.java:1808–1835`; `WorkspaceRepairPlanningIT.java:343–373`: injected storage failure keeps exact old document/version and reviewable Proposal for retry. | PASS |
| G1 | `WorkspaceBrowserIT.java:2637–2666,2693–2704,2509–2513`: stable IDs, one combined tile, two move sides, authoritative distinct and overlapping counts. | PASS |
| G2 | `WorkspaceBrowserIT.java:2540–2583,2707–2766`: either-side matching and exact old/new IDs with named target; missing optional name has stable ID/unavailable cue. | PASS |
| G3 | `WorkspaceBrowserIT.java:2774–2809`; inspected `target/workbench-layout/uc4-proposal-{1600,1280}.png`: task ≤35% viewport, headings and class row with side inspector, reachable decisions, collapse/reopen retains selection/count. | PASS |
| G4 | `WorkspaceBrowserIT.java:1795–1798,2473,2760–2762,2773`: presentation actions leave exact stored document/version; only explicit guarded decision routes write. | PASS |
| G5 | `WorkspaceBrowserIT.java:2684–2689,2757–2770`: text/structural origin, destination, effect, pin, selection, availability and accepted/Proposal cues; complete accessible name. | PASS |
| G6 | `WorkspaceBrowserIT.java:1808–1845,2812–2820,2915–2932`; `WorkspaceRepairPlanningIT.java:306–373`: only confirmed, revalidated and durable success advances Current. | PASS |
| G7 | `WorkspaceBrowserIT.java:2588–2932`: normative-scale real browser covers all comparison shapes, filters, counts, navigation, protection, decisions, narrow view and exact JDBC state; all 36 shared browser cases passed. | PASS |
| Success postcondition | `WorkspaceBrowserIT.java:2812–2820`: exact verified successor becomes the sole Current, with whole-school context and no repair task area. | PASS |
| Minimal guarantee | `WorkspaceBrowserIT.java:1808–1845,2827–2860,2915–2932`: no presentation, refusal, failed storage, revise or discard path partially advances Current. | PASS |
| Requires UC-3 | `WorkspaceBrowserIT.java:2612–2636`: actual Draft solve invokes packaged run and independent verifier, then UC-4 reads the persisted verified Proposal and comparison context. | PASS |

## Rule Evidence

| Rule | Evidence | Result |
|---|---|---|
| RULE-1 | `app.js:26–42,211–349`, existing accepted-model/comparison index, and no route/schema/dependency changes. | PASS |
| RULE-2 | `inspection-state.js:5–129`; browser mode/filter/focus/task and exact no-write assertions at `WorkspaceBrowserIT.java:1795–1798,2473,2760–2762,2773`. | PASS |
| RULE-3 | `WorkspaceRepairPlanningIT.java:290–373,420–450`: guarded service lifecycle, stale identity and storage refusal; UI uses existing routes. | PASS |
| RULE-5 | `styles.css:338–380,425–428`; `WorkspaceBrowserIT.java:2774–2809`: wide task owns review/decisions, inspector stays beside canvas and task does not overlay it. | PASS |
| RULE-6 | `app.js:689–748,1151–1232`; `WorkspaceBrowserIT.java:2540–2583,2693–2704,2735–2762`: complete population and either-side narrowing with labelled adjustments. | PASS |
| RULE-7 | `app.js:802–873,1151–1173`; `WorkspaceBrowserIT.java:2707–2734,2757–2770`: exact display names/IDs, distinct availability, escaped text and complete accessible name. | PASS |
| RULE-10 | `app.js:214–370,802–833`; `WorkspaceBrowserIT.java:2509–2583,2637–2762`: stable two-sided index, group navigation, exact distinct totals and all comparison shapes. | PASS |
| RULE-11 | `app.js:290–296`; `WorkspaceBrowserIT.java:1808–1845,2812–2860,2915–2932`: existing guarded decisions, exact acceptance and retry/refusal state. | PASS |
| RULE-12 | No server/security diff; shared hostile Host/Origin/CSRF/`If-Match` and 700/701/390 px browser regression; `WorkspaceBrowserIT.java:2526–2537`. | PASS |
| RULE-13 | `messages.js:129–166`, native review links/confirmation controls in `app.js:214–370`, structural and accessible cues in `WorkspaceBrowserIT.java:2757–2770`. | PASS |
| RULE-14 | `WorkspaceBrowserIT.java:2588–2932,3117–3135`: isolated normative school, real browser/process/JDBC, exact successor and 36/36 shared browser cases passed; human walkthrough is not claimed. | PASS |

## Validation

- Focused command: `./mvnw -q -pl timetable-workspace -am '-Dit.test=WorkspaceBrowserIT#reviewsIndependentlyVerifiedNormativeRepairInRealBrowser' -Dfailsafe.failIfNoSpecifiedTests=false verify` — final-diff run passed with Docker access: 1 real-browser test, 26 workspace unit tests and 58 kernel tests; 0 failures/errors/skips.
- Full relevant suite: `./mvnw -q -pl timetable-workspace -am verify` — final-diff run passed 195 tests: 58 kernel unit, 27 kernel integration, 26 workspace unit and 84 workspace integration (36 real-browser), with 0 failures/errors/skips.
- Syntax/hygiene: `node --check` for all three changed ES modules and `git diff --check` passed on the final diff.
- Working tree impact from tests: no tracked runtime data or unrelated file changed; only this UC's intentional edits remain.
- Runtime evidence: school timetable administrator in packaged Chrome at `/workspace/`, using 1,000-lesson isolated school and production solve/verify/accept HTTP paths; exact PostgreSQL aggregate comparisons after consequential actions.
- Changed files: `timetable-workspace/src/main/resources/static/workspace/{app.js,inspection-state.js,messages.js,styles.css}`, `timetable-workspace/src/test/java/org/schoolkernel/workspace/WorkspaceBrowserIT.java`, `spec/timetable-workbench-layout/{status.md,checkpoints/UC-4.md}`.
- Approved UCs regression-tested: UC-1, UC-2, UC-3 shared browser/service journeys passed in the complete final-diff reactor.

## Notes

- No approved deviation. The separate `timetable-ux-polish` UC-5 administrator gate is unchanged.
- Human administrator walkthrough is deferred to converge; automation cannot approve this UI use case.

READY FOR CONVERGENCE: UC-4
