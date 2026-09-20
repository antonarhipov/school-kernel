# Convergence: UC-2 - Replan a current timetable

## Summary

- Submission: `spec/checkpoints/UC-2.md` at `acd488c`
- Verdict: REJECT
- Findings: 1 critical, 0 gaps, 0 protocol, 0 drift, 0 cosmetic
- Suite: 61 run, 0 failed, 0 errors, 0 skipped
- Working tree impact from verification: none; pre-existing untracked debugger and Merivälja files remain unchanged

## Protocol Gate

1. PASS - exactly UC-2 was `READY_FOR_CONVERGENCE`.
2. PASS - checkpoint and implementation revision are committed together at `acd488c` from revision base `801deb5`.
3. PASS - required UC-1 remains `APPROVED`.
4. PASS - no other use case is active or awaiting convergence.
5. PASS - the revised checkpoint covers scenarios, guarantees, rules, relationships, changed files, commands, and regression.
6. PASS - the 11-file revision diff is attributable to C-1 and G-1 through G-3; concurrent untracked files are excluded.

## Runtime Reproduction

| Actor | Step or extension | Executor reported | Converge observed |
|---|---|---|---|
| Scheduling client | Main steps 1-3 | Production plan-to-replan succeeds with zero avoidable change | Exit 0; complete `FEASIBLE`, two unchanged assignments, `0/0/0`, six empty categories |
| Scheduling client | Extension 2a, prior C-1 | Duplicate current lesson assignment becomes deterministic invalid input | Exit 2; `INVALID_INPUT`, one ordered error, no solver/timetable/change report |
| Scheduling client | Extension 2b and RULE-14 | Every non-feasible current timetable is rejected | A revision-verifiable snapshot assigning both lessons to `teacher-alex` and `cohort-7a` in `mon-1` exited 0, invoked Timefold, and published a revised `FEASIBLE` result with one period move |
| Build client | Full regression | 61 tests green | `./mvnw -q clean verify`: 42 unit plus 19 integration, 0 failures/errors/skips |

## Evidence Ledger

| Contract element | Executor claim | Evidence obtained | Strength | Verified |
|---|---|---|---|---|
| Main step 1 | Packaged three-path request | Main and direct-successor process test plus fresh launcher journey | STRONG | yes |
| Main step 2 | Feasible stable result and complete report | Exact result schema, assignments, product vector, and all categories | STRONG | yes |
| Main step 3 | Exit 0 and reusable revised baseline | Direct-successor packaged replan succeeds | STRONG | yes |
| Extension 1a | CLI misuse | Both-limit packaged case exits 64 with no output | STRONG | yes |
| Extension 1b | Equal paths | Packaged equal-current path preserves bytes | STRONG | yes |
| Extension 1c | Transport and preparation failures | Missing input, overwrite, safeguard, and injected publication paths preserve destinations | STRONG | yes |
| Extension 2a | Structural and semantic invalid input | Malformed and duplicate-current cases are strong; universal current hard collisions remain accepted | WEAK | no |
| Extension 2b | Current must be a verified `FEASIBLE` timetable | Status/revision/lineage pass, but an intrinsically hard-colliding current snapshot is accepted | WEAK | no |
| Extension 2c | Lock contradictions | Packaged pre-solver lock conflict | STRONG | yes |
| Extension 2d | Stale baseline accepted against updated definition | Availability, room, cancellation, teacher, and lock journeys | STRONG | yes |
| Extension 2e | Empty update | Empty timetable, sorted cancellations, `EMPTY_PROBLEM`, no search | STRONG | yes |
| Extension 2f | Obvious no-room failure | Exit 3, diagnostics, no timetable, no search | STRONG | yes |
| Extension 2g | Bounded feasible search | Actual `STEP_LIMIT`, complete result, no optimality claim | STRONG | yes |
| Extension 2h | Unsuccessful search | Exit 3, diagnostics, no timetable/change report | STRONG | yes |
| Extension 2i | Safe internal failure | Injected service failure asserts safe result and hidden detail | STRONG | yes |
| Extension 2j | Interruption | Injected interruption and packaged SIGINT exit 130 without publication | STRONG | yes |
| Extension 2k | Publication failure | Injected failure exits 74 and preserves prior bytes | STRONG | yes |
| G1 | UC-1 guarantees carry over | Full regression and UC-2 failures pass; current hard-valid prerequisite remains incomplete | WEAK | no |
| G2 | Current integrity and provenance | Revision tamper and duplicate checks pass, but recomputed hard-colliding content passes | WEAK | no |
| G3 | Current may be stale under the updated definition | Stale updated-definition cases are distinguished from intrinsic snapshot collision | STRONG | yes |
| G4 | Common IDs and additions/cancellations | Exact mapping and report arrays | STRONG | yes |
| G5 | Period > room > preferences | Deterministic competing solver fixtures prove both dominance relations | STRONG | yes |
| G6 | Non-overlapping period/room-only scores | Exact score/report cases | STRONG | yes |
| G7 | Forced dimensions and teacher changes | Full-value teacher-plus-period-force and room-force reports | STRONG | yes |
| G8 | Independent locks | Separate period-locked and room-locked packaged journeys | STRONG | yes |
| G9 | Exact deterministic change report | Complete arrays, values, sorting, and overlap assertions | STRONG | yes |
| G10 | Snapshot supports another direct replan | Direct-successor packaged journey | STRONG | yes |
| G11 | Bounded stability only | Actual termination and no global-minimum label | STRONG | yes |
| Success postcondition | Canonical revised timetable/report reusable | Main and direct-successor journeys | STRONG | yes |
| Minimal guarantee | All refused/failed paths preserve inputs/destinations | Packaged and injected negative paths | STRONG | yes |
| Requires UC-1 | Consume actual approved postcondition | Baselines come from production `plan`, but forged hard-invalid content is not fully refused | WEAK | no |
| Assignment fidelity | One hard-valid assignment per lesson | Duplicate IDs are rejected; teacher/cohort period collisions in the snapshot are not | WEAK | no |

