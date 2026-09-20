# Convergence: UC-1 - Generate an initial timetable

## Summary

- Submission: `spec/checkpoints/UC-1.md` at `422a7745489d35a6f186816e0503343d961c5a5a`
- Verdict: REJECT
- Findings: 2 critical, 0 gap, 0 protocol, 0 drift, 0 cosmetic
- Suite: 45 run, 0 failed, 0 errors, 0 skipped
- Working tree impact from verification: none; the four executor-identified concurrent fixture files remain untracked and unchanged

## Protocol Gate

1. PASS - exactly UC-1 is named and `spec/status.md` recorded `READY_FOR_CONVERGENCE` at the submitted commit.
2. PASS - `spec/checkpoints/UC-1.md` and the implementation are committed together at immutable submission `422a774`.
3. PASS - UC-1 has no `Requires`, `Includes`, or `Extends` dependency.
4. PASS - UC-2 is `NOT_STARTED`; no other use case is in progress or ready for convergence.
5. PASS - the checkpoint has rows for every scenario branch, guarantee, postcondition, applicable rule, command, changed-file group, and regression state. Incorrect claims are graded below rather than treated as a protocol omission.
6. PASS - all 51 files in `69cc268..422a774` are attributable to UC-1 or its build/test infrastructure. The four concurrent Merivälja fixture files are not in the commit.

## Runtime Reproduction

| Actor | Step or extension | Executor reported | Converge observed |
|---|---|---|---|
| Scheduling client | Main steps 1-3 | Exit 0, canonical complete `FEASIBLE`, revisions and score present | Exit 0; two hard-valid assignments; canonical result; `STEP_LIMIT`; input and timetable revisions present. Top-level keys do not contain `schoolId`. |
| Scheduling client | Extension 2a malformed input | Exit 2, `INVALID_INPUT`, no solver/timetable | `/dev/null` input exited 2 and published only base envelope plus validation report. Accepted seed 0 and step limit 10 were absent. |
| Scheduling client | Extension 2f exhausted search | Exit 3, deterministic diagnostics, no candidate timetable | Exit 3; `SEARCH_EXHAUSTED`; three exact collision diagnostics with involved IDs; no timetable. |
| Filesystem | Transport/publication preservation | Exit 74 or 130 without replacement | Reproduced by 11 packaged-process tests in the independently run full suite; byte-preservation assertions passed. |

## Evidence Ledger

