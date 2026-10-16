package org.schoolkernel.workspace;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.mock.web.MockHttpSession;
import tools.jackson.databind.*;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

class DemoPlanningTest {
    final JsonMapper json = JsonMapper.builder().build();
    class Fixture implements AutoCloseable {
        final DemoWorkspaces owners = new DemoWorkspaces(new DemoSettings(List.of("demo.example"),List.of(),false,
                Duration.ofHours(1),Duration.ofHours(8),50),json,new StaticListableBeanFactory().getBeanProvider(KernelPlanner.class));
        final DemoWorkspaceStore store = new DemoWorkspaceStore(owners);
        final WorkspaceMutation mutation = new WorkspaceMutation(store);
        final RepairDraftService drafts = mock(RepairDraftService.class);
        final ProposalReviewService reviews = mock(ProposalReviewService.class);
        final DemoVerificationService verification = mock(DemoVerificationService.class);
        final DemoKernelRunner runner = mock(DemoKernelRunner.class);
        final DemoKernelAllowance allowance;
        final DemoArtifactCache cache;
        final DemoPlanningService service;
        final AtomicInteger calls = new AtomicInteger();
        final CountDownLatch started = new CountDownLatch(1), release = new CountDownLatch(1);
        Fixture(boolean reuse, int queue, Duration estimate, Duration abandon, Duration daily) throws Exception {
            allowance = new DemoKernelAllowance(daily); cache = new DemoArtifactCache(reuse,67108864,Duration.ofHours(1));
            when(verification.build()).thenReturn("sha256:kernel-build"); when(verification.timeout()).thenReturn(Duration.ofSeconds(30));
            when(reviews.create(any(),any(),any(),any())).thenReturn(json.createObjectNode());
            when(runner.run(anyString(),any(),any(),any())).thenAnswer(call -> {
                calls.incrementAndGet(); started.countDown();
                try { release.await(); } catch (InterruptedException stop) { Thread.currentThread().interrupt(); return new DemoKernelRunner.Outcome(null,"FAILED","CANCELLED","Stopped",null); }
                var input = call.getArgument(1,DemoKernelRunner.Input.class);
                var result = (ObjectNode)json.readTree(Path.of("src/test/resources/demo/legacy-samples/small-result.json"));
                ((ObjectNode)result.path("limit")).put("duration",input.effort().toString());
                var verified = new KernelVerifier.Verification(input.definition().path("schoolId").asString(),result.path("inputRevision").asString(),result.path("timetableRevision").asString());
                return new DemoKernelRunner.Outcome(new DemoKernelRunner.Artifact(result,verified,1234,234,call.getArgument(0)),"FEASIBLE",null,null,null);
            });
            service = new DemoPlanningService(store,mutation,owners,drafts,reviews,new DemoDefinitionLimits(11000000,2500),
                    new DemoSearchSettings(5,60,200,queue,estimate,abandon,Duration.ofSeconds(10)),verification,allowance,cache,runner,json);
        }
        Fixture() throws Exception { this(true,8,Duration.ofMinutes(2),Duration.ofMinutes(2),Duration.ofHours(4)); }
        DemoWorkspaces.Entry owner(String variant) throws Exception {
            var owner = owners.open(new MockHttpSession(),true);
            ObjectNode definition = (ObjectNode)json.readTree(Path.of("src/test/resources/demo/legacy-samples/small-definition.json"));
            if (!variant.isEmpty()) definition.put("displayName",variant);
            var result = json.readTree(Path.of("src/test/resources/demo/legacy-samples/small-result.json"));
            ObjectNode document = json.createObjectNode(); document.set("initialDefinition",definition);
            document.put("definitionRevision",result.path("inputRevision").asString());
            in(owner,()->store.resetAndReplace(0,WorkspaceState.INITIAL_DRAFT,document)); return owner;
        }
        <T> T in(DemoWorkspaces.Entry owner,Supplier<T> action) { AtomicReference<T> value = new AtomicReference<>(); owners.bound(owner,()->value.set(action.get())); return value.get(); }
        WorkspaceAggregate current(DemoWorkspaces.Entry owner) { return in(owner,store::load); }
        WorkspaceAggregate start(DemoWorkspaces.Entry owner,String effort) { return in(owner,()->service.start(store.load().etag(),json.createObjectNode().put("effort",effort))); }
        JsonNode poll(DemoWorkspaces.Entry owner,UUID id) { return in(owner,()->service.run(id)); }
        void terminal(DemoWorkspaces.Entry owner) throws Exception { await(()->!current(owner).state().name().startsWith("SOLVING_")); }
        public void close() { release.countDown(); service.close(); owners.close(); cache.close(); }
    }
    static void await(java.util.function.BooleanSupplier condition) throws Exception {
        long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(6);
        while(!condition.getAsBoolean()&&System.nanoTime()<deadline) Thread.sleep(10);
        assertTrue(condition.getAsBoolean());
    }

