# UC-2 checkpoint

Status: **APPROVED**, 2026-10-16. Scope: UC-2 only.
Base: `d55ab636558224f2d33a5e71313e840dd9486428`, branch `demo`; changes remain uncommitted.
The pre-existing dirty checkout is preserved. The [prior blocked checkpoint](../../../output/execution/demo-deployment/UC-2/20261016-resume-1/prior-checkpoint.md)
and failed attempts remain available; the approval below supersedes their ingress/lifecycle blocker.

## Submission and deployed boundary

[Execution ledger](../../../output/execution/demo-deployment/UC-2/20261016-resume-1/ledger.md),
[final source manifest](../../../output/execution/demo-deployment/UC-2/20261016-resume-1/final-source-manifest.json),
[production delta](../../../output/execution/demo-deployment/UC-2/20261016-resume-1/production-delta-from-prior-deployment.json),
and [exact resumed patch](../../../output/execution/demo-deployment/UC-2/20261016-resume-1/resumed-production.patch)
identify the submission. Only DemoConfiguration, DemoSettings, HostOriginFilter and application-demo.yml changed in
production during this resumption; all other 145 uploaded production inputs match the preceding deployment.
The [149-file upload manifest](../../../output/execution/demo-deployment/UC-2/20261016-resume-1/upload-context-manifest.json)
is verified against both staged files and the checkout. Kernel, samples and UI production content were unchanged.

Public URL: https://demo-smoke-production.up.railway.app (authorized isolated Free smoke service).
Initial fixed-source deployment: `99089c8b-928e-4a53-93a7-f7a414339bd1`.
Final restored deployment: `1fc1827a-71d6-4d12-8c18-9e20545fd22a`, SUCCESS.
Both use image `sha256:c47828a3d718e4e38d2bdae59e1581f56adcf120ccff146b01105bcd71fd6222`.
The [final provider preflight](../../../output/execution/demo-deployment/UC-2/20261016-resume-1/boundary-final.json)
proves one project/environment service, one replica, only its generated domain and zero TCP proxies.

[Rule 1](../rules.md) records the provider-supported isolated-environment boundary. Railway publishes no stable proxy
CIDR. Explicit isolated ingress defaults off, requires deployment UUIDs and the exact generated domain, and cannot
be combined with address trust. This is an audited operational topology, not cryptographic request authentication;
another service/domain/TCP ingress requires requalification. Scheme forwarding and a single forwarded host matching
native authority are accepted only inside that boundary. Host, Origin, CSRF, route, session, CSP and no-store guards
remain enforced. No provider credential enters the service or evidence. The normal profile is unchanged by this mode.

## Implemented UC-2 contract

Private owner authority is held in the required Secure/HttpOnly/SameSite Strict browser session. Demo aggregates are
in memory behind WorkspaceStore; normal local JDBC persistence/recovery remain outside the demo profile. Every
workspace read/write, import/export and run route binds to the owner. Background planning/repair captures authority;
late, duplicate, stale or expired completion cannot publish into another/newer workspace. Atomic version-guarded
updates and owner-local repair bookkeeping preserve isolation. Foreign and random run IDs are indistinguishable.

First use shows EMPTY with Full previewed; selection starts no solve and accepts no baseline. The default cap is 50,
with this smoke service explicitly capped at two. Polling refreshes idle activity but cannot extend absolute expiry.
Expiry removes the aggregate and cancels its unshared process; restart loses work. Ended cookies receive the loss
explanation, import guidance and explicit Start again, which rotates the session and creates EMPTY without replay.
Browser-local exact saved text survives clear, reload and restart. Clear names discarded work and offers export;
declined, active-run and stale clear cannot mutate Current. Structured authoring routes return 404 and the authoring
state is unreachable. Retention/privacy covers all four required scopes and export/unfinished-work loss.
Health is session-free and its special platform host is accepted only for GET /healthz.

## Passing evidence

