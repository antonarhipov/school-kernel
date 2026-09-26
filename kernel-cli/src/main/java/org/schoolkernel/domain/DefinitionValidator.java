package org.schoolkernel.domain;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.schoolkernel.contract.SchoolDefinitionDto;
import org.schoolkernel.contract.SchoolDefinitionDto.CohortDto;
import org.schoolkernel.contract.SchoolDefinitionDto.LessonDto;
import org.schoolkernel.contract.SchoolDefinitionDto.PeriodDto;
import org.schoolkernel.contract.SchoolDefinitionDto.RoomDto;
import org.schoolkernel.contract.SchoolDefinitionDto.SoftConstraintOverrideDto;
import org.schoolkernel.contract.SchoolDefinitionDto.SubjectDto;
import org.schoolkernel.contract.SchoolDefinitionDto.TeacherDto;

public final class DefinitionValidator {
    public record Outcome(SchoolDefinition definition, ValidationReport report) {}

    public Outcome validateForPlan(SchoolDefinitionDto input) {
        return validate(input, false);
    }

    public Outcome validateForReplan(SchoolDefinitionDto input) {
        return validate(input, true);
    }

    private Outcome validate(SchoolDefinitionDto input, boolean replan) {
        var errors = new ArrayList<ValidationError>();
        if (!replan && input.basedOnRevision() != null) {
            error(errors, "/basedOnRevision", List.of(), "basedOnRevision is forbidden for initial planning");
        } else if (replan && input.basedOnRevision() == null) {
            error(errors, "/basedOnRevision", List.of(), "basedOnRevision is required for replanning");
        }

        checkDisplayNames(input, errors);
        checkUniqueIds("subjects", input.subjects(), SubjectDto::id, errors);
        checkUniqueIds("teachers", input.teachers(), TeacherDto::id, errors);
        checkUniqueIds("cohorts", input.cohorts(), CohortDto::id, errors);
        checkUniqueIds("rooms", input.rooms(), RoomDto::id, errors);
        checkUniqueIds("periods", input.periods(), PeriodDto::id, errors);
        checkUniqueIds("lessons", input.lessons(), LessonDto::id, errors);

        var subjectIds = ids(input.subjects(), SubjectDto::id);
        var teacherIds = ids(input.teachers(), TeacherDto::id);
        var cohortIds = ids(input.cohorts(), CohortDto::id);
        var roomIds = ids(input.rooms(), RoomDto::id);
        var periodIds = ids(input.periods(), PeriodDto::id);

        checkReferences("/reservedPeriodIds", input.schoolId(), input.reservedPeriodIds(),
                periodIds, "period", errors);
        checkTeacherReferences(input.teachers(), subjectIds, periodIds, errors);
        checkCohortReferences(input.cohorts(), periodIds, errors);
        checkRoomReferences(input.rooms(), periodIds, errors);
        checkPeriods(input.periods(), errors);
        checkLessons(input, subjectIds, teacherIds, cohortIds, roomIds, periodIds, errors);
        checkSeries(input.lessons(), errors);
        checkOverrides(input.softConstraintOverrides(), input, errors);
        checkLockConflicts(input, errors);

        var report = ValidationReport.from(errors);
        if (!report.isValid()) {
            return new Outcome(null, report);
        }
        return new Outcome(toDomain(input), report);
    }

    private static void checkDisplayNames(SchoolDefinitionDto input, List<ValidationError> errors) {
        if (input.displayName().isBlank()) {
            error(errors, "/displayName", List.of(input.schoolId()), "displayName must not be blank");
        }
        checkNames("subjects", input.subjects(), SubjectDto::id, SubjectDto::displayName, errors);
        checkNames("teachers", input.teachers(), TeacherDto::id, TeacherDto::displayName, errors);
        checkNames("cohorts", input.cohorts(), CohortDto::id, CohortDto::displayName, errors);
        checkNames("rooms", input.rooms(), RoomDto::id, RoomDto::displayName, errors);
        checkNames("periods", input.periods(), PeriodDto::id, PeriodDto::displayName, errors);
        checkNames("lessons", input.lessons(), LessonDto::id, LessonDto::displayName, errors);
    }

    private static <T> void checkNames(
            String collection,
            List<T> values,
            Function<T, String> id,
            Function<T, String> name,
            List<ValidationError> errors) {
        for (int i = 0; i < values.size(); i++) {
            T value = values.get(i);
            if (name.apply(value).isBlank()) {
                error(errors, "/" + collection + "/" + i + "/displayName", List.of(id.apply(value)),
                        "displayName must not be blank");
            }
        }
    }

