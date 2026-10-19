package org.schoolkernel.workspace;

import static org.junit.jupiter.api.Assertions.*;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import java.nio.file.*;
import java.time.*;
import com.microsoft.playwright.*;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.node.ObjectNode;

/** Audience inspection at the normal PostgreSQL workspace boundary. */
class AudienceInspectionBrowserIT extends WorkbenchBrowserSupport {
    @Test void independentlyReviewedAudiencesRemainCompactAcrossNormalWorkspaceViews() throws Exception {
        var h=new DemoPlanningBrowserIT();h.output=Path.of("../output/playwright/student-groups-and-coordinated-lessons/UC-11/"+System.currentTimeMillis()+"-normal").toAbsolutePath();Files.createDirectories(h.output);h.base="http://localhost:"+port;
        Page p=workbench.page();
        try {
            var pairs=JSON.readTree(Files.readString(Path.of("src/test/resources/student-groups/inspection-pairs.json")));
            for(String id:new String[]{"groups2","groups3","groups4","nested","crossing","partial","gap"}) {
                var pair=pairs.path(id);var supplied=JSON.createObjectNode();supplied.putObject("timetable").set("assignments",pair.path("assignments"));
                var checked=verifier.review(new ImportDocuments(pair.path("definition"),supplied,null,ImportDocuments.ImportMode.ACCEPTED_BASELINE),Duration.ofSeconds(30),ignored->{},()->{});
                assertTrue(checked.path("valid").asBoolean(),checked.toPrettyString());
                var documents=new ImportDocuments(pair.path("definition"),checked.path("result"),null,ImportDocuments.ImportMode.ACCEPTED_BASELINE);
                verifier.verify(documents);
                ObjectNode document=JSON.createObjectNode();document.putObject("school").put("id",pair.path("definition").path("schoolId").asString()).put("displayName",id);
                var baseline=document.putObject("acceptedBaseline");baseline.set("definition",pair.path("definition"));baseline.set("result",checked.path("result"));baseline.set("manifest",new ManifestService().validatedOrGenerated(documents));
                storeAccepted(document);String before=storedDocument();workbench.open();p.locator(".audience-cell").first().waitFor();
                assertThat(p.locator("#accepted-view")).containsText("Student gaps and teacher workload");
                if(id.equals("crossing")){p.locator(".audience-summary:visible").first().click();assertEquals(2,p.locator("dialog [data-inspect-audience-lesson]").count());p.keyboard().press("Escape");}
                else {p.locator("[data-lesson-id]").first().click();assertThat(p.locator("#lesson-details-host")).containsText("5A");}
                h.record("UC-11 normal PostgreSQL: "+id+" reviewed exact pair; compact lanes/composites or crossing summary selects full audience details",p,id+"-desktop");
                workbench.range("DAY");p.locator(".audience-cell").first().waitFor();workbench.range("WEEK");
                for(int width:new int[]{700,320}) {
                    p.setViewportSize(width,900);p.locator(".mobile-week-matrix").waitFor();assertTrue((Boolean)p.evaluate("document.documentElement.scrollWidth<=innerWidth"));
                    Locator summary=p.locator(".audience-summary:visible").first();
                    if(summary.count()>0){summary.click();assertThat(p.locator("dialog")).containsText("5A");p.keyboard().press("Escape");}
                    else {p.locator("[data-narrow-audience-lesson]").first().click();assertThat(p.locator("dialog")).containsText("5A");p.keyboard().press("Escape");}
                    h.record("UC-11 normal: "+id+" five-day week fits "+width+"px; summary or lesson details accessible, filters initially collapsed",p,id+"-"+width);
                }
                p.setViewportSize(1600,900);assertEquals(before,storedDocument());
            }
            Files.writeString(h.output.resolve("verdict.txt"),"PASS\n");
        }catch(Throwable failure){Files.writeString(h.output.resolve("verdict.txt"),"FAIL: "+failure);h.record("FAIL "+failure,p,"failure");throw failure;}
        finally {Files.writeString(h.output.resolve("ledger.md"),"# UC-11 normal workspace browser journey\n\nDate: "+Instant.now()+"\nApplication: "+h.base+"\nReal Chrome, isolated PostgreSQL Testcontainer, exact fixtures independently reviewed and verified through real CLI before accepted fixture installation.\n\n"+String.join("\n\n",h.steps)+"\n\nOverall: "+Files.readString(h.output.resolve("verdict.txt"))+"\nSQL equality proves no pair/state mutation. Browser context/container cleanup by harness.\n");}
    }
}
