# Use-Case Checkpoint: UC-5 - Complete a repair in the wide workbench

## Summary

- Status: READY_FOR_CONVERGENCE
- Base commit: `5542555b9855df55851c9880b7e45ad29ceae79a`; revision base `d0aeb17` after convergence rejection C-1, G-1 and G-2
- Submission commit: HEAD at convergence
- Relations verified: Requires and Includes UC-1, UC-2, UC-3 and UC-4 in order. One Chrome session starts from UC-1's verified accepted school, stages UC-2's saved protected Draft, follows UC-3's frozen run and verified Proposal, uses UC-4's comparison and explicit acceptance, then starts another Draft from the accepted successor.
- Submission scope: the original boundary added UC-5's continuous actor journey. This revision keeps Proposal Week subject and room visible with contained compact cues under simultaneous investigation, strengthens real-browser tile checks at both desktop widths, and preserves accessible full matching-side meaning. The separate dirty `spec.md` edit is excluded.

## Contract Evidence

| Contract element | Test or runtime evidence | Result |
|---|---|---|
| Main 1; Includes UC-1 | `WorkspaceBrowserIT.java:2991–3020`: Chrome opens a kernel-verified 60-class, 100-teacher, 100-room, 1,000-lesson Current; checks all IDs, exact affected assignment and names, Day/Week, selection, teacher focus/return and unchanged accepted JSON. | PASS |
| Main 2; Includes UC-2 | `WorkspaceBrowserIT.java:3021–3046`: same browser stages teacher-16 at period-0, sees direct lesson-960, introduces and resolves an actual blocking pin conflict, saves lesson-500 room protection, retains context and exact accepted bundle. | PASS |
| Main 3; Includes UC-3 | `WorkspaceBrowserIT.java:3048–3084`: saved conflict-free Draft starts one run; frozen Draft, run limit, accepted canvas, mode/range changes and cancellation are checked against exact persisted JSON; an explicit second run becomes a verifier-checked Proposal. | PASS |
| Main 4; Includes UC-4 | `WorkspaceBrowserIT.java:3085–3123`: same persisted Proposal shows a two-sided period move, combined same-slot room ripple, unchanged protected lesson and exact authoritative counts; Current/Draft/Proposal review changes no durable state, then confirmation and explicit acceptance occur. | PASS |
| Main 5 | `WorkspaceBrowserIT.java:3124–3154`: exact proposal definition/result and proposed revision become the sole accepted Current; Draft/Proposal disappear, Week returns all 1,000 IDs, representable Day context is retained and the moved selection clears with explanation. | PASS |
| Extension 2a | `WorkspaceBrowserIT.java:3027–3036` checks blocking conflict, disabled solve and zero scheduler starts; `:1688–1729` proves failed Draft persistence keeps document/version and ineligible solve. | PASS |
| Extension 3a | `WorkspaceBrowserIT.java:3068–3084` cancels the live run to the exact Draft; `:1900–2085,2090–2250` and `WorkspaceRepairPlanningIT.java:420–535` exercise restart, unsuccessful, timeout, transport and rejected output through production boundaries without publishing Proposal. | PASS |
| Extension 4a | `WorkspaceBrowserIT.java:2882–2968` consumes verified Proposals for both revise and discard, returns to exact saved Draft and old Current without acceptance. | PASS |
| Extension 4b | `WorkspaceBrowserIT.java:1808–1845,2969–2985` and `WorkspaceRepairPlanningIT.java:306–373` prove stale invalidation to Draft and failed PostgreSQL acceptance retaining old Current, exact version and a retryable Proposal. | PASS |
| Extension 5a | `WorkspaceBrowserIT.java:3129–3154` starts a room repair from exact accepted successor revision, asserts direct `[lesson-0]`, empty new attempt pins/bulk actions and no new scheduler start; `:1731–1895` checks the prior attempt lock is not treated as persistent policy. | PASS |
| G1 | `WorkspaceBrowserIT.java:2991–3154`: the same stable ID and authoritative accepted definition/result support Week/Day, focus, inspector and four modes; no mode-specific durable model or accepted mutation. | PASS |
| G2 | `WorkspaceBrowserIT.java:3021–3123,3158–3210`: accepted Current, active mode, conflict, frozen Solving and not-current Proposal remain named with wide-task decisions at each handoff. | PASS |
| G3 | `WorkspaceBrowserIT.java:3043,3060,3094–3104,3158–3245`: measures 1600×900 and 1280×800 Draft/Solving/Proposal task height, below-canvas position, side inspector, heading/row visibility, horizontal overflow and reachable active action; checks all 1,001 Proposal Week representations for contained visible subject, room and badges at both desktop widths. The same verified Proposal is read-only at 390×844. Fresh screenshots were inspected. | PASS |
| G4 | `WorkspaceBrowserIT.java:3027–3036,3068–3084,3114–3123` and the failure extension evidence: exact old accepted bundle remains Current through conflict, cancellation, review and refusal. | PASS |
| G5 automated portion | `WorkspaceBrowserIT.java:2991–3154` runs one Chrome/PostgreSQL/packaged-boundary journey at normative scale with exact handoff comparisons; complete shared browser and reactor pass. Five-participant feature gate awaits convergence and user evidence. | PASS automation; human pending |
| Success postcondition | `WorkspaceBrowserIT.java:3124–3154`: deliberate acceptance installs the exact verified successor, leaves only Current, and immediately permits the next repair from it. | PASS |
| Minimal guarantee | `WorkspaceBrowserIT.java:3027–3036,3068–3084,3114–3123,2882–2985` plus HTTP/JDBC failure tests: no incomplete step partially advances or mislabels Current. | PASS |

