package com.aicoding.platform.orchestration;

import com.aicoding.platform.github.domain.PrReviewFindingEntity;
import com.aicoding.platform.github.domain.PrReviewJobEntity;
import com.aicoding.platform.github.infrastructure.PrReviewFindingMapper;
import com.aicoding.platform.github.infrastructure.PrReviewJobMapper;
import com.aicoding.platform.support.IntegrationTestBase;
import com.aicoding.platform.support.TestJsonHelper;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PrReviewQualityIntegrationTest extends IntegrationTestBase {

    @Autowired
    private PrReviewJobMapper prReviewJobMapper;

    @Autowired
    private PrReviewFindingMapper prReviewFindingMapper;

    private String projectIdValue;
    private Long reviewJobId;

    @BeforeEach
    public void setUp() {
        String suffix = String.valueOf(System.currentTimeMillis());
        ResponseEntity<String> res = post("/api/projects", Map.of(
                "name", "IT-PrReviewQuality-" + suffix,
                "description", "PR review quality integration test",
                "techStack", List.of("Java")
        ));
        assertOk(res);
        projectIdValue = TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data.id");

        // Create a pr_review_job directly for API testing
        Long projectId = Long.valueOf(projectIdValue);
        PrReviewJobEntity job = new PrReviewJobEntity();
        job.setProjectId(projectId);
        job.setPullRequestId(1L);
        job.setStatus("COMPLETED");
        job.setModelProvider("openai");
        job.setModelName("gpt-4");
        job.setCreatorId(1L);
        prReviewJobMapper.insert(job);
        reviewJobId = job.getId();

        // Add some findings
        PrReviewFindingEntity finding1 = new PrReviewFindingEntity();
        finding1.setReviewJobId(reviewJobId);
        finding1.setProjectId(projectId);
        finding1.setSeverity("HIGH");
        finding1.setTitle("Security issue");
        finding1.setCategory("SECURITY");
        prReviewFindingMapper.insert(finding1);

        PrReviewFindingEntity finding2 = new PrReviewFindingEntity();
        finding2.setReviewJobId(reviewJobId);
        finding2.setProjectId(projectId);
        finding2.setSeverity("LOW");
        finding2.setTitle("Style issue");
        finding2.setCategory("STYLE");
        prReviewFindingMapper.insert(finding2);
    }

    @Test
    void shouldCreateQualityRecord() {
        ResponseEntity<String> res = post("/api/projects/" + projectIdValue + "/pr-review-quality/records",
                Map.of("reviewJobId", reviewJobId.toString()));
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(TestJsonHelper.getString(data, "id")).isNotEmpty();
        assertThat(TestJsonHelper.getString(data, "reviewStatus")).isEqualTo("COMPLETED");
        assertThat(TestJsonHelper.getString(data, "humanFeedbackStatus")).isEqualTo("PENDING");
        assertThat(TestJsonHelper.getString(data, "adoptionStatus")).isEqualTo("UNKNOWN");
    }

    @Test
    void shouldCreateQualityRecordWithScores() {
        ResponseEntity<String> res = post("/api/projects/" + projectIdValue + "/pr-review-quality/records",
                Map.of("reviewJobId", reviewJobId.toString(),
                        "usefulnessScore", 4,
                        "falsePositiveScore", 1,
                        "reviewComment", "Good review overall"));
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(data.get("usefulnessScore").asInt()).isEqualTo(4);
        assertThat(data.get("falsePositiveScore").asInt()).isEqualTo(1);
    }

    @Test
    void shouldReturn404WhenCreatingWithNonExistentReviewJob() {
        ResponseEntity<String> res = post("/api/projects/" + projectIdValue + "/pr-review-quality/records",
                Map.of("reviewJobId", "999999999"));
        assertCode(res, "NOT_FOUND");
    }

    @Test
    void shouldUpdateQualityRecord() {
        // First create a record
        ResponseEntity<String> createRes = post("/api/projects/" + projectIdValue + "/pr-review-quality/records",
                Map.of("reviewJobId", reviewJobId.toString()));
        assertOk(createRes);
        JsonNode createdData = TestJsonHelper.parse(createRes.getBody()).get("data");
        String recordId = TestJsonHelper.getString(createdData, "id");

        // Then update it
        ResponseEntity<String> updateRes = put("/api/pr-review-quality/records/" + recordId,
                Map.of("humanFeedbackStatus", "REVIEWED",
                        "adoptionStatus", "PARTIAL",
                        "usefulnessScore", 5,
                        "falsePositiveScore", 0));
        assertOk(updateRes);
        JsonNode data = TestJsonHelper.parse(updateRes.getBody()).get("data");
        assertThat(TestJsonHelper.getString(data, "humanFeedbackStatus")).isEqualTo("REVIEWED");
        assertThat(TestJsonHelper.getString(data, "adoptionStatus")).isEqualTo("PARTIAL");
        assertThat(data.get("usefulnessScore").asInt()).isEqualTo(5);
    }

    @Test
    void shouldReturn404WhenUpdatingNonExistentRecord() {
        ResponseEntity<String> res = put("/api/pr-review-quality/records/999999999",
                Map.of("humanFeedbackStatus", "REVIEWED"));
        assertCode(res, "NOT_FOUND");
    }

    @Test
    void shouldListQualityRecords() {
        ResponseEntity<String> res = get("/api/projects/" + projectIdValue + "/pr-review-quality/records");
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(data).isNotNull();
    }

    @Test
    void shouldListQualityRecordsWithFilters() {
        ResponseEntity<String> res = get("/api/projects/" + projectIdValue
                + "/pr-review-quality/records?status=COMPLETED");
        assertOk(res);
    }

    @Test
    void shouldListQualityRecordsWithPagination() {
        ResponseEntity<String> res = get("/api/projects/" + projectIdValue
                + "/pr-review-quality/records?page=1&size=10");
        assertOk(res);
    }

    @Test
    void shouldGetQualityDashboard() {
        ResponseEntity<String> res = get("/api/projects/" + projectIdValue + "/pr-review-quality/dashboard");
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(data.has("totalReviews")).isTrue();
        assertThat(data.has("highValueReviews")).isTrue();
        assertThat(data.has("actionableReviews")).isTrue();
        assertThat(data.has("lowSignalReviews")).isTrue();
        assertThat(data.has("failedReviews")).isTrue();
        assertThat(data.has("pendingFeedbackReviews")).isTrue();
        assertThat(data.has("adoptedReviews")).isTrue();
        assertThat(data.has("averageUsefulnessScore")).isTrue();
        assertThat(data.has("recentReviews")).isTrue();
    }

    @Test
    void shouldHandleInvalidProjectId() {
        ResponseEntity<String> res = get("/api/projects/invalid/pr-review-quality/dashboard");
        assertCode(res, "BAD_REQUEST");
    }
}
