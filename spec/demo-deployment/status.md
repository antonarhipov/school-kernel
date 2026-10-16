# Demo deployment execution

Base: `d55ab636558224f2d33a5e71313e840dd9486428` (`demo`).
Pre-existing changes: `spec/README.md` modified; `spec/demo-deployment/proposal.md` and `spec.md` untracked.
These specification inputs are preserved. No commit has been requested. The operator has authorized an isolated
Railway Free-plan deployment attempt; this does not authorize a plan upgrade or qualify public launch.

Current deployment: the user-authorized timetable-first experience is live on the existing Free demo as
`7af6ff33-8d8a-41ae-9022-4daf4d112d5c`, with unchanged service/runtime settings and confirmed FREE plan. Its complete
public Chrome, exact named save, Quick generation, 390px layout, restart/browser retention, static/sample and edge
checks pass. The compact retro matrix styling was restored at the user's request on 2026-10-17.
[Current deployment ledger](../../output/execution/demo-timetable-experience/retro-20261017-1/ledger.md),
[current feature status](../demo-timetable-experience/status.md). The older editor/import/repair UI contract and its
UC-11 public heading finding below are historical and superseded by that explicitly approved experience; their
evidence is retained, without claiming a rerun of removed journeys. UC-12/13 remain `IN_PROGRESS` for Hobby
capacity/spend/operator qualification. No full public-launch approval.

| UC | Status | Local implementation | Requires |
|---|---|---|---|
| UC-1 | APPROVED | Complete | None |
| UC-2 | APPROVED | Complete | None |
| UC-3 | APPROVED | Complete | UC-2 |
| UC-4 | APPROVED | Complete | UC-2 |
| UC-5 | APPROVED | Complete | UC-4 |
| UC-6 | APPROVED | Complete | UC-2 |
| UC-7 | APPROVED | Complete | UC-2 |
| UC-8 | APPROVED | Complete | UC-6 |
| UC-9 | APPROVED | Complete | UC-2 |
| UC-10 | APPROVED | Complete | UC-2 |
| UC-11 | NEEDS_REVISION | Historical import walkthrough finding; UI superseded by timetable experience | UC-2 |
| UC-12 | IN_PROGRESS | Complete; deployed qualification deferred | None |
| UC-13 | IN_PROGRESS | Complete; deployed qualification deferred | UC-12 |

## Evidence

- 2026-10-16: Read the complete specification and execute workflow. Relationship targets exist and the explicit
  dependency graph is acyclic. UC-1 is the first eligible slice. Its packaged CLI boundary requires no browser.
- Deployment qualification (UC-12/13) requires the operator's Railway Hobby service and spending configuration.
  Local checks will not be represented as deployed capacity or public-launch approval.
- 2026-10-16: UC-1 approved from the [packaged execution ledger](../../output/execution/demo-deployment/UC-1/20261016-2/ledger.md)
  and [checkpoint](checkpoints/UC-1.md). Next: UC-2. Storage abstraction and explicit owner propagation are enabling
  work for its session boundary; the local PostgreSQL implementation remains the non-demo profile.
- 2026-10-16: UC-2 implementation passed the full **local** packaged TLS/two-browser journey and 39 workspace unit
  tests. See its [checkpoint](checkpoints/UC-2.md), [technical ledger](../../output/execution/demo-deployment/UC-2/20261016-final/ledger.md)
  and [passing browser ledger](../../output/playwright/demo-deployment/UC-2/1792140998412/ledger.md).
  Browser findings (draft-state export, narrow JSON editing and ended-session CSRF/authoring guard) were fixed;
  failed attempts are retained. The normal-profile HTTP regression has 23 passes and two failures reproduced on
  unchanged base `d55ab636`; they are not introduced regressions.
- **Historical blocker, resolved by the UC-2 approval below:** The isolated Free deployment passed startup, sample verification and a real public
  two-owner Chrome smoke. The subsequent operator restart exposed intermittent public health 403s (13/15 requests
  200, 2/15 refused) despite provider SUCCESS. The empirically observed exact proxy-peer allowlist is insufficient
  for reliable access across restart. A stable, justified provider trust boundary and repeated restart/browser
  evidence remain required. SSH gateway trust was explicitly approved by the operator; its temporary access key
  has been revoked and local key files removed. UC-2 and UC-12 remain unapproved; dependent UCs have not advanced.
