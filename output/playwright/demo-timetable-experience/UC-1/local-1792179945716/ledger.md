# UC-1 recorded timetable demo walkthrough

Date: 2026-10-16T19:46:38.427369Z
Application: https://localhost:64541
Real installed Chrome through Java Playwright; isolated disposable browser owners; private copies of the packaged workspace and kernel; real TLS. No operator data or database.

- FAIL **FAIL: org.opentest4j.AssertionFailedError: Locator expected to have value
Expected: full
Received: Loading schools…

Call log:
  - Assert "hasValue" locator("#school-selector") with timeout 5000ms
  - waiting for locator("#school-selector")
    2 × locator resolved to <select aria-label="School" id="school-selector">…</select>
      - unexpected value "Loading schools…"**. Expected: specified outcome with exact value/privacy/non-mutation assertions above. Observed: assertion failed as recorded above. [Screenshot](failure.png), [rendered snapshot](failure.txt).

Overall: FAIL: org.opentest4j.AssertionFailedError: Locator expected to have value
Expected: full
Received: Loading schools…

Call log:
  - Assert "hasValue" locator("#school-selector") with timeout 5000ms
  - waiting for locator("#school-selector")
    2 × locator resolved to <select aria-label="School" id="school-selector">…</select>
      - unexpected value "Loading schools…"

Browser contexts close and the private server stops. Generation/editing destination handoffs are verified in their owning UCs.
