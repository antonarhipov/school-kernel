package org.schoolkernel.workspace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.schoolkernel.workspace.WorkbenchFixtures.jsonStrings;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/** UC-2 of the workbench specs: preparing, protecting, persisting and discarding a repair Draft. */
class RepairDraftBrowserIT extends WorkbenchBrowserSupport {
    private static final String DRAFT_ONLY = "NEW.lifecycle_state = 'REPAIR_DRAFT'";
    private static final Set<String> DEMO_LESSONS = Set.of("lesson-math-1", "lesson-science-1");

    @Test
    @DisplayName("Workbench layout UC-2 main/1a/1b/2a/3a/4a/4b/5a/6a/6b/G1-G7: normative browser prepares and discards a protected wide Draft")
    void preparesWideProtectedDraftAtNormativeScale() throws Exception {
        ObjectNode document = fixtures.investigationScaleDocument();
        storeAccepted(document);
        JsonNode baseline = document.path("acceptedBaseline").deepCopy();
        workbench.open();
        workbench.expect("document.querySelector('[data-lesson-id=lesson-960]') !== null");
        assertEquals(1_000, workbench.renderedLessonIds().size(), "UC-2 Requires UC-1's complete accepted canvas");
        String acceptedBefore = storedDocument();

        workbench.selectLesson("lesson-960");
        workbench.click("#open-repair-setup");
        workbench.expect("!document.querySelector('#workbench-task-area').hidden && document.querySelector('#repair-resource').value === 'teacher-16' && !document.querySelector('#workbench-inspector #start-repair-form')");
        assertEquals(acceptedBefore, storedDocument(), "UC-2 main 1: opening setup is presentation-only");
        workbench.click("#close-repair-setup");
        assertEquals(acceptedBefore, storedDocument(), "UC-2 ext 1a: closing unstaged setup saves nothing");
        workbench.openRepairSetup();
        workbench.submitRepair();
        workbench.awaitText("Select one or more weekly periods");
        assertEquals(acceptedBefore, storedDocument(), "UC-2 ext 2a: invalid period selection creates no Draft");
        workbench.checkRepairPeriod("period-0");
        workbench.submitRepair();
        workbench.awaitText("Draft is durably saved with no blocking conflict");
        JsonNode initial = assertDraftUnchangedBaseline(baseline);
        JsonNode change = initial.path("intent").path("changes").get(0);
        assertEquals("TEACHER", change.path("resourceType").stringValue());
        assertEquals("teacher-16", change.path("resourceId").stringValue());
        assertEquals(JSON.readTree("[\"period-0\"]"), change.path("unavailablePeriodIds"));
        assertEquals(Set.of("lesson-960"), jsonStrings(initial.path("directEffectLessonIds")));
        assertEquals(1_000, workbench.renderedLessonIds().size(), "Draft decorates, rather than replaces, accepted assignments");
        workbench.expect("document.querySelector('[data-lesson-id=lesson-960]').textContent.includes('Directly affected') && document.querySelector('#workbench-task-area #stage-repair-form') && document.querySelector('#workbench-task-area #apply-pin') && document.querySelector('#workbench-task-area #preview-bulk') && document.querySelector('#workbench-task-area #solve-draft') && !document.querySelector('#workbench-inspector #stage-repair-form, #workbench-inspector #apply-pin, #workbench-inspector #preview-bulk')",
                "UC-2 main 3/G2: accepted canvas cues and all Draft work belong to the wide task area");
        assertDraftTaskGeometry(1600, 900);
        workbench.screenshot("uc2-draft-1600.png");

        workbench.click("#apply-pin");
        workbench.awaitText("Resolve blocking conflicts before solving");
        JsonNode conflicted = assertDraftUnchangedBaseline(baseline);
        assertEquals("lesson-960", conflicted.path("conflicts").get(0).path("lessonId").stringValue());
        assertEquals("PIN_CONTRADICTS_UNAVAILABILITY", conflicted.path("conflicts").get(0).path("code").stringValue());
        assertFalse(conflicted.path("readyToSolve").booleanValue());
        workbench.expect("document.querySelector('#solve-draft').disabled && document.querySelector('[data-lesson-id=lesson-960]').textContent.includes('Blocking conflict')");
        String beforeNavigation = storedDocument();
        workbench.range("DAY");
        workbench.weekday("TUESDAY");
        workbench.filterRoom("room-99");
        workbench.search("Sixteen");
        workbench.investigateTeacher("teacher-16");
        workbench.click("[data-draft-conflict=lesson-960]");
        workbench.expect("document.querySelector('#weekday').value === 'MONDAY' && document.querySelector('#room-filter').value === '' && document.querySelector('#lesson-search').value === 'Sixteen' && document.querySelector('#teacher-investigation').value === 'teacher-16' && document.querySelector('[data-lesson-id=lesson-960]').getAttribute('aria-pressed') === 'true' && document.querySelector('#inspection-notice').textContent.includes('view was adjusted')",
                "UC-2 ext 4b: navigation changes only excluding context and keeps investigation");
        assertEquals(beforeNavigation, storedDocument(), "navigation cannot alter the exact durable Draft");
        // Reaching the room filter opened Filters; with it open, the 1280x800 task area no longer fits (G2).
        workbench.closeDisclosure("#filters");
        workbench.click("#remove-pin");
        workbench.awaitText("Draft is durably saved with no blocking conflict");
        assertTrue(assertDraftUnchangedBaseline(baseline).path("conflicts").isEmpty());

        workbench.range("WEEK");
        workbench.pin("lesson-500", false, true);
        workbench.awaitText("Accepted room pinned");
        JsonNode individual = assertDraftUnchangedBaseline(baseline);
        assertEquals("lesson-500", individual.path("intent").path("pins").get(0).path("lessonId").stringValue());
        assertEquals(JSON.readTree("[\"INDIVIDUAL\"]"), individual.path("intent").path("pins").get(0).path("roomSources"));
        Set<String> classEightLessons = new HashSet<>();
        for (int number = 480; number < 540; number++) classEightLessons.add("lesson-" + number);
        String beforePreview = storedDocument();
        workbench.previewBulk("CLASS", "cohort-8");
        workbench.awaitText("60 lessons in this immutable snapshot");
        assertEquals(beforePreview, storedDocument(), "UC-2 G5: preview has no durable effect");
        workbench.expect("Array.from(document.querySelectorAll('#bulk-preview-host li')).length === 60 && Array.from(document.querySelectorAll('#bulk-preview-host li')).every((item, index) => item.textContent.includes('lesson-' + (480 + index))) && document.querySelector('#bulk-preview-host').textContent.includes('Accepted period')",
                "UC-2 main 5: the browser preview names every expected class-eight lesson in order");
        workbench.click("#cancel-bulk");
        assertEquals(beforePreview, storedDocument(), "UC-2 ext 5a: cancel retains exact prior Draft");
        workbench.previewBulk("CLASS", "cohort-8");
        workbench.awaitText("60 lessons in this immutable snapshot");
        withRejectedWorkspaceUpdates(DRAFT_ONLY, () -> {
            workbench.click("#confirm-bulk");
            workbench.awaitText("The latest repair change was not durably saved");
            assertEquals(beforePreview, storedDocument(), "UC-2 ext 5b: failed bulk confirmation preserves exact Draft/version");
            workbench.expect("document.querySelector('#solve-draft').disabled && !document.querySelector('[data-undo-bulk]')");
        });
        workbench.previewBulk("CLASS", "cohort-8");
        workbench.awaitText("60 lessons in this immutable snapshot");
        workbench.click("#confirm-bulk");
        workbench.awaitText("Confirmed bulk snapshot · 60 lessons");
        JsonNode action = assertDraftUnchangedBaseline(baseline).path("intent").path("bulkActions").get(0);
        assertEquals("CLASS", action.path("scope").stringValue());
        assertEquals("cohort-8", action.path("scopeId").stringValue());
        assertEquals(JSON.readTree("[\"PERIOD\"]"), action.path("dimensions"));
        assertEquals(classEightLessons, jsonStrings(action.path("lessonIds")),
                "UC-2 main 5: confirmation applies the exact previewed class-eight lesson IDs");
        workbench.expect("document.querySelector('.bulk-history').textContent.includes('Confirmed bulk snapshot · 60 lessons') && document.querySelector('.bulk-history').textContent.includes('Accepted period')");
        String beforeFailedUndo = storedDocument();
        withRejectedWorkspaceUpdates(DRAFT_ONLY, () -> {
            workbench.click("[data-undo-bulk]");
            workbench.awaitText("The latest repair change was not durably saved");
            assertEquals(beforeFailedUndo, storedDocument(), "UC-2 ext 5b: failed undo retains the named bulk action exactly");
            workbench.expect("document.querySelector('#solve-draft').disabled && document.querySelector('[data-undo-bulk]')");
        });
        workbench.click("[data-undo-bulk]");
        workbench.expect("!document.querySelector('[data-undo-bulk]')");
        assertEquals(individual.path("intent"), assertDraftUnchangedBaseline(baseline).path("intent"),
                "UC-2 main 5: undo removes only the named bulk action and retains individual room protection");
        assertTrue(assertDraftUnchangedBaseline(baseline).path("readyToSolve").booleanValue());
        workbench.expect("!document.querySelector('#solve-draft').disabled && document.querySelector('.protection-list summary').textContent.endsWith('1')",
                "UC-2 success: the saved conflict-free Draft is ready to solve with one unique protected lesson");

        String beforePresentation = storedDocument();
        Workbench.MutationLog mutations = workbench.recordMutations();
        workbench.click("#collapse-draft-task");
        workbench.mode("CURRENT");
        workbench.mode("DRAFT");
        workbench.expect("document.querySelector('#workbench-task-area').hidden && !document.querySelector('#reopen-draft-task').hidden");
        workbench.click("#reopen-draft-task");
        workbench.openFocus("teacherId");
        workbench.awaitText("Teacher schedule");
        workbench.returnToMatrix();
        workbench.expect("!document.querySelector('#workbench-task-area').hidden && document.querySelector('#lesson-panel-title')?.textContent === 'Declared lesson 500'");
        assertEquals(List.of(), mutations.requests(), "UC-2 G3: mode, task collapse, focus and return issue no mutating request");
        assertEquals(beforePresentation, storedDocument(), "UC-2 main 6/G3: mode, collapse, focus and return save nothing");
        workbench.viewport(1280, 800);
        assertDraftTaskGeometry(1280, 800);
        workbench.screenshot("uc2-draft-1280.png");
        workbench.viewport(1279, 800);
        workbench.expect("document.querySelector('#workbench-inspector').getBoundingClientRect().top >= document.querySelector('.canvas-region').getBoundingClientRect().bottom && document.querySelector('#workbench-task-area').getBoundingClientRect().top >= document.querySelector('#workbench-inspector').getBoundingClientRect().bottom && document.documentElement.scrollWidth <= innerWidth",
                "UC-2 G6: at 1279px the inspector stacks between canvas and wide task area without page-level horizontal scroll");
        workbench.viewport(1280, 800);
        jdbc.sql("UPDATE workspace_aggregate SET version=version+1 WHERE workspace_id=1").update();
        String beforeStale = storedDocument();
        workbench.click("#stage-repair-form button[type=submit]");
        workbench.awaitText("The latest repair change was not durably saved");
        assertEquals(beforeStale, storedDocument(), "UC-2 ext 2b/7a: stale revision preserves the last durable Draft/version");
        workbench.expect("document.querySelector('#solve-draft').disabled");
        workbench.viewport(701, 844);
        workbench.expect("document.querySelector('#workbench-inspector').getBoundingClientRect().top >= document.querySelector('.canvas-region').getBoundingClientRect().bottom && document.querySelector('#workbench-task-area #stage-repair-form') && document.documentElement.scrollWidth <= innerWidth",
                "UC-2 G6: intermediate width keeps a wide task area and below-canvas inspector");
        for (int[] narrow : new int[][] { { 700, 844 }, { 390, 844 } }) {
            workbench.viewport(narrow[0], narrow[1]);
            workbench.awaitText("Read-only focused schedule");
            workbench.expectNot("document.querySelector('#start-repair-form, #stage-repair-form, #apply-pin, #preview-bulk, #solve-draft, #discard-draft, #workbench-task-area')",
                    "UC-2 ext 1b: " + narrow[0] + " px Draft is truly read-only");
        }
        assertEquals(beforeStale, storedDocument());
        workbench.viewport(1280, 800);
        workbench.reload();
        workbench.expect("document.querySelector('[data-mode=DRAFT]')?.getAttribute('aria-pressed') === 'true' && document.querySelector('#workbench-task-area') && !document.querySelector('#workbench-task-area').hidden");
        workbench.expect("!document.querySelector('#lesson-panel-title') && document.querySelector('#room-filter').value === '' && document.querySelector('#lesson-search').value === ''");
        assertEquals(beforeStale, storedDocument(), "UC-2 ext 6b: reload retains only durable Draft and range preference");
        workbench.discardDraft();
        workbench.awaitText("Start a protected repair");
        assertEquals("ACCEPTED_BASELINE", storedLifecycle());
        assertEquals(baseline, storedWorkspaceDocument().path("acceptedBaseline"));
        assertFalse(storedWorkspaceDocument().has("repairDraft"));

        workbench.startRepair("ROOM", "room-99", "period-0");
        workbench.awaitText("This rule currently conflicts with no accepted assignment");
        JsonNode room = assertDraftUnchangedBaseline(baseline);
        assertEquals("ROOM", room.path("intent").path("changes").get(0).path("resourceType").stringValue());
        assertEquals("room-99", room.path("intent").path("changes").get(0).path("resourceId").stringValue());
        assertTrue(room.path("directEffectLessonIds").isEmpty());
        assertTrue(room.path("readyToSolve").booleanValue());
    }

