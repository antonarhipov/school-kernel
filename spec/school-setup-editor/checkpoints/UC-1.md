# UC-1 checkpoint

Base: `be5b76d0ffc8a9851c4e8308540b1de12c5b4425`; uncommitted reviewable submission. No pre-existing dirty files.

Implemented: full-screen modal setup, exactly six sections, empty new-school snapshots, retained source drafts,
independent identities and explicit draft choice, literal names, catalog search/focused browsing, editing location,
settled autosave and flush boundaries, explicit durability labels/retry, refusal of stale overwrites, recovery
download/latest-data reload/separate recovered draft, scoped confirmed discard, focus and reduced-motion behavior.
The complete retained source document remains unchanged; no conversion, named school save or generation is implied.

Changed files: `setup-model.js`, `setup-editor.js`, `demo-workbench.js`, `demo-workbench.css`, `timetable-storage.js`
under `timetable-workspace/src/main/resources/static/workspace/`; `SchoolSetupBrowserIT.java`;
`tools/demo/timetable-storage.test.mjs`; this feature's status/checkpoint. Exact source hashes: passing run's `submission.json`.

Checks:

- `node --test tools/demo/*.test.mjs`: 19 passed, zero failures.
- `JAVA_HOME=/Users/anton/Library/Java/JavaVirtualMachines/jbr-25.0.2/Contents/Home ./mvnw -q -pl timetable-workspace -am -DskipTests -Dit.test=SchoolSetupBrowserIT -Dfailsafe.failIfNoSpecifiedTests=false verify`: packaged build and real Chrome journey passed. Local HTTPS/socket/browser execution required sandbox escalation; no public deployment.
- `git diff --check`: passed.

Source fidelity: source document is cloned whole, separate from timetable alternatives. Quota and conflict checks assert
exact durable non-mutation; explicit recovered copy retains the entire authoring snapshot. Desktop and 320/390px
screenshots inspected. Real-browser steps exercise 41 teachers and 22 classes, reload/reopen, conflict, unavailable
storage, failed writes, cancelled/confirmed discard, unknown formats and same-name draft selection.

Outstanding handoff: explicit source conversion preview/adoption belongs to UC-15. Subsequent grid, entity editing,
readiness, named school save and generation are not completed or approved by this checkpoint.
