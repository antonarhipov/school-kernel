# UC-1 — reusable class splits

Base `3cb66f30ef996a8cf3bb93f968cc53db15fc1191`; uncommitted exact [submission hashes/source and patch](../../../output/student-groups-and-coordinated-lessons/UC-1/submission.json).

Implemented class-local split/group UUIDs, editable complete labels/counts, balanced defaults for 2–128 groups, reusable subject references, independent named arrangements, explicit balanced resizing preserving surviving identities, affected-reference preview/protection, field-linked issues, raw unsupported-data protection and full authoring document/recovery retention. Existing atomic autosave, stale-tab, retry and discard lifecycle are reused. Split/group totals do not silently follow class-size edits.

Validation:

- `node --test tools/demo/*.test.mjs`: 56/56 PASS.
- `./mvnw -pl timetable-workspace -am -DskipTests package`: PASS.
- `./mvnw -pl timetable-workspace -am -Dtest=NoUnitTests -Dsurefire.failIfNoSpecifiedTests=false -Dit.test=StudentGroupsBrowserIT -Dfailsafe.failIfNoSpecifiedTests=false verify`: 1/1 PASS with real installed Chrome and packaged demo. Local sockets require unsandboxed execution; no public service or operator data touched.
- `git diff --check`: PASS.

[Recorded administrator journey](../../../output/playwright/student-groups-and-coordinated-lessons/UC-1/1792347335832/ledger.md): two/three/four/five groups, normative sizes, custom Unicode/markup text, invalid/unfinished totals/counts, reload exact equality, protected reference refusal/cancel/explicit rename, quota failure with unchanged durable revision, actual stale-tab refusal, scoped removal, keyboard traversal, discard and 768/1024/1440px layout assertions. Desktop and narrow screenshots inspected. Empty browser-error log. Disposable server/context cleaned up. Failed attempts remain under the same UC directory.

Changed production: `setup-splits.js`, `setup-catalog.js`, `setup-editor.js`, `setup-model.js`, `school-document-schema.js`, `styles.css`. Checks: `setup-splits.test.mjs`, `StudentGroupsBrowserIT.java`. Unrelated Python cache change preserved.

APPROVED (automated). Destination handoffs: UC-2 relationships; UC-3 teaching consumption; UC-6 readiness of active grouped definitions; UC-8/9 published kernel semantics. These destinations remain unapproved until independently implemented and verified.


## D035 correction — 2026-10-19

APPROVED automatically for the corrected subject-facing journey; [scoped contract, exact diff and final runtime evidence](D035.md). Earlier implementation/destination statements above are historical and are superseded by the feature closure and this correction. The existing class-setup journey also passes against the corrected build.