    @Test
    @DisplayName("UC-4 main/extensions/G4/RULE-19/20/24: real keyboard browser stages, pins, previews, resolves conflict, and discards safely")
    void preparesProtectedRepairDraftWithKeyboard() {
        storeAccepted(fixtures.acceptedDocument(false));
        JsonNode acceptedBefore = storedWorkspaceDocument().path("acceptedBaseline").deepCopy();
        workbench.open().awaitText("Start a protected repair");
        workbench.startRepair(null, null, "mon-1");
        String draft = workbench.awaitText("Repair draft · not current");
        assertTrue(draft.contains("Directly affected lessons\n1"));
        assertTrue(draft.contains("Accepted baseline remains current"));
        assertTrue(draft.contains("Mathematics"));

        String durableDraft = storedDocument();
        String workspaceRequests = "performance.getEntriesByType('resource').filter(entry => entry.name.endsWith('/api/workspace')).length";
        workbench.selectLesson("lesson-math-1");
        int requestsBeforeMode = workbench.value(workspaceRequests).intValue();
        workbench.mode("CURRENT");
        String currentView = workbench.awaitText("Complete school population");
        assertTrue(currentView.contains("Repair draft · not current") && currentView.contains("Current · accepted"));
        workbench.expect("document.querySelector('#workbench-modes [data-mode=CURRENT]').getAttribute('aria-pressed') === 'true' && document.querySelector('#accepted-view .week-matrix') !== null && document.querySelector('#workbench-inspector #lesson-panel-title')?.textContent === 'Mathematics 1' && !document.querySelector('#apply-pin')",
                "Current from Draft shows exact accepted selection without draft mutation controls");
        workbench.filterClass("cohort-7a");
        workbench.mode("DRAFT");
        workbench.awaitText("Bulk-protect accepted assignments");
        workbench.mode("CURRENT");
        workbench.expect("document.querySelector('#cohort-filter').value === 'cohort-7a' && document.querySelector('[data-range=WEEK]').getAttribute('aria-pressed') === 'true' && document.querySelector('#workbench-inspector #lesson-panel-title')?.textContent === 'Mathematics 1'",
                "mode round trip retains representable filter, Week, and selection");
        assertEquals(requestsBeforeMode, workbench.value(workspaceRequests).intValue(), "presentation switches do not reload the workspace");
        assertEquals(durableDraft, storedDocument(), "mode switches cannot mutate draft or accepted state");
        workbench.mode("DRAFT");

        workbench.viewport(390, 844);
        String narrow = workbench.awaitText("Read-only focused schedule");
        assertFalse(narrow.contains("Apply selected pins"));
        assertFalse(narrow.contains("Discard repair draft"));
        workbench.expectNot("document.querySelector('.matrix-wrap')");
        workbench.viewport(1280, 800);
        workbench.awaitText("Bulk-protect accepted assignments");

        workbench.press("[data-lesson-id=lesson-math-1]", "Space");
        workbench.awaitText("Protect accepted assignment dimensions");
        workbench.press("#apply-pin", "Space");
        assertTrue(workbench.awaitText("Resolve blocking conflicts before solving").contains("Blocking conflict"));

        workbench.selectLesson("lesson-math-1");
        workbench.click("#remove-pin");
        workbench.awaitText("Draft is durably saved with no blocking conflict");

        workbench.click("#preview-bulk");
        assertTrue(workbench.awaitText("Bulk pin preview · no changes applied yet").contains("1 lesson in this immutable snapshot"));
        workbench.click("#confirm-bulk");
        workbench.awaitText("Confirmed bulk snapshot · 1 lesson");
        workbench.click("[data-undo-bulk]");
        workbench.awaitText("Attempt-scoped pins\n0");

        workbench.discardDraft();
        workbench.awaitText("Start a protected repair");
        JsonNode after = storedWorkspaceDocument();
        assertEquals("ACCEPTED_BASELINE", storedLifecycle());
        assertEquals(acceptedBefore, after.path("acceptedBaseline"));
        assertFalse(after.has("repairDraft"));
    }

