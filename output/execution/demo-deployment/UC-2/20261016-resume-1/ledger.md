# UC-2 resumed execution and approval ledger

Date: 2026-10-16. Verdict: **PASS / UC-2 APPROVED**.
Base: `d55ab636558224f2d33a5e71313e840dd9486428`, branch `demo`. No commit requested/performed.
Scope is only UC-2; preserve the [starting dirty inventory](start-status.txt). The
[prior checkpoint](prior-checkpoint.md) and earlier failed ledgers remain historical evidence.

## Source and infrastructure identity

The [production delta](production-delta-from-prior-deployment.json) and [patch](resumed-production.patch) identify
exactly four changed production inputs: DemoConfiguration.java, DemoSettings.java, HostOriginFilter.java and
application-demo.yml. The [149-file upload manifest](upload-context-manifest.json) binds the actual production-only
upload, with 145 other inputs unchanged from the previous image. The [final verification](manifest-verification-final.json)
checks both checkout and isolated stage against every upload hash. No tests, operator school data, credentials or
execution output were uploaded. The [final source manifest](final-source-manifest.json) also binds tests/tool/docs.
The [tracked source patch](final-tracked-source.patch) is a checkout snapshot against base, includes pre-existing
work and is not attributed wholesale to this resumption. New source files are identified by manifest paths/hashes.

Initial fixed-source deployment: `99089c8b-928e-4a53-93a7-f7a414339bd1`; final restored deployment:
`1fc1827a-71d6-4d12-8c18-9e20545fd22a`, SUCCESS. Same image throughout lifecycle/expiry tests:
`sha256:c47828a3d718e4e38d2bdae59e1581f56adcf120ccff146b01105bcd71fd6222`.
URL: https://demo-smoke-production.up.railway.app, normal public TLS certificate validation.
Project `13d509af-1436-4024-bb64-f9f78fa439fc`, environment `a41a9c0e-ec3f-4ef4-9e08-996d998d1815`,
service `672cabae-b54e-4965-bbb2-5236bec81c33`. [Final preflight](boundary-final.json): one service/replica,
only generated domain, no TCP proxies. EU region, one CPU, 500 MB, sleeping enabled retained; no billing change.

Railway staff supplies no stable proxy CIDR. [RULE-1](../../../../../spec/demo-deployment/rules.md) records the
audited isolated-project/environment trust invariant. Explicit forwarding mode defaults off, validates deployment
identities/domain and excludes proxy-address trust. Native Host, HTTPS scheme, matching single forwarded Host,
Origin, CSRF, owner, route and security response guards remain. Another service/domain/TCP ingress needs
requalification. Headers are not cryptographic private-caller identity. The preflight tool is read-only.

## Checks and real-browser journeys

All browser checks use disposable independent Chrome contexts, actual rendered controls and the real packaged kernel.
No separate Playwright skill is available. Each attempt has a unique retained directory and durable walkthrough ledger.

| Check | Observed result | Evidence |
|---|---|---|
| Workspace unit checks | 46 tests / 12 classes, no failures/errors/skips | [summary](surefire-summary.txt), [log](unit.log) |
| Reactor/local packaged verification | PASS, full local session journey | [log](local-verify.log), [report](failsafe-summary.txt) |
| Final local regression after harness changes | 1 test, no failures/errors/skips, 99.424 s | [report](local-final-org.schoolkernel.workspace.DemoSessionBrowserIT.txt), [ledger](../../../../playwright/demo-deployment/UC-2/local-final-20261016-1/ledger.md), [log](local-final-browser.log) |
| Full deployed lifecycle | 1 test, no failures/errors/skips, 188.2 s | [report](public-browser-3-result.txt), [ledger](../../../../playwright/demo-deployment/UC-2/public-20261016-3/ledger.md) |
| Recovery/privacy/stale-clear supplement | 1 test, no failures/errors/skips, 41.95 s | [report](ended-diagnostic-2-result.txt), [ledger](../../../../playwright/demo-deployment/UC-2/ended-diagnostic-20261016-2/ledger.md) |
| Deployed idle/absolute expiry | 1 test, no failures/errors/skips, 105.6 s | [report](public-expiry-3-result.txt), [ledger](../../../../playwright/demo-deployment/UC-2/public-expiry-20261016-3/ledger.md) |
| Stateless public security/health | 73 probes PASS before expiry; 73 PASS again after restoration | [original](edge-probes.json), [restored](edge-probes-restored.json) |
| Final provider/configuration restoration | SUCCESS, original 60m/8h, max 2, same image/topology | [boundary](boundary-final.json), [settings](restored-settings-final.json) |

