package org.schoolkernel.workspace;

import static org.junit.jupiter.api.Assertions.*;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import com.microsoft.playwright.*;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.JsonNode;

/** Real local or explicitly selected public browser, with disposable owners only. */
class DemoLocalDataBrowserIT {
    @TempDir Path temporary;
    static final String KEY = "school-kernel.timetable-demo.v1";

    @Test void deletesOnlyLocalArtifactsWithCancelFailureRecoveryAndCleanReload() throws Exception {
        var h = new DemoPlanningBrowserIT(); h.temporary = temporary;
        h.initialize("local-data", Path.of(System.getProperty("demo.local-data.walkthrough.dir",
                "../output/playwright/demo-timetable-experience/UC-4/local-" + System.currentTimeMillis())));
        try (Playwright pw = Playwright.create(new Playwright.CreateOptions().setEnv(Map.of("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD", "1")));
             Browser browser = pw.chromium().launch(new BrowserType.LaunchOptions().setChannel("chrome").setHeadless(true));
             BrowserContext owner = h.context(browser); BrowserContext other = h.context(browser)) {
            Page p = owner.newPage(); p.onPageError(h.errors::add);
            try {
                DemoTimetableBrowserIT.ready(p, h.base);
                JsonNode bundle = DemoPlanningBrowserIT.JSON.readTree(p.request().get(h.base + "/api/demo/timetables").body());
                p.locator("#school-selector").selectOption("two-class");
                p.locator("#demo-utilities summary").click();
                String definition = p.locator("#school-definition-json").inputValue();
                p.locator("#school-copy-name").fill("Disposable cleanup school"); p.locator("#save-school-copy").click();
                p.waitForFunction("()=>document.querySelector('#editor-status').textContent.includes('saved in this browser')");
                String copy = p.locator("#school-selector").inputValue();
                assertEquals(DemoPlanningBrowserIT.JSON.readTree(definition), stored(p).path("schools").path(copy).path("definition"));
                p.locator("#close-editor").click(); p.locator("#school-selector").selectOption("two-class");
                JsonNode assignment = null;
                for (JsonNode a : bundle.path("schools").get(0).path("timetables").get(0).path("result").path("timetable").path("assignments"))
                    if (a.path("subjectId").asString().equals("demo-subject-english")) { assignment = a; break; }
                assertNotNull(assignment);
                p.locator("[data-lesson-id='" + assignment.path("lessonId").asString() + "']").click();
                p.locator("#inspector-teacher").selectOption("demo-teacher-english-cover"); DemoTimetableBrowserIT.valid(p);
                p.keyboard().press("Escape"); p.locator("#new-timetable-name").fill("Disposable cleanup timetable"); p.locator("#save-new-timetable").click();
                assertThat(p.locator("#timetable-title")).hasText("Disposable cleanup timetable");
                // Retain a second unfinished change and editor work as well as the saved artifacts.
                p.locator("[data-lesson-id='" + assignment.path("lessonId").asString() + "']").click();
                p.locator("#inspector-teacher").selectOption(assignment.path("teacherId").asString()); DemoTimetableBrowserIT.valid(p);
                p.keyboard().press("Escape"); p.locator("#demo-utilities summary").click();
                p.locator("#school-definition-json").fill(definition + "\n"); p.locator("#school-copy-name").fill("Unfinished school edit");
                assertFalse(stored(p).path("drafts").isEmpty()); assertFalse(stored(p).path("timetables").isEmpty());
                p.evaluate("""
                    definition => {
                      localStorage.setItem('school-kernel.definition.v1.cleanup-legacy', JSON.stringify({formatVersion:1, identifier:'cleanup-legacy',
                        savedAt:'2026-10-17T00:00:00.000Z',derivedFrom:{kind:'sample',identifier:'two-class'},documentText:definition}));
                      localStorage.setItem('school-kernel.definition.v1.unreadable', '{retained unreadable');
                      localStorage.setItem('school-kernel.definition-use.v1', '{old artifact reference');
                      localStorage.setItem('school-kernel.view.preference', 'preserve exact view preference');
                      localStorage.setItem('unrelated-browser-data', 'preserve exact unrelated value');
                    }
                    """, definition);
                p.reload(); DemoTimetableBrowserIT.readyWait(p); DemoTimetableBrowserIT.valid(p);
                assertEquals(6, p.locator("#school-selector option").count());
                Page stale = owner.newPage(); stale.onPageError(h.errors::add); DemoTimetableBrowserIT.ready(stale, h.base);
                Page foreign = other.newPage(); foreign.onPageError(h.errors::add); DemoTimetableBrowserIT.ready(foreign, h.base);
                foreign.evaluate("definition=>localStorage.setItem('school-kernel.timetable-demo.v1',JSON.stringify({formatVersion:1,schools:{foreign:{name:'Other browser school',definition:JSON.parse(definition)}},timetables:{},drafts:{},selected:null,lastTimetable:{},editor:null}))", definition);
                String foreignBefore = rawStorage(foreign), before = rawStorage(p);
                p.locator("#demo-utilities summary").click(); p.locator("#delete-local-data").scrollIntoViewIfNeeded();
                assertThat(p.locator("#local-data-description")).containsText("only school definitions, timetables and unfinished edits saved in this browser on this device");
                h.record("UC-4 main 1/G1: Utilities explains the browser/device-only scope and bundled-example preservation; real saved school, timetable, draft, editor work and legacy artifacts exist", p, "01-utilities-scope");
                p.locator("#delete-local-data").click(); assertThat(p.locator("#cancel-delete-local-data")).isFocused();
                p.locator("#cancel-delete-local-data").click(); assertEquals(before, rawStorage(p));
                p.locator("#delete-local-data").click(); p.keyboard().press("Escape"); assertEquals(before, rawStorage(p));
                h.record("UC-4 extension 2a/G2: Cancel and Escape preserve every raw stored value and the exact current draft/editor work", p, "02-cancel-preserves-data");
                p.setViewportSize(390, 844); p.locator("#delete-local-data").click();
                var box = p.locator("#delete-local-data-dialog").boundingBox(); assertTrue(box.x >= 0 && box.x + box.width <= 391);
                assertTrue((Boolean) p.evaluate("()=>document.documentElement.scrollWidth<=innerWidth+1"));
                h.record("UC-4 main 2/G2: keyboard-accessible confirmation repeats local-only scope and fits 390px with Cancel initially focused", p, "03-mobile-confirmation");
                p.evaluate("""
                    () => { const remove=Storage.prototype.removeItem; window.refuseLocalDelete=true;
                      Storage.prototype.removeItem=function(k){if(window.refuseLocalDelete&&k.startsWith('school-kernel.definition.v1.'))throw new DOMException('refused','SecurityError');return remove.call(this,k);}; }
                    """);
                p.locator("#confirm-delete-local-data").click();
                assertThat(p.locator("#delete-local-data-error")).containsText("Existing saved data has been retained"); assertEquals(before, rawStorage(p));
                h.record("UC-4 extension 3a/G4: injected browser removal refusal after the first removal restores all exact raw values, reports failure and permits retry", p, "04-refused-removal");
                p.evaluate("()=>window.refuseLocalDelete=false"); p.setViewportSize(1440, 1000);
                JsonNode serverBefore = h.snapshot(p); var cookiesBefore = cookieValues(owner);
                var mutationRequests = new ArrayList<String>(); p.onRequest(r -> { if (!r.method().equals("GET")) mutationRequests.add(r.method()+" "+r.url()); });
                deleteConfirmed(p); assertClean(p);
                assertEquals(serverBefore, h.snapshot(p)); assertTrue(cookiesBefore.equals(cookieValues(owner)), "Cleanup preserves session cookie values");
                assertTrue(mutationRequests.isEmpty(), mutationRequests.toString());
                assertEquals(foreignBefore, rawStorage(foreign));
                stale.locator("#demo-utilities summary").click(); stale.locator("#school-copy-name").fill("Stale tab must not restore deleted artifacts");
                assertThat(stale.locator("#demo-notice")).containsText("Another tab"); assertNull(p.evaluate("()=>localStorage.getItem('"+KEY+"')"));
                h.record("UC-4 main 3-4/G1/G3: confirmed cleanup removes saved and legacy artifacts, drafts and editor work; clean default reloads; cookies, exact server state, unrelated preferences and another browser remain unchanged; stale tab cannot resurrect data; no server mutation request", p, "05-clean-default");
                for (String school : List.of("two-class", "full", "star-wars", "english")) {
                    p.locator("#school-selector").selectOption(school); assertEquals(2, p.locator("#timetable-selector option").count());
                }
                assertTrue(stored(p).path("schools").isEmpty()); assertTrue(stored(p).path("timetables").isEmpty()); assertTrue(stored(p).path("drafts").isEmpty());
                p.evaluate("()=>{localStorage.setItem('school-kernel.timetable-demo.v1','{corrupt root');localStorage.setItem('school-kernel.definition.v1.broken','{bad legacy');}");
                p.reload(); DemoTimetableBrowserIT.readyWait(p); assertThat(p.locator("#demo-notice")).containsText("retained");
                p.locator("#demo-utilities summary").click(); p.locator("#delete-local-data").click(); deleteConfirmed(p); assertClean(p);
                p.locator("#demo-utilities summary").click(); p.locator("#school-copy-name").fill("New school after cleanup"); p.locator("#save-school-copy").click();
                p.waitForFunction("()=>document.querySelector('#editor-status').textContent.includes('saved in this browser')");
                assertEquals("New school after cleanup", stored(p).path("schools").path(p.locator("#school-selector").inputValue()).path("name").asString());
                h.record("UC-4 extension 3b/main 4: explicit cleanup recovers an unreadable root and malformed legacy definition; fresh school work saves successfully afterward", p, "06-new-work-after-recovery");
                p.locator("#delete-local-data").click(); deleteConfirmed(p); assertClean(p);
                assertTrue(h.errors.isEmpty(), h.errors.toString()); Files.writeString(h.output.resolve("verdict.txt"), "PASS\n");
            } catch (Throwable failure) {
                Files.writeString(h.output.resolve("failure-workspace-response.json"), h.snapshot(p).toPrettyString());
                Files.writeString(h.output.resolve("failure-module-import.txt"), (String)p.evaluate("async()=>{try{await import('/workspace/demo-workbench.js');return 'Module import succeeded'}catch(e){return String(e.stack||e)}}"));
                Files.writeString(h.output.resolve("verdict.txt"), "FAIL: "+failure+"\n"); h.record("FAIL: "+failure, p, "failure"); throw failure;
            } finally {
                Files.write(h.output.resolve("browser-errors.txt"), h.errors);
                Files.writeString(h.output.resolve("ledger.md"), "# UC-4 local-artifact cleanup walkthrough\n\n"+Instant.now()+"; real installed Chrome / Java Playwright; "+h.base+"; isolated disposable browser owners. Deployment "+System.getProperty("demo.local-data.deployment", "private packaged source pinned in execution ledger")+".\n\n"+String.join("\n\n", h.steps)+"\n\nOverall: "+Files.readString(h.output.resolve("verdict.txt"))+"\nBrowser contexts close; local server stops; no operator storage touched. Public server cleanup is recorded separately.\n");
            }
        } finally { h.stop(); }
    }