    @Test
    @DisplayName("Timetable polish UC-2 main/7b/G1-G5: teacher intent preserves accepted IDs, selection, mode and focused context")
    void retainsAcceptedCanvasThroughTeacherDraftModesAndReload() {
        ObjectNode accepted = fixtures.validAcceptedDocument();
        verifier.verify(new ImportDocuments(accepted.path("acceptedBaseline").path("definition"),
                accepted.path("acceptedBaseline").path("result"), null, ImportDocuments.ImportMode.ACCEPTED_BASELINE));
        storeAccepted(accepted);
        JsonNode baseline = accepted.path("acceptedBaseline").deepCopy();
        workbench.open().awaitText("Complete recurring Week");
        assertEquals(DEMO_LESSONS, workbench.renderedLessonIds());
        workbench.selectLesson("lesson-math-1");
        workbench.range("DAY");
        workbench.awaitText("Day · Monday");
        workbench.investigateTeacher("teacher-alex");
        workbench.startRepair(null, null, "mon-1");
        String draftText = workbench.awaitText("Repair draft · not current");
        assertTrue(draftText.contains("Accepted baseline remains current"));
        assertTrue(draftText.contains("Alex"));
        JsonNode draft = assertDraftUnchangedBaseline(baseline);
        assertEquals(1, draft.path("intent").path("changes").size());
        assertEquals("TEACHER", draft.path("intent").path("changes").get(0).path("resourceType").stringValue());
        assertEquals("teacher-alex", draft.path("intent").path("changes").get(0).path("resourceId").stringValue());
        assertEquals(JSON.readTree("[\"mon-1\"]"), draft.path("intent").path("changes").get(0).path("unavailablePeriodIds"));
        assertEquals(JSON.readTree("[\"lesson-math-1\"]"), draft.path("directEffectLessonIds"));
        assertEquals(0, draft.path("conflicts").size());
        assertTrue(draft.path("readyToSolve").booleanValue());
        assertTrue(draft.path("persisted").booleanValue());
        assertEquals(0, draft.path("intent").path("pins").size());
        assertEquals(0, draft.path("intent").path("bulkActions").size());
        String durable = storedDocument();
        assertEquals(DEMO_LESSONS, workbench.renderedLessonIds());
        workbench.expect("document.querySelector('#workbench-modes [data-mode=DRAFT]')?.getAttribute('aria-pressed') === 'true' && document.querySelector('#lesson-panel-title')?.textContent === 'Mathematics 1' && document.querySelector('[data-lesson-id=lesson-math-1]')?.textContent.includes('Directly affected')");

        workbench.mode("CURRENT");
        workbench.awaitText("Current · accepted");
        assertEquals(DEMO_LESSONS, workbench.renderedLessonIds());
        workbench.expect("document.querySelector('#lesson-panel-title')?.textContent === 'Mathematics 1' && !document.querySelector('#apply-pin') && document.querySelector('#teacher-investigation')?.value === 'teacher-alex' && document.querySelector('[data-range=DAY]')?.getAttribute('aria-pressed') === 'true'");
        workbench.mode("DRAFT");
        workbench.awaitText("Directly affected");
        workbench.expect("document.querySelector('#lesson-panel-title')?.textContent === 'Mathematics 1' && document.querySelector('#apply-pin') !== null");
        workbench.collapseInspector();
        workbench.expect("document.querySelector('.workbench-layout')?.classList.contains('inspector-collapsed') && document.querySelector('#inspector-summary')?.textContent.includes('Mathematics 1')");
        workbench.reopenInspector();
        workbench.openFocus("teacherId");
        workbench.awaitText("Teacher schedule · Alex");
        workbench.returnToMatrix();
        workbench.awaitText("Day · Monday");
        assertEquals(DEMO_LESSONS, workbench.renderedLessonIds());
        assertEquals(durable, storedDocument(), "mode, range, inspector and focused return are presentation-only");

        workbench.reload();
        workbench.expect("document.querySelector('#workbench-modes [data-mode=DRAFT]')?.getAttribute('aria-pressed') === 'true' && document.querySelector('[data-range=DAY]')?.getAttribute('aria-pressed') === 'true' && !document.querySelector('#lesson-panel-title')");
        assertEquals(durable, storedDocument(), "reload restores the durable draft, not ephemeral selection or mode");
    }

