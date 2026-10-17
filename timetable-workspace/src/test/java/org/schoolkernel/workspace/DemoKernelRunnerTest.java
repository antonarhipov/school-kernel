package org.schoolkernel.workspace;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.*;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

class DemoKernelRunnerTest {
    final JsonMapper json=JsonMapper.builder().build();
    ObjectNode definition()throws Exception{return(ObjectNode)json.readTree(Path.of("src/test/resources/demo/legacy-samples/small-definition.json"));}
    ObjectNode result()throws Exception{return(ObjectNode)json.readTree(Path.of("src/test/resources/demo/legacy-samples/small-result.json"));}
    DemoSearchSettings settings(int thorough){return new DemoSearchSettings(5,thorough,200,8,Duration.ofMinutes(2),Duration.ofMinutes(2),Duration.ofSeconds(1));}
    @Test void bothOperationsPassActualDurationsThroughQualified120SecondsAndBindCompleteVerification()throws Exception {
        for(String operation:List.of("INITIAL","REPAIR"))for(int seconds:List.of(5,60,120)) for(String termination:List.of("TIME_LIMIT","SEARCH_EXHAUSTED")) {
            var verification=mock(DemoVerificationService.class);var accepted=result();var definition=definition();if(operation.equals("REPAIR"))definition.put("basedOnRevision",accepted.path("inputRevision").asString());
            var commands=new AtomicReference<List<String>>();
            var launcher=new KernelProcessLauncher(){public Process start(List<String> args)throws java.io.IOException {
                commands.set(args);var out=resultUnchecked();out.put("terminationReason",termination);out.put("correlationId",args.get(args.indexOf("--correlation-id")+1));((ObjectNode)out.path("limit")).put("duration",args.get(args.indexOf("--time-limit")+1));
                if(operation.equals("REPAIR")){var changes=out.putObject("changeReport");for(String field:List.of("additions","cancellations","teacherChanges","forcedMoves","periodMoves","roomOnlyMoves"))changes.putArray(field);}
                Files.write(Path.of(args.get(args.indexOf("--output")+1)),json.writeValueAsBytes(out));return new ProcessBuilder("/usr/bin/true").start();
            }};
            when(verification.verifyReserved(any(),any())).thenAnswer(call->{ImportDocuments docs=call.getArgument(0);assertEquals(definition,docs.definition());assertEquals(ImportDocuments.ImportMode.ACCEPTED_BASELINE,docs.mode());assertEquals(Duration.ofSeconds(seconds).toString(),docs.result().path("limit").path("duration").asString());return new KernelVerifier.Verification(definition.path("schoolId").asString(),docs.result().path("inputRevision").asString(),docs.result().path("timetableRevision").asString());});
            var runner=new DemoKernelRunner("kernel",launcher,verification,settings(120));var budget=new DemoKernelAllowance(Duration.ofHours(4));
            try(var held=budget.reserve(Duration.ofSeconds(160))){var outcome=runner.run("computation",new DemoKernelRunner.Input(operation,definition,operation.equals("REPAIR")?definition():null,operation.equals("REPAIR")?accepted:null,Duration.ofSeconds(seconds)),held,()->{});assertNotNull(outcome.artifact());assertEquals(operation.equals("INITIAL")?"plan":"replan",commands.get().get(1));assertEquals("0",commands.get().get(commands.get().indexOf("--seed")+1));}
            assertEquals(0,budget.snapshot().reservedNanos());
            assertFalse(Files.exists(Path.of(commands.get().get(commands.get().indexOf("--definition")+1)).getParent()));
        }
        assertThrows(IllegalArgumentException.class,()->settings(121));
    }
    ObjectNode resultUnchecked(){try{return result();}catch(Exception e){throw new RuntimeException(e);}}
    @Test void mismatchedBudgetSeedCorrelationTerminationAndUnverifiedResultsNeverBecomeArtifacts()throws Exception {
        for(String corruption:List.of("duration","seed","correlation","termination","verification")) {
            var verification=mock(DemoVerificationService.class);
            when(verification.verifyReserved(any(),any())).thenThrow(new WorkspaceProblem(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,"KERNEL_UNAVAILABLE","failure"));
            var launcher=new KernelProcessLauncher(){public Process start(List<String> args)throws java.io.IOException {
                var out=resultUnchecked();out.put("correlationId",args.get(args.indexOf("--correlation-id")+1));
                switch(corruption){case "duration"->((ObjectNode)out.path("limit")).put("duration","PT2M");case "seed"->out.remove("seed");case "correlation"->out.put("correlationId","foreign");case "termination"->out.put("terminationReason","STEP_LIMIT");}
                Files.write(Path.of(args.get(args.indexOf("--output")+1)),json.writeValueAsBytes(out));return new ProcessBuilder("/usr/bin/true").start();
            }};
            var runner=new DemoKernelRunner("kernel",launcher,verification,settings(60));var budget=new DemoKernelAllowance(Duration.ofHours(4));
            try(var held=budget.reserve(Duration.ofSeconds(53))){var outcome=runner.run("computation",new DemoKernelRunner.Input("INITIAL",definition(),null,null,Duration.ofSeconds(5)),held,()->{});assertNull(outcome.artifact());assertEquals("REJECTED_OUTPUT",outcome.code());}
            if(!corruption.equals("verification"))verifyNoInteractions(verification);
            assertEquals(0,budget.snapshot().reservedNanos());
        }
    }
    @Test void lastSubscriberInterruptForcesNoncooperativeProcessAfterGraceAndSettlesActualLifetime()throws Exception {
        AtomicReference<Process> process=new AtomicReference<>();CountDownLatch launched=new CountDownLatch(1);AtomicReference<Path> directory=new AtomicReference<>();
        var launcher=new KernelProcessLauncher(){public Process start(List<String> args)throws java.io.IOException {
            directory.set(Path.of(args.get(args.indexOf("--definition")+1)).getParent());var p=new ProcessBuilder("/bin/sh","-c","trap '' TERM; exec /bin/sleep 60").start();process.set(p);launched.countDown();return p;
        }};
        var budget=new DemoKernelAllowance(Duration.ofSeconds(10));var held=budget.reserve(Duration.ofSeconds(9));
        var runner=new DemoKernelRunner("kernel",launcher,mock(DemoVerificationService.class),settings(60));var input=new DemoKernelRunner.Input("INITIAL",definition(),null,null,Duration.ofSeconds(5));
        AtomicReference<DemoKernelRunner.Outcome> outcome=new AtomicReference<>();
        Thread worker=Thread.ofVirtual().start(()->{try(held){outcome.set(runner.run("forced",input,held,()->{}));}});
        assertTrue(launched.await(3,TimeUnit.SECONDS));Thread.sleep(100);worker.interrupt();Thread.sleep(100);
        assertTrue(process.get().isAlive());assertTrue(budget.snapshot().reservedNanos()>0);worker.join(6000);
        assertFalse(worker.isAlive());assertFalse(process.get().isAlive());assertFalse(Files.exists(directory.get()));
        assertEquals("CANCELLED",outcome.get().code());assertEquals(0,budget.snapshot().reservedNanos());assertTrue(budget.snapshot().consumedNanos()>=Duration.ofSeconds(2).toNanos());assertTrue(budget.snapshot().consumedNanos()<Duration.ofSeconds(5).toNanos());
    }
}
