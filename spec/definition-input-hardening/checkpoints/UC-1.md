# UC-1 — Safely edit and check a school definition

## Submission

- User authorization: recommendations 1–5 and 8, 2026-10-17.
- Base: `d55ab636558224f2d33a5e71313e840dd9486428` plus the pre-existing dirty checkout.
- Initial status/diff and exact copies of touched pre-existing files:
  `/private/tmp/definition-hardening-base-9g95r8vf`. ProblemResponder's baseline was reconstructed from the captured
  initial patch and HEAD; no checkout file was reset.
- Isolated build and source manifests: `/private/tmp/definition-hardening-check-lbrm_j5v` and
  [run-2 source manifest](../../../output/execution/definition-input-hardening/source-manifest-run-2.json).
- Initial implementation made no commit or deployment. The later user-authorized Free deployment is recorded below.
  Preserve concurrent local-data cleanup and all earlier demo/kernel work.

## Implemented contract

- UC-1 main step 3 / extension 3a / G1–G3: a demo-only filter follows CSRF/authorization and precedes MVC/multipart
  decoding on definition check/use, review, import and definition upload. Four global and two session-owned immediate
  leases span processing and response production. Cancellation and safe reads are excluded. A busy response has
  `INPUT_BUSY`, 429 and `Retry-After: 1`; it does not copy or read the workspace aggregate.
- UC-1 main step 4 / extension 4a: shared demo parsing limits depth, number/string/name length and token count, rejects
  duplicate/trailing content, and preserves strict UTF-8 definition/import handling. Definition dimensions are checked
  with bounded traversal; schema checking fails on its first error before canonicalization. Canonicalization failure
  becomes an ordinary 422 response. Existing byte and multiplication-overflow-safe placement gates remain intact.
- UC-1 extension 4b: unexpected exceptions return a fixed public explanation and correlation ID. The log records that
  ID and exception class only, excluding potentially sensitive exception messages, stack traces and request text.
- UC-1 main step 2 / extensions 2a–2b / G5: 300 ms editor debounce, UTF-8 size checks before repository cloning/writing,
  eligible flush on close/navigation/save/page exit, and pause after storage refusal until explicit retry. Pending
  editor work is cancelled after successful copy creation and after the existing explicit local cleanup succeeds.
- UC-1 G4: schema, domain references and existing display escaping are retained. Valid school definitions keep all
  fields and optional values. Shared kernel schema and PostgreSQL ownership are unchanged.

## Technical evidence

- New `DefinitionInputHardeningTest`: six passing cases cover all bundled schools by value; SQL/HTML-like names as
  data; numeric overflow, duplicates, trailing content, depth and invalid UTF-8; all collection/string/object bounds;
  bounded schema diagnostics; no verifier/no mutation on refusal; public exception concealment; concurrency ceilings,
  safe reads/cancellation, rejection before body access, no workspace read during refusal, and lease release.
- Consolidated isolated workspace suite: **86 tests, zero failures/errors/skips** in verification run 2.
- First sandbox attempt hit existing Mockito self-attach/process-inspection restrictions. Supplying the Mockito agent
  and allowing local process access resolves those checks. The first isolated full run also exposed an omitted
  `compose.yaml` fixture; adding the unchanged fixture produces the passing 86-test result. Retain these attempts.
- Browser/API regression suite: **six integration scenarios passed**, zero failures/errors/skips: two new hardening
  scenarios, all three existing timetable journeys and existing local-artifact cleanup. The final size-guard and stale
  quota-notice delta then passed all six hardening tests and the complete editor browser journey again.

## Actor evidence

- [Packaged TLS admission ledger](../../../output/execution/definition-input-hardening/http-run-2/ledger.md): real
  incomplete HTTP bodies fill per-session/global leases; excess owners receive 429 before decoding; safe reads retain
  exact workspace state; closing the incomplete clients releases leases and subsequent input reaches normal 422
  validation. No visitor-triggered kernel process is launched.
- [Recorded Chrome editor journey](../../../output/playwright/definition-input-hardening/UC-1/run-2/ledger.md): exact
  debounce/reload retention, multibyte oversize non-persistence, injected quota refusal/pause/recovery, real HTTP 422
  for the 400-digit integer without kernel launch, and an exact checked school copy without solving.
- Desktop oversized feedback and 390px checked-copy screenshots were opened and inspected: controls and feedback fit,
  the textarea scrolls internally, and there is no page-level horizontal overflow.
- Disposable TLS sockets/browser owners/server processes close; no operator school data or database is used.

## Final verification and approval

2026-10-17: **APPROVED**. The final [Chrome editor ledger](../../../output/playwright/definition-input-hardening/UC-1/run-3/ledger.md)
adds explicit oversized Save refusal and stale quota-notice clearing to the earlier complete journey. Its desktop
and 390px screenshots were opened and inspected; controls and feedback fit. The browser reported no page errors.

Run 2, from the isolated build directory above:

