package com.aicoding.platform.orchestration.application;

import com.aicoding.platform.orchestration.domain.ProjectToolConfigEntity;
import com.aicoding.platform.orchestration.domain.ToolCatalogEntity;
import com.aicoding.platform.orchestration.domain.ToolPolicyDecisionType;
import com.aicoding.platform.orchestration.domain.ToolRiskLevel;
import com.aicoding.platform.orchestration.infrastructure.ProjectToolConfigMapper;
import com.aicoding.platform.orchestration.infrastructure.ToolCatalogMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ToolPolicyService {

    private final ToolCatalogMapper toolCatalogMapper;
    private final ProjectToolConfigMapper projectToolConfigMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ToolPolicyService(ToolCatalogMapper toolCatalogMapper,
                             ProjectToolConfigMapper projectToolConfigMapper) {
        this.toolCatalogMapper = toolCatalogMapper;
        this.projectToolConfigMapper = projectToolConfigMapper;
    }

    public ToolPolicyDecision checkToolAllowed(Long projectId, String toolKey,
                                                String stepType, Long agentId) {
        // 1. Lookup tool by toolKey
        ToolCatalogEntity tool = toolCatalogMapper.selectOne(
                new LambdaQueryWrapper<ToolCatalogEntity>()
                        .eq(ToolCatalogEntity::getToolKey, toolKey));
        if (tool == null) {
            return ToolPolicyDecision.blocked(null, null, "工具不存在: " + toolKey);
        }

        // 2. Global disabled
        if (tool.getEnabled() == null || tool.getEnabled() != 1) {
            return ToolPolicyDecision.blocked(tool, null, "工具已被全局禁用");
        }

        // 3. DANGEROUS always blocked (no approval path)
        String riskLevel = tool.getRiskLevel();
        if (ToolRiskLevel.DANGEROUS.name().equals(riskLevel)) {
            return ToolPolicyDecision.blocked(tool, null, "工具风险等级过高: " + riskLevel);
        }

        // 4. Check project_tool_config
        ProjectToolConfigEntity config = projectToolConfigMapper.selectOne(
                new LambdaQueryWrapper<ProjectToolConfigEntity>()
                        .eq(ProjectToolConfigEntity::getProjectId, projectId)
                        .eq(ProjectToolConfigEntity::getToolId, tool.getId()));

        boolean projectEnabled;
        if (config != null) {
            projectEnabled = config.getEnabled() != null && config.getEnabled() == 1;
        } else {
            // No config: default — LOW allowed, MEDIUM/HIGH blocked
            projectEnabled = ToolRiskLevel.LOW.name().equals(riskLevel);
        }

        if (!projectEnabled) {
            return ToolPolicyDecision.blocked(tool, config,
                    "工具未在项目中启用（" + riskLevel + " 风险" + (config == null ? "默认禁止" : "已禁用") + "）");
        }

        // 5. Check policy_json
        if (tool.getPolicyJson() != null && !tool.getPolicyJson().isBlank()) {
            try {
                JsonNode policy = objectMapper.readTree(tool.getPolicyJson());

                // allowedStepTypes
                if (policy.has("allowedStepTypes") && stepType != null) {
                    boolean stepAllowed = false;
                    for (JsonNode allowed : policy.get("allowedStepTypes")) {
                        if (allowed.asText().equals(stepType)) {
                            stepAllowed = true;
                            break;
                        }
                    }
                    if (!stepAllowed) {
                        return ToolPolicyDecision.blocked(tool, config,
                                "工具不允许用于当前步骤: " + stepType);
                    }
                }

                // Shell allowed → blocked (never allow shell)
                if (policy.has("allowShell") && policy.get("allowShell").asBoolean()) {
                    return ToolPolicyDecision.blocked(tool, config,
                            "工具策略包含禁止的 shell 权限");
                }

                // Git write → blocked
                if (policy.has("allowGitWrite") && policy.get("allowGitWrite").asBoolean()) {
                    return ToolPolicyDecision.blocked(tool, config,
                            "工具策略包含禁止的 Git 写权限");
                }

                // File write → blocked
                if (policy.has("allowFileWrite") && policy.get("allowFileWrite").asBoolean()) {
                    return ToolPolicyDecision.blocked(tool, config,
                            "工具策略包含禁止的文件写权限");
                }
            } catch (JsonProcessingException e) {
                return ToolPolicyDecision.blocked(tool, config,
                        "工具策略解析失败: " + e.getMessage());
            }
        }

        // 6. HIGH risk → REQUIRES_APPROVAL (project must be enabled to reach here)
        if (ToolRiskLevel.HIGH.name().equals(riskLevel)) {
            return ToolPolicyDecision.requiresApproval(tool, config);
        }

        // LOW / MEDIUM with project enabled → ALLOWED
        return ToolPolicyDecision.allowed(tool, config);
    }

    public List<ToolCatalogEntity> listAllTools() {
        return toolCatalogMapper.selectList(
                new LambdaQueryWrapper<ToolCatalogEntity>()
                        .orderByAsc(ToolCatalogEntity::getId));
    }

    public ToolCatalogEntity getToolByKey(String toolKey) {
        return toolCatalogMapper.selectOne(
                new LambdaQueryWrapper<ToolCatalogEntity>()
                        .eq(ToolCatalogEntity::getToolKey, toolKey));
    }

    // ========================
    // Policy decision model
    // ========================

    public static class ToolPolicyDecision {
        private ToolPolicyDecisionType decisionType;
        private boolean allowed;
        private boolean requiresApproval;
        private String blockedReason;
        private ToolCatalogEntity toolCatalog;
        private ProjectToolConfigEntity projectConfig;

        public ToolPolicyDecision() {}

        public static ToolPolicyDecision allowed(ToolCatalogEntity tool, ProjectToolConfigEntity config) {
            ToolPolicyDecision d = new ToolPolicyDecision();
            d.decisionType = ToolPolicyDecisionType.ALLOWED;
            d.allowed = true;
            d.requiresApproval = false;
            d.toolCatalog = tool;
            d.projectConfig = config;
            return d;
        }

        public static ToolPolicyDecision blocked(ToolCatalogEntity tool, ProjectToolConfigEntity config,
                                                  String reason) {
            ToolPolicyDecision d = new ToolPolicyDecision();
            d.decisionType = ToolPolicyDecisionType.BLOCKED;
            d.allowed = false;
            d.requiresApproval = false;
            d.blockedReason = reason;
            d.toolCatalog = tool;
            d.projectConfig = config;
            return d;
        }

        public static ToolPolicyDecision requiresApproval(ToolCatalogEntity tool, ProjectToolConfigEntity config) {
            ToolPolicyDecision d = new ToolPolicyDecision();
            d.decisionType = ToolPolicyDecisionType.REQUIRES_APPROVAL;
            d.allowed = false;
            d.requiresApproval = true;
            d.toolCatalog = tool;
            d.projectConfig = config;
            return d;
        }

        public ToolPolicyDecisionType getDecisionType() { return decisionType; }
        public void setDecisionType(ToolPolicyDecisionType decisionType) { this.decisionType = decisionType; }

        public boolean isAllowed() { return allowed; }
        public void setAllowed(boolean allowed) { this.allowed = allowed; }

        public boolean isRequiresApproval() { return requiresApproval; }
        public void setRequiresApproval(boolean requiresApproval) { this.requiresApproval = requiresApproval; }

        public String getBlockedReason() { return blockedReason; }
        public void setBlockedReason(String blockedReason) { this.blockedReason = blockedReason; }

        public ToolCatalogEntity getToolCatalog() { return toolCatalog; }
        public void setToolCatalog(ToolCatalogEntity toolCatalog) { this.toolCatalog = toolCatalog; }

        public ProjectToolConfigEntity getProjectConfig() { return projectConfig; }
        public void setProjectConfig(ProjectToolConfigEntity projectConfig) { this.projectConfig = projectConfig; }
    }
}
