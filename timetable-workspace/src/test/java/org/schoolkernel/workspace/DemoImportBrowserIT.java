package org.schoolkernel.workspace;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.zip.*;
import java.io.*;
import com.microsoft.playwright.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

class DemoImportBrowserIT {
    @TempDir Path temporary;
    void choose(Page p,Path file){p.locator("#demo-bundle-panel summary").evaluate("e=>e.parentElement.open=true");p.locator("#demo-bundle-archive").setInputFiles(file);}
    void imported(Page p){p.waitForFunction("()=>document.querySelector('#demo-bundle-status').textContent.includes('Accepted bundle opened')");}
    Path zip(String name,ImportDocuments documents) throws Exception {
        Path path=temporary.resolve(name);try(var z=new ZipOutputStream(Files.newOutputStream(path))){for(var e:Map.of("school-definition.json",documents.definition(),"timetable-result.json",documents.result(),"workspace-manifest.json",documents.manifest()).entrySet()){z.putNextEntry(new ZipEntry(e.getKey()));z.write(CanonicalJson.bytes(e.getValue()));z.closeEntry();}}return path;
    }
    @Test void verifiedFreshOwnerRoundTripsHitsFailuresStaleAndDailyRefusalWithoutSolving() throws Exception {
        var h=new DemoPlanningBrowserIT();h.temporary=temporary;
        h.initialize("import",Path.of(System.getProperty("demo.import.walkthrough.dir","../output/playwright/demo-deployment/UC-11/local-"+System.currentTimeMillis())));
        Path wrapper=temporary.resolve("kernel");Files.writeString(wrapper,Files.readString(wrapper).replace("#!/bin/sh\n","#!/bin/sh\nif [ \"$1\" = verify ] && [ -f '"+temporary.resolve("fail-verify")+"' ]; then exit 4; fi\n"));
        Path shared=temporary.resolve("shared-repair.zip");
        try(Playwright pw=Playwright.create(new Playwright.CreateOptions().setEnv(Map.of("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD","1")));
            Browser browser=pw.chromium().launch(new BrowserType.LaunchOptions().setChannel("chrome").setHeadless(true));BrowserContext owner=h.context(browser);BrowserContext other=h.context(browser);BrowserContext fresh=h.context(browser)) {
            Page one=owner.newPage(),two=other.newPage(),three=fresh.newPage();one.onPageError(h.errors::add);two.onPageError(h.errors::add);
            try {
                h.open(one);h.open(two);h.open(three);h.sample(one,"full",false);h.sample(two,"small",false);
                h.repair(two);two.locator(".repair-controls summary").click();two.locator("#preview-bulk").click();two.locator("#confirm-bulk").click();two.locator("[data-undo-bulk]").waitFor();
                two.locator("#solve-draft").click();h.proposal(two,"REPAIR_PROPOSAL");two.locator("#confirm-repair-accept").check();two.locator("#accept-repair").click();two.locator("#open-repair-setup").waitFor();
                byte[] sharedBytes=DemoExportBrowserIT.download(h,two,shared);
                ImportDocuments documents=new SafeImportReader().readArchive(sharedBytes);
                assertTrue(documents.manifest().path("locks").toString().contains("ATTEMPT_SCOPED"));
                // A real same-image restart clears originating solve/evidence, so first import is demonstrably cold.
                h.stop();h.start(List.of(),2);
                for(Page page:List.of(one,two,three)){page.reload();page.locator("#demo-start-again").click();page.locator("#demo-use").waitFor();}
                h.sample(one,"full",false);h.sample(two,"small",false);
                int importStart=Files.readAllLines(temporary.resolve("launches")).size();
                JsonNode foreign=h.snapshot(two),before=h.snapshot(one);
                choose(one,shared);one.onceDialog(Dialog::dismiss);one.locator("#demo-bundle-submit").click();assertEquals(before,h.snapshot(one));
                one.onceDialog(Dialog::accept);one.locator("#demo-bundle-submit").click();imported(one);
                JsonNode installed=h.snapshot(one).path("workspace").path("acceptedBaseline");assertEquals(documents.definition(),installed.path("definition"));assertEquals(documents.result(),installed.path("result"));assertEquals(documents.manifest(),installed.path("manifest"));assertEquals(foreign,h.snapshot(two));
                assertTrue(one.locator("#demo-in-use").innerText().contains("imported bundle"));
                assertEquals(importStart+1,Files.readAllLines(temporary.resolve("launches")).size());
                choose(three,shared);three.locator("#demo-bundle-submit").click();imported(three);assertEquals(installed,h.snapshot(three).path("workspace").path("acceptedBaseline"));assertEquals(importStart+1,Files.readAllLines(temporary.resolve("launches")).size());
                // The same exact documents supplied separately have no manifest authority: all locks become persistent policy.
                Path definitionFile=temporary.resolve("definition.json"),resultFile=temporary.resolve("result.json");Files.write(definitionFile,CanonicalJson.bytes(documents.definition()));Files.write(resultFile,CanonicalJson.bytes(documents.result()));
                three.locator("#demo-bundle-archive").setInputFiles(new Path[0]);three.locator("#demo-bundle-definition").setInputFiles(definitionFile);three.locator("#demo-bundle-result").setInputFiles(resultFile);three.onceDialog(Dialog::accept);three.locator("#demo-bundle-submit").click();
                three.waitForCondition(()->h.snapshot(three).path("workspace").path("acceptedBaseline").path("manifest").path("locks").toString().contains("PERSISTENT_POLICY"));
                assertEquals(documents.definition(),h.snapshot(three).path("workspace").path("acceptedBaseline").path("definition"));assertEquals(importStart+1,Files.readAllLines(temporary.resolve("launches")).size());
                one.setViewportSize(390,844);one.locator(".narrow-banner").waitFor();assertEquals(true,one.evaluate("document.documentElement.scrollWidth<=innerWidth"));h.record("retained narrow inspection: accepted import controls and exact Current stay readable with no horizontal page overflow at 390px",one,"01-narrow-import");one.setViewportSize(1440,1000);
                h.record("main 1-5/2a/G1-G4: exported accepted repair cold-verifies once, atomically replaces only confirmed owner and retains complete ancestry/manifest; fresh independent browser reuses exact verification without solving",one,"01-round-trip");
                Path unsafe=temporary.resolve("unsafe.zip");try(var z=new ZipOutputStream(Files.newOutputStream(unsafe))){z.putNextEntry(new ZipEntry("../definition.json"));z.write("{}".getBytes());z.closeEntry();}
                choose(one,unsafe);before=h.snapshot(one);one.onceDialog(Dialog::accept);one.locator("#demo-bundle-submit").click();one.waitForFunction("()=>document.querySelector('#demo-bundle-status').textContent.includes('Nothing was imported')");assertEquals(before,h.snapshot(one));assertEquals(importStart+1,Files.readAllLines(temporary.resolve("launches")).size());
                ObjectNode changed=(ObjectNode)documents.result().deepCopy();changed.put("correlationId",UUID.randomUUID().toString());Path different=zip("different.zip",new ImportDocuments(documents.definition(),changed,documents.manifest(),documents.mode()));
                Files.writeString(temporary.resolve("fail-verify"),"1");choose(one,different);one.onceDialog(Dialog::accept);one.locator("#demo-bundle-submit").click();one.waitForFunction("()=>document.querySelector('#demo-bundle-status').textContent.includes('Nothing was imported')");assertEquals(before,h.snapshot(one));Files.delete(temporary.resolve("fail-verify"));
                h.record("1b/3c/G5: unsafe paths refuse before any kernel command; changed complete result misses old evidence and actual verifier process failure preserves byte-for-byte Current",one,"02-refusals");
                Page stale=owner.newPage();h.open(stale);choose(stale,shared);h.sample(one,"small",true);JsonNode newer=h.snapshot(one);stale.onceDialog(Dialog::accept);stale.locator("#demo-bundle-submit").click();stale.waitForFunction("()=>document.querySelector('#demo-bundle-status').textContent.includes('workspace changed')");assertEquals(newer,h.snapshot(one));stale.close();
                h.record("4a/G5: stale same-owner tab cannot replace intervening sample use; request refuses with reload guidance and exact newer workspace retained",one,"03-stale");
                h.stop();h.start(List.of("--demo.kernel.daily-allowance=1s"),3);one.reload();one.locator("#demo-start-again").click();one.locator("#demo-use").waitFor();h.sample(one,"small",false);before=h.snapshot(one);choose(one,shared);one.onceDialog(Dialog::accept);one.locator("#demo-bundle-submit").click();one.waitForFunction("()=>document.querySelector('#demo-bundle-status').textContent.includes('daily kernel allowance')");assertEquals(before,h.snapshot(one));
                byte[] export=one.request().get(h.base+"/api/accepted/export").body();DemoExportBrowserIT.exact(before.path("workspace").path("acceptedBaseline"),export);
                h.record("3b/G1/G5: actual depleted reservation settings refuse new import verification before launch; sample Current stays exact and accepted export remains available without a process",one,"04-daily");
                assertTrue(Files.readAllLines(temporary.resolve("launches")).subList(importStart,Files.readAllLines(temporary.resolve("launches")).size()).stream().allMatch("verify"::equals));assertTrue(h.errors.isEmpty());Files.writeString(h.output.resolve("verdict.txt"),"PASS\n");
            }catch(Throwable failure){Files.writeString(h.output.resolve("verdict.txt"),"FAIL: "+failure);h.record("FAIL: "+failure,one,"failure");throw failure;}
            finally{Files.writeString(h.output.resolve("ledger.md"),"# UC-11 recorded import journey\n\n"+Instant.now()+"; local TLS, real packaged kernel/service and Chrome; three isolated visitors plus stale same-owner tab. Source accepted-repair archive is created through actual protected repair, explicit acceptance and the UC-10 download control in this isolated journey. Exact standalone documents without manifest are separately imported and their locks become persistent policy. Faults explicitly inject verifier exit/configure tiny allowance.\n\n"+String.join("\n\n",h.steps)+"\n\nOverall: "+Files.readString(h.output.resolve("verdict.txt"))+"\nCloses UC-10 fresh-owner accepted-repair round-trip handoff. No solve command, deployment or operator data; contexts/service cleaned up.\n");}
        }finally{h.stop();}
    }
}
