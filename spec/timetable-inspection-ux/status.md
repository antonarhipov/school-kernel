# Use-Case Status: timetable-inspection-ux

## Current

- Use case: UC-2
- Status: READY_FOR_CONVERGENCE
- Next eligible: UC-2 reconvergence

## Progress

| Use case | Status | Depends on | Implementation | Convergence |
|---|---|---|---|---|
| UC-1 | APPROVED | none | Revision C-1 through C-3 technically converged; user walkthrough PASS | convergence/UC-1.md — APPROVE |
| UC-2 | READY_FOR_CONVERGENCE | UC-1 | Represented-only unique-ID totals; normative-scale verified fixture and explicit/omitted teacher availability; 17 browser tests passed | convergence/UC-2.md — prior REJECT (C-1, G-2, P-1), pending reconvergence |
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
- Revision started from: 5d426b5 (C-1, G-2, P-1)
- Pre-existing dirty files for this revision: `.idea/encodings.xml`, `spec/timetable-inspection-ux/convergence/UC-2.md`, `spec/timetable-inspection-ux/checkpoints/UC-2.md`, this status file, `WorkspaceBrowserIT.java`, and untracked `spec/timetable-ux-polish/rules.md`, `spec/timetable-ux-polish/status.md`. The existing G-1 checkpoint/status/test changes belong to this UC-2 revision; unrelated and verifier-owned files remain excluded from the implementation commit.
- Implementation submission: HEAD at convergence
- Changed files: `timetable-workspace/src/main/resources/static/workspace/accepted-model.js`, `timetable-workspace/src/main/resources/static/workspace/inspection-state.js`, `timetable-workspace/src/main/resources/static/workspace/app.js`, `timetable-workspace/src/main/resources/static/workspace/messages.js`, `timetable-workspace/src/main/resources/static/workspace/styles.css`, `timetable-workspace/src/test/java/org/schoolkernel/workspace/WorkspaceBrowserIT.java`, this status file, and `checkpoints/UC-2.md`.
- Commands and results: prior `node --check` for changed ES modules and `git diff --check` passed. The C-1 assertion was updated to reject the prior pre-intersection count of two; it was not run separately before the production fix. The new scale-browser test passed with both kernel verification calls returning `VERIFIED`. After correcting a test-only Jackson compilation error, `./mvnw -q -pl timetable-workspace -am -Dit.test=WorkspaceBrowserIT verify` passed 17 real Chrome/Testcontainers PostgreSQL browser tests (0 failures/errors/skips) plus 26 unit tests. `node --check timetable-workspace/src/main/resources/static/workspace/app.js` and `git diff --check` passed. See `checkpoints/UC-2.md` for revision evidence.

| Contract element | Evidence |
|---|---|
| UC-2 main steps 1-5 | `WorkspaceBrowserIT.tracesSubjectTeachingAndTeacherLoadInRealBrowser` selects exact Math/Alex identities, asserts non-destructive cues and dual count, checks all three ribbon classifications, and retains identities through Week-to-Day at `WorkspaceBrowserIT.java:328-366`. |
| UC-2 main steps 6-9 | The real-browser journey now rejects out-of-population teacher totals at `WorkspaceBrowserIT.java:363-370`; the scale journey computes independent represented/subject/teacher/dual unique-ID sets and compares all rendered IDs and counts across highlight, subject-only, intersected, teacher-only, empty, clear-one and reset. `app.js:744-753` counts only after all explicit filters. |
| UC-2 extensions 2a, 4a, 6a, 7a | Tuesday asserts exact zero subject/teacher counts and a no-match intersection; filtered science selection is cleared with a status announcement while identities remain selected at `WorkspaceBrowserIT.java:353-392`. |
| UC-2 G1-G7 | Stable IDs and immutable indexes: `accepted-model.js:3-45`; state reset/no durable preference expansion: `inspection-state.js:4-63`; visible non-color cue markup, accessible names, and authoritative ribbon: `app.js:678-715`. |
| UC-2 G4, G8, success, minimal guarantee | `WorkspaceBrowserIT.tracesScaleInvestigationAndOmittedAvailabilityInRealBrowser` validates 60/100/100/1,000/60 fixture cardinalities, long names, unique lessons, full lesson-to-assignment identities, populated/empty entities and periods, and verifies the complete synthetic accepted pair with `KernelVerifier`. Chrome checks every assigned/available-unassigned/unavailable teacher-16 slot by value and every teacher-17 omitted-availability slot in Day and Week, plus exact durable before/after equality. The small journey retains zero/selection branches and the same mutation check. |
| RULE-1, RULE-2, RULE-4, RULE-6-RULE-12, RULE-14, RULE-16 | Existing native module boundary remains; state owner retains only range/day locally. `app.js:744-753` counts unique IDs from the represented filtered result; `app.js:778-789` derives ribbon state from accepted assignments and omitted/declared availability. The scale fixture is isolated, kernel-verified, and checked by value against rendered Chrome IDs/ribbon; no route or workspace mutation was introduced. |

### UC-2 Convergence Findings

- G-1 revision: The full `WorkspaceBrowserIT` class ran to completion. Its first run exposed a stale UC-2 assertion for `active criteria` after the shared UC-3 empty-view copy became `active filters` (16 browser tests, 1 failure). The assertion was corrected to match the rendered catalog message; `./mvnw -q -pl timetable-workspace -am -Dit.test=WorkspaceBrowserIT verify` then passed all 16 real Chrome/Testcontainers PostgreSQL browser tests with 0 failures, errors, or skips. `git diff --check` passed. The checkpoint now supplies the missing shared browser regression evidence for reconvergence; the previous rejection remains the historical verdict, not an approval.
- C-1/G-2/P-1 revision: `app.js` counts only unique represented lesson IDs after intersections. `WorkspaceBrowserIT` independently checks those sets and authoritative/omitted ribbon states on a kernel-verified 1,000-assignment accepted baseline. Full class: 17 passed, 0 failed/errors/skipped; the attributable implementation, test, status, and checkpoint form a single commit for reconvergence, without verifier-owned or unrelated changes. The historical REJECT is retained pending independent convergence.

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
