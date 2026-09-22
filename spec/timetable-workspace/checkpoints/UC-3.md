# Use-Case Checkpoint: UC-3 - Understand the accepted whole-school timetable

## Summary

- Status: READY_FOR_CONVERGENCE
- Base commit: `a9ab56a950d32e2f77d5daddffbc156a70efb1b9`
- Submission commit: HEAD at convergence
- Relations verified: Requires UC-1; the approved verified accepted baseline is read through the production workspace snapshot.

## Contract Evidence

| Contract element | Test or runtime evidence | Result |
|---|---|---|
| Main step 1 | `WorkspaceBrowserIT.inspectsAcceptedWholeSchoolTimetableInRealBrowser` (`timetable-workspace/src/test/java/org/schoolkernel/workspace/WorkspaceBrowserIT.java:185`) opens an accepted workspace and observes the definition-derived Monday view; `app.js:106-116,275` binds and orders day periods. | PASS |
| Main step 2 | The browser journey asserts subject, teacher, room, explicit empty cells, accepted label, and complete class count; `app.js:129-142` renders every declared class against every definition-ordered period. | PASS |
| Main step 3 | `WorkspaceBrowserIT.java:211-240` uses browser text input, search/filter state, native keyboard lesson/focused-schedule activation, and density controls while retaining the accepted state; the scale test covers search, filters, day change, and selection. | PASS |
| Main step 4 | `WorkspaceBrowserIT.java:213-231` asserts filtered labeling, match state, active criteria, reset, and accepted assignment details without leaving the matrix; `app.js:221-250` updates these locally. | PASS |
| Main step 5 | `WorkspaceBrowserIT.java:233-240` opens the teacher schedule with a trusted Space key event, observes Monday and the return action, then returns to the retained matrix; `app.js:157-170` supplies class, teacher, and room schedules. | PASS |
| Extension 2a | `WorkspaceBrowserIT.showsDeclaredEmptyAcceptedTimetableInRealBrowser` (`:255`) asserts the empty accepted message, declared `Year 7A`, `Monday 1`, and empty cells, then compares the exact stored document. | PASS |
| Extension 3a | The main browser journey changes search criteria, asserts `Filtered whole-school matrix`, the exact criterion, match marking, and reduced status without a completeness claim. | PASS |
| Extension 3b | `WorkspaceBrowserIT.java:218-223` observes the no-match message, active `Search: not-present` criterion and reset action; the final database comparison proves no mutation. | PASS |
| Extension 5a | `WorkspaceBrowserIT.java:242-250` emulates 390x844, observes `Read-only focused schedule` and class schedule, proves acceptance controls and the desktop matrix are absent, and reports no browser errors. | PASS |
| G1 | `app.js:84-93,129-154,261-280` joins opaque IDs through maps and uses definition display names, weekday/order, optional times, and separately disclosed technical IDs; the browser asserts names and opaque lesson ID. | PASS |
| G2 | `messages.js:54,94` supplies `Class` and the explicit administrator-to-kernel `cohort` mapping; `ArchitectureTest.workspaceUsesOneMessageCatalog` asserts both. | PASS |
| G3 | Accepted, match, and selected cells have visible text in `app.js:98,147,152,175`; no draft, pin, required, moved, or failed state is presented in this accepted-inspection UC. | PASS |
| G4 | Native inputs, selects, and buttons implement every control; `WorkspaceBrowserIT.java:211-240` proves text entry plus keyboard-only Space activation for lesson selection and focused-schedule entry. | PASS |
| G5 | `WorkspaceBrowserIT.measuresTargetScaleInspectionInteractionsInRealBrowser` (`:300`) verifies 1,000 lessons, 100 teachers, 60 classes, 100 rooms, 60 periods; latest p95 was search 69.5 ms, filter 38.8 ms, day 172.6 ms, selection 81.1 ms, all below 250 ms with solver time excluded. | PASS |
| G6 | All three UC-3 HTTP/browser tests compare the exact PostgreSQL version/document before and after inspection; the UI issues no mutation request during accepted inspection. | PASS |
| Success postcondition | The real-browser journey identifies `Mathematics 1`, `Year 7A`, teacher, room, period, accepted state, complete school matrix, and focused teacher schedule; the UI retains the later repair entry context without implementing UC-4. | PASS |
| Minimal guarantee | Empty/no-match/presentation journeys disclose only definition-backed cells and compare the exact stored accepted document; no assignment is synthesized by the browser. | PASS |
| Requires UC-1 | UC-1 is approved; `returnsCompleteAcceptedSnapshotWithoutMutation` stores and reads the same complete definition/result accepted pair through production `GET /api/workspace`, including state and ETag. | PASS |

