# Convergence: UC-7 - Keep the weekly timetable operational after a disruption

## Summary

- Submission: revised `spec/timetable-workspace/checkpoints/UC-7.md` at `433ea70aab9b6d21ff266e9cd411caa754f0b792`
- Verdict: REJECT
- Findings: 1 critical; 0 gap; 0 protocol
- Suite: complete real-Chrome class - 10 run, 0 failed, 0 errors, 0 skipped; clean reactor - 167 run, 1 failure, 0 errors, 0 skipped
- Working tree impact from verification: none

## Protocol Gate

- Exactly UC-7 was `READY_FOR_CONVERGENCE`; no other UC was in progress or ready.
- Original checkpoint is `4acf363`; rejected convergence is `7c76b2e`; the attributable C-1 revision and revised checkpoint are committed at `433ea70`. The pre/post verification worktree is clean.
- UC-1 and included UC-3, UC-4, UC-5, and UC-6 are recorded `APPROVED`.
- The checkpoint covers the main scenario, every extension and guarantee, both postconditions, all relationships, RULE-1/RULE-2 and every UC-7 rule, changed files, commands, and approved-UC regression claims.
- The diff contains one manifest lock-dimension guard, its regression, two composite actor journeys, and checkpoint artifacts. It contains no later-UC behavior or unrelated user change.

## Runtime Reproduction

| Actor | Step or extension | Executor reported | Converge observed |
|---|---|---|---|
| Administrator over HTTP/PostgreSQL/packaged kernel | Inspect, stage teacher outage plus room pin, solve, accept, stage later room outage, solve, accept | PASS | Focused production journey passed. Both 30-second replans returned `FEASIBLE`, independent verification returned `VERIFIED`, exact first/second accepted bundles and direct lineage matched, prior attempt pin was absent, and two `replan` invocations were recorded. |
| Administrator in real Chrome | Complete two-repair native-control journey | PASS | Focused real-Chrome journey passed with both proposals reviewed/accepted and zero browser errors. The later room draft visibly reported zero direct effects and zero attempt pins. |
| Administrator in included UC-4 browser journey | Stage, conflict, resolve, bulk preview/confirm/undo, discard | C-1 revision PASS | The complete 10-test Chrome class passed in actor order with the original 15-second CDP timeout, including UC-7 followed by UC-4. C-1 is resolved. |
| Administrator failure paths | Conflict, failed/cancelled solve, discard, stale proposal, storage rollback | PASS | Clean runs reached and passed the PostgreSQL/HTTP negative matrices before the browser error; logs showed the expected safe cancellation, no-feasible, invalid, internal, transport, interruption, timeout, rejected-output, and terminated-database branches. |

## Evidence Ledger

