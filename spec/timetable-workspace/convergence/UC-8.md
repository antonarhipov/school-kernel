# Convergence: UC-8 - Export the accepted baseline

## Summary

- Submission: `spec/timetable-workspace/checkpoints/UC-8.md` at `b1e8077`
- Verdict: APPROVE
- Findings: 0 critical; 0 gap; 0 protocol; 0 drift; 0 cosmetic
- Suite: focused HTTP/PostgreSQL - PASS; focused real Chrome - PASS; clean reactor - 170 run, 0 failed, 0 errors, 0 skipped
- Working tree impact from verification: none

## Protocol Gate

- Exactly UC-8 was `READY_FOR_CONVERGENCE`; no other use case was `IN_PROGRESS` or `READY_FOR_CONVERGENCE`.
- The immutable submission is committed at `b1e8077`; its base is `bcce1c443385ac4b231b1eae75f5c30b0c601608`.
- Required UC-1 is recorded `APPROVED` and its production archive-import and packaged-verification path is consumed.
- The checkpoint records every main step, extension, guarantee, both postconditions, the required relationship, all applicable rules, focused and full commands, changed files, and regression coverage.
- The submitted diff is attributable to accepted-baseline export: archive creation, bounded reparse, local verification, the local GET route, accepted-state UI, and boundary tests. It adds no later use-case behavior.

## Runtime Reproduction

| Actor | Step or extension | Executor reported | Converge observed |
|---|---|---|---|
| Administrator over HTTP/PostgreSQL/packaged kernel | Export an accepted baseline and re-import it | PASS | `WorkspaceImportIT.exportsExactAcceptedBaselineAndReimportsIt` returned a ZIP with exactly the three normative entries in order, reparsed it through the production bounded reader, ran packaged `verify`, left the aggregate byte-equivalent, reset the workspace, and re-imported the same accepted definition/result/manifest. |
| Administrator in real Chrome | Inspect accepted identity and request export | PASS | `WorkspaceBrowserIT.exportsAcceptedBaselineInRealBrowser` rendered Demo School, input revision, timetable revision, and the native download control; same-origin `fetch` received `200 application/zip`, attachment disposition, non-zero bytes, and no browser errors. |
| Administrator on refusal/failure paths | Empty state, writer failure, completed-archive integrity failure | PASS | The real HTTP test returned safe `409 ACCEPTED_BASELINE_REQUIRED`, `503 EXPORT_FAILED`, and `422 EXPORT_VERIFICATION_FAILED` responses without a content-disposition header; complete PostgreSQL aggregate snapshots were unchanged. |

## Evidence Ledger

