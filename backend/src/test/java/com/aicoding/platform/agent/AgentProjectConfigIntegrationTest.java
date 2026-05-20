package com.aicoding.platform.agent;

import com.aicoding.platform.auth.domain.UserEntity;
import com.aicoding.platform.auth.domain.UserRoleEntity;
import com.aicoding.platform.auth.infrastructure.UserMapper;
import com.aicoding.platform.auth.infrastructure.UserRoleMapper;
import com.aicoding.platform.agent.infrastructure.AiAgentMapper;
import com.aicoding.platform.agent.infrastructure.AiAgentVersionMapper;
import com.aicoding.platform.agent.infrastructure.ModelConfigMapper;
import com.aicoding.platform.agent.domain.AiAgentEntity;
import com.aicoding.platform.agent.domain.AiAgentVersionEntity;
import com.aicoding.platform.agent.domain.AgentStatus;
import com.aicoding.platform.agent.domain.AgentVersionStatus;
import com.aicoding.platform.agent.domain.ModelConfigEntity;
import com.aicoding.platform.rag.domain.KnowledgeBaseEntity;
import com.aicoding.platform.rag.infrastructure.KnowledgeBaseMapper;
import com.aicoding.platform.support.IntegrationTestBase;
import com.aicoding.platform.support.TestJsonHelper;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.lang.NonNull;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

class AgentProjectConfigIntegrationTest extends IntegrationTestBase {

    private static final AtomicLong TEST_COUNTER = new AtomicLong(System.nanoTime());

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserRoleMapper userRoleMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AiAgentMapper aiAgentMapper;

    @Autowired
    private AiAgentVersionMapper aiAgentVersionMapper;

    @Autowired
    private ModelConfigMapper modelConfigMapper;

    @Autowired
    private KnowledgeBaseMapper knowledgeBaseMapper;

    private String projectIdValue;
    private String secondProjectIdValue;
    private String secondUserEmailValue;
    private String secondUserPasswordValue;

    private void ensureTestData() {
        if (projectIdValue != null) {
            return;
        }

        long counter = TEST_COUNTER.incrementAndGet();
        String suffix = String.valueOf(counter);

        // Create a test project (admin is auto-assigned as OWNER)
        ResponseEntity<String> res = post("/api/projects", Map.of(
                "name", "IT-AgentCfg-" + suffix,
                "description", "Agent config integration test",
                "techStack", List.of("Java")
        ));
        assertOk(res);
        projectIdValue = TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data.id");

        // Create a second user for non-member / non-owner tests
        secondUserEmailValue = "testuser" + suffix + "@example.com";
        secondUserPasswordValue = "Test@123456";

        UserEntity user = new UserEntity();
        user.setId(900000L + (counter % 100000));
        user.setUsername("testuser" + suffix);
        user.setEmail(secondUserEmailValue);
        user.setPassword(passwordEncoder.encode(secondUserPasswordValue));
        user.setStatus("ENABLED");
        userMapper.insert(user);

        UserRoleEntity userRole = new UserRoleEntity();
        userRole.setUserId(user.getId());
        userRole.setRoleId(2L); // USER role
        userRoleMapper.insert(userRole);
    }

    private @NonNull String projectId() {
        ensureTestData();
        return Objects.requireNonNull(projectIdValue);
    }

    private @NonNull String secondUserEmail() {
        ensureTestData();
        return Objects.requireNonNull(secondUserEmailValue);
    }

    private @NonNull String secondUserPassword() {
        ensureTestData();
        return Objects.requireNonNull(secondUserPasswordValue);
    }

    private @NonNull String secondProjectId() {
        ensureTestData();
        if (secondProjectIdValue == null) {
            String suffix = String.valueOf(TEST_COUNTER.incrementAndGet());
            ResponseEntity<String> res = post("/api/projects", Map.of(
                    "name", "IT-AgentCfg-Second-" + suffix,
                    "description", "Second project for cross-project KB validation",
                    "techStack", List.of("Python")
            ));
            assertOk(res);
            secondProjectIdValue = TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data.id");
        }
        return Objects.requireNonNull(secondProjectIdValue);
    }

