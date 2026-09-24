# Use-Case Checkpoint: UC-3 - Follow repair generation while retaining timetable context

## Summary

- Status: READY_FOR_CONVERGENCE
- Base commit: `06e89134600b685304354bf7aa982a4a146819e0`
- Submission commit: HEAD at convergence
- Relations verified: Requires approved UC-2. The run starts from its saved, conflict-free Draft and accepted canvas through the existing guarded browser and service path; no Included or Extends relation. The independently verified Proposal is handed to UC-4 without accepting it.

## Contract Evidence

| Contract element | Test or runtime evidence | Result |
|---|---|---|
| Main 1 | `WorkspaceBrowserIT.java:2090–2143` saves exact teacher-16/period-0 intent and lesson-500 room protection, verifies `readyToSolve`, then launches repair from the Draft task area. | PASS |
| Main 2 | `WorkspaceBrowserIT.java:2144–2152,2163–2200` checks `SOLVING_REPAIR`, exact accepted/Draft/run values, PT30S, frozen named intent and pin in a wide task area, visible cancel, no editing/decision controls, and compact geometry. | PASS |
| Main 3 | `WorkspaceBrowserIT.java:2153–2219` selects the same accepted lesson, changes range, mode, inspector, task detail, responsive width and weekday during the live run; `WorkspaceBrowserIT.java:1925–1942` opens and returns from a focused teacher schedule. | PASS |
| Main 4 | `WorkspaceBrowserIT.java:2144–2219` keeps accepted Current labelled, checks that there is no Proposal or candidate during Solving, preserves representable selection and exact running document, records no mutation request and only one replan invocation. | PASS |
| Main 5 | `WorkspaceBrowserIT.java:1731–1794` obtains a genuine packaged feasible replan; `WorkspaceBrowserIT.java:2229–2272` supplies the normative complete feasible fixture at the production process boundary and checks independent packaged verification. | PASS |
| Main 6 | `WorkspaceBrowserIT.java:2229–2272` checks exact Proposal and old accepted/Draft values, absent active run/Solving mode, default Proposal mode and open wide review area, retained lesson context and reachable task decisions. | PASS |
| Extension 1a | `WorkspaceRepairDraftIT.java:129–160,329–354` and `WorkspaceRepairPlanningIT.java:502–535` refuse conflict, unsaved persistence and stale generation with exact prior aggregate/version and no process. | PASS |
| Extension 2a | `WorkspaceBrowserIT.java:2219–2225` cancels the live run through its task control and proves editable Draft, cancelled terminal outcome, exact old Current and absent Proposal. | PASS |
| Extension 2b | `WorkspaceBrowserIT.java:2033–2085` actually stops/restarts the application on one durable database and proves safe interrupted recovery, exact Draft/Current, no eligible Proposal and no late publication. | PASS |
| Extension 3a | `WorkspaceBrowserIT.java:2208–2213` changes to Tuesday while Monday's selected lesson cannot be represented, checks the explanation and closed details, exact unchanged run, then restores Monday/lesson identity. | PASS |
| Extension 5a | `WorkspaceBrowserIT.java:1976–2030` exercises no-feasible, invalid, transport, interruption, mismatch and watchdog outcomes with safe task/Utilities diagnostics, no candidate and exact accepted/Draft; `WorkspaceRepairPlanningIT.java:420–453` covers HTTP/JDBC refusal. | PASS |
| Extension 5b | `WorkspaceBrowserIT.java:1987–2005` starts PT2M only from unchanged no-feasible intent and checks cancellation removes retry; `WorkspaceRepairPlanningIT.java:376–419` confirms service gating. | PASS |
| Extension 6a | `WorkspaceBrowserIT.java:2224–2228` rejects mismatched output with no Proposal; `WorkspaceRepairPlanningIT.java:420–453` covers incomplete, stale, non-feasible, mismatched and independently rejected results at HTTP boundary. | PASS |
| G1 | `app.js:437–477`, `WorkspaceBrowserIT.java:2144–2152,2200–2219`: frozen accepted view has no Draft mutation/discard/Proposal decision control, while cancellation remains. | PASS |
| G2 | `styles.css:337–351,410–412`, `WorkspaceBrowserIT.java:2153–2200,2277–2307`: status/cancel persist on detail collapse; task remains beneath canvas, ≤35% height, with headings and full row visible at 1600/1280; 1279/701 stack. | PASS |
| G3 | `WorkspaceBrowserIT.java:2153–2219,1925–1942` checks retained context, zero mutating fetch methods, exact aggregate and one scheduler invocation across presentation changes. | PASS |
| G4 | `WorkspaceBrowserIT.java:1731–1794,2224–2272` and `WorkspaceRepairPlanningIT.java:93–140,420–453` prove independent verification is the only Proposal gate and rejected/failed output is never Current. | PASS |
| G5 | `app.js:450–477`, `messages.js:122–137`, `WorkspaceBrowserIT.java:1976–2030,2151–2169`: visible textual limit/status/frozen intent/cancel and safe outcomes, native buttons, no global-optimum claim. | PASS |
| G6 | `WorkspaceBrowserIT.java:2183–2200` checks 700/390 read-only Solving with no cancellation or mutation; full `WorkspaceImportIT` security regressions and exact database comparison pass. | PASS |
| G7 | `WorkspaceBrowserIT.java:2090–2272,1976–2085` exercises all required run outcomes on the verifier-checked normative school or actual restart, with exact state and viewport/browser evidence. | PASS |
| Success postcondition | `WorkspaceBrowserIT.java:2229–2272` checks one independently verified Proposal, reviewable task, exact old Current, retained Draft and selected context. | PASS |
| Minimal guarantee | `WorkspaceBrowserIT.java:1976–2085,2219–2228`, `WorkspaceRepairPlanningIT.java:420–535` check exact accepted/Draft retention and candidate absence after cancellation, restart, failure, timeout and rejection. | PASS |
| Requires UC-2 | `WorkspaceBrowserIT.java:2090–2143` consumes UC-2's saved, ready Draft and the approved accepted canvas, including exact room protection; shared UC-1/UC-2 browser regressions pass. | PASS |

