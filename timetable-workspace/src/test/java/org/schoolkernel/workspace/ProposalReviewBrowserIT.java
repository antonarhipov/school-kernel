package org.schoolkernel.workspace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.schoolkernel.workspace.WorkbenchFixtures.jsonStrings;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/** UC-4 of the workbench specs: reviewing, navigating and deciding a repair proposal beside the timetable. */
class ProposalReviewBrowserIT extends WorkbenchBrowserSupport {
    private static final Duration SCALE = Duration.ofSeconds(20);
    private static final Duration VERIFIED_SOLVE = Duration.ofSeconds(70);

    @Test
    @DisplayName("UC-4 extensions 1b/1c/2a/3a/3b/4a/6c: comparison shapes and narrow read-only agenda in real browser")
    void comparesOneSidedAndSameSlotChanges() {
        storeProposal(fixtures.comparisonShapeDocument());
        String before = storedDocument();
        String rendered = workbench.open().awaitText("Unique changed lessons");
        assertTrue(rendered.contains("103"), "overlapping category membership counts each changed ID once");
        assertTrue(rendered.contains("Teacher changes"));
        workbench.expect("document.querySelector('[data-category=teacherChanges] span')?.textContent === '0' && document.querySelector('[data-category=forcedMoves] span')?.textContent === '1'",
                "empty categories and overlapping explanations retain independent meanings");
        workbench.expect("[...document.querySelectorAll('.proposal-facts div')].some(row => row.textContent.includes('Protected accepted assignments') && row.textContent.includes('1'))");
        workbench.expect("document.querySelectorAll('.matrix-wrap [data-lesson-id=lesson-100]').length === 1 && document.querySelector('[data-lesson-id=lesson-100]')?.dataset.comparisonSide === 'combined' && document.querySelector('[data-lesson-id=lesson-101]')?.dataset.comparisonSide === 'accepted' && document.querySelector('[data-lesson-id=lesson-added]')?.dataset.comparisonSide === 'proposed'",
                "UC-4 extensions 1b/1c: same-slot change, cancellation and addition have exactly their existing sides");
        workbench.selectLesson("lesson-100");
        workbench.expect("document.querySelector('.comparison-details')?.textContent.includes('Room 1') && document.querySelector('.comparison-details')?.textContent.includes('room-2 (Name unavailable)')",
                "UC-4 4a: missing proposal display name retains the stable room ID and unavailable-name cue");
        workbench.filterRoom("room-2");
        workbench.expect("!document.querySelector('[data-lesson-id=lesson-100]').hidden && document.querySelector('[data-lesson-id=lesson-100]')?.getAttribute('aria-label').includes('Proposed-side match') && document.querySelector('[data-lesson-id=lesson-100] .side-match-label')?.textContent === 'Proposed match' && document.querySelector('#represented-lesson-count')?.textContent.includes('61')",
                "UC-4 3a: proposal-only resource filter retains the changed lesson as one identity");
        workbench.resetView();
        workbench.selectLesson("lesson-101");
        workbench.expect("document.querySelector('.comparison-details')?.textContent.includes('Not present') && document.querySelector('.comparison-details')?.textContent.includes('Cancellations')");
        workbench.selectLesson("lesson-added");
        workbench.expect("document.querySelector('.comparison-details')?.textContent.includes('Additions') && document.querySelector('.comparison-details')?.textContent.includes('Not present')");
        workbench.mode("CURRENT");
        workbench.expect("document.querySelector('#inspection-notice')?.textContent.includes('not present') && !document.querySelector('#lesson-panel-title')",
                "UC-4 mode navigation clears a proposal-only identity without inventing it in Current");
        workbench.mode("PROPOSAL");
        workbench.selectLesson("lesson-200");
        workbench.expect("document.querySelector('.comparison-details')?.textContent.includes('Accepted and unchanged') && document.querySelector('.comparison-details')?.textContent.includes('Declared lesson 200')");
        workbench.viewport(390, 844);
        String agenda = workbench.awaitText("Read-only focused schedule");
        assertTrue(agenda.contains("Repair proposal") && agenda.contains("Accepted origin") && agenda.contains("Proposed destination"));
        workbench.expectNot("document.querySelector('#accept-repair, #revise-proposal, #discard-proposal, .matrix-wrap') !== null",
                "UC-4 6c: narrow proposal agenda must not expose mutation or full-desktop controls");
        assertEquals(before, storedDocument(), "comparison and narrow viewing cannot mutate durable workspace data");
    }

