# Use-Case Status: School Timetable Operations Workspace

## Current

- Use case: UC-3
- Status: PENDING_WALKTHROUGH
- Next eligible: none until UC-3 walkthrough confirmation

## Progress

| Use case | Status | Depends on | Implementation | Convergence |
|---|---|---|---|---|
| UC-1 | APPROVED | none | `854493f`; revises `2671b2a` for C-1 through C-3 and G-1 through G-10 | `convergence/UC-1.md` - APPROVED |
| UC-2 | APPROVED | UC-1 | `b7180a9`; started from `dfe0e75` | `convergence/UC-2.md` - APPROVED WITH NOTES |
| UC-3 | PENDING_WALKTHROUGH | UC-1 | `1a796d2`; walkthrough startup `2523205`; started from `a9ab56a` | `convergence/UC-3.md` - PENDING WALKTHROUGH |
| UC-4 | NOT_STARTED | UC-1 | - | - |
| UC-5 | NOT_STARTED | UC-4 | - | - |
| UC-6 | NOT_STARTED | UC-5 | - | - |
| UC-7 | NOT_STARTED | UC-1; includes UC-3, UC-4, UC-5, UC-6 | - | - |
| UC-8 | NOT_STARTED | UC-1 | - | - |

## UC-1 Evidence

- Started: 2026-09-20T22:00:15Z
- Started from: `db18108c24ddc14b9a1496f0a258f47d3d06c870`
- Pre-existing dirty files: none
- Revision started: 2026-09-21; base `d0e90317359b08be026c9d8f9a8f37195cbea3f7`; user selected the required
  school-level `displayName` contract resolution for C-1.
- Implementation submission: HEAD at convergence
- Changed files in this revision: kernel verification/publication architecture tests; workspace process, security, problem,
  archive, import, credential, and UI production files; Flyway/process/browser/archive/HTTP tests; decisions, status,
  checkpoint, Jev bundle, and `output/playwright/uc1-display-name.png`.
- Commands and results: `mvn -q clean verify` — 120 tests, 0 failures, 0 errors, 0 skipped; focused malformed
  multipart test — PASS; independent kernel suites and packaged plan -> replan -> verify — PASS; `git diff --check` —
  PASS; visible Playwright import — `Demo School`, exact revision, zero console errors/warnings; Jev bundle local
  validation — valid, 33 items; external Jev preflight — REVIEW because disclosure was not authorized.

| Contract element | Evidence |
|---|---|
| UC-1 main steps 1-4 | Real HTTP definition, accepted-pair, and exact-archive journeys at `timetable-workspace/src/test/java/org/schoolkernel/workspace/WorkspaceImportIT.java:93`, `:115`, and `:141`; browser initial-draft journey in `output/playwright/uc1-initial-draft.png` |
| UC-1 extension 1a | Result-only HTTP request returns `MATCHING_DEFINITION_REQUIRED`, exposes no assignments, and preserves exact empty state at `WorkspaceImportIT.java:182` |
| UC-1 extension 1b | Bounded in-memory ZIP tests at `SafeImportReaderTest.java:41,56` and real HTTP unsafe-archive rejection at `WorkspaceImportIT.java:141` |
| UC-1 extension 2a | Packaged strict verification at `kernel-cli/src/test/java/org/schoolkernel/cli/VerifyCliIT.java:78,111`, shared hard-baseline mutation coverage, and HTTP no-mutation assertions |
| UC-1 extension 2b | Successor-only packaged verify refusal at `VerifyCliIT.java:78` |
| UC-1 extension 2c | Exact complete `FEASIBLE` packaged verify and accepted-pair HTTP persistence at `VerifyCliIT.java:56` and `WorkspaceImportIT.java:115` |
| UC-1 extension 4a | Accepted-pair PostgreSQL failure injection and complete rollback at `WorkspaceImportIT.java:333` |
| UC-1 G1-G5 | Distinct import-state assertions, full JSON comparison, persistent-lock manifest test, isolated version validation, and local/no-partial-state security tests recorded in `checkpoints/UC-1.md` |
| UC-1 success postcondition | Exact singleton state/version/document assertions after both successful modes |
| UC-1 minimal guarantee | All refused/failing HTTP journeys assert `EMPTY`, version `0`, `{}` JSONB, and candidate absence |
| RULE-1, RULE-2 | Reactor/contract archive/dependency/architecture checks plus clean Java 25 build |
| RULE-3 through RULE-9 | PostgreSQL aggregate, Flyway, lifecycle, ETag race, transaction rollback, canonical document fidelity, and packaged verify evidence in `checkpoints/UC-1.md` |
| RULE-13, RULE-14 | Private bounded process adapter and strict no-extraction archive reader/tests |
| RULE-21 through RULE-24 | Loopback Host/Origin/CSRF/CORS route tests, safe problem responses/logs, real PostgreSQL/HTTP/browser boundary |
| RULE-26 through RULE-28 | Typed shared handlers, controlled catalog/version/public solver APIs, bounded input and race-safe publication suites |

## UC-2 Evidence

