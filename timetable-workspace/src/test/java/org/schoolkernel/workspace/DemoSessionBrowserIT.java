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

/** Packaged service, real TLS and kernel, two isolated Chrome contexts; never uses PostgreSQL/operator data. */
class DemoSessionBrowserIT {
    private static final JsonMapper JSON = JsonMapper.builder().build();
    @TempDir Path temporary;
    private Process server;
    private int port;
    private Path output;
    private String base;
    private final List<String> steps = new ArrayList<>();

    @Test void privateSamplesDraftsRunsExportsStaleTabsAndRestart() throws Exception {
        output = Path.of(System.getProperty("demo.walkthrough.dir",
                "../output/playwright/demo-deployment/UC-2/" + System.currentTimeMillis())).toAbsolutePath();
        Files.createDirectories(output);
        try (ServerSocket available = new ServerSocket(0)) { port = available.getLocalPort(); }
        base = "https://localhost:" + port;
        Path key = temporary.resolve("demo.p12");
        Process keytool = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "keytool").toString(),
                "-genkeypair", "-alias", "demo", "-keyalg", "RSA", "-keysize", "2048", "-storetype", "PKCS12",
                "-keystore", key.toString(), "-storepass", "disposable-demo-test", "-keypass", "disposable-demo-test",
                "-dname", "CN=localhost", "-ext", "SAN=dns:localhost,ip:127.0.0.1", "-validity", "1")
                .redirectErrorStream(true).redirectOutput(temporary.resolve("keytool.log").toFile()).start();
        assertTrue(keytool.waitFor(15, TimeUnit.SECONDS)); assertEquals(0, keytool.exitValue());
        start(key, 1);
        List<String> errors = new ArrayList<>();
        try (Playwright playwright = Playwright.create(new Playwright.CreateOptions()
                .setEnv(Map.of("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD", "1")));
             Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setChannel("chrome").setHeadless(true));
             BrowserContext first = browser.newContext(new Browser.NewContextOptions().setIgnoreHTTPSErrors(true).setViewportSize(1280, 800));
             BrowserContext second = browser.newContext(new Browser.NewContextOptions().setIgnoreHTTPSErrors(true).setViewportSize(1280, 800))) {
            Page one = first.newPage(); one.onPageError(errors::add);
            Page two = second.newPage(); two.onPageError(errors::add);
            try {
            one.navigate(base + "/workspace/"); one.locator("#demo-sample-preview").filter(new Locator.FilterOptions().setHasText("544 lessons")).waitFor();
            assertEquals("full", one.locator("#demo-definition").inputValue());
            assertEquals("EMPTY", snapshot(one).path("state").stringValue());
            assertEquals(0, one.locator("#open-authoring").count());
            assertTrue(one.locator("#demo-retention").innerText().contains("export accepted timetables"));
            two.navigate(base + "/workspace/"); two.locator("#demo-json").waitFor();
            var cookie = first.cookies().stream().filter(value -> value.name.equals("JSESSIONID")).findFirst().orElseThrow();
            assertTrue(cookie.secure); assertTrue(cookie.httpOnly); assertEquals("STRICT", cookie.sameSite.name());
            record("UC-2 main/G2/G4: Full previews in EMPTY; no authoring entry; persistent retention/privacy; Secure/HttpOnly/Strict cookie", one, "01-empty");
            long startupVerifications = verificationCount(1);
            assertEquals(3, startupVerifications);
            one.locator("#demo-use").click(); awaitSchool(one, "Full school");
            assertEquals("EMPTY", snapshot(two).path("state").stringValue());
            assertEquals(startupVerifications, verificationCount(1));
            JsonNode full = snapshot(one).path("workspace").path("acceptedBaseline");
            assertEquals(544, full.path("result").path("timetable").path("assignments").size());
            record("UC-2 ownership / UC-3 enabling: Full accepted privately without a visitor process; other browser stays EMPTY", one, "02-full");
            one.setViewportSize(390, 844);
            one.locator(".narrow-banner").waitFor();
            assertEquals(true, one.evaluate("document.documentElement.scrollWidth <= innerWidth"));
            assertEquals(true, one.locator("#demo-json").evaluate("element => element.readOnly"));
            record("UC-2 retained narrow inspection: no horizontal page overflow at 390 x 844", one, "03-narrow");
            one.setViewportSize(1280, 800);
            for (String id : List.of("two-class", "small")) {
                one.locator("#demo-definition").selectOption(id);
                one.waitForFunction("id => document.querySelector('#demo-json').value.includes(id === 'small' ? 'Small school' : 'Two-class school')", id);
                one.onceDialog(Dialog::accept); one.locator("#demo-use").click();
                awaitSchool(one, id.equals("small") ? "Small school" : "Two-class school");
                assertEquals(startupVerifications, verificationCount(1));
            }
            JsonNode accepted = snapshot(one).path("workspace").path("acceptedBaseline");
            Page sameOwner = first.newPage(); sameOwner.navigate(base + "/workspace/"); sameOwner.locator("#demo-json").waitFor();
            assertEquals(accepted, snapshot(sameOwner).path("workspace").path("acceptedBaseline"));
            String staleVersion = "\"ws-" + snapshot(one).path("version").longValue() + "\"";
            one.locator("#open-repair-setup").click();
            one.locator("#start-repair-form input[name=period]").first().check();
            one.locator("#start-repair-form button[type=submit]").click();
            one.waitForFunction("() => document.querySelector('#solve-draft') !== null");
            assertEquals("REPAIR_DRAFT", snapshot(one).path("state").stringValue());
            assertEquals(accepted, snapshot(one).path("workspace").path("acceptedBaseline"));
            assertEquals("EMPTY", snapshot(two).path("state").stringValue());
            APIResponse stale = command(sameOwner, "POST", "/api/samples/full/use", "{\"confirmed\":true}", staleVersion);
            assertEquals(412, stale.status()); assertEquals(accepted, snapshot(one).path("workspace").path("acceptedBaseline"));
            record("UC-2 G1/G7 / stale tab: private repair staging preserves exact Current; old shared-tab version cannot replace it", one, "04-private-repair");

            if (!one.locator("#export-accepted").isVisible()) one.locator("#utilities > summary").click();
            Download exported = one.waitForDownload(() -> one.locator("#export-accepted").click());
            Path archive = temporary.resolve("accepted.zip"); exported.saveAs(archive);
            var documents = new SafeImportReader().readArchive(Files.readAllBytes(archive));
            assertEquals(accepted.path("definition"), documents.definition());
            assertEquals(accepted.path("result"), documents.result());
            assertEquals(accepted.path("manifest"), documents.manifest());
            record("UC-2 private export: download contains exact Current while the unaccepted repair remains excluded", one, "05-export");

            two.locator("#demo-bundle-panel summary").evaluate("e=>e.parentElement.open=true");
            two.locator("#demo-bundle-archive").setInputFiles(archive); two.locator("#demo-bundle-submit").click();
            awaitSchool(two, "Small school");
            assertEquals(accepted, snapshot(two).path("workspace").path("acceptedBaseline"));
            JsonNode ownerBeforeClear = snapshot(one);
            two.locator("#utilities > summary").click();
            two.onceDialog(dialog -> {
                assertTrue(dialog.message().contains("Export Current first")); dialog.dismiss();
            });
            two.locator("#clear-workspace").click();
            assertEquals(accepted, snapshot(two).path("workspace").path("acceptedBaseline"));
            two.onceDialog(Dialog::accept); two.locator("#clear-workspace").click();
            awaitState(two, "EMPTY");
            assertEquals(ownerBeforeClear, snapshot(one));
            record("UC-2 G7/4b/4c: exact bundle imports privately; declined clear preserves it; confirmed clear affects only that owner", two, "05b-private-import-clear");

            ObjectNode custom = (ObjectNode) JSON.readTree(Path.of("src/test/resources/demo/legacy-samples/small-definition.json"));
            custom.put("schoolId", "private-visitor-school").put("displayName", "Private visitor school");
            Path customPath = temporary.resolve("private-definition.json"); Files.write(customPath, JSON.writeValueAsBytes(custom));
            two.locator("#demo-file").setInputFiles(customPath);
            two.waitForFunction("() => document.querySelector('#demo-sample-preview').textContent.includes('Opened file')");
            two.onceDialog(dialog -> dialog.accept("private-visitor-definition")); two.locator("#demo-save").click();
            two.locator("#demo-use").click();
            two.locator("#start-plan").waitFor();
            assertEquals(custom, snapshot(two).path("workspace").path("initialDefinition"));
            assertFalse(snapshot(one).toString().contains("private-visitor-school"));
            two.locator("#start-plan").click();
            awaitState(two, "SOLVING_INITIAL");
            JsonNode running = snapshot(two); String runId = running.path("workspace").path("run").path("id").stringValue();
            assertTrue(two.locator("#demo-use").isDisabled());
            assertTrue(two.locator("#demo-use-hint").innerText().contains("Wait for or cancel"));
            assertEquals(409, command(two, "POST", "/api/samples/full/use", "{\"confirmed\":true}",
                    "\"ws-" + running.path("version").longValue() + "\"").status());
            assertEquals(running, snapshot(two));
            assertNotNull(runId);
            assertEquals(409, command(two, "POST", "/api/workspace/clear", null,
                    "\"ws-" + running.path("version").longValue() + "\"").status());
            assertEquals(running, snapshot(two));
            assertEquals(404, one.request().get(base + "/api/runs/" + runId).status());
            assertEquals(404, one.request().get(base + "/api/runs/" + UUID.randomUUID()).status());
            assertEquals(409, command(one, "DELETE", "/api/runs/" + runId, null, null).status());
            assertEquals(200, command(two, "DELETE", "/api/runs/" + runId, null,
                    "\"ws-" + running.path("version").longValue() + "\"").status());
            assertEquals("INITIAL_DRAFT", snapshot(two).path("state").stringValue());
            assertEquals(accepted, snapshot(one).path("workspace").path("acceptedBaseline"));
            record("UC-2 G7: real private definition import and asynchronous run; foreign/random IDs are indistinguishable; only owner cancels", two, "06-private-cancel");

            assertEquals(404, command(one, "POST", "/api/definition-draft", "{}", null).status());
            assertEquals(403, one.request().post(base + "/api/workspace/clear", RequestOptions.create()
                    .setHeader("Origin", base).setHeader("If-Match", staleVersion)).status());
            assertTrue(probe("healthcheck.railway.app", "/healthz", "").startsWith("HTTP/1.1 200"));
            assertFalse(probe("healthcheck.railway.app", "/healthz", "").toLowerCase().contains("set-cookie:"));
            assertTrue(probe("healthcheck.railway.app", "/api/workspace", "").startsWith("HTTP/1.1 403"));
            assertTrue(probe("evil.example", "/api/workspace", "").startsWith("HTTP/1.1 403"));
            assertTrue(probe("localhost:" + port, "/api/workspace", "X-Forwarded-Proto: https\r\n").startsWith("HTTP/1.1 403"));
            steps.add("PASS | UC-2 G2/G3/G6 | Raw TLS host/forwarded spoof checks; health has no session; authoring 404; missing CSRF 403");

            String exact = "{\n  \"unfinished\": true\n}\n";
            one.locator("#demo-json").fill(exact);
            one.onceDialog(dialog -> dialog.accept("visitor-copy")); one.locator("#demo-save").click();
            assertEquals(exact, one.evaluate("JSON.parse(localStorage.getItem('school-kernel.definition.v1.visitor-copy')).documentText"));
            if (!one.locator("#clear-workspace").isVisible()) one.locator("#utilities > summary").click();
            one.onceDialog(Dialog::accept); one.locator("#clear-workspace").click();
            awaitState(one, "EMPTY");
            assertEquals(exact, one.evaluate("JSON.parse(localStorage.getItem('school-kernel.definition.v1.visitor-copy')).documentText"));
            record("UC-2 4b: clearing the accepted timetable and repair returns to the definition panel while retaining browser versions", one, "06b-clear-retains-browser");
            one.reload(); one.locator("#demo-definition option[value=visitor-copy]").waitFor(new Locator.WaitForOptions().setState(com.microsoft.playwright.options.WaitForSelectorState.ATTACHED));
            one.locator("#demo-definition").selectOption("visitor-copy");
            assertEquals(exact, one.locator("#demo-json").inputValue());
            stop(); start(key, 2);
            one.reload(); one.locator("#demo-start-again").waitFor();
            assertTrue(one.locator("#state-card").innerText().contains("ended"));
            assertEquals(exact, one.evaluate("JSON.parse(localStorage.getItem('school-kernel.definition.v1.visitor-copy')).documentText"));
            record("UC-2 restart: old cookie cannot restore work; loss explanation and explicit start/import choices; browser text survives", one, "07-ended");
            one.locator("#demo-start-again").click();
            awaitState(one, "EMPTY");
            assertEquals("EMPTY", snapshot(one).path("state").stringValue());
            record("UC-2 explicit restart creates an empty workspace without replaying the failed mutation or accepting a proposal", one, "08-start-again");
            assertTrue(errors.isEmpty(), String.join("\n", errors));
            Files.writeString(output.resolve("ledger.md"), "# UC-2 local execution ledger\n\nDate: " + Instant.now()
                    + "\nApplication: packaged demo profile at " + base + "; real packaged kernel; Chrome; two isolated browser contexts.\n"
                    + "All imports, sessions, certificates and downloads are disposable. No PostgreSQL or operator school is used.\n\n"
                    + String.join("\n\n", steps) + "\n\nLocal journey: PASS. Public Railway boundary and resource/spend qualification remain unverified. "
                    + "This ledger does not approve UC-2 or claim deployment. Browser contexts close; both server processes stop; temporary files are removed by JUnit.\n");
            } catch (RuntimeException | Error failure) {
                try {
                    one.screenshot(new Page.ScreenshotOptions().setPath(output.resolve("failure.png")).setFullPage(true).setTimeout(3000));
                    Files.writeString(output.resolve("failure.txt"), one.locator("body").innerText());
                } catch (RuntimeException ignored) {}
                throw failure;
            }
        } finally {
            stop();
            if (!Files.exists(output.resolve("ledger.md"))) Files.writeString(output.resolve("failed-attempt.md"),
                    "# Incomplete local walkthrough\n\n" + String.join("\n\n", steps) + "\nSee the matching Failsafe report and retained server logs. Server stopped.\n");
        }
    }

    private void record(String step, Page page, String name) throws Exception {
        page.screenshot(new Page.ScreenshotOptions().setPath(output.resolve(name + ".png")).setFullPage(true));
        Files.writeString(output.resolve(name + ".txt"), page.locator("body").innerText());
        steps.add("PASS | " + step + " | [screenshot](" + name + ".png) / [snapshot](" + name + ".txt)");
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
        Path runtimeJar = temporary.resolve("workspace-runtime.jar");
        if (!Files.exists(runtimeJar)) Files.copy(Path.of("target/timetable-workspace-1.0.0-SNAPSHOT.jar"), runtimeJar);
        server = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "java").toString(), "-jar",
                runtimeJar.toAbsolutePath().toString(),
                "--spring.profiles.active=demo", "--server.address=127.0.0.1", "--server.port=" + port,
                "--demo.public-hosts=localhost:" + port, "--demo.trusted-proxy-addresses=",
                "--server.ssl.enabled=true", "--server.ssl.key-store=" + key,
                "--server.ssl.key-store-password=disposable-demo-test", "--server.ssl.key-store-type=PKCS12",
                "--workspace.kernel-executable=" + Path.of("../school-kernel").toAbsolutePath(),
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
