# Use-Case Status: School Timetable Operations Workspace

## Current

- Use case: none
- Status: APPROVED
- Next eligible: UC-2, UC-3, UC-4, UC-8

## Progress

| Use case | Status | Depends on | Implementation | Convergence |
|---|---|---|---|---|
| UC-1 | APPROVED | none | `854493f`; revises `2671b2a` for C-1 through C-3 and G-1 through G-10 | `convergence/UC-1.md` - APPROVED |
| UC-2 | NOT_STARTED | UC-1 | - | - |
| UC-3 | NOT_STARTED | UC-1 | - | - |
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

## Blockers

None. The user resolved C-1 on 2026-09-21 by selecting a required school-level `displayName` in the kernel definition.

## Deviations

None.
