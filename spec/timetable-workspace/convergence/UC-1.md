# Convergence: UC-1 - Import school data into an empty workspace

## Summary

- Submission: `spec/timetable-workspace/checkpoints/UC-1.md` at `2671b2a24e7d97b0bc3363a952011a99593e19d1`
- Verdict: BLOCKED
- Findings: 3 critical, 10 gaps, 0 protocol
- Suite: unrestricted `mvn -q clean verify` — 96 run, 0 failed, 0 errors, 0 skipped; an earlier sandboxed run reached the PostgreSQL layer and failed only because Docker-socket access was denied
- Working tree impact from verification: no tracked changes; pre-existing `output/` remains untracked and Playwright session files are ignored

## Protocol Gate

1. PASS — exactly UC-1 is `READY_FOR_CONVERGENCE`.
2. PASS — checkpoint and implementation are committed together at `2671b2a24e7d97b0bc3363a952011a99593e19d1` from base `db18108c24ddc14b9a1496f0a258f47d3d06c870`.
3. PASS — UC-1 has no `Requires`, `Includes`, or `Extends` dependency.
4. PASS — every other use case is `NOT_STARTED`.
5. PASS — the checkpoint has rows for the scenario, extensions, guarantees, postconditions, relationships, applicable rules, commands, changed files, and regression state. Jev is correctly recorded as `REVIEW`, not pass evidence.
6. PASS — the reactor/kernel changes are necessary UC-1 enabling infrastructure; no later workspace use-case route or actor behavior is implemented.

## Runtime Reproduction

| Actor | Step or extension | Executor reported | Converge observed |
|---|---|---|---|
| Administrator | Open empty local workspace | Empty workspace and import choices | Headed Chromium at `http://localhost:18080/workspace/` showed `Empty workspace`, `No accepted timetable`, JSON document inputs, and accepted-bundle input. |
| Administrator | Main step 1, select initial definition | Real fixture selected without command-line work by the actor | Playwright selected `examples/initial-school.json` through the file control and submitted `Import JSON documents`. |
| Workspace | Main steps 2 and 4, verify and persist | Packaged verify and durable `INITIAL_DRAFT` | The packaged application logged `kernelCommand=verify exitClass=VERIFIED`; the rendered page changed to `Initial draft` with definition revision `sha256:cdbf20756fef7b07f1e47622f0c549137f859ccd86d13a5798c8033bc6d0cc9e`. |
| Workspace | Main step 3, identify school and state | Stable-ID fallback with unavailable-name cue | Browser showed heading `demo-school` and `School name unavailable; showing the stable school ID.` This does not reproduce the specified definition-supplied school display name. |
| Administrator | Main accepted-pair/archive alternatives | Real HTTP journeys | The independently rerun `WorkspaceImportIT` used the packaged kernel and asserted exact accepted definition/result/manifest persistence for pair and archive modes. |
| Administrator | Extensions and failures | Real HTTP/package tests | The independently rerun suite reproduced result-only, malformed, unsafe archive, missing/stale precondition, race, local-security, occupied-state, and database-failure branches. Missing matrices are recorded below. |
| Browser | Console | Zero errors and warnings | Playwright console reported 0 messages, 0 errors, 0 warnings. |

The temporary headed browser, packaged workspace process, and loopback PostgreSQL 18.6 container were stopped after reproduction.

## Evidence Ledger