```sh
mvn -o -pl timetable-workspace -am -Pworkspace-browser verify \
  -Dit.test=DefinitionInputHardeningBrowserIT,DemoTimetableBrowserIT,DemoLocalDataBrowserIT \
  -Dfailsafe.failIfNoSpecifiedTests=false \
  -DargLine=-javaagent:/Users/anton/.m2/repository/org/mockito/mockito-core/5.23.0/mockito-core-5.23.0.jar \
  -Dhardening.walkthrough.dir=/Users/anton/IdeaProjects/school-kernel/school-kernel/output/playwright/definition-input-hardening/UC-1/run-2 \
  -Dhardening.http.dir=/Users/anton/IdeaProjects/school-kernel/school-kernel/output/execution/definition-input-hardening/http-run-2 \
  -Ddemo.local-data.walkthrough.dir=/Users/anton/IdeaProjects/school-kernel/school-kernel/output/playwright/definition-input-hardening/UC-1/local-data-regression-2
```

Result: BUILD SUCCESS; 86 workspace unit/architecture tests and six integration scenarios passed.
[Log](../../../output/execution/definition-input-hardening/verification-run-2.log),
[reports](../../../output/execution/definition-input-hardening/reports-run-2),
[timetable walkthroughs](../../../output/playwright/definition-input-hardening/UC-1/timetable-regression-run-2),
[cleanup walkthrough](../../../output/playwright/definition-input-hardening/UC-1/local-data-regression-2/ledger.md).

Run 3, after copying the final two JavaScript changes and strengthened editor assertions into the isolated build:

```sh
mvn -o -pl timetable-workspace -am -Pworkspace-browser verify \
  -Dtest=DefinitionInputHardeningTest -Dsurefire.failIfNoSpecifiedTests=false \
  '-Dit.test=DefinitionInputHardeningBrowserIT#safelyChecksDefinitionsAndRetainsDebouncedEditorWork' \
  -Dfailsafe.failIfNoSpecifiedTests=false \
  -DargLine=-javaagent:/Users/anton/.m2/repository/org/mockito/mockito-core/5.23.0/mockito-core-5.23.0.jar \
  -Dhardening.walkthrough.dir=/Users/anton/IdeaProjects/school-kernel/school-kernel/output/playwright/definition-input-hardening/UC-1/run-3 \
  -Dhardening.http.dir=/Users/anton/IdeaProjects/school-kernel/school-kernel/output/execution/definition-input-hardening/http-run-3
```

Result: BUILD SUCCESS; six hardening tests and one complete Chrome journey passed, zero failures/errors/skips.
[Log](../../../output/execution/definition-input-hardening/verification-3.log),
[reports](../../../output/execution/definition-input-hardening/reports-run-3),
[final source manifest](../../../output/execution/definition-input-hardening/source-manifest-run-3.json).
The final isolated manifest matches the live source. Run-2 sources remain recorded separately; only the final autosave
delta and a concurrent worker's two cleanup-test diagnostic lines changed between runs. Earlier sandbox/fixture
failure logs are retained in the same execution directory. Final whitespace validation: `git diff --check`.

## Changed files and exact diff

Under `timetable-workspace/src/main/java/org/schoolkernel/workspace/`: `DemoInputAdmissionFilter.java`,
`DemoInputJson.java` (new); `DemoDefinitionLimits.java`, `DemoTimetableController.java`, `SafeImportReader.java`,
`SecurityConfiguration.java`, `ProblemHandler.java`, `ProblemResponder.java`, `DemoEvents.java` (updated).
Under `src/main/resources/static/workspace/`: `definition-autosave.js` (new), `demo-definitions.js` and
`demo-workbench.js` (updated). Under `src/test/java/org/schoolkernel/workspace/`:
`DefinitionInputHardeningTest.java` and `DefinitionInputHardeningBrowserIT.java` (new).
The four Markdown files in `spec/definition-input-hardening/` record this feature and its approval.

[Original implementation submission patch](../../../output/execution/definition-input-hardening/task-only.patch) is relative to the exact
pre-task dirty baseline, not HEAD; applying it to HEAD alone requires the pre-existing demo work. The patch includes
only these 14 implementation/test files and four feature documents. Verification artifacts are retained separately.
No unrelated source was reset, committed or deployed.

## User-authorized deployment

2026-10-17: Deployed to the existing `school-kernel-free-smoke / production / demo-smoke` at the user's explicit
follow-up request. [Deployment ledger](../../../output/execution/definition-input-hardening/deployment-20261017-1/ledger.md)
pins the 177-file production-only payload, exact 12-file delta from the preceding cleanup release, provider image,
unchanged Free configuration, public Chrome journey and final same-image cleanup. The current deployment is
`2ecc350c-5519-4a0d-ba20-a4f19edd5137`; all public checks pass. No commit or provider setting change was made.

## Scope and limitations

These are in-process admission and definition safety limits for the single-replica demo, not a DDoS guarantee or
production capacity qualification. PostgreSQL tenant isolation, future database Unicode policy and cache privacy
remain outside authorization. No destination use-case handoffs remain.
