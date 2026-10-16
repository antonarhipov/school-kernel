# Deployed Free-plan browser smoke

Date: 2026-10-16T11:43:18.710655Z
Application: https://demo-smoke-production.up.railway.app; Chrome with normal TLS validation; two disposable browser contexts.

PASS | HTTPS with a valid public certificate; two EMPTY owners; Full preview; Secure/HttpOnly/Strict session cookie and HSTS | [screenshot](01-empty.png) / [snapshot](01-empty.txt)

PASS | All three samples open exact packaged definitions/results privately; second owner remains EMPTY | [screenshot](02-samples.png) / [snapshot](02-samples.txt)

PASS | Missing CSRF and foreign Origin refused without mutation; two-workspace cap refuses newcomer while both owners remain usable

PASS | Second owner explicitly opens Full; first owner's Small stays exact; another tab shares its owner's workspace | [screenshot](03-second-owner.png) / [snapshot](03-second-owner.txt)

PASS | 390px inspection fits viewport; JSON is read-only at narrow width | [screenshot](04-narrow.png) / [snapshot](04-narrow.txt)

Smoke PASS only. No solver, capacity, full UC-2 lifecycle or public launch qualification. Browser contexts close. Server workspaces remain until expiry or explicit operator restart.
