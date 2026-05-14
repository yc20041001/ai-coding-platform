package com.aicoding.platform.agent.application;

import com.aicoding.platform.agent.domain.AgentStatus;
import com.aicoding.platform.agent.domain.AgentVersionStatus;
import com.aicoding.platform.agent.domain.AiAgentEntity;
import com.aicoding.platform.agent.domain.AiAgentVersionEntity;
import com.aicoding.platform.agent.domain.ProjectAgentConfigEntity;
import com.aicoding.platform.agent.dto.AgentDetailResponse;
import com.aicoding.platform.agent.dto.AgentResponse;
import com.aicoding.platform.agent.dto.CreateAgentRequest;
import com.aicoding.platform.agent.dto.EnableProjectAgentRequest;
import com.aicoding.platform.agent.dto.UpdateAgentRequest;
import com.aicoding.platform.agent.infrastructure.AiAgentMapper;
import com.aicoding.platform.agent.infrastructure.AiAgentVersionMapper;
import com.aicoding.platform.agent.infrastructure.ProjectAgentConfigMapper;
import com.aicoding.platform.common.exception.BizException;
import com.aicoding.platform.common.exception.ErrorCode;
import com.aicoding.platform.member.application.ProjectPermissionService;
import com.aicoding.platform.member.domain.ProjectRole;
import com.aicoding.platform.security.context.LoginUser;
import com.aicoding.platform.security.context.LoginUserContext;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AgentApplicationService {

    private final AiAgentMapper aiAgentMapper;
    private final AiAgentVersionMapper aiAgentVersionMapper;
    private final ProjectAgentConfigMapper projectAgentConfigMapper;
    private final ProjectPermissionService projectPermissionService;

    public AgentApplicationService(AiAgentMapper aiAgentMapper,
                                    AiAgentVersionMapper aiAgentVersionMapper,
                                    ProjectAgentConfigMapper projectAgentConfigMapper,
                                    ProjectPermissionService projectPermissionService) {
        this.aiAgentMapper = aiAgentMapper;
        this.aiAgentVersionMapper = aiAgentVersionMapper;
        this.projectAgentConfigMapper = projectAgentConfigMapper;
        this.projectPermissionService = projectPermissionService;
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

        Long versionId = resolveVersionId(agentId, request.getAgentVersionId());

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
            if (request.getConfigJson() != null) {
                existing.setConfigJson(request.getConfigJson());
            }
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
            if (request.getConfigJson() != null) {
                config.setConfigJson(request.getConfigJson());
            }
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
            resp.setToolPolicy(version.getToolPolicy());
            resp.setExecutionPolicy(version.getExecutionPolicy());
        }
        return resp;
    }
}
