# UC-6 recorded visitor walkthrough

Date: 2026-10-16T15:27:32.603531Z
Application: https://localhost:52927
Build: pinned private packaged JAR copies; production input hashes in execution/UC-6. Browser: real installed Chrome / Java Playwright, isolated disposable contexts. No database or operator school data.

- PASS **main 1-4; 2b; G4/G5: real Quick initial requests promptly create private IDs, share one physical request, expose a small owner-only current-phase status**. Expected: specified outcome with exact value/privacy/non-mutation assertions above. Observed: assertions passed. [Screenshot](01-shared-running.png), [rendered snapshot](01-shared-running.txt).

- PASS **main 5-7; 4a; G4/G6: one subscriber cancels to its exact initial draft; the other obtains a complete independently verified private proposal, with status-only polling and no Current**. Expected: specified outcome with exact value/privacy/non-mutation assertions above. Observed: assertions passed. [Screenshot](02-computed-initial.png), [rendered snapshot](02-computed-initial.txt).

- PASS **2a; G3/G5: exact verified initial artifact creates a new private proposal, visibly reused, with original process duration separate from this request's wait**. Expected: specified outcome with exact value/privacy/non-mutation assertions above. Observed: assertions passed. [Screenshot](03-reused-initial.png), [rendered snapshot](03-reused-initial.txt).

- PASS **main 3/4; G2/G3: distinct Quick request waits behind Thorough with position one and an explicitly approximate refreshed start estimate**. Expected: specified outcome with exact value/privacy/non-mutation assertions above. Observed: assertions passed. [Screenshot](04-queued.png), [rendered snapshot](04-queued.txt).

- PASS **main 5/6; G1/G9: actual 60-second initial Thorough produces matching verified duration evidence, independent of Quick; queued cancellation launches no queued solve**. Expected: specified outcome with exact value/privacy/non-mutation assertions above. Observed: assertions passed. [Screenshot](05-thorough-initial.png), [rendered snapshot](05-thorough-initial.txt).

- PASS **FAIL: com.microsoft.playwright.TimeoutError: Error {**. Expected: specified outcome with exact value/privacy/non-mutation assertions above. Observed: assertions passed. [Screenshot](failure.png), [rendered snapshot](failure.txt).

Overall: FAIL: com.microsoft.playwright.TimeoutError: Error {
  message='Timeout 100000ms exceeded.
  name='TimeoutError
  stack='TimeoutError: Timeout 100000ms exceeded.
    at _ProgressController.run (/private/var/folders/c_/5_n98lpd65j__pbwgzctxx_80000gn/T/playwright-java-4155973321231841075/package/lib/coreBundle.js:12360:32)
    at DispatcherConnection.dispatch (/private/var/folders/c_/5_n98lpd65j__pbwgzctxx_80000gn/T/playwright-java-4155973321231841075/package/lib/coreBundle.js:19135:44)
}

Main journey uses actual 5-second Quick and 60-second Thorough for both operations. Fault method uses explicitly identified local process/configuration injection. Unit tests provide deterministic reservation, stale completion, subscriber expiry and identity boundaries; they are not presented as clicks. UC-7/8 own complete repair authoring and acceptance journeys; this walkthrough obtains and opens proposals only. Contexts close; local runtime processes stop; public disposable state requires same-image cleanup.
