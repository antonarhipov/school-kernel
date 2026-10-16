package org.schoolkernel.workspace;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.microsoft.playwright.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/** Explicit deployed fault-clock qualification: configure 15s idle / 75s absolute on the isolated smoke only. */
@EnabledIfSystemProperty(named = "demo.remote.expiry", matches = "true")
class DemoRemoteExpiryIT {
    private static final JsonMapper JSON = JsonMapper.builder().build();
    @TempDir Path temporary;

    @Test void publicIdleExpiryAndAbsoluteExpiryDespiteRunPolling() throws Exception {
        String base = System.getProperty("demo.remote.url");
        assertEquals("https://demo-smoke-production.up.railway.app", base);
        Path output = Path.of(System.getProperty("demo.walkthrough.dir")).toAbsolutePath();
        Files.createDirectories(output);
        List<String> steps = new ArrayList<>();
        List<String> polls = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        try (Playwright playwright = Playwright.create(new Playwright.CreateOptions().setEnv(Map.of("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD", "1")));
             Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setChannel("chrome").setHeadless(true));
             BrowserContext active = browser.newContext(); BrowserContext idle = browser.newContext()) {
            Page one = active.newPage(); Page two = idle.newPage();
            one.onPageError(errors::add); two.onPageError(errors::add);
            try {
                one.navigate(base + "/workspace/"); one.locator("#demo-sample-preview").filter(new Locator.FilterOptions().setHasText("520 lessons")).waitFor();
                String exact = "{\n  \"unfinished-expiry-copy\": true\n}\n";
                one.locator("#demo-json").fill(exact);
                one.onceDialog(dialog -> dialog.accept("expiry-copy")); one.locator("#demo-save").click();
                ObjectNode definition = (ObjectNode) JSON.readTree(Path.of("src/test/resources/demo/legacy-samples/small-definition.json"));
                definition.put("schoolId", "expiry-private-school").put("displayName", "Expiry private school");
                Path file = temporary.resolve("private-definition.json"); Files.write(file, JSON.writeValueAsBytes(definition));
                one.locator("#demo-file").setInputFiles(file);
                one.waitForFunction("()=>document.querySelector('#demo-sample-preview').textContent.includes('Opened file')");
                one.onceDialog(dialog -> dialog.accept("expiry-private-definition")); one.locator("#demo-save").click();
                one.locator("#demo-use").click();
                one.locator("#start-plan").waitFor(); one.locator("#start-plan").click();
                awaitState(one, "SOLVING_INITIAL");
                JsonNode solving = snapshot(one, base);
                String run = solving.path("workspace").path("run").path("id").stringValue();
                assertEquals(definition, solving.path("workspace").path("initialDefinition"));
                record(output, steps, one, "01-active-run", "UC-2 G5: real private kernel run with browser polling; exact browser version saved");

                two.navigate(base + "/workspace/"); two.locator("#demo-sample-preview").filter(new Locator.FilterOptions().setHasText("520 lessons")).waitFor();
                two.locator("#demo-definition").selectOption("small");
                two.waitForFunction("() => document.querySelector('#demo-json').value.includes('Small school')");
                two.locator("#demo-use").click();
                two.waitForFunction("() => document.querySelector('#state-card h2')?.textContent === 'Small school'");
                two.locator("#open-repair-setup").click();
                two.locator("#start-repair-form input[name=period]").first().check();
                two.locator("#start-repair-form button[type=submit]").click(); two.locator("#solve-draft").waitFor();
                assertEquals("REPAIR_DRAFT", snapshot(two, base).path("state").stringValue());
                assertFalse(snapshot(two, base).toString().contains("expiry-private-school"));
                record(output, steps, two, "02-idle-draft", "UC-2 idle owner holds a private unfinished repair; active owner's input remains private");
                long idleStart = System.nanoTime();
                boolean idleEnded = false;
                boolean activeEnded = false;
                long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(100);
                while (System.nanoTime() < deadline && !activeEnded) {
                    APIResponse polled = one.request().get(base + "/api/runs/" + run);
                    polls.add(Instant.now() + " private-run " + polled.status() + " " + JSON.readTree(polled.body()).path("status").asString(""));
                    if (polled.status() == 410) {
                        assertEquals("SESSION_ENDED", JSON.readTree(polled.body()).path("code").stringValue());
                        activeEnded = true;
                    } else {
                        assertEquals(200, polled.status());
                        assertFalse(JSON.readTree(polled.body()).has("acceptedBaseline"));
                    }
                    if (!idleEnded && System.nanoTime() - idleStart > java.util.concurrent.TimeUnit.SECONDS.toNanos(18)) {
                        APIResponse expired = two.request().get(base + "/api/workspace");
                        assertEquals(410, expired.status());
                        assertEquals("SESSION_ENDED", JSON.readTree(expired.body()).path("code").stringValue());
                        assertFalse(new String(expired.body(), java.nio.charset.StandardCharsets.UTF_8).contains("acceptedBaseline"));
                        idleEnded = true;
                        assertFalse(activeEnded, "Polling must retain the first session beyond the idle threshold");
                        two.reload(); two.locator("#demo-start-again").waitFor();
                        record(output, steps, two, "03-idle-ended", "UC-2 2b/G5: 15-second idle expiry loses unfinished repair while the polling owner remains active");
                    }
                    if (!activeEnded) Thread.sleep(1000);
                }
                assertTrue(idleEnded); assertTrue(activeEnded, "Run polling must never extend the 75-second absolute expiry");
                one.reload(); one.locator("#demo-start-again").waitFor();
                assertEquals(exact, one.evaluate("JSON.parse(localStorage.getItem('school-kernel.definition.v1.expiry-copy')).documentText"));
                record(output, steps, one, "04-absolute-ended", "UC-2 G5: private run/result is lost at absolute expiry despite polling; no proposal or Current recovered; browser version remains exact");
                one.locator("#demo-start-again").click();
                awaitState(one, "EMPTY");
                JsonNode fresh = snapshot(one, base);
                assertEquals("EMPTY", fresh.path("state").stringValue());
                assertFalse(fresh.toString().contains("expiry-private-school"));
                assertEquals(404, ((Number) one.evaluate("async id => (await fetch('/api/runs/' + id)).status", run)).intValue());
                assertEquals(404, ((Number) one.evaluate("async id => (await fetch('/api/runs/' + id)).status", UUID.randomUUID().toString())).intValue());
                assertEquals(fresh, snapshot(one, base));
                record(output, steps, one, "05-fresh-empty", "UC-2 2b: explicit Start again creates EMPTY without replay, recovery or acceptance; expired run is indistinguishable from a random ID");
                assertTrue(errors.isEmpty(), String.join("\n", errors));
                Files.writeString(output.resolve("ledger.md"), "# UC-2 deployed expiry walkthrough\n\n" + Instant.now()
                        + "\nApplication: " + base + "; real packaged kernel, Chrome with normal TLS validation, two isolated disposable contexts.\n"
                        + "Test-only effective clock configuration: 15-second idle / 75-second absolute; defaults restored separately by operator.\n\n"
                        + String.join("\n\n", steps) + "\n\nPASS. Browser contexts close, temporary input removed by JUnit. "
                        + "Disposable fresh EMPTY workspace remains until expiry/operator reset. No accepted work was recovered or silently accepted.\n");
            } catch (RuntimeException | Error failure) {
                try {
                    one.screenshot(new Page.ScreenshotOptions().setPath(output.resolve("failure.png")).setFullPage(true).setTimeout(3000));
                    Files.writeString(output.resolve("failure.txt"), one.locator("body").innerText());
                    two.screenshot(new Page.ScreenshotOptions().setPath(output.resolve("second-owner-failure.png")).setFullPage(true).setTimeout(3000));
                    Files.writeString(output.resolve("second-owner-failure.txt"), two.locator("body").innerText());
                } catch (RuntimeException ignored) { }
                Files.writeString(output.resolve("failed-attempt.md"), "# Failed expiry walkthrough\n\n" + String.join("\n\n", steps) + "\n" + failure.getClass().getName());
                throw failure;
            } finally {
                Files.writeString(output.resolve("polls.txt"), String.join("\n", polls));
                Files.writeString(output.resolve("page-errors.txt"), String.join("\n", errors));
            }
        }
    }
    private static JsonNode snapshot(Page page, String base) {
        JsonNode observed = JSON.readTree((String) page.evaluate("async () => { const r = await fetch('/api/workspace'); return JSON.stringify({status:r.status, body:await r.json()}); }"));
        assertEquals(200, observed.path("status").intValue()); return observed.path("body");
    }
    private static void record(Path output, List<String> steps, Page page, String name, String description) throws Exception {
        page.screenshot(new Page.ScreenshotOptions().setPath(output.resolve(name + ".png")).setFullPage(true));
        Files.writeString(output.resolve(name + ".txt"), page.locator("body").innerText());
        steps.add("PASS | " + description + " | [screenshot](" + name + ".png) / [snapshot](" + name + ".txt)");
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
