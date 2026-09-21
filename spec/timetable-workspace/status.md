# Use-Case Status: School Timetable Operations Workspace

## Current

- Use case: UC-1
- Status: READY_FOR_CONVERGENCE
- Next eligible: none

## Progress

| Use case | Status | Depends on | Implementation | Convergence |
|---|---|---|---|---|
| UC-1 | READY_FOR_CONVERGENCE | none | HEAD at convergence | Pending |
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
- Implementation submission: HEAD at convergence
- Changed files: `.gitignore`, `README.md`, `pom.xml`, `school-kernel`; root `src/` relocated into `kernel-cli/` and `kernel-contract/`; new `timetable-workspace/`; `spec/timetable-workspace/status.md`; `spec/timetable-workspace/checkpoints/UC-1.md`; `spec/timetable-workspace/checkpoints/UC-1.jev-bundle.json`
- Commands and results: `mvn -q clean verify` — 96 tests, 0 failures, 0 errors, 0 skipped; `git diff --check` — clean; contract JAR `.class` scan — none; Jackson dependency tree — `tools.jackson.core:jackson-databind:3.2.2`; Jev bundle validation — valid, 33 items; external Jev preflight — REVIEW because disclosure was not authorized

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

None.

## Deviations

None.
