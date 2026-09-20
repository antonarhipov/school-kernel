# Use-Case Status: School Kernel v1

## Current

- Use case: UC-1
- Status: NEEDS_REVISION
- Next eligible: UC-1

## Progress

| Use case | Status | Depends on | Implementation | Convergence |
|---|---|---|---|---|
| UC-1 | NEEDS_REVISION | none | `422a774` from `69cc268` | `spec/convergence/UC-1.md` - REJECT (C-1, C-2) |
| UC-2 | NOT_STARTED | UC-1 | - | - |

## UC-1 Evidence

- Started: 2026-09-19T23:38:42Z
- Completed: 2026-09-20T00:17:06Z
- Started from: `69cc268a1b90b960c320d4a82cb1e301d664d898`
- Pre-existing dirty files: none
- Implementation submission: `HEAD at convergence`
- Convergence findings: C-1 (published timetable lacks verifiable `schoolId` lineage), C-2 (handled outcomes omit derivable result-envelope metadata)
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

## Blockers

None.

## Deviations

None. Four concurrent fixture files appeared after UC-1 started and are deliberately excluded from this submission:
`src/test/java/org/schoolkernel/fixtures/MerivaljaExampleFixtureTests.java`,
`src/test/resources/fixtures/merivalja-5a-5b.json`,
`src/test/resources/fixtures/merivalja-1a-1b-4a-4avr-4b-9a-9b.json`, and
`src/test/resources/fixtures/README.md`.