- 2026-10-16: Independently eligible UC-12 has concrete [container/configuration preparation](../../deploy/demo/README.md)
  and a [checkpoint](checkpoints/UC-12.md). The pinned non-root image builds from a production-only context; its
  read-only-container [smoke ledger](../../output/execution/demo-deployment/UC-12/1792141341654519000/ledger.md)
  records all three real startup verifications, safe HTTP/header refusal, direct SIGTERM and cleanup. The artifact
  scan found zero source school/teacher name matches in 53 packaged text resources. Railway configuration fields
  match the live schema checked on this date. No deployment, financial change or capacity qualification occurred.
  UC-12 is BLOCKED on the same unavailable Railway boundary and its later-use-case launch prerequisites.
- Enabling work does not approve UC-3/4/10: three fictional sample pairs and local JSON save/download are present;
  custom stored-version use is disabled pending UC-5. UC-6's bounded queue, reuse/coalescing, daily reservations and
  visitor verification limits are not yet implemented. UC-7/8/9/11/13 still need their full feature-specific
  implementation and journeys. Do not expose this intermediate build as the public demo.
- 2026-10-16: The operator authorized a first Free-plan smoke test. A separate
  [smoke configuration](../../deploy/demo/railway-free-smoke.ts) preserves the 2-vCPU/3-GB qualification configuration.
  The [512-MB/one-CPU local preflight](../../output/execution/demo-deployment/UC-12/1792147970154196000/ledger.md)
  passed startup, three real packaged sample verifications, health/security refusal and shutdown with 128-MiB web /
  96-MiB verifier heaps. Idle memory observed after startup was 213.6 MiB; startup peak and deployed capacity are not
  established. A production-only CLI upload context has 150 files (1,681,314 bytes), with its hash manifest retained
  beside that ledger. No deployment or billing change occurred; browserless CLI authorization is pending.
- 2026-10-16, subsequent deployed attempt: CLI authorization succeeded. Created only `school-kernel-free-smoke` /
  `demo-smoke`; existing unrelated project preserved. Deployment `a8ceaa5c-4141-4835-bcab-58f4cfc7d4ce` is SUCCESS
  in `europe-west4-drams3a`, one CPU / 500,000,000 bytes / one replica, sleeping enabled as Free requires.
  All three packaged samples verified; startup completed in 8.067 s. Recorded platform memory samples reached
  0.202960896 GB and settled around 0.1688 GB; this is not continuous peak or solver capacity qualification.
  The actual upload had 149 production-only files (1,680,788 bytes); no legacy config, tests, examples, operator data
  or credentials were uploaded. See [deployed evidence](../../output/execution/demo-deployment/UC-12/railway-free-20261016-1/ledger.md).
  The API rejects new legacy config-file assignments, and Free requires sleeping. Effective settings were applied
  through supported service APIs and exported as IaC; SDK absence prevented IaC plan execution. No IaC apply, plan
  upgrade or billing mutation occurred. UC-12 remains unqualified; the full specification retains Hobby/always-on.

- 2026-10-16, first Free-test conclusion: [fixed-image/restart ledger](../../output/execution/demo-deployment/UC-12/railway-free-20261016-3/ledger.md)
  records the trusted matching X-Forwarded-Host fix, 8 focused passing tests, 29/29 modules available, real
  [two-owner Chrome smoke PASS](../../output/playwright/demo-deployment/free-smoke/20261016-5/ledger.md), and the
  post-restart public-access failure. Startup verifies all three exact fictional sample pairs. Cgroup peak during
  startup/sample smoke was 207,659,008 bytes; this proves no solve/full-load capacity. No plan or billing change
  occurred. Earlier CLI/SSH approval blockers are resolved. The isolated service remains sleeping-enabled and
  intermittently refuses access; do not represent the passing pre-restart browser journey as reliable deployment
  qualification. Temporary SSH access and disposable browser/workspace state have been cleaned up.

- 2026-10-16, UC-2 resumed on the same base/dirty checkout at the user's request. The previous failed restart
  evidence is retained. Railway's [staff-confirmed boundary](https://station.railway.com/questions/fastapi-uvicorn-trusted-proxy-configura-9c06fa12)
  explicitly provides no stable proxy CIDR. Use an opt-in isolated project/environment with exactly one service and
  no TCP proxy, after read-only provider preflight; never widen the empirical address list. See [technical rules](rules.md).
  Current provider inspection shows one service/replica and zero TCP proxies. Public lifecycle/restarts remain to run.

- UC-2 resumed technical checks PASS: 46 workspace unit tests in 12 classes plus the complete packaged local TLS
  browser journey. New deployment `99089c8b-928e-4a53-93a7-f7a414339bd1` is SUCCESS; 149 production-only inputs
  were uploaded. Isolated ingress is explicit, default-off and bound to Railway identity/generated domain.
  Public lifecycle, repeated provider restart, spoof and expiry walkthroughs are running before approval.

