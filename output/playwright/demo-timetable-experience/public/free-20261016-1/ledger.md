# Public timetable demo walkthrough

2026-10-16T20:30:29.021966Z; actual public HTTPS https://demo-smoke-production.up.railway.app; installed Chrome / Java Playwright. Isolated disposable owners, browser-only copies. Deployment 0a3da7be-c9ac-4d08-9d84-005c8d6ff087; exact uploaded production manifest in execution/free-20261016-1. No provider/configuration mutation in this test.

- PASS **All six public variants open; full Estonian matrix is default; same-school switching retains filters and makes zero requests; obsolete controls absent**. Expected: specified outcome with exact value/privacy/non-mutation assertions above. Observed: assertions passed. [Screenshot](01-six-variants.png), [rendered snapshot](01-six-variants.txt).

- PASS **Public kernel validation identifies unqualified teacher and both colliding lessons, blocks save and reveals the filtered-out lesson through its conflict link**. Expected: specified outcome with exact value/privacy/non-mutation assertions above. Observed: assertions passed. [Screenshot](02-conflict.png), [rendered snapshot](02-conflict.txt).

- PASS **Actual room validation refuses unsuitable equipment; corrected draft is ready; editable inspector and save toolbar fit 390px without overlap**. Expected: specified outcome with exact value/privacy/non-mutation assertions above. Observed: assertions passed. [Screenshot](03-mobile-valid.png), [rendered snapshot](03-mobile-valid.txt).

- PASS **Blank/duplicate names refused; exact teacher-only result is saved as immutable named version, survives reload, preserves original and is absent from another browser**. Expected: specified outcome with exact value/privacy/non-mutation assertions above. Observed: assertions passed. [Screenshot](04-named-save.png), [rendered snapshot](04-named-save.txt).

- FAIL **FAIL: com.microsoft.playwright.TimeoutError: Error {
  message='Timeout 90000ms exceeded.
  name='TimeoutError
  stack='TimeoutError: Timeout 90000ms exceeded.
    at _ProgressController.run (/private/var/folders/c_/5_n98lpd65j__pbwgzctxx_80000gn/T/playwright-java-18063622772722717599/package/lib/coreBundle.js:12360:32)
    at DispatcherConnection.dispatch (/private/var/folders/c_/5_n98lpd65j__pbwgzctxx_80000gn/T/playwright-java-18063622772722717599/package/lib/coreBundle.js:19135:44)
}**. Expected: specified outcome with exact value/privacy/non-mutation assertions above. Observed: assertion failed as recorded above. [Screenshot](failure.png), [rendered snapshot](failure.txt).

Overall: FAIL: com.microsoft.playwright.TimeoutError: Error {
  message='Timeout 90000ms exceeded.
  name='TimeoutError
  stack='TimeoutError: Timeout 90000ms exceeded.
    at _ProgressController.run (/private/var/folders/c_/5_n98lpd65j__pbwgzctxx_80000gn/T/playwright-java-18063622772722717599/package/lib/coreBundle.js:12360:32)
    at DispatcherConnection.dispatch (/private/var/folders/c_/5_n98lpd65j__pbwgzctxx_80000gn/T/playwright-java-18063622772722717599/package/lib/coreBundle.js:19135:44)
}

Screenshots and rendered snapshots accompany every material state. Browsers close. Temporary browser state stays outside source/evidence; separately recorded same-image cleanup clears server owners. Local fault/restart cases retain their original evidence.
