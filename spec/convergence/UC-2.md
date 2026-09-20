# Convergence: UC-2 - Replan a current timetable

## Summary

- Submission: `spec/checkpoints/UC-2.md` at `bfb7a40`
- Verdict: REJECT
- Findings: 1 critical, 3 gaps, 0 protocol, 0 drift, 0 cosmetic
- Suite: 53 run, 0 failed, 0 errors, 0 skipped
- Working tree impact from verification: none; the pre-existing untracked debugger skill and Merivälja fixture files remain unchanged

## Protocol Gate

1. PASS - exactly UC-2 is named and was `READY_FOR_CONVERGENCE` at the gate.
2. PASS - `spec/checkpoints/UC-2.md` and the implementation are committed together at `bfb7a40` from base `498fb2e`.
3. PASS - required UC-1 is `APPROVED` with convergence report `spec/convergence/UC-1.md`.
4. PASS - no other use case is `IN_PROGRESS` or `READY_FOR_CONVERGENCE`.
5. PASS - the checkpoint contains claims for the scenario, extensions, guarantees, postconditions, rules, relationship, commands, changed files, and UC-1 regression.
6. PASS - all 22 files in `498fb2e..bfb7a40` are attributable to UC-2; the concurrent debugger and Merivälja files are untracked and excluded.

## Runtime Reproduction

| Actor | Step or extension | Executor reported | Converge observed |
|---|---|---|---|
| Scheduling client | UC-1 prerequisite | Packaged `plan` creates the current timetable | Exit 0; canonical `FEASIBLE` current timetable with two assignments and verifiable revisions |
| Scheduling client | UC-2 main steps 1-3 | Replan preserves the feasible baseline and publishes an empty change set | Exit 0; `FEASIBLE`, two unchanged assignments, score `0/0/0`, all six change categories empty, stdout empty, solver diagnostics on stderr |
| Scheduling client | UC-2 extension 2a | Semantically invalid current documents publish `INVALID_INPUT` and exit 2 | A schema-valid, revision-verifiable current document with a duplicate `lesson-math-1` assignment exited 4 and published `INTERNAL_ERROR`; debug identified `Collectors.toMap` duplicate key at `ReplanService.java:128` |
| Scheduling client | UC-2 extension 2i | Unexpected failures publish only a safe result unless debug is selected | The duplicate-key failure produced safe `INTERNAL_ERROR` output without debug and printed its stack only with `--debug` |
| Build client | Focused UC-2 process suite | Seven packaged UC-2 tests pass | `./mvnw -q -Dit.test=ReplanCliIT verify` passed: 35 unit tests plus 7 UC-2 integration tests |
| Build client | Full regression | 53 tests pass | `./mvnw -q clean verify` passed: 35 unit and 18 integration tests, 0 failures/errors/skips |

## Evidence Ledger