    private static <T> void checkUniqueIds(
            String collection,
            List<T> values,
            Function<T, String> id,
            List<ValidationError> errors) {
        var seen = new HashSet<String>();
        for (int i = 0; i < values.size(); i++) {
            String current = id.apply(values.get(i));
            if (!seen.add(current)) {
                error(errors, "/" + collection + "/" + i + "/id", List.of(current),
                        "duplicate ID in " + collection);
            }
        }
    }

    private static <T> Set<String> ids(List<T> values, Function<T, String> id) {
        var result = new LinkedHashSet<String>();
        values.forEach(value -> result.add(id.apply(value)));
        return result;
    }

    private static void checkTeacherReferences(
            List<TeacherDto> teachers,
            Set<String> subjectIds,
            Set<String> periodIds,
            List<ValidationError> errors) {
        for (int i = 0; i < teachers.size(); i++) {
            var teacher = teachers.get(i);
            checkReferences("/teachers/" + i + "/qualifiedSubjectIds", teacher.id(), teacher.qualifiedSubjectIds(),
                    subjectIds, "subject", errors);
            checkReferences("/teachers/" + i + "/availablePeriodIds", teacher.id(), teacher.availablePeriodIds(),
                    periodIds, "period", errors);
            checkReferences("/teachers/" + i + "/undesirablePeriodIds", teacher.id(), teacher.undesirablePeriodIds(),
                    periodIds, "period", errors);
        }
    }

    private static void checkCohortReferences(
            List<CohortDto> cohorts,
            Set<String> periodIds,
            List<ValidationError> errors) {
        for (int i = 0; i < cohorts.size(); i++) {
            var cohort = cohorts.get(i);
            checkReferences("/cohorts/" + i + "/availablePeriodIds", cohort.id(), cohort.availablePeriodIds(),
                    periodIds, "period", errors);
            checkReferences("/cohorts/" + i + "/undesirablePeriodIds", cohort.id(), cohort.undesirablePeriodIds(),
                    periodIds, "period", errors);
        }
    }

    private static void checkRoomReferences(
            List<RoomDto> rooms,
            Set<String> periodIds,
            List<ValidationError> errors) {
        for (int i = 0; i < rooms.size(); i++) {
            var room = rooms.get(i);
            checkReferences("/rooms/" + i + "/availablePeriodIds", room.id(), room.availablePeriodIds(),
                    periodIds, "period", errors);
        }
    }

    private static void checkReferences(
            String location,
            String ownerId,
            List<String> references,
            Set<String> targets,
            String targetType,
            List<ValidationError> errors) {
        if (references == null) {
            return;
        }
        for (int i = 0; i < references.size(); i++) {
            String reference = references.get(i);
            if (!targets.contains(reference)) {
                error(errors, location + "/" + i, List.of(ownerId, reference),
                        "unknown " + targetType + " reference");
            }
        }
    }

    private static void checkPeriods(List<PeriodDto> periods, List<ValidationError> errors) {
        var orderOwners = new HashMap<String, String>();
        for (int i = 0; i < periods.size(); i++) {
            var period = periods.get(i);
            String orderKey = period.weekday() + ":" + period.order();
            String previous = orderOwners.putIfAbsent(orderKey, period.id());
            if (previous != null) {
                error(errors, "/periods/" + i + "/order", List.of(previous, period.id()),
                        "period order must be unique within a weekday");
            }
            if (period.startTime() != null) {
                try {
                    if (!LocalTime.parse(period.endTime()).isAfter(LocalTime.parse(period.startTime()))) {
                        error(errors, "/periods/" + i, List.of(period.id()),
                                "period endTime must be after startTime");
                    }
                } catch (DateTimeParseException exception) {
                    error(errors, "/periods/" + i, List.of(period.id()),
                            "period display times must use valid local time values");
                }
            }
        }
        for (int left = 0; left < periods.size(); left++) {
            var first = periods.get(left);
            if (first.startTime() == null) {
                continue;
            }
            for (int right = left + 1; right < periods.size(); right++) {
                var second = periods.get(right);
                if (!first.weekday().equals(second.weekday()) || second.startTime() == null) {
                    continue;
                }
                try {
                    LocalTime firstStart = LocalTime.parse(first.startTime());
                    LocalTime firstEnd = LocalTime.parse(first.endTime());
                    LocalTime secondStart = LocalTime.parse(second.startTime());
                    LocalTime secondEnd = LocalTime.parse(second.endTime());
                    if (firstStart.isBefore(secondEnd) && secondStart.isBefore(firstEnd)) {
                        error(errors, "/periods/" + right, List.of(first.id(), second.id()),
                                "period times overlap within a weekday");
                    }
                } catch (DateTimeParseException ignored) {
                    // The malformed value is reported on its own period above.
                }
            }
        }
    }

