package org.schoolkernel.workspace;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import com.microsoft.playwright.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.JsonNode;

class DemoRepairDraftBrowserIT {
    @TempDir Path temporary;
    @Test void protectedRepairStagingUndoConflictsPrivacyAndDiscard() throws Exception {
        var h = new DemoPlanningBrowserIT(); h.temporary = temporary;
        h.initialize("repair", Path.of(System.getProperty("demo.repair.walkthrough.dir", "../output/playwright/demo-deployment/UC-7/local-"+System.currentTimeMillis())));
        try (Playwright pw=Playwright.create(new Playwright.CreateOptions().setEnv(Map.of("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD","1")));
             Browser browser=pw.chromium().launch(new BrowserType.LaunchOptions().setChannel("chrome").setHeadless(true));
             BrowserContext owner=h.context(browser); BrowserContext other=h.context(browser)) {
            Page one=owner.newPage(),two=other.newPage(); one.onPageError(h.errors::add);
            try {
                h.open(one); h.open(two); h.sample(one,"small",false); h.sample(two,"full",false);
                JsonNode accepted=h.snapshot(one).path("workspace").path("acceptedBaseline");
                JsonNode foreign=h.snapshot(two);
                h.repair(one);
                JsonNode initial=h.snapshot(one).path("workspace").path("repairDraft");
                String affected=initial.path("directEffectLessonIds").get(0).asString();
                h.record("main 1-4/G1/G4: real sample starts private weekly unavailability with exact direct lesson identities and unchanged Current",one,"01-staging");
                one.locator("[data-draft-effect]").first().click();
                one.locator("#apply-pin").click();
                one.waitForFunction("()=>document.querySelector('#draft-conflict-count').textContent!=='0'");
                assertTrue(one.locator("#solve-draft").isDisabled());
                assertEquals(affected,h.snapshot(one).path("workspace").path("repairDraft").path("intent").path("pins").get(0).path("lessonId").asString());
                h.record("G1/main 4: pinning the accepted period conflicts with declared unavailability; complete draft is retained and solve is blocked",one,"02-conflicting-pin");
                one.locator("#remove-pin").click();
                one.waitForFunction("()=>document.querySelector('#draft-conflict-count').textContent==='0'");
                one.locator(".repair-controls summary").click(); one.locator("#preview-bulk").click(); one.locator("#cancel-bulk").waitFor();
                JsonNode before=h.snapshot(one); one.locator("#cancel-bulk").click(); assertEquals(before,h.snapshot(one));
                one.locator(".repair-controls summary").click(); one.locator("#preview-bulk").click(); one.locator("#confirm-bulk").click();
                one.locator("[data-undo-bulk]").waitFor();
                assertFalse(h.snapshot(one).path("workspace").path("repairDraft").path("intent").path("pins").isEmpty());
                h.record("main 3/3a/G1: exact unaffected bulk preview cancels without mutation; confirmed snapshot pins accepted dimensions with distinct bulk provenance",one,"03-bulk-pins");
                one.locator("[data-undo-bulk]").click(); one.waitForFunction("()=>document.querySelector('[data-undo-bulk]')===null");
                assertEquals(initial,h.snapshot(one).path("workspace").path("repairDraft"));
                one.reload(); one.locator("#solve-draft").waitFor(); assertEquals(initial,h.snapshot(one).path("workspace").path("repairDraft"));
                one.locator("#teacher-change-route summary").click(); assertTrue(one.locator("#teacher-change-route").innerText().contains("new initial planning"));
                one.locator("[data-undo-availability]").click(); one.waitForFunction("()=>document.querySelector('[data-undo-availability]')===null");
                assertTrue(h.snapshot(one).path("workspace").path("repairDraft").path("intent").path("changes").isEmpty());
                assertEquals(accepted,h.snapshot(one).path("workspace").path("acceptedBaseline"));
                assertEquals(foreign,h.snapshot(two));
                h.record("4a/G2-G5: individual/bulk undo and reload preserve exact intent; availability undo restores Current values; JSON guidance transfers no work; second owner unchanged",one,"04-undo");
                one.locator("#confirm-discard-draft").check(); one.locator("#discard-draft").click(); one.locator("#open-repair-setup").waitFor();
                assertEquals("ACCEPTED_BASELINE",h.snapshot(one).path("state").asString()); assertEquals(accepted,h.snapshot(one).path("workspace").path("acceptedBaseline"));
                assertEquals(3,Files.readAllLines(temporary.resolve("launches")).size());
                h.record("5a/minimal: confirmed discard removes repair work, retains complete exact Current, and starts zero visitor kernel commands",one,"05-discard");
                assertTrue(h.errors.isEmpty()); Files.writeString(h.output.resolve("verdict.txt"),"PASS\n");
            } catch(Throwable failure) { Files.writeString(h.output.resolve("verdict.txt"),"FAIL: "+failure); h.record("FAIL: "+failure,one,"failure"); throw failure; }
            finally { Files.writeString(h.output.resolve("ledger.md"),"# UC-7 packaged local visitor walkthrough\n\n"+Instant.now()+"; real Chrome, local TLS, packaged kernel/service; isolated disposable owners. Base d55ab636, exact changes in remaining execution evidence.\n\n"+String.join("\n\n",h.steps)+"\n\nOverall: "+Files.readString(h.output.resolve("verdict.txt"))+"\nDeployment/public qualification deferred by user instruction. Browser contexts and service cleaned up.\n"); }
        } finally { h.stop(); }
    }
}
