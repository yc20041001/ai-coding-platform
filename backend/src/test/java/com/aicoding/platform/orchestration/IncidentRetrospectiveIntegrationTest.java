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

class IncidentRetrospectiveIntegrationTest extends IntegrationTestBase {

    private String createProject(String suffix) {
        ResponseEntity<String> res = post("/api/projects", Map.of(
                "name", "IT-RETRO-" + suffix,
                "description", "Retrospective integration test project",
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
    // Retrospective Draft - Create
    // ==============================

    @Test
    void shouldCreateRetrospectiveDraft() {
        String pid = createProject("DraftCreate");
        String iid = createIncident(pid, "CRITICAL", "Retro test");
        ResponseEntity<String> res = post("/api/orchestration/incidents/" + iid + "/retrospective-draft", Map.of());
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(TestJsonHelper.getString(data, "status")).isEqualTo("DRAFT");
        assertThat(TestJsonHelper.getString(data, "incidentId")).isEqualTo(iid);
        assertThat(TestJsonHelper.getString(data, "regressionRisk")).isEqualTo("LOW");
        assertThat(data.has("whatHappened")).isTrue();
        assertThat(data.has("impactSummary")).isTrue();
    }

    @Test
    void shouldFailCreateDuplicateRetrospectiveDraft() {
        String pid = createProject("DraftDup");
        String iid = createIncident(pid, "HIGH", "dup retro");
        post("/api/orchestration/incidents/" + iid + "/retrospective-draft", Map.of());
        ResponseEntity<String> res = post("/api/orchestration/incidents/" + iid + "/retrospective-draft", Map.of());
        assertCode(res, "CONFLICT");
    }

    @Test
    void shouldFailCreateDraftForNonexistentIncident() {
        ResponseEntity<String> res = post("/api/orchestration/incidents/999999/retrospective-draft", Map.of());
        assertCode(res, "NOT_FOUND");
    }

    // ==============================
    // Retrospective - Update
    // ==============================

    @Test
    void shouldUpdateRetrospectiveContent() {
        String pid = createProject("UpdContent");
        String iid = createIncident(pid, "MEDIUM", "update retro");
        ResponseEntity<String> createRes = post("/api/orchestration/incidents/" + iid + "/retrospective-draft", Map.of());
        String retroId = TestJsonHelper.getString(TestJsonHelper.parse(createRes.getBody()).get("data"), "id");

        ResponseEntity<String> res = put("/api/orchestration/incident-retrospectives/" + retroId, Map.of(
                "summary", "Updated summary",
                "lessonsLearned", "Lesson learned: add monitoring",
                "preventionPlan", "Add more alerts"
        ));
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(TestJsonHelper.getString(data, "summary")).isEqualTo("Updated summary");
        assertThat(TestJsonHelper.getString(data, "lessonsLearned")).contains("monitoring");
    }

    @Test
    void shouldUpdateRetrospectiveStatusToReviewed() {
        String pid = createProject("UpdStatus");
        String iid = createIncident(pid, "HIGH", "status retro");
        ResponseEntity<String> createRes = post("/api/orchestration/incidents/" + iid + "/retrospective-draft", Map.of());
        String retroId = TestJsonHelper.getString(TestJsonHelper.parse(createRes.getBody()).get("data"), "id");

        ResponseEntity<String> res = put("/api/orchestration/incident-retrospectives/" + retroId, Map.of(
                "status", "REVIEWED"
        ));
        assertOk(res);
        assertThat(TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()).get("data"), "status")).isEqualTo("REVIEWED");
    }

    @Test
    void shouldPublishRetrospective() {
        String pid = createProject("PubRetro");
        String iid = createIncident(pid, "CRITICAL", "publish retro");
        ResponseEntity<String> createRes = post("/api/orchestration/incidents/" + iid + "/retrospective-draft", Map.of());
        String retroId = TestJsonHelper.getString(TestJsonHelper.parse(createRes.getBody()).get("data"), "id");

        put("/api/orchestration/incident-retrospectives/" + retroId, Map.of("status", "REVIEWED"));
        ResponseEntity<String> res = put("/api/orchestration/incident-retrospectives/" + retroId, Map.of("status", "PUBLISHED"));
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(TestJsonHelper.getString(data, "status")).isEqualTo("PUBLISHED");
        assertThat(TestJsonHelper.getString(data, "publishedAt")).isNotNull();
    }

    @Test
    void shouldFailInvalidStatusTransition() {
        String pid = createProject("BadTrans");
        String iid = createIncident(pid, "LOW", "bad transition");
        ResponseEntity<String> createRes = post("/api/orchestration/incidents/" + iid + "/retrospective-draft", Map.of());
        String retroId = TestJsonHelper.getString(TestJsonHelper.parse(createRes.getBody()).get("data"), "id");

        ResponseEntity<String> res = put("/api/orchestration/incident-retrospectives/" + retroId, Map.of(
                "status", "PUBLISHED"
        ));
        assertCode(res, "VALIDATION_ERROR");
    }

    @Test
    void shouldFailUpdateArchivedRetrospective() {
        String pid = createProject("ArchUpd");
        String iid = createIncident(pid, "MEDIUM", "archived update");
        ResponseEntity<String> createRes = post("/api/orchestration/incidents/" + iid + "/retrospective-draft", Map.of());
        String retroId = TestJsonHelper.getString(TestJsonHelper.parse(createRes.getBody()).get("data"), "id");

        put("/api/orchestration/incident-retrospectives/" + retroId, Map.of("status", "REVIEWED"));
        put("/api/orchestration/incident-retrospectives/" + retroId, Map.of("status", "PUBLISHED"));
        put("/api/orchestration/incident-retrospectives/" + retroId, Map.of("status", "ARCHIVED"));

        ResponseEntity<String> res = put("/api/orchestration/incident-retrospectives/" + retroId, Map.of(
                "summary", "try update archived"
        ));
        assertCode(res, "VALIDATION_ERROR");
    }

    @Test
    void shouldFailUpdateNonexistentRetrospective() {
        ResponseEntity<String> res = put("/api/orchestration/incident-retrospectives/999999", Map.of("summary", "nonexistent"));
        assertCode(res, "NOT_FOUND");
    }

    @Test
    void shouldUpdateRegressionRiskAndRepeatedFlag() {
        String pid = createProject("RegRisk");
        String iid = createIncident(pid, "HIGH", "regression risk");
        ResponseEntity<String> createRes = post("/api/orchestration/incidents/" + iid + "/retrospective-draft", Map.of());
        String retroId = TestJsonHelper.getString(TestJsonHelper.parse(createRes.getBody()).get("data"), "id");

        ResponseEntity<String> res = put("/api/orchestration/incident-retrospectives/" + retroId, Map.of(
                "regressionRisk", "HIGH",
                "repeatedIncident", true
        ));
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(TestJsonHelper.getString(data, "regressionRisk")).isEqualTo("HIGH");
        assertThat(data.get("repeatedIncident").asBoolean()).isTrue();
    }

    // ==============================
    // Retrospective - Get
    // ==============================

    @Test
    void shouldGetRetrospectiveById() {
        String pid = createProject("GetById");
        String iid = createIncident(pid, "CRITICAL", "get by id");
        ResponseEntity<String> createRes = post("/api/orchestration/incidents/" + iid + "/retrospective-draft", Map.of());
        String retroId = TestJsonHelper.getString(TestJsonHelper.parse(createRes.getBody()).get("data"), "id");

        ResponseEntity<String> res = get("/api/orchestration/incident-retrospectives/" + retroId);
        assertOk(res);
        assertThat(TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()).get("data"), "id")).isEqualTo(retroId);
    }

    @Test
    void shouldGetIncidentRetrospective() {
        String pid = createProject("GetByInc");
        String iid = createIncident(pid, "HIGH", "get by incident");
        post("/api/orchestration/incidents/" + iid + "/retrospective-draft", Map.of());

        ResponseEntity<String> res = get("/api/orchestration/incidents/" + iid + "/retrospective");
        assertOk(res);
        assertThat(TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()).get("data"), "incidentId")).isEqualTo(iid);
    }

    @Test
    void shouldFailGetIncidentRetrospectiveWhenNonexistent() {
        String pid = createProject("GetMiss");
        String iid = createIncident(pid, "LOW", "missing retro");
        ResponseEntity<String> res = get("/api/orchestration/incidents/" + iid + "/retrospective");
        assertCode(res, "NOT_FOUND");
    }

    @Test
    void shouldFailGetNonexistentRetrospective() {
        ResponseEntity<String> res = get("/api/orchestration/incident-retrospectives/999999");
        assertCode(res, "NOT_FOUND");
    }

    // ==============================
    // Retrospective - List
    // ==============================

    @Test
    void shouldListProjectRetrospectives() {
        String pid = createProject("ListRetro");
        String iid1 = createIncident(pid, "HIGH", "list retro 1");
        String iid2 = createIncident(pid, "MEDIUM", "list retro 2");
        post("/api/orchestration/incidents/" + iid1 + "/retrospective-draft", Map.of());
        post("/api/orchestration/incidents/" + iid2 + "/retrospective-draft", Map.of());

        ResponseEntity<String> res = get("/api/projects/" + pid + "/incident-retrospectives");
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(data.has("records")).isTrue();
        assertThat(data.get("records").size()).isGreaterThanOrEqualTo(2);
    }

    @Test
    void shouldListProjectRetrospectivesFilterByStatus() {
        String pid = createProject("ListFilt");
        String iid = createIncident(pid, "HIGH", "filter retro");
        ResponseEntity<String> createRes = post("/api/orchestration/incidents/" + iid + "/retrospective-draft", Map.of());
        String retroId = TestJsonHelper.getString(TestJsonHelper.parse(createRes.getBody()).get("data"), "id");
        put("/api/orchestration/incident-retrospectives/" + retroId, Map.of("status", "REVIEWED"));

        ResponseEntity<String> res = get("/api/projects/" + pid + "/incident-retrospectives?status=REVIEWED");
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(data.get("records").size()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void shouldReturnEmptyListForNoRetrospectives() {
        String pid = createProject("EmptyRet");
        ResponseEntity<String> res = get("/api/projects/" + pid + "/incident-retrospectives");
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(data.get("total").asLong()).isEqualTo(0);
    }

    // ==============================
    // Retrospective - Regression Check
    // ==============================

    @Test
    void shouldCheckRegressionWithNoSimilarIncidents() {
        String pid = createProject("RegNoSim");
        String iid = createIncident(pid, "LOW", "reg no similar");
        ResponseEntity<String> res = get("/api/orchestration/incidents/" + iid + "/regression-check");
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(data.get("repeatedIncident").asBoolean()).isFalse();
        assertThat(TestJsonHelper.getString(data, "regressionRisk")).isEqualTo("LOW");
        assertThat(data.get("similarCount").asInt()).isEqualTo(0);
    }

    @Test
    void shouldFailRegressionCheckForNonexistentIncident() {
        ResponseEntity<String> res = get("/api/orchestration/incidents/999999/regression-check");
        assertCode(res, "NOT_FOUND");
    }

    // ==============================
    // Retrospective - Archive
    // ==============================

    @Test
    void shouldArchiveRetrospective() {
        String pid = createProject("ArchRetro");
        String iid = createIncident(pid, "MEDIUM", "archive retro");
        ResponseEntity<String> createRes = post("/api/orchestration/incidents/" + iid + "/retrospective-draft", Map.of());
        String retroId = TestJsonHelper.getString(TestJsonHelper.parse(createRes.getBody()).get("data"), "id");

        put("/api/orchestration/incident-retrospectives/" + retroId, Map.of("status", "REVIEWED"));
        put("/api/orchestration/incident-retrospectives/" + retroId, Map.of("status", "PUBLISHED"));
        ResponseEntity<String> res = put("/api/orchestration/incident-retrospectives/" + retroId, Map.of("status", "ARCHIVED"));
        assertOk(res);
        assertThat(TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()).get("data"), "status")).isEqualTo("ARCHIVED");
    }

    @Test
    void shouldDraftFromReviewed() {
        String pid = createProject("DraftRev");
        String iid = createIncident(pid, "HIGH", "draft from reviewed");
        ResponseEntity<String> createRes = post("/api/orchestration/incidents/" + iid + "/retrospective-draft", Map.of());
        String retroId = TestJsonHelper.getString(TestJsonHelper.parse(createRes.getBody()).get("data"), "id");

        put("/api/orchestration/incident-retrospectives/" + retroId, Map.of("status", "REVIEWED"));
        ResponseEntity<String> res = put("/api/orchestration/incident-retrospectives/" + retroId, Map.of("status", "DRAFT"));
        assertOk(res);
        assertThat(TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()).get("data"), "status")).isEqualTo("DRAFT");
    }

    @Test
    void shouldCreateDraftWithRcaData() {
        String pid = createProject("DraftRca");
        String iid = createIncident(pid, "CRITICAL", "rca retro draft");

        post("/api/orchestration/incidents/" + iid + "/root-cause-note", Map.of(
                "rootCause", "Network partition",
                "impact", "Service disruption 10min",
                "prevention", "Implement retry logic",
                "followUpActions", "Review HA setup"
        ));

        ResponseEntity<String> res = post("/api/orchestration/incidents/" + iid + "/retrospective-draft", Map.of());
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(TestJsonHelper.getString(data, "whatHappened")).contains("rca retro draft");
        assertThat(TestJsonHelper.getString(data, "lessonsLearned")).contains("Network partition");
        assertThat(TestJsonHelper.getString(data, "preventionPlan")).contains("retry");
        assertThat(TestJsonHelper.getString(data, "actionItems")).contains("Review HA");
        assertThat(TestJsonHelper.getString(data, "incidentId")).isEqualTo(iid);
    }
}
