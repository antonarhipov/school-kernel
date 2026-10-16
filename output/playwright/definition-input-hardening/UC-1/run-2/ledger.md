# UC-1 definition input hardening walkthrough

Date: 2026-10-16T22:17:47.494200Z
Application: https://localhost:58643
Packaged workspace/kernel, real Chrome via Java Playwright, real TLS; isolated disposable browser data; no database.

- PASS **UC-1 main 1-2: eight rapid edits produce one durable write; an immediate reload flushes the exact latest incomplete JSON**. Expected: specified outcome with exact value/privacy/non-mutation assertions above. Observed: assertions passed. [Screenshot](01-debounced-reload.png), [rendered snapshot](01-debounced-reload.txt).

- PASS **UC-1 extension 2a/G2/G5: a multibyte paste above 2 MiB remains editable, visibly explains memory-only retention, performs zero storage writes and preserves the exact prior raw browser document**. Expected: specified outcome with exact value/privacy/non-mutation assertions above. Observed: assertions passed. [Screenshot](02-oversized-editor.png), [rendered snapshot](02-oversized-editor.txt).

- PASS **UC-1 extension 2b/G5: injected browser quota refusal preserves prior bytes, pauses automatic retry across later edits, and explicit close retries successfully after recovery; exact text/name survive reload**. Expected: specified outcome with exact value/privacy/non-mutation assertions above. Observed: assertions passed. [Screenshot](03-quota-recovery.png), [rendered snapshot](03-quota-recovery.txt).

- PASS **UC-1 extension 4a/G2: the actual packaged HTTP endpoint rejects the 400-digit integer with 422, starts no kernel process and retains exact server state**. Expected: specified outcome with exact value/privacy/non-mutation assertions above. Observed: assertions passed. [Screenshot](04-malformed-http.png), [rendered snapshot](04-malformed-http.txt).

- PASS **UC-1 main 3-5/G4/G6: real kernel verification saves a separate exact 56-lesson school without solving or resurrecting the pending editor; feedback fits 390px**. Expected: specified outcome with exact value/privacy/non-mutation assertions above. Observed: assertions passed. [Screenshot](05-checked-school-mobile.png), [rendered snapshot](05-checked-school-mobile.txt).

Overall: PASS

Quota failures are explicitly injected at the browser storage boundary; successful checks use the real kernel. Contexts close and the private server stops.
