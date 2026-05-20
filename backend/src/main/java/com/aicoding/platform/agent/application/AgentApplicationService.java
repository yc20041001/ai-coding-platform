package com.aicoding.platform.agent.application;

import com.aicoding.platform.agent.domain.AgentStatus;
import com.aicoding.platform.agent.domain.AgentVersionStatus;
import com.aicoding.platform.agent.domain.AiAgentEntity;
import com.aicoding.platform.agent.domain.AiAgentVersionEntity;
import com.aicoding.platform.agent.domain.ModelConfigEntity;
import com.aicoding.platform.agent.domain.ProjectAgentConfigEntity;
import com.aicoding.platform.agent.dto.AgentDetailResponse;
import com.aicoding.platform.agent.dto.AgentResponse;
import com.aicoding.platform.agent.dto.AgentVersionResponse;
import com.aicoding.platform.agent.dto.CreateAgentRequest;
import com.aicoding.platform.agent.dto.EnableProjectAgentRequest;
import com.aicoding.platform.agent.dto.ProjectAgentConfigResponse;
import com.aicoding.platform.agent.dto.ProjectAgentRuntimeConfig;
import com.aicoding.platform.agent.dto.UpdateAgentRequest;
import com.aicoding.platform.agent.infrastructure.AiAgentMapper;
import com.aicoding.platform.agent.infrastructure.AiAgentVersionMapper;
import com.aicoding.platform.agent.infrastructure.ModelConfigMapper;
import com.aicoding.platform.agent.infrastructure.ProjectAgentConfigMapper;
import com.aicoding.platform.common.exception.BizException;
import com.aicoding.platform.common.exception.ErrorCode;
import com.aicoding.platform.member.application.ProjectPermissionService;
import com.aicoding.platform.member.domain.ProjectRole;
import com.aicoding.platform.rag.domain.KnowledgeBaseEntity;
import com.aicoding.platform.rag.infrastructure.KnowledgeBaseMapper;
import com.aicoding.platform.security.context.LoginUser;
import com.aicoding.platform.security.context.LoginUserContext;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class AgentApplicationService {

    private static final Integer DEFAULT_MAX_TOKENS = 4096;
    private static final Integer DEFAULT_TIMEOUT_SECONDS = 60;

    private final AiAgentMapper aiAgentMapper;
    private final AiAgentVersionMapper aiAgentVersionMapper;
    private final ProjectAgentConfigMapper projectAgentConfigMapper;
    private final ModelConfigMapper modelConfigMapper;
    private final ProjectPermissionService projectPermissionService;
    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final ObjectMapper objectMapper;

    public AgentApplicationService(AiAgentMapper aiAgentMapper,
                                    AiAgentVersionMapper aiAgentVersionMapper,
                                    ProjectAgentConfigMapper projectAgentConfigMapper,
                                    ModelConfigMapper modelConfigMapper,
                                    ProjectPermissionService projectPermissionService,
                                    KnowledgeBaseMapper knowledgeBaseMapper,
                                    ObjectMapper objectMapper) {
        this.aiAgentMapper = aiAgentMapper;
        this.aiAgentVersionMapper = aiAgentVersionMapper;
        this.projectAgentConfigMapper = projectAgentConfigMapper;
        this.modelConfigMapper = modelConfigMapper;
        this.projectPermissionService = projectPermissionService;
        this.knowledgeBaseMapper = knowledgeBaseMapper;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<AgentResponse> listAgents(String type, String status) {
        LambdaQueryWrapper<AiAgentEntity> wrapper = new LambdaQueryWrapper<>();
        if (type != null && !type.isBlank()) {
            wrapper.eq(AiAgentEntity::getType, type);
        }
        if (status != null && !status.isBlank()) {
            wrapper.eq(AiAgentEntity::getStatus, status);
        }
        wrapper.orderByAsc(AiAgentEntity::getType);

        return aiAgentMapper.selectList(wrapper).stream()
                .map(this::toAgentResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public AgentDetailResponse createAgent(CreateAgentRequest request) {
        requireAdmin();

        AiAgentEntity agent = new AiAgentEntity();
        agent.setName(request.getName());
        agent.setCode(request.getCode());
        agent.setType(request.getType());
        agent.setDescription(request.getDescription());
        agent.setStatus(AgentStatus.ENABLED.name());
        aiAgentMapper.insert(agent);

        AiAgentVersionEntity version = new AiAgentVersionEntity();
        version.setAgentId(agent.getId());
        version.setVersionNo("1.0.0");
        version.setSystemPrompt(request.getSystemPrompt() != null ? request.getSystemPrompt() : "");
        version.setToolPolicy(request.getToolPolicy());
        version.setExecutionPolicy(request.getExecutionPolicy());
        if (request.getModelConfigId() != null && !request.getModelConfigId().isBlank()) {
            version.setModelConfigId(Long.valueOf(request.getModelConfigId()));
        }
        version.setStatus(AgentVersionStatus.PUBLISHED.name());
        version.setPublishTime(LocalDateTime.now());
        aiAgentVersionMapper.insert(version);

        return toAgentDetailResponse(agent, version);
    }

    @Transactional(readOnly = true)
    public AgentDetailResponse getAgentDetail(Long agentId) {
        AiAgentEntity agent = aiAgentMapper.selectById(agentId);
        if (agent == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "Agent 不存在");
        }

        AiAgentVersionEntity latestVersion = aiAgentVersionMapper.selectOne(
                new LambdaQueryWrapper<AiAgentVersionEntity>()
                        .eq(AiAgentVersionEntity::getAgentId, agentId)
                        .eq(AiAgentVersionEntity::getStatus, AgentVersionStatus.PUBLISHED.name())
                        .orderByDesc(AiAgentVersionEntity::getPublishTime)
                        .last("LIMIT 1"));

        return toAgentDetailResponse(agent, latestVersion);
    }

    @Transactional
    public boolean updateAgent(Long agentId, UpdateAgentRequest request) {
        requireAdmin();

        AiAgentEntity agent = aiAgentMapper.selectById(agentId);
        if (agent == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "Agent 不存在");
        }

        if (request.getName() != null) {
            agent.setName(request.getName());
        }
        if (request.getDescription() != null) {
            agent.setDescription(request.getDescription());
        }
        if (request.getStatus() != null) {
            agent.setStatus(request.getStatus());
        }
        aiAgentMapper.updateById(agent);
        return true;
    }

    @Transactional
    public boolean enableProjectAgent(Long projectId, Long agentId, EnableProjectAgentRequest request) {
        projectPermissionService.checkProjectRole(projectId, ProjectRole.OWNER);

        AiAgentEntity agent = aiAgentMapper.selectById(agentId);
        if (agent == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "Agent 不存在");
        }
        if (AgentStatus.DISABLED.name().equals(agent.getStatus())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "Agent 已停用，无法启用");
        }

        Long versionId = resolveVersionId(agentId, request.getAgentVersionId());
        validateVersionBelongsToAgent(agentId, versionId);

        if (request.getModelConfigId() != null && !request.getModelConfigId().isBlank()) {
            validateModelConfig(Long.valueOf(request.getModelConfigId()));
        }

        ProjectAgentRuntimeConfig runtimeConfig = normalizeRuntimeConfig(request.getConfig());
        validateRuntimeConfig(projectId, runtimeConfig);
        String configJson = serializeRuntimeConfig(runtimeConfig);

        ProjectAgentConfigEntity existing = projectAgentConfigMapper.selectOne(
                new LambdaQueryWrapper<ProjectAgentConfigEntity>()
                        .eq(ProjectAgentConfigEntity::getProjectId, projectId)
                        .eq(ProjectAgentConfigEntity::getAgentId, agentId));

        if (existing != null) {
            existing.setEnabled(1);
            existing.setAgentVersionId(versionId);
            if (request.getModelConfigId() != null && !request.getModelConfigId().isBlank()) {
                existing.setModelConfigId(Long.valueOf(request.getModelConfigId()));
            }
            existing.setConfigJson(configJson);
            projectAgentConfigMapper.updateById(existing);
        } else {
            ProjectAgentConfigEntity config = new ProjectAgentConfigEntity();
            config.setProjectId(projectId);
            config.setAgentId(agentId);
            config.setAgentVersionId(versionId);
            config.setEnabled(1);
            if (request.getModelConfigId() != null && !request.getModelConfigId().isBlank()) {
                config.setModelConfigId(Long.valueOf(request.getModelConfigId()));
            }
            config.setConfigJson(configJson);
            projectAgentConfigMapper.insert(config);
        }
        return true;
    }

    @Transactional
    public boolean disableProjectAgent(Long projectId, Long agentId) {
        projectPermissionService.checkProjectRole(projectId, ProjectRole.OWNER);

        ProjectAgentConfigEntity existing = projectAgentConfigMapper.selectOne(
                new LambdaQueryWrapper<ProjectAgentConfigEntity>()
                        .eq(ProjectAgentConfigEntity::getProjectId, projectId)
                        .eq(ProjectAgentConfigEntity::getAgentId, agentId));

        if (existing != null) {
            existing.setEnabled(0);
            projectAgentConfigMapper.updateById(existing);
        }
        return true;
    }

    @Transactional(readOnly = true)
    public List<AgentVersionResponse> listAgentVersions(Long agentId) {
        AiAgentEntity agent = aiAgentMapper.selectById(agentId);
        if (agent == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "Agent 不存在");
        }

        List<AiAgentVersionEntity> versions = aiAgentVersionMapper.selectList(
                new LambdaQueryWrapper<AiAgentVersionEntity>()
                        .eq(AiAgentVersionEntity::getAgentId, agentId)
                        .orderByDesc(AiAgentVersionEntity::getCreateTime));

        return versions.stream()
                .map(v -> toAgentVersionResponse(agentId, v))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AgentVersionResponse getAgentVersion(Long agentId, Long versionId) {
        AiAgentEntity agent = aiAgentMapper.selectById(agentId);
        if (agent == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "Agent 不存在");
        }

        AiAgentVersionEntity version = aiAgentVersionMapper.selectById(versionId);
        if (version == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "Agent 版本不存在");
        }
        if (!version.getAgentId().equals(agentId)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "版本不属于该 Agent");
        }

        return toAgentVersionResponse(agentId, version);
    }

    @Transactional(readOnly = true)
    public List<ProjectAgentConfigResponse> listProjectAgents(Long projectId) {
        projectPermissionService.checkProjectMember(projectId);

        List<AiAgentEntity> agents = aiAgentMapper.selectList(
                new LambdaQueryWrapper<AiAgentEntity>()
                        .orderByAsc(AiAgentEntity::getType));

        return agents.stream().map(agent -> {
            ProjectAgentConfigResponse resp = new ProjectAgentConfigResponse();
            resp.setProjectId(projectId.toString());
            resp.setAgentId(agent.getId().toString());
            resp.setAgentName(agent.getName());
            resp.setAgentCode(agent.getCode());
            resp.setAgentType(agent.getType());
            resp.setAgentStatus(agent.getStatus());
            resp.setAgentDescription(agent.getDescription());

            ProjectAgentConfigEntity config = projectAgentConfigMapper.selectOne(
                    new LambdaQueryWrapper<ProjectAgentConfigEntity>()
                            .eq(ProjectAgentConfigEntity::getProjectId, projectId)
                            .eq(ProjectAgentConfigEntity::getAgentId, agent.getId()));

            AiAgentVersionEntity latestVersion = aiAgentVersionMapper.selectOne(
                    new LambdaQueryWrapper<AiAgentVersionEntity>()
                            .eq(AiAgentVersionEntity::getAgentId, agent.getId())
                            .eq(AiAgentVersionEntity::getStatus, AgentVersionStatus.PUBLISHED.name())
                            .orderByDesc(AiAgentVersionEntity::getPublishTime)
                            .last("LIMIT 1"));

            if (config != null) {
                resp.setEnabled(config.getEnabled() == 1);
                resp.setProjectAgentConfigId(config.getId().toString());
                resp.setConfigJson(config.getConfigJson());
                resp.setConfig(deserializeRuntimeConfig(config.getConfigJson()));
                resp.setUpdateTime(config.getUpdateTime() != null ? config.getUpdateTime().toString() : null);

                if (config.getAgentVersionId() != null) {
                    resp.setAgentVersionId(config.getAgentVersionId().toString());
                    AiAgentVersionEntity selectedVersion = aiAgentVersionMapper.selectById(config.getAgentVersionId());
                    if (selectedVersion != null) {
                        resp.setAgentVersionNo(selectedVersion.getVersionNo());
                    }
                }
                if (config.getModelConfigId() != null) {
                    resp.setModelConfigId(config.getModelConfigId().toString());
                    ModelConfigEntity modelConfig = modelConfigMapper.selectById(config.getModelConfigId());
                    if (modelConfig != null) {
                        resp.setModelProvider(modelConfig.getProvider());
                        resp.setModelName(modelConfig.getModelName());
                    }
                }
            } else {
                resp.setEnabled(false);
                resp.setConfig(normalizeRuntimeConfig(null));
            }

            if (resp.getAgentVersionId() == null && latestVersion != null) {
                resp.setAgentVersionId(latestVersion.getId().toString());
                resp.setAgentVersionNo(latestVersion.getVersionNo());
            }

            return resp;
        }).collect(Collectors.toList());
    }

    private Long resolveVersionId(Long agentId, String agentVersionId) {
        if (agentVersionId != null && !agentVersionId.isBlank()) {
            return Long.valueOf(agentVersionId);
        }
        AiAgentVersionEntity latest = aiAgentVersionMapper.selectOne(
                new LambdaQueryWrapper<AiAgentVersionEntity>()
                        .eq(AiAgentVersionEntity::getAgentId, agentId)
                        .eq(AiAgentVersionEntity::getStatus, AgentVersionStatus.PUBLISHED.name())
                        .orderByDesc(AiAgentVersionEntity::getPublishTime)
                        .last("LIMIT 1"));
        if (latest == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "Agent 无 PUBLISHED 版本");
        }
        return latest.getId();
    }

    private void validateVersionBelongsToAgent(Long agentId, Long versionId) {
        AiAgentVersionEntity version = aiAgentVersionMapper.selectById(versionId);
        if (version == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "Agent 版本不存在");
        }
        if (!version.getAgentId().equals(agentId)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "版本不属于该 Agent");
        }
        if (!AgentVersionStatus.PUBLISHED.name().equals(version.getStatus())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "只能使用已发布的 Agent 版本");
        }
    }

    private void validateModelConfig(Long modelConfigId) {
        ModelConfigEntity modelConfig = modelConfigMapper.selectById(modelConfigId);
        if (modelConfig == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "模型配置不存在");
        }
        if (!"ENABLED".equals(modelConfig.getStatus())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "模型配置已停用，无法选择");
        }
    }

    private void requireAdmin() {
        LoginUser currentUser = LoginUserContext.currentUser()
                .orElseThrow(() -> new BizException(ErrorCode.UNAUTHORIZED));
        if (currentUser.getRoles() == null || !currentUser.getRoles().contains("ADMIN")) {
            throw new BizException(ErrorCode.FORBIDDEN, "需要平台管理员权限");
        }
    }

    private AgentResponse toAgentResponse(AiAgentEntity entity) {
        AgentResponse resp = new AgentResponse();
        resp.setId(entity.getId().toString());
        resp.setName(entity.getName());
        resp.setCode(entity.getCode());
        resp.setType(entity.getType());
        resp.setDescription(entity.getDescription());
        resp.setStatus(entity.getStatus());
        return resp;
    }

    private AgentVersionResponse toAgentVersionResponse(Long agentId, AiAgentVersionEntity version) {
        AgentVersionResponse resp = new AgentVersionResponse();
        resp.setId(version.getId().toString());
        resp.setAgentId(agentId.toString());
        resp.setVersionNo(version.getVersionNo());
        resp.setStatus(version.getStatus());
        resp.setSystemPrompt(version.getSystemPrompt());
        resp.setToolPolicy(version.getToolPolicy());
        resp.setExecutionPolicy(version.getExecutionPolicy());
        resp.setPublishTime(version.getPublishTime());
        resp.setCreateTime(version.getCreateTime());
        return resp;
    }

    private AgentDetailResponse toAgentDetailResponse(AiAgentEntity agent, AiAgentVersionEntity version) {
        AgentDetailResponse resp = new AgentDetailResponse();
        resp.setId(agent.getId().toString());
        resp.setName(agent.getName());
        resp.setCode(agent.getCode());
        resp.setType(agent.getType());
        resp.setDescription(agent.getDescription());
        resp.setStatus(agent.getStatus());
        resp.setAvatar(agent.getAvatar());

        if (version != null) {
            AgentDetailResponse.AgentVersionInfo info = new AgentDetailResponse.AgentVersionInfo();
            info.setId(version.getId().toString());
            info.setVersionNo(version.getVersionNo());
            info.setModelConfigId(version.getModelConfigId() != null ? version.getModelConfigId().toString() : null);
            info.setStatus(version.getStatus());
            resp.setLatestVersion(info);

            resp.setModelConfigId(version.getModelConfigId() != null ? version.getModelConfigId().toString() : null);
            resp.setSystemPrompt(version.getSystemPrompt());
            resp.setToolPolicy(version.getToolPolicy());
            resp.setExecutionPolicy(version.getExecutionPolicy());
        }
        return resp;
    }

    // ========================
    // Runtime Config Helpers
    // ========================

    ProjectAgentRuntimeConfig normalizeRuntimeConfig(ProjectAgentRuntimeConfig input) {
        ProjectAgentRuntimeConfig cfg = new ProjectAgentRuntimeConfig();
        if (input == null) {
            cfg.setTemperature(new BigDecimal("0.2"));
            cfg.setMaxTokens(4096);
            cfg.setTimeoutSeconds(60);
            cfg.setUseRag(false);
            cfg.setKnowledgeBaseId(null);
            cfg.setCustomInstruction("");
            return cfg;
        }
        cfg.setTemperature(input.getTemperature() != null ? input.getTemperature() : new BigDecimal("0.2"));
        cfg.setMaxTokens(Objects.requireNonNullElse(input.getMaxTokens(), DEFAULT_MAX_TOKENS));
        cfg.setTimeoutSeconds(Objects.requireNonNullElse(input.getTimeoutSeconds(), DEFAULT_TIMEOUT_SECONDS));
        cfg.setUseRag(Objects.requireNonNullElse(input.getUseRag(), Boolean.FALSE));
        cfg.setKnowledgeBaseId(input.getKnowledgeBaseId());
        cfg.setCustomInstruction(input.getCustomInstruction() != null ? input.getCustomInstruction() : "");
        return cfg;
    }

    void validateRuntimeConfig(Long projectId, ProjectAgentRuntimeConfig config) {
        if (config.getTemperature() != null) {
            BigDecimal t = config.getTemperature();
            if (t.compareTo(BigDecimal.ZERO) < 0 || t.compareTo(new BigDecimal("2.0")) > 0) {
                throw new BizException(ErrorCode.BAD_REQUEST, "Temperature 必须在 0.0 - 2.0 之间");
            }
        }
        if (config.getMaxTokens() != null) {
            int mt = config.getMaxTokens();
            if (mt < 256 || mt > 32768) {
                throw new BizException(ErrorCode.BAD_REQUEST, "MaxTokens 必须在 256 - 32768 之间");
            }
        }
        if (config.getTimeoutSeconds() != null) {
            int ts = config.getTimeoutSeconds();
            if (ts < 5 || ts > 600) {
                throw new BizException(ErrorCode.BAD_REQUEST, "TimeoutSeconds 必须在 5 - 600 之间");
            }
        }
        if (config.getCustomInstruction() != null && config.getCustomInstruction().length() > 2000) {
            throw new BizException(ErrorCode.BAD_REQUEST, "CustomInstruction 最长 2000 字符");
        }
        if (Boolean.TRUE.equals(config.getUseRag())
                && config.getKnowledgeBaseId() != null && !config.getKnowledgeBaseId().isBlank()) {
            KnowledgeBaseEntity kb = knowledgeBaseMapper.selectById(Long.valueOf(config.getKnowledgeBaseId()));
            if (kb == null) {
                throw new BizException(ErrorCode.BAD_REQUEST, "知识库不存在");
            }
            if (!kb.getProjectId().equals(projectId)) {
                throw new BizException(ErrorCode.BAD_REQUEST, "知识库不属于当前项目");
            }
        }
    }

    String serializeRuntimeConfig(ProjectAgentRuntimeConfig config) {
        try {
            return objectMapper.writeValueAsString(config);
        } catch (JsonProcessingException e) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "序列化运行配置失败");
        }
    }

    ProjectAgentRuntimeConfig deserializeRuntimeConfig(String configJson) {
        if (configJson == null || configJson.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(configJson, ProjectAgentRuntimeConfig.class);
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}
