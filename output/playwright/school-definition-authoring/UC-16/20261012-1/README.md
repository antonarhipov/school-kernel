# UC-16 walkthrough — attempt 1

Date 2026-10-12, submission `0c360a4`. Chrome headed, fresh `authoring-uc16` session. Build `/tmp/uc16-workspace.jar`, URL http://localhost:18097/workspace/. Isolated PostgreSQL `authoring_loop_uc16` in container `school-authoring-loop-20261011`, verified startup datasource. Operator data untouched. Starting state EMPTY. Overall: FAIL, not approved.

Primary journey creates the exact Small-school acceptance data through forms, partial reload, generated grid and individual adjustments, exact saved native-field assertion, Review/confirmation/cancel/narrow inspection, packaged verification and initial plan, then explicit acceptance. Screenshots and logs accompany each step. Technical faults additionally supported by the checkpoint's isolated integration tests; they are not represented as browser clicks.

UC-16 G5 step 1: script filled name, then addressed nonexistent `#school-id` instead of the rendered `#school-identifier`. Timeout, no identity save/finish/solve/publication. [Failure screenshot](failure.png), [snapshot](failure-snapshot.txt), [exact persisted state](failure-state.txt), [command failure](primary.txt). This was a harness selector error; corrected for full fresh repeat. Isolated failed database retained.
