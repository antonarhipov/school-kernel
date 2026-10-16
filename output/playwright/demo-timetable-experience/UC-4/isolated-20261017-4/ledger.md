# UC-4 local-artifact cleanup walkthrough

2026-10-16T22:18:07.595491Z; real installed Chrome / Java Playwright; https://localhost:58645; isolated disposable browser owners. Deployment private packaged source pinned in execution ledger.

- FAIL **FAIL: com.microsoft.playwright.TimeoutError: Error {
  message='Timeout 30000ms exceeded.
  name='TimeoutError
  stack='TimeoutError: Timeout 30000ms exceeded.
    at _ProgressController.run (/private/var/folders/c_/5_n98lpd65j__pbwgzctxx_80000gn/T/playwright-java-1084780061092468718/package/lib/coreBundle.js:12360:32)
    at DispatcherConnection.dispatch (/private/var/folders/c_/5_n98lpd65j__pbwgzctxx_80000gn/T/playwright-java-1084780061092468718/package/lib/coreBundle.js:19135:44)
}
Call log:
-   - waiting for locator("#school-selector option[value=full]")
**. Expected: specified outcome with exact value/privacy/non-mutation assertions above. Observed: assertion failed as recorded above. [Screenshot](failure.png), [rendered snapshot](failure.txt).

Overall: FAIL: com.microsoft.playwright.TimeoutError: Error {
  message='Timeout 30000ms exceeded.
  name='TimeoutError
  stack='TimeoutError: Timeout 30000ms exceeded.
    at _ProgressController.run (/private/var/folders/c_/5_n98lpd65j__pbwgzctxx_80000gn/T/playwright-java-1084780061092468718/package/lib/coreBundle.js:12360:32)
    at DispatcherConnection.dispatch (/private/var/folders/c_/5_n98lpd65j__pbwgzctxx_80000gn/T/playwright-java-1084780061092468718/package/lib/coreBundle.js:19135:44)
}
Call log:
-   - waiting for locator("#school-selector option[value=full]")


Browser contexts close; local server stops; no operator storage touched. Public server cleanup is recorded separately.
