package org.schoolkernel.solver;

import java.time.DayOfWeek;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.schoolkernel.domain.KernelCatalog;

import ai.timefold.solver.core.api.score.BendableScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintCollectors;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.api.score.stream.Joiners;

public final class SchoolConstraintProvider implements ConstraintProvider {
    static final BendableScore HARD = BendableScore.ofHard(1, 3, 0, 1);
    static final BendableScore PERIOD_MOVE = BendableScore.ofSoft(1, 3, 0, 1);
    static final BendableScore ROOM_ONLY_MOVE = BendableScore.ofSoft(1, 3, 1, 1);
    static final BendableScore PREFERENCE = BendableScore.ofSoft(1, 3, 2, 1);

    @Override
    public Constraint[] defineConstraints(ConstraintFactory factory) {
        return new Constraint[] {
                teacherPeriod(factory),
                cohortPeriod(factory),
                roomPeriod(factory),
                teacherAvailability(factory),
                cohortAvailability(factory),
                roomAvailability(factory),
                roomCapacity(factory),
                roomCapability(factory),
                periodLock(factory),
                roomLock(factory),
                periodMove(factory),
                roomOnlyMove(factory),
                teacherGap(factory),
                cohortGap(factory),
                cohortLateStart(factory),
                cohortWeekBalance(factory),
                seriesSameDay(factory),
                undesirablePeriod(factory),
                nonPreferredRoom(factory)
        };
    }

    public Constraint teacherPeriod(ConstraintFactory factory) {
        return factory.forEachUniquePair(
                        PlanningLesson.class,
                        Joiners.equal(PlanningLesson::getTeacherId),
                        Joiners.equal(PlanningLesson::getPeriod))
                .penalize(HARD)
                .asConstraint(KernelCatalog.TEACHER_PERIOD.id());
    }

    public Constraint cohortPeriod(ConstraintFactory factory) {
        return factory.forEachUniquePair(
                        PlanningLesson.class,
                        Joiners.equal(PlanningLesson::getCohortId),
                        Joiners.equal(PlanningLesson::getPeriod))
                .penalize(HARD)
                .asConstraint(KernelCatalog.COHORT_PERIOD.id());
    }

    public Constraint roomPeriod(ConstraintFactory factory) {
        return factory.forEachUniquePair(
                        PlanningLesson.class,
                        Joiners.equal(PlanningLesson::getRoom),
                        Joiners.equal(PlanningLesson::getPeriod))
                .penalize(HARD)
                .asConstraint(KernelCatalog.ROOM_PERIOD.id());
    }

    public Constraint teacherAvailability(ConstraintFactory factory) {
        return factory.forEach(PlanningLesson.class)
                .filter(lesson -> !lesson.getTeacherAvailablePeriodIds().contains(lesson.getPeriod().id()))
                .penalize(HARD)
                .asConstraint(KernelCatalog.TEACHER_AVAILABILITY.id());
    }

    public Constraint cohortAvailability(ConstraintFactory factory) {
        return factory.forEach(PlanningLesson.class)
                .filter(lesson -> !lesson.getCohortAvailablePeriodIds().contains(lesson.getPeriod().id()))
                .penalize(HARD)
                .asConstraint(KernelCatalog.COHORT_AVAILABILITY.id());
    }

    public Constraint roomAvailability(ConstraintFactory factory) {
        return factory.forEach(PlanningLesson.class)
                .filter(lesson -> !lesson.getRoom().availablePeriodIds().contains(lesson.getPeriod().id()))
                .penalize(HARD)
                .asConstraint(KernelCatalog.ROOM_AVAILABILITY.id());
    }

    public Constraint roomCapacity(ConstraintFactory factory) {
        return factory.forEach(PlanningLesson.class)
                .filter(lesson -> lesson.getRoom().capacity() < lesson.getCohortSize())
                .penalize(HARD)
                .asConstraint(KernelCatalog.ROOM_CAPACITY.id());
    }

