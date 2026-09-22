# Use-Case Status: timetable-inspection-ux

## Current

- Use case: UC-1
- Status: READY_FOR_CONVERGENCE
- Next eligible: none (UC-1 awaiting convergence)

## Progress

| Use case | Status | Depends on | Implementation | Convergence |
|---|---|---|---|---|
| UC-1 | READY_FOR_CONVERGENCE | none | Week/Day inspection implemented | pending |
| UC-2 | NOT_STARTED | UC-1 | - | - |
| UC-3 | NOT_STARTED | UC-1 | - | - |

## UC-1 Evidence

- Started from: 85afad2
- Pre-existing dirty files: none
- Implementation submission: HEAD at convergence
- Changed files: timetable-workspace/src/main/resources/static/workspace/app.js, timetable-workspace/src/main/resources/static/workspace/accepted-model.js, timetable-workspace/src/main/resources/static/workspace/messages.js, timetable-workspace/src/main/resources/static/workspace/styles.css, timetable-workspace/src/test/java/org/schoolkernel/workspace/WorkspaceBrowserIT.java, spec/timetable-inspection-ux/status.md
- Commands and results: `mvn -pl timetable-workspace -am test` passed 26 tests; `mvn -pl timetable-workspace -am -Dit.test=WorkspaceBrowserIT verify` passed 11 real Chrome/PostgreSQL tests with 0 failures, errors, or skips; `git diff --check` passed.

| Contract element | Evidence |
|---|---|
| UC-1 Week/Day rendering and preference | `WorkspaceBrowserIT.inspectsAcceptedWholeSchoolTimetableInRealBrowser` now asserts the Week table, Day transition, local preference record, fixed density, narrow mode, and unchanged workspace state. |
| UC-1 G1 and success postcondition | Browser journey compares the durable aggregate before and after inspection; production inspection calls only the existing reads. |
| RULE-2, RULE-5, RULE-6 | `accepted-model.js`; `app.js` preference validation and stable `lessonId` selection. |
| RULE-3, RULE-9, RULE-10 | `app.js` Week renderer and `styles.css` compact tile/sticky table styles; message catalog additions in `messages.js`. |

## Blockers

none

## Deviations

none
