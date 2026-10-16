# Free smoke: edge diagnosis and refused attempt

2026-10-16. Same isolated project/service and production base as attempt 1. The user explicitly authorized trusting
`ssh.railway.com` after seeing the exact ED25519 fingerprint. That fingerprint was accepted; a stale initial SSH
connection then closed, and subsequent authenticated SSH diagnostics worked. No host-key check was disabled.

Controlled HTTPS probes and `/proc/net/tcp6` sampling established the exact internal peers in
[observed-proxy-peers.json](observed-proxy-peers.json). Only observed individual IPv4 addresses were configured;
no inferred subnet or blanket proxy trust. This is an empirical smoke allowlist, not a stable provider-wide contract.
Deployment `e58c2664-86e4-4c24-bf9f-0ebefab1a472` retained the same image/settings with the initial peer list.
[Sixty ordinary health probes](initial-health-probes.json) were all 403: the peer list alone did not resolve access.

Added safe DEBUG-only boolean origin-rejection diagnostics, with no payload, cookie, URL, private school data,
client address or session identity. Diagnostic deployment `9dc07d7e-db3b-4580-be21-a7291ea7d703` is bound by its
[source upload manifest](upload-context-manifest.json). All three startup verifications and Railway readiness passed.
[Runtime diagnostic evidence](rejection-diagnostics.log) shows **allowedHost=true, trustedProxy=true, secure=true,
forwardedPresent=false, forwardedHostPresent=true**. Railway supplies X-Forwarded-Host on an ordinary HTTPS request,
and the implementation's unconditional rejection of that header caused the refusal.

The production fix permits a single X-Forwarded-Host only from a trusted peer and only if it matches the native Host,
which must independently be allowlisted. It never replaces authority or relaxes origin/scheme/CSRF checks.
Mismatched, duplicate, untrusted or alternative-authority cases remain refused. Forwarded remains refused.
DEBUG configuration was removed before the fixed deployment; safe diagnostics remain off at normal INFO level.

Focused checks: initial DemoSessionTest 4/4 PASS. A preliminary test-only Mockito mock failed to self-attach in the
sandbox; replaced it with a JDK interface proxy representing the unbound owner, then boundary checks passed 3/3.
The extended test initially had an invalid MockHttpServletRequest overload; corrected before running tests.
Final production-fix checks: **DemoOriginBoundaryTest 4/4 and DemoSessionTest 4/4 PASS**.
See retained [first sandbox failure](origin-boundary-test.log), [pre-fix boundary pass](origin-boundary-test-2.log),
[compile correction evidence](host-fix-tests.log), and [final passing tests](../railway-free-20261016-3/host-fix-tests.log).

This attempt records diagnosis, not public browser success. The final fixed-image upload and repeated public smoke
belong to attempt 3. No plan/resource/billing change or unrelated-project mutation occurred. Temporary diagnostic
SSH key remains pending cleanup after final checks; no private key or provider credential is in evidence.
