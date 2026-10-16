package org.schoolkernel.workspace;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import com.microsoft.playwright.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.JsonNode;

class DemoManualBrowserIT {
    @TempDir Path temporary;
    @Test void conflictRevertFailureRetryPublicationDiscardAndOwnership() throws Exception {
        var h=new DemoPlanningBrowserIT();h.temporary=temporary;
        h.initialize("manual",Path.of(System.getProperty("demo.manual.walkthrough.dir","../output/playwright/demo-deployment/UC-9/local-"+System.currentTimeMillis())));
        Path kernel=temporary.resolve("kernel");String script=Files.readString(kernel);
        Files.writeString(kernel,script.replace("#!/bin/sh\n","#!/bin/sh\nif [ \"$1\" = verify ] && [ -f '"+temporary.resolve("fail-verify")+"' ]; then exit 4; fi\n"));
        try(Playwright pw=Playwright.create(new Playwright.CreateOptions().setEnv(Map.of("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD","1")));
            Browser browser=pw.chromium().launch(new BrowserType.LaunchOptions().setChannel("chrome").setHeadless(true));
            BrowserContext owner=h.context(browser);BrowserContext other=h.context(browser)) {
            Page one=owner.newPage(),two=other.newPage();one.onPageError(h.errors::add);
            try {
                h.open(one);h.open(two);h.sample(one,"small",false);h.sample(two,"full",false);
                JsonNode current=h.snapshot(one).path("workspace").path("acceptedBaseline"),foreign=h.snapshot(two);
                String lesson="demo-lesson-001";
                one.locator(".lesson-cell[data-lesson-id='"+lesson+"']").first().click();
                one.locator("#edit-period").selectOption("demo-period-002");
                one.locator("#publish-manual-draft").waitFor();one.waitForFunction("()=>document.querySelector('#publish-manual-draft').disabled");
                JsonNode conflict=h.snapshot(one);assertFalse(conflict.path("workspace").path("manualDraft").path("conflicts").isEmpty());
                assertEquals(current,conflict.path("workspace").path("acceptedBaseline"));
                assertEquals(422,DemoAcceptanceBrowserIT.command(one,"/api/manual-draft/publish","POST",Map.of(),DemoAcceptanceBrowserIT.etag(h,one)));assertEquals(conflict,h.snapshot(one));
                h.record("main 1-3/2a: inline period edit starts an exact private manual draft with teacher/cohort conflict identities, blocks publication and retains complete Current",one,"01-conflict");
                one.locator("#revert-lesson-btn").click();one.waitForFunction("()=>!document.querySelector('#publish-manual-draft').disabled");
                assertEquals(current.path("result").path("timetable").path("assignments"),h.snapshot(one).path("workspace").path("manualDraft").path("assignments"));
                one.locator("#edit-period").selectOption("demo-period-003");one.waitForFunction("()=>document.querySelector('#revert-lesson-btn')!==null");
                one.locator("#edit-room").selectOption("demo-room-001");
                one.waitForFunction("()=>document.querySelector('#edit-room').value==='demo-room-001'");
                one.waitForCondition(()->h.snapshot(one).path("workspace").path("manualDraft").path("assignments").get(0).path("roomId").asString().equals("demo-room-001"));
                JsonNode draft=h.snapshot(one).path("workspace").path("manualDraft");one.reload();one.locator("#publish-manual-draft").waitFor();assertEquals(draft,h.snapshot(one).path("workspace").path("manualDraft"));
                one.locator("#teacher-change-route summary").click();assertTrue(one.locator("#teacher-change-route").innerText().contains("new initial planning"));
                JsonNode before=h.snapshot(one);
                assertEquals(422,DemoAcceptanceBrowserIT.command(one,"/api/manual-draft","PATCH",Map.of("action","REASSIGN_LESSON","lessonId",lesson,"teacherId","foreign"),DemoAcceptanceBrowserIT.etag(h,one)));assertEquals(before,h.snapshot(one));
                h.record("2b/2c/G1-G3: per-lesson revert restores full accepted assignment; valid period/room edits survive reload; JSON teacher guidance and refused teacher request preserve draft/Current",one,"02-valid-draft");
                Files.writeString(temporary.resolve("fail-verify"),"1");
                Response failed=one.waitForResponse(r->r.url().endsWith("/api/manual-draft/publish"),()->one.locator("#publish-manual-draft").click());assertEquals(503,failed.status());assertEquals(before,h.snapshot(one));Files.delete(temporary.resolve("fail-verify"));
                h.record("5a: actual verification process failure refuses publication, retains the exact valid draft and Current, and permits explicit retry",one,"03-failed-publication");
                one.locator("#publish-manual-draft").click();one.locator("#open-repair-setup").waitFor();
                JsonNode published=h.snapshot(one).path("workspace").path("acceptedBaseline");
                assertEquals(current.path("definition"),published.path("definition"));assertEquals(draft.path("assignments"),published.path("result").path("timetable").path("assignments"));
                assertEquals(current.path("manifest").path("locks"),published.path("manifest").path("locks"));assertEquals(foreign,h.snapshot(two));
                h.record("main 4-6/G2-G4: actual bounded verification succeeds on retry; complete matching baseline publishes atomically with exact definition/assignments/lock provenance and other owner unchanged",one,"04-published");
                one.locator(".lesson-cell[data-lesson-id='"+lesson+"']").first().click();one.locator("#edit-period").selectOption("demo-period-001");one.locator("#discard-manual-draft").waitFor();
                one.onceDialog(Dialog::dismiss);one.locator("#discard-manual-draft").click();assertEquals("MANUAL_DRAFT",h.snapshot(one).path("state").asString());
                one.onceDialog(Dialog::accept);one.locator("#discard-manual-draft").click();one.locator("#open-repair-setup").waitFor();assertEquals(published,h.snapshot(one).path("workspace").path("acceptedBaseline"));
                h.record("3a/minimal: declined discard retains manual work; confirmed discard removes only edits and retains exact last published Current",one,"05-discard");
                assertTrue(h.errors.isEmpty());Files.writeString(h.output.resolve("verdict.txt"),"PASS\n");
            }catch(Throwable failure){Files.writeString(h.output.resolve("verdict.txt"),"FAIL: "+failure);h.record("FAIL: "+failure,one,"failure");throw failure;}
            finally{Files.writeString(h.output.resolve("ledger.md"),"# UC-9 recorded manual journey\n\n"+Instant.now()+"; real Chrome/local TLS/packaged service and kernel; two disposable owners. Verification failure is an explicit local process-exit injection after startup.\n\n"+String.join("\n\n",h.steps)+"\n\nOverall: "+Files.readString(h.output.resolve("verdict.txt"))+"\nPublic validation deferred by user; contexts and service cleaned up.\n");}
        }finally{h.stop();}
    }
}
