package org.schoolkernel.solver;

import java.time.DayOfWeek;

import ai.timefold.solver.core.api.domain.common.PlanningId;

public record PeriodValue(@PlanningId String id, DayOfWeek weekday, int order) {}
