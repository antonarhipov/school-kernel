package org.schoolkernel.workspace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.schoolkernel.workspace.WorkbenchFixtures.jsonStrings;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ConfigurableApplicationContext;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/** UC-3 of the workbench specs: generating a repair, following the run, failures, cancellation and recovery. */
class RepairRunBrowserIT extends WorkbenchBrowserSupport {
    private static final Duration SCALE = Duration.ofSeconds(20);
    private static final Set<String> DEMO_LESSONS = Set.of("lesson-math-1", "lesson-science-1");

    @Test
    @DisplayName("UC-7 main/7a/G1-G4/RULE-24: real browser accepts a protected teacher repair then a directly parented room repair")
    void generatesAndAcceptsSuccessiveRepairProposals() throws Exception {
        ObjectNode document = fixtures.validAcceptedDocument();
        JsonNode acceptedBefore = document.path("acceptedBaseline").deepCopy();
        storeAccepted(document);
        workbench.open().awaitText("Start a protected repair");
        workbench.startRepair(null, null, "mon-1");
        workbench.awaitText("Generate 1-minute repair proposal");
        workbench.pin("lesson-science-1", false, true);
        assertTrue(workbench.awaitText("Accepted room pinned").contains("Attempt-scoped pins\n1"));
        JsonNode frozenDraft = assertDraftUnchangedBaseline(acceptedBefore).deepCopy();
        workbench.click("#solve-draft");
        String solving = workbench.awaitText("Repair generation · running");
        assertTrue(solving.contains("Accepted baseline remains current"));
        assertTrue(solving.contains("Search lessons"));
        assertFalse(solving.contains("Apply selected pins"));
        workbench.expect("document.querySelector('#workbench-task-area #cancel-run') !== null && document.querySelector('#workbench-task-area #run-secondary')?.textContent.includes('Frozen repair intent') && !document.querySelector('#workbench-inspector #cancel-run')",
                "Workbench layout UC-3 main 2: active run state and cancellation belong in the wide task area");

        String proposal = workbench.awaitText("Repair proposal · feasible", Workbench.SOLVE);
        for (String expected : new String[] { "Accepted baseline remains current", "period stability, then room-only stability",
                "Execution limit\nPT1M", "Period moves", "Unique changed lessons", "Direct effects of your intent",
                "Solver ripple effects", "Additions\n0", "Cancellations\n0", "Old assignment", "Proposed assignment" }) {
            assertTrue(proposal.contains(expected), expected);
        }
        assertEquals("REPAIR_PROPOSAL", storedLifecycle());
        JsonNode proposedDocument = storedWorkspaceDocument();
        assertEquals(acceptedBefore, proposedDocument.path("acceptedBaseline"));
        assertEquals(frozenDraft, proposedDocument.path("repairDraft"));
        assertEquals("REPAIR", proposedDocument.path("proposal").path("kind").stringValue());
        assertEquals("FEASIBLE", proposedDocument.path("lastRun").path("status").stringValue());
        assertEquals(2, proposedDocument.path("proposal").path("result").path("timetable").path("assignments").size());
        assertFalse(proposedDocument.has("run"));
        assertEquals(DEMO_LESSONS, workbench.renderedLessonIds());
        workbench.expect("document.querySelector('[data-mode=PROPOSAL]')?.getAttribute('aria-pressed') === 'true' && !document.querySelector('[data-mode=SOLVING]') && document.querySelector('#lesson-panel-title')?.textContent === 'Science 1' && document.querySelector('#workbench-inspector .state.accepted')?.textContent.includes('Accepted assignment')");
        workbench.expect("document.querySelector('#workbench-inspector .before-after')?.textContent.includes('Old assignment') && document.querySelector('#workbench-inspector .before-after')?.textContent.includes('Proposed assignment') && document.querySelector('#accept-repair')?.disabled === true",
                "UC-4 main 1-5: accepted and proposed fields stay inspectable and acceptance remains gated");
        JsonNode beforeDecision = storedWorkspaceDocument();
        workbench.mode("CURRENT");
        workbench.mode("DRAFT");
        workbench.mode("PROPOSAL");
        assertEquals(beforeDecision, storedWorkspaceDocument(), "UC-4 G3: mode navigation must not change durable proposal or accepted baseline");
        workbench.expect("document.querySelector('[data-mode=PROPOSAL]')?.getAttribute('aria-pressed') === 'true'");
        jdbc.sql("UPDATE workspace_aggregate SET document = document #- '{proposal,review}' WHERE workspace_id=1").update();
        assertTrue(workbench.open().awaitText("Repair proposal · feasible").contains("Unique changed lessons"));
        workbench.selectLesson("lesson-science-1");
        assertTrue(workbench.awaitText("Old assignment").contains("Proposed assignment"));
        workbench.press("#confirm-repair-accept", "Space");
        String beforeFailedAccept = storedDocument();
        withRejectedWorkspaceUpdates("NEW.lifecycle_state = 'ACCEPTED_BASELINE'", () -> {
            workbench.click("#accept-repair");
            assertTrue(workbench.awaitText("Current did not advance").contains("Repair proposal · feasible"),
                    "UC-4 6b: failed durable acceptance cannot present Current");
            assertEquals(beforeFailedAccept, storedDocument(), "UC-4 6b: failed acceptance preserves entire accepted/draft/proposal document and version");
            workbench.expect("document.querySelector('[data-mode=PROPOSAL]')?.getAttribute('aria-pressed') === 'true' && !document.querySelector('#accept-repair').disabled");
        });
        workbench.press("#accept-repair", "Space");
        assertTrue(workbench.awaitText("Accepted baseline · current timetable").contains("Start a protected repair"));
        workbench.expect("document.querySelector('[data-mode=CURRENT]')?.getAttribute('aria-pressed') === 'true' && !document.querySelector('[data-mode=PROPOSAL], [data-mode=DRAFT]') && document.querySelector('[data-range=WEEK]')?.getAttribute('aria-pressed') === 'true'",
                "UC-4 success: only Current remains on the retained range after durable acceptance");

        JsonNode firstAccepted = storedWorkspaceDocument().path("acceptedBaseline").deepCopy();
        assertEquals(proposedDocument.path("proposal").path("definition"), firstAccepted.path("definition"));
        assertEquals(proposedDocument.path("proposal").path("result"), firstAccepted.path("result"));
        assertEquals("ACCEPTED_BASELINE", storedLifecycle());
        assertFalse(storedWorkspaceDocument().has("proposal"));
        assertFalse(storedWorkspaceDocument().has("repairDraft"));
        assertTrue(firstAccepted.path("manifest").path("locks").valueStream()
                .anyMatch(lock -> "lesson-science-1".equals(lock.path("lessonId").stringValue())
                        && "ATTEMPT_SCOPED".equals(lock.path("roomLockOrigin").stringValue())));

        Workbench later = newTab();
        later.open().awaitText("Accepted baseline · current timetable");
        later.startRepair("ROOM", "room-102", "mon-1");
        String roomDraft = later.awaitText("Repair draft · not current");
        assertTrue(roomDraft.contains("Accepted baseline remains current"));
        assertTrue(roomDraft.contains("Directly affected lessons\n0"));
        assertTrue(roomDraft.contains("Attempt-scoped pins\n0"));
        later.selectLesson("lesson-science-1");
        later.expect("!document.querySelector('#draft-selected-protection')?.textContent.includes('Policy room lock') && !document.querySelector('[data-lesson-id=lesson-science-1]')?.textContent.includes('Policy room lock')",
                "Workbench layout UC-2 G4: an accepted prior attempt-scoped lock is not mislabeled as persistent policy");
        later.click("#solve-draft");
        String secondProposal = later.awaitText("Repair proposal · feasible", Workbench.SOLVE);
        assertTrue(secondProposal.contains("Accepted baseline remains current"));
        assertTrue(secondProposal.contains("Direct effects of your intent"));
        assertTrue(secondProposal.contains("Solver ripple effects"));
        later.acceptRepair();
        assertTrue(later.awaitText("Accepted baseline · current timetable").contains("Start a protected repair"));

        JsonNode stored = storedWorkspaceDocument();
        assertEquals("ACCEPTED_BASELINE", storedLifecycle());
        assertFalse(acceptedBefore.equals(stored.path("acceptedBaseline")));
        assertEquals("FEASIBLE", stored.path("acceptedBaseline").path("result").path("status").stringValue());
        assertTrue(stored.path("acceptedBaseline").path("manifest").path("locks").isEmpty());
        assertFalse(stored.has("proposal"));
        assertFalse(stored.has("repairDraft"));
    }

