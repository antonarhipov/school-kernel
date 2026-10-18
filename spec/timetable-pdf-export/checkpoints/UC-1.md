# UC-1 checkpoint

## Original current-view implementation (superseded by explicit user clarification)

Base: `3a10e51dbfba298d6ef93a0c784ba0145fa03e64`; submission is the working diff in this feature's files. No commit/deployment requested.

Implemented: current-view PDF download, Week/Day filters and class/teacher/room scope, immutable capture before asynchronous resources, draft labelling, readable A4 landscape pagination, Unicode fonts and explicit unsupported-character refusal, local lazy dependencies, empty/error/retry feedback and phone control layout. Entirely empty edge slots are omitted and the printed slot range is explicit.

Files: `demo-workbench.js`, `demo-workbench.css`, new `timetable-pdf.js` and `vendor/pdf/`; `TimetablePdfBrowserIT.java`; `tools/demo/timetable-pdf.test.mjs`, `tools/demo/verify_timetable_pdf.py`; this feature's documentation and `spec/README.md`.

Checks: `node --test tools/demo/*.test.mjs` 52/52 passed. `./mvnw -q -pl timetable-workspace -am package -DskipTests` passed. Sample PDFs generated with embedded Unicode fonts; a full school has 23 class pages. Rendered first class page inspected after pagination refinement. Maven verification and browser-ledger references will be appended after completion.

Source/Current fidelity: PDF generation consumes a frozen indexed model. Node tests compare complete source definition/result/filter values before/after; real browser checks compare exact localStorage and lesson identities. No database, backend routes, scheduling contract or accepted ZIP behavior changed.

Final validation (2026-10-18): `node --test tools/demo/*.test.mjs` 52/52 passed. Final Maven reactor command in status passed, including both selected Chrome browser tests. Independent PDF extraction compared every occupied cell by exact subject/teacher/room/time against original documents for 1,730 lessons in four sample schools, then checked the actual browser school download. All 65 sample pages satisfy A4 landscape, at least 8pt text and page bounds. Two draft pages and 46 stress pages preserve draft markers and bounds. Class preview, school contact sheet, draft page and desktop/phone screenshots inspected.

Walkthrough: [complete production-control journey](../../../output/playwright/timetable-pdf-export/UC-1/1792326022346/ledger.md); [concurrent school-switch capture](../../../output/playwright/timetable-pdf-export/UC-1/snapshot-1792325989758/ledger.md). Earlier failed harness attempts retained. The Java runner required explicit cold-load waits; the same installed Chrome through bundled Node Playwright produced the full school PDF in 394 ms. This is an observation from this host, not a general performance guarantee.

Outstanding destination handoffs: none. UC-1 APPROVED from passing automated evidence. No commit or deployment requested. Existing unrelated pycache modification remains excluded. Final deliverable preview: `output/pdf/timetable-export-preview.pdf`.


## Three whole-week types revision (2026-10-18)

Current contract: explicit class, teacher and full-school choices, with class/teacher pickers. Screen day and narrowing filters never affect PDF scope. Class/teacher grids show weekdays as columns; school overviews show each day's classes against period columns, with up to six periods per panel. Context is frozen when the chooser opens. Repeated row labels identify continued lessons. Draft labels, exact names and full lesson details remain.

Submission: working diff against the same base, restricted to the files above. Existing unrelated `check_packaged_samples.cpython-314.pyc` is preserved. No commit/deployment requested.

Focused technical evidence: 52/52 Node checks pass after final renderer changes. Independent extraction compared all 1,730 school lessons over 75 pages, plus class (22 lessons) and teacher (8 lessons) weekly artifacts. Each occupied and free cell matched source text exactly, shared school period headings retained correct start/end times, and all text is >=8pt within A4 landscape bounds. The real concurrent class download also matched its 22 source lessons. Stress artifacts retain all draft labels and text bounds over 6 class and 146 school pages. Full-size class/teacher/school previews, school contact sheet and stress pages inspected.

[Additional Chrome walkthrough](../../../output/playwright/timetable-pdf-export/UC-1/extra-1792326806974/ledger.md): desktop and 320px choosers, Escape cancel, empty teacher refusal with exact storage preservation, successful class retry, missing entity disabled download, held font/duplicate prevention and concurrent school switch preserving captured class export. This uses the first three-type packaged build; the later renderer-only change repeats row labels on continuation pages. Chooser/context behavior is unchanged. The final packaged regression below verifies the final revision.

Browser attempt `1792326662990` retained: successful school/class files; stale test expected nonempty screen after deliberately intersecting filters to empty. Corrected the assertion to preserve exact before/after displayed lesson identities. No product change was needed for this failure.

