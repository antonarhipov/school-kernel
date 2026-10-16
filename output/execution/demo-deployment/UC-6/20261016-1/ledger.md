# UC-6 execution ledger

Date: 2026-10-16. Feature: demo-deployment. Actor goal: obtain initial/repair proposals within shared demo capacity.
Base: `d55ab636558224f2d33a5e71313e840dd9486428`. No commit requested. Starting dirty files and hashes are in
`starting-status.txt`/`starting-files.json`; `starting-file-preservation.json` proves all unrelated files retained.

Current verdict: **APPROVED**. Explicitly authorized pinned deployment, local/public verification, visual QA and same-image cleanup complete. See the [checkpoint](../../../../../spec/demo-deployment/checkpoints/UC-6.md).
Production payload: `upload-context-manifest.json`, exactly 159 files; `production-delta.json`/`uc6-production.patch`
contain fifteen changed production paths relative to approved UC-5. Archive `production-submission.zip`, SHA-256
`f0b3dd8ccebd5843982dec0c886bebc0cfbea4d2a67e786b60040e7bcde488fe`. No tests, operator files, Git metadata or
credentials are uploaded. Earlier pre-visual-fix and pre-layout-fix archives/manifests/patches are retained with those prefixes and superseded.

## Commands and current evidence

- `./mvnw -q -pl timetable-workspace -am verify -Dit.test=DemoPlanningBrowserIT,DemoCustomDefinitionBrowserIT,DemoSessionBrowserIT,DemoSamplesBrowserIT,DemoDefinitionsBrowserIT -Dfailsafe.failIfNoSpecifiedTests=false -DargLine=-javaagent:/Users/anton/.m2/repository/org/mockito/mockito-core/5.23.0/mockito-core-5.23.0.jar ...`
  with unique UC-specific output properties: `final-regression-3.log` and `final-regression-3-reports/summary.json`.
  **177 unit tests PASS** (107+70). Five affected UC-2/3/4/5 Chrome tests PASS. New UC-6 main Chrome method PASS,
  fault method FAIL due its local one-second overhead override; full Maven verdict FAIL, not concealed.
- Final focused command: `./mvnw -q -pl timetable-workspace -am verify -Dtest=DemoPlanningTest,DemoKernelRunnerTest -Dsurefire.failIfNoSpecifiedTests=false -Dit.test=DemoPlanningBrowserIT -Dfailsafe.failIfNoSpecifiedTests=false -DargLine=-javaagent:/Users/anton/.m2/repository/org/mockito/mockito-core/5.23.0/mockito-core-5.23.0.jar -Ddemo.run.walkthrough.dir=../output/playwright/demo-deployment/UC-6/local-final-20261016-5`.
  `final-regression-5.log`; **PASS**: twelve focused tests and both complete UC-6 Chrome methods, zero failures/errors/skips (`final-regression-5-reports/summary.json`). Browser error logs empty; source hashes unchanged.
- `node --test tools/demo/definition-storage.test.mjs`: five PASS (`browser-storage-tests.log`).
- JavaScript module syntax and `git diff --check`: PASS. Final package exclusion scan PASS: 23,674 entries (`packaged-samples-final-5.json`); all 34 packaged static hashes and 159 source/stage/archive hashes match (`final-payload-audit.json`).
- `python3 tools/demo/check_railway_boundary.py ... --output .../boundary-before.json`: read-only PASS, existing
  service/deployment only, one replica, generated domain and no TCP proxy. No provider setting changed.

Actual boundary: Java Playwright with installed headless Chrome, private local TLS and independent disposable browser
contexts. Each method starts fixed private copies of the packaged workspace and kernel JARs; real controls navigate
and generate proposals. Requests/assertions compare complete Current, compiled intent, exact result/duration and
owner identities. The full main method performs actual 5s and 60s initial and repair searches; faults use explicitly
identified local watchdog/crash/allowance/reuse configuration. Controlled unit evidence is separately identified for
queue estimate/count, expiry, stale completion, two-second forced reaping, 120s arguments and UTC accounting.
No standalone Playwright skill is installed; the execute workflow's real journey/ledger requirement is fulfilled by
these recorded actor methods and screenshot inspection. No independent audit or human walkthrough is claimed.

## Retained attempts and corrections

1. `compile-tests-1.log`: production/tests compiled; sandbox dynamic Mockito attachment failed. Added explicit
   Mockito javaagent for subsequent checks.
