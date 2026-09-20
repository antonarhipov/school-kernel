# Use-Case Status: School Kernel v1

## Current

- Use case: UC-2
- Status: READY_FOR_CONVERGENCE
- Next eligible: none

## Progress

| Use case | Status | Depends on | Implementation | Convergence |
|---|---|---|---|---|
| UC-1 | APPROVED | none | `8c2fb7f`; revises `422a774` for C-1 and C-2 | `spec/convergence/UC-1.md` - APPROVE |
| UC-2 | READY_FOR_CONVERGENCE | UC-1 | `HEAD at convergence` from `498fb2e` | pending |

## UC-1 Evidence

- Started: 2026-09-19T23:38:42Z
- Completed: 2026-09-20T00:17:06Z
- Revision completed: 2026-09-20T00:27:54Z
- Started from: `69cc268a1b90b960c320d4a82cb1e301d664d898`
- Pre-existing dirty files: none
- Implementation submission: `HEAD at convergence`
- Convergence findings addressed: C-1 publishes and independently verifies `schoolId` lineage; C-2 preserves derivable execution and accepted-input metadata on handled outcomes.
- Changed files:
  - Build and distribution: `pom.xml`, `.mvn/wrapper/maven-wrapper.properties`, `mvnw`, `mvnw.cmd`, `school-kernel`, `README.md`
  - Examples: `examples/empty-school.json`, `examples/initial-school.json`
  - Application and CLI: `src/main/java/org/schoolkernel/application/{FileBoundary,PlanFiles,PlanRequest,PlanService,TransportException}.java`, `src/main/java/org/schoolkernel/cli/SchoolKernelMain.java`
  - Contract and domain: `src/main/java/org/schoolkernel/contract/{DefinitionSchemaValidator,JsonSupport,ResultFactory,RevisionService,SchoolDefinitionDto}.java`, `src/main/java/org/schoolkernel/domain/{DefinitionValidator,SchoolDefinition,ValidationError,ValidationReport}.java`
  - Solver: `src/main/java/org/schoolkernel/solver/{ConstraintDiagnostic,HardConstraintDiagnostics,InitialSolver,PeriodValue,PlanningLesson,PlanningMapper,PreflightFeasibilityCheck,RoomValue,ScheduleEvaluator,SchoolConstraintProvider,SchoolSchedule,SolverAdapter}.java`
  - Schemas: `src/main/resources/schema/{result-v1.schema.json,school-definition-v1.schema.json}`
  - Tests: `src/test/java/org/schoolkernel/application/PlanServiceTest.java`, `src/test/java/org/schoolkernel/benchmark/TargetScaleBenchmark.java`, `src/test/java/org/schoolkernel/cli/PlanCliIT.java`, `src/test/java/org/schoolkernel/contract/ContractTest.java`, `src/test/java/org/schoolkernel/domain/DefinitionValidatorTest.java`, `src/test/java/org/schoolkernel/solver/{ComponentBoundaryTest,SchoolConstraintProviderTest,SolverConfigurationTest}.java`
  - Fixtures: `src/test/resources/fixtures/{empty-plan,no-room-plan,search-conflict-plan,valid-plan}.json`
  - Execution artifacts: `spec/status.md`, `spec/checkpoints/UC-1.md`
- Commands and results:
  - `./mvnw -q clean verify` - PASS; 45 tests, 0 failures/errors/skips (43 UC-1-owned plus 2 concurrent fixture tests).
  - `./mvnw -Pbenchmark test` - PASS; 1 non-gating target-scale benchmark, 1,000 lessons / 100 teachers / 60 cohorts / 100 rooms / 60 periods, observed 11,821 ms, no threshold asserted.
  - `./mvnw dependency:tree -Dincludes=ai.timefold.solver:timefold-solver-core,tools.jackson.core:jackson-databind,com.networknt:json-schema-validator,info.picocli:picocli,io.github.erdtman:java-json-canonicalization -Dscope=runtime` - PASS; resolved Timefold 2.6.0, Jackson 3.2.2, NetworkNT 3.0.7, Picocli 4.7.7, and JCS 1.1.
  - `./school-kernel plan --definition examples/initial-school.json --output /tmp/school-kernel-uc1.5zpLTs/result.json --step-limit 100 --correlation-id uc1-final-journey` - exit 0; canonical `FEASIBLE`, 2 complete assignments, `STEP_LIMIT`, revisions and score breakdown present.
  - `git diff --check` - PASS.
  - Revision verification: `./mvnw -q clean verify` - PASS; 45 tests, 0 failures/errors/skips. Fresh launcher journeys published `schoolId: demo-school` on `FEASIBLE` and seed 0 / step limit 10 on malformed `INVALID_INPUT`.