| Contract element | Executor claim | Evidence obtained | Strength | Verified |
|---|---|---|---|---|
| UC-1 main step 1 | Packaged invocation accepts versioned definition and controls | Independent launcher invocation accepted example and step limit | STRONG | yes |
| UC-1 main step 2 | Canonical complete `FEASIBLE` with accepted metadata | Runtime result is complete and canonical, but lacks lineage operand `schoolId`; see C-1 | STRONG | no |
| UC-1 main step 3 | Exit 0, file-only machine result, stderr diagnostics | Runtime plus `PlanCliIT.mainSuccessScenario` at `src/test/java/org/schoolkernel/cli/PlanCliIT.java:36` | STRONG | yes |
| UC-1 extension 1a | Misuse exits 64 and preserves destination | `PlanCliIT.cliMisusePreservesFiles`; packaged process assertions | STRONG | yes |
| UC-1 extension 1b | Equal paths exit 64 and preserve input | Same packaged test asserts original input bytes | STRONG | yes |
| UC-1 extension 1c | Transport and safeguard failures exit 74 without replacement | Packaged missing-path tests plus injected safeguard byte-preservation test | STRONG | yes |
| UC-1 extension 2a | Structural/semantic failures publish `INVALID_INPUT` before solver | Independent malformed invocation plus zero-call recording-solver test; envelope metadata is wrong under G4, see C-2 | STRONG | yes |
| UC-1 extension 2b | Initial `basedOnRevision` is refused | Packaged successor input test and semantic validator assertion | STRONG | yes |
| UC-1 extension 2c | Individually impossible lesson bypasses search | Packaged no-room fixture returns stable hard diagnostic and no solver log | STRONG | yes |
| UC-1 extension 2d | Empty definition bypasses solver | Packaged result and recording-solver invocation count | STRONG | yes |
| UC-1 extension 2e | Reached limit publishes best feasible result and actual reason | Packaged one-move fixture asserts `STEP_LIMIT` and complete result | STRONG | yes |
| UC-1 extension 2f | Failed search emits diagnostics only | Independent exit-3 reproduction pins exact involved IDs and no timetable | STRONG | yes |
| UC-1 extension 2g | Safe internal result without undisclosed exception | Injected late solver failure proves exit 4 and no exception text; derivable metadata is missing, see C-2 | STRONG | no |
| UC-1 extension 2h | Interruption exits 130 without publication | Real SIGINT packaged-process test | STRONG | yes |
| UC-1 extension 2i | Publication failure exits 74 and preserves destination | Injected publication boundary with byte equality | STRONG | yes |
| UC-1 G1 | Complete logical definition contract | Strict schema, semantic validator, cap/order, and reference tests | STRONG | yes |
| UC-1 G2 | Exactly ten hard constraints and complete assignment | ConstraintVerifier rows, independent post-solve evaluator, runtime assignments | STRONG | yes |
| UC-1 G3 | Exactly four soft constraints and resolved weights | ConstraintVerifier rows and exact breakdown order/value checks | STRONG | yes |
| UC-1 G4 | Every handled outcome follows the result envelope | Independent `INVALID_INPUT` omits known seed/limit; late internal path discards derivable metadata; see C-2 | STRONG | no |
| UC-1 G5 | No partial/candidate timetable or forbidden status | Incomplete-adapter and exhausted-search boundary tests | STRONG | yes |
| UC-1 G6 | Interoperable definition and timetable revisions | Hash calculation is deterministic, but published timetable lacks `schoolId`, so its claimed revision cannot be independently verified; see C-1 | STRONG | no |
| UC-1 G7 | One thread, default seed/limit, effective controls reported | Configuration inspection and feasible result metadata | STRONG | yes |
| UC-1 G8 | No cross-engine byte-identity promise | Documentation and stable semantic assertions | STRONG | yes |
| UC-1 G9 | Atomic overwrite rules | Packaged refusal/replacement and injected failure byte checks | STRONG | yes |
| UC-1 G10 | Stateless invocation | Isolated complete-input process invocations and dependency/code inspection | STRONG | yes |
| UC-1 G11 | Benchmark is objective, not validity cap | Separate 1,000-lesson benchmark ran green without threshold | STRONG | yes |
| UC-1 success postcondition | Result is consumable and eligible as UC-2 current timetable | Missing `schoolId` prevents same-school lineage verification and recomputation of timetable revision; see C-1 | STRONG | no |
| UC-1 minimal guarantee | No non-feasible timetable; failures preserve destination | Negative process and injected-boundary byte/no-field assertions | STRONG | yes |
| UC-1 relationships | No Requires/Includes/Extends relation | Spec map and detailed UC agree | STRONG | yes |

## Rule Conformance

| Rule | Constraint | Evidence | Result |
|---|---|---|---|
| RULE-1 | Plain Java DTO/domain/planning separation | Reflection and mapping tests; source inspection | PASS |
| RULE-2 | Pinned stable dependencies/plugins | Dependency tree and clean Java 25 wrapper build | PASS |
| RULE-3 | Offline strict Draft 2020-12 contract | Bundled schemas, strict Jackson configuration, negative fixtures | PASS |
| RULE-4 | Validation precedes solving | Control flow and zero-call invalid-input test | PASS |
| RULE-5 | Interoperable content revisions | RFC/SHA tests pass, but the public timetable omits required revision-scope input `schoolId` | FAIL C-1 |
| RULE-6 | Fixed score and execution controls | Configuration/value tests and no raw score in public schema | PASS |
| RULE-7 | Exact hard/soft catalog | 14 declared constraints and positive/negative row tests | PASS |
| RULE-8 | Diagnostic candidates remain non-results | Independent exhausted-search reproduction | PASS |
| RULE-9 | Atomic same-directory publication | Source inspection plus preservation tests | PASS |
| RULE-10 | Central CLI outcome boundary | Packaged exit/channel suite | PASS |
| RULE-11 | Stateless side-effect-bounded runtime | Dependency/source review and temporary-file tests | PASS |
| RULE-12 | Executable JAR, launcher, wrapper | Clean build and normalized launcher/JAR comparison | PASS |
| RULE-13 | Independently verified complete UC-1 result | Post-solve evaluator and incomplete-candidate test | PASS |
| RULE-16 | Isolated layered verification | 45-test clean lifecycle plus separate benchmark; no tracked test writes | PASS |
| RULE-17 | Safe stderr observability | Empty stdout, no display names, safe exception suppression | PASS |

