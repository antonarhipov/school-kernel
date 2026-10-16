# UC-4 browser-version walkthrough

Date: 2026-10-16T14:01:25.449949Z
Application: https://demo-smoke-production.up.railway.app
Browser: real Chrome, Playwright; two isolated owners and one same-owner tab. Disposable data only.
Public TLS validates normally; local TLS is disposable. Build/source identity is in the execution manifest.

PASS | main 1-5/G1/G2: exact CRLF/Unicode file saved in a five-field file-origin envelope; unfinished object makes no server change; second tab list refreshes | [screenshot](01-exact-save.png) / [snapshot](01-exact-save.txt)

PASS | main 6/G3/G4: reload and selection preserve exact CRLF bytes; browser-local download matches the original file | [screenshot](02-reload-download.png) / [snapshot](02-reload-download.txt)

PASS | 1b/4b/6a: declined selection, file opening, revert and named replacement preserve exact open and saved text | [screenshot](03-declines.png) / [snapshot](03-declines.txt)

PASS | main 4-6/G2: confirmed named replacement stores exact edited text, saved-source derivation and explicit save time | [screenshot](04-replace.png) / [snapshot](04-replace.txt)

PASS | 4a/G1/G4: first syntax line/column, reserved/invalid labels and non-object roots refused; malformed text still downloads exactly | [screenshot](05-validation.png) / [snapshot](05-validation.txt)

PASS | 5a/minimal: injected browser quota and SecurityError preserve overwritten entry and open text; exact Download remains available | [screenshot](06-storage-failure.png) / [snapshot](06-storage-failure.txt)

PASS | 1c/4a: oversized UTF-8 file/editor and injected unreadable file refused; prior storage and text retained | [screenshot](07-file-failures.png) / [snapshot](07-file-failures.txt)

PASS | 1a/G2/G3: corrupt, unsupported and reserved-key entries stay unreadable, cannot execute and can only be explicitly deleted; samples remain immutable | [screenshot](08-corrupt.png) / [snapshot](08-corrupt.txt)

PASS | main 5/G7: cross-tab replacement refreshes newest-first list without overwriting open text; declined reload retains it, explicit reload adopts exact stored text | [screenshot](09-cross-tab.png) / [snapshot](09-cross-tab.txt)

PASS | main 6/6a/G3/minimal: declined, failed and confirmed single/all deletion preserve workspace, unrelated preferences and unsaved text; other tab retains removed version text | [screenshot](10-deletions.png) / [snapshot](10-deletions.txt)

PASS | G5/G6: actual loaded sample remains marked across selection/reload; 390px read-only editor has no page overflow | [screenshot](11-in-use-narrow.png) / [snapshot](11-in-use-narrow.txt)

PASS | G1/G6/G7: all local edits/saves/files/downloads send zero documents or labels and start zero visitor processes; saved copy is not falsely marked In use | [screenshot](12-no-server-work.png) / [snapshot](12-no-server-work.txt)

PASS | G6: real packaged Small repair leaves sample In use during proposal, then explicit acceptance changes definition and clears every dropdown mark | [screenshot](13-accepted-repair-tracking.png) / [snapshot](13-accepted-repair-tracking.txt)

PASS | G6: real same-definition accepted-bundle import shows its own origin, never a sample/saved mark; browser versions and second visitor remain intact | [screenshot](14-import-origin.png) / [snapshot](14-import-origin.txt)

Overall: PASS

Quota/SecurityError/save/delete/file-read faults are explicitly injected in browser APIs. No server failure is claimed.
Contexts closed; local server stopped. Public sessions retained for expiry or explicit cleanup. UC-5 consumes saved-use tracking; model checks do not claim that future actor journey.
