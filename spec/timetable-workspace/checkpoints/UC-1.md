# Use-Case Checkpoint: UC-1 - Import school data into an empty workspace

## Summary

- Status: READY_FOR_CONVERGENCE
- Base commit: `db18108c24ddc14b9a1496f0a258f47d3d06c870`
- Submission commit: HEAD at convergence
- Relations verified: none; UC-1 is a root use case

## Contract Evidence

| Contract element | Test or runtime evidence | Result |
|---|---|---|
| Main steps 1-4, definition mode | `WorkspaceImportIT.importsInitialDefinition` (`timetable-workspace/src/test/java/org/schoolkernel/workspace/WorkspaceImportIT.java:93`) submits the real fixture through local HTTP, asserts `INITIAL_DRAFT`, `No accepted timetable`, exact school ID, mode, durable JSONB, version, and ETag. | PASS |
| Main steps 1-4, accepted-pair mode | `WorkspaceImportIT.importsAcceptedPair` (`WorkspaceImportIT.java:115`) creates a packaged-kernel result, imports both exact documents over HTTP, and compares the durable accepted definition/result trees. | PASS |
| Main steps 1-4, archive mode | `WorkspaceImportIT.archiveImportAndUnsafeRejection` (`WorkspaceImportIT.java:141`) imports the exact three-entry archive into `ACCEPTED_BASELINE`. | PASS |
| Extension 1a | `SafeImportReaderTest.resultAloneIsRejected` (`SafeImportReaderTest.java:31`) and `WorkspaceImportIT.refusedImportsDoNotMutateOrDiscloseCandidate` (`WorkspaceImportIT.java:182`) assert `MATCHING_DEFINITION_REQUIRED`, no assignment disclosure, and the exact empty row. | PASS |
| Extension 1b | `SafeImportReaderTest.exactArchive` and `unsafeArchive` (`SafeImportReaderTest.java:41,56`) plus the real HTTP archive journey reject extra/traversal content without extraction or mutation. | PASS |
| Extension 2a | `VerifyCliIT.malformedAndSolveControlsAreRejected` (`kernel-cli/src/test/java/org/schoolkernel/cli/VerifyCliIT.java:111`), the contract/semantic suites, `BaselineVerifier`, and the refused-import HTTP assertions cover parsing, strict schema/catalog, identity, revision, assignment, hard-feasibility, and lineage rejection without solver use or candidate disclosure. | PASS |
| Extension 2b | `VerifyCliIT.rejectsSuccessorOnlyAndMismatchedPair` (`VerifyCliIT.java:78`) asserts definition-only `basedOnRevision` returns structured `INVALID_INPUT`. | PASS |
| Extension 2c | `VerifyCliIT.verifiesAcceptedBaseline` and `rejectsSuccessorOnlyAndMismatchedPair` (`VerifyCliIT.java:56,78`) require a complete exact `FEASIBLE` pair; workspace persistence occurs only after `VERIFIED`. | PASS |
| Extension 4a | `WorkspaceImportIT.storageFailureRollsBack` (`WorkspaceImportIT.java:333`) injects a PostgreSQL write failure while importing an accepted pair and asserts the exact empty row remains. | PASS |
| G1 | Definition-only and pair/archive HTTP journeys assert distinct `INITIAL_DRAFT` and `ACCEPTED_BASELINE` modes; `occupiedWorkspaceRefusesImport` (`WorkspaceImportIT.java:309`) prevents relabeling/replacement. | PASS |
| G2 | Both success journeys compare stored definitions/results by JSON value; `app.js` uses supplied display metadata and otherwise shows the stable ID with an explicit unavailable-name cue rather than interpreting it. | PASS |
| G3 | `ManifestServiceTest.rawPairCreatesPersistentLockManifest` (`ManifestServiceTest.java:18`) asserts imported locks default to `PERSISTENT_POLICY`; manifest mismatch is rejected. | PASS |
| G4 | Packaged verification rejects unsupported schema/catalog values; `WorkspaceImportIT` runs from a fresh isolated PostgreSQL 18.6 container and exact empty row. | PASS |
| G5 | Refused, stale, hostile-origin, missing-CSRF, missing-Origin, wrong-port, unsafe-archive, occupied, and storage-failure requests compare the row before/after and disclose no candidate. The listener is loopback-bound. | PASS |
| Success postcondition | HTTP journeys assert exactly one durable initial draft or exact accepted baseline, version `1`, and corresponding lifecycle state. | PASS |
| Minimal guarantee | All rejection journeys assert `EMPTY`, version `0`, `{}` JSONB, no accepted data, and no candidate assignments. | PASS |

## Rule Evidence