    @Test
    @DisplayName("Timetable polish UC-3 main 1-4/2a/2b/G1-G8: frozen run stays inspectable and cancellation or recovery restores the exact draft")
    void inspectsFrozenRepairRunAndRecoversWithoutPublishing() throws Exception {
        ObjectNode accepted = fixtures.validAcceptedDocument();
        storeAccepted(accepted);
        JsonNode baseline = accepted.path("acceptedBaseline").deepCopy();
        workbench.open().awaitText("Start a protected repair");
        workbench.startRepair(null, null, "mon-1");
        workbench.awaitText("Repair draft · not current");
        workbench.pin("lesson-science-1", false, true);
        workbench.awaitText("Attempt-scoped pins\n1");
        workbench.range("DAY");
        workbench.filterClass("cohort-7a");
        JsonNode draft = assertDraftUnchangedBaseline(baseline).deepCopy();
        assertEquals("lesson-science-1", draft.path("intent").path("pins").get(0).path("lessonId").stringValue());

        processes.blockReplan = true;
        workbench.click("#solve-draft");
        processes.awaitBlocked();
        workbench.awaitText("Repair generation · running");
        assertEquals("SOLVING_REPAIR", storedLifecycle());
        JsonNode running = storedWorkspaceDocument();
        String runId = running.path("run").path("id").stringValue();
        assertEquals("PT1M", running.path("run").path("limit").stringValue());
        assertEquals(baseline, running.path("acceptedBaseline"));
        assertEquals(draft, running.path("repairDraft"));
        assertFalse(running.has("proposal"));
        workbench.expect("document.querySelector('#workbench-task-area #cancel-run')?.textContent === 'Cancel run' && document.querySelector('#workbench-task-area')?.textContent.includes('PT1M') && !document.querySelector('#workbench-inspector #cancel-run') && document.querySelector('#cohort-filter')?.value === 'cohort-7a' && document.querySelector('[data-range=DAY]')?.getAttribute('aria-pressed') === 'true' && document.querySelector('#lesson-panel-title')?.textContent === 'Science 1' && !document.querySelector('#apply-pin')");
        workbench.page().evaluate("void (window.__uc3RunCanvas = document.querySelector('[data-lesson-id=lesson-science-1]'))");
        workbench.page().waitForTimeout(800);
        workbench.expect("window.__uc3RunCanvas === document.querySelector('[data-lesson-id=lesson-science-1]')",
                "unchanged run polling must not replace the selected lesson or steal focus");

        workbench.mode("DRAFT");
        workbench.awaitText("Frozen repair intent · not current");
        workbench.expect("document.querySelector('#workbench-task-area #run-secondary')?.textContent.includes('Alex') && document.querySelector('[data-lesson-id=lesson-science-1]')?.textContent.includes('Accepted room pinned') && !document.querySelector('#apply-pin') && !document.querySelector('#solve-draft') && !document.querySelector('#stage-repair-form')");
        workbench.mode("CURRENT");
        workbench.awaitText("Current · accepted");
        workbench.expect("!document.querySelector('#apply-pin') && document.querySelector('#cancel-run') !== null && document.querySelector('#cohort-filter')?.value === 'cohort-7a'");
        workbench.mode("SOLVING");
        workbench.filterTeacher("teacher-alex");
        workbench.awaitText("Lens: Teacher · Alex");
        workbench.expect("document.querySelector('#workbench-task-area #cancel-run') !== null");
        workbench.removeLens();
        workbench.awaitText("Day · Monday");

        workbench.viewport(390, 800);
        workbench.expect("document.querySelector('.focused-schedule') !== null && document.querySelector('#workbench-modes') === null && document.querySelector('#cancel-run') === null");
        assertEquals(running, storedWorkspaceDocument(), "narrow inspection must not mutate the active run");
        workbench.viewport(1280, 800);
        workbench.expect("document.querySelector('#workbench-task-area #cancel-run') !== null");
        assertEquals(runId, storedWorkspaceDocument().path("run").path("id").stringValue());
        workbench.click("#cancel-run");
        workbench.awaitText("Repair draft · not current");
        assertEquals("REPAIR_DRAFT", storedLifecycle());
        assertEquals(draft, assertDraftUnchangedBaseline(baseline));
        assertEquals("CANCELLED", storedWorkspaceDocument().path("lastRun").path("status").stringValue());
        workbench.expect("document.querySelector('#solve-draft') !== null && document.querySelector('#cancel-run') === null");

        processes.reset();
        processes.blockReplan = true;
        workbench.click("#solve-draft");
        processes.awaitBlocked();
        workbench.awaitText("Repair generation · running");
        recovery.recoverInterruptedRun();
        assertEquals("REPAIR_DRAFT", storedLifecycle());
        workbench.reload();
        workbench.awaitText("Repair draft · not current");
        assertEquals(draft, assertDraftUnchangedBaseline(baseline));
        assertEquals("INTERRUPTED", storedWorkspaceDocument().path("lastRun").path("code").stringValue());
        workbench.expect("document.querySelector('#workbench-task-area .conflict-list')?.textContent.includes('Repair generation was interrupted.') && document.querySelector('#utilities')?.textContent.includes('Repair generation was interrupted.') && !document.querySelector('#utilities').open");
        workbench.expect("document.querySelector('#solve-draft') !== null && !document.querySelector('#cancel-run')");
        processes.reset();
        assertEquals(draft, assertDraftUnchangedBaseline(baseline));
    }