    public Constraint roomCapability(ConstraintFactory factory) {
        return factory.forEach(PlanningLesson.class)
                .filter(lesson -> !lesson.getRoom().capabilityIds().containsAll(lesson.getRequiredRoomCapabilityIds()))
                .penalize(HARD)
                .asConstraint(KernelCatalog.ROOM_CAPABILITY.id());
    }

    public Constraint periodLock(ConstraintFactory factory) {
        return factory.forEach(PlanningLesson.class)
                .filter(lesson -> lesson.getPeriodLock() != null
                        && !lesson.getPeriodLock().equals(lesson.getPeriod().id()))
                .penalize(HARD)
                .asConstraint(KernelCatalog.PERIOD_LOCK.id());
    }

    public Constraint roomLock(ConstraintFactory factory) {
        return factory.forEach(PlanningLesson.class)
                .filter(lesson -> lesson.getRoomLock() != null && !lesson.getRoomLock().equals(lesson.getRoom().id()))
                .penalize(HARD)
                .asConstraint(KernelCatalog.ROOM_LOCK.id());
    }

    public Constraint periodMove(ConstraintFactory factory) {
        return factory.forEach(PlanningLesson.class)
                .filter(lesson -> lesson.getBaselinePeriodId() != null
                        && !lesson.getBaselinePeriodId().equals(lesson.getPeriod().id())
                        && (lesson.getPeriodLock() == null
                                || lesson.getPeriodLock().equals(lesson.getBaselinePeriodId())))
                .penalize(PERIOD_MOVE)
                .asConstraint(KernelCatalog.PERIOD_MOVE.id());
    }

    public Constraint roomOnlyMove(ConstraintFactory factory) {
        return factory.forEach(PlanningLesson.class)
                .filter(lesson -> lesson.getBaselinePeriodId() != null
                        && lesson.getBaselinePeriodId().equals(lesson.getPeriod().id())
                        && !lesson.getBaselineRoomId().equals(lesson.getRoom().id())
                        && (lesson.getRoomLock() == null
                                || lesson.getRoomLock().equals(lesson.getBaselineRoomId())))
                .penalize(ROOM_ONLY_MOVE)
                .asConstraint(KernelCatalog.ROOM_ONLY_MOVE.id());
    }

    public Constraint teacherGap(ConstraintFactory factory) {
        return factory.forEach(PlanningLesson.class)
                .groupBy(
                        lesson -> new TeacherDay(lesson.getTeacherId(), lesson.getPeriod().weekday(),
                                lesson.getTeacherAvailablePeriodIds(), lesson.getPeriodCatalog()),
                        ConstraintCollectors.toList(PlanningLesson::getPeriod))
                .penalize(PREFERENCE, (teacherDay, periods) -> countGaps(
                        periods, teacherDay.available(), teacherDay.catalog(), teacherDay.weekday()))
                .asConstraint(KernelCatalog.TEACHER_GAP.id());
    }

    public Constraint cohortGap(ConstraintFactory factory) {
        return factory.forEach(PlanningLesson.class)
                .groupBy(
                        lesson -> new CohortDay(lesson.getCohortId(), lesson.getPeriod().weekday(),
                                lesson.getCohortAvailablePeriodIds(), lesson.getPeriodCatalog()),
                        ConstraintCollectors.toList(PlanningLesson::getPeriod))
                .penalize(PREFERENCE, (cohortDay, periods) -> countGaps(
                        periods, cohortDay.available(), cohortDay.catalog(), cohortDay.weekday()))
                .asConstraint(KernelCatalog.COHORT_GAP.id());
    }

