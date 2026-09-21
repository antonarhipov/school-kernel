# Convergence: UC-1 - Import school data into an empty workspace

## Summary

- Submission: `spec/timetable-workspace/checkpoints/UC-1.md` at `854493f6f1b17f1d9c4437c4f816a89100d0ccbe`
- Verdict: PENDING WALKTHROUGH
- Findings: 0 critical, 0 gaps, 0 protocol, 0 drift, 0 cosmetic
- Suite: `mvn -q clean verify` — 120 run, 0 failed, 0 errors, 0 skipped
- Working tree impact from verification: none

## Protocol Gate

1. PASS — exactly UC-1 is `READY_FOR_CONVERGENCE`.
2. PASS — the revised checkpoint, implementation, tests, Jev bundle, and browser artifact are committed together at `854493f` from revision base `d0e9031`.
3. PASS — UC-1 has no `Requires`, `Includes`, or `Extends` dependency.
4. PASS — every other use case is `NOT_STARTED`.
5. PASS — the checkpoint has rows for every scenario, extension, guarantee, postcondition, relationship, applicable rule, command, changed file, prior finding, and regression. The 33-item Jev bundle validates locally and is correctly recorded as `REVIEW`, not pass evidence.
6. PASS — the shared kernel tests and workspace changes are attributable to resolving C-1 through C-3 and G-1 through G-10; no later workspace use-case route or actor behavior is implemented.

## Runtime Reproduction

| Actor | Step or extension | Executor reported | Converge observed |
|---|---|---|---|
| Administrator | Open empty local workspace | Empty workspace and import choices | Visible Chromium at `http://127.0.0.1:52464/workspace/` showed `Empty workspace`, `No accepted timetable`, JSON document inputs, and accepted-bundle input. |
| Administrator | Main step 1, select initial definition | Real fixture selected without command-line work by the actor | Playwright selected `examples/initial-school.json` through the file control and submitted `Import JSON documents`. |
| Workspace | Main steps 2 and 4, verify and persist | Packaged verify and durable `INITIAL_DRAFT` | The packaged application logged `kernelCommand=verify exitClass=VERIFIED`; the rendered page changed to `Initial draft` with definition revision `sha256:c2b046643fc71e3c8d8a4c274496a47dc5ccddab5d97f2f89d77b47dcea26d14`. |
| Workspace | Main step 3, identify school and state | Definition-supplied display name | Browser showed heading `Demo School`; `demo-school` and the old unavailable-name fallback were absent. |
| Administrator | Main accepted-pair/archive alternatives | Real HTTP journeys | Independent `WorkspaceImportIT` used packaged verify and asserted exact accepted definition/result/manifest persistence for pair and archive modes. |
| Administrator | Extensions and failures | Complete real HTTP/package matrices | Independent suites covered result-only, all malformed/unsafe archives, every definition/baseline invalidity, missing/stale preconditions, lifecycle refusals, local security, interruption, and statement/commit/connection storage failures with exact no-side-effect assertions. |
| Browser | Console | Zero errors and warnings | Visible Playwright reported 0 messages, 0 errors, and 0 warnings; standard verification independently ran the Chrome journey. |

The temporary headed browser, packaged workspace process, and loopback PostgreSQL 18.6 container were stopped after reproduction.

## Evidence Ledger