    @Test
    @DisplayName("Timetable polish UC-3 extensions 5a/5b/6a/G1-G6: run failures expose only safe Utilities evidence and unchanged retry is bounded")
    void showsFailedRepairEvidenceAndGatedRetry() throws Exception {
        ObjectNode accepted = fixtures.validAcceptedDocument();
        storeAccepted(accepted);
        JsonNode baseline = accepted.path("acceptedBaseline").deepCopy();
        workbench.open().awaitText("Start a protected repair");
        workbench.startRepair(null, null, "mon-1");
        workbench.awaitText("Repair draft · not current");
        JsonNode draft = assertDraftUnchangedBaseline(baseline).deepCopy();
        processes.failure = WorkspaceRepairPlanningIT.RepairFailure.NO_FEASIBLE;
        workbench.click("#solve-draft");
        workbench.awaitText("Retry unchanged draft for two minutes");
        assertEquals(draft, assertDraftUnchangedBaseline(baseline));
        workbench.expect("document.querySelector('#utilities')?.open && !document.querySelector('#workbench-inspector').hidden && document.querySelector('#workbench-task-area .conflict-list')?.textContent.includes('hard.teacher-period') && document.querySelector('#utilities')?.textContent.includes('2 matches')");
        workbench.click("#utilities [data-diagnostic-id=lesson-math-1]");
        workbench.expect("document.querySelector('#lesson-panel-title')?.textContent === 'Mathematics 1'");
        workbench.mode("CURRENT");
        workbench.expect("document.querySelector('#utilities')?.textContent.includes('hard.teacher-period') && document.querySelector('#solve-draft') === null");
        workbench.mode("DRAFT");
        processes.failure = null;
        processes.blockReplan = true;
        workbench.click("#retry-repair");
        processes.awaitBlocked();
        workbench.awaitText("Repair generation · running");
        assertEquals("PT2M", storedWorkspaceDocument().path("run").path("limit").stringValue());
        workbench.click("#cancel-run");
        workbench.awaitText("Repair draft · not current");
        assertEquals(draft, assertDraftUnchangedBaseline(baseline));
        workbench.expect("document.querySelector('#retry-repair') === null");

        for (var failure : new WorkspaceRepairPlanningIT.RepairFailure[] {
                WorkspaceRepairPlanningIT.RepairFailure.INVALID_INPUT,
                WorkspaceRepairPlanningIT.RepairFailure.TRANSPORT,
                WorkspaceRepairPlanningIT.RepairFailure.INTERRUPTED,
                WorkspaceRepairPlanningIT.RepairFailure.MISMATCHED,
                WorkspaceRepairPlanningIT.RepairFailure.WATCHDOG }) {
            String message = switch (failure) {
                case INVALID_INPUT -> "School Kernel rejected the repair definition.";
                case TRANSPORT -> "School Kernel could not be started.";
                case INTERRUPTED -> "Repair generation was interrupted.";
                case MISMATCHED -> "School Kernel returned an unverified repair result.";
                case WATCHDOG -> "Repair generation did not finish within its bounded run.";
                default -> throw new IllegalStateException();
            };
            processes.reset();
            processes.failure = failure;
            workbench.click("#solve-draft");
            workbench.awaitText(message);
            assertEquals(draft, assertDraftUnchangedBaseline(baseline));
            workbench.expect("document.querySelector('#utilities')?.textContent.includes('" + message + "') && !document.body.innerText.includes('secret raw') && !document.querySelector('#retry-repair')",
                    failure + " exposes only its safe message");
        }
    }

