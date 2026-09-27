# Use-Case Status: Cohort curators and home rooms

## Current

- Use case: UC-1
- Status: IN_PROGRESS
- Next eligible: none

## UC-1 Evidence

- Started from: `ba02fdd`.
- Pending: 6C home room. Olga's two 6C Russian lessons use A210 and A204; its IntÕ is taught by Grete Suurväli in A209. EduPage's 6C class record still omits the home room as of 2026-09-27.
- Home rooms for the other eleven older cohorts come from their curator-taught IntÕ lesson in the EduPage snapshot; 5D's comes from its Klassitund lessons.
- MVK now uses synthetic 6C curator Olga Simonovitš (`olga-simonovits`), randomly selected from teachers with 6C lessons; she teaches two 6C Russian lessons and curates no other cohort. EduPage declares no curator for 6C.
- MVK locks eleven curator-taught IntÕ lessons to their published home rooms and Siiri Aiaste's fourteen Ajalugu lessons to A231. A temporary candidate with exactly these 25 locks was `FEASIBLE` at `PT60S`, seed 0, with 520 assignments, zero late starts, two weekly-balance matches, and `VERIFIED`. The submitted MVK definition is JSON-equal to that candidate.
- A separate trial locking all 402 lessons in repeated, uniform teacher-subject room pairs was also `FEASIBLE` and `VERIFIED`, but had three late starts and seventeen weekly-balance matches. Uniformity in one snapshot alone did not establish that every pair is a mandatory school assignment; those locks were not added to MVK.
- Fresh `./mvnw -q -pl kernel-cli -am verify` after the 25 MVK locks: 99 unit tests and 48 packaged CLI tests, zero failures, errors, or skips. `git diff --check` is clean.
- Baseline, catalog 7, packaged MVK plan at `PT60S`, seed 0: 99 Klassitund lessons outside their curator's room;
  non-preferred-room 248, teacher gaps 13.
- `./mvnw -pl kernel-cli -am verify`: 99 unit and 48 packaged CLI tests, zero failures. New:
  `HomeRoomConstraintTest` (3), `HomeRoomCliIT` (1), `ContractTest.curatorFieldsSchemaAndRevision`,
  `ContractTest.mvkDeclaresCuratorsAndHomeRooms`, `DefinitionValidatorTest` (2).
- Packaged `./school-kernel plan --definition examples/mvk.json --time-limit PT60S`, seeds 0 through 2: all
  `FEASIBLE`, zero Klassitund outside the home room. Non-preferred-room 243 to 260, teacher gaps 46 to 66, weekly
  balance 0 to 2, late starts 0. Packaged `verify` of seed 0: `VERIFIED`.
- `./mvnw -pl timetable-workspace -am verify`: `WorkspaceImportIT` (after moving its unsupported catalog to 9),
  `WorkspaceRepairDraftIT`, `WorkspaceRepairPlanningIT`, `WorkspaceInitialPlanningIT` and the other non-browser ITs
  pass. The same five Playwright browser tests fail as before (`AcceptedInspectionBrowserIT` x3,
  `ProposalReviewBrowserIT`, `RepairCompletionBrowserIT`).
- Not verified: seeds 3 and 4, and repair of a catalog 7 predecessor with MVK scale.
