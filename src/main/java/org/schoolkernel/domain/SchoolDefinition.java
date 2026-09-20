package org.schoolkernel.domain;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record SchoolDefinition(
        int schemaVersion,
        int catalogVersion,
        String schoolId,
        List<Subject> subjects,
        List<Teacher> teachers,
        List<Cohort> cohorts,
        List<Room> rooms,
        List<Period> periods,
        List<Lesson> lessons,
        Map<String, Long> softWeights) {

    public static final List<String> SOFT_CONSTRAINT_IDS = List.of(
            "soft.teacher-gap",
            "soft.series-same-day",
            "soft.undesirable-period",
            "soft.non-preferred-room");

    public static final List<String> HARD_CONSTRAINT_IDS = List.of(
            "hard.teacher-period",
            "hard.cohort-period",
            "hard.room-period",
            "hard.teacher-availability",
            "hard.cohort-availability",
            "hard.room-availability",
            "hard.room-capacity",
            "hard.room-capability",
            "hard.period-lock",
            "hard.room-lock");

    public record Subject(String id, String displayName) {}

    public record Teacher(
            String id,
            String displayName,
            Set<String> qualifiedSubjectIds,
            Set<String> availablePeriodIds,
            Set<String> undesirablePeriodIds) {}

    public record Cohort(
            String id,
            String displayName,
            int size,
            Set<String> availablePeriodIds,
            Set<String> undesirablePeriodIds) {}

    public record Room(
            String id,
            String displayName,
            int capacity,
            Set<String> capabilityIds,
            Set<String> availablePeriodIds) {}

    public record Period(
            String id,
            String displayName,
            DayOfWeek weekday,
            int order,
            LocalTime startTime,
            LocalTime endTime) {}

    public record Lesson(
            String id,
            String displayName,
            String subjectId,
            String cohortId,
            String teacherId,
            String seriesId,
            Set<String> requiredRoomCapabilityIds,
            Set<String> preferredRoomIds,
            Set<String> undesirablePeriodIds,
            String periodLock,
            String roomLock) {}
}
