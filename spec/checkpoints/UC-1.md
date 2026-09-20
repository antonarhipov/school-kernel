# Use-Case Checkpoint: UC-1 - Generate an initial timetable

## Summary

- Status: READY_FOR_CONVERGENCE
- Base commit: `69cc268a1b90b960c320d4a82cb1e301d664d898`
- Submission commit: `HEAD at convergence`
- Relations verified: none; UC-1 has no Requires, Includes, or Extends relation

## Contract Evidence

| Contract element | Test or runtime evidence | Result |
|---|---|---|
| UC-1 main step 1 | Packaged invocation in `PlanCliIT.mainSuccessScenario` (`src/test/java/org/schoolkernel/cli/PlanCliIT.java:36`) and final launcher journey | PASS |
| UC-1 main step 2 | Exact assignment, metadata, schema, and canonical bytes in `PlanCliIT.mainSuccessScenario` (`PlanCliIT.java:36`) | PASS |
| UC-1 main step 3 | Exit 0, empty stdout, stderr-only lifecycle diagnostics in `PlanCliIT.mainSuccessScenario` | PASS |
| UC-1 extension 1a | Both limits, malformed option shapes, positive-limit enforcement, and destination absence in `PlanCliIT.cliMisusePreservesFiles` (`PlanCliIT.java:185`) | PASS |
| UC-1 extension 1b | Equal input/output path exits 64 and preserves input bytes in `PlanCliIT.cliMisusePreservesFiles` | PASS |
| UC-1 extension 1c | Missing input and destination parent in `PlanCliIT.transportFailuresDoNotPublish` (`PlanCliIT.java:223`), overwrite refusal in `overwriteRules`, and size safeguard in `PlanServiceTest.resourceSafeguardPreservesDestination` (`src/test/java/org/schoolkernel/application/PlanServiceTest.java:132`) | PASS |
| UC-1 extension 2a | Malformed JSON at packaged boundary in `PlanCliIT.invalidInputs` (`PlanCliIT.java:109`); unknown property, unsupported version, strict types, semantic error ordering, cap, and zero solver calls in `ContractTest`, `DefinitionValidatorTest`, and `PlanServiceTest.invalidInputNeverInvokesSolver` | PASS |
| UC-1 extension 2b | `basedOnRevision` is rejected before solving in `PlanCliIT.invalidInputs` and `DefinitionValidatorTest.collectsSemanticErrors` (`src/test/java/org/schoolkernel/domain/DefinitionValidatorTest.java:20`) | PASS |
| UC-1 extension 2c | No eligible room returns deterministic `hard.room-capacity` evidence without search in `PlanCliIT.obviousNoRoom` (`PlanCliIT.java:138`) | PASS |
| UC-1 extension 2d | Empty input returns complete empty timetable and `EMPTY_PROBLEM`; solver invocation count is zero in `PlanCliIT.emptyProblem` (`PlanCliIT.java:75`) and `PlanServiceTest.emptyInputBypassesSolver` (`PlanServiceTest.java:59`) | PASS |
| UC-1 extension 2e | Genuine one-move budget returns complete best feasible result with `STEP_LIMIT` in `PlanCliIT.reachedStepLimitPublishesBestFeasibleResult` (`PlanCliIT.java:92`) | PASS |
| UC-1 extension 2f | Joint resource conflict exhausts search and returns stable involved IDs, no timetable, no `INFEASIBLE`/`OPTIMAL` claim in `PlanCliIT.exhaustedSearchDoesNotPublishCandidate` (`PlanCliIT.java:157`) | PASS |
| UC-1 extension 2g | Injected unexpected solver failure returns safe `INTERNAL_ERROR`, hides technical detail without debug, and publishes no timetable in `PlanServiceTest.unexpectedFailurePublishesSafeInternalError` (`PlanServiceTest.java:74`) | PASS |
| UC-1 extension 2h | SIGINT exits 130 without destination publication in `PlanCliIT.interruptionDoesNotPublish` (`PlanCliIT.java:241`) | PASS |
| UC-1 extension 2i | Injected publication failure exits 74 and preserves destination bytes in `PlanServiceTest.publicationFailurePreservesDestination` (`PlanServiceTest.java:116`) | PASS |
| UC-1 G1 | Draft 2020-12 schema and semantic validation cover typed, case-sensitive identity, references, qualification, series, availability, periods, locks, weights, and overflow; representative tests at `ContractTest.java:21` and `DefinitionValidatorTest.java:20` | PASS |
| UC-1 G2 | Ten hard constraints each have matching and non-matching ConstraintVerifier evidence in `SchoolConstraintProviderTest` (`src/test/java/org/schoolkernel/solver/SchoolConstraintProviderTest.java:26`); independent post-solve check at `src/main/java/org/schoolkernel/application/PlanService.java:165` | PASS |
| UC-1 G3 | Four exact soft catalog rows and semantics are tested at `SchoolConstraintProviderTest.java:112`; result order and weights are asserted at `PlanCliIT.java:36` | PASS |
| UC-1 G4 | Valid structured outcomes are checked with the offline result schema and stable exit codes across `PlanCliIT` and `PlanServiceTest` | PASS |
| UC-1 G5 | Incomplete adapter result is refused at `PlanServiceTest.incompleteSolverCandidateIsRefused` (`PlanServiceTest.java:94`); unsuccessful search has diagnostics only at `PlanCliIT.java:157` | PASS |
| UC-1 G6 | Published RFC 8785 number vector, independent SHA-256 digest, normalized definition ordering, timetable scope mutation, and exact preserved IDs at `ContractTest.java:64` and `PlanCliIT.java:36` | PASS |
| UC-1 G7 | Seed, one-thread mode, default/selected limit, and deterministic move budget at `SolverConfigurationTest` (`src/test/java/org/schoolkernel/solver/SolverConfigurationTest.java:17`) and distribution test (`PlanCliIT.java:259`) | PASS |
| UC-1 G8 | Runtime reports effective controls without claiming cross-machine byte identity; README states the bounded reproducibility contract | PASS |
| UC-1 G9 | Refusal preserves bytes and `--force` atomically replaces in `PlanCliIT.overwriteRules` (`PlanCliIT.java:203`); same-path remains forbidden | PASS |
| UC-1 G10 | Multiple isolated packaged invocations use complete supplied inputs and separate temporary destinations in `PlanCliIT`; no retained-state dependency exists | PASS |
| UC-1 G11 | `TargetScaleBenchmark` runs separately with 1,000 lessons / 100 teachers / 60 cohorts / 100 rooms / 60 periods and reports timing without a threshold | PASS |
| UC-1 success postcondition | Final launcher journey published canonical `FEASIBLE` with two complete, eligible assignments, score vector, input revision, and timetable revision | PASS |
| UC-1 minimal guarantee | Negative boundary tests prove no timetable disclosure; byte checks cover existing destination preservation for misuse, transport, safeguard, overwrite, interruption, and publication failure | PASS |

