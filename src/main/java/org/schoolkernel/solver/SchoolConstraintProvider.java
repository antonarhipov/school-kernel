package org.schoolkernel.solver;

import java.time.DayOfWeek;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ai.timefold.solver.core.api.score.BendableScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintCollectors;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.api.score.stream.Joiners;

public final class SchoolConstraintProvider implements ConstraintProvider {
    private static final BendableScore HARD = BendableScore.ofHard(1, 3, 0, 1);
    private static final BendableScore PERIOD_MOVE = BendableScore.ofSoft(1, 3, 0, 1);
    private static final BendableScore ROOM_ONLY_MOVE = BendableScore.ofSoft(1, 3, 1, 1);
    private static final BendableScore PREFERENCE = BendableScore.ofSoft(1, 3, 2, 1);

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
                .asConstraint("hard.teacher-period");
    }

    public Constraint cohortPeriod(ConstraintFactory factory) {
        return factory.forEachUniquePair(
                        PlanningLesson.class,
                        Joiners.equal(PlanningLesson::getCohortId),
                        Joiners.equal(PlanningLesson::getPeriod))
                .penalize(HARD)
                .asConstraint("hard.cohort-period");
    }

    public Constraint roomPeriod(ConstraintFactory factory) {
        return factory.forEachUniquePair(
                        PlanningLesson.class,
                        Joiners.equal(PlanningLesson::getRoom),
                        Joiners.equal(PlanningLesson::getPeriod))
                .penalize(HARD)
                .asConstraint("hard.room-period");
    }

    public Constraint teacherAvailability(ConstraintFactory factory) {
        return factory.forEach(PlanningLesson.class)
                .filter(lesson -> !lesson.getTeacherAvailablePeriodIds().contains(lesson.getPeriod().id()))
                .penalize(HARD)
                .asConstraint("hard.teacher-availability");
    }

    public Constraint cohortAvailability(ConstraintFactory factory) {
        return factory.forEach(PlanningLesson.class)
                .filter(lesson -> !lesson.getCohortAvailablePeriodIds().contains(lesson.getPeriod().id()))
                .penalize(HARD)
                .asConstraint("hard.cohort-availability");
    }

    public Constraint roomAvailability(ConstraintFactory factory) {
        return factory.forEach(PlanningLesson.class)
                .filter(lesson -> !lesson.getRoom().availablePeriodIds().contains(lesson.getPeriod().id()))
                .penalize(HARD)
                .asConstraint("hard.room-availability");
    }

    public Constraint roomCapacity(ConstraintFactory factory) {
        return factory.forEach(PlanningLesson.class)
                .filter(lesson -> lesson.getRoom().capacity() < lesson.getCohortSize())
                .penalize(HARD)
                .asConstraint("hard.room-capacity");
    }

    public Constraint roomCapability(ConstraintFactory factory) {
        return factory.forEach(PlanningLesson.class)
                .filter(lesson -> !lesson.getRoom().capabilityIds().containsAll(lesson.getRequiredRoomCapabilityIds()))
                .penalize(HARD)
                .asConstraint("hard.room-capability");
    }

    public Constraint periodLock(ConstraintFactory factory) {
        return factory.forEach(PlanningLesson.class)
                .filter(lesson -> lesson.getPeriodLock() != null
                        && !lesson.getPeriodLock().equals(lesson.getPeriod().id()))
                .penalize(HARD)
                .asConstraint("hard.period-lock");
    }

    public Constraint roomLock(ConstraintFactory factory) {
        return factory.forEach(PlanningLesson.class)
                .filter(lesson -> lesson.getRoomLock() != null && !lesson.getRoomLock().equals(lesson.getRoom().id()))
                .penalize(HARD)
                .asConstraint("hard.room-lock");
    }

    public Constraint periodMove(ConstraintFactory factory) {
        return factory.forEach(PlanningLesson.class)
                .filter(lesson -> lesson.getBaselinePeriodId() != null
                        && !lesson.getBaselinePeriodId().equals(lesson.getPeriod().id())
                        && (lesson.getPeriodLock() == null
                                || lesson.getPeriodLock().equals(lesson.getBaselinePeriodId())))
                .penalize(PERIOD_MOVE)
                .asConstraint("stability.period-move");
    }

    public Constraint roomOnlyMove(ConstraintFactory factory) {
        return factory.forEach(PlanningLesson.class)
                .filter(lesson -> lesson.getBaselinePeriodId() != null
                        && lesson.getBaselinePeriodId().equals(lesson.getPeriod().id())
                        && !lesson.getBaselineRoomId().equals(lesson.getRoom().id())
                        && (lesson.getRoomLock() == null
                                || lesson.getRoomLock().equals(lesson.getBaselineRoomId())))
                .penalize(ROOM_ONLY_MOVE)
                .asConstraint("stability.room-only-move");
    }

    public Constraint teacherGap(ConstraintFactory factory) {
        return factory.forEach(PlanningLesson.class)
                .groupBy(
                        lesson -> new TeacherDay(lesson.getTeacherId(), lesson.getPeriod().weekday()),
                        ConstraintCollectors.toList())
                .penalize(PREFERENCE, (teacherDay, lessons) -> countTeacherGaps(lessons))
                .asConstraint("soft.teacher-gap");
    }

    public Constraint seriesSameDay(ConstraintFactory factory) {
        return factory.forEach(PlanningLesson.class)
                .filter(lesson -> lesson.getSeriesId() != null)
                .groupBy(
                        lesson -> new SeriesDay(lesson.getSeriesId(), lesson.getPeriod().weekday()),
                        ConstraintCollectors.count())
                .filter((seriesDay, count) -> count > 1)
                .penalize(PREFERENCE, (seriesDay, count) -> count - 1)
                .asConstraint("soft.series-same-day");
    }

    public Constraint undesirablePeriod(ConstraintFactory factory) {
        return factory.forEach(PlanningLesson.class)
                .filter(lesson -> undesirableMatchCount(lesson) > 0)
                .penalize(PREFERENCE, SchoolConstraintProvider::undesirableMatchCount)
                .asConstraint("soft.undesirable-period");
    }

    public Constraint nonPreferredRoom(ConstraintFactory factory) {
        return factory.forEach(PlanningLesson.class)
                .filter(lesson -> !lesson.getPreferredRoomIds().isEmpty()
                        && !lesson.getPreferredRoomIds().contains(lesson.getRoom().id()))
                .penalize(PREFERENCE)
                .asConstraint("soft.non-preferred-room");
    }

    static long countTeacherGaps(List<PlanningLesson> lessons) {
        if (lessons.size() < 2) {
            return 0;
        }
        var assignedOrders = new HashSet<Integer>();
        lessons.forEach(lesson -> assignedOrders.add(lesson.getPeriod().order()));
        PlanningLesson sample = lessons.getFirst();
        DayOfWeek day = sample.getPeriod().weekday();
        List<PeriodValue> dayPeriods = sample.getPeriodCatalog().stream()
                .filter(period -> period.weekday() == day)
                .sorted(java.util.Comparator.comparingInt(PeriodValue::order))
                .toList();
        Set<String> available = sample.getTeacherAvailablePeriodIds();

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

    private record TeacherDay(String teacherId, DayOfWeek weekday) {}
    private record SeriesDay(String seriesId, DayOfWeek weekday) {}
}
