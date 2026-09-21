# Use-Case Checkpoint: UC-2 - Replan a current timetable

## Summary

- Status: READY_FOR_CONVERGENCE
- Contract-revision base commit: `b5da3d6478b5291e5448b15df9157bd6ef43c1f4` (UC-1 school `displayName` reconvergence approved)
- Base commit: `498fb2e358cf03b46dc5f4bae84072691b5cad28`
- Revision base commit: `801deb59066831d9e213245305cf1ee460613136`
- Second revision base commit: `bbbecb1f9636b331068137ab5c497a27e4c358f5`
- Submission commit: `HEAD at convergence`
- Relations verified: Requires UC-1; every packaged UC-2 success journey generates and consumes its baseline through the approved `plan` command

The shared required school `displayName` contract is already committed in approved UC-1 submission `78b5bb7`; this
UC-2 reconciliation verifies that successor definitions, `basedOnRevision`, and plan -> replan -> verify behavior
consume that revised postcondition without changing UC-2's actor-visible contract.

## Contract Evidence

| Contract element | Test or runtime evidence | Result |
|---|---|---|
| Main steps 1-3 and success postcondition | `ReplanCliIT.mainSuccessAndDirectSuccessor`; fresh plan/replan launcher journey; revised output is accepted as the next direct baseline | PASS |
| Extension 1a | Replan with both limits exits 64 and creates no output | PASS |
| Extension 1b | Output equal to current exits 64 and current bytes remain identical | PASS |
| Extension 1c | Missing input and overwrite refusal are packaged; injected resource safeguard and publication failure preserve prior bytes | PASS |
| Extension 2a | Malformed input and a schema-valid, revision-verifiable duplicate lesson assignment return `INVALID_INPUT` before solving | PASS |
| Extension 2b | Tampered revision, duplicate IDs, teacher/cohort/room period collisions, wrong lineage, and wrong school are rejected before solving | PASS |
| Extension 2c | Contradictory period lock is semantic invalid input before solving | PASS |
| Extension 2d | Main fixture accepts the current timetable becoming stale under a complete updated definition | PASS |
| Extension 2e | `emptyUpdateReportsCancellations` bypasses search and lists both cancellations | PASS |
| Extension 2f | No-room updated definition returns deterministic preflight failure without search | PASS |
| Extension 2g | Step-bounded main/change journeys publish the best complete feasible result with actual termination | PASS |
| Extension 2h | Four lessons competing for three periods exhaust search; result has diagnostics, no timetable/change report | PASS |
| Extensions 2i-2k | Injected UC-2 internal/interrupt/publication failures plus packaged SIGINT prove safe result, exit 130/no publication, and exit 74/prior-byte preservation | PASS |
| G1-G3 | Approved UC-1 school-name/schema/revision rules regress green; current schema, status, revision, unique lessons, three snapshot-provable hard collisions, school, and direct lineage validate before solving | PASS |
| G4-G8 | Common-ID mapping, exact 1-hard/3-soft levels, solver trade-off fixtures, and independent period/room lock journeys prove stability ordering and exclusions | PASS |
| G9 | Packaged tests compare complete sorted arrays, all old/new fields, independent forced dimensions, allowed teacher overlap, and forbidden move overlap | PASS |
| G10 | Revised assignments preserve all six snapshot IDs and produce a new verifiable timetable revision | PASS |
| G11 | Limit-bounded score is reported without global-minimality claim | PASS |
| Minimal guarantee | Refusal, safeguard, validation, search, interruption, and injected publication paths omit replacement timetable/change report and preserve current/destination bytes | PASS |

## Rule Evidence

| Rule | Evidence | Result |
|---|---|---|
| RULE-1 through RULE-5 | Existing suites plus deterministic duplicate-current rejection prove schema, semantic, revision, lineage, and pre-solver validation | PASS |
| RULE-6 | Configuration asserts one hard/three soft levels; deterministic solver fixtures prove period > room > preference choices | PASS |
| RULE-7 and RULE-8 | Catalog rows unchanged; stability rows are separate product-score components; failed search exposes diagnostics only | PASS |
| RULE-9 through RULE-12 | UC-2-specific interrupt/publication/safeguard tests cover the shared atomic boundary; packaged CLI, stateless runtime, shaded JAR, launcher, and wrapper remain green | PASS |
| RULE-14 | Production UC-1 baselines are strict-schema/revision/semantic validated, including unique lessons and teacher/cohort/room period validity, and byte-preserved | PASS |
| RULE-15 | Trade-off solver fixtures and full-value packaged reports prove ordering, independent locks, exact categories, ordering, and overlap semantics | PASS |
| RULE-16 | 61-test clean lifecycle: 42 unit and 19 packaged tests; no tracked writes | PASS |
| RULE-17 | Packaged replan stdout is empty; solver lifecycle stays on stderr; no documents/timetables are logged | PASS |

## Validation

- Focused commands: service failure injection, solver dominance trade-offs, packaged plan -> replan -> replan direct-successor, exact report, duplicate/colliding-current, and SIGINT paths.
- Full relevant suite: `mvn -q clean verify` - 120 run, 0 failed, 0 errors, 0 skipped; independent
  kernel-only `mvn -q -pl kernel-cli -am verify` - 85 run, 0 failed, 0 errors, 0 skipped.
- Working tree impact from tests: none; only ignored `target/` artifacts.
- Runtime evidence: fresh packaged plan -> replan -> verify returned `FEASIBLE`, `FEASIBLE`, and `VERIFIED` with two
  assignments. `examples/updated-school.json.basedOnRevision` exactly matched the fresh current result's
  `inputRevision` (`sha256:c2b046643fc71e3c8d8a4c274496a47dc5ccddab5d97f2f89d77b47dcea26d14`).
- Changed files: exact groups are listed in `spec/status.md`.
- Approved UCs regression-tested: UC-1, all 85 kernel checks remain green.

## Notes

C-1, C-2, and G-1 through G-3 from the prior convergence reports remain addressed. The required school-name change is
shared approved UC-1 infrastructure; updated-school lineage and UC-2 constructor fixtures were regenerated in that
dependency submission. Timetable-workspace revision files remain excluded. No deviations.

READY FOR CONVERGENCE: UC-2
