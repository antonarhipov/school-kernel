# Remaining demo deployment implementation

2026-10-16. Base `d55ab636558224f2d33a5e71313e840dd9486428`, branch `demo`.
Authorized scope: implement remaining use cases and defer every deployment until requested. **Local implementation
PASS; deployed qualification NOT RUN.** No upload, deployment, provider/resource/financial change or commit occurred.
Earlier public approvals, failed attempts and unrelated dirty work remain preserved.

## Product coverage

- UC-7: protected repair conflicts, pins/bulk operations, unavailability undo, reload and confirmed discard.
- UC-8: complete proposal identity/provenance, explicit acceptance, common bounded verification and retry-safe refusal.
- UC-9: verified manual publication, unchanged definition/teacher policy, preserved lock provenance and exact retry.
- UC-10: exact immutable accepted-unit export with explicit exclusion of unfinished work and stale-version protection.
- UC-11: strict bounded archive/pair import, confirmed atomic replacement, common verification and complete round-trip
  provenance after restart. UC-10's continuation handoff is closed.
- UC-12: prepared modern qualification configuration, fictional representative fixtures/cold runner, read-only resource
  collector and concrete operator runbook. Shutdown closes verifier admission, cancels waiting/active calls, reaps
  processes and waits for cleanup/settlement. Full deployed capacity and financial protection remain deferred.
- UC-13: closed safe JSON vocabulary, exactly seven event families/summary groups, effective build/config/startup,
  physical versus logical accounting, queue/cache/allowance/UTC rollover, actual decisions and anonymous milestones.
  Framework diagnostics/poll spam/privacy and shutdown outcomes are covered locally. Provider interpretation remains deferred.

[Current status](../../../../spec/demo-deployment/status.md) and checkpoints UC-7 through UC-13 bind each journey.

## Verification and retained findings

**185 Java unit cases pass across the final commands**: 107 unchanged kernel units from the preceding verification,
78 workspace units on final source. The newly added lifecycle test uses two real processes and a semaphore waiter.
The four event tests cover accounting/privacy/rollover and logical shutdown exactly once.

The [final demo matrix](final-demo-matrix.json) selects the latest passing report per class: **14 packaged Chrome
cases across 12 classes**, including both real initial/repair Thorough searches, process/restart failures, ownership,
queue/cache/cancel, definitions, repair editing, acceptance, manual publication, export/import and safe events. This
combines recorded runs; it does not relabel the earlier broad sweep as green. Three earlier harness errors (new import
selectors and queued DOM timing) were corrected and fully repeated; failed reports/ledgers are preserved.

Final command (BUILD SUCCESS, 14 integration cases, zero failure/error/skip):

```sh
./mvnw -pl timetable-workspace -am verify -Dtest='*Test' -Dsurefire.failIfNoSpecifiedTests=false \
  -Dit.test='DemoVerificationShutdownBrowserIT,DemoObservabilityBrowserIT,DemoManualBrowserIT,WorkspaceManualDraftIT' \
  -Dfailsafe.failIfNoSpecifiedTests=false -Dfailsafe.runOrder=alphabetical \
  -Ddemo.shutdown.walkthrough.dir=../output/playwright/demo-deployment/UC-12/shutdown-final-20261016-1 \
  -Ddemo.observability.walkthrough.dir=../output/playwright/demo-deployment/UC-13/shutdown-final-20261016-1 \
  -Ddemo.manual.walkthrough.dir=../output/playwright/demo-deployment/UC-9/manifest-final-20261016-1
```

[Final log](final-shutdown-manual-1.log), [durable report summary](final-submission-reports/summary.json).
The final command rebuilds contract/workspace; it does not rerun the unrelated kernel module.

[Earlier affected/default regression](default-and-corrected-demo-reports/summary.json) passed all seven actual
PostgreSQL authoring/restart browser cases, eight initial-planning cases, eleven repair-draft cases, four selected
real repair planning/acceptance/stale/rollback cases, and corrected session/definition/operator browser journeys.
It found a new manual-publication 500: the service assumed a legacy accepted unit always had a manifest. The fix
now generates an absent manifest and preserves an existing one. All eleven normal manual cases and the full demo
manual publication/fault/retry/discard journey pass afterward. No new failure is waived.