| Contract element | Executor claim | Evidence obtained | Strength | Verified |
|---|---|---|---|---|
| Main step 1 | Both explicit choices are available | Rendered empty page plus exact definition/pair/archive HTTP journeys at `WorkspaceImportIT.java:109-237` | STRONG | yes |
| Main step 2 | Complete import validation | Packaged `VerifyCliIT.java:143`, direct `BaselineVerifierTest.java:18`, and HTTP `WorkspaceImportIT.java:272` cover every named definition/baseline invalidity before persistence | STRONG | yes |
| Main step 3 | School and outcome are shown | Required definition `displayName` is revision-bearing; real Chrome and visible Playwright render `Demo School` and exact state/revision without the school-ID fallback | STRONG | yes |
| Main step 4 | Verified state is durable and opened | Real PostgreSQL HTTP tests compare lifecycle/version/full JSON; browser reopened the resulting state | STRONG | yes |
| Extension 1a | Result alone requires definition and changes nothing | `WorkspaceImportIT.java:238` asserts 422, stable code, no assignments, exact empty row, and zero process calls | STRONG | yes |
| Extension 1b | Every absent/duplicate/extra/unreadable/unsafe archive is rejected | Unit matrix `SafeImportReaderTest.java:81-138` and HTTP matrix `WorkspaceImportIT.java:200-237` cover every named family, size edge, signature, and no-extraction/no-process/no-persistence obligation | STRONG | yes |
| Extension 2a | Every named validation failure is safe and mutation-free | Packaged and HTTP matrices plus direct ten-hard-rule mutations return structured invalidity, no solver/candidate, exact empty state | STRONG | yes |
| Extension 2b | Definition-only successor is rejected without mutation | Packaged and HTTP matrices reject `basedOnRevision` definition-only input and assert exact empty state | STRONG | yes |
| Extension 2c | Only exact complete FEASIBLE pair is accepted | Packaged positive/mismatch tests and accepted-pair HTTP persistence compare exact values | STRONG | yes |
| Extension 4a | Storage failure leaves no accepted state | Statement, deferred-commit, and connection-termination failures return safe 503 and preserve canonical empty state; restart proves successful durability | STRONG | yes |
| G1 | Import modes are explicit and never mislabeled | HTTP snapshots assert `INITIAL_DRAFT`/false and `ACCEPTED_BASELINE`/true | STRONG | yes |
| G2 | IDs are exact and user-facing names come from metadata | Full JSON/Unicode fidelity is exact; browser assertions require `Demo School` and forbid `demo-school`/fallback heading text | STRONG | yes |
| G3 | Unmanifested locks are persistent policy | Production `ManifestService` path and exact origin assertions at `ManifestServiceTest.java:18-33` | STRONG | yes |
| G4 | Validation is isolated and rejects unsupported versions | Disposable PostgreSQL plus real HTTP unsupported schema/catalog/name cases preserve exact empty state | STRONG | yes |
| G5 | Rejected or interrupted imports leave no partial state or remote surface | HTTP/security matrices and injectable interrupted child process prove exact state, zero prohibited invocation, child destruction, interrupt preservation, safe log, and private cleanup | STRONG | yes |
| Success postcondition | One durable initial draft or exact accepted baseline | Both real HTTP modes compare singleton lifecycle/version/document by value | STRONG | yes |
| Minimal guarantee | Every failed import stays empty and discloses no candidate | Complete archive, verification, lifecycle, security, interruption, and storage matrices assert exact prior state and candidate/collaborator absence | STRONG | yes |
| Relationships | UC-1 is a dependency-free root | Spec relation block at `spec.md:184-186`; no earlier UC is consumed | STRONG | yes |

## Rule Conformance

