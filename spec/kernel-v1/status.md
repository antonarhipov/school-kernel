# Use-Case Status: School Kernel v1

## Current

- Use case: UC-2
- Status: READY_FOR_CONVERGENCE
- Next eligible: none

## Progress

| Use case | Status | Depends on | Implementation | Convergence |
|---|---|---|---|---|
| UC-1 | APPROVED | none | `78b5bb7`; revises `8c2fb7f` for required school `displayName` | `convergence/UC-1.md` - APPROVE |
| UC-2 | READY_FOR_CONVERGENCE | UC-1 | HEAD at convergence; reconciles `e6401f6` with approved school `displayName` contract | pending reconvergence |

## UC-1 Evidence

- Contract revision started: 2026-09-21
- Contract revision base: `d0e90317359b08be026c9d8f9a8f37195cbea3f7`
- Contract revision decision: required nonblank school-level `displayName`; it participates in the complete definition revision.
- Pre-existing dirty files for this reconciliation: timetable-workspace UC-1 revision files; they are excluded from the
  kernel UC-1 submission.
- Contract revision submission: HEAD at convergence.
- Contract revision changed files: definition schema, DTO/domain propagation, kernel specification/decision, all
  definition fixtures/examples and regenerated lineage, constructor-call regressions, `ContractTest`, this status, and
  `checkpoints/UC-1.md`.
- Contract revision verification: `mvn -q clean verify` - PASS, 120 tests, 0 failures/errors/skips (85 kernel and 35
  timetable-workspace); `git diff --check` - PASS; tests caused no tracked-file drift.

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

- Contract-revision reconciliation started from: `b5da3d6478b5291e5448b15df9157bd6ef43c1f4`.
- Pre-existing dirty files: timetable-workspace UC-1 revision files; excluded from this submission.
- Implementation submission: HEAD at convergence.
- Changed files: `spec/kernel-v1/checkpoints/UC-2.md`, `spec/kernel-v1/status.md`; the shared contract, updated
  definition lineage, and constructor regression were committed and approved with required UC-1.
- Commands and results: fresh packaged plan -> replan -> verify exited 0/0/0 with statuses
  `FEASIBLE`/`FEASIBLE`/`VERIFIED`, two revised assignments, and exact direct lineage; kernel suite 85/0/0/0; full
  reactor 120/0/0/0; `git diff --check` PASS.