    @Test
    @DisplayName("Timetable polish UC-3 ext 2b/G7/RULE-11: stop the application mid-run and restart on the same durable school")
    void restoresInterruptedRepairAfterActualApplicationRestart() throws Exception {
        ObjectNode accepted = fixtures.validAcceptedDocument();
        storeAccepted(accepted);
        JsonNode baseline = accepted.path("acceptedBaseline").deepCopy();
        ConfigurableApplicationContext first = startRestartableWorkspace();
        ConfigurableApplicationContext restarted = null;
        WorkspaceRepairPlanningIT.SwitchingRepairProcessLauncher blocked = first.getBean(
                WorkspaceRepairPlanningIT.SwitchingRepairProcessLauncher.class);
        try {
            workbench.open(first.getEnvironment().getProperty("local.server.port", Integer.class))
                    .awaitText("Start a protected repair");
            workbench.startRepair(null, null, "mon-1");
            workbench.awaitText("Repair draft · not current");
            JsonNode draft = assertDraftUnchangedBaseline(baseline).deepCopy();
            blocked.blockReplan = true;
            workbench.click("#solve-draft");
            blocked.awaitBlocked();
            workbench.awaitText("Repair generation · running");
            assertEquals("SOLVING_REPAIR", storedLifecycle());
            JsonNode running = storedWorkspaceDocument();
            String originalRunId = running.path("run").path("id").stringValue();
            long runningVersion = storedVersion();
            assertEquals(baseline, running.path("acceptedBaseline"));
            assertEquals(draft, running.path("repairDraft"));
            assertFalse(running.has("proposal"));

            workbench.navigate("about:blank");
            first.close();
            assertFalse(first.isActive(), "the application that started the repair is stopped before recovery");
            restarted = startRestartableWorkspace();
            workbench.open(restarted.getEnvironment().getProperty("local.server.port", Integer.class))
                    .awaitText("Repair draft · not current", SCALE);
            assertEquals("REPAIR_DRAFT", storedLifecycle());
            assertEquals(runningVersion + 1, storedVersion());
            assertEquals(draft, assertDraftUnchangedBaseline(baseline));
            JsonNode recovered = storedWorkspaceDocument();
            assertEquals(originalRunId, recovered.path("lastRun").path("id").stringValue());
            assertEquals("INTERRUPTED", recovered.path("lastRun").path("code").stringValue());
            assertEquals("FAILED", recovered.path("lastRun").path("status").stringValue());
            workbench.expect("document.querySelector('[data-mode=DRAFT]')?.getAttribute('aria-pressed') === 'true' && !document.querySelector('[data-mode=PROPOSAL]') && !document.querySelector('#cancel-run') && document.querySelector('#utilities')?.textContent.includes('Repair generation was interrupted.') && document.querySelector('#solve-draft') !== null");
            blocked.reset();
            workbench.page().waitForTimeout(300);
            assertEquals(recovered, storedWorkspaceDocument(), "the old process must not publish after a fresh instance recovers its run");
        } finally {
            blocked.reset();
            if (first.isActive()) first.close();
            if (restarted != null) restarted.close();
        }
    }