## Rule Evidence

| Rule | Evidence | Result |
|---|---|---|
| RULE-1 | `app.js:52–174,437–465` reuses the complete `/api/workspace` snapshot, accepted model, inspection state and existing renderers; no route, persisted field, dependency or mode-specific timetable model was added. | PASS |
| RULE-2 | `inspection-state.js:1–104`, `app.js:117–155,450–465`, `WorkspaceBrowserIT.java:2153–2219`: four-row mode/default owner, separate page-session task detail per mode, retained representable context and no mutating request or version change from presentation. | PASS |
| RULE-3 | `WorkspaceRepairPlanningIT.java:376–535` exercises stale, conflict, unsaved, cancel, restart, failed/rejected and late output through existing HTTP/service guards with exact state and absence of ineligible process/Proposal. | PASS |
| RULE-5 | `app.js:176–257,437–477,595–647`, `styles.css:337–355,410–412`, `WorkspaceBrowserIT.java:2153–2200,2229–2272`: inspector keeps details, task owns status/cancel and review handoff; geometry, non-overlay, row/headings and reachable controls measured. | PASS |
| RULE-7 | `app.js:469–477,595–647,750–766`, `WorkspaceBrowserIT.java:2090–2272` plus shared verifier/import tests: authoritative long names/IDs and accepted availability remain, school text is escaped, invalid candidate never renders. | PASS |
| RULE-9 | `app.js:437–477`, `WorkspaceBrowserIT.java:1900–2272`, `WorkspaceRepairPlanningIT.java:376–535`: ready durable input, frozen intent, visible cancel, no other mutation control, independent verification, safe failure/retry/recovery. | PASS |
| RULE-12 | No server/security diff; full hostile Host/Origin/CSRF/`If-Match` and denied-route tests remain green. `WorkspaceBrowserIT.java:2183–2200` proves 700/390 have no cancel or edit and the exact run is unchanged. | PASS |
| RULE-13 | All new visible text is in `messages.js:122–137`; `app.js:450–465` uses native buttons and textual cues; shared Utilities, safe diagnostics, and initial-journey browser regressions pass. | PASS |
| RULE-14 | `WorkspaceBrowserIT.java:2090–2272,3122–3200` builds and independently verifies the normative 1,000-lesson school; exact browser/JDBC comparisons cover the actor journey. Complete 36-browser/84-integration regression is green. Human walkthrough is not claimed by automation. | PASS |

## Validation

- Focused commands: `./mvnw -q -pl timetable-workspace -am '-Dit.test=WorkspaceBrowserIT#inspectsFrozenRepairRunAndRecoversWithoutPublishingInRealBrowser+showsFailedRepairEvidenceAndGatedRetryInRealBrowser+restoresInterruptedRepairAfterActualApplicationRestartInRealBrowser+followsAndRefusesWholeSchoolRepairOnVerifiedNormativeSnapshotInRealBrowser,WorkspaceRepairPlanningIT' -Dfailsafe.failIfNoSpecifiedTests=false verify` — 26 unit + 4 browser + 10 repair-planning integration tests, 0 failures/errors/skips. The corrected shared real-repair browser regression and strengthened normative journey passed together; the final extension-3a normative journey passed alone.
- Full relevant suite: `./mvnw -q -pl timetable-workspace -am verify` — 26 unit + 84 integration, including all 36 real-browser tests; 0 failures/errors/skips on the final diff.
- Working tree impact from tests: no tracked runtime or unrelated file changed. `node --check` passed for `app.js` and `messages.js`; `git diff --check` passed. Screenshots remain ignored under `timetable-workspace/target/workbench-layout/`.
- Runtime evidence: local Chrome uses the packaged `/workspace/` page, isolated Testcontainers PostgreSQL, the packaged Kernel verifier and real process boundary. The administrator starts from an exact ready UC-2 Draft, monitors and inspects accepted Current during frozen Solving, cancels/recovers/refuses safely, uses the eligible retry, and receives an independently verified Proposal in the wide task area without advancing Current. Screenshots: `uc3-solving-{1600,1280,390}.png`, `uc3-proposal-handoff-1280.png`.
- Changed files: `timetable-workspace/src/main/resources/static/workspace/{app.js,messages.js,styles.css}`, `timetable-workspace/src/test/java/org/schoolkernel/workspace/WorkspaceBrowserIT.java`, `spec/timetable-workbench-layout/{status.md,checkpoints/UC-3.md}`.
- Approved UCs regression-tested: shared workspace UC-1–UC-8, accepted inspection UC-1–UC-3, and workbench-layout UC-1/UC-2 through the complete module/browser suite. The separate `timetable-ux-polish` UC-5 administrator gate remains `PENDING_WALKTHROUGH`.

## Notes

No approved deviation. Final full-run pin-feedback p95 was 291.4 ms against a 250 ms diagnostic-only reference; no functional/performance gate failed. The UC-3 human walkthrough remains for convergence. The next UC is not started.

READY FOR CONVERGENCE: UC-3