| Rule | Constraint | Evidence | Result |
|---|---|---|---|
| RULE-1 | Separate resource-only contract, CLI, and workspace modules; no workspace kernel implementation dependency | Reactor build, contract JAR scan with no `.class`, and `ArchitectureTest.java:13-23` | PASS |
| RULE-2 | Pinned Java 25/Spring Boot 4.1.1/PostgreSQL stack and excluded stacks absent | Clean build, dependency tree, POM inspection, PostgreSQL 18.6 runtime, and no `.impl` source | PASS |
| RULE-3 | One JdbcClient-managed singleton JSONB aggregate | Migration, repository SQL, and exact PostgreSQL row/document assertions | PASS |
| RULE-4 | Flyway-only schema plus refusal on checksum drift/failed migration | `FlywayFailureIT.java:34,50` proves real PostgreSQL startup refusal and failed-transaction rollback | PASS |
| RULE-5 | Exact lifecycle and exhaustive refused transition/action matrix | `WorkspaceImportIT.java:478` imports from every non-empty lifecycle and proves 409/current ETag, exact state/version/document, and zero process calls | PASS |
| RULE-6 | Strong ETag/If-Match and conditional SQL with one race winner | Missing/stale/racing HTTP tests plus source-order inspection prove checks precede kernel invocation and SQL uses the same version | PASS |
| RULE-7 | Accepted import is one rollback-safe transaction across write/commit failure boundaries | Statement, deferred-commit, connection-termination, restart, and canonical comparisons at `WorkspaceImportIT.java:511-634` prove complete rollback/durability | PASS |
| RULE-8 | Lossless canonical JSON and exact normalized revisions | `WorkspaceImportIT.java:636` proves Unicode, decomposed text, ordering/whitespace, arrays, omitted fields, and canonical-byte fidelity through HTTP/JSONB | PASS |
| RULE-9 | Structured non-solving `verify` covers both modes and every UC-1 invalidity | `VerifyCliIT.java:43,80,143` covers both modes, shared fixture traversal, and the complete invalidity matrix with no solver/candidate | PASS |
| RULE-13 | Private bounded process files/streams and cleanup on every outcome | `KernelVerifierTest.java:38,71` proves explicit arguments, owner permissions, 64 KiB channels, interruption/destruction, safe logs, and complete cleanup | PASS |
| RULE-14 | Exact bounded no-extraction archive parser and full boundary/malformed matrix | `SafeImportReaderTest.java:81-138` plus HTTP `WorkspaceImportIT.java:200` cover exact/over limits and every malformed/unsafe family without extraction or side effects | PASS |
| RULE-21 | Same-origin loopback surface and no committed DB credentials | Host/Origin/CSRF/CORS/route tests include current state and zero-process assertions; `ArchitectureTest.java:32` proves external-only credentials | PASS |
| RULE-22 | Stable safe problem shape with current state/ETag and complete status mappings | Shared `ProblemResponder` covers advice/Host/CSRF/resource paths; real HTTP asserts safe 400/403/404/409/412/413/422/428/503 with no disclosure | PASS |
| RULE-23 | Structured safe logs and captured success/failure evidence | Captured verified/interrupted logs contain only correlation/command/class/time and exclude names/private paths despite oversized channels | PASS |
| RULE-24 | Real PostgreSQL HTTP/browser journey in standard verification and negative side-effect proofs | Standard verification runs PostgreSQL 18.6, packaged CLI, real HTTP, and real headless Chrome; negative matrices instrument zero process calls; visible Playwright also passed | PASS |
| RULE-26 | Typed shared handlers, exhaustive mappings, shared fixtures, independent hard-verifier mutation corpus | Exact sealed subtype architecture, shared plan/verify/replan/verify fixture, and ten-hard-row direct mutation tests pass | PASS |
| RULE-27 | Single catalog/version source and public solver APIs | Catalog/manifest tests, packaged version assertion, source/bytecode architecture, and public `SolverManager`/`SolverJob` implementation pass | PASS |
| RULE-28 | Bounded inputs and race-safe atomic publication with capability/failure/crash proof | Numeric bounds and packaged races plus `FileBoundaryTest.java:84,103` cover unsupported link capability and directory/same-file/missing-parent failures with destination/temp preservation | PASS |

## Related-UC Regression

| Use case | Relationship/shared surface | Evidence | Result |
|---|---|---|---|
| None | UC-1 is the first workspace use case; no related UC is approved | Status ledger and relationship graph | N/A |
| Kernel v1 UC-1 and UC-2 | Shared contract, packaged commands, verifier, and publication boundary | Both were reconverged and approved; all 85 kernel tests passed inside the 120-test reactor lifecycle | PASS |

## Resolved Prior Findings

