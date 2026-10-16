# UC-2 technical execution ledger

Date: 2026-10-16. Base: `d55ab636558224f2d33a5e71313e840dd9486428` (`demo`).
Exact submitted implementation/test/tool files are listed in [changed-files.txt](changed-files.txt), bound by
[source-sha256.txt](source-sha256.txt). [production-diff.patch](production-diff.patch) records the tracked-code diff;
new files remain at the paths in the manifest. The manifest includes unchanged-since-approval UC-1 production edits.
Pre-existing `spec/README.md`, proposal and specification inputs were preserved; no commit was requested/performed.

Commands/results:

- `./mvnw -q -pl timetable-workspace -am test`: PASS, **39 tests / 10 workspace classes**, zero failures/errors/skips.
  [Per-class summary](unit-summary.txt), [full log](unit.log). Includes four DemoSessionTest cases and two
  DemoWorkspaceStoreTest cases: cap, fake-clock idle/absolute expiry and polling, deep-copy isolation, owner capture,
  run/version guards, authoring absence, security/health and ended-session CSRF/explicit restart.
- `./mvnw -q -pl timetable-workspace -am -Dtest=DemoSessionTest,DemoWorkspaceStoreTest -Dsurefire.failIfNoSpecifiedTests=false -Dit.test=DemoSessionBrowserIT -Dfailsafe.failIfNoSpecifiedTests=false verify`:
  PASS, six selected unit tests plus one packaged integration journey. [Browser report](browser-summary.txt),
  [command log](browser5-passed.log), [full browser ledger](../../../../playwright/demo-deployment/UC-2/1792140998412/ledger.md).
  The actual downloaded definition/result/manifest equal Current by value. A second owner's bundle import equals
  the complete exported accepted unit. Declined, active-run and stale operations preserve the observed aggregate.
  Clearing one owner preserves the other aggregate exactly. Browser saved text is byte-for-byte retained.
- Full default-profile `WorkspaceImportIT`: **25 tests, 23 passed, 2 failed** against disposable PostgreSQL, not operator
  storage. [Current report](default-profile-regression.txt). Both failures were run on an isolated `git archive` of
  the unchanged base, with its own rebuilt kernel, and reproduced: [baseline report](unchanged-base-failures.txt),
  [baseline log](baseline-regression.log). `exhaustiveLifecycleRefusalMatrix` expects 200 but gets 500 at session line 892;
  `completeVerificationInvalidityMatrixAtHttpBoundary` fails its response-content assertion at line 1066. These are
  established baseline failures, not silently waived new regressions. Existing authoring/restart behavior was not
  claimed fully requalified from this one API suite.
- `git diff --check`: PASS after the final implementation and deployment preparation.

Retained fix evidence:

- First sandbox-only browser attempt could not bind its disposable TLS socket. Native escalated execution made the
  boundary available; it was an environment restriction, not a passing browser result.
- [Export failure attempt](../../../../playwright/demo-deployment/UC-2/1792140404169/failed-attempt.md): existing export
  refused a repair-state Current. Demo export now copies the exact trusted accepted unit during staging and keeps
  integrity checks. Visual inspection also found narrow JSON editing enabled; it is now read-only.
- [Restart failure attempt](../../../../playwright/demo-deployment/UC-2/1792140629203/failed-attempt.md) and
  [log](browser2-failed.log): ended-session CSRF could not refresh. The focused regression then exposed the local
  definition-authoring access guard on explicit restart ([log](restart-regression-failed.log)); both guards are fixed.
- [First complete local PASS](../../../../playwright/demo-deployment/UC-2/1792140838144/ledger.md) was expanded to include
  exact accepted-bundle import, declined/confirmed clear and active-run refusal. The later full passing ledger is
  the current local evidence; earlier failures remain retained.

Final verdict: **local implementation and recorded walkthrough PASS; UC-2 BLOCKED for approval**. Required real
public Railway host/scheme forwarding and two-browser evidence are unavailable without the operator environment.
No Railway CLI or connected browser surface is available; no project/service/generated domain was supplied. An
environment-information question is pending. The execute workflow's unavailable-boundary rule is applied; local
results do not substitute for the spec's public application boundary.

Cleanup: isolated browser contexts and both temporary server processes closed; JUnit removed certificates, private
inputs and downloads. Testcontainers used disposable PostgreSQL; the unchanged baseline directory is retained under
`/private/tmp/school-demo-baseline-d55ab636` for reproducibility. No operator database, unrelated school or existing
browser tab was changed. No deployment, financial setting, external message or memory update was performed.
