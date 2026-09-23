# Use-Case Checkpoint: UC-2 - Trace subject teaching and teacher load

## Summary

- Status: READY_FOR_CONVERGENCE
- Base commit: `be92691`
- Revision base commit: `5d426b5` (C-1, G-2, P-1 after prior G-1)
- Submission commit: HEAD at convergence
- Relations verified: UC-1 `Requires` and extension at UC-1 4a, through the existing accepted-timetable rendering path.

## Contract Evidence

| Contract element | Test or runtime evidence | Result |
|---|---|---|
| UC-2 main steps 1-5 | `WorkspaceBrowserIT.tracesSubjectTeachingAndTeacherLoadInRealBrowser` selects exact Math and Alex, asserts non-destructive subject/teacher/dual cues and counts, all three authoritative ribbon states, and Week-to-Day identity retention. | PASS |
| UC-2 main steps 6-9 | The small Chrome journey now requires one teacher match in the one-lesson intersection rather than two out-of-population matches. `tracesScaleInvestigationAndOmittedAvailabilityInRealBrowser` independently derives unique represented/subject/teacher/dual lesson-ID sets from accepted assignments for Week and Day, highlight, subject-only, intersection, teacher-only, clear-one, and reset, and compares them with both displayed totals and visible IDs. | PASS |
| UC-2 extensions 2a, 4a, 6a | Tuesday drives zero Math/Alex matches, renders Alex's availability ribbon, and produces an explicit no-lessons active-filter result. | PASS |
| UC-2 extension 7a | The journey selects Science, applies the Math-only mode, receives the selected-lesson-cleared announcement, and observes no inspector. | PASS |
| UC-2 G1-G7 | Immutable model indexes and state-owner transitions drive catalogued native controls; classes, labels, patterns, accessible names, and selected lesson IDs remain distinguishable. | PASS |
| UC-2 G4, G8 and postconditions | `tracesScaleInvestigationAndOmittedAvailabilityInRealBrowser` drives a kernel-verified accepted pair with 60 classes, 100 teachers, 100 rooms, 1,000 distinct lessons, 60 periods, long authoritative names, occupied/empty entities and periods. It compares every explicit teacher-16 ribbon period against assignments and declared availability (assigned, available-unassigned, unavailable) and verifies that omitted teacher-17 availability yields available-unassigned throughout Day and Week. Packaged Chrome/PostgreSQL compares the exact durable document before/after and reports no console exception. | PASS |

## Rule Evidence

| Rule | Evidence | Result |
|---|---|---|
| RULE-1, RULE-2 | `accepted-model.js:3-45` extends the one immutable accepted model with subject/teacher indexes; no endpoint or dependency was added. | PASS |
| RULE-4, RULE-6 | `inspection-state.js:4-63`; `app.js:542-557,648-686` own identity, filter-mode, selection-clearing, and range transitions. | PASS |
| RULE-7, RULE-8 | `app.js:744-753` counts unique lesson IDs only from `filteredAssignments()`, after every active intersection; `app.js:778-789` prioritizes assignments and defaults omitted availability to every declared period. `WorkspaceBrowserIT.assertInvestigationPopulation` and `assertTeacherRibbon` compute expected IDs and states independently from the verified definition/result pair. | PASS |
| RULE-9, RULE-10 | `messages.js` supplies introduced text; `app.js:689-715` emits text/pattern/outline cues and native controls; `styles.css` makes states distinguishable without color alone. | PASS |
| RULE-11, RULE-12 | No Java, route, migration, workspace JSON, or security change; the Chrome journey compares the durable document before/after. | PASS |
| RULE-14, RULE-16 | Both UC-2 browser journeys begin at UC-1's accepted-timetable production postcondition; `investigationScaleDocument` builds complete synthetic fixture identity/manifest revisions and verifies the definition and accepted pair with `KernelVerifier`. The browser asserts cardinalities, long names, lesson-to-assignment identities, declared/omitted availability, all ribbon states, zero paths, and exact represented lesson IDs. | PASS |

## Validation

- Focused commands: `node --check` for changed modules and `git diff --check` — passed. `mvn -q -pl timetable-workspace -am -Dit.test=WorkspaceBrowserIT#tracesSubjectTeachingAndTeacherLoadInRealBrowser verify` — 26 unit tests plus 1 real Chrome/Testcontainers PostgreSQL test, all passed.
- Approved-UC regression: `mvn -q -pl timetable-workspace -am -Dit.test=WorkspaceBrowserIT#inspectsAcceptedWholeSchoolTimetableInRealBrowser verify` — 26 unit tests plus 1 real Chrome/Testcontainers UC-1 test, all passed.
- G-1 revision: `./mvnw -pl timetable-workspace -am -Dit.test=WorkspaceBrowserIT verify` completed all 16 real Chrome/Testcontainers PostgreSQL browser tests. The initial run exposed an outdated UC-2 test expectation (`active criteria`) after the shared UC-3 message changed to `active filters`: 16 run, 1 failure, 0 errors, 0 skipped. The assertion now checks the rendered catalog wording; `./mvnw -q -pl timetable-workspace -am -Dit.test=WorkspaceBrowserIT verify` passed with 16 browser tests, 0 failures, 0 errors, 0 skipped, including all approved shared browser journeys. This is evidence for reconvergence, not a claim of approval.
- C-1/G-2 revision: Updated the existing browser assertion to require one teacher match in the represented intersection; the previous implementation counted two from the pre-intersection assignments. After the production correction, `./mvnw -q -pl timetable-workspace -am -Dit.test=WorkspaceBrowserIT#tracesScaleInvestigationAndOmittedAvailabilityInRealBrowser verify` passed and kernel verification returned `VERIFIED` for both the definition and complete accepted pair. `./mvnw -q -pl timetable-workspace -am -Dit.test=WorkspaceBrowserIT verify` passed 17 real-browser tests, 0 failures/errors/skips (including approved UC-1, UC-3, and workspace regressions) and 26 unit tests; `node --check timetable-workspace/src/main/resources/static/workspace/app.js` and `git diff --check` passed.
- Working tree impact from tests: none.
- Runtime evidence: packaged static assets ran through the live workspace against Testcontainers PostgreSQL and headless Chrome; all inspected journeys performed no mutation.
- Changed files in this submission: `timetable-workspace/src/main/resources/static/workspace/app.js`, `timetable-workspace/src/test/java/org/schoolkernel/workspace/WorkspaceBrowserIT.java`, `spec/timetable-inspection-ux/checkpoints/UC-2.md`, and `spec/timetable-inspection-ux/status.md`. The verifier's prior `convergence/UC-2.md` rejection and unrelated `.idea`/`spec/timetable-ux-polish` work are excluded.

READY FOR CONVERGENCE: UC-2
