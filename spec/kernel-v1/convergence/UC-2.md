# Convergence: UC-2 - Replan a current timetable

## Summary

- Submission: `spec/kernel-v1/checkpoints/UC-2.md` at `c50aa5d7e90b54bb8a770ab2681e227c14bb259f`
- Verdict: APPROVE
- Findings: 0 critical, 0 gaps, 0 protocol, 0 drift, 0 cosmetic
- Suite: 85 kernel tests run, 0 failed, 0 errors, 0 skipped; focused packaged `ReplanCliIT` passed
- Working tree impact from verification: none; pre-existing timetable-workspace revision files remain unchanged

## Protocol Gate

1. PASS - exactly UC-2 was `READY_FOR_CONVERGENCE` at the gate.
2. PASS - the reconciled checkpoint is committed at `c50aa5d`; the shared contract/fixture delta is traceable to
   approved required-UC submission `78b5bb7`.
3. PASS - required UC-1 remains `APPROVED`.
4. PASS - no other use case is active or awaiting convergence.
5. PASS - the checkpoint covers every scenario, guarantee, postcondition, rule, relationship, command, changed file, and UC-1 regression.
6. PASS - the submission contains only UC-2 checkpoint/status reconciliation; all timetable-workspace files remain excluded.

## Runtime Reproduction

| Actor | Step or extension | Executor reported | Converge observed |
|---|---|---|---|
| Scheduling client | UC-1 prerequisite | Production plan provides the current timetable | Fresh named definition planned to `FEASIBLE`; updated definition `basedOnRevision` exactly matched its `inputRevision`. |
| Scheduling client | Main steps 1-3 | Replan preserves a valid baseline with zero avoidable changes | Fresh replan exited 0 with `FEASIBLE` and two assignments; packaged verification exited 0 with `VERIFIED`. |
| Scheduling client | Extensions 2a/2b and C-1 | Revision-verifiable duplicate lesson assignment is invalid before solving | Exit 2; one deterministic validation detail, no solver/timetable/change report |
| Scheduling client | Extension 2b and C-2 | Intrinsic teacher/cohort/room period collisions are invalid before solving | Packaged fixture exits 2 with three exact ordered details; independent two-collision journey exits 2; neither exposes timetable/change report or solver logging |
| Scheduling client | Extension 2j | Interrupt before publication | Packaged SIGINT exits 130 and leaves the destination absent |
| Build client | Full regression | 120 reactor tests green | Independent kernel lifecycle: 85 tests, 0 failures/errors/skips; focused packaged replan suite passed. |

## Evidence Ledger

| Contract element | Executor claim | Evidence obtained | Strength | Verified |
|---|---|---|---|---|
| Main step 1 | Complete updated definition, verified current, distinct paths, controls | Packaged main and direct-successor tests plus fresh launcher journey | STRONG | yes |
| Main step 2 | Atomic hard-valid result with stability and every observable change | Exact schema, assignments, score vector, and complete change-report arrays | STRONG | yes |
| Main step 3 | Exit 0 and revised baseline reusable | Direct-successor packaged replan succeeds | STRONG | yes |
| Extension 1a | CLI misuse | Both-limit packaged case exits 64 with no output | STRONG | yes |
| Extension 1b | Output equals an input | Equal-current path exits 64 and preserves bytes; source checks both inputs | STRONG | yes |
| Extension 1c | Read/preparation/overwrite/safeguard failures | Packaged and injected paths exit 74 and preserve inputs/prior destinations | STRONG | yes |
| Extension 2a | Malformed/schema/semantic invalidity | Malformed, duplicate-ID, and intrinsic collision cases produce exact `INVALID_INPUT` before solving | STRONG | yes |
| Extension 2b | Current feasibility, revision, school, and direct lineage | Status/tamper/school/lineage/unique-assignment and snapshot hard-collision checks | STRONG | yes |
| Extension 2c | Detectable lock contradictions | Packaged period-lock/availability conflict exits 2 before solving | STRONG | yes |
| Extension 2d | Former assignment may be stale only under updated definition | Availability, room, cancellation, teacher, and lock changes are accepted for replanning | STRONG | yes |
| Extension 2e | Empty update | `EMPTY_PROBLEM`, empty timetable, exact sorted cancellations, no search | STRONG | yes |
| Extension 2f | Obvious no-room failure | Exit 3, deterministic diagnostics, no timetable, no search | STRONG | yes |
| Extension 2g | Bounded feasible search | Complete result with actual `STEP_LIMIT` and no optimality claim | STRONG | yes |
| Extension 2h | Unsuccessful search | Exit 3, diagnostics, no timetable/change report | STRONG | yes |
| Extension 2i | Safe internal result | Injected replan failure asserts exit 4, safe envelope, and hidden detail | STRONG | yes |
| Extension 2j | Interruption safety | Injected interruption and packaged SIGINT assert exit 130 and no publication | STRONG | yes |
| Extension 2k | Atomic-publication failure | Injected failure exits 74 and preserves prior bytes | STRONG | yes |
| G1 | UC-1 guarantees carry over | All 85 approved UC-1/kernel checks, including required school name and revision scope, plus UC-2 failure boundaries pass | STRONG | yes |
| G2 | Current integrity/provenance before solving | Strict schema/status/revision, duplicate, and universally derivable collision validation | STRONG | yes |
| G3 | Current may be stale under updated definition | Intrinsic snapshot invalidity is rejected while legitimate updated-definition staleness solves | STRONG | yes |
| G4 | Common IDs; additions/cancellations excluded | Exact common-ID mapping and sorted report arrays | STRONG | yes |
| G5 | Hard > period > room > preferences | One hard/three soft levels and deterministic competing solver fixtures | STRONG | yes |
| G6 | Period and room-only moves do not overlap | Exact product vector and full report cases | STRONG | yes |
| G7 | Forced dimensions excluded; teacher changes coexist | Full-value teacher-plus-period-force and independent room-force results | STRONG | yes |
| G8 | Period and room locks independent | Separate packaged period-only and room-only lock journeys | STRONG | yes |
| G9 | Exact deterministic change report | Complete values, sorting, category set, and overlap assertions | STRONG | yes |
| G10 | Revised assignment snapshot supports later replan | All six IDs retained and direct successor succeeds | STRONG | yes |
| G11 | Stability only within bounded search | Counts and actual termination exposed without global-minimum claim | STRONG | yes |
| Success postcondition | Canonical revised timetable/report reusable | Schema validation, complete assignments, revision verification, direct successor | STRONG | yes |
| Minimal guarantee | Refused/interrupted/unsuccessful/failed paths preserve state | All boundary-specific packaged and injected negative paths | STRONG | yes |
| Requires UC-1 | Consume approved UC-1 production postcondition | Every success fixture generates current through production `plan`; forged violations are refused | STRONG | yes |
| Lineage state model | Same-school direct successors only | Wrong-school, wrong-lineage, and repeated direct-successor journeys | STRONG | yes |
| Product score and change report | Exact stable public values | Dominance solver tests and complete packaged JSON comparisons | STRONG | yes |

