package org.schoolkernel.solver;

import java.util.Set;

import ai.timefold.solver.core.api.domain.common.PlanningId;

public record RoomValue(
        @PlanningId String id,
        int capacity,
        Set<String> capabilityIds,
        Set<String> availablePeriodIds) {}
