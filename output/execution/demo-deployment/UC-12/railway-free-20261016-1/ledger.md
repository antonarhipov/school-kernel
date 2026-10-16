# Isolated Railway Free smoke attempt 1

2026-10-16. Base `d55ab636558224f2d33a5e71313e840dd9486428`, dirty `demo` checkout. User explicitly authorized
the first test on Free and completed Railway CLI browserless authentication. No commit, billing/plan mutation,
message to another person, database, real school input or unrelated project mutation occurred.

## Observed deployment

- Project `13d509af-1436-4024-bb64-f9f78fa439fc`, service `672cabae-b54e-4965-bbb2-5236bec81c33`, environment
  `a41a9c0e-ec3f-4ef4-9e08-996d998d1815`. [Exact settings](identity.json).
- Deployment `a8ceaa5c-4141-4835-bcab-58f4cfc7d4ce`: **SUCCESS**. Domain
  `https://demo-smoke-production.up.railway.app`; single EU replica (`europe-west4-drams3a`), one CPU / 500,000,000
  bytes. Authenticated API returned plan FREE and account maximum one CPU / 512,000,000 bytes.
- Source uploaded with `railway up --detach` from the explicitly staged production-only directory. Its
  [149-file manifest](upload-context-manifest.json) binds 1,680,788 bytes of production inputs. No examples, tests,
  Git metadata, operator files, previous build/evidence output or credentials entered the upload.
- Runtime Linux amd64 image digest from build logs:
  `sha256:42925ec541bd012f29124766bcd67492ebd3c238551469ebb913c2178f2abb56`.
- Real packaged startup verification: **three VERIFIED**, elapsed 1000 / 1145 / 1355 ms. Spring application startup
  8.067 s (8.798 s process lifetime). Railway readiness `/healthz` passed. See
  [deployment manifest and application logs](deployment-and-startup.txt).
- [Raw platform CPU/memory series](metrics-raw.json): highest recorded memory sample 0.202960896 GB; latest sample
  0.168833024 GB. This sampled observation excludes a continuous peak guarantee and proves no solver/load capacity.

## Provider behavior and configuration

The API rejected `railwayConfigFile:"railway.json"` with “Config as Code … is deprecated”. It also rejected
`sleepApplication:false`: “Free plan services must have … set to … true”. No cron workaround or plan upgrade was
used. Supported service APIs applied Dockerfile path, readiness, single replica, restart/shutdown settings and
Free resource limits. `railway service scale us-west=0 eu-west=1` selected the actual EU replica; merely setting the
legacy `region` field did not change the effective regional replica map.

Free sleeping is enabled. The full feature's Hobby/always-on and 2-vCPU/3-GB starting configuration remain separate.
The [TypeScript IaC export](railway.ts) records actual imported state without variable values. `config plan --json`
could not execute because the Railway TypeScript SDK is absent; no IaC apply is claimed. The actual deployment
manifest confirms the applied settings. The legacy JSON and IaC export were excluded from this CLI upload.

## Public boundary: incomplete

`DEMO_TRUSTED_PROXY_ADDRESSES` remains empty. [Public probes](closed-gate-probes.json) show HTTPS workspace
requests refused with 403 PUBLIC_REQUEST_DENIED and no session cookie, including supplied X-Forwarded-Proto,
Forwarded and X-Forwarded-Host. Public HTTP redirects 301 to HTTPS without a cookie. These closed-gate results do
not prove correct spoof handling after the real proxy is trusted.

The opt-in `DemoRemoteSmokeIT` compiled. The real deployed Chrome attempt has **one error, zero passes**: the
initial page is the safe 403 refusal instead of the sample UI. [Command log](browser-smoke.log),
[Failsafe result](browser-result.txt), [browser attempt](../../../../playwright/demo-deployment/free-smoke/20261016-1/failed-attempt.md)
and [inspected screenshot](../../../../playwright/demo-deployment/free-smoke/20261016-1/failure.png) retained.
Two-owner, sample UI, cookie, narrow-layout, CSRF and capacity assertions have not executed at this boundary.

Inspecting the actual internal proxy peer requires container diagnostics. A new temporary SSH key was created
locally and its public half registered as `school-demo-smoke-temporary` (ID `06a64fc3-cd61-40fa-acda-96a2ecf8da7e`).
No private key or account token is stored here. Railway's SSH gateway presented ED25519 host fingerprint
`SHA256:+S1xg92FrnHz6pY3bpkmh1OGtWQGNANXilPzlxA7B1g` for `ssh.railway.com`.
Automatic approval review rejected accepting this unverified host key. The official SSH documentation confirms
the gateway hostname but supplies no matching fingerprint; searches found no authoritative fingerprint.
No host-key trust was accepted and no alternative bypass was attempted. The exact-fingerprint operator decision
is pending. The temporary registered key must be revoked and both local key files removed after diagnostics.

## Disposition

**Deployed startup smoke PASS; public browser/session smoke BLOCKED.** This approves neither UC-2 nor UC-12,
public launch, full workload capacity or the feature. Sleeping-enabled isolated service remains deployed; the
unverified SSH prompt/key and sanitized staging directory remain pending continuation. Browser contexts closed;
no disposable local server/container remains. Complete proxy inspection, narrowly configure trust, run actual edge
spoof checks and repeat the two-owner browser smoke before reporting public success.

## Subsequent resolution (2026-10-16)

The operator explicitly trusted the displayed SSH gateway fingerprint. Diagnostics and narrowly configured
forwarded-host/observed-peer changes enabled a passing public two-owner Chrome smoke. The following service restart
exposed intermittent public 403s, so reliable access remains unresolved. The temporary SSH key was revoked and
its local files removed. See the [later evidence and final first-test disposition](../railway-free-20261016-3/ledger.md).
The earlier results above are retained as historical evidence.
