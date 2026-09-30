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

        // G4 / RULE-6: presentation only.
        assertEquals(List.of(), mutations.requests(), "UC-1 G4: applying lenses issues no workspace mutation");
        assertEquals(before, storedDocument(), "UC-1 G4: applying lenses changes no durable state");

        // RULE-2: only range and weekday persist; reload returns to class rows.
        assertEquals("{\"version\":1,\"range\":\"WEEK\",\"weekdayId\":\"FRIDAY\"}",
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
