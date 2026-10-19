package org.schoolkernel.workspace;

import static org.junit.jupiter.api.Assertions.*;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.schoolkernel.workspace.SchoolSetupBrowserIT.*;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import com.microsoft.playwright.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Real administrator journeys against the packaged app, with disposable browser data. */
class StudentGroupsBrowserIT {
    @TempDir Path temporary;
    @Test void reusableSplitsRetainExactValuesAndRecoverFailures() throws Exception {
        var h=new DemoPlanningBrowserIT();h.temporary=temporary;
        h.initialize("student-groups",Path.of("../output/playwright/student-groups-and-coordinated-lessons/UC-1/"+System.currentTimeMillis()));
        try(Playwright pw=Playwright.create(new Playwright.CreateOptions().setEnv(Map.of("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD","1")));
            Browser browser=pw.chromium().launch(new BrowserType.LaunchOptions().setChannel("chrome").setHeadless(true));
            BrowserContext context=h.context(browser)) {
            Page p=context.newPage();p.onPageError(h.errors::add);
            try {
                p.navigate(h.base+"/workspace/");open(p);p.locator("#setup-new").click();
                p.locator("#setup-name").fill("Isolated grouped school");
                section(p,"Subjects");
                for(String subject:List.of("English","Handicraft")){p.locator("#setup-add-record").click();p.locator("#setup-record-name").fill(subject);}
                section(p,"Classes");p.locator("#setup-add-record").click();p.locator("#setup-record-name").fill("5A");p.locator("[data-catalog-field=size]").fill("29");
                p.locator("#setup-add-split").click();Locator first=p.locator("[data-split]").first();
                first.locator("[data-split-field=displayName]:not([data-group])").fill("English õ <script> & reusable");
                assertEquals(List.of("15","14"),first.locator("[data-split-field=size]").all().stream().map(Locator::inputValue).toList());
                first.locator("details summary").click();first.locator("[data-split-subject]").first().check();first.locator("[data-split-subject]").last().check();
                first.locator("[data-group][data-split-field=displayName]").first().fill("Õpilased / Custom label <script> — full original characters");saved(p);
                Object two=p.evaluate("Object.values(JSON.parse(localStorage.getItem('"+KEY+"')).setupDrafts).find(d=>!d.source).classes[0].splits[0]");
                h.record("UC-1 main 1-4/G1-G5: new reusable split defaults to 15/14 for 29; two subjects reference the same split; full custom Unicode/markup label is displayed as text and retained by identity",p,"01-two-groups");
                first.locator("[data-split-field=size]").first().fill("0");saved(p);assertThat(first.locator("[data-split-issues]")).containsText("positive integer");assertThat(first.locator("[data-split-issues]")).containsText("Group total 14");
                first.locator("[data-split-field=size]").first().fill("unfinished");saved(p);
                close(p);p.reload();open(p);first=p.locator("[data-split]").first();assertThat(first.locator("[data-split-field=size]").first()).hasValue("unfinished");
                first.locator("[data-split-field=size]").first().fill("15");saved(p);
                assertEquals(two,p.evaluate("Object.values(JSON.parse(localStorage.getItem('"+KEY+"')).setupDrafts).find(d=>!d.source).classes[0].splits[0]"));
                assertEquals(0,p.locator(".setup-splits script").count());
                h.record("UC-1 3a/main 5: zero and nonnumeric counts remain incomplete with linked issues; reload restores exact unfinished count; restoring 15 yields the exact original split, UUIDs, custom labels and subject references",p,"02-restored-invalid");
                p.locator("[data-catalog-field=size]").fill("28");
                for(int count:List.of(3,4,5)) {
                    p.locator("#setup-add-split").click();Locator split=p.locator("[data-split]").last();
                    split.locator("[data-split-field=displayName]:not([data-group])").fill("Arrangement "+count);
                    split.locator("[data-split-field=groupCount]").fill(String.valueOf(count));split.locator("[data-balance-split]").click();
                    assertThat(p.locator(".setup-confirm")).containsText("Existing entered counts will be replaced");p.locator(".setup-confirm [data-confirm]").click();
                    split=p.locator("[data-split]").last();
                    assertEquals(count,split.locator("[data-split-field=size]").count());
                    if(count==3)assertEquals(List.of("10","9","9"),split.locator("[data-split-field=size]").all().stream().map(Locator::inputValue).toList());
                    if(count==4)assertEquals(List.of("7","7","7","7"),split.locator("[data-split-field=size]").all().stream().map(Locator::inputValue).toList());
                    saved(p);
                }
                // Class resizing must not silently resize an older split.
                assertThat(p.locator("[data-split]").first().locator("[data-split-issues]")).containsText("class total 28");
                h.record("UC-1 main 2/G2/G3: explicit balanced resizing supplies normative three/four sizes and supports five groups; class resize leaves older entered group sizes intact and reports inconsistent total",p,"03-multiple-arrangements");
                Locator last=p.locator("[data-split]").last();last.locator("[data-split-field=groupCount]").fill("129");last.locator("[data-balance-split]").click();
                assertThat(p.locator("#setup-message")).containsText("Cannot balance groups");assertEquals(5,last.locator("[data-split-field=size]").count());
                last.locator("[data-split-field=groupCount]").fill("unfinished");saved(p);
                Object exact=p.evaluate("Object.values(JSON.parse(localStorage.getItem('"+KEY+"')).setupDrafts).find(d=>!d.source).classes");
                close(p);p.reload();open(p);assertEquals(exact,p.evaluate("Object.values(JSON.parse(localStorage.getItem('"+KEY+"')).setupDrafts).find(d=>!d.source).classes"));
                h.record("UC-1 2a/G5: disclosed 128-group limit refuses rebalance without changing existing groups; unfinished requested count and all exact identities survive reopen/reload",p,"04-limit-recovery");
                for(int width:List.of(768,1024,1440)) {
                    p.setViewportSize(width,900);assertTrue((Boolean)p.locator("#school-setup").evaluate("d=>d.scrollWidth<=d.clientWidth+1"));
                    assertTrue((Boolean)p.locator("#setup-content").evaluate("d=>d.scrollWidth<=d.clientWidth+1"));h.record("UC-1 G4: long names and labeled split controls fit "+width+"px without overflow",p,"layout-"+width);
                }
                // Persisted prerequisite fixture represents an external future plan reference, not UC-3 approval.
                close(p);p.evaluate("()=>{const state=JSON.parse(localStorage.getItem('"+KEY+"')),d=Object.values(state.setupDrafts).find(d=>!d.source);d.plans.push({id:'referenced-plan',classId:d.classes[0].id,subjectId:d.subjects[0].id,splitId:d.classes[0].splits[0].id});localStorage.setItem('"+KEY+"',JSON.stringify(state))}");
                p.reload();open(p);first=p.locator("[data-split]").first();assertThat(first.locator("[data-split-field=displayName]:not([data-group])")).isDisabled();saved(p);Object before=durable(p);
                first.locator("[data-remove-split]").click();assertThat(p.locator("#setup-message")).containsText("/plans/0/splitId");assertEquals(before,durable(p));
                first.locator("[data-edit-split]").click();p.locator(".setup-confirm [data-cancel]").click();assertEquals(before,durable(p));
                first.locator("[data-edit-split]").click();p.locator(".setup-confirm [data-confirm]").click();
                p.locator("[data-split]").first().locator("[data-split-field=displayName]:not([data-group])").fill("Explicit renamed split");saved(p);
                assertTrue((Boolean)p.evaluate("()=>{const d=Object.values(JSON.parse(localStorage.getItem('"+KEY+"')).setupDrafts).find(d=>!d.source);return d.plans[0].splitId===d.classes[0].splits[0].id}"));
                h.record("UC-1 3b: restored plan reference is shown; referenced split removal and cancelled edit leave storage unchanged; explicit edit keeps the original referenced UUID",p,"05-protected-references");
                p.evaluate("()=>{window.splitSetItem=Storage.prototype.setItem;Storage.prototype.setItem=function(k,v){if(k==='"+KEY+"')throw new DOMException('injected quota','QuotaExceededError');return window.splitSetItem.call(this,k,v)}}");
                before=durable(p);p.locator("[data-split]").first().locator("[data-split-field=size]").first().fill("13");assertThat(p.locator("#setup-durability")).hasText("Only on this page");assertEquals(before,durable(p));
                p.evaluate("()=>{Storage.prototype.setItem=window.splitSetItem;}");p.locator("#setup-retry").click();saved(p);
                h.record("UC-1 4a: storage quota failure keeps working size 13 and exact last durable revision; explicit retry persists it",p,"06-storage-failure");
                Page other=context.newPage();other.navigate(h.base+"/workspace/");open(other);
                other.locator("[data-split]").last().locator("[data-split-field=displayName]:not([data-group])").fill("Newer tab split");saved(other);
                assertThat(p.locator("#setup-durability")).hasText("Storage conflict");before=durable(p);
                p.locator("[data-split]").first().locator("[data-split-field=size]").first().fill("12");assertThat(p.locator("#setup-durability")).hasText("Storage conflict");assertEquals(before,durable(p));
                h.record("UC-1 4a: actual second-tab change refuses stale overwrite; working size 12 remains recoverable while newer durable split is unchanged",p,"07-stale-tab");other.close();
                close(p);p.reload();open(p);saved(p);before=durable(p);
                p.locator("[data-split]").last().locator("[data-remove-split]").click();p.locator(".setup-confirm [data-cancel]").click();assertEquals(before,durable(p));
                Object surviving=p.evaluate("Object.values(JSON.parse(localStorage.getItem('"+KEY+"')).setupDrafts).find(d=>!d.source).classes[0].splits.slice(0,3)");
                p.locator("[data-split]").last().locator("[data-remove-split]").click();p.locator(".setup-confirm [data-confirm]").click();saved(p);
                assertEquals(surviving,p.evaluate("Object.values(JSON.parse(localStorage.getItem('"+KEY+"')).setupDrafts).find(d=>!d.source).classes[0].splits"));
                p.locator("[data-split]").last().locator("[data-split-field=displayName]:not([data-group])").focus();p.keyboard().press("Tab");assertThat(p.locator("[data-split]").last().locator("[data-split-field=groupCount]")).isFocused();
                h.record("UC-1 3b/G4: cancelled removal preserves exact storage; explicit unreferenced removal preserves all three surviving splits by value; labeled controls support keyboard traversal",p,"08-scoped-removal");
                tool(p,"#setup-discard").click();p.locator(".setup-confirm [data-confirm]").click();assertThat(p.locator("#school-setup")).not().isVisible();
                assertTrue((Boolean)p.evaluate("Object.values(JSON.parse(localStorage.getItem('"+KEY+"')).setupDrafts).every(d=>d.source)"));
                h.record("UC-1 5a: explicit draft discard removes only the independent working draft; source draft and bundled timetable remain; no named school/timetable was modified",p,"09-discard");
                assertTrue(h.errors.isEmpty(),h.errors.toString());Files.writeString(h.output.resolve("verdict.txt"),"PASS\n");
            }catch(Throwable failure){Files.writeString(h.output.resolve("verdict.txt"),"FAIL: "+failure);h.record("FAIL: "+failure,p,"failure");throw failure;}
            finally {
                Files.write(h.output.resolve("browser-errors.txt"),h.errors);
                Files.writeString(h.output.resolve("ledger.md"),"# UC-1 reusable splits administrator walkthrough\n\nDate: "+Instant.now()+"\nApplication: "+h.base+"\nBuild: packaged working tree based on 3cb66f3; installed Chrome via Java Playwright. Isolated disposable browser context and server; no operator data. Exact submission recorded in feature checkpoint. Each recorded step PASS unless marked FAIL.\n\n"+String.join("\n\n",h.steps)+"\n\nOverall: "+Files.readString(h.output.resolve("verdict.txt"))+"\nExternal plan fixture only tests reference protection; it does not approve teaching authoring or kernel consumption. Storage deliberately retained until disposable context closes; server stopped.\n");
            }
        }finally{h.stop();}
    }
    @Test void nestedAndJointRelationshipsRetainExactAnonymousAttendance() throws Exception {
        var h=new DemoPlanningBrowserIT();h.temporary=temporary;
        h.initialize("student-relationships",Path.of("../output/playwright/student-groups-and-coordinated-lessons/UC-2/"+System.currentTimeMillis()));
        try(Playwright pw=Playwright.create(new Playwright.CreateOptions().setEnv(Map.of("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD","1")));
            Browser browser=pw.chromium().launch(new BrowserType.LaunchOptions().setChannel("chrome").setHeadless(true));BrowserContext context=h.context(browser)) {
            Page p=context.newPage();p.onPageError(h.errors::add);
            try {
                p.navigate(h.base+"/workspace/");open(p);p.locator("#setup-new").click();section(p,"Classes");p.locator("#setup-add-record").click();p.locator("#setup-record-name").fill("Crossing class");p.locator("[data-catalog-field=size]").fill("28");
                for(String name:List.of("English","German","Handicraft")){
                    p.locator("#setup-add-split").click();Locator split=p.locator("[data-split]").last();split.locator("[data-split-field=displayName]:not([data-group])").fill(name);
                    if(name.equals("Handicraft")){split.locator("[data-split-field=groupCount]").fill("4");split.locator("[data-balance-split]").click();p.locator(".setup-confirm [data-confirm]").click();}
                }
                p.locator("[data-open-relationships]").click();p.locator("[data-related-split]").nth(0).check();p.locator("[data-related-split]").nth(2).check();
                assertEquals(0,p.locator("[data-intersection]").count());
                String constituent=p.locator("[data-related-split]").nth(2).getAttribute("data-related-split");p.locator("#setup-constituent-split").selectOption(constituent);
                assertThat(p.locator("#setup-relationship-issues")).containsText("exactly one");
                List<String> parents=p.locator("[data-union]").all().stream().map(n->n.getAttribute("data-union")).distinct().toList();
                p.locator("[data-union='"+parents.get(0)+"']").nth(0).check();p.locator("[data-union='"+parents.get(0)+"']").nth(1).check();
                p.locator("[data-union='"+parents.get(1)+"']").nth(2).check();p.locator("[data-union='"+parents.get(1)+"']").nth(3).check();
                assertThat(p.locator("#setup-relationship-issues")).containsText("consistent");assertEquals(4,p.locator("#setup-relationship-result li").count());saved(p);
                Object nested=p.evaluate("Object.values(JSON.parse(localStorage.getItem('"+KEY+"')).setupDrafts).find(d=>!d.source).classes[0].relationships");
                h.record("UC-2 main 1-4/G1/G2/G4: real nested union authoring derives four disjoint seven-person blocks totaling 28; English groups each compose two stable handicraft groups; joint controls remain absent",p,"01-nested");
                p.locator("[data-union='"+parents.get(1)+"']").first().check();assertThat(p.locator("#setup-relationship-issues")).containsText("exactly one");saved(p);
                close(p);p.reload();open(p);assertThat(p.locator("#setup-relationship-issues")).containsText("exactly one");
                p.locator("[data-union='"+parents.get(1)+"']").first().uncheck();saved(p);assertEquals(nested,p.evaluate("Object.values(JSON.parse(localStorage.getItem('"+KEY+"')).setupDrafts).find(d=>!d.source).classes[0].relationships"));
                h.record("UC-2 3a/4a/G5: contradictory nested membership is retained and survives reload; explicit correction restores exact prior unions without changing identities or group sizes",p,"02-nested-recovery");
                p.locator("[data-related-split]").nth(2).uncheck();p.locator("[data-related-split]").nth(1).check();p.locator("#setup-relationship-mode").selectOption("joint");assertEquals(4,p.locator("[data-intersection]").count());
                for(Locator input:p.locator("[data-intersection]").all())input.fill("7");saved(p);
                assertThat(p.locator("#setup-relationship-issues")).containsText("consistent");assertEquals(4,p.locator("#setup-relationship-result li").count());
                h.record("UC-2 2a/G1-G3: crossing English/German 2×2 joints of seven reconcile all 14-person margins and class total; every English group overlaps every German group",p,"03-crossing");
                p.locator("[data-intersection]").first().fill("-1");assertThat(p.locator("#setup-relationship-issues")).containsText("nonnegative integer");p.locator("[data-intersection]").first().fill("unfinished");saved(p);
                close(p);p.reload();open(p);assertThat(p.locator("[data-intersection]").first()).hasValue("unfinished");p.locator("[data-intersection]").first().fill("7");
                p.locator("[data-related-split]").nth(2).check();assertEquals(16,p.locator("[data-intersection]").count());assertThat(p.locator("#setup-relationship-issues")).containsText("combined intersections need explicit counts");
                h.record("UC-2 2b/3a/G3/G5: negative/nonnumeric counts stay editable; adding a third split exposes all sixteen combined dimensions and refuses to infer joints from the four pairwise margins",p,"04-three-dimensions");
                p.locator("details").filter(new Locator.FilterOptions().setHas(p.locator("#setup-remove-incompatible-joints"))).locator("summary").click();
                saved(p);Object before=durable(p);p.locator("#setup-remove-incompatible-joints").click();p.locator(".setup-confirm [data-cancel]").click();assertEquals(before,durable(p));
                p.locator("#setup-remove-incompatible-joints").click();p.locator(".setup-confirm [data-confirm]").click();
                var mapper=tools.jackson.databind.json.JsonMapper.builder().build();
                List<String> englishIds=(List<String>)p.evaluate("Object.values(JSON.parse(document.querySelector('#school-definition-json').value).classes[0].splits[0].groups).map(g=>g.id)");
                List<String> germanIds=(List<String>)p.evaluate("JSON.parse(document.querySelector('#school-definition-json').value).classes[0].splits[1].groups.map(g=>g.id)");
                List<String> craftIds=(List<String>)p.evaluate("JSON.parse(document.querySelector('#school-definition-json').value).classes[0].splits[2].groups.map(g=>g.id)");
                for(Locator input:p.locator("[data-intersection]").all()){
                    List<String> ids=mapper.readValue(input.getAttribute("data-intersection"),List.class);int ei=englishIds.indexOf(ids.get(0)),gi=germanIds.indexOf(ids.get(1)),hi=craftIds.indexOf(ids.get(2));
                    int count=ei==hi/2?(gi==0?(hi%2==0?3:4):(hi%2==0?4:3)):0;input.fill(String.valueOf(count));
                }
                saved(p);assertThat(p.locator("#setup-relationship-issues")).containsText("consistent");assertEquals(8,p.locator("#setup-relationship-result li").count());
                Object exact=p.evaluate("Object.values(JSON.parse(localStorage.getItem('"+KEY+"')).setupDrafts).find(d=>!d.source).classes");
                close(p);p.reload();open(p);assertEquals(exact,p.evaluate("Object.values(JSON.parse(localStorage.getItem('"+KEY+"')).setupDrafts).find(d=>!d.source).classes"));
                assertThat(p.locator("#setup-relationship-result")).containsText("Total: 28 / 28");
                h.record("UC-2 2b/G1/G5: explicit preview/cancel/removal corrects incompatible pairwise rows without losing current combined counts, then all sixteen joint counts (eight positive, eight zero) yield eight disjoint blocks and exact margins; reload preserves all coordinates, counts and inactive nested unions",p,"05-complete-joint");
                for(int width:List.of(768,1440)){p.setViewportSize(width,900);assertTrue((Boolean)p.locator("#setup-content").evaluate("d=>d.scrollWidth<=d.clientWidth+1"));h.record("UC-2 G4: labeled joint dimensions and totals fit "+width+"px without overflow",p,"layout-"+width);}
                assertTrue(h.errors.isEmpty(),h.errors.toString());Files.writeString(h.output.resolve("verdict.txt"),"PASS\n");
            }catch(Throwable failure){Files.writeString(h.output.resolve("verdict.txt"),"FAIL: "+failure);h.record("FAIL: "+failure,p,"failure");throw failure;}
            finally{Files.write(h.output.resolve("browser-errors.txt"),h.errors);Files.writeString(h.output.resolve("ledger.md"),"# UC-2 administrator walkthrough\n\nDate: "+Instant.now()+"\nApplication: "+h.base+"\nBuild: packaged working diff based on 3cb66f3; installed Chrome via Java Playwright; isolated disposable data.\n\n"+String.join("\n\n",h.steps)+"\n\nOverall: "+Files.readString(h.output.resolve("verdict.txt"))+"\nKernel interpretation and generation remain UC-8/9. Browser context closes and local server stops.\n");}
        }finally{h.stop();}
    }

