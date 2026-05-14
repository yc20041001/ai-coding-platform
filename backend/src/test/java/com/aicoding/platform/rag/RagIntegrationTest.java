package com.aicoding.platform.rag;

import com.aicoding.platform.support.IntegrationTestBase;
import com.aicoding.platform.support.TestJsonHelper;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RagIntegrationTest extends IntegrationTestBase {

    private String createProject() {
        ResponseEntity<String> res = post("/api/projects", Map.of(
                "name", "IT-RAG-Project-" + System.currentTimeMillis(),
                "description", "RAG integration test",
                "techStack", List.of("Python")
        ));
        return TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data.id");
    }

    @Test
    void shouldCreateKnowledgeBase() {
        String projectId = createProject();
        ResponseEntity<String> res = post("/api/projects/" + projectId + "/knowledge-bases", Map.of(
                "name", "IT-KB-" + System.currentTimeMillis(),
                "description", "Test knowledge base",
                "chunkSize", 200,
                "chunkOverlap", 20
        ));

        assertOk(res);
        JsonNode root = TestJsonHelper.parse(res.getBody());
        assertThat(TestJsonHelper.getString(root, "data.id")).isNotEmpty();
        assertThat(TestJsonHelper.getString(root, "data.status")).isNotEmpty();
    }

    @Test
    void shouldUploadDocumentAndCreateChunks() {
        String projectId = createProject();

        // Create KB
        ResponseEntity<String> kbRes = post("/api/projects/" + projectId + "/knowledge-bases", Map.of(
                "name", "IT-KB-Doc-" + System.currentTimeMillis(),
                "description", "Document test KB",
                "chunkSize", 200,
                "chunkOverlap", 20
        ));
        String kbId = TestJsonHelper.getString(TestJsonHelper.parse(kbRes.getBody()), "data.id");

        // Upload document
        String docContent = "# Integration Test Document\n\nThis document is used for RAG integration testing. Agent orchestrator processes tasks with context from the knowledge base.";
        ResponseEntity<String> docRes = post("/api/projects/" + projectId + "/knowledge-documents", Map.of(
                "knowledgeBaseId", kbId,
                "title", "IT-Doc-" + System.currentTimeMillis(),
                "documentType", "MARKDOWN",
                "sourceType", "MANUAL",
                "fileName", "integration-test.md",
                "content", docContent
        ));
        assertOk(docRes);
        JsonNode docRoot = TestJsonHelper.parse(docRes.getBody());
        String docId = TestJsonHelper.getString(docRoot, "data.id");
        assertThat(docId).isNotEmpty();

        // Verify chunks created
        ResponseEntity<String> chunksRes = get("/api/knowledge-documents/" + docId + "/chunks");
        assertOk(chunksRes);
        String chunksJson = TestJsonHelper.parse(chunksRes.getBody()).get("data").toString();
        assertThat(chunksJson).contains("chunkIndex");
    }

    @Test
    void shouldSearchRagAndReturnResults() {
        String projectId = createProject();

        // Create KB
        ResponseEntity<String> kbRes = post("/api/projects/" + projectId + "/knowledge-bases", Map.of(
                "name", "IT-KB-Search-" + System.currentTimeMillis(),
                "description", "Search test KB",
                "chunkSize", 200,
                "chunkOverlap", 20
        ));
        String kbId = TestJsonHelper.getString(TestJsonHelper.parse(kbRes.getBody()), "data.id");

        // Upload document with searchable content
        post("/api/projects/" + projectId + "/knowledge-documents", Map.of(
                "knowledgeBaseId", kbId,
                "title", "IT-Search-Doc-" + System.currentTimeMillis(),
                "documentType", "MARKDOWN",
                "sourceType", "MANUAL",
                "fileName", "search-test.md",
                "content", "# Agent Orchestrator Guide\n\nThe Agent Orchestrator is the core execution engine. It handles task orchestration with RAG context injection."
        ));

        // Search
        ResponseEntity<String> searchRes = post("/api/projects/" + projectId + "/rag/search", Map.of(
                "query", "Agent Orchestrator",
                "knowledgeBaseId", kbId,
                "limit", 5,
                "includeContent", true
        ));
        assertOk(searchRes);
        JsonNode searchRoot = TestJsonHelper.parse(searchRes.getBody());
        assertThat(TestJsonHelper.getLong(searchRoot, "data.total")).isGreaterThanOrEqualTo(1);
    }

    @Test
    void shouldRejectWithoutToken() {
        ResponseEntity<String> res = getNoAuth("/api/knowledge-documents/99999/chunks");
        assertCode(res, "UNAUTHORIZED");
    }
}