| Contract element | Executor claim | Evidence obtained | Strength | Verified |
|---|---|---|---|---|
| Main step 1 | Inspect accepted resource context | Focused Chrome opened accepted workspace and focused repair entry; HTTP first compared exact accepted snapshot at `WorkspaceRepairPlanningIT.java:198-202`. | STRONG | yes |
| Main step 2 | Accepted timetable remains unmistakable | Chrome observed `Accepted baseline remains current`; HTTP/PostgreSQL compared the complete accepted bundle during both drafts and proposals. | STRONG | yes |
| Main step 3 | Stage outage and protect room/period assignment | Production POST/PATCH created teacher outage plus room-only pin; assertions at `WorkspaceRepairPlanningIT.java:204-215` prove exact direct set, one pin, no conflict, and unchanged accepted data. | STRONG | yes |
| Main step 4 | Complete feasible proposal without replacement | Focused packaged journey returned 202 twice, polled two exact `REPAIR_PROPOSAL` snapshots, and independently verified both results. | STRONG | yes |
| Main step 5 | Account for changes and explicitly accept | Chrome rendered old/proposed, category, direct/ripple evidence and explicit confirmation; HTTP compared exact proposal definition/result after acceptance. | STRONG | yes |
| Main step 6 | Show result current and reusable | Both boundaries returned to `ACCEPTED_BASELINE`, exposed repair entry, and proved draft/proposal cleanup. | STRONG | yes |
| Main step 7 | Later disruption directly parents accepted result with no prior pins | Assertions at `WorkspaceRepairPlanningIT.java:237-283` prove exact parent revisions, absent prior room lock, second proposal, second acceptance, and two replans. | STRONG | yes |
| Extension 3a | Conflicting pin blocks until resolved | `WorkspaceRepairDraftIT.java:127-149` asserts exact conflict code/lesson, not ready, explicit unpin, then ready; passed in clean runs. | STRONG | yes |
| Extension 4a | Failed/cancelled solve preserves baseline and resumes only with proposal | Failure/retry matrices at `WorkspaceRepairPlanningIT.java:372-458` passed before browser failure and compare exact draft/accepted data with absent candidate. | STRONG | yes |
| Extension 5a | Proposal discard retains baseline and draft | `WorkspaceRepairPlanningIT.java:286-300` passed and compares both complete trees plus proposal absence. | STRONG | yes |
| Extension 5b | Durable acceptance failure retains retryable proposal | Injected PostgreSQL trigger at `WorkspaceRepairPlanningIT.java:340-370` passed: safe 503, same document/version/proposal, successful retry. | STRONG | yes |
| Extension 7a | Later different-room disruption follows same journey | Focused HTTP and Chrome tests selected ROOM, asserted no invented direct effects/pins, solved, reviewed, and accepted. | STRONG | yes |
| G1 | Exactly one identifiable accepted timetable | Lifecycle labels plus exact acceptedBaseline comparisons span every transition in both focused journeys. | STRONG | yes |
| G2 | Actor needs no JSON or CLI | Real Chrome completed the full journey through native forms, lesson controls, review, checkbox, and buttons. | STRONG | yes |
| G3 | Direct and ripple effects stay distinct | HTTP asserts direct-effect IDs separately; Chrome separately observed direct and solver-ripple review sections twice. | STRONG | yes |
| G4 | Subsequent repair uses exact new direct parent | `basedOnRevision` and accepted/proposed timetable identities are compared directly with first accepted result at `WorkspaceRepairPlanningIT.java:257-271`. | STRONG | yes |
| G5 | Failure paths preserve accepted bundle and disclose no partial replacement | Conflict, solve failure, stale proposal, discard, and injected acceptance rollback assertions compare complete authoritative data; safe-error suites reached green. | STRONG | yes |
| Success postcondition | Complete feasible repair is current, intact, ready for next disruption | Final assertions prove exact second FEASIBLE bundle, accepted lifecycle, empty transient state and empty prior-attempt lock manifest. | STRONG | yes |
| Minimal guarantee | Last durable accepted baseline survives every incomplete step | Included negative suites compare exact accepted data at every branch; both clean runs passed them before the unrelated browser transport error. | STRONG | yes |
| Requires UC-1 | Consumes actual approved accepted bundle | Both journeys start from and first compare the production singleton definition/result/manifest accepted bundle. | STRONG | yes |
| Includes UC-3 | Uses production accepted-inspection path | Chrome begins in the whole-school accepted screen; HTTP consumes `/api/workspace` complete snapshot. | STRONG | yes |
| Includes UC-4 | Uses production draft path | UC-7 and approved UC-4 journeys pass together in the independently run complete Chrome class after output backpressure was removed. | STRONG | yes |
| Includes UC-5 | Uses production packaged asynchronous repair | Two real packaged `replan` calls cross POST/poll proposal lifecycle with exact parent/candidate identities. | STRONG | yes |
| Includes UC-6 | Uses production review and atomic decision | Chrome explicitly reviews/confirms; PostgreSQL assertions prove exact atomic advancement and cleanup. | STRONG | yes |

## Rule Conformance

| Rule | Constraint | Evidence | Result |
|---|---|---|---|
| RULE-1 | Separate contract, CLI, workspace modules; process boundary | Clean reactor reached green architecture/dependency checks; diff introduces no kernel linkage. | PASS |
| RULE-2 | Pinned Java/Spring/PostgreSQL stack | Both reactors ran Java 25/Spring 4.1.1/PostgreSQL 18.6; no dependency file changed. | PASS |
| RULE-3 | Singleton JSONB aggregate via JdbcClient | Focused tests query and compare the single `workspace_id=1` document across both cycles. | PASS |
| RULE-5 | Exact lifecycle transitions | Two allowed repair cycles passed; exhaustive lifecycle regressions passed before browser error. | PASS |
| RULE-6 | Matching ETag and conditional SQL | Actor commands use current ETags; missing/stale/race suite passed exact no-mutation assertions. | PASS |
| RULE-7 | Atomic accepted-state changes | Exact two acceptances plus trigger rollback passed; manifest dimension fix prevents second atomic acceptance from crashing. | PASS |
| RULE-10 | Process-only exact packaged replan | Focused launcher recorded two `replan` calls; complete identity and output verification passed. | PASS |
| RULE-11 | One asynchronous run with conditional completion | 202/poll lifecycle passed twice; cancel/late/recovery regressions passed. | PASS |
| RULE-15 | Overlay compiles complete direct successor without accepted mutation | Both resource intents preserve exact baseline; later successor has exact direct parent and no inherited attempt lock. | PASS |
| RULE-16 | Immutable effects/conflicts/pin snapshots | Direct sets, independent room dimension, no-effect room draft, conflict matrix and snapshot matrix passed. | PASS |
| RULE-17 | Identity-bound independently verified proposal acceptance | Two candidates were verified; identity-corruption matrix passed unchanged-baseline refusals. | PASS |
| RULE-19 | Complete server snapshot and native presentation | Complete independent Chrome class passes UC-1 through UC-7 actor journeys and preserves all assertions. | PASS |
| RULE-20 | Accessible English native controls | UC-7 focus/Space/native controls and explicit state text passed; narrow/keyboard regressions reached green except the CDP discard timeout. | PASS |
| RULE-21 | Loopback same-origin session/CSRF security | No routes changed; full security matrix passed before browser phase. | PASS |
| RULE-22 | Stable safe failures | Expected problem-code and disclosure regressions passed. | PASS |
| RULE-23 | Safe observability | Safe run evidence/log capture passed; no logging code changed. | PASS |
| RULE-24 | PostgreSQL-only isolated real-boundary verification | Complete independent Chrome class and real PostgreSQL/packaged-kernel journeys pass; C-1 is resolved. | PASS |
| RULE-25 | Separate scale interaction measurements | Complete Chrome class passes, but the clean reactor records UC-3 day-change p95 282.9 ms under accumulated prior-context load, above 250 ms. | FAIL |
| RULE-26 | Shared typed kernel boundary | Architecture/typed-handler checks passed; no parallel mapper/status switch added. | PASS |
| RULE-27 | Controlled metadata and solver APIs | Kernel metadata/catalog/solver guard suites passed; no source changed. | PASS |
| RULE-28 | Bounded race-safe input/publication boundaries | Missing/malformed/mismatched/late-output matrix passed; exact successful identities passed. | PASS |
| RULE-29 | Reference corpus without universal promise | Kernel corpus passed; claims remain limited to observed independently verified FEASIBLE results. | PASS |