| Contract element | Executor claim | Evidence obtained | Strength | Verified |
|---|---|---|---|---|
| Main step 1 | Administrator requests export | `GET /api/accepted/export` is exercised through real loopback HTTP in `WorkspaceImportIT.java:235` and native same-origin Chrome fetch at `WorkspaceBrowserIT.java:212`. | STRONG | yes |
| Main step 2 | Exact accepted definition/result and school/revisions are identified | The HTTP test compares the stored accepted documents and ETag; Chrome asserts Demo School plus exact input/timetable revisions at `WorkspaceBrowserIT.java:207-223`. | STRONG | yes |
| Main step 3 | One exact normative ZIP is created | HTTP asserts `application/zip`, attachment, no-store, ETag, and exactly `school-definition.json`, `timetable-result.json`, `workspace-manifest.json` in that order at `WorkspaceImportIT.java:239-246`. | STRONG | yes |
| Main step 4 | Completed archive is locally verified for same accepted content/provenance | `AcceptedBaselineExportService.java:60-83` bounded-reparses, calls packaged `KernelVerifier`, strictly validates the manifest, and compares canonical documents and all revisions; the HTTP test proves an `ATTEMPT_SCOPED` lock survives at `WorkspaceImportIT.java:247-255`. | STRONG | yes |
| Main step 5 | Administrator receives archive while baseline stays the same | Real HTTP and Chrome receive a completed ZIP, while before/after aggregate assertions prove no mutation; reset plus production re-import recreates the exact baseline at `WorkspaceImportIT.java:257-263`. | STRONG | yes |
| Extension 1a | No baseline is safely refused | Empty-state real HTTP returns `409 ACCEPTED_BASELINE_REQUIRED`, no attachment, no assignments, and the exact empty aggregate remains at `WorkspaceImportIT.java:269-277`. | STRONG | yes |
| Extension 3a | Creation/publication failure exposes no completed archive | Injected archive-writer failure returns safe `503 EXPORT_FAILED`, has no attachment, and preserves the full accepted aggregate at `WorkspaceImportIT.java:287-294`. The response is constructed only after complete in-memory archive creation/verification. | STRONG | yes |
| Extension 4a | Integrity failure rejects archive safely and preserves state | Injected corrupted completed archive returns safe `422 EXPORT_VERIFICATION_FAILED`, no attachment, and the exact accepted aggregate remains at `WorkspaceImportIT.java:296-304`. | STRONG | yes |
| G1 | Only accepted data is exported | The service reads only `acceptedBaseline.definition`, `.result`, and `.manifest`; ordered-entry assertions prove no draft, proposal, run, or unrelated data entry exists. | STRONG | yes |
| G2 | Documents, versions, revisions, and lock provenance are exact | Canonical document equality, strict manifest validation, packaged baseline verification, and the `ATTEMPT_SCOPED` provenance round trip are asserted through export and production import. | STRONG | yes |
| G3 | No outbound publication/distribution occurs | The endpoint is a local same-origin GET; production dependencies are PostgreSQL, in-memory ZIP, bounded reader, and local packaged verifier only. The Chrome journey explicitly requests the download. | STRONG | yes |
| G4 | Export never mutates workspace state | Complete JSONB aggregate comparisons after success, refusal, writer failure, integrity failure, and browser export prove no state/version change. | STRONG | yes |
| Success postcondition | A verified archive reproduces the accepted baseline through UC-1 | The test resets the singleton and sends the exact response to production `/api/import`; the stored accepted baseline equals the original document by value. | STRONG | yes |
| Minimal guarantee | No refused/failed export is represented complete and accepted state remains | All three negative boundary responses lack `Content-Disposition` and compare the authoritative aggregate before/after. | STRONG | yes |
| Requires UC-1 | Export consumes approved accepted-state and archive-import postconditions | UC-1 is approved; UC-8 imports its source baseline and re-imports the produced archive through UC-1's real parser, manifest, packaged verify, and atomic persistence path. | STRONG | yes |

## Rule Conformance

