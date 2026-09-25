package org.schoolkernel.workspace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/** UC-1/UC-3 of the inspection specs: read-only Current inspection, investigation, narrowing and focus. */
class AcceptedInspectionBrowserIT extends WorkbenchBrowserSupport {
    private static final String PREFERENCE = "school-kernel.inspection.v1.opaque-school-id";

    @Test
    @DisplayName("Timetable inspection regression: accepted whole-school inspection remains local, narrow-safe, and immutable")
    void inspectsAcceptedWholeSchoolTimetable() {
        storeAccepted(fixtures.acceptedDocument(false));
        String before = storedDocument();

        String complete = workbench.open().awaitText("Complete school population");
        assertTrue(complete.contains("Accepted baseline · current timetable"));
        assertTrue(complete.contains("Mathematics"));
        assertTrue(complete.contains("Room 102"));
        assertTrue(complete.contains("Empty"));
        assertTrue(complete.contains("Showing 1 of 1 classes"));
        workbench.expect("document.querySelector('.week-matrix')");

        workbench.range("DAY");
        assertTrue(workbench.awaitText("Day · Monday").contains("Alex"));
        workbench.expect("document.querySelector('.matrix:not(.week-matrix)')");
        assertTrue(workbench.string("localStorage.getItem('" + PREFERENCE + "')").contains("\"range\":\"DAY\""));
        workbench.range("WEEK");
        workbench.awaitText("Complete recurring Week");

        workbench.type("#lesson-search", "Science");
        assertTrue(workbench.awaitText("Search matches: 1").contains("Complete school population"));
        workbench.expect("document.querySelector('.lesson-cell.search-match')");
        workbench.expect("[...document.querySelectorAll('[data-lesson-id]')].every(button => !button.hidden)");

        workbench.search("not-present");
        String noMatch = workbench.awaitText("Search matches: 0");
        assertTrue(noMatch.contains("Complete school population"));
        assertFalse(noMatch.contains("This narrowed view is empty"));
        workbench.resetView();
        workbench.awaitText("Complete school population");

        workbench.press("[data-lesson-id=lesson-math-1]", "Space");
        String details = workbench.awaitText("Accepted assignment");
        assertTrue(details.contains("Mathematics 1"));
        assertTrue(details.contains("Year 7A"));
        workbench.click(".lesson-panel details > summary");
        assertTrue(workbench.awaitText("kernel term cohort").contains("lesson-math-1"));

        workbench.press("[data-open-focus=teacherId]", "Space");
        String focused = workbench.awaitText("Teacher schedule · Alex");
        assertTrue(focused.contains("Monday"));
        assertTrue(focused.contains("Return to whole-school matrix"));
        workbench.returnToMatrix();
        workbench.expect("document.querySelector('.workspace-card').classList.contains('compact-density') && !document.querySelector('[data-density]')");

        workbench.viewport(390, 844);
        String narrow = workbench.awaitText("Read-only focused schedule");
        assertTrue(narrow.contains("Class schedule · Year 7A"));
        assertFalse(narrow.contains("Accept as current"));
        workbench.expectNot("document.querySelector('.matrix-wrap')");
        assertEquals(before, storedDocument(), "UC-3 G6 inspection must not mutate accepted workspace state");
    }

    @Test
    @DisplayName("Week period number and shared time fit on one line without stretching lesson rows")
    void displaysSharedWeekTimeBesidePeriodNumber() {
        ObjectNode document = fixtures.acceptedDocument(false);
        var periods = document.path("acceptedBaseline").path("definition").path("periods");
        for (int index : new int[] { 0, 3 }) {
            ((ObjectNode) periods.get(index)).put("startTime", "08:00:00").put("endTime", "08:40:00");
        }
        storeAccepted(document);

        workbench.open().awaitText("Complete recurring Week");
        JsonNode layout = workbench.value("""
                (() => {
                  const period = document.querySelector('.week-matrix tbody .week-period:has(small)');
                  const number = period.querySelector('span').getBoundingClientRect();
                  const time = period.querySelector('small').getBoundingClientRect();
                  const row = period.closest('tr').getBoundingClientRect();
                  return {text:period.querySelector('small').textContent,
                    inline:time.left > number.right && time.top < number.bottom,
                    fits:period.scrollWidth <= period.clientWidth + 1,
                    rowHeight:row.height};
                })()""");
        assertEquals("08:00–08:40", layout.path("text").stringValue());
        assertTrue(layout.path("inline").booleanValue(), "period number and time share a single line");
        assertTrue(layout.path("fits").booleanValue(), "period time fits without overlapping weekday columns");
        assertTrue(layout.path("rowHeight").doubleValue() <= 36, "timed period does not add empty space to lesson rows");
    }

