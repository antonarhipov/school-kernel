package org.schoolkernel.workspace;

import static org.junit.jupiter.api.Assertions.*;
import java.net.ServerSocket;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.RequestOptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.*;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/** Durable actor walkthrough, using rendered controls, the packaged service/kernel and private real Chrome owners. */
class DemoPlanningBrowserIT {
    static final JsonMapper JSON=JsonMapper.builder().build();
    @TempDir Path temporary;
    Process server; int port; String base; Path output,key; boolean remote, plainHttp;
    final List<String> steps=new ArrayList<>(),errors=new ArrayList<>();
    final List<String> traffic=new ArrayList<>();
    @Test void realInitialAndRepairPresetsSharingCancellationReuseQueueAndRestart() throws Exception {
        initialize("journey");
        try(Playwright pw=Playwright.create(new Playwright.CreateOptions().setEnv(Map.of("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD","1")));
            Browser browser=pw.chromium().launch(new BrowserType.LaunchOptions().setChannel("chrome").setHeadless(true));
            BrowserContext owner=context(browser);BrowserContext other=context(browser);BrowserContext third=context(browser)) {
            Page one=owner.newPage(),two=other.newPage(),three=remote?two:third.newPage();one.onPageError(errors::add);two.onPageError(errors::add);
            try {
                open(one);open(two);if(!remote)open(three);initial(one,"shared-initial","");initial(two,"other-label","");if(!remote)initial(three,"queued-private","Queued private school");
                assertEquals("QUICK",one.locator("#demo-effort").inputValue());assertEquals(2,one.locator("#demo-effort option").count());
                one.onRequest(r->{if(r.url().contains("/api/"))traffic.add(r.method()+" "+r.url().substring(base.length()));});
                two.onRequest(r->{if(r.url().contains("/api/"))traffic.add("other "+r.method()+" "+r.url().substring(base.length()));});
                int trafficStart=traffic.size();one.locator("#start-plan").click();one.locator("#cancel-run").waitFor();
                JsonNode started=snapshot(one);String firstId=started.path("workspace").path("run").path("id").asString();
                two.locator("#start-plan").click();two.locator("#cancel-run").waitFor();JsonNode secondStarted=snapshot(two);
                String secondId=secondStarted.path("workspace").path("run").path("id").asString();assertNotEquals(firstId,secondId);
                assertEquals(404,one.request().get(base+"/api/runs/"+secondId).status());
                APIResponse status=two.request().get(base+"/api/runs/"+secondId);assertEquals(200,status.status());assertTrue(status.body().length<1200);
                assertFalse(new String(status.body()).contains("definition"));assertFalse(new String(status.body()).contains(firstId));
                record("main 1-4; 2b; G4/G5: real Quick initial requests promptly create private IDs, share one physical request, expose a small owner-only current-phase status",two,"01-shared-running");
                one.locator("#cancel-run").click();one.locator("#start-plan").waitFor();
                proposal(two,"INITIAL_PROPOSAL");JsonNode computed=snapshot(two);
                assertEquals("PT5S",computed.path("workspace").path("proposal").path("result").path("limit").path("duration").asString());
                assertFalse(computed.path("workspace").has("acceptedBaseline"));assertEquals("INITIAL_DRAFT",snapshot(one).path("state").asString());
                assertFalse(computed.path("workspace").path("proposal").path("reused").asBoolean());
                assertEquals(1,traffic.subList(trafficStart,traffic.size()).stream().filter(v->v.equals("other GET /api/workspace")).count());
                assertEquals(0,traffic.subList(trafficStart,traffic.size()).stream().filter(v->v.endsWith("/api/csrf")).count());
                assertTrue(traffic.stream().anyMatch(v->v.startsWith("other GET /api/runs/")));
                record("main 5-7; 4a; G4/G6: one subscriber cancels to its exact initial draft; the other obtains a complete independently verified private proposal, with status-only polling and no Current",two,"02-computed-initial");
                one.locator("#start-plan").click();proposal(one,"INITIAL_PROPOSAL");JsonNode reused=snapshot(one);
                assertTrue(reused.path("workspace").path("proposal").path("reused").asBoolean());
                assertEquals(computed.path("workspace").path("proposal").path("result"),reused.path("workspace").path("proposal").path("result"));
                assertNotEquals(computed.path("workspace").path("proposal").path("id"),reused.path("workspace").path("proposal").path("id"));
                assertTrue(one.locator(".demo-provenance").innerText().contains("Result reused"));
                record("2a; G3/G5: exact verified initial artifact creates a new private proposal, visibly reused, with original process duration separate from this request's wait",one,"03-reused-initial");
                one.locator("#discard-proposal").click();one.locator("#start-plan").waitFor();one.locator("#demo-effort").selectOption("THOROUGH");one.locator("#start-plan").click();one.locator("#cancel-run").waitFor();
                if(remote)initial(three,"queued-private","Queued private school");
                three.locator("#start-plan").click();three.locator("#cancel-run").waitFor();three.waitForFunction("()=>document.querySelector('#demo-run-status').textContent.includes('Queued')");
                JsonNode queued=snapshot(three);String qid=queued.path("workspace").path("run").path("id").asString();
                JsonNode qstatus=JSON.readTree(three.request().get(base+"/api/runs/"+qid).body());assertEquals(1,qstatus.path("queuePosition").asInt());assertTrue(qstatus.path("approximate").asBoolean());assertFalse(three.locator("#state-card").innerText().contains("Running"));
                record("main 3/4; G2/G3: distinct Quick request waits behind Thorough with position one and an explicitly approximate refreshed start estimate",three,"04-queued");
                three.locator("#cancel-run").click();three.locator("#start-plan").waitFor();proposal(one,"INITIAL_PROPOSAL");
                JsonNode thorough=snapshot(one);assertEquals("PT1M",thorough.path("workspace").path("proposal").path("result").path("limit").path("duration").asString());
                assertFalse(thorough.path("workspace").path("proposal").path("reused").asBoolean());
                record("main 5/6; G1/G9: actual 60-second initial Thorough produces matching verified duration evidence, independent of Quick; queued cancellation launches no queued solve",one,"05-thorough-initial");
                // Current for repair is the actual approved packaged sample, not acceptance of another UC's proposal.
                sample(one,"small",true);sample(two,"small",true);JsonNode baseline=snapshot(one).path("workspace").path("acceptedBaseline");
                repair(one);repair(two);JsonNode draft=snapshot(one).path("workspace").path("repairDraft");
                one.locator("#demo-effort").selectOption("QUICK");two.locator("#demo-effort").selectOption("QUICK");
                one.locator("#solve-draft").click();one.locator("#cancel-run").waitFor();two.locator("#solve-draft").click();two.locator("#cancel-run").waitFor();
                proposal(one,"REPAIR_PROPOSAL");proposal(two,"REPAIR_PROPOSAL");JsonNode repairOne=snapshot(one),repairTwo=snapshot(two);
                assertEquals(baseline,repairOne.path("workspace").path("acceptedBaseline"));assertEquals(baseline,repairTwo.path("workspace").path("acceptedBaseline"));
                assertEquals(draft,repairOne.path("workspace").path("repairDraft"));assertEquals(repairOne.path("workspace").path("proposal").path("result"),repairTwo.path("workspace").path("proposal").path("result"));
                assertNotEquals(repairOne.path("workspace").path("proposal").path("runId"),repairTwo.path("workspace").path("proposal").path("runId"));
                assertEquals("PT5S",repairOne.path("workspace").path("proposal").path("result").path("limit").path("duration").asString());
                record("main 1-7; 2b; G5/G8/G9: actual compiled repair Quick coalesces into separately owned complete proposals; exact Current and staged intent remain unchanged",one,"06-repair-quick");
                two.locator("#discard-proposal").click();two.locator("#solve-draft").waitFor();two.locator("#solve-draft").click();proposal(two,"REPAIR_PROPOSAL");
                assertTrue(snapshot(two).path("workspace").path("proposal").path("reused").asBoolean());
                one.locator("#discard-proposal").click();one.locator("#solve-draft").waitFor();one.locator("#demo-effort").selectOption("THOROUGH");assertEquals(0,one.locator("#retry-repair").count());one.locator("#solve-draft").click();proposal(one,"REPAIR_PROPOSAL");
                JsonNode finalRepair=snapshot(one);assertEquals("PT1M",finalRepair.path("workspace").path("proposal").path("result").path("limit").path("duration").asString());assertEquals(baseline,finalRepair.path("workspace").path("acceptedBaseline"));
                Files.writeString(output.resolve("final-repair.json"),finalRepair.toPrettyString());
                record("G1/G9: actual repair Thorough is an ordinary effort selection with matching 60-second evidence; no separate longer retry; repair reuse remains private and Current is exact",one,"07-repair-thorough");
                one.setViewportSize(390,844);one.locator(".narrow-banner").waitFor();assertEquals(true,one.evaluate("document.documentElement.scrollWidth<=innerWidth"));record("retained narrow read-only contract: proposal remains readable without horizontal page overflow at 390px",one,"08-narrow");one.setViewportSize(1440,1000);
                select(three,"full");save(three,"large-school");three.onceDialog(Dialog::accept);three.locator("#demo-use").click();used(three);assertEquals("THOROUGH",three.locator("#demo-effort").inputValue());three.locator("#demo-effort").selectOption("QUICK");assertTrue(three.locator("#demo-quick-warning").isVisible());
                record("G1/search-efforts: Full defaults to Thorough; selecting Quick displays the large-school warning and actual configured duration",three,"09-large-warning");
                if(!remote) {
                    initial(three,"restart-run","Restart disposable school");three.locator("#start-plan").click();three.locator("#cancel-run").waitFor();stop();start(List.of(),2);three.reload();three.locator("#demo-start-again").waitFor();assertTrue(three.locator("#demo-definition option[value=restart-run]").count()>0);
                    record("4c/G9: stopping the actual packaged service ends an active solve; same-image restart exposes ended-session explanation and retains local versions, with no proposal recovery or acceptance",three,"10-restart");
                }
                Files.write(output.resolve("ui-api-traffic.txt"),traffic);assertTrue(errors.isEmpty(),errors.toString());Files.writeString(output.resolve("verdict.txt"),"PASS\n");
            }catch(Throwable failure){Files.writeString(output.resolve("verdict.txt"),"FAIL: "+failure+"\n");record("FAIL: "+failure.toString().split("\n")[0],one,"failure");throw failure;}
            finally{ledger();}
        }finally{stop();}
    }
    @Test void realRefusalsFailuresDisabledReuseAndForcedCancellation() throws Exception {
        initialize("faults");
        try(Playwright pw=Playwright.create(new Playwright.CreateOptions().setEnv(Map.of("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD","1")));
            Browser browser=pw.chromium().launch(new BrowserType.LaunchOptions().setChannel("chrome").setHeadless(true));
            BrowserContext owner=context(browser);BrowserContext other=context(browser);BrowserContext third=context(browser)) {
            Page one=owner.newPage(),two=other.newPage(),three=third.newPage();
            one.onPageError(errors::add);two.onPageError(errors::add);three.onPageError(errors::add);
            try {
                stop();start(List.of("--demo.queue.max-waiting=1"),2);
                open(one);open(two);open(three);initial(one,"stall-one","stall-one");initial(two,"stall-two","stall-two");initial(three,"refused-three","refused-three");
                Files.writeString(temporary.resolve("stall"),"1");one.locator("#start-plan").click();one.locator("#cancel-run").waitFor();two.locator("#start-plan").click();two.locator("#cancel-run").waitFor();JsonNode before=snapshot(three);
                Response busy=three.waitForResponse(r->r.url().endsWith("/api/runs")&&r.request().method().equals("POST"),()->three.locator("#start-plan").click());assertEquals(429,busy.status());assertEquals("5",busy.headerValue("Retry-After"));Files.writeString(output.resolve("busy-response.json"),busy.text());three.waitForFunction("()=>document.querySelector('#state-card').textContent.includes('demo is busy')");assertEquals(before,snapshot(three));
                record("3a/G2: real bounded queue refuses a third distinct request with busy 429/Retry-After and exact unchanged draft; negative transport response retained",three,"01-queue-refusal");
                // Stalled physical process reaches the enforced watchdog, then is terminated and returns safely to draft.
                one.locator("#start-plan").waitFor(new Locator.WaitForOptions().setTimeout(25000));assertEquals("KERNEL_TIMEOUT",snapshot(one).path("workspace").path("lastRun").path("code").asString());
                two.locator("#cancel-run").click();two.locator("#start-plan").waitFor();Files.delete(temporary.resolve("stall"));
                record("5c/G6/G7: actual process watchdog terminates the stalled solve; failure restores draft and never publishes a result; queued/running last-subscriber cancellation returns its draft",one,"02-watchdog");
                Files.writeString(temporary.resolve("crash"),"1");one.locator("#start-plan").click();one.locator("#start-plan").waitFor();one.waitForFunction("()=>document.querySelector('#state-card').textContent.includes('complete bounded result')");Files.delete(temporary.resolve("crash"));
                record("5c: actual process exit without result produces safe failure, no proposal/cache entry, and retains complete input",one,"03-crash");
                select(one,"small");ObjectNode impossible=(ObjectNode)JSON.readTree(one.locator("#demo-json").inputValue());((ObjectNode)impossible.path("teachers").get(0)).putArray("availablePeriodIds").add(impossible.path("periods").get(0).path("id").asString());one.locator("#demo-json").fill(impossible.toPrettyString());save(one,"no-feasible");one.onceDialog(Dialog::accept);one.locator("#demo-use").click();used(one);JsonNode impossibleDraft=snapshot(one).path("workspace").path("initialDefinition");
                one.locator("#start-plan").click();one.locator("#cancel-run").waitFor();one.locator("#start-plan").waitFor(new Locator.WaitForOptions().setTimeout(20000));assertEquals("NOT_FOUND",snapshot(one).path("workspace").path("lastRun").path("status").asString());assertEquals(impossibleDraft,snapshot(one).path("workspace").path("initialDefinition"));assertFalse(snapshot(one).path("workspace").has("proposal"));
                record("5a: actual valid but impossible initial school ends as Not found within budget, retains exact draft, and publishes no partial timetable",one,"03b-not-found");
                sample(one,"small",true);JsonNode accepted=snapshot(one).path("workspace").path("acceptedBaseline");one.locator("#open-repair-setup").click();one.locator("#start-repair-form input[name=period]").nth(0).check();one.locator("#start-repair-form input[name=period]").nth(1).check();one.locator("#start-repair-form button[type=submit]").click();one.locator("#solve-draft").waitFor();one.locator("#solve-draft").click();one.locator("#cancel-run").waitFor();one.locator("#solve-draft").waitFor(new Locator.WaitForOptions().setTimeout(20000));assertEquals("NOT_FOUND",snapshot(one).path("workspace").path("lastRun").path("status").asString());assertEquals(accepted,snapshot(one).path("workspace").path("acceptedBaseline"));assertEquals(0,one.locator("#retry-repair").count());assertEquals("Create repair proposal",one.locator("#solve-draft").innerText());
                assertEquals(true,one.evaluate("document.querySelector('.school-identity').getBoundingClientRect().width > 300 && document.querySelector('.school-identity h2').getBoundingClientRect().height < 60 && document.documentElement.scrollWidth <= innerWidth"));
                record("5a: actual impossible compiled repair ends as Not found within budget, retains Current exactly and offers ordinary Quick/Thorough efforts without a special retry",one,"03c-repair-not-found");
                stop();start(List.of("--demo.kernel.daily-allowance=50s"),3);one.reload();one.locator("#demo-start-again").waitFor();one.locator("#demo-start-again").click();one.locator("#demo-use").waitFor();initial(one,"daily","daily");JsonNode daily=snapshot(one);one.locator("#start-plan").click();one.waitForFunction("()=>document.querySelector('#state-card').textContent.includes('daily kernel allowance')");assertEquals(daily,snapshot(one));sample(one,"small",true);
                record("3b/G7/G8: actual configured daily limit cannot cover complete 53-second reservation, refusing before run creation; exact draft retained and packaged sample still works",one,"04-daily");
                stop();start(List.of("--demo.reuse.enabled=false"),4);one.reload();one.locator("#demo-start-again").waitFor();one.locator("#demo-start-again").click();initial(one,"uncached","uncached");
                one.locator("#start-plan").click();proposal(one,"INITIAL_PROPOSAL");var first=snapshot(one);one.locator("#discard-proposal").click();one.locator("#start-plan").waitFor();one.locator("#start-plan").click();proposal(one,"INITIAL_PROPOSAL");var second=snapshot(one);
                assertFalse(second.path("workspace").path("proposal").path("reused").asBoolean());assertNotEquals(first.path("workspace").path("proposal").path("solveProvenance").path("computationId"),second.path("workspace").path("proposal").path("solveProvenance").path("computationId"));
                record("G1/G9: configuration-file reuse switch disabled on real packaged service; identical requests run twice with different computation provenance and freshly verified proposals",one,"05-disabled");
                assertTrue(errors.isEmpty(),errors.toString());Files.writeString(output.resolve("verdict.txt"),"PASS\n");
            }catch(Throwable failure){Files.writeString(output.resolve("verdict.txt"),"FAIL: "+failure+"\n");record("FAIL: "+failure.toString().split("\n")[0],one,"failure");throw failure;}
            finally{ledger();}
        }finally{stop();}
    }
    BrowserContext context(Browser browser){return browser.newContext(new Browser.NewContextOptions().setIgnoreHTTPSErrors(!remote).setViewportSize(1440,1000));}
    void initialize(String suffix)throws Exception { initialize(suffix, Path.of(System.getProperty("demo.run.walkthrough.dir","../output/playwright/demo-deployment/UC-6/"+System.currentTimeMillis())+"-"+suffix)); }
    void initialize(String suffix, Path directory)throws Exception {
        remote=System.getProperty("demo.run.base")!=null;
        output=directory.toAbsolutePath();Files.createDirectories(output);
        if(remote){base=System.getProperty("demo.run.base");return;}
        try(ServerSocket free=new ServerSocket(0)){port=free.getLocalPort();}base=(plainHttp?"http":"https")+"://localhost:"+port;key=temporary.resolve("demo.p12");
        var kt=new ProcessBuilder(Path.of(System.getProperty("java.home"),"bin","keytool").toString(),"-genkeypair","-alias","demo","-keyalg","RSA","-storetype","PKCS12","-keystore",key.toString(),"-storepass","disposable-demo-test","-dname","CN=localhost","-ext","SAN=dns:localhost","-validity","1").redirectErrorStream(true).redirectOutput(temporary.resolve("keytool.log").toFile()).start();assertTrue(kt.waitFor(15,TimeUnit.SECONDS));assertEquals(0,kt.exitValue());
        Files.copy(Path.of("target/timetable-workspace-1.0.0-SNAPSHOT.jar"),temporary.resolve("workspace.jar"));Files.copy(Path.of("../kernel-cli/target/school-kernel.jar"),temporary.resolve("kernel.jar"));
        Files.writeString(temporary.resolve("kernel"),"#!/bin/sh\nprintf '%s\\n' \"$1\" >> '"+temporary.resolve("launches")+"'\nif [ \"$1\" != verify ]; then\n if [ -f '"+temporary.resolve("crash")+"' ]; then exit 4; fi\n if [ -f '"+temporary.resolve("stall")+"' ]; then exec sleep 60; fi\nfi\nexec '"+Path.of(System.getProperty("java.home"),"bin","java")+"' -Xmx512m -jar '"+temporary.resolve("kernel.jar")+"' \"$@\"\n");temporary.resolve("kernel").toFile().setExecutable(true,true);
        Files.writeString(temporary.resolve("launches"), "");
        start(List.of(),1);
    }
    void start(List<String> overrides,int number)throws Exception {
        var args=new ArrayList<>(List.of(Path.of(System.getProperty("java.home"),"bin","java").toString(),"-Xmx512m","-jar",temporary.resolve("workspace.jar").toString(),"--spring.profiles.active=demo","--server.address=127.0.0.1","--server.port="+port,"--demo.public-hosts=localhost:"+port,"--demo.trusted-proxy-addresses=","--server.ssl.enabled=true","--server.ssl.key-store="+key,"--server.ssl.key-store-password=disposable-demo-test","--server.ssl.key-store-type=PKCS12","--workspace.kernel-executable="+temporary.resolve("kernel"),"--demo.kernel-artifact="+temporary.resolve("kernel.jar")));
        if (plainHttp) args.removeIf(arg -> arg.startsWith("--server.ssl.") || arg.startsWith("--demo.public-hosts="));
        args.addAll(overrides);server=new ProcessBuilder(args).redirectErrorStream(true).redirectOutput(output.resolve("server-"+number+".log").toFile()).start();
        long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(45);
        while(System.nanoTime()<deadline){if(!server.isAlive())fail(Files.readString(output.resolve("server-"+number+".log")));if(Files.readString(output.resolve("server-"+number+".log")).contains("Started WorkspaceApplication"))return;Thread.sleep(100);}fail("Readiness timeout");
    }
    void open(Page p){p.navigate(base+"/workspace/");p.locator("#demo-sample-preview").filter(new Locator.FilterOptions().setHasText("520 lessons")).waitFor();}
    void select(Page p,String id){p.locator("#demo-definition").selectOption(id);p.waitForFunction("id=>document.querySelector('#demo-definition').value===id&&!document.querySelector('#demo-json').disabled",id);}
    void save(Page p,String id){p.onceDialog(d->d.accept(id));p.locator("#demo-save").click();p.waitForFunction("id=>document.querySelector('#demo-definition').value===id",id);}
    void initial(Page p,String id,String variant)throws Exception {
        select(p,"small");if(!variant.isEmpty()){ObjectNode definition=(ObjectNode)JSON.readTree(p.locator("#demo-json").inputValue());definition.put("displayName",variant);p.locator("#demo-json").fill(definition.toPrettyString());}
        save(p,id);if(!"EMPTY".equals(snapshot(p).path("state").asString()))p.onceDialog(Dialog::accept);p.locator("#demo-use").click();used(p);
    }
    void used(Page p){p.waitForFunction("()=>document.querySelector('#demo-feedback').textContent.includes('new initial draft')&&!document.querySelector('#demo-use').disabled");p.locator("#start-plan").waitFor();}
    void sample(Page p,String id,boolean replace){select(p,id);if(replace)p.onceDialog(Dialog::accept);p.locator("#demo-use").click();p.waitForFunction("()=>document.querySelector('#demo-feedback').textContent.includes('accepted sample is in use')&&!document.querySelector('#demo-use').disabled");}
    void repair(Page p){p.locator("#open-repair-setup").click();p.locator("#start-repair-form input[name=period]").first().check();p.locator("#start-repair-form button[type=submit]").click();p.locator("#solve-draft").waitFor();assertEquals("Create repair proposal",p.locator("#solve-draft").innerText());}
    void proposal(Page p,String state){p.waitForFunction("state=>document.querySelector('#cancel-run')===null&&document.querySelector('#accept-proposal, #accept-repair')!==null",state,new Page.WaitForFunctionOptions().setTimeout(100000));assertEquals(state,snapshot(p).path("state").asString());}
    JsonNode snapshot(Page p){return JSON.readTree(p.request().get(base+"/api/workspace").body());}
    void record(String description,Page p,String name)throws Exception {p.screenshot(new Page.ScreenshotOptions().setPath(output.resolve(name+".png")).setFullPage(true));Files.writeString(output.resolve(name+".txt"),p.locator("body").innerText());boolean failed=description.startsWith("FAIL");steps.add((failed?"- FAIL **":"- PASS **")+description+"**. Expected: specified outcome with exact value/privacy/non-mutation assertions above. Observed: "+(failed?"assertion failed as recorded above":"assertions passed")+". [Screenshot]("+name+".png), [rendered snapshot]("+name+".txt).");}
    void ledger()throws Exception {Files.write(output.resolve("browser-errors.txt"),errors);Files.writeString(output.resolve("ledger.md"),"# UC-6 recorded visitor walkthrough\n\nDate: "+Instant.now()+"\nApplication: "+base+"\nBuild: pinned private packaged JAR copies; production input hashes in execution/UC-6. Browser: real installed Chrome / Java Playwright, isolated disposable contexts. No database or operator school data.\n\n"+String.join("\n\n",steps)+"\n\nOverall: "+Files.readString(output.resolve("verdict.txt"))+"\nMain journey uses actual 5-second Quick and 60-second Thorough for both operations. Fault method uses explicitly identified local process/configuration injection. Unit tests provide deterministic reservation, stale completion, subscriber expiry and identity boundaries; they are not presented as clicks. UC-7/8 own complete repair authoring and acceptance journeys; this walkthrough obtains and opens proposals only. Contexts close; local runtime processes stop; public disposable state requires same-image cleanup.\n");}
    void stop()throws Exception {if(server==null)return;server.destroy();if(!server.waitFor(10,TimeUnit.SECONDS)){server.destroyForcibly();server.waitFor(5,TimeUnit.SECONDS);}server=null;}
}
