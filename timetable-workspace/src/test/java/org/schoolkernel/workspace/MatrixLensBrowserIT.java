package org.schoolkernel.workspace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import tools.jackson.databind.JsonNode;

/** Timetable matrix lenses: the Teacher and Room filters pivot the whole-school Week/Day matrix to one row group. */
class MatrixLensBrowserIT extends WorkbenchBrowserSupport {
    private static final String LONG = " with a deliberately long authoritative display name for timetable tiles";
    private static final String TEACHER16 = "Teacher Sixteen" + LONG;
    private static final String ROOM16 = "Room Sixteen" + LONG;
    private static final String CLASS16 = "Class Sixteen" + LONG;
    private static final String SUBJECT0 = "Subject Zero" + LONG;
    private static final String PREFERENCE = "school-kernel.inspection.v1.opaque-scale-school";
    private static final String VISIBLE_LESSONS =
            "[...document.querySelectorAll('#accepted-view .lesson-cell[data-lesson-id]:not([hidden])')].map(button => button.dataset.lessonId)";

    @Test
    @DisplayName("UC-1 main 1-6/G1-G4/ext 4c/RULE-1-6: teacher and room lenses pivot Week and Day to one row group with lens tiles")
    void pivotsTheMatrixToOneTeacherOrRoom() throws Exception {
        storeAccepted(fixtures.investigationScaleDocument());
        String before = storedDocument();
        workbench.open().awaitText("Showing 60 of 60 classes");
        Workbench.MutationLog mutations = workbench.recordMutations();

        // Main 1-3: choosing a teacher renders one lens row group naming the teacher and the lens type.
        workbench.filterTeacher("teacher-16");
        workbench.awaitText("Lens: Teacher · " + TEACHER16);
        assertLensRowGroup("week-matrix tbody", "TEACHER", "teacher-16", "Teacher · " + TEACHER16);
        assertEquals("Teacher", workbench.string("document.querySelector('.week-matrix thead th').textContent"),
                "UC-1 main 3: the row heading names the lens type instead of Class");

        // Main 4 / G2: every teacher-16 assignment (lessons 960-999), independent of scroll position.
        Set<String> teacherLessons = lessonRange(960, 999);
        assertEquals(teacherLessons, visibleLessons(), "UC-1 G2: the teacher lens represents every teacher-16 assignment");
        workbench.value("document.querySelector('.matrix-wrap').scrollTo(99999, 99999)");
        assertEquals(teacherLessons, visibleLessons(), "UC-1 G2: completeness does not depend on the scroll position");
        workbench.value("document.querySelector('.matrix-wrap').scrollTo(0, 0)");

        // Main 4: teacher-lens Week tile shows subject, room and class; the accessible name adds teacher, day, period, ID.
        assertEquals(List.of(SUBJECT0, ROOM16, CLASS16), tileFields("lesson-960"),
                "UC-1 main 4: teacher-lens Week tile is subject · room · class");
        String label = workbench.string("document.querySelector('[data-lesson-id=lesson-960]').getAttribute('aria-label')");
        for (String part : List.of(TEACHER16, "Monday", "Declared period 0", "lesson-960")) {
            assertTrue(label.contains(part), "UC-1 main 4: teacher-lens accessible name adds " + part + ": " + label);
        }

        // Main 4 / G3: teacher-16 declares periods 0-40, so empty 41-59 are unavailable and empty 40 is ordinary.
        assertEquals(periodTitles(41, 59), emptyCellTitles(true), "UC-1 main 4: exactly the empty undeclared periods are unavailable");
        assertEquals(Set.of("Declared period 40"), emptyCellTitles(false), "UC-1 G3: an empty declared period is ordinary");
        workbench.expect("[...document.querySelectorAll('.unavailable-cell:not([hidden])')].every(cue => cue.textContent === 'Unavailable' && cue.title === 'Outside declared availability')",
                "UC-1 G3/RULE-5: unavailability is a text cue, not color alone");

        // Main 5: narrowed status, lens entity, unique represented lessons, removable criterion.
        assertEquals("Filtered whole-school matrix", workbench.string("document.querySelector('#filter-title').textContent"));
        assertEquals("Lens: Teacher · " + TEACHER16, workbench.string("document.querySelector('#matrix-summary').textContent"));
        assertEquals("Represented lessons: 40", workbench.string("document.querySelector('#represented-lesson-count').textContent"));
        assertEquals("Remove Teacher: " + TEACHER16,
                workbench.string("document.querySelector('#active-criteria [data-remove-lens]').getAttribute('aria-label')"),
                "UC-1 main 5: the lens is a removable criterion");

        // G1: the same matrix surface; no focused surface, entry, or return control exists.
        workbench.expect("document.querySelector('.week-matrix') && !document.querySelector('.focused-schedule, #return-matrix, [data-open-focus], .focused-entry')",
                "UC-1 G1/RULE-8: the lens never replaces the matrix with a focused surface");

        // Main 3-4 in Day: one row with the selected weekday's periods and the teacher-lens Day tile.
        workbench.range("DAY");
        workbench.awaitText("Day · Monday");
        assertLensRowGroup("matrix:not(.week-matrix) tbody tr", "TEACHER", "teacher-16", "Teacher · " + TEACHER16);
        assertEquals("Teacher", workbench.string("document.querySelector('.matrix thead th').textContent"));
        assertEquals(lessonRange(960, 971), visibleLessons(), "UC-1 main 4: Day shows the teacher's Monday lessons");
        assertEquals(List.of(SUBJECT0, ROOM16, CLASS16), tileFields("lesson-960"),
                "UC-1 main 4: teacher-lens Day tile is subject · room · class");
        workbench.weekday("FRIDAY");
        workbench.awaitText("Day · Friday");
        assertEquals(Set.of(), visibleLessons(), "teacher-16 has no Friday lesson");
        assertEquals(periodTitles(48, 59), emptyCellTitles(true), "UC-1 main 4: every Friday period is outside the declared set");

        // Room lens: choosing a room clears the teacher lens (RULE-1), drops the constant room label, adds the teacher.
        workbench.range("WEEK");
        workbench.filterRoom("room-16");
        workbench.awaitText("Lens: Room · " + ROOM16);
        assertLensRowGroup("week-matrix tbody", "ROOM", "room-16", "Room · " + ROOM16);
        assertEquals("", workbench.string("document.querySelector('#teacher-filter').value"), "RULE-1: one lens at a time");
        workbench.expect("!document.querySelector('#active-criteria').textContent.includes('Teacher:') && document.querySelector('#active-criteria').textContent.includes('Room: " + ROOM16 + "')");
        assertEquals(teacherLessons, visibleLessons(), "UC-1 main 4: the room lens represents every room-16 assignment");
        assertEquals(List.of(SUBJECT0, TEACHER16, CLASS16), tileFields("lesson-960"),
                "UC-1 main 4: room-lens Week tile is subject · teacher · class");
        workbench.expect("!document.querySelector('[data-lesson-id=lesson-960] .week-room') && document.querySelector('[data-lesson-id=lesson-960]').getAttribute('aria-label').includes('" + ROOM16 + "')",
                "UC-1 main 4: the room label is dropped visibly but kept in the accessible name");
        // Ext 4c: room-16 declares no availability, so empty cells make no availability claim.
        assertEquals(Set.of(), emptyCellTitles(true), "UC-1 ext 4c: no availability claim without declared availability");
        assertEquals(periodTitles(40, 59), emptyCellTitles(false), "UC-1 ext 4c: ordinary empty cells only");

        // Main 3-4 in Day for the room lens: one row headed by the room, tile subject · teacher · class.
        workbench.weekday("MONDAY");
        workbench.awaitText("Day · Monday");
        assertLensRowGroup("matrix:not(.week-matrix) tbody tr", "ROOM", "room-16", "Room · " + ROOM16);
        assertEquals("Room", workbench.string("document.querySelector('.matrix thead th').textContent"));
        assertEquals(lessonRange(960, 971), visibleLessons(), "UC-1 main 4: Day shows the room's Monday lessons");
        assertEquals(List.of(SUBJECT0, TEACHER16, CLASS16), tileFields("lesson-960"),
                "UC-1 main 4: room-lens Day tile is subject · teacher · class");
        String dayLabel = workbench.string("document.querySelector('[data-lesson-id=lesson-960]').getAttribute('aria-label')");
        for (String part : List.of(ROOM16, "Monday", "Declared period 0", "lesson-960")) {
            assertTrue(dayLabel.contains(part), "UC-1 main 4: room-lens accessible name adds " + part + ": " + dayLabel);
        }
        assertEquals(Set.of(), emptyCellTitles(true), "UC-1 ext 4c: the Day room lens makes no availability claim");
        workbench.range("WEEK");
        workbench.awaitText("Complete recurring Week");

        // G4 / RULE-6: presentation only.
        assertEquals(List.of(), mutations.requests(), "UC-1 G4: applying lenses issues no workspace mutation");
        assertEquals(before, storedDocument(), "UC-1 G4: applying lenses changes no durable state");

        // RULE-2: only range and weekday persist; reload returns to class rows.
        assertEquals("{\"version\":1,\"range\":\"WEEK\",\"weekdayId\":\"MONDAY\"}",
                workbench.string("localStorage.getItem('" + PREFERENCE + "')"), "RULE-2: the lens is never persisted");
        assertEquals(0, workbench.value("sessionStorage.length").intValue());
        assertEquals("", workbench.string("location.search + location.hash"));
        workbench.reload();
        workbench.awaitText("Showing 60 of 60 classes");
        workbench.expect("document.querySelectorAll('.week-matrix tbody[data-row-kind=CLASS]').length === 60 && document.querySelector('#room-filter').value === '' && document.querySelector('#teacher-filter').value === ''",
                "resolved decision 7: reload returns the lens to NONE");
    }

