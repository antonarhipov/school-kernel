# Use-Case Checkpoint: UC-2 - Trace subject teaching and teacher load

## Summary

- Status: READY_FOR_CONVERGENCE
- Base commit: `be92691`
- Submission commit: HEAD at convergence
- Relations verified: UC-1 `Requires` and extension at UC-1 4a, through the existing accepted-timetable rendering path.

## Contract Evidence

| Contract element | Test or runtime evidence | Result |
|---|---|---|
| UC-2 main steps 1-5 | `WorkspaceBrowserIT.tracesSubjectTeachingAndTeacherLoadInRealBrowser` selects exact Math and Alex, asserts non-destructive subject/teacher/dual cues and counts, all three authoritative ribbon states, and Week-to-Day identity retention. | PASS |
| UC-2 main steps 6-9 | The Chrome journey turns on subject/teacher-only modes, proves the represented set intersection, clears subject while retaining Alex, then clears teacher/reset. | PASS |
| UC-2 extensions 2a, 4a, 6a | Tuesday drives zero Math/Alex matches, renders Alex's availability ribbon, and produces an explicit no-lessons active-filter result. | PASS |
| UC-2 extension 7a | The journey selects Science, applies the Math-only mode, receives the selected-lesson-cleared announcement, and observes no inspector. | PASS |
| UC-2 G1-G7 | Immutable model indexes and state-owner transitions drive catalogued native controls; classes, labels, patterns, accessible names, and selected lesson IDs remain distinguishable. | PASS |
| UC-2 G8 and postconditions | Packaged Chrome/Testcontainers test checks before/after PostgreSQL document equality and no console exception after every branch. | PASS |

## Rule Evidence

| Rule | Evidence | Result |
|---|---|---|
| RULE-1, RULE-2 | `accepted-model.js:3-45` extends the one immutable accepted model with subject/teacher indexes; no endpoint or dependency was added. | PASS |
| RULE-4, RULE-6 | `inspection-state.js:4-63`; `app.js:542-557,648-686` own identity, filter-mode, selection-clearing, and range transitions. | PASS |
| RULE-7, RULE-8 | `app.js:604-715` computes represented IDs and counts from the model, with assigned taking priority and availability defaulting to all periods only when omitted. | PASS |
| RULE-9, RULE-10 | `messages.js` supplies introduced text; `app.js:689-715` emits text/pattern/outline cues and native controls; `styles.css` makes states distinguishable without color alone. | PASS |
| RULE-11, RULE-12 | No Java, route, migration, workspace JSON, or security change; the Chrome journey compares the durable document before/after. | PASS |
| RULE-14, RULE-16 | `WorkspaceBrowserIT.java:310-400` begins at UC-1's production postcondition and covers exact subject/teacher, combined, clear, range, zero, filters, ribbon, and mutation checks with isolated fixture data. | PASS |

## Validation

- Focused commands: `node --check` for changed modules and `git diff --check` — passed. `mvn -q -pl timetable-workspace -am -Dit.test=WorkspaceBrowserIT#tracesSubjectTeachingAndTeacherLoadInRealBrowser verify` — 26 unit tests plus 1 real Chrome/Testcontainers PostgreSQL test, all passed.
- Approved-UC regression: `mvn -q -pl timetable-workspace -am -Dit.test=WorkspaceBrowserIT#inspectsAcceptedWholeSchoolTimetableInRealBrowser verify` — 26 unit tests plus 1 real Chrome/Testcontainers UC-1 test, all passed.
- Full-class limitation: the complete `WorkspaceBrowserIT` class was started but cannot survive the environment's 30-second command lifetime; its full-class result is not claimed.
- Working tree impact from tests: none.
- Runtime evidence: packaged static assets ran through the live workspace against Testcontainers PostgreSQL and headless Chrome; all inspected journeys performed no mutation.

READY FOR CONVERGENCE: UC-2