- Resumed public UC-2 journey PASS on [attempt 3](../../output/playwright/demo-deployment/UC-2/public-20261016-3/ledger.md):
  two independent owners plus a stale shared-owner tab, exact export/import, private draft/run/cancellation,
  declined/active/stale refusals, capacity UI, two real provider restarts and 40 consecutive health/static checks
  after each. [Recovery/privacy diagnostic](../../output/playwright/demo-deployment/UC-2/ended-diagnostic-20261016-2/ledger.md)
  also passed 20 browser/API reads, reload, complete disclosures/headers and stale-clear non-mutation.
  Earlier public attempts failed because the harness sampled asynchronous session rotation before completion;
  explicit awaited state polling fixed the harness, with no application session change. Failed evidence is retained.
  The 73 stateless public edge/spoof probes passed. Short-clock deployed expiry and exact default restoration remain.

- 2026-10-16, UC-2 **APPROVED**. The [resumed execution ledger](../../output/execution/demo-deployment/UC-2/20261016-resume-1/ledger.md)
  and [final checkpoint](checkpoints/UC-2.md) bind the deployed production inputs and exact four-file ingress fix.
  The public two-owner/repeated-restart walkthrough, recovery/privacy/stale-clear supplement and
  [deployed idle/absolute expiry walkthrough](../../output/playwright/demo-deployment/UC-2/public-expiry-20261016-3/ledger.md)
  all pass. Polling extends idle activity but does not defeat absolute expiry; no work is recovered or auto-accepted.
  Normal 60-minute/8-hour settings and the two-workspace smoke cap were restored on deployment
  `1fc1827a-71d6-4d12-8c18-9e20545fd22a` (same image). Final topology/config checks and another 73 stateless
  edge/security probes pass. The final packaged local browser regression passes; 46 workspace unit tests pass.
  The earlier proxy-peer failure is superseded by RULE-1's explicit, audited isolated-ingress mode, not erased.
  Scope ends at UC-2: later visitor UCs are unchanged; UC-12 remains BLOCKED on Hobby/always-on, measured capacity,
  spending controls and later-use-case launch prerequisites. No commit or financial setting change was made.

- 2026-10-16: UC-3 started on base `d55ab636` with approved UC-2. The [starting manifest](../../output/execution/demo-deployment/UC-3/20261016-1/start-source-manifest.json) and status/diff beside it preserve the pre-existing dirty boundary. Scope is bundled sample preview/use, replacement safety, artifact/scoring/anonymization checks, and public inspection/repair/export entry points. Later destination workflows retain their own approval gates.

- UC-3 walkthrough attempt `local-20261016-3` found a missing-confirmation request returning 500 (Jackson strict boolean access); exact Current/draft non-mutation passed. Status `NEEDS_REVISION` until fixed and re-walked. The controller now requires an actual JSON boolean before reading it. The failed ledger/screenshots are retained.

- UC-3 technical/local-browser checks PASS after the confirmation fix: 52 workspace and 107 kernel unit tests;
  exact inverse/order reproduction; score recomputation; artifact/image exclusion scans; real corrupt-package startup
  refusals; read-only container smoke; full [local sample walkthrough](../../output/playwright/demo-deployment/UC-3/local-20261016-6/ledger.md)
  and affected [UC-2 session regression](../../output/playwright/demo-deployment/UC-2/uc3-regression-20261016-1/ledger.md).
  The inline manual editor starts a draft on the first period/room change; attempt 5's obsolete button locator was
  corrected without changing production. Public HTTPS walkthrough is pending on the exact seven-file production delta.

- 2026-10-16: UC-3 **APPROVED**. The [checkpoint](checkpoints/UC-3.md) and
  [execution ledger](../../output/execution/demo-deployment/UC-3/20261016-1/ledger.md) bind the exact dirty submission,
  seven-file production delta and 149-file upload. [Public Chrome walkthrough](../../output/playwright/demo-deployment/UC-3/public-20261016-1/ledger.md)
  PASS on deployment `ef120a58-f97c-4ed1-b03d-f71a4d662eef`: all three exact sample pairs open without a visitor kernel
  process; Full Week/Day/lenses/details/narrow inspection, repair staging, accepted export and inline manual entry pass.
  Unsaved/declined/stale/unknown/unconfirmed/failing replacements preserve complete Current/drafts and the second owner.
  Real missing-confirmation 500 was fixed to safe 428; failed attempts are retained. All 159 unit tests pass; the affected
  UC-2 packaged session regression, artifact/scoring/anonymization/readiness/container checks and 73 deployed edge
  probes pass. Deployed logs show exactly three startup verifications, six sample opens and zero visitor computation.
  Public screenshots were visually inspected. A cleanup restart removes both disposable sessions, re-verifies all three
  pairs and passes 40 consecutive session-free health/static reads; the final isolated-ingress preflight passes.
  No later UC was started or approved, no commit was made, and no financial/provider configuration was changed.
  UC-6 must preserve sample independence when cache/allowance components are introduced; their lifecycle states are
  not claimed as live coverage here. UC-12's launch/capacity/spending blockers remain unchanged.