    @Test
    @DisplayName("Workbench layout UC-3 main/2a/6a/G1-G7: normative browser retains Current during wide Solving, cancellation, rejection and verified handoff")
    void followsAndRefusesWholeSchoolRepairOnVerifiedNormativeSnapshot() throws Exception {
        ObjectNode document = fixtures.investigationScaleDocument();
        JsonNode baseline = document.path("acceptedBaseline").deepCopy();
        JsonNode definition = baseline.path("definition");
        JsonNode assignments = baseline.path("result").path("timetable").path("assignments");
        KernelVerifier.Verification verified = verifier.verify(new ImportDocuments(definition,
                baseline.path("result"), null, ImportDocuments.ImportMode.ACCEPTED_BASELINE));
        assertEquals(document.path("definitionRevision").stringValue(), verified.definitionRevision());
        assertEquals(document.path("timetableRevision").stringValue(), verified.timetableRevision());
        assertEquals(60, definition.path("cohorts").size());
        assertEquals(100, definition.path("teachers").size());
        assertEquals(100, definition.path("rooms").size());
        assertEquals(1_000, assignments.size());
        assertEquals(60, definition.path("periods").size());
        assertEquals("subject-0", assignments.get(960).path("subjectId").stringValue());
        assertEquals("teacher-16", assignments.get(960).path("teacherId").stringValue());
        assertEquals("cohort-16", assignments.get(960).path("cohortId").stringValue());
        assertEquals("period-0", assignments.get(960).path("periodId").stringValue());
        assertEquals("room-16", assignments.get(960).path("roomId").stringValue());
        Set<String> expectedIds = new HashSet<>();
        Set<String> expectedMondayIds = new HashSet<>();
        for (JsonNode assignment : assignments) {
            expectedIds.add(assignment.path("lessonId").stringValue());
            int periodOrdinal = Integer.parseInt(assignment.path("periodId").stringValue().substring("period-".length()));
            if (periodOrdinal < 12) expectedMondayIds.add(assignment.path("lessonId").stringValue());
        }
        assertEquals(1_000, expectedIds.size());
        storeAccepted(document);

        workbench.open().awaitText("Showing 60 of 60 classes", SCALE);
        assertEquals(expectedIds, workbench.renderedLessonIds());
        workbench.startRepair(null, "teacher-16", "period-0");
        workbench.awaitText("Repair draft · not current", SCALE);
        JsonNode draft = assertDraftUnchangedBaseline(baseline).deepCopy();
        assertEquals("teacher-16", draft.path("intent").path("changes").get(0).path("resourceId").stringValue());
        assertEquals(JSON.readTree("[\"period-0\"]"), draft.path("intent").path("changes").get(0).path("unavailablePeriodIds"));
        assertTrue(jsonStrings(draft.path("directEffectLessonIds")).contains("lesson-960"));
        assertEquals(expectedIds, workbench.renderedLessonIds());
        workbench.pin("lesson-500", false, true);
        workbench.awaitText("Accepted room pinned");
        draft = assertDraftUnchangedBaseline(baseline).deepCopy();
        assertEquals("lesson-500", draft.path("intent").path("pins").get(0).path("lessonId").stringValue());
        workbench.selectLesson("lesson-960");
        workbench.range("DAY");
        workbench.awaitText("Day · Monday");
        assertEquals(expectedMondayIds, workbench.renderedLessonIds());
        workbench.expect("document.querySelector('#lesson-panel-title')?.textContent === 'Declared lesson 960' && document.querySelector('[data-lesson-id=lesson-960]')?.textContent.includes('Directly affected')");

        processes.blockReplan = true;
        workbench.click("#solve-draft");
        processes.awaitBlocked();
        workbench.awaitText("Repair generation · running", SCALE);
        JsonNode running = storedWorkspaceDocument();
        assertEquals("SOLVING_REPAIR", storedLifecycle());
        assertEquals(baseline, running.path("acceptedBaseline"));
        assertEquals(draft, running.path("repairDraft"));
        assertFalse(running.has("proposal"));
        assertEquals("PT1M", running.path("run").path("limit").stringValue());
        assertEquals(expectedMondayIds, workbench.renderedLessonIds());
        workbench.expect("document.querySelector('#workbench-task-area #cancel-run') && !document.querySelector('#workbench-inspector #cancel-run, #workbench-inspector #stage-repair-form, #workbench-inspector #proposal-context') && document.querySelector('#workbench-task-area #run-secondary')?.textContent.includes('Teacher Sixteen with a deliberately long authoritative display name for timetable tiles') && document.querySelector('#workbench-task-area #run-secondary')?.textContent.includes('Declared period 0') && document.querySelector('#workbench-task-area #run-secondary')?.textContent.includes('lesson-500') && !document.querySelector('#apply-pin, #discard-draft, #accept-repair')",
                "UC-3 main 2/G1: frozen intent, status and cancellation are in the wide task area only");
        Workbench.MutationLog mutations = workbench.recordMutations();
        workbench.range("WEEK");
        assertEquals(expectedIds, workbench.renderedLessonIds());
        assertSolvingTaskGeometry(1600, 900);
        workbench.screenshot("uc3-solving-1600.png");
        workbench.click("#toggle-run-detail");
        workbench.expect("document.querySelector('#run-secondary').hidden && document.querySelector('#cancel-run')?.getBoundingClientRect().bottom <= innerHeight && document.querySelector('#workbench-task-area')?.textContent.includes('PT1M')",
                "UC-3 G2: collapsed secondary detail cannot hide status, limit or cancellation");
        workbench.click("#toggle-run-detail");
        workbench.viewport(1280, 800);
        assertSolvingTaskGeometry(1280, 800);
        workbench.screenshot("uc3-solving-1280.png");
        workbench.collapseInspector();
        workbench.expect("document.querySelector('#workbench-inspector').hidden && document.querySelector('#inspector-summary')?.textContent.includes('Declared lesson 960') && document.querySelector('.canvas-region').getBoundingClientRect().width > 1000",
                "UC-3 G3: inspector collapse retains selection and widens the running canvas");
        workbench.reopenInspector();
        workbench.expect("!document.querySelector('#workbench-inspector').hidden && document.querySelector('#lesson-panel-title')?.textContent === 'Declared lesson 960'");
        workbench.viewport(1279, 800);
        workbench.expect("document.querySelector('#workbench-inspector').getBoundingClientRect().top >= document.querySelector('.canvas-region').getBoundingClientRect().bottom && document.querySelector('#workbench-task-area').getBoundingClientRect().top >= document.querySelector('#workbench-inspector').getBoundingClientRect().bottom && document.documentElement.scrollWidth <= innerWidth",
                "UC-3 G2/RULE-5: 1279px inspector stacks between canvas and task area");
        workbench.viewport(701, 844);
        workbench.expect("document.querySelector('#workbench-inspector').getBoundingClientRect().top >= document.querySelector('.canvas-region').getBoundingClientRect().bottom && document.querySelector('#workbench-task-area #cancel-run') && document.documentElement.scrollWidth <= innerWidth",
                "UC-3 G2/RULE-5: the last editing width retains a wide stacked task area");
        workbench.viewport(700, 844);
        workbench.expect("document.querySelector('.focused-schedule') !== null && document.querySelector('#workbench-modes') === null && document.querySelector('#cancel-run, #toggle-run-detail, #workbench-task-area') === null");
        workbench.expect("document.body.innerText.includes('Repair generation · running') && document.body.innerText.includes('Accepted baseline remains current') && document.documentElement.scrollWidth <= innerWidth",
                "UC-3 G6/RULE-12: the 700px agenda names the true lifecycle and is read-only");
        assertEquals(running, storedWorkspaceDocument(), "UC-3 G6: responsive inspection cannot mutate the run");
        workbench.viewport(390, 844);
        workbench.expect("document.querySelector('.focused-schedule') !== null && document.querySelector('#cancel-run, #toggle-run-detail, #workbench-task-area') === null && document.documentElement.scrollWidth <= innerWidth",
                "UC-3 G6/G7: normative 390px agenda remains read-only without page overflow");
        workbench.screenshot("uc3-solving-390.png");
        workbench.viewport(1280, 800);
        workbench.expect("document.querySelector('#workbench-task-area #cancel-run') !== null");
        workbench.range("DAY");
        assertEquals(expectedMondayIds, workbench.renderedLessonIds());
        workbench.mode("CURRENT");
        workbench.expect("document.querySelector('#lesson-panel-title')?.textContent === 'Declared lesson 960' && document.querySelector('[data-range=DAY]')?.getAttribute('aria-pressed') === 'true' && document.querySelector('#workbench-task-area #cancel-run') && document.querySelector('#run-secondary').hidden");
        workbench.mode("DRAFT");
        workbench.expect("document.querySelector('#workbench-task-area #run-secondary')?.textContent.includes('Frozen repair intent') && !document.querySelector('#solve-draft')");
        workbench.mode("SOLVING");
        workbench.weekday("TUESDAY");
        workbench.expect("document.querySelector('#range-summary')?.textContent.includes('Tuesday') && !document.querySelector('#lesson-panel-title') && document.querySelector('#inspection-notice')?.textContent.includes('outside the represented Day') && document.querySelector('#workbench-task-area #cancel-run')",
                "UC-3 extension 3a: an unrepresentable selected lesson clears with an explanation while the run stays visible");
        assertEquals(running, storedWorkspaceDocument(), "UC-3 extension 3a: clearing selection cannot change the running workspace");
        workbench.weekday("MONDAY");
        workbench.selectLesson("lesson-960");
        workbench.expect("document.querySelector('#lesson-panel-title')?.textContent === 'Declared lesson 960'");
        assertEquals(running, storedWorkspaceDocument());
        assertEquals(List.of(), mutations.requests(),
                "UC-3 G3/RULE-2: range, inspector, task-detail, mode, and responsive actions issue no mutating request");
        assertEquals(1, replanCommands(), "UC-3 G3: presentation actions cannot restart the scheduler");
        workbench.click("#cancel-run");
        workbench.awaitText("Repair draft · not current", SCALE);
        assertEquals(draft, assertDraftUnchangedBaseline(baseline));
        assertEquals("CANCELLED", storedWorkspaceDocument().path("lastRun").path("status").stringValue());

        processes.reset();
        processes.failure = WorkspaceRepairPlanningIT.RepairFailure.MISMATCHED;
        workbench.click("#solve-draft");
        workbench.awaitText("School Kernel returned an unverified repair result.", SCALE);
        assertEquals(draft, assertDraftUnchangedBaseline(baseline));
        assertEquals("FAILED", storedWorkspaceDocument().path("lastRun").path("status").stringValue());
        workbench.expect("document.querySelector('#workbench-task-area .conflict-list')?.textContent.includes('unverified repair result') && document.querySelector('#utilities')?.textContent.includes('unverified repair result') && !document.querySelector('[data-mode=PROPOSAL]')");
        assertEquals(expectedMondayIds, workbench.renderedLessonIds());

        processes.reset();
        processes.verifiedFeasibleResult = fixtures::verifiedNormativeRepairResult;
        workbench.click("#solve-draft");
        workbench.awaitText("Repair proposal · feasible", Duration.ofSeconds(30));
        JsonNode proposed = storedWorkspaceDocument();
        assertEquals("REPAIR_PROPOSAL", storedLifecycle());
        assertEquals(baseline, proposed.path("acceptedBaseline"));
        assertEquals(draft, proposed.path("repairDraft"));
        assertFalse(proposed.has("run"));
        assertEquals("FEASIBLE", proposed.path("lastRun").path("status").stringValue());
        assertEquals("period-40", proposed.path("proposal").path("result").path("timetable").path("assignments").get(960).path("periodId").stringValue());
        workbench.expect("document.querySelector('[data-mode=PROPOSAL]')?.getAttribute('aria-pressed') === 'true' && !document.querySelector('[data-mode=SOLVING]') && !document.querySelector('#workbench-task-area').hidden && document.querySelector('#workbench-task-area #proposal-context') && !document.querySelector('#workbench-inspector #proposal-context, #workbench-inspector #accept-repair') && document.querySelector('#lesson-panel-title')?.textContent === 'Declared lesson 960' && document.querySelector('#cohort-filter')?.value === ''",
                "UC-3 main 6: independently verified Proposal opens its wide review area and retains the representable accepted context");
        workbench.screenshot("uc3-proposal-handoff-1280.png");
        workbench.scrollToTop();
        JsonNode handoff = workbench.value("""
                (() => { const task=document.querySelector('#workbench-task-area');
                  const canvas=document.querySelector('.canvas-region'); const wrap=document.querySelector('.matrix-wrap');
                  const inspector=document.querySelector('#workbench-inspector');
                  task.scrollTop=task.scrollHeight;
                  const bounds=task.getBoundingClientRect();
                  return {taskHeight:bounds.height, taskBottom:bounds.bottom, taskTop:bounds.top,
                    canvasBottom:canvas.getBoundingClientRect().bottom,
                    inspectorLeft:inspector.getBoundingClientRect().left,
                    canvasRight:canvas.getBoundingClientRect().right,
                    visible:wrap.clientHeight, heading:document.querySelector('.matrix thead').getBoundingClientRect().height,
                    row:document.querySelector('.matrix tbody tr').getBoundingClientRect().height,
                    page:document.documentElement.scrollWidth,
                    decisionVisible:['#accept-repair','#revise-proposal','#discard-proposal'].every(selector => {
                      const action=document.querySelector(selector).getBoundingClientRect();
                      return action.top >= bounds.top && action.bottom <= bounds.bottom;
                    })}; })()""");
        assertTrue(handoff.path("taskHeight").doubleValue() <= 800 * .35
                        && handoff.path("taskBottom").doubleValue() <= 800
                        && handoff.path("taskTop").doubleValue() >= handoff.path("canvasBottom").doubleValue()
                        && handoff.path("inspectorLeft").doubleValue() >= handoff.path("canvasRight").doubleValue()
                        && handoff.path("visible").doubleValue() >= handoff.path("heading").doubleValue() + handoff.path("row").doubleValue()
                        && handoff.path("page").doubleValue() <= 1281
                        && handoff.path("decisionVisible").booleanValue(),
                "UC-3 main 6/RULE-5: verified handoff keeps the review task and its decisions reachable below the canvas: " + handoff);
    }