    @Test
    @DisplayName("UC-4 C-1: focusing either teacher or room retains the joined accepted/proposed lesson")
    void retainsBothSidesInFocusedResourceSchedules() {
        ObjectNode document = fixtures.comparisonShapeDocument();
        ObjectNode proposal = (ObjectNode) document.path("proposal");
        ObjectNode result = (ObjectNode) proposal.path("result");
        ((ObjectNode) result.path("timetable").path("assignments").get(0)).put("roomId", "room-90");
        ((ObjectNode) result.path("timetable").path("assignments").get(1)).put("teacherId", "teacher-21")
                .put("subjectId", "subject-1").put("cohortId", "cohort-21");
        ((ArrayNode) result.path("changeReport").path("teacherChanges")).addObject().put("lessonId", "lesson-1");
        proposal.set("review", reviews.create(document.path("acceptedBaseline"), document.path("repairDraft"),
                proposal.path("definition"), result));
        storeProposal(document);
        String durable = storedDocument();
        workbench.open().awaitText("Unique changed lessons");
        for (String[] focus : new String[][] {
                { "teacherId", "teacher-0", "teacher-21", "lesson-1" },
                { "roomId", "room-0", "room-90", "lesson-0" } }) {
            if (focus[0].equals("teacherId")) workbench.filterTeacher(focus[1]);
            else workbench.filterRoom(focus[1]);
            workbench.openFocus(focus[0]);
            for (String resource : new String[] { focus[1], focus[2] }) {
                workbench.focusEntity(resource);
                String selector = ".focused-schedule [data-lesson-id=" + focus[3] + "]";
                workbench.expect("document.querySelectorAll('" + selector + "').length === 2 && document.querySelector('" + selector + "[data-comparison-side=accepted]') && document.querySelector('" + selector + "[data-comparison-side=proposed]') && [...document.querySelectorAll('" + selector + "')].some(item => item.textContent.includes('Related comparison side'))",
                        "UC-4 C-1: old/new " + focus[0] + " focus " + resource + " retains both exact sides without calling both current");
            }
            workbench.returnToMatrix();
            workbench.resetView();
        }
        String lessonOneSides = "[...document.querySelectorAll('.matrix-wrap [data-lesson-id=lesson-1]')]";
        workbench.investigateSubject("subject-1");
        workbench.expect(lessonOneSides + ".every(el=>!el.hidden && el.getAttribute('aria-label').includes('Proposed-side match') && el.querySelector('.side-match-label')?.textContent === 'Proposed match')",
                "UC-4 3a: proposed-only subject investigation retains accepted origin");
        workbench.investigateSubject("subject-0");
        workbench.expect(lessonOneSides + ".every(el=>!el.hidden && el.getAttribute('aria-label').includes('Accepted-side match') && el.querySelector('.side-match-label')?.textContent === 'Accepted match')",
                "UC-4 3a: accepted-only subject investigation retains proposed destination");
        workbench.resetView();
        workbench.filterClass("cohort-21");
        workbench.expect(lessonOneSides + ".every(el=>!el.hidden && el.getAttribute('aria-label').includes('Proposed-side match') && el.querySelector('.side-match-label')?.textContent === 'Proposed match')",
                "UC-4 3a: one-sided class filter retains both placements");
        workbench.resetView();
        workbench.search("Room 90");
        workbench.expect("[...document.querySelectorAll('.matrix-wrap [data-lesson-id=lesson-0]')].every(el=>!el.hidden && el.getAttribute('aria-label').includes('Proposed-side match') && el.querySelector('.side-match-label')?.textContent === 'Proposed match') && document.querySelector('#search-summary')?.textContent.includes('1')",
                "UC-4 3a: a proposed-only search name highlights exactly one joined identity and retains the accepted origin");
        assertEquals(durable, storedDocument());
    }

