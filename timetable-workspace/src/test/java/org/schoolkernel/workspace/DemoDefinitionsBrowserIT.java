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

class DemoDefinitionsBrowserIT {
    private static final JsonMapper JSON = JsonMapper.builder().build();
    @TempDir Path temporary;
    private Process server;
    private int port;
    private Path output;
    private String base;
    private final List<String> steps = new ArrayList<>();
    @Test void completeBrowserVersionJourney() throws Exception {
        output = Path.of(System.getProperty("demo.version.walkthrough.dir", System.getProperty("demo.walkthrough.dir", "../output/playwright/demo-deployment/UC-4/" + System.currentTimeMillis()))).toAbsolutePath();
        Files.createDirectories(output);
        boolean remote = Boolean.getBoolean("demo.remote.versions");
        base = remote ? "https://demo-smoke-production.up.railway.app" : null;
        if (!remote) {
            try (ServerSocket available = new ServerSocket(0)) { port = available.getLocalPort(); }
            base = "https://localhost:" + port;
            Path key = temporary.resolve("demo.p12");
            Process keytool = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "keytool").toString(),
                    "-genkeypair", "-alias", "demo", "-keyalg", "RSA", "-storetype", "PKCS12", "-keystore", key.toString(),
                    "-storepass", "disposable-demo-test", "-dname", "CN=localhost", "-ext", "SAN=dns:localhost", "-validity", "1")
                    .redirectErrorStream(true).redirectOutput(temporary.resolve("keytool.log").toFile()).start();
            assertTrue(keytool.waitFor(15, TimeUnit.SECONDS)); assertEquals(0, keytool.exitValue());
            start(key, 1);
        }
        List<String> errors = new ArrayList<>(), network = new ArrayList<>();
        try (Playwright playwright = Playwright.create(new Playwright.CreateOptions().setEnv(Map.of("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD", "1")));
             Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setChannel("chrome").setHeadless(true));
             BrowserContext first = browser.newContext(new Browser.NewContextOptions().setIgnoreHTTPSErrors(!remote).setViewportSize(1440, 1000));
             BrowserContext second = browser.newContext(new Browser.NewContextOptions().setIgnoreHTTPSErrors(!remote).setViewportSize(1440, 1000))) {
            Page one = first.newPage(), two = second.newPage();
            for (Page page : List.of(one,two)) {page.onPageError(errors::add);page.onRequest(r -> network.add(r.method()+" "+r.url()));}
            try {
                open(one);open(two);select(one,"small");use(one,null);
                JsonNode baseline=snapshot(one), other=snapshot(two);
                Files.writeString(output.resolve("initial-workspace.json"),baseline.toPrettyString());Files.writeString(output.resolve("second-owner.json"),other.toPrettyString());
                Page tab=first.newPage();tab.onPageError(errors::add);open(tab);
                one.evaluate("localStorage.setItem('uc4-unrelated-preference','keep')");
                String exact="{\r\n  \"unfinished\": \"é and 👩‍🏫\",\r\n  \"notes\": [1, 2]\r\n}\r\n ";
                Path file=temporary.resolve("My Draft.json");Files.writeString(file,exact);
                one.locator("#demo-file").setInputFiles(file);
                one.waitForFunction("() => document.querySelector('#demo-sample-preview').textContent.includes('Opened file')");
                assertEquals(exact.replace("\r\n","\n"),one.locator("#demo-json").inputValue());
                // A textarea normalizes CRLF by browser definition. The raw file bytes must remain intact until edited.
                saveAs(one,"my-draft",true);
                assertEquals(exact,stored(one,"my-draft").path("documentText").asString());
                Files.writeString(output.resolve("file-origin-envelope.json"),stored(one,"my-draft").toPrettyString());
                assertEquals("file",stored(one,"my-draft").path("derivedFrom").path("kind").asString());
                assertEquals(Set.of("formatVersion","identifier","savedAt","derivedFrom","documentText"),new HashSet<>(stored(one,"my-draft").propertyNames()));
                tab.waitForFunction("() => !!document.querySelector('#demo-definition option[value=my-draft]')");
                assertEquals(baseline,snapshot(one));assertEquals(other,snapshot(two));
                record("main 1-5/G1/G2: exact CRLF/Unicode file saved in a five-field file-origin envelope; unfinished object makes no server change; second tab list refreshes",one,"01-exact-save");
                one.reload();openReady(one);selectSaved(one,"my-draft");
                assertEquals(exact,download(one,"reload.json"));
                record("main 6/G3/G4: reload and selection preserve exact CRLF bytes; browser-local download matches the original file",one,"02-reload-download");
                String edit=one.locator("#demo-json").inputValue()+" ";one.locator("#demo-json").fill(edit);
                assertTrue(one.locator("#demo-edit-status").innerText().contains("Changed, not saved"));
                one.onceDialog(Dialog::dismiss);one.locator("#demo-revert").click();assertEquals(edit,one.locator("#demo-json").inputValue());
                one.onceDialog(Dialog::dismiss);one.locator("#demo-definition").selectOption("full");assertEquals("my-draft",one.locator("#demo-definition").inputValue());assertEquals(edit,one.locator("#demo-json").inputValue());
                java.util.concurrent.atomic.AtomicBoolean declinedFile = new java.util.concurrent.atomic.AtomicBoolean();
                one.onceDialog(dialog -> {dialog.dismiss();declinedFile.set(true);});one.locator("#demo-file").setInputFiles(file);one.waitForCondition(declinedFile::get);assertEquals(edit,one.locator("#demo-json").inputValue());
                saveAs(one,"my-draft",false);assertEquals(exact,stored(one,"my-draft").path("documentText").asString());assertEquals(edit,one.locator("#demo-json").inputValue());
                record("1b/4b/6a: declined selection, file opening, revert and named replacement preserve exact open and saved text",one,"03-declines");
                one.onceDialog(Dialog::accept);one.locator("#demo-revert").click();assertEquals(exact,download(one,"reverted.json"));
                edit="{\n \"unfinished\": true,\n \"second\": 2\n}\n";one.locator("#demo-json").fill(edit);saveAs(one,"my-draft",true);
                assertEquals(edit,stored(one,"my-draft").path("documentText").asString());assertEquals("saved",stored(one,"my-draft").path("derivedFrom").path("kind").asString());
                assertEquals("my-draft",stored(one,"my-draft").path("derivedFrom").path("identifier").asString());
                Files.writeString(output.resolve("replaced-envelope.json"),stored(one,"my-draft").toPrettyString());
                record("main 4-6/G2: confirmed named replacement stores exact edited text, saved-source derivation and explicit save time",one,"04-replace");
                for(String id:List.of("full","small","two-class","UpperCase","-bad","x".repeat(41))) {saveAs(one,id,true);assertTrue(one.locator("#demo-feedback").innerText().contains("reserved"));assertNull(local(one,"school-kernel.definition.v1."+id));}
                one.locator("#demo-json").fill("{\n \"a\": [1,]\n}");assertTrue(one.locator("#demo-edit-status").innerText().contains("line 2, column 10"));
                one.locator("#demo-save").click();assertTrue(one.locator("#demo-feedback").innerText().contains("line 2, column 10"));
                assertEquals("{\n \"a\": [1,]\n}",download(one,"invalid.json"));
                for(String text:List.of("[]","null","42","\"text\"")){one.locator("#demo-json").fill(text);one.locator("#demo-save").click();assertTrue(one.locator("#demo-feedback").innerText().contains("JSON object"));}
                record("4a/G1/G4: first syntax line/column, reserved/invalid labels and non-object roots refused; malformed text still downloads exactly",one,"05-validation");
                one.locator("#demo-json").fill("{\"keep\":true}");
                String old=local(one,"school-kernel.definition.v1.my-draft");
                one.evaluate("() => { window.uc4set=Storage.prototype.setItem; Storage.prototype.setItem=function(k,v){if(k.startsWith('school-kernel.definition.v1.'))throw new DOMException('full','QuotaExceededError');return uc4set.call(this,k,v);};}");
                saveAs(one,"my-draft",true);assertEquals(old,local(one,"school-kernel.definition.v1.my-draft"));assertEquals("{\"keep\":true}",one.locator("#demo-json").inputValue());
                assertTrue(one.locator("#demo-feedback").innerText().contains("Nothing was saved"));assertEquals("{\"keep\":true}",download(one,"quota.json"));
                one.evaluate("() => {Storage.prototype.setItem=window.uc4set;}");
                one.evaluate("() => { window.uc4get=Storage.prototype.getItem;Storage.prototype.getItem=function(){throw new DOMException('denied','SecurityError');};}");
                saveAs(one,"no-storage",true);assertTrue(one.locator("#demo-feedback").innerText().contains("unavailable"));assertEquals("{\"keep\":true}",one.locator("#demo-json").inputValue());
                one.evaluate("() => {Storage.prototype.getItem=window.uc4get;}");
                assertNull(local(one,"school-kernel.definition.v1.no-storage"));
                record("5a/minimal: injected browser quota and SecurityError preserve overwritten entry and open text; exact Download remains available",one,"06-storage-failure");
                Path large=temporary.resolve("large.json");Files.writeString(large," ".repeat(2*1024*1024+1));one.locator("#demo-file").setInputFiles(large);
                assertTrue(one.locator("#demo-feedback").innerText().contains("at most 2 MiB"));assertEquals("{\"keep\":true}",one.locator("#demo-json").inputValue());
                one.evaluate("() => {window.uc4buffer=File.prototype.arrayBuffer;File.prototype.arrayBuffer=async()=>{throw Error('file read failed');};}");
                one.locator("#demo-file").setInputFiles(file);one.waitForFunction("() => document.querySelector('#demo-feedback').textContent.includes('file read failed')");assertTrue(one.locator("#demo-feedback").innerText().contains("file read failed"));one.evaluate("() => {File.prototype.arrayBuffer=window.uc4buffer;}");
                assertEquals("{\"keep\":true}",one.locator("#demo-json").inputValue());
                one.evaluate("() => {const e=document.querySelector('#demo-json');e.value=JSON.stringify({x:'é'.repeat(1048576)});e.dispatchEvent(new Event('input'));}");
                one.locator("#demo-save").click();assertTrue(one.locator("#demo-feedback").innerText().contains("2 MiB"));assertEquals(old,local(one,"school-kernel.definition.v1.my-draft"));
                record("1c/4a: oversized UTF-8 file/editor and injected unreadable file refused; prior storage and text retained",one,"07-file-failures");
                one.locator("#demo-json").fill("{}");saveAs(one,"fresh",true);
                one.evaluate("() => {localStorage.setItem('school-kernel.definition.v1.corrupt','{');localStorage.setItem('school-kernel.definition.v1.full','{}');}");
                // Storage events come from another tab, never from synthetic calls on the same document.
                tab.evaluate("localStorage.setItem('school-kernel.definition.v1.unsupported',JSON.stringify({formatVersion:2}))");
                one.waitForFunction("() => document.querySelector('#demo-unreadable').textContent.includes('unsupported')");
                assertEquals(true,one.locator("#demo-definition option[value=corrupt]").evaluate("e=>e.disabled"));assertEquals(1,one.locator("#demo-definition optgroup[label='Sample schools'] option[value=full]").count());
                one.onceDialog(Dialog::dismiss);one.getByText("Delete unreadable version \"corrupt\"",new Page.GetByTextOptions().setExact(true)).click();assertEquals("{",local(one,"school-kernel.definition.v1.corrupt"));
                one.onceDialog(Dialog::accept);one.getByText("Delete unreadable version \"corrupt\"",new Page.GetByTextOptions().setExact(true)).click();assertNull(local(one,"school-kernel.definition.v1.corrupt"));
                record("1a/G2/G3: corrupt, unsupported and reserved-key entries stay unreadable, cannot execute and can only be explicitly deleted; samples remain immutable",one,"08-corrupt");
                selectSaved(tab,"fresh");tab.locator("#demo-json").fill("{\"external\":1}");saveAs(tab,"fresh",true);
                one.waitForFunction("() => document.querySelector('#demo-storage-status').textContent.includes('another tab')");
                assertEquals("{}",one.locator("#demo-json").inputValue());assertTrue(one.locator("#demo-use").isDisabled());
                one.onceDialog(Dialog::dismiss);one.locator("#demo-refresh-version").click();assertEquals("{}",one.locator("#demo-json").inputValue());
                one.onceDialog(Dialog::accept);one.locator("#demo-refresh-version").click();assertEquals("{\"external\":1}",one.locator("#demo-json").inputValue());
                assertEquals("fresh",one.locator("#demo-definition optgroup[label='Saved in this browser'] option").first().getAttribute("value"));
                record("main 5/G7: cross-tab replacement refreshes newest-first list without overwriting open text; declined reload retains it, explicit reload adopts exact stored text",one,"09-cross-tab");
                one.locator("#demo-json").fill("{\"unsaved\":true}");one.onceDialog(Dialog::dismiss);one.locator("#demo-delete").click();assertNotNull(local(one,"school-kernel.definition.v1.fresh"));
                one.evaluate("() => {window.uc4remove=Storage.prototype.removeItem;Storage.prototype.removeItem=function(k){if(k.startsWith('school-kernel.definition.v1.'))throw new DOMException('denied','SecurityError');return uc4remove.call(this,k);};}");
                one.onceDialog(Dialog::accept);one.locator("#demo-delete").click();assertNotNull(local(one,"school-kernel.definition.v1.fresh"));assertEquals("{\"unsaved\":true}",one.locator("#demo-json").inputValue());
                assertTrue(one.locator("#demo-feedback").innerText().contains("could not be changed"));one.evaluate("() => {Storage.prototype.removeItem=uc4remove;}");
                one.onceDialog(Dialog::accept);one.locator("#demo-delete").click();assertNull(local(one,"school-kernel.definition.v1.fresh"));assertEquals("{\"unsaved\":true}",one.locator("#demo-json").inputValue());
                tab.waitForFunction("() => document.querySelector('#demo-storage-status').textContent.includes('removed')");assertEquals("{\"external\":1}",tab.locator("#demo-json").inputValue());
                one.onceDialog(Dialog::dismiss);one.locator("#demo-delete-all").click();assertNotNull(local(one,"school-kernel.definition.v1.my-draft"));
                one.onceDialog(Dialog::accept);one.locator("#demo-delete-all").click();
                assertEquals(0,((Number)one.evaluate("Object.keys(localStorage).filter(k=>k.startsWith('school-kernel.definition.v1.')).length")).intValue());
                assertEquals("keep",local(one,"uc4-unrelated-preference"));assertEquals("{\"unsaved\":true}",one.locator("#demo-json").inputValue());assertEquals(baseline,snapshot(one));assertEquals(other,snapshot(two));
                record("main 6/6a/G3/minimal: declined, failed and confirmed single/all deletion preserve workspace, unrelated preferences and unsaved text; other tab retains removed version text",one,"10-deletions");
                one.onceDialog(Dialog::accept);select(one,"small");
                assertTrue(one.locator("#demo-in-use").innerText().contains("Small school · In use"));one.reload();openReady(one);
                assertTrue(one.locator("#demo-in-use").innerText().contains("Small school · In use"));
                // A real imported sample pair must have its own origin, not inherit the sample dropdown mark.
                assertEquals(baseline,snapshot(one));
                one.setViewportSize(390,844);assertEquals(true,one.locator("#demo-json").evaluate("e=>e.readOnly"));assertEquals(true,one.evaluate("document.documentElement.scrollWidth<=innerWidth"));
                record("G5/G6: actual loaded sample remains marked across selection/reload; 390px read-only editor has no page overflow",one,"11-in-use-narrow");
                one.setViewportSize(1440,1000);one.locator("#demo-definition").selectOption("small");
                one.waitForFunction("() => !document.querySelector('#demo-use').disabled");
                // Save exact sample text without changing Current. Saving never marks a custom version as used.
                saveAs(one,"small-copy",true);assertTrue(one.locator("#demo-in-use").innerText().contains("Small school · In use"));
                assertFalse(one.locator("#demo-definition option[value=small-copy]").innerText().contains("In use"));
                assertEquals(baseline,snapshot(one));assertEquals(other,snapshot(two));
                assertTrue(network.stream().noneMatch(line -> line.startsWith("POST") && !line.contains("/api/samples/small/use")));
                if(!remote)assertEquals(List.of("verify","verify","verify"),Files.readAllLines(temporary.resolve("kernel-launches")));
                record("G1/G6/G7: all local edits/saves/files/downloads send zero documents or labels and start zero visitor processes; saved copy is not falsely marked In use",one,"12-no-server-work");
                if (!one.locator("#export-accepted").isVisible()) one.locator("#utilities > summary").click();
                Download portable=one.waitForDownload(()->one.locator("#export-accepted").click());Path archive=temporary.resolve("small-accepted.zip");portable.saveAs(archive);
                one.locator("#open-repair-setup").click();one.locator("#start-repair-form input[name=period]").first().check();one.locator("#start-repair-form button[type=submit]").click();
                one.locator("#solve-draft").click();one.locator("#confirm-repair-accept").waitFor(new Locator.WaitForOptions().setTimeout(90000));
                assertTrue(one.locator("#demo-in-use").innerText().contains("Small school · In use"));
                one.locator("#confirm-repair-accept").check();one.locator("#accept-repair").click();
                one.waitForFunction("() => document.querySelector('#demo-in-use').textContent.includes('accepted repair')");
                Files.writeString(output.resolve("accepted-repair.json"),snapshot(one).toPrettyString());
                assertEquals("REPAIR_ACCEPTANCE",snapshot(one).path("workspace").path("importMode").asString());
                assertNotEquals(baseline.path("workspace").path("acceptedBaseline").path("definition"),snapshot(one).path("workspace").path("acceptedBaseline").path("definition"));
                assertEquals(0,one.locator("#demo-definition option").filter(new Locator.FilterOptions().setHasText("In use")).count());assertEquals(other,snapshot(two));
                record("G6: real packaged Small repair leaves sample In use during proposal, then explicit acceptance changes definition and clears every dropdown mark",one,"13-accepted-repair-tracking");
                if(!one.locator("#clear-workspace").isVisible())one.locator("#utilities > summary").click();
                one.onceDialog(Dialog::accept);one.locator("#clear-workspace").click();
                one.waitForFunction("()=>document.querySelector('#demo-in-use').textContent===''");
                one.locator("#demo-bundle-panel summary").evaluate("e=>e.parentElement.open=true");
                one.locator("#demo-bundle-archive").setInputFiles(archive);one.locator("#demo-bundle-submit").click();
                one.waitForFunction("() => document.querySelector('#demo-in-use').textContent.includes('imported bundle')");
                assertEquals(baseline.path("workspace").path("acceptedBaseline"),snapshot(one).path("workspace").path("acceptedBaseline"));
                assertEquals(0,one.locator("#demo-definition option").filter(new Locator.FilterOptions().setHasText("In use")).count());
                assertNotNull(local(one,"school-kernel.definition.v1.small-copy"));assertEquals(other,snapshot(two));
                Files.writeString(output.resolve("imported-workspace.json"),snapshot(one).toPrettyString());
                record("G6: real same-definition accepted-bundle import shows its own origin, never a sample/saved mark; browser versions and second visitor remain intact",one,"14-import-origin");
                assertTrue(errors.isEmpty(),errors.toString());tab.close();Files.writeString(output.resolve("verdict.txt"),"PASS\n");
            } catch(Throwable failure) {
                Files.writeString(output.resolve("verdict.txt"),"FAIL: "+failure+"\n");record("FAIL: "+failure.toString().split("\n")[0],one,"failure");throw failure;
            } finally {
                Files.write(output.resolve("network.txt"),network);Files.write(output.resolve("browser-errors.txt"),errors);
                Files.writeString(output.resolve("ledger.md"),"# UC-4 browser-version walkthrough\n\nDate: "+Instant.now()+"\nApplication: "+base+"\nBrowser: real Chrome, Playwright; two isolated owners and one same-owner tab. Disposable data only.\nPublic TLS validates normally; local TLS is disposable. Build/source identity is in the execution manifest.\n\n"+String.join("\n\n",steps)+"\n\nOverall: "+Files.readString(output.resolve("verdict.txt"))+"\nQuota/SecurityError/save/delete/file-read faults are explicitly injected in browser APIs. No server failure is claimed.\nContexts closed; local server stopped. Public sessions retained for expiry or explicit cleanup. UC-5 consumes saved-use tracking; model checks do not claim that future actor journey.\n");
            }
        } finally {if(!remote)stop();}
    }
    private void saveAs(Page page,String id,boolean replacement) {
        java.util.function.Consumer<Dialog> handler = dialog -> {
            if(dialog.type().equals("prompt")) dialog.accept(id);
            else {assertTrue(dialog.message().contains(id));if(replacement)dialog.accept();else dialog.dismiss();}
        };
        page.onDialog(handler);try {page.locator("#demo-save").click();} finally {page.offDialog(handler);}
    }
    private void selectSaved(Page page,String id) {
        page.locator("#demo-definition").selectOption(id);page.waitForFunction("id=>document.querySelector('#demo-definition').value===id && !document.querySelector('#demo-json').disabled",id);
    }
    private String local(Page page,String key) {return (String)page.evaluate("k=>localStorage.getItem(k)",key);}
    private JsonNode stored(Page page,String id) {return JSON.readTree(local(page,"school-kernel.definition.v1."+id));}
    private String download(Page page,String filename) throws Exception {
        Download download=page.waitForDownload(()->page.locator("#demo-download").click());Path file=temporary.resolve(filename);download.saveAs(file);return Files.readString(file);
    }
    private void open(Page page) { page.navigate(base + "/workspace/"); openReady(page); }
    private void openReady(Page page) { page.locator("#demo-sample-preview").filter(new Locator.FilterOptions().setHasText("520 lessons")).waitFor(); }
    private void select(Page page, String id) {
        page.locator("#demo-definition").selectOption(id);
        page.waitForFunction("id => !document.querySelector('#demo-use').disabled && document.querySelector('#demo-definition').value === id && JSON.parse(document.querySelector('#demo-json').value).displayName === ({small:'Small school', 'two-class':'Two-class school', full:'Full school'})[id]", id);
    }
    private void use(Page page, Boolean confirm) {
        if (confirm != null) page.onceDialog(confirm ? Dialog::accept : Dialog::dismiss);
        page.locator("#demo-use").click();
        page.waitForFunction("() => document.querySelector('#demo-feedback').textContent.includes('accepted sample is in use') && !document.querySelector('#demo-use').disabled");
    }
    private void assertPair(Page page, String id) throws Exception {
        JsonNode baseline = snapshot(page).path("workspace").path("acceptedBaseline");
        for (String kind : List.of("definition", "result"))
            assertEquals(JSON.readTree(Path.of("src/test/resources/demo/legacy-samples/" + id + "-" + kind + ".json")), baseline.path(kind));
    }
    private void exportExact(Page page, JsonNode baseline) throws Exception {
        if (!page.locator("#export-accepted").isVisible()) page.locator("#utilities > summary").click();
        Download download = page.waitForDownload(() -> page.locator("#export-accepted").click());
        Path file = temporary.resolve("sample-accepted.zip"); download.saveAs(file);
        var documents = new SafeImportReader().readArchive(Files.readAllBytes(file));
        assertEquals(baseline.path("definition"), documents.definition()); assertEquals(baseline.path("result"), documents.result());
        assertEquals(baseline.path("manifest"), documents.manifest());
        record("main 5: Full export entry downloads exact accepted definition/result/manifest and excludes unfinished repair", page, "09b-exact-export");
    }
    private void record(String step, Page page, String name) throws Exception {
        System.out.println("UC4 STEP " + name + ": " + step.split("\n")[0]);
        page.screenshot(new Page.ScreenshotOptions().setPath(output.resolve(name + ".png")).setFullPage(true));
        Files.writeString(output.resolve(name + ".txt"), page.locator("body").innerText());
        steps.add((step.startsWith("FAIL") ? "FAIL" : "PASS") + " | " + step + " | [screenshot](" + name + ".png) / [snapshot](" + name + ".txt)");
    }
    private JsonNode snapshot(Page page) { return JSON.readTree(page.request().get(base + "/api/workspace").body()); }
    private void awaitSchool(Page page, String title) {
        page.waitForFunction("title => document.querySelector('#state-card h2')?.textContent === title", title);
    }
    private long verificationCount(int number) throws Exception {
        return Files.readString(output.resolve("server-" + number + ".log")).lines().filter(line -> line.contains("kernelCommand=verify")).count();
    }
    private APIResponse command(Page page, String method, String path, String body, String etag) {
        JsonNode token = JSON.readTree(page.request().get(base + "/api/csrf").body());
        if (etag == null) etag = "\"ws-" + snapshot(page).path("version").longValue() + "\"";
        var options = RequestOptions.create().setMethod(method).setHeader("Origin", base).setHeader("If-Match", etag)
                .setHeader(token.path("headerName").stringValue(), token.path("token").stringValue());
        if (body != null) options.setHeader("Content-Type", "application/json").setData(body);
        return page.request().fetch(base + path, options);
    }
    private void start(Path key, int number) throws Exception {
        Path launcher = temporary.resolve("kernel");
        String command = "#!/bin/sh\nprintf '%s\\n' \"$1\" >> '" + temporary.resolve("kernel-launches") + "'\nexec '"
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
                "--demo.kernel-artifact=" + Path.of("../kernel-cli/target/school-kernel.jar").toAbsolutePath())
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

    private static void awaitState(Page page, String expected) throws Exception {
        long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(25);
        String actual = "";
        while (System.nanoTime() < deadline) {
            actual = (String) page.evaluate("async () => { const r=await fetch('/api/workspace'); const b=await r.json(); return r.status + ':' + b.state; }");
            if (actual.equals("200:" + expected)) return;
            Thread.sleep(100);
        }
        fail("Expected " + expected + "; last observed response " + actual);
    }
}