    @Test
    @DisplayName("Timetable UX polish UC-1: scale Current workbench retains Week/Day, focus, selection, inspector, and Utilities through failed export and narrow view")
    void inspectsPolishedCurrentWorkbench() {
        ObjectNode document = fixtures.scaleDocument();
        String acceptedRevision = document.path("acceptedBaseline").path("result").path("timetableRevision")
                .asText(document.path("timetableRevision").stringValue());
        storeAccepted(document);
        String before = storedDocument();

        String current = workbench.open().awaitText("Showing 60 of 60 classes");
        assertTrue(current.contains("Scale School"));
        assertTrue(current.contains("Accepted baseline · current timetable"));
        assertEquals("Accepted revision: " + acceptedRevision,
                workbench.string("document.querySelector('.accepted-heading .revision')?.textContent"),
                "header must identify the accepted revision");
        workbench.expect("document.querySelector('#workbench-modes [data-mode=\"CURRENT\"]')?.getAttribute('aria-pressed') === 'true' && document.querySelectorAll('#workbench-modes [data-mode]').length === 1",
                "only Current is available in accepted state");
        workbench.expect("document.querySelector('#accepted-view .week-matrix') && document.querySelectorAll('#accepted-view [data-lesson-id]').length === 1000",
                "complete whole-school Week must represent every scale lesson");

        workbench.range("DAY");
        assertTrue(workbench.awaitText("Day · Monday").contains("Complete school population"));
        workbench.expect("document.querySelectorAll('#accepted-view .matrix:not(.week-matrix) [data-lesson-id]').length === 204",
                "Monday has 17 groups of 12 assignments");
        workbench.range("WEEK");
        workbench.awaitText("Complete recurring Week");
        workbench.investigateSubject("subject-0");
        assertTrue(workbench.awaitText("Subject matches: 60").contains("Complete school population"));
        workbench.investigateSubject("");
        workbench.filterClass("cohort-0");
        workbench.awaitText("Filtered whole-school matrix");
        workbench.selectLesson("lesson-0");
        assertTrue(workbench.awaitText("Accepted assignment").contains("Declared lesson 0"));
        workbench.expect("document.querySelector('#lesson-details-host')?.contains(document.querySelector('#lesson-panel-title')) && document.querySelector('#workbench-inspector')?.contains(document.querySelector('#lesson-panel-title')) && document.querySelector('#lesson-panel-title')?.textContent === 'Declared lesson 0'",
                "exact selection belongs to the side inspector");
        workbench.click("#lesson-details-host details > summary");
        String acceptedFields = workbench.string("document.querySelector('#lesson-details-host').innerText");
        for (String field : new String[] { "Subject 0", "Class 0", "Teacher 0", "Declared period 0", "Room 0", "lesson-0" }) {
            assertTrue(acceptedFields.contains(field), field);
        }

        workbench.openFocus("cohortId");
        workbench.awaitText("Class schedule · Class 0");
        workbench.returnToMatrix();
        assertTrue(workbench.awaitText("Filtered whole-school matrix").contains("Class: Class 0"));
        workbench.expect("document.querySelector('#cohort-filter').value === 'cohort-0' && document.querySelector('[data-range=WEEK]').getAttribute('aria-pressed') === 'true' && document.querySelector('[data-lesson-id=lesson-0]').getAttribute('aria-pressed') === 'true'",
                "focused return restores range, filter, and selection");

        String layout = "({width:document.querySelector('.canvas-region').getBoundingClientRect().width, inspector:document.querySelector('#workbench-inspector').getBoundingClientRect().width, selected:document.querySelector('[data-lesson-id=lesson-0]').getAttribute('aria-pressed'), summary:document.querySelector('#inspector-summary')?.innerText, requests:performance.getEntriesByType('resource').filter(entry => entry.name.endsWith('/api/workspace')).length})";
        JsonNode open = workbench.value(layout);
        assertTrue(open.path("inspector").doubleValue() > 200, "open inspector must occupy a fixed desktop column");
        workbench.collapseInspector();
        JsonNode collapsed = workbench.value(layout);
        assertTrue(collapsed.path("width").doubleValue() > open.path("width").doubleValue(), "collapse expands the canvas");
        assertEquals("true", collapsed.path("selected").stringValue());
        assertTrue(collapsed.path("summary").stringValue().contains("Declared lesson 0"), "collapsed inspector retains a visible selection summary");
        assertEquals(open.path("requests").intValue(), collapsed.path("requests").intValue(), "collapse must not reload workspace");
        workbench.reopenInspector();
        workbench.expect("document.querySelector('#workbench-inspector').getBoundingClientRect().width === " + open.path("inspector").doubleValue() + " && document.querySelector('#lesson-panel-title')?.textContent === 'Declared lesson 0'",
                "reopen restores the same width and exact selection");

        workbench.expect("!document.querySelector('#utilities').open && !document.querySelector('#export-accepted')?.getClientRects().length",
                "export starts inside the closed Utilities disclosure");
        workbench.click("#utilities > summary");
        assertTrue(workbench.awaitText("Download verified accepted bundle").contains("Export accepted baseline"));
        workbench.expect("document.querySelector('#utilities').contains(document.querySelector('#export-accepted')) && !/Discard|Cancel run|Accept as current|Revise|Import/.test(document.querySelector('#utilities').innerText)",
                "Utilities contains export, not lifecycle or import actions");
        workbench.blockRequests("**/api/accepted/export*");
        workbench.click("#export-accepted");
        assertTrue(workbench.awaitText("No accepted bundle was produced").contains("Declared lesson 0"));
        workbench.expect("document.querySelector('#workbench-modes [data-mode=\"CURRENT\"]')?.getAttribute('aria-pressed') === 'true' && document.querySelector('#cohort-filter').value === 'cohort-0' && document.querySelector('[data-lesson-id=lesson-0]').getAttribute('aria-pressed') === 'true'",
                "failed export retains Current, filter, and selection");
        assertEquals(before, storedDocument(), "failed export cannot change the accepted bundle or workspace version");

        workbench.viewport(390, 844);
        String narrow = workbench.awaitText("Read-only focused schedule");
        assertTrue(narrow.contains("Accepted baseline"));
        assertFalse(narrow.contains("Complete school population"));
        workbench.expectNot("document.querySelector('.matrix-wrap, #start-repair-form, #apply-pin, #cancel-run, #accept-repair')",
                "narrow view must not expose desktop canvas or mutation controls");
        assertEquals(before, storedDocument(), "UC-1 workbench presentation and failed export must preserve exact durable state");
    }