| Rule | Evidence | Result |
|---|---|---|
| RULE-1 | Root/module POMs define resource-only `kernel-contract`, executable `kernel-cli`, and Spring workspace; `ArchitectureTest.workspaceDoesNotDependOnKernelImplementation` (`ArchitectureTest.java:13`) passes; archive inspection found no `.class` in the contract JAR. | PASS |
| RULE-2 | The effective reactor pins Java 25, Spring Boot 4.1.1, PostgreSQL/JDBC 18.6/42.7.13, Testcontainers 2.0.5, Flyway, JdbcClient, MVC, and Security. Clean verification resolves Jackson 3.2.2 and contains none of the excluded stacks. | PASS |
| RULE-3 | `V1__create_workspace_aggregate.sql:1` creates the singleton JSONB row; `WorkspaceRepository.java:23` uses explicit `JdbcClient`; HTTP tests compare relational guard values and complete JSON. | PASS |
| RULE-4 | Flyway alone creates and seeds the exact `EMPTY` singleton (`V1__create_workspace_aggregate.sql:1-13`); fresh PostgreSQL startup validates and applies V1 with no school data. | PASS |
| RULE-5 | `WorkspaceState.java:3` contains exactly eight lifecycle values; `WorkspaceMutation.java:17` conditionally permits only `EMPTY` to the verified import state; occupied import returns `INVALID_WORKSPACE_TRANSITION` without mutation. | PASS |
| RULE-6 | `WorkspaceController.java:56`, `ImportService.java:77`, and conditional SQL in `WorkspaceRepository.java:48` bind strong ETag/If-Match to one version. Missing/stale requests and `racingImportsHaveOneWinner` (`WorkspaceImportIT.java:215`) prove one durable winner. | PASS |
| RULE-7 | One `@Transactional` conditional JSONB update stores the complete accepted bundle and advances lifecycle; accepted-pair injected storage failure rolls back definition, result, manifest, state, and version. | PASS |
| RULE-8 | `CanonicalJson.java:12`, kernel revision vectors, exact JSONB comparisons, strict IDs, and explicit missing-name presentation preserve documents without display-model reconstruction or ID parsing. | PASS |
| RULE-9 | `VerifyCliIT` (`VerifyCliIT.java:33-190`) exercises both modes, structured schemas/status/exit codes, invalid inputs, no solve controls, no candidate disclosure, bounds, and packaged atomic publication. | PASS |
| RULE-13 | `KernelVerifier.java:41-100,152-181,202-216` creates a private per-call directory, uses explicit distinct files/correlation ID, caps both streams at 64 KiB, never uses debug, and cleans every outcome. | PASS |
| RULE-14 | `SafeImportReader.java:27-107` applies document, manifest, compressed, total-uncompressed, count, duplicate, root-name, directory, traversal/separator, and no-extraction checks; unit and real HTTP unsafe/oversize tests assert zero persistence. | PASS |
| RULE-21 | `application.yml:1-3`, `SecurityConfiguration.java:22-42`, and `HostOriginFilter.java:19-38` bind loopback, reject unconfigured Host/Origin/CORS/forwarded trust, require CSRF, and deny the unlisted route surface. Real HTTP tests cover these refusals. | PASS |
| RULE-22 | `ProblemHandler.java:22-57` emits the stable safe problem shape/status/ETag; real HTTP checks cover 403, 409, 412, 422, 428, and 503 without paths, SQL, stack traces, raw stderr, or candidates. | PASS |
| RULE-23 | `KernelVerifier.java:79-80` logs only correlation ID, command kind, exit class, and elapsed time; retained workspace evidence contains no secret/process diagnostics. | PASS |
| RULE-24 | `WorkspaceImportIT` uses an ephemeral Spring server, PostgreSQL 18.6 Testcontainers, Flyway, the packaged launcher, real HTTP, and packaged static assets. The browser success journey imported the real fixture with zero console errors/warnings. | PASS |
| RULE-26 | `KernelArchitectureTest` (`KernelArchitectureTest.java:28-66`) proves CLI-only adapter construction, sealed exhaustive outcomes, no `catch(Throwable)`, and distinct handlers; `DefinitionLoader` and `BaselineVerifier` are shared by plan/replan/verify. | PASS |
| RULE-27 | `KernelCatalogTest.catalogIsExact` (`KernelCatalogTest.java:13`), `KernelMetadataTest.malformedVersionIsRejected` (`KernelMetadataTest.java:11`), architecture checks, and public `SolverManager`/`SolverJob` integration establish one catalog/version source and no `.impl` APIs. | PASS |
| RULE-28 | `FileBoundaryTest`, `JsonSupportLimitsTest`, and packaged `VerifyCliIT` test byte/depth/string/number boundaries, exactly-one concurrent unforced publisher, forced atomic replacement, exit `74`, and destination preservation. | PASS |

## Validation

- Focused commands: packaged `verify` integration tests and real HTTP/PostgreSQL import journeys are included in the clean reactor run.
- Full relevant suite: `mvn -q clean verify` — 96 tests, 0 failures, 0 errors, 0 skipped.
- Working tree impact from tests: none; status after the run contains only the intentional UC-1 reactor/workspace changes and the browser screenshot artifact.
- Runtime evidence: browser import of `examples/initial-school.json` observed `Initial draft`, `No accepted timetable`, and `School name unavailable; showing the stable school ID.` with zero console errors or warnings; screenshot `output/playwright/uc1-initial-draft.png`.
- Changed files: root reactor/launcher/readme/ignore files; kernel sources, schemas, fixtures, and tests relocated to `kernel-cli/` and `kernel-contract/`; new `timetable-workspace/` application, migration, native assets, and tests; this feature status/checkpoint and validated Jev bundle.
- Approved UCs regression-tested: none; UC-1 is the first use case.
- Jev preflight: REVIEW — the 33-item bundle validated locally for pinned `jev-1.13.0`, but the external call was denied because sending project-derived excerpts to TypeSafe requires separate disclosure authorization. No semantic result or pass is claimed.

## Notes

- The v1 definition has no top-level school display-name field. The UI preserves and shows the stable school ID and explicitly states that the school name is unavailable; it never guesses meaning from the ID.
- Product validation with five qualified administrators and the target-scale reference machine remains the feature-level external dependency stated in the specification; it is not claimed by this implementation checkpoint.

READY FOR CONVERGENCE: UC-1
