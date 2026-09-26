package org.schoolkernel.solver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.schoolkernel.domain.SchoolDefinition;
import org.schoolkernel.domain.KernelCatalog;

public final class PreflightFeasibilityCheck {
    public List<ConstraintDiagnostic> findObviousFailures(SchoolDefinition definition) {
        Map<String, SchoolDefinition.Teacher> teachers = index(definition.teachers(), SchoolDefinition.Teacher::id);
        Map<String, SchoolDefinition.Cohort> cohorts = index(definition.cohorts(), SchoolDefinition.Cohort::id);
        var failures = new LinkedHashMap<String, List<List<String>>>();
        KernelCatalog.hardConstraintIds().forEach(id -> failures.put(id, new ArrayList<>()));

        for (var lesson : definition.lessons().stream().sorted(java.util.Comparator.comparing(SchoolDefinition.Lesson::id)).toList()) {
            var teacher = teachers.get(lesson.teacherId());
            var cohort = cohorts.get(lesson.cohortId());
            var candidatePeriods = definition.periods().stream()
                    .filter(period -> !definition.reservedPeriodIds().contains(period.id()))
                    .filter(period -> lesson.periodLock() == null || lesson.periodLock().equals(period.id()))
                    .filter(period -> teacher.availablePeriodIds().contains(period.id()))
                    .filter(period -> cohort.availablePeriodIds().contains(period.id()))
                    .toList();
            if (candidatePeriods.isEmpty()) {
                if (definition.periods().stream()
                        .filter(period -> !definition.reservedPeriodIds().contains(period.id()))
                        .noneMatch(period -> teacher.availablePeriodIds().contains(period.id()))) {
                    failures.get(KernelCatalog.TEACHER_AVAILABILITY.id()).add(List.of(lesson.id(), teacher.id()));
                }
                if (definition.periods().stream()
                        .filter(period -> !definition.reservedPeriodIds().contains(period.id()))
                        .noneMatch(period -> cohort.availablePeriodIds().contains(period.id()))) {
                    failures.get(KernelCatalog.COHORT_AVAILABILITY.id()).add(List.of(lesson.id(), cohort.id()));
                }
                if (lesson.periodLock() != null) {
                    failures.get(KernelCatalog.PERIOD_LOCK.id()).add(List.of(lesson.id(), lesson.periodLock()));
                }
                if (failures.values().stream().noneMatch(list -> list.stream().anyMatch(ids -> ids.contains(lesson.id())))) {
                    failures.get(KernelCatalog.TEACHER_AVAILABILITY.id())
                            .add(List.of(lesson.id(), teacher.id(), cohort.id()));
                }
                continue;
            }

            var selectableRooms = definition.rooms().stream()
                    .filter(room -> lesson.roomLock() == null || lesson.roomLock().equals(room.id()))
                    .toList();
            boolean capacityPossible = selectableRooms.stream().anyMatch(room -> room.capacity() >= cohort.size());
            boolean capabilityPossible = selectableRooms.stream()
                    .anyMatch(room -> room.capabilityIds().containsAll(lesson.requiredRoomCapabilityIds()));
            boolean combinationPossible = selectableRooms.stream().anyMatch(room -> room.capacity() >= cohort.size()
                    && room.capabilityIds().containsAll(lesson.requiredRoomCapabilityIds())
                    && candidatePeriods.stream().anyMatch(period -> room.availablePeriodIds().contains(period.id())));
            if (!combinationPossible) {
                if (!capacityPossible) {
                    failures.get(KernelCatalog.ROOM_CAPACITY.id()).add(List.of(lesson.id(), cohort.id()));
                }
                if (!capabilityPossible) {
                    failures.get(KernelCatalog.ROOM_CAPABILITY.id()).add(List.of(lesson.id()));
                }
                if (capacityPossible && capabilityPossible) {
                    failures.get(KernelCatalog.ROOM_AVAILABILITY.id()).add(List.of(lesson.id()));
                }
                if (lesson.roomLock() != null) {
                    failures.get(KernelCatalog.ROOM_LOCK.id()).add(List.of(lesson.id(), lesson.roomLock()));
                }
            }
        }

        return failures.entrySet().stream()
                .filter(entry -> !entry.getValue().isEmpty())
                .map(entry -> new ConstraintDiagnostic(
                        entry.getKey(),
                        entry.getValue().size(),
                        entry.getValue().stream().limit(1_000).toList()))
                .toList();
    }

    private static <T> Map<String, T> index(List<T> values, Function<T, String> id) {
        return values.stream().collect(Collectors.toMap(id, Function.identity()));
    }
}