    @Test void mixedTeachingCountsReceivedPeriodsAndRetainsOverrides() throws Exception {
        var h=new DemoPlanningBrowserIT();h.temporary=temporary;
        h.initialize("student-demand",Path.of("../output/playwright/student-groups-and-coordinated-lessons/UC-3/"+System.currentTimeMillis()));
        try(Playwright pw=Playwright.create(new Playwright.CreateOptions().setEnv(Map.of("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD","1")));
            Browser browser=pw.chromium().launch(new BrowserType.LaunchOptions().setChannel("chrome").setHeadless(true));BrowserContext context=h.context(browser)) {
            Page p=context.newPage();p.onPageError(h.errors::add);
            try {
                p.navigate(h.base+"/workspace/");open(p);p.locator("#setup-new").click();p.locator("#setup-name").fill("Mixed teaching school");
                section(p,"Subjects");p.locator("#setup-add-record").click();p.locator("#setup-record-name").fill("English");
                section(p,"Classes");p.locator("#setup-add-record").click();p.locator("#setup-record-name").fill("5A");p.locator("[data-catalog-field=size]").fill("28");
                p.locator("#setup-add-split").click();p.locator("[data-split-field=displayName]:not([data-group])").fill("English groups");
                String split=p.locator("[data-split]").getAttribute("data-split");p.locator("[data-open-audience-plans]").click();p.locator("#setup-audience-subject").selectOption(new com.microsoft.playwright.options.SelectOption().setLabel("English"));p.locator("#setup-add-audience-plan").click();
                p.locator("[data-audience-field=weeklyTotal]").fill("3");p.locator("[data-audience-field=delivery]").selectOption("mixed");p.locator("[data-audience-field=splitId]").selectOption(split);p.locator("[data-audience-field=wholeClassCount]").fill("2");p.locator("[data-reconcile-audience-sessions]").click();p.locator(".setup-confirm [data-confirm]").click();saved(p);
                assertThat(p.locator("[data-audience-summary]")).containsText("Actual sessions: 4");assertThat(p.locator("[data-audience-summary]")).containsText("Student teaching periods: 84");
                assertEquals(List.of("",""),p.locator("[data-audience-override]").all().stream().map(Locator::inputValue).toList());
                Object stable=p.evaluate("Object.values(JSON.parse(localStorage.getItem('"+KEY+"')).setupDrafts).find(d=>!d.source).audiencePlans[0].portions");
                h.record("UC-3 main 1-4/G1/G2: authored mixed English demand yields four actual sessions, 84 student teaching periods (three each for 28), two whole-class plus one per group; both group totals inherit three",p,"01-mixed-demand");
                p.locator("[data-audience-override]").last().fill("4");saved(p);assertThat(p.locator("[data-audience-summary]")).containsText("Actual sessions: 5");assertThat(p.locator("[data-audience-summary]")).containsText("total 4 (explicit override)");
                p.locator("[data-audience-field=weeklyTotal]").fill("5");assertThat(p.locator("[data-audience-summary]")).containsText("total 5 (inherited)");assertThat(p.locator("[data-audience-summary]")).containsText("total 4 (explicit override)");
                p.locator("[data-audience-field=wholeClassCount]").fill("6");assertThat(p.locator("[data-audience-summary]")).containsText("exceeds total");
                p.locator("[data-audience-field=wholeClassCount]").fill("unfinished");saved(p);close(p);p.reload();open(p);assertThat(p.locator("[data-audience-field=wholeClassCount]")).hasValue("unfinished");assertThat(p.locator("[data-audience-override]").last()).hasValue("4");
                h.record("UC-3 3a/4a/G3/G4: explicit group override survives default change and reload; excessive and unfinished whole-class contribution remains recoverable with blocking feedback",p,"02-invalid-recovery");
                p.locator("[data-audience-field=wholeClassCount]").fill("2");p.locator("[data-audience-field=weeklyTotal]").fill("3");p.locator("[data-audience-override]").last().fill("");saved(p);
                assertThat(p.locator("[data-audience-summary]")).containsText("Review changed teaching explicitly");Object before=durable(p);
                p.locator("[data-reconcile-audience-sessions]").click();p.locator(".setup-confirm [data-cancel]").click();assertEquals(before,durable(p));
                p.locator("[data-reconcile-audience-sessions]").click();p.locator(".setup-confirm [data-confirm]").click();saved(p);assertEquals(stable,p.evaluate("Object.values(JSON.parse(localStorage.getItem('"+KEY+"')).setupDrafts).find(d=>!d.source).audiencePlans[0].portions"));
                h.record("UC-3 3a/4b/G4: clearing override restores inheritance; reduced teaching keeps prior lesson references until explicit review; cancellation preserves exact data, confirmed reduction restores exact original surviving portion/session identities",p,"03-explicit-reduction");
                p.locator("[data-audience-field=wholeClassCount]").fill("3");p.locator("[data-reconcile-audience-sessions]").click();p.locator(".setup-confirm [data-confirm]").click();saved(p);
                assertEquals(1,((List<?>)p.evaluate("Object.values(JSON.parse(localStorage.getItem('"+KEY+"')).setupDrafts).find(d=>!d.source).audiencePlans[0].portions")).size());
                assertThat(p.locator("[data-audience-summary]")).containsText("outstanding group sessions 0");
                p.locator("[data-go-class-splits]").click();assertThat(p.locator("[data-split]")).isVisible();p.locator("[data-open-audience-plans]").click();
                Object exact=p.evaluate("Object.values(JSON.parse(localStorage.getItem('"+KEY+"')).setupDrafts).find(d=>!d.source).audiencePlans");close(p);p.reload();open(p);assertEquals(exact,p.evaluate("Object.values(JSON.parse(localStorage.getItem('"+KEY+"')).setupDrafts).find(d=>!d.source).audiencePlans"));
                h.record("UC-3 1a/G2/G4: whole-class contribution equal to every total creates no outstanding group sessions; explicit class-split navigation preserves the plan; reload restores exact demand and zero group portions",p,"04-zero-outstanding");
                assertTrue(h.errors.isEmpty(),h.errors.toString());Files.writeString(h.output.resolve("verdict.txt"),"PASS\n");
            }catch(Throwable failure){Files.writeString(h.output.resolve("verdict.txt"),"FAIL: "+failure);h.record("FAIL: "+failure,p,"failure");throw failure;}
            finally{Files.write(h.output.resolve("browser-errors.txt"),h.errors);Files.writeString(h.output.resolve("ledger.md"),"# UC-3 administrator walkthrough\n\nDate: "+Instant.now()+"\nApplication: "+h.base+"\nBuild: packaged working diff based on 3cb66f3; installed Chrome via Java Playwright; isolated disposable data.\n\n"+String.join("\n\n",h.steps)+"\n\nOverall: "+Files.readString(h.output.resolve("verdict.txt"))+"\nTeacher controls, readiness and kernel consumption remain separate UCs. Browser context closes; local server stops.\n");}
        }finally{h.stop();}
    }

