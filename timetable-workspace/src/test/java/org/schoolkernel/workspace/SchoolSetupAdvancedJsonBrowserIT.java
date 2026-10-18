package org.schoolkernel.workspace;

import static org.junit.jupiter.api.Assertions.*;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import java.nio.file.*;
import java.util.*;
import com.microsoft.playwright.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Real administrator navigation against a pinned demo archive and isolated Chrome. */
class SchoolSetupAdvancedJsonBrowserIT {
    @TempDir Path temporary;
    static final String KEY=SchoolSetupBrowserIT.KEY;
    static String document(Page p) { return p.locator("#school-definition-json").inputValue(); }
    static void advanced(Page p) { if(!p.locator("#school-definition-json").isVisible()) p.locator("#setup-tools > summary").click(); }
    static void ready(Page p) { DemoTimetableBrowserIT.readyWait(p); }
    static void newSchool(Page p) { p.locator("#school-setup-entry").click(); p.locator("#setup-new").click(); }

    static String tinyDocument(String name) {
        return """
                {"documentType":"school-setup","formatVersion":1,"name":"Complete JSON school",
                 "grid":{"activeDays":["MONDAY"],"intervals":[
                 {"id":"interval","periodId":"p","day":"MONDAY","kind":"Lesson","label":"First lesson","start":"09:00","end":"09:45"},
                 {"id":"pause","day":"MONDAY","kind":"Break","label":"Named morning pause","start":"09:45","end":"10:00"}]},
                 "subjects":[{"id":"s","displayName":"Math"}],
                 "teachers":[{"id":"t","displayName":"Teacher","qualifiedSubjectIds":["s"],"eligibleClassIdsBySubject":{"s":["c"]}}],
                 "classes":[{"id":"c","displayName":"Class","size":10,"lunchPeriodIds":[]}],
                 "rooms":[{"id":"r","displayName":"Room","capacity":20,"capabilityIds":[]}],
                 "plans":[{"id":"plan","classId":"c","subjectId":"s","requiredCount":1,"allocations":[{"teacherId":"t","count":1}],"occurrences":[{"id":"l","displayName":"Math lesson","teacherId":"t","seriesId":"series"}],"blockRules":[]}]}
                """.replace("Complete JSON school", name);
    }
    static void createLocalSchool(Page p,String name) {
        newSchool(p);advanced(p);p.locator("#school-definition-json").fill(tinyDocument(name));p.locator("#setup-apply-json").click();
        SchoolSetupBrowserIT.section(p,"Review");p.locator("#setup-tools > summary").click();p.locator("#setup-save-school").click();
        assertThat(p.locator("#school-setup")).not().isVisible(new com.microsoft.playwright.assertions.LocatorAssertions.IsVisibleOptions().setTimeout(30000));
    }
    static void review(Page p) {
        if(!p.locator("#school-setup").isVisible())SchoolSetupBrowserIT.open(p);
        SchoolSetupBrowserIT.section(p,"Review");
        if((Boolean)p.locator("#setup-tools").evaluate("d=>d.open"))p.locator("#setup-tools > summary").click();
    }
    static void tools(Page p) {
        if(!p.locator("#school-setup").isVisible())SchoolSetupBrowserIT.open(p);
        if(!(Boolean)p.locator("#setup-tools").evaluate("d=>d.open"))p.locator("#setup-tools > summary").click();
    }
    @Test void editsOneDocumentWithExplicitApplyAndDurableUnfinishedJson() throws Exception {
        var h=new DemoPlanningBrowserIT();h.temporary=temporary;h.plainHttp=true;
        h.initialize("advanced-json",Path.of("../output/playwright/school-setup-advanced-json/UC-1/"+System.currentTimeMillis()));
        try(Playwright pw=Playwright.create(new Playwright.CreateOptions().setEnv(Map.of("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD","1")));
            Browser browser=pw.chromium().launch(new BrowserType.LaunchOptions().setChannel("chrome").setHeadless(true));
            BrowserContext context=h.context(browser)) {
            Page p=context.newPage();p.onPageError(h.errors::add);
            try {
                p.navigate(h.base+"/workspace/");ready(p);
                assertThat(p.locator("#school-setup")).not().isVisible();
                assertEquals(0,p.locator("#demo-utilities").count());
                String matrix=p.locator("#timetable-matrix").innerHTML();
                newSchool(p);
                assertFalse((Boolean)p.locator("#setup-tools").evaluate("d=>d.open"));
                p.locator("#setup-name").fill("Forms school");SchoolSetupBrowserIT.saved(p);advanced(p);
                assertEquals("Forms school",DemoPlanningBrowserIT.JSON.readTree(document(p)).path("name").asString());
                String original=document(p);
                p.locator("#school-definition-json").fill("{\n\"name\":");
                SchoolSetupBrowserIT.saved(p);
                assertThat(p.locator("#setup-name")).isDisabled();
                p.locator("#setup-apply-json").click();assertThat(p.locator("#setup-json-status")).containsText("line 2");
                assertThat(p.locator("#setup-name")).hasValue("Forms school");
                SchoolSetupBrowserIT.section(p,"Review");p.locator("#setup-save-school").click();
                assertThat(p.locator(".setup-confirm")).containsText("Unapplied JSON changes");
                p.locator(".setup-confirm [data-keep]").click();
                assertThat(p.locator("#school-setup")).isVisible();
                h.record("UC-1 main 1-2, extension 3a/4a: Advanced starts collapsed; form name appears in JSON; malformed Apply is located and preserves setup; pending JSON disables form mutation and refuses school save",p,"01-invalid-retained");
                assertThat(p.locator("#school-setup")).isVisible();
                p.locator("#setup-save-school").click();
                assertThat(p.locator(".setup-confirm")).isVisible();
                for (int width : List.of(700,390,320)) {
                    p.setViewportSize(width,1000);
                    assertThat(p.locator("#school-setup")).not().isVisible();
                    assertThat(p.locator(".setup-confirm")).not().isVisible();
                    assertThat(p.locator("#school-setup-entry")).not().isVisible();
                    p.locator("#school-setup-entry").evaluate("b=>b.click()");
                    assertThat(p.locator("#school-setup")).not().isVisible();
                    assertThat(p.locator("#timetable-matrix")).isVisible();
                }
                h.record("Desktop-only setup: resizing to 700/390/320px dismisses setup and its pending-change dialog, hides its entry and refuses opening; timetable stays usable and pending JSON is retained",p,"01b-mobile-hidden");
                p.setViewportSize(701,1000);
                assertThat(p.locator("#school-setup-entry")).isVisible();
                assertThat(p.locator("#school-setup")).not().isVisible();
                p.locator("#school-setup-entry").click();advanced(p);
                assertEquals("{\n\"name\":",document(p));
                p.setViewportSize(1440,1000);
                SchoolSetupBrowserIT.close(p);assertEquals(matrix,p.locator("#timetable-matrix").innerHTML());
                p.reload();ready(p);assertThat(p.locator("#school-setup")).not().isVisible();
                p.locator("#school-setup-entry").click();advanced(p);
                assertEquals("{\n\"name\":",document(p));
                p.locator("#setup-discard-json").click();p.locator(".setup-confirm [data-cancel]").click();
                assertEquals("{\n\"name\":",document(p));
                p.locator("#setup-discard-json").click();p.locator(".setup-confirm [data-confirm]").click();
                assertThat(p.locator("#setup-name")).isEnabled();assertEquals(original,document(p));
                h.record("UC-1 extension 2a/4a: close and reload preserve exact malformed text, still land on bundled matrix; cancel discard retains text; confirmed discard restores original document",p,"02-reloaded-and-discarded");
                String valid=tinyDocument("Complete JSON school");
                p.locator("#school-definition-json").fill(valid.replace("\"requiredCount\":1","\"requiredCount\":2"));p.locator("#setup-apply-json").click();
                assertThat(p.locator("#setup-json-status")).containsText("/plans/0");
                assertThat(p.locator("#setup-json-status")).containsText("unassigned");
                p.locator("#setup-review-issues button").filter(new Locator.FilterOptions().setHasText("/plans/0/allocations:")).click();
                assertThat(p.locator("#setup-class-plan")).isVisible();
                p.locator("#school-definition-json").fill(valid);p.locator("#setup-apply-json").click();SchoolSetupBrowserIT.saved(p);
                assertThat(p.locator("#setup-name")).hasValue("Complete JSON school");
                SchoolSetupBrowserIT.section(p,"Timetable grid");assertThat(p.locator(".setup-interval")).hasCount(2);
                assertThat(p.locator("[data-interval-field=label]").last()).hasValue("Named morning pause");
                SchoolSetupBrowserIT.section(p,"Review");advanced(p);
                var applied=DemoPlanningBrowserIT.JSON.readTree(document(p));
                for(int width:List.of(768,1024,1440)) {
                    p.setViewportSize(width,1000);
                    assertTrue((Boolean)p.locator("#school-setup").evaluate("d=>d.scrollWidth<=d.clientWidth+1"));
                    h.record("UC-1 G4: complete JSON and controls fit "+width+"px",p,"03-advanced-"+width);
                }
                p.locator("#setup-tools > summary").click();p.locator("#setup-save-school").click();
                assertThat(p.locator("#school-setup")).not().isVisible(new com.microsoft.playwright.assertions.LocatorAssertions.IsVisibleOptions().setTimeout(30000));
                var state=DemoPlanningBrowserIT.JSON.readTree((String)p.evaluate("localStorage.getItem('"+KEY+"')"));
                var school=state.path("schools").properties().iterator().next().getValue();
                assertEquals("Complete JSON school",school.path("name").asString());
                assertEquals(applied.path("grid"),school.path("authoring").path("grid"));
                assertEquals(applied.path("teachers"),school.path("authoring").path("teachers"));
                assertEquals(applied.path("classes"),school.path("authoring").path("classes"));
                assertEquals(applied.path("plans"),school.path("authoring").path("plans"));
                assertFalse(Files.readString(temporary.resolve("launches")).contains("generate"));
                assertThat(p.locator("#timetable-matrix")).containsText("School setup → Review");
                h.record("UC-1 main 3-5/G1-G3: incomplete demand applies with path; corrected document populates forms and saves one checked copy with exact grid, eligibility, explicit empty lunch and occurrence data; saving starts no generation",p,"04-saved-complete-school");
                assertTrue(h.errors.isEmpty(),h.errors.toString());Files.writeString(h.output.resolve("verdict.txt"),"PASS\n");
            } catch(Throwable failure){h.record("FAIL: "+failure,p,"failure");Files.writeString(h.output.resolve("verdict.txt"),"FAIL: "+failure);throw failure;}
            finally{Files.writeString(h.output.resolve("ledger.md"),"# UC-1 Advanced JSON walkthrough\n\nDate: 2026-10-18. Application: "+h.base+". Pinned packaged demo, installed Chrome, disposable context. Source manifest/checkpoint identifies submission.\n\n"+String.join("\n\n",h.steps)+"\n\nOverall: "+Files.readString(h.output.resolve("verdict.txt"))+"\nContext/browser and local server closed. No operator data used.\n");}
        } finally{h.stop();}
    }
    @Test void exchangesCompleteDocumentsAndPreviewsLegacyConversion() throws Exception {
        var h=new DemoPlanningBrowserIT();h.temporary=temporary;h.plainHttp=true;
        h.initialize("advanced-exchange",Path.of("../output/playwright/school-setup-advanced-json/UC-2/"+System.currentTimeMillis()));
        try(Playwright pw=Playwright.create(new Playwright.CreateOptions().setEnv(Map.of("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD","1")));
            Browser browser=pw.chromium().launch(new BrowserType.LaunchOptions().setChannel("chrome").setHeadless(true));
            BrowserContext context=h.context(browser)) {
            Page p=context.newPage();p.onPageError(h.errors::add);
            try {
                p.navigate(h.base+"/workspace/");ready(p);newSchool(p);p.locator("#setup-name").fill("Incomplete export school");advanced(p);
                Download export=p.waitForDownload(()->p.locator("#setup-export").click());
                Path file=h.output.resolve("complete-school.json");export.saveAs(file);
                var expected=DemoPlanningBrowserIT.JSON.readTree(file);
                assertEquals("school-setup",expected.path("documentType").asString());
                assertEquals("Incomplete export school",expected.path("name").asString());
                assertEquals(0,expected.path("grid").path("intervals").size());
                SchoolSetupBrowserIT.saved(p);Object before=SchoolSetupBrowserIT.durable(p);
                p.locator("#setup-import-file").setInputFiles(file);
                assertThat(p.locator(".setup-confirm")).containsText("complete school document",new com.microsoft.playwright.assertions.LocatorAssertions.ContainsTextOptions().setTimeout(30000));
                p.locator(".setup-confirm [data-cancel]").click();assertEquals(before,SchoolSetupBrowserIT.durable(p));
                p.locator("#setup-import-file").setInputFiles(file);p.locator(".setup-confirm [data-confirm]").click();advanced(p);
                assertEquals(expected,DemoPlanningBrowserIT.JSON.readTree(document(p)));
                assertEquals(2,((Number)p.evaluate("Object.keys(JSON.parse(localStorage.getItem('"+KEY+"')).setupDrafts).length")).intValue());
                Download schema=p.waitForDownload(()->p.locator("#setup-schema").click());schema.saveAs(h.output.resolve("school-schema.json"));
                assertEquals("object",DemoPlanningBrowserIT.JSON.readTree(h.output.resolve("school-schema.json")).path("type").asString());
                h.record("UC-2 main 1-4/G1: incomplete complete-document export/import preserves exact school values in a separate draft; cancelled adoption preserves browser data; downloadable JSON Schema describes school data",p,"01-complete-roundtrip");
                Path malformed=h.output.resolve("unsupported.json");Files.writeString(malformed,Files.readString(file).replace("\"name\":", "\"unsupportedConstraint\":true,\"name\":"));
                p.locator("#setup-import-file").setInputFiles(malformed);assertThat(p.locator("#setup-message")).containsText("/unsupportedConstraint",new com.microsoft.playwright.assertions.LocatorAssertions.ContainsTextOptions().setTimeout(30000));
                assertEquals(expected,DemoPlanningBrowserIT.JSON.readTree(document(p)));
                Path legacy=Path.of("src/main/resources/demo/timetables/two-class-definition.json").toAbsolutePath();
                p.locator("#setup-import-file").setInputFiles(legacy);
                assertThat(p.locator(".setup-confirm")).containsText("Reconstruct only observed teacher/class eligibility",new com.microsoft.playwright.assertions.LocatorAssertions.ContainsTextOptions().setTimeout(30000));
                p.locator(".setup-confirm [data-confirm]").click();advanced(p);
                var converted=DemoPlanningBrowserIT.JSON.readTree(document(p));
                assertEquals("school-setup",converted.path("documentType").asString());
                assertEquals(2,converted.path("classes").size());
                SchoolSetupBrowserIT.saved(p);
                Object convertedState=SchoolSetupBrowserIT.durable(p);
                String source=Files.readString(legacy);Path duplicate=h.output.resolve("duplicate.json");Files.writeString(duplicate,source.replaceFirst("\\{","{\"catalogVersion\":10,"));
                p.locator("#setup-import-file").setInputFiles(duplicate);assertThat(p.locator("#setup-message")).containsText("Duplicate field catalogVersion",new com.microsoft.playwright.assertions.LocatorAssertions.ContainsTextOptions().setTimeout(30000));
                assertEquals(convertedState,SchoolSetupBrowserIT.durable(p));
                h.record("UC-2 extension 2a/G2: unknown complete-document constraint refused at its exact path; real legacy kernel input previews missing intent, then preserves supported settings; duplicate-key import refuses without mutation",p,"02-legacy-and-refusals");
                p.locator("#school-definition-json").fill("{unfinished");SchoolSetupBrowserIT.saved(p);
                Download recovery=p.waitForDownload(()->p.locator("#setup-download").click());Path recovered=h.output.resolve("recovery.json");recovery.saveAs(recovered);
                assertEquals("{unfinished",DemoPlanningBrowserIT.JSON.readTree(recovered).path("draft").path("advanced").path("text").asString());
                p.locator("#setup-export").click();assertThat(p.locator("#setup-message")).containsText("Apply or discard");
                p.locator("#setup-discard-json").click();p.locator(".setup-confirm [data-confirm]").click();
                p.locator("#setup-import-file").setInputFiles(recovered);p.locator(".setup-confirm [data-confirm]").click();advanced(p);
                assertEquals("{unfinished",document(p));
                assertThat(p.locator("#setup-name")).isDisabled();
                h.record("UC-2 extension 1a/G3: export refuses pending raw text; recovery download/import preserves exact unfinished JSON and its form lock in a separate recovered draft",p,"03-full-recovery");
                p.evaluate("()=>{const s=JSON.parse(localStorage.getItem('"+KEY+"'));s.editor={text:'{previous unfinished',name:'Previous copy name',source:'two-class'};localStorage.setItem('"+KEY+"',JSON.stringify(s));}");
                p.reload();ready(p);tools(p);
                assertEquals("{unfinished",document(p));
                p.locator("#setup-previous-json").locator("..").locator("..").locator("summary").click();
                assertThat(p.locator("#setup-previous-json")).hasValue("{previous unfinished");
                Download previous=p.waitForDownload(()->p.locator("#setup-download-legacy").click());previous.saveAs(h.output.resolve("previous-editor.json"));
                assertEquals("{previous unfinished",Files.readString(h.output.resolve("previous-editor.json")));
                h.record("UC-2 G3: pre-existing standalone unfinished text and copy name remain accessible; exact raw text can be downloaded without parsing or overwriting current work",p,"04-previous-editor-recovery");
                assertTrue(h.errors.isEmpty(),h.errors.toString());Files.writeString(h.output.resolve("verdict.txt"),"PASS\n");
            } catch(Throwable failure){h.record("FAIL: "+failure,p,"failure");Files.writeString(h.output.resolve("verdict.txt"),"FAIL: "+failure);throw failure;}
            finally{Files.writeString(h.output.resolve("ledger.md"),"# UC-2 document exchange walkthrough\n\nDate: 2026-10-18. Application: "+h.base+". Pinned demo archive, installed Chrome, isolated context.\n\n"+String.join("\n\n",h.steps)+"\n\nOverall: "+Files.readString(h.output.resolve("verdict.txt"))+"\nBrowser/context/server closed; downloads retained as evidence; no operator data.\n");}
        } finally{h.stop();}
    }

