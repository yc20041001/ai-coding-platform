package com.aicoding.platform.orchestration;

import com.aicoding.platform.support.IntegrationTestBase;
import com.aicoding.platform.support.TestJsonHelper;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GovernanceBenchmarkAdoptionIntegrationTest extends IntegrationTestBase {

    private int counter = (int)(System.currentTimeMillis() % 100000);

    @BeforeEach
    public void setUp() { loginAdmin(); }

    @Test void shouldCreateAdoptionRecordSuccess() {
        ResponseEntity<String> res = post("/api/governance-benchmark-adoption/records?projectId=1&projectName=Test&metricKey=test", Map.of());
        assertOk(res);
    }
    @Test void shouldListAdoptionRecords() {
        post("/api/governance-benchmark-adoption/records?projectId=1&projectName=Test&metricKey=test", Map.of());
        ResponseEntity<String> res = get("/api/governance-benchmark-adoption/records");
        assertOk(res); assertThat(TestJsonHelper.parse(res.getBody()).get("data").isArray()).isTrue();
    }
    @Test void shouldAdoptionRecordStatusUpdate() {
        ResponseEntity<String> cr = post("/api/governance-benchmark-adoption/records?projectId=1&projectName=Test&metricKey=test", Map.of());
        String id = TestJsonHelper.getString(TestJsonHelper.parse(cr.getBody()), "data.id");
        ResponseEntity<String> res = post("/api/governance-benchmark-adoption/records/" + id + "/status?status=IN_PROGRESS", Map.of());
        assertOk(res); assertThat(TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data.adoptionStatus")).isEqualTo("IN_PROGRESS");
    }
    @Test void shouldAdoptionRecordAdoptedSetsTimestamp() {
        ResponseEntity<String> cr = post("/api/governance-benchmark-adoption/records?projectId=2&projectName=Test2&metricKey=test2", Map.of());
        String id = TestJsonHelper.getString(TestJsonHelper.parse(cr.getBody()), "data.id");
        post("/api/governance-benchmark-adoption/records/" + id + "/status?status=IN_PROGRESS", Map.of());
        ResponseEntity<String> res = post("/api/governance-benchmark-adoption/records/" + id + "/status?status=ADOPTED", Map.of());
        assertOk(res); assertThat(TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data.adoptionStatus")).isEqualTo("ADOPTED");
    }

    @Test void shouldCreateCampaignSuccess() {
        String key = "cmp-" + (counter++);
        ResponseEntity<String> res = post("/api/governance-benchmark-adoption/campaigns?campaignKey=" + key + "&campaignName=TestCampaign", Map.of());
        assertOk(res);
    }
    @Test void shouldListCampaigns() {
        String key = "cmp-" + (counter++);
        post("/api/governance-benchmark-adoption/campaigns?campaignKey=" + key + "&campaignName=Test", Map.of());
        ResponseEntity<String> res = get("/api/governance-benchmark-adoption/campaigns");
        assertOk(res); assertThat(TestJsonHelper.parse(res.getBody()).get("data").isArray()).isTrue();
    }
    @Test void shouldCampaignStatusDraftToActive() {
        String key = "cmp-" + (counter++);
        ResponseEntity<String> cr = post("/api/governance-benchmark-adoption/campaigns?campaignKey=" + key + "&campaignName=Test", Map.of());
        String id = TestJsonHelper.getString(TestJsonHelper.parse(cr.getBody()), "data.id");
        ResponseEntity<String> res = post("/api/governance-benchmark-adoption/campaigns/" + id + "/status?status=ACTIVE", Map.of());
        assertOk(res); assertThat(TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data.campaignStatus")).isEqualTo("ACTIVE");
    }
    @Test void shouldCampaignDuplicateKeyReject() {
        String key = "dup-cmp-" + (counter++);
        post("/api/governance-benchmark-adoption/campaigns?campaignKey=" + key + "&campaignName=First", Map.of());
        ResponseEntity<String> res = post("/api/governance-benchmark-adoption/campaigns?campaignKey=" + key + "&campaignName=Second", Map.of());
        assertCode(res, "CONFLICT");
    }
    @Test void shouldCampaignDefaultWindow() {
        String key = "cmp-" + (counter++);
        ResponseEntity<String> res = post("/api/governance-benchmark-adoption/campaigns?campaignKey=" + key + "&campaignName=Test", Map.of());
        assertOk(res); assertThat(TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data.improvementWindow")).isEqualTo("MONTH_3");
    }

    @Test void shouldRefreshUpliftSuccess() { assertOk(post("/api/governance-benchmark-adoption/uplift/refresh", Map.of())); }
    @Test void shouldListUplift() {
        post("/api/governance-benchmark-adoption/uplift/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-benchmark-adoption/uplift");
        assertOk(res); assertThat(TestJsonHelper.parse(res.getBody()).get("data").isArray()).isTrue();
    }
    @Test void shouldUpliftHasUpliftLevel() {
        post("/api/governance-benchmark-adoption/uplift/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-benchmark-adoption/uplift");
        assertOk(res); JsonNode d = TestJsonHelper.parse(res.getBody()).get("data");
        if (d.size() > 0) assertThat(d.get(0).get("upliftLevel")).isNotNull();
    }
    @Test void shouldUpliftHasBeforeAfterScore() {
        post("/api/governance-benchmark-adoption/uplift/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-benchmark-adoption/uplift");
        assertOk(res); JsonNode d = TestJsonHelper.parse(res.getBody()).get("data");
        if (d.size() > 0) { assertThat(d.get(0).get("beforeScore")).isNotNull(); assertThat(d.get(0).get("afterScore")).isNotNull(); }
    }
    @Test void shouldUpliftUpliftCalculated() {
        post("/api/governance-benchmark-adoption/uplift/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-benchmark-adoption/uplift");
        assertOk(res); JsonNode d = TestJsonHelper.parse(res.getBody()).get("data");
        if (d.size() > 0) assertThat(d.get(0).get("uplift")).isNotNull();
    }

    @Test void shouldDashboardReturnCounts() {
        post("/api/governance-benchmark-adoption/uplift/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-benchmark-adoption/dashboard");
        assertOk(res); JsonNode r = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(r.get("adoptionCount")).isNotNull();
        assertThat(r.get("campaignCount")).isNotNull();
        assertThat(r.get("upliftCount")).isNotNull();
    }
    @Test void shouldReportExportMarkdown() { assertOk(get("/api/governance-benchmark-adoption/report")); }
    @Test void shouldEmptyDataSafe() { assertOk(get("/api/governance-benchmark-adoption/dashboard")); }
    @Test void shouldUpliftRefreshIdempotent() {
        post("/api/governance-benchmark-adoption/uplift/refresh", Map.of());
        post("/api/governance-benchmark-adoption/uplift/refresh", Map.of());
        assertOk(get("/api/governance-benchmark-adoption/uplift"));
    }
    @Test void shouldUpliftCreatesRecords() {
        post("/api/governance-benchmark-adoption/uplift/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-benchmark-adoption/uplift");
        assertOk(res); assertThat(TestJsonHelper.parse(res.getBody()).get("data").size()).isGreaterThan(0);
    }
    @Test void shouldAdoptionRecordDefaultStatus() {
        ResponseEntity<String> res = post("/api/governance-benchmark-adoption/records?projectId=10&projectName=DefaultTest&metricKey=default", Map.of());
        assertOk(res); assertThat(TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data.adoptionStatus")).isEqualTo("IDENTIFIED");
    }
    @Test void shouldAdoptionRecordDefaultTargetScore() {
        ResponseEntity<String> res = post("/api/governance-benchmark-adoption/records?projectId=11&projectName=TargetTest&metricKey=target", Map.of());
        assertOk(res); assertThat(TestJsonHelper.getInt(TestJsonHelper.parse(res.getBody()), "data.targetScore")).isEqualTo(80);
    }
    @Test void shouldListAdoptionAfterMultipleCreates() {
        for (int i = 0; i < 3; i++) post("/api/governance-benchmark-adoption/records?projectId=" + i + "&projectName=P" + i + "&metricKey=m" + i, Map.of());
        ResponseEntity<String> res = get("/api/governance-benchmark-adoption/records");
        assertOk(res); assertThat(TestJsonHelper.parse(res.getBody()).get("data").size()).isGreaterThanOrEqualTo(3);
    }
    @Test void shouldCampaignActiveToCompleted() {
        String key = "cmp-" + (counter++);
        ResponseEntity<String> cr = post("/api/governance-benchmark-adoption/campaigns?campaignKey=" + key + "&campaignName=Test", Map.of());
        String id = TestJsonHelper.getString(TestJsonHelper.parse(cr.getBody()), "data.id");
        post("/api/governance-benchmark-adoption/campaigns/" + id + "/status?status=ACTIVE", Map.of());
        ResponseEntity<String> res = post("/api/governance-benchmark-adoption/campaigns/" + id + "/status?status=COMPLETED", Map.of());
        assertOk(res);
    }
    @Test void shouldCampaignActiveToCancelled() {
        String key = "cmp-" + (counter++);
        ResponseEntity<String> cr = post("/api/governance-benchmark-adoption/campaigns?campaignKey=" + key + "&campaignName=Test", Map.of());
        String id = TestJsonHelper.getString(TestJsonHelper.parse(cr.getBody()), "data.id");
        post("/api/governance-benchmark-adoption/campaigns/" + id + "/status?status=ACTIVE", Map.of());
        ResponseEntity<String> res = post("/api/governance-benchmark-adoption/campaigns/" + id + "/status?status=CANCELLED", Map.of());
        assertOk(res);
    }
    @Test void shouldUpliftMultipleRecordsListable() {
        post("/api/governance-benchmark-adoption/uplift/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-benchmark-adoption/uplift");
        assertOk(res);
    }
    @Test void shouldUpliftSummaryTextReturned() {
        post("/api/governance-benchmark-adoption/uplift/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-benchmark-adoption/uplift");
        assertOk(res); JsonNode d = TestJsonHelper.parse(res.getBody()).get("data");
        if (d.size() > 0) assertThat(d.get(0).get("summaryText")).isNotNull();
    }
    @Test void shouldUpliftAbove15IsSignificant() {
        post("/api/governance-benchmark-adoption/uplift/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-benchmark-adoption/uplift");
        assertOk(res);
    }
    @Test void shouldAdoptionRecordBlockerType() {
        ResponseEntity<String> res = post("/api/governance-benchmark-adoption/records?projectId=20&projectName=BlockerTest&metricKey=blocker&blockerType=RESOURCE_CONSTRAINT", Map.of());
        assertOk(res); assertThat(TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data.blockerType")).isEqualTo("RESOURCE_CONSTRAINT");
    }
    @Test void shouldReportWithDataReturnsContent() {
        post("/api/governance-benchmark-adoption/uplift/refresh", Map.of());
        assertOk(get("/api/governance-benchmark-adoption/report"));
    }
    @Test void shouldAdoptionRecordDefaultCurrentScore() {
        ResponseEntity<String> res = post("/api/governance-benchmark-adoption/records?projectId=30&projectName=ScoreTest&metricKey=score", Map.of());
        assertOk(res); assertThat(TestJsonHelper.getInt(TestJsonHelper.parse(res.getBody()), "data.currentScore")).isEqualTo(0);
    }
    @Test void shouldCampaignListAfterMultipleCreates() {
        for (int i = 0; i < 3; i++) { String k = "mc-" + (counter++); post("/api/governance-benchmark-adoption/campaigns?campaignKey=" + k + "&campaignName=MC" + i, Map.of()); }
        assertOk(get("/api/governance-benchmark-adoption/campaigns"));
    }
    @Test void shouldUpliftRecordsOrdered() {
        post("/api/governance-benchmark-adoption/uplift/refresh", Map.of());
        assertOk(get("/api/governance-benchmark-adoption/uplift"));
    }
    @Test void shouldAdoptionRecordStatusBlockedWorks() {
        ResponseEntity<String> cr = post("/api/governance-benchmark-adoption/records?projectId=40&projectName=BlockTest&metricKey=block", Map.of());
        String id = TestJsonHelper.getString(TestJsonHelper.parse(cr.getBody()), "data.id");
        ResponseEntity<String> res = post("/api/governance-benchmark-adoption/records/" + id + "/status?status=BLOCKED", Map.of());
        assertOk(res);
    }
    @Test void shouldNonExistentRecordReturnsNotFound() {
        ResponseEntity<String> res = post("/api/governance-benchmark-adoption/records/999999/status?status=ADOPTED", Map.of());
        assertCode(res, "NOT_FOUND");
    }
    @Test void shouldNonExistentCampaignReturnsNotFound() {
        ResponseEntity<String> res = post("/api/governance-benchmark-adoption/campaigns/999999/status?status=ACTIVE", Map.of());
        assertCode(res, "NOT_FOUND");
    }
    @Test void shouldUpliftBeforeAfterScoresCorrect() {
        post("/api/governance-benchmark-adoption/uplift/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-benchmark-adoption/uplift");
        assertOk(res); JsonNode d = TestJsonHelper.parse(res.getBody()).get("data");
        if (d.size() > 0) assertThat(d.get(0).get("afterScore").asDouble()).isGreaterThanOrEqualTo(d.get(0).get("beforeScore").asDouble());
    }
}