    private KnowledgeBaseEntity createKnowledgeBase(Long projectId, String suffix) {
        KnowledgeBaseEntity kb = new KnowledgeBaseEntity();
        kb.setId(700000L + (TEST_COUNTER.incrementAndGet() % 100000));
        kb.setProjectId(projectId);
        kb.setName("KB-" + suffix);
        kb.setDescription("Test KB " + suffix);
        kb.setStatus("ENABLED");
        kb.setEmbeddingProvider("OPENAI");
        kb.setEmbeddingModel("text-embedding-3-small");
        kb.setChunkSize(800);
        kb.setChunkOverlap(100);
        kb.setDocumentCount(0L);
        kb.setChunkCount(0L);
        knowledgeBaseMapper.insert(kb);
        return kb;
    }

    // --- helper: login as a specific user and return token ---

    private @NonNull String loginAs(@NonNull String email, @NonNull String password) {
        ResponseEntity<String> res = restTemplate.postForEntity(
                baseUrl() + "/api/auth/login",
                Map.of("email", email, "password", password),
                String.class);
        JsonNode root = TestJsonHelper.parse(res.getBody());
        String code = TestJsonHelper.getString(root, "code");
        if (!"OK".equals(code)) {
            throw new RuntimeException("Login failed for " + email + ": " + res.getBody());
        }
        return Objects.requireNonNull(TestJsonHelper.getString(root, "data.accessToken"));
    }

    // --- helper: authenticated GET as a specific user ---

    private ResponseEntity<String> getAs(@NonNull String token, @NonNull String path) {
        RequestEntity<Void> entity = RequestEntity.get(nonNullUri(path))
                .headers(headers -> headers.setBearerAuth(nonNullString(token)))
                .build();
        return restTemplate.exchange(entity, String.class);
    }

    // --- helper: authenticated POST as a specific user ---

    private ResponseEntity<String> postAs(@NonNull String token, @NonNull String path, Object body) {
        RequestEntity<Object> entity = RequestEntity.post(nonNullUri(path))
                .contentType(nonNullJson())
                .headers(headers -> headers.setBearerAuth(nonNullString(token)))
                .body(Objects.requireNonNull(body));
        return restTemplate.exchange(entity, String.class);
    }

    // --- helper: unauthenticated POST ---

    private ResponseEntity<String> postNoAuth(@NonNull String path, Object body) {
        RequestEntity<Object> entity = RequestEntity.post(nonNullUri(path))
                .contentType(nonNullJson())
                .body(Objects.requireNonNull(body));
        return restTemplate.exchange(entity, String.class);
    }

    private @NonNull URI nonNullUri(@NonNull String path) {
        return Objects.requireNonNull(URI.create(baseUrl() + path));
    }

    private @NonNull MediaType nonNullJson() {
        return Objects.requireNonNull(MediaType.APPLICATION_JSON);
    }

    private @NonNull String nonNullString(@NonNull String value) {
        return Objects.requireNonNull(value);
    }

    // ========================
    // 1. GET list tests
    // ========================

    @Test
    void shouldListProjectAgentsAsOwner() {
        ResponseEntity<String> res = get("/api/projects/" + projectId() + "/agents");
        assertOk(res);

        JsonNode root = TestJsonHelper.parse(res.getBody());
        JsonNode data = root.get("data");
        assertThat(data.isArray()).isTrue();
        assertThat(data.size()).isGreaterThanOrEqualTo(6);

        // Each entry should have required fields
        JsonNode first = data.get(0);
        assertThat(TestJsonHelper.getString(first, "agentId")).isNotEmpty();
        assertThat(TestJsonHelper.getString(first, "agentName")).isNotEmpty();
        assertThat(TestJsonHelper.getString(first, "agentCode")).isNotEmpty();
        assertThat(TestJsonHelper.getString(first, "agentType")).isNotEmpty();
        assertThat(first.has("enabled")).isTrue();
    }

    @Test
    void shouldReturnEnabledFalseWhenNoProjectConfig() {
        // No enable call has been made, all agents should have enabled=false
        ResponseEntity<String> res = get("/api/projects/" + projectId() + "/agents");
        assertOk(res);

        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        for (JsonNode agent : data) {
            assertThat(TestJsonHelper.getBool(agent, "enabled")).isFalse();
        }
    }