    @Test
    @DisplayName("UC-1 trigger/main 1-2/G1: inspector Show week and teacher investigation Show only matches apply the lens")
    void appliesLensesFromInspectorAndTeacherInvestigation() throws Exception {
        storeAccepted(fixtures.investigationScaleDocument());
        String before = storedDocument();
        workbench.open().awaitText("Showing 60 of 60 classes");
        Workbench.MutationLog mutations = workbench.recordMutations();

        workbench.selectLesson("lesson-960");
        workbench.expect("!document.querySelector('#workbench-inspector').hidden && document.querySelector('#lesson-panel-title')?.textContent === 'Declared lesson 960'");
        workbench.expect("document.querySelector('[data-show-week=TEACHER]').getAttribute('aria-label') === 'Show week for teacher " + TEACHER16 + "' && document.querySelector('[data-show-week=ROOM]').getAttribute('aria-label') === 'Show week for room " + ROOM16 + "'",
                "UC-1 trigger: the inspector names the teacher and room with Show week actions");
        workbench.click("[data-show-week=TEACHER]");
        workbench.awaitText("Lens: Teacher · " + TEACHER16);
        assertLensRowGroup("week-matrix tbody", "TEACHER", "teacher-16", "Teacher · " + TEACHER16);
        workbench.expect("document.querySelector('#teacher-filter').value === 'teacher-16' && !document.querySelector('#workbench-inspector').hidden && document.querySelector('#lesson-panel-title')?.textContent === 'Declared lesson 960' && document.querySelector('[data-lesson-id=lesson-960]').getAttribute('aria-pressed') === 'true'",
                "UC-1 G1: Show week applies the lens without closing the inspector or dropping the selection");

        workbench.click("[data-show-week=ROOM]");
        workbench.awaitText("Lens: Room · " + ROOM16);
        workbench.expect("document.querySelector('#room-filter').value === 'room-16' && document.querySelector('#teacher-filter').value === '' && !document.querySelector('#workbench-inspector').hidden",
                "UC-1 main 2: a room Show week clears the teacher lens first");

        workbench.resetView();
        workbench.awaitText("Showing 60 of 60 classes");
        workbench.investigateTeacher("teacher-16");
        workbench.click("#teacher-only");
        workbench.awaitText("Lens: Teacher · " + TEACHER16);
        assertLensRowGroup("week-matrix tbody", "TEACHER", "teacher-16", "Teacher · " + TEACHER16);
        workbench.expect("document.querySelector('#teacher-only').checked && document.querySelector('#teacher-investigation').value === 'teacher-16' && document.querySelectorAll('.lesson-cell.teacher-match:not([hidden])').length === 40",
                "resolved decision 5: Show only matches applies the teacher lens and keeps the investigation highlight");
        workbench.click("#teacher-only");
        workbench.awaitText("Showing 60 of 60 classes");
        workbench.expect("!document.querySelector('#teacher-only').checked && document.querySelector('#teacher-filter').value === '' && document.querySelector('#teacher-investigation').value === 'teacher-16'",
                "resolved decision 5: unchecking clears that lens and keeps the whole-school investigation");

        assertEquals(List.of(), mutations.requests(), "UC-1 G4: lens entry points issue no workspace mutation");
        assertEquals(before, storedDocument());
    }