2. `focused-tests-2.log`: illegal compound `var` test declarations fixed. `focused-tests-3.log`: sandbox sysctl/process
   descendants restriction and Mockito re-stubbing a blocking Answer; interrupted run retained. `doAnswer` and an
   escalated local process check resolve it. `focused-tests-4.log`: eighteen then-current focused checks PASS.
3. `package-1.log`: `verify -DskipTests` still invoked this repository's Failsafe configuration. Docker unavailable
   and browser sandbox failures are environmental, not valid product regression or passing evidence. The accidental
   timestamped UC-6 output directories remain retained.
4. `browser-1.log`, `local-20261016-1-{journey,faults}`: safe crash wording and repair acceptance-control selectors in
   the new harness were wrong. Actual initial Quick/Thorough and repair Quick passed before harness failure. Both
   attempts retain overall FAIL/screenshots. A failure-record helper initially said assertions passed in the failure
   detail; overall FAIL was correct and helper wording was subsequently fixed.
5. `all-unit-tests-1.log`: a test anonymous-class brace was missing; corrected. `final-regression-1.log` and `-2.log`:
   one old privacy fixture fabricated RUNNING state without a coordinator computation; switched to a terminal owned
   CANCELLED lastRun. Actual active-run ownership is separately tested through coordinator and real Chrome. Both
   failed before browser execution. Status contains an explicit correction of an initial mistaken command diagnosis.
6. `final-regression-3.log`: all units and affected UCs pass; full new main method passes. The fault method's one-second
   local overhead persisted into no-feasible checks, timing out real JVM startup. Use the production ten-second
   overhead throughout the fault method; actual stalled process still proves watchdog termination. Visual inspection
   of `04-queued.png` found Queued beside legacy Status Running. Final production removes the conflicting demo field,
   makes repair headings phase-neutral, preserves narrow live status and corrects effort explanation wording. All
   normal-profile status text remains on its existing path. The next full UC-6 journey asserts no duplicate Running
   label while queued. Fault browser errors are now also explicitly collected/asserted.
7. `final-regression-4.log`: twelve focused checks PASS and full fault journey PASS; main method interrupted after
   initial coalescing/reuse when visual QA revealed two further defects: legacy one-minute repair action despite
   selected Quick, and opened failure utilities reducing the school heading to a narrow column. Final demo action
   is effort-neutral; opened utilities span a full row. Browser assertions check both the label and heading geometry.
   The incomplete main attempt remains retained and never counts as overall PASS. Final complete repetition is run 5.

Every failed attempt, log, screenshot and ledger remains available. No failing attempt has been overwritten by a
passing one. No further production changes are planned unless final evidence reveals an actual defect.

Final run-5 main/fault ledgers are under `output/playwright/demo-deployment/UC-6/local-final-20261016-5-{journey,faults}/`.
Both PASS. `visual-qa.md` records opened/inspected final screenshots; queue contradictions, one-minute action wording
and collapsed failure headers are resolved. Same-image local restart ends an active solve/session and retains saved
browser definitions. Final package scan/static/source/stage/archive audits all pass. The public boundary is pending.

## Fidelity, cleanup and scope

New proposals leave complete Current and draft intent unchanged. Completion requires active owner/version/run/input
guards; cache/coalescing supplies immutable kernel evidence, never another owner's authority. Failed/refused calls
preserve snapshots by value. Local browser contexts close and test services/processes stop; forced process tests
assert actual elapsed settlement and private-file cleanup. Public disposable-state cleanup remains pending until
authorized deployment and public verification. No operator database or school files are used.

UC-7/8 are destination handoffs only, particularly initial acceptance's legacy one-minute identity and integration
of remaining operation verification with reserved accounting. UC-12 Hobby/capacity/spend qualification remains blocked.
The existing Free smoke settings and resource/workspace cap must remain unchanged. No UC-6 public approval, feature
completion, later UC, commit, financial/provider configuration or memory change is claimed at this stage.

## Public deployment authorization blocker

Automatic approval review rejected `railway up` before the command executed. Exact reason: “This uploads 159 files of potentially sensitive production source to an external Railway service and mutates that deployment, but the trusted user request only authorized implementing UC6, not sending this payload to this destination.”
The rejected action was the exact 159-file production stage, archive SHA-256
`f0b3dd8ccebd5843982dec0c886bebc0cfbea4d2a67e786b60040e7bcde488fe`, to the existing isolated service
`672cabae-b54e-4965-bbb2-5236bec81c33` at `demo-smoke-production.up.railway.app`. See
`deployment-approval-block.json`. No upload, remote deployment, session mutation, public visitor process or provider
configuration change occurred. Previous UC-5 deployment remains the last observed public image.