- **46 workspace unit tests / 12 classes**, zero failures/errors/skips; [summary](../../../output/execution/demo-deployment/UC-2/20261016-resume-1/surefire-summary.txt).
- [Final packaged local TLS browser regression](../../../output/playwright/demo-deployment/UC-2/local-final-20261016-1/ledger.md):
  PASS, one complete integration journey; two owners, real packaged kernel and disposable process restart.
- [Full public two-owner walkthrough](../../../output/playwright/demo-deployment/UC-2/public-20261016-3/ledger.md):
  PASS for exact accepted export/import, private draft/run/cancellation, foreign/random probes, stale shared tab,
  declined/active clear, private clear, two-owner capacity refusal, narrow inspection, browser text and two provider
  restarts. Each restart passed 40 consecutive public health/static checks. No proposal was silently accepted.
- [Recovery/privacy/stale-clear supplement](../../../output/playwright/demo-deployment/UC-2/ended-diagnostic-20261016-2/ledger.md):
  PASS for explicit recovery, 20 actual-browser and API reads/reload, complete disclosures/groups/security headers,
  and stale clear 412 with the complete accepted snapshot unchanged.
- [Deployed expiry walkthrough](../../../output/playwright/demo-deployment/UC-2/public-expiry-20261016-3/ledger.md):
  PASS at temporary 15-second idle / 75-second absolute limits. An idle owner's private unfinished repair expired
  while another owner's polling retained activity. A real private run was observed RUNNING and then FEASIBLE before
  absolute expiry; continued polling still ended the session with 410. No result/Current recovered or auto-accepted;
  explicit restart was EMPTY and expired/random run IDs were indistinguishable. Exact browser text survived.
- [Expiry/restoration record](../../../output/execution/demo-deployment/UC-2/20261016-resume-1/expiry-and-restoration-3.json)
  and [final settings](../../../output/execution/demo-deployment/UC-2/20261016-resume-1/restored-settings-final.json):
  PASS, test overrides removed, **60-minute idle / 8-hour absolute**, original max-workspaces=2 restored, same image.
- 73 stateless public edge/security probes passed before expiry, then
  [73 more after restoration](../../../output/execution/demo-deployment/UC-2/20261016-resume-1/edge-probes-restored.json).
  Includes concurrent independent health requests, edge forwarding replacement, Forwarded/Origin/CSRF refusal,
  HTTP redirect and absence of session cookies on these requests.

Screenshots were inspected for public 390px overflow/read-only JSON, private staging, capacity and lost-session states.
The accelerated-test banner rounds sub-minute/hour settings to 0 minutes/0 hours; those screenshots establish loss
and recovery presentation, while exact provider configuration and recorded polling establish timing. Normal 60m/8h
presentation was separately checked publicly. No separate Playwright skill is available; Java Playwright drives the
actual rendered controls and records unique durable ledgers. All failed attempts are retained. Early public failures
sampled session rotation too soon; awaited state polling fixed the harness, with no application session fix.

## Limits, cleanup and outstanding destinations

Existing default-profile WorkspaceImportIT evidence remains 23/25 passing; both failures reproduced on unchanged
base d55ab636. Exact IDs/reports are in the [earlier technical ledger](../../../output/execution/demo-deployment/UC-2/20261016-final/ledger.md).
This resumption does not claim fresh full normal-profile qualification or a fully green unrelated suite.

Browser contexts and local server processes closed; test downloads/private inputs were removed. Provider restarts
and final restoration cleared disposable workspaces. The isolated sleeping-enabled Free service is retained at its
original CPU/memory/cap and expiry settings. No financial/resource/plan change, SSH access, external message, memory
write or commit was performed during this resumption.

UC-3 sample maintenance/full sample contract, UC-4 complete browser-version behavior, UC-5 custom use, UC-6 shared
queue/reuse/allowance and expiry detachment, UC-7–11 full repair/review/manual/export/import, and UC-13 remain
outstanding. Their entry points/enabling work are not approvals. UC-12 remains BLOCKED on Hobby/always-on,
capacity/spending/default-profile and later-use-case launch prerequisites. This is UC-2 approval, not public launch.
