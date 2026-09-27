# Use-Case Status: Cohort start bounds and day-edge subjects

## Current

- Use case: UC-1, UC-2
- Status: SUBMITTED
- Next eligible: none

## Evidence (UC-1 and UC-2)

- Started from: `96bdf18`.
- `./mvnw -pl kernel-cli -am verify`: 84 unit and 46 packaged CLI tests, zero failures. New: `DayEdgeConstraintTest`
  (8), `DayEdgeCliIT` (3), `ContractTest.dayEdgeFieldsSchemaAndRevision`,
  `DefinitionValidatorTest.dayEdgeFieldDefaults`, `DefinitionValidatorTest.reservedPeriodLockIsValidOnlyForPermittedSubjects`.
- Packaged `./school-kernel plan --definition examples/mvk.json --time-limit PT30S`, seeds 0 through 4: all
  `FEASIBLE`. An independent audit of the raw assignments found zero cohort gaps, zero hard late starts, zero
  middle-of-day Õpiabi, zero second Õpiabi on a day, zero second slot-0 Õpiabi per cohort, and zero non-Õpiabi lessons
  in reserved periods. Õpiabi used slot 0 three to seven times per week across seeds. Soft late-start matches were zero.
- Cost of the new hard rules: weekly-balance matches 14 to 24 across seeds (catalog 5 seed 0: 13); ordinary penalty
  16,692 to 26,773 (catalog 5 seed 0: 15,544).
- Packaged `verify` of the final-jar MVK seed 0 result: `VERIFIED`, catalog 6.
- `./mvnw -pl timetable-workspace -am verify`: 94 integration tests; `WorkspaceImportIT`, `WorkspaceRepairDraftIT`, and
  `WorkspaceRepairPlanningIT` pass with catalog 6 successors. Five Playwright browser tests fail
  (`AcceptedInspectionBrowserIT` x3, `ProposalReviewBrowserIT`, `RepairCompletionBrowserIT`), the same set recorded as
  failing on unmodified code in `kernel-cohort-no-gaps`.
- Not verified: how the workspace UI renders a lesson in a reserved slot. No browser walkthrough was done.