- Final UC-3 hash audit: a concurrent text-only Delete all browser-storage label/tooltip/confirmation edit was found
  in `demo-panel.js` after the approved upload. It is preserved and captured separately in the execution directory;
  the approval/deployment ZIP uses the exact staged production bytes. All other 148 uploaded production files still
  match the checkout. This separate copy edit is outside UC-3 and is not represented as part of its browser evidence.

- 2026-10-16: UC-4 started on base `d55ab636` after approved UC-2/3. Starting dirty files and source hashes are retained in `output/execution/demo-deployment/UC-4/20261016-1/`. Scope is complete browser-local definition management and exact In use tracking; UC-5 custom use remains a destination handoff. The prior Delete all copy change is preserved.

- UC-4 production is complete for the browser-local boundary. Java build/unit checks (107 kernel + 52 workspace), five focused storage/identity checks, exact packaged static hashes and shipped-artifact exclusion checks pass. Local walkthrough attempts retain sandbox and harness findings; the current full journey is completing real repair/import tracking. UC-5 must call the saved-use identity hook after its own successful custom-definition installation; that destination is not approved here.

- 2026-10-16: UC-4 **APPROVED** after the [complete final public Chrome walkthrough](../../output/playwright/demo-deployment/UC-4/public-20261016-1/ledger.md)
  and [local journey](../../output/playwright/demo-deployment/UC-4/local-20261016-5/ledger.md). The [checkpoint](checkpoints/UC-4.md)
  binds three production changes, the exact 150-file upload and preserved dirty boundary. Exact CRLF/Unicode saves,
  syntax line/column, strict/corrupt envelopes, declined/failed actions, UTF-8 limits, cross-tab safety, deletion and
  actual accepted-repair/import In use tracking pass. 159 Java unit tests, five focused storage checks, affected UC-2/3
  full browser regressions, final artifact exclusion and all 33 served static hashes pass. Browser API faults are
  explicitly injected. Earlier harness/sandbox attempts and the overlapping-JAR regression failure remain retained;
  the fixed-artifact session rerun passes. Deployment `f4be45da-754d-4e6c-a80d-7c004b47c1d0` uses the unchanged
  isolated Free smoke settings. Cleanup restart removes both disposable workspaces, verifies all three samples,
  passes 40 stateless HTTPS reads plus nine edge probes, and rechecks one service/replica/no TCP proxy.
  UC-5 is now eligible but NOT_STARTED; it owns custom use and must consume the successful-use identity hook.
  No later UC, public launch, commit, financial/provider setting or operator-data change is claimed.

- 2026-10-16: UC-5 started on the preserved dirty base. Scope is custom saved-definition use only; UC-4 approval and all other starting changes are retained. Starting source/status evidence is under `output/execution/demo-deployment/UC-5/20261016-1/`.

- 2026-10-16: UC-5 implementation and all local verification are complete. See [checkpoint](checkpoints/UC-5.md),
  [execution ledger](../../output/execution/demo-deployment/UC-5/20261016-1/ledger.md),
  [final full local journey](../../output/playwright/demo-deployment/UC-5/local-final-20261016-1-journey/ledger.md),
  and [real verification faults](../../output/playwright/demo-deployment/UC-5/local-final-20261016-1-faults/ledger.md).
  All **165 unit tests** and **five selected Chrome integration tests** pass, zero failures/errors/skips, including
  complete affected UC-2/3/4 regressions. Five browser-storage checks, artifact exclusions and all 33 packaged static
  hashes pass. Exact raw-text/optional/order fidelity, independent inclusive complexity limits, initial-only lineage,
  confirmation, real kernel reference errors, busy/watchdog/process/daily refusals, stale completion and owner isolation
  are verified. The saved-use callback consumes UC-4's actual returned snapshot and retains its In use mark on reload.