    public Constraint cohortLateStart(ConstraintFactory factory) {
        return factory.forEach(PlanningLesson.class)
                .groupBy(
                        lesson -> new CohortDay(lesson.getCohortId(), lesson.getPeriod().weekday(),
                                lesson.getCohortAvailablePeriodIds(), lesson.getPeriodCatalog()),
                        ConstraintCollectors.toList(PlanningLesson::getPeriod))
                .penalize(PREFERENCE, (cohortDay, periods) -> countLateStart(
                        periods, cohortDay.catalog(), cohortDay.weekday()))
                .asConstraint(KernelCatalog.COHORT_LATE_START.id());
    }

    public Constraint cohortWeekBalance(ConstraintFactory factory) {
        return factory.forEach(PlanningLesson.class)
                .groupBy(
                        lesson -> new CohortWeek(lesson.getCohortId(),
                                lesson.getCohortAvailablePeriodIds(), lesson.getPeriodCatalog()),
                        ConstraintCollectors.toList(lesson -> lesson.getPeriod().weekday()))
                .penalize(PREFERENCE, (cohortWeek, weekdays) -> countCohortWeekImbalance(
                        weekdays, cohortWeek.available(), cohortWeek.catalog()))
                .asConstraint(KernelCatalog.COHORT_WEEK_BALANCE.id());
    }

    public Constraint seriesSameDay(ConstraintFactory factory) {
        return factory.forEach(PlanningLesson.class)
                .filter(lesson -> lesson.getSeriesId() != null)
                .groupBy(
                        lesson -> new SeriesDay(lesson.getSeriesId(), lesson.getPeriod().weekday()),
                        ConstraintCollectors.count())
                .filter((seriesDay, count) -> count > 1)
                .penalize(PREFERENCE, (seriesDay, count) -> count - 1)
                .asConstraint(KernelCatalog.SERIES_SAME_DAY.id());
    }

    public Constraint undesirablePeriod(ConstraintFactory factory) {
        return factory.forEach(PlanningLesson.class)
                .filter(lesson -> undesirableMatchCount(lesson) > 0)
                .penalize(PREFERENCE, SchoolConstraintProvider::undesirableMatchCount)
                .asConstraint(KernelCatalog.UNDESIRABLE_PERIOD.id());
    }

    public Constraint nonPreferredRoom(ConstraintFactory factory) {
        return factory.forEach(PlanningLesson.class)
                .filter(lesson -> !lesson.getPreferredRoomIds().isEmpty()
                        && !lesson.getPreferredRoomIds().contains(lesson.getRoom().id()))
                .penalize(PREFERENCE)
                .asConstraint(KernelCatalog.NON_PREFERRED_ROOM.id());
    }

    static long countTeacherGaps(List<PlanningLesson> lessons) {
        if (lessons.isEmpty()) return 0;
        PlanningLesson sample = lessons.getFirst();
        return countGaps(lessons.stream().map(PlanningLesson::getPeriod).toList(),
                sample.getTeacherAvailablePeriodIds(), sample.getPeriodCatalog(), sample.getPeriod().weekday());
    }

    static long countCohortGaps(List<PlanningLesson> lessons) {
        if (lessons.isEmpty()) return 0;
        PlanningLesson sample = lessons.getFirst();
        return countGaps(lessons.stream().map(PlanningLesson::getPeriod).toList(),
                sample.getCohortAvailablePeriodIds(), sample.getPeriodCatalog(), sample.getPeriod().weekday());
    }

    static long countCohortLateStart(List<PlanningLesson> lessons) {
        if (lessons.isEmpty()) return 0;
        PlanningLesson sample = lessons.getFirst();
        return countLateStart(lessons.stream().map(PlanningLesson::getPeriod).toList(),
                sample.getPeriodCatalog(), sample.getPeriod().weekday());
    }

    private static long countLateStart(List<PeriodValue> assigned, List<PeriodValue> catalog, DayOfWeek day) {
        if (assigned.isEmpty()) return 0;
        int firstAssignedOrder = assigned.stream().mapToInt(PeriodValue::order).min().orElseThrow();
        long earlierSlots = catalog.stream()
                .filter(period -> period.weekday() == day && period.order() < firstAssignedOrder)
                .count();
        return earlierSlots >= 3 ? 1 : 0;
    }

