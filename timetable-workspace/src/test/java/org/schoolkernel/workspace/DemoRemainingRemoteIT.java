package org.schoolkernel.workspace;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.zip.*;
import com.microsoft.playwright.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.JsonNode;

/** Explicit opt-in public acceptance of the pinned remaining implementation; no provider/configuration mutation. */
@EnabledIfSystemProperty(named = "demo.remaining.remote", matches = "true")
class DemoRemainingRemoteIT {
    @TempDir Path temporary;
    @Test @EnabledIfSystemProperty(named = "demo.remaining.narrow", matches = "true")
    void inspectNarrowPublishedImport() throws Exception {
        var h = new DemoPlanningBrowserIT(); h.temporary = temporary;
        h.initialize("narrow", Path.of(System.getProperty("demo.remaining.walkthrough.dir")));
        try (Playwright pw = Playwright.create(new Playwright.CreateOptions().setEnv(Map.of("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD", "1")));
                Browser browser = pw.chromium().launch(new BrowserType.LaunchOptions().setChannel("chrome").setHeadless(true)); BrowserContext owner = h.context(browser)) {
            Page page = owner.newPage(); h.open(page);
            choose(page, Path.of(System.getProperty("demo.remaining.archive"))); page.locator("#demo-bundle-submit").click(); imported(page);
            page.setViewportSize(390, 844); page.locator(".narrow-banner").waitFor();
            h.record("Narrow inspection of actual public imported Current", page, "narrow");
            Object overflow = page.evaluate("() => ({width:innerWidth,root:document.documentElement.scrollWidth,items:[...document.querySelectorAll('body *')].filter(e=>e.getBoundingClientRect().right>innerWidth+1).map(e=>({tag:e.tagName,id:e.id,classes:e.className,left:e.getBoundingClientRect().left,right:e.getBoundingClientRect().right,width:e.getBoundingClientRect().width,scroll:e.scrollWidth,text:e.textContent.slice(0,90)}))})");
            Files.writeString(h.output.resolve("overflow.json"), DemoPlanningBrowserIT.JSON.writeValueAsString(overflow));
            String patch = System.getProperty("demo.remaining.cssPatch");
            if (patch != null) {
                String preview = page.request().get(h.base + "/workspace/styles.css").text() + "\n" + Files.readString(Path.of(patch));
                owner.route("**/workspace/styles.css", route -> route.fulfill(new Route.FulfillOptions().setStatus(200).setContentType("text/css").setBody(preview)));
                page.reload(); page.locator(".narrow-banner").waitFor();
                assertEquals(true, page.evaluate("document.documentElement.scrollWidth<=innerWidth"));
                h.record("Local CSS preview only: imported long school heading wraps and root fits 390px; this CSS is not deployed", page, "narrow-fixed-preview");
                Files.writeString(h.output.resolve("preview-verdict.txt"), "PASS: exact CSS-only local preview; public stylesheet remains the authorized original\n");
            }
            page.setViewportSize(1440, 1000); Page stale = owner.newPage(); h.open(stale);
            choose(stale, Path.of(System.getProperty("demo.remaining.archive"))); h.sample(page, "small", true); JsonNode current = h.snapshot(page);
            stale.onceDialog(Dialog::accept); stale.locator("#demo-bundle-submit").click();
            stale.waitForFunction("()=>document.querySelector('#demo-bundle-status').textContent.includes('workspace changed')"); assertEquals(current, h.snapshot(page));
            for (int i = 0; i < 6; i++) { h.snapshot(page); assertEquals(404, page.request().get(h.base + "/api/runs/" + UUID.randomUUID()).status()); }
            h.record("Public stale import refused with exact newer Current retained; six valid/foreign polls stay usable", page, "stale-refused");
            Files.writeString(h.output.resolve("ledger.md"), "# Public mobile finding and local fix preview\n\n" + Instant.now() + "; real public TLS/Chrome and actual accepted archive, no provider/configuration mutation.\n\n" + String.join("\n\n", h.steps) + "\n\nCurrent public root is 393px at 390px viewport due to a long unbroken school name. The optional preview injects the exact prepared one-line CSS in this disposable browser only; it does not change the deployed source or approve the public mobile layout. Stale refusal/poll outcomes use actual unchanged production endpoints. Browser closes; same-image cleanup follows.\n");
        }
    }
    @Test void acceptanceRepairManualAndExactPrivateBundleRoundTrip() throws Exception {
        var h = new DemoPlanningBrowserIT(); h.temporary = temporary;
        h.initialize("remaining", Path.of(System.getProperty("demo.remaining.walkthrough.dir")));
        assertTrue(h.remote); assertEquals("https://demo-smoke-production.up.railway.app", h.base);
        try (Playwright pw = Playwright.create(new Playwright.CreateOptions().setEnv(Map.of("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD", "1")));
                Browser browser = pw.chromium().launch(new BrowserType.LaunchOptions().setChannel("chrome").setHeadless(true));
                BrowserContext owner = h.context(browser); BrowserContext other = h.context(browser)) {
            Page one = owner.newPage(), two = other.newPage(); one.onPageError(h.errors::add); two.onPageError(h.errors::add);
            try {
                h.open(one); h.open(two);
                assertEquals("EMPTY", h.snapshot(one).path("state").asString());
                assertEquals(409, one.request().get(h.base + "/api/accepted/export").status());
                h.initial(one, "public-accept-one", "PUBLIC_PRIVATE_SENTINEL_20261016");
                h.initial(two, "public-accept-two", "PUBLIC_PRIVATE_SENTINEL_20261016");
                one.locator("#start-plan").click(); h.proposal(one, "INITIAL_PROPOSAL");
                JsonNode proposed = h.snapshot(one); String staleVersion = DemoAcceptanceBrowserIT.etag(h, one);
                assertTrue(one.locator("#accept-proposal").isDisabled());
                assertEquals(428, DemoAcceptanceBrowserIT.command(one, "/api/proposal/accept", "POST", Map.of("confirmed", false), staleVersion));
                assertEquals(proposed, h.snapshot(one));
                one.locator("#confirm-accept").check(); one.locator("#accept-proposal").click(); one.locator("#open-repair-setup").waitFor();
                JsonNode accepted = h.snapshot(one).path("workspace").path("acceptedBaseline");
                assertEquals(proposed.path("workspace").path("initialDefinition"), accepted.path("definition"));
                assertEquals(proposed.path("workspace").path("proposal").path("result"), accepted.path("result"));
                assertEquals(412, DemoAcceptanceBrowserIT.command(one, "/api/proposal/accept", "POST", Map.of("confirmed", true), staleVersion));
                assertEquals(accepted, h.snapshot(one).path("workspace").path("acceptedBaseline"));
                two.locator("#start-plan").click(); h.proposal(two, "INITIAL_PROPOSAL");
                assertTrue(h.snapshot(two).path("workspace").path("proposal").path("reused").asBoolean());
                two.locator("#confirm-accept").check(); two.locator("#accept-proposal").click(); two.locator("#open-repair-setup").waitFor();
                JsonNode foreign = h.snapshot(two);
                assertEquals(accepted, foreign.path("workspace").path("acceptedBaseline"));
                h.record("UC-8 initial: real Quick/custom definition, explicit acknowledgement, exact atomic acceptance, stale refusal and independent reused acceptance", one, "01-initial-accepted");

                h.repair(one); JsonNode initial = h.snapshot(one).path("workspace").path("repairDraft");
                one.locator("[data-draft-effect]").first().click(); one.locator("#apply-pin").click();
                one.waitForFunction("()=>document.querySelector('#draft-conflict-count').textContent!=='0'"); assertTrue(one.locator("#solve-draft").isDisabled());
                assertEquals(accepted, h.snapshot(one).path("workspace").path("acceptedBaseline"));
                one.locator("#remove-pin").click(); one.waitForFunction("()=>document.querySelector('#draft-conflict-count').textContent==='0'");
                one.locator(".repair-controls summary").click(); one.locator("#preview-bulk").click(); one.locator("#cancel-bulk").waitFor();
                JsonNode before = h.snapshot(one); one.locator("#cancel-bulk").click(); assertEquals(before, h.snapshot(one));
                one.locator(".repair-controls summary").click(); one.locator("#preview-bulk").click(); one.locator("#confirm-bulk").click(); one.locator("[data-undo-bulk]").waitFor();
                one.locator("[data-undo-bulk]").click(); one.waitForFunction("()=>document.querySelector('[data-undo-bulk]')===null");
                assertEquals(initial, h.snapshot(one).path("workspace").path("repairDraft"));
                one.reload(); one.locator("#solve-draft").waitFor(); assertEquals(initial, h.snapshot(one).path("workspace").path("repairDraft"));
                one.locator("[data-undo-availability]").click(); one.waitForFunction("()=>document.querySelector('[data-undo-availability]')===null");
                assertTrue(h.snapshot(one).path("workspace").path("repairDraft").path("intent").path("changes").isEmpty());
                one.locator("#confirm-discard-draft").check(); one.locator("#discard-draft").click(); one.locator("#open-repair-setup").waitFor();
                assertEquals(accepted, h.snapshot(one).path("workspace").path("acceptedBaseline")); assertEquals(foreign, h.snapshot(two));
                h.record("UC-7: conflict blocking, individual/bulk pin cancel/undo, exact reload, availability undo and confirmed discard preserve complete Current and foreign owner", one, "02-repair-undo");

                h.repair(one); one.locator(".repair-controls summary").click(); one.locator("#preview-bulk").click(); one.locator("#confirm-bulk").click(); one.locator("[data-undo-bulk]").waitFor();
                before = h.snapshot(one); DemoExportBrowserIT.exact(accepted, DemoExportBrowserIT.download(h, one, h.output.resolve("draft-excluded.zip"))); assertEquals(before, h.snapshot(one));
                one.locator("#solve-draft").click(); h.proposal(one, "REPAIR_PROPOSAL");
                JsonNode repair = h.snapshot(one); assertEquals(accepted, repair.path("workspace").path("acceptedBaseline"));
                assertEquals(6, repair.path("workspace").path("proposal").path("changeCounts").size());
                DemoExportBrowserIT.exact(accepted, DemoExportBrowserIT.download(h, one, h.output.resolve("proposal-excluded.zip"))); assertEquals(repair, h.snapshot(one));
                assertTrue(one.locator("#accept-repair").isDisabled()); one.locator("#confirm-repair-accept").check(); one.locator("#accept-repair").click(); one.locator("#open-repair-setup").waitFor();
                JsonNode repaired = h.snapshot(one).path("workspace").path("acceptedBaseline");
                assertEquals(repair.path("workspace").path("proposal").path("definition"), repaired.path("definition"));
                assertEquals(repair.path("workspace").path("proposal").path("result"), repaired.path("result"));
                assertEquals(accepted.path("result").path("inputRevision"), repaired.path("definition").path("basedOnRevision"));
                assertTrue(repaired.path("manifest").path("locks").toString().contains("ATTEMPT_SCOPED"));
                Path repairedArchive = h.output.resolve("accepted-repair.zip"); DemoExportBrowserIT.exact(repaired, DemoExportBrowserIT.download(h, one, repairedArchive));
                h.record("UC-8/10: real protected repair explicitly accepted with exact pair/ancestry/attempt provenance; draft and unaccepted proposal exports contain only prior Current", one, "03-repair-accepted");

                String lesson = "demo-lesson-001"; String room = null;
                for (JsonNode assignment : repaired.path("result").path("timetable").path("assignments")) if (assignment.path("lessonId").asString().equals(lesson)) room = assignment.path("roomId").asString();
                assertNotNull(room); one.locator(".lesson-cell[data-lesson-id='" + lesson + "']").first().click();
                one.locator("#edit-room").selectOption(room.equals("demo-room-001") ? "demo-room-002" : "demo-room-001"); one.locator("#publish-manual-draft").waitFor();
                one.waitForFunction("()=>!document.querySelector('#publish-manual-draft').disabled"); JsonNode manual = h.snapshot(one);
                assertEquals(422, DemoAcceptanceBrowserIT.command(one, "/api/manual-draft", "PATCH", Map.of("action", "REASSIGN_LESSON", "lessonId", lesson, "teacherId", "foreign"), DemoAcceptanceBrowserIT.etag(h, one))); assertEquals(manual, h.snapshot(one));
                DemoExportBrowserIT.exact(repaired, DemoExportBrowserIT.download(h, one, h.output.resolve("manual-excluded.zip"))); assertEquals(manual, h.snapshot(one));
                one.reload(); one.locator("#publish-manual-draft").waitFor(); assertEquals(manual, h.snapshot(one));
                one.locator("#publish-manual-draft").click(); one.locator("#open-repair-setup").waitFor();
                JsonNode published = h.snapshot(one).path("workspace").path("acceptedBaseline");
                assertEquals(repaired.path("definition"), published.path("definition"));
                assertEquals(manual.path("workspace").path("manualDraft").path("assignments"), published.path("result").path("timetable").path("assignments"));
                assertEquals(repaired.path("manifest").path("locks"), published.path("manifest").path("locks")); assertEquals(foreign, h.snapshot(two));
                Path archive = h.output.resolve("published.zip"); DemoExportBrowserIT.exact(published, DemoExportBrowserIT.download(h, one, archive));
                h.record("UC-9/10: real verified manual room publication preserves exact definition/assignments/lock origins; teacher refusal, reload and draft-excluding export preserve Current", one, "04-manual-published");

                choose(two, archive); before = h.snapshot(two); two.onceDialog(Dialog::dismiss); two.locator("#demo-bundle-submit").click(); assertEquals(before, h.snapshot(two));
                two.onceDialog(Dialog::accept); two.locator("#demo-bundle-submit").click(); imported(two);
                assertEquals(published, h.snapshot(two).path("workspace").path("acceptedBaseline")); assertEquals(published, h.snapshot(one).path("workspace").path("acceptedBaseline"));
                Path definitionFile = temporary.resolve("definition.json"), resultFile = temporary.resolve("result.json");
                Files.write(definitionFile, CanonicalJson.bytes(published.path("definition"))); Files.write(resultFile, CanonicalJson.bytes(published.path("result")));
                two.locator("#demo-bundle-archive").setInputFiles(new Path[0]); two.locator("#demo-bundle-definition").setInputFiles(definitionFile); two.locator("#demo-bundle-result").setInputFiles(resultFile);
                two.onceDialog(Dialog::accept); two.locator("#demo-bundle-submit").click();
                two.waitForCondition(() -> h.snapshot(two).path("workspace").path("acceptedBaseline").path("manifest").path("locks").toString().contains("PERSISTENT_POLICY"));
                assertEquals(published.path("definition"), h.snapshot(two).path("workspace").path("acceptedBaseline").path("definition"));
                assertEquals(published.path("result"), h.snapshot(two).path("workspace").path("acceptedBaseline").path("result"));
                Path unsafe = temporary.resolve("unsafe.zip"); try (var zip = new ZipOutputStream(Files.newOutputStream(unsafe))) { zip.putNextEntry(new ZipEntry("../definition.json")); zip.write("{}".getBytes()); zip.closeEntry(); }
                choose(two, unsafe); before = h.snapshot(two); two.onceDialog(Dialog::accept); two.locator("#demo-bundle-submit").click();
                two.waitForFunction("()=>document.querySelector('#demo-bundle-status').textContent.includes('Nothing was imported')"); assertEquals(before, h.snapshot(two));
                two.setViewportSize(390, 844); two.locator(".narrow-banner").waitFor(); assertEquals(true, two.evaluate("document.documentElement.scrollWidth<=innerWidth"));
                h.record("UC-11/10: declined replacement retains exact owner; verified independent archive round-trip retains complete pair/manifest; standalone pair treats locks as policy; unsafe ZIP refuses unchanged; 390px fits", two, "05-import-narrow");
                two.setViewportSize(1440, 1000);
                Page stale = owner.newPage(); h.open(stale); choose(stale, archive); h.sample(one, "small", true); JsonNode newer = h.snapshot(one);
                stale.onceDialog(Dialog::accept); stale.locator("#demo-bundle-submit").click(); stale.waitForFunction("()=>document.querySelector('#demo-bundle-status').textContent.includes('workspace changed')"); assertEquals(newer, h.snapshot(one)); stale.close();
                for (int i = 0; i < 6; i++) { h.snapshot(one); assertEquals(404, one.request().get(h.base + "/api/runs/" + UUID.randomUUID()).status()); }
                Files.writeString(h.output.resolve("accepted-published.json"), published.toPrettyString()); assertTrue(h.errors.isEmpty());
                Files.writeString(h.output.resolve("verdict.txt"), "PASS\n");
                h.record("UC-11 stale: old same-owner tab cannot replace intervening sample; repeated normal/foreign status polls stay usable", one, "06-stale-refused");
            } catch (Throwable failure) { Files.writeString(h.output.resolve("verdict.txt"), "FAIL: " + failure); h.record("FAIL: " + failure, one, "failure"); throw failure; }
            finally {
                Files.write(h.output.resolve("browser-errors.txt"), h.errors);
                Files.writeString(h.output.resolve("ledger.md"), "# Remaining use cases: public Chrome walkthrough\n\n" + Instant.now() + "; " + h.base + "; normal TLS validation; two disposable private owners. Exact authorized production archive d292a016f6edb85874eb98597643e555159829e2ec3f10a73cc082d46f054dd7, deployment a96c3c89-1c5e-4a13-9093-fcfb40f29368.\n\n" + String.join("\n\n", h.steps) + "\n\nOverall: " + Files.readString(h.output.resolve("verdict.txt")) + "\nPublic actor checks only; local process fault/allowance boundary evidence remains separately recorded. Browser contexts close; same-image provider restart must remove disposable sessions. No account/resource/billing setting mutation. This is a Free-demo verification, not Hobby capacity/spend qualification.\n");
            }
        }
    }
    private static void choose(Page page, Path archive) {
        page.locator("#demo-bundle-panel summary").evaluate("e=>e.parentElement.open=true");
        page.locator("#demo-bundle-definition").setInputFiles(new Path[0]);
        page.locator("#demo-bundle-result").setInputFiles(new Path[0]);
        page.locator("#demo-bundle-archive").setInputFiles(archive);
    }
    private static void imported(Page page) { page.waitForFunction("()=>document.querySelector('#demo-bundle-status').textContent.includes('Accepted bundle opened')"); }
}
