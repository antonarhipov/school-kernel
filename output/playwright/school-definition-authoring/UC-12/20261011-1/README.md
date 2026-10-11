# UC-12 walkthrough attempt 1 — FAIL

Submission 068f9ce, 2026-10-11. Chrome authoring-loop, http://localhost:18088/workspace/, isolated authoring_loop_uc12 in school-authoring-loop-20261011. Actual datasource verified in /tmp/uc12-walkthrough-server.log; EMPTY/imported catalog-1 source and exact opening v2 asserted in entry-evidence.json. Technical prerequisites: 30 relevant journeys and 27 unit/architecture pass in /tmp/uc12-verify.log and /tmp/uc12-cohort-verify.log.

Main script main.txt observed preview/cancel, exact adjustments, responsive inspection, newer other-tab weight 4 and stale refusal. Stale refusal screenshot retained. After refresh and transport abort, read-only state still v3/catalog 1, but the transport screenshot showed the confirmation dialog gone and the subsequent Refresh catalog preview control timed out. Overall FAIL F-1: recovery dialog retention is unresolved. No partial upgrade occurred during that failed sequence. Screenshots and snapshots/errors retained; none supports whole-UC approval.

A later isolated reproduction clicked Preview/Confirm after the CLI invocation's route interception ended; that legitimately saved catalog 9/v4, so its timeout waiting for an absent failure message does not reproduce the original finding. Exact resulting state is failure-state.json; do not classify it as transport-failure non-mutation evidence. No operator data changed. Database retained.

Revision: bind dialog closure to its owned instance, bound modal dimensions/internal scrolling, strengthen runtime regression to assert failure controls remain visible after screenshot, then repeat the complete main journey on a fresh database. Preserve this attempt and its evidence. No approval inferred.