    /** UC-3 G2: the running task area stays compact below the canvas, with cancellation visible and unclipped. */
    private void assertSolvingTaskGeometry(int width, int height) {
        workbench.scrollToTop();
        JsonNode geometry = workbench.value("""
                (() => { const task=document.querySelector('#workbench-task-area');
                  const canvas=document.querySelector('.canvas-region'); const wrap=document.querySelector('.matrix-wrap');
                  const inspector=document.querySelector('#workbench-inspector');
                  const cancel=document.querySelector('#cancel-run').getBoundingClientRect();
                  return {taskHeight:task.getBoundingClientRect().height, taskTop:task.getBoundingClientRect().top,
                    taskBottom:task.getBoundingClientRect().bottom, canvasBottom:canvas.getBoundingClientRect().bottom,
                    inspectorLeft:inspector.getBoundingClientRect().left, canvasRight:canvas.getBoundingClientRect().right,
                    row:document.querySelector('.week-matrix tbody tr').getBoundingClientRect().height,
                    heading:document.querySelector('.week-matrix thead').getBoundingClientRect().height,
                    visible:wrap.clientHeight, page:document.documentElement.scrollWidth,
                    cancelBottom:cancel.bottom, cancelRight:cancel.right}; })()""");
        assertTrue(geometry.path("taskHeight").doubleValue() <= height * .35, "UC-3 G2: task area is at most 35% of the viewport: " + geometry);
        assertTrue(geometry.path("taskBottom").doubleValue() <= height, "UC-3 G2: task remains visible with the canvas: " + geometry);
        assertTrue(geometry.path("taskTop").doubleValue() >= geometry.path("canvasBottom").doubleValue(), "UC-3 G2: task never overlays timetable cells: " + geometry);
        assertTrue(geometry.path("inspectorLeft").doubleValue() >= geometry.path("canvasRight").doubleValue(), "UC-3 G2: inspector remains beside the canvas: " + geometry);
        assertTrue(geometry.path("visible").doubleValue() >= geometry.path("heading").doubleValue() + geometry.path("row").doubleValue(),
                "UC-3 G2: a complete class row and time heading are visible: " + geometry);
        assertTrue(geometry.path("page").doubleValue() <= width + 1 && geometry.path("cancelRight").doubleValue() <= width + 1
                        && geometry.path("cancelBottom").doubleValue() <= height,
                "UC-3 G2: no page overflow or clipped cancellation: " + geometry);
    }
}
