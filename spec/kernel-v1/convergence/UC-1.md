# Convergence: UC-1 - Generate an initial timetable

## Summary

- Submission: `spec/kernel-v1/checkpoints/UC-1.md` at `78b5bb7af8854521907222db814098cf239dae72`
- Verdict: APPROVE
- Findings: 0 critical, 0 gap, 0 protocol, 0 drift, 0 cosmetic
- Suite: 85 run, 0 failed, 0 errors, 0 skipped
- Working tree impact from verification: none; the executor-identified timetable-workspace revision remains dirty and unchanged

## Protocol Gate

1. PASS - exactly UC-1 was `READY_FOR_CONVERGENCE` at submission `78b5bb7`.
2. PASS - the revised checkpoint and school-name contract implementation are committed at one immutable boundary.
3. PASS - UC-1 has no use-case dependency.
4. PASS - UC-2 remains `NEEDS_REVISION`; no other UC is active or ready.
5. PASS - the checkpoint covers every scenario, guarantee, postcondition, applicable rule, command, file group, and prior finding.
6. PASS - the submission is attributable to the user-selected complete-definition contract change and the fixture,
   lineage, constructor, and regression updates it necessarily causes. The timetable-workspace implementation remains
   outside the commit.

## Runtime Reproduction

| Actor | Step or extension | Executor reported | Converge observed |
|---|---|---|---|
| Scheduling client | Main steps 1-3 | Exit 0, canonical complete `FEASIBLE`, published `schoolId`, verifiable revisions | Fresh `./school-kernel plan` exited 0 with `FEASIBLE` and two complete assignments from the named definition. |
| Scheduling client | Extension 2a missing school name | Exit 2, `INVALID_INPUT`, no solver or timetable | Fresh packaged invocation exited 2 with `INVALID_INPUT` and no `timetable`; the packaged test also proves absence of solver-start evidence for missing and blank names. |
| Scheduling client | Extension 2a malformed input | Exit 2, `INVALID_INPUT`, retained known controls, no timetable | `/dev/null` exited 2; result contained seed 0 and step limit 10, validation report, and no timetable. |
| Scheduling client | Extension 2f exhausted search | Exit 3, deterministic diagnostics, no candidate timetable | Prior convergence reproduction remains attributable to unchanged code: `SEARCH_EXHAUSTED`, exact teacher/cohort/room collision evidence, no timetable. Full packaged test reran green. |
| Filesystem | Transport/interruption/publication paths | Stable exits and destination preservation | Real packaged-process byte/no-publication assertions reran green in the independent suite. |

## Evidence Ledger

