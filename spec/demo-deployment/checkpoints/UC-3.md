# UC-3 checkpoint

Status: **APPROVED**, 2026-10-16. Scope: UC-3 only. Requires UC-2, whose approved private-session
postconditions were consumed by the real two-owner and same-owner stale-tab journeys below.
Base: `d55ab636558224f2d33a5e71313e840dd9486428`, branch `demo`; no commit requested or made.

## Submission

The [execution ledger](../../../output/execution/demo-deployment/UC-3/20261016-1/ledger.md),
[starting source manifest](../../../output/execution/demo-deployment/UC-3/20261016-1/start-source-manifest.json),
[final source manifest](../../../output/execution/demo-deployment/UC-3/20261016-1/final-source-manifest.json),
[exact production patch](../../../output/execution/demo-deployment/UC-3/20261016-1/uc3-production.patch), and
[seven-file production delta](../../../output/execution/demo-deployment/UC-3/20261016-1/production-delta-from-uc2.json)
bind the previously dirty checkout and this submission. All seven before hashes match the starting manifest and
UC-2's retained production upload. The [149-file upload manifest](../../../output/execution/demo-deployment/UC-3/20261016-1/upload-context-manifest.json)
was checked against the checkout and staged production-only inputs before upload. The immutable submission ZIP
uses these approved staged production bytes. Test, maintenance and documentation changes are listed in the final
source manifest/delta; unrelated existing changes are preserved.

The final hash audit found one concurrent, text-only change to demo-panel.js's Delete all label/tooltip/confirmation,
after upload and outside UC-3. It is preserved in the checkout and recorded in
[preserved-concurrent-ui-copy.patch](../../../output/execution/demo-deployment/UC-3/20261016-1/preserved-concurrent-ui-copy.patch).
It is not included in the approved/deployed production snapshot or represented as browser-tested. The final checkout
manifest deliberately includes its current bytes; the approved-production-source-manifest.json and submission ZIP
identify UC-3's verified production bytes. Every other uploaded production file still matches the checkout.

The [submission ZIP](../../../output/execution/demo-deployment/UC-3/20261016-1/submission.zip),
[SHA-256](../../../output/execution/demo-deployment/UC-3/20261016-1/submission.sha256), and
[final verification](../../../output/execution/demo-deployment/UC-3/20261016-1/final-verification.json)
record the completed archive, source-boundary and evidence-link checks.

Public URL: https://demo-smoke-production.up.railway.app.
Deployment: `ef120a58-f97c-4ed1-b03d-f71a4d662eef`, SUCCESS.
Image: `sha256:8aee98505e0739bdd3974b6c302325e5183e868f4d0d0f358c83fed0758d3dec`.
This remains the previously authorized isolated Free smoke service, with no financial/configuration change.
The [final ingress preflight](../../../output/execution/demo-deployment/UC-3/20261016-1/boundary-after-cleanup.json)
passes for its one service/replica/generated domain and zero TCP proxies.

## Implemented behavior and verification

| Contract | Observed evidence |
|---|---|
| Main 1 | All three previews match their complete packaged definitions. Exact ordered catalog; computed sizes; fictional notice; JSON; configurable threshold/default-preset duration. New session previews Full while EMPTY. |
| Main 2 | Each of Small, Two-class and Full opens only on explicit use. Exact complete definition/result values are asserted through the production API. |
| Main 3 | Confirmation names the school and accepted/initial/repair/manual/proposal work. Accepted export is offered before replacement. Decline preserves exact full snapshot. |
| Main 4 | Startup evidence binds complete documents, catalog, current kernel fingerprint and recomputed score. Real startup verification precedes readiness. Atomic owner/version-guarded sample installation is independent of runtime verification/solver paths. |
| Main 5 | Current opens at whole-school Week, with In use on the actual source, surviving reload. Full inspection, Day, teacher/room lenses, details and 390px read-only inspection pass. Full repair staging, exact accepted export and inline manual entry pass. |
| 1a | A real trailing-space edit followed by declined selection preserves exact editor text, selection and Current. An older delayed preview cannot overwrite a newer selection. |
| 2a | Unsaved text disables use with visible save/revert guidance. |
| 2b | The affected UC-2 real-run regression checks disabled sample use, wait/cancel guidance and HTTP 409 with the complete running snapshot unchanged. |
| 3a | Declined repair/manual replacement preserves Current plus the exact draft and other visitor. |
| 4a | Unknown sample is 404 SAMPLE_NOT_FOUND with full snapshot unchanged. |
| 4b | A stale rendered shared-owner tab receives 412; intervening repair survives. Rendered Reload current workspace fetches current state while preserving selected sample/text. |
| G1 | Local launcher records exactly the three startup verify commands and refuses every later command; all opens/inspection/export/manual entry still pass. Public logs likewise contain exactly three startup kernel events and no visitor kernel launches. Sample use has no visitor-cache/admission dependency. |
| G2 | Real score recomputation matches complete score breakdown, weights and revisions for all three current-catalog samples. Old-build, stale-score and invalid-pair disposable JARs exit 1 before readiness. The maintenance command refuses a tampered score. |
| G3 | Generator check reproduces exact definitions and inverse/order evidence. Distinct committed pool: 51 names. Local and image JAR scans inspect 23,656 entries including nested dependencies, source references, exact string/JVM literals and full names; no source identity or real example is packaged. Ordinary production inputs exclude examples, tests and maintenance tools. |
| G4 | Preview alone preserves workspace. Real private repair/manual edits preserve exact Current, source previews/ETags and second visitor. Service tests also prove deep copies and owner isolation. |
| G5 | Full desktop and narrow screenshots were opened and visually inspected. Current/draft badges, fictional names, readable controls and contained matrix scrolling pass; no page overflow at 390px. Existing shared inspection code is unchanged. |
| G6 | Public sample catalog revalidates 304 by ETag; visitor state is no-store. Six successful opens emit safe sample_open events after installation; no proposal acceptance request is made. |
| G7 | Complete real Chrome public journey passes for all three samples, Full inspection/repair/export/manual entry, two owners and stale shared-owner tab. Public process/event counts corroborate the browser. |
| Success | Exact selected verified complete pair becomes only this visitor's accepted Current without visitor computation. |
| Minimal guarantee | Declined, stale, unknown, unconfirmed and active-run refusals retain exact prior work. A browser-injected HTTP 503 preserves prior state/text and offers reload/retry. Service installation-failure coverage is supporting test evidence, not a claimed production storage outage. |
| Relations/rules | UC-2 ownership/CSRF/Origin/host boundary remains in force. Its packaged session regression passes; 73 stateless deployed edge probes pass. No Includes or Extends dependency exists. |

