package org.schoolkernel.workspace;

import static org.junit.jupiter.api.Assertions.*;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.WaitForSelectorState;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** D035: the real subject-facing administrator journey, without operator browser data. */
class SubjectTeachingBrowserIT {
    @TempDir Path temporary;
    static final String KEY="school-kernel.timetable-demo.v1";
    static Object teaching(Page p) {
        return p.evaluate("""
            () => {const s=JSON.parse(localStorage.getItem('school-kernel.timetable-demo.v1'));
              const d=Object.values(s.setupDrafts).find(d=>!d.retained&&d.source?.id==='full');
              return {plans:d.plans,audiencePlans:d.audiencePlans,coordinationRules:d.coordinationRules,classes:d.classes};}
            """);
    }
    static void noOverflow(Page p) {
        assertTrue((Boolean)p.evaluate("""
            () => document.documentElement.scrollWidth<=innerWidth &&
              [...document.querySelectorAll('#setup-content, #setup-record-detail, .setup-teaching-card')]
              .filter(n=>n.checkVisibility()).every(n=>n.scrollWidth<=n.clientWidth+1)
            """), "Readable content without horizontal overflow");
    }
    @Test void inspectAndEditSubjectArrangementsWithoutClassWideSplitting() throws Exception {
        var h=new DemoPlanningBrowserIT();h.temporary=temporary;
        h.initialize("subject-teaching",Path.of("../output/playwright/student-groups-and-coordinated-lessons/UC-3/subject-"+System.currentTimeMillis()));
        try(Playwright pw=Playwright.create(new Playwright.CreateOptions().setEnv(Map.of("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD","1")));
            Browser browser=pw.chromium().launch(new BrowserType.LaunchOptions().setChannel("chrome").setHeadless(true));
            BrowserContext context=h.context(browser)) {
            Page p=context.newPage();p.onPageError(h.errors::add);
            try {
                p.navigate(h.base+"/workspace/");p.locator("#school-selector option[value=full]").waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.ATTACHED));
                p.locator("#school-selector").selectOption("full");SchoolSetupBrowserIT.open(p);SchoolSetupBrowserIT.section(p,"Subjects");
                p.locator("#setup-search").fill("Inglise keel");p.locator("[data-record=demo-subject-010]").click();
                Locator card=p.locator("[data-subject-class=demo-cohort-018]");
                assertThat(card).containsText("Grouped");assertThat(card).containsText("Inglise keele rühmad");
                assertThat(card).containsText("Group 1 · 12 students · 3 lessons per week");assertThat(card).containsText("Group 2 · 12 students · 3 lessons per week");
                assertThat(card).containsText("Fixed teacher:");assertThat(card).containsText("Keelteklass");assertThat(card).containsText("Parallel teaching: 3 starts together per week");
                assertThat(p.locator("#setup-record-detail pre")).not().isVisible();
                card.scrollIntoViewIfNeeded();noOverflow(p);h.record("UC-3 G5: bundled English → 7B shows groups, sizes, weekly teaching, fixed teachers/rooms and three parallel starts without conversion or JSON",p,"01-english-preview");
                p.setViewportSize(768,1000);card.scrollIntoViewIfNeeded();noOverflow(p);h.record("UC-3 G7: English preview readable at 768px",p,"02-narrow-preview");p.setViewportSize(1440,1000);
                p.locator("#setup-search").fill("");p.locator("[data-record=demo-subject-001]").click();
                assertThat(p.locator(".setup-subject-teaching")).containsText("Whole class");
                h.record("UC-3 G5: ordinary subject clearly shows whole-class teaching despite available English groups",p,"03-whole-preview");
                p.locator("#setup-search").fill("Inglise keel");p.locator("[data-record=demo-subject-010]").click();
                p.locator("#setup-convert").click();p.locator(".setup-confirm [data-confirm]").click();
                SchoolSetupBrowserIT.section(p,"Subjects");p.locator("#setup-search").fill("Inglise keel");p.locator("[data-record=demo-subject-010]").click();SchoolSetupBrowserIT.saved(p);
                p.locator("#setup-search").fill("");p.locator("[data-record=demo-subject-001]").click();
                p.locator("[data-subject-teaching=demo-cohort-018]").click();assertEquals(1,p.locator("[data-plan]").count());
                p.locator("[data-back-subject-teaching]").click();
                p.locator("#setup-search").fill("Inglise keel");p.locator("[data-record=demo-subject-010]").click();SchoolSetupBrowserIT.saved(p);
                Object before=teaching(p);
                p.locator("[data-subject-teaching=demo-cohort-018]").click();assertEquals(1,p.locator("[data-audience-plan]").count());
                assertThat(p.locator("[data-audience-field=splitId]")).hasValue("demo-cohort-018-english");
                p.locator("[data-manage-subject-groups]").click();assertThat(p.locator("[data-subject-arrangement]")).containsText("Inglise keel · 7B");
                p.locator("[data-remove-split]").click();assertThat(p.locator("#setup-message")).containsText("Cannot remove this split");
                SchoolSetupBrowserIT.saved(p);assertEquals(before,teaching(p));
                h.record("UC-1 3b / UC-3 G6: subject-context groups refuse referenced deletion; teaching, coordination, groups and unrelated plans compare exactly",p,"04-refused-delete");
                p.locator("[data-edit-split]").click();p.locator(".setup-confirm [data-confirm]").click();
                p.locator("[data-group=demo-cohort-018-english-1][data-split-field=displayName]").fill("English north");
                p.locator("[data-back-subject-arrangement]").click();assertThat(p.locator("[data-audience-plan]")).containsText("English north");
                p.locator("[data-back-subject-teaching]").click();assertThat(p.locator("#setup-search")).hasValue("Inglise keel");assertThat(p.locator("[data-subject-class=demo-cohort-018]")).containsText("English north");
                SchoolSetupBrowserIT.saved(p);
                Object renamed=p.evaluate("value=>{value.classes.find(c=>c.id==='demo-cohort-018').splits[0].groups[0].displayName='English north';return value;}",before);
                assertEquals(renamed,teaching(p));
                h.record("UC-1 main / UC-3 G6: referenced group rename keeps identities and returns to the English/7B plan, then subject overview with the search retained",p,"05-return-context");
                p.locator("#setup-add-record").click();p.locator("#setup-record-name").fill("Language workshop");
                assertThat(p.locator(".setup-subject-teaching")).containsText("No teaching requirements");
                p.locator("#setup-subject-class").selectOption("demo-cohort-018");p.locator("[data-add-subject-teaching]").click();
                p.locator("[data-audience-field=weeklyTotal]").fill("3");p.locator("[data-audience-field=delivery]").selectOption("mixed");
                p.locator("[data-audience-field=wholeClassCount]").fill("1");p.locator("[data-audience-field=splitId]").selectOption("demo-cohort-018-english");
                assertThat(p.locator("[data-audience-summary]")).containsText("outstanding group sessions 2");
                p.locator("[data-reconcile-audience-sessions]").click();p.locator(".setup-confirm [data-confirm]").click();
                assertThat(p.locator("[data-audience-summary]")).not().containsText("Retained 3 lessons");
                p.locator("[data-back-subject-teaching]").click();assertThat(p.locator("[data-subject-class=demo-cohort-018]")).containsText("Mixed whole class and groups");
                assertThat(p.locator("[data-subject-class=demo-cohort-018]")).containsText("1 whole class + 2 group");
                p.locator("[data-subject-teaching=demo-cohort-018]").click();p.locator("[data-manage-subject-groups]").click();
                p.locator("#setup-add-split").click();p.locator("[data-split]").last().locator("[data-split-field=displayName]:not([data-group])").fill("Workshop teams");
                p.locator("[data-back-subject-arrangement]").click();
                assertEquals(3,p.locator("[data-audience-field=splitId] option").count());
                // Reuse retains the referenced arrangement; newly created groups are independently available.
                assertThat(p.locator("[data-audience-field=splitId]")).hasValue("demo-cohort-018-english");
                SchoolSetupBrowserIT.saved(p);Object exact=teaching(p);
                p.setViewportSize(768,1000);noOverflow(p);h.record("UC-3 1b/G7: new subject → 7B creates mixed demand, reuses English audiences and creates another available arrangement without retargeting existing teaching",p,"06-create-reuse-narrow");p.setViewportSize(1440,1000);
                SchoolSetupBrowserIT.close(p);p.reload();p.locator("#school-setup-entry").waitFor();SchoolSetupBrowserIT.open(p);
                assertThat(p.locator("[data-audience-field=splitId]")).hasValue("demo-cohort-018-english");assertEquals(exact,teaching(p));
                p.locator("[data-back-subject-teaching]").click();assertThat(p.locator("[data-subject-class=demo-cohort-018]")).containsText("1 whole class + 2 group");
                h.record("UC-1 main5 / UC-3 G6: reload restores exact subject/class context, group identities, teaching and unrelated plans",p,"07-reload");
                SchoolSetupBrowserIT.close(p);p.locator("#school-selector").selectOption("star-wars");SchoolSetupBrowserIT.open(p);SchoolSetupBrowserIT.section(p,"Subjects");
                p.locator("#setup-search").fill("Mission Planning");p.locator("#school-setup [data-record]").first().click();
                assertThat(p.locator(".setup-subject-teaching")).containsText("Mixed whole class and groups");
                assertThat(p.locator(".setup-subject-teaching")).containsText("1 whole class + 3 group");
                h.record("UC-3 1c/G5: actual bundled mixed teaching shows its whole-class contribution and three-group arrangement without editing",p,"08-mixed-preview");
                assertTrue(h.errors.isEmpty(),h.errors.toString());Files.writeString(h.output.resolve("verdict.txt"),"PASS\n");
            }catch(Throwable failure){Files.writeString(h.output.resolve("verdict.txt"),"FAIL: "+failure);h.record("FAIL "+failure,p,"failure");throw failure;}
            finally {
                Files.write(h.output.resolve("browser-errors.txt"),h.errors);
                Files.writeString(h.output.resolve("ledger.md"),"# D035 — UC-1/UC-3 subject-facing teaching arrangements\n\n"+Instant.now()+"\nApplication: "+h.base+"; pinned packaged working tree, installed Chrome. Isolated browser and disposable server. Exact source delta recorded in checkpoint.\n\n"+String.join("\n\n",h.steps)+"\n\nVerdict: "+Files.readString(h.output.resolve("verdict.txt"))+"\nScreenshots and text snapshots retained; browser context disposed and server stopped. No operator data, commits or deployment.\n");
            }
        }finally{h.stop();}
    }
}