| Contract element | Executor claim | Evidence obtained | Strength | Verified |
|---|---|---|---|---|
| UC-1 main step 1 | Packaged invocation accepts definition and controls | Fresh repository-launcher invocation | STRONG | yes |
| UC-1 main step 2 | Atomic complete `FEASIBLE` with accepted metadata | Fresh result, schema assertions, exact assignments, `schoolId`, and independent digest recomputation | STRONG | yes |
| UC-1 main step 3 | Exit 0, file-only machine output, stderr diagnostics | Fresh exit plus packaged stdout/stderr assertions | STRONG | yes |
| UC-1 extension 1a | Misuse exits 64 without publication | Real packaged both-limit/equal-option assertions | STRONG | yes |
| UC-1 extension 1b | Equal input/output preserves input | Real packaged byte equality | STRONG | yes |
| UC-1 extension 1c | Transport/safeguard failures exit 74 and preserve | Packaged missing paths, overwrite refusal, and injected safeguard byte checks | STRONG | yes |
| UC-1 extension 2a | Invalid structures/semantics publish valid `INVALID_INPUT` before solving | Fresh missing-name journey, packaged missing/blank-name no-solver assertions, malformed journey, unsupported-version schema validation, and zero-call solver test | STRONG | yes |
| UC-1 extension 2b | `basedOnRevision` is invalid for plan | Packaged successor fixture and zero solver log | STRONG | yes |
| UC-1 extension 2c | Individually impossible lesson bypasses search | Packaged no-room diagnostic and absent solver log | STRONG | yes |
| UC-1 extension 2d | Empty definition bypasses solver | Packaged empty result and zero-call recording solver | STRONG | yes |
| UC-1 extension 2e | Reached limit publishes best feasible with actual reason | Packaged one-move step-budget fixture | STRONG | yes |
| UC-1 extension 2f | Failed search emits diagnostics only | Exact packaged collision evidence and no timetable/forbidden labels | STRONG | yes |
| UC-1 extension 2g | Safe internal result, debug-controlled detail | Injected late failure retains all derived identity/controls and no timetable; non-debug detail absent | STRONG | yes |
| UC-1 extension 2h | Interruption exits 130 without publication | Real SIGINT packaged process | STRONG | yes |
| UC-1 extension 2i | Publication failure exits 74 and preserves | Injected publication boundary with byte equality | STRONG | yes |
| UC-1 G1 | Complete strict definition contract | Draft 2020-12 schema requires school `displayName`; schema/semantic tests reject missing and blank, accept Unicode, and preserve it through DTO/domain mapping | STRONG | yes |
| UC-1 G2 | Exactly ten hard rules and complete assignment | Every hard row positive/negative, independent evaluator, fresh assignments | STRONG | yes |
| UC-1 G3 | Exactly four soft rules with effective weights | Every soft row positive/negative and exact ordered result breakdown | STRONG | yes |
| UC-1 G4 | Every handled outcome follows complete envelope | Result schema plus feasible, malformed, semantic, no-solution, empty, and late-internal exact assertions | STRONG | yes |
| UC-1 G5 | No partial/diagnostic timetable or forbidden status | Incomplete-adapter and exhausted-search boundary tests | STRONG | yes |
| UC-1 G6 | Interoperable content revisions and exact IDs | RFC vectors, fixed digest, normalization, explicit school-name mutation, regenerated example lineage, and fresh output-only timetable digest | STRONG | yes |
| UC-1 G7 | One thread, seed 0/default limit, effective controls reported | Config inspection and fresh feasible/invalid result metadata | STRONG | yes |
| UC-1 G8 | Bounded reproducibility claim | Documentation and semantic assertions | STRONG | yes |
| UC-1 G9 | Atomic overwrite policy | Real refusal/replacement and injected failure preservation | STRONG | yes |
| UC-1 G10 | Stateless complete-input invocation | Repeated isolated process journeys and source/dependency review | STRONG | yes |
| UC-1 G11 | Target scale is non-gating objective | Separate 1,000-lesson benchmark passed and reported timing without threshold | STRONG | yes |
| UC-1 success postcondition | Canonical timetable is consumable as UC-2 current input | Published `schoolId`, `inputRevision`, canonical assignments, and independently verifiable timetable revision provide the required baseline identity | STRONG | yes |
| UC-1 minimal guarantee | Negative paths never disclose timetable and preserve destinations | Packaged and injected boundary assertions | STRONG | yes |
| UC-1 relationships | No Requires/Includes/Extends | Spec map and detailed UC agree | STRONG | yes |

## Rule Conformance

| Rule | Constraint | Evidence | Result |
|---|---|---|---|
| RULE-1 | Plain Java DTO/domain/planning boundaries | Reflection/value mapping and source inspection | PASS |
| RULE-2 | Pinned stable dependency baseline | Dependency tree and clean Java 25 wrapper build | PASS |
| RULE-3 | Strict offline Draft 2020-12 JSON | Bundled schema requires the shared nonblank `displayName`; strict mapper and packaged negative fixtures passed | PASS |
| RULE-4 | Validation precedes solving | Control-flow inspection and zero-call invalid tests | PASS |
| RULE-5 | Interoperable exact revision scopes | School-name-only mutation changes the definition revision; updated definition and timetable lineage passed packaged plan/replan regression | PASS |
| RULE-6 | Fixed solver score and controls | Config/value tests; no raw score in output | PASS |
| RULE-7 | Exact catalog only | 14 declared rows and row-isolated tests | PASS |
| RULE-8 | Diagnostic candidates remain non-results | Exhausted-search boundary evidence | PASS |
| RULE-9 | Atomic same-directory publication | Source and preservation tests | PASS |
| RULE-10 | Central CLI outcome boundary | Packaged stable-exit/channel suite | PASS |
| RULE-11 | Stateless bounded side effects | Dependency/source review and temp-file isolation | PASS |
| RULE-12 | Wrapper, shaded JAR, launcher | Clean build and normalized launcher/JAR comparison | PASS |
| RULE-13 | Independent complete-result validation | Post-solve evaluator, incomplete-candidate refusal, empty bypass | PASS |
| RULE-16 | Layered isolated verification | 85-test kernel lifecycle, 0 failures/errors/skips; no tracked writes | PASS |
| RULE-17 | Safe stderr observability | Empty stdout, no display names, safe internal detail handling | PASS |

## Related-UC Regression

| Use case | Relationship/shared surface | Evidence | Result |
|---|---|---|---|
| None currently approved | UC-1 is the first UC | No approved regression set exists | N/A |
| UC-2 needs reconvergence | Requires UC-1 success postcondition | Updated-definition fixtures and packaged replan regression consumed the new UC-1 lineage successfully | PASS |

## Findings

None. The selected contract delta is implemented and independently reproduced at the packaged CLI/result boundary.

## Status Update

`READY_FOR_CONVERGENCE` -> `APPROVED`. UC-2 becomes eligible for its required reconvergence.

## Response to execute

APPROVED
