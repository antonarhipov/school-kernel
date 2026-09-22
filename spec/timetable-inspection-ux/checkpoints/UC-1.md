# Use-Case Checkpoint: UC-1 - Inspect the accepted school in Week or Day

## Summary

- Status: READY_FOR_CONVERGENCE
- Base commit: `1d9f761` (revision of findings C-1 through C-3)
- Submission commit: HEAD at convergence
- Relations verified: none

## Contract Evidence

| Contract element | Test or runtime evidence | Result |
|---|---|---|
| UC-1 main steps 1-5 | Existing Chrome journey opens the accepted baseline, checks complete Week, changes to Day, validates structure/order/Compact density, and confirms the represented range. The state owner applies Week-to-Day selection retention. | PASS |
| UC-1 main steps 6-7 | Chrome selects a stable `lessonId` and asserts the accepted subject, class, teacher, weekday, period, room, and identity in the inspector. | PASS |
| UC-1 extension 2a | `WorkspaceBrowserIT.showsDeclaredEmptyAcceptedTimetableInRealBrowser` verifies classes, declared periods, and explicit empty slots without invented lessons. | PASS |
| UC-1 extension 2b, 5a, 5b | `WorkspaceBrowserIT.handlesInvalidAndBlockedInspectionPreferencesInRealBrowser` covers malformed, stale, invalid range, unknown weekday, oversized, other-school, valid restore, manual Day exclusion announcement/cleared inspector, and blocked `localStorage` write. | PASS |
| UC-1 extension 2c | Existing Chrome journey sets a narrow viewport and verifies the read-only agenda, desktop-workbench notice, and withheld acceptance action. | PASS |
| UC-1 extension 7a | `WorkspaceBrowserIT.measuresTargetScaleInspectionInteractionsInRealBrowser` scrolls the 60-class Week table, selects off-viewport `lesson-999`, asserts its inspector, then verifies its Thursday Day transition. | PASS |
| UC-1 guarantees and postconditions | Browser tests compare exact durable workspace document before/after every inspection journey; no inspection request mutates the aggregate. | PASS |

## Rule Evidence

| Rule | Evidence | Result |
|---|---|---|
| RULE-1 | Native ES modules: `inspection-state.js` owns inspection state; `day-renderer.js`, `week-renderer.js`, and `focused-renderer.js` consume shared model data and return markup. | PASS |
| RULE-2, RULE-6 | `accepted-model.js` remains the single immutable accepted model; UI selection uses `lessonId` across renderers. | PASS |
| RULE-3 | `week-renderer.js` renders complete native Week table slots; Chrome browser assertions exercise the full 60-class matrix and scroll context. | PASS |
| RULE-4 | `app.js` delegates accepted-baseline range/day/selection transitions to `inspection-state.js`; renderers receive inputs and make no state writes. | PASS |
| RULE-5 | `inspection-state.js` uses only `school-kernel.inspection.v1.<schoolId>` and validates exact `{version: 1, range, weekdayId}` records under 1KiB; Chrome covers all invalid/failure variants, isolation, and allowed shape. | PASS |
| RULE-9, RULE-10 | Existing catalog/escaping/native-control browser coverage remains green; extracted renderers preserve escaped authoritative labels. | PASS |
| RULE-11, RULE-12 | No server, schema, route, lifecycle, or security edits; Chrome aggregate comparisons prove presentation-only behavior. | PASS |
| RULE-13 | `mvn -pl timetable-workspace -am -Dit.test=WorkspaceBrowserIT verify` used packaged assets, Chrome, and Testcontainers PostgreSQL: 12 passed, zero failures/errors/skips. | PASS |
| RULE-16 | Isolated generated fixture remains used. Five-professional, three-school walkthrough has not occurred. | PENDING WALKTHROUGH |

## Validation

- Focused command: `mvn -pl timetable-workspace -am -Dit.test=WorkspaceBrowserIT verify` — 26 unit tests and 12 Chrome/PostgreSQL integration tests passed; 0 failures, errors, or skips.
- Hygiene: `git diff --check` — passed.
- Runtime evidence: packaged workspace assets ran against Docker/Testcontainers PostgreSQL and a real Chrome DevTools client; each inspection scenario compared durable workspace state before/after.
- Changed files: `app.js`, `inspection-state.js`, `day-renderer.js`, `week-renderer.js`, `focused-renderer.js`, `WorkspaceBrowserIT.java`, `status.md`, this checkpoint.
- Approved UCs regression-tested: existing accepted workspace, repair, proposal, and scale browser paths remain in the 12-test `WorkspaceBrowserIT` suite.

## Notes

This is the required revision of C-1 through C-3. It does not claim administrator approval; the normative human walkthrough remains the next gate.

READY FOR CONVERGENCE: UC-1
