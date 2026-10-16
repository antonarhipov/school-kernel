package org.schoolkernel.workspace;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.mock.web.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

class DemoCustomDefinitionTest {
    final JsonMapper json = JsonMapper.builder().build();
    ObjectNode sample() throws Exception { return (ObjectNode) json.readTree(Path.of("src/test/resources/demo/legacy-samples/small-definition.json")); }
    DemoDefinitionLimits limits(long w, int l) { return new DemoDefinitionLimits(w, l); }
    @Test void exactIndependentInclusiveComplexityAndStrictDocumentGates() throws Exception {
        var small = sample();
        limits(12,2).check(small);
        assertEquals("DEFINITION_COMPLEXITY_LIMIT",assertThrows(WorkspaceProblem.class,()->limits(11,2).check(small)).code());
        assertEquals("DEFINITION_COMPLEXITY_LIMIT",assertThrows(WorkspaceProblem.class,()->limits(12,1).check(small)).code());
        var huge = small.deepCopy(); var lessons=huge.putArray("lessons"); for(int i=0;i<2501;i++)lessons.addObject();
        assertEquals("DEFINITION_COMPLEXITY_LIMIT",assertThrows(WorkspaceProblem.class,()->limits(11000000,2500).check(huge)).code());
        for(String field: List.of("lessons","rooms","periods")) {
            var bad=small.deepCopy();bad.putNull(field);
            assertTrue(assertThrows(WorkspaceProblem.class,()->limits(11000000,2500).check(bad)).getMessage().contains("/"+field));
        }
        for(String text:List.of("{}{}","{\"x\":1,\"x\":2}","[]","null","{"))
            assertThrows(WorkspaceProblem.class,()->limits(11000000,2500).read(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8))));
        byte[] badUtf8={(byte)0xc3,0x28};assertThrows(WorkspaceProblem.class,()->limits(12,2).read(new ByteArrayInputStream(badUtf8)));
        String text=json.writeValueAsString(small);
        String boundary=text+" ".repeat(DemoDefinitionLimits.DOCUMENT_BYTES-text.getBytes(StandardCharsets.UTF_8).length);
        assertEquals(small,limits(12,2).read(new ByteArrayInputStream(boundary.getBytes(StandardCharsets.UTF_8))));
        assertEquals("DEFINITION_TOO_LARGE",assertThrows(WorkspaceProblem.class,()->limits(12,2).read(new ByteArrayInputStream((boundary+" ").getBytes(StandardCharsets.UTF_8)))).code());
        for(String field:List.of("unknown","basedOnRevision")) {
            var bad=small.deepCopy();bad.put(field,"sha256:"+"0".repeat(64));
            assertTrue(assertThrows(WorkspaceProblem.class,()->limits(12,2).check(bad)).getMessage().contains(field));
        }
        var optional=small.deepCopy();optional.putArray("reservedPeriodIds");
        assertEquals(optional,limits(12,2).read(new ByteArrayInputStream(json.writeValueAsBytes(optional))));
        ((ObjectNode)optional.path("teachers").get(0)).putNull("availablePeriodIds");
        assertThrows(WorkspaceProblem.class,()->limits(12,2).check(optional));
    }

    @Test void reservationsAccrueActualProcessTimeAndCarryAcrossUtcMidnight() {
        MutableClock clock=new MutableClock(Instant.parse("2026-10-16T23:59:50Z"));
        var budget=new DemoKernelAllowance(Duration.ofSeconds(100),clock);
        var r=budget.reserve(Duration.ofSeconds(40));clock.advance(5);
        assertEquals(Duration.ofSeconds(40).toNanos(),budget.snapshot().reservedNanos());
        assertEquals(0,budget.snapshot().consumedNanos());r.started(clock.instant());clock.advance(3);
        assertEquals(new DemoKernelAllowance.Snapshot(Duration.ofSeconds(3).toNanos(),Duration.ofSeconds(37).toNanos(),Duration.ofSeconds(60).toNanos()),budget.snapshot());
        clock.advance(7); // 00:00:05; five seconds today, ten seconds total process life
        assertEquals(new DemoKernelAllowance.Snapshot(Duration.ofSeconds(5).toNanos(),Duration.ofSeconds(30).toNanos(),Duration.ofSeconds(65).toNanos()),budget.snapshot());
        assertThrows(WorkspaceProblem.class,()->budget.reserve(Duration.ofSeconds(66)));
        r.close();assertEquals(0,budget.snapshot().reservedNanos());assertEquals(Duration.ofSeconds(95).toNanos(),budget.snapshot().remainingNanos());
        var waiting=budget.reserve(Duration.ofSeconds(90));waiting.close();assertEquals(Duration.ofSeconds(95).toNanos(),budget.snapshot().remainingNanos());
    }

    DemoVerificationService service(KernelVerifier verifier,DemoKernelAllowance budget,boolean reuse,long max,Duration ttl,Duration wait) throws Exception {
        return new DemoVerificationService(verifier,budget,reuse,max,ttl,wait,Duration.ofSeconds(30),"../kernel-cli/target/school-kernel.jar");
    }
    @Test void evidenceUsesCompleteDocumentsModeAndBuildWithFixedTtlAndCeiling() throws Exception {
        var verifier=mock(KernelVerifier.class);var count=new AtomicInteger();
        when(verifier.verify(any(),any(),any(),any())).thenAnswer(call->{count.incrementAndGet();return new KernelVerifier.Verification("school","sha256:"+"a".repeat(64),null);});
        var budget=new DemoKernelAllowance(Duration.ofHours(4));
        var cache=service(verifier,budget,true,67108864,Duration.ofHours(1),Duration.ZERO);
        var a=sample();var initial=new ImportDocuments(a,null,null,ImportDocuments.ImportMode.INITIAL_DEFINITION);
        cache.verify(initial);cache.verify(initial);assertEquals(1,count.get());
        var reordered=a.deepCopy();var first=reordered.withArray("lessons").remove(0);reordered.withArray("lessons").add(first);
        cache.verify(new ImportDocuments(reordered,null,null,initial.mode()));assertEquals(2,count.get());
        var optional=a.deepCopy();optional.putArray("reservedPeriodIds");cache.verify(new ImportDocuments(optional,null,null,initial.mode()));assertEquals(3,count.get());
        cache.verify(new ImportDocuments(a,null,null,ImportDocuments.ImportMode.SUCCESSOR_DEFINITION));assertEquals(4,count.get());
        cache.verify(new ImportDocuments(a,json.createObjectNode().put("result",1),null,ImportDocuments.ImportMode.ACCEPTED_BASELINE));
        cache.verify(new ImportDocuments(a,json.createObjectNode().put("result",2),null,ImportDocuments.ImportMode.ACCEPTED_BASELINE));assertEquals(6,count.get());
        var disabled=service(verifier,budget,false,67108864,Duration.ofHours(1),Duration.ZERO);disabled.verify(initial);disabled.verify(initial);assertEquals(8,count.get());
        var tiny=service(verifier,budget,true,1,Duration.ofHours(1),Duration.ZERO);tiny.verify(initial);tiny.verify(initial);assertEquals(10,count.get());
        var expired=service(verifier,budget,true,67108864,Duration.ofNanos(1),Duration.ZERO);expired.verify(initial);expired.verify(initial);assertEquals(12,count.get());
        assertEquals(0,budget.snapshot().consumedNanos());assertEquals(0,budget.snapshot().reservedNanos());
        doThrow(new WorkspaceProblem(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,"KERNEL_UNAVAILABLE","Failed")).when(verifier).verify(any(),any(),any(),any());
        assertThrows(WorkspaceProblem.class,()->disabled.verify(initial));assertThrows(WorkspaceProblem.class,()->disabled.verify(initial));assertEquals(0,budget.snapshot().reservedNanos());
        // New kernel code changes server-computed build identity; each service instance clears visitor evidence on restart.
        assertNotEquals(DemoSamples.kernelBuild(Path.of("../kernel-cli/target/school-kernel.jar")),"untrusted-client-build");
    }
    @Test void successfulHitStillWorksAtExhaustionAndPhysicalFailureSettlesActualTime() throws Exception {
        var verifier=mock(KernelVerifier.class);var budget=new DemoKernelAllowance(Duration.ofSeconds(34));
        when(verifier.verify(any(),any(),any(),any())).thenReturn(new KernelVerifier.Verification("school","sha256:"+"a".repeat(64),null));
        var cache=service(verifier,budget,true,67108864,Duration.ofHours(1),Duration.ZERO);
        var docs=new ImportDocuments(sample(),null,null,ImportDocuments.ImportMode.INITIAL_DEFINITION);cache.verify(docs);
        try(var held=budget.reserve(Duration.ofSeconds(34))){assertEquals(0,budget.snapshot().remainingNanos());assertNotNull(cache.verify(docs));}
        verify(verifier,times(1)).verify(any(),any(),any(),any());cache.close();
        var processRef=new java.util.concurrent.atomic.AtomicReference<Process>();
        var real=new KernelVerifier("ignored",new KernelProcessLauncher(){
            @Override public Process start(List<String> args)throws java.io.IOException {
                Process p=new ProcessBuilder("/bin/sleep","60").start();processRef.set(p);return p;
            }
        },Files.createTempDirectory("uc5-watchdog-"));
        var physical=new DemoKernelAllowance(Duration.ofSeconds(10));
        var bounded=new DemoVerificationService(real,physical,false,67108864,Duration.ofHours(1),Duration.ZERO,Duration.ofMillis(100),"../kernel-cli/target/school-kernel.jar");
        long wall=System.nanoTime();assertThrows(WorkspaceProblem.class,()->bounded.verify(docs));
        assertFalse(processRef.get().isAlive());assertEquals(0,physical.snapshot().reservedNanos());
        assertTrue(physical.snapshot().consumedNanos()>=Duration.ofMillis(90).toNanos());
        assertTrue(physical.snapshot().consumedNanos()<System.nanoTime()-wall);bounded.close();
    }

    @Test void busyAndExhaustedVerificationStartNoThirdProcessAndReleaseReservations() throws Exception {
        var verifier=mock(KernelVerifier.class);var entered=new CountDownLatch(2);var release=new CountDownLatch(1);
        when(verifier.verify(any(),any(),any(),any())).thenAnswer(call->{entered.countDown();release.await();return new KernelVerifier.Verification("school","sha256:"+"a".repeat(64),null);});
        var budget=new DemoKernelAllowance(Duration.ofHours(4));var service=service(verifier,budget,false,67108864,Duration.ofHours(1),Duration.ofMillis(20));
        var docs=new ImportDocuments(sample(),null,null,ImportDocuments.ImportMode.INITIAL_DEFINITION);
        var executor=Executors.newVirtualThreadPerTaskExecutor();
        try {
            var one=executor.submit(()->service.verify(docs));var two=executor.submit(()->service.verify(docs));assertTrue(entered.await(5,TimeUnit.SECONDS));
            assertEquals("VERIFICATION_BUSY",assertThrows(WorkspaceProblem.class,()->service.verify(docs)).code());
            assertEquals(Duration.ofSeconds(68).toNanos(),budget.snapshot().reservedNanos());
            release.countDown();one.get();two.get();assertEquals(0,budget.snapshot().reservedNanos());
        }finally{release.countDown();executor.close();}
        var exhausted=service(verifier,new DemoKernelAllowance(Duration.ofSeconds(33)),true,67108864,Duration.ofHours(1),Duration.ZERO);
        assertEquals("DAILY_KERNEL_LIMIT",assertThrows(WorkspaceProblem.class,()->exhausted.verify(docs)).code());
        verify(verifier,times(2)).verify(any(),any(),any(),any());
    }

    @Test void everyLifecycleRefusalStaleCompletionAndSessionEndPreserveCompleteWorkspace() throws Exception {
        var beans=new StaticListableBeanFactory();
        var owners=new DemoWorkspaces(new DemoSettings(List.of("demo.example"),List.of(),false,Duration.ofHours(1),Duration.ofHours(8),50),json,beans.getBeanProvider(KernelPlanner.class));
        var store=new DemoWorkspaceStore(owners);var mutation=new WorkspaceMutation(store);var verifier=mock(DemoVerificationService.class);
        var controller=new DemoDefinitionController(store,mutation,limits(11000000,2500),verifier,mock(WorkspaceController.class),json,200,5,60);
        var owner=owners.open(new MockHttpSession(),true);
        try { owners.bound(owner,()->{
            try {
                for(var state:WorkspaceState.values()) {
                    if(state==WorkspaceState.DEFINITION_DRAFT)continue;
                    var current=store.load();var document=json.createObjectNode().put("exact","kept "+state);document.putObject("acceptedBaseline").set("definition",sample());
                    store.replace(current.version(),current.state(),state,document);var before=store.load();
                    var request=new MockHttpServletRequest();request.setContent(json.writeValueAsBytes(sample()));
                    assertEquals("STALE_WORKSPACE_VERSION",assertThrows(WorkspaceProblem.class,()->controller.use(request,"\"ws-0\"",true,"custom")).code());
                    boolean solving=state.name().startsWith("SOLVING_");
                    if(state!=WorkspaceState.EMPTY)assertEquals(solving?"INVALID_WORKSPACE_TRANSITION":"REPLACEMENT_CONFIRMATION_REQUIRED",assertThrows(WorkspaceProblem.class,()->controller.use(request,before.etag(),false,"custom")).code());
                    doThrow(new WorkspaceProblem(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,"KERNEL_UNAVAILABLE","not used")).when(verifier).verify(any());
                    assertEquals(solving?"INVALID_WORKSPACE_TRANSITION":"KERNEL_UNAVAILABLE",assertThrows(WorkspaceProblem.class,()->controller.use(request,before.etag(),true,"custom")).code());
                    assertEquals(before,store.load());
                }
                var old=store.load();store.replace(old.version(),old.state(),WorkspaceState.ACCEPTED_BASELINE,old.document());var before=store.load();
                var request=new MockHttpServletRequest();request.setContent(json.writeValueAsBytes(sample()));
                doAnswer(call->{var now=store.load();store.replace(now.version(),now.state(),now.state(),json.createObjectNode().put("newer","keep"));return new KernelVerifier.Verification("school","sha256:"+"a".repeat(64),null);}).when(verifier).verify(any());
                assertEquals("STALE_WORKSPACE_VERSION",assertThrows(WorkspaceProblem.class,()->controller.use(request,before.etag(),true,"custom")).code());
                assertEquals("keep",store.load().document().path("newer").asString());
                var fresh=store.load();request.setContent(json.writeValueAsBytes(sample()));
                doAnswer(call->{owner.ended=true;return new KernelVerifier.Verification("school","sha256:"+"a".repeat(64),null);}).when(verifier).verify(any());
                assertEquals("SESSION_ENDED",assertThrows(WorkspaceProblem.class,()->controller.use(request,fresh.etag(),true,"custom")).code());
            } catch(Exception e){throw new RuntimeException(e);}
        }); }finally { owners.close(); }
    }
    static final class MutableClock extends Clock {
        Instant now;MutableClock(Instant now){this.now=now;}void advance(long seconds){now=now.plusSeconds(seconds);}
        public ZoneId getZone(){return ZoneOffset.UTC;}public Clock withZone(ZoneId zone){return this;}public Instant instant(){return now;}
    }
}