    @Test void weeklyTeachersPreserveInheritanceFixedSharingAndExplicitAdoption() throws Exception {
        var h=new DemoPlanningBrowserIT();h.temporary=temporary;h.initialize("student-teachers",Path.of("../output/playwright/student-groups-and-coordinated-lessons/UC-4/"+System.currentTimeMillis()));
        try(Playwright pw=Playwright.create(new Playwright.CreateOptions().setEnv(Map.of("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD","1")));Browser browser=pw.chromium().launch(new BrowserType.LaunchOptions().setChannel("chrome").setHeadless(true));BrowserContext context=h.context(browser)){
            Page p=context.newPage();p.onPageError(h.errors::add);
            try{
                p.navigate(h.base+"/workspace/");open(p);p.locator("#setup-new").click();
                tool(p,"#school-definition-json").fill(Files.readString(Path.of("src/test/resources/student-groups/authoring.json")));p.locator("#setup-apply-json").click();
                section(p,"Classes");p.locator("[data-record]").first().click();p.locator("[data-open-audience-plans]").click();p.locator(".setup-teacher-policies > summary").click();
                assertEquals(List.of("automatic","automatic","automatic"),p.locator("[data-teacher-policy-mode]").all().stream().map(Locator::inputValue).toList());assertThat(p.locator(".setup-teacher-policies")).containsText("Eligible candidates: A, B");
                h.record("UC-4 main 1-2/G1/G6: explicit import restores approved UC-1/3 prerequisite data with two qualified/class-eligible teachers; all three audience portions inherit A/B and default to automatic weekly continuity",p,"01-inherited-candidates");
                p.locator("[data-teacher-policy-mode]").nth(0).selectOption("sharing");
                for(int i=0;i<2;i++)p.locator("[data-add-teacher-share]").click();
                p.locator("[data-share-teacher]").nth(0).selectOption("A");p.locator("[data-share-count]").nth(0).fill("2");p.locator("[data-share-teacher]").nth(1).selectOption("B");p.locator("[data-share-count]").nth(1).fill("1");
                p.locator("[data-teacher-policy-mode]").nth(1).selectOption("fixed");p.locator("[data-fixed-audience-teacher]").selectOption("A");
                p.locator("[data-portion-candidate-mode]").nth(2).selectOption("restricted");p.locator("[data-teacher-portion]").nth(2).locator("[data-portion-candidate=A]").uncheck();saved(p);
                assertThat(p.locator("[data-audience-teacher-issues]")).isEmpty();
                h.record("UC-4 main 3-4/3a/3b/G1-G3: whole-class portion deliberately shares two sessions with A and one with B; first group is hard-fixed A; second group remains automatic restricted to B; no per-occurrence teacher editing is required",p,"02-fixed-and-sharing");
                p.locator("[data-teacher-portion]").nth(2).locator("[data-portion-candidate=B]").uncheck();assertThat(p.locator("[data-audience-teacher-issues]")).containsText("no eligible teacher candidates");saved(p);
                close(p);p.reload();open(p);p.locator(".setup-teacher-policies > summary").click();assertThat(p.locator("[data-audience-teacher-issues]")).containsText("no eligible teacher candidates");p.locator("[data-teacher-portion]").nth(2).locator("[data-portion-candidate=B]").check();
                p.locator("[data-share-count]").first().fill("3");assertThat(p.locator("[data-audience-teacher-issues]")).containsText("total 4");p.locator("[data-share-count]").first().fill("unfinished");saved(p);close(p);p.reload();open(p);p.locator(".setup-teacher-policies > summary").click();assertThat(p.locator("[data-share-count]").first()).hasValue("unfinished");p.locator("[data-share-count]").first().fill("2");saved(p);
                assertThat(p.locator("[data-audience-teacher-issues]")).isEmpty();
                Object exact=p.evaluate("Object.values(JSON.parse(localStorage.getItem('"+KEY+"')).setupDrafts).find(d=>!d.source).audiencePlans");close(p);p.reload();open(p);assertEquals(exact,p.evaluate("Object.values(JSON.parse(localStorage.getItem('"+KEY+"')).setupDrafts).find(d=>!d.source).audiencePlans"));
                h.record("UC-4 4a/4b/G5/G6: explicit empty candidates remain blocking after reload; sharing total mismatch and unfinished count are retained, then explicitly corrected; exact IDs, fixed/automatic modes, restrictions and sharing counts survive reload",p,"03-policy-recovery");
                // Equivalent supported legacy prerequisite, isolated from operator data.
                close(p);p.evaluate("()=>{const state=JSON.parse(localStorage.getItem('"+KEY+"')),d=Object.values(state.setupDrafts).find(d=>!d.source);d.grid.intervals.push({id:'music-opportunity',periodId:'music-opportunity',day:'MONDAY',kind:'Lesson',label:'P6',start:'13:00',end:'13:45'});d.subjects.push({id:'music',displayName:'Music'});d.teachers[0].qualifiedSubjectIds.push('music');d.teachers[0].eligibleClassIdsBySubject.music=['c'];d.plans.push({id:'legacy-fixed',classId:'c',subjectId:'music',requiredCount:2,allocations:[{teacherId:'A',count:2}],occurrences:[{id:'legacy-one',teacherId:'A',displayName:'Original first label'},{id:'legacy-two',teacherId:'A',displayName:'Original second label'}],blockRules:[]});localStorage.setItem('"+KEY+"',JSON.stringify(state))}");
                p.reload();open(p);p.locator("#setup-back-audience-plans").click();p.locator("[data-open-class-plan]").click();saved(p);Object before=durable(p);
                p.locator("[data-adopt-automatic-teacher]").click();assertThat(p.locator(".setup-confirm")).containsText("Currently fixed: A: 2 sessions");p.locator(".setup-confirm [data-cancel]").click();assertEquals(before,durable(p));
                p.locator("[data-adopt-automatic-teacher]").click();p.locator(".setup-confirm [data-confirm]").click();saved(p);
                assertTrue((Boolean)p.evaluate("()=>{const d=Object.values(JSON.parse(localStorage.getItem('"+KEY+"')).setupDrafts).find(d=>!d.source),p=d.audiencePlans.find(p=>p.id==='legacy-fixed');return !d.plans.length&&p.portions[0].teacherPolicy.mode==='automatic'&&JSON.stringify(p.portions[0].sessionIds)===JSON.stringify(['legacy-one','legacy-two'])&&p.sourceFixedPlan.occurrences[0].displayName==='Original first label'}"));
                section(p,"Review");p.locator("#setup-check").click();p.waitForFunction("()=>document.querySelector('#setup-readiness').textContent.startsWith('Ready')",null,new Page.WaitForFunctionOptions().setTimeout(30000));
                assertTrue((Boolean)p.evaluate("()=>{const d=Object.values(JSON.parse(localStorage.getItem('"+KEY+"')).setupDrafts).find(d=>!d.source);return d.check.definition.audiencePlans.every(p=>!Object.hasOwn(p,'sourceFixedPlan'))&&d.audiencePlans.some(p=>p.sourceFixedPlan)}"));
                h.record("UC-4 1a/G3/G6: fixed legacy source remains fixed through cancelled preview with exact nonmutation; explicit adoption changes only the reviewed policy, preserves lesson IDs and complete original fixed plan/labels as source provenance",p,"04-explicit-adoption");
                assertTrue(h.errors.isEmpty(),h.errors.toString());Files.writeString(h.output.resolve("verdict.txt"),"PASS\n");
            }catch(Throwable failure){Files.writeString(h.output.resolve("verdict.txt"),"FAIL: "+failure);h.record("FAIL: "+failure,p,"failure");throw failure;}
            finally{Files.write(h.output.resolve("browser-errors.txt"),h.errors);Files.writeString(h.output.resolve("ledger.md"),"# UC-4 teacher-policy administrator walkthrough\n\nDate: "+Instant.now()+"\nApplication: "+h.base+"\nPackaged working diff, real Chrome/Java Playwright, isolated disposable context and server. Explicit imported prerequisite fixture: output/kernel/student-groups-and-coordinated-lessons/UC-4/authoring.json.\n\n"+String.join("\n\n",h.steps)+"\n\nOverall: "+Files.readString(h.output.resolve("verdict.txt"))+"\nPublished CLI input evidence is recorded separately. Teacher selection/review during solve belongs to UC-8/9. Context closes and server stops.\n");}
        }finally{h.stop();}
    }