## Passing checks

- `./mvnw -q -pl timetable-workspace -am -DargLine=-javaagent:<local-mockito-core.jar> -DskipITs package`:
  **107 kernel + 52 workspace unit tests**, zero failures/errors/skips. The explicit agent avoids the environment's
  self-attachment restriction; it is not a production option.
- `python3 tools/demo/record_scoring_evidence.py`: evaluates stored assignments with the packaged current kernel;
  complete scores/weights/revisions match, without solving. Kernel code/schema fingerprint remains
  `e52ad2b0638c13259f4c14d5a48e2ba82365798c24b2e599a0399ee33e2be86b`.
- `python3 tools/demo/generate_samples.py --check`; `check_packaged_samples.py` for local and extracted image JARs;
  `check_sample_readiness.py`; read-only non-root container smoke under the Free smoke memory/CPU/heap limits: PASS.
- [Full local UC-3 journey](../../../output/playwright/demo-deployment/UC-3/local-20261016-6/ledger.md): PASS.
- [Affected UC-2 session regression](../../../output/playwright/demo-deployment/UC-2/uc3-regression-20261016-1/ledger.md):
  PASS, including real private run/cancellation, active sample refusal, imports/exports, restart and ended-session recovery.
- [Full public UC-3 journey](../../../output/playwright/demo-deployment/UC-3/public-20261016-1/ledger.md): PASS, normal
  HTTPS certificate validation, real Chrome, disposable data, no browser errors.
- [Public process/event counts](../../../output/execution/demo-deployment/UC-3/20261016-1/public-process-counts.json):
  three startup verifications, six sample opens, zero visitor processes or acceptance requests.
- [73 public edge probes](../../../output/execution/demo-deployment/UC-3/20261016-1/edge-probes.json): PASS.
- [Cleanup restart checks](../../../output/execution/demo-deployment/UC-3/20261016-1/cleanup-checks.json): fresh startup
  verifies all three pairs; 40 consecutive health/static HTTPS reads pass without creating sessions. Disposable public
  workspaces removed. Local retained servers were checked/stopped; browser contexts are closed. Deployment retained.
- `git diff --check`: PASS. Final status/source boundary retained in the execution directory.

## Findings and scope boundary

Attempt 3 found a real missing-confirmation HTTP 500 caused by strict JSON boolean access. It is fixed: only an actual
boolean true confirms replacement; absent/null/string/number values safely refuse. Exact Current/draft preservation
and the corrected 428 are covered at controller and public browser boundaries. Earlier failures are retained, including
sandbox socket refusal, large-text fill timeout, interrupted shared-output run and obsolete manual-entry locator.
Attempt 6 and the public attempt pass the complete journey after fixes.

There is no installed standalone Playwright skill; the repository's Java Playwright runtime drove the actual UI and
wrote step ledgers/screenshots. This is automated approval under execute, not human confirmation or an independent
reviewer's verdict. Existing non-demo behavior is unchanged by these demo-only code/config paths; the previously
recorded normal-profile baseline failures remain UC-2 evidence, not newly claimed passes.

UC-4/5 still own full saved/custom-definition behavior. UC-6 owns actual queue/cache/daily-allowance lifecycles and must
retain sample independence when those components are added; live full/expired/exhausted states are not claimed to
exist in this intermediate build. UC-7/8/9/10/11 own complete repair execution/review, manual publication and accepted
bundle round-trip journeys. Their entry points/enabling behavior here do not approve them. UC-12 remains blocked
on Hobby/always-on, measured capacity, spending controls and later-UC launch requirements. UC-3 approval does not
qualify a public release. No later use case was started.