    @Test
    @DisplayName("UC-4 G-1/G-2 and RULE-9/12: full verified 1,000-lesson repair has exact two-sided overlay, review and durable decision")
    void reviewsIndependentlyVerifiedNormativeRepair() throws Exception {
        ObjectNode document = fixtures.investigationScaleDocument();
        JsonNode baseline = document.path("acceptedBaseline").deepCopy();
        assertEquals(60, baseline.path("definition").path("cohorts").size());
        assertEquals(100, baseline.path("definition").path("teachers").size());
        assertEquals(100, baseline.path("definition").path("rooms").size());
        assertEquals(60, baseline.path("definition").path("periods").size());
        assertEquals(1_000, baseline.path("result").path("timetable").path("assignments").size());
        assertEquals("Subject Zero with a deliberately long authoritative display name for timetable tiles",
                baseline.path("definition").path("subjects").get(0).path("displayName").stringValue());
        assertEquals("Room Sixteen with a deliberately long authoritative display name for timetable tiles",
                baseline.path("definition").path("rooms").get(16).path("displayName").stringValue());
        assertEquals(document.path("timetableRevision").stringValue(), verifier.verify(new ImportDocuments(
                baseline.path("definition"), baseline.path("result"), null,
                ImportDocuments.ImportMode.ACCEPTED_BASELINE)).timetableRevision());
        storeAccepted(document);
        AtomicReference<ObjectNode> independentlyVerified = new AtomicReference<>();
        processes.verifiedFeasibleResult = arguments -> {
            ObjectNode result = fixtures.verifiedNormativeRepairResult(arguments);
            independentlyVerified.set(result.deepCopy());
            return result;
        };

        workbench.open().awaitText("Showing 60 of 60 classes", SCALE);
        workbench.startRepair(null, "teacher-16", "period-0");
        workbench.awaitText("Repair draft · not current", SCALE);
        assertEquals(Set.of("lesson-960"), jsonStrings(storedWorkspaceDocument().path("repairDraft").path("directEffectLessonIds")));
        workbench.pin("lesson-500", true, true);
        workbench.awaitText("Accepted room pinned");
        JsonNode draft = assertDraftUnchangedBaseline(baseline).deepCopy();
        assertEquals("lesson-500", draft.path("intent").path("pins").get(0).path("lessonId").stringValue());
        assertEquals(JSON.readTree("[\"INDIVIDUAL\"]"), draft.path("intent").path("pins").get(0).path("roomSources"));
        assertTrue(draft.path("readyToSolve").booleanValue());
        workbench.selectLesson("lesson-0");
        workbench.range("DAY");
        workbench.awaitText("Day · Monday");
        workbench.click("#solve-draft");
        try {
            workbench.awaitText("Repair proposal · feasible", VERIFIED_SOLVE);
        } catch (AssertionError failure) {
            throw new AssertionError("The normative candidate must pass the independent production verifier: "
                    + processes.verifiedFeasibleFailure, processes.verifiedFeasibleFailure == null ? failure : processes.verifiedFeasibleFailure);
        }
        Workbench.MutationLog mutations = workbench.recordMutations();
        JsonNode stored = storedWorkspaceDocument();
        assertEquals("REPAIR_PROPOSAL", storedLifecycle());
        assertEquals(baseline, stored.path("acceptedBaseline"));
        assertEquals(draft, stored.path("repairDraft"));
        JsonNode proposal = stored.path("proposal");
        assertEquals(independentlyVerified.get(), proposal.path("result"),
                "the real planner must persist only the result independently verified by the production kernel");
        assertNormativeReview(proposal.path("review"));

        workbench.expect("document.querySelector('[data-range=DAY]')?.getAttribute('aria-pressed') === 'true' && document.querySelector('#lesson-panel-title')?.textContent.includes('Declared lesson 0') && document.querySelector('.impact-totals')?.textContent.includes('1')");
        assertEquals(JSON.readTree("""
                [["additions",0],["cancellations",0],["teacherChanges",0],["forcedMoves",0],
                ["periodMoves",1],["roomOnlyMoves",1]]
                """), workbench.value("[...document.querySelectorAll('.review-category')].map(c=>[c.dataset.category,Number(c.querySelector('h4 span').textContent)])"));
        assertEquals(JSON.readTree("""
                [["Unique changed lessons","2"],["Protected accepted assignments","1"],
                ["Termination reason","TIME_LIMIT"],["Execution limit","PT1M"],["Elapsed time","2004 ms"]]
                """), workbench.value("[...document.querySelectorAll('.proposal-facts div')].map(row=>[row.querySelector('dt').textContent,row.querySelector('dd').textContent])"));
        assertEquals(JSON.readTree("[\"Direct effects of your intent: 1\",\"Solver ripple effects: 1\"]"),
                workbench.value("[...document.querySelectorAll('.impact-totals span')].map(el=>el.textContent)"));
        assertEquals(JSON.readTree("[\"Protected accepted assignments · 1\",\"By class · 2\",\"By teacher · 2\",\"By room · 3\",\"By day · 2\"]"),
                workbench.value("[...document.querySelectorAll('.review-groups summary')].map(el=>el.textContent)"));
        workbench.investigateTeacher("teacher-16");
        workbench.expect("document.querySelector('#teacher-ribbon-title')?.textContent.includes('Proposed teacher load by period') && [...document.querySelectorAll('.teacher-ribbon li')].find(el=>el.querySelector('strong')?.textContent==='Declared period 0')?.querySelector('span')?.textContent==='Unavailable'",
                "RULE-6: proposal Monday availability must use the successor definition");
        workbench.mode("CURRENT");
        workbench.expect("document.querySelector('#teacher-ribbon-title')?.textContent.includes('Teacher load by period') && !document.querySelector('#teacher-ribbon-title')?.textContent.includes('Proposed') && [...document.querySelectorAll('.teacher-ribbon li')].find(el=>el.querySelector('strong')?.textContent==='Declared period 0')?.querySelector('span')?.textContent==='Assigned'",
                "RULE-6: accepted Monday load must still use the accepted definition and assignment");
        workbench.mode("PROPOSAL");
        workbench.investigateTeacher("");
        workbench.range("WEEK");
        workbench.awaitText("Complete recurring Week");
        var expectedTiles = new ArrayList<String>();
        for (int i = 0; i < 1_000; i++) {
            String id = "lesson-" + i;
            if (i == 960) { expectedTiles.add(id + ":accepted"); expectedTiles.add(id + ":proposed"); }
            else if (i == 0) expectedTiles.add(id + ":combined");
            else expectedTiles.add(id + ":unchanged");
        }
        expectedTiles.sort(String::compareTo);
        assertEquals(JSON.valueToTree(expectedTiles), workbench.value("[...document.querySelectorAll('.matrix-wrap [data-lesson-id]')].map(el=>el.dataset.lessonId+':'+el.dataset.comparisonSide).sort()"),
                "all 1,000 stable IDs and exactly one additional period-move side must be present");
        workbench.click("[data-lesson-id=lesson-960][data-comparison-side=accepted]");
        workbench.expect("document.querySelector('.comparison-details')?.textContent.includes('period-0') && document.querySelector('.comparison-details')?.textContent.includes('period-40') && document.querySelector('.comparison-details')?.textContent.includes('room-16') && document.querySelector('.comparison-details')?.textContent.includes('Directly affected')");
        assertDisplayedComparisonSides("""
                [["Old assignment",[["Weekday","Monday",null],
                ["Subject","Subject Zero with a deliberately long authoritative display name for timetable tiles","subject-0"],
                ["Class","Class Sixteen with a deliberately long authoritative display name for timetable tiles","cohort-16"],
                ["Teacher","Teacher Sixteen with a deliberately long authoritative display name for timetable tiles","teacher-16"],
                ["Period · Changed","Declared period 0","period-0"],
                ["Room","Room Sixteen with a deliberately long authoritative display name for timetable tiles","room-16"]]],
                ["Proposed assignment",[["Weekday","Thursday",null],
                ["Subject","Subject Zero with a deliberately long authoritative display name for timetable tiles","subject-0"],
                ["Class","Class Sixteen with a deliberately long authoritative display name for timetable tiles","cohort-16"],
                ["Teacher","Teacher Sixteen with a deliberately long authoritative display name for timetable tiles","teacher-16"],
                ["Period · Changed","Declared period 40","period-40"],
                ["Room","Room Sixteen with a deliberately long authoritative display name for timetable tiles","room-16"]]]]
                """);
        workbench.click("[data-lesson-id=lesson-0][data-comparison-side=combined]");
        workbench.expect("document.querySelector('.comparison-details')?.textContent.includes('room-0') && document.querySelector('.comparison-details')?.textContent.includes('room-50') && document.querySelector('.comparison-details')?.textContent.includes('Solver ripple')");
        assertDisplayedComparisonSides("""
                [["Old assignment",[["Weekday","Monday",null],
                ["Subject","Subject Zero with a deliberately long authoritative display name for timetable tiles","subject-0"],
                ["Class","Class 0","cohort-0"],["Teacher","Teacher 0","teacher-0"],
                ["Period","Declared period 0","period-0"],["Room · Changed","Room 0","room-0"]]],
                ["Proposed assignment",[["Weekday","Monday",null],
                ["Subject","Subject Zero with a deliberately long authoritative display name for timetable tiles","subject-0"],
                ["Class","Class 0","cohort-0"],["Teacher","Teacher 0","teacher-0"],
                ["Period","Declared period 0","period-0"],["Room · Changed","Room 50","room-50"]]]]
                """);
        workbench.expect("document.querySelector('#review-selection .before-after')?.textContent.includes('Room 0') && document.querySelector('#review-selection .before-after')?.textContent.includes('Room 50') && document.querySelector('#review-selection')?.textContent.includes('Solver ripple effects')",
                "UC-4 main 4: the wide review area repeats the exact selected before/after and effect explanation");

        String origin = "[data-category=periodMoves] [data-review-lesson=lesson-960][data-review-side=accepted]";
        String destination = "[data-category=periodMoves] [data-review-lesson=lesson-960][data-review-side=proposed]";
        workbench.click(origin);
        workbench.expect("document.querySelector('#lesson-panel-title')?.textContent === 'Declared lesson 960' && document.querySelector('#review-selection')?.textContent.includes('Review target: Accepted origin · current') && document.querySelector('#review-selection .before-after')?.textContent.includes('Declared period 40')",
                "UC-4 main 3-4: category origin navigation selects one stable identity and keeps the destination details in the task area");
        workbench.range("DAY");
        workbench.focusPeriod("period-0");
        workbench.click(destination);
        workbench.expect("document.querySelector('#range-summary')?.textContent.includes('Thursday') && document.querySelector('#period-focus')?.value === '' && !document.querySelector('[data-lesson-id=lesson-960][data-comparison-side=proposed]')?.hidden && document.querySelector('#inspection-notice')?.textContent.includes('Cleared Period focus')",
                "UC-4 extension 3a: destination navigation clears an origin-only Day period column");
        workbench.click("#review-selection [data-review-lesson=lesson-960][data-review-side=accepted]");
        workbench.filterRoom("room-50");
        workbench.search("Room 50");
        workbench.investigateTeacher("teacher-16");
        workbench.expect("document.querySelector('#room-filter')?.value === 'room-50' && document.querySelector('#range-summary')?.textContent.includes('Monday')");
        workbench.click(destination);
        workbench.expect("document.querySelector('#range-summary')?.textContent.includes('Thursday') && document.querySelector('#room-filter')?.value === '' && document.querySelector('#lesson-search')?.value === 'Room 50' && document.querySelector('#teacher-investigation')?.value === 'teacher-16' && document.querySelector('#lesson-panel-title')?.textContent === 'Declared lesson 960' && document.querySelector('#review-selection')?.textContent.includes('Review target: Proposed destination · not current') && !document.querySelector('[data-lesson-id=lesson-960][data-comparison-side=proposed]')?.hidden && document.querySelector('#inspection-notice')?.textContent.includes('Selected Thursday') && document.querySelector('#inspection-notice')?.textContent.includes('Cleared Room filter')",
                "UC-4 extension 3a: destination navigation changes only Day and excluding room filter, with search and highlight retained");
        workbench.click("#review-selection [data-review-lesson=lesson-960][data-review-side=accepted]");
        workbench.expect("document.querySelector('#range-summary')?.textContent.includes('Monday') && document.querySelector('#lesson-panel-title')?.textContent === 'Declared lesson 960' && document.querySelector('#review-selection')?.textContent.includes('Review target: Accepted origin · current') && document.querySelector('#lesson-search')?.value === 'Room 50'",
                "UC-4 main 3: review detail can navigate back to accepted origin without losing the investigation");
        workbench.range("WEEK");
        workbench.filterClass("cohort-0");
        workbench.openFocus("cohortId");
        workbench.expect("document.querySelector('.focused-schedule') !== null", "UC-4 focused entry must open the selected class schedule");
        workbench.click(".review-groups >> nth=1 >> [data-review-lesson=lesson-960][data-review-side=proposed]");
        workbench.expect("!document.querySelector('.focused-schedule') && document.querySelector('.matrix-wrap [data-lesson-id=lesson-960][data-comparison-side=proposed]') && document.querySelector('#review-selection')?.textContent.includes('Review target: Proposed destination · not current') && document.querySelector('#lesson-search')?.value === 'Room 50' && document.querySelector('#inspection-notice')?.textContent.includes('Returned to the whole-school canvas')",
                "UC-4 extension 3a: grouping navigation leaves only an excluding focus and retains the search and stable lesson identity");
        workbench.openFocus("cohortId");
        workbench.click(origin);
        workbench.expect("document.querySelector('.focused-schedule [data-lesson-id=lesson-960][data-comparison-side=accepted].selected[aria-current=true] .selected-label')?.textContent === 'Selected' && document.querySelector('#review-selection')?.textContent.includes('Review target: Accepted origin · current') && document.querySelector('#lesson-panel-title')?.textContent === 'Declared lesson 960'",
                "UC-4 extension 3a: an in-focus accepted origin remains in the focused schedule with an explicit selected cue");
        workbench.returnToMatrix();
        workbench.expect("document.querySelector('#lesson-search')?.value === 'Room 50'", "UC-4 G4: focused review navigation and return retain the search");
        workbench.resetView();
        workbench.range("WEEK");
        workbench.click("[data-review-lesson=lesson-500]");
        workbench.expect("document.querySelector('.comparison-details')?.textContent.includes('Accepted and unchanged') && document.querySelector('.comparison-details')?.textContent.includes('Accepted room pinned')");
        workbench.expect("document.querySelector('.matrix-wrap [data-lesson-id=lesson-500] .pin-label')?.textContent === 'Both pinned' && document.querySelector('.matrix-wrap [data-lesson-id=lesson-500]')?.getAttribute('aria-label').includes('Accepted period pinned · Accepted room pinned')",
                "UC-4 G5: compact Week protection remains visible and exposes its full accessible meaning");
        workbench.expect("document.querySelector('.matrix-wrap [data-lesson-id=lesson-500] strong')?.getBoundingClientRect().width >= 38 && document.querySelector('.matrix-wrap [data-lesson-id=lesson-500] .week-room')?.getBoundingClientRect().width > 0 && document.querySelector('.matrix-wrap [data-lesson-id=lesson-500]')?.getAttribute('aria-pressed') === 'true'",
                "UC-4 G5: a selected protected Week tile keeps subject and room visible with structural selection");
        assertEquals("room-8", proposal.path("result").path("timetable").path("assignments").get(500).path("roomId").stringValue());
        assertEquals("period-20", proposal.path("result").path("timetable").path("assignments").get(500).path("periodId").stringValue());
        assertEquals(stored, storedWorkspaceDocument(), "inspection and protected-lesson navigation must not change the accepted/draft/proposal bundle");

        // Reaching the period, room and class filters opened Filters; with it open, the wide task area no longer fits (G3).
        workbench.closeDisclosure("#filters");
        for (int[] viewport : new int[][] { { 1600, 900 }, { 1280, 800 }, { 1279, 800 }, { 701, 844 } }) {
            workbench.viewport(viewport[0], viewport[1]);
            workbench.scrollToTop();
            JsonNode geometry = workbench.value("""
                    (() => { const task=document.querySelector('#workbench-task-area');
                      const canvas=document.querySelector('.canvas-region'); const wrap=document.querySelector('.matrix-wrap');
                      const inspector=document.querySelector('#workbench-inspector');
                      task.scrollTop=task.scrollHeight;
                      const bounds=task.getBoundingClientRect();
                      return {taskHeight:bounds.height, taskBottom:bounds.bottom, taskTop:bounds.top,
                        canvasBottom:canvas.getBoundingClientRect().bottom,
                        inspectorTop:inspector.getBoundingClientRect().top,
                        inspectorBottom:inspector.getBoundingClientRect().bottom,
                        inspectorLeft:inspector.getBoundingClientRect().left,
                        canvasRight:canvas.getBoundingClientRect().right,
                        visible:wrap.clientHeight, heading:document.querySelector('.week-matrix thead').getBoundingClientRect().height,
                        row:document.querySelector('.week-matrix tbody tr').getBoundingClientRect().height,
                        page:document.documentElement.scrollWidth,
                        taskClientWidth:task.clientWidth, taskScrollWidth:task.scrollWidth,
                        decisions:['#accept-repair','#revise-proposal','#discard-proposal'].every(selector => {
                          const action=document.querySelector(selector).getBoundingClientRect();
                          return action.top >= bounds.top && action.bottom <= bounds.bottom
                            && action.left >= bounds.left && action.right <= bounds.right;
                        })}; })()""");
            assertTrue(geometry.path("taskHeight").doubleValue() <= viewport[1] * .35
                            && geometry.path("visible").doubleValue() >= geometry.path("heading").doubleValue() + geometry.path("row").doubleValue()
                            && geometry.path("page").doubleValue() <= viewport[0] + 1
                            && geometry.path("taskScrollWidth").doubleValue() <= geometry.path("taskClientWidth").doubleValue() + 1
                            && geometry.path("decisions").booleanValue(),
                    "UC-4 G3: Proposal review controls, headings and a full class row fit without horizontal page/task clipping at " + viewport[0] + ": " + geometry);
            if (viewport[0] >= 1280) {
                assertTrue(geometry.path("taskBottom").doubleValue() <= viewport[1]
                                && geometry.path("taskTop").doubleValue() >= geometry.path("canvasBottom").doubleValue()
                                && geometry.path("inspectorLeft").doubleValue() >= geometry.path("canvasRight").doubleValue(),
                        "UC-4 G3: wide Proposal keeps canvas and inspector beside its below-canvas task at " + viewport[0] + ": " + geometry);
                workbench.screenshot("uc4-proposal-" + viewport[0] + ".png");
            } else {
                assertTrue(geometry.path("inspectorTop").doubleValue() >= geometry.path("canvasBottom").doubleValue()
                                && geometry.path("taskTop").doubleValue() >= geometry.path("inspectorBottom").doubleValue(),
                        "UC-4 G3/RULE-5: Proposal inspector stacks between the canvas and wide task at " + viewport[0] + ": " + geometry);
            }
        }
        for (int[] viewport : new int[][] { { 700, 844 }, { 390, 844 } }) {
            workbench.viewport(viewport[0], viewport[1]);
            workbench.expect("document.querySelector('.focused-schedule') !== null && document.querySelector('#workbench-task-area') === null");
            workbench.focusEntity("cohort-16");
            workbench.expect("document.body.innerText.includes('Repair proposal') && document.body.innerText.includes('Accepted baseline remains current') && document.body.innerText.includes('Read-only focused schedule') && document.querySelectorAll('.focused-schedule [data-lesson-id=lesson-960]').length === 2 && document.querySelector('.focused-schedule [data-lesson-id=lesson-960][data-comparison-side=accepted]')?.textContent.includes('Accepted origin') && document.querySelector('.focused-schedule [data-lesson-id=lesson-960][data-comparison-side=proposed]')?.textContent.includes('Proposed destination') && !document.querySelector('#accept-repair, #revise-proposal, #discard-proposal, #workbench-task-area, .matrix-wrap') && document.documentElement.scrollWidth <= innerWidth",
                    "UC-4 extension 1b/G7: verified Proposal at " + viewport[0] + "px is a read-only accepted/proposed agenda without page overflow or decisions");
            assertEquals(stored, storedWorkspaceDocument(), "responsive Proposal reading must not change the durable workspace");
        }
        workbench.viewport(1280, 800);
        workbench.expect("document.querySelector('.matrix-wrap') !== null && document.querySelector('#workbench-task-area') !== null");
        JsonNode beforeCollapseScroll = workbench.value("(() => { const matrix=document.querySelector('.matrix-wrap'); matrix.scrollTo(140,120); return {left:matrix.scrollLeft, top:matrix.scrollTop}; })()");
        assertTrue(beforeCollapseScroll.path("left").intValue() > 0 && beforeCollapseScroll.path("top").intValue() > 0,
                "UC-4 G3: the Proposal matrix must have a real horizontal and vertical scroll position before collapse: " + beforeCollapseScroll);
        workbench.click("#collapse-proposal-task");
        workbench.expect("document.querySelector('#workbench-task-area').hidden && document.querySelector('#lesson-panel-title')?.textContent === 'Declared lesson 500'");
        workbench.click("#reopen-proposal-task");
        workbench.expect("!document.querySelector('#workbench-task-area').hidden && document.querySelector('#review-selection')?.textContent.includes('Declared lesson 500') && document.querySelector('.proposal-facts')?.textContent.includes('Unique changed lessons')",
                "UC-4 G3: task collapse and reopen retain selected protection and authoritative counts");
        assertEquals(beforeCollapseScroll, workbench.value("(() => { const matrix=document.querySelector('.matrix-wrap'); return {left:matrix.scrollLeft, top:matrix.scrollTop}; })()"),
                "UC-4 G3: task collapse and reopen retain the representable matrix scroll position");
        assertEquals(List.of(), mutations.requests(),
                "UC-4 G4/RULE-2: review navigation, mode, search, filter, focus, responsive and task actions issue no mutating request");
        assertEquals(stored, storedWorkspaceDocument(), "UC-4 G4: review-only actions keep the exact accepted/Draft/Proposal document and version");
        workbench.selectLesson("lesson-0");
        workbench.range("DAY");
        workbench.expect("document.querySelector('[data-range=DAY]')?.getAttribute('aria-pressed') === 'true' && document.querySelector('#lesson-panel-title')?.textContent.includes('Declared lesson 0')");
        workbench.check("#confirm-repair-accept");
        assertEquals(List.of(), mutations.requests(), "UC-4 G6: confirmation alone cannot send an acceptance request");
        workbench.click("#accept-repair");
        workbench.awaitText("Accepted baseline · current timetable", SCALE);
        assertEquals(List.of("POST /api/proposal/accept"), mutations.requests(),
                "UC-4 G4/G6: only the explicit confirmed acceptance issues a mutation request");
        JsonNode accepted = storedWorkspaceDocument();
        assertEquals("ACCEPTED_BASELINE", storedLifecycle());
        assertEquals(proposal.path("definition"), accepted.path("acceptedBaseline").path("definition"));
        assertEquals(proposal.path("result"), accepted.path("acceptedBaseline").path("result"));
        assertFalse(accepted.has("proposal"));
        assertFalse(accepted.has("repairDraft"));
        workbench.expect("document.querySelector('[data-range=DAY]')?.getAttribute('aria-pressed') === 'true' && document.querySelector('#lesson-panel-title')?.textContent.includes('Declared lesson 0') && !document.querySelector('[data-mode=PROPOSAL]')",
                "accepted Monday and unchanged lesson-0 selection remain representable on the exact new Current baseline");
    }