| Rule | Constraint | Evidence | Result |
|---|---|---|---|
| RULE-1 | Separate reactor modules and process boundary | Clean reactor architecture/dependency checks pass; workspace invokes the packaged verifier without a kernel implementation dependency. | PASS |
| RULE-2 | Pinned Java/Spring/PostgreSQL baseline | Clean Java 25 reactor uses Spring Boot 4.1.1 and disposable PostgreSQL 18.6; no dependency change was introduced. | PASS |
| RULE-3 | One authoritative JSONB aggregate | Export performs one read of the singleton aggregate and no persistence; outcome tests compare the full PostgreSQL JSONB document. | PASS |
| RULE-8 | Canonical fidelity at export boundary | `CanonicalJson.bytes` serializes all three documents and canonical equality is checked after bounded reparse; production re-import equality passes. | PASS |
| RULE-9 | Public structured packed `verify` command | Every successful export calls `KernelVerifier.verify` against the reparsed archive and tests assert one additional verifier invocation. | PASS |
| RULE-13 | Private bounded process files | The existing verifier's private temporary-file, output-bound, cleanup, correlation, and no-debug checks passed in the clean reactor. | PASS |
| RULE-14 | Safe bounded archive parsing | Export verification reuses `SafeImportReader` with compressed/uncompressed/document limits, exact safe entry names, and no extraction; its corpus remains green. | PASS |
| RULE-18 | Exactly three entries and strict manifest | Linked insertion order produces the three specified entries; strict manifest validation and lock-provenance round trip pass. | PASS |
| RULE-21 | Local same-origin security route matrix | Security permits only the listed local GET export route; same-origin Chrome and full Host/Origin/CORS/static-route regressions pass. | PASS |
| RULE-22 | Stable safe API failures | Empty, creation, and integrity branches return standard safe problem codes and no partial attachment or sensitive assignment disclosure. | PASS |
| RULE-23 | Safe observability | Export adds no data logging; packaged verifier's existing structured safe event path is covered by clean regressions. | PASS |
| RULE-24 | PostgreSQL, HTTP, packaged CLI, and browser verification | Independent focused Testcontainers HTTP, packaged verifier, and real Chrome journeys pass; clean reactor is 170/0/0/0. | PASS |
| RULE-26 | Shared typed kernel boundary | Export uses the approved typed baseline-verifier path; shared handler/architecture outcome checks pass. | PASS |
| RULE-27 | Controlled metadata source | Strict manifest/catalog/version checks and kernel catalog/version regressions pass without a second metadata source. | PASS |
| RULE-28 | Bounded kernel input/publication boundary | Reparsing routes all exported documents through the existing bounded verifier input and atomic structured-output path; clean boundary regressions pass. | PASS |

## Related-UC Regression

| Use case | Relationship/shared surface | Evidence | Result |
|---|---|---|---|
| UC-1 | Accepted import, manifest, archive reader, packaged verify | Focused `WorkspaceImportIT` plus clean import/verification/security/fidelity regressions pass. | PASS |
| UC-2 | Async process and acceptance shared surfaces | Clean initial-planning success/failure/cancel/recovery/acceptance suites pass. | PASS |
| UC-3 | Accepted browser presentation | Clean whole-school/keyboard/narrow/scale journeys pass. | PASS |
| UC-4 | Accepted baseline and Chrome runtime | The previously timed-out repair-draft Chrome method was rerun alone and passed; clean rerun also passed it. | PASS |
| UC-5 | Packaged repair process boundary | Clean repair generation/failure/cancel/retry/late-completion suites pass. | PASS |
| UC-6 | Manifest/provenance and accepted-state advancement | Clean review, identity guard, rollback, and acceptance suites pass. | PASS |
| UC-7 | Composite accepted timetable operation | Clean two-repair operational browser and HTTP/process regressions pass. | PASS |
| Kernel UC-1/UC-2 | Shared plan/replan/verify contracts | Kernel unit, architecture, CLI, and corpus suites pass in the clean reactor. | PASS |

## Findings

No critical, gap, protocol, drift, or cosmetic findings. The first clean reactor attempt ended with a transient UC-4 Chrome/CDP timeout after UC-8 had passed; rerunning that exact UC-4 browser method passed, and the following clean reactor run passed 170 tests with zero failures, errors, or skips.

## Walkthrough

Automated evidence is strong. As a UI use case, administrator confirmation is still required:

1. Open a workspace with an accepted timetable and confirm the accepted school name, definition revision, and timetable revision are visible.
2. Select **Download verified accepted bundle** and save the ZIP.
3. Open the ZIP and confirm it contains only `school-definition.json`, `timetable-result.json`, and `workspace-manifest.json` in that order.
4. In a fresh empty workspace, import that ZIP and confirm the same school, definition revision, timetable revision, and accepted timetable reappear.
5. Return to the exporting workspace and confirm it remained on the same accepted baseline throughout.

On 2026-09-22, the administrator completed the walkthrough and replied that it looks like a `PASS`.

## Status Update

`PENDING_WALKTHROUGH -> APPROVED`; every specified UC is now approved.

## Response to execute

APPROVED
