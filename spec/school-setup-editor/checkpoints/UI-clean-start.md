# Clean bundled timetable startup and example retirement

Date: 2026-10-18. Base: `be5b76d0ffc8a9851c4e8308540b1de12c5b4425` plus the pre-existing uncommitted school setup/catalog-10 work. User correction supersedes automatic startup selection/draft restoration. No deployment or commit performed.

## Result

- Every arrival/reload opens bundled Kasevälja Variant A. Stored local-school selections, generated candidates and manual changes cannot replace that baseline. Draft save/review bars stay closed. Retained manual changes have an explicit Resume action; local schools/candidates remain available through explicit navigation. Setup stays closed until the vertical left entry is clicked.
- Removed automatic catalog promotion of individual-key legacy definitions. Raw browser data remains untouched; no operator storage or database was cleared.
- All six public `examples/*.json` files and both source-example conversion scripts are removed. Minimal CLI fixtures live under test resources. Older room-policy, quality and shared-teacher regression inputs now have fictional school/staff identities; their behavioral rules remain covered. The standalone viewer uses the authoritative bundled full-school result. Current README/maintenance instructions no longer recommend MVK/MV5.
- The four definitions and eight results under `timetable-workspace/src/main/resources/demo/timetables/` remain authoritative. Browser checks compare every served definition/result by value. The packaged scanner checks resource bytes against these files and retains historical identity exclusion using SHA-256 hashes rather than retaining source identities.
- Negative CLI fixtures now use catalog 11 as unsupported because the pre-existing feature implements catalog 10. No runtime kernel change belongs to this correction.

## Runtime finding and limits

Read-only inspection found the open Chrome timetable tabs on `https://demo-smoke-production.up.railway.app/workspace/`, showing the older Utilities editor and no School setup entry. The local implementation has not been deployed. MVK was not observed in those public tabs, so this does not claim to reproduce that exact reported screen. Source inspection did identify automatic startup draft restoration and legacy definition promotion; the new packaged browser journey reproduces those retained-state inputs and proves a clean baseline.

## Automated verification: PASS

- 41 JavaScript checks.
- 40 focused kernel unit/contract/fictional-fixture checks and 39 packaged CLI checks.
- Eight applicable Chrome journeys: landing; three timetable navigation/edit/generation journeys; setup recovery, grid and catalog journeys; prototype layout/rollout. See the [execution ledger](../../../output/school-setup-editor/2026-10-18-clean-start/ledger.md) for exact runs and failed attempts.
- Directly inspected retained-data landing and prototype desktop screenshots. Prototype journey covers 320/390/768/1440px, left entry, opening animation, Escape/focus and reduced motion.
- Packaged scan passes across 23,718 entries. `git diff --check` passes. Browser contexts and disposable local servers close normally.

A sandbox-only first attempt could not bind a localhost server; the rerun used authorized local test access. Browser failures exposed old implicit-school restoration expectations, which now explicitly navigate to retained work. A wider CLI run exposed two obsolete unsupported-catalog test inputs; corrected checks pass. Failed logs/screenshots are retained. This is local correction evidence, not a new approval of all setup UCs or a deployment qualification.
