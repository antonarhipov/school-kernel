package org.schoolkernel.workspace;

import static org.junit.jupiter.api.Assertions.*;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import java.nio.file.*;
import java.util.*;
import com.microsoft.playwright.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Phone-first visitor journey against the packaged demo, with real rendered geometry and inspection. */
class MobileTimetableBrowserIT {
    @TempDir Path temporary;

    @Test void wholeWeekFitsPhonesWithCollapsedFiltersAndPreservedInspection() throws Exception {
        var h = new DemoPlanningBrowserIT(); h.temporary = temporary;
        h.initialize("mobile-timetable", Path.of("../output/playwright/demo-timetable-experience/UC-1/mobile-" + System.currentTimeMillis()));
        try (Playwright pw = Playwright.create(new Playwright.CreateOptions().setEnv(Map.of("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD", "1")));
             Browser browser = pw.chromium().launch(new BrowserType.LaunchOptions().setChannel("chrome").setHeadless(true));
             BrowserContext owner = h.context(browser)) {
            Page p = owner.newPage(); p.onPageError(h.errors::add);
            var mutations = new ArrayList<String>();
            p.onRequest(r -> { if (!List.of("GET", "HEAD").contains(r.method())) mutations.add(r.method() + " " + r.url()); });
            try {
                p.setViewportSize(390, 844);
                DemoTimetableBrowserIT.ready(p, h.base);
                assertThat(p.locator("#school-selector")).hasValue("full");
                assertThat(p.locator("#timetable-filter-summary")).hasText("All lessons");
                assertNull(p.locator("#timetable-filter-disclosure").getAttribute("open"));
                assertFalse(p.locator("#cohort-filter").isVisible());
                assertTrue(p.locator("#timetable-filter-disclosure").boundingBox().height <= 36);
                assertEquals(520, p.locator(".demo-lesson").count());
                var bundle = DemoPlanningBrowserIT.JSON.readTree(p.request().get(h.base + "/api/demo/timetables").body());
                assertAssignmentIdentities(p, bundle, "full", "a");
                assertEquals(23, p.locator(".mobile-population-row").count());
                assertTrue((Boolean)p.locator(".matrix-wrap").evaluate("e=>e.getBoundingClientRect().top < 280"));
                assertPhoneGeometry(p, 5);
                h.record("UC-1 extension 3a/G7: fresh 390px visit collapses filters, keeps all 520 lessons across 23 class sections, and shows all five weekdays above the fold without horizontal scrolling", p, "01-phone-default");

                for (int width : List.of(320, 360, 390, 430, 700)) {
                    p.setViewportSize(width, 844); assertPhoneGeometry(p, 5);
                    assertThat(p.locator("#school-setup-entry")).not().isVisible();
                    assertThat(p.locator("#school-setup")).not().isVisible();
                    if (width == 320 || width == 700) h.record("UC-1 G7: full five-day timetable fits " + width + "px with 22px period column and visible room labels", p, "02-width-" + width);
                }
                p.setViewportSize(390, 844);
                p.locator("#timetable-filter-disclosure > summary").click();
                p.locator("#cohort-filter").selectOption("demo-cohort-001");
                assertNotNull(p.locator("#timetable-filter-disclosure").getAttribute("open"));
                assertEquals(1, p.locator(".mobile-population-row").count());
                assertThat(p.locator("#timetable-filter-summary")).hasText("1 active");
                p.locator("#lesson-search").fill("keel");
                assertTrue(p.locator(".demo-lesson.match").count() > 0);
                assertThat(p.locator("#timetable-filter-summary")).hasText("2 active");
                h.record("UC-1 main 3/extension 3a: expanding the disclosure exposes usable controls; class filtering and search highlights work, and rerender preserves the open disclosure", p, "03-expanded-filters");
                p.locator("#timetable-filter-disclosure > summary").click();
                p.locator("#timetable-selector").selectOption("b");
                assertNull(p.locator("#timetable-filter-disclosure").getAttribute("open"));
                assertThat(p.locator("#cohort-filter")).hasValue("demo-cohort-001");
                assertThat(p.locator("#lesson-search")).hasValue("keel");
                assertTrue(p.locator(".demo-lesson.match").count() > 0);
                p.locator(".demo-lesson").first().click();
                assertThat(p.locator("#lesson-inspector")).isVisible();
                assertTrue(p.locator("#inspector-period option:checked").textContent().matches(".*[0-9]{2}:[0-9]{2}.*"));
                var inspector = p.locator("#lesson-inspector").boundingBox();
                assertTrue(inspector.x >= 0 && inspector.x + inspector.width <= 391);
                String period = p.locator("#inspector-period").inputValue();
                p.setViewportSize(701, 844);
                p.locator(".week-matrix").waitFor();
                assertEquals(1, p.locator(".week-matrix").count());
                assertThat(p.locator("#cohort-filter")).isVisible();
                assertThat(p.locator("#inspector-period")).hasValue(period);
                p.setViewportSize(390, 844); assertPhoneGeometry(p, 5);
                assertThat(p.locator("#inspector-period")).hasValue(period);
                h.record("UC-1 main 3-4/G7: collapsed active filters and highlights survive variant switch; tapping shows exact timing; phone/desktop/phone resize preserves timetable, filters and selected lesson", p, "04-inspector-and-resize");
                p.keyboard().press("Escape");
                p.locator("#timetable-filter-disclosure > summary").click();
                p.locator("#day-filter").selectOption("MONDAY");
                p.locator("#timetable-filter-disclosure > summary").click();
                assertPhoneGeometry(p, 1);
                assertNull(p.locator("#timetable-filter-disclosure").getAttribute("open"));
                p.locator("#timetable-filter-disclosure > summary").click();
                p.locator("#day-filter").selectOption(""); assertPhoneGeometry(p, 5);

                p.locator("#clear-timetable-filters").click();
                assertThat(p.locator("#timetable-filter-summary")).hasText("All lessons");
                String teacher = p.locator("#teacher-filter option").nth(1).getAttribute("value");
                p.locator("#teacher-filter").selectOption(teacher);
                assertEquals("TEACHER", p.locator(".mobile-week-matrix tbody").getAttribute("data-row-kind"));
                assertPhoneGeometry(p, 5);
                String room = p.locator("#room-filter option").nth(1).getAttribute("value");
                p.locator("#room-filter").selectOption(room);
                assertThat(p.locator("#teacher-filter")).hasValue("");
                assertEquals("ROOM", p.locator(".mobile-week-matrix tbody").getAttribute("data-row-kind"));
                assertPhoneGeometry(p, 5);
                p.locator("#timetable-filter-disclosure > summary").click();
                h.record("UC-1 main 3/G7: Day dropdown switches between a single day and whole week; Clear restores all lessons; teacher and room lenses use full-width population headings and preserve five-day geometry", p, "05-room-lens");

                Map<String, Integer> counts = Map.of("full", 520, "two-class", 56, "star-wars", 540, "english", 614);
                for (String school : List.of("two-class", "star-wars", "english", "full")) {
                    p.locator("#school-selector").selectOption(school);
                    assertThat(p.locator("#timetable-filter-summary")).hasText("All lessons");
                    for (String variant : List.of("a", "b")) {
                        p.locator("#timetable-selector").selectOption(variant);
                        assertEquals(counts.get(school), p.locator(".demo-lesson").count());
                        assertAssignmentIdentities(p, bundle, school, variant);
                        assertPhoneGeometry(p, 5);
                    }
                    h.record("UC-1 main 5/G1/G7: " + school + " retains every lesson in both variants; school switch clears criteria; all five weekdays fit 390px", p, "06-school-" + school);
                }
                p.setViewportSize(1440, 1000);
                p.locator(".week-matrix").waitFor();
                assertEquals(1, p.locator(".week-matrix").count());
                assertThat(p.locator("#cohort-filter")).isVisible();
                assertEquals(520, p.locator(".demo-lesson").count());
                assertThat(p.locator("#school-setup-entry")).isVisible();
                h.record("UC-1 G7: desktop retains original compact retro matrix, expanded filters and School setup entry; mobile hides the editor", p, "07-desktop");
                p.setViewportSize(390, 844); p.reload();
                p.locator(".mobile-week-matrix").waitFor();
                assertNull(p.locator("#timetable-filter-disclosure").getAttribute("open"));
                assertEquals(520, p.locator(".demo-lesson").count()); assertPhoneGeometry(p, 5);
                assertEquals(0, Files.readAllLines(temporary.resolve("launches")).size(), "Startup and inspection launch no kernel process");
                assertTrue(mutations.isEmpty(), mutations.toString());
                assertTrue(h.errors.isEmpty(), h.errors.toString());
                h.record("UC-1 G2/G6/G7: reload starts collapsed; complete journey emits zero mutating requests or additional kernel launches and zero browser runtime errors", p, "08-reload");
                Files.writeString(h.output.resolve("verdict.txt"), "PASS\n");
            } catch (Throwable failure) {
                Files.writeString(h.output.resolve("verdict.txt"), "FAIL: " + failure + "\n");
                h.record("FAIL: " + failure.toString().split("\n")[0], p, "failure"); throw failure;
            } finally { DemoTimetableBrowserIT.ledger(h, "UC-1 mobile correction"); }
        } finally { h.stop(); }
    }