    @Test
    @DisplayName("Timetable polish UC-2 extensions 4a/4b/5a: conflicting individual pin, canceled preview, confirmed bulk and exact undo")
    void resolvesConflictingPinAndUndoesOnlyConfirmedBulkSources() {
        ObjectNode accepted = fixtures.validAcceptedDocument();
        storeAccepted(accepted);
        JsonNode baseline = accepted.path("acceptedBaseline").deepCopy();
        workbench.open().awaitText("Start a protected repair");
        workbench.startRepair(null, null, "mon-1");
        workbench.awaitText("Repair draft · not current");
        assertEquals(JSON.readTree("[\"lesson-math-1\"]"), assertDraftUnchangedBaseline(baseline).path("directEffectLessonIds"));
        workbench.pin("lesson-math-1", true, false);
        assertTrue(workbench.awaitText("Resolve blocking conflicts before solving").contains("Blocking conflict"));
        JsonNode conflicted = assertDraftUnchangedBaseline(baseline);
        assertEquals(1, conflicted.path("conflicts").size());
        assertEquals("lesson-math-1", conflicted.path("conflicts").get(0).path("lessonId").stringValue());
        assertEquals("PIN_CONTRADICTS_UNAVAILABILITY", conflicted.path("conflicts").get(0).path("code").stringValue());
        assertEquals(JSON.readTree("[\"INDIVIDUAL\"]"), conflicted.path("intent").path("pins").get(0).path("periodSources"));
        assertFalse(conflicted.path("readyToSolve").booleanValue());
        workbench.expect("document.querySelector('#solve-draft')?.disabled === true && document.querySelector('[data-lesson-id=lesson-math-1]')?.textContent.includes('Blocking conflict')");
        workbench.selectLesson("lesson-math-1");
        workbench.click("#remove-pin");
        workbench.awaitText("Draft is durably saved with no blocking conflict");
        JsonNode resolved = assertDraftUnchangedBaseline(baseline);
        assertEquals(0, resolved.path("conflicts").size());
        assertEquals(0, resolved.path("intent").path("pins").size());
        assertTrue(resolved.path("readyToSolve").booleanValue());

        workbench.pin("lesson-science-1", true, true);
        workbench.awaitText("Accepted room pinned");
        JsonNode individual = assertDraftUnchangedBaseline(baseline);
        assertEquals(1, individual.path("intent").path("pins").size());
        JsonNode individualPin = individual.path("intent").path("pins").get(0);
        assertEquals("lesson-science-1", individualPin.path("lessonId").stringValue());
        assertEquals(JSON.readTree("[\"INDIVIDUAL\"]"), individualPin.path("periodSources"));
        assertEquals(JSON.readTree("[\"INDIVIDUAL\"]"), individualPin.path("roomSources"));
        String beforePreview = storedDocument();
        workbench.click("#preview-bulk");
        assertTrue(workbench.awaitText("Bulk pin preview · no changes applied yet").contains("1 lesson in this immutable snapshot"));
        assertEquals(beforePreview, storedDocument(), "preview must not write pins or advance version");
        workbench.click("#cancel-bulk");
        workbench.expect("!document.querySelector('#confirm-bulk') && document.querySelector('#lesson-panel-title')?.textContent === 'Science 1'");
        assertEquals(beforePreview, storedDocument(), "cancellation must leave exact individual protection intact");

        workbench.click("#preview-bulk");
        workbench.awaitText("Bulk pin preview · no changes applied yet");
        assertEquals(beforePreview, storedDocument());
        workbench.click("#confirm-bulk");
        workbench.awaitText("Confirmed bulk snapshot · 1 lesson");
        JsonNode bulk = assertDraftUnchangedBaseline(baseline);
        assertEquals(1, bulk.path("intent").path("bulkActions").size());
        JsonNode action = bulk.path("intent").path("bulkActions").get(0);
        assertEquals("UNAFFECTED", action.path("scope").stringValue());
        assertEquals(JSON.readTree("[\"lesson-science-1\"]"), action.path("lessonIds"));
        assertEquals(JSON.readTree("[\"PERIOD\"]"), action.path("dimensions"));
        JsonNode bulkPin = bulk.path("intent").path("pins").get(0);
        assertEquals("lesson-science-1", bulkPin.path("lessonId").stringValue());
        assertEquals(Set.of("INDIVIDUAL", action.path("id").stringValue()), jsonStrings(bulkPin.path("periodSources")));
        assertEquals(JSON.readTree("[\"INDIVIDUAL\"]"), bulkPin.path("roomSources"));
        workbench.click("[data-undo-bulk]");
        workbench.expect("!document.querySelector('[data-undo-bulk]') && document.querySelector('#attempt-pin-count')?.textContent === '1'");
        JsonNode undone = assertDraftUnchangedBaseline(baseline);
        assertEquals(individual.path("intent"), undone.path("intent"), "undo restores individual dimensions, removing only this bulk source");
        assertEquals(0, undone.path("conflicts").size());
        assertTrue(undone.path("readyToSolve").booleanValue());
    }

