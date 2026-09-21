# Use-Case Checkpoint: UC-1 - Import school data into an empty workspace

## Summary

- Status: READY_FOR_CONVERGENCE
- Base commit: `d0e90317359b08be026c9d8f9a8f37195cbea3f7` (revision after blocked convergence)
- Submission commit: HEAD at convergence
- Relations verified: none; UC-1 is a root use case
- Prior findings addressed: C-1 through C-3 and G-1 through G-10 from `convergence/UC-1.md`

## Contract Evidence

| Contract element | Test or runtime evidence | Result |
|---|---|---|
| Main steps 1-4, definition mode | `WorkspaceImportIT.importsInitialDefinition` (`timetable-workspace/src/test/java/org/schoolkernel/workspace/WorkspaceImportIT.java:109`) submits the real fixture through local HTTP and asserts `INITIAL_DRAFT`, no accepted timetable, exact school ID and definition-supplied `Demo School`, mode, durable JSONB, version, ETag, and one verify process. `WorkspaceBrowserIT.java:96` and the visible Playwright journey reproduce the actor view. | PASS |
| Main steps 1-4, accepted-pair mode | `WorkspaceImportIT.importsAcceptedPair` (`WorkspaceImportIT.java:133`) creates a packaged-kernel result, imports both exact documents over HTTP, and compares the durable accepted definition/result trees. | PASS |
| Main steps 1-4, archive mode | `WorkspaceImportIT.archiveImportAndUnsafeRejection` (`WorkspaceImportIT.java:159`) imports the exact three-entry archive into `ACCEPTED_BASELINE`. | PASS |
| Extension 1a | `SafeImportReaderTest.resultAloneIsRejected` (`SafeImportReaderTest.java:41`) and `WorkspaceImportIT.refusedImportsDoNotMutateOrDiscloseCandidate` (`WorkspaceImportIT.java:238`) assert `MATCHING_DEFINITION_REQUIRED`, no assignment disclosure, exact empty state, and zero process calls. | PASS |
| Extension 1b | `SafeImportReaderTest.completeMalformedArchiveMatrix` (`SafeImportReaderTest.java:81`) and the real HTTP matrix (`WorkspaceImportIT.java:200`) cover absent, duplicate, extra, directory, traversal, separator, encrypted, unreadable/signature, truncation, dishonest-size, and oversize forms without extraction, persistence, or kernel invocation. | PASS |
| Extension 2a | `VerifyCliIT.rejectsCompleteBaselineInvalidityMatrix` (`kernel-cli/src/test/java/org/schoolkernel/cli/VerifyCliIT.java:143`), `BaselineVerifierTest.java:18`, and the real HTTP matrix (`WorkspaceImportIT.java:272`) cover parsing, schema/catalog, required school name, identity, revision, completeness, every hard rule, and lineage without solving or candidate disclosure. | PASS |
| Extension 2b | The packaged and HTTP matrices at `VerifyCliIT.java:143` and `WorkspaceImportIT.java:272` assert definition-only `basedOnRevision` returns structured `INVALID_INPUT` and exact empty state. | PASS |
| Extension 2c | `VerifyCliIT.verifiesAcceptedBaseline` (`VerifyCliIT.java:43`) and `WorkspaceImportIT.importsAcceptedPair` (`WorkspaceImportIT.java:133`) require a complete exact `FEASIBLE` pair; persistence occurs only after `VERIFIED`. | PASS |
| Extension 4a | Statement, deferred-commit, connection-termination, and restart cases at `WorkspaceImportIT.java:511,541,573,607` prove complete rollback or durable restoration with canonical state comparison. | PASS |
| G1 | Definition-only and pair/archive HTTP journeys assert distinct `INITIAL_DRAFT` and `ACCEPTED_BASELINE` modes; `occupiedWorkspaceRefusesImport` (`WorkspaceImportIT.java:309`) prevents relabeling/replacement. | PASS |
| G2 | HTTP/JSONB assertions preserve exact identifiers and metadata; `WorkspaceBrowserIT.java:96` and `output/playwright/uc1-display-name.png` prove `Demo School` is rendered from definition metadata while `demo-school` and the old fallback cue are absent from the heading. | PASS |
| G3 | `ManifestServiceTest.rawPairCreatesPersistentLockManifest` (`ManifestServiceTest.java:18`) asserts imported locks default to `PERSISTENT_POLICY`; manifest mismatch is rejected. | PASS |
| G4 | Unsupported schema/catalog and missing/blank school names are rejected through packaged verify and real HTTP; every integration class uses isolated PostgreSQL 18.6 state. | PASS |
| G5 | HTTP refusals assert exact state and zero process calls; `KernelVerifierTest.interruptionLeavesNoArtifact` (`KernelVerifierTest.java:38`) proves interrupt preservation, child destruction, safe response/logging, and complete private-file cleanup. | PASS |
| Success postcondition | HTTP journeys assert exactly one durable initial draft or exact accepted baseline, version `1`, and corresponding lifecycle state. | PASS |
| Minimal guarantee | All rejection journeys assert `EMPTY`, version `0`, `{}` JSONB, no accepted data, and no candidate assignments. | PASS |

