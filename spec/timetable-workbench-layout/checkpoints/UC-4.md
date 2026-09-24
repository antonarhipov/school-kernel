# Use-Case Checkpoint: UC-4 - Explain and decide a proposal beside the timetable

## Summary

- Status: READY_FOR_CONVERGENCE
- Base commit: `e2137b1` (revision of the `fa2323a` implementation after G-1, G-2 and G-3)
- Submission commit: HEAD at convergence
- Relations verified: Requires UC-3; the browser begins with UC-2's saved ready Draft, runs the packaged repair path, independently verifies the feasible Proposal, and consumes that actual Proposal rather than a mocked review.

## Contract Evidence

| Contract element | Test or runtime evidence | Result |
|---|---|---|
| Main 1 | `WorkspaceBrowserIT.java:2621–2648,2705–2716`: exact old Current and verified Proposal share the 1,000-ID canvas, with both sides of the move and explicit not-current label. | PASS |
| Main 2 | `WorkspaceBrowserIT.java:2649–2695`: authoritative unique/category/group/direct/ripple/protection/run values in the wide area. | PASS |
| Main 3 | `WorkspaceBrowserIT.java:2747–2778`: category, detail and grouping origin/destination links; in-focus and out-of-focus navigation. | PASS |
| Main 4 | `WorkspaceBrowserIT.java:2717–2749,3186–3190`: exact before/after subject/class/teacher/weekday/period/room names and IDs, dimensions and effects in inspector and task area. | PASS |
| Main 5 | `WorkspaceBrowserIT.java:1792–1798,2696–2703,2775–2863`: other lessons, mode/range/focus, unchanged accepted Current and explicit decision confirmation. | PASS |
| Main 6 | `WorkspaceBrowserIT.java:2863–2875,3100–3115`: exact successor accepted, only Current remains, representable context retained and unrepresentable Day selection explained. | PASS |
| Extension 1a | `WorkspaceBrowserIT.java:2224–2228`; `WorkspaceRepairPlanningIT.java:420–450`: invalid/rejected candidate returns to Draft with no Proposal or Current change; `WorkspaceImportIT.java:395–411` rejects missing required accepted metadata at the verifier boundary. | PASS |
| Extension 1b | `WorkspaceBrowserIT.java:2829–2838`: the verified 1,000-lesson Proposal is a labelled read-only accepted/proposed agenda at 700 and 390 px, with no decision, matrix or page overflow. | PASS |
| Extension 2a | `WorkspaceBrowserIT.java:2919–2938`: zero category/group labels and no accepted advance. | PASS |
| Extension 3a | `WorkspaceBrowserIT.java:2540–2583,2747–2778`: one-sided narrowing, both move targets, Day/period/room/focus adjustments, retained search and highlights. | PASS |
| Extension 3b | `WorkspaceBrowserIT.java:2509–2513,2733–2746`: one combined same-slot changed tile with old/proposed room. | PASS |
| Extension 3c | `WorkspaceBrowserIT.java:2509–2523`: addition and cancellation show their sole existing side and explicit status. | PASS |
| Extension 4a | `WorkspaceBrowserIT.java:2523–2526,2776–2785`: accepted unchanged/protected lesson remains outside changed total. | PASS |
| Extension 5a | `WorkspaceBrowserIT.java:2883–2915`: verified Proposal revision opens exact editable Draft, preserves Current. | PASS |
| Extension 5b | `WorkspaceBrowserIT.java:2883–2915,2945–2968`: discard removes only Proposal and preserves exact accepted/Draft documents. | PASS |
| Extension 6a | `WorkspaceBrowserIT.java:2969–2985`; `WorkspaceRepairPlanningIT.java:306–340`: stale identity invalidates Proposal to Draft with exact old Current. | PASS |
| Extension 6b | `WorkspaceBrowserIT.java:1808–1835`; `WorkspaceRepairPlanningIT.java:343–373`: injected storage failure keeps exact old document/version and reviewable Proposal for retry. | PASS |
| G1 | `WorkspaceBrowserIT.java:2649–2678,2705–2716,2509–2513`: stable IDs, one combined tile, two move sides, authoritative distinct and overlapping counts. | PASS |
| G2 | `WorkspaceBrowserIT.java:2540–2583,2719–2778`: either-side matching and exact old/new IDs with named target; missing optional name has stable ID/unavailable cue. | PASS |
| G3 | `styles.css:413–429`; `WorkspaceBrowserIT.java:2786–2852`; inspected `target/workbench-layout/uc4-proposal-{1600,1280}.png`: 1600/1280 task ≤35% viewport with headers, row and side inspector; 1279/701 stacked regions without page or task horizontal overflow; 700/390 read-only; decisions reachable; selection/count and both matrix scroll offsets survive collapse/reopen. | PASS |
| G4 | `WorkspaceBrowserIT.java:2630–2641,2853–2868`: zero mutating fetches across review-only navigation, mode, filter, focus, resize and task actions, with exact stored document/version; confirmation alone sends none, and explicit acceptance sends only `POST /api/proposal/accept`. | PASS |
| G5 | `WorkspaceBrowserIT.java:2696–2701,2769–2782`: text/structural origin, destination, effect, pin, selection, availability and accepted/Proposal cues; complete accessible name. | PASS |
| G6 | `WorkspaceBrowserIT.java:1808–1845,2859–2875,2969–2985`; `WorkspaceRepairPlanningIT.java:306–373`: only confirmed, revalidated and durable success advances Current. | PASS |
| G7 | `WorkspaceBrowserIT.java:2588–2985`: normative-scale real browser covers all comparison shapes, filters, counts, navigation, protection, decisions, all responsive boundaries and exact JDBC state; all 36 shared browser cases passed. | PASS |
| Success postcondition | `WorkspaceBrowserIT.java:2863–2875`: exact verified successor becomes the sole Current, with whole-school context and no repair task area. | PASS |
| Minimal guarantee | `WorkspaceBrowserIT.java:1808–1845,2853–2868,2883–2915,2969–2985`: no presentation, refusal, failed storage, revise or discard path partially advances Current. | PASS |
| Requires UC-3 | `WorkspaceBrowserIT.java:2612–2648`: actual Draft solve invokes packaged run and independent verifier, then UC-4 reads the persisted verified Proposal and comparison context. | PASS |

