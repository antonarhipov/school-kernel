# UC-5 Real bounded verification faults

Date: 2026-10-16T14:28:10.469712Z
Application: https://localhost:63896
Browser: real installed Chrome through Java Playwright. Isolated disposable owner contexts; local runtime is a fixed private JAR copy. Submission hashes are in execution/UC-5. No operator data.

- PASS **3c; G4/G6: two real stalled standalone verifier processes occupy both slots; third real UI use returns busy after configured 200ms, retaining complete accepted state**. Expected: contract outcome and exact non-mutation assertions described above. Observed: assertions passed. [Screenshot](01-verification-busy.png), [snapshot](01-verification-busy.txt).

- PASS **3d: real verifier watchdog terminates both deliberately stalled commands; neither installs a definition; reservations settle after actual termination**. Expected: contract outcome and exact non-mutation assertions described above. Observed: assertions passed. [Screenshot](02-watchdog.png), [snapshot](02-watchdog.txt).

- PASS **FAIL: com.microsoft.playwright.TimeoutError: Error {**. Expected: contract outcome and exact non-mutation assertions described above. Observed: assertions passed. [Screenshot](failure.png), [snapshot](failure.txt).

Overall: FAIL: com.microsoft.playwright.TimeoutError: Error {
  message='Timeout 30000ms exceeded.
  name='TimeoutError
  stack='TimeoutError: Timeout 30000ms exceeded.
    at _ProgressController.run (/private/var/folders/c_/5_n98lpd65j__pbwgzctxx_80000gn/T/playwright-java-2565575629921020208/package/lib/coreBundle.js:12360:32)
    at DispatcherConnection.dispatch (/private/var/folders/c_/5_n98lpd65j__pbwgzctxx_80000gn/T/playwright-java-2565575629921020208/package/lib/coreBundle.js:19135:44)
}

No solve is started by use. UC-6 owns effort selection/admission and generated proposals. Oversized transport, process stalls/exits and bounded test configuration are explicitly identified. Browser contexts close; local server stops; public workspaces require cleanup restart.
