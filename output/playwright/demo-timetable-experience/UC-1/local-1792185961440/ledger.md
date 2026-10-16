# UC-1 recorded timetable demo walkthrough

Date: 2026-10-16T21:27:26.344497Z
Application: https://localhost:55174
Real installed Chrome through Java Playwright; isolated disposable browser owners; private copies of the packaged workspace and kernel; real TLS. No operator data or database.

- PASS **UC-1 main 1-2/G1/G4: default full Estonian timetable, exact four schools and two variants each, 520 real labels, Utilities closed and obsolete actions absent**. Expected: specified outcome with exact value/privacy/non-mutation assertions above. Observed: assertions passed. [Screenshot](01-default-matrix.png), [rendered snapshot](01-default-matrix.txt).

- PASS **UC-1 main 3-4/G2: same-school variants retain the exact class filter, visibly differ, perform zero network requests and open lesson details**. Expected: specified outcome with exact value/privacy/non-mutation assertions above. Observed: assertions passed. [Screenshot](02-filtered-alternative-inspector.png), [rendered snapshot](02-filtered-alternative-inspector.txt).

- PASS **UC-1 main 5/G1-G3: changing schools clears filters; 5A/5B has 56 lessons including English/PE; Star Wars has 520 lessons and fictional character teachers**. Expected: specified outcome with exact value/privacy/non-mutation assertions above. Observed: assertions passed. [Screenshot](03-star-wars.png), [rendered snapshot](03-star-wars.txt).

- PASS **UC-1 G1/G5 English addition: Meadowbrook's independent Years 1–9 matrix renders 614 lessons, 22 classes, 41 teachers and 35 rooms with real English curriculum labels and compact retro styling**. Expected: specified outcome with exact value/privacy/non-mutation assertions above. Observed: assertions passed. [Screenshot](03a-english-matrix.png), [rendered snapshot](03a-english-matrix.txt).

- PASS **UC-1 G1-G3 English addition: its second variant differs, retains the exact class filter, opens English lesson details and makes zero switching requests; all eight variants start no visitor kernel process**. Expected: specified outcome with exact value/privacy/non-mutation assertions above. Observed: assertions passed. [Screenshot](03b-english-inspector.png), [rendered snapshot](03b-english-inspector.txt).

- PASS **UC-1 G5 English addition: the complete Meadowbrook matrix remains internally scrollable at 390px without page overflow**. Expected: specified outcome with exact value/privacy/non-mutation assertions above. Observed: assertions passed. [Screenshot](03c-english-mobile.png), [rendered snapshot](03c-english-mobile.txt).

- PASS **UC-1 main 7/G4/G6: reload retains this browser's exact selected school/version; another owner independently opens the default; Utilities contains only the school definition editor and related generation**. Expected: specified outcome with exact value/privacy/non-mutation assertions above. Observed: assertions passed. [Screenshot](04-reload-and-utilities.png), [rendered snapshot](04-reload-and-utilities.txt).

- PASS **UC-1 extension 3a/G5: 390px selectors wrap, matrix scroll stays inside its container, and the clickable lesson inspector fits the viewport**. Expected: specified outcome with exact value/privacy/non-mutation assertions above. Observed: assertions passed. [Screenshot](05-mobile-inspector.png), [rendered snapshot](05-mobile-inspector.txt).

- PASS **UC-1 extension 1a/G3: unreadable draft is explained and retained byte-for-byte while readable bundled schools remain inspectable**. Expected: specified outcome with exact value/privacy/non-mutation assertions above. Observed: assertions passed. [Screenshot](06-unreadable-draft.png), [rendered snapshot](06-unreadable-draft.txt).

- PASS **UC-1 extensions 1a/1b: corrupt browser root is preserved, samples remain navigable, and failed writes explain that changes are only in memory**. Expected: specified outcome with exact value/privacy/non-mutation assertions above. Observed: assertions passed. [Screenshot](07-unavailable-storage.png), [rendered snapshot](07-unavailable-storage.txt).

Overall: PASS

Browser contexts close and the private server stops. Generation/editing destination handoffs are verified in their owning UCs.
