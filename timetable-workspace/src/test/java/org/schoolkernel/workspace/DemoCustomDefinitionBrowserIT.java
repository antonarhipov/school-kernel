package org.schoolkernel.workspace;

import static org.junit.jupiter.api.Assertions.*;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.*;
import java.security.cert.X509Certificate;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.RequestOptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

class DemoCustomDefinitionBrowserIT {
    static final JsonMapper JSON=JsonMapper.builder().build();
    @TempDir Path temporary;
    Process server;int port;Path output;String base;
    String daily="4h",timeout="30s";
    final List<String> steps=new ArrayList<>();
    final List<String> errors=new ArrayList<>();
    Path key;
    boolean remote;
    @Test void customSavedDefinitionJourney() throws Exception {
        initialize("journey");
        try(Playwright pw=Playwright.create(new Playwright.CreateOptions().setEnv(Map.of("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD","1")));
            Browser browser=pw.chromium().launch(new BrowserType.LaunchOptions().setChannel("chrome").setHeadless(true));
            BrowserContext owner=browser.newContext(new Browser.NewContextOptions().setIgnoreHTTPSErrors(!remote).setViewportSize(1440,1000));
            BrowserContext other=browser.newContext(new Browser.NewContextOptions().setIgnoreHTTPSErrors(!remote).setViewportSize(1440,1000))) {
            Page one=owner.newPage(),two=other.newPage();one.onPageError(errors::add);two.onPageError(errors::add);
            try {
                open(one);open(two);sample(one,"small",false);sample(two,"two-class",false);
                JsonNode baseline=snapshot(one),second=snapshot(two);
                Files.writeString(output.resolve("prior-workspace.json"),baseline.toPrettyString());Files.writeString(output.resolve("other-owner.json"),second.toPrettyString());
                select(one,"full");ObjectNode full=(ObjectNode)JSON.readTree(one.locator("#demo-json").inputValue());
                full.put("displayName","Visitor timetable é");full.withArray("lessons").add(full.withArray("lessons").remove(0));
                full.putArray("reservedPeriodIds");
                String exact=JSON.writerWithDefaultPrettyPrinter().writeValueAsString(full).replace("\n","\r\n")+"\r\n ";
                Path file=temporary.resolve("private-not-sent.json");Files.writeString(file,exact);
                // Full was edited in the real editor first. Reopen its CRLF file to prove exact request bytes.
                Files.writeString(output.resolve("before-full-edit.json"),JSON.writeValueAsString(one.evaluate("() => ({width:innerWidth, readOnly:document.querySelector('#demo-json').readOnly, disabled:document.querySelector('#demo-json').disabled, display:getComputedStyle(document.querySelector('#demo-json')).display, hint:document.querySelector('#demo-use-hint').textContent})")));
                one.locator("#demo-json").press("ControlOrMeta+End");one.locator("#demo-json").press("Space");assertTrue(one.locator("#demo-use").isDisabled());
                assertTrue(one.locator("#demo-use-hint").innerText().contains("Save or revert"));
                one.onceDialog(Dialog::accept);one.locator("#demo-file").setInputFiles(file);one.waitForFunction("() => document.querySelector('#demo-sample-preview').textContent.includes('Opened file')");
                save(one,"private-not-sent");assertEquals(exact,storedText(one));
                one.reload();ready(one);select(one,"private-not-sent");assertEquals(exact,storedText(one));
                assertEquals(baseline,snapshot(one));
                record("main 1; 1a; G1: edited Full, saved exact CRLF/Unicode text with array order and explicit empty optional field, reloaded; unsaved use disabled and no replacement",one,"01-saved-full");
                one.onceDialog(Dialog::dismiss);one.locator("#demo-use").click();assertEquals(baseline,snapshot(one));assertEquals(exact,storedText(one));
                record("main 2; 2a: replacement names accepted work and export option; declining preserves complete Current and exact saved/editor text",one,"02-declined");
                // Invalid saved versions remain editable, while every refused use retains the original accepted pair.
                ObjectNode invalid=full.deepCopy();((ObjectNode)invalid.path("lessons").get(0)).put("teacherId","unknown-teacher");
                saveText(one,invalid,"invalid-reference");useFailure(one,"/lessons");assertEquals(baseline,snapshot(one));
                record("3b: real packaged kernel returns located unknown-reference validation; accepted state and local version stay exact",one,"03-invalid-reference");
                ObjectNode lineage=full.deepCopy();lineage.put("basedOnRevision","sha256:"+"a".repeat(64));saveText(one,lineage,"lineage");useFailure(one,"/basedOnRevision");assertEquals(baseline,snapshot(one));
                ObjectNode unsupported=full.deepCopy();unsupported.put("unsupported",true);saveText(one,unsupported,"unsupported");useFailure(one,"unsupported");assertEquals(baseline,snapshot(one));
                ObjectNode excessive=full.deepCopy();var lessons=excessive.withArray("lessons");while(lessons.size()<2501)lessons.add(full.path("lessons").get(0));saveText(one,excessive,"too-complex");useFailure(one,"complexity limit");assertEquals(baseline,snapshot(one));
                record("3a/3b; G2/G3: repair ancestry, unsupported field and small-byte excessive lessons refuse before replacement; complete accepted state retained",one,"04-limits");
                select(one,"private-not-sent");
                // A modified transport request exercises the real byte guard through the actual use control.
                one.route("**/api/demo/definition",route->route.resume(new Route.ResumeOptions().setPostData(" ".repeat(2*1024*1024+1))));
                useFailure(one,"2 MiB");one.unroute("**/api/demo/definition");assertEquals(baseline,snapshot(one));assertEquals(exact,storedText(one));
                record("3a; G6: oversized transport request is refused by the real server byte guard; the readable local version and complete Current stay exact",one,"05-oversized");
                Page tab=owner.newPage();open(tab);sample(tab,"two-class",true);JsonNode newer=snapshot(tab);
                useFailure(one,"workspace changed");assertEquals(newer,snapshot(one));assertEquals(exact,storedText(one));
                record("4a; G5/G6: a real same-owner tab replaces Current; stale use refuses without overwriting its exact newer accepted state or editor",one,"06-stale");
                one.locator("#demo-reload").click();one.waitForFunction("() => document.querySelector('#demo-reload').hidden");
                final List<String> posted=new ArrayList<>();one.onRequest(r->{if(r.url().endsWith("/api/demo/definition"))posted.add(r.postData());});
                one.onceDialog(Dialog::accept);one.locator("#demo-use").click();awaitUse(one);
                JsonNode installed=snapshot(one);assertEquals("INITIAL_DRAFT",installed.path("state").asString());assertEquals(full,installed.path("workspace").path("initialDefinition"));
                assertFalse(installed.path("acceptedTimetable").asBoolean());assertFalse(installed.path("workspace").has("acceptedBaseline"));assertFalse(installed.path("workspace").has("proposal"));assertFalse(installed.path("workspace").has("run"));
                assertEquals("THOROUGH",installed.path("workspace").path("defaultEffort").asString());assertEquals(60,installed.path("workspace").path("defaultEffortSeconds").asInt());
                assertEquals(List.of(exact),posted);assertTrue(one.locator("#demo-default-effort").innerText().contains("Thorough search (60 seconds)"));assertTrue(one.locator("#start-plan").isVisible());
                one.waitForFunction("() => document.querySelector('#demo-in-use').textContent.includes('private-not-sent · In use as initial draft')");
                assertEquals(second,snapshot(two));Files.writeString(output.resolve("installed-workspace.json"),installed.toPrettyString());
                record("main 3-5; G1/G2/G5/success: complete raw stored text sent unchanged without label; verified exact definition installed atomically as INITIAL_DRAFT with no Current/run/proposal, summary and default Thorough; planning entry available; other owner exact",one,"07-initial-draft");
                one.reload();ready(one);select(one,"private-not-sent");one.waitForFunction("() => document.querySelector('#demo-in-use').textContent.includes('private-not-sent · In use as initial draft')");assertEquals(installed,snapshot(one));
                record("G5; UC-4 G6: loaded saved-version mark and exact complete initial draft survive real page reload",one,"08-reloaded");
                // Exact successful definition use again exercises reuse while retaining per-owner/version installation.
                one.onceDialog(Dialog::accept);one.locator("#demo-use").click();awaitUse(one);assertEquals(full,snapshot(one).path("workspace").path("initialDefinition"));
                record("G4/G5: identical use keeps exact draft values and receives a separately guarded installation; process-count reuse corroborated in local runtime logs",one,"09-reused");
                one.setViewportSize(390,844);one.waitForTimeout(150);assertEquals(true,one.locator("#demo-json").evaluate("e=>e.readOnly"));
                assertEquals(false,one.evaluate("document.documentElement.scrollWidth > innerWidth"));record("presentation: 390px definition panel is read-only and has no document overflow",one,"10-narrow");
                one.setViewportSize(1440,1000);select(one,"small");
                ObjectNode quick=(ObjectNode)JSON.readTree(one.locator("#demo-json").inputValue());quick.put("displayName","Small custom");saveText(one,quick,"small-custom");
                one.onceDialog(Dialog::accept);one.locator("#demo-use").click();awaitUse(one);
                assertEquals("small",snapshot(one).path("workspace").path("origin").asString());
                assertEquals("QUICK",snapshot(one).path("workspace").path("defaultEffort").asString());
                assertEquals(5,snapshot(one).path("workspace").path("defaultEffortSeconds").asInt());assertEquals(second,snapshot(two));
                record("main 4; G5: separately saved sample-derived definition records only its recognized sample origin and chooses the Quick default for its small lesson count",one,"11-sample-origin-quick");
                assertTrue(errors.isEmpty(),errors.toString());Files.writeString(output.resolve("verdict.txt"),"PASS\n");tab.close();
                if(!remote){String launches=Files.readString(temporary.resolve("kernel-launches"));assertEquals(6,launches.lines().count());assertTrue(launches.lines().allMatch("verify"::equals));Files.writeString(output.resolve("kernel-launches.txt"),launches);}
            }catch(Throwable failure){Files.writeString(output.resolve("verdict.txt"),"FAIL: "+failure+"\n");record("FAIL: "+failure.toString().split("\n")[0],one,"failure");throw failure;}
            finally{ledger("Custom saved-definition journey");}
        } finally{if(!remote)stop();}
    }

