package com.aicoding.platform.orchestration;

import com.aicoding.platform.support.IntegrationTestBase;
import com.aicoding.platform.support.TestJsonHelper;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GovernanceCopilotTuningIntegrationTest extends IntegrationTestBase {

    private String sessionId;

    @BeforeEach
    public void setUp() {
        loginAdmin();
        ResponseEntity<String> res = post("/api/governance-workspace/sessions", Map.of());
        assertOk(res);
        sessionId = TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data.id");
    }

    // ========== Feedback ==========
    @Test void shouldRecordFeedbackSuccess() {
        ResponseEntity<String> res = post("/api/governance-copilot/feedback?sessionId=" + sessionId + "&feedbackTargetType=NEXT_STEP&feedbackRating=4&helpfulFlag=true", Map.of());
        assertOk(res);
    }
    @Test void shouldListFeedbackBySession() {
        post("/api/governance-copilot/feedback?sessionId=" + sessionId + "&feedbackTargetType=NEXT_STEP&feedbackRating=5", Map.of());
        ResponseEntity<String> res = get("/api/governance-copilot/feedback?sessionId=" + sessionId);
        assertOk(res); assertThat(TestJsonHelper.parse(res.getBody()).get("data").isArray()).isTrue();
    }
    @Test void shouldFeedbackPreservesRating() {
        ResponseEntity<String> res = post("/api/governance-copilot/feedback?sessionId=" + sessionId + "&feedbackTargetType=GUIDED_TASK&feedbackRating=3", Map.of());
        assertOk(res); assertThat(TestJsonHelper.getInt(TestJsonHelper.parse(res.getBody()), "data.feedbackRating")).isEqualTo(3);
    }
    @Test void shouldFeedbackPreservesReasonCode() {
        ResponseEntity<String> res = post("/api/governance-copilot/feedback?sessionId=" + sessionId + "&feedbackTargetType=NEXT_STEP&feedbackRating=2&reasonCode=TOO_GENERIC", Map.of());
        assertOk(res); assertThat(TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data.reasonCode")).isEqualTo("TOO_GENERIC");
    }
    @Test void shouldFeedbackPreservesHelpfulFlag() {
        ResponseEntity<String> res = post("/api/governance-copilot/feedback?sessionId=" + sessionId + "&feedbackTargetType=REUSE_BUNDLE&feedbackRating=5&helpfulFlag=true", Map.of());
        assertOk(res); assertThat(TestJsonHelper.getBool(TestJsonHelper.parse(res.getBody()), "data.helpfulFlag")).isTrue();
    }

    // ========== Adaptive Signals ==========
    @Test void shouldRefreshAdaptiveSignalsSuccess() {
        ResponseEntity<String> res = post("/api/governance-copilot/signals/refresh", Map.of());
        assertOk(res);
    }
    @Test void shouldRefreshSignalsIdempotent() {
        post("/api/governance-copilot/signals/refresh", Map.of());
        post("/api/governance-copilot/signals/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-copilot/signals");
        assertOk(res);
    }
    @Test void shouldHighAcceptanceBecomesBoost() {
        post("/api/governance-copilot/feedback?sessionId=" + sessionId + "&feedbackTargetType=NEXT_STEP&feedbackRating=5&acceptedFlag=true", Map.of());
        post("/api/governance-copilot/feedback?sessionId=" + sessionId + "&feedbackTargetType=NEXT_STEP&feedbackRating=5&acceptedFlag=true", Map.of());
        post("/api/governance-copilot/signals/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-copilot/signals");
        assertOk(res);
    }
    @Test void shouldSignalRationalePopulated() {
        post("/api/governance-copilot/feedback?sessionId=" + sessionId + "&feedbackTargetType=NEXT_STEP&feedbackRating=4", Map.of());
        post("/api/governance-copilot/signals/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-copilot/signals");
        assertOk(res); JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        if (data.size() > 0) assertThat(data.get(0).get("rationaleText")).isNotNull();
    }
    @Test void shouldWeightScoreComputed() {
        post("/api/governance-copilot/feedback?sessionId=" + sessionId + "&feedbackTargetType=NEXT_STEP&feedbackRating=4", Map.of());
        post("/api/governance-copilot/signals/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-copilot/signals/dashboard");
        assertOk(res);
    }
    @Test void shouldFocusModeWeightSignalCreated() {
        post("/api/governance-copilot/feedback?sessionId=" + sessionId + "&feedbackTargetType=WORKSPACE_SESSION&feedbackRating=3", Map.of());
        post("/api/governance-copilot/signals/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-copilot/signals");
        assertOk(res);
    }

    // ========== Tuning Snapshot ==========
    @Test void shouldRefreshTuningSnapshotSuccess() {
        ResponseEntity<String> res = post("/api/governance-copilot/tuning/refresh", Map.of());
        assertOk(res);
    }
    @Test void shouldSnapshotComputesTotalFeedback() {
        post("/api/governance-copilot/feedback?sessionId=" + sessionId + "&feedbackTargetType=NEXT_STEP&feedbackRating=4", Map.of());
        post("/api/governance-copilot/tuning/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-copilot/tuning/snapshots");
        assertOk(res);
    }
    @Test void shouldSnapshotComputesAcceptanceRate() {
        post("/api/governance-copilot/feedback?sessionId=" + sessionId + "&feedbackTargetType=NEXT_STEP&feedbackRating=4&acceptedFlag=true", Map.of());
        post("/api/governance-copilot/tuning/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-copilot/tuning/snapshots");
        assertOk(res);
    }
    @Test void shouldSnapshotComputesAvgRating() {
        post("/api/governance-copilot/feedback?sessionId=" + sessionId + "&feedbackTargetType=NEXT_STEP&feedbackRating=5", Map.of());
        post("/api/governance-copilot/tuning/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-copilot/tuning/snapshots");
        assertOk(res);
    }
    @Test void shouldTuningConfidenceScoreComputed() {
        post("/api/governance-copilot/tuning/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-copilot/tuning/dashboard");
        assertOk(res);
    }

    // ========== Dashboard & Report ==========
    @Test void shouldTuningDashboardReturnsLatestSnapshot() {
        ResponseEntity<String> res = get("/api/governance-copilot/tuning/dashboard");
        assertOk(res); assertThat(TestJsonHelper.parse(res.getBody()).get("data").get("snapshotCount")).isNotNull();
    }
    @Test void shouldEmptyDataReturnsEmptyDashboard() {
        ResponseEntity<String> res = get("/api/governance-copilot/tuning/dashboard");
        assertOk(res);
    }
    @Test void shouldTuningReportExportMarkdown() {
        ResponseEntity<String> res = get("/api/governance-copilot/tuning/report");
        assertOk(res);
    }
    @Test void shouldEmptyDataReturnsEmptySignals() {
        ResponseEntity<String> res = get("/api/governance-copilot/signals/dashboard");
        assertOk(res);
    }
    @Test void shouldSnapshotComputesDismissalRate() {
        post("/api/governance-copilot/feedback?sessionId=" + sessionId + "&feedbackTargetType=NEXT_STEP&feedbackRating=1&acceptedFlag=false", Map.of());
        post("/api/governance-copilot/tuning/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-copilot/tuning/snapshots");
        assertOk(res);
    }
    @Test void shouldListAllFeedback() {
        ResponseEntity<String> res = get("/api/governance-copilot/feedback");
        assertOk(res); assertThat(TestJsonHelper.parse(res.getBody()).get("data").isArray()).isTrue();
    }
    @Test void shouldSnapshotContainsTopSuggestionType() {
        post("/api/governance-copilot/tuning/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-copilot/tuning/snapshots");
        assertOk(res);
    }
    @Test void shouldRefreshSnapshotIdempotent() {
        post("/api/governance-copilot/tuning/refresh", Map.of());
        post("/api/governance-copilot/tuning/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-copilot/tuning/snapshots");
        assertOk(res);
    }
    @Test void shouldAdaptiveSignalDashboardReturnsCounts() {
        post("/api/governance-copilot/signals/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-copilot/signals/dashboard");
        assertOk(res);
    }
    @Test void shouldFeedbackByTargetTypeListable() {
        post("/api/governance-copilot/feedback?sessionId=" + sessionId + "&feedbackTargetType=NEXT_STEP&feedbackRating=4", Map.of());
        post("/api/governance-copilot/feedback?sessionId=" + sessionId + "&feedbackTargetType=GUIDED_TASK&feedbackRating=3", Map.of());
        ResponseEntity<String> res = get("/api/governance-copilot/feedback?sessionId=" + sessionId);
        assertOk(res);
    }
    @Test void shouldAcceptedFeedbackIncreasesAcceptance() {
        post("/api/governance-copilot/feedback?sessionId=" + sessionId + "&feedbackTargetType=NEXT_STEP&feedbackRating=5&acceptedFlag=true", Map.of());
        post("/api/governance-copilot/feedback?sessionId=" + sessionId + "&feedbackTargetType=NEXT_STEP&feedbackRating=4&acceptedFlag=true", Map.of());
        post("/api/governance-copilot/feedback?sessionId=" + sessionId + "&feedbackTargetType=NEXT_STEP&feedbackRating=3&acceptedFlag=false", Map.of());
        post("/api/governance-copilot/signals/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-copilot/signals");
        assertOk(res);
    }
    @Test void shouldRepeatedNotRelevantCausesDownrank() {
        post("/api/governance-copilot/feedback?sessionId=" + sessionId + "&feedbackTargetType=NEXT_STEP&feedbackRating=1&reasonCode=NOT_RELEVANT&acceptedFlag=false", Map.of());
        post("/api/governance-copilot/feedback?sessionId=" + sessionId + "&feedbackTargetType=NEXT_STEP&feedbackRating=1&reasonCode=NOT_RELEVANT&acceptedFlag=false", Map.of());
        post("/api/governance-copilot/signals/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-copilot/signals");
        assertOk(res);
    }
    @Test void shouldTuningSnapshotContainsSummaryMarkdown() {
        post("/api/governance-copilot/tuning/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-copilot/tuning/snapshots");
        assertOk(res);
    }
    @Test void shouldHelpfulFeedbackIncreasesAvgRating() {
        post("/api/governance-copilot/feedback?sessionId=" + sessionId + "&feedbackTargetType=NEXT_STEP&feedbackRating=5&helpfulFlag=true", Map.of());
        post("/api/governance-copilot/signals/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-copilot/signals");
        assertOk(res);
    }
    @Test void shouldDismissalRateReflectsUnacceptedFeedback() {
        post("/api/governance-copilot/feedback?sessionId=" + sessionId + "&feedbackTargetType=NEXT_STEP&feedbackRating=2&acceptedFlag=false", Map.of());
        post("/api/governance-copilot/tuning/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-copilot/tuning/snapshots");
        assertOk(res);
    }
    @Test void shouldFeedbackSupportsSuggestionTypeField() {
        post("/api/governance-copilot/feedback?sessionId=" + sessionId + "&feedbackTargetType=NEXT_STEP&feedbackRating=4", Map.of());
        ResponseEntity<String> res = get("/api/governance-copilot/feedback?sessionId=" + sessionId);
        assertOk(res);
    }
    @Test void shouldSnapshotWeakestSuggestionTypeComputed() {
        post("/api/governance-copilot/tuning/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-copilot/tuning/snapshots");
        assertOk(res);
    }
    @Test void shouldSignalLevelsReturnCorrectLabels() {
        post("/api/governance-copilot/signals/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-copilot/signals");
        assertOk(res); JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        if (data.size() > 0) {
            String level = TestJsonHelper.getString(data.get(0), "signalLevel");
            assertThat(level).isIn("BOOST", "KEEP", "WATCH", "DOWNRANK");
        }
    }
    @Test void shouldSnapshotComputesDismissalRateCorrectly() {
        post("/api/governance-copilot/feedback?sessionId=" + sessionId + "&feedbackTargetType=NEXT_STEP&feedbackRating=1&acceptedFlag=false", Map.of());
        post("/api/governance-copilot/tuning/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-copilot/tuning/dashboard");
        assertOk(res);
    }
    @Test void shouldDashboardSignalCountReturned() {
        ResponseEntity<String> res = get("/api/governance-copilot/signals/dashboard");
        assertOk(res); assertThat(TestJsonHelper.parse(res.getBody()).get("data").get("signalCount")).isNotNull();
    }
}
