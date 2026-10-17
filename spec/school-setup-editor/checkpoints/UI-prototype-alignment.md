# Planning prototype alignment

Approved 2026-10-18 following the user's request to review the planning prototype and simplify the implemented UI. This supersedes the earlier presentation evidence; the feature's scheduling and persistence contracts remain in force.

Reference: `output/design/school-setup-revival/grid-editor-concepts.html`, Day pattern variant, and its standalone preview. Reviewed the actual rendered prototype alongside the application. Both are captured in the final visual walkthrough. The previous accepted uncommitted implementation is preserved in `output/school-setup-editor/2026-10-18-final/`; Git base remains `be5b76d0ffc8a9851c4e8308540b1de12c5b4425`.

Resolved review findings:

- `demo-workbench.css:129`: keep the vertical tab on the timetable's upper-left edge at desktop and phone widths, matching the prototype's orientation.
- `setup-editor.js:202`: play a 350ms transform animation on every opening and closing. Honor reduced motion, retain native modal isolation and return focus to the entry on Escape/close. Keyboard focus starts at the section heading to avoid opening a mobile keyboard unnecessarily.
- `setup-editor.js:124`: replace the large preliminary form/action block with the prototype's compact school-name/save-state header, numbered sidebar, focused task area and next/previous footer. Keep recovery failures visible beside the task; place JSON, draft selection and recovery downloads under Draft & data tools. Context changes restore the task's top rather than inheriting another section's scroll offset.
- `setup-grid.js:31`: replace stacked interval fieldsets with compact named rows, inline reorder/remove actions, independent day-copy controls and the prototype's week overview. Preserve raw invalid times, exact period identities and imported boundary intent. Responsive rows retain labels and fit the task's scroll container.
- `setup-catalog.js:93`: keep technical IDs out of the ordinary record browser while retaining ID search, underlying identities and protected-source inspection. Fix live name updates so they do not reinsert IDs. Group class details, shorten help while preserving curator/room meaning, and keep optional scheduling controls collapsed.
- `setup-teachers.js:32`: shorten the qualification and workload explanations; class eligibility and availability remain explicit, separate actions.
- `setup-review.js:15`: disclose the detailed hard/soft contract and demo limits under an expandable summary, leaving readiness, totals and issues prominent.
- `demo-workbench.css:159`: preserve the selected section's green highlight while hovered, including on phones. Status dots distinguish pending and failed storage states.

Validation:

- Full Chrome regression: 18 journeys, 17 initially passed. The exchange journey exposed a test synchronization race: its exact nonmutation snapshot was taken while the preceding explicitly accepted advanced-buffer transfer was still autosaving. The assertion now waits for that exact transferred text to be persisted; its exact nonmutation check remains intact. Failed evidence is retained.
- Final affected Chrome rerun: 8/8 PASS (UC-1/2/3/14/15/16/17 plus prototype comparison). Includes real kernel generation, independent manual review, process interruption, save/reload, storage recovery and explicit JSON exchange.
- Final screenshot-discovered hover correction and curator-help clarification: 2/2 PASS (prototype comparison and UC-4). All 18 distinct browser journeys therefore have passing current or unchanged-applicable evidence.
- Node suite 41/41 PASS; `git diff --check` PASS. Kernel/server contracts were unchanged in this refinement, so their previously passing technical evidence is reused.
- Final visual walkthrough: `output/playwright/school-setup-editor/UI-prototype/1792275494844/ledger.md`. Screenshots inspected: original prototype, day-pattern desktop, rollout mid-animation, subject/class/teacher screens, and 320/390px layouts. Automated layout checks also cover 768/1440px, footer reachability, active-section hover, reduced motion and keyboard return.

Exact UI diff against the previous accepted working state, complete source hashes, test commands, logs and passing ledger index: `output/school-setup-editor/2026-10-18-ui-alignment/ledger.md`. Browser/server contexts are disposable and stopped. No deployment or commit; no scheduling/data-model changes or operator-data mutations.