    @Test
    @DisplayName("Workbench layout UC-1: compact complete Current, honest Filters, inspector and read-only breakpoints")
    void inspectsCompactWideCurrentWorkbench() throws Exception {
        storeAccepted(fixtures.investigationScaleDocument());
        String before = storedDocument();

        assertTrue(workbench.open().awaitText("Showing 60 of 60 classes").contains("Scale School"));
        Set<String> represented = workbench.renderedLessonIds();
        assertEquals(1_000, represented.size(), "UC-1 main 2: every verified accepted lesson is represented");
        for (int index = 0; index < 1_000; index++) {
            assertTrue(represented.contains("lesson-" + index), "missing lesson-" + index);
        }
        JsonNode wide = workbench.value("""
                (() => {
                  const shell = document.querySelector('.workspace-card');
                  const wrap = document.querySelector('.week-wrap');
                  const slot = document.querySelector('.week-slot:has([data-lesson-id=lesson-1])');
                  const inspector = document.querySelector('#workbench-inspector');
                  const group = document.querySelector('.week-matrix tbody');
                  return {shell:shell.getBoundingClientRect().width, wrap:wrap.clientWidth,
                    content:wrap.scrollWidth, slot:slot.getBoundingClientRect().height,
                    row:group.rows[0].getBoundingClientRect().height,
                    classHeight:group.querySelector('.week-class').getBoundingClientRect().height,
                    groupHeight:group.getBoundingClientRect().height,
                    inspector:inspector.getBoundingClientRect().width,
                    days:document.querySelectorAll('.week-matrix thead th').length - 2,
                    empty:document.querySelectorAll('.week-slot .empty-cell').length,
                    filters:document.querySelector('#filters').open,
                    utilities:document.querySelector('#utilities').open,
                    task:document.querySelector('#workbench-task-area').hidden,
                    toolbarOutside:!wrap.contains(document.querySelector('.inspection-toolbar'))};
                })()""");
        assertTrue(wide.path("shell").doubleValue() >= 1520, "UC-1 G1: shell occupies at least 95% of 1600 px");
        assertTrue(wide.path("content").doubleValue() <= wide.path("wrap").doubleValue() + 1,
                "UC-1 G1: five weekdays fit with inspector open");
        assertTrue(wide.path("slot").doubleValue() <= 36, "UC-1 G2: ordinary occupied Week slot is compact");
        assertTrue(wide.path("row").doubleValue() <= 42, "Week period rows fit their lesson tiles without a fixed Day-cell height");
        assertEquals(wide.path("groupHeight").doubleValue(), wide.path("classHeight").doubleValue(), 1,
                "the Class cell spans the full height of its compact period rows");
        assertEquals(5, wide.path("days").intValue());
        workbench.expect("(() => { const table=document.querySelector('.week-matrix'); const group=table.tBodies[0]; const periods=[...group.querySelectorAll('.week-period')]; const cohort=group.querySelector('.week-class').getBoundingClientRect(); const period=periods[0].getBoundingClientRect(); return table.tHead.rows[0].cells.length === 7 && table.tBodies.length === 60 && group.querySelectorAll('.week-class').length === 1 && group.querySelector('.week-class').textContent === 'Class 0' && cohort.width <= 110 && period.left >= cohort.right - 2 && periods.length === 12 && periods[0].textContent.includes('1') && periods[11].textContent.includes('12') && periods[0].title === 'Declared period 0' && !table.querySelector('td .week-period') && group.rows[0].cells.length === 7; })()",
                "Week shows a single class label and one ordered time column alongside five weekdays");
        workbench.expect("(() => { const first=document.querySelector('[data-lesson-id=lesson-0]'); const other=document.querySelector('[data-lesson-id=lesson-60]'); return first.dataset.subjectId !== other.dataset.subjectId && getComputedStyle(first).backgroundColor !== getComputedStyle(other).backgroundColor && getComputedStyle(first).borderLeftColor !== getComputedStyle(other).borderLeftColor; })()",
                "distinct subjects have distinct visible Week tile colors");
        assertTrue(wide.path("empty").intValue() > 0, "UC-1 ext 2a: declared empty positions are retained");
        assertTrue(wide.path("inspector").doubleValue() >= 200);
        assertFalse(wide.path("filters").booleanValue());
        assertFalse(wide.path("utilities").booleanValue());
        assertTrue(wide.path("task").booleanValue(), "Current task area starts closed");
        assertTrue(wide.path("toolbarOutside").booleanValue());
        Workbench.MutationLog mutations = workbench.recordMutations();
        workbench.screenshot("uc1-current-1600.png");
        workbench.range("DAY");
        workbench.screenshot("uc1-current-day-1600.png");
        double ordinaryDay = workbench.value("document.querySelector('[data-lesson-id=lesson-60]').closest('td').getBoundingClientRect().height").doubleValue();
        assertTrue(ordinaryDay <= 60, "UC-1 G2: ordinary occupied Day cell is compact: " + ordinaryDay);
        workbench.range("WEEK");

        workbench.search("Sixteen");
        workbench.investigateSubject("subject-0");
        workbench.investigateTeacher("teacher-16");
        workbench.expect("document.querySelector('#filter-title').textContent === 'Complete school population' && document.querySelector('#represented-lesson-count').textContent === 'Represented lessons: 1000' && document.querySelector('.teacher-ribbon').textContent.includes('Unavailable')",
                "UC-1 main 3-4: search and highlights do not narrow and availability is visible");
        workbench.openDisclosure("#filters");
        workbench.filterClass("cohort-16");
        workbench.expect("document.querySelector('#represented-lesson-count').textContent === 'Represented lessons: 40' && document.querySelector('#filter-title').textContent === 'Filtered whole-school matrix'",
                "UC-1 main 4: explicit class narrowing intersects with the highlighted subject and teacher only when requested");
        workbench.filterRoom("room-99");
        workbench.closeDisclosure("#filters");
        workbench.expect("document.querySelector('#represented-lesson-count').textContent === 'Represented lessons: 0' && !document.querySelector('#no-matches').hidden && document.querySelectorAll('.week-matrix tbody').length === 60 && !document.querySelector('#clear-filters').hidden && document.querySelector('#active-criteria').textContent.includes('Room: Room 99')",
                "UC-1 ext 4a: zero matches retain declared time structure and visible closed-filter summary");
        workbench.click("#clear-filters");
        workbench.expect("document.querySelector('#represented-lesson-count').textContent === 'Represented lessons: 1000' && document.querySelector('#lesson-search').value === 'Sixteen' && document.querySelector('#subject-investigation').value === 'subject-0' && document.querySelector('#teacher-investigation').value === 'teacher-16'",
                "UC-1 main 4: clearing explicit narrowing retains search and highlights");

        workbench.selectLesson("lesson-960");
        workbench.expect("document.querySelector('#lesson-panel-title').textContent === 'Declared lesson 960' && document.querySelector('#workbench-inspector').textContent.includes('Class Sixteen with a deliberately long authoritative display name') && document.querySelector('[data-lesson-id=lesson-960]').getAttribute('aria-label').includes('Monday · Declared period 0 · lesson-960')",
                "UC-1 main 6: inspector and accessible name retain authoritative full details and identity");
        workbench.openFocus("teacherId");
        workbench.awaitText("Teacher schedule · Teacher Sixteen");
        workbench.returnToMatrix();
        workbench.expect("document.querySelector('[data-lesson-id=lesson-960]').getAttribute('aria-pressed') === 'true' && document.querySelector('#lesson-search').value === 'Sixteen'",
                "UC-1 main 5-6: focused return restores selection and investigation");
        workbench.collapseInspector();
        workbench.expect("document.querySelector('#workbench-inspector').hidden && document.querySelector('#inspector-summary').textContent.includes('Declared lesson 960')");
        workbench.reopenInspector();
        workbench.click("#open-repair-setup");
        assertEquals(before, storedDocument(), "UC-1 ext 5a: merely opening repair setup writes nothing");
        workbench.click("#close-repair-setup");
        assertEquals(before, storedDocument(), "UC-1 ext 5a: closing unstaged setup writes nothing");
        assertEquals(List.of(), mutations.requests(), "UC-1 G4/RULE-14: inspection and unstaged repair setup issue no mutating request");

        workbench.viewport(1280, 800);
        workbench.click("#open-repair-setup");
        workbench.scrollToTop();
        JsonNode medium = workbench.value("""
                (() => { const wrap=document.querySelector('.matrix-wrap');
                  const inspector=document.querySelector('#workbench-inspector');
                  const task=document.querySelector('#workbench-task-area');
                  return {page:document.documentElement.scrollWidth, viewport:innerWidth,
                    taskHeight:task.getBoundingClientRect().height, taskTop:task.getBoundingClientRect().top,
                    matrixBottom:wrap.getBoundingClientRect().bottom,
                    inspectorLeft:inspector.getBoundingClientRect().left,
                    canvasRight:document.querySelector('.canvas-region').getBoundingClientRect().right,
                    row:document.querySelector('.week-matrix tbody tr').getBoundingClientRect().height,
                    visible:wrap.clientHeight}; })()""");
        workbench.screenshot("uc1-current-1280.png");
        assertTrue(medium.path("page").doubleValue() <= medium.path("viewport").doubleValue() + 1,
                "UC-1 G1: 1280 page has no horizontal scrolling");
        assertTrue(medium.path("inspectorLeft").doubleValue() >= medium.path("canvasRight").doubleValue(),
                "UC-1 G1: inspector stays beside the canvas at 1280");
        assertTrue(medium.path("taskHeight").doubleValue() <= 280, "UC-1 G1: task area occupies no more than 35% of 800 px");
        assertTrue(medium.path("taskTop").doubleValue() >= medium.path("matrixBottom").doubleValue(),
                "UC-1 G1: task area does not overlay the canvas");
        assertTrue(medium.path("visible").doubleValue() >= medium.path("row").doubleValue(),
                "UC-1 G1: an entire class row remains visible with setup open: " + medium);

        workbench.viewport(1279, 800);
        workbench.reload();
        workbench.expect("document.querySelector('.week-wrap') !== null");
        workbench.expect("document.querySelector('#workbench-inspector').hidden && document.querySelector('#inspector-summary').getBoundingClientRect().top >= document.querySelector('.canvas-region').getBoundingClientRect().bottom && !document.querySelector('#filters').open && !document.querySelector('#utilities').open && document.querySelector('#workbench-task-area').hidden && !document.querySelector('#lesson-panel-title')",
                "UC-1 ext 3a/G1: intermediate reload resets presentation state and stacks collapsed inspector below canvas");
        workbench.selectLesson("lesson-960");
        workbench.expect("!document.querySelector('#workbench-inspector').hidden && document.querySelector('#workbench-inspector').getBoundingClientRect().top >= document.querySelector('.canvas-region').getBoundingClientRect().bottom",
                "UC-1 main 6: selection opens below-canvas inspector at 1279");
        workbench.viewport(701, 844);
        workbench.expect("!document.querySelector('#workbench-inspector').hidden && document.querySelector('#workbench-inspector').getBoundingClientRect().top >= document.querySelector('.canvas-region').getBoundingClientRect().bottom",
                "UC-1 G1: the 701 px inspector is below the canvas");
        workbench.viewport(700, 844);
        assertTrue(workbench.awaitText("Read-only focused schedule").contains("Accepted baseline"));
        workbench.expectNot("document.querySelector('#open-repair-setup, #start-repair-form, #workbench-modes, .matrix-wrap')",
                "UC-1 ext 1b: 700 px is read-only and does not claim the desktop canvas");
        assertEquals(1, workbench.value("document.querySelectorAll('.narrow-banner').length").intValue(),
                "UC-1 ext 1b: narrow Current has one clear read-only notice");
        workbench.viewport(390, 844);
        assertTrue(workbench.awaitText("Read-only focused schedule").contains("Accepted baseline"));
        workbench.screenshot("uc1-current-390.png");
        assertEquals(before, storedDocument(), "UC-1 G4/minimal: every presentation action leaves the exact durable document unchanged");
    }

