# UC-6 — Configure classes

- Base `ea2356c`, pre-existing dirty files none; approved UC-1 draft supplies the lifecycle boundary.
- All 12 Cohort fields: immutable identity and literal name, required size, optional availability/undesirability arrays, five versioned daily/start numeric settings, curator/home-room links. Default/value/no-limit choices preserve exact omission and explicit 2147483647. Catalog 5–9 default gap allowance shown as zero; older unavailable settings explain the required version.
- Selected-class atomic CAS saves supported invalid values with complete blockers and preserves source/Current, all other entities/lessons/policies, authoring context and reserved lineage.
- Shared dependent diagnostics now recompute affected lessons from exact saved entity values: ordinary qualification versus curator exception, curator/home-room capacity/capabilities, teacher/class availability locks, reserved-period permission, all matching room policy intersections and lock/home-room/hosting obligations. No dependent record is rewritten.
- Feasibility advisories are separate from blockers: taught class load versus eligible-period union upper bound (availability, reservation permission, locks), room-capacity shortage and unsupported required capability combinations. They do not refuse finish or claim complete feasibility.
- Working values survive resize/failure/stale refresh. Narrow saved-only inspection, Save/Discard/Stay and explicit conflict choices follow approved forms. Catalog/removal/restoration remain destination handoffs UC-12/13/14.
- Initial `/tmp/uc6-verify.log`: 26 unit/architecture and all 19 teacher/grid/school journeys pass; subject/class policy variants fail on strict optional boolean access. Fixed omission handling, preserving failed evidence. Full entity revision BUILD SUCCESS `/tmp/uc6-revision-verify.log`: 26 unit/architecture and all 17 class/subject/teacher journeys pass; prior 14 unchanged grid/school journeys pass. Typed-invalid link/array fidelity assertions added for a final focused class check `/tmp/uc6-types-verify.log`:

```sh
./mvnw -pl timetable-workspace -am -Pworkspace-browser '-Dit.test=DefinitionCohortsBrowserIT,DefinitionSubjectsBrowserIT,DefinitionTeachersBrowserIT,SchoolIdentityBrowserIT,DefinitionGridBrowserIT' -Dfailsafe.failIfNoSpecifiedTests=false verify
```

- Packaged compilation and syntax/diff checks pass. Six new class journeys cover all fields, range/types/catalog gates, curator/policies, advisories versus locks, exact initial/accepted source/Current, restart and identity/state/CSRF/storage/transport/stale/responsive boundaries. Runtime tests use isolated PostgreSQL/Chrome; operator data untouched.
- Standalone Playwright walkthrough pending; no UC-6 approval yet.

- Final corrected production check: `./mvnw -pl timetable-workspace -am -Pworkspace-browser -Dit.test=DefinitionCohortsBrowserIT,DefinitionSubjectsBrowserIT,DefinitionTeachersBrowserIT -Dfailsafe.failIfNoSpecifiedTests=false verify`; BUILD SUCCESS, 26 unit/architecture checks and all 17 entity journeys, `/tmp/uc6-final-verify.log`. Typed-invalid array homeRoomId initially exposed strict string coercion (`/tmp/uc6-types-verify.log`); fallback diagnostic reads now retain invalid JSON and report blockers. Standalone walkthrough is PENDING_WALKTHROUGH.

- APPROVED automatically after [standalone Playwright PASS](../../../output/playwright/school-definition-authoring/UC-6/20261011-1/README.md), submission 6ca5e84; exact v18 document and inspected screenshots retained. No open finding.
