# UC-1 recorded timetable demo walkthrough

Date: 2026-10-16T19:51:02.091224Z
Application: https://localhost:64995
Real installed Chrome through Java Playwright; isolated disposable browser owners; private copies of the packaged workspace and kernel; real TLS. No operator data or database.

- PASS **UC-1 main 1-2/G1/G4: default full Estonian timetable, exact three schools and two variants, 520 real labels, Utilities closed and obsolete actions absent**. Expected: specified outcome with exact value/privacy/non-mutation assertions above. Observed: assertions passed. [Screenshot](01-default-matrix.png), [rendered snapshot](01-default-matrix.txt).

- PASS **UC-1 main 3-4/G2: same-school variants retain the exact class filter, visibly differ, perform zero network requests and open lesson details**. Expected: specified outcome with exact value/privacy/non-mutation assertions above. Observed: assertions passed. [Screenshot](02-filtered-alternative-inspector.png), [rendered snapshot](02-filtered-alternative-inspector.txt).

- PASS **UC-1 main 5/G1-G3: changing schools clears filters; 5A/5B has 56 lessons including English/PE; Star Wars has 520 lessons and fictional character teachers; all six variants open without kernel processes**. Expected: specified outcome with exact value/privacy/non-mutation assertions above. Observed: assertions passed. [Screenshot](03-star-wars.png), [rendered snapshot](03-star-wars.txt).

- PASS **UC-1 main 7/G4/G6: reload retains this browser's exact selected school/version; another owner independently opens the default; Utilities contains only the school definition editor and related generation**. Expected: specified outcome with exact value/privacy/non-mutation assertions above. Observed: assertions passed. [Screenshot](04-reload-and-utilities.png), [rendered snapshot](04-reload-and-utilities.txt).

- PASS **UC-1 extension 3a/G5: 390px selectors wrap, matrix scroll stays inside its container, and the clickable lesson inspector fits the viewport**. Expected: specified outcome with exact value/privacy/non-mutation assertions above. Observed: assertions passed. [Screenshot](05-mobile-inspector.png), [rendered snapshot](05-mobile-inspector.txt).

Overall: PASS

Browser contexts close and the private server stops. Generation/editing destination handoffs are verified in their owning UCs.
