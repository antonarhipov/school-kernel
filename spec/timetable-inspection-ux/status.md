# Use-Case Status: timetable-inspection-ux

## Current

- Use case: UC-3
- Status: APPROVED
- Next eligible: UC-2

## Progress

| Use case | Status | Depends on | Implementation | Convergence |
|---|---|---|---|---|
| UC-1 | APPROVED | none | Revision C-1 through C-3 technically converged; user walkthrough PASS | convergence/UC-1.md — APPROVE |
| UC-2 | NEEDS_REVISION | UC-1 | Exact subject/teacher investigation, explicit intersection filters, and authoritative teacher ribbon | convergence/UC-2.md — REJECT (G-1) |
| UC-3 | APPROVED | UC-1 | Search highlighting, explicit intersections, and focused accepted schedules | convergence/UC-3.md — APPROVE |

## UC-1 Evidence

- Started from: 85afad2
- Revision started from: 1d9f761 (C-1, C-2, C-3)
- Pre-existing dirty files: none
- Implementation submission: HEAD at convergence
- Changed files: `timetable-workspace/src/main/resources/static/workspace/app.js`, `timetable-workspace/src/main/resources/static/workspace/inspection-state.js`, `timetable-workspace/src/main/resources/static/workspace/day-renderer.js`, `timetable-workspace/src/main/resources/static/workspace/week-renderer.js`, `timetable-workspace/src/main/resources/static/workspace/focused-renderer.js`, `timetable-workspace/src/test/java/org/schoolkernel/workspace/WorkspaceBrowserIT.java`, `spec/timetable-inspection-ux/status.md`, `spec/timetable-inspection-ux/checkpoints/UC-1.md`.
- Commands and results: `mvn -pl timetable-workspace -am -Dit.test=WorkspaceBrowserIT verify` passed 26 unit and 12 real Chrome/PostgreSQL tests with 0 failures, errors, or skips; `git diff --check` passed.

| Contract element | Evidence |
|---|---|
| UC-1 main, 2a, 2c, G1-G12 | Existing `WorkspaceBrowserIT` journeys retain complete Week/Day structure, empty timetable, narrow read-only agenda, stable lesson details, compact density, and exact before/after aggregate comparisons. |
| UC-1 extensions 2b, 5a, 5b | `WorkspaceBrowserIT.handlesInvalidAndBlockedInspectionPreferencesInRealBrowser` drives malformed, stale, invalid enum, unknown weekday, oversized, cross-school, valid restore, manual Day exclusion, and blocked persistence through Chrome; it asserts unchanged aggregate. |
| UC-1 extension 7a | `WorkspaceBrowserIT.measuresTargetScaleInspectionInteractionsInRealBrowser` scrolls the 60-class matrix, selects off-viewport `lesson-999`, asserts its exact inspector detail, and verifies Week-to-Day follows Thursday. |
| RULE-1, RULE-4 | `inspection-state.js` is the inspection transition owner; `day-renderer.js`, `week-renderer.js`, and `focused-renderer.js` are independent renderers; `app.js` delegates transition and rendering intents. |
| RULE-5 | `inspection-state.js` validates the versioned, namespaced, exact-shape, <=1KiB `{version, range, weekdayId}` record; Chrome checks validate fallback, isolation, allowed stored shape, blocked writes, and unchanged durable state. |
| RULE-13 | `mvn -pl timetable-workspace -am -Dit.test=WorkspaceBrowserIT verify` drives all UC-1 paths with packaged assets, real Chrome, and Testcontainers PostgreSQL: 12 passed. |

## UC-2 Evidence

- Started from: be92691
- Pre-existing dirty files: none
- Implementation submission: HEAD at convergence
- Changed files: `timetable-workspace/src/main/resources/static/workspace/accepted-model.js`, `timetable-workspace/src/main/resources/static/workspace/inspection-state.js`, `timetable-workspace/src/main/resources/static/workspace/app.js`, `timetable-workspace/src/main/resources/static/workspace/messages.js`, `timetable-workspace/src/main/resources/static/workspace/styles.css`, `timetable-workspace/src/test/java/org/schoolkernel/workspace/WorkspaceBrowserIT.java`, this status file, and `checkpoints/UC-2.md`.
- Commands and results: `node --check` for all changed ES modules and `git diff --check` passed. `mvn -q -pl timetable-workspace -am -Dit.test=WorkspaceBrowserIT#tracesSubjectTeachingAndTeacherLoadInRealBrowser verify` passed 26 unit tests and 1 Chrome/Testcontainers PostgreSQL UC-2 journey with 0 failures, errors, or skips. `mvn -q -pl timetable-workspace -am -Dit.test=WorkspaceBrowserIT#inspectsAcceptedWholeSchoolTimetableInRealBrowser verify` passed the shared UC-1 browser regression with 26 unit tests and 1 Chrome/Testcontainers journey, 0 failures, errors, or skips. The complete `WorkspaceBrowserIT` class exceeded the command lifetime available to this environment, so it is not claimed as rerun.

