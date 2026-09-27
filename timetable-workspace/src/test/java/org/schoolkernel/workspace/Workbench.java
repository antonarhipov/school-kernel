package org.schoolkernel.workspace;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.Request;
import com.microsoft.playwright.Route;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Page object for the timetable workbench. Interactions go through real Playwright input (clicks, key presses,
 * option selection), so actionability matters: a control inside a closed disclosure is revealed by clicking its
 * summary, exactly as an administrator would.
 */
final class Workbench {
    static final Duration UI = Duration.ofSeconds(10);
    static final Duration SOLVE = Duration.ofSeconds(80);
    private static final ObjectMapper JSON = JsonMapper.builder().build();
    private static final Path SCREENSHOTS = Path.of("target/workbench-layout");

    private final Page page;
    private final int defaultPort;

    Workbench(Page page, int defaultPort) {
        this.page = page;
        this.defaultPort = defaultPort;
    }

    Page page() {
        return page;
    }

    // Navigation

    Workbench open() {
        return open(defaultPort);
    }

    Workbench open(int port) {
        page.navigate("http://localhost:" + port + "/workspace/");
        return this;
    }

    void navigate(String url) {
        page.navigate(url);
    }

    void reload() {
        page.reload();
    }

    void viewport(int width, int height) {
        page.setViewportSize(width, height);
    }

    // Reading and waiting

    /** Waits until the rendered text contains {@code expected} and returns the matching snapshot of the text. */
    String awaitText(String expected) {
        return awaitText(expected, UI);
    }

    String awaitText(String expected, Duration timeout) {
        try {
            return (String) page.waitForFunction("""
                            expected => { const text = document.body?.innerText || '';
                              return text.includes(expected) ? text : false; }""", expected,
                    new Page.WaitForFunctionOptions().setTimeout(timeout.toMillis()).setPollingInterval(50))
                    .jsonValue();
        } catch (PlaywrightException timeout_) {
            throw new AssertionError("Browser did not render: " + expected + "\nRendered: " + text(), timeout_);
        }
    }

    String text() {
        return (String) page.evaluate("document.body?.innerText || ''");
    }

    /** Waits until the browser expression becomes truthy, failing with {@code message} otherwise. */
    void expect(String expression, String message) {
        try {
            page.waitForFunction("() => Boolean(" + expression + ")", null,
                    new Page.WaitForFunctionOptions().setTimeout(5_000).setPollingInterval(50));
        } catch (PlaywrightException failure) {
            throw new AssertionError(message + "\nExpression: " + expression, failure);
        }
    }

    void expect(String expression) {
        expect(expression, "browser condition did not hold");
    }

    void expectNot(String expression, String message) {
        expect("!(" + expression + ")", message);
    }

    void expectNot(String expression) {
        expectNot(expression, "browser condition unexpectedly held");
    }

    /** One-shot evaluation, for values the test compares with independently computed expectations. */
    JsonNode value(String expression) {
        return JSON.valueToTree(page.evaluate(expression));
    }

    String string(String expression) {
        return (String) page.evaluate(expression);
    }

    Set<String> renderedLessonIds() {
        Set<String> ids = new HashSet<>();
        value("[...document.querySelectorAll('#accepted-view [data-lesson-id]')].map(button => button.dataset.lessonId)")
                .forEach(id -> ids.add(id.stringValue()));
        return ids;
    }

    // Generic input

    void click(String selector) {
        revealed(selector).click();
    }

    void press(String selector, String key) {
        revealed(selector).press(key);
    }

    void select(String selector, String value) {
        revealed(selector).selectOption(value);
    }

    void fill(String selector, String text) {
        revealed(selector).fill(text);
    }

    void type(String selector, String text) {
        revealed(selector).pressSequentially(text);
    }

    void check(String selector) {
        revealed(selector).check();
    }

    void setChecked(String selector, boolean checked) {
        revealed(selector).setChecked(checked);
    }

    void setFiles(String selector, Path file) {
        page.locator(selector).setInputFiles(file);
    }

    void openDisclosure(String detailsSelector) {
        Locator details = page.locator(detailsSelector).first();
        if (!(Boolean) details.evaluate("details => details.open")) details.locator(":scope > summary").click();
    }

    void closeDisclosure(String detailsSelector) {
        Locator details = page.locator(detailsSelector).first();
        if ((Boolean) details.evaluate("details => details.open")) details.locator(":scope > summary").click();
    }

    void scrollToTop() {
        page.evaluate("window.scrollTo(0, 0)");
    }

    void screenshot(String filename) {
        try {
            Files.createDirectories(SCREENSHOTS);
        } catch (java.io.IOException exception) {
            throw new IllegalStateException(exception);
        }
        page.screenshot(new Page.ScreenshotOptions().setPath(SCREENSHOTS.resolve(filename)));
    }

    // Inspection vocabulary

    void range(String range) {
        click("[data-range=" + range + "]");
    }

