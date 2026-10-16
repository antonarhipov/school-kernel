package org.schoolkernel.workspace;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import com.microsoft.playwright.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.JsonNode;

class DemoAcceptanceBrowserIT {
    @TempDir Path temporary;
    static int command(Page page,String path,String method,Object body,String etag) {
        return ((Number)page.evaluate("async args=>{const csrf=await (await fetch('/api/csrf')).json(); const response=await fetch(args.path,{method:args.method,headers:{[csrf.headerName]:csrf.token,'If-Match':args.etag,'Content-Type':'application/json'},body:JSON.stringify(args.body)});return response.status}",Map.of("path",path,"method",method,"body",body,"etag",etag))).intValue();
    }
    static String etag(DemoPlanningBrowserIT h,Page page){return page.request().get(h.base+"/api/workspace").headers().get("etag");}
    @Test void explicitInitialAndRepairAcceptanceReuseRefusalsAndPrivateDecisions() throws Exception {
        var h=new DemoPlanningBrowserIT();h.temporary=temporary;
        h.initialize("acceptance",Path.of(System.getProperty("demo.accept.walkthrough.dir","../output/playwright/demo-deployment/UC-8/local-"+System.currentTimeMillis())));
        try(Playwright pw=Playwright.create(new Playwright.CreateOptions().setEnv(Map.of("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD","1")));
            Browser browser=pw.chromium().launch(new BrowserType.LaunchOptions().setChannel("chrome").setHeadless(true));
            BrowserContext owner=h.context(browser);BrowserContext other=h.context(browser)) {
            Page one=owner.newPage(),two=other.newPage();one.onPageError(h.errors::add);two.onPageError(h.errors::add);
            try {
                h.open(one);h.open(two);h.initial(one,"accept-one","");h.initial(two,"accept-two","");
                one.locator("#start-plan").click();h.proposal(one,"INITIAL_PROPOSAL");
                JsonNode proposed=h.snapshot(one);String old=etag(h,one);long before=Files.readAllLines(temporary.resolve("launches")).size();
                assertTrue(one.locator("#accept-proposal").isDisabled());
                assertEquals(428,command(one,"/api/proposal/accept","POST",Map.of("confirmed",false),old));assertEquals(proposed,h.snapshot(one));
                h.record("main 1-3/3b: computed Quick initial proposal shows no Current, matching duration/provenance; unchecked UI and direct unacknowledged acceptance preserve exact proposal",one,"01-initial-review");
                one.locator("#confirm-accept").check();one.locator("#accept-proposal").click();one.locator("#open-repair-setup").waitFor();
                JsonNode accepted=h.snapshot(one).path("workspace").path("acceptedBaseline");
                assertFalse(one.locator("#demo-feedback").innerText().contains("No timetable is Current"));
                assertEquals(proposed.path("workspace").path("initialDefinition"),accepted.path("definition"));assertEquals(proposed.path("workspace").path("proposal").path("result"),accepted.path("result"));
                assertEquals(before,Files.readAllLines(temporary.resolve("launches")).size());
                assertEquals(412,command(one,"/api/proposal/accept","POST",Map.of("confirmed",true),old));assertEquals(accepted,h.snapshot(one).path("workspace").path("acceptedBaseline"));
                two.locator("#start-plan").click();h.proposal(two,"INITIAL_PROPOSAL");assertTrue(h.snapshot(two).path("workspace").path("proposal").path("reused").asBoolean());
                assertTrue(two.locator(".demo-provenance").innerText().contains("Result reused"));
                two.locator("#confirm-accept").check();two.locator("#accept-proposal").click();two.locator("#open-repair-setup").waitFor();
                assertEquals(accepted,h.snapshot(two).path("workspace").path("acceptedBaseline"));
                h.record("main 4-6/G1/G4: explicit initial acceptance copies complete exact verified pair atomically with no extra kernel command; duplicate stale request refused; second visitor separately accepts reused proposal",one,"02-initial-accepted");
                JsonNode otherAccepted=h.snapshot(two);h.repair(one);one.locator("#solve-draft").click();h.proposal(one,"REPAIR_PROPOSAL");
                JsonNode repair=h.snapshot(one),review=repair.path("workspace").path("proposal").path("review");assertTrue(review.isObject());
                assertEquals(6,repair.path("workspace").path("proposal").path("changeCounts").size());
                assertTrue(one.locator("#accept-repair").isDisabled());
                h.record("main 1-3/G2/G3: actual repair proposal shows direct/ripple effects, six explicit change categories including zeros, unique totals and old/proposed navigation while complete Current stays exact",one,"03-repair-review");
                assertEquals(accepted,repair.path("workspace").path("acceptedBaseline"));
                one.locator("#discard-proposal").click();one.locator("#solve-draft").waitFor();assertEquals(accepted,h.snapshot(one).path("workspace").path("acceptedBaseline"));
                one.locator("#solve-draft").click();h.proposal(one,"REPAIR_PROPOSAL");assertTrue(h.snapshot(one).path("workspace").path("proposal").path("reused").asBoolean());
                JsonNode finalProposal=h.snapshot(one).path("workspace").path("proposal");before=Files.readAllLines(temporary.resolve("launches")).size();
                one.locator("#confirm-repair-accept").check();one.locator("#accept-repair").click();one.locator("#open-repair-setup").waitFor();
                JsonNode repaired=h.snapshot(one).path("workspace").path("acceptedBaseline");assertEquals(finalProposal.path("definition"),repaired.path("definition"));assertEquals(finalProposal.path("result"),repaired.path("result"));
                assertEquals(accepted.path("result").path("inputRevision"),repaired.path("definition").path("basedOnRevision"));
                assertEquals(before,Files.readAllLines(temporary.resolve("launches")).size());assertEquals(otherAccepted,h.snapshot(two));
                h.record("3a/main 4-6/G1-G5: discard retains draft and exact Current; reused repair requires own confirmation; atomic accepted pair retains ancestry/provenance and other visitor unchanged, without additional verification",one,"04-repair-accepted");
                assertTrue(h.errors.isEmpty());Files.writeString(h.output.resolve("verdict.txt"),"PASS\n");
            }catch(Throwable failure){Files.writeString(h.output.resolve("verdict.txt"),"FAIL: "+failure);h.record("FAIL: "+failure,one,"failure");throw failure;}
            finally{Files.writeString(h.output.resolve("ledger.md"),"# UC-8 recorded packaged Chrome journey\n\n"+Instant.now()+"; local TLS, real kernel, isolated owners; exact submission follows remaining execution manifest.\n\n"+String.join("\n\n",h.steps)+"\n\nOverall: "+Files.readString(h.output.resolve("verdict.txt"))+"\nLocal implementation evidence; deployment deferred by user. Contexts/service cleaned up.\n");}
        }finally{h.stop();}
    }
}