    @Test
    @DisplayName("UC-4 G-1: revise and discard two independently verified whole-school proposals without advancing Current")
    void revisesAndDiscardsVerifiedNormativeRepair() throws Exception {
        processes.verifiedFeasibleResult = fixtures::verifiedNormativeRepairResult;
        for (String decision : new String[] { "#revise-proposal", "#discard-proposal" }) {
            ObjectNode document = fixtures.investigationScaleDocument();
            JsonNode baseline = document.path("acceptedBaseline").deepCopy();
            storeAccepted(document);
            workbench.open().awaitText("Complete school population", SCALE);
            workbench.startRepair(null, "teacher-16", "period-0");
            workbench.awaitText("Repair draft · not current", SCALE);
            workbench.pin("lesson-500", true, true);
            workbench.awaitText("Accepted room pinned");
            JsonNode draft = storedWorkspaceDocument().path("repairDraft").deepCopy();
            workbench.click("#solve-draft");
            workbench.awaitText("Repair proposal · feasible", VERIFIED_SOLVE);
            JsonNode before = storedWorkspaceDocument();
            assertEquals(baseline, before.path("acceptedBaseline"));
            assertEquals(draft, before.path("repairDraft"));
            assertEquals(2, before.path("proposal").path("review").path("uniqueChangedLessonCount").intValue());
            assertEquals("sha256:", before.path("proposal").path("proposedTimetableRevision").stringValue().substring(0, 7));
            workbench.click(decision);
            workbench.awaitText("Repair draft · not current");
            JsonNode after = storedWorkspaceDocument();
            assertEquals("REPAIR_DRAFT", storedLifecycle());
            assertEquals(baseline, after.path("acceptedBaseline"));
            assertEquals(draft, after.path("repairDraft"));
            assertFalse(after.has("proposal"));
            workbench.expect("document.querySelector('[data-mode=DRAFT]')?.getAttribute('aria-pressed') === 'true' && !document.querySelector('[data-mode=PROPOSAL], #accept-repair')");
            workbench.expect("!document.querySelector('#workbench-task-area')?.hidden && document.querySelector('#workbench-task-area #stage-repair-form') && document.querySelector('#workbench-task-area').textContent.includes('Teacher Sixteen') && document.querySelector('#workbench-task-area').textContent.includes('Attempt-scoped pins') && !document.querySelector('#workbench-inspector #stage-repair-form')",
                    "Workbench layout UC-2 ext 1c: verified proposal " + decision + " reopens the same saved Draft in the wide task area");
        }
    }

