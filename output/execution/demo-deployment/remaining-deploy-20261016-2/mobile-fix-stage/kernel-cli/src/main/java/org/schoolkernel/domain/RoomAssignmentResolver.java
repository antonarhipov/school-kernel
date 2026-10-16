package org.schoolkernel.domain;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Resolves every matching mandatory assignment for one lesson without changing the definition. */
public final class RoomAssignmentResolver {
    public record Resolution(
            Set<String> allowedRoomIds,
            List<String> policyIds,
            List<String> missingHomeRoomPolicyIds) {
        public boolean applies() {
            return !policyIds.isEmpty();
        }

        public boolean allows(String roomId) {
            return !applies() || allowedRoomIds.contains(roomId);
        }
    }

    private RoomAssignmentResolver() {}

    public static Resolution resolve(SchoolDefinition definition, SchoolDefinition.Lesson lesson) {
        var cohort = definition.cohorts().stream()
                .filter(value -> value.id().equals(lesson.cohortId()))
                .findFirst().orElseThrow();
        var matched = new ArrayList<String>();
        var missingHome = new ArrayList<String>();
        Set<String> allowed = null;
        for (var policy : definition.roomAssignments()) {
            if (!policy.subjectId().equals(lesson.subjectId())
                    || policy.teacherId() != null && !policy.teacherId().equals(lesson.teacherId())) {
                continue;
            }
            matched.add(policy.id());
            Set<String> target;
            if (policy.useHomeRoom()) {
                if (cohort.homeRoomId() == null) {
                    missingHome.add(policy.id());
                    target = Set.of();
                } else {
                    target = Set.of(cohort.homeRoomId());
                }
            } else {
                target = policy.allowedRoomIds();
            }
            if (allowed == null) {
                allowed = new HashSet<>(target);
            } else {
                allowed.retainAll(target);
            }
        }
        return new Resolution(allowed == null ? Set.of() : Set.copyOf(allowed),
                List.copyOf(matched), List.copyOf(missingHome));
    }
}
