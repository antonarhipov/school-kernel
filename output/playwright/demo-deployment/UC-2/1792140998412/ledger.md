# UC-2 local execution ledger

Date: 2026-10-16T08:58:22.073163Z
Application: packaged demo profile at https://localhost:62089; real packaged kernel; Chrome; two isolated browser contexts.
All imports, sessions, certificates and downloads are disposable. No PostgreSQL or operator school is used.

PASS | UC-2 main/G2/G4: Full previews in EMPTY; no authoring entry; persistent retention/privacy; Secure/HttpOnly/Strict cookie | [screenshot](01-empty.png) / [snapshot](01-empty.txt)

PASS | UC-2 ownership / UC-3 enabling: Full accepted privately without a visitor process; other browser stays EMPTY | [screenshot](02-full.png) / [snapshot](02-full.txt)

PASS | UC-2 retained narrow inspection: no horizontal page overflow at 390 x 844 | [screenshot](03-narrow.png) / [snapshot](03-narrow.txt)

PASS | UC-2 G1/G7 / stale tab: private repair staging preserves exact Current; old shared-tab version cannot replace it | [screenshot](04-private-repair.png) / [snapshot](04-private-repair.txt)

PASS | UC-2 private export: download contains exact Current while the unaccepted repair remains excluded | [screenshot](05-export.png) / [snapshot](05-export.txt)

PASS | UC-2 G7/4b/4c: exact bundle imports privately; declined clear preserves it; confirmed clear affects only that owner | [screenshot](05b-private-import-clear.png) / [snapshot](05b-private-import-clear.txt)

PASS | UC-2 G7: real private definition import and asynchronous run; foreign/random IDs are indistinguishable; only owner cancels | [screenshot](06-private-cancel.png) / [snapshot](06-private-cancel.txt)

PASS | UC-2 G2/G3/G6 | Raw TLS host/forwarded spoof checks; health has no session; authoring 404; missing CSRF 403

PASS | UC-2 4b: clearing the accepted timetable and repair returns to the definition panel while retaining browser versions | [screenshot](06b-clear-retains-browser.png) / [snapshot](06b-clear-retains-browser.txt)

PASS | UC-2 restart: old cookie cannot restore work; loss explanation and explicit start/import choices; browser text survives | [screenshot](07-ended.png) / [snapshot](07-ended.txt)

PASS | UC-2 explicit restart creates an empty workspace without replaying the failed mutation or accepting a proposal | [screenshot](08-start-again.png) / [snapshot](08-start-again.txt)

Local journey: PASS. Public Railway boundary and resource/spend qualification remain unverified. This ledger does not approve UC-2 or claim deployment. Browser contexts close; both server processes stop; temporary files are removed by JUnit.

Submission/presentation review, 2026-10-16: base `d55ab636558224f2d33a5e71313e840dd9486428`, with the
[exact source manifest](../../../../execution/demo-deployment/UC-2/20261016-final/source-sha256.txt) and
[technical ledger](../../../../execution/demo-deployment/UC-2/20261016-final/ledger.md). No application production
source changed after this full passing journey. Desktop clear/start-again and ended-session screenshots and the
390px screenshot were visually inspected: readable retained banner/panel, no horizontal page overflow, internal
matrix scrolling, disabled narrow editing, explicit restart/import controls. Stale/declined/active clear responses
were additionally asserted by exact aggregate equality; screenshots cannot establish those values by themselves.
Host/CSRF/raw-TLS checks have no useful visual state; their HTTP assertions and logs are the evidence. Fake-clock
expiry/cap checks belong to the unit ledger; the browser's process restart is an actual server stop/start.