- **Historical authority blocker, resolved below:** UC-5 was blocked on public deployment authorization after local implementation. Automatic approval review
  rejected uploading the exact pinned production build to the existing isolated Railway service: external source/artifact
  transfer and remote environment mutation need trusted explicit user authorization naming this exact destination and
  payload. The rejected command never executed; the current UC-4 public deployment, all provider/financial settings
  and remote workspaces remain untouched. The [154-file manifest](../../output/execution/demo-deployment/UC-5/20261016-1/upload-context-manifest.json),
  [production patch](../../output/execution/demo-deployment/UC-5/20261016-1/uc5-production.patch) and
  [review record](../../output/execution/demo-deployment/UC-5/20261016-1/deployment-approval-block.json) make the
  requested action concrete. After authorization, deploy this same build, run its public Chrome journey, inspect
  served bytes/safe events, restart the same image to clean disposable visitors and recheck topology before UC-5 approval.
  No approval, commit, public launch, UC-6 completion, memory or operator-data change is claimed.

- 2026-10-16: User explicitly authorized the pinned UC-5 deployment and verification. Prior automatic-review rejection is retained as history and its authority blocker is resolved. Production archive/source/stage still match the 154-file manifest. UC-5 is PENDING_WALKTHROUGH while the public deployment, journey and cleanup run; provider settings remain unchanged.

- 2026-10-16: UC-5 **APPROVED** after the user explicitly authorized deployment and verification. Pinned 154-file
  production inputs deployed as `8bc49107-ee22-40f3-9e13-1ee2d5eb6439` to the existing Free smoke service, with
  unchanged provider settings. The [public Chrome journey](../../output/playwright/demo-deployment/UC-5/public-20261016-1-journey/ledger.md)
  passes complete custom-definition use/refusal/reload/ownership checks. Safe application events show three startup
  plus three visitor verifications, successful exact reuse and zero solve processes. All 33 served static hashes match.
  Same-image cleanup restart passes fresh sample checks, 40 stateless reads, nine edge probes and final isolated
  topology checks, leaving no disposable visitor work. The first cleanup probe's header-casing assertion is retained
  and resolved by a passing full repetition without a production edit. See the [checkpoint](checkpoints/UC-5.md)
  and [execution ledger](../../output/execution/demo-deployment/UC-5/20261016-1/ledger.md). No commit requested/made.
  Scope ends at UC-5. UC-6's actual effort/solve/admission behavior remains a destination handoff; UC-12/13 statuses
  and public-launch qualification are unchanged.

- 2026-10-16: UC-6 started on preserved base `d55ab636558224f2d33a5e71313e840dd9486428`. Starting dirty-file hashes/status are under `output/execution/demo-deployment/UC-6/20261016-1/`. UC-2 is approved; UC-5 provides real verified initial drafts and ordinary repair compilation supplies the repair precondition. Scope ends at UC-6; UC-7/8 remain separate destination journeys.

- 2026-10-16: UC-6 focused checks pass: 8 coordinator/cache tests, 6 custom-definition/accounting regressions and 4 kernel-verifier tests. Real packaged Chrome walkthrough is running. A sandbox packaging attempt used `verify -DskipTests`, but this repository explicitly still runs Failsafe; its unavailable-Docker/browser failures are retained in `package-1.log` and are not passing evidence. No public deployment/approval is yet claimed.

- 2026-10-16: First UC-6 browser attempt retained as FAIL: crash feedback expectation and the repair proposal selector in the new test harness were incorrect; observed real initial Quick/Thorough and repair Quick computations verified successfully. Harness fixes retain both failed ledgers. Production review also preserved UC-1 construction wording/located errors in the initial UI and tightened shutdown/late-run eligibility. A first final-regression command was stopped before browser work because its UC-3/4 output property names would have collided; no overlapping browser evidence is accepted. Fresh final verification follows.

- 2026-10-16: Correction to the preceding command note: final regression 1 had already exited on one legacy unit fixture before the stop request; regression 2 confirms the same fixture failure and never reaches browser work. It seeded a RUNNING aggregate without a coordinator computation. The privacy fixture now uses an owned terminal record; active-run ownership remains covered through actual coordinator admission and real Chrome. All other workspace units, including forced two-second termination and 120-second argument binding, pass.

- 2026-10-16: UC-6 final-regression-3 unit checks pass (107 kernel + 70 workspace). The fault journey retained a FAIL: a local one-second overhead override also affected the real no-feasible search, timing out JVM startup; restored production overhead for the next attempt. Visual inspection found an actual conflicting legacy “Running” field beside the truthful queued status. Demo rendering now removes that duplicate, uses phase-neutral repair headings and keeps live status at narrow widths. A new queue assertion covers the contradiction; no normal-profile status text changed. UC-6 remains PENDING_WALKTHROUGH until the corrected full journey and required public boundary pass.

