package org.schoolkernel.workspace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class ProposalReviewServiceTest {
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private final ProposalReviewService reviews = new ProposalReviewService(JSON);

    @Test
    @DisplayName("UC-6 main/extensions 1a/2a/G1-G3/RULE-18: review preserves six kernel categories and independently counts overlapping lessons")
    void buildsExactOverlappingImpactReview() throws Exception {
        JsonNode baseline = JSON.readTree("""
                {"definition":{"lessons":[
                  {"id":"l1","subjectId":"s1","cohortId":"c1","teacherId":"t1"},
                  {"id":"l2","subjectId":"s2","cohortId":"c1","teacherId":"t2"}],
                  "periods":[{"id":"p1","weekday":"MONDAY"},{"id":"p2","weekday":"TUESDAY"}]},
                 "result":{"timetable":{"assignments":[
                  {"lessonId":"l1","subjectId":"s1","cohortId":"c1","teacherId":"t1","periodId":"p1","roomId":"r1"},
                  {"lessonId":"l2","subjectId":"s2","cohortId":"c1","teacherId":"t2","periodId":"p2","roomId":"r2"}]}}}
                """);
        JsonNode draft = JSON.readTree("{\"directEffectLessonIds\":[\"l1\"]}");
        JsonNode definition = JSON.readTree("""
                {"lessons":[
                  {"id":"l1","subjectId":"s1","cohortId":"c1","teacherId":"t3"},
                  {"id":"l2","subjectId":"s2","cohortId":"c1","teacherId":"t2"}],
                 "periods":[{"id":"p1","weekday":"MONDAY"},{"id":"p2","weekday":"TUESDAY"}]}
                """);
        JsonNode result = JSON.readTree("""
                {"timetable":{"assignments":[
                  {"lessonId":"l1","subjectId":"s1","cohortId":"c1","teacherId":"t3","periodId":"p2","roomId":"r1"},
                  {"lessonId":"l2","subjectId":"s2","cohortId":"c1","teacherId":"t2","periodId":"p2","roomId":"r3"}]},
                 "changeReport":{"additions":[],"cancellations":[],
                  "teacherChanges":[{"lessonId":"l1","oldTeacherId":"t1","newTeacherId":"t3"}],
                  "forcedMoves":[],
                  "periodMoves":[{"lessonId":"l1","oldPeriodId":"p1","newPeriodId":"p2","oldRoomId":"r1","newRoomId":"r1"}],
                  "roomOnlyMoves":[{"lessonId":"l2","oldRoomId":"r2","newRoomId":"r3"}]}}
                """);

        JsonNode review = reviews.create(baseline, draft, definition, result);

        var categoryIds = JSON.createArrayNode();
        review.path("categories").forEach(category -> categoryIds.add(category.path("id").stringValue()));
        assertEquals(JSON.readTree("""
                ["additions","cancellations","teacherChanges","forcedMoves","periodMoves","roomOnlyMoves"]
                """), categoryIds);
        assertEquals(2, review.path("uniqueChangedLessonCount").intValue());
        assertEquals(1, review.path("directEffectChangedCount").intValue());
        assertEquals(1, review.path("rippleEffectCount").intValue());
        assertEquals(2, review.path("changedLessons").size());
        JsonNode l1 = review.path("changedLessons").get(0);
        assertEquals(JSON.readTree("[\"teacherChanges\",\"periodMoves\"]"), l1.path("categories"));
        assertTrue(l1.path("changedDimensions").valueStream().anyMatch(node -> "teacherId".equals(node.stringValue())));
        assertTrue(l1.path("changedDimensions").valueStream().anyMatch(node -> "periodId".equals(node.stringValue())));
        assertFalse(l1.path("changedDimensions").valueStream().anyMatch(node -> "roomId".equals(node.stringValue())));
        assertTrue(review.path("groupings").path("classes").valueStream()
                .anyMatch(group -> "c1".equals(group.path("id").stringValue()) && "BOTH".equals(group.path("context").stringValue())));
        assertTrue(review.path("groupings").path("days").valueStream()
                .anyMatch(group -> "MONDAY".equals(group.path("id").stringValue()) && "OLD".equals(group.path("context").stringValue())));
    }
}
