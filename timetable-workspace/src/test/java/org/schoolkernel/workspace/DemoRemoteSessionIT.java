package org.schoolkernel.workspace;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.RequestOptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/** Explicit opt-in only: restarts the isolated Free smoke service; normal public TLS validation. */
@org.junit.jupiter.api.condition.EnabledIfSystemProperty(named = "demo.remote.lifecycle", matches = "true")
class DemoRemoteSessionIT {
    private static final JsonMapper JSON = JsonMapper.builder().build();
    @TempDir Path temporary;
    private Path output;
    private String base;
    private final List<String> steps = new ArrayList<>();

    @Test void privateSamplesDraftsRunsExportsStaleTabsAndRestart() throws Exception {
        output = Path.of(System.getProperty("demo.walkthrough.dir",
                "../output/playwright/demo-deployment/UC-2/" + System.currentTimeMillis())).toAbsolutePath();
        Files.createDirectories(output);
        base = System.getProperty("demo.remote.url");
        assertEquals("https://demo-smoke-production.up.railway.app", base);
        assertNotNull(System.getProperty("demo.railway.cli"), "Explicit Railway CLI is required for operator restarts");
        List<String> errors = new ArrayList<>();
        List<String> network = new ArrayList<>();
        try (Playwright playwright = Playwright.create(new Playwright.CreateOptions()
                .setEnv(Map.of("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD", "1")));
             Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setChannel("chrome").setHeadless(true));
             BrowserContext first = browser.newContext(new Browser.NewContextOptions().setViewportSize(1280, 800));
             BrowserContext second = browser.newContext(new Browser.NewContextOptions().setViewportSize(1280, 800))) {
            Page one = first.newPage(); one.onPageError(errors::add);
            Page two = second.newPage(); two.onPageError(errors::add);
            for (Page page : List.of(one, two)) page.onResponse(response -> network.add(
                    response.request().method() + " " + response.status() + " " + response.url()));
            try {
            one.navigate(base + "/workspace/"); one.locator("#demo-sample-preview").filter(new Locator.FilterOptions().setHasText("520 lessons")).waitFor();
            assertEquals("full", one.locator("#demo-definition").inputValue());
            assertEquals("EMPTY", snapshot(one).path("state").stringValue());
            assertEquals(0, one.locator("#open-authoring").count());
            assertTrue(one.locator("#demo-retention").innerText().contains("export accepted timetables"));
            two.navigate(base + "/workspace/"); two.locator("#demo-json").waitFor();
            var cookie = first.cookies().stream().filter(value -> value.name.equals("JSESSIONID")).findFirst().orElseThrow();
            assertTrue(cookie.secure); assertTrue(cookie.httpOnly); assertEquals("STRICT", cookie.sameSite.name());
            record("UC-2 main/G2/G4: Full previews in EMPTY; no authoring entry; persistent retention/privacy; Secure/HttpOnly/Strict cookie", one, "01-empty");
            one.locator("#demo-use").click(); awaitSchool(one, "Full school");
            assertEquals("EMPTY", snapshot(two).path("state").stringValue());
            JsonNode full = snapshot(one).path("workspace").path("acceptedBaseline");
            assertEquals(520, full.path("result").path("timetable").path("assignments").size());
            record("UC-2 ownership / UC-3 enabling: Full accepted privately; other browser stays EMPTY", one, "02-full");
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
                two.waitForFunction("()=>document.querySelector('#demo-sample-preview').textContent.includes('Opened file')");
                two.onceDialog(dialog -> dialog.accept("private-visitor-definition")); two.locator("#demo-save").click();
                two.locator("#demo-use").click();
            two.locator("#start-plan").waitFor();
            assertEquals(custom, snapshot(two).path("workspace").path("initialDefinition"));
            assertFalse(snapshot(one).toString().contains("private-visitor-school"));
            two.locator("#start-plan").click();
            awaitState(two, "SOLVING_INITIAL");
            JsonNode running = snapshot(two); String runId = running.path("workspace").path("run").path("id").stringValue();
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
            assertEquals(403, one.request().get(base + "/api/workspace", RequestOptions.create()
                    .setHeader("Origin", "https://evil.example")).status());
            assertEquals(accepted, snapshot(one).path("workspace").path("acceptedBaseline"));
            var newcomer = playwright.request().newContext();
            try {
                APIResponse fullResponse = newcomer.get(base + "/api/workspace");
                assertEquals(503, fullResponse.status());
                assertEquals("DEMO_FULL", JSON.readTree(fullResponse.body()).path("code").stringValue());
            } finally { newcomer.dispose(); }
            try (BrowserContext third = browser.newContext()) {
                Page fullPage = third.newPage(); fullPage.navigate(base + "/workspace/");
                fullPage.locator("#state-card").filter(new Locator.FilterOptions().setHasText("Demo full")).waitFor();
                assertTrue(fullPage.locator("#state-card").innerText().contains("try again later"));
                record("UC-2 2a: a third browser sees Demo full and retry guidance without affecting either owner", fullPage, "06a-demo-full");
            }
            assertEquals(accepted, snapshot(one).path("workspace").path("acceptedBaseline"));
            steps.add("PASS | UC-2 2a/G2/G6 | Two-owner capacity refuses newcomer; foreign Origin and missing CSRF refused; authoring 404; Current unchanged");

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
            restart(1);
            one.reload(); one.locator("#demo-start-again").waitFor();
            assertTrue(one.locator("#state-card").innerText().contains("ended"));
            assertEquals(exact, one.evaluate("JSON.parse(localStorage.getItem('school-kernel.definition.v1.visitor-copy')).documentText"));
            record("UC-2 restart: old cookie cannot restore work; loss explanation and explicit start/import choices; browser text survives", one, "07-ended");
            one.locator("#demo-start-again").click();
            awaitState(one, "EMPTY");
            assertEquals("EMPTY", snapshot(one).path("state").stringValue());
            for (int check = 0; check < 10; check++) assertEquals("EMPTY", snapshot(one).path("state").stringValue());
            record("UC-2 explicit restart creates an empty workspace without replaying the failed mutation or accepting a proposal", one, "08-start-again");
            restart(2);
            one.reload(); one.locator("#demo-start-again").waitFor();
            two.reload(); two.locator("#demo-start-again").waitFor();
            assertEquals(exact, one.evaluate("JSON.parse(localStorage.getItem('school-kernel.definition.v1.visitor-copy')).documentText"));
            record("UC-2 repeated provider restart: both old owners are ended; browser versions retained; no silent recovery", one, "09-second-restart");
            assertTrue(errors.isEmpty(), String.join("\n", errors));
            Files.writeString(output.resolve("ledger.md"), "# UC-2 deployed session walkthrough\n\nDate: " + Instant.now()
                    + "\nApplication: deployed demo profile at " + base + "; real packaged kernel; Chrome; two isolated browser contexts.\n"
                    + "All imports, sessions and downloads are disposable. Public TLS uses normal certificate validation. No PostgreSQL or operator school is used.\n\n"
                    + String.join("\n\n", steps) + "\n\nPublic session journey: PASS, including two real provider restarts and repeated health/static probes. "
                    + "Idle/absolute expiry is recorded separately. This does not qualify UC-12 or later destination use cases. Browser contexts close; the second restart cleared disposable server workspaces; downloaded files are removed by JUnit.\n");
            } catch (RuntimeException | Error failure) {
                try {
                    one.screenshot(new Page.ScreenshotOptions().setPath(output.resolve("failure.png")).setFullPage(true).setTimeout(3000));
                    Files.writeString(output.resolve("failure.txt"), one.locator("body").innerText());
                } catch (RuntimeException ignored) {}
                throw failure;
            }
        } finally {
            Files.writeString(output.resolve("network.txt"), String.join("\n", network));
            Files.writeString(output.resolve("page-errors.txt"), String.join("\n", errors));
            if (!Files.exists(output.resolve("ledger.md"))) Files.writeString(output.resolve("failed-attempt.md"),
                    "# Incomplete deployed walkthrough\n\n" + String.join("\n\n", steps) + "\nSee the matching Failsafe report and retained server logs. Browser contexts close; retained isolated service is not stopped.\n");
        }
    }

    private void record(String step, Page page, String name) throws Exception {
        page.screenshot(new Page.ScreenshotOptions().setPath(output.resolve(name + ".png")).setFullPage(true));
        Files.writeString(output.resolve(name + ".txt"), page.locator("body").innerText());
        steps.add("PASS | " + step + " | [screenshot](" + name + ".png) / [snapshot](" + name + ".txt)");
    }
    private JsonNode snapshot(Page page) {
        // Read through the real browser actor after explicitly awaiting the session transition.
        String response = (String) page.evaluate("async () => { const r = await fetch('/api/workspace'); return JSON.stringify({status:r.status, body:await r.json()}); }");
        JsonNode observed = JSON.readTree(response);
        assertEquals(200, observed.path("status").intValue()); return observed.path("body");
    }
    private void awaitSchool(Page page, String title) {
        page.waitForFunction("title => document.querySelector('#state-card h2')?.textContent === title", title);
    }
    private APIResponse command(Page page, String method, String path, String body, String etag) {
        JsonNode token = JSON.readTree(page.request().get(base + "/api/csrf").body());
        if (etag == null) etag = "\"ws-" + snapshot(page).path("version").longValue() + "\"";
        var options = RequestOptions.create().setMethod(method).setHeader("Origin", base).setHeader("If-Match", etag)
                .setHeader(token.path("headerName").stringValue(), token.path("token").stringValue());
        if (body != null) options.setHeader("Content-Type", "application/json").setData(body);
        return page.request().fetch(base + path, options);
    }
    private void restart(int number) throws Exception {
        Process operation = new ProcessBuilder(System.getProperty("demo.railway.cli"), "restart", "--yes", "--json",
                "--project", "13d509af-1436-4024-bb64-f9f78fa439fc", "--environment", "a41a9c0e-ec3f-4ef4-9e08-996d998d1815",
                "--service", "672cabae-b54e-4965-bbb2-5236bec81c33")
                .redirectErrorStream(true).redirectOutput(output.resolve("restart-" + number + ".json").toFile()).start();
        assertTrue(operation.waitFor(45, TimeUnit.SECONDS)); assertEquals(0, operation.exitValue());
        // The provider command returns before readiness. An unchanged cookie must eventually be ended,
        // preventing a response from the outgoing instance from being mistaken for a completed restart.
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(150);
        var client = java.net.http.HttpClient.newHttpClient();
        int consecutive = 0;
        List<String> observations = new ArrayList<>();
        while (System.nanoTime() < deadline && consecutive < 40) {
            try {
                String path = consecutive % 2 == 0 ? "/healthz" : "/workspace/app.js";
                var response = client.send(java.net.http.HttpRequest.newBuilder(java.net.URI.create(base + path))
                        .timeout(java.time.Duration.ofSeconds(10)).GET().build(), java.net.http.HttpResponse.BodyHandlers.ofString());
                observations.add(response.statusCode() + " " + path);
                consecutive = response.statusCode() == 200 ? consecutive + 1 : 0;
            } catch (java.io.IOException unavailable) { consecutive = 0; observations.add("temporarily unavailable during restart"); }
            Thread.sleep(250);
        }
        Files.writeString(output.resolve("restart-" + number + "-probes.txt"), String.join("\n", observations));
        assertEquals(40, consecutive, "Restart must deliver 40 consecutive health/static responses");
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