Commands: workspace units use `./mvnw -q -pl timetable-workspace -am test`. Browser integration entry point is
`./mvnw -q -pl timetable-workspace -am -Dit.test=<class> -Dfailsafe.failIfNoSpecifiedTests=false`
with `-Ddemo.walkthrough.dir=<unique-output>` and `test-compile failsafe:integration-test failsafe:verify`.
Classes: DemoSessionBrowserIT (local), DemoRemoteSessionIT (`-Ddemo.remote.lifecycle=true`),
DemoRemoteEndedSessionIT (`-Ddemo.remote.ended=true`), DemoRemoteExpiryIT (`-Ddemo.remote.expiry=true`).
Remote runs supply `-Ddemo.remote.url=https://demo-smoke-production.up.railway.app`; lifecycle also supplies
`-Ddemo.railway.cli=<official CLI path>` for two hardcoded isolated-service restarts. The
[expiry controller](expiry-run-3.py) records its exact command and guarantees original JSON restoration in finally.
Read-only final topology command: `python3 tools/demo/check_railway_boundary.py --railway <CLI> --project <project>
--environment <environment> --service <service> --domain demo-smoke-production.up.railway.app --output <evidence>`.

The full public journey proves UC-2 main/2a–2d/4a–4c/G1–G7: EMPTY/Full preview without solve, required cookie,
persistent retention, private exact bundle import/export, private repair staging, custom run and owner cancellation,
foreign/random run indistinguishability, shared-owner stale guard, declined/active/confirmed clear, newcomer
capacity refusal without disturbing owners, authoring 404, security refusal without Current mutation, read-only
narrow JSON and no page overflow. Two real provider restarts end both owners without recovery/replay/acceptance.
Each restart has 40 consecutive health/static 200s; browser-local exact text survives clear/reload/restart.
The supplement checks 20 successful actual-browser AND API reads after explicit restart, reload, exact definition
groups, four privacy scopes, CSP/HSTS/nosniff/no-store, and stale-clear 412 with full snapshot non-mutation.

Expiry configuration is temporary **15s idle / 75s absolute**, applied only to this disposable service via its
existing JSON override. An idle private unfinished repair expired while another owner's polling kept idle alive.
A real private kernel run was recorded RUNNING, then FEASIBLE shortly before absolute expiry; polling still ended
the session with 410. No result or accepted baseline recovered; explicit Start again was EMPTY and expired/random
run IDs indistinguishable. Exact saved browser text remained. The [control report](expiry-and-restoration-3.json)
records exit 0 and redeployment restoration. `SPRING_APPLICATION_JSON` is again exactly
`{"demo":{"max-workspaces":2}}`; separate timeout variables remain absent. Final public config reports 60m/8h.

Screenshots inspected: public attempt 3 narrow state and second restart; earlier public capacity/private staging;
expiry attempt 3 idle and absolute ended screens. Test-only display limitation: accelerated sub-minute/hour
durations round to 0 minutes/0 hours in the banner. These screenshots prove ended-state presentation; provider JSON
and polling chronology prove the short timing. Default 60m/8h banner was checked in the normal public journey.
The final local ledger's public-unverified caveat is scoped to that local run; the separate public ledgers above
provide the deployment evidence.

## Retained failures and regression limits

Earlier exact-peer trust failed after restart; its evidence is preserved in the prior checkpoint and UC-12 first
Free-test ledger. Do not reinterpret the old pre-restart smoke as a full PASS. New isolated ingress fixes that
deployment boundary without inventing a CIDR. Public attempts 1/2 and initial recovery diagnostics failed when the
test sampled an asynchronous rotation too soon. Bounded Java polling now awaits page.evaluate and requires HTTP 200
plus the requested state. No production session fix was needed. The diagnostic ledger includes the corrected
premature stale-cookie attribution; all 20 API reads also passed.

Initial expiry orchestration hit a transient provider-status failure and restored defaults. Next browser attempt
interacted before Full preview initialization; the final harness waits for the actual 520-lesson preview, and the
test-only absolute clock was raised from 45s to 75s to allow setup. Failed logs/screenshots remain unchanged.
An early diagnostic attempted Playwright automatic downloads; later creation explicitly skips downloads and uses
installed Chrome. No user browser caches were deleted.

Reuse the earlier normal-profile WorkspaceImportIT evidence: 23/25 tests pass, two failures independently reproduce
on unchanged base d55ab636. Exact IDs/current and baseline reports are in the
[earlier ledger](../20261016-final/ledger.md). No fresh full normal-profile qualification is claimed this turn.
Default-profile production behavior is unchanged by the explicit new demo mode. `git diff --check` passes.

## Cleanup, approval and remaining scope

Independent browser contexts closed; JUnit removed private test inputs/downloads and stopped both local servers.
The second restart and final restoration cleared disposable provider workspaces. The retained isolated Free service
uses original CPU/memory/sleep/cap and 60m/8h expiry. No SSH key/access was created this turn; prior temporary SSH
credentials were already revoked. No operator database, unrelated browser tab, billing/plan, external message,
memory file or unrelated working-tree content was changed. No commit was made.

UC-2 is approved from the technical checks and recorded public journeys. UC-3–11 and UC-13 remain outstanding;
sample/browser-save/custom/export/import entry points are enabling work only. UC-6 must still implement shared
queueing, cache/coalescing, reservations and expiry detachment from shared work. UC-12 remains BLOCKED on the
Hobby/always-on platform, target-school/process/aggregate-memory measurements, spending controls, default-profile
qualification and remaining visitor journeys. No public launch or full-feature completion is asserted.
