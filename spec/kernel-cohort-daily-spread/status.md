# Use-Case Status: Hard cohort daily lesson spread

## Current

- Use case: UC-1
- Status: SUBMITTED
- Next eligible: none

## UC-1 Evidence

- Started from: `25a5729`.
- Baseline, catalog 6, packaged MVK plan at `PT30S`, seed 0: seven cohorts over a spread of one; 5B `2,5,3,6,4`,
  2A `6,4,3,5,4`; weekly-balance 21 matches.
- Rejected alternatives, MVK at `PT30S`, seeds 0 through 2: weekly-balance weight 1,000,000 left four to six cohorts
  over target and 23 to 28 soft late starts. Hard spread 1 for every cohort ended at hard level 1 score -4 on every
  seed. Tiered hard spread was feasible in one of three seeds; every failure was a single remaining cohort gap,
  usually 5B's. Split period-then-room construction and the Tabu, Great Deluge, and Diversified Late Acceptance local
  search types were each worse than the current configuration.
- `./mvnw -pl kernel-cli -am verify`: 92 unit and 47 packaged CLI tests, zero failures. New: `DailySpreadConstraintTest`
  (6), `DailySpreadCliIT` (1), `ContractTest.dailySpreadLimitSchemaAndRevision`,
  `DefinitionValidatorTest.dailySpreadLimitIsUnboundedUnlessDeclared`.
- Packaged `./school-kernel plan --definition examples/mvk.json --time-limit PT60S`, seeds 0 through 4: all
  `FEASIBLE`. Maximum spread 1 for grades 1 through 4 and 2 for grades 5 through 9 in every seed. 5B `4,4,4,4,4` in
  four seeds and `5,3,4,4,4` in one. Weekly-balance 1 to 4 matches, teacher gaps 25 to 58, soft late starts 1 to 2.
- Packaged `verify` of MVK seed 0 at `PT60S`: `VERIFIED`, catalog 7.
- Follow-up: that configuration still produced afternoon-only days in every seed (5B Wednesday from period 6, 6B
  Thursday from period 5). The single music room was full on all five weekdays at periods 2 through 5. Grades 4
  through 9 with hard start slots 2 or 3 at `PT60S`: 0 of 5 feasible each; slot 3 at `PT120S`: 0 of 5; slot 4: 3 of
  5; slot 3 with a second music room: 2 of 5; slot 4 with a second music room: 5 of 5.
- Final MVK (slot 4 for grades 4 through 9, `k2` music-capable), packaged plan at `PT60S`, seeds 0 through 4: all
  `FEASIBLE`. Latest first lesson period 4 (11:50), 6B in two seeds; every other cohort-day starts by period 3. No
  day ends after period 8; those ending at period 8 are six-lesson grade 8 and 9 days from period 3. Spread still at
  most 1 for grades 1 through 4 and 2 for grades 5 through 9. Packaged `verify` of seed 0: `VERIFIED`, catalog 7.
- `./mvnw -pl kernel-cli -am verify` after the MVK change: 92 unit and 47 packaged CLI tests, zero failures.
- `./mvnw -pl timetable-workspace -am verify`: 94 integration tests. `WorkspaceImportIT`, `WorkspaceRepairDraftIT`, and
  `WorkspaceInitialPlanningIT` pass. `WorkspaceRepairPlanningIT` first failed 8 of 10 because its 45-second waits were
  shorter than the new one-minute run. With 75-second waits it passes 10 of 10 in a targeted rerun; the full suite was
  not rerun. The same five Playwright browser tests fail as before (`AcceptedInspectionBrowserIT` x3,
  `ProposalReviewBrowserIT`, `RepairCompletionBrowserIT`) on layout and ribbon assertions.
- Not verified: `PT30S` reliability over more than three seeds, and a browser walkthrough of the relabelled
  one-minute buttons.
