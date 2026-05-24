package com.aicoding.platform.orchestration;

import com.aicoding.platform.support.IntegrationTestBase;
import com.aicoding.platform.support.TestJsonHelper;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

class KnowledgeQualityReviewIntegrationTest extends IntegrationTestBase {

    private String createProject(String suffix) {
        ResponseEntity<String> res = post("/api/projects", Map.of(
                "name", "IT-QUAL-" + suffix,
                "description", "Quality review integration test project",
                "techStack", List.of("Java")
        ));
        assertOk(res);
        return Objects.requireNonNull(TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data.id"));
    }

    private String createIncident(String projectId, String severity, String title) {
        ResponseEntity<String> res = post("/api/orchestration/incidents", Map.of(
                "projectId", projectId,
                "sourceType", "MANUAL",
                "severity", severity,
                "title", title
        ));
        assertOk(res);
        return Objects.requireNonNull(TestJsonHelper.getString(
                TestJsonHelper.parse(res.getBody()), "data.id"));
    }

    // ==============================
    // Quality Review - Create
    // ==============================

    @Test
    void shouldCreateQualityReview() {
        String pid = createProject("QualCreate");
        String iid = createIncident(pid, "CRITICAL", "quality review test");

        ResponseEntity<String> res = post("/api/orchestration/incidents/" + iid + "/knowledge-quality-reviews", Map.of(
                "completenessScore", 4,
                "accuracyScore", 5,
                "actionabilityScore", 3,
                "relevanceScore", 4,
                "reviewComment", "Good document, needs more action items"
        ));
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(TestJsonHelper.getString(data, "reviewStatus")).isEqualTo("PENDING");
        assertThat(TestJsonHelper.getString(data, "overallStatus")).isEqualTo("APPROVED");
        assertThat(data.get("completenessScore").asInt()).isEqualTo(4);
        assertThat(data.get("averageScore").asDouble()).isEqualTo(4.0);
    }

    @Test
    void shouldCreateQualityReviewWithNeedsWorkScore() {
        String pid = createProject("QualNeed");
        String iid = createIncident(pid, "HIGH", "needs work quality");

        ResponseEntity<String> res = post("/api/orchestration/incidents/" + iid + "/knowledge-quality-reviews", Map.of(
                "completenessScore", 2,
                "accuracyScore", 3,
                "actionabilityScore", 2,
                "relevanceScore", 3,
                "reviewComment", "Needs improvement"
        ));
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(TestJsonHelper.getString(data, "overallStatus")).isEqualTo("NEEDS_WORK");
    }

    @Test
    void shouldCreateQualityReviewWithRejectedScore() {
        String pid = createProject("QualRej");
        String iid = createIncident(pid, "MEDIUM", "rejected quality");

        ResponseEntity<String> res = post("/api/orchestration/incidents/" + iid + "/knowledge-quality-reviews", Map.of(
                "completenessScore", 1,
                "accuracyScore", 1,
                "actionabilityScore", 0,
                "relevanceScore", 1,
                "reviewComment", "Insufficient quality"
        ));
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(TestJsonHelper.getString(data, "overallStatus")).isEqualTo("REJECTED");
    }

    @Test
    void shouldFailCreateWithInvalidScore() {
        String pid = createProject("QualInv");
        String iid = createIncident(pid, "LOW", "invalid score");

        ResponseEntity<String> res = post("/api/orchestration/incidents/" + iid + "/knowledge-quality-reviews", Map.of(
                "completenessScore", 6,
                "accuracyScore", 5,
                "actionabilityScore", 3,
                "relevanceScore", 4
        ));
        assertCode(res, "VALIDATION_ERROR");
    }

    @Test
    void shouldFailCreateWithNegativeScore() {
        String pid = createProject("QualNeg");
        String iid = createIncident(pid, "HIGH", "negative score");

        ResponseEntity<String> res = post("/api/orchestration/incidents/" + iid + "/knowledge-quality-reviews", Map.of(
                "completenessScore", -1,
                "accuracyScore", 3,
                "actionabilityScore", 3,
                "relevanceScore", 3
        ));
        assertCode(res, "VALIDATION_ERROR");
    }

    @Test
    void shouldFailCreateWithNullScore() {
        String pid = createProject("QualNull");
        String iid = createIncident(pid, "MEDIUM", "null score");

        ResponseEntity<String> res = post("/api/orchestration/incidents/" + iid + "/knowledge-quality-reviews", Map.of(
                "completenessScore", 4,
                "accuracyScore", 3
        ));
        assertCode(res, "VALIDATION_ERROR");
    }

    @Test
    void shouldFailCreateForNonexistentIncident() {
        ResponseEntity<String> res = post("/api/orchestration/incidents/999999/knowledge-quality-reviews", Map.of(
                "completenessScore", 4,
                "accuracyScore", 4,
                "actionabilityScore", 4,
                "relevanceScore", 4
        ));
        assertCode(res, "NOT_FOUND");
    }

    // ==============================
    // Quality Review - Update
    // ==============================

    @Test
    void shouldUpdateQualityReviewScores() {
        String pid = createProject("QualUpd");
        String iid = createIncident(pid, "CRITICAL", "update quality");
        ResponseEntity<String> createRes = post("/api/orchestration/incidents/" + iid + "/knowledge-quality-reviews", Map.of(
                "completenessScore", 3,
                "accuracyScore", 3,
                "actionabilityScore", 3,
                "relevanceScore", 3,
                "reviewComment", "Initial"
        ));
        String reviewId = TestJsonHelper.getString(TestJsonHelper.parse(createRes.getBody()).get("data"), "id");

        ResponseEntity<String> res = put("/api/orchestration/knowledge-quality-reviews/" + reviewId, Map.of(
                "completenessScore", 5,
                "accuracyScore", 5,
                "actionabilityScore", 4,
                "relevanceScore", 5,
                "reviewComment", "Updated: much better"
        ));
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(data.get("completenessScore").asInt()).isEqualTo(5);
        assertThat(TestJsonHelper.getString(data, "overallStatus")).isEqualTo("APPROVED");
        assertThat(data.get("averageScore").asDouble()).isEqualTo(4.75);
    }

    @Test
    void shouldUpdateQualityReviewStatusToInReview() {
        String pid = createProject("QualInRev");
        String iid = createIncident(pid, "HIGH", "in review quality");
        ResponseEntity<String> createRes = post("/api/orchestration/incidents/" + iid + "/knowledge-quality-reviews", Map.of(
                "completenessScore", 4,
                "accuracyScore", 4,
                "actionabilityScore", 4,
                "relevanceScore", 4
        ));
        String reviewId = TestJsonHelper.getString(TestJsonHelper.parse(createRes.getBody()).get("data"), "id");

        ResponseEntity<String> res = put("/api/orchestration/knowledge-quality-reviews/" + reviewId, Map.of(
                "reviewStatus", "IN_REVIEW"
        ));
        assertOk(res);
        assertThat(TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()).get("data"), "reviewStatus")).isEqualTo("IN_REVIEW");
    }

    @Test
    void shouldCompleteQualityReview() {
        String pid = createProject("QualComp");
        String iid = createIncident(pid, "MEDIUM", "complete quality");
        ResponseEntity<String> createRes = post("/api/orchestration/incidents/" + iid + "/knowledge-quality-reviews", Map.of(
                "completenessScore", 4,
                "accuracyScore", 4,
                "actionabilityScore", 4,
                "relevanceScore", 4
        ));
        String reviewId = TestJsonHelper.getString(TestJsonHelper.parse(createRes.getBody()).get("data"), "id");

        put("/api/orchestration/knowledge-quality-reviews/" + reviewId, Map.of("reviewStatus", "IN_REVIEW"));
        ResponseEntity<String> res = put("/api/orchestration/knowledge-quality-reviews/" + reviewId, Map.of(
                "reviewStatus", "COMPLETED"
        ));
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(TestJsonHelper.getString(data, "reviewStatus")).isEqualTo("COMPLETED");
        assertThat(data.has("reviewedAt")).isTrue();
        assertThat(TestJsonHelper.getString(data, "overallStatus")).isEqualTo("APPROVED");
    }

    @Test
    void shouldFailUpdateCompletedReview() {
        String pid = createProject("QualComUp");
        String iid = createIncident(pid, "HIGH", "completed update");
        ResponseEntity<String> createRes = post("/api/orchestration/incidents/" + iid + "/knowledge-quality-reviews", Map.of(
                "completenessScore", 4,
                "accuracyScore", 4,
                "actionabilityScore", 4,
                "relevanceScore", 4
        ));
        String reviewId = TestJsonHelper.getString(TestJsonHelper.parse(createRes.getBody()).get("data"), "id");

        put("/api/orchestration/knowledge-quality-reviews/" + reviewId, Map.of("reviewStatus", "IN_REVIEW"));
        put("/api/orchestration/knowledge-quality-reviews/" + reviewId, Map.of("reviewStatus", "COMPLETED"));

        ResponseEntity<String> res = put("/api/orchestration/knowledge-quality-reviews/" + reviewId, Map.of(
                "completenessScore", 5
        ));
        assertCode(res, "VALIDATION_ERROR");
    }

    @Test
    void shouldFailReviewInvalidStatusTransition() {
        String pid = createProject("QualBadTr");
        String iid = createIncident(pid, "LOW", "bad trans");
        ResponseEntity<String> createRes = post("/api/orchestration/incidents/" + iid + "/knowledge-quality-reviews", Map.of(
                "completenessScore", 3,
                "accuracyScore", 3,
                "actionabilityScore", 3,
                "relevanceScore", 3
        ));
        String reviewId = TestJsonHelper.getString(TestJsonHelper.parse(createRes.getBody()).get("data"), "id");

        ResponseEntity<String> res = put("/api/orchestration/knowledge-quality-reviews/" + reviewId, Map.of(
                "reviewStatus", "COMPLETED"
        ));
        assertCode(res, "VALIDATION_ERROR");
    }

    @Test
    void shouldFailUpdateNonexistentReview() {
        ResponseEntity<String> res = put("/api/orchestration/knowledge-quality-reviews/999999", Map.of(
                "completenessScore", 4
        ));
        assertCode(res, "NOT_FOUND");
    }

    // ==============================
    // Quality Review - Get
    // ==============================

    @Test
    void shouldGetQualityReviewById() {
        String pid = createProject("QualGet");
        String iid = createIncident(pid, "CRITICAL", "get quality");
        ResponseEntity<String> createRes = post("/api/orchestration/incidents/" + iid + "/knowledge-quality-reviews", Map.of(
                "completenessScore", 5,
                "accuracyScore", 5,
                "actionabilityScore", 5,
                "relevanceScore", 5
        ));
        String reviewId = TestJsonHelper.getString(TestJsonHelper.parse(createRes.getBody()).get("data"), "id");

        ResponseEntity<String> res = get("/api/orchestration/knowledge-quality-reviews/" + reviewId);
        assertOk(res);
        assertThat(TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()).get("data"), "id")).isEqualTo(reviewId);
    }

    @Test
    void shouldFailGetNonexistentReview() {
        ResponseEntity<String> res = get("/api/orchestration/knowledge-quality-reviews/999999");
        assertCode(res, "NOT_FOUND");
    }

    @Test
    void shouldCreateAndGetMultipleQualityReviews() {
        String pid = createProject("QualMult");
        String iid = createIncident(pid, "HIGH", "multiple quality");

        ResponseEntity<String> res1 = post("/api/orchestration/incidents/" + iid + "/knowledge-quality-reviews", Map.of(
                "completenessScore", 4,
                "accuracyScore", 4,
                "actionabilityScore", 4,
                "relevanceScore", 4
        ));
        assertOk(res1);
        String reviewId = TestJsonHelper.getString(TestJsonHelper.parse(res1.getBody()).get("data"), "id");

        ResponseEntity<String> res2 = get("/api/orchestration/knowledge-quality-reviews/" + reviewId);
        assertOk(res2);
        JsonNode data = TestJsonHelper.parse(res2.getBody()).get("data");
        assertThat(TestJsonHelper.getString(data, "id")).isEqualTo(reviewId);
        assertThat(data.get("completenessScore").asInt()).isEqualTo(4);
    }

    @Test
    void shouldGetProjectQualityStatusSummary() {
        String pid = createProject("QualSumm");
        String iid1 = createIncident(pid, "CRITICAL", "summary 1");
        String iid2 = createIncident(pid, "HIGH", "summary 2");

        post("/api/orchestration/incidents/" + iid1 + "/knowledge-quality-reviews", Map.of(
                "completenessScore", 5,
                "accuracyScore", 5,
                "actionabilityScore", 5,
                "relevanceScore", 5
        ));
        post("/api/orchestration/incidents/" + iid2 + "/knowledge-quality-reviews", Map.of(
                "completenessScore", 2,
                "accuracyScore", 2,
                "actionabilityScore", 2,
                "relevanceScore", 2
        ));

        ResponseEntity<String> res = get("/api/projects/" + pid + "/knowledge-quality-reviews");
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(data.get("totalReviews").asLong()).isGreaterThanOrEqualTo(2);
        assertThat(data.get("averageCompletenessScore").asDouble()).isBetween(3.0, 4.0);
        assertThat(data.has("approvedCount")).isTrue();
        assertThat(data.has("needsWorkCount")).isTrue();
    }

    @Test
    void shouldReturnEmptySummaryForNoReviews() {
        String pid = createProject("QualEmpty");
        ResponseEntity<String> res = get("/api/projects/" + pid + "/knowledge-quality-reviews");
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(data.get("totalReviews").asLong()).isEqualTo(0);
    }

    // ==============================
    // Quality Review - Edge Cases
    // ==============================

    @Test
    void shouldHandleReviewWithChecklist() {
        String pid = createProject("QualCheck");
        String iid = createIncident(pid, "MEDIUM", "checklist review");

        ResponseEntity<String> res = post("/api/orchestration/incidents/" + iid + "/knowledge-quality-reviews", Map.of(
                "completenessScore", 4,
                "accuracyScore", 4,
                "actionabilityScore", 3,
                "relevanceScore", 4,
                "checklistJson", "[\"Root cause identified\", \"Resolution documented\"]",
                "reviewComment", "LGTM"
        ));
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(TestJsonHelper.getString(data, "checklistJson")).contains("Root cause identified");
    }

    @Test
    void shouldRevertReviewFromInReviewToPending() {
        String pid = createProject("QualRevert");
        String iid = createIncident(pid, "HIGH", "revert quality");
        ResponseEntity<String> createRes = post("/api/orchestration/incidents/" + iid + "/knowledge-quality-reviews", Map.of(
                "completenessScore", 3,
                "accuracyScore", 3,
                "actionabilityScore", 3,
                "relevanceScore", 3
        ));
        String reviewId = TestJsonHelper.getString(TestJsonHelper.parse(createRes.getBody()).get("data"), "id");

        put("/api/orchestration/knowledge-quality-reviews/" + reviewId, Map.of("reviewStatus", "IN_REVIEW"));
        ResponseEntity<String> res = put("/api/orchestration/knowledge-quality-reviews/" + reviewId, Map.of(
                "reviewStatus", "PENDING"
        ));
        assertOk(res);
        assertThat(TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()).get("data"), "reviewStatus")).isEqualTo("PENDING");
    }
}