| Contract element | Executor claim | Evidence obtained | Strength | Verified |
|---|---|---|---|---|
| Main step 1 | Real packaged command with three distinct paths and controls | `ReplanCliIT.mainSuccessAndDirectSuccessor` and fresh launcher run | STRONG | yes |
| Main step 2 | Atomic feasible result, stability, and complete change report | Fresh plan-to-replan run produced a schema-valid complete timetable, `0/0/0` score, and six empty categories | STRONG | yes |
| Main step 3 | Exit 0 and direct-successor eligibility | `mainSuccessAndDirectSuccessor` successfully replans the first revised output | STRONG | yes |
| Extension 1a | Both limits cause misuse and no output | Packaged test asserts exit 64 and absent destination | STRONG | yes |
| Extension 1b | Output equal to current is refused without mutation | Packaged test asserts exit 64 and byte-identical current; source checks both input paths | STRONG | yes |
| Extension 1c | Missing input proves the whole transport branch | Only missing-definition input is exercised; overwrite refusal, pre-parse safeguard, destination preparation, and late publication failure are not exercised through `replan` | WEAK | no |
| Extension 2a | Malformed documents produce `INVALID_INPUT` before solving | Malformed JSON passes, but a recomputed-revision duplicate assignment reaches `Collectors.toMap` and becomes `INTERNAL_ERROR` | WEAK | no |
| Extension 2b | Status, tamper, school, lineage, and revision checks reject before solving | Strict result schema/status reader plus packaged tamper, school, and lineage cases | STRONG | yes |
| Extension 2c | Direct lock contradictions are semantic invalid input | Packaged period-lock/teacher-availability conflict exits 2 before solver logging | STRONG | yes |
| Extension 2d | A stale current assignment is accepted as the replan baseline | Packaged period/room invalidation and cancellation/change fixture paths solve successfully | STRONG | yes |
| Extension 2e | Empty update bypasses search and reports cancellations | Packaged test asserts `EMPTY_PROBLEM`, empty timetable, two cancellations, and no solver start | STRONG | yes |
| Extension 2f | Obvious no-room case publishes diagnostics without a timetable | Packaged test exits 3 before search and omits timetable | STRONG | yes |
| Extension 2g | Bounded feasible search reports its real termination | Main launcher run exits 0 with `STEP_LIMIT`, complete timetable, and no optimality claim | STRONG | yes |
| Extension 2h | Exhausted unsuccessful search exposes diagnostics only | Packaged conflict fixture exits 3 and omits timetable and change report | STRONG | yes |
| Extension 2i | Safe internal result and debug-only exception detail | Independent normal/debug duplicate-key runs exercise both channel behaviors | STRONG | yes |
| Extension 2j | Interruption exits 130 and preserves destinations | Only source checks and UC-1 tests are cited; no UC-2 service or packaged-process interruption evidence | ABSENT | no |
| Extension 2k | Serialization or atomic publication failure exits 74 and preserves destinations | Shared `FileBoundary` source is present, but no UC-2 service or packaged-process failure injection exercises its catch/publication boundary | ABSENT | no |
| G1 | UC-1 guarantees regress for replan | Full UC-1 regression is green, but UC-2 failure-boundary coverage is incomplete | WEAK | no |
| G2 | Current integrity and revision are verified | Public tamper rejection and independent recomputed revision establish the implemented revision boundary | STRONG | yes |
| G3 | Current may be stale; only revised timetable must satisfy updated hard rules | Period, room, cancellation, and changed-teacher/lock journeys exercise stale baselines | STRONG | yes |
| G4 | Common IDs alone incur stability; additions/cancellations do not | Common-ID mapping and packaged addition/cancellation fixture with zero period moves | STRONG | yes |
| G5 | Period moves dominate room-only moves, which dominate preferences | Constraint tests count each component at its level, but no deterministic trade-off fixture proves either dominance relation through solving | WEAK | no |
| G6 | Period moves and room-only moves do not overlap | Packaged forced period and forced room-update journeys assert mutually exclusive score counts | STRONG | yes |
| G7 | Forced dimensions are excluded and teacher changes coexist correctly | One packaged fixture samples a teacher change plus forced period, but does not assert the complete category contents or both forced dimensions | WEAK | no |
| G8 | Period and room locks are independent | Unit test forces both dimensions together; no fixture proves one locked dimension leaves the other solver-controlled | WEAK | no |
| G9 | Exactly six deterministic categories with full field fidelity and non-overlap | Tests sample selected IDs/fields but do not assert exact arrays, counts, ordering, old/new values, and overlap rules | WEAK | no |
| G10 | Revised assignment snapshot supports another direct replan | Packaged direct-successor journey succeeds from the revised result | STRONG | yes |
| G11 | Stability is bounded-search behavior without a global minimum claim | Result contract exposes counts and actual termination without `OPTIMAL`; README makes no stronger promise | STRONG | yes |
| Success postcondition | Canonical feasible revised timetable and report can be reused | Schema validation plus direct-successor packaged journey | STRONG | yes |
| Minimal guarantee | All refused, interrupted, unsuccessful, and failed paths preserve current/destination | Refusal and search paths are strong; UC-2 interruption and late publication failures are absent | WEAK | no |
| Requires UC-1 | UC-2 consumes the approved UC-1 production postcondition | Every packaged success fixture creates its current file through the production `plan` command | STRONG | yes |
| Lineage state model | Only same-school direct successors are accepted | Packaged wrong-school, wrong-lineage, and direct-successor cases | STRONG | yes |
| Assignment fidelity | A feasible timetable has one assignment per lesson and deterministic IDs | Revised output is complete, but duplicate lesson IDs in a supplied current `FEASIBLE` document are not semantically rejected | WEAK | no |
| Product score | Stable `periodMoves`, `roomOnlyMoves`, and preference vector | Values and catalog breakdown are emitted; dominance trade-offs lack a solver fixture | WEAK | no |
| Change report | Exact categories, values, order, and non-overlap | Schema and source structure are correct; actor-boundary assertions are incomplete | WEAK | no |
| Result envelope and exit codes | Stable status/field/exit mapping | Main, misuse, validation, no-solution, and internal outcomes reproduced; UC-2 interruption/publication exits are not | WEAK | no |

