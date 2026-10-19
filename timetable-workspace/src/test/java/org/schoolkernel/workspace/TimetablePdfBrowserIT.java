package org.schoolkernel.workspace;

import static org.junit.jupiter.api.Assertions.*;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import java.nio.file.*;
import java.util.*;
import com.microsoft.playwright.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Real downloads through the administrator's production controls, under the production CSP. */
class TimetablePdfBrowserIT {
    @TempDir Path temporary;

    @Test void downloadsSelectedFiltersWithClassPrecedenceAndSelectedSchedule() throws Exception {
        var h = new DemoPlanningBrowserIT(); h.temporary = temporary; h.plainHttp = true;
        h.initialize("pdf-export", Path.of("../output/playwright/timetable-pdf-export/UC-1/" + System.currentTimeMillis()));
        try (Playwright pw = Playwright.create(new Playwright.CreateOptions().setEnv(Map.of("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD", "1")));
             Browser browser = pw.chromium().launch(new BrowserType.LaunchOptions().setChannel("chrome").setHeadless(true));
             BrowserContext context = browser.newContext(new Browser.NewContextOptions().setViewportSize(1440, 1000).setAcceptDownloads(true))) {
            Page p = context.newPage(); p.onPageError(h.errors::add);
            var console = new ArrayList<String>();
            p.onConsoleMessage(message -> console.add(message.type() + ": " + message.text()));
            var traffic = new ArrayList<String>();
            p.onRequest(r -> traffic.add(r.method() + " " + r.url()));
            try {
                DemoTimetableBrowserIT.ready(p, h.base);
                assertFalse(traffic.stream().anyMatch(s -> s.contains("/vendor/pdf/")), "PDF code is lazy loaded");
                assertThat(p.locator("#school-selector")).hasValue("full");
                assertThat(p.locator("#timetable-selector")).hasValue("a");
                assertThat(p.locator("#pdf-export-dialog")).hasCount(0);
                assertThat(p.locator("[data-range]")).hasCount(0);
                assertThat(p.locator("#export-timetable-pdf")).hasAttribute("aria-label", "Export to PDF");
                assertThat(p.locator("#export-timetable-pdf svg")).isVisible();
                Download school = download(p);
                savePdf(school, h.output.resolve("school-a.pdf"));
                assertTrue(school.suggestedFilename().contains("-Variant-A-school-week"));
                assertThat(p.locator("#export-timetable-pdf svg")).isVisible();
                h.record("UC-1 main 1-4/G1-G4: selected Kasevälja school and Variant A with no class/teacher downloads one combined PDF of all class weekly timetables directly, with no chooser", p, "01-school-a");

                p.locator("#cohort-filter").selectOption("demo-cohort-001");
                p.locator("#lesson-search").fill("does-not-match");
                p.locator("#day-filter").selectOption("MONDAY");
                p.locator("#room-filter").selectOption(p.locator("#room-filter option").nth(1).getAttribute("value"));
                p.locator("#period-filter").selectOption(p.locator("#period-filter option").nth(1).getAttribute("value"));
                Download cohort = download(p);
                savePdf(cohort, h.output.resolve("class-a.pdf"));
                assertTrue(cohort.suggestedFilename().contains("-Variant-A-class-1A-week"));
                assertThat(p.locator("#cohort-filter")).hasValue("demo-cohort-001");
                assertThat(p.locator("#day-filter")).hasValue("MONDAY");
                h.record("UC-1 scope row 2/G2: selected class yields its whole week from Variant A; unrelated day/room/period/search do not truncate it; screen and storage unchanged", p, "02-class-a");

                p.locator("#clear-timetable-filters").click();
                String teacher = p.locator("#teacher-filter option").nth(1).getAttribute("value");
                p.locator("#teacher-filter").selectOption(teacher);
                Download teacherPdf = download(p);
                savePdf(teacherPdf, h.output.resolve("teacher-a.pdf"));
                assertTrue(teacherPdf.suggestedFilename().contains("-Variant-A-teacher-"));
                h.record("UC-1 scope row 3/G2: only teacher selected downloads that teacher's complete weekly timetable from Variant A", p, "03-teacher-a");

                p.locator("#cohort-filter").selectOption("demo-cohort-001");
                assertThat(p.locator("#teacher-filter")).hasValue(teacher);
                Download both = download(p);
                savePdf(both, h.output.resolve("class-teacher-a.pdf"));
                assertTrue(both.suggestedFilename().contains("-Variant-A-class-1A-week"));
                assertThat(p.locator("#cohort-filter")).hasValue("demo-cohort-001");
                assertThat(p.locator("#teacher-filter")).hasValue(teacher);
                h.record("UC-1 scope row 4/G2: class plus teacher selected yields the whole class PDF, including other teachers, without clearing either screen filter", p, "04-class-priority");

                p.locator("#timetable-selector").selectOption("b");
                assertThat(p.locator("#cohort-filter")).hasValue("demo-cohort-001");
                assertThat(p.locator("#teacher-filter")).hasValue(teacher);
                Download variantB = download(p);
                savePdf(variantB, h.output.resolve("class-teacher-b.pdf"));
                assertTrue(variantB.suggestedFilename().contains("-Variant-B-class-1A-week"));
                h.record("UC-1 G2/G5: switching schedules preserves class/teacher filters and exports actual Variant B assignments with class priority", p, "05-variant-b");

                p.locator("#school-selector").selectOption("two-class");
                p.locator("#timetable-selector").selectOption("b");
                assertThat(p.locator("#cohort-filter")).hasValue("");
                assertThat(p.locator("#teacher-filter")).hasValue("");
                Download otherSchool = download(p);
                savePdf(otherSchool, h.output.resolve("other-school-b.pdf"));
                assertTrue(otherSchool.suggestedFilename().contains("-Variant-B-school-week"));
                h.record("UC-1 G2: another selected school/schedule exports all class weeks for that school's Variant B with no inherited class or teacher", p, "06-other-school");

                String phoneClass = p.locator("#cohort-filter option").nth(1).getAttribute("value");
                p.locator("#cohort-filter").selectOption(phoneClass);
                p.setViewportSize(320, 844);
                assertTrue((Boolean)p.evaluate("document.documentElement.scrollWidth<=innerWidth+1"));
                assertFalse((Boolean)p.locator("#timetable-filter-disclosure").evaluate("d=>d.open"));
                assertThat(p.locator("#export-timetable-pdf")).isVisible();
                savePdf(download(p), h.output.resolve("phone-class-b.pdf"));
                h.record("UC-1 G4: direct selected-class download at 320px, collapsed filters, zero page overflow", p, "07-phone");

                // A fresh page has its own font cache, so a failed resource must be retried.
                Page failed = context.newPage(); failed.onPageError(h.errors::add);
                failed.route("**/LiberationSans-*.ttf", route -> route.abort());
                DemoTimetableBrowserIT.ready(failed, h.base);
                failed.locator("#export-timetable-pdf").click();
                assertThat(failed.locator("#pdf-export-status")).containsText("PDF fonts could not load",
                    new com.microsoft.playwright.assertions.LocatorAssertions.ContainsTextOptions().setTimeout(90000));
                assertThat(failed.locator("#export-timetable-pdf")).isEnabled();
                assertThat(failed.locator("#export-timetable-pdf svg")).isVisible();
                failed.unroute("**/LiberationSans-*.ttf");
                savePdf(download(failed), h.output.resolve("retry.pdf"));
                h.record("UC-1 extension 3a/G1: failed font request gives a retryable explanation; restoring resource permits actual successful download", failed, "06-retry");
                failed.close();

                assertTrue(traffic.stream().noneMatch(s -> !s.startsWith("GET ")), traffic.toString());
                assertTrue(traffic.stream().allMatch(s -> s.startsWith("GET " + h.base)), "No third-party requests");
                assertEquals("", Files.readString(temporary.resolve("launches")));
                assertTrue(h.errors.isEmpty(), h.errors.toString());

                p.setViewportSize(1440, 1000);
                p.locator("#school-selector").selectOption("two-class");
                p.locator(".demo-lesson").first().click();
                String otherRoom = (String)p.locator("#inspector-room").evaluate("s=>[...s.options].find(o=>o.value!==s.value).value");
                p.locator("#inspector-room").selectOption(otherRoom);
                p.keyboard().press("Escape");
                p.waitForFunction("()=>{const t=document.querySelector('#timetable-save-bar').textContent;return t.includes('Must fix')||t.includes('Ready to save');}");
                String draftStorage = (String)p.evaluate("JSON.stringify(localStorage)");
                int requestsBefore = traffic.size();
                Download draft = download(p);
                savePdf(draft, h.output.resolve("draft.pdf"));
                assertTrue(draft.suggestedFilename().endsWith("-draft.pdf"));
                assertEquals(draftStorage, p.evaluate("JSON.stringify(localStorage)"));
                assertThat(p.locator("#pdf-export-status")).isHidden();
                assertTrue(traffic.subList(requestsBefore, traffic.size()).stream().allMatch(s -> s.startsWith("GET ")),
                    "Export starts no additional review or solve");
                h.record("UC-1 extension 4a/G1: actual unsaved room edit exports a clearly named unpublished draft without changing stored edits or starting another review/solve", p, "07-draft");
                Files.writeString(h.output.resolve("verdict.txt"), "PASS\n");
            } catch (Throwable failure) {
                Files.writeString(h.output.resolve("verdict.txt"), "FAIL: " + failure + "\n");
                h.record("FAIL: " + failure, p, "failure"); throw failure;
            } finally {
                Files.write(h.output.resolve("traffic.txt"), traffic);
                Files.write(h.output.resolve("console.txt"), console);
                Files.writeString(h.output.resolve("ledger.md"), "# Timetable PDF export UC-1 walkthrough\n\n2026-10-18. Installed Chrome, disposable browser context and packaged local demo, real production CSP, no operator data. Source based on 3a10e51 plus timetable-pdf-export diff. Downloads retained beside screenshots for PDF rendering/text review.\n\n" + String.join("\n\n", h.steps) + "\n\n" + Files.readString(h.output.resolve("verdict.txt")) + "\nBrowser contexts and server closed by harness.\n");
            }
        } finally { h.stop(); }
    }

