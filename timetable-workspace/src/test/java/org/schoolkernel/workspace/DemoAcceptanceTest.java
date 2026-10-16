package org.schoolkernel.workspace;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.node.ObjectNode;

class DemoAcceptanceTest {
    @Test void unavailableVerificationPreservesRetryableProposalAndCurrentThenPublishesExactly() throws Exception {
        var parent=new DemoPlanningTest();
        try(var f=parent.new Fixture()) {
            var owner=f.owner("");f.start(owner,"QUICK");f.release.countDown();f.terminal(owner);
            var current=f.current(owner);var proposal=current.document().path("proposal");
            var ordinary=mock(KernelVerifier.class);
            var service=new InitialPlanningService(f.store,f.mutation,mock(KernelPlanner.class),ordinary,new ManifestService(),parent.json);
            ReflectionTestUtils.setField(service,"demoVerification",f.verification);
            try {
                for(String code:java.util.List.of("DAILY_KERNEL_LIMIT","VERIFICATION_BUSY","KERNEL_UNAVAILABLE","KERNEL_VERIFICATION_FAILED")) {
                    doThrow(new WorkspaceProblem(HttpStatus.SERVICE_UNAVAILABLE,code,"Verification refused")).when(f.verification).verify(any());
                    assertEquals(code,assertThrows(WorkspaceProblem.class,()->f.in(owner,()->service.accept(current.etag()))).code());
                    assertEquals(current,f.current(owner));
                }
                doReturn(new KernelVerifier.Verification(proposal.path("definition").path("schoolId").asString(),proposal.path("successorDefinitionRevision").asString(),proposal.path("proposedTimetableRevision").asString())).when(f.verification).verify(any());
                var accepted=f.in(owner,()->service.accept(current.etag()));
                assertEquals(WorkspaceState.ACCEPTED_BASELINE,accepted.state());
                assertEquals(proposal.path("result"),accepted.document().path("acceptedBaseline").path("result"));
                assertEquals(current.document().path("initialDefinition"),accepted.document().path("acceptedBaseline").path("definition"));
                verifyNoInteractions(ordinary);
            }finally{service.close();}
        }
    }
    @Test void changedCompleteProposalIdentityInvalidatesInsteadOfPublishing() throws Exception {
        var parent=new DemoPlanningTest();
        try(var f=parent.new Fixture()) {
            var owner=f.owner("");f.start(owner,"QUICK");f.release.countDown();f.terminal(owner);
            var current=f.current(owner);ObjectNode changed=(ObjectNode)current.document().deepCopy();
            ((ObjectNode)changed.path("proposal").path("definition")).put("displayName","tampered");
            owner.aggregate=new WorkspaceAggregate(current.state(),current.version(),null,changed);
            var service=new InitialPlanningService(f.store,f.mutation,mock(KernelPlanner.class),mock(KernelVerifier.class),new ManifestService(),parent.json);
            ReflectionTestUtils.setField(service,"demoVerification",f.verification);
            try {
                assertEquals("STALE_PROPOSAL",assertThrows(WorkspaceProblem.class,()->f.in(owner,()->service.accept(current.etag()))).code());
                assertEquals(WorkspaceState.INITIAL_DRAFT,f.current(owner).state());assertFalse(f.current(owner).document().has("proposal"));assertFalse(f.current(owner).document().has("acceptedBaseline"));
            }finally{service.close();}
        }
    }
}
