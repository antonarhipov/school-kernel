package org.schoolkernel.workspace;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@org.springframework.context.annotation.Profile("!demo")
public class WorkspaceRecovery {
    private final WorkspaceStore repository;

    public WorkspaceRecovery(WorkspaceStore repository) {
        this.repository = repository;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void recoverInterruptedRun() {
        repository.recoverInterruptedInitialRun();
        repository.recoverInterruptedRepairRun();
    }
}
