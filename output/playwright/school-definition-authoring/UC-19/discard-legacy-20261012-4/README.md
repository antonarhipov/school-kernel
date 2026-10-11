# UC-19 persisted legacy discard correction — PASS / automatically APPROVED

2026-10-12. Base `5c87f44`; corrected submission `726774df028002d136dde376d1f5086af21cb2b7`. Packaged `/tmp/discard-authoring-fixed-r2.jar`, SHA256 `8c7cc1010d65d6f9932caa9adbfa7c7ec7e888e48c2a17fddcfbb561dc025fb9`; no production changes since this recording. Headed Chrome via installed Playwright CLI, fresh `discard-legacy-final` and `discard-operator-verify` contexts.

Failure was reproduced with the exact persisted operator v233, whose legacy draft has sourceState/sourceDefinitionRevision/sourceCatalogVersion and no sourceDocument. Preserved accepted source remains alongside authoring. Isolated copied data at http://127.0.0.1:18111/workspace/, disposable PostgreSQL `authoring_discard_legacy_20261012` in `school-authoring-loop-20261011`. Ordinary initial/EMPTY and explicitly null-source variants were fixture setup only in that isolated database. Actual localhost8080 used only for load, opening confirmation and cancellation; operator draft was never confirmed/discarded or edited. Original user's browser/working form not reloaded.

## Real administrator journey

| Contract | Action / expected | Observed / evidence | Verdict |
|---|---|---|---|
| Main1–2, G2–3 | Click Discard authoring on exact legacy draft; source identified, no crash/mutation | Dialog names ACCEPTED_BASELINE / Merivälja Kool, explains legacy source verification. Whole v233 unchanged. [Image01](01-legacy-source-confirmation.png), [full values](walkthrough-values.json). | PASS |
| Extension3a | Enter unsaved literal name, cancel, keep saved and working values | Exact unsaved `Unsaved <literal> Õ` retained, entire saved workspace/version unchanged. [Journey](journey.js), [values](walkthrough-values.json). | PASS |
| G3 | Resize open dialog390, require read-only confirm, then cancel/resize back | Confirm disabled, document width≤390, cancel works, saved v233 and unsaved form exact. [Image02](02-narrow-confirmation-disabled.png). | PASS |
| Main3–5, G1–2 | Explicitly confirm legacy discard; restore exact original accepted workspace | DELETE200, v234 ACCEPTED_BASELINE. Kernel independently verifies normalized source revision and accepted result; removes only definitionDraft. Entire source,520 lesson assignments, original name, definition/result/manifest/importMode/lastRun/revisions equal preserved source by value. Reload entire snapshot exact. [Image03](03-legacy-current-restored.png), [values](walkthrough-values.json), [kernel verification log](isolated-server.log). | PASS |
| Main1–5, G1 | Re-enter normal accepted authoring; discard modern recorded source | Modern v235 sourceDocument exactly v234 workspace; explicit discard200 restores exact v236 source. [Image04](04-modern-source-confirmation.png), [values](walkthrough-values.json). | PASS |
| Main1–5, G1 | Ordinary initial source entry/confirmation/discard/reload | Isolated initial v237→authoring238→initial239; definition/revision/school source exact. [Image06](06-initial-source-confirmation.png), [values](initial-source-values.json), [log](initial-source.log). | PASS |
| Main1–5, G1 | Create from EMPTY then explicitly discard/reload | Empty v240→authoring241→EMPTY242, exact empty document and no adopted school/Current. Confirmation explains newly created definition removal. [Image07](07-empty-source-confirmation.png), [values](empty-source-values.json), [log](empty-source.log). | PASS |
| Extension4a, G2 | Attempt explicit null-source discard, then cancel | Clear unavailable-source explanation; DELETE409 INVALID_AUTHORING_SOURCE, whole v243 exact; no pageerror, cancel closes normally. [Image08](08-unavailable-source-refusal.png), [response/state](unavailable-source-values.json), [log](unavailable-source.log). | PASS |
| Actual reported workspace | Open real localhost8080 legacy confirmation; cancel, preserve operator work | Accurate Merivälja source dialog, zero uncaught errors; entire saved DEFINITION_DRAFT v233 before/after exact. No confirmed DELETE issued. [Image05](05-actual-operator-confirmation.png), [full values](operator-values.json), [log](operator.log), [snapshot](operator-snapshot.log). | PASS |

