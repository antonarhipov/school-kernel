# Use-Case Status: timetable-inspection-ux

## Current

- Use case: UC-1
- Status: PENDING_WALKTHROUGH
- Next eligible: UC-1

## Progress

| Use case | Status | Depends on | Implementation | Convergence |
|---|---|---|---|---|
| UC-1 | PENDING_WALKTHROUGH | none | Revision C-1 through C-3 technically converged | convergence/UC-1.md — PENDING WALKTHROUGH |
| UC-2 | NOT_STARTED | UC-1 | - | - |
| UC-3 | NOT_STARTED | UC-1 | - | - |

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

## Blockers

none

## Deviations

none