    @Test
    @DisplayName("UC-2 main/extensions/G1-G8/RULE-14: real browser traces exact subject teaching and teacher load without mutation")
    void tracesSubjectTeachingAndTeacherLoad() {
        storeAccepted(fixtures.acceptedDocument(false));
        String before = storedDocument();
        workbench.open().awaitText("Complete school population");

        workbench.investigateSubject("math");
        assertTrue(workbench.awaitText("Subject matches: 1").contains("Teacher matches: 0"));
        workbench.expect("[...document.querySelectorAll('[data-lesson-id]')].every(button => !button.hidden)",
                "subject highlighting must retain nonmatches");
        workbench.expect("document.querySelector('[data-lesson-id=lesson-math-1]').classList.contains('subject-match')");

        workbench.investigateTeacher("teacher-alex");
        String combined = workbench.awaitText("Dual matches: 1");
        assertTrue(combined.contains("Teacher matches: 2"));
        assertTrue(combined.contains("Teacher load by period · Alex"));
        assertTrue(combined.contains("Monday 1\nAssigned"));
        assertTrue(combined.contains("Monday 2\nAssigned"));
        assertTrue(combined.contains("Monday 3\nUnavailable"));
        assertTrue(combined.contains("Tuesday 1\nAvailable · unassigned"));
        workbench.expect("document.querySelector('[data-lesson-id=lesson-math-1]').classList.contains('dual-match')");

        workbench.selectLesson("lesson-science-1");
        workbench.click("#subject-only");
        assertTrue(workbench.awaitText("outside the active filters").contains("Subject: Mathematics"));
        workbench.expectNot("document.querySelector('#lesson-details-host .lesson-panel')");
        workbench.expect("document.querySelector('[data-lesson-id=lesson-science-1]').hidden");

        workbench.click("#teacher-only");
        String filtered = workbench.awaitText("Represented lessons: 1");
        assertTrue(filtered.contains("Teacher matches: 1"), "filtered totals must count only represented lessons");
        assertTrue(filtered.contains("Dual matches: 1"));
        workbench.range("DAY");
        String day = workbench.awaitText("Day · Monday");
        assertTrue(day.contains("Subject matches: 1"));
        assertTrue(day.contains("Teacher matches: 1"));

        workbench.click("#clear-subject");
        assertTrue(workbench.awaitText("Teacher matches: 2").contains("Subject matches: 0"));
        assertEquals("teacher-alex", workbench.string("document.querySelector('#teacher-investigation').value"));

        workbench.weekday("TUESDAY");
        workbench.awaitText("Day · Tuesday");
        workbench.investigateSubject("math");
        workbench.awaitText("Subject matches: 0");
        workbench.click("#subject-only");
        String emptyIntersection = workbench.awaitText("No lessons match the active filters");
        assertTrue(emptyIntersection.contains("Subject matches: 0"));
        assertTrue(emptyIntersection.contains("Teacher matches: 0"));

        workbench.click("#clear-subject");
        String noTeacherAssignments = workbench.awaitText("Teacher matches: 0");
        assertTrue(noTeacherAssignments.contains("Teacher load by period · Alex"));
        assertTrue(noTeacherAssignments.contains("Available · unassigned"));

        workbench.click("#clear-teacher");
        workbench.resetView();
        String cleared = workbench.awaitText("No active filters");
        assertTrue(cleared.contains("Subject matches: 0"));
        assertTrue(cleared.contains("Teacher matches: 0"));
        assertEquals(before, storedDocument(), "UC-2 investigation must not mutate accepted workspace state");
    }