    @Test
    @DisplayName("UC-1 ext 4a/4c: an entity without represented assignments renders the full empty lens row group")
    void rendersAnEmptyLensRowGroup() {
        storeAccepted(fixtures.scaleDocument());
        String before = storedDocument();
        workbench.open().awaitText("Showing 60 of 60 classes");
        Workbench.MutationLog mutations = workbench.recordMutations();

        workbench.filterRoom("room-99");
        workbench.awaitText("Lens: Room · Room 99");
        assertLensRowGroup("week-matrix tbody", "ROOM", "room-99", "Room · Room 99");
        assertEquals(Set.of(), visibleLessons(), "UC-1 ext 4a: the system never invents an assignment");
        assertEquals("Represented lessons: 0", workbench.string("document.querySelector('#represented-lesson-count').textContent"));
        assertEquals(periodTitles(0, 59), emptyCellTitles(false), "UC-1 ext 4a/4c: every period cell renders, ordinary and empty");
        workbench.expect("!document.querySelector('#no-matches').hidden && document.querySelector('#no-matches #reset-empty')",
                "UC-1 ext 4a: zero lessons are reported and reset is offered");

        // An intersecting class filter can also empty the lens; the lens row group stays whole.
        workbench.filterTeacher("teacher-16");
        workbench.awaitText("Lens: Teacher · Teacher 16");
        workbench.filterClass("cohort-0");
        workbench.expect("document.querySelector('#represented-lesson-count').textContent === 'Represented lessons: 0' && document.querySelectorAll('.week-matrix tbody').length === 1 && !document.querySelector('.week-matrix tbody').hidden && !document.querySelector('#no-matches').hidden",
                "UC-1 ext 4a: a lens intersected to zero lessons keeps its row group");
        workbench.click("#reset-empty");
        workbench.awaitText("Showing 60 of 60 classes");

        assertEquals(List.of(), mutations.requests());
        assertEquals(before, storedDocument());
    }

    @Test
    @DisplayName("UC-1 ext 4b/RULE-4: a manual-draft room clash stacks both tiles in one room-lens cell with conflict cues")
    void stacksClashingLessonsInOneLensCell() throws Exception {
        storeAccepted(fixtures.investigationScaleDocument());
        workbench.open().awaitText("Showing 60 of 60 classes");
        // Precondition: a manual draft moving lesson-0 into room-16 at period-0, where lesson-960 already is.
        workbench.selectLesson("lesson-0");
        Workbench.MutationLog edit = workbench.recordMutations();
        workbench.select("#edit-room", "room-16");
        workbench.awaitText("2 conflicts");
        String draft = storedDocument();
        Workbench.MutationLog mutations = workbench.recordMutations();

        workbench.filterRoom("room-16");
        workbench.awaitText("Lens: Room · " + ROOM16);
        assertLensRowGroup("week-matrix tbody", "ROOM", "room-16", "Room · " + ROOM16);
        Set<String> expected = new LinkedHashSet<>(lessonRange(960, 999));
        expected.add("lesson-0");
        assertEquals(expected, visibleLessons(), "RULE-4: the room lens is sourced from the manual-draft assignments");
        workbench.expect("(() => { const cell = document.querySelector('[data-lesson-id=lesson-0]').closest('td'); return cell.querySelector('[data-lesson-id=lesson-960]') && cell.title === 'Declared period 0' && [...cell.querySelectorAll('.lesson-cell')].every(tile => tile.classList.contains('conflicting') && tile.querySelector('.conflict-indicator')); })()",
                "UC-1 ext 4b: both clashing tiles share the period-0 lens cell with the conflict highlight and indicator");
        workbench.press("[data-lesson-id=lesson-960] .conflict-indicator", "Enter");
        workbench.expect("!document.querySelector('#conflict-overlay-lesson-960').hidden && document.querySelector('#conflict-overlay-lesson-960').textContent.includes('ROOM_CLASH')",
                "UC-1 ext 4b: the conflict overlay works in the lens as on class rows");

        assertEquals(List.of(), mutations.requests(), "UC-1 G4: the lens issues no draft mutation");
        assertEquals(draft, storedDocument());
        assertEquals(List.of("PATCH /api/manual-draft"), edit.requests(), "setup: one inline edit issues exactly one save");
    }

    @Test
    @DisplayName("UC-1 all lifecycles/G1/G4: a lens in Solving keeps the run controls and the inspector and writes nothing")
    void appliesALensWhileARepairIsSolving() throws Exception {
        storeAccepted(fixtures.validAcceptedDocument());
        workbench.open().awaitText("Start a protected repair");
        workbench.startRepair(null, null, "mon-1");
        workbench.awaitText("Repair draft · not current");
        processes.blockReplan = true;
        workbench.click("#solve-draft");
        processes.awaitBlocked();
        workbench.awaitText("Repair generation · running");
        assertEquals("SOLVING_REPAIR", storedLifecycle());
        JsonNode running = storedWorkspaceDocument();
        Workbench.MutationLog mutations = workbench.recordMutations();

        workbench.mode("SOLVING");
        workbench.selectLesson("lesson-math-1");
        workbench.showWeek("TEACHER");
        workbench.awaitText("Lens: Teacher · Alex");
        assertLensRowGroup("week-matrix tbody", "TEACHER", "teacher-alex", "Teacher · Alex");
        workbench.expect("document.querySelector('#workbench-task-area #cancel-run') && !document.querySelector('#workbench-inspector').hidden && document.querySelector('[data-lesson-id=lesson-math-1]').getAttribute('aria-pressed') === 'true' && document.querySelector('#filter-title').textContent === 'Filtered whole-school matrix'",
                "UC-1 G1: in Solving the lens keeps the run controls, the inspector, and the selection");
        assertEquals("Mathematics", tileFields("lesson-math-1").get(0));
        assertEquals("Year 7A", tileFields("lesson-math-1").get(2), "UC-1 main 4: the Solving teacher-lens tile names the class");
        workbench.range("DAY");
        workbench.awaitText("Day · Monday");
        assertLensRowGroup("matrix:not(.week-matrix) tbody tr", "TEACHER", "teacher-alex", "Teacher · Alex");
        workbench.removeLens();
        workbench.expect("document.querySelectorAll('.matrix tbody tr[data-row-kind=CLASS]').length > 0 && document.querySelector('#workbench-task-area #cancel-run')");

        assertEquals(List.of(), mutations.requests(), "UC-1 G4: a Solving lens issues no mutation");
        assertEquals(running, storedWorkspaceDocument(), "UC-1 G4: a Solving lens changes nothing durable");
        assertEquals("SOLVING_REPAIR", storedLifecycle());
    }

