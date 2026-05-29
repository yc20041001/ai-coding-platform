package com.aicoding.platform.orchestration;

import com.aicoding.platform.orchestration.domain.BetaReleaseGateRuleEntity;
import com.aicoding.platform.orchestration.infrastructure.BetaReleaseGateRuleMapper;
import com.aicoding.platform.support.IntegrationTestBase;
import com.aicoding.platform.support.TestJsonHelper;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class BetaReleaseGateRuleIntegrationTest extends IntegrationTestBase {

    @Autowired
    private BetaReleaseGateRuleMapper betaReleaseGateRuleMapper;

    private String projectId;
    private Long projectIdLong;

    @BeforeEach
    public void setUp() {
        String suffix = String.valueOf(System.currentTimeMillis());
        ResponseEntity<String> res = post("/api/projects", Map.of(
                "name", "IT-BetaGateRule-" + suffix,
                "description", "Beta release gate rule integration test",
                "techStack", List.of("Java")
        ));
        assertOk(res);
        projectId = TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data.id");
        projectIdLong = Long.valueOf(projectId);
    }

    @Test
    void shouldListRules() {
        ResponseEntity<String> res = get("/api/projects/" + projectId + "/beta/release-gate/rules");
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(data.isArray()).isTrue();
        // The seed data has 9 global rules that are enabled
        assertThat(data.size()).isGreaterThanOrEqualTo(9);
    }

    @Test
    void shouldListRulesSorted() {
        ResponseEntity<String> res = get("/api/projects/" + projectId + "/beta/release-gate/rules");
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(data.isArray()).isTrue();
        int previousOrder = -1;
        for (int i = 0; i < data.size(); i++) {
            int currentOrder = data.get(i).get("sortOrder").asInt();
            assertThat(currentOrder).isGreaterThanOrEqualTo(previousOrder);
            previousOrder = currentOrder;
        }
    }

    @Test
    void shouldUpdateRuleEnabled() {
        ResponseEntity<String> listRes = get("/api/projects/" + projectId + "/beta/release-gate/rules");
        assertOk(listRes);
        JsonNode rules = TestJsonHelper.parse(listRes.getBody()).get("data");
        String ruleId = rules.get(0).get("id").asText();

        // Toggle enabled to false
        ResponseEntity<String> res = put("/api/projects/" + projectId + "/beta/release-gate/rules/" + ruleId + "?enabled=false",
                Map.of());
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(data.get("enabled").asInt()).isEqualTo(0);

        // Toggle back to true
        res = put("/api/projects/" + projectId + "/beta/release-gate/rules/" + ruleId + "?enabled=true",
                Map.of());
        assertOk(res);
        data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(data.get("enabled").asInt()).isEqualTo(1);
    }

    @Test
    void shouldUpdateRuleBlocking() {
        ResponseEntity<String> listRes = get("/api/projects/" + projectId + "/beta/release-gate/rules");
        assertOk(listRes);
        JsonNode rules = TestJsonHelper.parse(listRes.getBody()).get("data");
        String ruleId = rules.get(0).get("id").asText();

        // Fetch the original blocking value
        int originalBlocking = rules.get(0).get("blocking").asInt();

        // Toggle blocking
        ResponseEntity<String> res = put("/api/projects/" + projectId + "/beta/release-gate/rules/" + ruleId + "?blocking=false",
                Map.of());
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(data.get("blocking").asInt()).isEqualTo(0);

        // Restore original
        res = put("/api/projects/" + projectId + "/beta/release-gate/rules/" + ruleId + "?blocking=" + originalBlocking,
                Map.of());
        assertOk(res);
        data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(data.get("blocking").asInt()).isEqualTo(originalBlocking);
    }

    @Test
    void shouldUpdateRuleThreshold() {
        ResponseEntity<String> listRes = get("/api/projects/" + projectId + "/beta/release-gate/rules");
        assertOk(listRes);
        JsonNode rules = TestJsonHelper.parse(listRes.getBody()).get("data");
        String ruleId = rules.get(0).get("id").asText();

        // Grab the rule's original threshold to restore later
        BigDecimal originalThreshold = rules.get(0).get("thresholdValue").decimalValue();

        // Update threshold to a known value
        ResponseEntity<String> res = put("/api/projects/" + projectId + "/beta/release-gate/rules/" + ruleId + "?thresholdValue=5.5",
                Map.of());
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(data.get("thresholdValue").decimalValue()).isEqualByComparingTo(new BigDecimal("5.5"));

        // Restore original threshold
        res = put("/api/projects/" + projectId + "/beta/release-gate/rules/" + ruleId + "?thresholdValue=" + originalThreshold,
                Map.of());
        assertOk(res);
    }

    @Test
    void shouldReturn404WhenUpdatingNonExistentRule() {
        ResponseEntity<String> res = put("/api/projects/" + projectId + "/beta/release-gate/rules/999999999?enabled=true",
                Map.of());
        assertCode(res, "NOT_FOUND");
    }

    @Test
    void shouldHandleInvalidProjectId() {
        ResponseEntity<String> res = get("/api/projects/invalid/beta/release-gate/rules");
        assertCode(res, "BAD_REQUEST");
    }

    @Test
    void shouldListRulesForSpecificProject() {
        // Insert a project-specific rule
        BetaReleaseGateRuleEntity projectRule = new BetaReleaseGateRuleEntity();
        projectRule.setProjectId(projectIdLong);
        projectRule.setRuleKey("PROJECT_SPECIFIC_RULE");
        projectRule.setCategory("CUSTOM");
        projectRule.setDisplayName("Project Custom Rule");
        projectRule.setThresholdOperator("GT");
        projectRule.setThresholdValue(new BigDecimal("10"));
        projectRule.setEnabled(1);
        projectRule.setBlocking(1);
        projectRule.setSortOrder(99);
        projectRule.setDescription("A project-specific rule");
        projectRule.setCreateTime(LocalDateTime.now());
        projectRule.setUpdateTime(LocalDateTime.now());
        betaReleaseGateRuleMapper.insert(projectRule);

        ResponseEntity<String> res = get("/api/projects/" + projectId + "/beta/release-gate/rules");
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(data.isArray()).isTrue();
        // Should have 9 global + 1 project-specific = 10 rules (all enabled)
        assertThat(data.size()).isGreaterThanOrEqualTo(10);

        // Verify the project-specific rule is included
        boolean found = false;
        for (int i = 0; i < data.size(); i++) {
            JsonNode rule = data.get(i);
            if ("PROJECT_SPECIFIC_RULE".equals(TestJsonHelper.getString(rule, "ruleKey"))) {
                found = true;
                assertThat(TestJsonHelper.getString(rule, "projectId")).isEqualTo(projectId);
                assertThat(TestJsonHelper.getString(rule, "category")).isEqualTo("CUSTOM");
                break;
            }
        }
        assertThat(found).isTrue();
    }

    @Test
    void shouldUpdateRuleWithProjectScope() {
        // Insert a project-specific rule
        BetaReleaseGateRuleEntity projectRule = new BetaReleaseGateRuleEntity();
        projectRule.setProjectId(projectIdLong);
        projectRule.setRuleKey("PROJECT_SCOPE_UPDATE");
        projectRule.setCategory("CUSTOM");
        projectRule.setDisplayName("Project Scope Rule");
        projectRule.setThresholdOperator("GTE");
        projectRule.setThresholdValue(new BigDecimal("50"));
        projectRule.setEnabled(1);
        projectRule.setBlocking(1);
        projectRule.setSortOrder(100);
        projectRule.setDescription("Rule for scope update test");
        projectRule.setCreateTime(LocalDateTime.now());
        projectRule.setUpdateTime(LocalDateTime.now());
        betaReleaseGateRuleMapper.insert(projectRule);

        String ruleId = projectRule.getId().toString();

        // Update all three attributes of the project-scoped rule
        ResponseEntity<String> res = put("/api/projects/" + projectId + "/beta/release-gate/rules/" + ruleId
                        + "?enabled=false&blocking=false&thresholdValue=25",
                Map.of());
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(data.get("enabled").asInt()).isEqualTo(0);
        assertThat(data.get("blocking").asInt()).isEqualTo(0);
        assertThat(data.get("thresholdValue").decimalValue()).isEqualByComparingTo(new BigDecimal("25"));
    }

    @Test
    void shouldReturn404ForNonExistentRuleOnUpdate() {
        // The update endpoint does not validate projectId format since it only looks up by ruleId.
        // A non-existent ruleId should return NOT_FOUND regardless of projectId format.
        ResponseEntity<String> res = put("/api/projects/not-a-number/beta/release-gate/rules/999999999?enabled=true",
                Map.of());
        assertCode(res, "NOT_FOUND");
    }

    @Test
    void shouldIncludeAllRuleFields() {
        ResponseEntity<String> res = get("/api/projects/" + projectId + "/beta/release-gate/rules");
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(data.isArray()).isTrue();
        assertThat(data.size()).isGreaterThan(0);

        JsonNode rule = data.get(0);
        assertThat(rule.has("id")).isTrue();
        assertThat(rule.has("ruleKey")).isTrue();
        assertThat(rule.has("category")).isTrue();
        assertThat(rule.has("displayName")).isTrue();
        assertThat(rule.has("thresholdOperator")).isTrue();
        assertThat(rule.has("thresholdValue")).isTrue();
        assertThat(rule.has("enabled")).isTrue();
        assertThat(rule.has("blocking")).isTrue();
        assertThat(rule.has("sortOrder")).isTrue();
        assertThat(rule.has("description")).isTrue();
        assertThat(rule.has("createTime")).isTrue();
        assertThat(rule.has("updateTime")).isTrue();
    }

    @Test
    void shouldReturn404WhenProjectNotFound() {
        // A project ID with valid long format but no existing project
        ResponseEntity<String> res = get("/api/projects/999999999/beta/release-gate/rules");
        JsonNode root = TestJsonHelper.parse(res.getBody());
        String code = TestJsonHelper.getString(root, "code");
        // The permission check throws PROJECT_ACCESS_DENIED when no member record exists
        assertThat(code).isIn("PROJECT_ACCESS_DENIED", "NOT_FOUND");
    }
}