    @Test
    @DisplayName("UC-2 G3/G4/G8/RULE-7/8/16: scale browser compares represented IDs and explicit/omitted teacher availability")
    void tracesScaleInvestigationAndOmittedAvailability() throws Exception {
        ObjectNode document = fixtures.investigationScaleDocument();
        assertInvestigationScaleShape(document);
        storeAccepted(document);
        String before = storedDocument();

        assertTrue(workbench.open().awaitText("Showing 60 of 60 classes").contains("Accepted baseline · current timetable"));
        workbench.investigateSubject("subject-0");
        assertInvestigationPopulation(document, null, "subject-0", null, false, false);
        workbench.investigateTeacher("teacher-16");
        assertInvestigationPopulation(document, null, "subject-0", "teacher-16", false, false);
        assertTeacherRibbon(document, "teacher-16", null);
        workbench.expect("document.querySelector('[data-lesson-id=lesson-960]').classList.contains('dual-match')");

        workbench.click("#subject-only");
        assertInvestigationPopulation(document, null, "subject-0", "teacher-16", true, false);
        workbench.click("#teacher-only");
        assertInvestigationPopulation(document, null, "subject-0", "teacher-16", true, true);
        workbench.range("DAY");
        workbench.awaitText("Day · Monday");
        assertInvestigationPopulation(document, "MONDAY", "subject-0", "teacher-16", true, true);
        assertTeacherRibbon(document, "teacher-16", "MONDAY");

        workbench.click("#clear-subject");
        assertInvestigationPopulation(document, "MONDAY", null, "teacher-16", false, true);
        workbench.click("#clear-teacher");
        assertInvestigationPopulation(document, "MONDAY", null, null, false, false);

        workbench.investigateSubject("subject-19");
        assertInvestigationPopulation(document, "MONDAY", "subject-19", null, false, false);
        workbench.click("#subject-only");
        assertInvestigationPopulation(document, "MONDAY", "subject-19", null, true, false);
        assertTrue(workbench.awaitText("This narrowed view is empty").contains("Subject: Subject 19"));
        workbench.click("#clear-subject");

        workbench.investigateTeacher("teacher-17");
        assertInvestigationPopulation(document, "MONDAY", null, "teacher-17", false, false);
        assertTeacherRibbon(document, "teacher-17", "MONDAY");
        workbench.click("#teacher-only");
        assertInvestigationPopulation(document, "MONDAY", null, "teacher-17", false, true);
        workbench.range("WEEK");
        workbench.awaitText("Complete recurring Week");
        assertInvestigationPopulation(document, null, null, "teacher-17", false, true);
        assertTeacherRibbon(document, "teacher-17", null);
        workbench.click("#clear-teacher");
        workbench.resetView();
        assertInvestigationPopulation(document, null, null, null, false, false);
        assertEquals(before, storedDocument(), "UC-2 scale investigation must not mutate accepted workspace state");
    }