## Rule Evidence

| Rule | Evidence | Result |
|---|---|---|
| RULE-1 | `app.js:26–42,211–349`, existing accepted-model/comparison index, and no route/schema/dependency changes. | PASS |
| RULE-2 | `inspection-state.js:5–129`; `WorkspaceBrowserIT.java:2630–2641,2853–2868` records zero mutating fetches for presentation actions, exact durable state, and the sole explicit accept request. | PASS |
| RULE-3 | `WorkspaceRepairPlanningIT.java:290–373,420–450`: guarded service lifecycle, stale identity and storage refusal; UI uses existing routes. | PASS |
| RULE-5 | `styles.css:338–380,413–429`; `WorkspaceBrowserIT.java:2786–2852`: wide task owns review/decisions, inspector is beside at 1280 and stacked at 1279/701, with no overlay, horizontal task clipping or lost scroll. | PASS |
| RULE-6 | `app.js:689–748,1151–1232`; `WorkspaceBrowserIT.java:2540–2583,2705–2716,2747–2778`: complete population and either-side narrowing with labelled adjustments. | PASS |
| RULE-7 | `app.js:802–873,1151–1173`; `WorkspaceBrowserIT.java:2719–2746,2769–2782`: exact display names/IDs, distinct availability, escaped text and complete accessible name. | PASS |
| RULE-10 | `app.js:214–370,802–833`; `WorkspaceBrowserIT.java:2509–2583,2649–2778`: stable two-sided index, group navigation, exact distinct totals and all comparison shapes. | PASS |
| RULE-11 | `app.js:290–296`; `WorkspaceBrowserIT.java:1808–1845,2859–2915,2969–2985`: existing guarded decisions, exact acceptance and retry/refusal state. | PASS |
| RULE-12 | No server/security diff; shared hostile Host/Origin/CSRF/`If-Match` regression; verified Proposal at 701/700/390 px in `WorkspaceBrowserIT.java:2829–2838` is read-only at narrow widths. | PASS |
| RULE-13 | `messages.js:129–166`, native review links/confirmation controls in `app.js:214–370`, structural and accessible cues in `WorkspaceBrowserIT.java:2769–2782`. | PASS |
| RULE-14 | `WorkspaceBrowserIT.java:2588–2985,3186–3190`: isolated normative school, real browser/process/JDBC, exact successor, request/viewport/scroll traces and clean 36/36 shared browser regression; human walkthrough is not claimed. | PASS |

## Validation

- Focused command: `./mvnw -q -pl timetable-workspace -am '-Dit.test=WorkspaceBrowserIT#reviewsIndependentlyVerifiedNormativeRepairInRealBrowser' -Dfailsafe.failIfNoSpecifiedTests=false verify` — revised-diff run passed with Docker access: 1 real-browser test, 26 workspace unit tests and 58 kernel unit tests; 0 failures/errors/skips. The first revised focused run found a 10 px Proposal task-area overflow at 701 px; `styles.css:423–426` stacks the detail panels there, and the rerun passed.
- Full relevant suite: `./mvnw -q -pl timetable-workspace -am verify` — revised-diff run passed 195 tests: 58 kernel unit, 27 kernel integration, 26 workspace unit and 84 workspace integration (36 real-browser), with 0 failures/errors/skips. This clean run resolves prior G-3 for submission.
- Syntax/hygiene: no ES-module source changed in this revision; `git diff --check` passed on the revised diff.
- Working tree impact from tests: no tracked runtime data or unrelated file changed; only this UC's intentional edits remain.
- Runtime evidence: school timetable administrator in packaged Chrome at `/workspace/`, using the independently verified 1,000-lesson school and production solve/verify/accept HTTP paths; exact PostgreSQL aggregate, network request, viewport geometry and matrix scroll comparisons after consequential actions.
- Changed files in this revision: `timetable-workspace/src/main/resources/static/workspace/styles.css`, `timetable-workspace/src/test/java/org/schoolkernel/workspace/WorkspaceBrowserIT.java`, `spec/timetable-workbench-layout/{status.md,checkpoints/UC-4.md}`. The earlier `fa2323a` submission contains the main UC-4 production slice.
- Approved UCs regression-tested: UC-1, UC-2, UC-3 shared browser/service journeys passed in the complete revised-diff reactor, including the UC-2 browser case that timed out once in the prior independent run.

## Notes

- No approved deviation. The separate `timetable-ux-polish` UC-5 administrator gate is unchanged.
- Human administrator walkthrough is deferred to converge; automation cannot approve this UI use case.

READY FOR CONVERGENCE: UC-4
