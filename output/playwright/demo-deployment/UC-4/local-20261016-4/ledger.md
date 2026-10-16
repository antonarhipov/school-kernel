# UC-4 browser-version walkthrough

Date: 2026-10-16T13:46:55.383380Z
Application: https://localhost:61048
Browser: real Chrome, Playwright; two isolated owners and one same-owner tab. Disposable data only.
Public TLS validates normally; local TLS is disposable. Build/source identity is in the execution manifest.

PASS | main 1-5/G1/G2: exact CRLF/Unicode file saved in a five-field file-origin envelope; unfinished object makes no server change; second tab list refreshes | [screenshot](01-exact-save.png) / [snapshot](01-exact-save.txt)

PASS | main 6/G3/G4: reload and selection preserve exact CRLF bytes; browser-local download matches the original file | [screenshot](02-reload-download.png) / [snapshot](02-reload-download.txt)

PASS | 1b/4b/6a: declined selection, file opening, revert and named replacement preserve exact open and saved text | [screenshot](03-declines.png) / [snapshot](03-declines.txt)

PASS | main 4-6/G2: confirmed named replacement stores exact edited text, saved-source derivation and explicit save time | [screenshot](04-replace.png) / [snapshot](04-replace.txt)

PASS | 4a/G1/G4: first syntax line/column, reserved/invalid labels and non-object roots refused; malformed text still downloads exactly | [screenshot](05-validation.png) / [snapshot](05-validation.txt)

PASS | 5a/minimal: injected browser quota and SecurityError preserve overwritten entry and open text; exact Download remains available | [screenshot](06-storage-failure.png) / [snapshot](06-storage-failure.txt)

PASS | 1c/4a: oversized UTF-8 file/editor and injected unreadable file refused; prior storage and text retained | [screenshot](07-file-failures.png) / [snapshot](07-file-failures.txt)

PASS | FAIL: org.opentest4j.AssertionFailedError: expected: <true> but was: <false> | [screenshot](failure.png) / [snapshot](failure.txt)

Overall: FAIL: org.opentest4j.AssertionFailedError: expected: <true> but was: <false>

Quota/SecurityError/file-read faults are explicitly injected in browser APIs. No server failure is claimed.
Contexts closed; local server stopped. Public sessions retained for expiry or explicit cleanup. UC-5 consumes saved-use tracking; model checks do not claim that future actor journey.
