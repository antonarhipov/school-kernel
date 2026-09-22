# Use-Case Checkpoint: UC-1 - Inspect the accepted school in Week or Day

## Summary

- Status: READY_FOR_CONVERGENCE
- Base commit: 85afad2
- Submission commit: HEAD at convergence
- Relations verified: none

## Contract Evidence

| Contract element | Test or runtime evidence | Result |
|---|---|---|
| UC-1 main steps 1-5 | `WorkspaceBrowserIT.inspectsAcceptedWholeSchoolTimetableInRealBrowser` opens the accepted baseline, asserts the Week table, switches to Day, and verifies the local range preference. | PASS |
| UC-1 main steps 6-7 | The same real-Chrome journey selects `lesson-math-1` and asserts its accepted inspector details. | PASS |
| UC-1 extension 2a | `WorkspaceBrowserIT.showsDeclaredEmptyAcceptedTimetableInRealBrowser` asserts declared class and empty period structure. | PASS |
| UC-1 extension 2c | The main browser journey switches to a 390px viewport and asserts the read-only agenda and withheld desktop actions. | PASS |
| UC-1 G1 and postconditions | Browser journeys compare the workspace aggregate before and after inspection. | PASS |
| UC-1 G3-G5, G8-G10 | Browser DOM assertions cover complete Week structure, Day tile details, fixed compact density, sticky matrix classes, and non-color labels. | PASS |
| UC-1 G12 | `WorkspaceBrowserIT` completed 11 real Chrome/PostgreSQL tests with 0 failures, errors, or skips. | PASS |

## Rule Evidence

| Rule | Evidence | Result |
|---|---|---|
| RULE-1, RULE-2, RULE-6 | `accepted-model.js` creates the accepted inspection model once; `app.js` renders using stable lesson IDs. | PASS |
| RULE-3 | `app.js` Week table and `styles.css` fixed compact/sticky table rules. | PASS |
| RULE-4, RULE-5 | `app.js` is the range-transition authority and validates bounded versioned local preferences. | PASS |
| RULE-9, RULE-10 | `messages.js`, escaped rendering, native controls, accessible Week labels, and browser assertions. | PASS |
| RULE-11, RULE-12 | Inspection stays on the existing workspace read path; browser state comparisons prove no aggregate mutation. | PASS |
| RULE-13 | Docker-backed real-browser suite passed. | PASS |

## Validation

- Focused commands: `mvn -pl timetable-workspace -am test` — 26 passed; `git diff --check` — passed.
- Full relevant suite: `mvn -pl timetable-workspace -am -Dit.test=WorkspaceBrowserIT verify` — 11 passed, 0 failures, 0 errors, 0 skips.
- Working tree impact from tests: none beyond intentional UC files.
- Runtime evidence: accepted baseline was rendered by packaged workspace assets in Chrome against Testcontainers PostgreSQL; durable workspace aggregate remained unchanged.
- Changed files: `app.js`, `accepted-model.js`, `messages.js`, `styles.css`, `WorkspaceBrowserIT.java`, `status.md`, this checkpoint.
- Approved UCs regression-tested: existing accepted workspace and repair/proposal browser coverage ran in `WorkspaceBrowserIT`.

## Notes

The human administrator walkthrough and the feature-wide five-professional gate remain convergence work; no administrator approval is claimed here.

READY FOR CONVERGENCE: UC-1
