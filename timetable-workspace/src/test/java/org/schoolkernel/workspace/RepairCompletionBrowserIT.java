package org.schoolkernel.workspace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/** UC-5 of the workbench specs: one wide workbench carries a repair from Current to an accepted successor. */
class RepairCompletionBrowserIT extends WorkbenchBrowserSupport {
    private static final Duration SCALE = Duration.ofSeconds(20);

    @Test
    @DisplayName("Workbench layout UC-5 main/2a/3a/5a/G1-G5: one wide workbench completes repair and parents the next Draft")
    void completesWholeSchoolRepairAndStartsNextFromAcceptedSuccessor() throws Exception {
        ObjectNode document = fixtures.investigationScaleDocument();
        JsonNode original = document.path("acceptedBaseline").deepCopy();
        assertEquals(document.path("timetableRevision").stringValue(), verifier.verify(new ImportDocuments(
                original.path("definition"), original.path("result"), null,
                ImportDocuments.ImportMode.ACCEPTED_BASELINE)).timetableRevision());
        assertEquals(60, original.path("definition").path("cohorts").size());
        assertEquals(100, original.path("definition").path("teachers").size());
        assertEquals(100, original.path("definition").path("rooms").size());
        assertEquals(1_000, original.path("result").path("timetable").path("assignments").size());
        assertEquals(JSON.readTree("{\"lessonId\":\"lesson-960\",\"subjectId\":\"subject-0\",\"cohortId\":\"cohort-16\",\"teacherId\":\"teacher-16\",\"periodId\":\"period-0\",\"roomId\":\"room-16\"}"),
                original.path("result").path("timetable").path("assignments").get(960));
        storeAccepted(document);

        workbench.open().awaitText("Showing 60 of 60 classes", SCALE);
        workbench.expect("document.querySelector('.accepted-heading')?.textContent.includes('Scale School') && document.querySelector('[data-mode=CURRENT]')?.getAttribute('aria-pressed') === 'true' && document.querySelector('[data-range=WEEK]')?.getAttribute('aria-pressed') === 'true'");
        Set<String> expectedIds = new HashSet<>();
        for (int i = 0; i < 1_000; i++) expectedIds.add("lesson-" + i);
        assertEquals(expectedIds, workbench.renderedLessonIds(), "UC-5 main 1: every accepted lesson identity is visible");
        workbench.selectLesson("lesson-960");
        workbench.range("DAY");
        workbench.investigateSubject("subject-0");
        workbench.investigateTeacher("teacher-16");
        workbench.expect("document.querySelector('#lesson-panel-title')?.textContent === 'Declared lesson 960' && document.querySelector('#workbench-inspector')?.textContent.includes('period-0') && document.querySelector('#workbench-inspector')?.textContent.includes('room-16') && document.querySelector('#range-summary')?.textContent.includes('Monday')");
        workbench.openFocus("teacherId");
        workbench.focusEntity("teacher-16");
        workbench.expect("document.querySelector('#return-matrix') && document.querySelector('.focused-schedule')?.textContent.includes('Teacher Sixteen')");
        workbench.returnToMatrix();
        workbench.expect("document.querySelector('[data-range=DAY]')?.getAttribute('aria-pressed') === 'true' && document.querySelector('#lesson-panel-title')?.textContent === 'Declared lesson 960'");
        assertEquals(original, storedWorkspaceDocument().path("acceptedBaseline"));

        workbench.startRepair(null, "teacher-16", "period-0");
        workbench.awaitText("Repair draft · not current", SCALE);
        JsonNode started = assertDraftUnchangedBaseline(original).deepCopy();
        assertEquals(JSON.readTree("[\"lesson-960\"]"), started.path("directEffectLessonIds"));
        assertEquals("teacher-16", started.path("intent").path("changes").get(0).path("resourceId").stringValue());
        workbench.expect("document.querySelector('[data-mode=DRAFT]')?.getAttribute('aria-pressed') === 'true' && document.querySelector('[data-range=DAY]')?.getAttribute('aria-pressed') === 'true' && document.querySelector('#lesson-panel-title')?.textContent === 'Declared lesson 960' && document.querySelector('#teacher-investigation')?.value === 'teacher-16'");
        workbench.pin(null, true, false);
        workbench.awaitText("Resolve blocking conflicts before solving");
        JsonNode conflicted = assertDraftUnchangedBaseline(original);
        assertFalse(conflicted.path("readyToSolve").booleanValue());
        assertEquals("lesson-960", conflicted.path("conflicts").get(0).path("lessonId").stringValue());
        workbench.expect("document.querySelector('[data-draft-conflict=lesson-960]') && document.querySelector('#solve-draft')?.disabled === true");
        assertEquals(0, replanCommands(), "UC-5 2a: a conflict starts no scheduling process");
        workbench.click("#remove-pin");
        workbench.awaitText("Draft is durably saved with no blocking conflict");
        workbench.range("WEEK");
        workbench.pin("lesson-500", true, true);
        workbench.awaitText("Accepted room pinned");
        JsonNode draft = assertDraftUnchangedBaseline(original).deepCopy();
        assertTrue(draft.path("readyToSolve").booleanValue());
        assertEquals(JSON.readTree("[\"lesson-960\"]"), draft.path("directEffectLessonIds"));
        assertEquals("lesson-500", draft.path("intent").path("pins").get(0).path("lessonId").stringValue());
        assertEquals(JSON.readTree("[\"INDIVIDUAL\"]"), draft.path("intent").path("pins").get(0).path("roomSources"));
        assertWideJourneyPhase("DRAFT", "#solve-draft", "draft");
        workbench.selectLesson("lesson-960");
        workbench.range("DAY");
        workbench.filterClass("cohort-16");
        workbench.collapseInspector();
        workbench.expect("document.querySelector('#inspector-summary')?.textContent.includes('Declared lesson 960') && document.querySelector('#workbench-inspector')?.hidden === true");
        workbench.reopenInspector();
        // Reaching the class filter opened Filters; with it open, the wide task area no longer fits (G3).
        workbench.closeDisclosure("#filters");
        assertEquals(draft, assertDraftUnchangedBaseline(original), "UC-5 main 2: inspector and filter changes cannot edit the durable Draft");

        processes.blockReplan = true;
        workbench.click("#solve-draft");
        processes.awaitBlocked();
        workbench.awaitText("Repair generation · running", SCALE);
        JsonNode running = storedWorkspaceDocument();
        assertEquals("SOLVING_REPAIR", storedLifecycle());
        assertEquals(original, running.path("acceptedBaseline"));
        assertEquals(draft, running.path("repairDraft"));
        assertFalse(running.has("proposal"));
        assertEquals("PT30S", running.path("run").path("limit").stringValue());
        workbench.range("WEEK");
        assertWideJourneyPhase("SOLVING", "#cancel-run", "solving");
        workbench.range("DAY");
        workbench.expect("document.querySelector('[data-mode=SOLVING]')?.getAttribute('aria-pressed') === 'true' && document.querySelector('#cancel-run') && !document.querySelector('#apply-pin') && document.querySelector('#cohort-filter')?.value === 'cohort-16' && document.querySelector('#lesson-panel-title')?.textContent === 'Declared lesson 960'");
        workbench.mode("CURRENT");
        workbench.expect("document.querySelector('[data-mode=CURRENT]')?.getAttribute('aria-pressed') === 'true' && document.querySelector('#lesson-panel-title')?.textContent === 'Declared lesson 960'");
        workbench.mode("DRAFT");
        workbench.expect("document.querySelector('#workbench-task-area #run-secondary')?.textContent.includes('Frozen repair intent') && !document.querySelector('#solve-draft')");
        workbench.mode("SOLVING");
        assertEquals(running, storedWorkspaceDocument());
        assertEquals(1, replanCommands(), "UC-5 main 3: presentation changes cannot launch another run");
        workbench.click("#cancel-run");
        workbench.awaitText("Repair draft · not current", SCALE);
        assertEquals(draft, assertDraftUnchangedBaseline(original));
        assertEquals("CANCELLED", storedWorkspaceDocument().path("lastRun").path("status").stringValue());
        workbench.expect("document.querySelector('[data-mode=DRAFT]')?.getAttribute('aria-pressed') === 'true' && !document.querySelector('[data-mode=PROPOSAL]') && document.querySelector('#solve-draft') && document.querySelector('#cohort-filter')?.value === 'cohort-16'");

        processes.blockReplan = false;
        processes.verifiedFeasibleResult = fixtures::verifiedNormativeRepairResult;
        workbench.click("#solve-draft");
        workbench.awaitText("Repair proposal · feasible", Duration.ofSeconds(30));
        JsonNode proposed = storedWorkspaceDocument();
        assertEquals("REPAIR_PROPOSAL", storedLifecycle());
        assertEquals(original, proposed.path("acceptedBaseline"));
        assertEquals(draft, proposed.path("repairDraft"));
        assertFalse(proposed.has("run"));
        assertEquals(2, replanCommands());
        JsonNode proposal = proposed.path("proposal");
        assertEquals(2, proposal.path("review").path("uniqueChangedLessonCount").intValue());
        assertEquals(1, proposal.path("review").path("directEffectChangedCount").intValue());
        assertEquals(1, proposal.path("review").path("rippleEffectCount").intValue());
        assertEquals("period-40", proposal.path("result").path("timetable").path("assignments").get(960).path("periodId").stringValue());
        assertEquals("room-50", proposal.path("result").path("timetable").path("assignments").get(0).path("roomId").stringValue());
        workbench.expect("document.querySelector('[data-mode=PROPOSAL]')?.getAttribute('aria-pressed') === 'true' && document.querySelector('#cohort-filter')?.value === 'cohort-16' && document.querySelector('#lesson-panel-title')?.textContent === 'Declared lesson 960' && document.querySelector('.comparison-details')?.textContent.includes('period-40')");
        workbench.range("WEEK");
        workbench.resetView();
        assertWideJourneyPhase("PROPOSAL", "#accept-repair", "proposal");
        workbench.viewport(390, 844);
        workbench.expect("document.querySelector('.focused-schedule') !== null && document.querySelector('#workbench-task-area') === null");
        workbench.expect("document.body.innerText.includes('Repair proposal') && document.body.innerText.includes('Accepted baseline remains current') && !document.querySelector('#accept-repair, #revise-proposal, #discard-proposal, #cancel-run') && document.documentElement.scrollWidth <= innerWidth",
                "UC-5 G3/RULE-12: the same verified Proposal becomes a read-only narrow agenda");
        assertEquals(proposed, storedWorkspaceDocument(), "narrow reading cannot change the accepted/Draft/Proposal document");
        workbench.viewport(1280, 800);
        workbench.expect("document.querySelector('#workbench-task-area #accept-repair') !== null");
        workbench.expect("document.querySelectorAll('.matrix-wrap [data-lesson-id=lesson-960]').length === 2 && document.querySelectorAll('.matrix-wrap [data-lesson-id=lesson-0]').length === 1 && document.querySelector('[data-lesson-id=lesson-960][data-comparison-side=accepted]') && document.querySelector('[data-lesson-id=lesson-960][data-comparison-side=proposed]')");
        workbench.click("[data-lesson-id=lesson-0][data-comparison-side=combined]");
        workbench.expect("document.querySelector('.comparison-details')?.textContent.includes('room-0') && document.querySelector('.comparison-details')?.textContent.includes('room-50') && document.querySelector('.comparison-details')?.textContent.includes('Solver ripple')");
        workbench.selectLesson("lesson-500");
        workbench.expect("document.querySelector('.comparison-details')?.textContent.includes('Accepted and unchanged') && document.querySelector('.comparison-details')?.textContent.includes('Accepted room pinned')");
        workbench.click("[data-lesson-id=lesson-960][data-comparison-side=accepted]");
        workbench.range("DAY");
        workbench.expect("document.querySelector('#range-summary')?.textContent.includes('Monday') && document.querySelector('.comparison-details')?.textContent.includes('period-40')");
        workbench.mode("CURRENT");
        workbench.expect("document.querySelector('[data-mode=CURRENT]')?.getAttribute('aria-pressed') === 'true' && !document.querySelector('[data-lesson-id=lesson-960]')?.textContent.includes('Proposed destination')");
        workbench.mode("DRAFT");
        workbench.mode("PROPOSAL");
        assertEquals(proposed, storedWorkspaceDocument(), "UC-5 main 4: review and navigation cannot advance Current");
        workbench.expect("document.querySelector('[data-range=DAY]')?.getAttribute('aria-pressed') === 'true' && document.querySelector('#lesson-panel-title')?.textContent === 'Declared lesson 960'");
        workbench.acceptRepair();
        workbench.awaitText("Accepted baseline · current timetable", SCALE);
        JsonNode successorDocument = storedWorkspaceDocument();
        JsonNode successor = successorDocument.path("acceptedBaseline").deepCopy();
        assertEquals("ACCEPTED_BASELINE", storedLifecycle());
        assertEquals(proposal.path("definition"), successor.path("definition"));
        assertEquals(proposal.path("result"), successor.path("result"));
        assertFalse(successorDocument.has("repairDraft"));
        assertFalse(successorDocument.has("proposal"));
        assertEquals(proposal.path("proposedTimetableRevision").stringValue(), successorDocument.path("timetableRevision").stringValue());
        assertFalse(original.path("result").path("timetableRevision").equals(successorDocument.path("timetableRevision")),
                "UC-5 main 5: the accepted revision must advance to the verified successor");
        assertTrue(successor.path("manifest").path("locks").valueStream().anyMatch(lock ->
                        "lesson-500".equals(lock.path("lessonId").stringValue())
                                && "ATTEMPT_SCOPED".equals(lock.path("roomLockOrigin").stringValue())),
                "UC-5 5a: the accepted successor records the previous attempt's lock provenance");
        workbench.expect("document.querySelector('[data-mode=CURRENT]')?.getAttribute('aria-pressed') === 'true' && !document.querySelector('[data-mode=DRAFT], [data-mode=SOLVING], [data-mode=PROPOSAL]') && document.querySelector('[data-range=DAY]')?.getAttribute('aria-pressed') === 'true' && document.querySelector('#range-summary')?.textContent.includes('Monday') && !document.querySelector('[data-lesson-id=lesson-960]') && !document.querySelector('#lesson-panel-title') && document.querySelector('#inspection-notice')?.textContent.includes('outside the represented Day')",
                "UC-5 main 5: accepted period move must clear only an unrepresentable Monday selection, with an explanation");
        assertEquals(1_000, successor.path("result").path("timetable").path("assignments").size());
        workbench.range("WEEK");
        assertEquals(expectedIds, workbench.renderedLessonIds(), "UC-5 main 5: no accepted lesson can disappear or duplicate after acceptance");
        workbench.range("DAY");

        workbench.startRepair("ROOM", "room-50", "period-0");
        workbench.awaitText("Repair draft · not current", SCALE);
        JsonNode nextDraft = assertDraftUnchangedBaseline(successor).deepCopy();
        assertEquals("ROOM", nextDraft.path("intent").path("changes").get(0).path("resourceType").stringValue());
        assertEquals("room-50", nextDraft.path("intent").path("changes").get(0).path("resourceId").stringValue());
        assertEquals(JSON.readTree("[\"period-0\"]"), nextDraft.path("intent").path("changes").get(0).path("unavailablePeriodIds"));
        assertEquals(JSON.readTree("[\"lesson-0\"]"), nextDraft.path("directEffectLessonIds"));
        assertTrue(nextDraft.path("intent").path("pins").isEmpty(), "UC-5 5a: the next attempt must not inherit previous pins");
        assertTrue(nextDraft.path("intent").path("bulkActions").isEmpty());
        assertEquals(successor.path("result").path("timetableRevision"), storedWorkspaceDocument().path("timetableRevision"),
                "UC-5 5a: the next Draft keeps the successor timetable revision as Current");
        workbench.expect("!document.querySelector('#draft-selected-protection')?.textContent.includes('Policy room lock')",
                "UC-5 5a: the previous attempt pin must not appear as a persistent policy lock");
        workbench.expect("document.querySelector('[data-mode=DRAFT]')?.getAttribute('aria-pressed') === 'true' && document.querySelector('[data-range=DAY]')?.getAttribute('aria-pressed') === 'true' && document.querySelector('#draft-conflict-count')?.textContent === '0' && document.querySelector('#attempt-pin-count')?.textContent === '0'");
        assertEquals(2, replanCommands(), "the next Draft must not launch a run without an explicit request");
    }

