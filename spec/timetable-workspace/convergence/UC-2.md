# Convergence: UC-2 - Establish the first accepted timetable

## Summary

- Submission: `spec/timetable-workspace/checkpoints/UC-2.md` at `b7180a985063e3c8ef01a79c0e455c5b4dd507f0`
- Verdict: PENDING WALKTHROUGH
- Findings: 0 critical, 0 gap, 0 protocol, 1 cosmetic
- Suite: 135 run, 0 failed, 0 errors, 0 skipped; focused rerun 14 run, 0 failed, 0 errors, 0 skipped
- Working tree impact from verification: none

## Protocol Gate

1. PASS - exactly UC-2 is `READY_FOR_CONVERGENCE`.
2. PASS - implementation, checkpoint, Jev artifacts, tests, and runtime evidence are committed together at `b7180a9` from base `dfe0e75`.
3. PASS - required UC-1 is `APPROVED` at `dfe0e75`.
4. PASS - no other use case is `IN_PROGRESS` or `READY_FOR_CONVERGENCE`.
5. PASS - the checkpoint has rows for all six main steps, eight extensions, six guarantees, both postconditions, the UC-1 relation, and all twenty applicable rules.
6. PASS - inspection of every changed file found only UC-2 initial-planning behavior, necessary shared workspace wiring, tests, and evidence; no unrelated or later-UC behavior is present.

## Runtime Reproduction

| Actor | Step or extension | Executor reported | Converge observed |
|---|---|---|---|
| Administrator | Import and request initial planning | `202`, `SOLVING_INITIAL`, `PT30S`, no accepted timetable | Real HTTP/PostgreSQL rerun reproduced the state, limit, run identity, and non-acceptance |
| Administrator | Review proposal | Complete verified `FEASIBLE` proposal with 2 lessons, `TIME_LIMIT`, and no accepted timetable | Real Chrome test passed; inspected screenshot shows `Initial proposal · feasible`, 2 lessons, `TIME_LIMIT`, `PT30S`, timetable details, and explicit non-acceptance text |
| Administrator | Explicitly confirm and accept | Exact definition/result becomes `ACCEPTED_BASELINE` | HTTP test compared full trees and revisions; real Chrome passed; inspected screenshot shows `Accepted baseline`, `Demo School`, revision, and both assignments |
| Administrator | Cancel and discard | Exact draft retained, no proposal, late completion suppressed | Focused rerun directly compared the definition, proposal absence, `CANCELLED`, and stable later state |
| Administrator | Kernel planning failures | Exact safe diagnostics, exact draft, no proposal or raw process detail | Focused HTTP matrix reproduced invalid input, no-feasible, internal, transport, interruption, and mismatch branches with no acceptance/candidate |
| Administrator | Stale or failed acceptance | Stale proposal invalidated; transient verifier/storage failure keeps exact proposal | Every-identity corruption matrix and trigger-injected rollback passed against PostgreSQL |

## Evidence Ledger