| Contract element | Evidence |
|---|---|
| UC-2 main steps 1-5 | `WorkspaceBrowserIT.tracesSubjectTeachingAndTeacherLoadInRealBrowser` selects exact Math/Alex identities, asserts non-destructive cues and dual count, checks all three ribbon classifications, and retains identities through Week-to-Day at `WorkspaceBrowserIT.java:328-366`. |
| UC-2 main steps 6-9 | The same real-browser journey asserts single and intersected filters, narrowed status, clear-one, clear-all/reset behavior at `WorkspaceBrowserIT.java:353-398`; `app.js:542-557,633-686` owns the transitions and exact set counts. |
| UC-2 extensions 2a, 4a, 6a, 7a | Tuesday asserts exact zero subject/teacher counts and a no-match intersection; filtered science selection is cleared with a status announcement while identities remain selected at `WorkspaceBrowserIT.java:353-392`. |
| UC-2 G1-G7 | Stable IDs and immutable indexes: `accepted-model.js:3-45`; state reset/no durable preference expansion: `inspection-state.js:4-63`; visible non-color cue markup, accessible names, and authoritative ribbon: `app.js:678-715`. |
| UC-2 G8, success, minimal guarantee | Packaged Chrome/Testcontainers path compares the exact database document before/after at `WorkspaceBrowserIT.java:313-400`; it observes subject-only, teacher-only, combined, clear-one, clear-all, Day change, zero range, single/intersected filters, filtered selection, all ribbon states, and no console errors. |
| RULE-1, RULE-2, RULE-4, RULE-6-RULE-12, RULE-14, RULE-16 | Existing native module boundary remains; model indexes identities once, the state owner rejects unknown identities and retains only range/day in local storage, all text is catalogued, and the real browser uses only the approved snapshot/read path. The UC-2 fixture is isolated in `WorkspaceBrowserIT`. |

### UC-2 Convergence Findings

- G-1: The mandatory complete `WorkspaceBrowserIT` regression class was not completed after the UC-2 change. The targeted UC-2 and shared UC-1 journeys pass, but the feature's applicable rules require the full relevant suite and approved related-UC regression. Re-run the full browser class in an environment that permits a command to outlive 30 seconds, then reconverge without changing the contract.

## UC-3 Evidence

- Started from: a76b802
- Pre-existing dirty files: none
- Implementation submission: HEAD at convergence
- Changed files: `app.js`, `inspection-state.js`, `messages.js`, `styles.css`, `WorkspaceBrowserIT.java`, this status file, and `checkpoints/UC-3.md`.
- Commands and results: `mvn -q -pl timetable-workspace -am test` passed; `git diff --check` passed. Three isolated Chrome/Testcontainers PostgreSQL journeys passed for search/narrowing (28.28s), class/teacher/room focus (24.44s), and empty focused return (26.75s).

| Contract element | Evidence |
|---|---|
| UC-3 main, 2a, 3a, G1-G3 | `WorkspaceBrowserIT.narrowsAndFocusesAcceptedTimetableInRealBrowser` drives scale search cues, explicit intersected filters, empty search, empty narrowed result, reset, and unchanged durable state. |
| UC-3 main, 4a, 5a, G4-G7 | `WorkspaceBrowserIT.opensFocusedAcceptedSchedulesInRealBrowser` and `returnsFromEmptyAndNarrowFocusedSchedulesInRealBrowser` drive selected details, class/teacher/room focus, an empty Room 99 schedule, return context, and unchanged durable state. |
| UC-3 extension 4b, G8 | Existing accepted-workspace narrow read-only browser regression remains covered; the UC-3 focused state uses the same narrow renderer and withholds repair actions. |
| RULE-1, RULE-2, RULE-4, RULE-6, RULE-7, RULE-9-RULE-12, RULE-15, RULE-16 | Native shared state/model path, catalogued safe rendering, exact ID filters, real browser scale fixture, and before/after aggregate comparisons. |

## Blockers

none

## Deviations

none