Final Maven/browser regression: PASS, exit 0. Failsafe 2 completed/0 failures/0 errors/0 skipped; Node 52/52. [Final browser ledger](../../../output/playwright/timetable-pdf-export/UC-1/1792327088311/ledger.md) and [final-build additional ledger](../../../output/playwright/timetable-pdf-export/UC-1/extra-1792326980870/ledger.md). Final source identity is recorded in `submission-sha256.json` beside the main ledger. The second failed harness attempt `1792326877665` is retained: existing room selection clears the teacher screen lens; tests now capture and compare every filter immediately before/after export.

| Contract | Final evidence |
| --- | --- |
| Main 1-5; success postcondition | Main browser ledger: three explicit choices, actual downloads; final PDF cell extraction and rendered previews |
| Extension 1a | Final additional ledger: no timetable disables export |
| Extension 2a | Main/additional ledgers: Cancel and Escape close chooser; exact storage retained |
| Extensions 3a/3b | Final additional ledger: empty teacher refuses without attachment/storage change, class retry succeeds, missing entities disable Download |
| Extension 4a | Main ledger: actual failed font load gives explicit error and successful retry; Node unsupported-glyph refusal |
| Extension 4b | Final additional ledger: held fonts disable repeated export before and after school switch |
| Extension 5a | Main ledger: actual room-edit draft; every PDF page independently checked for draft marker |
| G1; minimal guarantee | Complete localStorage and source document comparisons; screen filter/lesson identity snapshots; no export-triggered solve/review |
| G2 | Independent exact lesson cell comparison: all four schools and six final browser PDFs; whole-week scopes ignore screen intersections |
| G3 | A4 landscape, font >=8pt and text bounds checked across normal/stress/draft files; rendered pages/contact sheet inspected |
| G4 | Actual Chrome/CSP, only same-origin GET requests, lazy libraries, labelled native dialog, desktop/320px screenshots; existing mobile regression passes |
| G5 | Fresh-context held font request plus concurrent school switch; original captured class file independently matches source |
| Constraints/relationships | No feature-specific technical rules or UC dependencies; browser-local read-only path, no new backend routes or persistence changes |

UC-1 APPROVED for the clarified contract. No handoffs or blockers. Final sample deliverables: `output/pdf/timetable-class.pdf`, `timetable-teacher.pdf`, `timetable-school.pdf`.


## Filter-driven scope revision (2026-10-18)

User contract supersedes the chooser: selected school and schedule define the source; class wins over teacher; teacher-only selects that teacher; neither selects the full school. The existing controls are the sole scope selectors and Export PDF downloads directly. Other screen filters never narrow the complete weekly scope. Previous revision evidence above is historical.

Implementation: removed chooser and its styles; new lightweight `pdf-scope.js` resolves priority for both UI feedback and PDF content/filenames. `demo-workbench.js` freezes model/context/filter values before awaiting resources. `timetable-pdf.js` consumes captured filters. Existing layouts remain. Also updated `TimetablePdfBrowserIT.java`, `tools/demo/timetable-pdf.test.mjs`, proposal/spec/status and feature index. Source base remains `3a10e51dbfba298d6ef93a0c784ba0145fa03e64`; no commit/deployment requested. Pre-existing pycache modification preserved.

Technical evidence: 53/53 Node checks pass, covering all four class/teacher combinations, other-teacher lesson inclusion, selected Variant B source values, immutable inputs, empty/unavailable scopes, fonts and long-label pagination. Independent PDF extraction confirms 520 school lessons, class Variant A/B 22 lessons each, and final-build extra browser class/combined/captured 22 lessons and teacher 20 lessons. Variant B page rendered and inspected.

[Final-build extra walkthrough](../../../output/playwright/timetable-pdf-export/UC-1/filter-extra-1792328478469/ledger.md): actual combined class+teacher export, 320px teacher-only export, empty teacher refusal/retry with class priority, empty school refusal, no-timetable guard, duplicate prevention and changing class/schedule/school while fonts are held. Captured Variant A class file independently matches all original class lessons. Screenshots inspected. Browser errors none; only local GET requests; disposable browser/server/JARs cleaned up. Packaged SHA-256 recorded beside ledger.

Final packaged Maven/main browser regression: PASS, exit 0; 2 completed/0 failures/0 errors/0 skipped. Node 53/53 pass. [Main final ledger](../../../output/playwright/timetable-pdf-export/UC-1/1792328429692/ledger.md); exact production/test source SHA-256 fingerprints in `submission-sha256.json` beside it.

