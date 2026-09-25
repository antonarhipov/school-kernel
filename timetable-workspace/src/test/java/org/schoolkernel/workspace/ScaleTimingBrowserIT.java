package org.schoolkernel.workspace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.Arrays;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * RULE-25 diagnostic timing at target scale. Samples are measured inside the page around the app's own handlers, so
 * they exclude Playwright round trips; the p95 values are reported, never enforced. Exclude with
 * {@code -DexcludedGroups=performance}.
 */
@Tag("performance")
class ScaleTimingBrowserIT extends WorkbenchBrowserSupport {
    private static final Duration SCALE = Duration.ofSeconds(20);

    @Test
    @DisplayName("UC-3 G5 and RULE-25: target-scale post-load interactions record diagnostic p95 evidence")
    void measuresTargetScaleInspectionInteractions() {
        ObjectNode document = fixtures.scaleDocument();
        JsonNode definition = document.path("acceptedBaseline").path("definition");
        assertEquals(1_000, definition.path("lessons").size());
        assertEquals(100, definition.path("teachers").size());
        assertEquals(60, definition.path("cohorts").size());
        assertEquals(100, definition.path("rooms").size());
        assertEquals(60, definition.path("periods").size());
        storeAccepted(document);
        String before = storedDocument();

        workbench.open().awaitText("Showing 60 of 60 classes", SCALE);
        workbench.selectLesson("lesson-999");
        assertTrue(workbench.awaitText("Accepted assignment").contains("Declared lesson 999"));
        workbench.expect("document.querySelector('.matrix-wrap').scrollTop > 0", "selecting an off-viewport lesson scrolls the canvas to it");
        workbench.range("DAY");
        workbench.awaitText("Day · Thursday");

        JsonNode search = measured("""
                const element=document.querySelector('#lesson-search');
                element.value=i%2===0?'Subject 1':'';
                element.dispatchEvent(new Event('input',{bubbles:true}));""");
        JsonNode filters = measured("""
                const element=document.querySelector('#teacher-filter');
                element.value=i%2===0?'teacher-0':'';
                element.dispatchEvent(new Event('change',{bubbles:true}));""");
        workbench.resetView();
        workbench.weekday("TUESDAY");
        workbench.awaitText("Day · Tuesday");
        JsonNode selections = measured("document.querySelector('[data-lesson-id]').click();");

        recordPerformance("search", search, 250.0);
        recordPerformance("filter", filters, 250.0);
        recordPerformance("selection", selections, 250.0);
        System.out.printf("UC-3 scale samples search=%s filter=%s selection=%s; solver time excluded%n", search, filters, selections);
        assertEquals(before, storedDocument());
    }

    @Test
    @DisplayName("UC-4 G6 and RULE-25: target-scale persisted pin feedback records diagnostic p95 evidence")
    void measuresTargetScalePinFeedback() {
        storeAccepted(fixtures.scaleDocument());
        workbench.open().awaitText("Showing 60 of 60 classes", SCALE);
        workbench.startRepair("TEACHER", "teacher-99", "period-59");
        workbench.awaitText("Repair draft · not current");
        JsonNode samples = workbench.value("""
                (async () => {
                  const samples=[];
                  for(let i=0;i<20;i++) {
                    document.querySelector('[data-lesson-id="lesson-0"]').click();
                    await new Promise(resolve => requestAnimationFrame(resolve));
                    const applying=i%2===0;
                    const started=performance.now();
                    document.querySelector(applying?'#apply-pin':'#remove-pin').click();
                    const expected=applying?'Accepted period pinned':'Unpinned · kernel stability ordering applies';
                    while(!document.body.innerText.includes(expected)) await new Promise(resolve => setTimeout(resolve,2));
                    samples.push(Number((performance.now()-started).toFixed(3)));
                  }
                  return samples;
                })()""");
        recordPerformance("pin feedback", samples, 250.0);
        System.out.printf("UC-4 scale pin-feedback samples=%s; solver time excluded%n", samples);
        JsonNode stored = storedWorkspaceDocument();
        assertEquals(1_000, stored.path("acceptedBaseline").path("result").path("timetable").path("assignments").size());
        assertEquals("REPAIR_DRAFT", storedLifecycle());
    }

