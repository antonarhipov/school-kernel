package org.schoolkernel.workspace;

import static org.junit.jupiter.api.Assertions.*;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.schoolkernel.workspace.SchoolSetupBrowserIT.*;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import com.microsoft.playwright.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.node.ObjectNode;

class AudienceWorkspaceBrowserIT {
    @TempDir Path temporary;
    @Test void generateInspectKeepAndReopenExactGroupedPair() throws Exception {
        var h=new DemoPlanningBrowserIT();h.temporary=temporary;
        h.initialize("audience-generation",Path.of("../output/playwright/student-groups-and-coordinated-lessons/UC-10/"+System.currentTimeMillis()));
        h.stop();h.start(List.of("--demo.reuse.enabled=false"),2);
        try(Playwright pw=Playwright.create(new Playwright.CreateOptions().setEnv(Map.of("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD","1")));Browser browser=pw.chromium().launch(new BrowserType.LaunchOptions().setChannel("chrome").setHeadless(true));BrowserContext context=h.context(browser)) {
            Page p=context.newPage();p.onPageError(h.errors::add);
            try {
                p.navigate(h.base+"/workspace/");open(p);p.locator("#setup-new").click();
                var document=(ObjectNode)DemoPlanningBrowserIT.JSON.readTree(Files.readString(Path.of("src/test/resources/student-groups/generation-authoring.json")));
                // Explicit short-day boundaries make this small mixed-demand fixture feasible without undeclared consecutive runs.
                document.path("grid").path("intervals").forEach(i->((ObjectNode)i).put("boundaryAfter",true));
                tool(p,"#school-definition-json").fill(document.toPrettyString());p.locator("#setup-apply-json").click();
                if((Boolean)p.locator("#setup-tools").evaluate("e=>e.open"))p.locator("#setup-tools > summary").click();
                section(p,"Classes");p.locator("[data-record]").first().click();
                p.locator("[data-edit-split]").first().click();p.locator(".setup-confirm [data-confirm]").click();
                p.locator("[data-group][data-split-field=displayName]").first().fill("North Õ students — full audience label");saved(p);
                p.locator("[data-open-relationships]").click();assertThat(p.locator("#setup-relationship-issues")).containsText("consistent");assertEquals(8,p.locator("[data-union]").count());p.locator("#setup-back-relationships").click();
                p.locator("[data-open-audience-plans]").click();p.locator("[data-open-coordination]").click();p.locator("#setup-coordination-type").selectOption("GAP_FREE");p.locator("#setup-add-coordination").click();
                section(p,"Review");p.locator("#setup-name").fill("Grouped generation journey");saved(p);p.locator("#setup-save-school").click();
                assertThat(p.locator("#school-setup")).not().isVisible(new com.microsoft.playwright.assertions.LocatorAssertions.IsVisibleOptions().setTimeout(30000));
                String school=p.locator("#school-selector").inputValue();Object exactSchool=p.evaluate("JSON.parse(localStorage.getItem('"+KEY+"')).schools['"+school+"'].definition");
                open(p);section(p,"Review");p.locator("#setup-start-generation").click();
                p.waitForFunction("()=>document.querySelector('#setup-generation-status').textContent.includes('ready for naming')||document.querySelector('#setup-generation-status').textContent.includes('Close the editor')",null,new Page.WaitForFunctionOptions().setTimeout(45000));
                close(p);assertThat(p.locator("#save-new-timetable")).isEnabled(new com.microsoft.playwright.assertions.LocatorAssertions.IsEnabledOptions().setTimeout(30000));
                assertEquals(8,p.locator(".demo-lesson").count());
                open(p);p.locator("[data-setup-quality]").click();assertThat(p.locator("#setup-content")).containsText("0 student-periods");close(p);
                Object exact=p.evaluate("Object.values(JSON.parse(localStorage.getItem('"+KEY+"')).drafts).find(d=>d.generated).source");
                h.record("UC-10 main: source-linked mixed/automatic plan and custom split edited; optional hard gap-free enabled; exact named school generated through packaged kernel, independently checked; eight actual sessions and zero student gaps available",p,"01-generated");
                p.locator(".demo-lesson").first().click();assertThat(p.locator("#lesson-inspector")).not().containsText("Consecutive policy");p.keyboard().press("Escape");
                p.locator("#new-timetable-name").fill("Exact grouped alternative");
                p.evaluate("()=>{window.savedSet=Storage.prototype.setItem;Storage.prototype.setItem=function(k,v){if(k==='"+KEY+"'&&Object.keys(JSON.parse(v).timetables).length)throw new DOMException('quota','QuotaExceededError');return window.savedSet.call(this,k,v)}}");
                p.locator("#save-new-timetable").click();assertThat(p.locator("#demo-notice")).containsText("Nothing was saved");assertThat(p.locator("#new-timetable-name")).hasValue("Exact grouped alternative");
                assertTrue((Boolean)p.evaluate("Object.keys(JSON.parse(localStorage.getItem('"+KEY+"')).timetables).length===0"));
                p.evaluate("()=>{Storage.prototype.setItem=window.savedSet}");p.locator("#save-new-timetable").click();assertThat(p.locator("#timetable-save-bar")).not().isVisible();
                String timetable=p.locator("#timetable-selector").inputValue();Object kept=p.evaluate("JSON.parse(localStorage.getItem('"+KEY+"')).timetables['"+school+"']['"+timetable+"']");
                assertEquals(exactSchool,p.evaluate("JSON.parse(localStorage.getItem('"+KEY+"')).timetables['"+school+"']['"+timetable+"'].definition"));
                assertTrue((Boolean)p.evaluate("()=>{const t=JSON.parse(localStorage.getItem('"+KEY+"')).timetables['"+school+"']['"+timetable+"'];return t.result.schemaVersion===2&&t.result.limit.type==='TIME'&&t.result.weeklyTeacherAllocations.length===7&&t.result.coordinationMatches.length===t.definition.coordinationRules.length}"));
                h.record("UC-10 5a/main5: atomic quota refusal preserves candidate/name and no timetable; retry keeps exact generated pair, search budget, weekly allocations, coordination and quality",p,"02-kept");
                Files.writeString(h.output.resolve("kept-pair.json"),DemoPlanningBrowserIT.JSON.writeValueAsString(kept));
                p.reload();assertThat(p.locator("#school-selector")).hasValue("full");p.locator("#school-selector").selectOption(school);p.locator("#timetable-selector").selectOption(timetable);
                assertEquals(kept,p.evaluate("JSON.parse(localStorage.getItem('"+KEY+"')).timetables['"+school+"']['"+timetable+"']"));
                for(int width:List.of(700,320)){p.setViewportSize(width,900);assertTrue((Boolean)p.evaluate("document.documentElement.scrollWidth<=innerWidth"));assertFalse((Boolean)p.locator("#timetable-filter-disclosure").evaluate("e=>e.open"));assertEquals(2, p.locator(".audience-span[data-span-length=\"4\"] .demo-lesson:visible").count(), "Whole-class sessions retain full-width lesson controls in a nested class");h.record("UC-10 G3/G5: kept exact pair reopened explicitly after bundled startup; whole week and collapsed filters fit "+width+"px",p,"03-reopen-"+width);}
                p.setViewportSize(1440,1000);open(p);section(p,"Review");
                Files.writeString(temporary.resolve("stall"),"1");p.locator("#setup-start-generation").click();
                p.locator("#setup-cancel-generation").waitFor();p.locator("#setup-cancel-generation").click();
                p.waitForFunction("()=>document.querySelector('#setup-generation-status').textContent.includes('cancelled')",null,new Page.WaitForFunctionOptions().setTimeout(30000));Files.delete(temporary.resolve("stall"));
                assertEquals(exactSchool,p.evaluate("JSON.parse(localStorage.getItem('"+KEY+"')).schools['"+school+"'].definition"));
                Files.writeString(temporary.resolve("crash"),"1");p.locator("#setup-start-generation").click();
                p.waitForFunction("()=>document.querySelector('#setup-generation-status').textContent.includes('no complete bounded result')",null,new Page.WaitForFunctionOptions().setTimeout(30000));Files.delete(temporary.resolve("crash"));
                assertEquals(kept,p.evaluate("JSON.parse(localStorage.getItem('"+KEY+"')).timetables['"+school+"']['"+timetable+"']"));
                h.record("UC-10 3a: actual cancellation and kernel exit without result preserve the exact saved school and kept timetable; no partial candidate or accepted baseline",p,"04-cancel-failure");
                p.locator("#setup-start-generation").click();close(p);p.locator("#school-selector").selectOption("full");
                p.waitForFunction("()=>Object.values(JSON.parse(localStorage.getItem('"+KEY+"')).drafts).some(d=>d.generated)",null,new Page.WaitForFunctionOptions().setTimeout(30000));
                assertThat(p.locator("#school-selector")).hasValue("full");assertThat(p.locator("#timetable-title")).not().hasText("Generated timetable");
                p.locator("[data-view-generated]").click();assertThat(p.locator("#save-new-timetable")).isEnabled(new com.microsoft.playwright.assertions.LocatorAssertions.IsEnabledOptions().setTimeout(30000));
                p.onceDialog(Dialog::accept);p.locator("#discard-timetable-changes").click();
                assertEquals(kept,p.evaluate("JSON.parse(localStorage.getItem('"+KEY+"')).timetables['"+school+"']['"+timetable+"']"));
                h.record("UC-10 3b/4a: late completion stays associated with originating school while active bundled school is unchanged; explicit View reaches candidate; explicit discard preserves kept timetable",p,"05-late-discard");
                assertTrue(h.errors.isEmpty(),h.errors.toString());Files.writeString(h.output.resolve("verdict.txt"),"PASS\n");
            }catch(Throwable failure){Files.writeString(h.output.resolve("verdict.txt"),"FAIL: "+failure);h.record("FAIL "+failure,p,"failure");throw failure;}
            finally{Files.write(h.output.resolve("browser-errors.txt"),h.errors);Files.writeString(h.output.resolve("ledger.md"),"# UC-10 grouped generation administrator journey\n\nDate: "+Instant.now()+"\nApplication: "+h.base+"\nPackaged working tree, isolated disposable Chrome context and local server. Source imported supported authoring fixture, explicit interval boundaries.\n\n"+String.join("\n\n",h.steps)+"\n\nOverall: "+Files.readString(h.output.resolve("verdict.txt"))+"\nBrowser and server close; no operator data.\n");}
        }finally{h.stop();}
    }
    @Test void compactAudienceInspectionAndExactMonitoring() throws Exception {
        var h=new DemoPlanningBrowserIT();h.temporary=temporary;
        h.initialize("audience-inspection",Path.of("../output/playwright/student-groups-and-coordinated-lessons/UC-11/"+System.currentTimeMillis()));
        try(Playwright pw=Playwright.create(new Playwright.CreateOptions().setEnv(Map.of("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD","1")));Browser browser=pw.chromium().launch(new BrowserType.LaunchOptions().setChannel("chrome").setHeadless(true));BrowserContext context=browser.newContext(new Browser.NewContextOptions().setIgnoreHTTPSErrors(true).setHasTouch(true).setViewportSize(1440,1000))) {
            Page p=context.newPage();p.onPageError(h.errors::add);
            try {
                p.navigate(h.base+"/workspace/");p.locator("#school-setup-entry").waitFor();
                String fixtures=Files.readString(Path.of("src/test/resources/student-groups/inspection-pairs.json"));
                Object installed=p.evaluate("""
                  async fixtures => {
                    const {demoRequest}=await import('/workspace/demo-transport.js');
                    const state=JSON.parse(localStorage.getItem('school-kernel.timetable-demo.v1'))??{formatVersion:1,schools:{},timetables:{},drafts:{},lastTimetable:{},selected:null,editor:null};
                    const evidence={};
                    for(const [id,pair] of Object.entries(fixtures)) {
                      const {value}=await demoRequest('/api/demo/timetable/review',{method:'POST',body:{definition:pair.definition,result:{timetable:{assignments:pair.assignments}}}});
                      if(!value.valid)throw Error(id+': '+JSON.stringify(value));
                      const exact={id,name:id,definition:pair.definition,result:value.result};
                      state.schools[id]={id,name:id,definition:pair.definition};state.timetables[id]={[id]:exact};evidence[id]=exact;
                    }
                    localStorage.setItem('school-kernel.timetable-demo.v1',JSON.stringify(state));return evidence;
                  }
                """,DemoPlanningBrowserIT.JSON.readValue(fixtures,Map.class));
                Files.writeString(h.output.resolve("independently-reviewed-pairs.json"),DemoPlanningBrowserIT.JSON.writeValueAsString(installed));
                p.reload();p.locator("#school-selector").waitFor();
                for(String id:List.of("whole","groups2","groups3","groups4","groups8","partial","crossing","nested")) {
                    p.locator("#school-selector").selectOption(id);
                    p.waitForTimeout(100);
                    assertTrue((Boolean)p.evaluate("document.documentElement.scrollWidth<=innerWidth"));
                    if(id.equals("whole"))assertEquals(1,p.locator(".audience-span .demo-lesson:visible").count());
                    if(id.startsWith("groups")&&!id.equals("groups8")) {
                        int k=Integer.parseInt(id.substring(6));assertEquals(k,p.locator(".audience-span .demo-lesson:visible").count());
                        assertTrue((Boolean)p.locator(".audience-sections:visible").evaluate("e=>{const a=[...e.children].map(n=>n.getBoundingClientRect());return a.every(r=>r.width>=24&&Math.abs(r.top-a[0].top)<1)&&e.getBoundingClientRect().height<=22}"));
                    }
                    if(id.equals("partial")){assertEquals(1,p.locator(".audience-untaught:visible").count());p.locator(".audience-untaught:visible").click();assertThat(p.locator("#lesson-inspector")).containsText("No lesson");p.keyboard().press("Escape");}
                    if(id.equals("crossing")){assertEquals(2,p.locator(".audience-summary:visible").count());p.locator(".audience-summary:visible").first().focus();p.keyboard().press("Enter");assertEquals(2,p.locator("[data-inspect-audience-lesson]").count());p.keyboard().press("Escape");assertThat(p.locator(".audience-summary:visible").first()).isFocused();}
                    if(id.equals("nested")) {
                        assertEquals("1 / span 2",p.locator("[data-lesson-id=e1]").evaluate("e=>e.parentElement.style.gridColumn"));
                        assertEquals("3 / span 2",p.locator("[data-lesson-id=e2]").evaluate("e=>e.parentElement.style.gridColumn"));
                        p.locator("#subject-filter").selectOption("handicraft");p.locator(".audience-summary:visible").first().click();assertThat(p.locator("#lesson-inspector")).containsText("hidden by filter");assertEquals(3,p.locator("[data-inspect-audience-lesson]").count());assertThat(p.locator("#lesson-inspector")).not().containsText("No lesson");p.keyboard().press("Escape");
                        p.locator("#clear-timetable-filters").click();
                    }
                    h.record("UC-11: independently reviewed "+id+" exact pair; compact faithful audience positions, complete individual selection, neutral untaught or crossing fallback as applicable",p,"desktop-"+id);
                    for(int width:List.of(700,320)) {
                        p.setViewportSize(width,900);p.waitForTimeout(100);assertTrue((Boolean)p.evaluate("document.documentElement.scrollWidth<=innerWidth"));
                        assertFalse((Boolean)p.locator("#timetable-filter-disclosure").evaluate("e=>e.open"));
                        if(!id.equals("whole")) {
                            Locator summary=p.locator(".audience-summary:visible").first();
                            if(summary.count()>0){summary.tap();assertTrue(p.locator("[data-inspect-audience-lesson]").count()>0);assertTrue((Boolean)p.locator("[data-inspect-audience-lesson]").evaluateAll("a=>a.every(n=>n.getBoundingClientRect().height>=44)"));p.keyboard().press("Escape");}
                        }
                        h.record("UC-11 G4-G7: "+id+" five-day week fits "+width+"px, collapsed filters, touch summary with full text and >=44px lesson rows",p,"narrow-"+id+"-"+width);
                    }
                    p.setViewportSize(1440,1000);
                }
                p.locator("#school-selector").selectOption("gap");p.locator("#school-setup-entry").click();p.locator("[data-setup-quality]").click();
                assertThat(p.locator(".audience-quality")).containsText("14 student-periods");assertThat(p.locator(".audience-quality")).containsText("28 uncovered student-periods");assertThat(p.locator(".audience-quality")).containsText("2 actual taught periods");assertThat(p.locator(".audience-quality")).containsText("2 above reference (nonblocking)");assertThat(p.locator(".audience-quality")).containsText("No reference set");
                Object before=durable(p);p.locator("[data-quality-class]").first().click();assertThat(p.locator("#lesson-inspector")).containsText("No lesson");p.keyboard().press("Escape");
                p.locator("#school-setup-entry").click();p.locator("[data-setup-quality]").click();p.locator("[data-quality-teacher=T0]").click();assertThat(p.locator("#teacher-filter")).hasValue("T0");assertThat(p.locator("#lesson-inspector")).containsText("Teacher 1");p.keyboard().press("Escape");
                assertEquals(((Map<?,?>)before).get("timetables"),((Map<?,?>)durable(p)).get("timetables"));
                h.record("UC-12: exact independently reviewed gaps locate 14 waiting student-periods at P2, ordinary coverage28 separate; teacher actual2/reference0/difference2 nonblocking, absent reference explicit; period/teacher links inspect without pair mutation",p,"monitoring-gap-workload");
                p.setViewportSize(320,900);assertTrue((Boolean)p.evaluate("document.documentElement.scrollWidth<=innerWidth"));h.record("UC-12 narrow readable exact monitoring",p,"monitoring-320");
                String retained=(String)p.evaluate("localStorage.getItem('"+KEY+"')");
                p.evaluate("()=>{const s=JSON.parse(localStorage.getItem('"+KEY+"'));s.timetables.gap.gap.result.quality.teacherWorkload[0].actualSessions=999;localStorage.setItem('"+KEY+"',JSON.stringify(s));}");
                p.setViewportSize(1440,1000);p.reload();p.locator("#school-selector").selectOption("gap");p.locator("#school-setup-entry").click();p.locator("[data-setup-quality]").click();assertThat(p.locator("#setup-content")).containsText("stale or cannot be reconciled");assertThat(p.locator("#setup-content")).not().containsText("999");
                h.record("UC-12 4a: incompatible retained monitoring values are explicitly unavailable; no stale total or invented threshold is shown, assignments remain inspectable",p,"monitoring-stale");
                p.evaluate("raw=>localStorage.setItem('"+KEY+"',raw)",retained);p.setViewportSize(1440,1000);p.reload();p.locator("#school-selector").selectOption("gap");p.locator("#school-setup-entry").click();p.locator("[data-setup-quality]").click();assertThat(p.locator("#setup-content")).containsText("14 student-periods");

                assertTrue(h.errors.isEmpty(),h.errors.toString());Files.writeString(h.output.resolve("verdict.txt"),"PASS\n");
            }catch(Throwable failure){Files.writeString(h.output.resolve("verdict.txt"),"FAIL: "+failure);h.record("FAIL "+failure,p,"failure");throw failure;}
            finally{Files.write(h.output.resolve("browser-errors.txt"),h.errors);Files.writeString(h.output.resolve("ledger.md"),"# UC-11 inspection and UC-12 monitoring administrator journey\n\nDate: "+Instant.now()+"\nApplication: "+h.base+"\nPackaged working diff, isolated touch-enabled real Chrome; each prerequisite pair independently reviewed through production API before disposable browser-local installation.\n\n"+String.join("\n\n",h.steps)+"\n\nOverall: "+Files.readString(h.output.resolve("verdict.txt"))+"\nContext/server close. Normal workspace and further journeys have separate status.\n");}
        }finally{h.stop();}
    }

}