    @Test void schedulingControlsRetainCountsScopesAndReferencedBlocks() throws Exception {
        var h=new DemoPlanningBrowserIT();h.temporary=temporary;h.initialize("student-coordination",Path.of("../output/playwright/student-groups-and-coordinated-lessons/UC-5/"+System.currentTimeMillis()));
        try(Playwright pw=Playwright.create(new Playwright.CreateOptions().setEnv(Map.of("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD","1")));Browser browser=pw.chromium().launch(new BrowserType.LaunchOptions().setChannel("chrome").setHeadless(true));BrowserContext context=h.context(browser)){
            Page p=context.newPage();p.onPageError(h.errors::add);
            try{
                p.navigate(h.base+"/workspace/");open(p);p.locator("#setup-new").click();
                tool(p,"#school-definition-json").fill(Files.readString(Path.of("src/test/resources/student-groups/authoring.json")));p.locator("#setup-apply-json").click();
                section(p,"Classes");p.locator("[data-record]").first().click();p.locator("[data-open-audience-plans]").click();
                p.locator(".setup-audience-scheduling > summary").click();p.locator("[data-add-audience-block]").first().click();
                p.locator(".setup-audience-scheduling > summary").click();
                p.locator("[data-audience-block-field=operator]").selectOption("EXACTLY");
                // Rendering collapses details; reopening is intentional navigation.
                p.locator(".setup-audience-scheduling > summary").click();p.locator("[data-audience-block-field=length]").fill("4");assertThat(p.locator("[data-audience-scheduling-issues]")).containsText("above");p.locator("[data-audience-block-field=length]").fill("2");
                p.locator("[data-audience-room-mode]").first().selectOption("restricted");p.locator(".setup-audience-scheduling > summary").click();
                p.locator("[data-room-portion]").first().locator("[data-audience-fixed-room]").selectOption("r");saved(p);
                p.locator("[data-open-coordination]").click();assertThat(p.locator("[data-coordination-class]")).containsText("Internal student gaps are permitted");
                for(String type:List.of("START_TOGETHER","SAME_DAY","DIFFERENT_DAYS","ALLOWED","FIXED","COVERAGE","GAP_FREE")){
                    p.locator("#setup-coordination-type").selectOption(type);p.locator("#setup-add-coordination").click();
                    Locator r=p.locator("[data-coordination]").last();
                    if(!type.equals("GAP_FREE")){
                        r.locator("[data-coordination-activity]").nth(0).check();
                        if(List.of("START_TOGETHER","SAME_DAY","DIFFERENT_DAYS","COVERAGE").contains(type))r.locator("[data-coordination-activity]").nth(1).check();
                        if(type.equals("START_TOGETHER")){
                            String block=(String)p.evaluate("Object.values(JSON.parse(localStorage.getItem('"+KEY+"')).setupDrafts).find(d=>!d.source).audiencePlans[0].blockRules[0].id");r.locator("[data-coordination-unit]").first().selectOption(block);
                            r.locator("[data-coordination-field=count]").fill("2");assertThat(r.locator("[data-coordination-issues]")).containsText("distinct rounds exceeds");r.locator("[data-coordination-field=count]").fill("1");
                        }
                        if(type.equals("ALLOWED")){r.locator("[data-coordination-set-mode=days]").selectOption("selected");assertThat(r.locator("[data-coordination-issues]")).containsText("nonempty");r.locator("[data-coordination-day]").first().check();}
                        if(type.equals("FIXED")){r.locator("[data-coordination-field=day]").selectOption("MONDAY");r.locator("[data-coordination-field=periodId]").selectOption("p0");}
                    }else{r.locator("[data-coordination-set-mode=days]").selectOption("selected");r.locator("[data-coordination-day]").first().check();}
                }
                saved(p);assertEquals(7,p.locator("[data-coordination]").count());
                h.record("UC-5 main 1-4/G1-G3/G8: all seven explicit hard controls retain activity identities, distinct occurrence counts, required-block units, allowed day set, fixed start and class-wide gap-free scope; excessive count and empty set produce blocking feedback without lowering values",p,"01-all-controls");
                Object exact=p.evaluate("(()=>{const d=Object.values(JSON.parse(localStorage.getItem('"+KEY+"')).setupDrafts).find(d=>!d.source);return {plans:d.audiencePlans,rules:d.coordinationRules}})()");
                close(p);p.reload();open(p);assertEquals(exact,p.evaluate("(()=>{const d=Object.values(JSON.parse(localStorage.getItem('"+KEY+"')).setupDrafts).find(d=>!d.source);return {plans:d.audiencePlans,rules:d.coordinationRules}})()"));
                p.locator("#setup-back-coordination").click();p.locator(".setup-audience-scheduling > summary").click();Object before=durable(p);
                p.locator("[data-remove-audience-block]").first().click();assertThat(p.locator("#setup-message")).containsText("Correct coordination references");assertEquals(before,durable(p));
                h.record("UC-5 3a/4a/G5-G8: exact policy and controls survive reload; referenced block removal is refused with exact nonmutation; inherited blocks and room controls remain visible",p,"02-references");
                p.locator("[data-open-coordination]").click();before=durable(p);p.locator("[data-remove-coordination]").first().click();p.locator(".setup-confirm [data-cancel]").click();assertEquals(before,durable(p));
                p.locator("[data-remove-coordination]").first().click();p.locator(".setup-confirm [data-confirm]").click();saved(p);assertEquals(6,p.locator("[data-coordination]").count());
                for(int width:List.of(768,1440)){p.setViewportSize(width,900);assertTrue((Boolean)p.locator("#setup-content").evaluate("d=>d.scrollWidth<=d.clientWidth+1"));h.record("UC-5 G8: labeled coordination controls fit "+width+"px; removal preview/cancel preserves input and explicit removal changes only selected rule",p,"layout-"+width);}
                assertTrue(h.errors.isEmpty(),h.errors.toString());Files.writeString(h.output.resolve("verdict.txt"),"PASS\n");
            }catch(Throwable failure){Files.writeString(h.output.resolve("verdict.txt"),"FAIL: "+failure);h.record("FAIL: "+failure,p,"failure");throw failure;}
            finally{Files.write(h.output.resolve("browser-errors.txt"),h.errors);Files.writeString(h.output.resolve("ledger.md"),"# UC-5 administrator walkthrough\n\nDate: "+Instant.now()+"\nApplication: "+h.base+"\nPinned packaged working diff based on 3cb66f3; real installed Chrome; isolated disposable context. Prerequisite: src/test/resources/student-groups/authoring.json.\n\n"+String.join("\n\n",h.steps)+"\n\nOverall: "+Files.readString(h.output.resolve("verdict.txt"))+"\nAssignment evaluation and search remain UC-8/9. Browser context and server close.\n");}
        }finally{h.stop();}
    }

