# Use-Case Checkpoint: UC-1 - Generate an initial timetable

## Summary

- Status: READY_FOR_CONVERGENCE
- Base commit: `d0e90317359b08be026c9d8f9a8f37195cbea3f7` (required school `displayName` contract revision)
- Submission commit: `HEAD at convergence`
- Relations verified: none; UC-1 has no Requires, Includes, or Extends relation
- Prior convergence: APPROVED; invalidated only by the user-selected required school `displayName` contract revision

## Contract Evidence

| Contract element | Test or runtime evidence | Result |
|---|---|---|
| UC-1 main step 1 | Packaged invocation in `PlanCliIT.mainSuccessScenario` (`src/test/java/org/schoolkernel/cli/PlanCliIT.java:37`) and final launcher journey | PASS |
| UC-1 main step 2 | Exact assignment, `schoolId`, metadata, schema, canonical bytes, and revision recomputation solely from output fields in `PlanCliIT.mainSuccessScenario` (`PlanCliIT.java:37`) | PASS |
| UC-1 main step 3 | Exit 0, empty stdout, stderr-only lifecycle diagnostics in `PlanCliIT.mainSuccessScenario` | PASS |
| UC-1 extension 1a | Both limits, malformed option shapes, positive-limit enforcement, and destination absence in `PlanCliIT.cliMisusePreservesFiles` (`PlanCliIT.java:213`) | PASS |
| UC-1 extension 1b | Equal input/output path exits 64 and preserves input bytes in `PlanCliIT.cliMisusePreservesFiles` | PASS |
| UC-1 extension 1c | Missing input and destination parent in `PlanCliIT.transportFailuresDoNotPublish` (`PlanCliIT.java:251`), overwrite refusal in `overwriteRules`, and size safeguard in `PlanServiceTest.resourceSafeguardPreservesDestination` (`src/test/java/org/schoolkernel/application/PlanServiceTest.java:138`) | PASS |
| UC-1 extension 2a | Malformed, unsupported-version, and semantic failures at packaged boundary in `PlanCliIT.invalidInputs`; each result validates against the result schema and retains all derivable controls/identity metadata. Unknown properties, strict types, ordering, cap, and zero solver calls are covered by `ContractTest`, `DefinitionValidatorTest`, and `PlanServiceTest.invalidInputNeverInvokesSolver` | PASS |
| UC-1 extension 2b | `basedOnRevision` is rejected before solving in `PlanCliIT.invalidInputs` and `DefinitionValidatorTest.collectsSemanticErrors` (`src/test/java/org/schoolkernel/domain/DefinitionValidatorTest.java:20`) | PASS |
| UC-1 extension 2c | No eligible room returns deterministic `hard.room-capacity` evidence without search in `PlanCliIT.obviousNoRoom` (`PlanCliIT.java:166`) | PASS |
| UC-1 extension 2d | Empty input returns complete empty timetable and `EMPTY_PROBLEM`; solver invocation count is zero in `PlanCliIT.emptyProblem` (`PlanCliIT.java:84`) and `PlanServiceTest.emptyInputBypassesSolver` (`PlanServiceTest.java:59`) | PASS |
| UC-1 extension 2e | Genuine one-move budget returns complete best feasible result with `STEP_LIMIT` in `PlanCliIT.reachedStepLimitPublishesBestFeasibleResult` (`PlanCliIT.java:101`) | PASS |
| UC-1 extension 2f | Joint resource conflict exhausts search and returns stable involved IDs, no timetable, no `INFEASIBLE`/`OPTIMAL` claim in `PlanCliIT.exhaustedSearchDoesNotPublishCandidate` (`PlanCliIT.java:185`) | PASS |
| UC-1 extension 2g | Injected late solver failure returns safe `INTERNAL_ERROR`, hides technical detail, publishes no timetable, and retains school/catalog/revision/weights/seed/limit in `PlanServiceTest.unexpectedFailurePublishesSafeInternalError` (`PlanServiceTest.java:74`) | PASS |
| UC-1 extension 2h | SIGINT exits 130 without destination publication in `PlanCliIT.interruptionDoesNotPublish` (`PlanCliIT.java:269`) | PASS |
| UC-1 extension 2i | Injected publication failure exits 74 and preserves destination bytes in `PlanServiceTest.publicationFailurePreservesDestination` (`PlanServiceTest.java:122`) | PASS |
| UC-1 G1 | Draft 2020-12 schema and semantic validation cover typed, case-sensitive identity, references, qualification, series, availability, periods, locks, weights, overflow, and the required nonblank school `displayName`; `kernel-cli/src/test/java/org/schoolkernel/contract/ContractTest.java` exercises missing, blank, and Unicode school names. | PASS |
| UC-1 G2 | Ten hard constraints each have matching and non-matching ConstraintVerifier evidence in `SchoolConstraintProviderTest` (`src/test/java/org/schoolkernel/solver/SchoolConstraintProviderTest.java:26`); independent post-solve check at `src/main/java/org/schoolkernel/application/PlanService.java:200` | PASS |
| UC-1 G3 | Four exact soft catalog rows and semantics are tested at `SchoolConstraintProviderTest.java:112`; result order and weights are asserted at `PlanCliIT.java:36` | PASS |
| UC-1 G4 | Every packaged structured status is checked with the offline result schema; malformed input proves seed/limit retention and late internal failure proves retention of every already-derived field | PASS |
| UC-1 G5 | Incomplete adapter result is refused at `PlanServiceTest.incompleteSolverCandidateIsRefused` (`PlanServiceTest.java:100`); unsuccessful search has diagnostics only at `PlanCliIT.java:185` | PASS |
| UC-1 G6 | Published RFC 8785 number vector, independent SHA-256 digest, normalized definition ordering, school-name revision mutation, timetable scope mutation, exact preserved IDs, and packaged timetable-revision recomputation using only published `schemaVersion`, `schoolId`, `inputRevision`, and assignments | PASS |
| UC-1 G7 | Seed, one-thread mode, default/selected limit, and deterministic move budget at `SolverConfigurationTest` (`src/test/java/org/schoolkernel/solver/SolverConfigurationTest.java:17`) and distribution test (`PlanCliIT.java:287`) | PASS |
| UC-1 G8 | Runtime reports effective controls without claiming cross-machine byte identity; README states the bounded reproducibility contract | PASS |
| UC-1 G9 | Refusal preserves bytes and `--force` atomically replaces in `PlanCliIT.overwriteRules` (`PlanCliIT.java:231`); same-path remains forbidden | PASS |
| UC-1 G10 | Multiple isolated packaged invocations use complete supplied inputs and separate temporary destinations in `PlanCliIT`; no retained-state dependency exists | PASS |
| UC-1 G11 | `TargetScaleBenchmark` runs separately with 1,000 lessons / 100 teachers / 60 cohorts / 100 rooms / 60 periods and reports timing without a threshold | PASS |
| UC-1 success postcondition | Final revision launcher journey published canonical `FEASIBLE` with `schoolId: demo-school`, two complete eligible assignments, score vector, input revision, and independently verifiable timetable revision, making the file consumable as UC-2 lineage input | PASS |
| UC-1 minimal guarantee | Negative boundary tests prove no timetable disclosure; byte checks cover existing destination preservation for misuse, transport, safeguard, overwrite, interruption, and publication failure | PASS |

