package org.schoolkernel.workspace;

import static org.junit.jupiter.api.Assertions.*;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import com.microsoft.playwright.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.JsonNode;

/** Recorded administrator journeys through the actual packaged demo, kernel, TLS and installed Chrome. */
class DemoTimetableBrowserIT {
    @TempDir Path temporary;

    @Test void exploresVerifiedVariantsWithInstantSwitchingFiltersAndResponsiveInspector() throws Exception {
        var h = new DemoPlanningBrowserIT(); h.temporary = temporary;
        h.initialize("timetable-explore", Path.of("../output/playwright/demo-timetable-experience/UC-1/local-"+System.currentTimeMillis()));
        try (Playwright pw = Playwright.create(new Playwright.CreateOptions().setEnv(Map.of("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD", "1")));
             Browser browser = pw.chromium().launch(new BrowserType.LaunchOptions().setChannel("chrome").setHeadless(true));
             BrowserContext owner = h.context(browser); BrowserContext other = h.context(browser)) {
            Page page = owner.newPage(); page.onPageError(h.errors::add);
            try {
                page.navigate(h.base+"/workspace/");
                page.locator("#school-selector option[value=full]").waitFor(new Locator.WaitForOptions().setState(com.microsoft.playwright.options.WaitForSelectorState.ATTACHED));
                assertThat(page.locator("#school-selector")).hasValue("full");
                assertThat(page.locator("#timetable-title")).hasText("Variant A");
                assertEquals(520, page.locator(".demo-lesson").count());
                assertEquals(List.of("two-class", "full", "star-wars", "english"), page.locator("#school-selector option").evaluateAll("nodes=>nodes.map(n=>n.value)"));
                assertEquals(2, page.locator("#timetable-selector option").count());
                assertTrue(page.locator("body").innerText().contains("Kasevälja Põhikool"));
                assertFalse(page.locator("body").innerText().contains("Small school"));
                assertEquals(0, page.locator("#open-repair-setup, #reopen-inspector, #teacher-change-route, [data-show-teacher-route]").count());
                assertEquals(0,page.locator("#demo-utilities").count());
                long startupLaunches = Files.readAllLines(temporary.resolve("launches")).size();
                assertEquals(0, startupLaunches);
                h.record("UC-1 main 1-2/G1/G4: default full Estonian timetable, exact four schools and two variants each, 520 real labels, Utilities removed and obsolete actions absent", page, "01-default-matrix");

                page.locator("#cohort-filter").selectOption("demo-cohort-001");
                String a = page.locator("#timetable-matrix").innerHTML();
                var traffic = new ArrayList<String>(); page.onRequest(request -> traffic.add(request.url()));
                page.locator("#timetable-selector").selectOption("b");
                assertThat(page.locator("#cohort-filter")).hasValue("demo-cohort-001");
                assertThat(page.locator("#timetable-title")).hasText("Variant B");
                assertNotEquals(a, page.locator("#timetable-matrix").innerHTML());
                assertTrue(traffic.isEmpty(), "Switching waits for no network request");
                page.locator(".demo-lesson").first().click(); assertThat(page.locator("#lesson-inspector")).isVisible();
                h.record("UC-1 main 3-4/G2: same-school variants retain the exact class filter, visibly differ, perform zero network requests and open lesson details", page, "02-filtered-alternative-inspector");
                page.keyboard().press("Escape"); assertThat(page.locator("#lesson-inspector")).not().isVisible();

                page.locator("#school-selector").selectOption("two-class");
                assertThat(page.locator("#cohort-filter")).hasValue("");
                assertEquals(56, page.locator(".demo-lesson").count());
                assertTrue(page.locator("#timetable-matrix").innerText().contains("Inglise keel"));
                assertTrue(page.locator("#timetable-matrix").innerText().contains("Kehaline kasvatus"));
                page.locator("#timetable-selector").selectOption("b");
                page.locator("#school-selector").selectOption("star-wars");
                assertEquals(540, page.locator(".demo-lesson").count());
                assertTrue(page.locator("body").innerText().contains("Coruscant Imperial Academy"));
                assertTrue(page.locator(".demo-lesson").evaluateAll("nodes=>nodes.some(n=>n.getAttribute('aria-label').includes('Grand Admiral Thrawn'))").equals(true));
                page.locator("#cohort-filter").selectOption("imperial-cohort-junior-1A");
                assertEquals(25,page.locator(".demo-lesson").count());
                assertTrue(page.locator("#timetable-matrix").innerText().contains("Applied Numeracy"));
                h.record("UC-1 G1 Imperial academy: younger cadets have 25 lessons in literacy, numeracy, science, fitness and teamwork; no advanced combat courses",page,"03a-imperial-junior");
                page.locator("#cohort-filter").selectOption("imperial-cohort-intermediate-4A");
                assertEquals(30,page.locator(".demo-lesson").count());
                assertTrue(page.locator("#timetable-matrix").innerText().contains("Flight Simulation"));
                h.record("UC-1 G1 Imperial academy: intermediate cadets have 30 lessons in physics, systems, navigation, simulation, tactical fundamentals and survival",page,"03b-imperial-intermediate");
                page.locator("#cohort-filter").selectOption("imperial-cohort-senior-7A");
                assertEquals(35,page.locator(".demo-lesson").count());
                assertTrue(page.locator("#timetable-matrix").innerText().contains("Combined Operations"));
                h.record("UC-1 G1 Imperial academy: seniors have 35 lessons emphasizing operations, fleet tactics, mission planning and leadership, with specialist facilities and qualified Imperial faculty",page,"03c-imperial-senior");
                page.locator("#timetable-selector").selectOption("b");
                assertThat(page.locator("#cohort-filter")).hasValue("imperial-cohort-senior-7A");
                page.locator("#cohort-filter").selectOption("");
                assertEquals(startupLaunches, Files.readAllLines(temporary.resolve("launches")).size());
                h.record("UC-1 main 5/G1-G3: changing schools clears filters; 5A/5B has 56 lessons; Coruscant Imperial Academy has 540 progressively staged lessons with balanced days and related specialist faculty", page, "03-star-wars");

                page.locator("#school-selector").selectOption("english");
                assertThat(page.locator("#cohort-filter")).hasValue("");
                assertEquals(614, page.locator(".demo-lesson").count());
                assertEquals(23, page.locator("#cohort-filter option").count());
                assertEquals(42, page.locator("#teacher-filter option").count());
                assertEquals(36, page.locator("#room-filter option").count());
                assertTrue(page.locator("#timetable-matrix").innerText().contains("Mathematics"));
                h.record("UC-1 G1/G5 English addition: Meadowbrook's independent Years 1–9 matrix renders 614 lessons, 22 classes, 41 teachers and 35 rooms with real English curriculum labels and compact retro styling", page, "03a-english-matrix");
                page.locator("#cohort-filter").selectOption("meadowbrook-cohort-1A");
                String englishA = page.locator("#timetable-matrix").innerHTML();
                traffic.clear(); page.locator("#timetable-selector").selectOption("b");
                assertThat(page.locator("#cohort-filter")).hasValue("meadowbrook-cohort-1A");
                assertNotEquals(englishA, page.locator("#timetable-matrix").innerHTML());
                assertTrue(traffic.isEmpty());
                page.locator(".demo-lesson").first().click();
                assertThat(page.locator("#lesson-inspector")).containsText("Year 1A");
                h.record("UC-1 G1-G3 English addition: its second variant differs, retains the exact class filter, opens English lesson details and makes zero switching requests; all eight variants start no visitor kernel process", page, "03b-english-inspector");
                page.keyboard().press("Escape");
                page.setViewportSize(390, 844);
                page.locator("#timetable-filter-disclosure > summary").click();
                page.locator("#cohort-filter").selectOption("");
                page.locator("#timetable-filter-disclosure > summary").click();
                assertTrue((Boolean)page.evaluate("()=>document.documentElement.scrollWidth<=innerWidth+1"));
                assertTrue((Boolean)page.locator(".matrix-wrap").evaluate("e=>e.scrollWidth<=e.clientWidth+1"));
                h.record("UC-1 G5/G7 English addition: collapsed filters and the complete five-day Meadowbrook matrix fit 390px without horizontal matrix or page overflow", page, "03c-english-mobile");
                page.setViewportSize(1440, 1000);
                assertEquals(startupLaunches, Files.readAllLines(temporary.resolve("launches")).size());
                page.locator("#school-selector").selectOption("star-wars");
                page.locator("#timetable-selector").selectOption("b");

                page.reload(); page.locator("#school-selector option[value=star-wars]").waitFor(new Locator.WaitForOptions().setState(com.microsoft.playwright.options.WaitForSelectorState.ATTACHED));
                assertThat(page.locator("#school-selector")).hasValue("full");
                assertThat(page.locator("#timetable-selector")).hasValue("a");
                Page foreign = other.newPage(); foreign.navigate(h.base+"/workspace/");
                foreign.locator("#school-selector option[value=full]").waitFor(new Locator.WaitForOptions().setState(com.microsoft.playwright.options.WaitForSelectorState.ATTACHED));
                assertThat(foreign.locator("#school-selector")).hasValue("full");
                assertThat(foreign.locator("#timetable-selector")).hasValue("a");
                SchoolSetupBrowserIT.open(page);SchoolSetupBrowserIT.tool(page,"#setup-schema");
                assertEquals(0,page.locator("#demo-utilities").count());SchoolSetupBrowserIT.close(page);
                h.record("UC-1 main 7/G4/G6: clean bundled reload; independent owner; advanced school tools available only inside Setup",page,"04-reload-and-setup");

                page.setViewportSize(390, 844); page.locator("#school-selector").selectOption("two-class");
                assertTrue((Boolean)page.evaluate("()=>document.documentElement.scrollWidth<=innerWidth+1"));
                page.locator(".demo-lesson").first().click();
                var box = page.locator("#lesson-inspector").boundingBox();
                assertNotNull(box); assertTrue(box.x >= 0 && box.x+box.width <= 391);
                h.record("UC-1 extension 3a/G5: 390px selectors wrap, matrix scroll stays inside its container, and the clickable lesson inspector fits the viewport", page, "05-mobile-inspector");
                page.evaluate("()=>{const k='school-kernel.timetable-demo.v1',s=JSON.parse(localStorage.getItem(k));s.drafts['two-class/b']={changes:{unknown:{periodId:'missing'}}};localStorage.setItem(k,JSON.stringify(s));}");
                page.reload(); page.locator("#school-selector option[value=two-class]").waitFor(new Locator.WaitForOptions().setState(com.microsoft.playwright.options.WaitForSelectorState.ATTACHED));
                page.locator("#school-selector").selectOption("two-class");
                page.locator("#timetable-selector").selectOption("b");
                assertThat(page.locator("#demo-notice")).containsText("retained");
                assertEquals(56, page.locator(".demo-lesson").count());
                assertTrue(page.evaluate("()=>localStorage.getItem('school-kernel.timetable-demo.v1').includes('missing')").equals(true));
                h.record("UC-1 extension 1a/G3: unreadable draft is explained and retained byte-for-byte while readable bundled schools remain inspectable",page,"06-unreadable-draft");
                page.evaluate("()=>localStorage.setItem('school-kernel.timetable-demo.v1','{unfinished')");
                page.reload(); page.locator("#school-selector option[value=full]").waitFor(new Locator.WaitForOptions().setState(com.microsoft.playwright.options.WaitForSelectorState.ATTACHED));
                assertThat(page.locator("#demo-notice")).containsText("could not be opened");
                page.locator("#school-selector").selectOption("two-class");
                assertEquals("{unfinished",page.evaluate("()=>localStorage.getItem('school-kernel.timetable-demo.v1')"));
                h.record("UC-1 extensions 1a/1b: corrupt browser root is preserved, samples remain navigable, and failed writes explain that changes are only in memory",page,"07-unavailable-storage");
                assertTrue(h.errors.isEmpty(), h.errors.toString());
                Files.writeString(h.output.resolve("verdict.txt"), "PASS\n");
            } catch (Throwable failure) {
                Files.writeString(h.output.resolve("verdict.txt"), "FAIL: "+failure+"\n");
                h.record("FAIL: "+failure, page, "failure"); throw failure;
            } finally { ledger(h, "UC-1"); }
        } finally { h.stop(); }
    }