- 2026-10-16: Corrected UC-6 fault journey 4 passed real queue/watchdog/crash/impossible initial/impossible repair/daily-limit/disabled-reuse paths. Screenshot inspection then found a legacy one-minute repair-button label despite Quick, plus opened failure utilities collapsing the school heading. The demo now uses an effort-neutral repair action and a full-width utilities row. Journey 4 was interrupted during its main method and is retained as incomplete, never overall PASS. A final complete UC-6 run 5 repeats both methods with layout and label assertions. All Java production bytes remain unchanged since the full 177-unit/affected-UC passing checks.

- 2026-10-16: UC-6 local verification is complete: all 177 Java units, five affected UC-2/3/4/5 Chrome regressions, and both final complete UC-6 Chrome journeys pass. Twelve focused coordinator/process checks pass again on the final submission. Final queue/reuse/repair/390px/failure screenshots were visually inspected; all browser error logs are empty. Actual restart, failure, daily refusal, disabled reuse and exact Current preservation pass. Final package scan covers 23,674 entries; all 34 static resources and all 159 source/stage/archive files match. See [checkpoint](checkpoints/UC-6.md) and [execution ledger](../../output/execution/demo-deployment/UC-6/20261016-1/ledger.md). Required public HTTPS verification is pending on the pinned production archive `f0b3dd8ccebd5843982dec0c886bebc0cfbea4d2a67e786b60040e7bcde488fe`; UC-6 is not yet approved.

- 2026-10-16: UC-6 **BLOCKED only on required public deployment authority/evidence**. Automatic approval review rejected uploading the concrete 159-file production stage to the existing Railway smoke service: trusted authorization covers implementation, but not this external source transfer and remote deployment mutation. The command never executed; no source was sent, deployment/session was changed or provider setting was altered. The [exact review record](../../output/execution/demo-deployment/UC-6/20261016-1/deployment-approval-block.json), [payload manifest](../../output/execution/demo-deployment/UC-6/20261016-1/upload-context-manifest.json), [archive](../../output/execution/demo-deployment/UC-6/20261016-1/production-submission.zip) and [checkpoint](checkpoints/UC-6.md) make authorization reviewable. Local implementation, seven final/affected Chrome journeys, all 177 units, visual QA and artifact/preservation checks are complete. After explicit user authorization for this payload/destination, run public verification and same-image cleanup with unchanged settings before UC-6 approval. No later UC, commit or launch qualification is claimed.

- 2026-10-16: User explicitly authorized the previously requested pinned UC-6 deployment. Historical automatic-review rejection is retained and its authority blocker is resolved. All 159 source/stage/archive files still match archive `f0b3dd8ccebd5843982dec0c886bebc0cfbea4d2a67e786b60040e7bcde488fe`. UC-6 is PENDING_WALKTHROUGH while deploying, verifying the public HTTPS journey and cleaning disposable state; provider/resource/spend settings remain unchanged.

- 2026-10-16: UC-6 **APPROVED** after explicitly authorized pinned deployment and the [complete public Chrome journey](../../output/playwright/demo-deployment/UC-6/public-20261016-1-journey/ledger.md). Deployment `a3ebd477-0aa8-46ee-a8b5-38d33d35b78a` uses the exact 159-file payload, with unchanged provider settings. Both actual Quick/Thorough efforts for initial/repair, sharing/privacy, cancellation, queue estimates, immutable reuse/provenance and exact Current preservation pass. All twelve focused checks and the selected public method pass with no failures/errors/skips; browser errors empty and public screenshots inspected. Prior 177 full units, both final local journeys and all five affected UC regressions pass. Safe events corroborate four feasible distinct computations, two coalesced subscriptions, two artifact hits and cancelled waiting work. Same-image cleanup removes both disposable visitors, verifies all samples, passes 40 stateless reads/nine edge probes/all 34 exact static hashes and final isolation checks. See [checkpoint](checkpoints/UC-6.md) and [execution ledger](../../output/execution/demo-deployment/UC-6/20261016-1/ledger.md). The historical automatic-review rejection is resolved by the user's explicit authorization. No commit requested/made. UC-7/8 remain NOT_STARTED destination journeys; UC-12/13 launch qualification and provider/spend settings are unchanged.

