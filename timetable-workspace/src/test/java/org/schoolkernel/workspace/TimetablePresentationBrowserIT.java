package org.schoolkernel.workspace;

import static org.junit.jupiter.api.Assertions.*;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import com.microsoft.playwright.*;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TimetablePresentationBrowserIT {
    @TempDir Path temporary;
    @Test void downloadsQuietlyAndShowsWorkloadOnlyInSchoolSetup() throws Exception {
        var h = new DemoPlanningBrowserIT(); h.temporary=temporary; h.plainHttp=true;
        h.initialize("timetable-presentation",Path.of("../output/playwright/timetable-presentation/"+System.currentTimeMillis()));
        try(Playwright pw=Playwright.create(new Playwright.CreateOptions().setEnv(Map.of("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD","1")));
            Browser browser=pw.chromium().launch(new BrowserType.LaunchOptions().setChannel("chrome").setHeadless(true));
            BrowserContext context=browser.newContext(new Browser.NewContextOptions().setViewportSize(1440,1000).setAcceptDownloads(true))) {
            Page p=context.newPage();p.onPageError(h.errors::add);
            try {
                DemoTimetableBrowserIT.ready(p,h.base);
                assertThat(p.locator("#audience-quality")).hasCount(0);
                assertThat(p.locator(".audience-quality:visible")).hasCount(0);
                p.locator("#cohort-filter").selectOption("demo-cohort-001");
                Download pdf=p.waitForDownload(new Page.WaitForDownloadOptions().setTimeout(90000),()->p.locator("#export-timetable-pdf").click());
                pdf.saveAs(h.output.resolve("class.pdf"));assertNull(pdf.failure());
                assertThat(p.locator("#pdf-export-status")).isHidden();assertThat(p.locator("#pdf-export-status")).hasText("");
                h.record("PDF downloads without preparation or success labels",p,"01-quiet-download");
                String stored=(String)p.evaluate("localStorage.getItem('school-kernel.timetable-demo.v1')");
                p.locator("#school-setup-entry").click();p.locator("[data-setup-quality]").click();
                assertThat(p.locator("#setup-section-title")).hasText("Student gaps and teacher workload");
                assertThat(p.locator("#school-setup .audience-quality")).isVisible();
                assertThat(p.locator("#setup-content")).containsText("Variant A");
                assertThat(p.locator("#setup-content")).containsText("actual taught periods");
                assertEquals(stored,p.evaluate("localStorage.getItem('school-kernel.timetable-demo.v1')"),"Opening monitoring does not create a setup draft");
                h.record("School setup monitoring tab is available without creating or converting a school",p,"02-setup-monitoring");
                String teacher=p.locator("#school-setup [data-quality-teacher]").first().getAttribute("data-quality-teacher");
                p.locator("#school-setup [data-quality-teacher]").first().click();
                assertThat(p.locator("#school-setup")).not().isVisible();assertThat(p.locator("#teacher-filter")).hasValue(teacher);
                assertThat(p.locator(".audience-quality:visible")).hasCount(0);
                h.record("Teacher drill-down closes setup and filters the timetable",p,"03-teacher-link");
                SchoolSetupBrowserIT.open(p);p.locator("[data-setup-quality]").click();
                assertThat(p.locator("#school-setup .audience-quality")).isVisible();assertThat(p.locator("#setup-tools")).isHidden();
                SchoolSetupBrowserIT.section(p,"Subjects");assertThat(p.locator("#setup-section-title")).hasText("Subjects");
                p.locator("[data-setup-quality]").click();assertThat(p.locator("#school-setup .audience-quality")).isVisible();
                p.locator("#setup-new").click();p.locator("[data-setup-quality]").click();
                assertThat(p.locator("#setup-content")).containsText("Select a timetable for this school");
                assertThat(p.locator("#school-setup .audience-quality")).hasCount(0);
                h.record("Monitoring works beside setup sections and does not attribute another school's totals to a new school",p,"04-setup-navigation");
                SchoolSetupBrowserIT.close(p);
                // Resource errors remain visible and successful retry clears them.
                Page failed=context.newPage();failed.onPageError(h.errors::add);
                failed.route("**/LiberationSans-*.ttf",route->route.abort());DemoTimetableBrowserIT.ready(failed,h.base);
                failed.locator("#cohort-filter").selectOption("demo-cohort-001");failed.locator("#export-timetable-pdf").click();
                assertThat(failed.locator("#pdf-export-status")).containsText("PDF fonts could not load",new com.microsoft.playwright.assertions.LocatorAssertions.ContainsTextOptions().setTimeout(90000));
                assertThat(failed.locator("#pdf-export-status")).isVisible();
                failed.unroute("**/LiberationSans-*.ttf");Download retry=failed.waitForDownload(()->failed.locator("#export-timetable-pdf").click());retry.saveAs(h.output.resolve("retry.pdf"));
                assertThat(failed.locator("#pdf-export-status")).isHidden();assertThat(failed.locator("#pdf-export-status")).hasText("");
                h.record("PDF failures remain visible; retry clears the error without adding a success label",failed,"05-retry");
                assertTrue(h.errors.isEmpty(),h.errors.toString());Files.writeString(h.output.resolve("verdict.txt"),"PASS\n");
            }catch(Throwable failure){Files.writeString(h.output.resolve("verdict.txt"),"FAIL: "+failure);h.record("FAIL "+failure,p,"failure");throw failure;}
            finally{Files.writeString(h.output.resolve("ledger.md"),String.join("\n\n",h.steps)+"\n\n"+Files.readString(h.output.resolve("verdict.txt")));}
        }finally{h.stop();}
    }
}
