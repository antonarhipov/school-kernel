package org.schoolkernel.workspace;

import static org.junit.jupiter.api.Assertions.*;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/** Explicit opt-in public Chrome journey; never changes provider settings or restarts a service. */
@EnabledIfSystemProperty(named = "demo.timetable.remote", matches = "true")
class DemoTimetableRemoteIT {
    @TempDir Path temporary;

    @Test @EnabledIfSystemProperty(named = "demo.timetable.phase", matches = "main")
    void publicTimetableEditingSavingAndGeneration() throws Exception {
        var h = helper();
        try (Playwright pw = playwright(); Browser browser = chrome(pw); BrowserContext owner = h.context(browser);
             BrowserContext other = h.context(browser)) {
            Page p = owner.newPage(); p.onPageError(h.errors::add);
            try {
                DemoTimetableBrowserIT.ready(p,h.base);
                assertThat(p.locator("#school-selector")).hasValue("full");
                assertThat(p.locator("#timetable-title")).hasText("Variant A");
                assertEquals(520,p.locator(".demo-lesson").count());
                assertEquals(List.of("two-class","full","star-wars","english"),p.locator("#school-selector option").evaluateAll("ns=>ns.map(n=>n.value)"));
                assertEquals(0,p.locator("#open-repair-setup,#reopen-inspector,#teacher-change-route,[data-show-teacher-route]").count());
                Files.writeString(h.output.resolve("matrix-layout.json"),DemoPlanningBrowserIT.JSON.writeValueAsString(p.evaluate("""
                    () => { const tile=document.querySelector('.demo-lesson'), room=tile.querySelector('.week-room'), row=tile.closest('tr');
                    return {tileHeight:tile.getBoundingClientRect().height,rowHeight:row.getBoundingClientRect().height,
                      tileDisplay:getComputedStyle(tile).display,roomVisible:!!room&&room.getBoundingClientRect().width>0,
                      timeFont:getComputedStyle(document.querySelector('.week-period')).fontFamily,
                      background:getComputedStyle(document.body).backgroundColor,densitySwitchCount:document.querySelectorAll('#compact-view').length}; }
                    """)));
                h.record("Restored compact retro full-school matrix: original shared subject colours, single-line subject/room tiles, monospace time labels and paper background",p,"00-default-retro-matrix");
                JsonNode bundle=DemoPlanningBrowserIT.JSON.readTree(p.request().get(h.base+"/api/demo/timetables").body());
                for(JsonNode school:bundle.path("schools")) {
                    p.locator("#school-selector").selectOption(school.path("identifier").asString());
                    assertEquals(2,p.locator("#timetable-selector option").count());
                    p.locator("#timetable-selector").selectOption("a");
                    // A valid alternative may retain some classes exactly; exercise a class with a real moved lesson.
                    var alternatives=new HashMap<String,JsonNode>();
                    school.path("timetables").get(1).path("result").path("timetable").path("assignments")
                        .forEach(a->alternatives.put(a.path("lessonId").asString(),a));
                    String changedCohort=null;
                    for(JsonNode a:school.path("timetables").get(0).path("result").path("timetable").path("assignments"))
                        if(!a.path("periodId").equals(alternatives.get(a.path("lessonId").asString()).path("periodId"))){changedCohort=a.path("cohortId").asString();break;}
                    assertNotNull(changedCohort,"Each bundled school must have genuinely different timetable variants");
                    p.locator("#cohort-filter").selectOption(changedCohort);
                    p.waitForLoadState(LoadState.NETWORKIDLE);
                    String a=p.locator("#timetable-matrix").innerHTML();
                    var requests=new ArrayList<String>();
                    java.util.function.Consumer<Request> listener=r->requests.add(r.url());p.onRequest(listener);
                    p.locator("#timetable-selector").selectOption("b");
                    assertThat(p.locator("#cohort-filter")).hasValue(changedCohort);
                    assertNotEquals(a,p.locator("#timetable-matrix").innerHTML());
                    assertTrue(requests.isEmpty(),requests.toString());p.offRequest(listener);
                }
                p.locator("#cohort-filter").selectOption("");
                assertThat(p.locator("#school-selector")).hasValue("english");
                assertEquals(614,p.locator(".demo-lesson").count());
                assertTrue(p.locator("#timetable-matrix").innerText().contains("Design and Technology"));
                h.record("All eight public variants open; Meadowbrook shows its independent 614-lesson English curriculum in the compact matrix; same-school switching retains filters and makes zero requests; obsolete controls absent",p,"01-eight-variants-english");
                JsonNode englishAssignments=bundle.path("schools").get(3).path("timetables").get(1).path("result").path("timetable").path("assignments");
                JsonNode englishLesson=englishAssignments.get(0),englishCollision=null;
                for(JsonNode assignment:englishAssignments)if(assignment.path("cohortId").equals(englishLesson.path("cohortId"))&&!assignment.path("lessonId").equals(englishLesson.path("lessonId"))){englishCollision=assignment;break;}
                assertNotNull(englishCollision);
                p.locator("[data-lesson-id='"+englishLesson.path("lessonId").asString()+"']").click();
                p.locator("#inspector-period").selectOption(englishCollision.path("periodId").asString());feedback(p,"Class double-booked");
                assertThat(p.locator("#save-new-timetable")).isDisabled();
                assertTrue(p.locator(".demo-lesson.has-conflict").count()>=2);
                h.record("The entire new 614-lesson English school is authoritatively checked on the existing Free service; a real class collision identifies affected English lessons and blocks saving",p,"01b-english-conflict");
                p.keyboard().press("Escape");p.locator("#discard-timetable-changes").click();
                assertThat(p.locator("#timetable-save-bar")).not().isVisible();assertEquals(614,p.locator(".demo-lesson").count());
                p.locator("#school-selector").selectOption("star-wars");
                assertTrue(p.locator("body").innerText().contains("Coruscant Imperial Academy"));
                assertEquals(540,p.locator(".demo-lesson").count());
                p.locator("#cohort-filter").selectOption("imperial-cohort-junior-1A");assertEquals(25,p.locator(".demo-lesson").count());
                assertTrue(p.locator("#timetable-matrix").innerText().contains("Applied Numeracy"));
                h.record("Imperial juniors study literacy, numeracy, sciences and teamwork in five-lesson days",p,"01c-imperial-junior");
                p.locator("#cohort-filter").selectOption("imperial-cohort-intermediate-4A");assertEquals(30,p.locator(".demo-lesson").count());
                assertTrue(p.locator("#timetable-matrix").innerText().contains("Flight Simulation"));
                h.record("Imperial intermediate cohorts prepare through six-lesson days of systems, navigation, simulation and fundamentals",p,"01d-imperial-intermediate");
                p.locator("#cohort-filter").selectOption("imperial-cohort-senior-7A");assertEquals(35,p.locator(".demo-lesson").count());
                assertTrue(p.locator("#timetable-matrix").innerText().contains("Combined Operations"));
                h.record("Imperial seniors practise fleet tactics, combined operations, mission planning and leadership in seven-lesson days",p,"01e-imperial-senior");
                p.setViewportSize(390,844);assertTrue((Boolean)p.evaluate("()=>document.documentElement.scrollWidth<=innerWidth+1"));
                h.record("Senior Imperial curriculum retains the compact internally scrollable matrix at 390px",p,"01f-imperial-mobile");
                p.setViewportSize(1440,1000);p.locator("#cohort-filter").selectOption("");
                JsonNode imperialAssignments=bundle.path("schools").get(2).path("timetables").get(1).path("result").path("timetable").path("assignments");
                JsonNode imperialLesson=imperialAssignments.get(0),imperialCollision=null;
                for(JsonNode assignment:imperialAssignments)if(assignment.path("cohortId").equals(imperialLesson.path("cohortId"))&&!assignment.path("lessonId").equals(imperialLesson.path("lessonId"))){imperialCollision=assignment;break;}
                assertNotNull(imperialCollision);
                p.locator("[data-lesson-id='"+imperialLesson.path("lessonId").asString()+"']").click();
                p.locator("#inspector-period").selectOption(imperialCollision.path("periodId").asString());feedback(p,"Class double-booked");
                assertThat(p.locator("#save-new-timetable")).isDisabled();assertTrue(p.locator(".demo-lesson.has-conflict").count()>=2);
                h.record("Full 540-lesson Imperial validation on Free identifies a real collision and prevents saving",p,"01g-imperial-conflict");
                p.keyboard().press("Escape");p.locator("#discard-timetable-changes").click();assertThat(p.locator("#timetable-save-bar")).not().isVisible();
                p.locator("#school-selector").selectOption("two-class");
                assertThat(p.locator("#cohort-filter")).hasValue("");
                assertThat(p.locator("#teacher-filter")).hasValue("");assertThat(p.locator("#room-filter")).hasValue("");
                assertEquals(56,p.locator(".demo-lesson").count());p.locator("#timetable-selector").selectOption("a");
                JsonNode school=bundle.path("schools").get(0),source=school.path("timetables").get(0).path("result");
                JsonNode lesson=null,collision=null;
                for(JsonNode a:source.path("timetable").path("assignments"))if(a.path("subjectId").asString().equals("demo-subject-english")){lesson=a;break;}
                assertNotNull(lesson);
                for(JsonNode a:source.path("timetable").path("assignments"))if(a.path("cohortId").equals(lesson.path("cohortId"))&&!a.path("lessonId").equals(lesson.path("lessonId"))){collision=a;break;}
                assertNotNull(collision);
                String id=lesson.path("lessonId").asString();p.locator("[data-lesson-id='"+id+"']").click();
                p.locator("#inspector-teacher").selectOption("demo-teacher-extra-pe");feedback(p,"not qualified");
                assertThat(p.locator("#save-new-timetable")).isDisabled();assertEquals(1,p.locator(".demo-lesson.has-conflict").count());
                p.locator("#inspector-teacher").selectOption("demo-teacher-english-cover");DemoTimetableBrowserIT.valid(p);
                p.locator("#inspector-period").selectOption(collision.path("periodId").asString());feedback(p,"Class double-booked");
                assertTrue(p.locator(".demo-lesson.has-conflict").count()>=2);assertThat(p.locator("#save-new-timetable")).isDisabled();
                p.keyboard().press("Escape");p.locator("#cohort-filter").selectOption(school.path("definition").path("cohorts").get(1).path("id").asString());
                p.locator("#draft-feedback [data-conflict-lesson='"+id+"']").first().click();assertThat(p.locator("#cohort-filter")).hasValue("");
                h.record("Public kernel validation identifies unqualified teacher and both colliding lessons, blocks save and reveals the filtered-out lesson through its conflict link",p,"02-conflict");
                p.locator("#inspector-period").selectOption(lesson.path("periodId").asString());DemoTimetableBrowserIT.valid(p);
                p.locator("#inspector-room").selectOption("demo-room-pe");feedback(p,"Room lacks required equipment");
                assertThat(p.locator("#save-new-timetable")).isDisabled();
                p.locator("#inspector-room").selectOption(lesson.path("roomId").asString());DemoTimetableBrowserIT.valid(p);
                p.setViewportSize(390,844);
                var popup=p.locator("#lesson-inspector").boundingBox();var toolbar=p.locator("#timetable-save-bar").boundingBox();
                assertTrue(popup.x>=0&&popup.x+popup.width<=391&&popup.y+popup.height<=toolbar.y+1);
                assertTrue((Boolean)p.evaluate("()=>document.documentElement.scrollWidth<=innerWidth+1"));
                h.record("Actual room validation refuses unsuitable equipment; corrected draft is ready; editable inspector and save toolbar fit 390px without overlap",p,"03-mobile-valid");
                p.setViewportSize(1440,1000);p.keyboard().press("Escape");p.reload();DemoTimetableBrowserIT.readyWait(p);DemoTimetableBrowserIT.valid(p);
                p.locator("#save-new-timetable").click();assertThat(p.locator("#timetable-name-error")).containsText("Enter a name");
                p.locator("#new-timetable-name").fill("Variant A");p.locator("#save-new-timetable").click();assertThat(p.locator("#timetable-name-error")).containsText("already has");
                p.locator("#new-timetable-name").fill("Public cover teacher");p.locator("#save-new-timetable").click();
                assertThat(p.locator("#timetable-title")).hasText("Public cover teacher");assertThat(p.locator("#timetable-save-bar")).not().isVisible();
                JsonNode saved=stored(p,"two-class");
                var expected=source.path("timetable").path("assignments").deepCopy();
                for(JsonNode a:expected)if(a.path("lessonId").asString().equals(id))((ObjectNode)a).put("teacherId","demo-teacher-english-cover");
                assertEquals(expected,saved.path("result").path("timetable").path("assignments"));
                assertEquals("MANUAL_REVIEW",saved.path("result").path("terminationReason").asString());
                Files.writeString(h.output.resolve("saved-timetable.json"),saved.toPrettyString());
                p.locator("#timetable-selector").selectOption("a");p.locator("[data-lesson-id='"+id+"']").click();assertThat(p.locator("#inspector-teacher")).hasValue(lesson.path("teacherId").asString());p.keyboard().press("Escape");
                p.locator("#timetable-selector").selectOption(saved.path("id").asString());p.reload();DemoTimetableBrowserIT.readyWait(p);assertThat(p.locator("#timetable-title")).hasText("Public cover teacher");
                Page foreign=other.newPage();DemoTimetableBrowserIT.ready(foreign,h.base);assertEquals(2,foreign.locator("#timetable-selector option").count());assertThat(foreign.locator("#school-selector")).hasValue("full");
                h.record("Blank/duplicate names refused; exact teacher-only result is saved as immutable named version, survives reload, preserves original and is absent from another browser",p,"04-named-save");
                p.locator("#demo-utilities summary").click();assertEquals(1,p.locator("#demo-utilities section").count());
                String definition=p.locator("#school-definition-json").inputValue();p.locator("#school-copy-name").fill("Public disposable Ranniku");p.locator("#save-school-copy").click();
                p.waitForFunction("()=>document.querySelector('#editor-status').textContent.includes('saved in this browser')");
                String copy=p.locator("#school-selector").inputValue();assertEquals(0,p.locator(".demo-lesson").count());
                p.locator("#generation-effort").selectOption("QUICK");p.locator("#generate-timetable").click();
                p.waitForFunction("()=>document.querySelector('#timetable-title').textContent==='Generated timetable'",null,new Page.WaitForFunctionOptions().setTimeout(90000));
                DemoTimetableBrowserIT.valid(p);assertEquals(56,p.locator(".demo-lesson").count());
                p.locator("#new-timetable-name").fill("Public generated week");p.locator("#save-new-timetable").click();assertThat(p.locator("#timetable-title")).hasText("Public generated week");
                JsonNode generated=stored(p,copy);assertEquals(DemoPlanningBrowserIT.JSON.readTree(definition),generated.path("definition"));assertEquals("PT5S",generated.path("result").path("limit").path("duration").asString());
                Files.writeString(h.output.resolve("saved-generated.json"),generated.toPrettyString());
                p.setViewportSize(390,844);assertTrue((Boolean)p.evaluate("()=>document.documentElement.scrollWidth<=innerWidth+1"));
                h.record("Utilities contains only definition editing/generation; exact school copy produces a real Quick result on Free, validated and saved as a named 56-lesson timetable; narrow layout fits",p,"05-public-generation");
                p.locator("#school-selector").selectOption("two-class");p.locator("#timetable-selector").selectOption(saved.path("id").asString());
                owner.storageState(new BrowserContext.StorageStateOptions().setPath(Path.of(System.getProperty("demo.timetable.storage"))));
                assertTrue(h.errors.isEmpty(),h.errors.toString());Files.writeString(h.output.resolve("verdict.txt"),"PASS\n");
            } catch(Throwable failure){Files.writeString(h.output.resolve("verdict.txt"),"FAIL: "+failure+"\n");h.record("FAIL: "+failure,p,"failure");throw failure;}
            finally { ledger(h); }
        }
    }

