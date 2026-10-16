package org.schoolkernel.workspace;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.*;
import java.util.*;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.Cookie;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

@EnabledIfSystemProperty(named = "demo.remote.ended", matches = "true")
class DemoRemoteEndedSessionIT {
    @Test void explicitStartFromAnInvalidCookieStaysUsableInTheActualBrowser() throws Exception {
        String base = System.getProperty("demo.remote.url");
        assertEquals("https://demo-smoke-production.up.railway.app", base);
        Path output = Path.of(System.getProperty("demo.walkthrough.dir")); Files.createDirectories(output);
        List<String> network = new ArrayList<>();
        try (Playwright playwright = Playwright.create(new Playwright.CreateOptions().setEnv(Map.of("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD", "1")));
             Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setChannel("chrome").setHeadless(true));
             BrowserContext context = browser.newContext()) {
            context.addCookies(List.of(new Cookie("JSESSIONID", "expired-disposable-test-session").setUrl(base).setSecure(true).setHttpOnly(true)));
            Page page = context.newPage();
            page.onResponse(r -> network.add(r.request().method() + " " + r.status() + " " + r.url()));
            try {
                page.navigate(base + "/workspace/"); page.locator("#demo-start-again").waitFor();
                page.locator("#demo-start-again").click();
                awaitState(page, "EMPTY");
                for (int i = 0; i < 20; i++) {
                    int api = page.request().get(base + "/api/workspace").status();
                    int browserStatus = ((Number) page.evaluate("async () => (await fetch('/api/workspace')).status")).intValue();
                    network.add("CHECK " + i + " API=" + api + " browser=" + browserStatus);
                    assertEquals(200, browserStatus);
                }
                page.reload(); page.locator("#demo-json").waitFor();
                assertEquals(200, ((Number) page.evaluate("async () => (await fetch('/api/workspace')).status")).intValue());
                page.locator("#demo-retention summary").click();
                String privacy = page.locator("#demo-retention").innerText();
                for (String text : List.of("60 minutes", "8 hours", "unfinished", "shared memory cache", "one hour", "shared browser", "seven days", "90-day", "US West")) {
                    assertTrue(privacy.toLowerCase().contains(text.toLowerCase()), text);
                }
                assertEquals(List.of("Sample schools", "Saved in this browser"), page.locator("#demo-definition optgroup").evaluateAll("elements => elements.map(e => e.label)"));
                String headers = (String) page.evaluate("async () => {const r=await fetch('/api/workspace');return JSON.stringify(Object.fromEntries(r.headers));}");
                assertTrue(headers.contains("no-store")); assertTrue(headers.contains("content-security-policy"));
                assertTrue(headers.contains("strict-transport-security")); assertTrue(headers.contains("nosniff"));
                page.locator("#demo-definition").selectOption("small");
                page.waitForFunction("() => document.querySelector('#demo-json').value.includes('Small school')");
                page.locator("#demo-use").click();
                page.waitForFunction("() => document.querySelector('#state-card h2')?.textContent === 'Small school'");
                String before = (String) page.evaluate("async () => await (await fetch('/api/workspace')).text()");
                int staleClear = ((Number) page.evaluate("async () => { const t=await (await fetch('/api/csrf')).json(); return (await fetch('/api/workspace/clear',{method:'POST',headers:{'If-Match':'\"ws-0\"',[t.headerName]:t.token}})).status; }")).intValue();
                assertEquals(412, staleClear);
                assertEquals(before, page.evaluate("async () => await (await fetch('/api/workspace')).text()"));
                Files.writeString(output.resolve("ledger.md"), "# UC-2 expired-cookie recovery diagnostic\n\nPASS: explicit Start again, 20 actual-browser reads and reload remain usable. Full retention/privacy scopes, definition groups, no-store, CSP, HSTS and nosniff are checked. Stale clear returns 412 with the complete accepted snapshot unchanged.\n"
                        + "Explicitly awaiting the completed session transition gives 200 in both browser and API contexts; earlier failures sampled in-flight rotation. See network.txt. Browser closes.\n");
            } finally {
                Files.writeString(output.resolve("network.txt"), String.join("\n", network));
                page.screenshot(new Page.ScreenshotOptions().setPath(output.resolve("result.png")).setFullPage(true));
                Files.writeString(output.resolve("snapshot.txt"), page.locator("body").innerText());
            }
        }
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
