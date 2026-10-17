package org.schoolkernel.workspace;

import static org.junit.jupiter.api.Assertions.*;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import java.nio.file.*;
import java.util.*;
import com.microsoft.playwright.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SchoolSetupLandingBrowserIT {
    @TempDir Path temporary;
    @Test void landsOnBundledTimetableBeforeAnySetupInteraction() throws Exception {
        var h = new DemoPlanningBrowserIT(); h.temporary = temporary;
        h.initialize("setup-landing", Path.of("../output/playwright/school-setup-editor/UI-landing/"+System.currentTimeMillis()));
        try (Playwright pw=Playwright.create(new Playwright.CreateOptions().setEnv(Map.of("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD","1")));
             Browser browser=pw.chromium().launch(new BrowserType.LaunchOptions().setChannel("chrome").setHeadless(true));
             BrowserContext context=h.context(browser)) {
            Page p=context.newPage(); p.onPageError(h.errors::add);
            try {
                p.navigate(h.base+"/workspace/");
                p.locator("#school-selector option[value=english]").waitFor(new Locator.WaitForOptions().setState(com.microsoft.playwright.options.WaitForSelectorState.ATTACHED));
                assertThat(p.locator("#timetable-matrix")).isVisible();
                assertThat(p.locator("#school-setup")).not().isVisible();
                assertFalse((Boolean)p.locator("#school-setup").evaluate("d=>d.open"));
                assertEquals(0,((Number)p.evaluate("Object.keys(JSON.parse(localStorage.getItem('school-kernel.timetable-demo.v1')||'{}').setupDrafts||{}).length")).intValue());
                h.record("Fresh demo arrival, before any setup click: bundled school selectors and timetable matrix visible; setup closed; no setup draft created",p,"01-fresh-matrix");
                var bundle=DemoPlanningBrowserIT.JSON.readTree(p.request().get(h.base+"/api/demo/timetables").text());
                assertEquals(4,bundle.path("schools").size());
                for(var school:bundle.path("schools")) {
                    String id=school.path("identifier").asString();
                    assertEquals(DemoPlanningBrowserIT.JSON.readTree(Path.of("src/main/resources/demo/timetables/"+id+"-definition.json")),school.path("definition"));
                    for(var version:school.path("timetables")) assertEquals(DemoPlanningBrowserIT.JSON.readTree(Path.of("src/main/resources/demo/timetables/"+id+"-"+version.path("id").asString()+"-result.json")),version.path("result"));
                }
                String baseline=p.locator("#timetable-matrix").innerHTML();
                p.evaluate("""
                  async()=>{
                    const bundle=await(await fetch('/api/demo/timetables')).json(),school=bundle.schools.find(s=>s.identifier==='full');
                    const source={...school.timetables[0],definition:school.definition},a=source.result.timetable.assignments[0];
                    const draft={source,changes:{[a.lessonId]:{periodId:school.definition.periods.find(x=>x.id!==a.periodId).id}},name:'Retained week',createdAt:new Date().toISOString()};
                    localStorage.setItem('school-kernel.timetable-demo.v1',JSON.stringify({formatVersion:1,schools:{local:{name:'My saved school',definition:school.definition}},timetables:{},drafts:{'full/a':draft,'local/generated-week':{source:{...source,id:'generated-week',name:'Generated timetable'},generated:true,changes:{}}},lastTimetable:{full:'b',local:'generated-week'},selected:{schoolId:'local',timetableId:'generated-week'}}));
                    localStorage.setItem('school-kernel.definition.v1.old-school',JSON.stringify({formatVersion:1,identifier:'old-school',savedAt:new Date().toISOString(),derivedFrom:{kind:'file'},documentText:JSON.stringify({...school.definition,displayName:'Retired school'})}));
                  }
                  """);
                String retained=(String)p.evaluate("localStorage.getItem('school-kernel.timetable-demo.v1')");
                p.reload(); DemoTimetableBrowserIT.readyWait(p);
                assertThat(p.locator("#school-selector")).hasValue("full");
                assertThat(p.locator("#timetable-selector")).hasValue("a");
                assertThat(p.locator("#timetable-save-bar")).not().isVisible();
                assertThat(p.locator("#demo-notice")).not().isVisible();
                assertThat(p.locator("#generation-indicator")).not().isVisible();
                assertEquals(0,p.locator("#school-selector option[value=legacy-old-school]").count());
                assertEquals(baseline,p.locator("#timetable-matrix").innerHTML());
                assertEquals(retained,p.evaluate("localStorage.getItem('school-kernel.timetable-demo.v1')"));
                h.record("All four definitions and eight results equal authoritative resource files by value. A saved local selection, generated candidate, full-school manual draft and legacy individual-key example cannot replace the clean bundled Variant A; browser data is unchanged",p,"01b-retained-data-clean-matrix");
                p.locator("#resume-timetable-draft").click();
                assertThat(p.locator("#timetable-save-bar")).isVisible();
                assertThat(p.locator("#new-timetable-name")).hasValue("Retained week");
                p.locator("#school-selector").selectOption("local");
                assertThat(p.locator("#timetable-selector")).hasValue("generated-week");
                assertThat(p.locator("#timetable-save-bar")).isVisible();
                h.record("Saved manual changes resume only through explicit Resume; the local generated candidate remains accessible through the school selector",p,"01c-explicit-draft-resume");
                p.evaluate("localStorage.clear()"); p.reload(); DemoTimetableBrowserIT.readyWait(p);
                p.locator("#school-selector").selectOption("english");
                assertThat(p.locator("#timetable-matrix")).isVisible();
                p.reload();p.locator("#school-selector option[value=english]").waitFor(new Locator.WaitForOptions().setState(com.microsoft.playwright.options.WaitForSelectorState.ATTACHED));
                assertThat(p.locator("#timetable-matrix")).isVisible();
                assertThat(p.locator("#school-setup")).not().isVisible();
                h.record("Reload keeps timetable as landing; setup remains closed",p,"02-reloaded-matrix");
                p.locator("#school-selector").selectOption("english");
                String matrix=p.locator("#timetable-matrix").innerHTML();
                String bundled=p.request().get(h.base+"/api/demo/timetables").text();
                p.locator("#school-setup-entry").click();
                assertThat(p.locator(".setup-start")).isVisible();
                assertThat(p.locator("#setup-copy-source")).containsText("Edit a copy");
                assertThat(p.locator("#setup-new")).containsText("Create new school");
                p.locator("#school-setup").evaluate("async d=>{await Promise.all(d.getAnimations().map(a=>a.finished.catch(()=>{})));}");
                h.record("Explicit setup entry offers selected-school copy or empty new school",p,"03-setup-choice");
                p.locator("#setup-copy-source").click();
                assertThat(p.locator(".setup-source-preview .setup-interval").first()).isVisible();
                assertThat(p.locator(".setup-source-preview [data-interval-kind]").first()).isDisabled();
                h.record("Bundled source shows the compact real grid; original remains read-only until explicit conversion adoption",p,"04-source-grid");
                p.locator("#setup-convert").click();
                p.locator(".setup-confirm [data-confirm]").click();
                SchoolSetupBrowserIT.section(p,"Timetable grid");
                assertThat(p.locator("[data-interval-kind]").first()).isEnabled();
                p.locator("#setup-name").fill("My independent school copy");
                SchoolSetupBrowserIT.saved(p);
                assertTrue((Boolean)p.evaluate("()=>Object.values(JSON.parse(localStorage.getItem('school-kernel.timetable-demo.v1')).setupDrafts).some(d=>d.source?.id==='english'&&d.name==='My independent school copy'&&d.grid.intervals.length>0&&!d.retained)"));
                assertEquals(bundled,p.request().get(h.base+"/api/demo/timetables").text());
                h.record("Explicit adoption creates an editable browser-local draft from the selected school; bundled payload unchanged",p,"05-editable-copy");
                p.locator("#setup-new").click();
                assertThat(p.locator("#setup-name")).hasValue("");
                assertEquals(0,p.locator(".setup-interval").count());
                p.locator("#setup-name").fill("My empty school");
                SchoolSetupBrowserIT.saved(p);
                SchoolSetupBrowserIT.close(p);
                assertEquals(matrix,p.locator("#timetable-matrix").innerHTML());
                p.reload();p.locator("#school-setup-entry").waitFor();
                assertThat(p.locator("#school-setup")).not().isVisible();
                assertThat(p.locator("#timetable-matrix")).isVisible();
                h.record("New school starts empty; close restores exact matrix; reload with stored copy and empty draft still lands on matrix",p,"06-draft-reload-matrix");
                p.locator("#school-selector").selectOption("english");
                p.locator("#school-setup-entry").click();
                assertThat(p.locator("#setup-name")).hasValue("My empty school");
                assertTrue((Boolean)p.evaluate("()=>{const d=Object.values(JSON.parse(localStorage.getItem('school-kernel.timetable-demo.v1')).setupDrafts).find(d=>d.name==='My empty school');return !d.source&&['subjects','classes','teachers','rooms','plans'].every(k=>d[k].length===0)&&d.grid.intervals.length===0;}"));
                for(int width:List.of(320,390,1440)) {
                    p.setViewportSize(width,900);
                    assertTrue((Boolean)p.locator("#school-setup").evaluate("d=>d.scrollWidth<=d.clientWidth+1"));
                    h.record("Resumed empty school fits "+width+"px and keeps visible new/copy actions",p,"07-empty-"+width);
                }
                p.locator("[data-active-day=MONDAY]").check();
                p.locator("#setup-add-interval").click();
                p.locator("[data-interval-kind]").selectOption("Break");
                p.locator(".setup-confirm [data-apply]").click();
                SchoolSetupBrowserIT.saved(p);
                assertThat(p.locator("[data-interval-field=label]")).hasValue("Break 1");
                p.locator("#setup-interval-options > summary").click();
                p.locator("[data-interval-field=label]").fill("Morning pause");
                p.locator("[data-interval-kind]").selectOption("Lunch");
                assertThat(p.locator("[data-interval-field=label]")).hasValue("Morning pause");
                p.locator("[data-grid-day=MONDAY]").click();
                p.locator("[data-active-day=TUESDAY]").check();
                p.locator("[data-active-day=MONDAY]").click();
                p.locator(".setup-confirm [data-apply]").click();
                assertThat(p.locator("[data-grid-day=TUESDAY]")).hasAttribute("aria-current","true");
                assertThat(p.locator("#setup-add-interval")).isEnabled();
                SchoolSetupBrowserIT.saved(p);
                p.reload();p.locator("#school-setup-entry").waitFor();
                assertThat(p.locator("#school-setup")).not().isVisible();
                assertThat(p.locator("#timetable-matrix")).isVisible();
                h.record("Compact rows name new intervals by type while retaining custom names; removing the selected school day selects a remaining day; reload while setup was open still lands on the matrix",p,"08-open-setup-reload-matrix");
                assertTrue(h.errors.isEmpty(),h.errors.toString());
                Files.writeString(h.output.resolve("verdict.txt"),"PASS\n");
            } catch(Throwable failure) {h.record("FAIL: "+failure,p,"failure");Files.writeString(h.output.resolve("verdict.txt"),"FAIL: "+failure);throw failure;}
            finally {Files.writeString(h.output.resolve("ledger.md"),"# Timetable landing\n\nApplication: "+h.base+"\nIsolated installed Chrome; packaged demo.\n\n"+String.join("\n\n",h.steps)+"\n\n"+Files.readString(h.output.resolve("verdict.txt")));}
        } finally {h.stop();}
    }
}