## Rule Evidence

| Rule | Evidence | Result |
|---|---|---|
| RULE-1 | One `GET /api/workspace` snapshot, `app.js` accepted model and `inspection-state.js` owner; UC-5 diff has no route, model, storage, dependency or prototype asset change; browser verifies the same 1,000 IDs across modes. | PASS |
| RULE-2 | `WorkspaceBrowserIT.java:3043,3064–3067,3114–3123,3158–3210` compares the exact document plus version across range/mode/inspector/resize/review actions; UC-1–UC-4 browser traces also record zero presentation mutation requests. | PASS |
| RULE-3 | Existing guarded `RepairDraftService`/`RepairPlanningService` and `WorkspaceRepairPlanningIT.java:90–535` enforce allowed/refused transitions; UC-5 browser checks conflict, cancellation, verification and atomic acceptance. | PASS |
| RULE-4 | `app.js:802–818,1160–1180`; `styles.css:325–360`; `WorkspaceBrowserIT.java:3158–3245`: Proposal Week uses the compact investigation cue, reserves 40px for room, wraps contained badges only where needed, retains a complete class row at both desktop widths, and asserts every occupied tile's subject/room/badge geometry. Fresh 1600/1280 screenshots confirm legible room labels and no cross-tile badge spill. | PASS |
| RULE-5 | `WorkspaceBrowserIT.java:3043,3060,3094,3158–3210`: Draft, Solving and Proposal task regions stay beneath canvas and beside 240px inspector at both desktops; task height ≤35%, visible heading/row and reachable action. | PASS |
| RULE-6 | `WorkspaceBrowserIT.java:490–648,2509–2583,2991–3020,3085–3123`: complete 1,000-ID population, named narrowing, non-narrowing highlights, focused return and either-side Proposal matching. One-sided Week matches have visible compact Accepted/Proposed match cues and complete accessible names; focused UC-4 regression passed. | PASS |
| RULE-7 | `WorkspaceBrowserIT.java:164–206,2991–3020,3085–3123`; `app.js:802–873`: authoritative names/IDs and accepted/proposed values, verifier refusal of invalid metadata and safe escaped labels. | PASS |
| RULE-8 | `WorkspaceBrowserIT.java:660–899,1688–1729,3021–3046`: setup, explicit staged intent, conflict, protection, preview/undo and refused-save paths preserve accepted authority. | PASS |
| RULE-9 | `WorkspaceBrowserIT.java:1900–2250,3048–3084`: saved ready Draft only; frozen run, visible cancellation, exact failed/cancelled/recovered terminal Draft and independently verified Proposal. | PASS |
| RULE-10 | `WorkspaceBrowserIT.java:2509–2583,2588–2985,3085–3123`: stable comparison identity, two-sided move, combined same-slot change, one-sided shapes, counts and necessary context adjustments. | PASS |
| RULE-11 | `WorkspaceBrowserIT.java:1808–1895,2882–2985,3124–3154`: guarded decisions, exact stale/write-failure refusal, atomic acceptance and next Draft with no inherited pins. | PASS |
| RULE-12 | No server/security diff; `WorkspaceImportIT.java:459–545` hostile local-access/CSRF/`If-Match` regression and `WorkspaceBrowserIT.java:3094–3104` read-only 390 Proposal pass. | PASS |
| RULE-13 | `messages.js:152–156` catalogs the compact matching-side cues; native controls and text/structural Current/Draft/Solving/Proposal cues remain. `WorkspaceBrowserIT.java:2509–2583,2991–3245` checks visible/accessible labels, contained badges and reachable actions; Utilities and initial journeys pass unchanged. | PASS |
| RULE-14 | Normative verified school, exact isolated document/version, one included-UC browser chain, fresh desktop screenshots, 390 narrow check and all 36 browser cases/195 reactor tests passed on the revision. Five real-administrator outcomes remain a human convergence gate. | PASS automation; human pending |