    @Test
    @DisplayName("UC-3 main/extensions/G1-G8/RULE-15: real browser highlights search, narrows explicitly, and returns from focused accepted schedules")
    void narrowsAndFocusesAcceptedTimetable() {
        storeAccepted(fixtures.scaleDocument());
        String before = storedDocument();
        workbench.open().awaitText("Showing 60 of 60 classes");

        workbench.search("Subject 0");
        assertTrue(workbench.awaitText("Search matches: 60").contains("Complete school population"));
        workbench.expect("document.querySelectorAll('.lesson-cell.search-match').length === 60 && [...document.querySelectorAll('[data-lesson-id]')].every(button => !button.hidden)",
                "search must highlight without narrowing");

        workbench.search("not present");
        String emptySearch = workbench.awaitText("Search matches: 0");
        assertTrue(emptySearch.contains("Complete school population"));
        assertFalse(emptySearch.contains("This narrowed view is empty"));
        workbench.resetView();
        workbench.awaitText("No active filters");

        workbench.range("DAY");
        workbench.awaitText("Day · Monday");
        workbench.filterClass("cohort-0");
        workbench.filterTeacher("teacher-0");
        workbench.filterRoom("room-0");
        workbench.focusPeriod("period-0");
        String narrowed = workbench.awaitText("Represented lessons: 1");
        assertTrue(narrowed.contains("Filtered whole-school matrix"));
        assertTrue(narrowed.contains("Class: Class 0"));
        assertTrue(narrowed.contains("Teacher: Teacher 0"));
        assertTrue(narrowed.contains("Room: Room 0"));
        assertTrue(narrowed.contains("Period: Declared period 0"));

        workbench.filterRoom("room-1");
        String emptyFiltered = workbench.awaitText("This narrowed view is empty");
        assertTrue(emptyFiltered.contains("Filtered whole-school matrix"));
        assertTrue(emptyFiltered.contains("Reset view"));
        workbench.click("#reset-empty");
        workbench.awaitText("Complete school population");
        assertEquals(before, storedDocument(), "UC-3 search and narrowing must not mutate accepted workspace state");
    }

    @Test
    @DisplayName("UC-3 focused schedules/G4-G8/RULE-15: real browser returns from class, teacher, room, empty, and narrow agendas")
    void opensFocusedAcceptedSchedules() {
        storeAccepted(fixtures.scaleDocument());
        String before = storedDocument();
        workbench.open().awaitText("Showing 60 of 60 classes");
        narrowMondayToClassTeacherAndRoomZero();
        workbench.selectLesson("lesson-0");
        String details = workbench.awaitText("Accepted assignment");
        assertTrue(details.contains("Declared lesson 0"));
        assertTrue(details.contains("Class 0"));

        workbench.openFocus("cohortId");
        assertTrue(workbench.awaitText("Class schedule · Class 0").contains("Monday"));
        workbench.click("[data-focus-type=teacherId]");
        workbench.awaitText("Teacher schedule · Teacher 0");
        workbench.click("[data-focus-type=roomId]");
        workbench.awaitText("Room schedule · Room 0");
        assertEquals(before, storedDocument(), "UC-3 focused schedules must not mutate accepted workspace state");
    }

    @Test
    @DisplayName("UC-3 focused extensions/G4-G8/RULE-15: real browser returns from empty and narrow room agendas")
    void returnsFromEmptyAndNarrowFocusedSchedules() {
        storeAccepted(fixtures.scaleDocument());
        String before = storedDocument();
        workbench.open().awaitText("Showing 60 of 60 classes");
        narrowMondayToClassTeacherAndRoomZero();
        workbench.openFocus("roomId");
        workbench.focusEntity("room-99");
        assertTrue(workbench.awaitText("No accepted lessons are scheduled for this selection.").contains("Room 99"));
        workbench.returnToMatrix();
        assertTrue(workbench.awaitText("Day · Monday").contains("Filtered whole-school matrix"));
        workbench.expect("document.querySelector('#cohort-filter').value === 'cohort-0' && document.querySelector('#teacher-filter').value === 'teacher-0' && document.querySelector('#room-filter').value === 'room-0'",
                "return must retain the whole-school context");
        assertEquals(before, storedDocument(), "UC-3 empty focused inspection must not mutate accepted workspace state");
    }

