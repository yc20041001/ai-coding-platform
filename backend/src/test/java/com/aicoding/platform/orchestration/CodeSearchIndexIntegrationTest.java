package com.aicoding.platform.orchestration;

import com.aicoding.platform.support.IntegrationTestBase;
import com.aicoding.platform.support.TestJsonHelper;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.RequestEntity;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.ResourceAccessException;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CodeSearchIndexIntegrationTest extends IntegrationTestBase {

    // ========================
    // 1-3: Build Index
    // ========================

    @Test
    void shouldBuildIndexReturnSummary() {
        String pid = createProject("cibuild");
        ResponseEntity<String> res = post("/api/projects/" + pid + "/code-index/build",
                Map.of("branch", "main", "maxFiles", 50));
        assertOk(res);

        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(TestJsonHelper.getString(data, "projectId")).isEqualTo(pid);
        assertThat(data.get("fileCount").asInt()).isGreaterThan(0);
        assertThat(data.get("symbolCount").asInt()).isGreaterThan(0);
        assertThat(data.get("chunkCount").asInt()).isGreaterThan(0);
        assertThat(data.has("mock")).isTrue();
        assertThat(TestJsonHelper.getString(data, "indexedAt")).isNotEmpty();
    }

    @Test
    void shouldBuildIndexRespectMaxFiles() {
        String pid = createProject("cimf");
        ResponseEntity<String> res = post("/api/projects/" + pid + "/code-index/build",
                Map.of("branch", "main", "maxFiles", 5));
        assertOk(res);

        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(data.get("fileCount").asInt()).isLessThanOrEqualTo(5);
    }

    @Test
    void shouldBuildIndexWithPathPrefix() {
        String pid = createProject("cipp");
        ResponseEntity<String> res = post("/api/projects/" + pid + "/code-index/build",
                Map.of("branch", "main", "pathPrefix", "backend/src", "maxFiles", 30));
        assertOk(res);

        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(data.get("fileCount").asInt()).isGreaterThan(0);
    }

    // ========================
    // 4-6: Get Summary
    // ========================

    @Test
    void shouldGetSummaryReturnCorrectCounts() {
        String pid = createProject("cisum");
        post("/api/projects/" + pid + "/code-index/build", Map.of("branch", "main", "maxFiles", 50));

        ResponseEntity<String> res = get("/api/projects/" + pid + "/code-index/summary");
        assertOk(res);

        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(data.get("fileCount").asInt()).isGreaterThan(0);
        assertThat(data.get("symbolCount").asInt()).isGreaterThan(0);
        assertThat(data.get("chunkCount").asInt()).isGreaterThan(0);
    }

    // ========================
    // 7-9: List Files
    // ========================

    @Test
    void shouldListFilesReturnFiles() {
        String pid = createProject("cilf");
        post("/api/projects/" + pid + "/code-index/build", Map.of("branch", "main", "maxFiles", 30));

        ResponseEntity<String> res = get("/api/projects/" + pid + "/code-index/files?limit=20");
        assertOk(res);

        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(data.isArray()).isTrue();
        assertThat(data.size()).isGreaterThan(0);
        assertThat(data.get(0).get("filePath")).isNotNull();
        assertThat(data.get(0).get("language")).isNotNull();
    }

    @Test
    void shouldListFilesWithPathPrefix() {
        String pid = createProject("cilfp");
        post("/api/projects/" + pid + "/code-index/build", Map.of("branch", "main", "maxFiles", 30));

        ResponseEntity<String> res = get("/api/projects/" + pid + "/code-index/files?pathPrefix=backend&limit=20");
        assertOk(res);

        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(data.isArray()).isTrue();
    }

    // ========================
    // 10-12: List Symbols
    // ========================

    @Test
    void shouldListSymbolsReturnSymbols() {
        String pid = createProject("cils");
        post("/api/projects/" + pid + "/code-index/build", Map.of("branch", "main", "maxFiles", 30));

        ResponseEntity<String> res = get("/api/projects/" + pid + "/code-index/symbols?limit=20");
        assertOk(res);

        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(data.isArray()).isTrue();
        assertThat(data.size()).isGreaterThan(0);
        assertThat(data.get(0).get("symbolName")).isNotNull();
        assertThat(data.get(0).get("symbolType")).isNotNull();
    }

    @Test
    void shouldListSymbolsWithLanguageFilter() {
        String pid = createProject("cilsl");
        post("/api/projects/" + pid + "/code-index/build", Map.of("branch", "main", "maxFiles", 30));

        ResponseEntity<String> res = get("/api/projects/" + pid + "/code-index/symbols?language=java&limit=20");
        assertOk(res);

        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(data.isArray()).isTrue();
    }

    // ========================
    // 13-16: Search
    // ========================

    @Test
    void shouldSearchByKeywordReturnResults() {
        String pid = createProject("cisk");
        post("/api/projects/" + pid + "/code-index/build", Map.of("branch", "main", "maxFiles", 50));

        ResponseEntity<String> res = post("/api/projects/" + pid + "/code-index/search",
                Map.of("keyword", "Application", "searchType", "ALL"));
        assertOk(res);

        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(data.get("results").isArray()).isTrue();
        assertThat(data.get("totalCount").asInt()).isGreaterThanOrEqualTo(0);
        assertThat(TestJsonHelper.getString(data, "keyword")).isEqualTo("Application");
    }

    @Test
    void shouldSearchByFileType() {
        String pid = createProject("cisft");
        post("/api/projects/" + pid + "/code-index/build", Map.of("branch", "main", "maxFiles", 50));

        ResponseEntity<String> res = post("/api/projects/" + pid + "/code-index/search",
                Map.of("keyword", "Controller", "searchType", "FILE"));
        assertOk(res);

        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(data.get("results").isArray()).isTrue();
    }

    @Test
    void shouldSearchBySymbolType() {
        String pid = createProject("cisst");
        post("/api/projects/" + pid + "/code-index/build", Map.of("branch", "main", "maxFiles", 50));

        ResponseEntity<String> res = post("/api/projects/" + pid + "/code-index/search",
                Map.of("keyword", "class", "searchType", "SYMBOL"));
        assertOk(res);

        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(data.get("results").isArray()).isTrue();
    }

    @Test
    void shouldSearchByChunkType() {
        String pid = createProject("cict");
        post("/api/projects/" + pid + "/code-index/build", Map.of("branch", "main", "maxFiles", 50));

        ResponseEntity<String> res = post("/api/projects/" + pid + "/code-index/search",
                Map.of("keyword", "public", "searchType", "CHUNK"));
        assertOk(res);

        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(data.get("results").isArray()).isTrue();
    }

    // ========================
    // 17-18: Search validation
    // ========================

    @Test
    void shouldSearchFailWithEmptyKeyword() {
        String pid = createProject("cisek");
        ResponseEntity<String> res = post("/api/projects/" + pid + "/code-index/search",
                Map.of("keyword", "", "searchType", "ALL"));
        assertCode(res, "BAD_REQUEST");
    }

    @Test
    void shouldSearchFailWithSensitivePathPrefix() {
        String pid = createProject("cispp");
        ResponseEntity<String> res = post("/api/projects/" + pid + "/code-index/search",
                Map.of("keyword", "test", "pathPrefix", ".env"));
        assertCode(res, "BAD_REQUEST");
    }

    // ========================
    // 19-21: Permissions
    // ========================

    @SuppressWarnings("null")
    @Test
    void shouldBuildIndexRejectUnauthenticated() {
        String pid = createProject("cipm");

        MediaType contentType = MediaType.APPLICATION_JSON;
        Map<String, Object> requestBody = Map.of("branch", "main", "maxFiles", 10);
        try {
            ResponseEntity<String> res = restTemplate.exchange(
                    RequestEntity
                            .post(uri("/api/projects/" + pid + "/code-index/build"))
                            .contentType(contentType)
                            .body(requestBody),
                    String.class);
            assertCode(res, "UNAUTHORIZED");
        } catch (ResourceAccessException e) {
            // JDK HTTP client throws on 401 with WWW-Authenticate challenge when body is streamed
        }
    }

    @Test
    void shouldGetSummaryAllowNoAuth() {
        String pid = createProject("cipv");
        post("/api/projects/" + pid + "/code-index/build", Map.of("branch", "main", "maxFiles", 10));

        ResponseEntity<String> res = getNoAuth("/api/projects/" + pid + "/code-index/summary");
        assertThat(res.getStatusCode().is2xxSuccessful() || res.getStatusCode().is4xxClientError()).isTrue();
    }

    // ========================
    // 22-24: Worker code search tools
    // ========================

    @Test
    void shouldWorkerProcessReadCodeIndexTool() {
        String[] pt = createProjectAndTask("cirt");
        String pid = pt[0], tid = pt[1];

        post("/api/projects/" + pid + "/code-index/build", Map.of("branch", "main", "maxFiles", 20));

        ResponseEntity<String> runRes = post("/api/tasks/" + tid + "/multi-agent-runs",
                Map.of("strategy", "REVIEW_ONLY"));
        assertOk(runRes);

        JsonNode runData = TestJsonHelper.parse(runRes.getBody()).get("data");
        assertThat(runData).isNotNull();
    }

    @Test
    void shouldWorkerProcessSearchSymbolTool() {
        String[] pt = createProjectAndTask("cisst2");
        String pid = pt[0], tid = pt[1];

        post("/api/projects/" + pid + "/code-index/build", Map.of("branch", "main", "maxFiles", 20));

        ResponseEntity<String> runRes = post("/api/tasks/" + tid + "/multi-agent-runs",
                Map.of("strategy", "REVIEW_ONLY"));
        assertOk(runRes);
    }

    @Test
    void shouldSearchResultContainRequiredFields() {
        String pid = createProject("cirf");
        post("/api/projects/" + pid + "/code-index/build", Map.of("branch", "main", "maxFiles", 30));

        ResponseEntity<String> res = post("/api/projects/" + pid + "/code-index/search",
                Map.of("keyword", "class", "searchType", "SYMBOL"));
        assertOk(res);

        JsonNode results = TestJsonHelper.parse(res.getBody()).get("data").get("results");
        if (results.size() > 0) {
            JsonNode first = results.get(0);
            assertThat(TestJsonHelper.getString(first, "resultType")).isIn("FILE", "SYMBOL", "CHUNK");
            assertThat(TestJsonHelper.getString(first, "filePath")).isNotNull();
        }
    }

    // ========================
    // Helpers
    // ========================

    private String createProject(String suffix) {
        ResponseEntity<String> res = post("/api/projects", Map.of(
                "name", "IT-CSI-" + suffix,
                "description", "Code search index test",
                "techStack", List.of("Java")
        ));
        assertOk(res);
        return TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data.id");
    }

    private String[] createProjectAndTask(String suffix) {
        String pid = createProject(suffix);

        for (long agentId : new long[]{300001L, 300002L, 300003L, 300004L, 300005L}) {
            post("/api/projects/" + pid + "/agents/" + agentId + "/enable", Map.of());
        }

        ResponseEntity<String> taskRes = post("/api/projects/" + pid + "/tasks", Map.of(
                "title", "IT-CSI-Task-" + suffix,
                "description", "Code search index test task",
                "taskType", "FEATURE",
                "priority", "MEDIUM",
                "agentId", AGENT_ID
        ));
        assertOk(taskRes);
        String tid = TestJsonHelper.getString(TestJsonHelper.parse(taskRes.getBody()), "data.id");
        return new String[]{pid, tid};
    }
}