    @Test
    @DisplayName("UC-1 trigger/all lifecycles/RULE-4: Proposal Show week applies a lens and review targets enter or clear it")
    void appliesAndLeavesLensesDuringProposalReview() {
        storeProposal(fixtures.comparisonShapeDocument());
        String durable = storedDocument();
        workbench.open().awaitText("Unique changed lessons");
        Workbench.MutationLog mutations = workbench.recordMutations();

        workbench.click("[data-lesson-id=lesson-0][data-comparison-side=accepted]");
        workbench.expect("document.querySelector('#workbench-inspector .comparison-details') && document.querySelector('#workbench-inspector [data-show-week=TEACHER]')?.dataset.showWeekId === 'teacher-0' && document.querySelector('#workbench-inspector [data-show-week=ROOM]')?.dataset.showWeekId === 'room-0'",
                "UC-1 trigger: the Proposal inspector offers Show week for the lesson's teacher and room");
        workbench.showWeek("TEACHER");
        workbench.awaitText("Lens: Teacher · Teacher 0");
        assertLensRowGroup("week-matrix tbody", "TEACHER", "teacher-0", "Teacher · Teacher 0");
        workbench.expect("[...document.querySelectorAll('[data-lesson-id=lesson-0]:not([hidden])')].map(tile => tile.dataset.comparisonSide).sort().join() === 'accepted,proposed' && !document.querySelector('#workbench-inspector').hidden && document.querySelector('#lesson-panel-title')?.textContent === 'Declared lesson 0'",
                "RULE-4: the Proposal lens keeps both comparison sides and the inspector stays on the lesson");

        // A review target the lens does not represent clears it, with an announcement.
        workbench.click("[data-category=periodMoves] [data-review-lesson=lesson-60][data-review-side=accepted]");
        workbench.expect("document.querySelector('#teacher-filter').value === '' && document.querySelectorAll('.week-matrix tbody[data-row-kind=CLASS]').length === 60 && document.querySelector('#inspection-notice').textContent.includes('Cleared Teacher filter') && document.querySelector('#lesson-panel-title')?.textContent === 'Declared lesson 60'",
                "review navigation out of a lens clears it and announces the cleared criterion");

        // A review target the lens represents keeps the lens and selects inside it.
        workbench.filterTeacher("teacher-1");
        workbench.awaitText("Lens: Teacher · Teacher 1");
        workbench.click("[data-category=periodMoves] [data-review-lesson=lesson-61][data-review-side=proposed]");
        workbench.expect("document.querySelector('#teacher-filter').value === 'teacher-1' && document.querySelector('.week-matrix tbody[data-row-id=teacher-1] [data-lesson-id=lesson-61][data-comparison-side=proposed]')?.getAttribute('aria-pressed') === 'true' && document.querySelector('#inspection-notice').textContent === '' && document.querySelector('#lesson-panel-title')?.textContent === 'Declared lesson 61'",
                "review navigation into a lens keeps the lens and selects the represented side");

        assertEquals(List.of(), mutations.requests(), "UC-1 G4: Proposal lenses issue no mutation");
        assertEquals(durable, storedDocument());
    }

    @Test
    @DisplayName("UC-1 minimal guarantee/state rule 4/RULE-1: an undeclared lens entity is refused and the prior rows remain")
    void refusesAnUndeclaredLensEntity() {
        storeAccepted(fixtures.scaleDocument());
        String before = storedDocument();
        workbench.open().awaitText("Showing 60 of 60 classes");
        Workbench.MutationLog mutations = workbench.recordMutations();
        workbench.filterRoom("room-3");
        workbench.awaitText("Lens: Room · Room 3");

        workbench.value("document.querySelector('#teacher-filter').insertAdjacentHTML('beforeend', '<option value=\"teacher-ghost\">Ghost</option>')");
        workbench.filterTeacher("teacher-ghost");
        workbench.expect("document.querySelector('#teacher-filter').value === '' && document.querySelector('#room-filter').value === 'room-3' && document.querySelector('#matrix-summary').textContent === 'Lens: Room · Room 3' && document.querySelectorAll('.week-matrix tbody[data-row-kind=ROOM]').length === 1",
                "UC-1 minimal guarantee: a refused lens keeps the prior rows and filters unchanged");

        JsonNode transitions = workbench.value("""
                (async () => {
                  const { createInspectionState } = await import('/workspace/inspection-state.js');
                  const state = createInspectionState({ schoolId: null, weekdays: ['MONDAY'], teacherIds: ['t'], roomIds: ['r'],
                    cohortIds: ['c'], storage: { getItem: () => null, setItem: () => {} } });
                  const teacher = state.selectFilter('teacherFilterId', 't');
                  const room = state.selectFilter('roomId', 'r');
                  const ghost = state.selectFilter('teacherFilterId', 'ghost');
                  const back = state.selectFilter('teacherFilterId', 't');
                  const cohort = state.selectFilter('cohortId', 'c');
                  const reset = state.resetFilters().state;
                  return { teacher: teacher.state.teacherFilterId, roomTeacher: room.state.teacherFilterId, room: room.state.roomId,
                    ghostChanged: ghost.changed, ghostRoom: ghost.state.roomId, backRoom: back.state.roomId,
                    cohortTeacher: cohort.state.teacherFilterId, resetTeacher: reset.teacherFilterId, resetRoom: reset.roomId };
                })()""");
        assertEquals(JSON.readTree("""
                {"teacher":"t","roomTeacher":null,"room":"r","ghostChanged":false,"ghostRoom":"r","backRoom":null,
                 "cohortTeacher":"t","resetTeacher":null,"resetRoom":null}"""), transitions,
                "RULE-1: lenses are mutually exclusive, undeclared IDs are refused, class filters intersect, reset clears");

        assertEquals(List.of(), mutations.requests());
        assertEquals(before, storedDocument());
    }

    @Test
    @DisplayName("UC-2 main 1-5/G1/G2/state rule 2: clearing a lens restores class rows and the class-row scroll position")
    void clearsALensBackToTheRecordedScroll() {
        storeAccepted(fixtures.scaleDocument());
        String before = storedDocument();
        workbench.open().awaitText("Showing 60 of 60 classes");
        Workbench.MutationLog mutations = workbench.recordMutations();
        workbench.value("document.querySelector('.matrix-wrap').scrollTo(0, 2400)");
        double recorded = workbench.value("document.querySelector('.matrix-wrap').scrollTop").doubleValue();
        assertTrue(recorded > 1000, "setup: the class rows are scrolled well past the top: " + recorded);

        // Main 1 via the removable criterion; 1a and 1b in between never overwrite the recorded entry position.
        workbench.filterTeacher("teacher-16");
        workbench.awaitText("Lens: Teacher · Teacher 16");
        workbench.filterTeacher("teacher-3");
        workbench.awaitText("Lens: Teacher · Teacher 3");
        workbench.filterRoom("room-5");
        workbench.awaitText("Lens: Room · Room 5");
        workbench.value("document.querySelector('.matrix-wrap').scrollTo(0, 120)");
        workbench.removeLens();
        workbench.awaitText("Showing 60 of 60 classes");
        assertClassRowsRestoredAt(recorded);
        assertEquals("Complete school population", workbench.string("document.querySelector('#filter-title').textContent"),
                "UC-2 main 5: with no remaining criterion the status is complete");

        // Main 1 via "All teachers" in the Filters select.
        workbench.value("document.querySelector('.matrix-wrap').scrollTo(0, 1800)");
        double second = workbench.value("document.querySelector('.matrix-wrap').scrollTop").doubleValue();
        workbench.filterTeacher("teacher-30");
        workbench.awaitText("Lens: Teacher · Teacher 30");
        workbench.filterTeacher("");
        workbench.awaitText("Showing 60 of 60 classes");
        assertClassRowsRestoredAt(second);

        workbench.expectNot("document.querySelector('.focused-schedule, #return-matrix')", "UC-2 G2: no separate surface or return control");
        assertEquals(List.of(), mutations.requests(), "UC-2: changing and clearing lenses issues no mutation");
        assertEquals(before, storedDocument());
    }