    @Test
    void shouldIncludeLatestVersionInfo() {
        ResponseEntity<String> res = get("/api/projects/" + projectId() + "/agents");
        assertOk(res);

        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        for (JsonNode agent : data) {
            assertThat(TestJsonHelper.getString(agent, "agentVersionId")).isNotEmpty();
            assertThat(TestJsonHelper.getString(agent, "agentVersionNo")).isNotEmpty();
        }
    }

    @Test
    void shouldRejectUnauthenticated() {
        ResponseEntity<String> res = getNoAuth("/api/projects/" + projectId() + "/agents");
        assertCode(res, "UNAUTHORIZED");
    }

    @Test
    void shouldRejectNonMember() {
        String secondToken = loginAs(secondUserEmail(), secondUserPassword());
        ResponseEntity<String> res = getAs(secondToken, "/api/projects/" + projectId() + "/agents");
        assertCode(res, "PROJECT_ACCESS_DENIED");
    }

    @Test
    void shouldRejectEnableUnauthenticated() {
        try {
            ResponseEntity<String> res = postNoAuth(
                    "/api/projects/" + projectId() + "/agents/" + AGENT_ID + "/enable",
                    Map.of());
            assertCode(res, "UNAUTHORIZED");
        } catch (org.springframework.web.client.ResourceAccessException e) {
            // JDK HTTP client may throw on 401 when WWW-Authenticate challenge cannot be satisfied
        }
    }

    @Test
    void shouldRejectEnableByNonOwner() {
        String secondToken = loginAs(secondUserEmail(), secondUserPassword());
        ResponseEntity<String> res = postAs(secondToken,
                "/api/projects/" + projectId() + "/agents/" + AGENT_ID + "/enable",
                Map.of());
        assertCode(res, "PROJECT_ACCESS_DENIED");
    }

    @Test
    void shouldRejectDisableByNonOwner() {
        // First enable as admin so there is a config to disable
        post("/api/projects/" + projectId() + "/agents/" + AGENT_ID + "/enable", Map.of());

        String secondToken = loginAs(secondUserEmail(), secondUserPassword());
        ResponseEntity<String> res = postAs(secondToken,
                "/api/projects/" + projectId() + "/agents/" + AGENT_ID + "/disable",
                Map.of());
        assertCode(res, "PROJECT_ACCESS_DENIED");
    }

    // ========================
    // 3. Enable tests
    // ========================

    @Test
    void shouldEnableAgentSuccessfully() {
        ResponseEntity<String> res = post("/api/projects/" + projectId() + "/agents/" + AGENT_ID + "/enable",
                Map.of());
        assertOk(res);

        // Verify GET reflects enabled=true
        ResponseEntity<String> listRes = get("/api/projects/" + projectId() + "/agents");
        assertOk(listRes);
        JsonNode data = TestJsonHelper.parse(listRes.getBody()).get("data");
        boolean found = false;
        for (JsonNode agent : data) {
            if (AGENT_ID.equals(TestJsonHelper.getString(agent, "agentId"))) {
                assertThat(TestJsonHelper.getBool(agent, "enabled")).isTrue();
                assertThat(TestJsonHelper.getString(agent, "projectAgentConfigId")).isNotEmpty();
                found = true;
            }
        }
        assertThat(found).isTrue();
    }

    @Test
    void shouldEnableAgentIdempotent() {
        // Enable twice should succeed both times
        post("/api/projects/" + projectId() + "/agents/" + AGENT_ID + "/enable", Map.of());
        ResponseEntity<String> res = post("/api/projects/" + projectId() + "/agents/" + AGENT_ID + "/enable",
                Map.of());
        assertOk(res);
    }

    @Test
    void shouldRejectDisabledGlobalAgent() {
        // Find a global agent and disable it via update API (admin only)
        AiAgentEntity agent = aiAgentMapper.selectById(Long.valueOf(AGENT_ID));
        String originalStatus = agent.getStatus();
        agent.setStatus(AgentStatus.DISABLED.name());
        aiAgentMapper.updateById(agent);

        try {
            ResponseEntity<String> res = post("/api/projects/" + projectId() + "/agents/" + AGENT_ID + "/enable",
                    Map.of());
            assertCode(res, "BAD_REQUEST");
        } finally {
            // Restore original status
            agent.setStatus(originalStatus);
            aiAgentMapper.updateById(agent);
        }
    }

