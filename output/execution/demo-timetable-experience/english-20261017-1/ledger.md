# English bundled school added and deployed

2026-10-17 (Europe/Tallinn); branch `demo`, base `d55ab636558224f2d33a5e71313e840dd9486428`.
User requested a fourth school in English at Kasevälja's scale, with different counts and its own schedule.
Existing unrelated dirty work is preserved. No commit requested.

[Public demo](https://demo-smoke-production.up.railway.app/workspace/): deployment `dc3b9ac9-909b-4d18-a60d-b87a5f4cf7e0` (SUCCESS),
image `sha256:67c8d7a05bb70d7a6b556d41c7492ea7d1d3a84ad3657a67dddd6c02306ec595`. [Verification summary](verification-summary.json): PASS.

## School and preserved behavior

Meadowbrook Community School is independently constructed, without reading a source example or existing placements.
22 classes across Years 1–9 (two forms in Years 1–5, three in Years 6–9), 614 weekly lessons, 41 fictional
English-speaking teachers, 35 rooms and 30 periods. Kasevälja has 23 classes, 520 lessons, 51 teachers, 42 rooms
and 50 periods. [Exact comparison, workloads, scores and preservation hashes](sample-comparison.json).

Real subjects include English, Mathematics, Science, History, Geography, French/Spanish, PE, Art and Design,
Music, Computing, Design and Technology and PSHE. Each course keeps one teacher. Younger classes have 25 weekly
lessons, Year 5 has 27, and Years 6–9 have 30. Science labs, art/music rooms, computer suites, design workshops
and sports halls have equipment/capacity constraints. Daily subject limits apply to both variants.

Variant A uses the independent constructive seed; Variant B uses complete-kernel-evaluated feasible swaps.
32 lessons change weekdays while requirements, teachers, rooms, availability and catalogs stay exact.
Both are complete FEASIBLE results, independently re-evaluated before evidence is recorded. Startup physically
verifies all eight pairs. [Live startup](startup-ready.json), [final JAR/privacy scan](packaged-provenance-final.json).

All nine original school definition/result files and all 40 workspace static files are byte-for-byte unchanged
from the retro deployment. No styles or browser persistence/navigation implementation changed. The catalog and
safe origin/event allowlists now include `english`. [Seven-file production delta](production-delta.json).
Maintenance tools support reproducible isolated rebuilding of only Meadowbrook; seeds/tools stay outside production.

## Verification

- Seven final DemoSamplesTest checks and four DemoEventsTest checks pass, including four-school publication,
  immutable preview/use, distinct dimensions, course-teacher consistency and different complete alternatives.
  [Sample XML](sample-final-tests.xml), [events XML](events-tests.xml).
- All three existing local Chrome actor journeys passed before daily limits were tightened in the new example.
  Their unchanged school/edit/save/generation resources remain exact. [Broad XML](local-all-journeys-before-daily-limits.xml).
  The complete affected UC-1 journey was rerun on the final packaged data and passed:
  [final local ledger](../../../playwright/demo-timetable-experience/UC-1/local-1792185961440/ledger.md),
  [XML](local-final-browser-tests.xml). English desktop, inspector and 390px screenshots were opened and inspected.
  All eight matrices open, school changes clear filters, same-school versions retain them with zero requests,
  and no visitor verification process starts. Corrupt browser data remains recoverable; no browser runtime errors.
- Five [final refusal checks](readiness-final/checks.json) pass: stale build, stale score, invalid original pair,
  invalid new English pair even with recomputed hashes, and maintenance refusing a stale claimed score.
- Complete [public Chrome journey](../../../playwright/demo-timetable-experience/public/english-20261017-1/ledger.md)
  passes, [XML](public-browser-tests.xml). All eight variants match exact local data. A real 614-lesson English
  collision is validated on Free with the existing 96 MiB verifier heap, explains named affected lessons and blocks
  saving. Discard restores the matrix. Existing teacher/room/time conflicts, named immutable save/reload,
  original preservation, independent browser, Quick generation and 390px controls also pass.
- Actual same-image restart preserves exact saved timetable and school copy:
  [passing ledger](../../../playwright/demo-timetable-experience/public/english-restart-20261017-2/ledger.md),
  [XML](restart-browser-tests.xml). The first restart test used an incorrect relative evidence-file path;
  its [failed ledger](../../../playwright/demo-timetable-experience/public/english-restart-20261017-1/ledger.md)
  and log/XML are retained. Rerun used the correct absolute path; no product changes or weakened assertions.
- [Before](public-before-cleanup/cleanup-public-checks.json) and
  [after](public-after-cleanup/cleanup-public-checks.json) cleanup: 40 consecutive stateless reads, nine public
  edge/Origin/CSRF probes, all 40 static hashes and eight exact definition/result pairs pass. No cookies created.
- `git diff --check` passes. [Verification source hashes](verification-source-sha256.json) identify maintenance/tests.
  No broad unrelated backend suite repeated; unchanged evidence retains its original scope.

## Deployment and cleanup

[174-file production-only manifest](upload-manifest.json), [preflight](payload-preflight.json),
[archive](production-source.tar.gz) SHA-256 `1e1b9b74139909c0d7ace934062d45aebcba760bf4d569ca7988137ff2b65b0c`.
Source, stage, archive and final packaged sample resources agree. No examples, tests, seeds, operator data,
credentials, Git metadata or old artifacts entered the upload. [Upload](deployment-upload.log),
[successful build](build-final.jsonl), [ready provider metadata](deployment-ready.json).

Same service/project/environment, [before](plan-before.json) and [after](plan-after.json) plan FREE.
[Complete service/file manifests match](provider-comparison.json): one CPU, 500 MB, one EU replica, sleeping,
existing heaps and two-workspace cap. No variable/resource/billing/plan mutation was issued.
[Before](boundary-before.json) and [final](boundary-final.json) isolated ingress checks pass.

[Cleanup](cleanup-final.json) proves identical service artifact/kernel, a new counter epoch and all pairs verified
again. [Final aggregate](events-final.jsonl) records zero active workspaces, zero running solve/verification
processes and no retained cache entries. Browser contexts/private services stopped; temporary browser cookie state
removed. Local staged production payload is retained under the recorded private-tmp path for review.
UC-1's catalog extension is APPROVED; existing UC-2/UC-3 behavior remains approved. Capacity/billing qualification
retains its separate scope.
