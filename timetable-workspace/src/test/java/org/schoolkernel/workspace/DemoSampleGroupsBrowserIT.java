package org.schoolkernel.workspace;

import static org.junit.jupiter.api.Assertions.*;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import com.microsoft.playwright.*;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Inspect the actual bundled schools and variants, without injecting fixtures. */
class DemoSampleGroupsBrowserIT {
    @TempDir Path temporary;

    @Test void splitLessonsSeparateShortSubjectGroupAndRoom() throws Exception {
        var h=new DemoPlanningBrowserIT();h.temporary=temporary;
        h.initialize("split-labels",Path.of("../output/playwright/sample-groups/labels-"+System.currentTimeMillis()));
        try(Playwright pw=Playwright.create(new Playwright.CreateOptions().setEnv(Map.of("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD","1")));
            Browser browser=pw.chromium().launch(new BrowserType.LaunchOptions().setChannel("chrome").setHeadless(true));BrowserContext context=h.context(browser)) {
            Page p=context.newPage();p.onPageError(h.errors::add);
            try {
                p.navigate(h.base+"/workspace/");p.locator("#school-selector option[value=full]").waitFor(new Locator.WaitForOptions().setState(com.microsoft.playwright.options.WaitForSelectorState.ATTACHED));
                for(String school:List.of("full","star-wars")) {
                    p.locator("#school-selector").selectOption(school);
                    p.locator("#cohort-filter").selectOption(school.equals("full")?"demo-cohort-023":"imperial-cohort-senior-8A");
                    for(int width:List.of(1440,1280)) {
                        p.setViewportSize(width,1000);
                        p.evaluate("() => new Promise(resolve=>requestAnimationFrame(()=>requestAnimationFrame(resolve)))");
                        assertTrue((Boolean)p.evaluate("""
                            () => [...document.querySelectorAll('.demo-lesson')].filter(n=>n.checkVisibility()).every(n=>Math.abs(n.getBoundingClientRect().height-32)<1)
                            """),"All whole-class and split lesson tiles have the same height");
                        Locator whole=p.locator(".demo-lesson:visible").filter(new Locator.FilterOptions().setHas(p.locator(".whole-title"))).first();
                        assertThat(whole).isVisible();
                        assertTrue((Boolean)whole.evaluate("""
                            tile => {
                              const title=tile.querySelector('.whole-title'),room=tile.querySelector('.whole-room'),teacher=tile.querySelector('.whole-teacher');
                              const a=title.getBoundingClientRect(),b=room.getBoundingClientRect(),c=teacher.getBoundingClientRect();
                              return room.textContent.trim().length>0 && teacher.textContent.trim().length>0 &&
                                getComputedStyle(room).textAlign==='left' && getComputedStyle(teacher).textAlign==='right' &&
                                b.top>=a.bottom && c.top>=a.bottom && b.right<=c.left && Math.abs(b.top-c.top)<1;
                            }
                            """),"Room is left and teacher is right beneath the whole-class subject");
                        if(school.equals("star-wars") && width==1280) {
                            assertTrue(p.locator(".audience-summary:visible").count()>0);
                            h.record("Three groups use the lesson summary when their labels cannot fit",p,school+"-"+width);
                            continue;
                        }
                        Locator tile=p.locator(".demo-lesson:visible").filter(new Locator.FilterOptions().setHas(p.locator(".split-title"))).first();
                        assertThat(tile).isVisible();
                        assertThat(tile.locator(".audience-short")).hasText("G1");
                        if(school.equals("full")) {
                            assertThat(tile.locator(".split-title")).hasText("Ingl. k.");
                            assertThat(tile.locator(".split-room")).containsText("Keelteklass");
                        }
                        assertTrue((Boolean)tile.evaluate("""
                            tile => {
                              const title=tile.querySelector('.split-title'),group=tile.querySelector('.audience-short'),room=tile.querySelector('.split-room');
                              const a=title.getBoundingClientRect(),b=group.getBoundingClientRect(),c=room.getBoundingClientRect();
                              return a.right<=b.left && c.top>=a.bottom && c.top>=b.bottom &&
                                [title,group,room].every(n=>n.scrollWidth<=n.clientWidth) && room.textContent.trim().length>0 && room.title.startsWith('Room: ') &&
                                !/Group\\s+\\d|\\bG\\d/.test(room.textContent);
                            }
                            """));
                        h.record("Short subject and group badge above a readable room: "+school+" at "+width+"px",p,school+"-"+width);
                    }
                    p.setViewportSize(390,844);assertTrue((Boolean)p.evaluate("document.documentElement.scrollWidth<=innerWidth"));
                    h.record("Narrow screen retains access to group lessons without horizontal overflow",p,school+"-phone");
                    p.setViewportSize(1440,1000);
                }
                assertTrue(h.errors.isEmpty(),h.errors.toString());Files.writeString(h.output.resolve("verdict.txt"),"PASS\n");
            }catch(Throwable failure){Files.writeString(h.output.resolve("verdict.txt"),"FAIL: "+failure);h.record("FAIL "+failure,p,"failure");throw failure;}
        }finally{h.stop();}
    }

