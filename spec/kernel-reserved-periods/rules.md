# Technical Rules: Reserved school periods

## Design overview

The existing strict definition pipeline accepts an optional set of reserved period IDs. The kernel retains every declared period for reference and display but excludes reserved periods from the ordinary planning range and quality period catalog. Direct baseline verification independently rejects assignments to a reserved period under the definition being verified. Repair verifies its predecessor under that predecessor's definition, then applies the successor policy. No new catalog score row, result status, route, database table, or dependency is needed.

## Codebase alignment

Schema version 1 and catalog versions 1 through 3 remain readable. The optional reservation field is additive; absence preserves existing behavior and revisions. The field is separate from resource availability lists. Revision normalization treats it as an unordered ID set. Existing CLI and workspace service boundaries remain unchanged.

## Security surface

| Surface | Access |
|---|---|
| Local CLI `plan`, `replan`, `verify` | Explicit input and output paths; no account or network access |
| GET `/`, `/workspace/**`, `/api/csrf`, `/api/workspace`, `/api/runs/*`, `/api/proposal`, `/api/accepted/export` | Existing loopback, Host/Origin, and Spring Security filters; permitted |
| POST `/api/import`, `/api/initial-draft/replace`, `/api/repair-draft`, `/api/repair-draft/bulk-pin-preview`, `/api/runs`, `/api/proposal/accept`, `/api/workspace/clear`, `/api/workspace/upload-definition` | Existing loopback, Host/Origin, CSRF, and state guards; permitted |
| PATCH `/api/repair-draft`; DELETE `/api/repair-draft`, `/api/runs/*`, `/api/proposal`, `/api/workspace` | Existing loopback, Host/Origin, CSRF, and state guards; permitted |
| Login/logout, other routes or methods | No login/logout routes; all other requests denied |

## Verification strategy

Tests use isolated temporary files and workspace test data. Schema/domain tests check unknown and duplicate reserved IDs, reserved period locks, empty/omitted lists, and revision order invariance. Solver and evaluator tests check reservation versus resource availability, gap/balance/start semantics, baseline movement, and failure when only reserved slots could fit. Packaged CLI journeys exercise initial `plan`, direct `replan`, and `verify`, asserting both outcome and absence of a timetable on failures. Workspace repair tests confirm accepted predecessor bytes and revision remain unchanged. A normal 30-second MVK run inspects all assignment periods and independently verifies the result. Full relevant regression gates and a clean test working-tree check follow.

## Rules

### RULE-1 - Explicit additive policy

- Applies to: UC-1
- Constraint: MUST accept optional `reservedPeriodIds` as a unique array of references to declared period IDs in the school definition. Missing or empty means no reservations. MUST NOT infer reservation from ID, order, time, or display text or rewrite resource `availablePeriodIds`. The definition revision MUST normalize reservation list order while preserving every legacy document's revision.
- Reason: The MVK convention is school-specific, and accepted predecessors must remain stable.
- Verification: Schema and semantic validation, canonical revision tests, legacy definition/result verification, and inspection of the materialized definition.

### RULE-2 - Global assignment exclusion

- Applies to: UC-1
- Constraint: MUST exclude reserved periods from the planning value range and from baseline retention under the successor definition. Direct verification of a definition/result pair MUST reject an assignment in a period reserved by that definition. A lock to a reserved period MUST be rejected before search. The exclusion MUST NOT be relaxed by availability, a soft weight, or a time limit.
- Reason: Reservation is an unconditional global rule, not a preference or resource-specific availability workaround.
- Verification: Solver tests, packaged plan/replan/verify paths, lock-validation cases, and a no-regular-period failure with no candidate.

### RULE-3 - Regular-period quality

- Applies to: UC-1
- Constraint: MUST calculate teacher/cohort gaps, available weekdays, and the catalog-3 late-start threshold over regular periods only when reservations are configured. Solver scoring and independent result evaluation MUST agree, including nonconsecutive weekday order values. Definitions without reservations MUST retain their existing counts.
- Reason: A deliberately unused special slot is neither a gap nor the first ordinary school-day slot.
- Verification: Boundary fixtures compare reserved and unreserved counts in constraint scoring, evaluator output, and packaged result rows.

### RULE-4 - Repair, MVK, and regression boundary

- Applies to: UC-1
- Constraint: MUST preserve an accepted predecessor unchanged while compiling a complete reservation-aware successor. The MVK definition MUST reserve exactly its five `*-0` periods. A packaged 30-second MVK plan MUST produce a complete feasible result with zero reserved assignments and pass independent verification, without claiming optimality. Applicable kernel and workspace regression gates MUST be green before convergence approval.
- Reason: This must work in the administrator's actual initial and repair journeys without obscuring a pre-existing accepted result or regression.
- Verification: Packaged CLI run and verifier, MVK ID/assignment audit, workspace repair integration, full Maven verification, and `git diff --check`.

## Use-case cross-reference

| Use case | Rules |
|---|---|
| UC-1 | RULE-1, RULE-2, RULE-3, RULE-4 |

## Design exclusions

No new hard-score or soft-score catalog row, result field, HTTP route, UI control, role, migration, or dependency is introduced. Reserved-period exceptions are deferred.