All unaffected work is complete: implementation, final local actor journeys, focused/full units, affected-UC regressions,
visual QA, package exclusions, exact static/payload fingerprints, reviewable checkpoint and preserved starting changes.
No safe local alternative can supply the specification's required public boundary. No workaround/retry is attempted.
After explicit authority, verify source/stage/archive drift, deploy this exact stage, run only the remote main method
using two disposable visitors at the existing workspace cap, inspect public static hashes/safe process events, restart
this same image to remove disposable state and recheck health/topology. Keep all provider/resource/spend settings.
Only then approve UC-6; UC-7/8/12 remain outside this completion claim.

## Authorized public resumption

The user explicitly authorized deployment on 2026-10-16. `authorized-resumption.json` records the scope and passing
159-file source/stage/archive audit. `boundary-authorized.json` rechecks the isolated one-service/replica topology.
The historical rejection above is resolved; no workaround was used. The exact reviewed stage uploaded successfully
(`deployment-upload-authorized.log`) as deployment `a3ebd477-0aa8-46ee-a8b5-38d33d35b78a`, image
`sha256:6bbb29d521a633a0934a8066533b807e79e63e2f92c7c3ee7f8218c5c605b084`.
`deployed-identity.json` verifies SUCCESS and exact service-manifest equality to approved UC-5; no provider variables,
resources, plan or spending settings changed. Fresh startup performs three successful packaged sample verifications.
`public-static-hashes.json` verifies UP and all 34 exact static resources without creating a session.

The complete public main actor method is now running via:
`./mvnw -q -pl timetable-workspace -am verify -Dtest=DemoPlanningTest,DemoKernelRunnerTest -Dsurefire.failIfNoSpecifiedTests=false '-Dit.test=DemoPlanningBrowserIT#realInitialAndRepairPresetsSharingCancellationReuseQueueAndRestart' -Dfailsafe.failIfNoSpecifiedTests=false -DargLine=-javaagent:/Users/anton/.m2/repository/org/mockito/mockito-core/5.23.0/mockito-core-5.23.0.jar -Ddemo.run.base=https://demo-smoke-production.up.railway.app -Ddemo.run.walkthrough.dir=../output/playwright/demo-deployment/UC-6/public-20261016-1`.
Only two disposable visitors are opened; remote branch reuses the second owner for distinct queued work, retaining
the existing two-workspace cap. The real active-run restart remains passing local evidence; public same-image
operator cleanup follows this browser journey. `public-browser-1.log` and `public-20261016-1-journey/` retain outcomes.

## Final UC-6 approval

**APPROVED**, 2026-10-16. The complete public Chrome method passes with normal TLS, two private visitors and all
actual initial/repair Quick/Thorough computations. `public-test-summary.json` records twelve focused tests and one
selected browser method, all zero failures/errors/skips; page-error log empty. Existing 177 full units, both complete
final local methods and five affected UC regressions remain valid and retained. Public screenshots were inspected
(`public-screenshot-inspection.json`); provenance, Current distinction, queue text and 390px layout pass.

`public-process-evidence.json` corroborates four feasible distinct computations, two shared subscriptions and two
artifact hits, one queued cancellation without a solve, plus ten successful verification commands (three startup,
seven visitor). No browser labels, owner/session credentials or definition payloads occur in fetched application logs.
Provider HTTP/request logs were not fetched. No production source changed during authorized resumption.

The authorized restart returned the same deployment ID, `a3ebd477-0aa8-46ee-a8b5-38d33d35b78a`, same image digest
and exact service settings. `cleanup-startup-fresh.jsonl`, `cleanup-checks.json` and `deployment-final.json` show fresh
sample verification and no visitor work after restart; both disposable in-memory workspaces/jobs/cache are removed.
`cleanup-public-checks.json`: 40 consecutive stateless reads, nine edge probes and all 34 static hashes PASS, no cookie.
`boundary-final.json`: one isolated service/replica, generated domain, no TCP proxy. No provider/resource/spend/variable
configuration changed. Scope ends at UC-6; no UC-7/8/12 approval, global feature completion or Git commit is claimed.