    @Test @EnabledIfSystemProperty(named = "demo.timetable.phase", matches = "after-restart")
    void savedVersionsSurviveActualProviderRestart() throws Exception {
        var h=helper();
        try(Playwright pw=playwright();Browser browser=chrome(pw);BrowserContext owner=browser.newContext(new Browser.NewContextOptions().setViewportSize(1440,1000).setStorageStatePath(Path.of(System.getProperty("demo.timetable.storage"))))) {
            Page p=owner.newPage();p.onPageError(h.errors::add);
            try {
                DemoTimetableBrowserIT.ready(p,h.base);assertThat(p.locator("#timetable-title")).hasText("Public cover teacher");
                JsonNode saved=DemoPlanningBrowserIT.JSON.readTree(Files.readString(Path.of(System.getProperty("demo.timetable.saved"))));assertEquals(saved,stored(p,"two-class"));
                assertEquals(5,p.locator("#school-selector option").count());assertEquals(3,p.locator("#timetable-selector option").count());
                assertTrue(h.errors.isEmpty(),h.errors.toString());h.record("After the separately recorded same-image provider restart, the exact saved timetable and school copy remain in browser storage and the named matrix opens",p,"01-after-provider-restart");
                Files.writeString(h.output.resolve("verdict.txt"),"PASS\n");
            }catch(Throwable failure){Files.writeString(h.output.resolve("verdict.txt"),"FAIL: "+failure+"\n");h.record("FAIL: "+failure,p,"failure");throw failure;}
            finally{ledger(h);}
        }
    }
    DemoPlanningBrowserIT helper()throws Exception{var h=new DemoPlanningBrowserIT();h.temporary=temporary;h.initialize("remote",Path.of(System.getProperty("demo.timetable.walkthrough.dir")));assertTrue(h.remote);return h;}
    static Playwright playwright(){return Playwright.create(new Playwright.CreateOptions().setEnv(Map.of("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD","1")));}
    static Browser chrome(Playwright pw){return pw.chromium().launch(new BrowserType.LaunchOptions().setChannel("chrome").setHeadless(true));}
    static void feedback(Page p,String text){p.waitForFunction("text=>document.querySelector('#draft-feedback').textContent.includes(text)",text,new Page.WaitForFunctionOptions().setTimeout(40000));}
    static JsonNode stored(Page p,String school){return DemoPlanningBrowserIT.JSON.readTree((String)p.evaluate("id=>{const s=JSON.parse(localStorage.getItem('school-kernel.timetable-demo.v1'));return JSON.stringify(s.timetables[id][s.selected.timetableId])}",school));}
    static void ledger(DemoPlanningBrowserIT h)throws Exception{
        Files.write(h.output.resolve("browser-errors.txt"),h.errors);
        Files.writeString(h.output.resolve("ledger.md"),"# Public timetable demo walkthrough\n\n"+Instant.now()+"; actual public HTTPS "+h.base+"; installed Chrome / Java Playwright. Isolated disposable owners, browser-only copies. Deployment "+System.getProperty("demo.timetable.deployment","identified by accompanying provider evidence")+"; exact uploaded production manifest in the accompanying execution ledger. No provider/configuration mutation in this test.\n\n"+String.join("\n\n",h.steps)+"\n\nOverall: "+Files.readString(h.output.resolve("verdict.txt"))+"\nScreenshots and rendered snapshots accompany every material state. Browsers close. Temporary browser state stays outside source/evidence; separately recorded same-image cleanup clears server owners. Local fault/restart cases retain their original evidence.\n");
    }
}
