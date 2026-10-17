# UC-8 checkpoint

Base `be5b76d0ffc8a9851c4e8308540b1de12c5b4425`; uncommitted diff on approved UC-1–7.
Teacher availability shows exact teaching times/reserved states, all three specified hard/soft tools, individual
marking, multi-period selection across days, whole-day marking and an explicit timed afternoon range. Replacement
clears/replaces both authored facts for only selected periods; imported simultaneous unavailability/avoidance remain
visible until edited. All-period and explicit-empty-whitelist intent stay distinct; no allocation is changed.
Capacity issues remain visible and source data survive reload. No break/lunch is placeable or markable.

Files: `setup-availability.js`, integrations in setup editor/teachers/model, responsive marking CSS;
`tools/demo/setup-availability.test.mjs`, UC-8 browser journey. Node checks 30/30; full packaged UC-8 journey and
affected UC-1/2/3/5 browser journeys pass, no browser errors. Exact dual facts/whitelist/allocations are asserted;
no-op timed range proves durable non-mutation; 320/390/1440px evidence retained. UC-5 availability handoff closed.
Block-window recomputation joins the shared issue pipeline in UC-11; kernel enforcement remains UC-12/13.
