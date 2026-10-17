# UC-2 checkpoint

Base: `be5b76d0ffc8a9851c4e8308540b1de12c5b4425`; reviewable uncommitted diff on approved UC-1.

Implemented: Monday–Sunday active days; named timed intervals of the four specified kinds; numbered teaching
opportunities; independent interval/period identities; freely switched day editing; invalid timing retained with
located issues; day copy preview/replacement; explicit cancellation and complete per-reference clear/remap before
removing teaching opportunities. All-period versus explicit-empty whitelist intent is preserved. Break/lunch kinds
and exact order are stored for the later compiler; no availability marking or lesson placement is copied.

Changed production files: new `setup-grid.js`, integration in `setup-editor.js` and `setup-model.js`, layout in
`demo-workbench.css`; browser `SchoolSetupBrowserIT.java` and focused `tools/demo/setup-grid.test.mjs`.

Checks: Node demo suite 21/21; full packaged Chrome UC-2 journey and affected UC-1 regression pass; `git diff --check`.
The UC-2 journey creates the actual week, tests copy/cancel, invalid-input reload, independent values/identities,
all five restored availability/lunch/preference/real periodLock references, non-mutation on unresolved/cancelled
changes, explicit clears and empty-whitelist retention, and 320/390/1440px overflow. Screenshots inspected.

Handoffs: later UCs create availability/lunch/block rules through their controls. Compiler and published kernel
consumption of stored adjacency are UC-12/13/15/17 work; this approval does not establish their implementation.
