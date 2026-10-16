# UC-5 execution ledger

Scope: custom saved-definition use only. Base `d55ab636558224f2d33a5e71313e840dd9486428`, preserved dirty `demo` checkout.
No commit requested. `start-source-manifest.json` / `start-git-status.txt` identify the starting boundary.
`production-submission.zip`, its SHA-256, `upload-context-manifest.json` and `uc5-production.patch` bind the 154 production-only inputs and eleven-file production delta from approved UC-4. All other production bytes retain UC-4 hashes.

Implemented: exact raw stored UTF-8 text without browser label, explicit replacement/export guidance, independent 2 MiB and inclusive W/lesson gates before process work, strict duplicate/trailing/object/schema/collection/lineage checks, real initial-definition kernel verification, atomic owner/session/version installation into INITIAL_DRAFT, complete field/order/optional-value retention, safe fixed sample/file/custom origin, summary/default effort, saved-version In use callback using the actual returned snapshot. No use starts a solve or installs Current. Legacy demo definition-only upload routes cannot bypass this path. Normal-profile behavior is retained.

Verification infrastructure for this slice: two standalone slots, bounded slot wait/watchdog/termination, in-memory up-front daily reservations, elapsed physical-process settlement, UTC rollover with outstanding reservations, exact complete-document/mode/kernel-build evidence, default-on bounded cache, fixed one-hour maximum TTL and proactive expiry. Startup packaged sample verification stays outside visitor accounting. Later UCs must route their own work through the shared resource infrastructure; this slice does not approve queueing, coalescing, solving, repair, acceptance, manual publication, bundle verification limits or operational summaries.

## Checks

- `./mvnw -q -pl timetable-workspace -am verify -Dit.test=DemoCustomDefinitionBrowserIT,DemoSessionBrowserIT,DemoSamplesBrowserIT,DemoDefinitionsBrowserIT -Dfailsafe.failIfNoSpecifiedTests=false ...`: final run in `final-regression-1.log`; 107 kernel and 58 workspace unit tests pass. All five selected real Chrome tests pass, zero failures/errors/skips; `browser-summary.json`.
- Focused checks: strict syntax/UTF-8/byte boundaries, exact inclusive independent complexity, null/malformed collections, unsupported/null/ancestry fields; complete cache document/order/optional/mode/result identity, disabled reuse, byte ceiling and expiry; live cache hit at exhausted reservation allowance; two slots/busy refusal, reservation release, actual terminated physical process consumption; UTC rollover; every workspace lifecycle, failed validation, stale completion, ended session non-mutation.
- `node --test tools/demo/definition-storage.test.mjs`: five PASS, `storage-tests.log`.
- `python3 tools/demo/check_packaged_samples.py --output ... kernel-cli/target/school-kernel.jar timetable-workspace/target/timetable-workspace-1.0.0-SNAPSHOT.jar`: PASS, exact three samples/inverse/order/exclusions, 23,664 packaged entries; `artifact-scan.json` / `.log`.
- All 33 packaged static resources match pinned production source: `packaged-static-hashes.json`.
- Local complete journey and actual process/busy/daily/stale-during-verification faults: `local-20261016-3-journey` and `local-20261016-3-faults` both PASS. Final source adds proactive cache expiry and temporary-storage/generic planning-button wording; `local-final-20261016-1-journey` and `local-final-20261016-1-faults` both PASS on the final submission. Affected UC-2 session/restart, UC-3 complete sample, and UC-4 complete storage/repair/import tracking regressions all PASS under their `uc5-regression-20261016-1` directories.
- Public provider preflight: `boundary-before.json` PASS, same isolated Free smoke service, one replica/service, generated domain, no TCP proxy. No platform or financial settings changed.

## Retained findings

The initial focused run was sandbox-blocked from Mockito JVM attachment; the escalated rerun exposed test restubbing of a throwing mock. `doThrow`/`doAnswer` corrects the test harness; final six new tests pass. An initial browser test compilation used the wrong Playwright nested options type; corrected without production change. The first browser run's full-text native fill and failure screenshot timed out; its busy/watchdog checks passed, while the later process-error expectation used different wording from the real safe response. The revised journey edits Full using real keystrokes and opens the exact large document through the real file picker; it still asserts complete unchanged raw request text and installed values. Run 2 had overlapping task-owned Maven runs with the same output location; both process trees were stopped, and all run-2 evidence is invalidated. No result from that run establishes PASS. Run 3 completes both full journeys successfully. All attempt directories/logs remain retained.

Screenshot inspection of the passing initial and narrow layouts found a pre-existing normal-profile sentence promising durable storage in the demo and a fixed one-minute planning button. Demo-only wording now says temporary workspace and Create proposal; the local profile retains its text. UC-5 exposes the initial-planning handoff and correct default metadata. UC-6 still owns actual selectable effort, admission/accounting for solves and generated-proposal behavior; no UC-6 completion is claimed.