| Contract element | Executor claim | Evidence obtained | Strength | Verified |
|---|---|---|---|---|
| Main step 1 | Review imported summary and request proposal | Real Chrome file input and summary assertions; HTTP begins from production UC-1 `INITIAL_DRAFT` | STRONG | yes |
| Main step 2 | Freeze replacement, stay responsive, packaged 30-second plan | HTTP `202`/`SOLVING_INITIAL`; exact process arguments; browser polling; state-qualified replacement refusal | STRONG | yes |
| Main step 3 | Complete verified feasible result becomes proposal only | Full result/identity assertions, packaged verify invocation, proposal-only lifecycle | STRONG | yes |
| Main step 4 | Show status, count, termination, limit, details, non-acceptance | Real Chrome assertions and inspected proposal screenshot | STRONG | yes |
| Main step 5 | Explicit administrator confirmation | Checkbox-gated accept control and browser click sequence | STRONG | yes |
| Main step 6 | Atomically accept exact bundle and open timetable | Full definition/result/revision comparison, draft/proposal absence, real Chrome accepted view | STRONG | yes |
| Extension 1a | Valid replacement exact; invalid/storage failure preserves prior draft | Multipart HTTP full-tree equality, `422`, unchanged version/document | STRONG | yes |
| Extension 2a | Cancel stops run and restores unchanged draft | HTTP cancel, process graceful/forced termination, late-result suppression, no proposal | STRONG | yes |
| Extension 2b | Restart restores draft without eligible proposal | Durable `SOLVING_INITIAL` recovery plus repository coverage of `INITIAL_PROPOSAL` | STRONG | yes |
| Extension 3a | Named kernel failures show safe diagnostics and retain draft | HTTP/PostgreSQL matrix for all named classes; adapter watchdog/missing/malformed tests; raw diagnostic absence | STRONG | yes |
| Extension 3b | Non-complete/mismatched result discloses no partial timetable | Adapter verifier-not-called/null-result assertions plus HTTP `REJECTED_OUTPUT`, no proposal/acceptance | STRONG | yes |
| Extension 4a | Discard removes proposal and restores draft | Real HTTP exact definition and proposal-absence assertions | STRONG | yes |
| Extension 5a | Every stale identity/result refuses and invalidates | Eleven-corruption PostgreSQL/HTTP matrix with exact draft and accepted-baseline absence | STRONG | yes |
| Extension 6a | Verification/storage failure keeps proposal and no accepted state | Verifier outage and PostgreSQL trigger rollback compare exact version/document | STRONG | yes |
| G1 | Only explicit confirmation creates accepted state | All non-acceptance branches assert false/absent; only accept reaches `ACCEPTED_BASELINE` | STRONG | yes |
| G2 | Feasible is never described as optimal/best/global minimum | Rendered browser label and complete proposal-rendering source inspection | STRONG | yes |
| G3 | Failed search is run-bounded, not impossibility | Exact message assertion and draft/no-proposal state | STRONG | yes |
| G4 | No seed/step controls; actual limit/reason shown | Process argument negatives plus real Chrome `PT30S`/termination assertions | STRONG | yes |
| G5 | Administrator needs neither JSON inspection nor direct kernel invocation | Complete real-browser journey uses only file input and native controls | STRONG | yes |
| G6 | Acceptance is all-or-nothing | PostgreSQL failure trigger proves unchanged proposal/version and absent accepted baseline | STRONG | yes |
| Success postcondition | Exact accepted bundle is inspectable and usable as current baseline | Full HTTP tree/revision assertions and accepted whole-timetable rendering | STRONG | yes |
| Minimal guarantee | Failure leaves no accepted timetable and deliberate retry state | Failure/cancel/recovery/stale/storage matrices compare exact draft or proposal | STRONG | yes |
| Requires UC-1 | Consume approved durable verified initial draft | UC-1 approval plus production import-to-plan HTTP/browser journeys | STRONG | yes |

## Rule Conformance