    @Test void schoolDefinitionShowsGroupsAndRefreshesOldPreviewsWithoutLosingThem() throws Exception {
        var h=new DemoPlanningBrowserIT();h.temporary=temporary;
        h.initialize("school-splits",Path.of("../output/playwright/sample-groups/setup-"+System.currentTimeMillis()));
        try(Playwright pw=Playwright.create(new Playwright.CreateOptions().setEnv(Map.of("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD","1")));
            Browser browser=pw.chromium().launch(new BrowserType.LaunchOptions().setChannel("chrome").setHeadless(true));BrowserContext context=h.context(browser)) {
            Page p=context.newPage();p.onPageError(h.errors::add);
            try {
                p.navigate(h.base+"/workspace/");p.locator("#school-setup-entry").waitFor();
                SchoolSetupBrowserIT.open(p);SchoolSetupBrowserIT.section(p,"Classes");
                p.locator("[data-record=demo-cohort-017]").click();
                assertThat(p.locator(".setup-splits-preview")).containsText("Inglise keele rühmad");
                assertThat(p.locator(".setup-splits-preview tbody td").first()).hasText("Group 1");
                assertEquals(2,p.locator(".setup-splits-preview tbody tr").count());
                assertThat(p.locator(".setup-splits-preview")).containsText("3 lessons per week in groups");
                h.record("Bundled Kasevälja definition: 7A shows English split, Group 1/2, sizes and weekly teaching without conversion or JSON",p,"01-definition-groups");
                SchoolSetupBrowserIT.close(p);
                Object old=p.evaluate("""
                    () => {
                      const key='school-kernel.timetable-demo.v1',state=JSON.parse(localStorage.getItem(key));
                      const draft=Object.values(state.setupDrafts).find(d=>d.source?.id==='full');
                      for(const c of [...draft.classes,...draft.retained.cohorts])delete c.splits;
                      localStorage.setItem(key,JSON.stringify(state));return structuredClone(draft);
                    }
                    """);
                p.reload();p.locator("#school-setup-entry").waitFor();SchoolSetupBrowserIT.open(p);
                assertThat(p.locator(".setup-source-notice")).containsText("school has changed");
                p.locator("[data-current-source]").click();SchoolSetupBrowserIT.section(p,"Classes");p.locator("[data-record=demo-cohort-017]").click();
                assertEquals(2,p.locator(".setup-splits-preview tbody tr").count());
                assertEquals(old,p.evaluate("old=>JSON.parse(localStorage.getItem('school-kernel.timetable-demo.v1')).setupDrafts[old.id]",old));
                h.record("An older saved definition preview is disclosed; opening the current school copy restores visible groups and preserves the earlier snapshot exactly",p,"02-current-source");
                p.locator("#setup-convert").click();p.locator(".setup-confirm [data-confirm]").click();
                p.locator("[data-record=demo-cohort-017]").click();
                assertEquals(List.of("Group 1","Group 2"),p.locator("[data-group][data-split-field=displayName]").all().stream().map(Locator::inputValue).toList());
                h.record("Explicit conversion retains Group 1/2 in the normal editable class split forms",p,"03-editable-groups");
                SchoolSetupBrowserIT.close(p);p.locator("#school-selector").selectOption("star-wars");SchoolSetupBrowserIT.open(p);SchoolSetupBrowserIT.section(p,"Classes");p.locator("[data-record=imperial-cohort-intermediate-4A]").click();
                assertEquals(2,p.locator("[data-preview-split]").count());assertEquals(6,p.locator(".setup-splits-preview tbody tr").count());
                assertThat(p.locator(".setup-splits-preview")).containsText("Group 4");
                h.record("Coruscant 4A definition visibly lists both simulator and engineering splits with Group 1 through Group 4",p,"04-nested-groups");
                assertTrue(h.errors.isEmpty(),h.errors.toString());Files.writeString(h.output.resolve("verdict.txt"),"PASS\n");
            }catch(Throwable failure){Files.writeString(h.output.resolve("verdict.txt"),"FAIL: "+failure);h.record("FAIL "+failure,p,"failure");throw failure;}
            finally {Files.writeString(h.output.resolve("ledger.md"),"# School definition groups\n\n"+Instant.now()+"\nPackaged application, real Chrome, isolated local browser data.\n\n"+String.join("\n\n",h.steps)+"\n\n"+Files.readString(h.output.resolve("verdict.txt")));}
        }finally{h.stop();}
    }