    @Test
    @DisplayName("UC-2 main 2-5/G1/state rules 1-2: clearing retains range, day, search, highlights, filters and the selection in view")
    void clearsALensRetainingTheInvestigativeContext() throws Exception {
        storeAccepted(fixtures.investigationScaleDocument());
        String before = storedDocument();
        workbench.open().awaitText("Showing 60 of 60 classes");
        Workbench.MutationLog mutations = workbench.recordMutations();
        workbench.range("DAY");
        workbench.awaitText("Day · Monday");
        workbench.search("Sixteen");
        workbench.investigateSubject("subject-0");
        workbench.investigateTeacher("teacher-17");
        workbench.selectLesson("lesson-960");
        workbench.showWeek("TEACHER");
        workbench.awaitText("Lens: Teacher · " + TEACHER16);
        workbench.focusPeriod("period-0");
        workbench.expect("document.querySelector('#represented-lesson-count').textContent === 'Represented lessons: 1'");

        workbench.filterTeacher("");
        workbench.expect("document.querySelectorAll('.matrix tbody tr[data-row-kind=CLASS]:not([hidden])').length === 17 && document.querySelector('#represented-lesson-count').textContent === 'Represented lessons: 17'",
                "UC-2 main 2: class rows return under the remaining period filter (classes 0-16 teach in period 0)");
        workbench.expect("document.querySelector('[data-range=DAY]').getAttribute('aria-pressed') === 'true' && document.querySelector('#weekday').value === 'MONDAY' && document.querySelector('#lesson-search').value === 'Sixteen' && document.querySelector('#subject-investigation').value === 'subject-0' && document.querySelector('#teacher-investigation').value === 'teacher-17' && document.querySelector('#period-focus').value === 'period-0' && document.querySelector('#teacher-filter').value === ''",
                "UC-2 main 3/G1: only the lens changed");
        workbench.expect("document.querySelector('[data-lesson-id=lesson-960]').getAttribute('aria-pressed') === 'true' && !document.querySelector('#workbench-inspector').hidden && document.querySelector('#lesson-panel-title')?.textContent === 'Declared lesson 960'",
                "UC-2 main 3: the selected lesson stays selected and inspected");
        workbench.expect("(() => { const wrap = document.querySelector('.matrix-wrap'); const tile = document.querySelector('[data-lesson-id=lesson-960]').getBoundingClientRect(); const box = wrap.getBoundingClientRect(); return wrap.scrollTop > 0 && tile.top >= box.top && tile.bottom <= box.bottom && tile.left >= box.left && tile.right <= box.right; })()",
                "UC-2 main 4/state rule 2: a represented selection is brought into view instead of restoring the scroll");
        assertEquals("Filtered whole-school matrix", workbench.string("document.querySelector('#filter-title').textContent"),
                "UC-2 main 5: the remaining period filter keeps the status narrowed");
        workbench.expect("document.querySelector('#active-criteria').textContent === 'Period: Declared period 0' && !document.querySelector('#active-criteria [data-remove-lens]')");

        assertEquals(List.of(), mutations.requests());
        assertEquals(before, storedDocument());
    }

    @Test
    @DisplayName("UC-2 ext 1a/1b/3a: changing the lens entity or type keeps the selection only when the lesson belongs to it")
    void changesTheLensEntityOrType() throws Exception {
        storeAccepted(fixtures.investigationScaleDocument());
        String before = storedDocument();
        workbench.open().awaitText("Showing 60 of 60 classes");
        Workbench.MutationLog mutations = workbench.recordMutations();
        workbench.selectLesson("lesson-960");
        workbench.showWeek("TEACHER");
        workbench.awaitText("Lens: Teacher · " + TEACHER16);

        // Ext 1b: the other type, and the lesson belongs to it, so the selection stays.
        workbench.showWeek("ROOM");
        workbench.awaitText("Lens: Room · " + ROOM16);
        assertLensRowGroup("week-matrix tbody", "ROOM", "room-16", "Room · " + ROOM16);
        workbench.expect("document.querySelector('#teacher-filter').value === '' && document.querySelector('[data-lesson-id=lesson-960]').getAttribute('aria-pressed') === 'true' && document.querySelector('#inspection-notice').textContent === ''",
                "UC-2 ext 1b: the room lens replaces the teacher lens and keeps a lesson that belongs to it");

        // Ext 1a + 3a: another room the lesson does not belong to clears the selection with an announcement.
        workbench.filterRoom("room-3");
        workbench.awaitText("Lens: Room · Room 3");
        assertLensRowGroup("week-matrix tbody", "ROOM", "room-3", "Room · Room 3");
        workbench.expect("document.querySelector('#inspection-notice').textContent === 'The selected lesson is not in the Room · Room 3 lens, so its details were closed.' && document.querySelector('#workbench-inspector').hidden && !document.querySelector('.lesson-cell.selected')",
                "UC-2 ext 3a: an unrepresented selection is cleared and the reason announced");

        // Ext 1b + 3a: the other type, without the lesson.
        workbench.selectLesson("lesson-180");
        workbench.filterTeacher("teacher-16");
        workbench.awaitText("Lens: Teacher · " + TEACHER16);
        workbench.expect("document.querySelector('#room-filter').value === '' && document.querySelector('#inspection-notice').textContent === 'The selected lesson is not in the Teacher · " + TEACHER16 + " lens, so its details were closed.'",
                "UC-2 ext 1b/3a: switching type clears the other lens and an unrepresented selection");

        assertEquals(List.of(), mutations.requests());
        assertEquals(before, storedDocument());
    }

