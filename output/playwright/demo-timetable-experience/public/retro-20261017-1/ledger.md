# Public timetable demo walkthrough

2026-10-16T21:08:42.857485Z; actual public HTTPS https://demo-smoke-production.up.railway.app; installed Chrome / Java Playwright. Isolated disposable owners, browser-only copies. Deployment 7af6ff33-8d8a-41ae-9022-4daf4d112d5c; exact uploaded production manifest in the accompanying execution ledger. No provider/configuration mutation in this test.

- PASS **Restored compact retro full-school matrix: original shared subject colours, single-line subject/room tiles, monospace time labels and paper background**. Expected: specified outcome with exact value/privacy/non-mutation assertions above. Observed: assertions passed. [Screenshot](00-default-retro-matrix.png), [rendered snapshot](00-default-retro-matrix.txt).

- PASS **All six public variants open; full Estonian matrix is default; same-school switching retains filters and makes zero requests; obsolete controls absent**. Expected: specified outcome with exact value/privacy/non-mutation assertions above. Observed: assertions passed. [Screenshot](01-six-variants.png), [rendered snapshot](01-six-variants.txt).

- PASS **Public kernel validation identifies unqualified teacher and both colliding lessons, blocks save and reveals the filtered-out lesson through its conflict link**. Expected: specified outcome with exact value/privacy/non-mutation assertions above. Observed: assertions passed. [Screenshot](02-conflict.png), [rendered snapshot](02-conflict.txt).

- PASS **Actual room validation refuses unsuitable equipment; corrected draft is ready; editable inspector and save toolbar fit 390px without overlap**. Expected: specified outcome with exact value/privacy/non-mutation assertions above. Observed: assertions passed. [Screenshot](03-mobile-valid.png), [rendered snapshot](03-mobile-valid.txt).

- PASS **Blank/duplicate names refused; exact teacher-only result is saved as immutable named version, survives reload, preserves original and is absent from another browser**. Expected: specified outcome with exact value/privacy/non-mutation assertions above. Observed: assertions passed. [Screenshot](04-named-save.png), [rendered snapshot](04-named-save.txt).

- PASS **Utilities contains only definition editing/generation; exact school copy produces a real Quick result on Free, validated and saved as a named 56-lesson timetable; narrow layout fits**. Expected: specified outcome with exact value/privacy/non-mutation assertions above. Observed: assertions passed. [Screenshot](05-public-generation.png), [rendered snapshot](05-public-generation.txt).

Overall: PASS

Screenshots and rendered snapshots accompany every material state. Browsers close. Temporary browser state stays outside source/evidence; separately recorded same-image cleanup clears server owners. Local fault/restart cases retain their original evidence.