## Rule Conformance

| Rule | Constraint | Evidence | Result |
|---|---|---|---|
| RULE-1 through RULE-3 | Boundaries, dependencies, strict Draft 2020-12 JSON | Architecture/dependency/schema suites and source inspection | PASS |
| RULE-4 | Validation precedes solving | Intrinsic current teacher/cohort period collisions reach Timefold | FAIL (C-2) |
| RULE-5 through RULE-12 | Revisions, scoring, catalogs, diagnostics, publication, CLI, statelessness, distribution | Revised evidence and full lifecycle | PASS |
| RULE-14 | Require the UC-1 `FEASIBLE` postcondition | Reader checks schema/status/revision/unique lesson IDs but not universally detectable hard collisions | FAIL (C-2) |
| RULE-15 | Stability and exact change classification | Trade-off and complete-report fixtures | PASS |
| RULE-16 | Isolated layered verification | 61 clean tests, but missing current-collision semantic fixture | FAIL (C-2) |
| RULE-17 | Safe observability | Channel and injected internal/debug evidence | PASS |

## Related-UC Regression

| Use case | Relationship/shared surface | Evidence | Result |
|---|---|---|---|
| UC-1 | Required producer and shared solver/result/file boundary | All 45 prior checks remain green inside the 61-test lifecycle | PASS |

## Findings

### C-2 CRITICAL - Intrinsically hard-invalid current snapshots are accepted

UC-2 extension 2b requires `INVALID_INPUT` when the current document "is not a `FEASIBLE` timetable"; UC-1 G2 defines feasible as satisfying every hard constraint, assignment fidelity requires a complete valid assignment set, and RULE-14 requires the UC-1 `FEASIBLE` postcondition. The revised reader rejects duplicate lesson IDs, but after revision verification it otherwise accepts assignments without checking the three hard rules fully derivable from the snapshot: teacher-period, cohort-period, and room-period (`CurrentTimetableReader.java:63-104`).

Independent reproduction changed the second assignment in a genuine UC-1 result to `mon-1`, recomputed the public timetable revision, and supplied that current document to packaged replan. Both lessons then used the same teacher and cohort in `mon-1`, so the snapshot cannot be a UC-1-feasible timetable under any definition. The command nevertheless started Timefold, exited 0, and published `FEASIBLE` with one period move. This conflates an intrinsically invalid current snapshot with extension 2d, where a formerly valid timetable is merely stale under the updated definition.

Revision outcome: before lineage/solving, deterministically reject every teacher-period, cohort-period, and room-period collision observable from the current assignment snapshot. Add a revision-verifiable packaged fixture that triggers all three, asserts exit 2, exact ordered validation details, no solver/timetable/change report, and current/destination preservation. Do not validate old-definition availability, eligibility, or locks against the updated definition; those remain legitimate extension 2d inputs.

## Status Update

`READY_FOR_CONVERGENCE` -> `NEEDS_REVISION`; next eligible use cases: none. UC-1 remains `APPROVED`.

## Response to execute

REVISE UC-2: reject intrinsically hard-colliding current assignment snapshots before solving (C-2).