    @Test
    @DisplayName("UC-2 ext 1c/main 4: reset and clear-filters clear the lens with every narrowing criterion and restore the scroll")
    void resetsTheLensWithEveryNarrowingCriterion() {
        storeAccepted(fixtures.scaleDocument());
        String before = storedDocument();
        workbench.open().awaitText("Showing 60 of 60 classes");
        Workbench.MutationLog mutations = workbench.recordMutations();
        workbench.investigateSubject("subject-4");
        workbench.search("Room");
        workbench.value("document.querySelector('.matrix-wrap').scrollTo(0, 2000)");
        double recorded = workbench.value("document.querySelector('.matrix-wrap').scrollTop").doubleValue();
        workbench.filterRoom("room-4");
        workbench.awaitText("Lens: Room · Room 4");
        workbench.filterClass("cohort-4");
        workbench.focusPeriod("period-3");

        workbench.resetView();
        workbench.awaitText("Showing 60 of 60 classes");
        assertClassRowsRestoredAt(recorded);
        workbench.expect("document.querySelector('#filter-title').textContent === 'Complete school population' && document.querySelector('#room-filter').value === '' && document.querySelector('#cohort-filter').value === '' && document.querySelector('#period-focus').value === '' && document.querySelector('#lesson-search').value === '' && document.querySelector('#subject-investigation').value === 'subject-4' && document.querySelector('[data-range=WEEK]').getAttribute('aria-pressed') === 'true'",
                "UC-2 ext 1c: reset clears the lens and every narrowing criterion, retains range and keeps the subject investigation");

        // The empty-lens reset offer (clear narrowing) takes the same path and keeps the search.
        workbench.search("Room");
        workbench.value("document.querySelector('.matrix-wrap').scrollTo(0, 1500)");
        double again = workbench.value("document.querySelector('.matrix-wrap').scrollTop").doubleValue();
        workbench.filterRoom("room-99");
        workbench.awaitText("Lens: Room · Room 99");
        workbench.click("#reset-empty");
        workbench.awaitText("Showing 60 of 60 classes");
        assertClassRowsRestoredAt(again);
        assertEquals("Room", workbench.string("document.querySelector('#lesson-search').value"));

        assertEquals(List.of(), mutations.requests());
        assertEquals(before, storedDocument());
    }

    @Test
    @DisplayName("UC-2 ext 1c/inspection-ux rule 7: Reset and Clear filters return Show only matches modes to highlight mode")
    void returnsShowOnlyMatchesModesToHighlightsOnReset() throws Exception {
        storeAccepted(fixtures.investigationScaleDocument());
        String before = storedDocument();
        workbench.open().awaitText("Showing 60 of 60 classes");
        Workbench.MutationLog mutations = workbench.recordMutations();
        workbench.investigateSubject("subject-0");
        workbench.investigateTeacher("teacher-16");

        for (String clear : List.of("#reset-view", "#clear-filters")) {
            workbench.search("Sixteen");
            workbench.value("document.querySelector('.matrix-wrap').scrollTo(0, 2400)");
            double recorded = workbench.value("document.querySelector('.matrix-wrap').scrollTop").doubleValue();
            workbench.click("#teacher-only");
            workbench.awaitText("Lens: Teacher · " + TEACHER16);
            workbench.click("#subject-only");
            workbench.expect("document.querySelector('#teacher-only').checked && document.querySelector('#subject-only').checked && document.querySelector('#represented-lesson-count').textContent === 'Represented lessons: 1'",
                    "setup: the teacher lens comes from Show only matches and the subject is in filter mode");

            workbench.click(clear);
            workbench.awaitText("Showing 60 of 60 classes");
            assertClassRowsRestoredAt(recorded);
            workbench.expect("!document.querySelector('#teacher-only').checked && !document.querySelector('#subject-only').checked && document.querySelector('#teacher-investigation').value === 'teacher-16' && document.querySelector('#subject-investigation').value === 'subject-0' && document.querySelectorAll('.lesson-cell.teacher-match:not([hidden])').length === 40 && document.querySelectorAll('.lesson-cell.subject-match:not([hidden])').length === 61 && document.querySelector('#represented-lesson-count').textContent === 'Represented lessons: 1000' && document.querySelector('#filter-title').textContent === 'Complete school population' && document.querySelector('#active-criteria').textContent === 'No active filters'",
                    "UC-2 ext 1c (" + clear + "): the lens and the subject filter mode are cleared and both investigations return to highlight mode");
            assertEquals(clear.equals("#reset-view") ? "" : "Sixteen", workbench.string("document.querySelector('#lesson-search').value"),
                    "UC-2 ext 1c (" + clear + "): Reset view clears the search, Clear filters keeps it");
        }

        assertEquals(List.of(), mutations.requests());
        assertEquals(before, storedDocument());
    }

    @Test
    @DisplayName("UC-2 main 4/state rule 2: a represented selection wins over the recorded scroll position")
    void prefersTheSelectionOverTheRecordedScroll() {
        storeAccepted(fixtures.scaleDocument());
        String before = storedDocument();
        workbench.open().awaitText("Showing 60 of 60 classes");
        Workbench.MutationLog mutations = workbench.recordMutations();
        workbench.selectLesson("lesson-0");
        workbench.value("document.querySelector('.matrix-wrap').scrollTo(0, 3000)");
        assertTrue(workbench.value("document.querySelector('.matrix-wrap').scrollTop").doubleValue() > 2500, "setup: lesson-0 is scrolled out of view");
        workbench.filterTeacher("teacher-0");
        workbench.awaitText("Lens: Teacher · Teacher 0");
        workbench.expect("document.querySelector('[data-lesson-id=lesson-0]').getAttribute('aria-pressed') === 'true'", "setup: lesson-0 belongs to the lens and stays selected");

        workbench.removeLens();
        workbench.awaitText("Showing 60 of 60 classes");
        workbench.expect("(() => { const wrap = document.querySelector('.matrix-wrap'); const tile = document.querySelector('[data-lesson-id=lesson-0]').getBoundingClientRect(); const box = wrap.getBoundingClientRect(); return wrap.scrollTop < 1000 && tile.top >= box.top && tile.bottom <= box.bottom && document.querySelector('[data-lesson-id=lesson-0]').getAttribute('aria-pressed') === 'true'; })()",
                "UC-2 state rule 2: the selected lesson is scrolled into view instead of restoring the recorded 3000 px");

        assertEquals(List.of(), mutations.requests());
        assertEquals(before, storedDocument());
    }

