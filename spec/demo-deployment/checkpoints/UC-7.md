# UC-7 checkpoint

Local implementation APPROVED, 2026-10-16. Public regression deferred by explicit user instruction.
Base d55ab636; starting dirty boundary in `output/execution/demo-deployment/remaining-20261016-1/`.
Production changes: RepairDraftService adds explicit per-resource staging removal; app.js binds Undo unavailability and demo JSON teacher-route wording. Existing accepted-value individual/bulk pin and conflict guards remain authoritative. No process is needed for staging.

`./mvnw -q -pl timetable-workspace -am verify -Dtest=DemoPlanningTest,DemoCustomDefinitionTest -Dsurefire.failIfNoSpecifiedTests=false -Dit.test=DemoRepairDraftBrowserIT -Dfailsafe.failIfNoSpecifiedTests=false -DargLine=-javaagent:<local Mockito jar>` PASS. [Recorded walkthrough](../../../output/playwright/demo-deployment/UC-7/local-20261016-1/ledger.md) covers exact Current, two-owner privacy, availability/direct effects, conflicting pin blocks solve, individual undo, bulk preview cancel/confirm/undo, reload, JSON handoff, availability undo and confirmed discard. All three observed kernel invocations are startup sample verification; zero visitor processes. Screenshot visually inspected. Later acceptance/provenance and file workflows are verified by their own UCs. No deployment or commit made.