    @Test void coalescedPrivateRunsCancelIndependentlyAndReuseAtExhaustionWithoutProcesses() throws Exception {
        try(var f=new Fixture()) {
            var a=f.owner("");var b=f.owner(""); var sa=f.start(a,"QUICK");assertTrue(f.started.await(3,TimeUnit.SECONDS));var sb=f.start(b,"QUICK");
            assertNotEquals(sa.activeRunId(),sb.activeRunId()); assertEquals(1,f.calls.get());
            assertEquals(Duration.ofSeconds(53).toNanos(),f.allowance.snapshot().reservedNanos());
            assertEquals("RUNNING",f.poll(b,sb.activeRunId()).path("status").asString());
            assertEquals("RUN_NOT_FOUND",assertThrows(WorkspaceProblem.class,()->f.poll(a,sb.activeRunId())).code());
            f.in(a,()->f.service.cancel(sa.etag(),sa.activeRunId()));assertEquals(WorkspaceState.INITIAL_DRAFT,f.current(a).state());
            f.release.countDown();f.terminal(b); assertEquals(WorkspaceState.INITIAL_PROPOSAL,f.current(b).state());
            assertEquals(WorkspaceState.INITIAL_DRAFT,f.current(a).state());assertEquals(0,f.allowance.snapshot().reservedNanos());
            var c=f.owner("");try(var held=f.allowance.reserve(Duration.ofHours(4))) {
                var hit=f.start(c,"QUICK");assertEquals(WorkspaceState.INITIAL_PROPOSAL,hit.state());
                assertTrue(hit.document().path("proposal").path("reused").asBoolean());
                assertNotEquals(f.current(b).document().path("proposal").path("id"),hit.document().path("proposal").path("id"));
                assertNotEquals(f.current(b).document().path("proposal").path("runId"),hit.document().path("proposal").path("runId"));
                assertEquals(f.current(b).document().path("proposal").path("result"),hit.document().path("proposal").path("result"));
                ((ObjectNode)hit.document().path("proposal").path("result")).put("tampered",true);
            }
            var d=f.owner("");assertFalse(f.start(d,"QUICK").document().path("proposal").path("result").has("tampered"));assertEquals(1,f.calls.get());
        }
    }
    @Test void distinctFifoQueueCountAndEstimateGatesRefuseWithoutMutationOrReservation() throws Exception {
        try(var f=new Fixture(true,1,Duration.ofMinutes(2),Duration.ofMinutes(2),Duration.ofHours(4))) {
            var a=f.owner("a");var b=f.owner("b");var c=f.owner("c");f.start(a,"QUICK");assertTrue(f.started.await(3,TimeUnit.SECONDS));var queued=f.start(b,"THOROUGH");
            assertEquals(1,f.poll(b,queued.activeRunId()).path("queuePosition").asInt());
            assertTrue(f.poll(b,queued.activeRunId()).path("approximate").asBoolean());
            var before=f.current(c);var allowance=f.allowance.snapshot();
            assertEquals("DEMO_BUSY",assertThrows(WorkspaceProblem.class,()->f.start(c,"QUICK")).code());assertEquals(before,f.current(c));assertEquals(allowance,f.allowance.snapshot());
            f.release.countDown();f.terminal(a);f.terminal(b);assertEquals(2,f.calls.get());
        }
        try(var f=new Fixture(true,8,Duration.ofMillis(1),Duration.ofMinutes(2),Duration.ofHours(4))) {
            f.start(f.owner("a"),"THOROUGH");assertTrue(f.started.await(3,TimeUnit.SECONDS));var b=f.owner("b");
            assertEquals("DEMO_BUSY",assertThrows(WorkspaceProblem.class,()->f.start(b,"QUICK")).code());assertEquals(WorkspaceState.INITIAL_DRAFT,f.current(b).state());
        }
    }
    @Test void dailyReservationCoversSolveWatchdogTerminationAndVerificationBeforeAnyMutation() throws Exception {
        try(var f=new Fixture(true,8,Duration.ofMinutes(2),Duration.ofMinutes(2),Duration.ofSeconds(52))) {
            var a=f.owner("");var before=f.current(a);
            assertEquals("DAILY_KERNEL_LIMIT",assertThrows(WorkspaceProblem.class,()->f.start(a,"QUICK")).code());
            assertEquals(before,f.current(a));assertEquals(0,f.calls.get());assertEquals(0,f.allowance.snapshot().reservedNanos());
            assertEquals("INVALID_SEARCH_EFFORT",assertThrows(WorkspaceProblem.class,()->f.in(a,()->f.service.start(before.etag(),json.createObjectNode().put("limit","PT2M")))).code());
            assertEquals("STALE_WORKSPACE_VERSION",assertThrows(WorkspaceProblem.class,()->f.in(a,()->f.service.start("\"ws-0\"",null))).code());
        }
    }
    @Test void disabledReuseKeepsRequestsDistinctAndEveryCompleteIdentityDimensionMatters() throws Exception {
        try(var f=new Fixture(false,8,Duration.ofMinutes(2),Duration.ofMinutes(2),Duration.ofHours(4))) {
            var a=f.owner("");var b=f.owner("");f.start(a,"QUICK");assertTrue(f.started.await(3,TimeUnit.SECONDS));var queued=f.start(b,"QUICK");
            assertEquals("QUEUED",f.poll(b,queued.activeRunId()).path("status").asString());f.release.countDown();f.terminal(a);f.terminal(b);assertEquals(2,f.calls.get());
            f.start(f.owner(""),"QUICK");await(()->f.calls.get()==3);
        }
        try(var f=new Fixture()) {
            f.release.countDown();var a=f.owner("");f.start(a,"QUICK");f.terminal(a);
            var b=f.owner("");f.start(b,"THOROUGH");f.terminal(b);
            var c=f.owner("");f.in(c,()->{var now=f.store.load();var document=(ObjectNode)now.document().deepCopy();var definition=(ObjectNode)document.path("initialDefinition");definition.withArray("lessons").add(definition.withArray("lessons").remove(0));return f.store.replace(now.version(),now.state(),now.state(),document);});f.start(c,"QUICK");f.terminal(c);
            var d=f.owner("");f.in(d,()->{var now=f.store.load();var document=(ObjectNode)now.document().deepCopy();((ObjectNode)document.path("initialDefinition")).putArray("reservedPeriodIds");return f.store.replace(now.version(),now.state(),now.state(),document);});f.start(d,"QUICK");f.terminal(d);assertEquals(4,f.calls.get());
        }
    }
    @Test void queuedAbandonmentDetachesOnlyUnpolledSubscriberAndReleasesUnusedWork() throws Exception {
        try(var f=new Fixture(true,8,Duration.ofMinutes(2),Duration.ofMillis(250),Duration.ofHours(4))) {
            var a=f.owner("a");var b=f.owner("b");var c=f.owner("b");f.start(a,"QUICK");assertTrue(f.started.await(3,TimeUnit.SECONDS));var sb=f.start(b,"QUICK");var sc=f.start(c,"QUICK");
            long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(4);
            while(f.current(b).state()==WorkspaceState.SOLVING_INITIAL&&System.nanoTime()<deadline){f.poll(c,sc.activeRunId());Thread.sleep(50);}
            assertEquals("EXPIRED",f.current(b).document().path("lastRun").path("status").asString());assertEquals("QUEUED",f.poll(c,sc.activeRunId()).path("status").asString());
            f.in(c,()->f.service.cancel(f.store.load().etag(),sc.activeRunId()));assertEquals(Duration.ofSeconds(53).toNanos(),f.allowance.snapshot().reservedNanos());
            assertEquals(1,f.calls.get());
        }
    }
    @Test void checkingPhaseCancellationHoldsReservationUntilPhysicalExitAndLateCompletionCannotPublish() throws Exception {
        try(var f=new Fixture()) {
            CountDownLatch checking=new CountDownLatch(1),interrupted=new CountDownLatch(1),stopped=new CountDownLatch(1);
            doAnswer(call->{
                call.getArgument(3,Runnable.class).run();checking.countDown();
                try{f.release.await();}catch(InterruptedException stop){interrupted.countDown();stopped.await();}
                return new DemoKernelRunner.Outcome(null,"FAILED","CANCELLED","Stopped",null);
            }).when(f.runner).run(anyString(),any(),any(),any());
            var a=f.owner("");var run=f.start(a,"QUICK");assertTrue(checking.await(3,TimeUnit.SECONDS));
            assertEquals("CHECKING_RESULT",f.poll(a,run.activeRunId()).path("status").asString());
            f.in(a,()->f.service.cancel(f.store.load().etag(),run.activeRunId()));assertTrue(interrupted.await(3,TimeUnit.SECONDS));
            assertEquals(Duration.ofSeconds(53).toNanos(),f.allowance.snapshot().reservedNanos());stopped.countDown();await(()->f.allowance.snapshot().reservedNanos()==0);
            assertEquals(WorkspaceState.INITIAL_DRAFT,f.current(a).state());assertFalse(f.current(a).document().has("proposal"));
        }
        try(var f=new Fixture()) {
            var a=f.owner("");f.start(a,"QUICK");assertTrue(f.started.await(3,TimeUnit.SECONDS));
            f.in(a,()->{var now=f.store.load();return f.store.replace(now.version(),now.state(),WorkspaceState.INITIAL_DRAFT,now.document());});var newer=f.current(a);
            f.release.countDown();await(()->f.allowance.snapshot().reservedNanos()==0);Thread.sleep(100);assertEquals(newer,f.current(a));
        }
    }
    @Test void unsuccessfulResultsNeverCacheAndSessionEndDetachesSharedWork() throws Exception {
        try(var f=new Fixture()) {
            doAnswer(call->{f.calls.incrementAndGet();return new DemoKernelRunner.Outcome(null,"NOT_FOUND","NO_FEASIBLE_SOLUTION_FOUND","Budget ended",json.createObjectNode().put("terminationReason","TIME_LIMIT"));}).when(f.runner).run(anyString(),any(),any(),any());
            var a=f.owner("");f.start(a,"QUICK");f.terminal(a);assertEquals("NOT_FOUND",f.current(a).document().path("lastRun").path("status").asString());
            f.start(a,"QUICK");f.terminal(a);assertEquals(2,f.calls.get());assertEquals(0,f.cache.residentBytes());
        }
        try(var f=new Fixture()) {
            var a=f.owner("");var b=f.owner("");f.start(a,"QUICK");assertTrue(f.started.await(3,TimeUnit.SECONDS));f.start(b,"QUICK");a.ended=true;
            Thread.sleep(1200);f.release.countDown();f.terminal(b);assertEquals(WorkspaceState.INITIAL_PROPOSAL,f.current(b).state());assertTrue(a.ended);
        }
    }
    @Test void repairReuseIdentityIncludesBothCompleteBaselineDocumentsAndKernelBuild() throws Exception {
        try(var f=new Fixture()) {
            f.release.countDown();
            for(int variant=0;variant<4;variant++) {
                var owner=f.owner("");
                var definition=(ObjectNode)json.readTree(Path.of("src/test/resources/demo/legacy-samples/small-definition.json"));
                var baseline=(ObjectNode)json.readTree(Path.of("src/test/resources/demo/legacy-samples/small-result.json"));
                if(variant==1)definition.putArray("reservedPeriodIds");
                if(variant==2)baseline.put("elapsedTimeMs",baseline.path("elapsedTimeMs").asLong()+1);
                if(variant==3)when(f.verification.build()).thenReturn("different-kernel-build");
                var successor=definition.deepCopy();successor.put("basedOnRevision",baseline.path("inputRevision").asString());
                when(f.drafts.compiledDefinition(any())).thenReturn(successor);
                f.in(owner,()->{
                    var current=f.store.load();var document=json.createObjectNode();
                    var accepted=document.putObject("acceptedBaseline");accepted.set("definition",definition);accepted.set("result",baseline);
                    var draft=document.putObject("repairDraft");draft.put("readyToSolve",true);draft.putArray("conflicts");draft.put("intentRevision","same-intent");
                    return f.store.resetAndReplace(current.version(),WorkspaceState.REPAIR_DRAFT,document);
                });
                f.start(owner,"QUICK");f.terminal(owner);assertEquals(WorkspaceState.REPAIR_PROPOSAL,f.current(owner).state());
            }
            assertEquals(4,f.calls.get());
        }
    }
    @Test void sharedCacheCountsCompleteKeysAndPayloadAndNeverRefreshesCompletionTtl() throws Exception {
        try(var ignored=Executors.newVirtualThreadPerTaskExecutor()) {
            var cache=new DemoArtifactCache(true,2000,Duration.ofMillis(200));
            try {
                cache.put("verification-key",new Object(),100);assertTrue(cache.residentBytes()>0);
                cache.put("solve-key",new Object(),1300);assertNull(cache.get("verification-key"));assertNotNull(cache.get("solve-key"));assertTrue(cache.residentBytes()<=2000);
                Thread.sleep(130);assertNotNull(cache.get("solve-key"));Thread.sleep(100);assertNull(cache.get("solve-key"));assertEquals(0,cache.residentBytes());
                cache.put("too-big",new Object(),2001);assertNull(cache.get("too-big"));
            }finally{cache.close();}
        }
    }
}