    @Test void managesGenerationAndConfirmedCleanupInsideSetup() throws Exception {
        var h=new DemoPlanningBrowserIT();h.temporary=temporary;h.plainHttp=true;
        h.initialize("advanced-management",Path.of("../output/playwright/school-setup-advanced-json/UC-3/"+System.currentTimeMillis()));
        try(Playwright pw=Playwright.create(new Playwright.CreateOptions().setEnv(Map.of("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD","1")));
            Browser browser=pw.chromium().launch(new BrowserType.LaunchOptions().setChannel("chrome").setHeadless(true));
            BrowserContext context=h.context(browser)) {
            Page p=context.newPage();p.onPageError(h.errors::add);
            try {
                p.navigate(h.base+"/workspace/");ready(p);createLocalSchool(p,"Generation school");
                String origin=p.locator("#school-selector").inputValue();
                String definition=(String)p.evaluate("id=>JSON.stringify(JSON.parse(localStorage.getItem('"+KEY+"')).schools[id].definition)",origin);
                review(p);
                var held=new java.util.concurrent.atomic.AtomicReference<Route>();
                p.route("**/api/demo/definition/check",held::set);
                long launchesBefore=Files.readAllLines(temporary.resolve("launches")).size();
                p.locator("#setup-start-generation").click();p.waitForCondition(()->held.get()!=null);
                p.locator("#setup-cancel-generation").click();assertThat(p.locator("#setup-generation-status")).containsText("Cancelling preparation");
                held.get().resume();p.unroute("**/api/demo/definition/check");
                assertThat(p.locator("#setup-generation-status")).containsText("cancelled",new com.microsoft.playwright.assertions.LocatorAssertions.ContainsTextOptions().setTimeout(30000));
                assertEquals(launchesBefore,Files.readAllLines(temporary.resolve("launches")).size());
                h.record("UC-3 extension 3a: cancel during a held real preparation request is remembered; releasing the request finishes cancellation without launching a kernel process",p,"00-preparation-cancel");
                Files.writeString(temporary.resolve("stall"),"1");
                p.locator("#setup-effort").selectOption("THOROUGH");p.locator("#setup-start-generation").click();
                assertThat(p.locator("#setup-generation-status")).containsText("Running",new com.microsoft.playwright.assertions.LocatorAssertions.ContainsTextOptions().setTimeout(30000));
                SchoolSetupBrowserIT.close(p);p.locator("#school-selector").selectOption("english");
                assertThat(p.locator("#generation-indicator")).containsText("Generation school");p.locator("[data-show-generation]").click();
                assertThat(p.locator("#school-selector")).hasValue(origin);assertThat(p.locator("#setup-section-title")).hasText("Review");
                assertThat(p.locator("#setup-cancel-generation")).isVisible();p.locator("#setup-cancel-generation").click();
                assertThat(p.locator("#setup-generation-status")).containsText("cancelled",new com.microsoft.playwright.assertions.LocatorAssertions.ContainsTextOptions().setTimeout(30000));Files.delete(temporary.resolve("stall"));
                assertEquals(definition,p.evaluate("id=>JSON.stringify(JSON.parse(localStorage.getItem('"+KEY+"')).schools[id].definition)",origin));
                h.record("UC-3 main 1-3/extension 3a/G1-G2: no Utilities; Review starts an explicit saved-school run; progress indicator returns from another selected school to the originating Review; real stalled-process cancellation retains exact saved definition",p,"01-progress-origin-cancel");
                p.locator("#setup-effort").selectOption("QUICK");p.locator("#setup-start-generation").click();
                assertThat(p.locator("#setup-generation-status")).containsText("name and save",new com.microsoft.playwright.assertions.LocatorAssertions.ContainsTextOptions().setTimeout(60000));
                assertThat(p.locator("#school-setup")).isVisible();SchoolSetupBrowserIT.close(p);DemoTimetableBrowserIT.valid(p);
                p.locator("#new-timetable-name").fill("Generated local week");p.locator("#save-new-timetable").click();
                assertThat(p.locator("#timetable-title")).hasText("Generated local week");assertEquals(1,p.locator(".demo-lesson").count());
                h.record("UC-3 main 2-4/G1: real Quick solver and independent verification produce a complete candidate without replacing setup work; explicit close and naming save the timetable",p,"02-generated-and-named");
                p.reload();ready(p);assertThat(p.locator("#school-selector")).hasValue("full");assertThat(p.locator("#school-setup")).not().isVisible();
                p.locator("#school-selector").selectOption(origin);assertThat(p.locator("#timetable-title")).hasText("Generated local week");
                tools(p);p.locator("#school-definition-json").fill("{ unfinished");SchoolSetupBrowserIT.saved(p);
                Page stale=context.newPage();stale.onPageError(h.errors::add);stale.navigate(h.base+"/workspace/");ready(stale);stale.locator("#school-selector").selectOption(origin);SchoolSetupBrowserIT.open(stale);
                p.locator("#delete-local-data").click();Object before=p.evaluate("localStorage.getItem('"+KEY+"')");
                p.locator("#cancel-delete-local-data").click();assertEquals(before,p.evaluate("localStorage.getItem('"+KEY+"')"));
                p.evaluate("()=>{window.realRemove=Storage.prototype.removeItem;Storage.prototype.removeItem=function(k){if(k==='"+KEY+"')throw new DOMException('refused','SecurityError');return window.realRemove.call(this,k)}}");
                p.locator("#delete-local-data").click();p.locator("#confirm-delete-local-data").click();
                assertThat(p.locator("#delete-local-data-error")).containsText("retained");assertEquals(before,p.evaluate("localStorage.getItem('"+KEY+"')"));
                p.evaluate("()=>Storage.prototype.removeItem=window.realRemove");
                h.record("UC-3 main 5/extension 5a/6a: Advanced cleanup is accessible with pending JSON, cancellation preserves exact bytes and removal failure rolls data back, including pending JSON and saved school/timetable",p,"03-cleanup-refusal");
                List<String> writes=new ArrayList<>();p.onRequest(r->{if(!r.method().equals("GET"))writes.add(r.method()+" "+r.url());});
                p.locator("#confirm-delete-local-data").click();ready(p);assertThat(p.locator("#school-selector option")).hasCount(4);
                assertNull(p.evaluate("localStorage.getItem('"+KEY+"')"));assertThat(p.locator("#school-selector")).hasValue("full");
                tools(stale);stale.locator("#setup-retry").click();assertThat(stale.locator("#setup-durability")).hasText("Storage conflict");
                assertNull(p.evaluate("localStorage.getItem('"+KEY+"')"));assertTrue(writes.isEmpty(),writes.toString());
                stale.close();newSchool(p);p.locator("#setup-name").fill("Fresh after cleanup");SchoolSetupBrowserIT.saved(p);
                h.record("UC-3 main 6/G3-G4: confirmed cleanup returns to four bundled schools, deletes pending work, refuses stale-tab resurrection, makes no server mutation and allows fresh setup",p,"04-cleanup-complete");
                assertTrue(h.errors.isEmpty(),h.errors.toString());Files.writeString(h.output.resolve("verdict.txt"),"PASS\n");
            } catch(Throwable failure){h.record("FAIL: "+failure,p,"failure");Files.writeString(h.output.resolve("verdict.txt"),"FAIL: "+failure);throw failure;}
            finally{Files.writeString(h.output.resolve("ledger.md"),"# UC-3 Setup management walkthrough\n\nDate: 2026-10-18. Application: "+h.base+". Pinned archive, installed Chrome, disposable local data. Cancellation uses an explicitly injected stalled kernel; Quick generation uses the real solver.\n\n"+String.join("\n\n",h.steps)+"\n\nOverall: "+Files.readString(h.output.resolve("verdict.txt"))+"\nBrowser/context/server closed; no operator data.\n");}
        } finally{h.stop();}
    }

}