The C-1 through C-3 and G-1 through G-10 entries below are retained as the historical findings against submission
`2671b2a`. Submission `854493f` closes every one with the strong evidence in the current ledger; there are no current
critical, gap, or protocol findings.

### C-1 RESOLVED — Required school display name

- Reference: UC-1 main step 3 requires, “The workspace shows the school by definition-supplied display name”; G2 requires user-facing names to come from definition metadata.
- Evidence: strict root schema `kernel-contract/src/main/resources/schema/school-definition-v1.schema.json:6-25` has `additionalProperties: false` and no school display-name property. `ImportService.java:53-58` looks for that forbidden property and otherwise copies `schoolId` into `displayName`. Independent browser reproduction rendered `demo-school` as the heading plus an unavailable-name cue.
- Why it fails: no valid input can drive the specified display-name branch. The implementation cannot satisfy the main scenario without either changing the kernel contract or changing the workspace specification.
- Revision outcome: product decision required — add an explicit school display-name field to the normative kernel definition and propagation/revision rules, or revise UC-1 to specify the stable-ID unavailable-name fallback.

### C-2 RESOLVED — External-only database credentials

- Reference: RULE-21 says database credentials “MUST come from local environment or configuration and MUST NOT be committed”.
- Evidence: `timetable-workspace/src/main/resources/application.yml:15-16` commits default username and password values `school_workspace`.
- Why it fails: a missing environment variable silently selects committed credentials, contrary to the security constraint.
- Revision outcome: require externally supplied local credentials without committed credential defaults, and add configuration/secret-scan tests.

### C-3 RESOLVED — Current lifecycle state in security denials

- Reference: RULE-22 requires one problem shape “containing stable code, localized safe message, correlation ID, current lifecycle state, current ETag when disclosure is safe”.
- Evidence: Host/Origin denial writes `state:"UNKNOWN",etag:null` at `HostOriginFilter.java:48-49`; CSRF/security denial does the same at `SecurityConfiguration.java:55-56`, even while the database is available. Tests assert only status/absence of mutation.
- Why it fails: common 403 responses do not provide the current lifecycle recovery context required by the API contract.
- Revision outcome: route these denials through a safe problem-response component that supplies current state and ETag when safe, and assert the complete body/headers.

### G-1 RESOLVED — Complete import invalidity boundary evidence

- References: UC-1 extensions 1b, 2a, 2b; G4; RULE-9 and RULE-14 verification requirements.
- Evidence gap: missing/duplicate/unreadable/encrypted/truncated/bomb archive forms and every named identity/revision/assignment/hard-validity/version failure are not each driven through packaged `verify` and real HTTP with response, no candidate, no mutation, and no process-call assertions.
- Revision outcome: add the complete deterministic packaged/HTTP matrix, including exact archive byte boundaries and definition-only successor HTTP refusal.

### G-2 RESOLVED — Interrupted verification safety

- Reference: UC-1 G5 includes interrupted imports; RULE-13 requires cleanup on every outcome.
- Evidence gap: `KernelVerifier` has interruption code, but no controllable process test reproduces interruption, asserts no state/candidate, and snapshots cleanup.
- Revision outcome: inject a controllable child process and prove interruption, safe response, exact empty row, bounded channels, and temporary-directory cleanup.

### G-3 RESOLVED — Flyway failure refusal

- Reference: RULE-4 requires startup refusal for migration or validation error and its verification requires checksum-drift/failed-migration cases.
- Evidence gap: only a fresh successful V1 migration is run.
- Revision outcome: add PostgreSQL startup tests for checksum drift and a failed transactional migration without weakening `validate-on-migrate`.

### G-4 RESOLVED — Exhaustive lifecycle refusal matrix

- Reference: RULE-5 requires every unlisted transition to return 409 without any prohibited side effect and exhaustive state/action coverage.
- Evidence gap: import refusal is tested only after an initial draft; accepted/solving/proposal/repair states and process/export side effects are not covered.
- Revision outcome: establish every real source state in PostgreSQL and run the UC-1 import action against each with complete before/after and collaborator assertions.

