# Use-Case Checkpoint: UC-4 - Review and decide a proposal on the combined timetable canvas

## Summary

- Status: READY_FOR_CONVERGENCE
- Base commit: `4bc2c64042fe54ed2309c408d8f96df703372566`
- Submission commit: HEAD at convergence
- Revision base: `8c6ac9f3201c961aaa30e09e0ad6036191c2a97f`; previously rejected findings C-1, G-1 and G-2 in `convergence/UC-4.md` addressed below.
- Relations verified: Requires UC-3; packaged repair completion supplies the verified proposal and exact accepted baseline/retained draft consumed by this review.

## Contract Evidence

| Contract element | Test or runtime evidence | Result |
|---|---|---|
| Main 1–2; G1, G2, G4; 1a–1c, 2a | `WorkspaceBrowserIT.measuresTargetScaleProposalReviewOpeningInRealBrowser`, `comparesOneSidedAndSameSlotChangesInRealBrowser` (`:1859–1973`); `proposal-comparison.js:1–33`, `app.js:189–205,467–527,553–612` | PASS |
| Main 3–5; 3a, 3b, 4a; G3, G5, G6 | `WorkspaceBrowserIT.generatesRepairProposalInRealBrowser` (`:1313–1387`) and `comparesOneSidedAndSameSlotChangesInRealBrowser` (`:1923–1973`); each comparison side, filter, Day/Week, focused return, selected identity, missing proposed display name, unchanged selection, Current/Draft/Proposal navigation, and exact durable equality | PASS |
| Main 6; success postcondition; G7, G8 | `WorkspaceBrowserIT.generatesRepairProposalInRealBrowser` (`:1388–1466`); explicit acceptance of packaged verified output -> exact successor accepted bundle, only Current, no draft/proposal | PASS |
| 5a, 5b, 6a; minimal guarantee | `WorkspaceBrowserIT.revisesDiscardsAndRejectsStaleProposalInRealBrowser` (`:1977–2021`); browser action -> exact accepted/draft preservation, Draft mode, no proposal. Existing `WorkspaceRepairPlanningIT` verifies service refusals | PASS |
| 6b; G7; minimal guarantee | `WorkspaceBrowserIT.generatesRepairProposalInRealBrowser` (`:1388–1428`); PostgreSQL trigger rejects acceptance -> unchanged document/version, explicit not-advanced alert, Proposal still reviewable -> retry succeeds | PASS |
| 6c; G9 | `WorkspaceBrowserIT.comparesOneSidedAndSameSlotChangesInRealBrowser` (`:1963–1973`); real narrow browser has labelled read-only agenda and no decision/matrix actions | PASS |
| Requires UC-3 | `WorkspaceBrowserIT.generatesRepairProposalInRealBrowser` (`:1313–1371`); starts from previously approved verified accepted bundle and calls packaged kernel through actual run boundary to a feasible repair proposal | PASS |

## Rule Evidence

| Rule | Evidence | Result |
|---|---|---|
| RULE-1 | `app.js:1–7,128–166,467–527`; shared model/state, native packaged renderers/assets and existing snapshot only | PASS |
| RULE-2 | `inspection-state.js:1–134` unchanged; `app.js:143–154,258–277,467–527`; browser mode navigation exact document equality, reload and filtered/focused return | PASS |
| RULE-3 | `app.js:199–226,1021–1043`; `RepairProposalService` unchanged; browser acceptance, stale refusal and failed-write retry plus existing service refusals | PASS |
| RULE-5 | `proposal-comparison.js:1–33`; `app.js:189–205,553–612,850–997`; browser indexed placement/side match and unique vs overlap assertions | PASS |
| RULE-6 | `app.js:178–205,553–612,985–1005`, `messages.js:124–140`; real-browser unavailable proposed name and proposal availability cues, existing invalid accepted import refusal regressions | PASS |
| RULE-7 | `focused-renderer.js:1–14`, `styles.css:193–210`, `messages.js:124–140`; browser Day/Week/focused, inspector collapse/keyboard confirmation and narrow read-only states | PASS |
| RULE-8 | No new routes/browser storage/dependencies; unchanged local security HTTP suite, read-only narrow browser | PASS |
| RULE-9 | `WorkspaceBrowserIT.java:1313–1466,1859–2021,2250–2310`; isolated PostgreSQL, real kernel-verified repair for main and DOM-only edge-case fixtures; no tracked runtime-data impact | PASS |
| RULE-12 | `app.js:128–166`; only authoritative `REPAIR_PROPOSAL` snapshot builds the comparison, `WorkspaceRepairPlanningIT` production run and acceptance guards unchanged | PASS |

## Validation

