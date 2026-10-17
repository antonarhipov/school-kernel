# UC-4 checkpoint

Base `be5b76d0ffc8a9851c4e8308540b1de12c5b4425`; uncommitted reviewable diff on approved UC-1–3.
Class add/find/rename, positive student counts and retained invalid input; optional existing curator/home room;
exactly five hard/soft scheduling controls with visible inherited defaults and explicit unrestricted states;
retained availability/preference disclosure; class-plan handoff. One teacher can curate multiple classes without
creating qualification, eligibility or demand. Scoped removal previews complete teaching/eligibility/lunch scope,
refuses protected occurrences/retained references, and requires confirmation before supported deletion.

Production changes: `setup-catalog.js`, `setup-model.js`, `setup-editor.js`. Evidence: new
`SchoolSetupCatalogBrowserIT.java`, focused class cases in `tools/demo/setup-catalog.test.mjs`.
Node suite 24/24 passes; packaged UC-4 full Chrome journey passes; affected UC-1–3 browser regressions passed on
this production submission. A failed test searched a short label also legitimately present in random IDs; the
corrected test uses a unique full name. Failure retained. Browser asserts by-value settings, incomplete reload,
20-class browsing, duplicate curator nomination, protected/cancelled deletion non-mutation and exact cleanup.
Screenshots/overflow cover 320,390,1440px. Class-plan editing remains the explicit UC-6 handoff.