    @Test
    @DisplayName("UC-4 extension 2a: a zero-change review retains empty categories and groupings without publishing a proposal")
    void displaysEmptyComparisonGroups() {
        ObjectNode document = fixtures.scaleProposalDocument();
        ObjectNode proposal = (ObjectNode) document.path("proposal");
        ObjectNode result = (ObjectNode) proposal.path("result");
        ((ObjectNode) result.path("timetable")).set("assignments", document.path("acceptedBaseline").path("result").path("timetable").path("assignments").deepCopy());
        ((ArrayNode) result.path("changeReport").path("periodMoves")).removeAll();
        proposal.set("review", reviews.create(document.path("acceptedBaseline"), document.path("repairDraft"),
                proposal.path("definition"), result));
        storeProposal(document);
        String durable = storedDocument();
        workbench.open().awaitText("Unique changed lessons");
        assertEquals(JSON.readTree("[\"By class · 0\",\"By teacher · 0\",\"By room · 0\",\"By day · 0\"]"),
                workbench.value("[...document.querySelectorAll('.review-groups summary')].slice(1).map(el=>el.textContent)"));
        workbench.expect("[...document.querySelectorAll('.review-category')].every(el=>el.querySelector('h4 span').textContent==='0' && el.textContent.includes('No lessons in this category')) && [...document.querySelectorAll('.review-groups')].slice(1).every(el=>el.textContent.includes('No lessons in this category')) && document.querySelectorAll('.matrix-wrap [data-comparison-side=unchanged]').length === 1000");
        assertEquals(durable, storedDocument(), "supplemental UI-only zero-group snapshot must not advance Current");
    }

