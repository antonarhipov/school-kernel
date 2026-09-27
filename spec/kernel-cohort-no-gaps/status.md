# Use-Case Status: Gap-free cohort days

## Current

- Use case: UC-1
- Status: SUBMITTED
- Next eligible: none

## UC-1 Evidence

- Started from: `1903aaf`.
- `./mvnw -pl kernel-cli -am verify`: 73 unit and 43 packaged CLI tests, zero failures.
- Packaged `./school-kernel plan --definition examples/mvk.json --time-limit PT30S`, seeds 0 through 4: all
  `FEASIBLE`, zero cohort gaps, zero late starts. Solo seed 0: weekly-balance 13 matches, ordinary penalty 15,544
  (catalog 4 soft-only run: 8 matches, 10,585).
- Before the two-level split, a single hard level produced `FEASIBLE` in 3 of 5 seeds; failures were residual
  `hard.room-capability` conflicts on music lessons.
- Packaged `verify` of MVK seed 0 result: `VERIFIED`, catalog 5.
- `./mvnw -pl timetable-workspace -am verify`: 94 integration tests; `WorkspaceImportIT`, `WorkspaceRepairDraftIT`, and
  `WorkspaceRepairPlanningIT` pass. Five Playwright browser tests fail (`AcceptedInspectionBrowserIT` x3,
  `ProposalReviewBrowserIT`, `RepairCompletionBrowserIT`); the same classes also fail on unmodified `1903aaf`.
