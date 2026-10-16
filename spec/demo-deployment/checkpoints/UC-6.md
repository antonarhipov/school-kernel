# UC-6 checkpoint

Status: **APPROVED**, 2026-10-16. Implementation, technical checks, complete local/public Chrome journeys,
visual inspection and same-image cleanup pass. The user explicitly authorized the pinned deployment.
Requires approved UC-2. UC-5's real verified initial draft and the existing ordinary repair compiler supply actual
preconditions. Scope ends at obtaining/opening private verified proposals; no later UC is approved.

Base `d55ab636558224f2d33a5e71313e840dd9486428`, preserved dirty `demo` checkout. No commit requested or made.
[Execution ledger](../../../output/execution/demo-deployment/UC-6/20261016-1/ledger.md),
[starting status](../../../output/execution/demo-deployment/UC-6/20261016-1/starting-status.txt),
[starting file hashes](../../../output/execution/demo-deployment/UC-6/20261016-1/starting-files.json),
[preservation audit](../../../output/execution/demo-deployment/UC-6/20261016-1/starting-file-preservation.json),
[exact production patch](../../../output/execution/demo-deployment/UC-6/20261016-1/uc6-production.patch), and
[159-file production manifest](../../../output/execution/demo-deployment/UC-6/20261016-1/upload-context-manifest.json)
bind the submission. Archive SHA-256: `f0b3dd8ccebd5843982dec0c886bebc0cfbea4d2a67e786b60040e7bcde488fe`.
The archive excludes tests, operator files, credentials, source examples, Git metadata and earlier evidence.

## Implementation and contract coverage

Exactly fifteen production files differ from the approved UC-5 staging tree. Five new production files implement
the shared artifact cache, validated effort/queue settings, physical kernel boundary, coordinator and demo run UI.
Ten existing files integrate limits, allowance, verification, workspace lifetime, safe failures, routes, configuration
and rendering. The [hash delta](../../../output/execution/demo-deployment/UC-6/20261016-1/production-delta.json)
lists every path. Three new test files cover coordinator/cache, real process integrity/termination and real browser
journeys. Two existing test fixtures adapt the verifier callback and terminal-run privacy fixture. Deployment
documentation records the actual effort and reservation settings; all unrelated starting changes are preserved.

| Contract | Implementation and evidence |
|---|---|
| Main 1–3; 2c; G1 | Server-built complete initial/compiled repair input; lifecycle/version/owner guards; inclusive independent lesson/W and byte limits; Quick 5s/Thorough 60s, configurable through 120s. Real process argument tests bind 5/60/120 for both operations. No arbitrary visitor limit/seed/retry. |
| Main 3–6; G2/G3/G8 | One FIFO physical slot covers solve plus independent result verification; queue gates count distinct work and approximate start; verification allowance reserved before admission. Status distinguishes queued/running/checking, with approximate refreshed estimates. |
| 2a/2b; G3/G5 | Complete documents, array order/optional presence, baseline/result, operation/duration/seed/config/build identify immutable verified reuse and coalescing. Shared byte ceiling and fixed completion TTL, default-on configurable-off switch. Fresh private run/proposal IDs and current owner/version guards remain independent. Original process durations are separate from request wait. |
| Main 5–7; 5a–5c; G8 | Strict bounded kernel output and configured solve evidence are checked, then complete local verification precedes publication. Real Quick/Thorough for initial and repair; impossible initial/repair, crash and watchdog paths retain drafts/Current and publish no candidate. Only verified feasible artifacts are cached. |
| 3a/3b; G7/G8 | Queue refusal 429 with Retry-After 5; complete reservation refusal before run/mutation/process. Actual process lifetimes transfer reserved to consumed, including failures/cancellation/verification, UTC rollover and unused release. Reuse works at exhausted allowance; packaged samples remain independent. Default maximum reservations: Quick 53s, Thorough 108s. |
| 4a/4b/6a; G5/G6 | Logical cancellation/expiry detaches one subscriber; last departure removes waiting work or interrupts physical execution. Two-second graceful stop then forced termination; reservations held until reaped. Tests cover checking cancellation, stale/version/late completion, queued abandonment, ended owners and shutdown. |
| G4 | About 1s owner-only status polling, 2.5s queued backoff; no repeated school/CSRF fetch. Terminal response triggers one full snapshot. Status stays small and excludes other subscribers and input. Stable stored snapshots allow exact non-mutation checks. |
| 4c/G9; minimal | Real packaged restart ends active run/session, explains loss and retains browser-local definitions. Exact Current and repair intent remain unchanged for proposals, refusals and cancellation. Narrow read-only inspection remains available. |

## Verification and retained findings

- **177 Java unit tests PASS**: 107 kernel + 70 workspace; zero failures/errors/skips in final-regression-3.
  [Copied reports](../../../output/execution/demo-deployment/UC-6/20261016-1/final-regression-3-reports/summary.json)
  preserve the full result before later focused runs. This command's overall verdict is FAIL because one new browser
  fault setup used insufficient local process overhead; its unit and affected-UC results remain independently valid.
- Five affected UC-2/3/4/5 real Chrome tests PASS in that same packaged run:
  [sessions/restart](../../../output/playwright/demo-deployment/UC-2/uc6-regression-20261016-3/ledger.md),
  [samples](../../../output/playwright/demo-deployment/UC-3/uc6-regression-20261016-3/ledger.md),
  [browser storage/real repair acceptance/import](../../../output/playwright/demo-deployment/UC-4/uc6-regression-20261016-3/ledger.md),
  [custom-use journey](../../../output/playwright/demo-deployment/UC-5/uc6-regression-20261016-3-journey/ledger.md),
  and [custom verification faults](../../../output/playwright/demo-deployment/UC-5/uc6-regression-20261016-3-faults/ledger.md).
  Subsequent production changes are confined to phase-neutral demo solve labels and effort explanation copy; the
  complete UC-6 browser walkthrough is repeated for those changed rendering paths. Visual QA additionally corrected
  the repair action's legacy one-minute label and opened utilities' collapsed heading; run 5 asserts both geometry
  and truthful effort-neutral action wording.