    private static void checkLessons(
            SchoolDefinitionDto input,
            Set<String> subjectIds,
            Set<String> teacherIds,
            Set<String> cohortIds,
            Set<String> roomIds,
            Set<String> periodIds,
            List<ValidationError> errors) {
        var teachers = index(input.teachers(), TeacherDto::id);
        for (int i = 0; i < input.lessons().size(); i++) {
            var lesson = input.lessons().get(i);
            checkReference("/lessons/" + i + "/subjectId", lesson.id(), lesson.subjectId(), subjectIds, "subject", errors);
            checkReference("/lessons/" + i + "/teacherId", lesson.id(), lesson.teacherId(), teacherIds, "teacher", errors);
            checkReference("/lessons/" + i + "/cohortId", lesson.id(), lesson.cohortId(), cohortIds, "cohort", errors);
            checkReferences("/lessons/" + i + "/preferredRoomIds", lesson.id(), lesson.preferredRoomIds(), roomIds,
                    "room", errors);
            if (lesson.periodLock() != null) {
                checkReference("/lessons/" + i + "/periodLock", lesson.id(), lesson.periodLock(), periodIds, "period", errors);
            }
            if (lesson.roomLock() != null) {
                checkReference("/lessons/" + i + "/roomLock", lesson.id(), lesson.roomLock(), roomIds, "room", errors);
            }
            checkReferences("/lessons/" + i + "/undesirablePeriodIds", lesson.id(), lesson.undesirablePeriodIds(),
                    periodIds, "period", errors);

            var teacher = teachers.get(lesson.teacherId());
            if (teacher != null && subjectIds.contains(lesson.subjectId())
                    && !teacher.qualifiedSubjectIds().contains(lesson.subjectId())) {
                error(errors, "/lessons/" + i + "/teacherId", List.of(lesson.id(), teacher.id(), lesson.subjectId()),
                        "teacher is not qualified for the lesson subject");
            }
        }
    }

    private static void checkReference(
            String location,
            String ownerId,
            String reference,
            Set<String> targets,
            String targetType,
            List<ValidationError> errors) {
        if (!targets.contains(reference)) {
            error(errors, location, List.of(ownerId, reference), "unknown " + targetType + " reference");
        }
    }

    private static void checkSeries(List<LessonDto> lessons, List<ValidationError> errors) {
        var series = new HashMap<String, LessonDto>();
        for (int i = 0; i < lessons.size(); i++) {
            var lesson = lessons.get(i);
            if (lesson.seriesId() == null) {
                continue;
            }
            var first = series.putIfAbsent(lesson.seriesId(), lesson);
            if (first != null && (!first.subjectId().equals(lesson.subjectId())
                    || !first.cohortId().equals(lesson.cohortId()))) {
                error(errors, "/lessons/" + i + "/seriesId", List.of(first.id(), lesson.id(), lesson.seriesId()),
                        "all lessons in a series must share subject and cohort");
            }
        }
    }

    private static void checkOverrides(
            List<SoftConstraintOverrideDto> overrides,
            SchoolDefinitionDto input,
            List<ValidationError> errors) {
        var seen = new HashSet<String>();
        if (overrides != null) {
            for (int i = 0; i < overrides.size(); i++) {
                var override = overrides.get(i);
                if (!seen.add(override.constraintId())) {
                    error(errors, "/softConstraintOverrides/" + i + "/constraintId", List.of(override.constraintId()),
                            "soft constraint may be overridden only once");
                }
            }
        }

        var weights = effectiveWeights(overrides, input.catalogVersion());
        try {
            long gapMaximum = Math.multiplyExact((long) input.teachers().size(), input.periods().size());
            long cohortGapMaximum = Math.multiplyExact((long) input.cohorts().size(), input.periods().size());
            long lessonMaximum = input.lessons().size();
            long weekBalanceMaximum = Math.multiplyExact(21L, lessonMaximum);
            long undesirableMaximum = Math.multiplyExact(3L, lessonMaximum);
            long maximum = 0;
            maximum = Math.addExact(maximum,
                    Math.multiplyExact(gapMaximum, weights.get(KernelCatalog.TEACHER_GAP.id())));
            maximum = Math.addExact(maximum,
                    Math.multiplyExact(cohortGapMaximum, weights.getOrDefault(KernelCatalog.COHORT_GAP.id(), 0L)));
            maximum = Math.addExact(maximum,
                    Math.multiplyExact(lessonMaximum, weights.getOrDefault(KernelCatalog.COHORT_LATE_START.id(), 0L)));
            maximum = Math.addExact(maximum,
                    Math.multiplyExact(weekBalanceMaximum,
                            weights.getOrDefault(KernelCatalog.COHORT_WEEK_BALANCE.id(), 0L)));
            maximum = Math.addExact(maximum,
                    Math.multiplyExact(lessonMaximum, weights.get(KernelCatalog.SERIES_SAME_DAY.id())));
            maximum = Math.addExact(maximum,
                    Math.multiplyExact(undesirableMaximum, weights.get(KernelCatalog.UNDESIRABLE_PERIOD.id())));
            Math.addExact(maximum,
                    Math.multiplyExact(lessonMaximum, weights.get(KernelCatalog.NON_PREFERRED_ROOM.id())));
        } catch (ArithmeticException exception) {
            error(errors, "/softConstraintOverrides", List.of(),
                    "maximum possible weighted preference penalty exceeds signed 64-bit range");
        }
    }