    private static Download download(Page page) {
        String storage = (String)page.evaluate("JSON.stringify(localStorage)");
        String selections = (String)page.locator("#school-selector, #timetable-selector, #timetable-filters select, #lesson-search")
            .evaluateAll("nodes=>JSON.stringify(nodes.map(n=>[n.id,n.value]))");
        String assignments = (String)page.locator(".demo-lesson").evaluateAll("nodes=>JSON.stringify(nodes.map(n=>n.dataset.lessonId))");
        // Allow the host's cold Java Playwright module/font processing.
        Download result = page.waitForDownload(new Page.WaitForDownloadOptions().setTimeout(90000),
            () -> page.locator("#export-timetable-pdf").click());
        assertThat(page.locator("#pdf-export-status")).isHidden();
        assertEquals(storage, page.evaluate("JSON.stringify(localStorage)"));
        assertEquals(selections, page.locator("#school-selector, #timetable-selector, #timetable-filters select, #lesson-search")
            .evaluateAll("nodes=>JSON.stringify(nodes.map(n=>[n.id,n.value]))"));
        assertEquals(assignments, page.locator(".demo-lesson").evaluateAll("nodes=>JSON.stringify(nodes.map(n=>n.dataset.lessonId))"));
        return result;
    }

    private static void savePdf(Download download, Path target) throws Exception {
        assertNull(download.failure()); download.saveAs(target);
        assertTrue(Files.size(target) > 1000);
        assertEquals("%PDF-", new String(Files.readAllBytes(target), 0, 5, java.nio.charset.StandardCharsets.US_ASCII));
    }
}