| Contract element | Executor claim | Evidence obtained | Strength | Verified |
|---|---|---|---|---|
| Main step 1 | Both explicit choices are available | Rendered empty page plus HTTP pair/archive tests at `WorkspaceImportIT.java:93-177` | STRONG | yes |
| Main step 2 | Complete import validation | Positive modes and representative failures pass, but not every named invalidity through packaged `verify` and HTTP | WEAK — G-1 | no |
| Main step 3 | School and outcome are shown | State label is correct, but schema `school-definition-v1.schema.json:6-25` cannot carry a school display name and `ImportService.java:53-58` always falls back for a valid definition | IMPOSSIBLE — C-1 | no |
| Main step 4 | Verified state is durable and opened | Real PostgreSQL HTTP tests compare lifecycle/version/full JSON; browser reopened the resulting state | STRONG | yes |
| Extension 1a | Result alone requires definition and changes nothing | `WorkspaceImportIT.java:182-189` asserts 422, stable code, no assignments, and exact empty row | STRONG | yes |
| Extension 1b | Every absent/duplicate/extra/unreadable/unsafe archive is rejected | Extra/traversal is strong; other required archive forms are code inspection or absent tests | WEAK — G-1 | no |
| Extension 2a | Every named validation failure is safe and mutation-free | Malformed/mismatch paths are exercised, but the complete named matrix is not driven through packaged `verify` and real HTTP | WEAK — G-1 | no |
| Extension 2b | Definition-only successor is rejected without mutation | Packaged verify asserts rejection, but the HTTP/state branch is not reproduced | MISPLACED — G-1 | no |
| Extension 2c | Only exact complete FEASIBLE pair is accepted | Packaged positive/mismatch tests and accepted-pair HTTP persistence compare exact values | STRONG | yes |
| Extension 4a | Storage failure leaves no accepted state | PostgreSQL trigger failure during accepted-pair import returns 503 and preserves exact empty row | STRONG | yes |
| G1 | Import modes are explicit and never mislabeled | HTTP snapshots assert `INITIAL_DRAFT`/false and `ACCEPTED_BASELINE`/true | STRONG | yes |
| G2 | IDs are exact and user-facing names come from metadata | Full JSON is exact, but the school ID is rendered as the heading because no school name exists in the contract | IMPOSSIBLE — C-1 | no |
| G3 | Unmanifested locks are persistent policy | Production `ManifestService` path and exact origin assertions at `ManifestServiceTest.java:18-33` | STRONG | yes |
| G4 | Validation is isolated and rejects unsupported versions | Isolation is strong; unsupported versions are not exercised at the workspace actor boundary | WEAK — G-1 | no |
| G5 | Rejected or interrupted imports leave no partial state or remote surface | Rejection/local access paths are strong; interrupted verify and cleanup are not reproduced | WEAK — G-2 | no |
| Success postcondition | One durable initial draft or exact accepted baseline | Both real HTTP modes compare singleton lifecycle/version/document by value | STRONG | yes |
| Minimal guarantee | Every failed import stays empty and discloses no candidate | Strong for tested branches, incomplete for the missing extension/failure matrices | WEAK — G-1, G-2 | no |
| Relationships | UC-1 is a dependency-free root | Spec relation block at `spec.md:184-186`; no earlier UC is consumed | STRONG | yes |

## Rule Conformance

| Rule | Constraint | Evidence | Result |
|---|---|---|---|
| RULE-1 | Separate resource-only contract, CLI, and workspace modules; no workspace kernel implementation dependency | Reactor build, contract JAR scan with no `.class`, and `ArchitectureTest.java:13-23` | PASS |
| RULE-2 | Pinned Java 25/Spring Boot 4.1.1/PostgreSQL stack and excluded stacks absent | Clean build, dependency tree, POM inspection, PostgreSQL 18.6 runtime, and no `.impl` source | PASS |
| RULE-3 | One JdbcClient-managed singleton JSONB aggregate | Migration, repository SQL, and exact PostgreSQL row/document assertions | PASS |
| RULE-4 | Flyway-only schema plus refusal on checksum drift/failed migration | Fresh V1 migration passes, but checksum-drift and failed-migration startup refusal are untested | GAP — G-3 |
| RULE-5 | Exact lifecycle and exhaustive refused transition/action matrix | Enum/migration are exact and second import is refused only from `INITIAL_DRAFT`; remaining real source states/actions are not exhaustively tested | GAP — G-4 |
| RULE-6 | Strong ETag/If-Match and conditional SQL with one race winner | Missing/stale/racing HTTP tests plus source-order inspection prove checks precede kernel invocation and SQL uses the same version | PASS |
| RULE-7 | Accepted import is one rollback-safe transaction across write/commit failure boundaries | Single-update trigger rollback is strong; commit-boundary, connection-termination, restart, and canonical-byte comparisons are absent | GAP — G-5 |
| RULE-8 | Lossless canonical JSON and exact normalized revisions | Exact JSON trees and kernel revision vectors pass; Unicode JSONB, key/whitespace permutations at the workspace boundary, and canonical boundary-byte cycles are absent | GAP — G-6 |
| RULE-9 | Structured non-solving `verify` covers both modes and every UC-1 invalidity | Both modes, schema, exits, no solve controls, bounds, and publication pass; several baseline invalidities are only exercised through `replan` or lower layers | GAP — G-1 |
| RULE-13 | Private bounded process files/streams and cleanup on every outcome | Code at `KernelVerifier.java:41-100,152-181,202-216` implements the controls; permission, oversized-output, collision, failure-log, and every-outcome cleanup tests are absent | GAP — G-7 |
| RULE-14 | Exact bounded no-extraction archive parser and full boundary/malformed matrix | Reader code is bounded; tests cover exact, extra/traversal, and one oversize form, not the rule's complete below/at/above and malformed ZIP matrix | GAP — G-7 |
| RULE-21 | Same-origin loopback surface and no committed DB credentials | Route/Host/Origin/CSRF/CORS tests pass, but committed default username/password at `application.yml:15-16` violate the explicit credential prohibition | FAIL — C-2 |
| RULE-22 | Stable safe problem shape with current state/ETag and complete status mappings | Ordinary advice uses current state/ETag, but Host/CSRF denials hard-code `UNKNOWN`/`null` at `HostOriginFilter.java:48-49` and `SecurityConfiguration.java:55-56`; the complete mapping/disclosure scan is absent | FAIL — C-3, G-8 |
| RULE-23 | Structured safe logs and captured success/failure evidence | Runtime success log is safe; there is no captured every-failure-class test or complete prohibited-value scan | GAP — G-8 |
| RULE-24 | Real PostgreSQL HTTP/browser journey in standard verification and negative side-effect proofs | Converge reproduced the browser manually and Maven runs real HTTP/PostgreSQL/package tests, but the standard lifecycle has no automated browser journey and several negatives do not assert zero process calls/export | GAP — G-9 |
| RULE-26 | Typed shared handlers, exhaustive mappings, shared fixtures, independent hard-verifier mutation corpus | Core architecture and shared production classes exist; the architecture test only inspects outcome shape, and the required exhaustive mapper/shared-fixture/every-hard-row corpus is absent | GAP — G-10 |
| RULE-27 | Single catalog/version source and public solver APIs | Catalog/manifest tests, packaged version assertion, source/bytecode architecture, and public `SolverManager`/`SolverJob` implementation pass | PASS |
| RULE-28 | Bounded inputs and race-safe atomic publication with capability/failure/crash proof | Numeric boundaries and concurrent packaged publishers pass; capability, serialization, close, link, move, and controlled-crash failure matrix is absent | GAP — G-10 |