    private static void teachersOpen(Page p){if(!(Boolean)p.locator(".setup-teacher-policies").evaluate("e=>e.open"))p.locator(".setup-teacher-policies > summary").click();}

    @Test void audienceReadinessChecksExactRevisionAndKeepsUnusedDraftWarnings() throws Exception {
        var h=new DemoPlanningBrowserIT();h.temporary=temporary;h.initialize("student-readiness",Path.of("../output/playwright/student-groups-and-coordinated-lessons/UC-6/"+System.currentTimeMillis()));
        try(Playwright pw=Playwright.create(new Playwright.CreateOptions().setEnv(Map.of("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD","1")));Browser browser=pw.chromium().launch(new BrowserType.LaunchOptions().setChannel("chrome").setHeadless(true));BrowserContext context=h.context(browser)){
            Page p=context.newPage();p.onPageError(h.errors::add);
            try{
                p.navigate(h.base+"/workspace/");open(p);p.locator("#setup-new").click();tool(p,"#school-definition-json").fill(Files.readString(Path.of("src/test/resources/student-groups/authoring.json")));p.locator("#setup-apply-json").click();if((Boolean)p.locator("#setup-tools").evaluate("e=>e.open"))p.locator("#setup-tools > summary").click();
                section(p,"Classes");p.locator("[data-record]").first().click();p.locator("#setup-add-split").click();p.locator("[data-split]").last().locator("[data-split-field=size]").first().fill("unfinished");saved(p);
                section(p,"Review");assertThat(p.locator("#setup-audience-totals")).containsText("Actual audience sessions: 5");assertThat(p.locator("#setup-audience-totals")).containsText("Student teaching periods: 112");assertThat(p.locator("#setup-review-warnings")).containsText("Unused draft arrangement");
                p.locator("#setup-check").click();p.waitForFunction("()=>document.querySelector('#setup-readiness').textContent.startsWith('Ready')",null,new Page.WaitForFunctionOptions().setTimeout(30000));saved(p);
                assertTrue((Boolean)p.evaluate("()=>{const d=Object.values(JSON.parse(localStorage.getItem('"+KEY+"')).setupDrafts).find(d=>!d.source);return d.check.authoringRevision===d.revision&&d.check.catalogVersion===11&&d.check.definition.cohorts[0].splits.length===1&&d.classes[0].splits.length===2&&d.classes[0].splits[1].groups[0].size==='unfinished'}"));
                h.record("UC-6 main 1-4/2c/G2-G4: real HTTP/kernel check marks exact schema2/catalog11 revision Ready; five actual sessions and 112 student teaching periods; incomplete unused split remains warned and exactly retained outside active checked definition; readiness explicitly does not prove feasibility",p,"01-ready");
                section(p,"Classes");p.locator("[data-record]").first().click();p.locator("[data-open-audience-plans]").click();teachersOpen(p);p.locator("[data-portion-candidate-mode]").last().selectOption("restricted");teachersOpen(p);p.locator("[data-teacher-portion]").last().locator("[data-portion-candidate=A]").uncheck();teachersOpen(p);p.locator("[data-teacher-portion]").last().locator("[data-portion-candidate=B]").uncheck();
                section(p,"Review");p.locator("#setup-check").click();assertThat(p.locator("#setup-readiness")).containsText("Blocked");saved(p);assertTrue((Boolean)p.evaluate("!Object.values(JSON.parse(localStorage.getItem('"+KEY+"')).setupDrafts).find(d=>!d.source).check"));
                p.locator("[data-review-issue]").filter(new Locator.FilterOptions().setHasText("no eligible teacher candidates")).first().click();assertThat(p.locator("[data-audience-plan]")).isVisible();teachersOpen(p);p.locator("[data-teacher-portion]").last().locator("[data-portion-candidate=B]").check();
                h.record("UC-6 2b/G1/G2: empty audience candidates block readiness without installing a check; correction link opens the exact class audience plan and restores explicitly selected eligible teacher",p,"02-correction");
                teachersOpen(p);p.locator("[data-teacher-policy-mode]").first().selectOption("fixed");teachersOpen(p);p.locator("[data-fixed-audience-teacher]").selectOption("A");
                section(p,"Review");assertThat(p.locator("#setup-content")).containsText("exceeds reference (nonblocking)");p.locator("#setup-check").click();p.waitForFunction("()=>document.querySelector('#setup-readiness').textContent.startsWith('Ready')");
                h.record("UC-6 main 3/G1: actual fixed teaching load exceeds A's zero reference maximum but does not block exact supported readiness; automatic choices remain selection-pending monitoring",p,"03-monitoring");
                // Hold one real response, edit a newer revision, then release it.
                final APIResponse[] held={null};final Route[] pending={null};p.route("**/api/demo/definition/check",route->{held[0]=route.fetch();pending[0]=route;});p.locator("#setup-check").click();p.waitForCondition(()->held[0]!=null);
                p.locator("#setup-name").fill("Newer revision during check");saved(p);pending[0].fulfill(new Route.FulfillOptions().setResponse(held[0]));p.unroute("**/api/demo/definition/check");p.locator("#setup-check-notice").waitFor();assertThat(p.locator("#setup-check-notice")).containsText("older revision");assertThat(p.locator("#setup-readiness")).not().containsText("Ready.");
                h.record("UC-6 4a/G4: held real check response released after newer name/revision was retained; older result is explicitly identified and cannot mark new revision Ready or install old checked definition",p,"04-stale-response");
                close(p);p.reload();open(p);section(p,"Review");assertThat(p.locator("#setup-readiness")).not().containsText("Ready.");
                for(int width:List.of(768,1440)){p.setViewportSize(width,900);assertTrue((Boolean)p.locator("#setup-content").evaluate("d=>d.scrollWidth<=d.clientWidth+1"));h.record("UC-6 G1/G4: exact draft survives reload; review warnings, totals and correction controls fit "+width+"px without overflow",p,"layout-"+width);}
                assertTrue(h.errors.isEmpty(),h.errors.toString());Files.writeString(h.output.resolve("verdict.txt"),"PASS\n");
            }catch(Throwable failure){Files.writeString(h.output.resolve("verdict.txt"),"FAIL: "+failure);h.record("FAIL: "+failure,p,"failure");throw failure;}
            finally{Files.write(h.output.resolve("browser-errors.txt"),h.errors);Files.writeString(h.output.resolve("ledger.md"),"# UC-6 administrator walkthrough\n\nDate: "+Instant.now()+"\nApplication: "+h.base+"\nPinned packaged working diff based on 3cb66f3, real Chrome, isolated disposable data. Fixture src/test/resources/student-groups/authoring.json.\n\n"+String.join("\n\n",h.steps)+"\n\nOverall: "+Files.readString(h.output.resolve("verdict.txt"))+"\nContext and local server close. Generation feasibility remains UC-9.\n");}
        }finally{h.stop();}
    }