## Rule Conformance

| Rule | Constraint | Evidence | Result |
|---|---|---|---|
| RULE-1 | Plain Java DTO/domain/Timefold boundaries | Component boundary tests and inspected UC-2 mapping keep Timefold types out of public DTO/domain | PASS |
| RULE-2 | Pinned stable dependency baseline | Dependency tree resolves Timefold 2.6.0, Jackson 3.2.2, NetworkNT 3.0.7, Picocli 4.7.7, and JCS 1.1; clean Java 25 build passes | PASS |
| RULE-3 | Strict offline Draft 2020-12 schema-first JSON | Bundled result/definition schemas, strict tests, and unknown-field rejection regress green | PASS |
| RULE-4 | All validation precedes solving and returns deterministic invalid reports | Duplicate current lesson IDs are not validated; they reach baseline collection and exit as internal error | FAIL (C-1) |
| RULE-5 | RFC 8785 content revisions | Existing vectors/digests plus tamper and independently recomputed timetable revision checks pass | PASS |
| RULE-6 | Hard, period, room, preference levels and deterministic controls | Source uses Bendable levels 0/1/2, but committed tests do not inspect level count or prove dominance trade-offs | FAIL (G-2) |
| RULE-7 | Exact hard/soft catalogs without extras | Catalog regression and new stability constraints are separated from product catalog breakdown | PASS |
| RULE-8 | Diagnostic candidates remain non-results | Packaged failed-search path exposes diagnostics without timetable/change report | PASS |
| RULE-9 | Atomic sibling publication preserves destinations on every failure | Shared boundary source is correct, but UC-2 does not inject late serialization/move failure or prove destination preservation | FAIL (G-1) |
| RULE-10 | Central CLI outcomes, channels, exits, interruption | Replan CLI covers ordinary channels/exits; exit 130 and late exit 74 are not exercised | FAIL (G-1) |
| RULE-11 | Stateless, side-effect-bounded runtime | Dependency/source inspection and direct-successor process isolation show no retained state/network/database | PASS |
| RULE-12 | Complete shaded executable and launcher | Clean build, launcher, and packaged tests pass | PASS |
| RULE-14 | Consume a verified UC-1 output and preserve current | Production UC-1 journeys and tamper/lineage checks pass, but a current document violating assignment fidelity is not refused as invalid | FAIL (C-1) |
| RULE-15 | Stability facts and exact deterministic classification | Component counting exists; dominance, independent lock dimensions, exact fields/order, and overlap semantics are not fully proven | FAIL (G-2, G-3) |
| RULE-16 | Isolated layered tests with no tracked writes | 53 tests are clean, but UC-2 lacks the required failure-boundary and deterministic trade-off/exactness fixtures | FAIL (G-1, G-2, G-3) |
| RULE-17 | Safe stderr observability and debug-only exceptions | Main stdout is empty; independent normal/debug internal-failure runs show safe/default and debug-only exception detail | PASS |

## Related-UC Regression

| Use case | Relationship/shared surface | Evidence | Result |
|---|---|---|---|
| UC-1 | Required current-timetable producer; shares schemas, solver, result factory, CLI, and file boundary | Full clean suite includes all 45 previously reported UC-1 checks and remains green | PASS |

## Findings

### C-1 CRITICAL - Duplicate current assignments escape semantic validation

UC-2 extension 2a requires a document that "fails semantic validation" to publish `INVALID_INPUT` with exit 2, assignment fidelity requires one assignment for every active lesson and no others, and RULE-4 requires validation before Timefold. `CurrentTimetableReader.read` validates schema, status, and revision but never rejects duplicate `lessonId` values (`CurrentTimetableReader.java:52-89`). `ReplanService` then collects assignments with an unguarded unique-key collector (`ReplanService.java:127-130`) and converts the duplicate-key exception to `INTERNAL_ERROR` (`ReplanService.java:165-179`).