| Current contract | Evidence |
| --- | --- |
| Main 1-4, scope rows 1-4, success | Main browser downloads: school A 520, class A 22, teacher A 20, combined A 22 lessons; independent exact cell verification |
| Selected school/schedule, G2 | Combined Variant B 22, second school Variant B 56, phone class Variant B 28 lessons match exact source documents |
| Extension 1a | Additional ledger: no timetable disables export |
| Extension 2a | Additional ledger: unassigned teacher/empty school refuse with no download or storage change; selecting class retries successfully despite selected unassigned teacher |
| Extension 3a | Main ledger: aborted font resource gives retryable error then actual successful download; Node checks unavailable class and unsupported glyph refusal |
| Extension 3b, G5 | Additional ledger: held fonts disable duplicate requests; changes to class/schedule/school do not alter original captured class/Variant A PDF |
| Extension 4a | Main ledger: real unsaved room edit exports draft; all 10 pages checked for draft marker and text bounds |
| G1, minimal guarantee | Every main download compares exact storage, every selector and visible lesson identities before/after; no export-triggered mutation/solve; failure storage checked in additional ledger |
| G3/G4 | Extracted PDFs >=8pt/A4/in bounds; combined Variant B rendered and inspected; native direct button/320px screenshots, CSP, only local GETs, lazy dependencies and mobile regression pass |
| Constraints/relationships | No new routes/schema/persistence/dependencies; browser-local read-only export; existing layouts retained |

UC-1 APPROVED for the filter-driven contract. No handoffs, blocker, commit or deployment. Unrelated pycache modification remains excluded.


## Combined class collection revision (2026-10-18)

User request: full-school download combines all class weekly timetables, using the existing individual class layout. School/schedule selection and class-before-teacher priority remain. Each class begins on a fresh page in definition/class-list order; continuation pages retain its heading. No separate school overview remains. An unassigned class receives a weekly empty grid within an otherwise nonempty book; a wholly empty scope still refuses.

Submission: working diff from `3a10e51dbfba298d6ef93a0c784ba0145fa03e64`. This revision changes `timetable-pdf.js`, PDF tests/verifier, browser-ledger descriptions and feature documentation/index only. Existing unrelated pycache modification preserved. No commit or deployment requested.

Implementation: all scopes share the existing weekly plan construction. School iterates every class, filters assignments per class and trims each class's own unused edge slots; authoring breaks/lunch now follow the individual class path. Teacher and individual class behavior remain unchanged.

Technical evidence: `node --test tools/demo/*.test.mjs` 54/54 pass. `output/pdf/class-book-verification.txt` records independent checks of all 1,730 exact lessons across the four bundled school books (65 pages), all 23 full-school class sections against individually generated class PDFs by text/font/position/grid geometry, and authored class parity including an empty class. Draft/stress files satisfy A4, >=8pt, page bounds and every-page draft markers across 152/6/3 pages. Rendered first class page, all-class contact sheet, authored/empty-class pages and stress page inspected. `git diff --check` passes.

Scope, snapshot/error, storage and mobile guarantees retain prior unchanged evidence; final packaged/browser evidence for this submission will be appended below. No destination handoffs.

Final packaged verification: PASS, exit 0; Failsafe 2/0/0/0. [Passing Chrome ledger](../../../output/playwright/timetable-pdf-export/UC-1/1792329426289/ledger.md), `pdf-verification.txt` and `submission-sha256.json` beside it identify actual downloads and exact production/test/artifact hashes. The final class/teacher layouts exactly match the previous approved browser outputs; all scope rows and selected schedule/school details remain correct. The actual full-school PDF has 23 class pages and all 520 lessons; the other-school Variant B PDF has 2 class pages and all 56 lessons. Final PDF and draft pages, desktop screenshot and 320px screenshot visually inspected.

| Contract | Current submission evidence |
| --- | --- |
| Main 1-4, scope rows, G2, success | Final browser ledger plus exact cell extraction of all source lessons in actual downloads |
| G3 combined class layout/order/new-page and authoring details | Independent 23-class collection versus individual font/text/geometry comparison; four-school 1,730-lesson checks; authored class parity/empty class; rendered contact sheet and actual PDF pages |
| G1/minimal guarantee, G4 | Final browser exact storage/filter/lesson preservation, same-origin GET traffic, real 320px download, existing mobile regression, final screenshots |
| Extensions 3a/4a | Actual failed font and retry, real draft download with all-page markers and bounds; Node unsupported-glyph refusal |
| Extensions 1a/2a/3b, G5 | Earlier filter-extra ledger remains valid for unchanged controls/capture/refusal behavior; Node scope/empty/unavailable/immutable checks pass |
| Constraints/relationships | No new backend routes/persistence; no dependencies or handoffs. Unrelated pycache preserved. |

UC-1 APPROVED. No commit/deployment requested. Final sample: `output/pdf/all-class-timetables.pdf`.
