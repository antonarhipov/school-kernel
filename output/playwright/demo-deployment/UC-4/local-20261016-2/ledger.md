# UC-4 browser-version walkthrough

Date: 2026-10-16T13:41:32.238198Z
Application: https://localhost:60445
Browser: real Chrome, Playwright; two isolated owners and one same-owner tab. Disposable data only.
Public TLS validates normally; local TLS is disposable. Build/source identity is in the execution manifest.

PASS | main 1-5/G1/G2: exact CRLF/Unicode file saved in a five-field file-origin envelope; unfinished object makes no server change; second tab list refreshes | [screenshot](01-exact-save.png) / [snapshot](01-exact-save.txt)

PASS | main 6/G3/G4: reload and selection preserve exact CRLF bytes; browser-local download matches the original file | [screenshot](02-reload-download.png) / [snapshot](02-reload-download.txt)

PASS | FAIL: org.opentest4j.AssertionFailedError: expected: <true> but was: <false> | [screenshot](failure.png) / [snapshot](failure.txt)

Overall: FAIL: org.opentest4j.AssertionFailedError: expected: <true> but was: <false>

Quota/SecurityError/file-read faults are explicitly injected in browser APIs. No server failure is claimed.
Contexts closed; local server stopped. Public sessions retained for expiry or explicit cleanup. UC-5 consumes saved-use tracking; model checks do not claim that future actor journey.