    void mode(String mode) {
        click("#workbench-modes [data-mode=" + mode + "]");
    }

    void selectLesson(String lessonId) {
        click("[data-lesson-id=" + lessonId + "]");
    }

    void weekday(String weekday) {
        select("#weekday", weekday);
    }

    void search(String text) {
        fill("#lesson-search", text);
    }

    void investigateSubject(String subjectId) {
        select("#subject-investigation", subjectId);
    }

    void investigateTeacher(String teacherId) {
        select("#teacher-investigation", teacherId);
    }

    void filterClass(String cohortId) {
        select("#cohort-filter", cohortId);
    }

    void filterTeacher(String teacherId) {
        select("#teacher-filter", teacherId);
    }

    void filterRoom(String roomId) {
        select("#room-filter", roomId);
    }

    void focusPeriod(String periodId) {
        select("#period-focus", periodId);
    }

    void resetView() {
        click("#reset-view");
    }

    void openFocus(String focusType) {
        click("[data-open-focus=" + focusType + "]");
    }

    void focusEntity(String entityId) {
        select("#focus-entity", entityId);
    }

    void returnToMatrix() {
        click("#return-matrix");
    }

    void collapseInspector() {
        click("#toggle-inspector");
    }

    void reopenInspector() {
        click("#reopen-inspector");
    }

    // Repair vocabulary

    /** Opens the Current repair setup when it is closed; a no-op when the start form is already reachable. */
    void openRepairSetup() {
        Locator launch = page.locator("#open-repair-setup");
        if (launch.count() > 0 && launch.isVisible()) launch.click();
    }

    void repairResource(String resourceType, String resourceId) {
        if (resourceType != null) select("#repair-resource-type", resourceType);
        if (resourceId != null) select("#repair-resource", resourceId);
    }

    void checkRepairPeriod(String periodId) {
        check("#start-repair-form [name=period][value=" + periodId + "]");
    }

    void submitRepair() {
        click("#start-repair-form button[type=submit]");
    }

    /** Starts a repair from Current. Null type or resource keeps the form's prefilled choice. */
    void startRepair(String resourceType, String resourceId, String periodId) {
        openRepairSetup();
        repairResource(resourceType, resourceId);
        checkRepairPeriod(periodId);
        submitRepair();
    }

    /** Selects a lesson (when given) and applies an individual pin with exactly the requested dimensions. */
    void pin(String lessonId, boolean period, boolean room) {
        if (lessonId != null) selectLesson(lessonId);
        setChecked("[name=lesson-dimension][value=PERIOD]", period);
        setChecked("[name=lesson-dimension][value=ROOM]", room);
        click("#apply-pin");
    }

    void previewBulk(String scope, String scopeId) {
        openDisclosure(".repair-controls");
        select("#bulk-scope", scope);
        select("#bulk-scope-id", scopeId);
        click("#preview-bulk");
    }

    void discardDraft() {
        check("#confirm-discard-draft");
        click("#discard-draft");
    }

    void acceptRepair() {
        check("#confirm-repair-accept");
        click("#accept-repair");
    }

    // Network

    /** Records every non-GET/HEAD request from now on as "METHOD /path". */
    MutationLog recordMutations() {
        MutationLog log = new MutationLog();
        page.onRequest(log);
        return log;
    }

    void blockRequests(String urlGlob) {
        page.route(urlGlob, Route::abort);
    }

    final class MutationLog implements Consumer<Request> {
        private final List<String> requests = new CopyOnWriteArrayList<>();

        @Override
        public void accept(Request request) {
            String method = request.method();
            if (!"GET".equals(method) && !"HEAD".equals(method)) {
                requests.add(method + " " + URI.create(request.url()).getPath());
            }
        }

        /** A browser round trip first, so request events already issued by the page are delivered. */
        List<String> requests() {
            page.evaluate("0");
            return List.copyOf(requests);
        }
    }

    // Disclosure handling

    private Locator revealed(String selector) {
        Locator target = page.locator(selector).first();
        target.waitFor(new Locator.WaitForOptions().setState(com.microsoft.playwright.options.WaitForSelectorState.ATTACHED));
        JsonNode closed = JSON.valueToTree(target.evaluate("""
                element => {
                  const closed = [];
                  for (let details = element.parentElement?.closest('details'); details;
                       details = details.parentElement?.closest('details')) {
                    const inSummary = element.closest('summary')?.parentElement === details;
                    if (!details.open && !inSummary) {
                      const selector = details.id ? '#' + CSS.escape(details.id)
                        : 'details' + [...details.classList].map(name => '.' + CSS.escape(name)).join('');
                      closed.unshift([selector, [...document.querySelectorAll(selector)].indexOf(details)]);
                    }
                  }
                  return closed;
                }"""));
        for (JsonNode disclosure : closed) {
            page.locator(disclosure.get(0).stringValue()).nth(disclosure.get(1).intValue())
                    .locator(":scope > summary").click();
        }
        return target;
    }
}
