# Coruscant Imperial Academy curriculum revision

2026-10-17 (Europe/Tallinn); branch `demo`, base `d55ab636558224f2d33a5e71313e840dd9486428`.
User requested the Imperial name and a balanced progression from basic education through practical preparation to
professional operational training. Existing unrelated dirty work is preserved. No commit requested.

## Definition

Independent constructive definition, not a renamed copy of another school: 18 cohorts (two forms per level 1–9),
540 weekly lessons, 29 specialist instructors, 37 rooms, 27 subjects and 35 periods. The school identifier remains
`demo-star-wars-school`; browser-saved historical versions are not migrated or overwritten.

- Junior levels 1–3: 25 lessons/week, five/day; literacy, numeracy, history, science, droid literacy, fitness,
  citizenship, teamwork and first aid. No advanced combat courses.
- Intermediate levels 4–6: 30 lessons/week, six/day; physics, navigation, systems, simulation, tactical foundations,
  fieldcraft, emergency medicine, communication and command ethics/law.
- Senior levels 7–9: 35 lessons/week, seven/day; flight/fleet/combined operations, mission planning, intelligence,
  engineering, leadership, combat medicine, briefings and fitness.

Every course retains its teacher. Qualified faculty groups and real room capabilities/capacities constrain both
schedules. Teaching loads range from 13 to 25 weekly lessons. Classes start at the first period, have no gaps,
have equal daily loads, and study each subject at most once/day. Both complete variants pass the actual kernel
evaluator; 32 lessons move across weekdays. [Exact curriculum/preservation](curriculum-and-preservation.json).

## Verification before deployment

- Eight [sample tests](sample-tests.xml) and all three [packaged Chrome journeys](local-browser-tests.xml) pass.
  [UC-1](../../../playwright/demo-timetable-experience/UC-1/local-1792187246218/ledger.md),
  [UC-2](../../../playwright/demo-timetable-experience/UC-2/local-1792187335264/ledger.md),
  [UC-3](../../../playwright/demo-timetable-experience/UC-3/local-1792187440509/ledger.md).
  Junior/intermediate/senior screenshots were opened and inspected: compact retro subject/room tiles,
  appropriate course labels, five/six/seven filled rows and no class gaps. Existing editing, saved-version
  restart, school copying and actual Quick generation paths pass.
- Six [startup/maintenance refusal checks](readiness/checks.json) pass, including a corrupted Imperial pair
  with recomputed hashes. [Final packaged provenance/privacy scan](packaged-provenance.json) passes.
- All other school definition/result files and all 40 static UI/style files remain byte-for-byte unchanged.
  [Five-file production delta](production-delta.json), [174-file manifest](upload-manifest.json),
  [payload preflight](payload-preflight.json), [verification source hashes](verification-source-sha256.json).
  Only production files enter the upload; maintenance builders/seeds, tests, operator examples, credentials and
  Git metadata are excluded. Source, stage and archive hashes agree.

## Deployment

The same existing Railway Free service is live as deployment `fe66a8a4-9004-4d01-b229-2e7216d83c5f` (SUCCESS),
image `sha256:8698504f77f230139b0ff6a45cfaebb9b30f91c3ce5018d4b9f67460f00a8772`, at
[the public demo](https://demo-smoke-production.up.railway.app/workspace/).
[Upload](deployment-upload.log), [successful build](build-final.jsonl), [ready metadata](deployment-ready.json).
The [source archive](production-source.tar.gz) SHA-256 is
`9b40c4755036e441f482e0a266efd556f733c93b7f128a07f7e018765f8f9f2e`.
[Provider comparison](provider-comparison.json) proves unchanged complete service/file manifests, Free plan,
runtime effective limits and process bounds: one CPU, 500 MB, one EU replica, sleeping, existing heaps and
two-workspace cap. No plan/resource/variable/billing mutation was issued. [Plan before](plan-before.json),
[after](plan-after.json), [ingress preflight](boundary-before.json), [final](boundary-final.json).

The initial public walkthrough found a test assumption: it required the first cohort of every school to move
between variants. The independent Imperial seed's first cohort is unchanged, although 32 lessons in six other
cohorts move. No requirement promises that every cohort changes. The harness now derives a genuinely changed
cohort from the complete actual variants, then still requires a visibly different matrix, exact retained filter
and zero switching requests. [Failed attempt](../../../playwright/demo-timetable-experience/public/imperial-20261017-1/ledger.md),
[XML](public-initial-failed-tests.xml), [log](public-browser.log) are retained. No production edit or redeployment
was needed for this correction. The complete public journey passed in a new run directory.

## Public verification and cleanup

- The complete [public Chrome journey](../../../playwright/demo-timetable-experience/public/imperial-20261017-2/ledger.md)
  passes: all eight variants, retained same-school filters/zero switching requests, junior/intermediate/senior
  Imperial curricula, compact 390px layout, authoritative full 540-lesson conflict validation, blocked invalid save,
  discard, teacher/room/time correction, exact immutable named save/reload, owner isolation and real Quick generation.
  [XML](public-browser-tests.xml), [log](public-browser-final.log). Public stage/mobile/conflict screenshots were
  opened and inspected; correction text identifies affected lessons and required fixes, and save remains disabled.
- The actual same-image provider restart reverifies all eight pairs with identical artifact/kernel and a new
  counter epoch. [Restart startup](restart-startup-ready.json), [restart request](restart.json).
  The complete saved timetable and saved school copy remain exact in the browser:
  [restart Chrome ledger](../../../playwright/demo-timetable-experience/public/imperial-restart-20261017-1/ledger.md),
  [XML](restart-browser-tests.xml). The restart screenshot was opened and inspected.
- [Before cleanup](public-before-cleanup/cleanup-public-checks.json) and
  [after cleanup](public-after-cleanup/cleanup-public-checks.json): 40 consecutive stateless reads, nine public
  edge/Origin/CSRF probes, all 40 static hashes and eight exact definition/result pairs pass. No cookies created.
- [Final aggregate](events-final.jsonl) and [cleanup proof](cleanup-final.json) show zero active workspaces,
  zero running solve/verification processes, no cache entries and no queued work. Browser contexts/private
  servers stopped; temporary browser cookie state removed. The production-only stage is retained under the
  recorded private-tmp path. `git diff --check` and final source/stage/archive/sample-resource hashes pass.

Overall PASS. UC-1's Imperial revision is APPROVED. Existing UC-2/UC-3 handoffs remain closed. No unrelated
backend suite was repeated; prior unchanged evidence retains its recorded scope. Capacity/billing qualification
remains separate from this functional update.