All8 screenshots and [contact sheet](contact-sheet.jpg) inspected. Source identity, English explanation, exact retained literal input, reachable buttons,390 viewport and refusal feedback visible; no horizontal page overflow. API state assertions provide full values where another screenshot would add no useful evidence. Console-error arrays empty on all final scripts. Prior failing old-context console entries are retained in earlier attempts, never counted as final success.

## Technical checks

[Technical log](technical.log) — BUILD SUCCESS:

```sh
mvn -pl timetable-workspace -am verify '-Dtest=*Test' -Dsurefire.failIfNoSpecifiedTests=false '-Dit.test=DefinitionDiscardBrowserIT,SavedDefinitionDraftBrowserIT' -Dfailsafe.failIfNoSpecifiedTests=false
```

33 workspace unit/architecture checks;5 discard +3 persistence browser/API cases, zero failures/errors/skips. New legacy accepted/initial source fixture deliberately reverses lesson array order, proving kernel-semantic revisions rather than authoring JSON hashes.8 missing/null/array/revision/catalog/changed-definition/changed-source-name/competing-draft variants preserve whole snapshots and return409. Original3 discard cases retain all3 source, stale/access/storage failure/retry and cancel/narrow coverage. [Package](package.log) and syntax/diff checks PASS. Final text-only count wording avoids claiming derived modern change counts for legacy drafts and is verified by final screenshots; production behavior checks unchanged.

Legacy compatibility uses existing independent KernelVerifier; it preserves source representation rather than sorting/adopting authored values. Requires matching recorded/top-level/kernel/result revision, source catalog and school identity, and no competing workflow. Explicit null/malformed sourceDocument is not treated as recoverable legacy. Kernel invalid-input or mismatched evidence refuses; unavailable verifier prevents restoration. Normal modern recorded-source path unchanged, no migration.

Earlier attempts retained: [attempt1](../discard-legacy-20261012-1/README.md) reproduced exact uncaught school lookup; [attempt2](../discard-legacy-20261012-2/README.md) safely refused legitimate kernel-normalized revision due to first raw-hash guard; [attempt3](../discard-legacy-20261012-3/README.md) initial reload stopped by previous context's native beforeunload, with no mutation. No assertions weakened; actual source repair proven through complete changed-build repeat.

No new fault injection is claimed in this CLI walkthrough; storage/access/stale paths come from the fresh technical discard tests. Other approved UCs and final136-unit/28-representative baseline evidence remain unchanged; this correction reran all applicable discard/persistence checks.

## Runtime disposition and approval

Isolated18111/PID34731 stopped; test contexts `discard-legacy-repro` / `discard-legacy-final` closed ([cleanup](cleanup.json)). Disposable database and immutable artifacts retained. Its final explicit null-source fixture remains saved atv243; no solve or fault trigger active.

The operator8080 runtime was stopped when reported browser's old page was inspected. Started verified fixed build at http://localhost:8080/workspace/, PID34734/native session78123, `/tmp/discard-operator-server-fixed.log`, using existing `school-kernel-workspace-postgres-1` database `school_workspace` port62855. Flyway validated existing3 migrations, made none; whole saved operator v233 exactly unchanged before/after startup and dialog/cancel. Fixed verification browser `discard-operator-verify` retained, user's original browser untouched. Reload original page to load fixed script; discard remains an explicit administrator confirmation. No operator authoring data was removed.

Overall **PASS**, UC-19 automatically **APPROVED** for the correction. Earlier approvals/ledgers preserved. All20 feature use cases remain approved; no separate human/convergence gate added.