    @Test
    @DisplayName("Timetable polish UC-2 extensions 3a/7a/7b/G6: no-effect room intent, narrow read-only draft and confirmed discard")
    void keepsZeroEffectRoomDraftOnReloadThenDiscardsExplicitly() {
        ObjectNode accepted = fixtures.validAcceptedDocument();
        storeAccepted(accepted);
        JsonNode baseline = accepted.path("acceptedBaseline").deepCopy();
        workbench.open().awaitText("Start a protected repair");
        workbench.startRepair("ROOM", "room-101", "mon-1");
        String zero = workbench.awaitText("Repair draft · not current");
        assertTrue(zero.contains("Accepted baseline remains current"));
        assertTrue(zero.contains("This rule currently conflicts with no accepted assignment"));
        JsonNode noEffect = assertDraftUnchangedBaseline(baseline);
        assertEquals(0, noEffect.path("directEffectLessonIds").size());
        assertEquals(0, noEffect.path("conflicts").size());
        assertEquals("ROOM", noEffect.path("intent").path("changes").get(0).path("resourceType").stringValue());
        assertEquals("room-101", noEffect.path("intent").path("changes").get(0).path("resourceId").stringValue());
        assertEquals(JSON.readTree("[\"mon-1\"]"), noEffect.path("intent").path("changes").get(0).path("unavailablePeriodIds"));
        assertTrue(noEffect.path("readyToSolve").booleanValue());
        assertEquals(DEMO_LESSONS, workbench.renderedLessonIds());
        String beforeNarrow = storedDocument();
        workbench.viewport(390, 844);
        assertTrue(workbench.awaitText("Read-only focused schedule").contains("Repair draft · not current"));
        workbench.expectNot("document.querySelector('#start-repair-form, #apply-pin, #preview-bulk, #discard-draft, #solve-draft, #workbench-modes')");
        assertEquals(beforeNarrow, storedDocument());
        workbench.viewport(1280, 800);
        workbench.awaitText("Discard repair draft");
        workbench.reload();
        workbench.expect("document.querySelector('#workbench-modes [data-mode=DRAFT]')?.getAttribute('aria-pressed') === 'true' && document.querySelector('#discard-draft')?.disabled === true");
        assertEquals(beforeNarrow, storedDocument());
        workbench.discardDraft();
        workbench.awaitText("Start a protected repair");
        JsonNode discarded = storedWorkspaceDocument();
        assertEquals("ACCEPTED_BASELINE", storedLifecycle());
        assertEquals(baseline, discarded.path("acceptedBaseline"));
        assertFalse(discarded.has("repairDraft"));
        assertFalse(discarded.has("run"));
        assertFalse(discarded.has("proposal"));
        workbench.expect("document.querySelectorAll('#workbench-modes [data-mode]').length === 1 && document.querySelector('#workbench-modes [data-mode=CURRENT]')?.getAttribute('aria-pressed') === 'true'");
    }

