# UC-3 sample walkthrough

Date: 2026-10-16T13:02:58.149135Z
Application: https://localhost:58171
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

PASS | FAIL: com.microsoft.playwright.TimeoutError: Error { | [screenshot](failure.png) / [snapshot](failure.txt)

Overall: FAIL: com.microsoft.playwright.TimeoutError: Error {
  message='Timeout 30000ms exceeded.
  name='TimeoutError
  stack='TimeoutError: Timeout 30000ms exceeded.
    at _ProgressController.run (/private/var/folders/c_/5_n98lpd65j__pbwgzctxx_80000gn/T/playwright-java-4984106176445706181/package/lib/coreBundle.js:12360:32)
    at DispatcherConnection.dispatch (/private/var/folders/c_/5_n98lpd65j__pbwgzctxx_80000gn/T/playwright-java-4984106176445706181/package/lib/coreBundle.js:19135:44)
}
Call log:
-   - waiting for locator("#start-manual-draft")


Failure injection is explicitly a browser-simulated 503; service save failure and readiness faults have separate technical evidence.
Contexts closed. Local server stopped; public disposable workspaces remain until expiry/restart, with no shared browser identity retained.
