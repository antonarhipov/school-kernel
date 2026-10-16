# UC-5 Real bounded verification faults

Date: 2026-10-16T14:33:19.282355Z
Application: https://localhost:64798
Browser: real installed Chrome through Java Playwright. Isolated disposable owner contexts; local runtime is a fixed private JAR copy. Submission hashes are in execution/UC-5. No operator data.

- PASS **3c; G4/G6: two real stalled standalone verifier processes occupy both slots; third real UI use returns busy after configured 200ms, retaining complete accepted state**. Expected: contract outcome and exact non-mutation assertions described above. Observed: assertions passed. [Screenshot](01-verification-busy.png), [snapshot](01-verification-busy.txt).

- PASS **3d: real verifier watchdog terminates both deliberately stalled commands; neither installs a definition; reservations settle after actual termination**. Expected: contract outcome and exact non-mutation assertions described above. Observed: assertions passed. [Screenshot](02-watchdog.png), [snapshot](02-watchdog.txt).

- PASS **3d: injected real process exit without structured output yields safe failure; prior Current and saved text retained**. Expected: contract outcome and exact non-mutation assertions described above. Observed: assertions passed. [Screenshot](03-process-failure.png), [snapshot](03-process-failure.txt).

- PASS **4a: validation launches first, same-owner tab changes Current during actual packaged verification; completed evidence cannot overwrite the newer exact baseline**. Expected: contract outcome and exact non-mutation assertions described above. Observed: assertions passed. [Screenshot](04-stale-completion.png), [snapshot](04-stale-completion.txt).

- PASS **3c; G6: configured real daily allowance cannot cover the seven-second reservation; UI shows daily refusal with complete accepted baseline/local version retained; sample use remains available**. Expected: contract outcome and exact non-mutation assertions described above. Observed: assertions passed. [Screenshot](05-daily-exhausted.png), [snapshot](05-daily-exhausted.txt).

Overall: PASS

No solve is started by use. UC-6 owns effort selection/admission and generated proposals. Oversized transport, process stalls/exits and bounded test configuration are explicitly identified. Browser contexts close; local server stops; public workspaces require cleanup restart.
