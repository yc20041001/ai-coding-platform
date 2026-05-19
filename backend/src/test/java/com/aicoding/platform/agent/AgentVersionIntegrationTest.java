package com.aicoding.platform.agent;

import com.aicoding.platform.agent.domain.AgentVersionStatus;
import com.aicoding.platform.agent.domain.AiAgentVersionEntity;
import com.aicoding.platform.agent.infrastructure.AiAgentVersionMapper;
import com.aicoding.platform.support.IntegrationTestBase;
import com.aicoding.platform.support.TestJsonHelper;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;

import static org.assertj.core.api.Assertions.assertThat;

class AgentVersionIntegrationTest extends IntegrationTestBase {

    @Autowired
    private AiAgentVersionMapper aiAgentVersionMapper;

    // ========================
    // 1. List versions
    // ========================

    @Test
    void shouldListAgentVersions() {
        ResponseEntity<String> res = get("/api/agents/" + AGENT_ID + "/versions");
        assertOk(res);

        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(data.isArray()).isTrue();
        assertThat(data.size()).isGreaterThanOrEqualTo(1);

        JsonNode first = data.get(0);
        assertThat(TestJsonHelper.getString(first, "id")).isNotEmpty();
        assertThat(TestJsonHelper.getString(first, "agentId")).isNotEmpty();
        assertThat(TestJsonHelper.getString(first, "versionNo")).isNotEmpty();
        assertThat(TestJsonHelper.getString(first, "status")).isNotEmpty();
        assertThat(first.has("systemPrompt")).isTrue();
        assertThat(first.has("toolPolicy")).isTrue();
        assertThat(first.has("executionPolicy")).isTrue();
        assertThat(first.has("createTime")).isTrue();
    }

    @Test
    void shouldReturnPublishedVersionsInList() {
        ResponseEntity<String> res = get("/api/agents/" + AGENT_ID + "/versions");
        assertOk(res);

        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        boolean hasPublished = false;
        for (JsonNode v : data) {
            if ("PUBLISHED".equals(TestJsonHelper.getString(v, "status"))) {
                hasPublished = true;
                assertThat(TestJsonHelper.getString(v, "systemPrompt")).isNotNull();
                assertThat(TestJsonHelper.getString(v, "toolPolicy")).isNotNull();
                assertThat(TestJsonHelper.getString(v, "executionPolicy")).isNotNull();
            }
        }
        assertThat(hasPublished).isTrue();
    }

    @Test
    void shouldRejectListVersionsUnauthenticated() {
        ResponseEntity<String> res = getNoAuth("/api/agents/" + AGENT_ID + "/versions");
        assertCode(res, "UNAUTHORIZED");
    }

    @Test
    void shouldReturnNotFoundForInvalidAgent() {
        ResponseEntity<String> res = get("/api/agents/99999999/versions");
        assertCode(res, "NOT_FOUND");
    }

    // ========================
    // 2. Get version detail
    // ========================

    @Test
    void shouldGetVersionDetail() {
        // Get first version from list
        ResponseEntity<String> listRes = get("/api/agents/" + AGENT_ID + "/versions");
        assertOk(listRes);
        JsonNode listData = TestJsonHelper.parse(listRes.getBody()).get("data");
        String versionId = TestJsonHelper.getString(listData.get(0), "id");

        ResponseEntity<String> res = get("/api/agents/" + AGENT_ID + "/versions/" + versionId);
        assertOk(res);

        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(TestJsonHelper.getString(data, "id")).isEqualTo(versionId);
        assertThat(TestJsonHelper.getString(data, "agentId")).isNotEmpty();
        assertThat(TestJsonHelper.getString(data, "versionNo")).isNotEmpty();
        assertThat(TestJsonHelper.getString(data, "status")).isNotEmpty();
        assertThat(data.has("systemPrompt")).isTrue();
        assertThat(data.has("toolPolicy")).isTrue();
        assertThat(data.has("executionPolicy")).isTrue();
    }

    @Test
    void shouldRejectVersionDetailUnauthenticated() {
        ResponseEntity<String> listRes = get("/api/agents/" + AGENT_ID + "/versions");
        assertOk(listRes);
        String versionId = TestJsonHelper.getString(
                TestJsonHelper.parse(listRes.getBody()).get("data").get(0), "id");

        ResponseEntity<String> res = getNoAuth("/api/agents/" + AGENT_ID + "/versions/" + versionId);
        assertCode(res, "UNAUTHORIZED");
    }

    @Test
    void shouldReturnNotFoundForInvalidVersionId() {
        ResponseEntity<String> res = get("/api/agents/" + AGENT_ID + "/versions/99999999");
        assertCode(res, "NOT_FOUND");
    }