### G-5 RESOLVED — Accepted-transaction fault matrix

- Reference: RULE-7 requires rollback at any statement or commit boundary; verification names connection termination, restart, exact JSON, and canonical export bytes.
- Evidence gap: one `BEFORE UPDATE` trigger failure is tested.
- Revision outcome: add commit/connection/restart fault cases and compare full accepted state/canonical bytes before and after.

### G-6 RESOLVED — Workspace-boundary canonical fidelity

- Reference: RULE-8 requires lossless JSONB/canonical boundary behavior including Unicode and omitted/materialized distinctions.
- Evidence gap: ordinary full-tree comparisons and kernel vectors exist, but workspace JSONB permutations, Unicode, and canonical process/export cycle proof do not.
- Revision outcome: add value and canonical-byte round trips for key order, whitespace, Unicode, arrays/sets, and omitted defaults at the workspace boundary.

### G-7 RESOLVED — Private process and archive resource safety

- References: RULE-13 and RULE-14 verification requirements.
- Evidence gap: owner permissions, 64 KiB overflow, path collision, every-outcome cleanup, and the complete archive boundary/malformed matrix are not automated.
- Revision outcome: add filesystem/process doubles and the exact no-extraction ZIP corpus with post-run directory/database snapshots.

### G-8 RESOLVED — Failure-shape and logging matrices

- References: RULE-22 and RULE-23 verification requirements.
- Evidence gap: not every status mapping/failure class is scanned across response, headers, logs, state, process invocation, and prohibited details.
- Revision outcome: add production-mode parameterized HTTP/log capture covering all declared mappings and disclosure prohibitions.

### G-9 RESOLVED — Standard-lifecycle browser and collaborator proof

- Reference: RULE-24 requires a real browser journey per UC in standard verification and negative assertions for process/export side effects.
- Evidence gap: browser reproduction is manual; Maven has no browser automation, and several negatives infer rather than assert zero kernel/export invocation.
- Revision outcome: add repeatable browser automation to verification and instrument real-boundary negatives for collaborator absence.

### G-10 RESOLVED — Kernel architecture and failure corpus

- References: RULE-26 and RULE-28 verification requirements.
- Evidence gap: no exhaustive outcome-mapper subtype test, identical shared-fixture pass through every command, every-hard-row direct-verifier mutation corpus, or complete capability/serialization/close/link/move/crash publication matrix.
- Revision outcome: implement the named architecture, shared-fixture, mutation, and filesystem fault-injection suites.

## Walkthrough

Automated evidence passes. Approval now requires the administrator to confirm this UC-derived walkthrough:

1. Open an empty local workspace and confirm it offers JSON-document and accepted-bundle import modes and says
   `No accepted timetable`.
2. Choose `examples/initial-school.json` as the school definition and select `Import JSON documents`.
3. Confirm the workspace opens `Initial draft`, shows `Demo School` as the school heading, says the definition is
   verified and durably stored awaiting initial planning, still says `No accepted timetable`, and shows definition
   revision `sha256:c2b046643fc71e3c8d8a4c274496a47dc5ccddab5d97f2f89d77b47dcea26d14`.
4. In a fresh empty workspace, try a result without its matching definition and confirm the workspace explains that
   the definition is required and remains empty.
5. In fresh empty workspaces, import a matching definition/result pair and then the exact accepted-bundle archive;
   confirm each opens an accepted baseline rather than an initial draft.

User result: pending.

## Status Update

`READY_FOR_CONVERGENCE` -> `PENDING_WALKTHROUGH`. Next eligible use cases: none. UC-2 remains `NOT_STARTED` until the
walkthrough is confirmed and UC-1 becomes `APPROVED`.

## Response to execute

PENDING WALKTHROUGH: Confirm the five-step UC-1 administrator walkthrough.