## Historical deployment-approval blocker (resolved by explicit user authorization below)

All local work is complete. The attempt to deploy the exact staged production submission to the existing isolated Free smoke service was rejected by automatic approval review before command execution. The reviewer requires trusted explicit user authorization naming this exact external destination and payload. No upload, deployment, restart, provider/financial setting or remote workspace mutation occurred. This rejection is not bypassed. `deployment-approval-block.json` records the exact rejected action and reason.

Pending authority: deploy `production-submission.zip`, SHA-256 `7910c5df9c866cd5c24434bcd90393f3c47572ad4dd51c4164892d54846c0e01`, to `demo-smoke-production.up.railway.app`, project `13d509af-1436-4024-bb64-f9f78fa439fc`, environment `a41a9c0e-ec3f-4ef4-9e08-996d998d1815`, service `672cabae-b54e-4965-bbb2-5236bec81c33`, keeping all existing Free smoke settings unchanged. Then run only the public `DemoCustomDefinitionBrowserIT#customSavedDefinitionJourney`, inspect public screenshots/static hashes/safe process counts, and restart the same deployment to remove disposable visitor work, rechecking readiness/topology. Source and settings must be rechecked before resumption if they drift.

UC-5 remains BLOCKED solely on this public deployment/verification authority; no approval or public PASS is claimed. The existing deployed UC-4 image remains unchanged. Public launch remains separately unqualified under UC-12. No second human product walkthrough or independent convergence gate is introduced. Local proof does not substitute for the specification's real public boundary.

## Authorized public deployment, verification and approval

The user explicitly requested deployment and verification. `authorized-resumption.json` records the exact authority,
archive hash and fresh 154-file source/stage/archive match; the prior rejection remains retained, not bypassed.
The fresh topology preflight passes. `deploy-authorized.log` records the upload from only the pinned production
stage to the existing isolated smoke destination. Deployment `8bc49107-ee22-40f3-9e13-1ee2d5eb6439` is SUCCESS,
image `sha256:77a5d8d450a9d0498234b2f147cffb3dd14a2c82f71480d011147e96ff80cd09`.
Its service manifest equals approved UC-4. No plan, spending, resources or provider settings changed.

Command: `./mvnw -q -pl timetable-workspace -am verify -Dtest=DemoCustomDefinitionTest,KernelVerifierTest
-Dsurefire.failIfNoSpecifiedTests=false '-Dit.test=DemoCustomDefinitionBrowserIT#customSavedDefinitionJourney'
-Dfailsafe.failIfNoSpecifiedTests=false -Ddemo.remote.custom=true
-Ddemo.custom.walkthrough.dir=../output/playwright/demo-deployment/UC-5/public-20261016-1`.
Result: ten focused unit tests and one complete real public Chrome journey PASS, zero failures/errors/skips;
`public-browser-1.log`, `public-test-summary.json`, copied `public-reports/`.
The remote fault method was not run; unchanged local process/configuration fault evidence is reused explicitly.
The [public ledger](../../../../playwright/demo-deployment/UC-5/public-20261016-1-journey/ledger.md) binds steps and exact
API snapshots. Initial-draft, invalid-reference and narrow screenshots were visually inspected: readable layouts,
located refusal beside preserved Current, temporary-storage text and no 390px page overflow all pass.

`public-process-evidence.json` corroborates exactly three startup and three visitor verifier processes (one invalid,
two successful distinct custom definitions), cache reuse without another Full verifier and zero solve processes.
No private browser label, filename or raw definition fields were logged. Only application logs were fetched.
`public-static-hashes.json` matches all 33 public resources to pinned source and packaged JAR.

`cleanup-restart.json` restarts that exact deployment. `cleanup-startup-fresh.jsonl` proves new process startup and
all three sample verifications. `same-image-settings-check.json` proves deployment/image identity and unchanged
service manifest. `cleanup-public-checks.json` passes 40 consecutive stateless health/static reads, nine public
scheme/forwarding/Origin/missing-CSRF/HTTP-redirect probes and all 33 static hashes, with no cookies/new sessions.
`boundary-final.json` passes one-service/replica/generated-domain/no-TCP invariants. Final application events prove
no visitor verification or sample use since restart. Disposable public workspaces/caches are removed; Chrome contexts close.

Retained cleanup finding: attempt 1 looked up Location in a case-sensitive response dictionary. The provider sent
lowercase `location`, with correct 301 and exact HTTPS target. `redirect-diagnostic.json` and
`cleanup-attempt1-finding.json` retain the failure. The corrected harness normalizes header names, including cookie
checks; the complete attempt 2 passes without any production change.

UC-5 **APPROVED**, 2026-10-16. UC-6 remains the effort/solve/admission destination handoff; UC-12/13 public-launch
qualification is unchanged. This is the existing Free smoke service, not a Hobby launch approval. No commit,
operator database, unrelated source, memory, provider configuration or financial setting was changed.
