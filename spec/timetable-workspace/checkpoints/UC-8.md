# Use-Case Checkpoint: UC-8 - Export the accepted baseline

## Summary

- Status: READY_FOR_CONVERGENCE
- Base commit: `bcce1c443385ac4b231b1eae75f5c30b0c601608`
- Submission commit: HEAD at convergence
- Relations verified: Requires approved UC-1; the completed archive is re-imported through the production UC-1 archive route to the exact same accepted definition, result, and manifest.

## Contract Evidence

| Contract element | Test or runtime evidence | Result |
|---|---|---|
| Main steps 1-5 | `WorkspaceImportIT.exportsExactAcceptedBaselineAndReimportsIt` requests `GET /api/accepted/export`, asserts the attachment and exact three-entry order, parses it through the production bounded reader, compares all accepted documents, proves packaged verification ran, resets to empty, and re-imports the bytes to the exact baseline. `WorkspaceBrowserIT.exportsAcceptedBaselineInRealBrowser` shows the school and both revisions and receives a non-empty ZIP from the native same-origin action. | PASS |
| Extension 1a | `WorkspaceImportIT.refusedAndFailedExportsPublishNothingAndPreserveState` requests export from `EMPTY`, receives `409 ACCEPTED_BASELINE_REQUIRED`, observes no candidate assignments or attachment header, and compares exact empty state/version. | PASS |
| Extension 3a | The same HTTP/PostgreSQL test injects archive-writer failure, receives safe `503 EXPORT_FAILED` with no attachment, and compares the accepted aggregate exactly unchanged. | PASS |
| Extension 4a | The same test injects a corrupt completed archive, receives safe `422 EXPORT_VERIFICATION_FAILED` with no attachment, and compares the accepted aggregate exactly unchanged. | PASS |
| G1 | The exporter reads only `acceptedBaseline.definition`, `.result`, and `.manifest`; the HTTP test asserts exactly the three normative entry names in order. No draft, proposal, run, or unrelated aggregate field has an archive entry. | PASS |
| G2 | The HTTP journey compares parsed definition, result, manifest, schema/catalog/revision fields, and an `ATTEMPT_SCOPED` period-lock origin with the stored accepted values, then proves exact re-import equality. | PASS |
| G3 | The UI uses one same-origin `/api/accepted/export` link and the service uses only PostgreSQL, in-memory ZIP creation, bounded parsing, and the local packaged verifier. No publication or outbound client exists; download occurs only on the administrator request. | PASS |
| G4 | Success, refusal, archive-creation failure, verification failure, and real-browser receipt compare the complete PostgreSQL document before and after; every comparison is identical and the ETag remains `ws-1` on success. | PASS |
| Success postcondition | The response is one locally verified ZIP; resetting the workspace and submitting that exact response to UC-1 recreates the same accepted definition/result/manifest baseline. | PASS |
| Minimal guarantee | Every refused or injected failure response has no `Content-Disposition`, returns no archive represented as complete, and leaves lifecycle, version, and document unchanged. | PASS |
| Requires UC-1 | UC-1 is approved. The success test consumes its accepted-baseline postcondition and then invokes its production archive import and packaged verification path for the exported bytes. | PASS |

## Rule Evidence

| Rule | Evidence | Result |
|---|---|---|
| RULE-1 | Clean reactor verification preserves separate contract, CLI, and workspace modules; export invokes the packaged verifier and adds no kernel implementation dependency. | PASS |
| RULE-2 | `./mvnw -q clean verify` passes on the pinned Java 25/Spring Boot 4.1.1/PostgreSQL 18.6 stack with no dependency change. | PASS |
| RULE-3 | `AcceptedBaselineExportService` loads the singleton JSONB aggregate once and performs no repository mutation or parallel persistence. PostgreSQL before/after comparisons cover every outcome. | PASS |
| RULE-8 | `AcceptedBaselineExportService` writes all three entries with `CanonicalJson.bytes` and compares canonical bytes after bounded reparse; the re-import assertion compares every JSON value and revision. | PASS |
| RULE-9 | Every successful export runs the existing packaged structured `verify` command in accepted-baseline mode before returning bytes; invocation counts distinguish import, export, and re-import verification. | PASS |
| RULE-13 | Export verification reuses `KernelVerifier`, whose owner-only temporary directory, explicit files, bounded output, correlation ID, no-debug invocation, and cleanup regressions pass in the clean suite. | PASS |
| RULE-14 | The completed archive is reparsed by `SafeImportReader` under the same compressed/uncompressed/entry limits and exact safe-root-entry rules used by UC-1. The existing boundary corpus and new corrupt-output path pass. | PASS |
| RULE-18 | The writer receives a `LinkedHashMap` in normative entry order; HTTP asserts that exact order. Strict manifest validation and exact equality preserve the eight manifest fields and ordered lock provenance. | PASS |
| RULE-21 | Security explicitly permits only local GET export; the existing Host/CORS/route/header matrix and real same-origin Chrome fetch pass. No login, remote route, or mutation is added. | PASS |
| RULE-22 | Refusal, creation failure, and integrity failure return the standard safe problem shape and no uploaded values, paths, candidate assignments, or partial archive attachment. | PASS |
| RULE-23 | The only export subprocess log is the existing safe structured verifier event; no definition, result, display value, assignment, pin, credential, or archive content is logged. | PASS |
| RULE-24 | Disposable PostgreSQL 18.6, real HTTP, packaged verify, exact state assertions, and real Chrome execute in the standard clean Maven lifecycle. | PASS |
| RULE-26 | Export crosses the already approved typed verify handler and shared baseline verifier; architecture and outcome-mapping regressions pass. | PASS |
| RULE-27 | The packaged verifier supplies catalog/kernel metadata from the existing controlled source; exact catalog and version regressions pass without a second metadata source. | PASS |
| RULE-28 | Every exported kernel document is fed through the existing bounded verifier inputs and atomic structured-output path; the clean bounded-input/publication regressions pass. | PASS |

## Validation

- Focused commands: `mvn -q -pl timetable-workspace -am -Dit.test=WorkspaceImportIT -Dfailsafe.failIfNoSpecifiedTests=false verify` - PASS; focused `WorkspaceBrowserIT#exportsAcceptedBaselineInRealBrowser` - PASS; focused lock-provenance export/re-import method - PASS.
- Full relevant suite: escalated `./mvnw -q clean verify` - 170 tests, 0 failures, 0 errors, 0 skipped. The initial sandboxed attempt could not open the Docker socket; the unchanged approved rerun is authoritative and green.
- Working tree impact from tests: none; `git diff --check` passes.
- Runtime evidence: real Chrome renders the accepted school, input revision, timetable revision, and download action, then receives `200 application/zip` with an attachment filename and non-zero bytes over the loopback server; browser errors are empty.
- Changed files: `AcceptedBaselineExportService`, `AcceptedBundleArchiver`, `ZipAcceptedBundleArchiver`, `SafeImportReader`, controller/security route, native UI/messages/styles, HTTP/browser tests, status, and checkpoint.
- Approved UCs regression-tested: timetable-workspace UC-1 through UC-7 and kernel UC-1/UC-2 all pass in the 170-test clean reactor.

## Notes

- Export is intentionally available only in stable `ACCEPTED_BASELINE`, matching the declared lifecycle transition. Drafts and proposals remain ineligible and cannot be mislabeled or included as current.
- The first clean reactor attempt failed only because the sandbox denied the Docker socket. The approved rerun used the same command and completed green.
- The required administrator walkthrough and independent evidence audit remain convergence responsibilities; this checkpoint claims automated technical readiness only.

READY FOR CONVERGENCE: UC-8