- Five browser-storage checks PASS; JavaScript module parsing and `git diff --check` PASS.
- Final packaged fictional-sample/exclusion scan PASS: **23,674 entries**; [result](../../../output/execution/demo-deployment/UC-6/20261016-1/packaged-samples-final-5.json).
  All **34 static resources** match source and the packaged JAR; all 159 source/stage/archive inputs match
  [the final audit](../../../output/execution/demo-deployment/UC-6/20261016-1/final-payload-audit.json).
- Earlier compilation, Mockito attach, sandbox/Docker/process and harness failures are retained and explained in
  the execution ledger. No failed command is labelled a passing suite. Screenshot inspection found an actual
  legacy Running label beside Queued; final rendering removes it and adds a specific browser assertion.

The final [complete UC-6 local journey](../../../output/playwright/demo-deployment/UC-6/local-final-20261016-5-journey/ledger.md)
and [complete fault journey](../../../output/playwright/demo-deployment/UC-6/local-final-20261016-5-faults/ledger.md)
**PASS** against the final source. Twelve focused coordinator/process checks also PASS, zero failures/errors/skips;
[final reports](../../../output/execution/demo-deployment/UC-6/20261016-1/final-regression-5-reports/summary.json).
Both browser error logs are empty. Final queue/reuse/repair/narrow/failure screenshots were opened and inspected:
[visual QA](../../../output/execution/demo-deployment/UC-6/20261016-1/visual-qa.md). Local contexts and test services close;
actual restart exposes ended-session guidance while keeping browser-local saved definitions.

## Public boundary and destination handoffs

The user explicitly authorized deployment and verification after the initial automatic-review rejection. The
[historical rejection](../../../output/execution/demo-deployment/UC-6/20261016-1/deployment-approval-block.json)
is retained; [authorized resumption](../../../output/execution/demo-deployment/UC-6/20261016-1/authorized-resumption.json)
records its resolution and the passing 159-file source/stage/archive checks. No authority blocker remains.

The exact pinned production stage deployed to https://demo-smoke-production.up.railway.app as
`a3ebd477-0aa8-46ee-a8b5-38d33d35b78a`, image
`sha256:6bbb29d521a633a0934a8066533b807e79e63e2f92c7c3ee7f8218c5c605b084`.
The [complete public Chrome journey](../../../output/playwright/demo-deployment/UC-6/public-20261016-1-journey/ledger.md)
**PASS** uses two isolated disposable visitors and normal public TLS validation. Both actual 5-second Quick and
60-second Thorough efforts pass for initial and repair proposals. Sharing/private IDs, subscriber cancellation,
small owner-only polling, distinct queue position/approximate estimate, initial/repair artifact reuse, large-school
warning, exact Current/intent preservation and 390px read-only inspection pass. Twelve focused coordinator/process
checks pass again alongside this selected public method, with zero failures/errors/skips;
[reports](../../../output/execution/demo-deployment/UC-6/20261016-1/public-test-summary.json). Page-error log is empty.
Queue/reuse/repair/narrow public screenshots were opened and visually inspected:
[inspection](../../../output/execution/demo-deployment/UC-6/20261016-1/public-screenshot-inspection.json).

[Safe process events](../../../output/execution/demo-deployment/UC-6/20261016-1/public-process-evidence.json)
corroborate four distinct verified feasible computations, two coalesced subscriptions, two artifact reuses and a
queued cancellation without a solve. Ten verification commands succeed: three startup and seven visitor commands.
Fetched application logs contain no browser labels/definition payloads; provider HTTP logs were not fetched.
All 34 served assets match pinned source/package before and after cleanup.

The authorized cleanup restart retains the **same deployment/image** and exact service manifest. Fresh startup
verifies all three samples; zero visitor work/sample opens occur after restart. Both disposable owners/caches/jobs
are cleared. [Cleanup checks](../../../output/execution/demo-deployment/UC-6/20261016-1/cleanup-checks.json),
[40 stateless reads, nine edge probes and 34 exact assets](../../../output/execution/demo-deployment/UC-6/20261016-1/cleanup-public-checks.json),
and [final topology](../../../output/execution/demo-deployment/UC-6/20261016-1/boundary-final.json) PASS. Cleanup creates
no new session. Provider plan, resources, spending, variables, sleeping and workspace cap remain unchanged. This is
the existing isolated Free smoke service; UC-12 Hobby launch and large-school capacity remain separate qualifications.

UC-6 is approved from observed automated evidence. No additional human walkthrough or independent convergence gate
applies. Public main method skips infrastructure restart injection; the passing real local active-run restart and
fault/reservation/expiry checks supply that required technical evidence, with same-image public cleanup observed above.

UC-7 owns complete repair authoring. UC-8 owns acceptance integration with new effort/provenance and common reserved
verification. In particular, the legacy initial acceptance service still expects its old one-minute run identity;
Quick initial acceptance is not claimed complete here. Existing repair acceptance regression passes, but its verifier
integration remains a later-UC handoff. All newly implemented solves/result checks and custom-use verification obey
the shared accounting; global completion of every visitor command is not claimed. UC-12/13 and launch blockers remain
unchanged. No memory, operator school data, unrelated changes or Git commit is modified by this work.