Independent reproduction duplicated the `lesson-math-1` assignment in a valid UC-1 result, recomputed its public timetable revision, and invoked packaged `replan`. The process printed `Internal failure; correlation ID: duplicate-current`, exited 4, and published `INTERNAL_ERROR`; `--debug` confirmed `IllegalStateException: Duplicate key lesson-math-1` at line 128. This is observable wrong status and exit behavior and means RULE-14 does not fully require the UC-1 feasible assignment postcondition.

Revision outcome: validate the semantic invariants available from the current snapshot, at minimum unique lesson assignments, return a deterministic `INVALID_INPUT` report before baseline collection/solving, and add a packaged regression asserting exit 2, no solver start, no timetable/change report, and byte-identical current/destination guarantees.

### G-1 GAP - UC-2 failure-boundary evidence stops at shared source

UC-2 extensions 1c, 2j, and 2k and the minimal guarantee require destination preservation for preparation, interruption, serialization, and atomic-publication failures. RULE-9 requires injected serialization/move failures, RULE-10 requires controllable interruption, and RULE-16 requires per-use-case packaged verification. The checkpoint groups extensions 2i-2k under UC-1 regression (`spec/checkpoints/UC-2.md:26`), while the UC-2 process suite only exercises a missing definition for transport failure (`ReplanCliIT.java:187-192`). The replan interruption and publication catches exist (`ReplanService.java:142-143`, `162-182`, `203-210`) but are not driven through the UC-2 boundary.

Revision outcome: add controllable UC-2 evidence for interruption before publication and injected serialization/atomic-move failure, plus the untested destination/overwrite/resource-safeguard alternatives needed to prove extension 1c and the minimal guarantee. Assert exact exit, channels, absence of new/partial results, current immutability, prior-destination bytes, and temporary-artifact handling.

### G-2 GAP - No fixture proves lexicographic dominance

UC-2 G5 requires period moves to dominate room-only moves and room-only moves to dominate ordinary preferences. RULE-6 requires the configured levels, and RULE-15 explicitly calls for small step-limited dominance fixtures. Source assigns Bendable soft levels 0, 1, and 2 (`SchoolConstraintProvider.java:15-19`), and the unit test proves isolated match counts (`SchoolConstraintProviderTest.java:170-194`). The packaged move tests make the baseline period or room unavailable (`ReplanCliIT.java:238-267`), so the solver has no competing feasible choice and the tests cannot prove either dominance relation. `SolverConfigurationTest` also does not inspect score level counts.

Revision outcome: add deterministic solver/process fixtures with competing feasible schedules where preserving one period is chosen despite any number of room/preference improvements, and preserving one room is chosen despite preference improvements; assert assignments, product vector, catalog breakdown, termination, and configured level count.

### G-3 GAP - Exact change-report and independent-lock claims are only sampled

UC-2 G7-G9 and RULE-15 require independent lock dimensions plus exact counts, fields, ordering, and non-overlap for all six categories. `classifiesObservableChanges` checks one ID or selected field in four categories without asserting array sizes, full old/new values, ordering, category absence, or overlap (`ReplanCliIT.java:103-139`). `classifiesSolverChosenMoves` checks only lesson IDs and score counts for the remaining move categories (`ReplanCliIT.java:238-267`). The unit forced-move case locks period and room together (`SchoolConstraintProviderTest.java:186-193`), so it does not prove that the unlocked dimension stays solver-controlled.

Revision outcome: assert complete change-report arrays by value for additions, cancellations, teacher changes, independently forced period/room moves, solver period moves including both room IDs, and room-only moves; include deliberately permuted IDs and coexisting teacher/assignment changes to prove deterministic ordering and allowed/forbidden overlap.

## Status Update

`READY_FOR_CONVERGENCE` -> `NEEDS_REVISION`; next eligible use cases: none. UC-1 remains `APPROVED`.

## Response to execute

REVISE UC-2: resolve C-1 and add strong UC-2 evidence for G-1 through G-3 before resubmission.