## Related-UC Regression

| Use case | Relationship/shared surface | Evidence | Result |
|---|---|---|---|
| UC-1 | Accepted aggregate, verification, import | Clean suites passed import/verification/security/fidelity cases. | PASS |
| UC-2 | Async process/run lifecycle and acceptance | Clean suites passed initial planning success/failure/cancel/recovery/acceptance cases. | PASS |
| UC-3 | Accepted browser inspection and scale | Complete Chrome class passes, but full reactor day-change p95 is 282.9 ms against the 250 ms target. | FAIL |
| UC-4 | Draft browser, conflict, pin, bulk snapshot, discard | Complete 10-test Chrome class passes after the C-1 output-backpressure fix. | PASS |
| UC-5 | Repair generation and failure matrix | Packaged success/failure/cancel/retry/late completion suites passed. | PASS |
| UC-6 | Review, identity guard, atomic accept/rollback, scale | Review/decision/rollback cases passed; review opening 294.700 ms. | PASS |
| Kernel UC-1/UC-2 | Shared plan/replan/verify contracts and corpus | Kernel modules and corpus passed before workspace browser failure. | PASS |

## Findings

### C-2 CRITICAL - UC-3 validation-scale browser timing is unstable under the complete lifecycle

- Contract: UC-7 includes approved UC-3 at step 1. RULE-25 requires validation-scale post-load interactions to remain below 250 ms p95 and verification to separate them from solving.
- Evidence: the independently run complete `WorkspaceBrowserIT` class passed all 10 tests, including UC-3 day-change p95 below 250 ms. The subsequent independent `./mvnw -q clean verify` reached the same test after several cached Spring/Testcontainers contexts and failed at `WorkspaceBrowserIT.java:542`: day p95 282.9 ms with samples `[129.9,282.9,89.1,10.6,232.8,15.1,14.2,45.9,198.1,66.4,23.3,25.3,7.1,10.3,82.1,91.5,27,316.8,21.2,18.5]`. The reactor ended 167 tests, 1 failure, 0 errors, 0 skipped.
- Why it fails: the required full lifecycle leaves earlier Spring contexts and their stopped-container Hikari pools alive while the browser scale test runs, producing continuous connection retries and scheduler/logging load. The same production UI meets the target when the browser class owns an isolated lifecycle, but related approved-UC evidence is not stable in the standard reactor.
- Revision outcome: close each Testcontainers-backed Spring context after its class so stale pools cannot load later browser measurements; preserve the real browser, 1,000-lesson fixture, 20 wall-clock samples, p95 calculation, and 250 ms threshold, then pass the complete Chrome class and clean reactor.

## Walkthrough

Automated convergence is blocked by C-2, so no administrator walkthrough is requested yet. After the regression is repaired and automated convergence passes, the UC-7 walkthrough must follow the specified two-repair main scenario and later-room extension.

## Status Update

`READY_FOR_CONVERGENCE -> NEEDS_REVISION`; C-1 is resolved and next eligible work is the UC-7 C-2 revision. UC-8 is not started.

## Response to execute

REVISE UC-7: C-2 isolate Testcontainers-backed Spring contexts so the unchanged UC-3 validation-scale p95 gate is stable in the complete clean reactor.
