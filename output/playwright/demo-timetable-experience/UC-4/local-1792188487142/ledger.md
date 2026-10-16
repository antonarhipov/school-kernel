# UC-4 local-artifact cleanup walkthrough

2026-10-16T22:10:00.429972Z; real installed Chrome / Java Playwright; https://localhost:57926; isolated disposable browser owners. Deployment private packaged source pinned in execution ledger.

- PASS **UC-4 main 1/G1: Utilities explains the browser/device-only scope and bundled-example preservation; real saved school, timetable, draft, editor work and legacy artifacts exist**. Expected: specified outcome with exact value/privacy/non-mutation assertions above. Observed: assertions passed. [Screenshot](01-utilities-scope.png), [rendered snapshot](01-utilities-scope.txt).

- PASS **UC-4 extension 2a/G2: Cancel and Escape preserve every raw stored value and the exact current draft/editor work**. Expected: specified outcome with exact value/privacy/non-mutation assertions above. Observed: assertions passed. [Screenshot](02-cancel-preserves-data.png), [rendered snapshot](02-cancel-preserves-data.txt).

- PASS **UC-4 main 2/G2: keyboard-accessible confirmation repeats local-only scope and fits 390px with Cancel initially focused**. Expected: specified outcome with exact value/privacy/non-mutation assertions above. Observed: assertions passed. [Screenshot](03-mobile-confirmation.png), [rendered snapshot](03-mobile-confirmation.txt).

- PASS **UC-4 extension 3a/G4: injected browser removal refusal after the first removal restores all exact raw values, reports failure and permits retry**. Expected: specified outcome with exact value/privacy/non-mutation assertions above. Observed: assertions passed. [Screenshot](04-refused-removal.png), [rendered snapshot](04-refused-removal.txt).

- PASS **UC-4 main 3-4/G1/G3: confirmed cleanup removes saved and legacy artifacts, drafts and editor work; clean default reloads; cookies, exact server state, unrelated preferences and another browser remain unchanged; stale tab cannot resurrect data; no server mutation request**. Expected: specified outcome with exact value/privacy/non-mutation assertions above. Observed: assertions passed. [Screenshot](05-clean-default.png), [rendered snapshot](05-clean-default.txt).

- PASS **UC-4 extension 3b/main 4: explicit cleanup recovers an unreadable root and malformed legacy definition; fresh school work saves successfully afterward**. Expected: specified outcome with exact value/privacy/non-mutation assertions above. Observed: assertions passed. [Screenshot](06-new-work-after-recovery.png), [rendered snapshot](06-new-work-after-recovery.txt).

Overall: PASS

Browser contexts close; local server stops; no operator storage touched. Public server cleanup is recorded separately.
