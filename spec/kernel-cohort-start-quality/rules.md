# Technical Rules: Cohort day-start quality

## Design overview

The existing CLI and workspace keep their current boundaries. Catalog version 3 adds one soft constraint and one result breakdown row while versions 1 and 2 retain their exact contracts. The same validated period catalog drives the Timefold score and an independent result evaluator. The workspace compiles a repair successor from an accepted predecessor without rewriting accepted documents. No new route, persisted state type, dependency, or UI control is introduced.

## Codebase alignment

The kernel retains schema version 1, hand-authored offline JSON schemas, Java 25, Timefold 2.6.0, and the current hard/stability/ordinary score levels. The new constraint remains an ordinary preference. Version-specific effective weights and breakdown rows are selected by catalog version; old results remain verifiable. Workspace repair compilation advances the successor catalog to version 3 while preserving the accepted version and revision.

## Security surface

| Surface | Access |
|---|---|
| Local CLI `plan`, `replan`, `verify` | Explicit input and output paths; no account or network access |
| GET `/`, `/workspace/**`, `/api/csrf`, `/api/workspace`, `/api/runs/*`, `/api/proposal`, `/api/accepted/export` | Existing loopback, Host/Origin, and Spring Security filters; permitted |
| POST `/api/import`, `/api/initial-draft/replace`, `/api/repair-draft`, `/api/repair-draft/bulk-pin-preview`, `/api/runs`, `/api/proposal/accept`, `/api/workspace/clear`, `/api/workspace/upload-definition` | Existing loopback, Host/Origin, CSRF, and state guards; permitted |
| PATCH `/api/repair-draft`; DELETE `/api/repair-draft`, `/api/runs/*`, `/api/proposal`, `/api/workspace` | Existing loopback, Host/Origin, CSRF, and state guards; permitted |
| Login/logout, other routes or methods | No login/logout routes; all other requests denied |

## Verification strategy

Unit checks compare exact late-start counts at slots three and four, nonconsecutive period orders, multiple lessons on one day, empty cohort-days, zero weight, catalog rows, and overflow bounds. Packaged CLI journeys prove catalog 3 initial planning and direct replanning, and verify unchanged catalog 1 and 2 definition/result pairs. Isolated workspace integration proves an accepted version 1 or 2 pair remains unchanged while its repair successor uses version 3. A normal 30-second packaged MVK plan is independently inspected for both internal gaps and late cohort-days; time-limit evidence is reported as one observed run, not an optimality proof. Full relevant regression and working-tree checks follow.

## Rules

### RULE-1 - Additive versioned catalog

- Applies to: UC-1
- Constraint: MUST retain catalog versions 1 and 2 with exactly their original preference IDs, defaults, and result rows. Catalog version 3 MUST add exactly `soft.cohort-late-start` as a configurable ordinary preference with default weight one and report the actual catalog version in results and verification metadata.
- Reason: Existing accepted timetables and their revision lineage must remain readable without a score reinterpretation.
- Verification: Schema, catalog, result breakdown, old-result verification, and workspace predecessor/successor tests compare versions, rows, and revisions by value.

### RULE-2 - One authoritative late-start meaning

- Applies to: UC-1
- Constraint: MUST count one match for each taught cohort-day whose earliest assigned lesson is after the third declared period of its weekday, determined by ordered position rather than numeric order value. Days without lessons MUST contribute zero. Timefold scoring and the independent product evaluator MUST calculate identical counts, and the maximum weighted preference sum MUST remain signed-64-bit-safe.
- Reason: The search objective and published quality result must agree at the exact third-slot boundary.
- Verification: ConstraintVerifier, incremental-score, evaluator, and validator tests use slot-three/slot-four, nonconsecutive-order, multi-lesson, empty-day, zero-weight, and overflow cases.

### RULE-3 - Preserve repair priority and lineage

- Applies to: UC-1
- Constraint: MUST retain hard feasibility, period stability, room stability, then ordinary preference priority. Workspace repair MUST produce a complete version 3 successor from an accepted version 1 or 2 definition/result pair without mutating the accepted pair or adding period/room moves solely to satisfy the new preference.
- Reason: Start quality should improve equal-stability choices while accepted authority remains immutable.
- Verification: Solver priority cases, packaged direct replanning, and workspace integration compare exact predecessor bytes/revisions and successor catalog, score, and assignments.

### RULE-4 - Real-boundary quality evidence

- Applies to: UC-1
- Constraint: MUST verify catalog version 3 planning through the packaged CLI with isolated files, including feasible and invalid/unsuccessful outcomes and the absence of any candidate on failure. A normal-limit MVK run MUST be independently tallied for late cohort-days and internal gaps. Applicable kernel and workspace regression gates MUST remain green before convergence approval.
- Reason: Score-unit checks alone do not show the administrator the intended result.
- Verification: Packaged process tests, MVK run/result audit, workspace integration, full relevant Maven verification, and `git diff --check`.

## Use-case cross-reference

| Use case | Rules |
|---|---|
| UC-1 | RULE-1, RULE-2, RULE-3, RULE-4 |

## Design exclusions

No new HTTP route, UI string, authentication flow, database migration, dependency, hard constraint, or general solution-quality guarantee is introduced.