## Rule Evidence

| Rule | Evidence | Result |
|---|---|---|
| RULE-1 | Root/module POMs define resource-only `kernel-contract`, executable `kernel-cli`, and Spring workspace; `ArchitectureTest.workspaceDoesNotDependOnKernelImplementation` (`ArchitectureTest.java:13`) passes; archive inspection found no `.class` in the contract JAR. | PASS |
| RULE-2 | The effective reactor pins Java 25, Spring Boot 4.1.1, PostgreSQL/JDBC 18.6/42.7.13, Testcontainers 2.0.5, Flyway, JdbcClient, MVC, and Security. Clean verification resolves Jackson 3.2.2 and contains none of the excluded stacks. | PASS |
| RULE-3 | `V1__create_workspace_aggregate.sql:1` creates the singleton JSONB row; `WorkspaceRepository.java:23` uses explicit `JdbcClient`; HTTP tests compare relational guard values and complete JSON. | PASS |
| RULE-4 | Flyway alone creates the exact `EMPTY` singleton; `FlywayFailureIT.java:34,50` proves real startup refusal on checksum drift and failed transactional migration, including rollback of the failed schema. | PASS |
| RULE-5 | `WorkspaceState.java:3` contains exactly eight lifecycle values; `WorkspaceImportIT.exhaustiveLifecycleRefusalMatrix` (`WorkspaceImportIT.java:478`) materializes every non-empty source state and proves 409/current ETag, exact state/version/document preservation, and zero process calls. | PASS |
| RULE-6 | `WorkspaceController.java:56`, `ImportService.java:77`, and conditional SQL in `WorkspaceRepository.java:48` bind strong ETag/If-Match to one version. Missing/stale requests and `racingImportsHaveOneWinner` (`WorkspaceImportIT.java:215`) prove one durable winner. | PASS |
| RULE-7 | One `@Transactional` conditional JSONB update stores the bundle; statement, deferred-commit, connection-termination, and fresh-context restart cases at `WorkspaceImportIT.java:511,541,573,607` prove rollback/durability across all named boundaries. | PASS |
| RULE-8 | `WorkspaceImportIT.canonicalWorkspaceBoundaryFidelity` (`WorkspaceImportIT.java:636`) proves Unicode/decomposed text, whitespace/key-order variation, arrays, omitted optional fields, and canonical bytes round-trip through HTTP/JSONB without reconstruction or ID parsing. | PASS |
| RULE-9 | `VerifyCliIT.java:43,80,143` exercises both modes, shared plan/verify/replan fixtures, every named definition/baseline invalidity, no solver/candidate, schemas, bounds, and atomic publication. | PASS |
| RULE-13 | Injectable `KernelProcessLauncher` plus `KernelVerifierTest.java:38,71` prove explicit arguments, owner-only files, 64 KiB channel bounds, interruption/destruction, safe logs, and cleanup for interrupted and verified outcomes. | PASS |
| RULE-14 | `SafeImportReaderTest.java:81,106,114,126` and `WorkspaceImportIT.java:200` cover the complete size/signature/structure/malformed archive matrix in memory and over real HTTP, with no extraction, persistence, or kernel call. | PASS |
| RULE-21 | Loopback Host/Origin/CSRF/CORS/route tests at `WorkspaceImportIT.java:351,375` assert current-state safe denials and zero mutation/process calls; `ArchitectureTest.databaseCredentialsAreExternalOnly` (`ArchitectureTest.java:32`) proves no committed credential defaults. | PASS |
| RULE-22 | Shared `ProblemResponder` serves advice, Host, CSRF, and missing-resource paths; real HTTP checks cover safe 400, 403, 404, 409, 412, 413, 422, 428, and 503 shapes with current state/ETag/correlation and no technical/candidate disclosure. | PASS |
| RULE-23 | Captured verified/interrupted logs at `KernelVerifierTest.java:38,71` contain only correlation ID, command, exit class, and elapsed time and exclude definition names/private filenames despite oversized child output. | PASS |
| RULE-24 | The standard lifecycle includes PostgreSQL 18.6, real HTTP, packaged CLI, and real headless Chrome (`WorkspaceBrowserIT.java:96`); a separate visible Playwright journey produced `output/playwright/uc1-display-name.png` with zero console errors/warnings. | PASS |
| RULE-26 | `KernelArchitectureTest` proves exact sealed subtype exhaustiveness and handler boundaries; `VerifyCliIT.java:80` shares one fixture across plan/verify/replan/verify; `BaselineVerifierTest.java:18` mutates every hard catalog row. | PASS |
| RULE-27 | `KernelCatalogTest.catalogIsExact` (`KernelCatalogTest.java:13`), `KernelMetadataTest.malformedVersionIsRejected` (`KernelMetadataTest.java:11`), architecture checks, and public `SolverManager`/`SolverJob` integration establish one catalog/version source and no `.impl` APIs. | PASS |
| RULE-28 | `JsonSupportLimitsTest`, packaged publisher races, and `FileBoundaryTest.java:84,103` cover byte/depth/string/number boundaries, exactly-one publication, forced replacement, unsupported link capability, directory/same-file/missing-parent collisions, no partial destination, and no temp leak. | PASS |