## Validation

- Focused command on the final revision: `./mvnw -q -pl timetable-workspace -am '-Dit.test=WorkspaceBrowserIT#completesWholeSchoolRepairAndStartsNextFromAcceptedSuccessorInRealBrowser' -Dfailsafe.failIfNoSpecifiedTests=false verify` — PASS with Docker, 1 real-browser case; applicable unit tests passed, 0 failures/errors/skips. Two earlier revision iterations correctly exposed a class-row height issue and then a test-only hidden-badge measurement issue; both were corrected before this passing run.
- Affected UC-4 focused regression: `./mvnw -q -pl timetable-workspace -am '-Dit.test=WorkspaceBrowserIT#comparesOneSidedAndSameSlotChangesInRealBrowser+retainsBothSidesInFocusedResourceSchedulesInRealBrowser' -Dfailsafe.failIfNoSpecifiedTests=false verify` — PASS, 2 real-browser cases, 0 failures/errors/skips.
- Full relevant suite on the final revision: `./mvnw -q -pl timetable-workspace -am verify` — PASS, 195 tests: 58 kernel unit, 27 kernel integration, 26 workspace unit, 84 workspace integration including all 36 browser cases; 0 failures/errors/skips. The prior independent full-suite UC-2 CDP timeout did not recur.
- `node --check` on changed modules and `git diff --check` — PASS. Tests left tracked runtime data unchanged. The separate `spec.md` edit is not part of this checkpoint.
- Runtime: isolated PostgreSQL 18.6, ephemeral loopback Spring Boot 4.1.1, real Chrome, packaged kernel verifier and process contract. The normative feasible result is generated from the actual frozen input arguments and independently verified before the persisted Proposal is exposed; separate shared browser/CLI regressions exercise actual packaged solver execution.
- Changed files in this revision: `timetable-workspace/src/main/resources/static/workspace/{app.js,messages.js,styles.css}`, `timetable-workspace/src/test/java/org/schoolkernel/workspace/WorkspaceBrowserIT.java`, `spec/timetable-workbench-layout/status.md`, and this checkpoint.
- Approved UCs regression-tested: UC-1, UC-2, UC-3 and UC-4 through their shared browser cases and full reactor; 0 regressions. All 36 browser cases passed.

## Notes

- The six screenshots are ignored test artifacts. The former Proposal Week cross-tile badge spill and lost room labels are no longer present in the fresh desktop images; the human feature gate still needs to record actual layout friction.
- The distinct `timetable-ux-polish` UC-5 gate remains pending in its own feature directory. No participant results for this feature have been claimed.

READY FOR CONVERGENCE: UC-5
