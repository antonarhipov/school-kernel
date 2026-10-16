# Compact retro timetable restored and deployed

2026-10-17; branch `demo`, base `d55ab636558224f2d33a5e71313e840dd9486428`. User rejected the newly introduced
matrix styling and explicitly requested restoration. Existing unrelated dirty work is preserved. No commit requested.

The [public demo](https://demo-smoke-production.up.railway.app/workspace/) now runs deployment
`7af6ff33-8d8a-41ae-9022-4daf4d112d5c` (SUCCESS), image
`sha256:a154c471df74d8ed1521cc5201c3e3d3dc1152689cb3d5e6019aa1b434537bb6`.
[Verification summary](verification-summary.json): PASS.

## Restoration

The previous demo-specific overrides introduced 62px stacked lesson cards, large padding, an eight-colour pastel
palette and rounded white panels. Removed those overrides and restored the shared existing `styles.css`
`current-mode`, `compact-density`, `week-lesson` and `subject-color-*` rules. The main stylesheet itself is unchanged.
The demo again has a paper background, small-radius borders, slim full-width framing, monospace time/room labels,
24-colour bordered tiles and a fixed compact density. Week class tiles show subject/room on one line; Day and
inspector retain teacher details. Teacher/room lenses retain their original context fields. Room text stays visible;
the introduced density switch that hid room names is removed. Long labels have full title/accessible text.

Compared visually with the [previous compact full-school view](../../../playwright/demo-deployment/UC-3/uc4-regression-20261016-1/04-open-full.png).
The [published restored full matrix](../../../playwright/demo-timetable-experience/public/retro-20261017-1/00-default-retro-matrix.png)
was opened and inspected. Actual [computed layout](../../../playwright/demo-timetable-experience/public/retro-20261017-1/matrix-layout.json):
**20px lesson tiles, 27px rows**, visible room labels, monospace times, `rgb(240,236,225)` paper background, no density
switch. More than two full ten-period classes fit in the desktop matrix instead of a fraction of one class.

Only [two production files](production-delta.json) differ from the prior deployment: `demo-workbench.css` and the
presentation portions of `demo-workbench.js`. Every other production file, kernel/backend, sample pair, storage and
validation implementation is exact. User navigation, named versions and authoritative conflict checks are retained.
The feature specification now explicitly records the user's existing visual requirement. The existing local
walkthrough's Yoda check uses the complete accessible lesson label, since original compact Week class tiles show
subject and room while teacher detail remains available in the inspector/Day view.

## Verification

- Two existing real-Chrome packaged journeys pass: [UC-1](../../../playwright/demo-timetable-experience/UC-1/local-1792184548371/ledger.md)
  and [UC-2](../../../playwright/demo-timetable-experience/UC-2/local-1792184625963/ledger.md), [XML](local-browser-tests.xml).
  All six matrices, instant/filter-aware switching, retained data, actionable conflicts, failed/stale checks,
  exact named save, quota refusal, actual local restart and narrow inspector/save-toolbar layout remain good.
- The complete [public Chrome journey](../../../playwright/demo-timetable-experience/public/retro-20261017-1/ledger.md)
  passes on the corrected deployment, [XML](public-browser-tests.xml). Inspected actual published desktop and mobile
  screenshots. Teacher, room and period errors identify lessons and block save; exact named result/reload and
  unchanged original pass. Actual Quick generation and exact browser school/timetable save also pass.
- A separately recorded [public same-image restart proof](../../../playwright/demo-timetable-experience/public/retro-restart-20261017-1/ledger.md)
  passes with the exact saved timetable and school copy, [XML](restart-browser-tests.xml). No browser runtime errors.
- [Before](public-before-cleanup/cleanup-public-checks.json) and [after](public-after-cleanup/cleanup-public-checks.json)
  cleanup: 40 consecutive stateless reads, nine public edge/Origin/CSRF/redirect probes, all 40 static hashes, three
  exact definitions/six exact bundled results and kernel fingerprint pass. No cookies created by these checks.
- `git diff --check` passes. [Test source hashes](verification-source-sha256.json) identify the executed harness.
  No broad backend suite repeated for this presentation correction; earlier unchanged kernel/unit evidence retains scope.

Local command:

```sh
./mvnw -q -pl timetable-workspace -am verify -Dtest=__none__ \
  '-Dit.test=DemoTimetableBrowserIT#exploresVerifiedVariantsWithInstantSwitchingFiltersAndResponsiveInspector+editsConflictsPersistsDraftAndAtomicallySavesNamedAlternative' \
  -Dsurefire.failIfNoSpecifiedTests=false -Dfailsafe.failIfNoSpecifiedTests=false \
  -DargLine=-javaagent:/Users/anton/.m2/repository/org/mockito/mockito-core/5.23.0/mockito-core-5.23.0.jar
```

Public main uses existing opt-in `DemoTimetableRemoteIT`, `demo.timetable.remote=true`, `demo.timetable.phase=main`,
the public base URL, current deployment ID and its unique walkthrough directory. Restart proof uses phase
`after-restart`. Full command output is retained in [local](local-browser.log), [public](public-browser.log) and
[restart](restart-browser.log) logs. All cases passed on their first correction attempt.

## Deployment and cleanup

The [171-file production-only upload manifest](upload-manifest.json) matches source/stage/archive exactly.
[Archive](production-source.tar.gz) SHA-256 `232f2e32598541baf1244a1c088bba2b9584aab87d6697756dab577b2b0345a7`.
[Preflight](payload-preflight.json), [upload](deployment-upload.log), [successful build](build.jsonl).
No tests, operator data, real examples, credentials, Git metadata or previous artifacts were uploaded.

Same existing `school-kernel-free-smoke / demo-smoke / production` service. [Before](plan-before.json) and
[after](plan-after.json) both report FREE. Complete provider service manifests match before/after, preserving one
CPU, 500,000,000 bytes, one EU replica and sleeping. No resource/plan/billing/variable mutation issued.
[Initial](boundary-before.json) and [final](boundary-final.json) isolated-ingress checks pass.

[Final same-image cleanup](cleanup-final.json) clears disposable server workspaces, re-verifies packaged pairs,
keeps artifact/kernel identity and changes the counter epoch. [Final events](events-final.jsonl) show **zero active
workspaces**; exact browser-saved timetable survived. Temporary browser state is removed, browsers/private local
servers stopped. No unrelated-service action or commit. This is the requested styling restoration, not a new
capacity or paid-plan launch qualification.