    @Test
    @DisplayName("UC-2 main 4/user decision D-1: the recorded scroll is restored only in the range it was recorded in")
    void restoresTheRecordedScrollOnlyInItsRange() {
        storeAccepted(fixtures.scaleDocument());
        String before = storedDocument();
        workbench.open().awaitText("Showing 60 of 60 classes");
        Workbench.MutationLog mutations = workbench.recordMutations();

        // Week offset, lens, switch to Day, clear: the Day class rows start at the top, not at a Week offset.
        workbench.value("document.querySelector('.matrix-wrap').scrollTo(0, 2400)");
        workbench.filterTeacher("teacher-5");
        workbench.awaitText("Lens: Teacher · Teacher 5");
        workbench.range("DAY");
        workbench.awaitText("Day · Monday");
        workbench.removeLens();
        workbench.expect("document.querySelectorAll('.matrix tbody tr[data-row-kind=CLASS]').length === 60 && document.querySelector('.matrix-wrap').scrollTop === 0",
                "D-1: a Week scroll offset is not applied to Day class rows");

        // Day offset, lens, change weekday within Day, clear: still the same range, so the offset is restored.
        workbench.value("document.querySelector('.matrix-wrap').scrollTo(0, 900)");
        double recorded = workbench.value("document.querySelector('.matrix-wrap').scrollTop").doubleValue();
        assertTrue(recorded > 500, "setup: Day class rows are scrolled: " + recorded);
        workbench.filterTeacher("teacher-5");
        workbench.awaitText("Lens: Teacher · Teacher 5");
        workbench.weekday("WEDNESDAY");
        workbench.awaitText("Day · Wednesday");
        workbench.removeLens();
        workbench.expect("document.querySelectorAll('.matrix tbody tr[data-row-kind=CLASS]').length === 60 && Math.abs(document.querySelector('.matrix-wrap').scrollTop - " + recorded + ") < 1",
                "UC-2 main 4: within the same range the recorded offset is restored");

        assertEquals(List.of(), mutations.requests());
        assertEquals(before, storedDocument());
    }

    @Test
    @DisplayName("UC-3 main 1-5/G1/G3: a lesson edited inside its lens moves to its new lens cell, selected, with one save per edit")
    void editsALessonInsideItsLens() throws Exception {
        storeAccepted(fixtures.investigationScaleDocument());
        workbench.open().awaitText("Showing 60 of 60 classes");
        workbench.filterTeacher("teacher-16");
        workbench.awaitText("Lens: Teacher · " + TEACHER16);
        Workbench.MutationLog mutations = workbench.recordMutations();

        // Main 1-2: the inspector opens from a lens tile and offers Show week for teacher, room and class.
        workbench.selectLesson("lesson-960");
        workbench.expect("!document.querySelector('#workbench-inspector').hidden && document.querySelector('#lesson-panel-title')?.textContent === 'Declared lesson 960' && ['TEACHER', 'ROOM', 'CLASS'].every(kind => document.querySelector('#workbench-inspector [data-show-week=' + kind + ']')) && document.querySelector('[data-show-week=CLASS]').getAttribute('aria-label') === 'Show week for class " + CLASS16 + "'",
                "UC-3 main 2: complete details with Show week for the teacher, room and class");
        assertEquals(List.of(), mutations.requests(), "UC-3 G3: selecting and inspecting issue no mutation");

        // Main 3-5 in Week: period-0 to the free, declared period-40 keeps the lesson in the lens, in its new cell.
        workbench.select("#edit-period", "period-40");
        workbench.expect("document.querySelector('[data-lesson-id=lesson-960]')?.closest('td')?.title === 'Declared period 40' && document.querySelector('[data-lesson-id=lesson-960]').getAttribute('aria-pressed') === 'true' && !document.querySelector('#workbench-inspector').hidden && document.querySelector('#teacher-filter').value === 'teacher-16' && document.querySelector('.week-matrix tbody').dataset.rowId === 'teacher-16'",
                "UC-3 main 5: the edited lesson is rendered in its new lens cell with the selection retained");
        assertEquals(List.of("PATCH /api/manual-draft"), mutations.requests(), "UC-3 main 4: one edit issues exactly one save");
        assertEquals("MANUAL_DRAFT", storedLifecycle());
        assertEquals("period-40", draftAssignment("lesson-960").path("periodId").stringValue(), "UC-3 main 4: the edit is persisted");

        // Main 3-5 in Day: a period on another weekday moves the Day to that weekday, still in the lens.
        workbench.range("DAY");
        workbench.weekday("MONDAY");
        workbench.awaitText("Day · Monday");
        workbench.selectLesson("lesson-961");
        workbench.select("#edit-period", "period-40");
        workbench.awaitText("Day · Thursday");
        workbench.expect("document.querySelector('.matrix tbody tr').dataset.rowId === 'teacher-16' && document.querySelector('[data-lesson-id=lesson-961]')?.getAttribute('aria-pressed') === 'true' && document.querySelector('#weekday').value === 'THURSDAY'",
                "UC-3 main 5: a cross-day edit follows the lesson to its weekday inside the lens");
        assertEquals(List.of("PATCH /api/manual-draft", "PATCH /api/manual-draft"), mutations.requests());
    }

    @Test
    @DisplayName("UC-3 ext 5a/G2: an edit that reassigns the lesson out of the lens keeps it inspected, announces it, and keeps the lens")
    void keepsALessonEditedOutOfTheLens() throws Exception {
        storeAccepted(fixtures.investigationScaleDocument());
        workbench.open().awaitText("Showing 60 of 60 classes");
        workbench.filterTeacher("teacher-16");
        workbench.awaitText("Lens: Teacher · " + TEACHER16);
        Workbench.MutationLog mutations = workbench.recordMutations();

        workbench.selectLesson("lesson-961");
        workbench.select("#edit-teacher", "teacher-17");
        workbench.awaitText("Declared lesson 961 left the Teacher · " + TEACHER16 + " lens: its teacher is now Teacher 17. It stays open in the inspector.");
        workbench.expect("!document.querySelector('[data-lesson-id=lesson-961]') && document.querySelector('#teacher-filter').value === 'teacher-16' && document.querySelector('.week-matrix tbody').dataset.rowId === 'teacher-16' && !document.querySelector('#workbench-inspector').hidden && document.querySelector('#lesson-panel-title')?.textContent === 'Declared lesson 961' && document.querySelector('#edit-teacher').value === 'teacher-17'",
                "UC-3 ext 5a 1-4: the tile leaves the lens, the lesson stays selected and inspected, and the lens is unchanged");
        assertEquals("teacher-17", draftAssignment("lesson-961").path("teacherId").stringValue());

        // The same for a room lens.
        workbench.filterRoom("room-16");
        workbench.awaitText("Lens: Room · " + ROOM16);
        workbench.selectLesson("lesson-962");
        workbench.select("#edit-room", "room-90");
        workbench.awaitText("Declared lesson 962 left the Room · " + ROOM16 + " lens: its room is now Room 90. It stays open in the inspector.");
        workbench.expect("!document.querySelector('[data-lesson-id=lesson-962]') && document.querySelector('#room-filter').value === 'room-16' && !document.querySelector('#workbench-inspector').hidden && document.querySelector('#lesson-panel-title')?.textContent === 'Declared lesson 962'",
                "UC-3 ext 5a: a room reassignment leaves the room lens the same way");
        assertEquals(List.of("PATCH /api/manual-draft", "PATCH /api/manual-draft"), mutations.requests(), "UC-3 G3: only the two edits mutate");
    }