    /** UC-5 G1-G3 at both wide breakpoints: canvas, inspector and the phase's decisive action stay usable, nothing saved. */
    private void assertWideJourneyPhase(String mode, String actionSelector, String screenshotPhase) {
        String beforePresentation = storedDocument();
        for (int[] viewport : new int[][] { { 1600, 900 }, { 1280, 800 } }) {
            workbench.viewport(viewport[0], viewport[1]);
            workbench.expect("document.querySelector('.week-matrix') !== null && document.querySelector('#workbench-task-area') !== null");
            workbench.scrollToTop();
            JsonNode geometry = workbench.value("""
                    (() => { const task=document.querySelector('#workbench-task-area');
                      const canvas=document.querySelector('.canvas-region');
                      const wrap=document.querySelector('.matrix-wrap');
                      const inspector=document.querySelector('#workbench-inspector');
                      task.scrollTop=%s;
                      const bounds=task.getBoundingClientRect();
                      const action=document.querySelector('%s').getBoundingClientRect();
                      return {taskHeight:bounds.height, taskTop:bounds.top, taskBottom:bounds.bottom,
                        canvasBottom:canvas.getBoundingClientRect().bottom,
                        inspectorLeft:inspector.getBoundingClientRect().left,
                        canvasRight:canvas.getBoundingClientRect().right,
                        visible:wrap.clientHeight,
                        heading:document.querySelector('.week-matrix thead').getBoundingClientRect().height,
                        row:[...document.querySelectorAll('.week-matrix tbody tr')]
                          .find(row => !row.hidden && row.getBoundingClientRect().height > 0)
                          ?.getBoundingClientRect().height || 0,
                        page:document.documentElement.scrollWidth,
                        taskWidth:task.clientWidth, taskScrollWidth:task.scrollWidth,
                        actionTop:action.top, actionBottom:action.bottom,
                        actionLeft:action.left, actionRight:action.right,
                        taskLeft:bounds.left, taskRight:bounds.right}; })()"""
                    .formatted("SOLVING".equals(mode) ? "0" : "task.scrollHeight", actionSelector));
            double width = viewport[0], height = viewport[1];
            assertTrue(geometry.path("taskHeight").doubleValue() <= height * .35
                            && geometry.path("taskTop").doubleValue() >= geometry.path("canvasBottom").doubleValue()
                            && geometry.path("inspectorLeft").doubleValue() >= geometry.path("canvasRight").doubleValue()
                            && geometry.path("visible").doubleValue() >= geometry.path("heading").doubleValue() + geometry.path("row").doubleValue()
                            && geometry.path("page").doubleValue() <= width + 1
                            && geometry.path("taskScrollWidth").doubleValue() <= geometry.path("taskWidth").doubleValue() + 1
                            && geometry.path("actionTop").doubleValue() >= geometry.path("taskTop").doubleValue()
                            && geometry.path("actionBottom").doubleValue() <= geometry.path("taskBottom").doubleValue()
                            && geometry.path("actionBottom").doubleValue() <= height
                            && geometry.path("actionLeft").doubleValue() >= geometry.path("taskLeft").doubleValue()
                            && geometry.path("actionRight").doubleValue() <= geometry.path("taskRight").doubleValue(),
                    "UC-5 G3: " + mode + " must keep canvas, inspector and task action usable at " + viewport[0] + "px: " + geometry);
            workbench.expect("document.querySelector('[data-mode=" + mode + "]')?.getAttribute('aria-pressed') === 'true' && document.querySelector('.accepted-heading .state.accepted')?.textContent.includes('Current')",
                    "UC-5 G2: " + mode + " must keep Current and active mode identifiable");
            if ("PROPOSAL".equals(mode)) {
                JsonNode tiles = workbench.value("""
                        (() => { const buttons=[...document.querySelectorAll('.week-matrix .week-lesson:not([hidden])')]
                            .filter(button => button.getClientRects().length);
                          const inside=(child,parent) => child.left>=parent.left-1 && child.right<=parent.right+1
                            && child.top>=parent.top-1 && child.bottom<=parent.bottom+1;
                          const violations=[];
                          for (const button of buttons) {
                            const box=button.getBoundingClientRect();
                            const subject=button.querySelector('strong');
                            const room=button.querySelector('.week-room');
                            const labels=[...button.querySelectorAll('em:not([hidden])')]
                              .filter(label => label.getClientRects().length);
                            const bad=!subject || !room || !subject.textContent.trim() || !room.textContent.trim()
                              || subject.getBoundingClientRect().width<38 || room.getBoundingClientRect().width<38
                              || !inside(subject.getBoundingClientRect(),box) || !inside(room.getBoundingClientRect(),box)
                              || labels.some(label => !inside(label.getBoundingClientRect(),box)
                                || label.scrollWidth>label.clientWidth+2)
                              || button.scrollWidth>button.clientWidth+2;
                            if (bad && violations.length<5) violations.push({id:button.dataset.lessonId,
                              side:button.dataset.comparisonSide, subjectWidth:subject?.getBoundingClientRect().width,
                              roomWidth:room?.getBoundingClientRect().width,
                              room:room?.textContent, buttonWidth:box.width,
                              badges:labels.map(label => ({text:label.textContent,
                                width:label.getBoundingClientRect().width, scrollWidth:label.scrollWidth,
                                contained:inside(label.getBoundingClientRect(),box)}))});
                          }
                          return {checked:buttons.length,violations}; })()""");
                assertTrue(tiles.path("checked").intValue() >= 1_000 && tiles.path("violations").isEmpty(),
                        "UC-5 RULE-4: all Proposal Week tiles must visibly contain subject, room and every cue at " + viewport[0] + "px: " + tiles);
            }
            workbench.screenshot("uc5-" + screenshotPhase + "-" + viewport[0] + ".png");
        }
        assertEquals(beforePresentation, storedDocument(),
                "UC-5 G1/RULE-2: responsive workbench changes cannot mutate the workspace document or version");
    }
}