## Rule Evidence

| Rule | Evidence | Result |
|---|---|---|
| RULE-1 | Reflection and value mapping in `ComponentBoundaryTest` (`src/test/java/org/schoolkernel/solver/ComponentBoundaryTest.java:20`); Timefold types remain under solver package | PASS |
| RULE-2 | `pom.xml` pins every dependency/plugin; dependency-tree command resolved Timefold 2.6.0, Jackson 3.2.2, NetworkNT 3.0.7, Picocli 4.7.7, JCS 1.1; clean Java 25 wrapper build passed | PASS |
| RULE-3 | Hand-authored schemas declare Draft 2020-12 and reject unknown/version/type drift; offline loaders at `DefinitionSchemaValidator.java:25` and `ContractTest.java:55`; strict Jackson flags at `JsonSupport.java:16` | PASS |
| RULE-4 | Schema and all semantic checks precede `solver.solve`; exact cap/order tests and zero-call recording solver at `PlanServiceTest.java:42` | PASS |
| RULE-5 | Explicit scope/order/hash implementation at `RevisionService.java:23`; RFC vector, permutation, exact digest, assignment mutation, and packaged output-only timetable-revision recomputation | PASS |
| RULE-6 | One hard plus three soft levels, one thread, seed and exclusive limits at `SolverAdapter.java:77` and `SolverConfigurationTest.java:17`; raw score absent from result schema | PASS |
| RULE-7 | `SchoolConstraintProvider.defineConstraints` has exactly 10 hard and 4 soft rows (`src/main/java/org/schoolkernel/solver/SchoolConstraintProvider.java:20`); every row has positive and negative ConstraintVerifier evidence | PASS |
| RULE-8 | `HardConstraintDiagnostics` maps only deterministic constraint evidence; packaged exhausted-search test proves absence of candidate assignments and forbidden labels | PASS |
| RULE-9 | Sibling temp, forced close, fsync, and `ATOMIC_MOVE` at `FileBoundary.java:68`; overwrite and injected publication failure preserve bytes | PASS |
| RULE-10 | Picocli parsing and exit mapping at `SchoolKernelMain`; packaged tests cover 0, 2, 3, 64, 74, 130 and unit boundary covers 4; stdout is empty | PASS |
| RULE-11 | Dependency review shows no database, DI framework, network client, or background service; isolated temp-file process tests and repeated invocations write only destinations | PASS |
| RULE-12 | Clean wrapper build produced executable `target/school-kernel.jar`; launcher/JAR normalized-result comparison at `PlanCliIT.java:287` | PASS |
| RULE-13 | Independent `ScheduleEvaluator` invocation at `PlanService.java:200`; incomplete candidate refusal and empty zero-call tests at `PlanServiceTest.java:59` and `:100` | PASS |
| RULE-16 | Default lifecycle: 45 tests, 0 failures/errors/skips across schema, validation, constraints, solver, and 11 packaged-process tests; benchmark profile is separate; no tracked runtime data changed | PASS |
| RULE-17 | SLF4J simple runtime backend emits to stderr; packaged tests prove empty stdout and absence of input display names, while internal-error test proves technical detail suppression | PASS |