## Rule Evidence

| Rule | Evidence | Result |
|---|---|---|
| RULE-1 | Reflection and value mapping in `ComponentBoundaryTest` (`src/test/java/org/schoolkernel/solver/ComponentBoundaryTest.java:20`); Timefold types remain under solver package | PASS |
| RULE-2 | `pom.xml` pins every dependency/plugin; dependency-tree command resolved Timefold 2.6.0, Jackson 3.2.2, NetworkNT 3.0.7, Picocli 4.7.7, JCS 1.1; clean Java 25 wrapper build passed | PASS |
| RULE-3 | Hand-authored schemas declare Draft 2020-12 and reject unknown/version/type drift; offline loaders at `DefinitionSchemaValidator.java:25` and `ContractTest.java:55`; strict Jackson flags at `JsonSupport.java:16` | PASS |
| RULE-4 | Schema and all semantic checks precede `solver.solve`; exact cap/order tests and zero-call recording solver at `PlanServiceTest.java:42` | PASS |
| RULE-5 | Explicit scope/order/hash implementation at `RevisionService.java:23`; RFC vector, permutation, exact digest, and assignment mutation evidence at `ContractTest.java:64` | PASS |
| RULE-6 | One hard plus three soft levels, one thread, seed and exclusive limits at `SolverAdapter.java:77` and `SolverConfigurationTest.java:17`; raw score absent from result schema | PASS |
| RULE-7 | `SchoolConstraintProvider.defineConstraints` has exactly 10 hard and 4 soft rows (`src/main/java/org/schoolkernel/solver/SchoolConstraintProvider.java:20`); every row has positive and negative ConstraintVerifier evidence | PASS |
| RULE-8 | `HardConstraintDiagnostics` maps only deterministic constraint evidence; packaged exhausted-search test proves absence of candidate assignments and forbidden labels | PASS |
| RULE-9 | Sibling temp, forced close, fsync, and `ATOMIC_MOVE` at `FileBoundary.java:68`; overwrite and injected publication failure preserve bytes | PASS |
| RULE-10 | Picocli parsing and exit mapping at `SchoolKernelMain`; packaged tests cover 0, 2, 3, 64, 74, 130 and unit boundary covers 4; stdout is empty | PASS |
| RULE-11 | Dependency review shows no database, DI framework, network client, or background service; isolated temp-file process tests and repeated invocations write only destinations | PASS |
| RULE-12 | Clean wrapper build produced executable `target/school-kernel.jar`; launcher/JAR normalized-result comparison at `PlanCliIT.java:259` | PASS |
| RULE-13 | Independent `ScheduleEvaluator` invocation at `PlanService.java:165`; incomplete candidate refusal and empty zero-call tests at `PlanServiceTest.java:59` and `:94` | PASS |
| RULE-16 | Default lifecycle: 45 tests, 0 failures/errors/skips across schema, validation, constraints, solver, and 11 packaged-process tests; benchmark profile is separate; no tracked runtime data changed | PASS |
| RULE-17 | SLF4J simple runtime backend emits to stderr; packaged tests prove empty stdout and absence of input display names, while internal-error test proves technical detail suppression | PASS |

## Validation

- Focused commands: `./mvnw -Pbenchmark test` - 1/0/0/0 and 11,821 ms observed; dependency tree - success with exact pinned versions.
- Full relevant suite: `./mvnw -q clean verify` - 45 run, 0 failed, 0 errors, 0 skipped.
- Working tree impact from tests: none; only ignored `target/` outputs were generated.
- Runtime evidence: scheduling client invoked repository launcher with `examples/initial-school.json`; exit 0; canonical 1,462-byte `FEASIBLE` result; exact assignments `lesson-math-1 -> mon-1/room-102`, `lesson-science-1 -> mon-2/room-101`; SHA-256 file digest `d679be9b4ce494e487ed918544c5c4a079292a88496f3e4829d4ba5c8bb86578`.
- Changed files: exact UC-1 paths are listed in `spec/status.md`; implementation spans pinned build/distribution, CLI/application, strict schemas/DTO/domain, Timefold adapter/catalog, examples/docs, layered tests, and this checkpoint.
- Approved UCs regression-tested: none; UC-1 is the first use case.

## Notes

The default suite also ran two concurrent fixture tests successfully. Their test source and three resources are not part
of this submission and must remain unstaged. There are no approved deviations.

READY FOR CONVERGENCE: UC-1
