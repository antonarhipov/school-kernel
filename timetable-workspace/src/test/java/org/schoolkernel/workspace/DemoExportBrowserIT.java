package org.schoolkernel.workspace;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import com.microsoft.playwright.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.JsonNode;

class DemoExportBrowserIT {
    @TempDir Path temporary;
    static byte[] download(DemoPlanningBrowserIT h,Page page,Path target) throws Exception {
        page.locator("#utilities").evaluate("e=>e.open=true");
        assertTrue(page.locator(".export-baseline").innerText().contains("unaccepted proposals are excluded"));
        Download download=page.waitForDownload(()->page.locator("#export-accepted").click());download.saveAs(target);return Files.readAllBytes(target);
    }
    static void exact(JsonNode accepted,byte[] archive) {
        var documents=new SafeImportReader().readArchive(archive);
        assertEquals(accepted.path("definition"),documents.definition());assertEquals(accepted.path("result"),documents.result());assertEquals(accepted.path("manifest"),documents.manifest());
    }
    @Test void sampleAndAcceptedRepairExportsExcludeEveryUnacceptedChangeWithoutKernelWork() throws Exception {
        var h=new DemoPlanningBrowserIT();h.temporary=temporary;
        h.initialize("export",Path.of(System.getProperty("demo.export.walkthrough.dir","../output/playwright/demo-deployment/UC-10/local-"+System.currentTimeMillis())));
        try(Playwright pw=Playwright.create(new Playwright.CreateOptions().setEnv(Map.of("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD","1")));
            Browser browser=pw.chromium().launch(new BrowserType.LaunchOptions().setChannel("chrome").setHeadless(true));BrowserContext owner=h.context(browser)) {
            Page one=owner.newPage();one.onPageError(h.errors::add);
            try {
                h.open(one);assertEquals(409,one.request().get(h.base+"/api/accepted/export").status());h.sample(one,"small",false);
                JsonNode sample=h.snapshot(one).path("workspace").path("acceptedBaseline");long commands=Files.readAllLines(temporary.resolve("launches")).size();
                exact(sample,download(h,one,h.output.resolve("sample.zip")));assertEquals(commands,Files.readAllLines(temporary.resolve("launches")).size());
                h.repair(one);JsonNode draft=h.snapshot(one);exact(sample,download(h,one,h.output.resolve("draft-excluded.zip")));assertEquals(draft,h.snapshot(one));
                h.record("main 1-4/1a/2a/G1-G3: no-Current export refused; sample and repair-draft exports contain exactly three accepted documents, exclude intent and start zero processes",one,"01-sample-export");
                one.locator("#solve-draft").click();h.proposal(one,"REPAIR_PROPOSAL");JsonNode proposal=h.snapshot(one);exact(sample,download(h,one,h.output.resolve("proposal-excluded.zip")));assertEquals(proposal,h.snapshot(one));
                one.locator("#confirm-repair-accept").check();one.locator("#accept-repair").click();one.locator("#open-repair-setup").waitFor();
                JsonNode repaired=h.snapshot(one).path("workspace").path("acceptedBaseline");commands=Files.readAllLines(temporary.resolve("launches")).size();
                byte[] repairedBytes=download(h,one,h.output.resolve("accepted-repair.zip"));exact(repaired,repairedBytes);assertEquals(commands,Files.readAllLines(temporary.resolve("launches")).size());
                assertEquals(sample.path("result").path("inputRevision"),new SafeImportReader().readArchive(repairedBytes).definition().path("basedOnRevision"));
                h.record("2a/G1/G4: generated proposal excluded; after own explicit acceptance, exported complete repair pair preserves parent lineage and provenance exactly without kernel work",one,"02-repair-export");
                one.locator(".lesson-cell[data-lesson-id='demo-lesson-001']").first().click();one.locator("#edit-room").selectOption("demo-room-001");one.locator("#publish-manual-draft").waitFor();
                JsonNode manual=h.snapshot(one);commands=Files.readAllLines(temporary.resolve("launches")).size();exact(repaired,download(h,one,h.output.resolve("manual-excluded.zip")));assertEquals(manual,h.snapshot(one));assertEquals(commands,Files.readAllLines(temporary.resolve("launches")).size());
                h.record("2a/G1/G2: manual edits remain private draft; export contains only the exact accepted repaired baseline and performs no verification or solve",one,"03-manual-excluded");
                assertTrue(h.errors.isEmpty());Files.writeString(h.output.resolve("verdict.txt"),"PASS\n");
            }catch(Throwable failure){Files.writeString(h.output.resolve("verdict.txt"),"FAIL: "+failure);h.record("FAIL: "+failure,one,"failure");throw failure;}
            finally{Files.writeString(h.output.resolve("ledger.md"),"# UC-10 recorded export journey\n\n"+Instant.now()+"; real Chrome/local TLS and packaged kernel; disposable owner.\n\n"+String.join("\n\n",h.steps)+"\n\nOverall: "+Files.readString(h.output.resolve("verdict.txt"))+"\nUC-11 owns fresh-owner verified re-import; archives retained here for round-trip verification. Deployment deferred; contexts/service cleaned up.\n");}
        }finally{h.stop();}
    }
}
