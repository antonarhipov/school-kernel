# UC-2 checkpoint - native Railway deployment

- Date: 2026-10-18. Status: APPROVED from local Linux container, actual public Chrome/HTTP, provider settings and restart evidence.
- Base: `b9458eaa7c2957d8766a33b481949975e255ab90`; no commit requested.
- Deployment: `15bb7d2d-6472-4f46-8197-d9c47c75ea6a`, school-kernel-free-smoke / production / demo-smoke.
- Public URL: https://demo-smoke-production.up.railway.app/workspace/.
- [Exact production manifest](../../../output/execution/demo-startup/railway-native-20261018/upload-manifest.json), [submission](../../../output/execution/demo-startup/railway-native-20261018/submission.json), [result](../../../output/execution/demo-startup/railway-native-20261018/results.json), [full ledger](../../../output/execution/demo-startup/railway-native-20261018/ledger.md).

## Implementation

`deploy/demo/Dockerfile` defaults to the Linux native web runtime, built with pinned GraalVM CE 25.0.1, compatibility CPU target, two compiler threads and 4 GiB compiler heap. The separate kernel stays on Java 25. `--target jvm` retains the previous web option. New `deploy/demo/start-native` keeps the existing heap setting, private umask, nonroot runtime and exec/PID1 SIGTERM behavior. `.dockerignore` includes the launcher. Root/demo READMEs document build and launch; `tools/demo/verify_native_container.py` verifies the actual Linux artifact. No application UI or kernel implementation changed in UC-2.

## Contract coverage and validation

- Main steps 1-2/G3:Linux AMD64 native and JVM fallback builds pass. Native container at 1 CPU / 500 MB passes native PID1/ELF/nonroot checks; exact static/sample/export values, two-owner isolation, real kernel verify/review, invalid input/Origin/forwarding non-mutation, readiness and SIGTERM/no-OOM. [Passing report](../../../output/execution/demo-startup/railway-native-20261018/container-2/results.json). First Rosetta-path assertion failure retained and corrected only in harness.
- Steps 3-4/G1-G2:verified existing Free plan, settings and isolated ingress; staged only 199 allowed production inputs, hashed before upload and checked again after verification. First Railway build failed during native analysis without diagnostic; previous healthy JVM release remained in place. Identical-payload retry succeeded without settings or source change.
- Steps 5-6/G3:[Full public Chrome journey](../../../output/playwright/demo-startup/UC-2/railway-native-20261018-2/ledger.md) passes bundled timetable, desktop/mobile JSON recovery/refusal, complete fictional school save, real QUICK generation, explicit timetable naming, exact one-lesson result/reload, and two-owner isolation. Significant mobile/refusal/saved screenshots inspected. Browser errors absent. First walkthrough's missing replacement confirmation was a retained harness error; the full rerun passes with explicit confirmation.
- Steps 6-7/G2-G4:all 60 static assets and four definitions/eight results match pinned input values before and after final restart; foreign Origin gets 403 without session cookie. Native logs show AOT-processed WorkspaceApplication, PID1 `/opt/demo/workspace`, sole demo profile, real kernel work and FEASIBLE generation. Two restarts preserve deployment/image/native artifact hash and reset disposable state; final health UP and deployment SUCCESS. Complete build/deploy manifest, Free plan,1 CPU / 500 MB, single EU sleeping replica and domain/ingress unchanged.
- Logged startup on the two native restarts: **0.141 s / 0.154 s**. Earlier same-service JVM logs: **4.717 /4.586 /4.858s**. Local ARM64 medians remain UC-1 evidence. These deployed observations do not measure sleeping-provider wake-up, establish a controlled benchmark or qualify full capacity.

## Limits and preservation

The known native Logback unknown package-version warning remains nonfatal (Railway tags it error); actual configured/structured logs work. Initial deployment's early framework lines were not retained, so native startup/profile evidence comes from the two complete same-image restart logs. SSH read-only identity check was unavailable without a registered account key; no account key was added. Runtime identity is confirmed by startup paths and native artifact hash.

The final existing 197-test pass covers UC-1 application changes; UC-2 packaging is verified through both Docker builds, actual container checks, public browser/HTTP and provider checks. `git diff --check` passes. All UC-1 work and the pre-existing pyc change are preserved. Browser contexts closed, disposable browser data cleared, final restart removes test sessions/cache/counters. No outstanding handoffs; no full load/cost/solver-throughput qualification claimed.