    @Test
    void shouldRejectVersionNotBelongingToAgent() {
        // Use version from agent 300001 but query under agent 300002
        ResponseEntity<String> otherListRes = get("/api/agents/300001/versions");
        assertOk(otherListRes);
        String otherVersionId = TestJsonHelper.getString(
                TestJsonHelper.parse(otherListRes.getBody()).get("data").get(0), "id");

        ResponseEntity<String> res = get("/api/agents/" + AGENT_ID + "/versions/" + otherVersionId);
        assertCode(res, "BAD_REQUEST");
    }

    // ========================
    // 3. Version selection in Project Agent Enable
    // ========================

    @Test
    void shouldEnableWithNonLatestPublishedVersion() {
        // Get the published versions for agent 300002 and enable with the first one
        // (explicitly, not relying on "latest" resolution)
        String suffix = String.valueOf(System.currentTimeMillis());

        // Create a test project
        ResponseEntity<String> projRes = post("/api/projects", java.util.Map.of(
                "name", "IT-VerSel-" + suffix,
                "description", "Version selection integration test",
                "techStack", java.util.List.of("Java")
        ));
        assertOk(projRes);
        String projectId = TestJsonHelper.getString(
                TestJsonHelper.parse(projRes.getBody()), "data.id");

        // Get all published versions for this agent
        ResponseEntity<String> versionsRes = get("/api/agents/" + AGENT_ID + "/versions");
        assertOk(versionsRes);
        JsonNode versions = TestJsonHelper.parse(versionsRes.getBody()).get("data");

        // Find a published version
        String explicitVersionId = null;
        for (JsonNode v : versions) {
            if ("PUBLISHED".equals(TestJsonHelper.getString(v, "status"))) {
                explicitVersionId = TestJsonHelper.getString(v, "id");
                break;
            }
        }
        assertThat(explicitVersionId).isNotNull();

        // Enable with this specific version
        ResponseEntity<String> enableRes = post("/api/projects/" + projectId + "/agents/" + AGENT_ID + "/enable",
                java.util.Map.of("agentVersionId", explicitVersionId));
        assertOk(enableRes);

        // Verify the version was stored
        ResponseEntity<String> listRes = get("/api/projects/" + projectId + "/agents");
        assertOk(listRes);
        JsonNode data = TestJsonHelper.parse(listRes.getBody()).get("data");
        for (JsonNode agent : data) {
            if (AGENT_ID.equals(TestJsonHelper.getString(agent, "agentId"))) {
                assertThat(TestJsonHelper.getString(agent, "agentVersionId"))
                        .isEqualTo(explicitVersionId);
            }
        }
    }

    @Test
    void shouldStillRejectDraftVersionInEnable() {
        String suffix = String.valueOf(System.currentTimeMillis());

        ResponseEntity<String> projRes = post("/api/projects", java.util.Map.of(
                "name", "IT-DraftRej-" + suffix,
                "description", "Draft version rejection test",
                "techStack", java.util.List.of("Java")
        ));
        assertOk(projRes);
        String projectId = TestJsonHelper.getString(
                TestJsonHelper.parse(projRes.getBody()), "data.id");

        // Create a DRAFT version for agent 300002
        AiAgentVersionEntity published = aiAgentVersionMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AiAgentVersionEntity>()
                        .eq(AiAgentVersionEntity::getAgentId, 300002L)
                        .eq(AiAgentVersionEntity::getStatus, AgentVersionStatus.PUBLISHED.name())
                        .last("LIMIT 1"));

        AiAgentVersionEntity draft = new AiAgentVersionEntity();
        draft.setAgentId(300002L);
        draft.setVersionNo("9.9.9-draft-ver-test");
        draft.setSystemPrompt(published != null ? published.getSystemPrompt() : "");
        draft.setToolPolicy(published != null ? published.getToolPolicy() : "{}");
        draft.setExecutionPolicy(published != null ? published.getExecutionPolicy() : "{}");
        draft.setModelConfigId(published != null ? published.getModelConfigId() : null);
        draft.setStatus(AgentVersionStatus.DRAFT.name());
        aiAgentVersionMapper.insert(draft);

        try {
            ResponseEntity<String> res = post("/api/projects/" + projectId + "/agents/" + AGENT_ID + "/enable",
                    java.util.Map.of("agentVersionId", draft.getId().toString()));
            assertCode(res, "BAD_REQUEST");
        } finally {
            aiAgentVersionMapper.deleteById(draft.getId());
        }
    }
}