    @Test void realBusyFailureDailyAndStaleDuringVerification() throws Exception {
        if(Boolean.getBoolean("demo.remote.custom")) return; // This method's injected process/config faults are local only.
        timeout="3s";initialize("faults");
        try(Playwright pw=Playwright.create(new Playwright.CreateOptions().setEnv(Map.of("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD","1")));
            Browser browser=pw.chromium().launch(new BrowserType.LaunchOptions().setChannel("chrome").setHeadless(true));
            BrowserContext owner=browser.newContext(new Browser.NewContextOptions().setIgnoreHTTPSErrors(true).setViewportSize(1440,1000))) {
            Page one=owner.newPage();one.onPageError(errors::add);
            try {
                open(one);sample(one,"small",false);JsonNode baseline=snapshot(one);
                select(one,"small");ObjectNode definition=(ObjectNode)JSON.readTree(one.locator("#demo-json").inputValue());definition.put("displayName","Disposable custom");saveText(one,definition,"custom-faults");
                Files.writeString(temporary.resolve("stall"),"fault");
                one.evaluate("""
                    async text => {const c=await (await fetch('/api/csrf')).json();const s=await (await fetch('/api/workspace')).json();
                    const options={method:'POST',headers:{'Content-Type':'text/plain','If-Match':'\"ws-'+s.version+'\"','X-Demo-Confirmed':'true',[c.headerName]:c.token},body:text};
                    window.heldChecks=[fetch('/api/demo/definition',options).then(r=>r.json()),fetch('/api/demo/definition',options).then(r=>r.json())];}
                    """,JSON.writeValueAsString(definition));
                one.waitForTimeout(350);useFailure(one,"busy");assertEquals(baseline,snapshot(one));
                record("3c; G4/G6: two real stalled standalone verifier processes occupy both slots; third real UI use returns busy after configured 200ms, retaining complete accepted state",one,"01-verification-busy");
                one.evaluate("async()=>window.failedChecks=await Promise.all(window.heldChecks)");Files.delete(temporary.resolve("stall"));assertEquals(baseline,snapshot(one));
                record("3d: real verifier watchdog terminates both deliberately stalled commands; neither installs a definition; reservations settle after actual termination",one,"02-watchdog");
                Files.writeString(temporary.resolve("crash"),"fault");useFailure(one,"bounded verification evidence");Files.delete(temporary.resolve("crash"));assertEquals(baseline,snapshot(one));
                record("3d: injected real process exit without structured output yields safe failure; prior Current and saved text retained",one,"03-process-failure");
                // Start validation, then mutate the real workspace before the verifier completes normally.
                // Delay wrapper uses a marker once and then execs the actual packaged kernel.
                Path delayed=temporary.resolve("kernel");String original=Files.readString(delayed);
                Files.writeString(delayed,original.replace("exec '","sleep 1\nexec '"));
                Page tab=owner.newPage();open(tab);
                one.onceDialog(Dialog::accept);one.locator("#demo-use").click();
                sample(tab,"two-class",true);JsonNode newer=snapshot(tab);
                one.waitForFunction("() => document.querySelector('#demo-feedback').textContent.toLowerCase().includes('workspace changed')");assertEquals(newer,snapshot(one));
                record("4a: validation launches first, same-owner tab changes Current during actual packaged verification; completed evidence cannot overwrite the newer exact baseline",one,"04-stale-completion");
                Files.writeString(delayed,original);tab.close();
                // A restart resets counters/session. New configuration deliberately cannot reserve one verifier maximum.
                stop();daily="6s";start(key,2);one.reload();one.locator("#demo-start-again").waitFor();one.locator("#demo-start-again").click();ready(one);sample(one,"small",false);JsonNode beforeDaily=snapshot(one);
                select(one,"custom-faults");useFailure(one,"daily kernel allowance");assertEquals(beforeDaily,snapshot(one));
                record("3c; G6: configured real daily allowance cannot cover the seven-second reservation; UI shows daily refusal with complete accepted baseline/local version retained; sample use remains available",one,"05-daily-exhausted");
                Files.writeString(output.resolve("verdict.txt"),"PASS\n");assertTrue(errors.isEmpty(),errors.toString());
            }catch(Throwable failure){Files.writeString(output.resolve("verdict.txt"),"FAIL: "+failure+"\n");record("FAIL: "+failure.toString().split("\n")[0],one,"failure");throw failure;}
            finally{ledger("Real bounded verification faults");}
        }finally{stop();}
    }
    void initialize(String suffix) throws Exception {
        remote=Boolean.getBoolean("demo.remote.custom");output=Path.of(System.getProperty("demo.custom.walkthrough.dir","../output/playwright/demo-deployment/UC-5/"+System.currentTimeMillis())+"-"+suffix).toAbsolutePath();Files.createDirectories(output);
        base=remote?"https://demo-smoke-production.up.railway.app":null;
        if(!remote){try(ServerSocket free=new ServerSocket(0)){port=free.getLocalPort();}base="https://localhost:"+port;key=temporary.resolve("demo.p12");
            var keytool=new ProcessBuilder(Path.of(System.getProperty("java.home"),"bin","keytool").toString(),"-genkeypair","-alias","demo","-keyalg","RSA","-storetype","PKCS12","-keystore",key.toString(),"-storepass","disposable-demo-test","-dname","CN=localhost","-ext","SAN=dns:localhost","-validity","1").redirectErrorStream(true).redirectOutput(temporary.resolve("keytool.log").toFile()).start();assertTrue(keytool.waitFor(15,TimeUnit.SECONDS));assertEquals(0,keytool.exitValue());start(key,1);}
    }
    void open(Page p){p.navigate(base+"/workspace/");ready(p);}
    void ready(Page p){p.locator("#demo-sample-preview").filter(new Locator.FilterOptions().setHasText("544 lessons")).waitFor();}
    void select(Page p,String id){p.locator("#demo-definition").selectOption(id);p.waitForFunction("id=>document.querySelector('#demo-definition').value===id&&!document.querySelector('#demo-json').disabled",id);}
    void sample(Page p,String id,boolean replace){select(p,id);if(replace)p.onceDialog(Dialog::accept);p.locator("#demo-use").click();p.waitForFunction("()=>document.querySelector('#demo-feedback').textContent.includes('accepted sample is in use')&&!document.querySelector('#demo-use').disabled");}
    void save(Page p,String id){p.onceDialog(d->d.accept(id));p.locator("#demo-save").click();p.waitForFunction("id=>document.querySelector('#demo-definition').value===id",id);}
    void saveText(Page p,JsonNode value,String id)throws Exception{
        String text=JSON.writerWithDefaultPrettyPrinter().writeValueAsString(value);
        if(text.length()<50000)p.locator("#demo-json").fill(text);
        else {Path file=temporary.resolve(id+".json");Files.writeString(file,text);p.locator("#demo-file").setInputFiles(file);p.waitForFunction("text=>document.querySelector('#demo-json').value===text",text);}
        save(p,id);
    }
    String storedText(Page p){return(String)p.evaluate("()=>JSON.parse(localStorage.getItem('school-kernel.definition.v1.'+document.querySelector('#demo-definition').value)).documentText");}
    void useFailure(Page p,String expected){p.onceDialog(Dialog::accept);p.locator("#demo-use").click();p.waitForFunction("text=>document.querySelector('#demo-feedback').textContent.toLowerCase().includes(text.toLowerCase())&&!document.querySelector('#demo-use').disabled",expected);}
    void awaitUse(Page p){p.waitForFunction("()=>document.querySelector('#demo-feedback').textContent.includes('new initial draft')&&!document.querySelector('#demo-use').disabled");}
    JsonNode snapshot(Page p){return JSON.readTree(p.request().get(base+"/api/workspace").body());}
    void record(String description,Page p,String name)throws Exception{p.screenshot(new Page.ScreenshotOptions().setPath(output.resolve(name+".png")).setFullPage(true).setTimeout(8000));Files.writeString(output.resolve(name+".txt"),p.locator("body").innerText());steps.add("- PASS **"+description+"**. Expected: contract outcome and exact non-mutation assertions described above. Observed: assertions passed. [Screenshot]("+name+".png), [snapshot]("+name+".txt).");}
    void ledger(String title)throws Exception{Files.write(output.resolve("browser-errors.txt"),errors);Files.writeString(output.resolve("ledger.md"),"# UC-5 "+title+"\n\nDate: "+Instant.now()+"\nApplication: "+base+"\nBrowser: real installed Chrome through Java Playwright. Isolated disposable owner contexts; local runtime is a fixed private JAR copy. Submission hashes are in execution/UC-5. No operator data.\n\n"+String.join("\n\n",steps)+"\n\nOverall: "+Files.readString(output.resolve("verdict.txt"))+"\nNo solve is started by use. UC-6 owns effort selection/admission and generated proposals. Oversized transport, process stalls/exits and bounded test configuration are explicitly identified. Browser contexts close; local server stops; public workspaces require cleanup restart.\n");}
    private void start(Path key, int number) throws Exception {
        Path launcher = temporary.resolve("kernel");
        String command = "#!/bin/sh\nif [ -f '" + temporary.resolve("stall") + "' ]; then exec sleep 60; fi\nif [ -f '" + temporary.resolve("crash") + "' ]; then exit 4; fi\nprintf '%s\\n' \"$1\" >> '" + temporary.resolve("kernel-launches") + "'\nexec '"
                + Path.of("../school-kernel").toAbsolutePath() + "' \"$@\"\n";
        Files.writeString(launcher, command); launcher.toFile().setExecutable(true, true);
        Path runtimeJar = temporary.resolve("workspace-runtime.jar");
        if (!Files.exists(runtimeJar)) Files.copy(Path.of("target/timetable-workspace-1.0.0-SNAPSHOT.jar"), runtimeJar);
        server = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "java").toString(), "-jar",
                runtimeJar.toAbsolutePath().toString(),
                "--spring.profiles.active=demo", "--server.address=127.0.0.1", "--server.port=" + port,
                "--demo.public-hosts=localhost:" + port, "--demo.trusted-proxy-addresses=",
                "--server.ssl.enabled=true", "--server.ssl.key-store=" + key,
                "--server.ssl.key-store-password=disposable-demo-test", "--server.ssl.key-store-type=PKCS12",
                "--workspace.kernel-executable=" + temporary.resolve("kernel"),
                "--demo.kernel-artifact=" + Path.of("../kernel-cli/target/school-kernel.jar").toAbsolutePath(),
                "--demo.kernel.daily-allowance=" + daily, "--demo.kernel.verification-wait=200ms",
                "--demo.kernel.verification-timeout=" + timeout)
                .redirectErrorStream(true).redirectOutput(output.resolve("server-" + number + ".log").toFile()).start();
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(45);
        while (System.nanoTime() < deadline) {
            if (!server.isAlive()) fail("Demo exited: " + Files.readString(output.resolve("server-" + number + ".log")));
            try { if (probe("localhost:" + port, "/healthz", "").startsWith("HTTP/1.1 200")) return; }
            catch (java.io.IOException ignored) {}
            Thread.sleep(100);
        }
        fail("Demo did not become ready");
    }
    private String probe(String host, String path, String extra) throws Exception {
        SSLContext tls = SSLContext.getInstance("TLS");
        tls.init(null, new TrustManager[]{new X509TrustManager() {
            public void checkClientTrusted(X509Certificate[] certificates, String auth) {}
            public void checkServerTrusted(X509Certificate[] certificates, String auth) {}
            public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
        }}, new java.security.SecureRandom());
        try (SSLSocket socket = (SSLSocket) tls.getSocketFactory().createSocket("localhost", port)) {
            socket.setSoTimeout(5000);
            socket.getOutputStream().write(("GET " + path + " HTTP/1.1\r\nHost: " + host + "\r\n" + extra + "Connection: close\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
            return new String(socket.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        }
    }
    private void stop() throws Exception {
        if (server == null) return;
        server.destroy(); if (!server.waitFor(8, TimeUnit.SECONDS)) { server.destroyForcibly(); server.waitFor(5, TimeUnit.SECONDS); }
        server = null;
    }


}
