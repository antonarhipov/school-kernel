# Definition-input hardening — Railway Free deployment

2026-10-17 (Europe/Tallinn). User explicitly requested deployment of the completed recommendations 1–5 and 8 to
Railway Free. No commit or plan/resource/variable/billing change was requested or performed.

## Payload and destination

Existing project `school-kernel-free-smoke` / environment `production` / service `demo-smoke`:

- Project: `13d509af-1436-4024-bb64-f9f78fa439fc`.
- Environment: `a41a9c0e-ec3f-4ef4-9e08-996d998d1815`.
- Service: `672cabae-b54e-4965-bbb2-5236bec81c33`.
- Public URL: https://demo-smoke-production.up.railway.app/workspace/.
- Preceding deployment: `8891ef46-d92d-4e51-8efc-66690e81e6de`, sleeping normally at preflight.
- New deployment: **`2ecc350c-5519-4a0d-ba20-a4f19edd5137` — SUCCESS**.
- Image: `sha256:53604e78f073345453d825b7264e2d0d9af9c41b6648dc7159324ca220232ae2`.

The production-only stage is `/private/tmp/definition-hardening-production-20261017-1`.
[Payload record](payload.json), [177-file manifest](upload-manifest.json), [exact 12-file delta](production-delta.json),
[retained source archive](production-source.tar.gz), SHA-256
`8be4e1bc7ab45fd25730d0351670c60f4a95dce8ce3bbe48401fbac0fb31921d`.
All application sources match the final isolated verification manifest; unchanged build/launcher inputs match the
preceding release. Tests, Git metadata, credentials, operator configuration, real-school examples and prior output
are excluded. Every staged file still matches the shared checkout; unrelated dirty changes are preserved.
The existing local-cleanup release is retained exactly, with the approved autosave integration added.

The read-only [initial topology check](boundary-before.json) passes. Provider reports **FREE**, one CPU,
500,000,000 bytes, one EU replica and sleeping enabled. No provider settings were altered.
[Before](provider-before.json), [after](provider-after.json), [plan before](plan-before.json),
[plan after](plan-after.json), [exact comparison](provider-integrity.json).

## Deployment and public verification

Railway CLI 5.69.0 uploaded the pinned stage with explicit project/environment/service IDs and `up --detach`.
[Upload result](deployment-upload.log), [provider ready](deployment-ready.json), [build progress](build-progress-2.jsonl).
All eight bundled pairs pass real kernel verification during startup; all four schools report VERIFIED in the
[initial readiness event](startup-ready.json).

The existing local evidence remains applicable: 86 workspace units and six packaged HTTP/browser integration
scenarios, followed by the six hardening units and final complete editor journey again. No production bytes changed
after those checks. The public-only test harness adapts that same journey by taking process evidence from provider
events rather than a local wrapper file; its slow-socket local test is not run against the public edge.
[Exact harness](DefinitionInputHardeningPublicIT.java), [compilation log](public-harness-compile.log),
[public test XML](TEST-org.schoolkernel.workspace.DefinitionInputHardeningPublicIT.xml), [test log](public-browser.log).

The complete [public Chrome ledger](../../../playwright/definition-input-hardening/UC-1/public-20261017-1/ledger.md)
is **PASS**: eight edits cause one durable write; immediate reload preserves the exact latest incomplete JSON;
oversized multibyte text and an explicit Save preserve prior raw storage; injected quota refusal pauses automatic
retry; explicit recovery retains exact text/name and clears the stale notice; actual 400-digit HTTP input gets 422
without workspace mutation; a real verified 56-lesson copy saves exactly and its feedback fits 390px. Browser error
list is empty. Desktop oversize and mobile checked-copy screenshots were opened and inspected.

[Public process events](public-events.jsonl) corroborate one successful visitor verification, zero visitor solves,
the ordinary invalid-definition refusal and no unexpected-request failure. Per-session/global immediate admission
and release were already exercised through real incomplete TLS bodies on the pinned local build; those local
assertions are not misrepresented as public load testing.

[Before-cleanup public checks](public-before-cleanup/cleanup-public-checks.json) pass 40 consecutive stateless reads,
nine edge probes, all **41 exact static assets** and all **eight exact timetable pairs**. No cookie is created.

## Restart, cleanup and final verdict

An actual [same-image provider restart](restart.json) clears the disposable test owner. The
[fresh readiness event](restart-startup-ready.json) has a new counter epoch, identical service artifact/kernel and
effective settings, and four VERIFIED schools. [Final aggregate](cleanup-final.json) proves zero active workspaces,
running solve/verification processes, waiting work and visitor cache entries. Browser contexts close; no operator
browser storage or database is touched.

[Final stateless public checks](public-after-cleanup/cleanup-public-checks.json) repeat all 40 reads, nine edge
probes, 41 static hashes and eight timetable pairs successfully after restart without creating a session.
[Final isolated ingress](boundary-final.json) passes. Plan and complete service manifest remain unchanged.

**Overall: PASS — the requested version is deployed and healthy on the existing Free plan.**
This functional deployment does not claim larger-school capacity, aggregate peak memory or paid-launch qualification.