    private static long countGaps(
            List<PeriodValue> assigned, Set<String> available, List<PeriodValue> catalog, DayOfWeek day) {
        if (assigned.size() < 2) {
            return 0;
        }
        var assignedOrders = new HashSet<Integer>();
        assigned.forEach(period -> assignedOrders.add(period.order()));
        List<PeriodValue> dayPeriods = catalog.stream()
                .filter(period -> period.weekday() == day)
                .sorted(java.util.Comparator.comparingInt(PeriodValue::order))
                .toList();
        long gaps = 0;
        int blockStart = 0;
        while (blockStart < dayPeriods.size()) {
            while (blockStart < dayPeriods.size() && !available.contains(dayPeriods.get(blockStart).id())) {
                blockStart++;
            }
            int blockEnd = blockStart;
            while (blockEnd < dayPeriods.size() && available.contains(dayPeriods.get(blockEnd).id())) {
                blockEnd++;
            }
            int firstAssigned = -1;
            int lastAssigned = -1;
            for (int index = blockStart; index < blockEnd; index++) {
                if (assignedOrders.contains(dayPeriods.get(index).order())) {
                    if (firstAssigned < 0) {
                        firstAssigned = index;
                    }
                    lastAssigned = index;
                }
            }
            if (firstAssigned >= 0) {
                for (int index = firstAssigned + 1; index < lastAssigned; index++) {
                    if (!assignedOrders.contains(dayPeriods.get(index).order())) {
                        gaps++;
                    }
                }
            }
            blockStart = Math.max(blockEnd + 1, blockStart + 1);
        }
        return gaps;
    }

    static long countCohortWeekImbalance(List<PlanningLesson> lessons) {
        if (lessons.isEmpty()) {
            return 0;
        }
        PlanningLesson sample = lessons.getFirst();
        return countCohortWeekImbalance(lessons.stream()
                        .map(lesson -> lesson.getPeriod().weekday()).toList(),
                sample.getCohortAvailablePeriodIds(), sample.getPeriodCatalog());
    }

    private static long countCohortWeekImbalance(
            List<DayOfWeek> assignments, Set<String> available, List<PeriodValue> catalog) {
        List<DayOfWeek> days = catalog.stream()
                .filter(period -> available.contains(period.id()))
                .map(PeriodValue::weekday)
                .distinct()
                .toList();
        var dailyCounts = new EnumMap<DayOfWeek, Long>(DayOfWeek.class);
        assignments.forEach(day -> dailyCounts.merge(day, 1L, Long::sum));
        long imbalance = 0;
        for (int left = 0; left < days.size(); left++) {
            for (int right = left + 1; right < days.size(); right++) {
                long difference = Math.abs(dailyCounts.getOrDefault(days.get(left), 0L)
                        - dailyCounts.getOrDefault(days.get(right), 0L));
                imbalance += Math.max(0L, difference - 1L);
            }
        }
        return imbalance;
    }

    static long undesirableMatchCount(PlanningLesson lesson) {
        String periodId = lesson.getPeriod().id();
        long count = 0;
        if (lesson.getTeacherUndesirablePeriodIds().contains(periodId)) {
            count++;
        }
        if (lesson.getCohortUndesirablePeriodIds().contains(periodId)) {
            count++;
        }
        if (lesson.getLessonUndesirablePeriodIds().contains(periodId)) {
            count++;
        }
        return count;
    }

    private record TeacherDay(
            String teacherId, DayOfWeek weekday, Set<String> available, List<PeriodValue> catalog) {}
    private record CohortDay(
            String cohortId, DayOfWeek weekday, Set<String> available, List<PeriodValue> catalog) {}
    private record CohortWeek(String cohortId, Set<String> available, List<PeriodValue> catalog) {}
    private record SeriesDay(String seriesId, DayOfWeek weekday) {}
}
