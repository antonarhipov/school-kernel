# UC-5 checkpoint

Base `be5b76d0ffc8a9851c4e8308540b1de12c5b4425`; uncommitted reviewable diff on approved UC-1–4.
Teacher add/find/rename/remove; independent subject qualification and per-subject class eligibility; new teachers
available in all teaching periods; curator qualification stays own-class/subject-specific; derived class-plan
allocations/workload and owning-class links. Qualification/eligibility removal preserves allocated work with
located blocking issues. Referenced teacher removal refuses and lists allocations, curator and retained rules.

Production: `setup-teachers.js`, integration in `setup-catalog.js`/`setup-model.js`/`setup-editor.js`,
`demo-workbench.css`. Tests: `SchoolSetupCatalogBrowserIT`, `tools/demo/setup-teachers.test.mjs`.
Node checks 25/25 pass. Packaged Chrome UC-5 plus affected UC-1–4 full journeys: 5/5 pass, no runtime errors.
Browser verifies exact non-cross-product eligibility/qualification, 40-teacher browse/search, reload, default all
availability, derived five-occurrence workload with block not double-counted, removal refusal non-mutation,
qualification-removal retention, ownership links and cancelled/confirmed unused teacher deletion.
320/390/1440px overflow/layout evidence retained. Allocation editing (UC-6) and marking availability (UC-8) remain
explicit handoffs; prerequisite plan data in this walkthrough is identified as a restored fixture.