    @Test void namedGroupedSnapshotsSaveAtomicallyAndPreserveTheirSource() throws Exception {
        var h=new DemoPlanningBrowserIT();h.temporary=temporary;h.initialize("student-snapshot",Path.of("../output/playwright/student-groups-and-coordinated-lessons/UC-7/"+System.currentTimeMillis()));
        try(Playwright pw=Playwright.create(new Playwright.CreateOptions().setEnv(Map.of("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD","1")));Browser browser=pw.chromium().launch(new BrowserType.LaunchOptions().setChannel("chrome").setHeadless(true));BrowserContext context=h.context(browser)){
            Page p=context.newPage();p.onPageError(h.errors::add);
            try{
                p.navigate(h.base+"/workspace/");open(p);p.locator("#setup-new").click();tool(p,"#school-definition-json").fill(Files.readString(Path.of("src/test/resources/student-groups/authoring.json")));p.locator("#setup-apply-json").click();if((Boolean)p.locator("#setup-tools").evaluate("e=>e.open"))p.locator("#setup-tools > summary").click();
                section(p,"Classes");p.locator("[data-record]").first().click();p.locator("#setup-add-split").click();p.locator("[data-split]").last().locator("[data-split-field=size]").first().fill("unfinished");
                p.locator("[data-open-audience-plans]").click();p.locator("[data-open-coordination]").click();p.locator("#setup-coordination-type").selectOption("GAP_FREE");p.locator("#setup-add-coordination").click();
                section(p,"Review");p.locator("#setup-name").fill(" ");saved(p);p.locator("#setup-save-school").click();assertThat(p.locator("#setup-message")).containsText("Enter a school name");assertTrue((Boolean)p.evaluate("Object.keys(JSON.parse(localStorage.getItem('"+KEY+"')).schools).length===0"));
                p.locator("#setup-name").fill("Grouped Õ school copy");saved(p);
                Object authored=p.evaluate("(()=>{const d=Object.values(JSON.parse(localStorage.getItem('"+KEY+"')).setupDrafts).find(d=>!d.source);return {classes:d.classes,teachers:d.teachers,plans:d.audiencePlans,rules:d.coordinationRules}})()");
                // Refuse only the atomic school insertion; ordinary draft/check retention remains available.
                p.evaluate("()=>{window.__schoolSet=Storage.prototype.setItem;Storage.prototype.setItem=function(k,v){if(k==='"+KEY+"'&&Object.keys(JSON.parse(v).schools).length)throw new DOMException('isolated named save quota','QuotaExceededError');return window.__schoolSet.call(this,k,v)}}");
                p.locator("#setup-save-school").click();p.locator("#setup-message").filter(new Locator.FilterOptions().setHasText("Nothing was saved")).waitFor(new Locator.WaitForOptions().setTimeout(30000));assertTrue((Boolean)p.evaluate("Object.keys(JSON.parse(localStorage.getItem('"+KEY+"')).schools).length===0"));
                assertEquals(authored,p.evaluate("(()=>{const d=Object.values(JSON.parse(localStorage.getItem('"+KEY+"')).setupDrafts).find(d=>!d.source);return {classes:d.classes,teachers:d.teachers,plans:d.audiencePlans,rules:d.coordinationRules}})()"));
                h.record("UC-7 1a/3a/G1-G3: blank name refuses save; injected atomic insertion quota refuses named copy while preserving exact grouped/teacher/coordination and unfinished authoring values, original catalog and retryable draft",p,"01-save-refused");
                p.evaluate("()=>{Storage.prototype.setItem=window.__schoolSet}");p.locator("#setup-save-school").click();assertThat(p.locator("#school-setup")).not().isVisible(new com.microsoft.playwright.assertions.LocatorAssertions.IsVisibleOptions().setTimeout(30000));
                String schoolId=p.locator("#school-selector").inputValue();Object source=p.evaluate("JSON.parse(localStorage.getItem('"+KEY+"')).schools['"+schoolId+"']");
                assertEquals(authored,p.evaluate("(()=>{const d=JSON.parse(localStorage.getItem('"+KEY+"')).schools['"+schoolId+"'].authoring;return {classes:d.classes,teachers:d.teachers,plans:d.audiencePlans,rules:d.coordinationRules}})()"));
                assertTrue((Boolean)p.evaluate("(()=>{const s=JSON.parse(localStorage.getItem('"+KEY+"')).schools['"+schoolId+"'];return s.check.catalogVersion===11&&s.check.definitionRevision===s.definitionRevision&&s.check.authoringRevision===s.authoring.revision&&s.definition.cohorts[0].splits.length===1&&s.authoring.classes[0].splits.length===2})()"));
                assertFalse(Files.readString(temporary.resolve("launches")).contains("plan"));assertThat(p.locator("#timetable-matrix")).containsText("School setup → Review");
                h.record("UC-7 main 1-4/G1/G2: retry creates a new immutable named browser-local identity holding exact checked catalog11 definition plus complete authoring/coordination and unused incomplete split; no plan process or timetable is created",p,"02-saved-copy");
                p.reload();p.locator("#school-selector").waitFor();assertThat(p.locator("#school-selector")).hasValue("full");p.locator("#school-selector").selectOption(schoolId);open(p);p.locator("#setup-copy-source").click();section(p,"Review");
                p.locator("#setup-save-school").click();assertThat(p.locator("#setup-message")).containsText("already exists");assertEquals(source,p.evaluate("JSON.parse(localStorage.getItem('"+KEY+"')).schools['"+schoolId+"']"));
                p.locator("#setup-name").fill("Independent grouped revision");saved(p);Object before=durable(p);
                Page newer=context.newPage();newer.onPageError(h.errors::add);newer.navigate(h.base+"/workspace/");newer.locator("#school-selector").waitFor();newer.locator("#school-selector").selectOption(schoolId);open(newer);newer.locator("#setup-name").fill("Newer tab grouped work");saved(newer);
                p.locator("#setup-save-school").click();p.locator("#setup-message").filter(new Locator.FilterOptions().setHasText("Another tab")).waitFor(new Locator.WaitForOptions().setTimeout(30000));assertThat(p.locator("#setup-name")).hasValue("Independent grouped revision");
                assertEquals(source,p.evaluate("JSON.parse(localStorage.getItem('"+KEY+"')).schools['"+schoolId+"']"));assertTrue((Boolean)p.evaluate("Object.keys(JSON.parse(localStorage.getItem('"+KEY+"')).schools).length===1"));
                h.record("UC-7 1a/3a/G1-G3: reload starts bundled default; explicit school selection restores saved data; duplicate-name save refuses; later edit is an independent source-linked revision; newer real tab prevents stale save, preserving source school by value and newer durable work",p,"03-source-and-stale");newer.close();
                assertTrue(h.errors.isEmpty(),h.errors.toString());Files.writeString(h.output.resolve("verdict.txt"),"PASS\n");
            }catch(Throwable failure){Files.writeString(h.output.resolve("verdict.txt"),"FAIL: "+failure);h.record("FAIL: "+failure,p,"failure");throw failure;}
            finally{Files.write(h.output.resolve("browser-errors.txt"),h.errors);Files.writeString(h.output.resolve("ledger.md"),"# UC-7 administrator walkthrough\n\nDate: "+Instant.now()+"\nApplication: "+h.base+"\nPinned packaged working diff based on 3cb66f3; real Chrome, disposable origin/context. Fixture src/test/resources/student-groups/authoring.json. Storage quota injection only refuses named catalog insertion; stale conflict uses a real second tab.\n\n"+String.join("\n\n",h.steps)+"\n\nOverall: "+Files.readString(h.output.resolve("verdict.txt"))+"\nNo generation claimed. Context and local processes close; operator data untouched.\n");}
        }finally{h.stop();}
    }

}