- 2026-10-16: Remaining-use-case implementation resumed at the user's explicit instruction to defer all deployments until requested after implementation. Base remains `d55ab636`; the starting dirty boundary is captured in `output/execution/demo-deployment/remaining-20261016-1/`. Existing approvals are retained. UC-7 through UC-11 will use packaged local TLS/real Chrome and kernel evidence; public verification is deferred, not inferred. UC-12/13 deployment, financial controls and capacity qualification remain pending; their local code/tools will be prepared without remote mutations.

- 2026-10-16: UC-7 local implementation/walkthrough PASS: `DemoRepairDraftBrowserIT` uses packaged TLS/real Chrome, two disposable owners, conflict-blocked solving, individual/bulk pin/cancel/undo, complete reload fidelity, unavailability undo and confirmed discard. Fourteen focused existing coordinator/custom-definition tests also pass. [Ledger](../../output/playwright/demo-deployment/UC-7/local-20261016-1/ledger.md), [checkpoint](checkpoints/UC-7.md). Added the missing per-resource Undo unavailability action; Current and other owner remain exact with zero visitor kernel commands. Screenshot inspected; demo teacher-route label now names JSON. Approval is for local implementation under the user's deployment deferral; public regression is explicitly pending. UC-8 now starts.

- 2026-10-16: UC-8 local implementation **APPROVED**. [Final real Chrome ledger](../../output/playwright/demo-deployment/UC-8/local-20261016-2/ledger.md) passes computed/reused initial and repair acceptance, explicit acknowledgement/server refusal, duplicate version refusal, discard/reuse and two-owner independence with exact pair/lineage comparisons. Two focused service tests prove all verification refusal classes preserve eligible retry state and changed complete identity invalidates before publication. Solve verification now seeds exact bounded visitor evidence; acceptance uses that evidence or the common reserved verifier. Current/configured effort/build/compiled successor are checked; original solve provenance is retained. Screenshot feedback finding fixed and full journey repeated; UC-7 regression passes. Public verification deferred. [Checkpoint](checkpoints/UC-8.md). UC-9 starts.

- 2026-10-16: UC-9 local implementation **APPROVED**. [Packaged manual Chrome ledger](../../output/playwright/demo-deployment/UC-9/local-20261016-1/ledger.md) passes actual conflict/revert/period+room changes/reload, direct teacher refusal and JSON guidance, actual verification process failure with exact retained draft/Current, successful explicit retry/publication, declined/confirmed discard and unaffected second owner. Eight selected unit tests pass. Manual publication now uses the reserved common verifier and preserves policy/attempt-lock origins in the updated manifest; stable candidate correlation enables exact retry evidence reuse. Screenshot inspected. No solve/deployment/remote mutation. UC-10 starts; import round-trip is tracked as a UC-11 destination handoff.

- 2026-10-16: UC-10 local implementation **APPROVED**, with UC-11 round-trip handoff outstanding. [Real Chrome download ledger](../../output/playwright/demo-deployment/UC-10/local-20261016-1/ledger.md) passes sample/accepted-repair exports, complete exact archive comparisons, ancestry/provenance and draft/proposal/manual exclusion with zero export kernel commands; five safe-reader tests pass. Demo export text now explicitly excludes unfinished work, and optional stale If-Match is checked. Portable archives retained for UC-11. No deployment. [Checkpoint](checkpoints/UC-10.md). UC-11 starts.

- UC-11 second local attempt retained as FAIL: the self-contained repair had already populated exact verification evidence, so the test's claimed cold-miss process count was wrong (5 observed, 6 expected). Exact import/manifest checks passed before that harness assertion. The next attempt restarts the same local image after exporting, explicitly clearing visitor evidence before cold import; no production change is needed.

- 2026-10-16: UC-11 local implementation **APPROVED** after [complete self-contained real Chrome journey 3](../../output/playwright/demo-deployment/UC-11/local-20261016-3/ledger.md). Actual protected repair/acceptance/export creates the shared file; a real restart clears evidence before cold import; a fresh browser hits exact verification, while standalone documents without manifest retain all locks as persistent policy. Unsafe archive, actual verifier exit, stale tab and daily refusal preserve complete prior Current; depleted allowance still permits export. Twelve selected strict-reader/custom-definition tests pass. Earlier harness cold-count failure retained. UC-10 round-trip handoff closed. [Checkpoint](checkpoints/UC-11.md). All visitor implementation UCs now pass locally. UC-12 local tooling/configuration and UC-13 observability proceed without deploying; deployed qualification remains deferred by user.