| Rule | Constraint | Evidence | Result |
|---|---|---|---|
| RULE-1 | Separate modules; packaged process only | Reactor checks, contract archive check, workspace ArchUnit, explicit child-process arguments | PASS |
| RULE-2 | Pinned Java/Spring/PostgreSQL stack; excluded alternatives absent | Clean Java 25 build, dependency/effective-POM checks, PostgreSQL 18.6 containers | PASS |
| RULE-3 | Singleton JdbcClient/JSONB aggregate | Repository SQL and PostgreSQL full-row/full-document comparisons | PASS |
| RULE-5 | Declared lifecycle transitions only | State predicates, conditional SQL, cancel/discard/recovery tests, wrong-state no-process assertion | PASS |
| RULE-6 | Strong ETag/If-Match through conditional SQL | Missing/stale/racing request tests and expected-version SQL predicates | PASS |
| RULE-7 | Accepted-state mutation is one transaction | Transactional revalidation/identity comparison and trigger-injected exact rollback | PASS |
| RULE-8 | Exact canonical kernel-document fidelity | Canonical input bytes and complete definition/result tree equality | PASS |
| RULE-10 | Explicit packaged `plan`; verify every boundary before proposal | Architecture/process tests plus missing/malformed/mismatched HTTP rejection | PASS |
| RULE-11 | One asynchronous pollable run; stale completion loses | `202`, durable polling, single executor/run conflict, run/version/state predicates, late suppression | PASS |
| RULE-12 | 30-second preset, bounded cancel/watchdog, no forbidden controls | Exact arguments, two-second force escalation, watchdog, no proposal | PASS |
| RULE-13 | Private bounded files/channels and cleanup | Permission/path/channel/source inspection, raw-diagnostic capture, empty temp-root assertions | PASS |
| RULE-17 | Complete proposal identity and transactional revalidation | Proposal construction plus eleven-field/result corruption matrix | PASS |
| RULE-21 | Same-origin narrow local surface | CSRF/Host/Origin/CORS/route/credential matrix including UC-2 hostile-origin denial | PASS |
| RULE-22 | Stable safe failure shape and mappings | Exact `403/409/428/503` response assertions and raw diagnostic/candidate absence | PASS |
| RULE-23 | Safe logs and durable solver evidence | Complete log template, captured raw-diagnostic negative assertion, bounded `lastRun` | PASS |
| RULE-24 | PostgreSQL-only real HTTP/browser/package verification | Standard lifecycle ran PostgreSQL 18.6, packaged CLI, and real Chrome; no skips/fallback | PASS |
| RULE-26 | Typed shared kernel handlers and independent verifier | Kernel architecture, sealed-outcome, shared pipeline, and hard-verifier regression suites | PASS |
| RULE-27 | Controlled metadata and public Timefold API | Catalog/manifest exactness and architecture/solver integration regression suites | PASS |
| RULE-28 | Bounded input and atomic race-safe publication | Kernel boundary/concurrency/preservation suites plus workspace result bound/verification | PASS |
| RULE-29 | Release corpus gates release candidates without universal promise | Conditional gate not triggered: submission is not a release candidate and makes no universal claim | PASS |

## Related-UC Regression

| Use case | Relationship/shared surface | Evidence | Result |
|---|---|---|---|
| Workspace UC-1 | Required production import path, aggregate, security, verifier | Full clean reactor reran import HTTP/browser/archive/security/rollback suites | PASS |
| Kernel-v1 UC-1 | Packaged plan and public result contract | Full kernel plan/application/CLI/solver suites | PASS |
| Kernel-v1 UC-2 | Shared loaders, verifier, result and publication boundaries | Full replan/verify/architecture/file-boundary suites | PASS |

## Findings

### K-1 COSMETIC - Checkpoint retains two stale evidence references

The checkpoint validation paragraph says approved regressions passed in the earlier 132-test run, while its immediately
preceding full-suite row correctly records the final 135-test run. A few rule rows also retain pre-expansion line
numbers. The committed tests and independent reproduction resolve the intended evidence unambiguously; this does not
change behavior or proof strength. Correct these references in a later documentation-only cleanup rather than mutate
the immutable submission.

## Walkthrough

Automated browser evidence is strong. User confirmation remains for this UC-derived script:

1. Open the empty workspace, select `examples/initial-school.json`, and confirm `Demo School`, 2 lessons, and `No accepted timetable`.
2. Select `Create 30-second proposal`; while it runs, confirm the workspace remains usable, then observe `Initial proposal · feasible`.
3. Confirm the proposal shows 2 lessons, `PT30S`, a termination reason, timetable details, and `No timetable is accepted yet`.
4. Confirm `Accept as current` is gated by the explicit checkbox; select it and accept.
5. Confirm the opened view says `Accepted baseline` / `Accepted timetable · Demo School` and shows both timetable assignments.

User result: pending.

## Status Update

`READY_FOR_CONVERGENCE` -> `PENDING_WALKTHROUGH`; no next UC may start until the walkthrough is approved.

## Response to execute

PENDING WALKTHROUGH: confirm the five-step UC-2 administrator journey above.
