# Timetable-first demo deployed on Railway Free

2026-10-16; branch `demo`, base `d55ab636558224f2d33a5e71313e840dd9486428`. User explicitly requested deployment
to the Free plan after local feature approval. Pre-existing dirty work is preserved; no commit requested or made.

The existing [public demo](https://demo-smoke-production.up.railway.app/workspace/) is running deployment
`0a3da7be-c9ac-4d08-9d84-005c8d6ff087` (SUCCESS), image
`sha256:69bd635760750362338f1c94bd0235c64f173a35d115d063db7836739a9502fa`.
The final [verification summary](deployment-verification.json) is PASS.

## Exact payload and unchanged Free settings

Only the [171 production inputs](upload-manifest.json) were staged and uploaded to existing
`school-kernel-free-smoke / demo-smoke / production` (project `13d509af-1436-4024-bb64-f9f78fa439fc`,
environment `a41a9c0e-ec3f-4ef4-9e08-996d998d1815`, service `672cabae-b54e-4965-bbb2-5236bec81c33`).
The [retained source archive](production-source.tar.gz) has SHA-256
`4fb6d4df3ebd4e0d02e85f23a7f9ab5c71b573642f17e8f8f87708d93e9c2e8e`.
Source/stage/archive hashes match and still match after completion. No tests, examples, real school data, operator
files, credentials, Git metadata, prior evidence or maintenance seeds entered the upload.
[Payload preflight](payload-preflight.json); [upload log](deployment-upload.log); [successful build](build-2.jsonl).

The local final implementation source pin remains exact. The only production addition in this deployment pass is
the shell launcher mapping `review` to the existing verification heap. Actual full-school CLI review passes at 96 MiB;
shell syntax and all four command-to-heap selections pass ([launcher checks](launcher-checks.json)). No Java or UI
production code changed in this deployment pass. The opt-in remote browser test is pinned separately in
[test-source-sha256.json](test-source-sha256.json).

[Before](plan-before.json) and [after](plan-after.json) provider reads both report `FREE`. The complete service
build/deploy manifest and runtime variable map are equal before/after: one EU replica, 1 CPU, 500,000,000 bytes,
sleeping enabled, zero deployment overlap, 128 MiB web, 96 MiB verification, 128 MiB solver heaps, two-workspace cap.
No plan/billing/resource/variable mutation was issued. Sensitive variable values were never emitted to evidence.
[Before](provider-before.json) / [final](provider-final.json) state and [safe runtime settings](runtime-settings-before.json).
The [before](boundary-before.json), [deployed](boundary-deployed.json) and [final](boundary-final.json) isolated-ingress
preflights pass: one service/replica, exact generated domain, no custom domain or TCP proxy, unchanged trust model.

## Public actor evidence

The complete [public Chrome journey](../../../playwright/demo-timetable-experience/public/free-20261016-2/ledger.md)
and [XML](public-browser-main.xml) pass against normal public TLS using installed Chrome and disposable owners:

- Default full Estonian matrix, exactly three schools/two variants each, all six matrices, zero-request same-school
  switching with retained class filter, cross-school cleared filters, removed controls absent.
- Actual authoritative teacher, room and period validation. Unqualified teacher identifies one affected lesson;
  class collision identifies both lessons and correction guidance; global link reveals a filtered-out lesson;
  equipment conflict blocks save. Corrected result opens the save toolbar.
- Draft reload, blank/duplicate name refusals, exact teacher-only named version, unchanged original and independent
  browser. The stored result is `MANUAL_REVIEW` and every assignment matches the expected one-field delta.
- Utilities has only school editing/generation. Exact browser school copy, real Quick solve on Free, independent
  verification and complete 56-lesson candidate; exact definition/result/provenance and five-second budget saved.
- Desktop and 390px screenshots inspected; root fits, inspector/save toolbar do not overlap, zero browser errors.

The [first attempt](../../../playwright/demo-timetable-experience/public/free-20261016-1/ledger.md) failed because
the harness awaited another-school completion notification while the active-school candidate had already opened.
Actual solve/verification succeeded. The test now asserts that active-school candidate and toolbar. No production
fix was needed; failed screenshots, [XML](public-browser-attempt-1.xml), [finding](public-browser-attempt-1-finding.md)
and log remain preserved. Same-image restart cleared its disposable owners before the full passing repeat.

The separate [post-restart real Chrome proof](../../../playwright/demo-timetable-experience/public/free-restart-20261016-1/ledger.md)
and [XML](public-browser-restart.xml) pass: exact saved timetable, selected version and browser school copy survive
the actual provider restart. Temporary browser state stayed outside source/evidence and was removed after proof.

Both [before](public-before-cleanup/cleanup-public-checks.json) and
[after](public-after-cleanup/cleanup-public-checks.json) final cleanup checks pass 40 consecutive stateless health/static
reads, nine edge/Origin/CSRF/HTTP-redirect probes, all 40 exact static hashes, three exact school definitions and six
exact timetable results/current kernel fingerprint. These requests create no sessions.

The opt-in main journey command was:

```sh
./mvnw -q -pl timetable-workspace -am test-compile failsafe:integration-test failsafe:verify \
  -Dit.test=DemoTimetableRemoteIT -Dfailsafe.failIfNoSpecifiedTests=false \
  -Ddemo.timetable.remote=true -Ddemo.timetable.phase=main \
  -Ddemo.run.base=https://demo-smoke-production.up.railway.app \
  -Ddemo.timetable.walkthrough.dir=../output/playwright/demo-timetable-experience/public/free-20261016-2 \
  -Ddemo.timetable.storage=/private/tmp/timetable-remote-browser-state.json
```

The restart proof uses `-Ddemo.timetable.phase=after-restart`, its unique walkthrough directory, and
`-Ddemo.timetable.saved=../output/playwright/demo-timetable-experience/public/free-20261016-2/saved-timetable.json`.
The test issues no provider mutation. Existing local 185-unit/3-review-CLI/10-Node/integrated-browser evidence retains
its original scope and was not unnecessarily repeated for the shell selection and operational deployment.

## Final cleanup and limits

[Same-image final restart](cleanup-final.json) retains the deployment/image, artifact hash and kernel fingerprint,
changes the counter epoch, and physically verifies all six packaged pairs again before readiness. The last aggregate
records **zero active workspaces**; no new server workspace was created by the post-restart browser proof.
[Final events](events-final.jsonl), [pre-cleanup events](events-before-final-cleanup.jsonl), [identity proof](deployment-verification.json).
Public browser labels do not appear in application logs. All browser contexts/processes close; temporary browser
state and raw variable snapshots are removed. `git diff --check` passes. No rollback or unrelated-service action.

Overall **PASS for this authorized Free deployment and timetable experience**. Free sleeps when idle. This does not
establish always-on availability, cold full-school solve capacity, combined peak memory or paid-plan/spend qualification.