## Related-UC Regression

| Use case | Relationship/shared surface | Evidence | Result |
|---|---|---|---|
| None | UC-1 is the first workspace use case; no related UC is approved | Status ledger and relationship graph | N/A |
| Kernel v1 | Shared packaged plan/replan behavior moved into reactor | Clean reactor reran the existing kernel suites as part of the 96 passing tests | PASS |

## Findings

### C-1 CRITICAL — Required school display name cannot exist in a valid v1 definition

- Reference: UC-1 main step 3 requires, “The workspace shows the school by definition-supplied display name”; G2 requires user-facing names to come from definition metadata.
- Evidence: strict root schema `kernel-contract/src/main/resources/schema/school-definition-v1.schema.json:6-25` has `additionalProperties: false` and no school display-name property. `ImportService.java:53-58` looks for that forbidden property and otherwise copies `schoolId` into `displayName`. Independent browser reproduction rendered `demo-school` as the heading plus an unavailable-name cue.
- Why it fails: no valid input can drive the specified display-name branch. The implementation cannot satisfy the main scenario without either changing the kernel contract or changing the workspace specification.
- Revision outcome: product decision required — add an explicit school display-name field to the normative kernel definition and propagation/revision rules, or revise UC-1 to specify the stable-ID unavailable-name fallback.

### C-2 CRITICAL — Database credentials are committed

- Reference: RULE-21 says database credentials “MUST come from local environment or configuration and MUST NOT be committed”.
- Evidence: `timetable-workspace/src/main/resources/application.yml:15-16` commits default username and password values `school_workspace`.
- Why it fails: a missing environment variable silently selects committed credentials, contrary to the security constraint.
- Revision outcome: require externally supplied local credentials without committed credential defaults, and add configuration/secret-scan tests.

### C-3 CRITICAL — Security denials do not contain the current lifecycle state

- Reference: RULE-22 requires one problem shape “containing stable code, localized safe message, correlation ID, current lifecycle state, current ETag when disclosure is safe”.
- Evidence: Host/Origin denial writes `state:"UNKNOWN",etag:null` at `HostOriginFilter.java:48-49`; CSRF/security denial does the same at `SecurityConfiguration.java:55-56`, even while the database is available. Tests assert only status/absence of mutation.
- Why it fails: common 403 responses do not provide the current lifecycle recovery context required by the API contract.
- Revision outcome: route these denials through a safe problem-response component that supplies current state and ETag when safe, and assert the complete body/headers.

### G-1 GAP — Import invalidity branches are not fully proven at their declared boundaries