    @Test void bundledGroupsAndExtendedAcademyGridAreAvailableInBothVariants() throws Exception {
        var h=new DemoPlanningBrowserIT();h.temporary=temporary;
        h.initialize("sample-groups",Path.of("../output/playwright/sample-groups/"+System.currentTimeMillis()));
        try(Playwright pw=Playwright.create(new Playwright.CreateOptions().setEnv(Map.of("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD","1")));
            Browser browser=pw.chromium().launch(new BrowserType.LaunchOptions().setChannel("chrome").setHeadless(true));
            BrowserContext context=h.context(browser)) {
            Page p=context.newPage();p.onPageError(h.errors::add);
            try {
                p.navigate(h.base+"/workspace/");p.locator("#school-selector option[value=full]").waitFor(new Locator.WaitForOptions().setState(com.microsoft.playwright.options.WaitForSelectorState.ATTACHED));
                for(String school:List.of("full","star-wars")) {
                    p.locator("#school-selector").selectOption(school);
                    for(String variant:List.of("a","b")) {
                        p.locator("#timetable-selector").selectOption(variant);
                        var cases=school.equals("full")
                            ? Map.of("demo-cohort-016","Group 1","demo-cohort-017","Group 1","demo-cohort-018","Group 2","demo-cohort-019","Group 1","demo-cohort-020","Group 2","demo-cohort-021","Group 1","demo-cohort-022","Group 2","demo-cohort-023","Group 1")
                            : Map.of("imperial-cohort-intermediate-4A","Group 1","imperial-cohort-senior-7A","Group 1","imperial-cohort-senior-8A","Group 1");
                        for(var example:cases.entrySet()) {
                            p.locator("#cohort-filter").selectOption(example.getKey());
                            assertTrue(p.locator(".audience-cell").count()>0);
                            Locator grouped=p.locator(".demo-lesson").filter(new Locator.FilterOptions().setHas(p.locator(".audience-short[title=\""+example.getValue()+"\"]"))).first();
                            p.evaluate("() => new Promise(resolve=>requestAnimationFrame(()=>requestAnimationFrame(resolve)))");
                            if(grouped.count()>0) { assertThat(grouped.locator(".audience-short")).hasText(example.getValue().replace("Group ","G")); grouped.click(); }
                            else {p.locator(".audience-summary:visible").first().click();p.locator("[data-inspect-audience-lesson]").first().click();}
                            assertThat(p.locator("#lesson-inspector")).not().containsText("Consecutive policy");
                            assertThat(p.locator("#lesson-inspector")).not().containsText("Coordination:");
                            assertThat(p.locator("#lesson-inspector")).not().containsText("Whole class");
                            assertThat(p.locator("#lesson-inspector")).not().containsText("students");
                            if(school.equals("full")) assertThat(p.locator("#lesson-inspector .audience-description")).containsText(example.getValue());
                            h.record(school+" / "+variant+" / "+example.getKey()+": bundled inspector shows group names without policy or whole-class labels",p,school+"-"+variant+"-"+example.getKey());
                            p.keyboard().press("Escape");
                        }
                        p.setViewportSize(390,844);
                        assertTrue((Boolean)p.evaluate("document.documentElement.scrollWidth<=innerWidth"));
                        h.record(school+" / "+variant+": grouped weekly timetable fits a phone",p,school+"-"+variant+"-mobile");
                        p.setViewportSize(1440,1000);
                        p.locator("#cohort-filter").selectOption("");
                    }
                }
                // Grid contents come from the real packaged bundle; all weekdays
                // expose ten slots, including spare time for rescheduling.
                assertTrue((Boolean)p.evaluate("""
                    async () => {
                      const bundle=await (await fetch('/api/demo/timetables')).json();
                      const school=bundle.schools.find(s=>s.identifier==='star-wars');
                      return ['MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY'].every(day=>school.definition.periods.filter(p=>p.weekday===day).length===10);
                    }
                    """));
                assertTrue(h.errors.isEmpty(),h.errors.toString());
                Files.writeString(h.output.resolve("verdict.txt"),"PASS\n");
            }catch(Throwable failure){Files.writeString(h.output.resolve("verdict.txt"),"FAIL: "+failure);h.record("FAIL "+failure,p,"failure");throw failure;}
            finally {Files.writeString(h.output.resolve("ledger.md"),"# Bundled school groups\n\n"+Instant.now()+"\n"+h.base+"\nPackaged application and installed Chrome; actual bundled data in both variants.\n\n"+String.join("\n\n",h.steps)+"\n\n"+Files.readString(h.output.resolve("verdict.txt")));}
        }finally {h.stop();}
    }
}
