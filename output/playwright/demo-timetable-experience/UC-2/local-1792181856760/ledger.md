# UC-2 recorded timetable demo walkthrough

Date: 2026-10-16T20:19:25.611899Z
Application: https://localhost:51968
Real installed Chrome through Java Playwright; isolated disposable browser owners; private copies of the packaged workspace and kernel; real TLS. No operator data or database.

- PASS **UC-2 main 1-4/extension 1a: unqualified teacher remains an explicit invalid draft, located eligibility explanation is visible, save blocked**. Expected: specified outcome with exact value/privacy/non-mutation assertions above. Observed: assertions passed. [Screenshot](01-unqualified.png), [rendered snapshot](01-unqualified.txt).

- PASS **UC-2 main 4-5/extension 4a: class collision identifies and marks both lessons with names/times and correction guidance; global conflict link reveals a filtered-out lesson**. Expected: specified outcome with exact value/privacy/non-mutation assertions above. Observed: assertions passed. [Screenshot](02-actionable-conflict.png), [rendered snapshot](02-actionable-conflict.txt).

- PASS **UC-2 extensions 3a/3b and G5: delayed actual stale review cannot replace newer valid state; injected transport failure blocks save and retry recovers; 390px editable inspector and toolbar fit without overlap**. Expected: specified outcome with exact value/privacy/non-mutation assertions above. Observed: assertions passed. [Screenshot](02b-stale-failed-mobile.png), [rendered snapshot](02b-stale-failed-mobile.txt).

- PASS **UC-2 extensions 2a/5b/6a/6c: exact complete source snapshot and draft survive navigation/reload; blank/duplicate names refused; injected browser quota failure adds no version and clears no draft**. Expected: specified outcome with exact value/privacy/non-mutation assertions above. Observed: assertions passed. [Screenshot](03-draft-name-quota.png), [rendered snapshot](03-draft-name-quota.txt).

- PASS **UC-2 main 6-7/G3-G4/G7: named save stores exact teacher-only delta and actual kernel revisions/score; original Variant A remains exact; saved alternative survives real packaged-server restart; discard restores source**. Expected: specified outcome with exact value/privacy/non-mutation assertions above. Observed: assertions passed. [Screenshot](04-saved-after-restart.png), [rendered snapshot](04-saved-after-restart.txt).

Overall: PASS

Browser contexts close and the private server stops. Generation/editing destination handoffs are verified in their owning UCs.
