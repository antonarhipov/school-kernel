package org.schoolkernel.workspace;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.RequestOptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Explicitly targeted deployed smoke only. No local service, solver, operator data or account credentials. */
@EnabledIfSystemProperty(named = "demo.remote.url", matches = "https://[a-z0-9.-]+\\.up\\.railway\\.app")
class DemoRemoteSmokeIT {
    private static final JsonMapper JSON = JsonMapper.builder().build();

    @Test void deployedSamplesKeepTwoOwnersPrivate() throws Exception {
        String base = System.getProperty("demo.remote.url");
        Path output = Path.of(System.getProperty("demo.walkthrough.dir",
                "../output/playwright/demo-deployment/free-smoke/" + System.currentTimeMillis())).toAbsolutePath();
        Files.createDirectories(output);
        List<String> steps = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        List<String> consoleErrors = new ArrayList<>();
        List<String> network = new ArrayList<>();
        try (Playwright playwright = Playwright.create(new Playwright.CreateOptions()
                .setEnv(Map.of("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD", "1")));
             Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setChannel("chrome").setHeadless(true));
             BrowserContext first = browser.newContext(new Browser.NewContextOptions().setViewportSize(1280, 800));
             BrowserContext second = browser.newContext(new Browser.NewContextOptions().setViewportSize(1280, 800))) {
            Page one = first.newPage(); one.onPageError(errors::add);
            Page two = second.newPage(); two.onPageError(errors::add);
            for (Page page : List.of(one, two)) {
                page.onConsoleMessage(message -> {
                    if (message.type().equals("error")) consoleErrors.add(message.text());
                });
                page.onResponse(response -> network.add(response.status() + " "
                        + response.headers().getOrDefault("content-type", "") + " " + response.url()));
                page.onRequestFailed(request -> network.add("FAILED " + request.url() + " " + request.failure()));
            }
            try {
                one.navigate(base + "/workspace/");
                one.locator("#demo-sample-preview").filter(new Locator.FilterOptions().setHasText("544 lessons")).waitFor();
                two.navigate(base + "/workspace/"); two.locator("#demo-json").waitFor();
                assertEquals("full", one.locator("#demo-definition").inputValue());
                assertEquals("EMPTY", snapshot(one, base).path("state").stringValue());
                assertEquals("EMPTY", snapshot(two, base).path("state").stringValue());
                var cookie = first.cookies().stream().filter(c -> c.name.equals("JSESSIONID")).findFirst().orElseThrow();
                assertTrue(cookie.secure); assertTrue(cookie.httpOnly); assertEquals("STRICT", cookie.sameSite.name());
                assertTrue(one.request().get(base + "/api/workspace").headers().get("strict-transport-security").contains("max-age="));
                record(output, steps, one, "01-empty", "HTTPS with a valid public certificate; two EMPTY owners; Full preview; Secure/HttpOnly/Strict session cookie and HSTS");

                for (String id : List.of("full", "two-class", "small")) {
                    String title = id.equals("full") ? "Full school" : id.equals("small") ? "Small school" : "Two-class school";
                    one.locator("#demo-definition").selectOption(id);
                    one.waitForFunction("title => document.querySelector('#demo-json').value.includes(title)", title);
                    if (!id.equals("full")) one.onceDialog(Dialog::accept);
                    one.locator("#demo-use").click();
                    one.waitForFunction("title => document.querySelector('#state-card h2')?.textContent === title", title);
                    JsonNode actual = snapshot(one, base).path("workspace").path("acceptedBaseline");
                    Path samples = Path.of("src/test/resources/demo/legacy-samples");
                    assertEquals(JSON.readTree(samples.resolve(id + "-definition.json")), actual.path("definition"));
                    assertEquals(JSON.readTree(samples.resolve(id + "-result.json")), actual.path("result"));
                    assertEquals("EMPTY", snapshot(two, base).path("state").stringValue());
                }
                record(output, steps, one, "02-samples", "All three samples open exact packaged definitions/results privately; second owner remains EMPTY");

                JsonNode accepted = snapshot(one, base);
                APIResponse refused = one.request().post(base + "/api/workspace/clear", RequestOptions.create().setHeader("Origin", base));
                assertEquals(403, refused.status());
                assertEquals(accepted, snapshot(one, base));
                refused = one.request().get(base + "/api/workspace", RequestOptions.create().setHeader("Origin", "https://evil.example"));
                assertEquals(403, refused.status());
                assertEquals(accepted, snapshot(one, base));
                APIRequestContext newcomer = playwright.request().newContext();
                try {
                    APIResponse full = newcomer.get(base + "/api/workspace");
                    assertEquals(503, full.status());
                    assertEquals("DEMO_FULL", JSON.readTree(full.body()).path("code").stringValue());
                } finally { newcomer.dispose(); }
                assertEquals(accepted, snapshot(one, base));
                assertEquals("EMPTY", snapshot(two, base).path("state").stringValue());
                steps.add("PASS | Missing CSRF and foreign Origin refused without mutation; two-workspace cap refuses newcomer while both owners remain usable");

                two.locator("#demo-use").click();
                two.waitForFunction("() => document.querySelector('#state-card h2')?.textContent === 'Full school'");
                assertEquals(accepted, snapshot(one, base));
                Page sameOwner = first.newPage(); sameOwner.navigate(base + "/workspace/"); sameOwner.locator("#demo-json").waitFor();
                assertEquals(accepted, snapshot(sameOwner, base));
                record(output, steps, two, "03-second-owner", "Second owner explicitly opens Full; first owner's Small stays exact; another tab shares its owner's workspace");

                one.setViewportSize(390, 844); one.locator(".narrow-banner").waitFor();
                assertEquals(true, one.evaluate("document.documentElement.scrollWidth <= innerWidth"));
                assertEquals(true, one.locator("#demo-json").evaluate("element => element.readOnly"));
                record(output, steps, one, "04-narrow", "390px inspection fits viewport; JSON is read-only at narrow width");
                assertTrue(errors.isEmpty(), String.join("\n", errors));
                Files.writeString(output.resolve("page-errors.txt"), String.join("\n", errors));
                Files.writeString(output.resolve("console-errors.txt"), String.join("\n", consoleErrors));
                Files.writeString(output.resolve("network.txt"), String.join("\n", network));
                Files.writeString(output.resolve("ledger.md"), "# Deployed Free-plan browser smoke\n\nDate: " + Instant.now()
                        + "\nApplication: " + base + "; Chrome with normal TLS validation; two disposable browser contexts.\n\n"
                        + String.join("\n\n", steps) + "\n\nSmoke PASS only. No solver, capacity, full UC-2 lifecycle or public launch qualification. "
                        + "Browser contexts close. Server workspaces remain until expiry or explicit operator restart.\n");
            } catch (RuntimeException | Error failure) {
                Files.writeString(output.resolve("page-errors.txt"), String.join("\n", errors));
                Files.writeString(output.resolve("console-errors.txt"), String.join("\n", consoleErrors));
                Files.writeString(output.resolve("network.txt"), String.join("\n", network));
                try {
                    one.screenshot(new Page.ScreenshotOptions().setPath(output.resolve("failure.png")).setFullPage(true).setTimeout(3000));
                    Files.writeString(output.resolve("failure.txt"), one.locator("body").innerText());
                } catch (RuntimeException ignored) { }
                Files.writeString(output.resolve("failed-attempt.md"), "# Incomplete deployed smoke\n\n" + String.join("\n\n", steps)
                        + "\n\n" + failure.getClass().getName() + "; see Failsafe report. Browser contexts close.\n");
                throw failure;
            }
        }
    }

    private static JsonNode snapshot(Page page, String base) {
        APIResponse response = page.request().get(base + "/api/workspace");
        assertEquals(200, response.status());
        return JSON.readTree(response.body());
    }

    private static void record(Path output, List<String> steps, Page page, String name, String description) throws Exception {
        page.screenshot(new Page.ScreenshotOptions().setPath(output.resolve(name + ".png")).setFullPage(true));
        Files.writeString(output.resolve(name + ".txt"), page.locator("body").innerText());
        steps.add("PASS | " + description + " | [screenshot](" + name + ".png) / [snapshot](" + name + ".txt)");
    }
}
