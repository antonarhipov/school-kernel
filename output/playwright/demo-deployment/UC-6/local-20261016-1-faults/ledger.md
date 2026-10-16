# UC-6 recorded visitor walkthrough

Date: 2026-10-16T15:23:47.024034Z
Application: https://localhost:52747
Build: pinned private packaged JAR copies; production input hashes in execution/UC-6. Browser: real installed Chrome / Java Playwright, isolated disposable contexts. No database or operator school data.

- PASS **3a/G2: real bounded queue refuses a third distinct request with busy 429/Retry-After and exact unchanged draft; negative transport response retained**. Expected: specified outcome with exact value/privacy/non-mutation assertions above. Observed: assertions passed. [Screenshot](01-queue-refusal.png), [rendered snapshot](01-queue-refusal.txt).

- PASS **5c/G6/G7: actual process watchdog terminates the stalled solve; failure restores draft and never publishes a result; queued/running last-subscriber cancellation returns its draft**. Expected: specified outcome with exact value/privacy/non-mutation assertions above. Observed: assertions passed. [Screenshot](02-watchdog.png), [rendered snapshot](02-watchdog.txt).

- PASS **FAIL: com.microsoft.playwright.TimeoutError: Error {**. Expected: specified outcome with exact value/privacy/non-mutation assertions above. Observed: assertions passed. [Screenshot](failure.png), [rendered snapshot](failure.txt).

Overall: FAIL: com.microsoft.playwright.TimeoutError: Error {
  message='Timeout 30000ms exceeded.
  name='TimeoutError
  stack='TimeoutError: Timeout 30000ms exceeded.
    at _ProgressController.run (/private/var/folders/c_/5_n98lpd65j__pbwgzctxx_80000gn/T/playwright-java-4155973321231841075/package/lib/coreBundle.js:12360:32)
    at DispatcherConnection.dispatch (/private/var/folders/c_/5_n98lpd65j__pbwgzctxx_80000gn/T/playwright-java-4155973321231841075/package/lib/coreBundle.js:19135:44)
}

Main journey uses actual 5-second Quick and 60-second Thorough for both operations. Fault method uses explicitly identified local process/configuration injection. Unit tests provide deterministic reservation, stale completion, subscriber expiry and identity boundaries; they are not presented as clicks. UC-7/8 own complete repair authoring and acceptance journeys; this walkthrough obtains and opens proposals only. Contexts close; local runtime processes stop; public disposable state requires same-image cleanup.