    private static void assertPhoneGeometry(Page p, int days) {
        p.locator(".mobile-week-matrix").waitFor();
        assertEquals(days + 1, p.locator(".mobile-week-matrix thead th").count());
        assertTrue((Boolean)p.evaluate("()=>document.documentElement.scrollWidth<=innerWidth+1"));
        assertTrue((Boolean)p.locator(".matrix-wrap").evaluate("e=>e.scrollWidth<=e.clientWidth+1"));
        assertTrue((Boolean)p.locator(".mobile-week-matrix").evaluate("e=>{const b=e.getBoundingClientRect();return b.x>=0&&b.right<=innerWidth}"));
        assertTrue((Boolean)p.locator(".mobile-week-matrix thead th").first().evaluate("e=>e.getBoundingClientRect().width<=24"));
        assertTrue((Boolean)p.locator(".demo-lesson").evaluateAll("nodes=>nodes.every(e=>{const b=e.getBoundingClientRect();return b.x>=0&&b.right<=innerWidth})"));
        assertTrue((Boolean)p.locator(".demo-tile-room").evaluateAll("nodes=>nodes.every(e=>getComputedStyle(e).display!=='none'&&e.getBoundingClientRect().height>0)"));
    }

    private static void assertAssignmentIdentities(Page p, tools.jackson.databind.JsonNode bundle, String schoolId, String variantId) {
        var schools = java.util.stream.StreamSupport.stream(bundle.path("schools").spliterator(), false);
        var school = schools.filter(s -> s.path("identifier").asString().equals(schoolId)).findFirst().orElseThrow();
        var variant = java.util.stream.StreamSupport.stream(school.path("timetables").spliterator(), false)
                .filter(t -> t.path("id").asString().equals(variantId)).findFirst().orElseThrow();
        var expected = java.util.stream.StreamSupport.stream(variant.path("result").path("timetable").path("assignments").spliterator(), false)
                .map(a -> a.path("lessonId").asString()).sorted().toList();
        assertEquals(expected, p.locator(".demo-lesson").evaluateAll("nodes=>nodes.map(e=>e.dataset.lessonId).sort()"));
    }
}