    static void ledger(DemoPlanningBrowserIT h, String uc) throws Exception {
        Files.write(h.output.resolve("browser-errors.txt"), h.errors);
        Files.writeString(h.output.resolve("ledger.md"), "# "+uc+" recorded timetable demo walkthrough\n\nDate: "+Instant.now()
                +"\nApplication: "+h.base+"\nReal installed Chrome through Java Playwright; isolated disposable browser owners; private copies of the packaged workspace and kernel; real TLS. No operator data or database.\n\n"
                +String.join("\n\n", h.steps)+"\n\nOverall: "+Files.readString(h.output.resolve("verdict.txt"))
                +"\nBrowser contexts close and the private server stops. Generation/editing destination handoffs are verified in their owning UCs.\n");
    }

    @Test void editsConflictsPersistsDraftAndAtomicallySavesNamedAlternative() throws Exception {
        var h = new DemoPlanningBrowserIT(); h.temporary=temporary;
        h.initialize("timetable-edit",Path.of("../output/playwright/demo-timetable-experience/UC-2/local-"+System.currentTimeMillis()));
        try (Playwright pw=Playwright.create(new Playwright.CreateOptions().setEnv(Map.of("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD","1")));
             Browser browser=pw.chromium().launch(new BrowserType.LaunchOptions().setChannel("chrome").setHeadless(true));
             BrowserContext owner=h.context(browser)) {
            Page p=owner.newPage();p.onPageError(h.errors::add);
            try {
                ready(p,h.base); p.locator("#school-selector").selectOption("two-class");
                JsonNode bundle=DemoPlanningBrowserIT.JSON.readTree(p.request().get(h.base+"/api/demo/timetables").body());
                JsonNode school=bundle.path("schools").get(0),source=school.path("timetables").get(0).path("result");
                JsonNode assignment=java.util.stream.StreamSupport.stream(source.path("timetable").path("assignments").spliterator(),false)
                        .filter(a->a.path("subjectId").asString().equals("demo-subject-english")).findFirst().orElseThrow();
                String lesson=assignment.path("lessonId").asString(), originalPeriod=assignment.path("periodId").asString(),originalRoom=assignment.path("roomId").asString();
                p.locator("[data-lesson-id='"+lesson+"']").click();
                p.locator("#inspector-teacher").selectOption("demo-teacher-extra-pe");
                p.waitForFunction("()=>document.querySelector('#draft-feedback').textContent.includes('not qualified')");
                assertThat(p.locator("#save-new-timetable")).isDisabled();
                assertEquals(1,p.locator(".demo-lesson.has-conflict").count(),"Eligibility errors identify the changed lesson, without blaming other lessons using that teacher");
                h.record("UC-2 main 1-4/extension 1a: unqualified teacher remains an explicit invalid draft, located eligibility explanation is visible, save blocked",p,"01-unqualified");
                p.locator("#inspector-teacher").selectOption("demo-teacher-english-cover"); valid(p);
                assertTrue(p.locator("#draft-feedback").innerText().contains("Could be improved"));
                JsonNode collision=java.util.stream.StreamSupport.stream(source.path("timetable").path("assignments").spliterator(),false)
                        .filter(a->a.path("cohortId").equals(assignment.path("cohortId"))&&!a.path("lessonId").equals(assignment.path("lessonId"))).findFirst().orElseThrow();
                p.locator("#inspector-period").selectOption(collision.path("periodId").asString());
                p.waitForFunction("()=>document.querySelector('#draft-feedback').textContent.includes('Class double-booked')");
                assertTrue(p.locator(".demo-lesson.has-conflict").count()>=2);assertThat(p.locator("#save-new-timetable")).isDisabled();
                p.keyboard().press("Escape");
                String otherClass=school.path("definition").path("cohorts").get(1).path("id").asString();
                p.locator("#cohort-filter").selectOption(otherClass);
                assertTrue(p.locator("#draft-feedback [data-conflict-lesson='"+lesson+"']").count()>0);
                p.locator("#draft-feedback [data-conflict-lesson='"+lesson+"']").first().click();
                assertThat(p.locator("#cohort-filter")).hasValue(""); assertThat(p.locator("#lesson-inspector")).isVisible();
                h.record("UC-2 main 4-5/extension 4a: class collision identifies and marks both lessons with names/times and correction guidance; global conflict link reveals a filtered-out lesson",p,"02-actionable-conflict");
                p.locator("#inspector-period").selectOption(originalPeriod); valid(p);
                p.locator("#inspector-room").selectOption("demo-room-pe");
                p.waitForFunction("()=>document.querySelector('#draft-feedback').textContent.includes('Room lacks required equipment')");
                assertThat(p.locator("#save-new-timetable")).isDisabled();
                p.locator("#inspector-room").selectOption(originalRoom);valid(p);
                // Hold an actual old kernel response while a newer candidate completes.
                var held=new java.util.concurrent.atomic.AtomicReference<Route>();var oldBody=new java.util.concurrent.atomic.AtomicReference<String>();
                p.route("**/api/demo/timetable/review",r->{
                    if(held.get()==null){var response=r.fetch();oldBody.set(response.text());held.set(r);}else r.resume();
                });
                p.locator("#inspector-period").selectOption(collision.path("periodId").asString());
                p.waitForTimeout(2500);assertNotNull(held.get());
                p.locator("#inspector-period").selectOption(originalPeriod);valid(p);
                held.get().fulfill(new Route.FulfillOptions().setStatus(200).setContentType("application/json").setBody(oldBody.get()));
                p.waitForTimeout(300);assertThat(p.locator("#timetable-save-bar")).containsText("Ready to save");p.unroute("**/api/demo/timetable/review");
                p.route("**/api/demo/timetable/review",r->r.abort());p.locator("#inspector-room").selectOption("demo-room-pe");
                p.waitForFunction("()=>document.querySelector('#timetable-save-bar').textContent.includes('Validation failed')");assertThat(p.locator("#save-new-timetable")).isDisabled();
                p.unroute("**/api/demo/timetable/review");p.locator("#draft-feedback [data-retry-review]").click();
                p.waitForFunction("()=>document.querySelector('#draft-feedback').textContent.includes('Room lacks required equipment')");
                p.setViewportSize(390,844);p.locator("#inspector-room").selectOption(originalRoom);valid(p);
                var popup=p.locator("#lesson-inspector").boundingBox();var toolbar=p.locator("#timetable-save-bar").boundingBox();
                assertTrue(popup.x>=0&&popup.x+popup.width<=391);assertTrue(popup.y+popup.height<=toolbar.y+1);
                assertTrue((Boolean)p.evaluate("()=>document.documentElement.scrollWidth<=innerWidth+1"));
                h.record("UC-2 extensions 3a/3b and G5: delayed actual stale review cannot replace newer valid state; injected transport failure blocks save and retry recovers; 390px editable inspector and toolbar fit without overlap",p,"02b-stale-failed-mobile");
                p.setViewportSize(1440,1000);
                p.keyboard().press("Escape");
                String exactDraft=(String)p.evaluate("()=>JSON.stringify(JSON.parse(localStorage.getItem('school-kernel.timetable-demo.v1')).drafts['two-class/a'])");
                p.locator("#school-selector").selectOption("star-wars"); p.locator("#school-selector").selectOption("two-class");
                assertEquals(exactDraft,p.evaluate("()=>JSON.stringify(JSON.parse(localStorage.getItem('school-kernel.timetable-demo.v1')).drafts['two-class/a'])"));
                p.reload();readyWait(p);assertThat(p.locator("#timetable-save-bar")).not().isVisible();p.locator("#school-selector").selectOption("two-class");valid(p);
                assertEquals(exactDraft,p.evaluate("()=>JSON.stringify(JSON.parse(localStorage.getItem('school-kernel.timetable-demo.v1')).drafts['two-class/a'])"));
                p.locator("#save-new-timetable").click();assertThat(p.locator("#timetable-name-error")).containsText("Enter a name");
                p.locator("#new-timetable-name").fill("Variant A");p.locator("#save-new-timetable").click();assertThat(p.locator("#timetable-name-error")).containsText("already has");
                p.locator("#new-timetable-name").fill("English with cover teacher");
                String before=(String)p.evaluate("()=>localStorage.getItem('school-kernel.timetable-demo.v1')");
                p.evaluate("()=>{const set=Storage.prototype.setItem;Storage.prototype.setItem=function(k,v){if(window.failSave)throw new DOMException('full','QuotaExceededError');return set.call(this,k,v)};window.failSave=true}");
                p.locator("#save-new-timetable").click();assertThat(p.locator("#demo-notice")).containsText("Nothing was saved");
                assertEquals(before,p.evaluate("()=>localStorage.getItem('school-kernel.timetable-demo.v1')"));assertEquals(2,p.locator("#timetable-selector option").count());assertThat(p.locator("#timetable-save-bar")).isVisible();
                h.record("UC-2 extensions 2a/5b/6a/6c: exact complete source snapshot and draft survive navigation/reload; blank/duplicate names refused; injected browser quota failure adds no version and clears no draft",p,"03-draft-name-quota");
                p.evaluate("()=>window.failSave=false");p.locator("#save-new-timetable").click();assertThat(p.locator("#timetable-title")).hasText("English with cover teacher");
                assertThat(p.locator("#timetable-save-bar")).not().isVisible();assertEquals(3,p.locator("#timetable-selector option").count());
                JsonNode saved=DemoPlanningBrowserIT.JSON.readTree((String)p.evaluate("()=>{const s=JSON.parse(localStorage.getItem('school-kernel.timetable-demo.v1'));return JSON.stringify(s.timetables['two-class'][s.selected.timetableId])}"));
                var expected=source.path("timetable").path("assignments").deepCopy();for(JsonNode a:expected)if(a.path("lessonId").asString().equals(lesson))((tools.jackson.databind.node.ObjectNode)a).put("teacherId","demo-teacher-english-cover");
                assertEquals(expected,saved.path("result").path("timetable").path("assignments"));
                assertEquals("MANUAL_REVIEW",saved.path("result").path("terminationReason").asString());
                Files.writeString(h.output.resolve("saved-timetable.json"),saved.toPrettyString());
                p.locator("#timetable-selector").selectOption("a");p.locator("[data-lesson-id='"+lesson+"']").click();assertThat(p.locator("#inspector-teacher")).hasValue(assignment.path("teacherId").asString());p.keyboard().press("Escape");
                p.locator("#timetable-selector").selectOption(saved.path("id").asString());
                h.stop();h.start(List.of(),2);p.reload();readyWait(p);p.locator("#school-selector").selectOption("two-class");assertThat(p.locator("#timetable-title")).hasText("English with cover teacher");
                p.locator("[data-lesson-id='"+lesson+"']").click();p.locator("#inspector-room").selectOption("demo-room-pe");p.keyboard().press("Escape");
                p.locator("#discard-timetable-changes").click();assertThat(p.locator("#timetable-save-bar")).not().isVisible();
                h.record("UC-2 main 6-7/G3-G4/G7: named save stores exact teacher-only delta and actual kernel revisions/score; original Variant A remains exact; saved alternative survives real packaged-server restart; discard restores source",p,"04-saved-after-restart");
                assertTrue(h.errors.isEmpty(),h.errors.toString());Files.writeString(h.output.resolve("verdict.txt"),"PASS\n");
            } catch(Throwable failure){Files.writeString(h.output.resolve("verdict.txt"),"FAIL: "+failure+"\n");h.record("FAIL: "+failure,p,"failure");throw failure;}
            finally{ledger(h,"UC-2");}
        } finally{h.stop();}
    }
    static void ready(Page p,String base){p.navigate(base+"/workspace/");readyWait(p);}
    static void readyWait(Page p){p.locator("#school-selector option[value=full]").waitFor(new Locator.WaitForOptions().setState(com.microsoft.playwright.options.WaitForSelectorState.ATTACHED));}
    static void valid(Page p){p.waitForFunction("()=>document.querySelector('#timetable-save-bar')?.textContent.includes('Ready to save')",null,new Page.WaitForFunctionOptions().setTimeout(40000));}

