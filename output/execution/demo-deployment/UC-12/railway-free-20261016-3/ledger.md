# Railway Free smoke: browser pass, restart failure

2026-10-16. Base `d55ab636558224f2d33a5e71313e840dd9486428`, dirty `demo` checkout. User authorized the
isolated Free test and explicitly trusted `ssh.railway.com` after its exact fingerprint was presented. No billing,
plan upgrade, unrelated project mutation, commit, database or real school input was used.

## Deployment and source

Project `13d509af-1436-4024-bb64-f9f78fa439fc`, environment `a41a9c0e-ec3f-4ef4-9e08-996d998d1815`, service
`672cabae-b54e-4965-bbb2-5236bec81c33`. Public URL: https://demo-smoke-production.up.railway.app/workspace/ .
The passing browser deployment and subsequent in-place restart share ID `43fe1c2b-5046-427c-ae9d-1902b7678e1b`.
The [actual final manifest](passing-deployment-manifest.json) reports SUCCESS, one EU replica, one CPU,
500,000,000 bytes, sleeping enabled. It records provider image digest
`sha256:b9539fbb4af77705fd440357d8881a0e84d3b8a531651b0e4a907dea7da59e9c`.
The earlier Docker build's Linux image manifest digest was
`sha256:6ec113bc34a875e854b92208b7bf052f1ece7f530754b10190230a61e69293aa`; these are distinct provider/build identifiers.

The [149-file production upload manifest](upload-context-manifest.json) is unchanged by the configuration-only
redeploy. [Verification](manifest-verification.json) checks every file against both checkout and sanitized stage,
and identifies the browser-test source hash. No dirty checkout upload occurred. [Runtime JAR hashes and cgroup
counters](runtime-hashes-and-memory.txt) identify the deployed artifacts. After the browser journey, cgroup
memory.max/current/peak were 499,998,720 / 190,496,768 / 207,659,008 bytes respectively. This process-lifetime
measurement covers startup and this sample-only test, not solver or intended visitor capacity.

## Host-filter fix and browser findings

Railway supplies X-Forwarded-Host on ordinary requests. The prior filter refused its presence outright. The fixed
filter accepts a single value only from an explicitly trusted peer and only when it equals the independently
allowlisted native Host. It never supplies authority. Scheme, duplicate headers, foreign Origin and Forwarded
remain guarded. The [focused tests](host-fix-tests.log) passed 8/8 (origin and session boundaries).
Diagnostic logging contains booleans only at DEBUG; the final deployment uses normal INFO logging.

The first fixed-image Chrome attempt still showed a blank styled shell; its [failure screenshot](../../../../playwright/demo-deployment/free-smoke/20261016-3/failure.png)
is retained. [Diagnostic attempt 4](../../../../playwright/demo-deployment/free-smoke/20261016-4/failed-attempt.md)
and [network output](../../../../playwright/demo-deployment/free-smoke/20261016-4/network.txt) observed a 403 even on
the initial page. Concurrent static-module checks also saw intermittent 403s. Sampling only the container's internal
proxy sockets during bounded static requests found 21 exact peers, listed in [connection evidence](concurrent-proxy-peers.json).
The eight additional observed peers were added individually; no subnet or unobserved address was trusted.
All [29 static modules](module-probes-after-peer-update.json) then returned 200 with text/javascript.

The real installed Chrome journey subsequently **passed**, using normal certificate validation and two disposable
browser contexts. [Browser ledger](../../../../playwright/demo-deployment/free-smoke/20261016-5/ledger.md),
[Maven command output](browser-smoke-5.log), [Failsafe result](browser-result-5.txt): one test, zero failures/errors.
It verified initial EMPTY owners and Full preview; Secure/HttpOnly/Strict cookie and HSTS; all three exact packaged
definition/result pairs; private changes between owners; shared-owner tab; missing-CSRF/foreign-Origin refusals
without mutation; newcomer 503 DEMO_FULL at the two-owner cap; and read-only JSON/no document overflow at 390px.
Desktop and narrow screenshots were inspected: controls and timetable render, matrix scroll remains internal,
and the narrow notice/JSON read-only treatment are visible. No page errors or console errors were recorded.
These checks exercise UC-2's public session subset and sample enabling work, not its complete lifecycle.

Passing command, from the repository root (output redirected to `browser-smoke-5.log`):

```sh
./mvnw -q -pl timetable-workspace -am \
  -Dtest=NoSuchTest -Dsurefire.failIfNoSpecifiedTests=false \
  -Dit.test=DemoRemoteSmokeIT -Dfailsafe.failIfNoSpecifiedTests=false \
  -Ddemo.remote.url=https://demo-smoke-production.up.railway.app \
  -Ddemo.walkthrough.dir=/Users/anton/IdeaProjects/school-kernel/school-kernel/output/playwright/demo-deployment/free-smoke/20261016-5 verify
```

[Edge probes](edge-probes.json) on the fixed image observed Railway overwriting client X-Forwarded-Proto and
X-Forwarded-Host, refusing forwarded/foreign Origin, refusing forged native authority at the edge, and redirecting
public HTTP. [Direct-container probes](direct-container-probes.txt) verified health creates no session,
health authority cannot access workspace APIs, and untrusted direct HTTP cannot forge proxy HTTPS.
Observed provider handling does not establish a stable forwarding/source-address contract.

## Restart finding and disposition

After the passing journey, `railway restart --yes --json` successfully restarted this exact deployment to clear
the two disposable in-memory workspaces. A subsequent health/security check failed immediately on public health
403. The [15-request follow-up](post-restart-health.json) recorded **13 responses 200 and two responses 403**,
all without session cookies, while the provider reported SUCCESS. This demonstrates that the empirical exact-peer
allowlist does not provide reliable access after restart. The additional rejecting peer was not inspected after
SSH cleanup; its address and precise rejection flags are not claimed. Do not widen trust by guessing a subnet.
A stable, justified Railway proxy trust configuration and a repeated restart/browser journey remain required.

**Overall: startup/sample and pre-restart browser smoke PASS; restart reliability FAIL / NEEDS_REVISION.**
Neither UC-2 nor UC-12 nor public launch is approved. Free can run this intermediate startup/sample workload;
solver, aggregate capacity, sleeping wake-up, full lifecycle/expiry and always-on requirements remain unqualified.
The sleeping-enabled isolated service is retained and may intermittently refuse requests. Both test browser
contexts are closed; the explicit restart cleared their workspace state. [Cleanup](ssh-cleanup.json) records
successful revocation of temporary public key `06a64fc3-cd61-40fa-acda-96a2ecf8da7e` and deletion of the local
private/public key and registration input. The operator-approved gateway known-host entry remains trusted.