- References: UC-1 extensions 1b, 2a, 2b; G4; RULE-9 and RULE-14 verification requirements.
- Evidence gap: missing/duplicate/unreadable/encrypted/truncated/bomb archive forms and every named identity/revision/assignment/hard-validity/version failure are not each driven through packaged `verify` and real HTTP with response, no candidate, no mutation, and no process-call assertions.
- Revision outcome: add the complete deterministic packaged/HTTP matrix, including exact archive byte boundaries and definition-only successor HTTP refusal.

### G-2 GAP — Interrupted verification safety is unproved

- Reference: UC-1 G5 includes interrupted imports; RULE-13 requires cleanup on every outcome.
- Evidence gap: `KernelVerifier` has interruption code, but no controllable process test reproduces interruption, asserts no state/candidate, and snapshots cleanup.
- Revision outcome: inject a controllable child process and prove interruption, safe response, exact empty row, bounded channels, and temporary-directory cleanup.

### G-3 GAP — Flyway failure refusal is unproved

- Reference: RULE-4 requires startup refusal for migration or validation error and its verification requires checksum-drift/failed-migration cases.
- Evidence gap: only a fresh successful V1 migration is run.
- Revision outcome: add PostgreSQL startup tests for checksum drift and a failed transactional migration without weakening `validate-on-migrate`.

### G-4 GAP — Lifecycle refusal matrix is incomplete

- Reference: RULE-5 requires every unlisted transition to return 409 without any prohibited side effect and exhaustive state/action coverage.
- Evidence gap: import refusal is tested only after an initial draft; accepted/solving/proposal/repair states and process/export side effects are not covered.
- Revision outcome: establish every real source state in PostgreSQL and run the UC-1 import action against each with complete before/after and collaborator assertions.

### G-5 GAP — Accepted-transaction fault matrix is incomplete

- Reference: RULE-7 requires rollback at any statement or commit boundary; verification names connection termination, restart, exact JSON, and canonical export bytes.
- Evidence gap: one `BEFORE UPDATE` trigger failure is tested.
- Revision outcome: add commit/connection/restart fault cases and compare full accepted state/canonical bytes before and after.

### G-6 GAP — Workspace-boundary canonical fidelity matrix is incomplete

- Reference: RULE-8 requires lossless JSONB/canonical boundary behavior including Unicode and omitted/materialized distinctions.
- Evidence gap: ordinary full-tree comparisons and kernel vectors exist, but workspace JSONB permutations, Unicode, and canonical process/export cycle proof do not.
- Revision outcome: add value and canonical-byte round trips for key order, whitespace, Unicode, arrays/sets, and omitted defaults at the workspace boundary.

### G-7 GAP — Private process and archive resource-safety evidence is incomplete

- References: RULE-13 and RULE-14 verification requirements.
- Evidence gap: owner permissions, 64 KiB overflow, path collision, every-outcome cleanup, and the complete archive boundary/malformed matrix are not automated.
- Revision outcome: add filesystem/process doubles and the exact no-extraction ZIP corpus with post-run directory/database snapshots.

### G-8 GAP — Failure-shape and logging matrices are incomplete

- References: RULE-22 and RULE-23 verification requirements.
- Evidence gap: not every status mapping/failure class is scanned across response, headers, logs, state, process invocation, and prohibited details.
- Revision outcome: add production-mode parameterized HTTP/log capture covering all declared mappings and disclosure prohibitions.

### G-9 GAP — Browser and negative collaborator proof is outside the standard lifecycle

- Reference: RULE-24 requires a real browser journey per UC in standard verification and negative assertions for process/export side effects.
- Evidence gap: browser reproduction is manual; Maven has no browser automation, and several negatives infer rather than assert zero kernel/export invocation.
- Revision outcome: add repeatable browser automation to verification and instrument real-boundary negatives for collaborator absence.

### G-10 GAP — Kernel architectural/failure verification corpus is incomplete

- References: RULE-26 and RULE-28 verification requirements.
- Evidence gap: no exhaustive outcome-mapper subtype test, identical shared-fixture pass through every command, every-hard-row direct-verifier mutation corpus, or complete capability/serialization/close/link/move/crash publication matrix.
- Revision outcome: implement the named architecture, shared-fixture, mutation, and filesystem fault-injection suites.

## Walkthrough

Not requested because automated convergence is blocked before the human-approval gate. Independent browser reproduction covered the UC-1 initial-definition success path and exposed C-1.

## Status Update

`READY_FOR_CONVERGENCE` -> `BLOCKED`. Next eligible use cases: none. UC-2 remains `NOT_STARTED` because its required UC-1 postcondition is not approved.

## Response to execute

BLOCKED: Resolve C-1 with an explicit product decision, then revise C-2, C-3, and G-1 through G-10 before reconverging UC-1.
