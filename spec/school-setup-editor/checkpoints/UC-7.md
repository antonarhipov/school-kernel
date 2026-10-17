# UC-7 checkpoint

Base `be5b76d0ffc8a9851c4e8308540b1de12c5b4425`; uncommitted diff on approved UC-1–6.
Curriculum copy previews source/destination and every subject/count, requires confirmation and an empty destination,
then atomically creates independent rows with unassigned demand and fresh row/series identities. Copies no teacher,
eligibility, class context, block/fixed-room rule, lock or occurrence. Source values and workloads remain exact.
Invalid source demand and occupied destination refuse without partial mutation. Cancellation changes nothing.

Files: `setup-curriculum.js`, integrations in `setup-plans.js`/`setup-editor.js`, corresponding Node and Java browser
journeys. Node 28/28 pass; packaged full Chrome UC-7 and affected UC-6 pass. Browser restores an explicitly labelled
UC-6 source with retained locks/rules, then verifies actual copy controls, exact source/class/eligibility preservation,
independent edits, reload and refusal durable equality. 320/390/1440px overflow evidence retained.