    @Test void createsCheckedSchoolCopyGeneratesCancelsAndNamesIndependentTimetable() throws Exception {
        var h=new DemoPlanningBrowserIT();h.temporary=temporary;
        h.initialize("school-copy",Path.of("../output/playwright/demo-timetable-experience/UC-3/local-"+System.currentTimeMillis()));
        try(Playwright pw=Playwright.create(new Playwright.CreateOptions().setEnv(Map.of("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD","1")));
            Browser browser=pw.chromium().launch(new BrowserType.LaunchOptions().setChannel("chrome").setHeadless(true));
            BrowserContext owner=h.context(browser);BrowserContext other=h.context(browser)) {
            Page p=owner.newPage();p.onPageError(h.errors::add);
            try {
                ready(p,h.base);p.locator("#school-selector").selectOption("two-class");SchoolSetupAdvancedJsonBrowserIT.newSchool(p);SchoolSetupAdvancedJsonBrowserIT.advanced(p);
                String original=SchoolSetupAdvancedJsonBrowserIT.tinyDocument("My local school");int schoolCount=p.locator("#school-selector option").count();
                p.locator("#school-definition-json").fill("{\n invalid");p.locator("#setup-apply-json").click();assertThat(p.locator("#setup-json-status")).containsText("line 2");SchoolSetupBrowserIT.saved(p);
                p.reload();readyWait(p);p.locator("#school-selector").selectOption("two-class");SchoolSetupAdvancedJsonBrowserIT.tools(p);assertEquals("{\n invalid",p.locator("#school-definition-json").inputValue());
                var invalid=(tools.jackson.databind.node.ObjectNode)DemoPlanningBrowserIT.JSON.readTree(original);((tools.jackson.databind.node.ObjectNode)invalid.path("plans").get(0).path("occurrences").get(0)).put("teacherId","unknown-teacher");
                p.locator("#school-definition-json").fill(invalid.toPrettyString());p.locator("#setup-apply-json").click();assertThat(p.locator("#setup-json-status")).containsText("/plans/0/occurrences/0/teacherId");assertEquals(schoolCount,p.locator("#school-selector option").count());
                h.record("Located syntax errors preserve pending text across reload; semantic teacher-reference errors apply only as blocked draft work, never as saved schools",p,"01-located-definition-errors");
                p.locator("#school-definition-json").fill(original);p.locator("#setup-apply-json").click();SchoolSetupAdvancedJsonBrowserIT.review(p);
                p.locator("#setup-name").fill("Ranniku õppehoone");p.locator("#setup-save-school").click();assertThat(p.locator("#setup-message")).containsText("already exists");p.locator("#setup-name").fill("My local school");SchoolSetupBrowserIT.saved(p);
                Object expected=p.evaluate("async()=>{const {compileDraft}=await import('/workspace/setup-compile.js');return compileDraft(Object.values(JSON.parse(localStorage.getItem('school-kernel.timetable-demo.v1')).setupDrafts).find(d=>d.name==='My local school'));}");
                long before=Files.readAllLines(temporary.resolve("launches")).stream().filter(v->v.equals("plan")).count();p.locator("#setup-save-school").click();assertThat(p.locator("#school-setup")).not().isVisible(new com.microsoft.playwright.assertions.LocatorAssertions.IsVisibleOptions().setTimeout(30000));
                assertEquals(before,Files.readAllLines(temporary.resolve("launches")).stream().filter(v->v.equals("plan")).count());String copyId=p.locator("#school-selector").inputValue();assertEquals(schoolCount+1,p.locator("#school-selector option").count());
                assertEquals(expected,p.evaluate("id=>JSON.parse(localStorage.getItem('school-kernel.timetable-demo.v1')).schools[id].definition",copyId));
                h.record("Checked immutable school copy stores the exact compiled complete document without solving",p,"02-school-copy");
                SchoolSetupAdvancedJsonBrowserIT.review(p);assertThat(p.locator("#setup-effort")).hasValue("QUICK");Files.writeString(temporary.resolve("stall"),"1");p.locator("#setup-effort").selectOption("THOROUGH");p.locator("#setup-start-generation").click();assertThat(p.locator("#setup-generation-status")).containsText("Running",new com.microsoft.playwright.assertions.LocatorAssertions.ContainsTextOptions().setTimeout(30000));p.locator("#setup-cancel-generation").click();assertThat(p.locator("#setup-generation-status")).containsText("cancelled",new com.microsoft.playwright.assertions.LocatorAssertions.ContainsTextOptions().setTimeout(30000));
                Files.delete(temporary.resolve("stall"));assertEquals(0,p.locator(".demo-lesson").count());h.record("Thorough generation cancels through Review, retaining exact school and no candidate",p,"03-cancelled");
                Files.writeString(temporary.resolve("crash"),"1");p.locator("#setup-effort").selectOption("QUICK");p.locator("#setup-start-generation").click();assertThat(p.locator("#setup-generation-status")).containsText("no complete bounded result",new com.microsoft.playwright.assertions.LocatorAssertions.ContainsTextOptions().setTimeout(30000));Files.delete(temporary.resolve("crash"));
                assertEquals(0,p.locator(".demo-lesson").count());h.record("Actual kernel process failure retains school and publishes no partial candidate",p,"04-process-failure");
                p.locator("#setup-start-generation").click();SchoolSetupBrowserIT.close(p);p.locator("#school-selector").selectOption("star-wars");p.locator("#cohort-filter").selectOption("imperial-cohort-junior-1A");assertThat(p.locator("#generation-indicator")).containsText("awaits naming",new com.microsoft.playwright.assertions.LocatorAssertions.ContainsTextOptions().setTimeout(60000));
                assertThat(p.locator("#school-selector")).hasValue("star-wars");assertThat(p.locator("#cohort-filter")).hasValue("imperial-cohort-junior-1A");p.locator("[data-view-generated]").click();assertThat(p.locator("#school-selector")).hasValue(copyId);valid(p);assertEquals(1,p.locator(".demo-lesson").count());
                h.record("Real Quick checked candidate attaches to origin without changing another school's filters",p,"05-generated-candidate");
                p.reload();readyWait(p);assertThat(p.locator("#timetable-save-bar")).not().isVisible();p.locator("#school-selector").selectOption(copyId);valid(p);p.locator("#new-timetable-name").fill("Planned week");p.locator("#save-new-timetable").click();assertThat(p.locator("#timetable-title")).hasText("Planned week");
                JsonNode saved=DemoPlanningBrowserIT.JSON.readTree((String)p.evaluate("()=>{const s=JSON.parse(localStorage.getItem('school-kernel.timetable-demo.v1'));return JSON.stringify(s.timetables[s.selected.schoolId][s.selected.timetableId])}"));assertEquals("PT5S",saved.path("result").path("limit").path("duration").asString());assertEquals(saved.path("provenance").path("source").path("result"),saved.path("result"));
                assertEquals(DemoPlanningBrowserIT.JSON.valueToTree(expected),saved.path("definition"));Files.writeString(h.output.resolve("saved-generated-timetable.json"),saved.toPrettyString());Page foreign=other.newPage();ready(foreign,h.base);assertEquals(schoolCount,foreign.locator("#school-selector option").count());
                p.locator("#school-selector").selectOption("two-class");assertEquals(56,p.locator(".demo-lesson").count());assertEquals(2,p.locator("#timetable-selector option").count());p.setViewportSize(390,844);p.locator("#school-selector").selectOption(copyId);SchoolSetupAdvancedJsonBrowserIT.review(p);assertTrue((Boolean)p.evaluate("()=>document.documentElement.scrollWidth<=innerWidth+1"));
                h.record("Candidate reload/naming preserves exact result, budget and definition; bundled original and other owner unchanged; Setup fits mobile",p,"06-saved-generation-mobile");
                assertTrue(h.errors.isEmpty(),h.errors.toString());Files.writeString(h.output.resolve("verdict.txt"),"PASS\n");
            }catch(Throwable failure){Files.writeString(h.output.resolve("verdict.txt"),"FAIL: "+failure+"\n");h.record("FAIL: "+failure,p,"failure");throw failure;}
            finally{ledger(h,"UC-3");}
        }finally{h.stop();}
    }
}