## Related-UC Regression

| Use case | Relationship/shared surface | Evidence | Result |
|---|---|---|---|
| None approved | UC-1 is the first use case | No approved-UC regression set exists | N/A |
| UC-2 not started | Requires UC-1 success postcondition | Static consumption audit found the missing current-timetable `schoolId`; C-1 blocks eligibility | FAIL C-1 |

## Findings

### C-1 CRITICAL - Published timetable cannot verify its revision or lineage

- Contract: UC-1 success says the result "is eligible to serve as the current timetable for UC-2" (`spec/spec.md:169`). The revision scope contains exactly `schemaVersion`, `schoolId`, `inputRevision`, and assignments (`spec/spec.md:393`), while UC-2 must reject a differing current `schoolId` (`spec/spec.md:196`).
- Code: `ResultFactory.feasible` uses `definition.schoolId()` when calculating the digest (`src/main/java/org/schoolkernel/contract/ResultFactory.java:106-107`) but never publishes it. The strict result schema has no `schoolId` property (`src/main/resources/schema/result-v1.schema.json:8-27`) and the timetable contains assignments only (`result-v1.schema.json:136-142`).
- Runtime: the independently published `FEASIBLE` result has `hasSchoolId: false`. A client given only that result cannot recompute `timetableRevision` or enforce UC-2's same-school lineage rule.
- Test gap: the main process test validates the schema and checks the digest's presence, but never recomputes it solely from published fields (`PlanCliIT.java:48-70`). `ContractTest` supplies an out-of-band literal school ID to the revision service (`ContractTest.java:100-115`).
- Revision outcome: publish the current timetable's `schoolId` in the strict result contract, then add a packaged test that reconstructs the exact revision scope only from output fields, recomputes the digest, and proves the result contains the same-school operand UC-2 requires.

### C-2 CRITICAL - Handled outcomes discard derivable execution and accepted-input metadata

- Contract: every structured result records effective seed and exactly one limit whenever derivable (`spec/spec.md:328-333`), and UC-1 G4 applies that envelope to every handled outcome (`spec/spec.md:158`).
- Code: `ResultFactory.invalidInput` has no execution-controls parameter and never calls `addExecutionMetadata` (`ResultFactory.java:22-42`). `ResultFactory.internalError` emits only the base envelope and safe message (`ResultFactory.java:132-135`), even for a late failure after catalog version, revision, effective weights, school definition, seed, and limit are known. `addExecutionMetadata` exists but is called only by feasible/no-solution paths (`ResultFactory.java:165-180`).
- Runtime: malformed input invoked with seed 0 and step limit 10 published `INVALID_INPUT` with `hasSeed: false` and `hasLimit: false`.
- Test gap: tests validate status and non-disclosure but do not assert the complete derivable envelope for each handled status.
- Revision outcome: carry execution controls into every structured result; retain accepted catalog/revision/effective weights (and the lineage identity from C-1) on late internal failures when already derived; omit only genuinely underivable fields. Add exact packaged/unit envelope assertions for malformed, semantic-invalid, no-solution, feasible, empty, and early/late internal paths.

## Status Update

`READY_FOR_CONVERGENCE` -> `NEEDS_REVISION`. UC-1 is the only next eligible use case; UC-2 remains blocked by UC-1.

## Response to execute

REVISE UC-1: publish verifiable timetable lineage including schoolId and preserve all derivable result-envelope metadata on handled failures.
