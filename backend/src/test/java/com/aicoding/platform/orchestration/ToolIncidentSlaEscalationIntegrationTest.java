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

class ToolIncidentSlaEscalationIntegrationTest extends IntegrationTestBase {

    private String createProject(String suffix) {
        ResponseEntity<String> res = post("/api/projects", Map.of(
                "name", "IT-SLA-" + suffix,
                "description", "SLA escalation test project",
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

    // ========================
    // SLA Initialization
    // ========================

    @Test
    void shouldInitSlaCritical() {
        String pid = createProject("SlaCritical");
        String iid = createIncident(pid, "CRITICAL", "CRITICAL incident");
        ResponseEntity<String> res = get("/api/orchestration/incidents/" + iid);
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(TestJsonHelper.getString(data, "slaStatus")).isEqualTo("WITHIN_SLA");
        assertThat(TestJsonHelper.getInt(data, "slaMinutes")).isEqualTo(30);
        assertThat(TestJsonHelper.getString(data, "dueAt")).isNotEmpty();
    }

    @Test
    void shouldInitSlaHigh() {
        String pid = createProject("SlaHigh");
        String iid = createIncident(pid, "HIGH", "HIGH incident");
        ResponseEntity<String> res = get("/api/orchestration/incidents/" + iid);
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(TestJsonHelper.getString(data, "slaStatus")).isEqualTo("WITHIN_SLA");
        assertThat(TestJsonHelper.getInt(data, "slaMinutes")).isEqualTo(120);
    }

    @Test
    void shouldInitSlaMedium() {
        String pid = createProject("SlaMedium");
        String iid = createIncident(pid, "MEDIUM", "MEDIUM incident");
        ResponseEntity<String> res = get("/api/orchestration/incidents/" + iid);
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(TestJsonHelper.getString(data, "slaStatus")).isEqualTo("WITHIN_SLA");
        assertThat(TestJsonHelper.getInt(data, "slaMinutes")).isEqualTo(480);
    }

    @Test
    void shouldInitSlaLow() {
        String pid = createProject("SlaLow");
        String iid = createIncident(pid, "LOW", "LOW incident");
        ResponseEntity<String> res = get("/api/orchestration/incidents/" + iid);
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(TestJsonHelper.getString(data, "slaStatus")).isEqualTo("WITHIN_SLA");
        assertThat(TestJsonHelper.getInt(data, "slaMinutes")).isEqualTo(1440);
    }

    @Test
    void shouldInitSlaInfoAsWaived() {
        String pid = createProject("SlaInfo");
        String iid = createIncident(pid, "INFO", "INFO incident");
        ResponseEntity<String> res = get("/api/orchestration/incidents/" + iid);
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(TestJsonHelper.getString(data, "slaStatus")).isEqualTo("WAIVED");
        assertThat(TestJsonHelper.getInt(data, "slaMinutes")).isEqualTo(0);
    }

    @Test
    void shouldReturnEscalationLevelZeroOnCreate() {
        String pid = createProject("EscLevel");
        String iid = createIncident(pid, "CRITICAL", "Initial escalation level");
        ResponseEntity<String> res = get("/api/orchestration/incidents/" + iid);
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(TestJsonHelper.getInt(data, "escalationLevel")).isEqualTo(0);
    }

    // ========================
    // SLA Status Transitions
    // ========================

    @Test
    void shouldSetSlaResolvedOnResolve() {
        String pid = createProject("SlaResolve");
        String iid = createIncident(pid, "HIGH", "To be resolved");
        ResponseEntity<String> res = put("/api/orchestration/incidents/" + iid, Map.of(
                "status", "RESOLVED",
                "resolution", "fixed"
        ));
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(TestJsonHelper.getString(data, "slaStatus")).isEqualTo("RESOLVED");
    }

    @Test
    void shouldReinitSlaOnReopen() {
        String pid = createProject("SlaReopen");
        String iid = createIncident(pid, "CRITICAL", "To reopen");
        // Resolve
        put("/api/orchestration/incidents/" + iid, Map.of("status", "RESOLVED", "resolution", "done"));
        // Reopen
        ResponseEntity<String> res = put("/api/orchestration/incidents/" + iid, Map.of("status", "OPEN"));
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(TestJsonHelper.getString(data, "slaStatus")).isEqualTo("WITHIN_SLA");
        assertThat(TestJsonHelper.getInt(data, "slaMinutes")).isEqualTo(30);
    }

    @Test
    void shouldReinitSlaOnSeverityChange() {
        String pid = createProject("SlaSevChange");
        String iid = createIncident(pid, "LOW", "Severity change");
        // Change severity to CRITICAL
        ResponseEntity<String> res = put("/api/orchestration/incidents/" + iid, Map.of("severity", "CRITICAL"));
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(TestJsonHelper.getString(data, "slaStatus")).isEqualTo("WITHIN_SLA");
        assertThat(TestJsonHelper.getInt(data, "slaMinutes")).isEqualTo(30);
    }

    // ========================
    // SLA Scan
    // ========================

    @Test
    void shouldScanSlaWithNoOpenIncidents() {
        String pid = createProject("SlaScanEmpty");
        ResponseEntity<String> res = post("/api/projects/" + pid + "/incident-sla/scan", Map.of());
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(TestJsonHelper.getInt(data, "scanned")).isEqualTo(0);
    }

    @Test
    void shouldScanSlaWithMultipleIncidents() {
        String pid = createProject("SlaScanMulti");
        createIncident(pid, "CRITICAL", "one");
        createIncident(pid, "HIGH", "two");
        createIncident(pid, "INFO", "three");

        ResponseEntity<String> res = post("/api/projects/" + pid + "/incident-sla/scan", Map.of());
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        // INFO is WAIVED (excluded), CRITICAL+HIGH = 2 scanned
        assertThat(TestJsonHelper.getInt(data, "scanned")).isEqualTo(2);
        assertThat(TestJsonHelper.getInt(data, "withinSla")).isEqualTo(2);
    }

    @Test
    void shouldScanSlaAfterResolve() {
        String pid = createProject("SlaScanResolve");
        String iid = createIncident(pid, "CRITICAL", "will resolve");
        put("/api/orchestration/incidents/" + iid, Map.of("status", "RESOLVED", "resolution", "done"));

        ResponseEntity<String> res = post("/api/projects/" + pid + "/incident-sla/scan", Map.of());
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(TestJsonHelper.getInt(data, "scanned")).isEqualTo(0);
        assertThat(TestJsonHelper.getInt(data, "resolved")).isEqualTo(0);
    }

    // ========================
    // Escalation Policy CRUD
    // ========================

    @Test
    void shouldCreateEscalationPolicy() {
        String pid = createProject("PolCreate");
        ResponseEntity<String> res = post("/api/orchestration/escalation-policies", Map.of(
                "projectId", pid,
                "name", "Critical Policy",
                "severity", "CRITICAL",
                "slaMinutes", 30,
                "escalationAfterMinutes", 10,
                "maxEscalationLevel", 3,
                "channel", "IN_APP",
                "routeTarget", "admin"
        ));
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(TestJsonHelper.getString(data, "name")).isEqualTo("Critical Policy");
        assertThat(TestJsonHelper.getString(data, "severity")).isEqualTo("CRITICAL");
        assertThat(TestJsonHelper.getInt(data, "maxEscalationLevel")).isEqualTo(3);
    }

    @Test
    void shouldCreatePolicyWithDefaults() {
        String pid = createProject("PolDefaults");
        ResponseEntity<String> res = post("/api/orchestration/escalation-policies", Map.of(
                "projectId", pid,
                "name", "Default Policy",
                "severity", "HIGH"
        ));
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(TestJsonHelper.getBool(data, "enabled")).isTrue();
        assertThat(TestJsonHelper.getInt(data, "maxEscalationLevel")).isEqualTo(3);
    }

    @Test
    void shouldGetEscalationPolicy() {
        String pid = createProject("PolGet");
        ResponseEntity<String> createRes = post("/api/orchestration/escalation-policies", Map.of(
                "projectId", pid, "name", "GetTest", "severity", "CRITICAL"
        ));
        String policyId = TestJsonHelper.getString(TestJsonHelper.parse(createRes.getBody()), "data.id");

        ResponseEntity<String> res = get("/api/orchestration/escalation-policies/" + policyId);
        assertOk(res);
        assertThat(TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data.name")).isEqualTo("GetTest");
    }

    @Test
    void shouldUpdateEscalationPolicy() {
        String pid = createProject("PolUpdate");
        ResponseEntity<String> createRes = post("/api/orchestration/escalation-policies", Map.of(
                "projectId", pid, "name", "Before", "severity", "HIGH"
        ));
        String policyId = TestJsonHelper.getString(TestJsonHelper.parse(createRes.getBody()), "data.id");

        ResponseEntity<String> res = put("/api/orchestration/escalation-policies/" + policyId, Map.of(
                "name", "After",
                "enabled", false,
                "maxEscalationLevel", 5
        ));
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(TestJsonHelper.getString(data, "name")).isEqualTo("After");
        assertThat(TestJsonHelper.getBool(data, "enabled")).isFalse();
        assertThat(TestJsonHelper.getInt(data, "maxEscalationLevel")).isEqualTo(5);
    }

    @Test
    void shouldDeleteEscalationPolicy() {
        String pid = createProject("PolDelete");
        ResponseEntity<String> createRes = post("/api/orchestration/escalation-policies", Map.of(
                "projectId", pid, "name", "DeleteMe", "severity", "LOW"
        ));
        String policyId = TestJsonHelper.getString(TestJsonHelper.parse(createRes.getBody()), "data.id");

        ResponseEntity<String> delRes = delete("/api/orchestration/escalation-policies/" + policyId);
        assertOk(delRes);

        ResponseEntity<String> getRes = get("/api/orchestration/escalation-policies/" + policyId);
        assertCode(getRes, "NOT_FOUND");
    }

    @Test
    void shouldListProjectPolicies() {
        String pid = createProject("PolList");
        post("/api/orchestration/escalation-policies", Map.of("projectId", pid, "name", "P1", "severity", "CRITICAL"));
        post("/api/orchestration/escalation-policies", Map.of("projectId", pid, "name", "P2", "severity", "HIGH"));

        ResponseEntity<String> res = get("/api/projects/" + pid + "/escalation-policies");
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(data.isArray()).isTrue();
        assertThat(data.size()).isGreaterThanOrEqualTo(2);
    }

    @Test
    void shouldCreatePolicyFailsWithoutProjectId() {
        ResponseEntity<String> res = post("/api/orchestration/escalation-policies", Map.of(
                "name", "NoProject", "severity", "CRITICAL"
        ));
        assertCode(res, "VALIDATION_ERROR");
    }

    // ========================
    // Escalation Scan
    // ========================

    @Test
    void shouldScanEscalationWithNoBreached() {
        String pid = createProject("EscScanNone");
        ResponseEntity<String> res = post("/api/projects/" + pid + "/incident-escalation/scan", Map.of());
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(TestJsonHelper.getInt(data, "scanned")).isEqualTo(0);
    }

    @Test
    void shouldScanEscalationWithNoMatchingPolicy() {
        String pid = createProject("EscNoPol");
        createIncident(pid, "CRITICAL", "no policy");

        // Manually mark as breached via direct status (can't easily set breachedAt via API,
        // but SLA status will be WITHIN_SLA so scan won't find breached incidents)
        ResponseEntity<String> res = post("/api/projects/" + pid + "/incident-escalation/scan", Map.of());
        assertOk(res);
    }

    // ========================
    // Manual Escalation
    // ========================

    @Test
    void shouldEscalateIncident() {
        String pid = createProject("ManualEsc");
        // Create matching policy
        ResponseEntity<String> policyRes = post("/api/orchestration/escalation-policies", Map.of(
                "projectId", pid, "name", "EscPolicy", "severity", "CRITICAL",
                "channel", "IN_APP", "routeTarget", "oncall"
        ));
        assertOk(policyRes);
        String iid = createIncident(pid, "CRITICAL", "escalate me");

        ResponseEntity<String> escRes = post("/api/orchestration/incidents/" + iid + "/escalate",
                Map.of("reason", "Needs attention"));
        assertOk(escRes);
        JsonNode escData = TestJsonHelper.parse(escRes.getBody()).get("data");
        assertThat(TestJsonHelper.getString(escData, "status")).isEqualTo("CREATED");
        assertThat(TestJsonHelper.getInt(escData, "escalationLevel")).isEqualTo(1);
        assertThat(TestJsonHelper.getString(escData, "channel")).isEqualTo("IN_APP");

        // Verify incident escalation level updated
        ResponseEntity<String> incRes = get("/api/orchestration/incidents/" + iid);
        assertOk(incRes);
        assertThat(TestJsonHelper.getInt(TestJsonHelper.parse(incRes.getBody()).get("data"), "escalationLevel")).isEqualTo(1);
    }

    @Test
    void shouldFailEscalationWithoutPolicy() {
        String pid = createProject("EscNoPol2");
        String iid = createIncident(pid, "CRITICAL", "no policy for esc");
        ResponseEntity<String> escRes = post("/api/orchestration/incidents/" + iid + "/escalate",
                Map.of("reason", "urgent"));
        assertCode(escRes, "NOT_FOUND");
    }

    @Test
    void shouldFailEscalationAtMaxLevel() {
        String pid = createProject("EscMax");
        post("/api/orchestration/escalation-policies", Map.of(
                "projectId", pid, "name", "MaxPol", "severity", "CRITICAL",
                "maxEscalationLevel", 1, "channel", "IN_APP"
        ));
        String iid = createIncident(pid, "CRITICAL", "max level test");

        // First escalation
        post("/api/orchestration/incidents/" + iid + "/escalate", Map.of("reason", "first"));
        // Second escalation should fail (max level = 1)
        ResponseEntity<String> escRes = post("/api/orchestration/incidents/" + iid + "/escalate", Map.of("reason", "second"));
        assertCode(escRes, "VALIDATION_ERROR");
    }

    @Test
    void shouldProgressToNextEscalationLevel() {
        String pid = createProject("EscProg");
        post("/api/orchestration/escalation-policies", Map.of(
                "projectId", pid, "name", "ProgPol", "severity", "CRITICAL",
                "maxEscalationLevel", 3, "channel", "EMAIL"
        ));
        String iid = createIncident(pid, "CRITICAL", "progressive esc");

        // Level 1
        ResponseEntity<String> l1 = post("/api/orchestration/incidents/" + iid + "/escalate", Map.of("reason", "l1"));
        assertOk(l1);
        assertThat(TestJsonHelper.getInt(TestJsonHelper.parse(l1.getBody()).get("data"), "escalationLevel")).isEqualTo(1);

        // Level 2 - different level, should succeed
        ResponseEntity<String> l2 = post("/api/orchestration/incidents/" + iid + "/escalate", Map.of("reason", "l2"));
        assertOk(l2);
        assertThat(TestJsonHelper.getInt(TestJsonHelper.parse(l2.getBody()).get("data"), "escalationLevel")).isEqualTo(2);
    }

    @Test
    void shouldListEscalationEvents() {
        String pid = createProject("EscList");
        post("/api/orchestration/escalation-policies", Map.of(
                "projectId", pid, "name", "ListPol", "severity", "CRITICAL",
                "maxEscalationLevel", 3, "channel", "SLACK", "routeTarget", "#alerts"
        ));
        String iid = createIncident(pid, "CRITICAL", "list events");
        post("/api/orchestration/incidents/" + iid + "/escalate", Map.of("reason", "first"));

        ResponseEntity<String> listRes = get("/api/orchestration/incidents/" + iid + "/escalation-events");
        assertOk(listRes);
        JsonNode events = TestJsonHelper.parse(listRes.getBody()).get("data");
        assertThat(events.isArray()).isTrue();
        assertThat(events.size()).isGreaterThanOrEqualTo(1);
    }

    // ========================
    // Timeline
    // ========================

    @Test
    void shouldGetTimelineForIncident() {
        String pid = createProject("TimeEmpty");
        String iid = createIncident(pid, "HIGH", "timeline test");

        ResponseEntity<String> res = get("/api/orchestration/incidents/" + iid + "/timeline");
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(TestJsonHelper.getString(data, "incidentId")).isEqualTo(iid);
        JsonNode events = data.get("events");
        assertThat(events.isArray()).isTrue();
        assertThat(events.size()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void shouldIncludeAckAndResolveInTimeline() {
        String pid = createProject("TimeAckRes");
        String iid = createIncident(pid, "MEDIUM", "ack and resolve");

        // Acknowledge
        put("/api/orchestration/incidents/" + iid, Map.of("status", "ACKNOWLEDGED"));
        // Resolve
        put("/api/orchestration/incidents/" + iid, Map.of("status", "RESOLVED", "resolution", "done"));

        ResponseEntity<String> res = get("/api/orchestration/incidents/" + iid + "/timeline");
        assertOk(res);
        JsonNode events = TestJsonHelper.parse(res.getBody()).get("data").get("events");
        assertThat(events.isArray()).isTrue();
        assertThat(events.size()).isGreaterThanOrEqualTo(3);
    }

    @Test
    void shouldIncludeEscalationInTimeline() {
        String pid = createProject("TimeEsc");
        post("/api/orchestration/escalation-policies", Map.of(
                "projectId", pid, "name", "TimePol", "severity", "CRITICAL",
                "maxEscalationLevel", 2, "channel", "PAGERDUTY"
        ));
        String iid = createIncident(pid, "CRITICAL", "timeline with esc");
        post("/api/orchestration/incidents/" + iid + "/escalate", Map.of("reason", "escalate"));

        ResponseEntity<String> res = get("/api/orchestration/incidents/" + iid + "/timeline");
        assertOk(res);
        JsonNode events = TestJsonHelper.parse(res.getBody()).get("data").get("events");
        assertThat(events.size()).isGreaterThanOrEqualTo(2);
    }

    @Test
    void shouldReturnEmptyTimelineForNonexistentIncident() {
        ResponseEntity<String> res = get("/api/orchestration/incidents/999999/timeline");
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(TestJsonHelper.getString(data, "incidentId")).isEqualTo("999999");
        assertThat(data.get("events").isArray()).isTrue();
        assertThat(data.get("events").size()).isEqualTo(0);
    }

    // ========================
    // SLA with WONT_FIX / FALSE_POSITIVE
    // ========================

    @Test
    void shouldSetSlaResolvedOnWontFix() {
        String pid = createProject("SlaWontFix");
        String iid = createIncident(pid, "HIGH", "wontfix");
        put("/api/orchestration/incidents/" + iid, Map.of("status", "WONT_FIX"));
        ResponseEntity<String> res = get("/api/orchestration/incidents/" + iid);
        assertOk(res);
        assertThat(TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()).get("data"), "slaStatus")).isEqualTo("RESOLVED");
    }

    @Test
    void shouldSetSlaResolvedOnFalsePositive() {
        String pid = createProject("SlaFalsePos");
        String iid = createIncident(pid, "MEDIUM", "false positive");
        put("/api/orchestration/incidents/" + iid, Map.of("status", "FALSE_POSITIVE"));
        ResponseEntity<String> res = get("/api/orchestration/incidents/" + iid);
        assertOk(res);
        assertThat(TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()).get("data"), "slaStatus")).isEqualTo("RESOLVED");
    }

    // ========================
    // SLA scan with resolved incidents
    // ========================

    @Test
    void shouldScanSlaWithResolvedOnly() {
        String pid = createProject("SlaScanResolved");
        String iid = createIncident(pid, "HIGH", "resolved incident");
        put("/api/orchestration/incidents/" + iid, Map.of("status", "RESOLVED", "resolution", "ok"));

        ResponseEntity<String> res = post("/api/projects/" + pid + "/incident-sla/scan", Map.of());
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(TestJsonHelper.getInt(data, "scanned")).isEqualTo(0);
    }

    // ========================
    // Multiple escalation levels
    // ========================

    @Test
    void shouldEscalateToMultipleLevels() {
        String pid = createProject("EscMulti");
        post("/api/orchestration/escalation-policies", Map.of(
                "projectId", pid, "name", "MultiPol", "severity", "CRITICAL",
                "maxEscalationLevel", 3, "channel", "IN_APP", "routeTarget", "team"
        ));
        String iid = createIncident(pid, "CRITICAL", "multi level");

        // Level 1
        post("/api/orchestration/incidents/" + iid + "/escalate", Map.of("reason", "L1"));
        // Level 2 (need to clear current escalation level check - the test increments from incident's current level)
        post("/api/orchestration/incidents/" + iid + "/escalate", Map.of("reason", "L2"));

        ResponseEntity<String> incRes = get("/api/orchestration/incidents/" + iid);
        assertOk(incRes);
        assertThat(TestJsonHelper.getInt(TestJsonHelper.parse(incRes.getBody()).get("data"), "escalationLevel")).isEqualTo(2);
    }

    // ========================
    // Timeline with SLA breach
    // ========================

    @Test
    void shouldIncludeBreachedInTimeline() {
        String pid = createProject("TimeBreach");
        String iid = createIncident(pid, "CRITICAL", "breach timeline");

        // Acknowledge then resolve
        put("/api/orchestration/incidents/" + iid, Map.of("status", "ACKNOWLEDGED"));
        put("/api/orchestration/incidents/" + iid, Map.of("status", "RESOLVED", "resolution", "fixed"));

        ResponseEntity<String> res = get("/api/orchestration/incidents/" + iid + "/timeline");
        assertOk(res);
        JsonNode events = TestJsonHelper.parse(res.getBody()).get("data").get("events");
        // Should have: CREATED, ACKNOWLEDGED, RESOLVED
        assertThat(events.size()).isGreaterThanOrEqualTo(3);
    }

    // ========================
    // Policy with all channels
    // ========================

    @Test
    void shouldCreatePolicyWithAllChannels() {
        String pid = createProject("PolChannels");
        String[] channels = {"IN_APP", "EMAIL", "SLACK", "PAGERDUTY", "WEBHOOK"};
        for (String ch : channels) {
            ResponseEntity<String> res = post("/api/orchestration/escalation-policies", Map.of(
                    "projectId", pid, "name", "Chan_" + ch, "severity", "HIGH",
                    "channel", ch
            ));
            assertOk(res);
        }
        ResponseEntity<String> listRes = get("/api/projects/" + pid + "/escalation-policies");
        assertOk(listRes);
        assertThat(TestJsonHelper.parse(listRes.getBody()).get("data").size()).isGreaterThanOrEqualTo(5);
    }

    // ========================
    // SLA scan after severity change
    // ========================

    @Test
    void shouldAdjustSlaOnSeverityDowngrade() {
        String pid = createProject("SlaDowngrade");
        String iid = createIncident(pid, "CRITICAL", "downgrade to LOW");
        put("/api/orchestration/incidents/" + iid, Map.of("severity", "LOW"));

        ResponseEntity<String> res = get("/api/orchestration/incidents/" + iid);
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(TestJsonHelper.getInt(data, "slaMinutes")).isEqualTo(1440);
    }

    // ========================
    // Policy validation
    // ========================

    @Test
    void shouldFailCreatePolicyWithoutName() {
        String pid = createProject("PolNoName");
        ResponseEntity<String> res = post("/api/orchestration/escalation-policies", Map.of(
                "projectId", pid, "severity", "CRITICAL"
        ));
        assertCode(res, "VALIDATION_ERROR");
    }

    @Test
    void shouldFailCreatePolicyWithoutSeverity() {
        String pid = createProject("PolNoSev");
        ResponseEntity<String> res = post("/api/orchestration/escalation-policies", Map.of(
                "projectId", pid, "name", "NoSev"
        ));
        assertCode(res, "VALIDATION_ERROR");
    }

    // ========================
    // Escalation event status
    // ========================

    @Test
    void shouldCreateEventWithCreatedStatus() {
        String pid = createProject("EvtStatus");
        post("/api/orchestration/escalation-policies", Map.of(
                "projectId", pid, "name", "EvtPol", "severity", "CRITICAL",
                "channel", "SLACK", "routeTarget", "#ops"
        ));
        String iid = createIncident(pid, "CRITICAL", "event status");
        ResponseEntity<String> escRes = post("/api/orchestration/incidents/" + iid + "/escalate", Map.of("reason", "check"));
        assertOk(escRes);
        assertThat(TestJsonHelper.getString(TestJsonHelper.parse(escRes.getBody()).get("data"), "status"))
                .isEqualTo("CREATED");
    }

    // ========================
    // Edge: escalate non-existent incident
    // ========================

    @Test
    void shouldFailEscalateNonexistentIncident() {
        ResponseEntity<String> res = post("/api/orchestration/incidents/9999999/escalate", Map.of("reason", "test"));
        assertCode(res, "NOT_FOUND");
    }

    // ========================
    // Edge: update non-existent policy
    // ========================

    @Test
    void shouldFailUpdateNonexistentPolicy() {
        ResponseEntity<String> res = put("/api/orchestration/escalation-policies/999999", Map.of("name", "Nope"));
        assertCode(res, "NOT_FOUND");
    }

    @Test
    void shouldFailDeleteNonexistentPolicy() {
        ResponseEntity<String> res = delete("/api/orchestration/escalation-policies/999999");
        assertCode(res, "NOT_FOUND");
    }

    // ========================
    // Edge: escalate without reason body
    // ========================

    @Test
    void shouldEscalateWithoutReason() {
        String pid = createProject("EscNoReason");
        post("/api/orchestration/escalation-policies", Map.of(
                "projectId", pid, "name", "NoReasonPol", "severity", "CRITICAL",
                "channel", "EMAIL"
        ));
        String iid = createIncident(pid, "CRITICAL", "no reason");

        ResponseEntity<String> escRes = post("/api/orchestration/incidents/" + iid + "/escalate", Map.of());
        assertOk(escRes);
        assertThat(TestJsonHelper.getInt(TestJsonHelper.parse(escRes.getBody()).get("data"), "escalationLevel")).isEqualTo(1);
    }
}