    @Test
    @DisplayName("Timetable polish UC-2 extension 2a/minimal guarantee: failed durable pin save cannot enable repair")
    void refusesSolveAfterRealDraftPersistenceFailure() throws Exception {
        ObjectNode accepted = fixtures.validAcceptedDocument();
        storeAccepted(accepted);
        JsonNode baseline = accepted.path("acceptedBaseline").deepCopy();
        workbench.open().awaitText("Start a protected repair");
        workbench.startRepair(null, null, "mon-1");
        workbench.awaitText("Repair draft · not current");
        String durable = storedDocument();
        withRejectedWorkspaceUpdates(DRAFT_ONLY, () -> {
            workbench.pin("lesson-science-1", false, true);
            String error = workbench.awaitText("The latest repair change was not durably saved");
            assertTrue(error.contains("Accepted baseline remains current"));
            assertTrue(error.contains("Local storage is unavailable. The action did not complete."));
            assertEquals(durable, storedDocument(), "failed autosave must roll back version and complete document");
            workbench.expect("document.querySelector('#solve-draft')?.disabled === true",
                    "the unpersisted revision must not be presented as ready to solve");
        });
        assertEquals(durable, storedDocument());
        assertDraftUnchangedBaseline(baseline);
        assertFalse(storedWorkspaceDocument().has("run"));
        assertFalse(storedWorkspaceDocument().has("proposal"));
    }

