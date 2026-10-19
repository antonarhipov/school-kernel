package org.schoolkernel.workspace;

import static org.junit.jupiter.api.Assertions.*;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import com.microsoft.playwright.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AudienceDemoManualBrowserIT {
    @TempDir Path temporary;
    static final String KEY="school-kernel.timetable-demo.v1";
    @Test void keepsAnExplicitGroupedAlternativeWithoutMutatingItsSource() throws Exception {
        var h=new DemoPlanningBrowserIT();h.temporary=temporary;h.initialize("audience-manual",Path.of("../output/playwright/student-groups-and-coordinated-lessons/UC-14/"+System.currentTimeMillis()+"-demo"));
        try(Playwright pw=Playwright.create(new Playwright.CreateOptions().setEnv(Map.of("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD","1")));Browser browser=pw.chromium().launch(new BrowserType.LaunchOptions().setChannel("chrome").setHeadless(true));BrowserContext context=h.context(browser)) {
            Page p=context.newPage();p.onPageError(h.errors::add);
            try {
                p.navigate(h.base+"/workspace/");p.locator("#school-setup-entry").waitFor();
                Object exact=p.evaluate("""
                  async pair => {
                    const {demoRequest}=await import('/workspace/demo-transport.js');
                    const {value}=await demoRequest('/api/demo/timetable/review',{method:'POST',body:{definition:pair.definition,result:{timetable:{assignments:pair.assignments}}}});
                    if(!value.valid)throw Error(JSON.stringify(value));
                    const exact={id:'manual',name:'Source with counted start',definition:pair.definition,result:value.result};
                    const state=JSON.parse(localStorage.getItem('school-kernel.timetable-demo.v1'))??{formatVersion:1,schools:{},timetables:{},drafts:{},lastTimetable:{},selected:null,editor:null};
                    state.schools.manual={id:'manual',name:'Manual school',definition:pair.definition};state.timetables.manual={manual:exact};
                    localStorage.setItem('school-kernel.timetable-demo.v1',JSON.stringify(state));return exact;
                  }
                """,DemoPlanningBrowserIT.JSON.readValue(Files.readString(Path.of("src/test/resources/student-groups/manual-pair.json")),Map.class));
                p.reload();p.locator("#school-selector").selectOption("manual");
                p.locator("[data-lesson-id=e2]").click();p.locator("#inspector-teacher").selectOption("TE2");assertThat(p.locator("#inspector-feedback")).containsText("fixed");assertFalse((Boolean)p.evaluate("Object.keys(JSON.parse(localStorage.getItem('"+KEY+"')).drafts).length>0"));p.keyboard().press("Escape");
                p.locator("[data-lesson-id=e1]").click();p.locator("#inspector-period").selectOption("0-4");assertThat(p.locator(".manual-combined-proposal")).containsText("h3");p.locator("#cancel-combined-change").click();assertFalse((Boolean)p.evaluate("Object.keys(JSON.parse(localStorage.getItem('"+KEY+"')).drafts).length>0"));
                p.locator("#inspector-period").selectOption("0-4");p.locator("#apply-individual-change").click();assertThat(p.locator("#save-new-timetable")).isDisabled();
                p.waitForFunction("()=>document.querySelector('#draft-feedback').textContent.includes('Must fix')",null,new Page.WaitForFunctionOptions().setTimeout(30000));
                assertTrue(p.locator(".audience-span .demo-conflict-mark").count()>0);
                assertThat(p.locator(".audience-span .demo-conflict-mark").first()).not().isVisible();
                assertEquals("1-2",p.evaluate("Object.values(JSON.parse(localStorage.getItem('"+KEY+"')).drafts)[0].source.result.timetable.assignments.find(a=>a.lessonId==='h3').periodId"));
                h.record("UC-14 demo1–4/3a/3b: fixed-policy replacement refused without draft; combined proposal cancel unchanged; provisional single lesson move preserves coincident lessons and is invalid/ineligible for keep",p,"01-provisional");
                p.locator("#inspector-period").selectOption("1-2");
                p.locator("#apply-combined-change").click();
                p.locator("#inspector-period").selectOption("0-4");p.locator("#apply-combined-change").click();
                p.locator("#inspector-teacher").selectOption("TE2");assertThat(p.locator(".manual-combined-proposal")).containsText("e1b");p.locator("#apply-combined-change").click();
                assertThat(p.locator("#save-new-timetable")).isEnabled(new com.microsoft.playwright.assertions.LocatorAssertions.IsEnabledOptions().setTimeout(30000));
                Object source=p.evaluate("JSON.parse(localStorage.getItem('"+KEY+"')).timetables.manual.manual");assertEquals(exact,source);
                p.keyboard().press("Escape");p.locator("#new-timetable-name").fill("Explicit grouped correction");p.locator("#save-new-timetable").click();assertThat(p.locator("#timetable-save-bar")).not().isVisible();
                String saved=p.locator("#timetable-selector").inputValue();assertNotEquals("manual",saved);
                assertTrue((Boolean)p.evaluate("()=>{const t=JSON.parse(localStorage.getItem('"+KEY+"')).timetables.manual['"+saved+"'];const a=t.result.timetable.assignments;return a.find(x=>x.lessonId==='e1').teacherId==='TE2'&&a.find(x=>x.lessonId==='e1b').teacherId==='TE2'&&a.find(x=>x.lessonId==='e2').teacherId==='TE'&&a.find(x=>x.lessonId==='h3').periodId==='0-4'&&t.result.score.internalGapStudentPeriods>0&&t.result.coordinationMatches.length===1}"));
                p.reload();assertThat(p.locator("#school-selector")).hasValue("full");p.locator("#school-selector").selectOption("manual");p.locator("#timetable-selector").selectOption(saved);assertEquals(exact,p.evaluate("JSON.parse(localStorage.getItem('"+KEY+"')).timetables.manual.manual"));
                h.record("UC-14 demo3a/3b/5–6: explicit combined round and whole two-session automatic teacher allocation independently reviewed; worse permitted gaps accepted; new named alternative kept/reloaded with exact recomputed matching and quality, source unchanged",p,"02-kept");
                p.locator("[data-lesson-id=e1]").click();p.locator("#inspector-room").selectOption("HR1");p.keyboard().press("Escape");
                p.onceDialog(Dialog::accept);p.locator("#discard-timetable-changes").click();assertEquals(exact,p.evaluate("JSON.parse(localStorage.getItem('"+KEY+"')).timetables.manual.manual"));
                assertTrue(h.errors.isEmpty(),h.errors.toString());Files.writeString(h.output.resolve("verdict.txt"),"PASS\n");
            }catch(Throwable f){Files.writeString(h.output.resolve("verdict.txt"),"FAIL: "+f);h.record("FAIL "+f,p,"failure");throw f;}
            finally {Files.writeString(h.output.resolve("ledger.md"),"# UC-14 demo manual browser journey\n\n"+Instant.now()+"\n"+h.base+"\nReal Chrome, disposable local browser data, actual stateless CLI review.\n\n"+String.join("\n\n",h.steps)+"\n\nOverall: "+Files.readString(h.output.resolve("verdict.txt"))+"\nBrowser/app/temp cleanup by harness.\n");}
        }finally {h.stop();}
    }
}