- Started: 2026-09-21; base `dfe0e75`.
- Pre-existing dirty files: none.
- Implementation submission: HEAD at convergence.
- Changed files: initial-planning service, packaged planner adapter, recovery hook, repository/mutation/controller/security/import/problem extensions, native workspace UI, planner/HTTP/browser tests, two Playwright screenshots, status, checkpoint, and Jev advisory artifacts.
- Commands and results: focused `KernelPlannerTest` — 6 tests PASS; focused `WorkspaceInitialPlanningIT` — 8 tests PASS; `./mvnw -q clean verify` — 135 tests, 0 failures, 0 errors, 0 skipped; `git diff --check` — PASS; Jev bundle validation — valid, 38 items; authorized external `jev-1.13.0` preflight — 13 findings, 22 reviews, 3 clear, with every flag independently dispositioned in `checkpoints/UC-2.md`.
- Runtime evidence: visible real-browser import -> production 30-second plan -> feasible initial proposal -> checkbox-confirmed acceptance opened `Demo School` at timetable revision `sha256:232c53bcf2ee6b373cb3965afc87849d245040c9c171f0cc543dff8742bf6b3c`; browser console reported 0 messages, errors, or warnings; screenshots are `output/playwright/uc2-initial-proposal.png` and `output/playwright/uc2-accepted-baseline.png`.

| Contract element | Evidence |
|---|---|
| UC-2 main steps 1-6 | Real PostgreSQL/HTTP/package journey at `WorkspaceInitialPlanningIT.java:100`; real Chrome journey at `WorkspaceBrowserIT.java:134`; visible production-limit journey and screenshots above |
| UC-2 extension 1a | Replacement success, unsuccessful search preservation, and invalid replacement no-mutation assertions at `WorkspaceInitialPlanningIT.java:179` |
| UC-2 extensions 2a and 4a | Cancel/late-result suppression and discard preserve the exact draft with no proposal at `WorkspaceInitialPlanningIT.java:148`; forced process cancellation at `KernelPlannerTest.java:86` |
| UC-2 extension 2b | Recovery of both interrupted solving and unaccepted proposal states at `WorkspaceInitialPlanningIT.java:214` and `WorkspaceRepository.java:92` |
| UC-2 extensions 3a and 3b | Safe invalid, no-feasible, internal, transport, interruption, watchdog, missing/malformed, and mismatched-output results at `KernelPlannerTest.java:67,126,165,214`; HTTP failure-class and mismatch journeys at `WorkspaceInitialPlanningIT.java:215,230` |
| UC-2 extension 5a | Stale identity invalidation plus the independent every-identity/result matrix at `WorkspaceInitialPlanningIT.java:261,394` |
| UC-2 extension 6a | Kernel revalidation unavailability and injected PostgreSQL acceptance failure retain the exact proposal/version at `WorkspaceInitialPlanningIT.java:261` |
| UC-2 G1-G6 and both postconditions | Explicit-confirmation browser/HTTP acceptance, exact definition/result comparison, safe labels/controls, all-or-nothing rollback, and retryable draft/proposal assertions recorded in `checkpoints/UC-2.md` |
| RULE-1 through RULE-29 applicable to UC-2 | Reactor, lifecycle, transaction, process, security, packaged-kernel, PostgreSQL/browser, and kernel compatibility evidence recorded by rule in `checkpoints/UC-2.md` |

## UC-3 Evidence

- Started: 2026-09-21; base `a9ab56a950d32e2f77d5daddffbc156a70efb1b9`.
- Pre-existing dirty files: none.
- Implementation submission: HEAD at convergence.
- Changed files: native accepted-inspection UI and centralized message catalog; architecture, HTTP, real-browser,
  empty-state, immutability, keyboard, narrow-screen, complete-snapshot, and target-scale tests; status, checkpoint, and
  Jev advisory artifacts.
- Commands and results: focused real-browser keyboard journey — PASS; `./mvnw -q clean verify` — 140 tests, 0
  failures, 0 errors, 0 skipped; `git diff --check` — PASS. Latest target-scale p95: search 69.5 ms, filter 38.8 ms,
  day 172.6 ms, selection 81.1 ms; solver time excluded.
- Walkthrough startup enablement: `2523205` adds Spring Boot-managed `postgres:18.6` Compose startup, loopback-only
  dynamic publication, health check, persistent named volume, packaged-JAR support, isolated-test disablement, and
  one-command documentation. Live packaged smoke returned `EMPTY` with ETag `ws-0`, served `/workspace/`, stopped the
  database with the application, and retained its volume. Final `./mvnw -q clean verify` — 141 tests, 0 failures,
  0 errors, 0 skipped; final loaded-suite day-change p95 199.4 ms after pre-indexing accepted assignments.

| Contract element | Evidence |
|---|---|
| UC-3 main steps 1-5 | Real Chrome whole-school, local-filter, keyboard selection, assignment-detail, focused schedule, and return journey at `WorkspaceBrowserIT.java:185` |
| UC-3 extensions 2a, 3a, 3b, 5a | Empty accepted browser journey at `WorkspaceBrowserIT.java:255`; filtered/no-match/narrow-screen assertions at `:211-250` |
| UC-3 G1-G6 | Opaque-ID/metadata rendering, catalog/state text, keyboard, scale p95, and exact non-mutation evidence in `checkpoints/UC-3.md` |
| UC-3 success postcondition | Browser identifies accepted assignment and complete/focused school context while preserving repair entry context |
| UC-3 minimal guarantee | Empty/no-match/presentation changes compare the exact unchanged PostgreSQL document and invent no assignments |
| Requires UC-1 | Approved UC-1 accepted pair consumed by complete production snapshot test at `WorkspaceBrowserIT.java:280` |
| RULE-1, RULE-2, RULE-4, RULE-19 through RULE-25 | Reactor, architecture, Flyway, snapshot, catalog, security, safe failure/logging, PostgreSQL/browser, and scale evidence in `checkpoints/UC-3.md` |

## Blockers

None. The user resolved C-1 on 2026-09-21 by selecting a required school-level `displayName` in the kernel definition.

## Deviations

None.
