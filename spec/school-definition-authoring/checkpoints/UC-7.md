# UC-7 — Configure rooms and capabilities

- Base bce5872; pre-existing dirty files none. Requires approved UC-1. Contract: all five Room fields, identifier/form/lifecycle rules, supported invalid work and exact source/Current fidelity. No feature rules.md.
- Selected-room transactional version/state CAS; local origin/CSRF/access protections. Required capacity includes zero, capability array includes empty/open exact tokens, optional availability preserves omission versus explicit empty.
- Render existing tokens from configured room/lesson data, allow new valid tokens without inventing a central catalog. Save supported invalid work with blockers. No lesson requirements, class home rooms or policies implicitly changed.
- Recompute all affected class room/load/capability advisories and lesson home-room/policy/lock blockers after a room edit; hard locked rooms enforce capacity, capabilities and locked-period availability. Advisories remain distinct from definition blockers.
- Reuse immutable identity reservations, working retention, narrow saved-only presentation, three-way stale refresh and explicit conflict choices; Save/Discard/Stay applies to room navigation and selection.
- Destination handoffs: UC-13 guided removal and UC-14 source restoration, pending until their UCs run.
- Technical browser suite running in /tmp/uc7-verify.log. Standalone Playwright walkthrough and approval pending.

- Technical evidence: `./mvnw -pl timetable-workspace -am -Pworkspace-browser -Dit.test=DefinitionRoomsBrowserIT,DefinitionCohortsBrowserIT,DefinitionSubjectsBrowserIT,DefinitionTeachersBrowserIT -Dfailsafe.failIfNoSpecifiedTests=false verify` passes all 17 existing entity journeys and three/four room journeys. One room fixture lacked its asserted lab requirement; explicitly added lab and repeated all four room journeys with `-Dit.test=DefinitionRoomsBrowserIT`: BUILD SUCCESS, plus 26 unit/architecture. Logs /tmp/uc7-verify.log and /tmp/uc7-revision-verify.log preserve both attempts. All five fields, open tokens, invalid types/ranges, source/accepted fidelity, restart, responsive/unsaved/storage/transport/stale/identity/state/CSRF boundaries pass. Standalone walkthrough pending.

- Automatically APPROVED after [recorded standalone Playwright PASS](../../../output/playwright/school-definition-authoring/UC-7/20261011-1/README.md), submission 6622f19. Exact isolated v34 document and inspected screenshots retained, no unresolved finding.