    private static void checkLockConflicts(SchoolDefinitionDto input, List<ValidationError> errors) {
        var teachers = index(input.teachers(), TeacherDto::id);
        var cohorts = index(input.cohorts(), CohortDto::id);
        var rooms = index(input.rooms(), RoomDto::id);
        Set<String> allPeriods = ids(input.periods(), PeriodDto::id);
        Set<String> reservedPeriods = set(input.reservedPeriodIds());
        var lockedResources = new HashMap<String, String>();

        for (int i = 0; i < input.lessons().size(); i++) {
            var lesson = input.lessons().get(i);
            var teacher = teachers.get(lesson.teacherId());
            var cohort = cohorts.get(lesson.cohortId());
            var lockedRoom = lesson.roomLock() == null ? null : rooms.get(lesson.roomLock());
            if (lockedRoom != null) {
                if (cohort != null && lockedRoom.capacity() < cohort.size()) {
                    error(errors, "/lessons/" + i + "/roomLock", List.of(lesson.id(), lockedRoom.id()),
                            "room lock contradicts room capacity");
                }
                if (lesson.requiredRoomCapabilityIds() != null
                        && !new HashSet<>(lockedRoom.capabilityIds()).containsAll(lesson.requiredRoomCapabilityIds())) {
                    error(errors, "/lessons/" + i + "/roomLock", List.of(lesson.id(), lockedRoom.id()),
                            "room lock contradicts required room capabilities");
                }
                if (lesson.periodLock() == null && teacher != null && cohort != null) {
                    boolean anyAvailablePeriod = allPeriods.stream().anyMatch(periodId ->
                            teacher.availablePeriodIds() == null || teacher.availablePeriodIds().contains(periodId))
                            && allPeriods.stream().anyMatch(periodId ->
                                    (teacher.availablePeriodIds() == null || teacher.availablePeriodIds().contains(periodId))
                                            && (cohort.availablePeriodIds() == null
                                                    || cohort.availablePeriodIds().contains(periodId))
                                            && (lockedRoom.availablePeriodIds() == null
                                                    || lockedRoom.availablePeriodIds().contains(periodId)));
                    if (!anyAvailablePeriod) {
                        error(errors, "/lessons/" + i + "/roomLock", List.of(lesson.id(), lockedRoom.id()),
                                "room lock contradicts resource availability");
                    }
                }
            }
            if (lesson.periodLock() == null || !allPeriods.contains(lesson.periodLock())) {
                continue;
            }
            if (reservedPeriods.contains(lesson.periodLock())) {
                error(errors, "/lessons/" + i + "/periodLock", List.of(lesson.id(), lesson.periodLock()),
                        "period lock contradicts school reservation");
            }
            if (teacher != null && !availability(teacher.availablePeriodIds(), allPeriods).contains(lesson.periodLock())) {
                error(errors, "/lessons/" + i + "/periodLock", List.of(lesson.id(), teacher.id(), lesson.periodLock()),
                        "period lock contradicts teacher availability");
            }
            if (cohort != null && !availability(cohort.availablePeriodIds(), allPeriods).contains(lesson.periodLock())) {
                error(errors, "/lessons/" + i + "/periodLock", List.of(lesson.id(), cohort.id(), lesson.periodLock()),
                        "period lock contradicts cohort availability");
            }
            checkLockedCollision(lockedResources, "teacher", lesson.teacherId(), lesson.periodLock(), lesson.id(), errors);
            checkLockedCollision(lockedResources, "cohort", lesson.cohortId(), lesson.periodLock(), lesson.id(), errors);

            if (lesson.roomLock() != null) {
                var room = rooms.get(lesson.roomLock());
                if (room != null) {
                    if (!availability(room.availablePeriodIds(), allPeriods).contains(lesson.periodLock())) {
                        error(errors, "/lessons/" + i + "/roomLock",
                                List.of(lesson.id(), room.id(), lesson.periodLock()),
                                "room lock contradicts room availability");
                    }
                    checkLockedCollision(lockedResources, "room", room.id(), lesson.periodLock(), lesson.id(), errors);
                }
            }
        }
    }

