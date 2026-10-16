# UC-1 recorded timetable demo walkthrough

Date: 2026-10-16T19:44:59.698383Z
Application: https://localhost:64271
Real installed Chrome through Java Playwright; isolated disposable browser owners; private copies of the packaged workspace and kernel; real TLS. No operator data or database.

- FAIL **FAIL: com.microsoft.playwright.TimeoutError: Error {
  message='Timeout 30000ms exceeded.
  name='TimeoutError
  stack='TimeoutError: Timeout 30000ms exceeded.
    at _ProgressController.run (/private/var/folders/c_/5_n98lpd65j__pbwgzctxx_80000gn/T/playwright-java-5750547520582038246/package/lib/coreBundle.js:12360:32)
    at DispatcherConnection.dispatch (/private/var/folders/c_/5_n98lpd65j__pbwgzctxx_80000gn/T/playwright-java-5750547520582038246/package/lib/coreBundle.js:19135:44)
}
Call log:
-   - waiting for locator("#school-selector option[value=full]") to be visible
-     45 × locator resolved to hidden <option selected value="full">Kasevälja Põhikool</option>
**. Expected: specified outcome with exact value/privacy/non-mutation assertions above. Observed: assertion failed as recorded above. [Screenshot](failure.png), [rendered snapshot](failure.txt).

Overall: FAIL: com.microsoft.playwright.TimeoutError: Error {
  message='Timeout 30000ms exceeded.
  name='TimeoutError
  stack='TimeoutError: Timeout 30000ms exceeded.
    at _ProgressController.run (/private/var/folders/c_/5_n98lpd65j__pbwgzctxx_80000gn/T/playwright-java-5750547520582038246/package/lib/coreBundle.js:12360:32)
    at DispatcherConnection.dispatch (/private/var/folders/c_/5_n98lpd65j__pbwgzctxx_80000gn/T/playwright-java-5750547520582038246/package/lib/coreBundle.js:19135:44)
}
Call log:
-   - waiting for locator("#school-selector option[value=full]") to be visible
-     45 × locator resolved to hidden <option selected value="full">Kasevälja Põhikool</option>


Browser contexts close and the private server stops. Generation/editing destination handoffs are verified in their owning UCs.
