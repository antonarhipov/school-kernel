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

class DemoSamplesBrowserIT {
    private static final JsonMapper JSON = JsonMapper.builder().build();
    @TempDir Path temporary;
    private Process server;
    private int port;
    private Path output;
    private String base;
    private final List<String> steps = new ArrayList<>();
    @Test void completeSampleJourney() throws Exception {
        output = Path.of(System.getProperty("demo.sample.walkthrough.dir", System.getProperty("demo.walkthrough.dir", "../output/playwright/demo-deployment/UC-3/" + System.currentTimeMillis()))).toAbsolutePath();
        Files.createDirectories(output);
        boolean remote = Boolean.getBoolean("demo.remote.samples");
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
             BrowserContext first = browser.newContext(new Browser.NewContextOptions().setIgnoreHTTPSErrors(!remote).setViewportSize(1440, 900));
             BrowserContext second = browser.newContext(new Browser.NewContextOptions().setIgnoreHTTPSErrors(!remote).setViewportSize(1440, 900))) {
            Page one = first.newPage(), two = second.newPage();
            for (Page page : List.of(one, two)) { page.onPageError(errors::add); page.onRequest(request -> network.add(request.method() + " " + request.url())); }
            try {
                open(one); open(two);
                JsonNode empty = snapshot(one);
                assertEquals("EMPTY", empty.path("state").asString());
                assertEquals("full", one.locator("#demo-definition").inputValue());
                assertTrue(one.locator("#demo-sample-preview").innerText().contains("Thorough search (60 seconds)"));
                APIResponse catalog = one.request().get(base + "/api/samples");
                assertTrue(catalog.headers().get("cache-control").contains("public"));
                assertEquals(304, one.request().get(base + "/api/samples", RequestOptions.create().setHeader("If-None-Match", catalog.headers().get("etag"))).status());
                assertTrue(one.request().get(base + "/api/workspace").headers().get("cache-control").contains("no-store"));
                record("main 1/G4/G6: Full preview in EMPTY, exact catalog/default effort, public ETag 304 and visitor no-store", one, "01-preview-full");
                Map<String, JsonNode> previews = new LinkedHashMap<>();
                Map<String, String> tags = new LinkedHashMap<>();
                for (String id : List.of("small", "two-class", "full")) {
                    APIResponse response = one.request().get(base + "/api/samples/" + id);
                    previews.put(id, JSON.readTree(response.body())); tags.put(id, response.headers().get("etag"));
                    select(one, id);
                    assertEquals(previews.get(id).path("definition"), JSON.readTree(one.locator("#demo-json").inputValue()));
                    assertEquals(empty, snapshot(one));
                }
                String exact = one.locator("#demo-json").inputValue() + " ";
                one.locator("#demo-json").press("ControlOrMeta+End"); one.locator("#demo-json").press("Space");
                assertTrue(one.locator("#demo-use").isDisabled());
                assertTrue(one.locator("#demo-use-hint").innerText().contains("Save or revert"));
                one.onceDialog(Dialog::dismiss); one.locator("#demo-definition").selectOption("small");
                assertEquals("full", one.locator("#demo-definition").inputValue()); assertEquals(exact, one.locator("#demo-json").inputValue());
                record("1a/2a: unsaved text disables use; declined selection retains exact text, selection and EMPTY", one, "02-unsaved-decline");
                one.onceDialog(Dialog::accept); one.locator("#demo-revert").click();
                // Delay one real response, then select another sample. Only the newest request may install a preview.
                one.route("**/api/samples/small", route -> { one.waitForTimeout(350); route.resume(); });
                one.locator("#demo-definition").selectOption("small"); one.locator("#demo-definition").selectOption("two-class");
                one.waitForFunction("() => !document.querySelector('#demo-use').disabled && document.querySelector('#demo-json').value.includes('Two-class school')");
                one.waitForTimeout(500); assertEquals("two-class", one.locator("#demo-definition").inputValue());
                assertEquals(previews.get("two-class").path("definition"), JSON.readTree(one.locator("#demo-json").inputValue()));
                one.unroute("**/api/samples/small");
                record("G4: delayed older preview cannot replace the latest selection/text or Current", one, "03-preview-race");
                if (!remote) Files.writeString(temporary.resolve("deny-kernel"), "Visitor processes unavailable after startup");
                for (String id : List.of("small", "two-class", "full")) {
                    select(one, id); use(one, snapshot(one).path("state").asString().equals("EMPTY") ? null : true);
                    assertPair(one, id); assertEquals("EMPTY", snapshot(two).path("state").asString());
                    assertTrue(one.locator("#demo-in-use").innerText().contains("In use as Current"));
                    record("main 2-5/G1: explicit " + id + " opens the exact complete accepted pair, no solve, other owner EMPTY", one, "04-open-" + id);
                }
                one.reload(); openReady(one); assertTrue(one.locator("#demo-in-use").innerText().contains("Full school"));
                select(two, "small"); use(two, null); JsonNode other = snapshot(two);
                assertTrue(one.locator("#represented-lesson-count").innerText().contains("520"));
                assertEquals(520, one.locator(".lesson-cell").count());
                one.locator(".lesson-cell").first().click();
                assertTrue(one.locator("#lesson-panel-title").isVisible());
                String detail = one.locator("#workbench-inspector").innerText(); assertTrue(detail.contains("Teacher")); assertTrue(detail.contains("Room"));
                record("G5: Full whole-school Week renders 520 lessons with fictional names and faithful lesson details", one, "05-full-week-details");
                one.locator("[data-range=DAY]").click();
                one.locator("#filters > summary").click();
                one.locator("#teacher-filter").selectOption("demo-teacher-001");
                assertTrue(one.locator("#active-criteria").innerText().contains("John Doe"));
                record("G5: Full Day and teacher lens retain names, availability and context", one, "06-full-day-teacher");
                one.locator("#room-filter").selectOption("demo-room-001");
                assertEquals("", one.locator("#teacher-filter").inputValue());
                record("G5: room lens replaces teacher lens and remains private read-only inspection", one, "07-full-room-lens");
                one.locator("#reset-view").click();
                one.setViewportSize(390, 844); one.locator(".narrow-banner").waitFor();
                assertEquals(true, one.evaluate("document.documentElement.scrollWidth <= innerWidth"));
                assertEquals(true, one.locator("#demo-json").evaluate("element => element.readOnly"));
                assertEquals(0, one.locator("#start-manual-draft").count());
                record("G5: 390px narrow Day inspection is read-only with no page overflow", one, "08-narrow");
                one.setViewportSize(1440, 900); one.locator("#open-repair-setup").waitFor();
                Page stale = first.newPage(); stale.onPageError(errors::add); open(stale);
                one.locator("#open-repair-setup").click();
                one.locator("#start-repair-form input[name=period]").first().check();
                one.locator("#start-repair-form button[type=submit]").click(); one.locator("#solve-draft").waitFor();
                JsonNode repaired = snapshot(one); assertEquals("REPAIR_DRAFT", repaired.path("state").asString());
                assertPair(one, "full"); assertEquals(other, snapshot(two));
                record("main 5/G4/G5: Full repair entry stages private intent; complete Current and second visitor stay exact", one, "09-private-repair");
                exportExact(one, repaired.path("workspace").path("acceptedBaseline"));
                select(one, "two-class");
                one.onceDialog(dialog -> { assertTrue(dialog.message().contains("Full school")); assertTrue(dialog.message().contains("repair draft")); assertTrue(dialog.message().contains("Export Current first")); dialog.dismiss(); });
                one.locator("#demo-use").click(); assertEquals(repaired, snapshot(one));
                record("main 3/3a: replacement names Full Current and repair draft, offers export, decline preserves everything", one, "10-replacement-decline");
                APIResponse unknown = command(one, "POST", "/api/samples/missing/use", "{\"confirmed\":true}", null);
                assertEquals(404, unknown.status()); assertEquals("SAMPLE_NOT_FOUND", JSON.readTree(unknown.body()).path("code").asString());
                assertEquals(428, command(one, "POST", "/api/samples/small/use", "{}", null).status()); assertEquals(repaired, snapshot(one));
                select(stale, "small"); stale.onceDialog(Dialog::accept); stale.locator("#demo-use").click();
                stale.locator("#demo-reload").waitFor(); assertEquals(repaired, snapshot(one));
                assertTrue(stale.locator("#demo-feedback").innerText().contains("workspace changed"));
                stale.locator("#demo-reload").click();
                stale.waitForFunction("() => document.querySelector('#demo-feedback').textContent.includes('reloaded')");
                assertEquals("small", stale.locator("#demo-definition").inputValue()); assertEquals(repaired, snapshot(stale));
                record("4a/4b: unknown 404, missing confirmation 428, stale browser 412; reload preserves selection and newer complete work", stale, "11-stale-reload");
                one.route("**/api/samples/two-class/use", route -> route.fulfill(new Route.FulfillOptions().setStatus(503).setContentType("application/problem+json").setBody("{\"message\":\"Sample service unavailable\"}")));
                one.onceDialog(Dialog::accept); one.locator("#demo-use").click(); one.locator("#demo-reload").waitFor();
                assertEquals(repaired, snapshot(one)); assertEquals("two-class", one.locator("#demo-definition").inputValue());
                assertTrue(one.locator("#demo-feedback").innerText().contains("Sample service unavailable"));
                record("minimal guarantee: simulated HTTP failure preserves complete server work and selected JSON; reload/retry offered", one, "12-failure-preserves");
                one.unroute("**/api/samples/two-class/use"); stale.close();
                use(one, true); assertPair(one, "two-class");
                // Restore Full and inspect the manual entry without publishing or solving.
                select(one, "full"); use(one, true);
                one.locator(".lesson-cell").first().click();
                String room = one.locator("#edit-room").inputValue();
                one.locator("#edit-room").selectOption(room.equals("demo-room-001") ? "demo-room-002" : "demo-room-001");
                one.locator("#discard-manual-draft").waitFor();
                JsonNode manual = snapshot(one); assertEquals("MANUAL_DRAFT", manual.path("state").asString()); assertPair(one, "full");
                select(one, "small"); one.onceDialog(dialog -> { assertTrue(dialog.message().contains("manual draft")); dialog.dismiss(); });
                one.locator("#demo-use").click(); assertEquals(manual, snapshot(one));
                record("main 5/G4: Full manual entry opens a private draft, cancelled replacement preserves Current/draft", one, "13-manual-entry");
                assertEquals(other, snapshot(two));
                for (String id : previews.keySet()) {
                    APIResponse response = two.request().get(base + "/api/samples/" + id);
                    assertEquals(tags.get(id), response.headers().get("etag")); assertEquals(previews.get(id), JSON.readTree(response.body()));
                }
                assertTrue(network.stream().noneMatch(line -> line.contains("/api/plan") || line.contains("/api/repair-plan") || line.contains("/api/proposal/accept")));
                if (!remote) assertEquals(List.of("verify", "verify", "verify"), Files.readAllLines(temporary.resolve("kernel-launches")));
                assertTrue(errors.isEmpty(), errors.toString());
                record("G1/G4/G6/G7: source ETags/documents and second owner unchanged; no solve/accept request, no visitor kernel launches", two, "14-second-owner-preserved");
                Files.writeString(output.resolve("verdict.txt"), "PASS\n");
            } catch (Throwable failure) {
                Files.writeString(output.resolve("verdict.txt"), "FAIL: " + failure + "\n");
                Files.writeString(output.resolve("failure-properties.json"), JSON.writeValueAsString(one.evaluate("() => ({width:innerWidth, disabled:document.querySelector('#demo-json')?.disabled, readOnly:document.querySelector('#demo-json')?.readOnly, status:document.querySelector('#demo-feedback')?.textContent})")));
                record("FAIL: " + failure.toString().split("\n")[0], one, "failure"); throw failure;
            } finally {
                Files.write(output.resolve("network.txt"), network); Files.write(output.resolve("browser-errors.txt"), errors);
                Files.writeString(output.resolve("ledger.md"), "# UC-3 sample walkthrough\n\nDate: " + Instant.now() + "\nApplication: " + base
                    + "\nBrowser: Chrome, Playwright; two isolated disposable contexts and one stale shared-owner tab.\n"
                    + "No operator data or database used. Public TLS uses normal certificate validation. Local TLS is disposable.\n\n"
                    + String.join("\n\n", steps) + "\n\nOverall: " + Files.readString(output.resolve("verdict.txt"))
                    + "\nFailure injection is explicitly a browser-simulated 503; service save failure and readiness faults have separate technical evidence."
                    + "\nContexts closed. Local server stopped; public disposable workspaces remain until expiry/restart, with no shared browser identity retained.\n");
            }
        } finally { if (!remote) stop(); }
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
        System.out.println("UC3 STEP " + name + ": " + step.split("\n")[0]);
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
        Path launcher = temporary.resolve("kernel");
        String command = "#!/bin/sh\nprintf '%s\\n' \"$1\" >> '" + temporary.resolve("kernel-launches") + "'\n"
                + "test ! -f '" + temporary.resolve("deny-kernel") + "' || exit 4\nexec '"
                + Path.of("../school-kernel").toAbsolutePath() + "' \"$@\"\n";
        Files.writeString(launcher, command); launcher.toFile().setExecutable(true, true);
        server = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "java").toString(), "-jar",
                Path.of("target/timetable-workspace-1.0.0-SNAPSHOT.jar").toAbsolutePath().toString(),
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