| Contract element | Evidence |
|---|---|
| UC-1 main steps 1-3 | `PlanCliIT.mainSuccessScenario` and the final launcher journey exercise the packaged actor boundary and canonical output. |
| UC-1 extensions 1a-1c | `PlanCliIT.cliMisusePreservesFiles`, `overwriteRules`, `transportFailuresDoNotPublish`; `PlanServiceTest.resourceSafeguardPreservesDestination`. |
| UC-1 extensions 2a-2d | `PlanCliIT.invalidInputs`, `obviousNoRoom`, `emptyProblem`; `PlanServiceTest.invalidInputNeverInvokesSolver`, `emptyInputBypassesSolver`. |
| UC-1 extensions 2e-2i | `PlanCliIT.reachedStepLimitPublishesBestFeasibleResult`, `exhaustedSearchDoesNotPublishCandidate`, `interruptionDoesNotPublish`; `PlanServiceTest.unexpectedFailurePublishesSafeInternalError`, `publicationFailurePreservesDestination`. |
| UC-1 G1-G3 | Strict schema/semantic tests, component mapping tests, all 10 hard and all 4 soft catalog ConstraintVerifier tests, and exact result breakdown assertions. |
| UC-1 G4-G6 | Result-schema/process tests, candidate non-disclosure test, RFC 8785 vectors, fixed SHA-256 digest, permutation and timetable-scope revision tests. |
| UC-1 G7-G11 | Solver configuration tests, overwrite and two-invocation distribution tests, documentation, and the separate target-scale benchmark. |
| UC-1 success postcondition | Packaged `FEASIBLE` result validates against the result schema, is canonical, complete, hard-valid, and has a verifiable timetable revision. |
| UC-1 minimal guarantee | Misuse, transport, safeguard, interruption, incomplete-candidate, internal, and injected publication failures prove absence of partial timetable disclosure and preservation where required. |
| RULE-1 through RULE-13, RULE-16, RULE-17 | Detailed code, test, runtime, and command pointers are in `spec/checkpoints/UC-1.md`. |

## UC-2 Evidence

- Started from: `498fb2e358cf03b46dc5f4bae84072691b5cad28`
- Pre-existing dirty files: `src/test/java/org/schoolkernel/fixtures/MerivaljaExampleFixtureTests.java`, `src/test/resources/fixtures/README.md`, `src/test/resources/fixtures/merivalja-1a-1b-4a-4avr-4b-9a-9b.json`, `src/test/resources/fixtures/merivalja-5a-5b.json`
- Prior convergence findings: none
- Implementation submission: `HEAD at convergence`
- Changed files: `README.md`, `examples/updated-school.json`, `spec/status.md`, `spec/checkpoints/UC-2.md`, `src/main/java/org/schoolkernel/application/{ReplanRequest,ReplanService}.java`, `src/main/java/org/schoolkernel/cli/SchoolKernelMain.java`, `src/main/java/org/schoolkernel/contract/{CurrentTimetableReader,ResultFactory}.java`, `src/main/java/org/schoolkernel/domain/{DefinitionValidator,SchoolDefinition}.java`, `src/main/java/org/schoolkernel/solver/{HardConstraintDiagnostics,PlanningLesson,PlanningMapper,ScheduleEvaluator,SchoolConstraintProvider,SolverAdapter}.java`, `src/main/resources/schema/result-v1.schema.json`, `src/test/java/org/schoolkernel/application/PlanServiceTest.java`, `src/test/java/org/schoolkernel/benchmark/TargetScaleBenchmark.java`, `src/test/java/org/schoolkernel/cli/ReplanCliIT.java`, `src/test/java/org/schoolkernel/solver/SchoolConstraintProviderTest.java`
- Commands and results:
  - `./mvnw -q clean verify` - PASS; 53 tests, 0 failures/errors/skips (51 implementation-owned plus 2 concurrent fixture tests).
  - `./school-kernel plan --definition examples/initial-school.json --output /tmp/school-kernel-uc2.rc692V/current.json --step-limit 100` - exit 0, approved UC-1 baseline.
  - `./school-kernel replan --definition examples/updated-school.json --current /tmp/school-kernel-uc2.rc692V/current.json --output /tmp/school-kernel-uc2.rc692V/revised.json --step-limit 100` - exit 0, complete `FEASIBLE`, zero avoidable moves, empty change categories, current preserved.
  - `git diff --check` - PASS.

| Contract element | Evidence |
|---|---|
| UC-2 main and success | `ReplanCliIT.mainSuccessAndDirectSuccessor` consumes production UC-1 output, preserves assignments, validates the result, preserves current bytes, and uses the revised result in a second direct replan. |
| UC-2 extensions 1a-1c, 2a-2c | `ReplanCliIT.misuseAndMalformedInputsPreserveFiles`, `rejectsTamperingAndLineageMismatch`, and `lockAndFeasibilityFailures`. |
| UC-2 extensions 2d-2h | Main journey accepts the updated definition, empty update reports cancellations, limit-bounded results remain feasible, preflight and exhausted-search fixtures disclose no timetable/change report. |
| UC-2 extensions 2i-2k | Shared safe-result, interrupt, and atomic-publication mechanisms remain covered by approved UC-1 tests; `ReplanService` applies them at the same output boundary. |
| UC-2 G1-G3 | Approved UC-1 invariants, current result schema/revision verification, same-school/direct-lineage checks, and updated-definition hard validation. |
| UC-2 G4-G8 | Baseline facts use only common lesson IDs; fixed Bendable levels and `stabilityMoves` prove period-before-room ordering and forced-dimension exclusion. |
| UC-2 G9-G11 | `classifiesObservableChanges`, `classifiesSolverChosenMoves`, `emptyUpdateReportsCancellations`, and main/direct-successor packaged journeys assert exact deterministic categories and bounded-search semantics. |
| UC-2 minimal guarantee | Tamper/lineage/misuse/transport/search failures publish no replacement timetable or change report and preserve the current file. |
| RULE-1 through RULE-12, RULE-14 through RULE-17 | Detailed evidence is recorded in `spec/checkpoints/UC-2.md`; the full suite regresses approved UC-1. |

## Blockers

None.

## Deviations

None. Four concurrent fixture files appeared after UC-1 started and are deliberately excluded from this submission:
`src/test/java/org/schoolkernel/fixtures/MerivaljaExampleFixtureTests.java`,
`src/test/resources/fixtures/merivalja-5a-5b.json`,
`src/test/resources/fixtures/merivalja-1a-1b-4a-4avr-4b-9a-9b.json`, and
`src/test/resources/fixtures/README.md`.
