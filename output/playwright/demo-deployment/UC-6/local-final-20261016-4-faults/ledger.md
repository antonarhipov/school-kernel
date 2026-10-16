# UC-6 recorded visitor walkthrough

Date: 2026-10-16T15:46:14.404701Z
Application: https://localhost:55499
Build: pinned private packaged JAR copies; production input hashes in execution/UC-6. Browser: real installed Chrome / Java Playwright, isolated disposable contexts. No database or operator school data.

- PASS **3a/G2: real bounded queue refuses a third distinct request with busy 429/Retry-After and exact unchanged draft; negative transport response retained**. Expected: specified outcome with exact value/privacy/non-mutation assertions above. Observed: assertions passed. [Screenshot](01-queue-refusal.png), [rendered snapshot](01-queue-refusal.txt).

- PASS **5c/G6/G7: actual process watchdog terminates the stalled solve; failure restores draft and never publishes a result; queued/running last-subscriber cancellation returns its draft**. Expected: specified outcome with exact value/privacy/non-mutation assertions above. Observed: assertions passed. [Screenshot](02-watchdog.png), [rendered snapshot](02-watchdog.txt).

- PASS **5c: actual process exit without result produces safe failure, no proposal/cache entry, and retains complete input**. Expected: specified outcome with exact value/privacy/non-mutation assertions above. Observed: assertions passed. [Screenshot](03-crash.png), [rendered snapshot](03-crash.txt).

- PASS **5a: actual valid but impossible initial school ends as Not found within budget, retains exact draft, and publishes no partial timetable**. Expected: specified outcome with exact value/privacy/non-mutation assertions above. Observed: assertions passed. [Screenshot](03b-not-found.png), [rendered snapshot](03b-not-found.txt).

- PASS **5a: actual impossible compiled repair ends as Not found within budget, retains Current exactly and offers ordinary Quick/Thorough efforts without a special retry**. Expected: specified outcome with exact value/privacy/non-mutation assertions above. Observed: assertions passed. [Screenshot](03c-repair-not-found.png), [rendered snapshot](03c-repair-not-found.txt).

- PASS **3b/G7/G8: actual configured daily limit cannot cover complete 53-second reservation, refusing before run creation; exact draft retained and packaged sample still works**. Expected: specified outcome with exact value/privacy/non-mutation assertions above. Observed: assertions passed. [Screenshot](04-daily.png), [rendered snapshot](04-daily.txt).

- PASS **G1/G9: configuration-file reuse switch disabled on real packaged service; identical requests run twice with different computation provenance and freshly verified proposals**. Expected: specified outcome with exact value/privacy/non-mutation assertions above. Observed: assertions passed. [Screenshot](05-disabled.png), [rendered snapshot](05-disabled.txt).

Overall: PASS

Main journey uses actual 5-second Quick and 60-second Thorough for both operations. Fault method uses explicitly identified local process/configuration injection. Unit tests provide deterministic reservation, stale completion, subscriber expiry and identity boundaries; they are not presented as clicks. UC-7/8 own complete repair authoring and acceptance journeys; this walkthrough obtains and opens proposals only. Contexts close; local runtime processes stop; public disposable state requires same-image cleanup.
