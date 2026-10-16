# UC-3 sample walkthrough

Date: 2026-10-16T13:06:47.200826Z
Application: https://localhost:58929
Browser: Chrome, Playwright; two isolated disposable contexts and one stale shared-owner tab.
No operator data or database used. Public TLS uses normal certificate validation. Local TLS is disposable.

PASS | main 1/G4/G6: Full preview in EMPTY, exact catalog/default effort, public ETag 304 and visitor no-store | [screenshot](01-preview-full.png) / [snapshot](01-preview-full.txt)

PASS | 1a/2a: unsaved text disables use; declined selection retains exact text, selection and EMPTY | [screenshot](02-unsaved-decline.png) / [snapshot](02-unsaved-decline.txt)

PASS | G4: delayed older preview cannot replace the latest selection/text or Current | [screenshot](03-preview-race.png) / [snapshot](03-preview-race.txt)

PASS | main 2-5/G1: explicit small opens the exact complete accepted pair, no solve, other owner EMPTY | [screenshot](04-open-small.png) / [snapshot](04-open-small.txt)

PASS | main 2-5/G1: explicit two-class opens the exact complete accepted pair, no solve, other owner EMPTY | [screenshot](04-open-two-class.png) / [snapshot](04-open-two-class.txt)

PASS | main 2-5/G1: explicit full opens the exact complete accepted pair, no solve, other owner EMPTY | [screenshot](04-open-full.png) / [snapshot](04-open-full.txt)

PASS | G5: Full whole-school Week renders 520 lessons with fictional names and faithful lesson details | [screenshot](05-full-week-details.png) / [snapshot](05-full-week-details.txt)

PASS | G5: Full Day and teacher lens retain names, availability and context | [screenshot](06-full-day-teacher.png) / [snapshot](06-full-day-teacher.txt)

PASS | G5: room lens replaces teacher lens and remains private read-only inspection | [screenshot](07-full-room-lens.png) / [snapshot](07-full-room-lens.txt)

PASS | G5: 390px narrow Day inspection is read-only with no page overflow | [screenshot](08-narrow.png) / [snapshot](08-narrow.txt)

PASS | main 5/G4/G5: Full repair entry stages private intent; complete Current and second visitor stay exact | [screenshot](09-private-repair.png) / [snapshot](09-private-repair.txt)

PASS | main 5: Full export entry downloads exact accepted definition/result/manifest and excludes unfinished repair | [screenshot](09b-exact-export.png) / [snapshot](09b-exact-export.txt)

PASS | main 3/3a: replacement names Full Current and repair draft, offers export, decline preserves everything | [screenshot](10-replacement-decline.png) / [snapshot](10-replacement-decline.txt)

PASS | 4a/4b: unknown 404, missing confirmation 428, stale browser 412; reload preserves selection and newer complete work | [screenshot](11-stale-reload.png) / [snapshot](11-stale-reload.txt)

PASS | minimal guarantee: simulated HTTP failure preserves complete server work and selected JSON; reload/retry offered | [screenshot](12-failure-preserves.png) / [snapshot](12-failure-preserves.txt)

PASS | main 5/G4: Full manual entry opens a private draft, cancelled replacement preserves Current/draft | [screenshot](13-manual-entry.png) / [snapshot](13-manual-entry.txt)

PASS | G1/G4/G6/G7: source ETags/documents and second owner unchanged; no solve/accept request, no visitor kernel launches | [screenshot](14-second-owner-preserved.png) / [snapshot](14-second-owner-preserved.txt)

Overall: PASS

Failure injection is explicitly a browser-simulated 503; service save failure and readiness faults have separate technical evidence.
Contexts closed. Local server stopped; public disposable workspaces remain until expiry/restart, with no shared browser identity retained.

## Submission and visual review supplement

Base: `d55ab636558224f2d33a5e71313e840dd9486428`, uncommitted dirty `demo` checkout.
The [checkpoint](../../../../../spec/demo-deployment/checkpoints/UC-3.md) binds this journey to its exact
[production patch](../../../../execution/demo-deployment/UC-3/20261016-1/uc3-production.patch) and final source manifest.
Full Week/details, Day/lenses, stale recovery, manual draft and 390px read-only screenshots were opened and visually
inspected: labels/names and Current/draft distinction are readable, controls fit, matrix scrolling stays contained,
and the recorded page-width assertion passes. No presentation failure was found. All sample Current documents and
second-owner state comparisons are by complete JSON value, rather than only status/count.

The final checkout also contains a separately preserved concurrent text-only Delete all storage-control edit.
The journey above ran the staged production snapshot, whose exact bytes are retained in submission.zip and the
approved-production-source-manifest.json. That separate copy edit is excluded from this verdict.