    @Test
    @DisplayName("UC-4 extensions 5a/5b/6a: browser revision, discard and stale acceptance preserve exact accepted bundle")
    void revisesDiscardsAndRejectsStaleProposal() {
        for (String action : new String[] { "#revise-proposal", "#discard-proposal" }) {
            storeProposal(fixtures.scaleProposalDocument());
            JsonNode before = storedWorkspaceDocument();
            workbench.open().awaitText("Unique changed lessons");
            workbench.click(action);
            workbench.awaitText("Repair draft · not current");
            JsonNode after = storedWorkspaceDocument();
            assertEquals("REPAIR_DRAFT", storedLifecycle());
            assertEquals(before.path("acceptedBaseline"), after.path("acceptedBaseline"));
            assertEquals(before.path("repairDraft"), after.path("repairDraft"));
            assertFalse(after.has("proposal"));
            workbench.expect("document.querySelector('[data-mode=DRAFT]')?.getAttribute('aria-pressed') === 'true' && !document.querySelector('[data-mode=PROPOSAL]') && !document.querySelector('#accept-repair')");
        }
        ObjectNode stale = fixtures.scaleProposalDocument();
        ((ObjectNode) stale.path("proposal")).put("sourceWorkspaceVersion", -1);
        storeProposal(stale);
        JsonNode before = storedWorkspaceDocument();
        workbench.open().awaitText("Unique changed lessons");
        workbench.acceptRepair();
        assertTrue(workbench.awaitText("no longer matches").contains("Repair draft · not current"),
                "UC-4 6a: stale identity must be refused and proposal presentation removed");
        JsonNode after = storedWorkspaceDocument();
        assertEquals("REPAIR_DRAFT", storedLifecycle());
        assertEquals(before.path("acceptedBaseline"), after.path("acceptedBaseline"));
        assertEquals(before.path("repairDraft"), after.path("repairDraft"));
        assertFalse(after.has("proposal"));
        workbench.expect("document.querySelector('[data-mode=DRAFT]')?.getAttribute('aria-pressed') === 'true' && !document.querySelector('#accept-repair')");
    }