    @Test
    @DisplayName("UC-3 extension 2a: an accepted school with no lessons retains declared classes and empty periods")
    void showsDeclaredEmptyAcceptedTimetable() {
        storeAccepted(fixtures.acceptedDocument(true));
        String before = storedDocument();
        String rendered = workbench.open().awaitText("No accepted lessons are scheduled");
        assertTrue(rendered.contains("Year 7A"));
        workbench.expect("document.querySelector('.week-matrix [data-weekday=MONDAY]')?.title === 'Monday 1' && document.querySelector('.week-matrix .week-period')?.textContent.includes('1')",
                "empty Week retains the declared weekday period name and the shared period order");
        assertTrue(rendered.contains("Empty"));
        assertTrue(rendered.contains("Current · accepted"));
        workbench.expect("document.querySelector('#workbench-modes [data-mode=CURRENT]')?.getAttribute('aria-pressed') === 'true' && document.querySelectorAll('#accepted-view [data-lesson-id]').length === 0",
                "empty accepted snapshot retains Current without inventing a lesson");
        assertEquals(before, storedDocument());
    }

    @Test
    @DisplayName("UC-1 extensions 2b, 5a, and 5b: invalid or blocked device preferences never alter accepted inspection")
    void handlesInvalidAndBlockedInspectionPreferences() {
        storeAccepted(fixtures.acceptedDocument(false));
        String before = storedDocument();
        workbench.open().awaitText("Complete recurring Week");

        for (String invalidPreference : List.of(
                "'{bad'",
                "JSON.stringify({version:2,range:'DAY',weekdayId:'MONDAY'})",
                "JSON.stringify({version:1,range:'OTHER',weekdayId:'MONDAY'})",
                "JSON.stringify({version:1,range:'DAY',weekdayId:'UNKNOWN'})",
                "'x'.repeat(1025)")) {
            workbench.page().evaluate("localStorage.setItem('" + PREFERENCE + "', " + invalidPreference + ")");
            assertTrue(workbench.open().awaitText("Complete recurring Week").contains("Week"), invalidPreference);
        }
        workbench.page().evaluate("localStorage.removeItem('" + PREFERENCE + "')");
        workbench.page().evaluate("localStorage.setItem('school-kernel.inspection.v1.other-school', JSON.stringify({version:1,range:'DAY',weekdayId:'TUESDAY'}))");
        workbench.open().awaitText("Complete recurring Week");

        workbench.page().evaluate("localStorage.setItem('" + PREFERENCE + "', JSON.stringify({version:1,range:'DAY',weekdayId:'MONDAY'}))");
        workbench.open().awaitText("Day · Monday");
        assertEquals("range,version,weekdayId",
                workbench.string("Object.keys(JSON.parse(localStorage.getItem('" + PREFERENCE + "'))).sort().join(',')"));
        workbench.selectLesson("lesson-math-1");
        workbench.weekday("TUESDAY");
        assertFalse(workbench.awaitText("outside the represented Day").contains("Accepted assignment"));
        workbench.expectNot("document.querySelector('#lesson-details-host .lesson-panel')");

        workbench.page().evaluate("void (Object.getPrototypeOf(localStorage).setItem = () => { throw new Error('blocked'); })");
        workbench.range("WEEK");
        assertTrue(workbench.awaitText("could not save the display preference").contains("Current · accepted"));
        assertEquals(before, storedDocument(), "UC-1 preference failures must not mutate accepted workspace state");
    }

    private void narrowMondayToClassTeacherAndRoomZero() {
        workbench.range("DAY");
        workbench.awaitText("Day · Monday");
        workbench.filterClass("cohort-0");
        workbench.filterTeacher("teacher-0");
        workbench.filterRoom("room-0");
        workbench.awaitText("Represented lessons: 12");
    }

    /** Independently checks the generated normative school before the browser is asked to display it. */
    private void assertInvestigationScaleShape(ObjectNode document) {
        JsonNode definition = document.path("acceptedBaseline").path("definition");
        JsonNode assignments = document.path("acceptedBaseline").path("result").path("timetable").path("assignments");
        KernelVerifier.Verification verified = verifier.verify(new ImportDocuments(definition,
                document.path("acceptedBaseline").path("result"), null, ImportDocuments.ImportMode.ACCEPTED_BASELINE));
        assertEquals(document.path("definitionRevision").stringValue(), verified.definitionRevision());
        assertEquals(document.path("timetableRevision").stringValue(), verified.timetableRevision());
        assertTrue(definition.path("cohorts").size() >= 60);
        assertTrue(definition.path("teachers").size() >= 100);
        assertTrue(definition.path("rooms").size() >= 100);
        assertTrue(assignments.size() >= 900 && assignments.size() <= 1_100);
        assertEquals(60, definition.path("periods").size());
        assertTrue(definition.path("subjects").get(0).path("displayName").stringValue().length() > 50);
        assertTrue(definition.path("teachers").get(16).path("displayName").stringValue().length() > 50);
        assertTrue(definition.path("cohorts").get(16).path("displayName").stringValue().length() > 50);
        assertTrue(definition.path("rooms").get(16).path("displayName").stringValue().length() > 50);
        assertEquals("subject-0", definition.path("lessons").get(960).path("subjectId").stringValue());
        assertEquals("subject-0", assignments.get(960).path("subjectId").stringValue());
        assertEquals("teacher-16", assignments.get(960).path("teacherId").stringValue());
        assertEquals(41, definition.path("teachers").get(16).path("availablePeriodIds").size());
        assertEquals("period-40", definition.path("teachers").get(16).path("availablePeriodIds").get(40).stringValue());
        assertFalse(definition.path("teachers").get(17).has("availablePeriodIds"));
        Set<String> lessonIds = new HashSet<>();
        for (JsonNode assignment : assignments) lessonIds.add(assignment.path("lessonId").stringValue());
        assertEquals(assignments.size(), lessonIds.size(), "every generated assignment has one unique declared lesson ID");
        assertEquals(definition.path("lessons").size(), lessonIds.size());
        for (JsonNode assignment : assignments) {
            int ordinal = Integer.parseInt(assignment.path("lessonId").stringValue().substring("lesson-".length()));
            JsonNode lesson = definition.path("lessons").get(ordinal);
            assertEquals(lesson.path("id"), assignment.path("lessonId"));
            assertEquals(lesson.path("subjectId"), assignment.path("subjectId"));
            assertEquals(lesson.path("cohortId"), assignment.path("cohortId"));
            assertEquals(lesson.path("teacherId"), assignment.path("teacherId"));
        }
        assertEquals("cohort-16", assignments.get(960).path("cohortId").stringValue());
        assertEquals("room-16", assignments.get(960).path("roomId").stringValue());
        assertEquals("period-0", assignments.get(960).path("periodId").stringValue());
        assertEquals("period-39", assignments.get(999).path("periodId").stringValue());
        Set<String> occupiedTeachers = new HashSet<>(), occupiedCohorts = new HashSet<>(), occupiedRooms = new HashSet<>();
        for (JsonNode assignment : assignments) {
            occupiedTeachers.add(assignment.path("teacherId").stringValue());
            occupiedCohorts.add(assignment.path("cohortId").stringValue());
            occupiedRooms.add(assignment.path("roomId").stringValue());
        }
        assertTrue(occupiedTeachers.contains("teacher-0"));
        assertFalse(occupiedTeachers.contains("teacher-17"));
        assertTrue(occupiedCohorts.contains("cohort-0"));
        assertFalse(occupiedCohorts.contains("cohort-59"));
        assertTrue(occupiedRooms.contains("room-0"));
        assertFalse(occupiedRooms.contains("room-99"));
    }