## Validation

- Focused commands: packaged `verify` integration tests and real HTTP/PostgreSQL import journeys are included in the clean reactor run.
- Full relevant suite: `mvn -q clean verify` — 120 tests, 0 failures, 0 errors, 0 skipped (85 kernel and 35 workspace).
- Working tree impact from tests: none; status after the run contains only intentional UC-1 revision files.
- Runtime evidence: a visible browser imported `examples/initial-school.json` and observed `Initial draft`, `No accepted timetable`, `Demo School`, and revision `sha256:c2b046643fc71e3c8d8a4c274496a47dc5ccddab5d97f2f89d77b47dcea26d14`, with zero console errors/warnings; screenshot `output/playwright/uc1-display-name.png`.
- Changed files: shared kernel verification tests; workspace security/problem/process/archive/import/UI production files; Flyway/process/browser/archive/HTTP/architecture tests; feature decision/status/checkpoint and Jev bundle; `output/playwright/uc1-display-name.png`.
- Approved UCs regression-tested: kernel-v1 UC-1 and UC-2, both independently reconverged and approved; all 85 kernel tests passed.
- Jev preflight: REVIEW — the 33-item bundle validated locally for pinned `jev-1.13.0`, but the external call was denied because sending project-derived excerpts to TypeSafe requires separate disclosure authorization. No semantic result or pass is claimed.

## Notes

- The user-selected contract adds required top-level school `displayName`; the UI shows it and has no school-ID heading fallback.
- Product validation with five qualified administrators and the target-scale reference machine remains the feature-level external dependency stated in the specification; it is not claimed by this implementation checkpoint.

READY FOR CONVERGENCE: UC-1