    /** The persisted review must describe exactly the independently verified two-lesson change. */
    private static void assertNormativeReview(JsonNode review) {
        assertEquals(2, review.path("uniqueChangedLessonCount").intValue());
        assertEquals(1, review.path("directEffectChangedCount").intValue());
        assertEquals(1, review.path("rippleEffectCount").intValue());
        assertEquals(JSON.readTree("[\"lesson-960\"]"), review.path("directEffectLessonIds"));
        assertEquals(JSON.readTree("""
                [{"id":"additions","count":0,"lessonIds":[]},{"id":"cancellations","count":0,"lessonIds":[]},
                {"id":"teacherChanges","count":0,"lessonIds":[]},{"id":"forcedMoves","count":0,"lessonIds":[]},
                {"id":"periodMoves","count":1,"lessonIds":["lesson-960"]},
                {"id":"roomOnlyMoves","count":1,"lessonIds":["lesson-0"]}]
                """), review.path("categories"));
        assertEquals(JSON.readTree("""
                [{"lessonId":"lesson-0","directEffect":false,"rippleEffect":true,"categories":["roomOnlyMoves"],
                "old":{"subjectId":"subject-0","cohortId":"cohort-0","teacherId":"teacher-0","periodId":"period-0","roomId":"room-0"},
                "proposed":{"subjectId":"subject-0","cohortId":"cohort-0","teacherId":"teacher-0","periodId":"period-0","roomId":"room-50"},"changedDimensions":["roomId"]},
                {"lessonId":"lesson-960","directEffect":true,"rippleEffect":false,"categories":["periodMoves"],
                "old":{"subjectId":"subject-0","cohortId":"cohort-16","teacherId":"teacher-16","periodId":"period-0","roomId":"room-16"},
                "proposed":{"subjectId":"subject-0","cohortId":"cohort-16","teacherId":"teacher-16","periodId":"period-40","roomId":"room-16"},"changedDimensions":["periodId"]}]
                """), review.path("changedLessons"));
        assertEquals(JSON.readTree("""
                {"classes":[{"id":"cohort-0","context":"BOTH","lessonIds":["lesson-0"]},
                {"id":"cohort-16","context":"BOTH","lessonIds":["lesson-960"]}],
                "teachers":[{"id":"teacher-0","context":"BOTH","lessonIds":["lesson-0"]},
                {"id":"teacher-16","context":"BOTH","lessonIds":["lesson-960"]}],
                "rooms":[{"id":"room-0","context":"OLD","lessonIds":["lesson-0"]},
                {"id":"room-16","context":"BOTH","lessonIds":["lesson-960"]},
                {"id":"room-50","context":"PROPOSED","lessonIds":["lesson-0"]}],
                "days":[{"id":"MONDAY","context":"BOTH","lessonIds":["lesson-0","lesson-960"]},
                {"id":"THURSDAY","context":"PROPOSED","lessonIds":["lesson-960"]}]}
                """), review.path("groupings"));
    }

    private void assertDisplayedComparisonSides(String expectedJson) {
        assertEquals(JSON.readTree(expectedJson), workbench.value("[...document.querySelectorAll('.comparison-details .before-after section')].map(side=>[side.querySelector('h5').textContent,[...side.querySelectorAll('dl > div')].map(row=>[row.querySelector('dt').textContent.trim(),row.querySelector('dd').firstChild.textContent.trim(),row.querySelector('dd small')?.textContent||null])])"),
                "UC-4 main 4: each displayed weekday, subject, class, teacher, period and room must match independently verified definition/result by name, stable ID and changed cue");
    }
}