**Two existing normal-profile import failures remain**:
`WorkspaceImportIT.completeVerificationInvalidityMatrixAtHttpBoundary` and
`WorkspaceImportIT.exhaustiveLifecycleRefusalMatrix`. Both were reproduced on unchanged base before this remaining
implementation pass. See [baseline evidence](../UC-2/20261016-final/ledger.md),
[exact unchanged-base failures](../UC-2/20261016-final/unchanged-base-failures.txt) and retained logs.
The selected full import class is 23 pass / 2 baseline failure; this is not a fully passing repository-wide suite.

Final [SIGTERM actor ledger](../../../playwright/demo-deployment/UC-12/shutdown-final-20261016-1/ledger.md) proves
exact Current before shutdown, unforced application exit within eight seconds after one-second web grace, dead real
verifier PID, removed private directory and one CANCELLED completion. Unit evidence separately proves reservation
release, waiter cancellation, refusal of new calls and idempotent close. Current is not silently accepted/restored.

Final [operator ledger](../../../playwright/demo-deployment/UC-13/shutdown-final-20261016-1/ledger.md) reconciles
four physical starts/completions (one solve, three verified checks), four logical outcomes, coalescing/reuse,
two actual acceptances, one sample open and one export/import. Final reservations/process gauges return to zero;
polls add no activity event; private sentinel names/labels/source IDs/addresses/cookies are absent from logs.
[Reconciliation values](operator-event-reconciliation.json) retain startup context and final aggregate.

Five browser-storage Node tests pass ([log](storage-final.log)); final ES-module syntax and `git diff --check` pass.
The final [artifact/privacy scan](packaged-sample-scan-submission-2.json) inspects 23,682 entries with zero source
identity/reference overlap or packaged examples. All three fictional sample mappings/orderings match exactly.
Real packaged verification of both generated 60/72-cohort definitions is VERIFIED, not cold-search qualification.
Final operator, shutdown and publication screenshots were opened/inspected; prior 390px import/export/queue evidence
is preserved. Tests used only disposable browsers, processes and PostgreSQL data and cleaned them up.

## Exact submission and preservation

- [Starting source hashes](starting-files.json) and [starting status](starting-status.txt) capture the dirty boundary.
- [Final audit](preservation-audit-final.json): all 522 starting paths exist; 29 intentional existing-file changes and
  24 added implementation/test/tool/checkpoint files. Earlier approvals UC-1–6, packaged samples, specification and
  unrelated source retain their starting hashes. [Final file hashes](final-files.json) bind implementation/test/docs.
- [Full tracked patch](final-full-workingtree.patch) includes prior dirty work by design; use starting/final hashes to
  distinguish this pass. It must not be indiscriminately staged as a new feature commit.
- [162-file production manifest](production-manifest-2.json), [source identity](production-source-2.json) and
  [local archive](production-source-2.tar.gz) bind the exact final production inputs. Archive SHA-256:
  `d292a016f6edb85874eb98597643e555159829e2ec3f10a73cc082d46f054dd7`.
  Stage/archive/source match by every file hash; all 49 packaged resources match current source.
  This archive was never uploaded. Earlier archive 1 is retained and superseded because shutdown source changed.
- [Retained runtime artifacts/hashes](final-submission-reports/artifacts.json): workspace
  `9bd1fd14a14be7002bdc3fba03b35cfef1547847d86199b7cff13fee7a30e8ba`, kernel
  `7c7bfc32b667a28ca0c2d368453935a87e6f2066996156767d65275879aff54e`.
- [Source/package fidelity](source-package-fidelity.json) confirms current input/resource identity. Historical container
  images/public deployments are not asserted to contain the new code. No fresh image build/deployment was needed
  for the local packaged process/browser boundary.

## Deferred by user

UC-12/13 remain IN_PROGRESS for deployment-dependent success, despite complete local implementation.
[Operator runbook](../../../../deploy/demo/QUALIFICATION.md) retains one Hobby EU replica, always-on,
$5 workspace alert / $15 hard limit, initial 2-vCPU/3-GB limits, reuse-disabled five-consecutive cold runs for each
sample/representative target, intended 60–72 cohorts/~100 teachers/70+ rooms/2,000–2,500 lessons and Thorough up to
120 seconds. No target is silently reduced. Full-cache/largest-workspace/large-solve/two-standalone-verifier combined
memory/process headroom, platform/egress/spend, public journey/edge/restart and deployed event reconciliation must be
observed later. The modern IaC is prepared, not SDK-typechecked/applied (SDK absent). No local PASS, idle memory,
fixture validity, historical Free service or application daily allowance substitutes for these deployed guarantees.

The authorized implementation scope is complete. No deployment permission question or external mutation is needed now.