    /** UC-2 G2: the wide task area leaves the canvas, inspector and a full class row usable, and its decisions reachable. */
    private void assertDraftTaskGeometry(int width, int height) {
        workbench.scrollToTop();
        JsonNode geometry = workbench.value("""
                (() => { const task=document.querySelector('#workbench-task-area');
                  const canvas=document.querySelector('.canvas-region'); const wrap=document.querySelector('.matrix-wrap');
                  const inspector=document.querySelector('#workbench-inspector');
                  return {taskHeight:task.getBoundingClientRect().height, taskTop:task.getBoundingClientRect().top,
                    taskBottom:task.getBoundingClientRect().bottom,
                    canvasBottom:canvas.getBoundingClientRect().bottom, inspectorLeft:inspector.getBoundingClientRect().left,
                    canvasRight:canvas.getBoundingClientRect().right, row:document.querySelector('.week-matrix tbody tr').getBoundingClientRect().height,
                    heading:document.querySelector('.week-matrix thead').getBoundingClientRect().height,
                    visible:wrap.clientHeight, page:document.documentElement.scrollWidth,
                    taskRight:task.getBoundingClientRect().right, shellRight:document.querySelector('.workspace-card').getBoundingClientRect().right,
                    taskScroll:task.scrollHeight > task.clientHeight}; })()""");
        assertTrue(geometry.path("taskHeight").doubleValue() <= height * .35, "UC-2 G2: task height: " + geometry);
        assertTrue(geometry.path("taskBottom").doubleValue() <= height, "UC-2 G2: task and canvas remain concurrently visible: " + geometry);
        assertTrue(geometry.path("taskTop").doubleValue() >= geometry.path("canvasBottom").doubleValue(), "UC-2 G2: task cannot overlay canvas: " + geometry);
        assertTrue(geometry.path("inspectorLeft").doubleValue() >= geometry.path("canvasRight").doubleValue(), "UC-2 G2: inspector remains beside canvas: " + geometry);
        assertTrue(geometry.path("visible").doubleValue() >= geometry.path("heading").doubleValue() + geometry.path("row").doubleValue(),
                "UC-2 G2: time heading and one complete class row remain visible: " + geometry);
        assertTrue(geometry.path("page").doubleValue() <= width + 1, "UC-2 G2: no page-level horizontal scroll: " + geometry);
        assertTrue(geometry.path("taskRight").doubleValue() <= geometry.path("shellRight").doubleValue(), "UC-2 G2: task remains within shell: " + geometry);
        assertTrue(geometry.path("taskScroll").booleanValue(), "UC-2 G2: long task content scrolls independently: " + geometry);
        workbench.expect("""
                (() => { const task=document.querySelector('#workbench-task-area'); task.scrollTop=task.scrollHeight;
                  const bounds=task.getBoundingClientRect();
                  return ['#solve-draft', '#discard-draft'].every(selector => {
                    const action=document.querySelector(selector).getBoundingClientRect();
                    return action.top >= bounds.top && action.bottom <= bounds.bottom && action.bottom <= innerHeight;
                  }); })()""", "UC-2 G2: " + width + "px solve/discard controls are reachable in the task scrollport");
    }
}