    static Map<String,String> cookieValues(BrowserContext context) { var result = new TreeMap<String,String>(); context.cookies().forEach(c -> result.put(c.name,c.value)); return result; }
    static JsonNode stored(Page p) { return DemoPlanningBrowserIT.JSON.readTree((String)p.evaluate("()=>localStorage.getItem('"+KEY+"')")); }
    static String rawStorage(Page p) { return (String)p.evaluate("()=>JSON.stringify(Object.keys(localStorage).sort().map(k=>[k,localStorage.getItem(k)]))"); }
    static void deleteConfirmed(Page p) { p.waitForNavigation(() -> p.locator("#confirm-delete-local-data").click()); DemoTimetableBrowserIT.readyWait(p); }
    static void assertClean(Page p) {
        assertThat(p.locator("#school-selector")).hasValue("full"); assertThat(p.locator("#timetable-selector")).hasValue("a");
        assertEquals(4,p.locator("#school-selector option").count()); assertEquals(520,p.locator(".demo-lesson").count());
        assertNull(p.evaluate("()=>localStorage.getItem('"+KEY+"')"));
        assertEquals(0,p.evaluate("()=>Object.keys(localStorage).filter(k=>k.startsWith('school-kernel.definition.v1.')||k==='school-kernel.definition-use.v1').length"));
        assertEquals("preserve exact view preference",p.evaluate("()=>localStorage.getItem('school-kernel.view.preference')"));
        assertEquals("preserve exact unrelated value",p.evaluate("()=>localStorage.getItem('unrelated-browser-data')"));
        assertThat(p.locator("#timetable-save-bar")).not().isVisible();
    }
}
