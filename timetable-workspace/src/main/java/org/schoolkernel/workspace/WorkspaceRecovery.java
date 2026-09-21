package org.schoolkernel.workspace;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class WorkspaceRecovery {
    private final WorkspaceRepository repository;

    public WorkspaceRecovery(WorkspaceRepository repository) {
        this.repository = repository;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void recoverInterruptedRun() {
        repository.recoverInterruptedInitialRun();
    }
}