## Rule Evidence

| Rule | Evidence | Result |
|---|---|---|
| RULE-1 | The existing dependency/archive checks and `ArchitectureTest.workspaceDoesNotDependOnKernelImplementation` pass in the clean reactor; UC-3 adds only native static resources and consumes the workspace snapshot. | PASS |
| RULE-2 | The Java 25 clean reactor resolves the pinned Spring Boot 4.1.1/PostgreSQL 18.6/Testcontainers/JDBC stack; no dependency or frontend framework was added. | PASS |
| RULE-4 | UC-3 changes no schema or seed; the full Flyway migration/failure suite passes against fresh PostgreSQL 18.6. | PASS |
| RULE-19 | `WorkspaceBrowserIT.returnsCompleteAcceptedSnapshotWithoutMutation` (`:280`) compares the entire accepted document and ETag; `app.js:84-280` performs all search/filter/day/selection/grouping/rendering locally with opaque-ID joins. | PASS |
| RULE-20 | `messages.js` is the single English catalog; `ArchitectureTest.java:41-55` rejects inline production labels and asserts the Class/cohort mapping; native-keyboard and narrow-screen browser evidence pass. | PASS |
| RULE-21 | The unchanged security route matrix remains green; `WorkspaceImportIT.java:373-399` additionally proves the localized module shell/catalog are allowlisted with CSP while login/management routes remain denied. | PASS |
| RULE-22 | UC-3 uses the approved read snapshot and adds no failure shape; the complete existing safe-problem HTTP suite remains green. | PASS |
| RULE-23 | Inspection emits no school-data logs or new operational logging and persists no run evidence; the existing captured-log suite remains green. | PASS |
| RULE-24 | Standard `clean verify` used disposable PostgreSQL 18.6, ephemeral HTTP, and real headless Chrome; the success, empty, snapshot, and scale journeys assert exact state and non-mutation. | PASS |
| RULE-25 | The target-scale browser test records 20 raw post-load samples per interaction, calculates nearest-rank p95, asserts below 250 ms, states solver exclusion, and compares exact stored state. | PASS |

## Validation

- Focused commands: `./mvnw -q -pl timetable-workspace -am -Dit.test='WorkspaceBrowserIT#inspectsAcceptedWholeSchoolTimetableInRealBrowser' -Dfailsafe.failIfNoSpecifiedTests=false verify` — PASS after trusted Space-key activation; earlier `rawKeyDown`/Enter attempts did not invoke Chrome's default button action and were replaced without weakening the keyboard boundary.
- Full relevant suite: `./mvnw -q clean verify` — 140 tests, 0 failures, 0 errors, 0 skipped.
- Working tree impact from tests: none; `git diff --check` passed.
- Runtime evidence: real Chrome over ephemeral HTTP/PostgreSQL rendered complete, filtered, no-match, assignment-detail, focused, empty, narrow-screen, and target-scale accepted views; the browser reported no errors.
- Changed files: `status.md`, this checkpoint; workspace `app.js`, `index.html`, `messages.js`, and `styles.css`; `ArchitectureTest.java`, `WorkspaceBrowserIT.java`, and `WorkspaceImportIT.java`.
- Approved UCs regression-tested: timetable-workspace UC-1 and UC-2 plus kernel UC-1/UC-2 all passed in the 140-test clean reactor.

## Notes

- The first cold sample in the latest selection series was 458.6 ms and one day sample was 261.7 ms; the declared target is p95, whose nearest-rank values were 81.1 ms and 172.6 ms respectively. Raw samples remain in the Maven run output.
- The required administrator walkthrough remains the independent convergence gate; this checkpoint claims automated technical readiness only.

READY FOR CONVERGENCE: UC-3