- Focused commands: `./mvnw -pl timetable-workspace -am '-Dit.test=WorkspaceBrowserIT#measuresTargetScaleProposalReviewOpeningInRealBrowser+comparesOneSidedAndSameSlotChangesInRealBrowser+generatesRepairProposalInRealBrowser' -Dfailsafe.failIfNoSpecifiedTests=false verify` — 26 unit + 3 browser passed, 0 failures/errors/skips.
- Full relevant suite: `./mvnw -pl timetable-workspace -am verify` — 26 unit + 77 integration (29 browser) passed, 0 failures/errors/skips, after correcting a test-local declaration scope; `git diff --check` passed.
- Revision focused command: `./mvnw -pl timetable-workspace -am '-Dit.test=WorkspaceBrowserIT#reviewsIndependentlyVerifiedNormativeRepairInRealBrowser+retainsBothSidesInFocusedResourceSchedulesInRealBrowser+revisesAndDiscardsVerifiedNormativeRepairInRealBrowser+displaysEmptyComparisonGroupsInRealBrowser' -Dfailsafe.failIfNoSpecifiedTests=false verify` — PASS, 26 unit + 4 browser, 0 failures/errors/skips.
- Revision full relevant suite: `./mvnw -pl timetable-workspace -am verify` — PASS, 26 unit + 81 integration (33 browser), 0 failures/errors/skips per `failsafe-summary.xml` and `WorkspaceBrowserIT` report; `git diff --check` passed. Earlier red iterations exposed mistaken test assumptions about combined same-slot tiles and the side-match labeling when an optional investigation remained active; both corrected and rerun green.
- Working tree impact from tests: only generated `.output.txt` (removed). Unrelated pre-existing `.idea/encodings.xml` untouched; no tracked runtime data changed.
- Runtime evidence: administrator in packaged browser stages protected Draft, generates verified kernel repair, sees two-sided Proposal with Current still prior accepted, navigates and deliberately accepts; real database write fault preserves retryable Proposal and exact prior bundle, then explicit retry accepts.
- Changed files in original implementation: `timetable-workspace/src/main/resources/static/workspace/{app.js,proposal-comparison.js,focused-renderer.js,messages.js,styles.css}`, `timetable-workspace/src/test/java/org/schoolkernel/workspace/WorkspaceBrowserIT.java`, `spec/timetable-ux-polish/{status.md,checkpoints/UC-4.md}`. Revision adds `timetable-workspace/src/test/java/org/schoolkernel/workspace/WorkspaceRepairPlanningIT.java` test-only process branch and changes `app.js`, `focused-renderer.js`, `messages.js`, `WorkspaceBrowserIT.java`, and this ledger/checkpoint.
- Approved UCs regression-tested: UC-1, UC-2, UC-3 via 33 complete browser tests (including their existing journeys) and relevant HTTP/service suites, no failures.

## Notes

- Addition, cancellation, same-slot, overlap, empty category, and unavailable proposed-name cases use a synthetic review snapshot to exercise the presentation boundary; the main decision, durable write failure, and retry use independently verified packaged-kernel output. No synthetic proposal is accepted as verified.
- Revision: period moves, room-only changes, protected unchanged, direct/ripple effect, all-zero category, 1,000 stable identities, and all decisions now additionally use a kernel-verified candidate from the real Draft and production handoff. The test-only launcher supplies a deterministic feasible result; it never bypasses the production verifier. Zero-group presentation and one-sided addition/cancellation remain supplemental UI-only cases because the current repair intent cannot introduce/remove a definition lesson.
- The administrator walkthrough remains for convergence. Unrelated `.idea/encodings.xml` is excluded from the submission.

## Revision Evidence

| Prior finding / contract | Independent test or production evidence | Result |
|---|---|---|
| C-1; 1a, 3a, G4 | `focused-renderer.js:1–14`, `app.js:694–707`, `WorkspaceBrowserIT.retainsBothSidesInFocusedResourceSchedulesInRealBrowser` (`:1977–2020`): both accepted/proposed representations in focused teacher/room on either old or new resource; related side labelled explicitly | PASS |
| G-1; main 1–6, 2a, G1, G2, G4, G8, RULE-9/12 | `WorkspaceBrowserIT.reviewsIndependentlyVerifiedNormativeRepairInRealBrowser` (`:2023–2205`) stages real Draft+pin on verified target-scale accepted pair, production verifier checks successor and result, and production planner accepts it; exact 1,000 assignment IDs, 1,001 rendered tiles, change values, groups, verified decision; `revisesAndDiscardsVerifiedNormativeRepairInRealBrowser` (`:2208–2255`) checks exact unadvanced baseline/Draft on both exits | PASS |
| G-2; main 2–5, 3a, 4a, G1, G2, G5, RULE-6 | `WorkspaceBrowserIT.java:1977–2205`: value-by-value six category arrays, four complete grouping maps with contexts/IDs, two complete before/after inspector records (six dimensions and stable IDs), exact 1,001-tile expected roster, proposed vs accepted teacher availability, one-sided class/teacher/subject/search filter labels, retained selected lesson/Day on accepted successor | PASS |

READY FOR CONVERGENCE: UC-4