- 2026-10-16: UC-12 preparation now includes generation-only fictional 60/72-cohort capacity fixtures and optional explicit HTTPS cold runner. Both 2,160-lesson / 100-teacher / 80-room / 50-period target definitions pass the real packaged verifier. Initial generator failure (missing lesson displayName) is retained; fixed generation 3 passes. No cold solve, deployment, memory or financial qualification inferred. UC-13 safe structured event implementation is in progress; affected UC-8/11 real Chrome regressions pass with process/decision logs.

- 2026-10-16: **Remaining local implementation complete; deployment intentionally deferred.** UC-12's prepared
  qualification tools, startup/process bounds and real shutdown cleanup pass; UC-13's seven-family structured events,
  interval accounting/privacy and real three-owner operator journey pass. [UC-12 checkpoint](checkpoints/UC-12.md),
  [UC-13 checkpoint](checkpoints/UC-13.md), [final implementation ledger](../../output/execution/demo-deployment/remaining-20261016-1/ledger.md).
  185 Java units pass across the final verification commands; the last command passes 14 integration cases
  (three packaged Chrome journeys plus all 11 normal manual-draft cases). Earlier affected authoring, initial/repair,
  session/definitions and remaining demo journeys pass with failed harness attempts preserved. Normal publication's
  absent-manifest regression was fixed and retested. Only two unchanged-base import failures remain. Package privacy,
  JS syntax/storage, source/archive fidelity and starting-file preservation checks pass. No deployment, upload,
  provider/resource/financial mutation or commit occurred. UC-12/13 retain IN_PROGRESS only for their unperformed
  deployed operator goals; Hobby/cold target/combined memory/spend/public evidence is never inferred from local PASS.

- 2026-10-16: User requested and generally authorized deployment of the remaining implementation. Fresh source/
  stage/archive fidelity and read-only existing-service/isolation checks pass. Automatic approval review rejected
  the upload **before execution** because private production source transfer requires explicit authorization naming
  the exact payload and destination. No deployment/upload/provider mutation occurred and no workaround was attempted.
  [Attempt ledger](../../output/execution/demo-deployment/remaining-deploy-20261016-1/ledger.md) and
  [concrete payload/destination/review](../../output/execution/demo-deployment/remaining-deploy-20261016-1/deployment-approval-block.json).
  Proposed scope is only the pinned 162-file build on existing `school-kernel-free-smoke / demo-smoke`, generated
  `demo-smoke-production.up.railway.app`; resource/plan/billing/spend settings stay unchanged. Current public build
  remains the earlier approved UC-6 deployment. Local implementation approvals and deferred Hobby qualification remain unchanged.

- 2026-10-16: Exact payload/destination authorization resolved the prior automatic-review rejection. Uploaded only
  the pinned 162-file archive `d292a016f6edb85874eb98597643e555159829e2ec3f10a73cc082d46f054dd7` to existing
  `school-kernel-free-smoke / demo-smoke / production`. Deployment `a96c3c89-1c5e-4a13-9093-fcfb40f29368` is SUCCESS;
  original resource/replica/region/sleeping settings are identical. Public runs pass actual custom Quick/reused initial
  acceptance, repair conflict/pin/bulk/cancel/undo/reload, exact repair acceptance/ancestry, verified manual publication,
  exact export exclusions, independent archive/pair import and unsafe refusal. A focused public diagnostic also passes
  stale-tab refusal and repeated valid/foreign polling. The joint journey is **not overall PASS**: attempt 1 retained
  a test file-mode selection error (fixed); attempt 2 exposed a genuine 393px page at a 390px viewport for a long
  unbroken imported school name. UC-11 needs revision; all earlier evidence remains preserved.
- Prepared one demo-only heading-wrap CSS rule. Actual public browser-only stylesheet preview passes at 390px;
  current deployed stylesheet remains original. New [162-file fix payload](../../output/execution/demo-deployment/remaining-deploy-20261016-2/mobile-fix-payload.json)
  changes exactly one production file, builds locally, and has not been uploaded because authorization named the
  original archive. No resource/billing/plan change is proposed. Final same-image cleanup ends both remaining test
  owners, re-verifies all three samples and resets the counter epoch. Post-cleanup 40 stateless reads, nine edge probes,
  all 35 original static hashes and isolation preflight pass without new sessions. Safe logs reconcile physical work
  and actual decisions with no private test markers. [Deployment evidence](../../output/execution/demo-deployment/remaining-deploy-20261016-2/ledger.md).
  Hobby capacity/financial controls and complete UC-12/13 qualification remain unperformed; Free deployment is not that approval.