- Started from: `498fb2e358cf03b46dc5f4bae84072691b5cad28`
- Pre-existing dirty files: `src/test/java/org/schoolkernel/fixtures/MerivaljaExampleFixtureTests.java`, `src/test/resources/fixtures/README.md`, `src/test/resources/fixtures/merivalja-1a-1b-4a-4avr-4b-9a-9b.json`, `src/test/resources/fixtures/merivalja-5a-5b.json`
- Prior convergence findings: C-1, G-1, G-2, G-3 from `801deb5`
- Implementation submission: `HEAD at convergence`
- Convergence findings: C-1, G-1, G-2, G-3
- Reconvergence finding: C-2
- Second revision started from: `bbbecb1f9636b331068137ab5c497a27e4c358f5`
- Second revision completed: 2026-09-20T01:06:29Z
- Final convergence: APPROVE at `e6401f6`; C-1, C-2, and G-1 through G-3 resolved.
- Revision started from: `801deb59066831d9e213245305cf1ee460613136`
- Revision completed: 2026-09-20T00:58:37Z
- Changed files: `README.md`, `examples/updated-school.json`, `spec/status.md`, `spec/checkpoints/UC-2.md`, `src/main/java/org/schoolkernel/application/{ReplanRequest,ReplanService}.java`, `src/main/java/org/schoolkernel/cli/SchoolKernelMain.java`, `src/main/java/org/schoolkernel/contract/{CurrentTimetableReader,ResultFactory}.java`, `src/main/java/org/schoolkernel/domain/{DefinitionValidator,SchoolDefinition}.java`, `src/main/java/org/schoolkernel/solver/{HardConstraintDiagnostics,PlanningLesson,PlanningMapper,ScheduleEvaluator,SchoolConstraintProvider,SolverAdapter}.java`, `src/main/resources/schema/result-v1.schema.json`, `src/test/java/org/schoolkernel/application/PlanServiceTest.java`, `src/test/java/org/schoolkernel/benchmark/TargetScaleBenchmark.java`, `src/test/java/org/schoolkernel/cli/ReplanCliIT.java`, `src/test/java/org/schoolkernel/solver/SchoolConstraintProviderTest.java`
- Revision files: `src/main/java/org/schoolkernel/solver/ReplanningSolver.java`, `src/test/java/org/schoolkernel/application/ReplanServiceTest.java`, `src/test/java/org/schoolkernel/solver/ReplanningSolverTest.java`; revised current reader, replan service/adapter/constraints, CLI integration tests, solver configuration tests, status, and checkpoint.
- Commands and results:
  - `./mvnw -q clean verify` - PASS; 61 tests, 0 failures/errors/skips (59 implementation-owned plus 2 concurrent fixture tests).
  - `./mvnw -q -Dtest=ReplanServiceTest,ReplanningSolverTest,SolverConfigurationTest,SchoolConstraintProviderTest test` - PASS; injected failure boundaries, score levels, dominance trade-offs, and stability matches.
  - `./mvnw -q -Dit.test=ReplanCliIT verify` - PASS; 42 unit tests and 8 packaged replan integration tests in the focused lifecycle.
  - `./school-kernel plan --definition examples/initial-school.json --output /tmp/school-kernel-uc2.rc692V/current.json --step-limit 100` - exit 0, approved UC-1 baseline.
  - `./school-kernel replan --definition examples/updated-school.json --current /tmp/school-kernel-uc2.rc692V/current.json --output /tmp/school-kernel-uc2.rc692V/revised.json --step-limit 100` - exit 0, complete `FEASIBLE`, zero avoidable moves, empty change categories, current preserved.
  - Revision journey: packaged replan exits 0 with the complete unchanged timetable; a schema-valid, revision-verifiable duplicate current assignment now exits 2 with deterministic `INVALID_INPUT`, no solver/timetable/change report.
  - C-2 journey: a recomputed-revision current snapshot with teacher, cohort, and room period collisions exits 2 with three exact ordered errors, no solver/timetable/change report, and preserved current bytes.
  - `git diff --check` - PASS.

| Contract element | Evidence |
|---|---|
| UC-2 main and success | `ReplanCliIT.mainSuccessAndDirectSuccessor` consumes production UC-1 output, preserves assignments, validates the result, preserves current bytes, and uses the revised result in a second direct replan. |
| UC-2 extensions 1a-1c, 2a-2c | Packaged misuse/transport/overwrite, malformed/tampered/duplicate/colliding current, lineage, school, and lock-conflict cases; every snapshot-provable invalidity precedes solving. |
| UC-2 extensions 2d-2h | Main journey accepts the updated definition, empty update reports cancellations, limit-bounded results remain feasible, preflight and exhausted-search fixtures disclose no timetable/change report. |
| UC-2 extensions 2i-2k | `ReplanServiceTest` injects internal, interruption, resource-safeguard, and publication failures; `ReplanCliIT.interruptionDoesNotPublish` proves packaged exit 130 and no publication. |
| UC-2 G1-G3 | Approved UC-1 invariants, current schema/revision/assignment collision verification, same-school/direct-lineage checks, and updated-definition hard validation. |
| UC-2 G4-G8 | Common-ID facts, exact 1-hard/3-soft levels, solver period-over-room/preference and room-over-preference choices, and independent lock evidence are verified. |
| UC-2 G9-G11 | Packaged tests compare complete sorted change arrays, full old/new values, allowed teacher overlap, forbidden move overlap, empty cancellations, and direct-successor reuse. |
| UC-2 minimal guarantee | Packaged and injected misuse, transport, safeguard, validation, search, interrupt, and publication failures preserve current/prior destination and omit replacement timetable/change report. |
| RULE-1 through RULE-12, RULE-14 through RULE-17 | Detailed evidence is recorded in `spec/checkpoints/UC-2.md`; the full suite regresses approved UC-1. |

## Blockers

None.

## Deviations

None. Four concurrent fixture files appeared after UC-1 started and are deliberately excluded from this submission:
`src/test/java/org/schoolkernel/fixtures/MerivaljaExampleFixtureTests.java`,
`src/test/resources/fixtures/merivalja-5a-5b.json`,
`src/test/resources/fixtures/merivalja-1a-1b-4a-4avr-4b-9a-9b.json`, and
`src/test/resources/fixtures/README.md`.