## Rule Conformance

| Rule | Constraint | Evidence | Result |
|---|---|---|---|
| RULE-1 | DTO/domain/planning boundaries | Architecture regression and inspected replan mapper/service | PASS |
| RULE-2 | Pinned stable dependencies | Dependency tree baseline and clean Java 25 build | PASS |
| RULE-3 | Strict offline Draft 2020-12 schemas | Required school name, schema/round-trip/unknown-property, and updated-definition regressions | PASS |
| RULE-4 | Complete validation before solving | Malformed, duplicate, collision, revision, lineage, and lock packaged cases omit solver start | PASS |
| RULE-5 | RFC 8785 revisions | Existing vectors/digests, name-bearing definition revision, exact updated `basedOnRevision`, tamper, and independently recomputed current cases | PASS |
| RULE-6 | Fixed lexicographic score and controls | Level-count configuration and two competing dominance fixtures | PASS |
| RULE-7 | Exact catalog | All hard/soft catalog tests remain exact; stability rows remain separate | PASS |
| RULE-8 | Diagnostic candidates remain non-results | Failed search exposes only capped hard evidence | PASS |
| RULE-9 | Atomic sibling publication | UC-2 injected publication and safeguard preservation plus shared boundary regression | PASS |
| RULE-10 | Central CLI outcome/channel/exit mapping | Packaged exits 0/2/3/64/74/130 and injected 4; stdout remains empty | PASS |
| RULE-11 | Stateless bounded runtime | Dependency/source inspection and repeated direct-successor process isolation | PASS |
| RULE-12 | Shaded JAR, launcher, wrapper | Clean lifecycle and distribution regression | PASS |
| RULE-14 | Consume verified UC-1 output | Production baselines plus status/revision/unique-ID/resource-collision/lineage checks and byte preservation | PASS |
| RULE-15 | Stability facts and deterministic classification | Trade-off solver fixtures and full-value packaged reports | PASS |
| RULE-16 | Isolated layered verification | 85 tests across schema, semantic, constraint, solver, service, and packaged layers; no tracked writes | PASS |
| RULE-17 | Safe observability | Normal channels plus injected safe internal/debug behavior | PASS |

## Related-UC Regression

| Use case | Relationship/shared surface | Evidence | Result |
|---|---|---|---|
| UC-1 | Required producer and shared schemas, solver, result, CLI, and file boundary | All 85 kernel checks remain green; fresh plan output provided exact direct lineage to replan | PASS |

## Findings

None. Prior C-1, C-2, and G-1 through G-3 remain resolved; the school-name contract change is consumed with strong evidence.

## Status Update

`READY_FOR_CONVERGENCE` -> `APPROVED`; next eligible use cases: none. All specified use cases are approved.

## Response to execute

APPROVED