## Validation

- Focused commands: `./mvnw -Pbenchmark test` - 1/0/0/0 and 11,821 ms observed; dependency tree - success with exact pinned versions.
- Full relevant suite after the school-name contract revision: `mvn -q clean verify` - 120 run, 0 failed,
  0 errors, 0 skipped (85 kernel and 35 timetable-workspace regression tests).
- Working tree impact from tests: none; only ignored `target/` outputs were generated.
- Runtime evidence: packaged plan/verify/replan journeys accepted named definitions, published exit-0 `FEASIBLE`
  results with revised lineage, and rejected missing/blank names as `INVALID_INPUT` without solver evidence.
- Changed files: exact UC-1 paths are listed in `spec/status.md`; implementation spans pinned build/distribution, CLI/application, strict schemas/DTO/domain, Timefold adapter/catalog, examples/docs, layered tests, and this checkpoint.
- Approved UCs regression-tested: prior UC-2 behavior and the timetable-workspace import boundary both passed in the
  full reactor run; UC-2 remains `NEEDS_REVISION` until its own checkpoint is reconciled.

## Contract Revision Evidence

| Element | Evidence | Result |
|---|---|---|
| Required serialized field | `kernel-contract/src/main/resources/schema/school-definition-v1.schema.json` requires `displayName` and reuses the nonblank display-name definition. | PASS |
| DTO and validated domain fidelity | `SchoolDefinitionDto`, `DefinitionValidator`, and `SchoolDefinition` carry the school name without deriving it from `schoolId`. | PASS |
| Revision identity | `ContractTest.schoolDisplayNameContract` proves a Unicode name is accepted and changing only it changes the definition revision. | PASS |
| Invalid input | `ContractTest.schoolDisplayNameContract` and packaged `VerifyCliIT.rejectsCompleteBaselineInvalidityMatrix` reject missing and blank names without solving. | PASS |
| Existing plan behavior | All initial-definition fixtures now supply a school name; the packaged plan and result lineage regressions remain green. | PASS |

## Notes

This submission preserves the previously approved UC-1 behavior and reconciles it with the selected complete-definition
contract. Timetable-workspace revision files were already dirty at the reconciliation base and are excluded from this
kernel submission. There are no approved deviations.

READY FOR CONVERGENCE: UC-1