    @Test
    @DisplayName("UC-6 G5 and RULE-25: target-scale proposal impact review records diagnostic timing evidence")
    void measuresTargetScaleProposalReviewOpening() {
        storeProposal(fixtures.scaleProposalDocument());
        String rendered = workbench.open().awaitText("Unique changed lessons", SCALE);
        assertTrue(rendered.contains("100"));
        assertTrue(rendered.contains("Direct effects of your intent: 50"));
        assertTrue(rendered.contains("Solver ripple effects: 50"));
        assertTrue(rendered.contains("By class"));
        workbench.expect("document.querySelector('.workbench-layout .matrix-wrap [data-lesson-id=lesson-0][data-comparison-side=accepted]') !== null && document.querySelector('.workbench-layout .matrix-wrap [data-lesson-id=lesson-0][data-comparison-side=proposed]') !== null",
                "UC-4 main/1a: accepted origin and proposed destination must coexist on the whole-school canvas");
        String durable = storedDocument();
        workbench.filterTeacher("teacher-0");
        workbench.expect("!document.querySelector('[data-lesson-id=lesson-0][data-comparison-side=accepted]').hidden && !document.querySelector('[data-lesson-id=lesson-0][data-comparison-side=proposed]').hidden && document.querySelector('#represented-lesson-count').textContent.includes('60')",
                "UC-4 extension 3a: filtering must retain both representations and count unique lesson IDs");
        workbench.resetView();
        workbench.click("[data-lesson-id=lesson-0][data-comparison-side=proposed]");
        workbench.expect("document.querySelectorAll('[data-lesson-id=lesson-0].selected').length === 2 && document.querySelector('.comparison-details')?.textContent.includes('Declared period 0') && document.querySelector('.comparison-details')?.textContent.includes('Declared period 1')",
                "UC-4 G1/G5: either side selects one identity and exposes both exact periods");
        workbench.collapseInspector();
        workbench.expect("document.querySelector('#workbench-inspector').hidden && document.querySelector('#inspector-summary').textContent.includes('Declared lesson 0')");
        workbench.reopenInspector();
        workbench.range("DAY");
        workbench.expect("document.querySelector('[data-mode=PROPOSAL]')?.getAttribute('aria-pressed') === 'true' && document.querySelector('#workbench-inspector .before-after')?.textContent.includes('Declared period 1')");
        workbench.range("WEEK");
        workbench.openFocus("cohortId");
        workbench.expect("document.querySelector('.focused-schedule [data-lesson-id=lesson-0][data-comparison-side=accepted]') !== null && document.querySelector('.focused-schedule [data-lesson-id=lesson-0][data-comparison-side=proposed]') !== null");
        workbench.returnToMatrix();
        workbench.expect("document.querySelector('.workbench-layout .matrix-wrap [data-lesson-id=lesson-0]') !== null");
        workbench.click("#show-unchanged");
        assertTrue(workbench.awaitText("Accepted and unchanged · not included in change totals").contains("Old assignment"));
        workbench.click("[data-review-lesson=lesson-0]");
        assertTrue(workbench.awaitText("Proposed change · not current").contains("Proposal impact review"));
        assertEquals(durable, storedDocument(), "UC-4 G6: comparison, filter, focus, and inspector actions are presentation-only");
        double openingMs = workbench.value("window.__workspaceProposalReviewMs").doubleValue();
        assertTrue(Double.isFinite(openingMs) && openingMs >= 0.0, "proposal review timing must be a finite non-negative value");
        System.out.printf("UC-6 scale proposal-review opening=%.3f ms; solver time excluded%n", openingMs);
        System.out.printf("proposal review reference=1000.0 ms; diagnostic only; exceeded=%s%n", openingMs >= 1_000.0);
    }

    /** Twenty in-page samples of {@code operation}, which may use the loop index {@code i}. */
    private JsonNode measured(String operation) {
        return workbench.value("""
                (async () => {
                  const samples=[];
                  for(let i=0;i<20;i++) {
                    await new Promise(resolve => requestAnimationFrame(resolve));
                    const started=performance.now();
                    %s
                    samples.push(Number((performance.now()-started).toFixed(3)));
                  }
                  return samples;
                })()""".formatted(operation));
    }

    private static void recordPerformance(String interaction, JsonNode samples, double referenceMs) {
        assertEquals(20, samples.size(), interaction + " must retain all raw timing samples");
        double[] ordered = new double[samples.size()];
        for (int index = 0; index < samples.size(); index++) ordered[index] = samples.get(index).doubleValue();
        Arrays.sort(ordered);
        double p95 = ordered[(int) Math.ceil(ordered.length * 0.95) - 1];
        assertTrue(Double.isFinite(p95) && p95 >= 0.0, interaction + " p95 must be a finite non-negative value");
        System.out.printf("%s p95=%.3f ms; reference=%.1f ms; diagnostic only; exceeded=%s%n",
                interaction, p95, referenceMs, p95 >= referenceMs);
    }
}
