# Technical Rules: Class timetable quality

## Design overview

The existing CLI and workspace paths remain the actor boundaries. The definition and result schemas accept catalog versions 1 and 2; the catalog selects defaults, overrides, emitted rows, and solver weights. Both planning paths use the same constraint provider and independent result evaluator. The workspace compiles a repair from the accepted definition/result pair and selects catalog version 2 for its successor. Existing accepted documents are read without rewriting them. No new persistence table, route, dependency, or UI control is introduced.

## Codebase alignment

The kernel keeps schema version 1 and its current CLI commands, Java domain/planning boundary, Timefold 2.6.0, four-level bendable score, and atomic result publication. Catalog version 1 remains exact. Catalog version 2 adds two preference rows only at the ordinary-preference level. The workspace continues to use its existing JSONB aggregate, subprocess verification, proposal review, and explicit acceptance path.

## Verification strategy

Unit checks compare exact gap and week-balance counts, zero-weight behavior, catalog rows, result metadata, and overflow bounds. Packaged CLI journeys prove initial planning and direct replanning with catalog version 2, as well as verification of a version 1 predecessor. The MV5 fixture reconstructs the accepted assignment pattern in isolated temporary files, compares the exact Friday candidate scores, and checks that a bounded packaged repair avoids a long class gap while preserving the predecessor. Workspace integration checks prove that compiling a repair upgrades only the successor and keeps accepted version 1 documents intact. Full reactor verification and a clean working-tree check follow.

## Rules

### RULE-1 - Versioned additive catalog

- Applies to: UC-1
- Constraint: MUST retain catalog version 1's exact four preference rows and accept its existing results; catalog version 2 MUST add exactly `soft.cohort-gap` and `soft.cohort-week-balance` with default weight one and configurable weights in the existing range. Result and verification metadata MUST identify the definition's actual catalog version.
- Reason: Existing accepted timetables and revision lineage must survive the new defaults.
- Verification: Schema and catalog exactness tests, old-result verification, version-specific result breakdown assertions, and an existing-baseline repair journey.

### RULE-2 - Identical solver and product metrics

- Applies to: UC-1
- Constraint: MUST calculate both new match counts from authoritative cohort availability and period weekday/order, include available zero-lesson days in week balance, and calculate the same counts in Timefold scoring and the independent result evaluator. The weighted sum MUST remain signed-64-bit-safe.
- Reason: The solver's preference and published score must describe the same candidate without overflow.
- Verification: ConstraintVerifier and evaluator tests compare compact, gapped, balanced, skewed, unavailable-day, and zero-weight examples by value; validator tests cover the maximum penalty bound.

### RULE-3 - Stability and repair lineage

- Applies to: UC-1
- Constraint: MUST preserve hard, period-move, room-only-move, then ordinary-preference priority; workspace repair compilation from catalog version 1 MUST produce a complete catalog version 2 successor while retaining the accepted predecessor unchanged.
- Reason: New preferences improve equal-stability choices while legacy state remains authoritative until acceptance.
- Verification: Solver priority tests, packaged MV5 plan-to-repair result, and workspace tests compare predecessor bytes/revisions and successor catalog/availability by value.

### RULE-4 - Actor-boundary regression

- Applies to: UC-1
- Constraint: MUST test both catalog version 2 planning and MV5 repair through the packaged CLI with isolated files, asserting feasibility, class-quality counts, preserved current input, and the absence of published timetable on rejected input or unsuccessful search. Existing kernel and workspace regression gates MUST remain green.
- Reason: A unit score check alone cannot prove the user receives the intended proposal.
- Verification: Packaged CLI tests, workspace integration, full reactor verification, and `git diff --check`.

## Use-case cross-reference

| Use case | Rules |
|---|---|
| UC-1 | RULE-1, RULE-2, RULE-3, RULE-4 |

## Design exclusions

No new HTTP route, persisted state type, access policy, dependency, or UI string is needed. The existing workspace security surface remains unchanged.
