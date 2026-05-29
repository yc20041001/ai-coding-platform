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

class IncidentKnowledgeIntegrationTest extends IntegrationTestBase {

    private String createProject(String suffix) {
        ResponseEntity<String> res = post("/api/projects", Map.of(
                "name", "IT-KNOW-" + suffix,
                "description", "Knowledge integration test project",
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

    private String createKnowledgeBase(String projectId, String name) {
        ResponseEntity<String> res = post("/api/projects/" + projectId + "/knowledge-bases", Map.of(
                "name", name
        ));
        assertOk(res);
        return Objects.requireNonNull(TestJsonHelper.getString(
                TestJsonHelper.parse(res.getBody()), "data.id"));
    }

    // ========================
    // Root Cause Note - Create
    // ========================

    @Test
    void shouldCreateRootCauseNote() {
        String pid = createProject("RcaCreate");
        String iid = createIncident(pid, "CRITICAL", "RCA test");
        ResponseEntity<String> res = post("/api/orchestration/incidents/" + iid + "/root-cause-note", Map.of(
                "rootCause", "Database connection pool exhaustion",
                "impact", "Service unavailable for 5 minutes",
                "resolution", "Increased max pool size from 10 to 50",
                "prevention", "Add connection pool monitoring alert",
                "followUpActions", "Review other services' pool settings",
                "tags", "database,connection-pool",
                "confidence", "HIGH"
        ));
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(TestJsonHelper.getString(data, "status")).isEqualTo("DRAFT");
        assertThat(TestJsonHelper.getString(data, "confidence")).isEqualTo("HIGH");
        assertThat(TestJsonHelper.getString(data, "rootCause")).contains("connection pool");
        assertThat(TestJsonHelper.getString(data, "incidentId")).isEqualTo(iid);
    }

    @Test
    void shouldCreateRootCauseNoteWithDefaultConfidence() {
        String pid = createProject("RcaDefault");
        String iid = createIncident(pid, "HIGH", "default confidence");
        ResponseEntity<String> res = post("/api/orchestration/incidents/" + iid + "/root-cause-note", Map.of(
                "rootCause", "Test root cause"
        ));
        assertOk(res);
        assertThat(TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()).get("data"), "confidence"))
                .isEqualTo("MEDIUM");
    }

    @Test
    void shouldFailCreateDuplicateRootCauseNote() {
        String pid = createProject("RcaDup");
        String iid = createIncident(pid, "CRITICAL", "duplicate rca");
        post("/api/orchestration/incidents/" + iid + "/root-cause-note", Map.of("rootCause", "first"));
        ResponseEntity<String> res = post("/api/orchestration/incidents/" + iid + "/root-cause-note", Map.of("rootCause", "second"));
        assertCode(res, "CONFLICT");
    }

    @Test
    void shouldFailCreateNoteForNonexistentIncident() {
        ResponseEntity<String> res = post("/api/orchestration/incidents/999999/root-cause-note",
                Map.of("rootCause", "test"));
        assertCode(res, "NOT_FOUND");
    }

    // ========================
    // Root Cause Note - Get
    // ========================

    @Test
    void shouldGetIncidentRootCauseNote() {
        String pid = createProject("RcaGet");
        String iid = createIncident(pid, "HIGH", "get rca");
        post("/api/orchestration/incidents/" + iid + "/root-cause-note", Map.of(
                "rootCause", "Memory leak",
                "impact", "OOM kills"
        ));
        ResponseEntity<String> res = get("/api/orchestration/incidents/" + iid + "/root-cause-note");
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(TestJsonHelper.getString(data, "rootCause")).contains("Memory leak");
        assertThat(TestJsonHelper.getString(data, "incidentId")).isEqualTo(iid);
    }

    @Test
    void shouldFailGetNoteNonexistent() {
        String pid = createProject("RcaGetNone");
        String iid = createIncident(pid, "LOW", "no rca");
        ResponseEntity<String> res = get("/api/orchestration/incidents/" + iid + "/root-cause-note");
        assertCode(res, "NOT_FOUND");
    }

    // ========================
    // Root Cause Note - Update
    // ========================

    @Test
    void shouldUpdateRootCauseNote() {
        String pid = createProject("RcaUpdate");
        String iid = createIncident(pid, "CRITICAL", "update rca");
        ResponseEntity<String> createRes = post("/api/orchestration/incidents/" + iid + "/root-cause-note",
                Map.of("rootCause", "old cause"));
        String noteId = TestJsonHelper.getString(TestJsonHelper.parse(createRes.getBody()), "data.id");

        ResponseEntity<String> res = put("/api/orchestration/incident-root-cause-notes/" + noteId, Map.of(
                "rootCause", "updated cause",
                "impact", "updated impact"
        ));
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(TestJsonHelper.getString(data, "rootCause")).isEqualTo("updated cause");
        assertThat(TestJsonHelper.getString(data, "impact")).isEqualTo("updated impact");
    }

    @Test
    void shouldUpdateNoteStatusToReviewed() {
        String pid = createProject("RcaStatusR");
        String iid = createIncident(pid, "HIGH", "status reviewed");
        ResponseEntity<String> createRes = post("/api/orchestration/incidents/" + iid + "/root-cause-note",
                Map.of("rootCause", "test"));
        String noteId = TestJsonHelper.getString(TestJsonHelper.parse(createRes.getBody()), "data.id");

        ResponseEntity<String> res = put("/api/orchestration/incident-root-cause-notes/" + noteId,
                Map.of("status", "REVIEWED"));
        assertOk(res);
        assertThat(TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()).get("data"), "status"))
                .isEqualTo("REVIEWED");
    }

    @Test
    void shouldUpdateNoteStatusToPublished() {
        String pid = createProject("RcaPub");
        String iid = createIncident(pid, "HIGH", "publish rca");
        ResponseEntity<String> createRes = post("/api/orchestration/incidents/" + iid + "/root-cause-note",
                Map.of("rootCause", "test"));
        String noteId = TestJsonHelper.getString(TestJsonHelper.parse(createRes.getBody()), "data.id");

        put("/api/orchestration/incident-root-cause-notes/" + noteId, Map.of("status", "REVIEWED"));
        ResponseEntity<String> res = put("/api/orchestration/incident-root-cause-notes/" + noteId,
                Map.of("status", "PUBLISHED"));
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(TestJsonHelper.getString(data, "status")).isEqualTo("PUBLISHED");
        assertThat(TestJsonHelper.getString(data, "publishedAt")).isNotEmpty();
    }

    @Test
    void shouldRejectNoteArchivedUpdate() {
        String pid = createProject("RcaArchUpd");
        String iid = createIncident(pid, "HIGH", "archived update");
        ResponseEntity<String> createRes = post("/api/orchestration/incidents/" + iid + "/root-cause-note",
                Map.of("rootCause", "test"));
        String noteId = TestJsonHelper.getString(TestJsonHelper.parse(createRes.getBody()), "data.id");

        put("/api/orchestration/incident-root-cause-notes/" + noteId, Map.of("status", "REVIEWED"));
        put("/api/orchestration/incident-root-cause-notes/" + noteId, Map.of("status", "PUBLISHED"));
        put("/api/orchestration/incident-root-cause-notes/" + noteId, Map.of("status", "ARCHIVED"));

        ResponseEntity<String> res = put("/api/orchestration/incident-root-cause-notes/" + noteId,
                Map.of("rootCause", "should fail"));
        assertCode(res, "VALIDATION_ERROR");
    }

    @Test
    void shouldRejectInvalidStatusTransition() {
        String pid = createProject("RcaInvTrans");
        String iid = createIncident(pid, "HIGH", "invalid trans");
        ResponseEntity<String> createRes = post("/api/orchestration/incidents/" + iid + "/root-cause-note",
                Map.of("rootCause", "test"));
        String noteId = TestJsonHelper.getString(TestJsonHelper.parse(createRes.getBody()), "data.id");

        // DRAFT -> PUBLISHED is invalid
        ResponseEntity<String> res = put("/api/orchestration/incident-root-cause-notes/" + noteId,
                Map.of("status", "PUBLISHED"));
        assertCode(res, "VALIDATION_ERROR");
    }

    @Test
    void shouldAllowReviewedToDraft() {
        String pid = createProject("RcaRev2Draft");
        String iid = createIncident(pid, "MEDIUM", "review to draft");
        ResponseEntity<String> createRes = post("/api/orchestration/incidents/" + iid + "/root-cause-note",
                Map.of("rootCause", "test"));
        String noteId = TestJsonHelper.getString(TestJsonHelper.parse(createRes.getBody()), "data.id");

        put("/api/orchestration/incident-root-cause-notes/" + noteId, Map.of("status", "REVIEWED"));
        ResponseEntity<String> res = put("/api/orchestration/incident-root-cause-notes/" + noteId,
                Map.of("status", "DRAFT"));
        assertOk(res);
        assertThat(TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()).get("data"), "status"))
                .isEqualTo("DRAFT");
    }

    @Test
    void shouldFailUpdateNonexistentNote() {
        ResponseEntity<String> res = put("/api/orchestration/incident-root-cause-notes/999999",
                Map.of("rootCause", "nope"));
        assertCode(res, "NOT_FOUND");
    }

    // ========================
    // Root Cause Note - List
    // ========================

    @Test
    void shouldListProjectRootCauseNotes() {
        String pid = createProject("RcaList");
        String iid1 = createIncident(pid, "HIGH", "list rca 1");
        String iid2 = createIncident(pid, "LOW", "list rca 2");
        post("/api/orchestration/incidents/" + iid1 + "/root-cause-note", Map.of("rootCause", "rca1"));
        post("/api/orchestration/incidents/" + iid2 + "/root-cause-note", Map.of("rootCause", "rca2"));

        ResponseEntity<String> res = get("/api/projects/" + pid + "/incident-root-cause-notes?page=1&size=20");
        assertOk(res);
        JsonNode records = TestJsonHelper.parse(res.getBody()).get("data").get("records");
        assertThat(records.isArray()).isTrue();
        assertThat(records.size()).isGreaterThanOrEqualTo(2);
    }

    @Test
    void shouldListNotesFilteredByStatus() {
        String pid = createProject("RcaListSt");
        String iid = createIncident(pid, "HIGH", "status filter");
        ResponseEntity<String> createRes = post("/api/orchestration/incidents/" + iid + "/root-cause-note",
                Map.of("rootCause", "test"));
        String noteId = TestJsonHelper.getString(TestJsonHelper.parse(createRes.getBody()), "data.id");
        put("/api/orchestration/incident-root-cause-notes/" + noteId, Map.of("status", "REVIEWED"));

        ResponseEntity<String> res = get("/api/projects/" + pid + "/incident-root-cause-notes?status=REVIEWED");
        assertOk(res);
        JsonNode records = TestJsonHelper.parse(res.getBody()).get("data").get("records");
        assertThat(records.isArray()).isTrue();
        assertThat(records.size()).isGreaterThanOrEqualTo(1);
        assertThat(TestJsonHelper.getString(records.get(0), "status")).isEqualTo("REVIEWED");
    }

    @Test
    void shouldListNotesWithEmptyResult() {
        String pid = createProject("RcaListEmpty");
        ResponseEntity<String> res = get("/api/projects/" + pid + "/incident-root-cause-notes");
        assertOk(res);
        JsonNode records = TestJsonHelper.parse(res.getBody()).get("data").get("records");
        assertThat(records.isArray()).isTrue();
        assertThat(records.size()).isEqualTo(0);
    }

    // ========================
    // Root Cause Note - Markdown Export
    // ========================

    @Test
    void shouldExportNoteMarkdown() {
        String pid = createProject("RcaMd");
        String iid = createIncident(pid, "CRITICAL", "markdown export");
        ResponseEntity<String> createRes = post("/api/orchestration/incidents/" + iid + "/root-cause-note", Map.of(
                "rootCause", "Root cause text",
                "impact", "Impact text",
                "resolution", "Resolution text",
                "prevention", "Prevention text"
        ));
        String noteId = TestJsonHelper.getString(TestJsonHelper.parse(createRes.getBody()), "data.id");

        ResponseEntity<String> res = get("/api/orchestration/incident-root-cause-notes/" + noteId + "/markdown");
        assertOk(res);
        String markdown = TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data");
        assertThat(markdown).contains("Root Cause Analysis");
        assertThat(markdown).contains("Root cause text");
        assertThat(markdown).contains("Impact text");
        assertThat(markdown).contains("Resolution text");
        assertThat(markdown).contains("CRITICAL");
    }

    @Test
    void shouldFailExportNonexistentNote() {
        ResponseEntity<String> res = get("/api/orchestration/incident-root-cause-notes/999999/markdown");
        assertCode(res, "NOT_FOUND");
    }

    // ========================
    // Known Issue Templates
    // ========================

    @Test
    void shouldCreateKnownIssueTemplate() {
        String pid = createProject("KitCreate");
        ResponseEntity<String> res = post("/api/projects/" + pid + "/known-issue-templates", Map.of(
                "title", "DB Connection Pool Exhaustion",
                "category", "TOOL_POLICY",
                "severity", "CRITICAL",
                "rootCauseTemplate", "Connection pool exhausted due to slow queries",
                "impactTemplate", "Service unavailable",
                "resolutionTemplate", "Increase pool size and optimize queries",
                "preventionTemplate", "Add pool monitoring alert",
                "tags", "database,connection-pool"
        ));
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(TestJsonHelper.getString(data, "title")).contains("DB Connection");
        assertThat(TestJsonHelper.getString(data, "category")).isEqualTo("TOOL_POLICY");
        assertThat(TestJsonHelper.getBool(data, "enabled")).isTrue();
    }

    @Test
    void shouldFailCreateTemplateWithoutTitle() {
        String pid = createProject("KitNoTitle");
        ResponseEntity<String> res = post("/api/projects/" + pid + "/known-issue-templates", Map.of(
                "category", "TOOL_POLICY"
        ));
        assertCode(res, "VALIDATION_ERROR");
    }

    @Test
    void shouldUpdateKnownIssueTemplate() {
        String pid = createProject("KitUpdate");
        ResponseEntity<String> createRes = post("/api/projects/" + pid + "/known-issue-templates", Map.of(
                "title", "Original",
                "category", "RABBITMQ",
                "severity", "HIGH"
        ));
        String tid = TestJsonHelper.getString(TestJsonHelper.parse(createRes.getBody()), "data.id");

        ResponseEntity<String> res = put("/api/orchestration/known-issue-templates/" + tid, Map.of(
                "title", "Updated Title",
                "enabled", false
        ));
        assertOk(res);
        assertThat(TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()).get("data"), "title"))
                .isEqualTo("Updated Title");
        assertThat(TestJsonHelper.getBool(TestJsonHelper.parse(res.getBody()).get("data"), "enabled")).isFalse();
    }

    @Test
    void shouldGetKnownIssueTemplate() {
        String pid = createProject("KitGet");
        post("/api/projects/" + pid + "/known-issue-templates", Map.of(
                "title", "GetTest", "category", "REDIS"
        ));

        ResponseEntity<String> res = get("/api/projects/" + pid + "/known-issue-templates");
        assertOk(res);
        JsonNode records = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(records.isArray()).isTrue();
    }

    @Test
    void shouldListTemplatesByCategory() {
        String pid = createProject("KitCat");
        post("/api/projects/" + pid + "/known-issue-templates", Map.of("title", "T1", "category", "GITHUB"));
        post("/api/projects/" + pid + "/known-issue-templates", Map.of("title", "T2", "category", "REDIS"));

        ResponseEntity<String> res = get("/api/projects/" + pid + "/known-issue-templates?category=GITHUB");
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(data.isArray()).isTrue();
        assertThat(data.size()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void shouldListTemplatesFilterByEnabled() {
        String pid = createProject("KitEnabled");
        post("/api/projects/" + pid + "/known-issue-templates", Map.of("title", "Enabled1", "category", "TOOL_POLICY"));
        ResponseEntity<String> createRes = post("/api/projects/" + pid + "/known-issue-templates",
                Map.of("title", "Disabled1", "category", "TOOL_POLICY"));
        String tid = TestJsonHelper.getString(TestJsonHelper.parse(createRes.getBody()), "data.id");
        put("/api/orchestration/known-issue-templates/" + tid, Map.of("enabled", false));

        ResponseEntity<String> res = get("/api/projects/" + pid + "/known-issue-templates?enabled=true");
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        for (int i = 0; i < data.size(); i++) {
            assertThat(TestJsonHelper.getBool(data.get(i), "enabled")).isTrue();
        }
    }

    @Test
    void shouldFailUpdateNonexistentTemplate() {
        ResponseEntity<String> res = put("/api/orchestration/known-issue-templates/999999",
                Map.of("title", "Nope"));
        assertCode(res, "NOT_FOUND");
    }

    // ========================
    // Apply Template to Incident
    // ========================

    @Test
    void shouldApplyKnownIssueTemplate() {
        String pid = createProject("KitApply");
        String iid = createIncident(pid, "CRITICAL", "apply template");
        ResponseEntity<String> tmplRes = post("/api/projects/" + pid + "/known-issue-templates", Map.of(
                "title", "Template for Apply",
                "rootCauseTemplate", "Root cause from template",
                "impactTemplate", "Impact from template",
                "resolutionTemplate", "Resolution from template",
                "preventionTemplate", "Prevention from template"
        ));
        String tid = TestJsonHelper.getString(TestJsonHelper.parse(tmplRes.getBody()), "data.id");

        ResponseEntity<String> res = post("/api/orchestration/incidents/" + iid + "/apply-known-issue-template/" + tid,
                Map.of());
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(TestJsonHelper.getString(data, "rootCause")).isEqualTo("Root cause from template");
        assertThat(TestJsonHelper.getString(data, "impact")).isEqualTo("Impact from template");
        assertThat(TestJsonHelper.getString(data, "resolution")).isEqualTo("Resolution from template");
        assertThat(TestJsonHelper.getString(data, "prevention")).isEqualTo("Prevention from template");
        assertThat(TestJsonHelper.getString(data, "status")).isEqualTo("DRAFT");
    }

    @Test
    void shouldFailApplyTemplateWhenNoteExists() {
        String pid = createProject("KitApplyFail");
        String iid = createIncident(pid, "HIGH", "apply conflict");
        post("/api/orchestration/incidents/" + iid + "/root-cause-note", Map.of("rootCause", "existing note"));
        ResponseEntity<String> tmplRes = post("/api/projects/" + pid + "/known-issue-templates",
                Map.of("title", "Template", "rootCauseTemplate", "tmpl"));
        String tid = TestJsonHelper.getString(TestJsonHelper.parse(tmplRes.getBody()), "data.id");

        ResponseEntity<String> res = post("/api/orchestration/incidents/" + iid + "/apply-known-issue-template/" + tid,
                Map.of());
        assertCode(res, "CONFLICT");
    }

    @Test
    void shouldFailApplyNonexistentTemplate() {
        String pid = createProject("KitApplyBad");
        String iid = createIncident(pid, "MEDIUM", "bad template");
        ResponseEntity<String> res = post("/api/orchestration/incidents/" + iid + "/apply-known-issue-template/999999",
                Map.of());
        assertCode(res, "NOT_FOUND");
    }

    // ========================
    // Knowledge Document Generation
    // ========================

    @Test
    void shouldGenerateKnowledgeDocument() {
        String pid = createProject("KbGen");
        String iid = createIncident(pid, "CRITICAL", "generate kb doc");
        String kbId = createKnowledgeBase(pid, "Test KB for Generation");

        post("/api/orchestration/incidents/" + iid + "/root-cause-note", Map.of(
                "rootCause", "Test root cause",
                "impact", "Test impact"
        ));

        ResponseEntity<String> res = post("/api/orchestration/incidents/" + iid + "/knowledge-document", Map.of(
                "knowledgeBaseId", kbId,
                "title", "Generated Knowledge Doc",
                "includeTimeline", false,
                "includeTraceSummary", false,
                "includeOperatorReview", false,
                "includeEscalation", false
        ));
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(TestJsonHelper.getString(data, "title")).contains("Generated Knowledge Doc");
        assertThat(TestJsonHelper.getString(data, "knowledgeBaseId")).isEqualTo(kbId);
        assertThat(TestJsonHelper.getString(data, "documentId")).isNotEmpty();
    }

    @Test
    void shouldFailGenerateWithoutKnowledgeBase() {
        String pid = createProject("KbGenNoKb");
        String iid = createIncident(pid, "HIGH", "no kb");
        ResponseEntity<String> res = post("/api/orchestration/incidents/" + iid + "/knowledge-document", Map.of(
                "title", "No KB Doc"
        ));
        assertCode(res, "VALIDATION_ERROR");
    }

    // ========================
    // Knowledge Links
    // ========================

    @Test
    void shouldListKnowledgeLinks() {
        String pid = createProject("KbLinkList");
        String iid = createIncident(pid, "CRITICAL", "link list");
        String kbId = createKnowledgeBase(pid, "Link List KB");

        post("/api/orchestration/incidents/" + iid + "/root-cause-note", Map.of("rootCause", "test"));
        post("/api/orchestration/incidents/" + iid + "/knowledge-document", Map.of(
                "knowledgeBaseId", kbId,
                "title", "Link Doc",
                "includeTimeline", false,
                "includeTraceSummary", false,
                "includeOperatorReview", false,
                "includeEscalation", false
        ));

        ResponseEntity<String> res = get("/api/orchestration/incidents/" + iid + "/knowledge-links");
        assertOk(res);
        JsonNode links = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(links.isArray()).isTrue();
        assertThat(links.size()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void shouldListEmptyKnowledgeLinks() {
        String pid = createProject("KbLinkEmpty");
        String iid = createIncident(pid, "LOW", "no links");
        ResponseEntity<String> res = get("/api/orchestration/incidents/" + iid + "/knowledge-links");
        assertOk(res);
        JsonNode links = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(links.isArray()).isTrue();
        assertThat(links.size()).isEqualTo(0);
    }

    @Test
    void shouldDeleteKnowledgeLink() {
        String pid = createProject("KbLinkDel");
        String iid = createIncident(pid, "HIGH", "delete link");
        String kbId = createKnowledgeBase(pid, "Delete KB");

        post("/api/orchestration/incidents/" + iid + "/root-cause-note", Map.of("rootCause", "test"));
        post("/api/orchestration/incidents/" + iid + "/knowledge-document", Map.of(
                "knowledgeBaseId", kbId,
                "title", "Delete Doc",
                "includeTimeline", false,
                "includeTraceSummary", false,
                "includeOperatorReview", false,
                "includeEscalation", false
        ));

        ResponseEntity<String> listRes = get("/api/orchestration/incidents/" + iid + "/knowledge-links");
        String linkId = TestJsonHelper.getString(
                TestJsonHelper.parse(listRes.getBody()).get("data").get(0), "id");

        ResponseEntity<String> delRes = delete("/api/orchestration/incident-knowledge-links/" + linkId);
        assertOk(delRes);

        ResponseEntity<String> afterDel = get("/api/orchestration/incidents/" + iid + "/knowledge-links");
        assertOk(afterDel);
        assertThat(TestJsonHelper.parse(afterDel.getBody()).get("data").size()).isEqualTo(0);
    }

    @Test
    void shouldFailDeleteNonexistentLink() {
        ResponseEntity<String> res = delete("/api/orchestration/incident-knowledge-links/999999");
        assertCode(res, "NOT_FOUND");
    }

    // ========================
    // Similar Incident Search
    // ========================

    @Test
    void shouldSearchSimilarByQuery() {
        String pid = createProject("SimSearch");
        createIncident(pid, "CRITICAL", "Database connection timeout error");
        createIncident(pid, "HIGH", "Database slow query detected");
        String iid = createIncident(pid, "MEDIUM", "API timeout error");

        ResponseEntity<String> res = get("/api/orchestration/incidents/" + iid + "/similar?query=database&limit=10");
        assertOk(res);
        JsonNode results = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(results.isArray()).isTrue();
        assertThat(results.size()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void shouldReturnEmptySimilarForNoMatch() {
        String pid = createProject("SimNoMatch");
        createIncident(pid, "INFO", "Something unrelated");
        String iid = createIncident(pid, "LOW", "Unique incident XYZ123");

        ResponseEntity<String> res = get("/api/orchestration/incidents/" + iid + "/similar?query=nonexistent_keyword_xyz&limit=10");
        assertOk(res);
        JsonNode results = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(results.isArray()).isTrue();
        assertThat(results.size()).isEqualTo(0);
    }

    @Test
    void shouldSearchSimilarByIncident() {
        String pid = createProject("SimByInc");
        createIncident(pid, "CRITICAL", "Similar incident 1");
        createIncident(pid, "CRITICAL", "Similar incident 2");
        String iid = createIncident(pid, "CRITICAL", "Main incident");

        ResponseEntity<String> res = get("/api/orchestration/incidents/" + iid + "/similar?limit=10");
        assertOk(res);
        JsonNode results = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(results.isArray()).isTrue();
        assertThat(results.size()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void shouldSortSimilarByScoreDescending() {
        String pid = createProject("SimSort");
        createIncident(pid, "CRITICAL", "Database connection lost");
        String iid = createIncident(pid, "HIGH", "Network timeout");

        ResponseEntity<String> res = get("/api/orchestration/incidents/" + iid + "/similar?query=database&limit=10");
        assertOk(res);
        JsonNode results = TestJsonHelper.parse(res.getBody()).get("data");
        if (results.size() >= 2) {
            double first = results.get(0).get("score").asDouble();
            double second = results.get(1).get("score").asDouble();
            assertThat(first).isGreaterThanOrEqualTo(second);
        }
    }

    @Test
    void shouldLimitSimilarResults() {
        String pid = createProject("SimLimit");
        createIncident(pid, "CRITICAL", "Error one");
        createIncident(pid, "HIGH", "Error two");
        createIncident(pid, "MEDIUM", "Error three");
        String iid = createIncident(pid, "LOW", "Main error");

        ResponseEntity<String> res = get("/api/orchestration/incidents/" + iid + "/similar?query=error&limit=2");
        assertOk(res);
        JsonNode results = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(results.size()).isLessThanOrEqualTo(2);
    }

    @Test
    void shouldFailSimilarForNonexistentIncident() {
        ResponseEntity<String> res = get("/api/orchestration/incidents/999999/similar?query=test&limit=10");
        assertCode(res, "NOT_FOUND");
    }

    // ========================
    // Edge Cases & Validation
    // ========================

    @Test
    void shouldCreateNoteAfterArchivingPrevious() {
        String pid = createProject("RcaArchiveNew");
        String iid = createIncident(pid, "CRITICAL", "archive and recreate");

        ResponseEntity<String> createRes = post("/api/orchestration/incidents/" + iid + "/root-cause-note",
                Map.of("rootCause", "first"));
        String noteId = TestJsonHelper.getString(TestJsonHelper.parse(createRes.getBody()), "data.id");

        put("/api/orchestration/incident-root-cause-notes/" + noteId, Map.of("status", "REVIEWED"));
        put("/api/orchestration/incident-root-cause-notes/" + noteId, Map.of("status", "PUBLISHED"));
        put("/api/orchestration/incident-root-cause-notes/" + noteId, Map.of("status", "ARCHIVED"));

        ResponseEntity<String> secondRes = post("/api/orchestration/incidents/" + iid + "/root-cause-note",
                Map.of("rootCause", "second"));
        assertOk(secondRes);
    }

    @Test
    void shouldUpdateNoteConfidence() {
        String pid = createProject("RcaConf");
        String iid = createIncident(pid, "HIGH", "confidence update");
        ResponseEntity<String> createRes = post("/api/orchestration/incidents/" + iid + "/root-cause-note",
                Map.of("rootCause", "test", "confidence", "LOW"));
        String noteId = TestJsonHelper.getString(TestJsonHelper.parse(createRes.getBody()), "data.id");

        ResponseEntity<String> res = put("/api/orchestration/incident-root-cause-notes/" + noteId,
                Map.of("confidence", "CONFIRMED"));
        assertOk(res);
        assertThat(TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()).get("data"), "confidence"))
                .isEqualTo("CONFIRMED");
    }

    @Test
    void shouldCreateTemplateWithAllFields() {
        String pid = createProject("KitAllFields");
        ResponseEntity<String> res = post("/api/projects/" + pid + "/known-issue-templates", Map.of(
                "title", "Redis Outage",
                "category", "REDIS",
                "severity", "CRITICAL",
                "rootCauseTemplate", "Redis node failure",
                "impactTemplate", "Cache miss storm",
                "resolutionTemplate", "Failover to replica",
                "preventionTemplate", "Deploy Redis Sentinel",
                "tags", "redis,cache,infrastructure"
        ));
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(TestJsonHelper.getString(data, "tags")).contains("redis");
    }

    @Test
    void shouldListTemplatesBySeverity() {
        String pid = createProject("KitSev");
        post("/api/projects/" + pid + "/known-issue-templates", Map.of(
                "title", "Critical Template", "severity", "CRITICAL"
        ));
        post("/api/projects/" + pid + "/known-issue-templates", Map.of(
                "title", "Low Template", "severity", "LOW"
        ));

        ResponseEntity<String> res = get("/api/projects/" + pid + "/known-issue-templates");
        assertOk(res);
        assertThat(TestJsonHelper.parse(res.getBody()).get("data").isArray()).isTrue();
    }

    @Test
    void shouldExportNoteMarkdownWithMissingFields() {
        String pid = createProject("RcaMdMin");
        String iid = createIncident(pid, "INFO", "minimal md");
        ResponseEntity<String> createRes = post("/api/orchestration/incidents/" + iid + "/root-cause-note",
                Map.of("rootCause", "Only cause"));
        String noteId = TestJsonHelper.getString(TestJsonHelper.parse(createRes.getBody()), "data.id");

        ResponseEntity<String> res = get("/api/orchestration/incident-root-cause-notes/" + noteId + "/markdown");
        assertOk(res);
        String md = TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data");
        assertThat(md).contains("Only cause");
        assertThat(md).contains("未填写。");
    }

    @Test
    void shouldGenerateKnowledgeDocumentWithoutNote() {
        String pid = createProject("KbGenNoNote");
        String iid = createIncident(pid, "HIGH", "no note doc");
        String kbId = createKnowledgeBase(pid, "No Note KB");

        ResponseEntity<String> res = post("/api/orchestration/incidents/" + iid + "/knowledge-document", Map.of(
                "knowledgeBaseId", kbId,
                "title", "Doc Without Note",
                "includeTimeline", false,
                "includeTraceSummary", false,
                "includeOperatorReview", false,
                "includeEscalation", false
        ));
        assertOk(res);
        assertThat(TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()).get("data"), "documentId"))
                .isNotEmpty();
    }

    @Test
    void shouldHaveLinkAfterKnowledgeGeneration() {
        String pid = createProject("KbLinkGen");
        String iid = createIncident(pid, "CRITICAL", "link after gen");
        String kbId = createKnowledgeBase(pid, "Link Gen KB");

        post("/api/orchestration/incidents/" + iid + "/root-cause-note", Map.of("rootCause", "test"));
        post("/api/orchestration/incidents/" + iid + "/knowledge-document", Map.of(
                "knowledgeBaseId", kbId,
                "title", "Linked Doc",
                "includeTimeline", false,
                "includeTraceSummary", false,
                "includeOperatorReview", false,
                "includeEscalation", false
        ));

        ResponseEntity<String> linkRes = get("/api/orchestration/incidents/" + iid + "/knowledge-links");
        assertOk(linkRes);
        JsonNode links = TestJsonHelper.parse(linkRes.getBody()).get("data");
        boolean hasGenerated = false;
        for (int i = 0; i < links.size(); i++) {
            if ("GENERATED_FROM_INCIDENT".equals(TestJsonHelper.getString(links.get(i), "linkType"))) {
                hasGenerated = true;
                break;
            }
        }
        assertThat(hasGenerated).isTrue();
    }
}