    /** The displayed population and counts must equal an independent filtering of the accepted assignments. */
    private void assertInvestigationPopulation(JsonNode document, String weekday, String subject, String teacher,
                                               boolean subjectOnly, boolean teacherOnly) {
        JsonNode baseline = document.path("acceptedBaseline");
        Set<String> periodIds = new HashSet<>();
        for (JsonNode period : baseline.path("definition").path("periods")) {
            if (weekday == null || weekday.equals(period.path("weekday").stringValue())) periodIds.add(period.path("id").stringValue());
        }
        Set<String> represented = new HashSet<>(), subjectMatches = new HashSet<>(), teacherMatches = new HashSet<>(), dualMatches = new HashSet<>();
        for (JsonNode assignment : baseline.path("result").path("timetable").path("assignments")) {
            if (!periodIds.contains(assignment.path("periodId").stringValue())) continue;
            boolean matchesSubject = subject != null && subject.equals(assignment.path("subjectId").stringValue());
            boolean matchesTeacher = teacher != null && teacher.equals(assignment.path("teacherId").stringValue());
            if (subjectOnly && !matchesSubject || teacherOnly && !matchesTeacher) continue;
            String id = assignment.path("lessonId").stringValue();
            represented.add(id);
            if (matchesSubject) subjectMatches.add(id);
            if (matchesTeacher) teacherMatches.add(id);
            if (matchesSubject && matchesTeacher) dualMatches.add(id);
        }
        String rendered = workbench.awaitText("Represented lessons: " + represented.size());
        assertTrue(rendered.contains("Subject matches: " + subjectMatches.size()));
        assertTrue(rendered.contains("Teacher matches: " + teacherMatches.size()));
        assertTrue(rendered.contains("Dual matches: " + dualMatches.size()));
        JsonNode visible = workbench.value("[...document.querySelectorAll('.lesson-cell[data-lesson-id]:not([hidden])')].map(tile => tile.dataset.lessonId)");
        Set<String> visibleIds = new HashSet<>();
        for (JsonNode id : visible) visibleIds.add(id.stringValue());
        assertEquals(represented, visibleIds, "the displayed population must equal the independently filtered accepted IDs");
        assertEquals(visible.size(), visibleIds.size(), "no lesson may be counted twice in the represented population");
    }

    private void assertTeacherRibbon(JsonNode document, String teacher, String weekday) {
        JsonNode baseline = document.path("acceptedBaseline");
        JsonNode definition = baseline.path("definition");
        JsonNode availability = null;
        for (JsonNode candidate : definition.path("teachers")) {
            if (teacher.equals(candidate.path("id").stringValue())) availability = candidate.path("availablePeriodIds");
        }
        JsonNode slots = workbench.value("[...document.querySelectorAll('.teacher-ribbon li')].map(li => ({period:li.querySelector('strong').textContent, state:li.querySelector('span').textContent}))");
        int index = 0;
        for (JsonNode period : definition.path("periods")) {
            if (weekday != null && !weekday.equals(period.path("weekday").stringValue())) continue;
            boolean assigned = false, available = availability == null || availability.isMissingNode();
            for (JsonNode assignment : baseline.path("result").path("timetable").path("assignments")) {
                if (teacher.equals(assignment.path("teacherId").stringValue()) && period.path("id").equals(assignment.path("periodId"))) assigned = true;
            }
            if (!available) for (JsonNode allowed : availability) {
                if (allowed.equals(period.path("id"))) available = true;
            }
            assertEquals(period.path("displayName").stringValue(), slots.get(index).path("period").stringValue());
            assertEquals(assigned ? "Assigned" : available ? "Available · unassigned" : "Unavailable",
                    slots.get(index).path("state").stringValue(), period.path("id").stringValue());
            index++;
        }
        assertEquals(index, slots.size());
    }
}
