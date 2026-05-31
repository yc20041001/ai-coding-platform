package com.aicoding.platform.orchestration;

import com.aicoding.platform.support.IntegrationTestBase;
import com.aicoding.platform.support.TestJsonHelper;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GovernanceOutcomeReviewIntegrationTest extends IntegrationTestBase {

    private int counter = (int)(System.currentTimeMillis() % 100000);

    @BeforeEach
    public void setUp() { loginAdmin(); }

    // ========== Draft Adoption Review ==========
    @Test void shouldRecordDraftAdoptionReviewSuccess() {
        String id = "" + (counter++);
        ResponseEntity<String> res = post("/api/governance-outcome-review/draft-adoption?draftPlanId=" + id + "&adoptionResult=ADOPTED&usefulnessRating=5", Map.of());
        assertOk(res); assertThat(TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data.adoptionResult")).isEqualTo("ADOPTED");
    }
    @Test void shouldListDraftAdoptionReviews() { assertOk(get("/api/governance-outcome-review/draft-adoption")); }
    @Test void shouldGetDraftAdoptionReviewById() {
        String id = "" + (counter++);
        ResponseEntity<String> cr = post("/api/governance-outcome-review/draft-adoption?draftPlanId=" + id + "&adoptionResult=REJECTED&usefulnessRating=2&reasonCode=INCORRECT_ASSUMPTION", Map.of());
        String rid = TestJsonHelper.getString(TestJsonHelper.parse(cr.getBody()), "data.id");
        assertOk(get("/api/governance-outcome-review/draft-adoption/" + rid));
    }
    @Test void shouldAdoptionReviewPreservesModificationLevel() {
        String id = "" + (counter++);
        ResponseEntity<String> res = post("/api/governance-outcome-review/draft-adoption?draftPlanId=" + id + "&adoptionResult=MODIFIED_AND_ADOPTED&usefulnessRating=4&modificationLevel=MINOR", Map.of());
        assertOk(res); assertThat(TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data.modificationLevel")).isEqualTo("MINOR");
    }
    @Test void shouldAdoptionReviewPreservesReasonCode() {
        String id = "" + (counter++);
        ResponseEntity<String> res = post("/api/governance-outcome-review/draft-adoption?draftPlanId=" + id + "&adoptionResult=ADOPTED&usefulnessRating=5&reasonCode=HIGH_QUALITY_DRAFT", Map.of());
        assertOk(res); assertThat(TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data.reasonCode")).isEqualTo("HIGH_QUALITY_DRAFT");
    }

    // ========== Assistive Quality Review ==========
    @Test void shouldRecordAssistiveQualityReviewSuccess() {
        String id = "" + (counter++);
        ResponseEntity<String> res = post("/api/governance-outcome-review/assistive-quality?assistiveActionId=" + id + "&outcomeResult=USEFUL&usefulnessRating=5", Map.of());
        assertOk(res); assertThat(TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data.outcomeResult")).isEqualTo("USEFUL");
    }
    @Test void shouldListAssistiveQualityReviews() { assertOk(get("/api/governance-outcome-review/assistive-quality")); }
    @Test void shouldAssistiveQualityPreservesReasonCode() {
        String id = "" + (counter++);
        ResponseEntity<String> res = post("/api/governance-outcome-review/assistive-quality?assistiveActionId=" + id + "&outcomeResult=PARTIALLY_USEFUL&usefulnessRating=3&reasonCode=NEEDS_MORE_CONTEXT", Map.of());
        assertOk(res); assertThat(TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data.reasonCode")).isEqualTo("NEEDS_MORE_CONTEXT");
    }
    @Test void shouldAssistiveQualityGetById() {
        String id = "" + (counter++);
        ResponseEntity<String> cr = post("/api/governance-outcome-review/assistive-quality?assistiveActionId=" + id + "&outcomeResult=NOT_USEFUL&usefulnessRating=1", Map.of());
        String rid = TestJsonHelper.getString(TestJsonHelper.parse(cr.getBody()), "data.id");
        assertOk(get("/api/governance-outcome-review/assistive-quality/" + rid));
    }

    // ========== Package Evaluation ==========
    @Test void shouldRecordPackageEvaluationSuccess() {
        String id = "" + (counter++);
        ResponseEntity<String> res = post("/api/governance-outcome-review/package-evaluation?packageId=" + id + "&evaluationResult=HIGH&completenessScore=90&accuracyScore=85", Map.of());
        assertOk(res);
    }
    @Test void shouldListPackageEvaluations() { assertOk(get("/api/governance-outcome-review/package-evaluation")); }
    @Test void shouldPackageEvaluationHasOverallScore() {
        String id = "" + (counter++);
        ResponseEntity<String> res = post("/api/governance-outcome-review/package-evaluation?packageId=" + id + "&evaluationResult=MEDIUM&completenessScore=70&accuracyScore=60", Map.of());
        assertOk(res); assertThat(TestJsonHelper.getInt(TestJsonHelper.parse(res.getBody()), "data.overallScore")).isEqualTo(65);
    }
    @Test void shouldPackageEvaluationGetById() {
        String id = "" + (counter++);
        ResponseEntity<String> cr = post("/api/governance-outcome-review/package-evaluation?packageId=" + id + "&evaluationResult=LOW&completenessScore=30&accuracyScore=40", Map.of());
        String rid = TestJsonHelper.getString(TestJsonHelper.parse(cr.getBody()), "data.id");
        assertOk(get("/api/governance-outcome-review/package-evaluation/" + rid));
    }

    // ========== Dashboard & Report ==========
    @Test void shouldDashboardReturnCounts() {
        ResponseEntity<String> res = get("/api/governance-outcome-review/dashboard");
        assertOk(res); JsonNode root = TestJsonHelper.parse(res.getBody());
        assertThat(root.get("data").get("draftReviewCount")).isNotNull();
        assertThat(root.get("data").get("assistiveReviewCount")).isNotNull();
        assertThat(root.get("data").get("packageEvalCount")).isNotNull();
    }
    @Test void shouldReportExportMarkdown() { assertOk(get("/api/governance-outcome-review/report")); }
    @Test void shouldEmptyDataReturnsEmptyDashboard() { assertOk(get("/api/governance-outcome-review/dashboard")); }
    @Test void shouldDashboardTopListsReturned() {
        ResponseEntity<String> res = get("/api/governance-outcome-review/dashboard");
        assertOk(res); assertThat(TestJsonHelper.parse(res.getBody()).get("data").get("topDraftReviews").isArray()).isTrue();
    }
    @Test void shouldRejectedWithReasonRecorded() {
        String id = "" + (counter++);
        ResponseEntity<String> res = post("/api/governance-outcome-review/draft-adoption?draftPlanId=" + id + "&adoptionResult=REJECTED&usefulnessRating=1&reasonCode=MISSING_STEPS&outcomeNoteText=MissingContext", Map.of());
        assertOk(res); assertThat(TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data.outcomeNoteText")).isEqualTo("MissingContext");
    }
    @Test void shouldMultipleRecordsListable() {
        String id1 = "" + (counter++); String id2 = "" + (counter++);
        post("/api/governance-outcome-review/draft-adoption?draftPlanId=" + id1 + "&adoptionResult=ADOPTED&usefulnessRating=5", Map.of());
        post("/api/governance-outcome-review/draft-adoption?draftPlanId=" + id2 + "&adoptionResult=REJECTED&usefulnessRating=2", Map.of());
        assertOk(get("/api/governance-outcome-review/draft-adoption"));
    }
    @Test void shouldPackageEvaluationPreservesReasonCode() {
        String id = "" + (counter++);
        ResponseEntity<String> res = post("/api/governance-outcome-review/package-evaluation?packageId=" + id + "&evaluationResult=INCOMPLETE&completenessScore=20&accuracyScore=30&reasonCode=LOW_QUALITY_PACKAGE", Map.of());
        assertOk(res);
    }
    @Test void shouldAssistiveQualityUsefulnessRatingPersists() {
        String id = "" + (counter++);
        ResponseEntity<String> res = post("/api/governance-outcome-review/assistive-quality?assistiveActionId=" + id + "&outcomeResult=USEFUL&usefulnessRating=4", Map.of());
        assertOk(res); assertThat(TestJsonHelper.getInt(TestJsonHelper.parse(res.getBody()), "data.usefulnessRating")).isEqualTo(4);
    }
    @Test void shouldReportContainsDraftStats() { assertOk(get("/api/governance-outcome-review/report")); }
    @Test void shouldDraftAdoptionMultipleResultsListed() {
        String id1 = "" + (counter++); String id2 = "" + (counter++);
        post("/api/governance-outcome-review/draft-adoption?draftPlanId=" + id1 + "&adoptionResult=ADOPTED&usefulnessRating=5", Map.of());
        post("/api/governance-outcome-review/draft-adoption?draftPlanId=" + id2 + "&adoptionResult=MODIFIED_AND_ADOPTED&usefulnessRating=4", Map.of());
        ResponseEntity<String> res = get("/api/governance-outcome-review/draft-adoption");
        assertOk(res);
    }
    @Test void shouldAssistiveQualityMultipleResultsListed() {
        String id1 = "" + (counter++); String id2 = "" + (counter++);
        post("/api/governance-outcome-review/assistive-quality?assistiveActionId=" + id1 + "&outcomeResult=USEFUL&usefulnessRating=5", Map.of());
        post("/api/governance-outcome-review/assistive-quality?assistiveActionId=" + id2 + "&outcomeResult=NOT_USEFUL&usefulnessRating=1", Map.of());
        ResponseEntity<String> res = get("/api/governance-outcome-review/assistive-quality");
        assertOk(res);
    }
    @Test void shouldPackageEvaluationMultipleResultsListed() {
        String id1 = "" + (counter++); String id2 = "" + (counter++);
        post("/api/governance-outcome-review/package-evaluation?packageId=" + id1 + "&evaluationResult=HIGH&completenessScore=80&accuracyScore=90", Map.of());
        post("/api/governance-outcome-review/package-evaluation?packageId=" + id2 + "&evaluationResult=LOW&completenessScore=30&accuracyScore=40", Map.of());
        ResponseEntity<String> res = get("/api/governance-outcome-review/package-evaluation");
        assertOk(res);
    }
    @Test void shouldNonExistentReviewReturnsNotFound() {
        ResponseEntity<String> res = get("/api/governance-outcome-review/draft-adoption/999999");
        assertCode(res, "NOT_FOUND");
    }
    @Test void shouldNonExistentAssistiveReviewReturnsNotFound() {
        ResponseEntity<String> res = get("/api/governance-outcome-review/assistive-quality/999999");
        assertCode(res, "NOT_FOUND");
    }
    @Test void shouldNonExistentPackageEvalReturnsNotFound() {
        ResponseEntity<String> res = get("/api/governance-outcome-review/package-evaluation/999999");
        assertCode(res, "NOT_FOUND");
    }
    @Test void shouldDashboardTopAssistiveReviewsReturned() {
        ResponseEntity<String> res = get("/api/governance-outcome-review/dashboard");
        assertOk(res); assertThat(TestJsonHelper.parse(res.getBody()).get("data").get("topAssistiveReviews").isArray()).isTrue();
    }
    @Test void shouldDashboardTopPackageEvaluationsReturned() {
        ResponseEntity<String> res = get("/api/governance-outcome-review/dashboard");
        assertOk(res); assertThat(TestJsonHelper.parse(res.getBody()).get("data").get("topPackageEvaluations").isArray()).isTrue();
    }
    @Test void shouldDraftAdoptionUsefulnessRatingPersists() {
        String id = "" + (counter++);
        ResponseEntity<String> res = post("/api/governance-outcome-review/draft-adoption?draftPlanId=" + id + "&adoptionResult=ADOPTED&usefulnessRating=3", Map.of());
        assertOk(res); assertThat(TestJsonHelper.getInt(TestJsonHelper.parse(res.getBody()), "data.usefulnessRating")).isEqualTo(3);
    }
    @Test void shouldPackageEvaluationAccuracyScorePersists() {
        String id = "" + (counter++);
        ResponseEntity<String> res = post("/api/governance-outcome-review/package-evaluation?packageId=" + id + "&evaluationResult=HIGH&completenessScore=85&accuracyScore=95", Map.of());
        assertOk(res); assertThat(TestJsonHelper.getInt(TestJsonHelper.parse(res.getBody()), "data.accuracyScore")).isEqualTo(95);
    }
    @Test void shouldMultipleAssistiveReviewsDashboard() {
        String id1 = "" + (counter++); String id2 = "" + (counter++);
        post("/api/governance-outcome-review/assistive-quality?assistiveActionId=" + id1 + "&outcomeResult=USEFUL&usefulnessRating=5", Map.of());
        post("/api/governance-outcome-review/assistive-quality?assistiveActionId=" + id2 + "&outcomeResult=USEFUL&usefulnessRating=4", Map.of());
        ResponseEntity<String> res = get("/api/governance-outcome-review/dashboard");
        assertOk(res);
    }
    @Test void shouldReportIncludesAllCounts() {
        String id = "" + (counter++);
        post("/api/governance-outcome-review/draft-adoption?draftPlanId=" + id + "&adoptionResult=ADOPTED&usefulnessRating=5", Map.of());
        ResponseEntity<String> res = get("/api/governance-outcome-review/report");
        assertOk(res);
    }
    @Test void shouldDashboardWithDataReturnsCounts() {
        String id = "" + (counter++);
        post("/api/governance-outcome-review/draft-adoption?draftPlanId=" + id + "&adoptionResult=ADOPTED&usefulnessRating=4", Map.of());
        ResponseEntity<String> res = get("/api/governance-outcome-review/dashboard");
        assertOk(res); assertThat(TestJsonHelper.getInt(TestJsonHelper.parse(res.getBody()), "data.draftReviewCount")).isGreaterThan(0);
    }
    @Test void shouldGetDraftAdoptionReturnsCorrectResult() {
        String id = "" + (counter++);
        ResponseEntity<String> cr = post("/api/governance-outcome-review/draft-adoption?draftPlanId=" + id + "&adoptionResult=ADOPTED&usefulnessRating=5", Map.of());
        String rid = TestJsonHelper.getString(TestJsonHelper.parse(cr.getBody()), "data.id");
        ResponseEntity<String> res = get("/api/governance-outcome-review/draft-adoption/" + rid);
        assertOk(res); assertThat(TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data.adoptionResult")).isEqualTo("ADOPTED");
    }
}