    @Test
    @DisplayName("UC-3 ext 5b: an edit that clashes with another lesson of the lens entity stacks both tiles with conflict cues")
    void stacksALensClashCausedByAnEdit() throws Exception {
        storeAccepted(fixtures.investigationScaleDocument());
        workbench.open().awaitText("Showing 60 of 60 classes");
        workbench.filterTeacher("teacher-16");
        workbench.awaitText("Lens: Teacher · " + TEACHER16);

        workbench.selectLesson("lesson-963");
        workbench.select("#edit-period", "period-4");
        workbench.expect("(() => { const cell = document.querySelector('[data-lesson-id=lesson-963]')?.closest('td'); return cell && cell.title === 'Declared period 4' && cell.querySelector('[data-lesson-id=lesson-964]') && [...cell.querySelectorAll('.lesson-cell')].every(tile => tile.classList.contains('conflicting') && tile.querySelector('.conflict-indicator')); })()",
                "UC-3 ext 5b: both lens lessons share the period-4 cell with the conflict highlight and indicator");
        workbench.press("[data-lesson-id=lesson-964] .conflict-indicator", "Enter");
        workbench.expect("!document.querySelector('#conflict-overlay-lesson-964').hidden && document.querySelector('#conflict-overlay-lesson-964').textContent.includes('TEACHER_CLASH')",
                "UC-3 ext 5b: the conflict overlay explains the teacher clash inside the lens");
        workbench.expect("document.querySelector('[data-lesson-id=lesson-963]').getAttribute('aria-pressed') === 'true' && document.querySelector('#teacher-filter').value === 'teacher-16'");
    }

    @Test
    @DisplayName("UC-3 ext 2a/1a: class Show week clears the lens for a class filter; a draft link outside the lens clears it with named criteria")
    void leavesTheLensForAClassWeekOrADraftLink() throws Exception {
        storeAccepted(fixtures.investigationScaleDocument());
        String before = storedDocument();
        workbench.open().awaitText("Showing 60 of 60 classes");
        Workbench.MutationLog mutations = workbench.recordMutations();

        workbench.filterTeacher("teacher-16");
        workbench.awaitText("Lens: Teacher · " + TEACHER16);
        workbench.selectLesson("lesson-960");
        workbench.showWeek("CLASS");
        workbench.awaitText("Class: " + CLASS16);
        workbench.expect("document.querySelector('#teacher-filter').value === '' && document.querySelector('#cohort-filter').value === 'cohort-16' && document.querySelectorAll('.week-matrix tbody[data-row-kind=CLASS]:not([hidden])').length === 1 && document.querySelector('[data-lesson-id=lesson-960]').getAttribute('aria-pressed') === 'true' && !document.querySelector('#workbench-inspector').hidden && !document.querySelector('#active-criteria [data-remove-lens]')",
                "UC-3 ext 2a: the class Show week clears the lens, applies the class filter and keeps the lesson selected");
        assertEquals(List.of(), mutations.requests());
        assertEquals(before, storedDocument());
        workbench.resetView();

        // Ext 1a through a repair-draft direct-effect link while an excluding room lens is active.
        workbench.startRepair("TEACHER", "teacher-16", "period-0");
        workbench.awaitText("Repair draft · not current");
        workbench.filterRoom("room-3");
        workbench.awaitText("Lens: Room · Room 3");
        workbench.click("[data-draft-effect=lesson-960]");
        workbench.expect("document.querySelector('#room-filter').value === '' && document.querySelectorAll('.week-matrix tbody[data-row-kind=CLASS]').length === 60 && document.querySelector('#inspection-notice').textContent.includes('Cleared Room filter') && document.querySelector('[data-lesson-id=lesson-960]').getAttribute('aria-pressed') === 'true' && document.querySelector('#lesson-panel-title')?.textContent === 'Declared lesson 960'",
                "UC-3 ext 1a: a draft link outside the lens clears it, names the cleared criterion and selects the target");
    }

    private JsonNode draftAssignment(String lessonId) {
        for (JsonNode assignment : storedWorkspaceDocument().path("manualDraft").path("assignments")) {
            if (lessonId.equals(assignment.path("lessonId").stringValue())) return assignment;
        }
        throw new AssertionError("no draft assignment for " + lessonId);
    }

    private void assertClassRowsRestoredAt(double scrollTop) {
        workbench.expect("document.querySelectorAll('.week-matrix tbody[data-row-kind=CLASS]').length === 60 && Math.abs(document.querySelector('.matrix-wrap').scrollTop - " + scrollTop + ") < 1",
                "UC-2 main 2/4: class rows return at the scroll position recorded at lens entry (" + scrollTop + ")");
    }

    private void assertLensRowGroup(String selector, String kind, String id, String header) {
        workbench.expect("(() => { const groups = document.querySelectorAll('." + selector + "'); return groups.length === 1 && !groups[0].hidden && groups[0].dataset.rowKind === '" + kind + "' && groups[0].dataset.rowId === '" + id + "' && groups[0].querySelector('th').textContent.startsWith(" + JSON.writeValueAsString(header) + "); })()",
                "UC-1 main 3: exactly one " + kind + " row group for " + id + " headed '" + header + "'");
    }

    private Set<String> visibleLessons() {
        Set<String> ids = new LinkedHashSet<>();
        workbench.value(VISIBLE_LESSONS).forEach(id -> ids.add(id.stringValue()));
        return ids;
    }

    private List<String> tileFields(String lessonId) {
        return workbench.value("[...document.querySelector('[data-lesson-id=" + lessonId + "]').children].filter(field => field.tagName === 'STRONG' || field.tagName === 'SPAN' && !field.classList.contains('conflict-indicator')).map(field => field.textContent)")
                .valueStream().map(JsonNode::stringValue).toList();
    }

    private Set<String> emptyCellTitles(boolean unavailable) {
        Set<String> titles = new LinkedHashSet<>();
        workbench.value("[...document.querySelectorAll('#accepted-view .matrix td')].filter(cell => cell.querySelector('.empty-cell:not([hidden])" + (unavailable ? ".unavailable-cell" : ":not(.unavailable-cell)") + "')).map(cell => cell.title || cell.closest('table').querySelectorAll('thead th')[cell.cellIndex].querySelector('span').textContent)")
                .forEach(title -> titles.add(title.stringValue()));
        return titles;
    }

    private static Set<String> lessonRange(int first, int last) {
        return IntStream.rangeClosed(first, last).mapToObj(i -> "lesson-" + i).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static Set<String> periodTitles(int first, int last) {
        return IntStream.rangeClosed(first, last).mapToObj(i -> "Declared period " + i).collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