    @Test
    void shouldRejectVersionFromDifferentAgent() {
        // Get a version from agent 300001 (Architect Agent)
        AiAgentVersionEntity otherVersion = aiAgentVersionMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AiAgentVersionEntity>()
                        .eq(AiAgentVersionEntity::getAgentId, 300001L)
                        .eq(AiAgentVersionEntity::getStatus, AgentVersionStatus.PUBLISHED.name())
                        .last("LIMIT 1"));
        assertThat(otherVersion).isNotNull();

        ResponseEntity<String> res = post("/api/projects/" + projectId() + "/agents/" + AGENT_ID + "/enable",
                Map.of("agentVersionId", otherVersion.getId().toString()));
        assertCode(res, "BAD_REQUEST");
    }

    @Test
    void shouldRejectNonPublishedVersion() {
        // Get a PUBLISHED version for agent 300002, then create a DRAFT copy
        AiAgentVersionEntity published = aiAgentVersionMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AiAgentVersionEntity>()
                        .eq(AiAgentVersionEntity::getAgentId, 300002L)
                        .eq(AiAgentVersionEntity::getStatus, AgentVersionStatus.PUBLISHED.name())
                        .last("LIMIT 1"));
        assertThat(published).isNotNull();

        AiAgentVersionEntity draft = new AiAgentVersionEntity();
        draft.setAgentId(published.getAgentId());
        draft.setVersionNo("9.9.9-draft");
        draft.setSystemPrompt(published.getSystemPrompt());
        draft.setToolPolicy(published.getToolPolicy());
        draft.setExecutionPolicy(published.getExecutionPolicy());
        draft.setModelConfigId(published.getModelConfigId());
        draft.setStatus(AgentVersionStatus.DRAFT.name());
        aiAgentVersionMapper.insert(draft);

        try {
            ResponseEntity<String> res = post("/api/projects/" + projectId() + "/agents/" + AGENT_ID + "/enable",
                    Map.of("agentVersionId", draft.getId().toString()));
            assertCode(res, "BAD_REQUEST");
        } finally {
            aiAgentVersionMapper.deleteById(draft.getId());
        }
    }

    // ========================
    // 4. Disable tests
    // ========================

    @Test
    void shouldDisableAgentSuccessfully() {
        // Enable first
        post("/api/projects/" + projectId() + "/agents/" + AGENT_ID + "/enable", Map.of());

        // Disable
        ResponseEntity<String> res = post("/api/projects/" + projectId() + "/agents/" + AGENT_ID + "/disable",
                Map.of());
        assertOk(res);

        // Verify GET reflects enabled=false
        ResponseEntity<String> listRes = get("/api/projects/" + projectId() + "/agents");
        assertOk(listRes);
        JsonNode data = TestJsonHelper.parse(listRes.getBody()).get("data");
        for (JsonNode agent : data) {
            if (AGENT_ID.equals(TestJsonHelper.getString(agent, "agentId"))) {
                assertThat(TestJsonHelper.getBool(agent, "enabled")).isFalse();
            }
        }
    }

    @Test
    void shouldDisableIdempotentWhenNoConfig() {
        // Disable without prior enable should succeed (no-op)
        ResponseEntity<String> res = post("/api/projects/" + projectId() + "/agents/" + AGENT_ID + "/disable",
                Map.of());
        assertOk(res);
    }

    // ========================
    // 5. Enable with version selection
    // ========================

    @Test
    void shouldEnableWithExplicitVersionId() {
        // Get the latest PUBLISHED version for agent 300002
        AiAgentVersionEntity version = aiAgentVersionMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AiAgentVersionEntity>()
                        .eq(AiAgentVersionEntity::getAgentId, 300002L)
                        .eq(AiAgentVersionEntity::getStatus, AgentVersionStatus.PUBLISHED.name())
                        .last("LIMIT 1"));
        assertThat(version).isNotNull();

        ResponseEntity<String> res = post("/api/projects/" + projectId() + "/agents/" + AGENT_ID + "/enable",
                Map.of("agentVersionId", version.getId().toString()));
        assertOk(res);

        // Verify the version was stored
        ResponseEntity<String> listRes = get("/api/projects/" + projectId() + "/agents");
        assertOk(listRes);
        JsonNode data = TestJsonHelper.parse(listRes.getBody()).get("data");
        for (JsonNode agent : data) {
            if (AGENT_ID.equals(TestJsonHelper.getString(agent, "agentId"))) {
                assertThat(TestJsonHelper.getString(agent, "agentVersionId"))
                        .isEqualTo(version.getId().toString());
                assertThat(TestJsonHelper.getString(agent, "agentVersionNo"))
                        .isEqualTo(version.getVersionNo());
            }
        }
    }

    @Test
    void shouldResolveLatestVersionWhenOmitted() {
        ResponseEntity<String> res = post("/api/projects/" + projectId() + "/agents/" + AGENT_ID + "/enable",
                Map.of());
        assertOk(res);

        ResponseEntity<String> listRes = get("/api/projects/" + projectId() + "/agents");
        assertOk(listRes);
        JsonNode data = TestJsonHelper.parse(listRes.getBody()).get("data");
        for (JsonNode agent : data) {
            if (AGENT_ID.equals(TestJsonHelper.getString(agent, "agentId"))) {
                assertThat(TestJsonHelper.getString(agent, "agentVersionId")).isNotEmpty();
                assertThat(TestJsonHelper.getString(agent, "agentVersionNo")).isNotEmpty();
            }
        }
    }

    // ========================
    // 6. Model config validation tests
    // ========================

    private ModelConfigEntity createModelConfig(String suffix, String status) {
        ModelConfigEntity mc = new ModelConfigEntity();
        mc.setId(800000L + (TEST_COUNTER.incrementAndGet() % 100000));
        mc.setProvider("OPENAI");
        mc.setModelName("gpt-4.1-mini-" + suffix);
        mc.setModelType("CHAT");
        mc.setApiBase("https://api.openai.com");
        mc.setStatus(status);
        modelConfigMapper.insert(mc);
        return mc;
    }

    @Test
    void shouldEnableWithValidModelConfigId() {
        ModelConfigEntity mc = createModelConfig("valid", "ENABLED");
        try {
            ResponseEntity<String> res = post("/api/projects/" + projectId() + "/agents/" + AGENT_ID + "/enable",
                    Map.of("modelConfigId", mc.getId().toString()));
            assertOk(res);

            ResponseEntity<String> listRes = get("/api/projects/" + projectId() + "/agents");
            assertOk(listRes);
            JsonNode data = TestJsonHelper.parse(listRes.getBody()).get("data");
            for (JsonNode agent : data) {
                if (AGENT_ID.equals(TestJsonHelper.getString(agent, "agentId"))) {
                    assertThat(TestJsonHelper.getString(agent, "modelConfigId"))
                            .isEqualTo(mc.getId().toString());
                    assertThat(TestJsonHelper.getString(agent, "modelProvider"))
                            .isEqualTo("OPENAI");
                    assertThat(TestJsonHelper.getString(agent, "modelName"))
                            .isEqualTo(mc.getModelName());
                }
            }
        } finally {
            modelConfigMapper.deleteById(mc.getId());
        }
    }

    @Test
    void shouldReturnModelProviderAndNameInList() {
        ModelConfigEntity mc = createModelConfig("listtest", "ENABLED");
        try {
            post("/api/projects/" + projectId() + "/agents/" + AGENT_ID + "/enable",
                    Map.of("modelConfigId", mc.getId().toString()));

            ResponseEntity<String> listRes = get("/api/projects/" + projectId() + "/agents");
            assertOk(listRes);
            JsonNode data = TestJsonHelper.parse(listRes.getBody()).get("data");
            for (JsonNode agent : data) {
                if (AGENT_ID.equals(TestJsonHelper.getString(agent, "agentId"))) {
                    assertThat(TestJsonHelper.getBool(agent, "enabled")).isTrue();
                    assertThat(TestJsonHelper.getString(agent, "modelConfigId"))
                            .isEqualTo(mc.getId().toString());
                    assertThat(TestJsonHelper.getString(agent, "modelProvider"))
                            .isEqualTo("OPENAI");
                    assertThat(TestJsonHelper.getString(agent, "modelName"))
                            .isEqualTo(mc.getModelName());
                }
            }
        } finally {
            modelConfigMapper.deleteById(mc.getId());
        }
    }

    @Test
    void shouldRejectInvalidModelConfigId() {
        ResponseEntity<String> res = post("/api/projects/" + projectId() + "/agents/" + AGENT_ID + "/enable",
                Map.of("modelConfigId", "99999999"));
        assertCode(res, "BAD_REQUEST");
    }

    @Test
    void shouldRejectDisabledModelConfig() {
        ModelConfigEntity mc = createModelConfig("disabled", "DISABLED");
        try {
            ResponseEntity<String> res = post("/api/projects/" + projectId() + "/agents/" + AGENT_ID + "/enable",
                    Map.of("modelConfigId", mc.getId().toString()));
            assertCode(res, "BAD_REQUEST");
        } finally {
            modelConfigMapper.deleteById(mc.getId());
        }
    }

    @Test
    void shouldEnableWithoutModelConfigId() {
        ResponseEntity<String> res = post("/api/projects/" + projectId() + "/agents/" + AGENT_ID + "/enable",
                Map.of("modelConfigId", ""));
        assertOk(res);

        ResponseEntity<String> listRes = get("/api/projects/" + projectId() + "/agents");
        assertOk(listRes);
        JsonNode data = TestJsonHelper.parse(listRes.getBody()).get("data");
        for (JsonNode agent : data) {
            if (AGENT_ID.equals(TestJsonHelper.getString(agent, "agentId"))) {
                assertThat(TestJsonHelper.getBool(agent, "enabled")).isTrue();
            }
        }
    }

    @Test
    void shouldClearModelConfigAfterDisableReenable() {
        ModelConfigEntity mc = createModelConfig("reenable", "ENABLED");
        try {
            // Enable with model config
            post("/api/projects/" + projectId() + "/agents/" + AGENT_ID + "/enable",
                    Map.of("modelConfigId", mc.getId().toString()));

            // Verify model config is set
            ResponseEntity<String> listRes = get("/api/projects/" + projectId() + "/agents");
            JsonNode data = TestJsonHelper.parse(listRes.getBody()).get("data");
            for (JsonNode agent : data) {
                if (AGENT_ID.equals(TestJsonHelper.getString(agent, "agentId"))) {
                    assertThat(TestJsonHelper.getString(agent, "modelConfigId"))
                            .isEqualTo(mc.getId().toString());
                }
            }

            // Disable
            post("/api/projects/" + projectId() + "/agents/" + AGENT_ID + "/disable", Map.of());

            // Verify disabled - model config info should not show as enabled
            listRes = get("/api/projects/" + projectId() + "/agents");
            data = TestJsonHelper.parse(listRes.getBody()).get("data");
            for (JsonNode agent : data) {
                if (AGENT_ID.equals(TestJsonHelper.getString(agent, "agentId"))) {
                    assertThat(TestJsonHelper.getBool(agent, "enabled")).isFalse();
                }
            }
        } finally {
            modelConfigMapper.deleteById(mc.getId());
        }
    }

    // ========================
    // 7. Runtime config tests
    // ========================

    @Test
    void shouldEnableWithRuntimeConfigAndReturnIt() {
        Map<String, Object> config = Map.of(
                "temperature", 0.7,
                "maxTokens", 8192,
                "timeoutSeconds", 120,
                "useRag", true,
                "customInstruction", "优先遵循项目代码规范"
        );
        ResponseEntity<String> res = post("/api/projects/" + projectId() + "/agents/" + AGENT_ID + "/enable",
                Map.of("config", config));
        assertOk(res);

        ResponseEntity<String> listRes = get("/api/projects/" + projectId() + "/agents");
        assertOk(listRes);
        JsonNode data = TestJsonHelper.parse(listRes.getBody()).get("data");
        for (JsonNode agent : data) {
            if (AGENT_ID.equals(TestJsonHelper.getString(agent, "agentId"))) {
                assertThat(TestJsonHelper.getBool(agent, "enabled")).isTrue();
                JsonNode cfg = agent.get("config");
                assertThat(cfg).isNotNull();
                assertThat(TestJsonHelper.getBigDecimal(cfg, "temperature")).isEqualByComparingTo(new BigDecimal("0.7"));
                assertThat(TestJsonHelper.getInt(cfg, "maxTokens")).isEqualTo(8192);
                assertThat(TestJsonHelper.getInt(cfg, "timeoutSeconds")).isEqualTo(120);
                assertThat(TestJsonHelper.getBool(cfg, "useRag")).isTrue();
                assertThat(TestJsonHelper.getString(cfg, "customInstruction")).isEqualTo("优先遵循项目代码规范");
            }
        }
    }

    @Test
    void shouldUseDefaultsWhenConfigIsEmpty() {
        ResponseEntity<String> res = post("/api/projects/" + projectId() + "/agents/" + AGENT_ID + "/enable",
                Map.of("config", Map.of()));
        assertOk(res);

        ResponseEntity<String> listRes = get("/api/projects/" + projectId() + "/agents");
        assertOk(listRes);
        JsonNode data = TestJsonHelper.parse(listRes.getBody()).get("data");
        for (JsonNode agent : data) {
            if (AGENT_ID.equals(TestJsonHelper.getString(agent, "agentId"))) {
                JsonNode cfg = agent.get("config");
                assertThat(cfg).isNotNull();
                assertThat(TestJsonHelper.getBigDecimal(cfg, "temperature")).isEqualByComparingTo(new BigDecimal("0.2"));
                assertThat(TestJsonHelper.getInt(cfg, "maxTokens")).isEqualTo(4096);
                assertThat(TestJsonHelper.getInt(cfg, "timeoutSeconds")).isEqualTo(60);
                assertThat(TestJsonHelper.getBool(cfg, "useRag")).isFalse();
                assertThat(TestJsonHelper.getString(cfg, "customInstruction")).isEqualTo("");
            }
        }
    }

    @Test
    void shouldUseDefaultsWhenConfigIsNull() {
        ResponseEntity<String> res = post("/api/projects/" + projectId() + "/agents/" + AGENT_ID + "/enable",
                Map.of());
        assertOk(res);

        ResponseEntity<String> listRes = get("/api/projects/" + projectId() + "/agents");
        assertOk(listRes);
        JsonNode data = TestJsonHelper.parse(listRes.getBody()).get("data");
        for (JsonNode agent : data) {
            if (AGENT_ID.equals(TestJsonHelper.getString(agent, "agentId"))) {
                JsonNode cfg = agent.get("config");
                assertThat(cfg).isNotNull();
                assertThat(TestJsonHelper.getBigDecimal(cfg, "temperature")).isEqualByComparingTo(new BigDecimal("0.2"));
                assertThat(TestJsonHelper.getInt(cfg, "maxTokens")).isEqualTo(4096);
                assertThat(TestJsonHelper.getInt(cfg, "timeoutSeconds")).isEqualTo(60);
                assertThat(TestJsonHelper.getBool(cfg, "useRag")).isFalse();
            }
        }
    }

    @Test
    void shouldUpdateConfigOnReEnable() {
        // First enable with initial config
        Map<String, Object> config1 = Map.of(
                "temperature", 0.5,
                "maxTokens", 4096,
                "useRag", false,
                "customInstruction", "初始指令"
        );
        post("/api/projects/" + projectId() + "/agents/" + AGENT_ID + "/enable",
                Map.of("config", config1));

        // Re-enable with updated config
        Map<String, Object> config2 = Map.of(
                "temperature", 1.2,
                "maxTokens", 16384,
                "useRag", true,
                "customInstruction", "更新后的指令"
        );
        ResponseEntity<String> res = post("/api/projects/" + projectId() + "/agents/" + AGENT_ID + "/enable",
                Map.of("config", config2));
        assertOk(res);

        ResponseEntity<String> listRes = get("/api/projects/" + projectId() + "/agents");
        assertOk(listRes);
        JsonNode data = TestJsonHelper.parse(listRes.getBody()).get("data");
        for (JsonNode agent : data) {
            if (AGENT_ID.equals(TestJsonHelper.getString(agent, "agentId"))) {
                JsonNode cfg = agent.get("config");
                assertThat(TestJsonHelper.getBigDecimal(cfg, "temperature")).isEqualByComparingTo(new BigDecimal("1.2"));
                assertThat(TestJsonHelper.getInt(cfg, "maxTokens")).isEqualTo(16384);
                assertThat(TestJsonHelper.getBool(cfg, "useRag")).isTrue();
                assertThat(TestJsonHelper.getString(cfg, "customInstruction")).isEqualTo("更新后的指令");
            }
        }
    }

    @Test
    void shouldRejectTemperatureBelowZero() {
        Map<String, Object> config = Map.of("temperature", -0.1);
        ResponseEntity<String> res = post("/api/projects/" + projectId() + "/agents/" + AGENT_ID + "/enable",
                Map.of("config", config));
        assertCode(res, "BAD_REQUEST");
    }

    @Test
    void shouldRejectTemperatureAboveTwo() {
        Map<String, Object> config = Map.of("temperature", 2.1);
        ResponseEntity<String> res = post("/api/projects/" + projectId() + "/agents/" + AGENT_ID + "/enable",
                Map.of("config", config));
        assertCode(res, "BAD_REQUEST");
    }

    @Test
    void shouldRejectMaxTokensBelowMinimum() {
        Map<String, Object> config = Map.of("maxTokens", 255);
        ResponseEntity<String> res = post("/api/projects/" + projectId() + "/agents/" + AGENT_ID + "/enable",
                Map.of("config", config));
        assertCode(res, "BAD_REQUEST");
    }

    @Test
    void shouldRejectMaxTokensAboveMaximum() {
        Map<String, Object> config = Map.of("maxTokens", 32769);
        ResponseEntity<String> res = post("/api/projects/" + projectId() + "/agents/" + AGENT_ID + "/enable",
                Map.of("config", config));
        assertCode(res, "BAD_REQUEST");
    }

    @Test
    void shouldRejectTimeoutSecondsBelowMinimum() {
        Map<String, Object> config = Map.of("timeoutSeconds", 4);
        ResponseEntity<String> res = post("/api/projects/" + projectId() + "/agents/" + AGENT_ID + "/enable",
                Map.of("config", config));
        assertCode(res, "BAD_REQUEST");
    }

    @Test
    void shouldRejectTimeoutSecondsAboveMaximum() {
        Map<String, Object> config = Map.of("timeoutSeconds", 601);
        ResponseEntity<String> res = post("/api/projects/" + projectId() + "/agents/" + AGENT_ID + "/enable",
                Map.of("config", config));
        assertCode(res, "BAD_REQUEST");
    }

    @Test
    void shouldRejectCustomInstructionTooLong() {
        String longInstruction = "A".repeat(2001);
        Map<String, Object> config = Map.of("customInstruction", longInstruction);
        ResponseEntity<String> res = post("/api/projects/" + projectId() + "/agents/" + AGENT_ID + "/enable",
                Map.of("config", config));
        assertCode(res, "BAD_REQUEST");
    }

    @Test
    void shouldRejectKnowledgeBaseNotBelongingToProject() {
        // Create a knowledge base in a different project
        KnowledgeBaseEntity kb = createKnowledgeBase(Long.valueOf(secondProjectId()), "cross-project");
        try {
            Map<String, Object> config = Map.of(
                    "useRag", true,
                    "knowledgeBaseId", kb.getId().toString()
            );
            ResponseEntity<String> res = post("/api/projects/" + projectId() + "/agents/" + AGENT_ID + "/enable",
                    Map.of("config", config));
            assertCode(res, "BAD_REQUEST");
        } finally {
            knowledgeBaseMapper.deleteById(kb.getId());
        }
    }

    @Test
    void shouldAcceptKnowledgeBaseBelongingToProject() {
        KnowledgeBaseEntity kb = createKnowledgeBase(Long.valueOf(projectId()), "same-project");
        try {
            Map<String, Object> config = Map.of(
                    "useRag", true,
                    "knowledgeBaseId", kb.getId().toString()
            );
            ResponseEntity<String> res = post("/api/projects/" + projectId() + "/agents/" + AGENT_ID + "/enable",
                    Map.of("config", config));
            assertOk(res);

            ResponseEntity<String> listRes = get("/api/projects/" + projectId() + "/agents");
            assertOk(listRes);
            JsonNode data = TestJsonHelper.parse(listRes.getBody()).get("data");
            for (JsonNode agent : data) {
                if (AGENT_ID.equals(TestJsonHelper.getString(agent, "agentId"))) {
                    JsonNode cfg = agent.get("config");
                    assertThat(TestJsonHelper.getString(cfg, "knowledgeBaseId"))
                            .isEqualTo(kb.getId().toString());
                }
            }
        } finally {
            knowledgeBaseMapper.deleteById(kb.getId());
        }
    }
}
