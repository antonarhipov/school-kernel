# Incomplete local walkthrough

PASS | UC-2 main/G2/G4: Full previews in EMPTY; no authoring entry; persistent retention/privacy; Secure/HttpOnly/Strict cookie | [screenshot](01-empty.png) / [snapshot](01-empty.txt)

PASS | UC-2 ownership / UC-3 enabling: Full accepted privately without a visitor process; other browser stays EMPTY | [screenshot](02-full.png) / [snapshot](02-full.txt)

PASS | UC-2 retained narrow inspection: no horizontal page overflow at 390 x 844 | [screenshot](03-narrow.png) / [snapshot](03-narrow.txt)

PASS | UC-2 G1/G7 / stale tab: private repair staging preserves exact Current; old shared-tab version cannot replace it | [screenshot](04-private-repair.png) / [snapshot](04-private-repair.txt)

PASS | UC-2 private export: download contains exact Current while the unaccepted repair remains excluded | [screenshot](05-export.png) / [snapshot](05-export.txt)

PASS | UC-2 G7: real private definition import and asynchronous run; foreign/random IDs are indistinguishable; only owner cancels | [screenshot](06-private-cancel.png) / [snapshot](06-private-cancel.txt)

PASS | UC-2 G2/G3/G6 | Raw TLS host/forwarded spoof checks; health has no session; authoring 404; missing CSRF 403

PASS | UC-2 restart: old cookie cannot restore work; loss explanation and explicit start/import choices; browser text survives | [screenshot](07-ended.png) / [snapshot](07-ended.txt)
See the matching Failsafe report and retained server logs. Server stopped.