    private static void checkLockedCollision(
            Map<String, String> lockedResources,
            String type,
            String resourceId,
            String periodId,
            String lessonId,
            List<ValidationError> errors) {
        String key = type + "\u0000" + resourceId + "\u0000" + periodId;
        String previous = lockedResources.putIfAbsent(key, lessonId);
        if (previous != null) {
            error(errors, "/lessons", List.of(previous, lessonId, resourceId, periodId),
                    "locked lessons collide on " + type + " and period");
        }
    }

    private static SchoolDefinition toDomain(SchoolDefinitionDto input) {
        Set<String> allPeriods = ids(input.periods(), PeriodDto::id);
        var subjects = input.subjects().stream()
                .map(value -> new SchoolDefinition.Subject(value.id(), value.displayName()))
                .toList();
        var teachers = input.teachers().stream()
                .map(value -> new SchoolDefinition.Teacher(
                        value.id(), value.displayName(), set(value.qualifiedSubjectIds()),
                        availability(value.availablePeriodIds(), allPeriods), set(value.undesirablePeriodIds())))
                .toList();
        var cohorts = input.cohorts().stream()
                .map(value -> new SchoolDefinition.Cohort(
                        value.id(), value.displayName(), value.size(),
                        availability(value.availablePeriodIds(), allPeriods), set(value.undesirablePeriodIds()),
                        value.maxDailyLessonSpread() == null ? 1 : value.maxDailyLessonSpread()))
                .toList();
        var rooms = input.rooms().stream()
                .map(value -> new SchoolDefinition.Room(
                        value.id(), value.displayName(), value.capacity(), set(value.capabilityIds()),
                        availability(value.availablePeriodIds(), allPeriods)))
                .toList();
        var periods = input.periods().stream()
                .map(value -> new SchoolDefinition.Period(
                        value.id(), value.displayName(), DayOfWeek.valueOf(value.weekday()), value.order(),
                        value.startTime() == null ? null : LocalTime.parse(value.startTime()),
                        value.endTime() == null ? null : LocalTime.parse(value.endTime())))
                .toList();
        var lessons = input.lessons().stream()
                .map(value -> new SchoolDefinition.Lesson(
                        value.id(), value.displayName(), value.subjectId(), value.cohortId(), value.teacherId(),
                        value.seriesId(), set(value.requiredRoomCapabilityIds()), set(value.preferredRoomIds()),
                        set(value.undesirablePeriodIds()), value.periodLock(), value.roomLock()))
                .toList();
        return new SchoolDefinition(
                input.schemaVersion(), input.catalogVersion(), input.schoolId(), input.displayName(),
                input.basedOnRevision(),
                subjects, teachers, cohorts, rooms,
                periods, set(input.reservedPeriodIds()), lessons,
                Map.copyOf(effectiveWeights(input.softConstraintOverrides(), input.catalogVersion())));
    }

    private static Map<String, Long> effectiveWeights(List<SoftConstraintOverrideDto> overrides, int catalogVersion) {
        var weights = new LinkedHashMap<String, Long>();
        weights.putAll(KernelCatalog.defaultSoftWeights(catalogVersion));
        if (overrides != null) {
            overrides.forEach(override -> weights.put(override.constraintId(), override.weight()));
        }
        return weights;
    }

    private static Set<String> availability(List<String> configured, Set<String> allPeriods) {
        return configured == null ? Set.copyOf(allPeriods) : set(configured);
    }

    private static Set<String> set(List<String> values) {
        return values == null ? Set.of() : Set.copyOf(values);
    }

    private static <T> Map<String, T> index(List<T> values, Function<T, String> id) {
        var result = new HashMap<String, T>();
        values.forEach(value -> result.putIfAbsent(id.apply(value), value));
        return result;
    }

    private static void error(
            List<ValidationError> errors,
            String location,
            List<String> entityIds,
            String message) {
        errors.add(new ValidationError(location, entityIds, message));
    }
}
