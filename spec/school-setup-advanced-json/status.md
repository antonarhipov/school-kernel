# Execution status

Base: 7c9a0c3b5429471cf213f2420546cda102e041bf

Pre-existing change preserved: tools/demo/__pycache__/check_packaged_samples.cpython-314.pyc.

| UC | Status | Dependencies |
| --- | --- | --- |
| UC-1 | APPROVED | None |
| UC-2 | APPROVED | UC-1 |
| UC-3 | APPROVED | UC-1 |

2026-10-18: User authorized proposal, specification and implementation after confirming design. Existing setup and public-demo access/cleanup guards remain applicable.

2026-10-18 UC-1 APPROVED: [checkpoint](checkpoints/UC-1.md), [Chrome walkthrough](../../output/playwright/school-setup-advanced-json/UC-1/1792280769636/ledger.md). Shared shell groundwork is not UC-3 approval. UC-2 now implements complete and legacy exchange.

2026-10-18 UC-2 APPROVED: [checkpoint](checkpoints/UC-2.md), [passing complete exchange](../../output/playwright/school-setup-advanced-json/UC-2/1792281428212/ledger.md). UC-3 now verifies relocated generation/cleanup and affected prior browser journeys.

2026-10-18 completed-slice regression: initial broad Chrome run completed 28 journeys with six failures. Retained `/tmp/advanced-json-regression.log` and per-UC failure artifacts. Findings: preparation cancellation was ignored before a run ID existed (fixed with remembered cancellation and safe preparation boundaries); autosave erased useful class-lunch action feedback (fixed by separating persistence errors from action messages). Other failures were old startup expectations and five-second checks under concurrent browser load; startup assertions now require bundled landing and zero startup kernel launches, with explicit local-school selection after reload. Final packaged regression repeats all 28 affected journeys. Additional checks cover exact error navigation, imported/recovered-draft resume, previous editor text download, and cancellation while a preparation response is held open.

2026-10-18 final regression finding: the old block-window fixture declared seven allocated lessons but provided zero individual occurrences. The new shared validator correctly reports that mismatch. Corrected the prerequisite fixture to contain seven identified occurrences with the declared teacher/series; all original block consumption, frequency, adjacency, protection and nonmutation assertions remain. The complete block journey is repeated against the final archive, together with recovery-message verification.


2026-10-18 feature complete: UC-1, UC-2 and UC-3 APPROVED. No outstanding destination handoffs or implementation blockers. Final build and `git diff --check` pass. All 49 JavaScript tests and 90 Maven unit tests pass. The broad affected Chrome run passed 27/28; its sole remaining invalid-prerequisite fixture was corrected and the full block journey repeated successfully. Both final targeted journeys pass (2/2), covering block rules and complete quota/stale-tab recovery, including the final recovery-message refinement. This provides passing evidence for all 28 distinct affected journeys. Unchanged browser evidence from the broad packaged run remains valid; the final production delta only clears an obsolete storage-error notice after explicit recovery and is covered by the repeated recovery journey.

- [UC-1 final shared-document journey](../../output/playwright/school-setup-advanced-json/UC-1/1792283074880/ledger.md)
- [UC-2 final exchange journey](../../output/playwright/school-setup-advanced-json/UC-2/1792282899914/ledger.md)
- [UC-3 final generation/cleanup journey](../../output/playwright/school-setup-advanced-json/UC-3/1792282986749/ledger.md)
- [Final complete block-rule repeat](../../output/playwright/school-setup-editor/UC-11/1792283661345/ledger.md)
- [Final complete storage/recovery repeat](../../output/playwright/school-setup-editor/UC-1/1792283661345/ledger.md)
- [Final submission manifest](../../output/school-setup-advanced-json/20261018-final/manifest.json), [exact tracked diff](../../output/school-setup-advanced-json/20261018-final/intended.patch), complete tracked/untracked source snapshot in `submission/`, and retained build/test logs in the same evidence directory. The unrelated pre-existing bytecode file is excluded. Broad-run archive hash is retained separately from the final archive hash.

Validation commands: `node --test tools/demo/*.test.mjs`; `./mvnw -pl timetable-workspace -am test`; `./mvnw -pl timetable-workspace -am package -DskipTests -DskipITs`; affected browser run with `failsafe:integration-test failsafe:verify`, selectors `SchoolSetupAdvancedJsonBrowserIT,SchoolSetupBrowserIT,SchoolSetupCatalogBrowserIT,SchoolSetupLandingBrowserIT,DemoTimetableBrowserIT,DemoLocalDataBrowserIT,DemoLocalHttpBrowserIT,MobileTimetableBrowserIT`, `-Dfailsafe.failIfNoSpecifiedTests=false` and JUnit fixed parallelism 3. Final repeat uses selectors `SchoolSetupCatalogBrowserIT#declaresExactBlockLengthsAndFrequenciesWithNecessaryWindows,SchoolSetupBrowserIT#startsResumesRetainsSourcesAndRecoversFailures` and parallelism 2. Retained logs show final Maven results; failed attempts are not overwritten.

No deployment or commit performed. Bundled resources and backend contracts are unchanged. Pre-existing unrelated working-tree change remains preserved.


2026-10-18 requested desktop-only refinement: School setup is hidden at the existing mobile breakpoint (700 CSS pixels and below). Opening is guarded, resizing closes Setup and its confirmations with a draft flush, and the timetable reclaims the old entry gutter. Desktop entry returns without automatically reopening Setup. Responsive setup checks now use 768/1024/1440px; focused Chrome checks cover mobile hiding, modal dismissal, pending-JSON retention, desktop reopening and the mobile timetable journey. Verified: package and diff checks pass; 49/49 Node tests pass. The mobile timetable Chrome journey passes; the complete UC-1 desktop/mobile transition journey passes in [final ledger](../../output/playwright/school-setup-advanced-json/UC-1/1792284303114/ledger.md), including modal dismissal, guarded opening, exact pending text and checked desktop save. Inspected 320px timetable and 768px setup screenshots. Initial transition attempts exposed a Playwright full-page screenshot temporarily setting viewport width to 1px; modal screenshots now capture the viewport without introducing that artificial layout change. Retained failed attempts and logs remain available. [Updated source manifest and logs](../../output/school-setup-advanced-json/20261018-desktop-only/manifest.json).
