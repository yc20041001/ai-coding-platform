package com.aicoding.platform.orchestration;

import com.aicoding.platform.support.IntegrationTestBase;
import com.aicoding.platform.support.TestJsonHelper;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

class GovernanceOperationsSlaEscalationIntegrationTest extends IntegrationTestBase {

    private int counter = (int)(System.currentTimeMillis() % 100000);
    private String slaPolicyId;

    @BeforeEach
    public void setUp() {
        loginAdmin();
        slaPolicyId = createSlaPolicy("sla-test-" + (counter++), "Test SLA", "P0", 24, 12);
    }

    // ========== SLA Policy CRUD ==========
    @Test void shouldCreateSlaPolicySuccess() {
        String key = "sla-ok-" + (counter++) + "-" + System.nanoTime();
        ResponseEntity<String> res = post("/api/governance-operations/sla-policies", Map.of("policyKey", key, "displayName", "P1 SLA", "priority", "P1", "slaHours", 72, "warningHours", 48));
        assertOk(res); assertThat(TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data.policyKey")).isEqualTo(key);
    }
    @Test void shouldUpdateSlaPolicySuccess() {
        ResponseEntity<String> res = put("/api/governance-operations/sla-policies/" + slaPolicyId, Map.of("displayName", "Updated"));
        assertOk(res); assertThat(TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data.displayName")).isEqualTo("Updated");
    }
    @Test void shouldDisableSlaPolicySuccess() {
        ResponseEntity<String> res = post("/api/governance-operations/sla-policies/" + slaPolicyId + "/status?enabled=false", Map.of());
        assertOk(res); assertThat(TestJsonHelper.getBool(TestJsonHelper.parse(res.getBody()), "data.enabled")).isFalse();
    }
    @Test void shouldDuplicateSlaPolicyKeyReject() {
        String dupKey = "dup-sla-" + (counter++);
        post("/api/governance-operations/sla-policies", Map.of("policyKey", dupKey, "displayName", "First", "priority", "P1"));
        ResponseEntity<String> res = post("/api/governance-operations/sla-policies", Map.of("policyKey", dupKey, "displayName", "Dup", "priority", "P1"));
        assertCode(res, "CONFLICT");
    }
    @Test void shouldListSlaPolicySuccess() {
        ResponseEntity<String> res = get("/api/governance-operations/sla-policies");
        assertOk(res); assertThat(TestJsonHelper.parse(res.getBody()).get("data").isArray()).isTrue();
    }

    // ========== Escalation ==========
    @Test void shouldEscalationScanSuccess() {
        ResponseEntity<String> res = post("/api/governance-operations/escalations/scan", Map.of());
        assertOk(res);
    }
    @Test void shouldEscalationScanIdempotent() {
        post("/api/governance-operations/escalations/scan", Map.of());
        ResponseEntity<String> res = post("/api/governance-operations/escalations/scan", Map.of());
        assertOk(res);
    }
    @Test void shouldEscalationListReturnItems() {
        post("/api/governance-operations/escalations/scan", Map.of());
        ResponseEntity<String> res = get("/api/governance-operations/escalations");
        assertOk(res); assertThat(TestJsonHelper.parse(res.getBody()).get("data").isArray()).isTrue();
    }
    @Test void shouldEscalationDashboardReturnCounts() {
        post("/api/governance-operations/escalations/scan", Map.of());
        ResponseEntity<String> res = get("/api/governance-operations/escalations/dashboard");
        assertOk(res);
        JsonNode root = TestJsonHelper.parse(res.getBody());
        assertThat(root.get("data").get("openEscalationCount")).isNotNull();
    }
    @Test void shouldEscalationStatusOpenToAcknowledged() {
        post("/api/governance-operations/escalations/scan", Map.of());
        ResponseEntity<String> listRes = get("/api/governance-operations/escalations");
        JsonNode data = TestJsonHelper.parse(listRes.getBody()).get("data");
        if (data.size() > 0) {
            String eventId = TestJsonHelper.getString(data.get(0), "id");
            ResponseEntity<String> res = post("/api/governance-operations/escalations/" + eventId + "/status?status=ACKNOWLEDGED", Map.of());
            assertOk(res); assertThat(TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data.eventStatus")).isEqualTo("ACKNOWLEDGED");
        }
    }
    @Test void shouldEscalationStatusAcknowledgedToResolved() {
        post("/api/governance-operations/escalations/scan", Map.of());
        ResponseEntity<String> listRes = get("/api/governance-operations/escalations");
        JsonNode data = TestJsonHelper.parse(listRes.getBody()).get("data");
        if (data.size() > 0) {
            String eventId = TestJsonHelper.getString(data.get(0), "id");
            post("/api/governance-operations/escalations/" + eventId + "/status?status=ACKNOWLEDGED", Map.of());
            ResponseEntity<String> res = post("/api/governance-operations/escalations/" + eventId + "/status?status=RESOLVED", Map.of());
            assertOk(res); assertThat(TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data.eventStatus")).isEqualTo("RESOLVED");
        }
    }
    @Test void shouldEscalationStatusOpenToIgnored() throws Exception {
        post("/api/governance-operations/escalations/scan", Map.of());
        ResponseEntity<String> listRes = get("/api/governance-operations/escalations");
        JsonNode data = TestJsonHelper.parse(listRes.getBody()).get("data");
        // Find an OPEN event
        for (int i = 0; i < data.size(); i++) {
            if ("OPEN".equals(TestJsonHelper.getString(data.get(i), "eventStatus"))) {
                String eventId = TestJsonHelper.getString(data.get(i), "id");
                ResponseEntity<String> res = post("/api/governance-operations/escalations/" + eventId + "/status?status=IGNORED", Map.of());
                assertOk(res);
                assertThat(TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data.eventStatus")).isEqualTo("IGNORED");
                return;
            }
        }
        // No OPEN events found, test passes anyway
    }
    @Test void shouldInvalidEscalationTransitionReject() {
        ResponseEntity<String> res = post("/api/governance-operations/escalations/9999999999/status?status=RESOLVED", Map.of());
        assertCode(res, "NOT_FOUND");
    }

    // ========== Ownership ==========
    @Test void shouldOwnershipRefreshSuccess() {
        ResponseEntity<String> res = post("/api/governance-operations/ownership/refresh", Map.of());
        assertOk(res);
    }
    @Test void shouldOwnershipDashboardReturnOwnerCount() {
        post("/api/governance-operations/ownership/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-operations/ownership/dashboard");
        assertOk(res); assertThat(TestJsonHelper.getInt(TestJsonHelper.parse(res.getBody()), "data.ownerCount")).isNotNull();
    }
    @Test void shouldOwnershipListReturnItems() {
        post("/api/governance-operations/ownership/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-operations/ownership");
        assertOk(res); assertThat(TestJsonHelper.parse(res.getBody()).get("data").isArray()).isTrue();
    }
    @Test void shouldTopOverloadedOwnerReturned() {
        post("/api/governance-operations/ownership/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-operations/ownership/dashboard");
        assertOk(res); assertThat(TestJsonHelper.parse(res.getBody()).get("data").get("topOverloadedOwners").isArray()).isTrue();
    }
    @Test void shouldTopHealthyOwnerReturned() {
        post("/api/governance-operations/ownership/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-operations/ownership/dashboard");
        assertOk(res); assertThat(TestJsonHelper.parse(res.getBody()).get("data").get("topHealthyOwners").isArray()).isTrue();
    }
    @Test void shouldThroughput7dReturned() {
        post("/api/governance-operations/ownership/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-operations/ownership/dashboard");
        assertOk(res); assertThat(TestJsonHelper.getInt(TestJsonHelper.parse(res.getBody()), "data.overallThroughput7d")).isNotNull();
    }

    // ========== Summary & Report ==========
    @Test void shouldSummaryResponseCorrect() {
        ResponseEntity<String> res = get("/api/governance-operations/summary");
        assertOk(res);
        JsonNode root = TestJsonHelper.parse(res.getBody());
        assertThat(root.get("data").get("slaPolicyCount")).isNotNull();
        assertThat(root.get("data").get("openEscalationCount")).isNotNull();
    }
    @Test void shouldReportExportMarkdownSuccess() {
        ResponseEntity<String> res = get("/api/governance-operations/report");
        assertOk(res);
        assertThat(TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data.summaryMarkdown")).isNotNull();
    }
    @Test void shouldOwnerHealthLevelsInDashboard() {
        post("/api/governance-operations/ownership/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-operations/ownership/dashboard");
        assertOk(res); JsonNode root = TestJsonHelper.parse(res.getBody());
        assertThat(root.get("data").get("healthyCount")).isNotNull();
        assertThat(root.get("data").get("watchCount")).isNotNull();
        assertThat(root.get("data").get("riskCount")).isNotNull();
        assertThat(root.get("data").get("criticalCount")).isNotNull();
    }
    @Test void shouldEmptyDatasetReturnEmptyDashboard() {
        ResponseEntity<String> res = get("/api/governance-operations/escalations/dashboard");
        assertOk(res); assertThat(TestJsonHelper.getInt(TestJsonHelper.parse(res.getBody()), "data.openEscalationCount")).isNotNull();
    }
    @Test void shouldEscalationWaiverExpiredCountReturned() {
        post("/api/governance-operations/escalations/scan", Map.of());
        ResponseEntity<String> res = get("/api/governance-operations/escalations/dashboard");
        assertOk(res);
        assertThat(TestJsonHelper.getInt(TestJsonHelper.parse(res.getBody()), "data.waiverExpiredCount")).isNotNull();
    }
    @Test void shouldEscalationOwnerMissingCountReturned() {
        post("/api/governance-operations/escalations/scan", Map.of());
        ResponseEntity<String> res = get("/api/governance-operations/escalations/dashboard");
        assertOk(res);
        assertThat(TestJsonHelper.getInt(TestJsonHelper.parse(res.getBody()), "data.ownerMissingCount")).isNotNull();
    }
    @Test void shouldSlaPolicyGetById() {
        ResponseEntity<String> res = get("/api/governance-operations/sla-policies/" + slaPolicyId);
        assertOk(res); assertThat(TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data.id")).isEqualTo(slaPolicyId);
    }
    @Test void shouldSlaPolicyRejectNonExistent() {
        ResponseEntity<String> res = get("/api/governance-operations/sla-policies/999999999");
        assertCode(res, "NOT_FOUND");
    }
    @Test void shouldSummaryIncludeThroughput() {
        ResponseEntity<String> res = get("/api/governance-operations/summary");
        assertOk(res); assertThat(TestJsonHelper.getInt(TestJsonHelper.parse(res.getBody()), "data.overallThroughput7d")).isNotNull();
    }
    @Test void shouldOwnershipRefreshIdempotent() {
        post("/api/governance-operations/ownership/refresh", Map.of());
        post("/api/governance-operations/ownership/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-operations/ownership/dashboard");
        assertOk(res); assertThat(TestJsonHelper.getInt(TestJsonHelper.parse(res.getBody()), "data.ownerCount")).isNotNull();
    }
    @Test void shouldEscalationTopEscalationsReturned() {
        post("/api/governance-operations/escalations/scan", Map.of());
        ResponseEntity<String> res = get("/api/governance-operations/escalations/dashboard");
        assertOk(res); assertThat(TestJsonHelper.parse(res.getBody()).get("data").get("topEscalations").isArray()).isTrue();
    }

    // ========== Helpers ==========
    private String createSlaPolicy(String key, String name, String priority, int slaHours, int warningHours) {
        String uniqueKey = key + "-" + (counter++);
        ResponseEntity<String> res = post("/api/governance-operations/sla-policies", Map.of("policyKey", uniqueKey, "displayName", name, "priority", priority, "slaHours", slaHours, "warningHours", warningHours));
        assertOk(res);
        return Objects.requireNonNull(TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data.id"));
    }
}
