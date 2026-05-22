package com.aicoding.platform.orchestration.application;

import com.aicoding.platform.audit.application.AuditLogApplicationService;
import com.aicoding.platform.audit.domain.AuditActionType;
import com.aicoding.platform.common.exception.BizException;
import com.aicoding.platform.common.exception.ErrorCode;
import com.aicoding.platform.member.application.ProjectPermissionService;
import com.aicoding.platform.member.domain.ProjectRole;
import com.aicoding.platform.orchestration.domain.ProjectToolConfigEntity;
import com.aicoding.platform.orchestration.domain.ToolCatalogEntity;
import com.aicoding.platform.orchestration.domain.ToolRiskLevel;
import com.aicoding.platform.orchestration.dto.ProjectToolConfigResponse;
import com.aicoding.platform.orchestration.dto.ToolCatalogResponse;
import com.aicoding.platform.orchestration.dto.UpdateProjectToolConfigRequest;
import com.aicoding.platform.orchestration.infrastructure.ProjectToolConfigMapper;
import com.aicoding.platform.orchestration.infrastructure.ToolCatalogMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ToolCatalogApplicationService {

    private final ToolCatalogMapper toolCatalogMapper;
    private final ProjectToolConfigMapper projectToolConfigMapper;
    private final ProjectPermissionService projectPermissionService;
    private final ToolParameterSchemaService toolParameterSchemaService;
    private final AuditLogApplicationService auditLogApplicationService;

    public ToolCatalogApplicationService(ToolCatalogMapper toolCatalogMapper,
                                          ProjectToolConfigMapper projectToolConfigMapper,
                                          ProjectPermissionService projectPermissionService,
                                          ToolParameterSchemaService toolParameterSchemaService,
                                          AuditLogApplicationService auditLogApplicationService) {
        this.toolCatalogMapper = toolCatalogMapper;
        this.projectToolConfigMapper = projectToolConfigMapper;
        this.projectPermissionService = projectPermissionService;
        this.toolParameterSchemaService = toolParameterSchemaService;
        this.auditLogApplicationService = auditLogApplicationService;
    }

    @Transactional(readOnly = true)
    public List<ToolCatalogResponse> listTools(String toolType, String riskLevel, Boolean enabled) {
        LambdaQueryWrapper<ToolCatalogEntity> qw = new LambdaQueryWrapper<>();
        if (toolType != null && !toolType.isBlank()) {
            qw.eq(ToolCatalogEntity::getToolType, toolType);
        }
        if (riskLevel != null && !riskLevel.isBlank()) {
            qw.eq(ToolCatalogEntity::getRiskLevel, riskLevel);
        }
        if (enabled != null) {
            qw.eq(ToolCatalogEntity::getEnabled, enabled ? 1 : 0);
        }
        qw.orderByAsc(ToolCatalogEntity::getId);
        return toolCatalogMapper.selectList(qw).stream()
                .map(this::toCatalogResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProjectToolConfigResponse> listProjectTools(Long projectId) {
        projectPermissionService.checkProjectMember(projectId);

        List<ToolCatalogEntity> allTools = toolCatalogMapper.selectList(
                new LambdaQueryWrapper<ToolCatalogEntity>()
                        .eq(ToolCatalogEntity::getEnabled, 1)
                        .orderByAsc(ToolCatalogEntity::getId));

        List<ProjectToolConfigEntity> configs = projectToolConfigMapper.selectList(
                new LambdaQueryWrapper<ProjectToolConfigEntity>()
                        .eq(ProjectToolConfigEntity::getProjectId, projectId));

        List<ProjectToolConfigResponse> result = new ArrayList<>();
        for (ToolCatalogEntity tool : allTools) {
            ProjectToolConfigEntity cfg = configs.stream()
                    .filter(c -> c.getToolId().equals(tool.getId()))
                    .findFirst().orElse(null);

            ProjectToolConfigResponse resp = new ProjectToolConfigResponse();
            resp.setId(cfg != null ? cfg.getId().toString() : null);
            resp.setProjectId(projectId.toString());
            resp.setToolId(tool.getId().toString());
            resp.setToolKey(tool.getToolKey());
            resp.setName(tool.getName());
            resp.setDescription(tool.getDescription());
            resp.setToolType(tool.getToolType());
            resp.setRiskLevel(tool.getRiskLevel());
            resp.setExecutionMode(tool.getExecutionMode());
            resp.setGlobalEnabled(tool.getEnabled() != null && tool.getEnabled() == 1);
            resp.setConfigJson(cfg != null ? cfg.getConfigJson() : null);
            resp.setParameterSchemaJson(tool.getParameterSchemaJson());
            resp.setParametersJson(cfg != null ? cfg.getParametersJson()
                    : serializeDefaultParameters(tool.getParameterSchemaJson()));

            if (cfg != null) {
                resp.setProjectEnabled(cfg.getEnabled() != null && cfg.getEnabled() == 1);
            } else {
                // Default: LOW -> enabled, MEDIUM -> disabled
                resp.setProjectEnabled(
                        ToolRiskLevel.LOW.name().equals(tool.getRiskLevel()));
            }

            resp.setCreateTime(cfg != null && cfg.getCreateTime() != null
                    ? cfg.getCreateTime().toString() : null);
            resp.setUpdateTime(cfg != null && cfg.getUpdateTime() != null
                    ? cfg.getUpdateTime().toString() : null);

            result.add(resp);
        }
        return result;
    }

    @Transactional
    public ProjectToolConfigResponse enableProjectTool(Long projectId, Long toolId,
                                                        UpdateProjectToolConfigRequest request) {
        projectPermissionService.checkProjectRole(projectId, ProjectRole.OWNER);

        ToolCatalogEntity tool = toolCatalogMapper.selectById(toolId);
        if (tool == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "工具不存在");
        }

        ProjectToolConfigEntity cfg = projectToolConfigMapper.selectOne(
                new LambdaQueryWrapper<ProjectToolConfigEntity>()
                        .eq(ProjectToolConfigEntity::getProjectId, projectId)
                        .eq(ProjectToolConfigEntity::getToolId, toolId));

        if (cfg == null) {
            cfg = new ProjectToolConfigEntity();
            cfg.setProjectId(projectId);
            cfg.setToolId(toolId);
            cfg.setEnabled(1);
            cfg.setConfigJson(null);
            projectToolConfigMapper.insert(cfg);
        } else {
            cfg.setEnabled(1);
            if (request != null && request.getConfig() != null && !request.getConfig().isEmpty()) {
                try {
                    cfg.setConfigJson(
                            new ObjectMapper().writeValueAsString(request.getConfig()));
                } catch (JsonProcessingException e) {
                    throw new BizException(ErrorCode.BAD_REQUEST, "config JSON 格式错误");
                }
            }
        }

        // Validate schema structure
        toolParameterSchemaService.validateSchema(tool.getParameterSchemaJson());

        // Store old parameters for audit comparison
        String oldParamsJson = cfg.getParametersJson();

        // Validate and save parameters
        if (request != null && request.getParameters() != null) {
            Map<String, Object> normalized = toolParameterSchemaService.normalizeAndValidate(
                    tool.getParameterSchemaJson(), request.getParameters());
            try {
                cfg.setParametersJson(new ObjectMapper().writeValueAsString(normalized));
            } catch (JsonProcessingException e) {
                throw new BizException(ErrorCode.BAD_REQUEST, "parameters JSON 格式错误");
            }
        } else {
            // Save default parameters
            Map<String, Object> defaults = toolParameterSchemaService.getDefaultParameters(
                    tool.getParameterSchemaJson());
            if (!defaults.isEmpty()) {
                try {
                    cfg.setParametersJson(new ObjectMapper().writeValueAsString(defaults));
                } catch (JsonProcessingException e) {
                    throw new BizException(ErrorCode.BAD_REQUEST, "parameters JSON 格式错误");
                }
            }
        }

        projectToolConfigMapper.updateById(cfg);

        // Audit log for parameter changes
        String newParamsJson = cfg.getParametersJson();
        if ((oldParamsJson == null && newParamsJson != null)
                || (oldParamsJson != null && !oldParamsJson.equals(newParamsJson))) {
            auditLogApplicationService.recordSuccess(
                    projectId, cfg.getId(), AuditActionType.TOOL_PARAMETER_UPDATE.name(),
                    "TOOL_CONFIG", "项目工具参数已更新：" + tool.getName()
            );
        }

        return buildProjectToolResponse(projectId, tool, cfg);
    }

    @Transactional
    public ProjectToolConfigResponse disableProjectTool(Long projectId, Long toolId) {
        projectPermissionService.checkProjectRole(projectId, ProjectRole.OWNER);

        ToolCatalogEntity tool = toolCatalogMapper.selectById(toolId);
        if (tool == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "工具不存在");
        }

        ProjectToolConfigEntity cfg = projectToolConfigMapper.selectOne(
                new LambdaQueryWrapper<ProjectToolConfigEntity>()
                        .eq(ProjectToolConfigEntity::getProjectId, projectId)
                        .eq(ProjectToolConfigEntity::getToolId, toolId));

        if (cfg == null) {
            cfg = new ProjectToolConfigEntity();
            cfg.setProjectId(projectId);
            cfg.setToolId(toolId);
            cfg.setEnabled(0);
            projectToolConfigMapper.insert(cfg);
        } else {
            cfg.setEnabled(0);
            projectToolConfigMapper.updateById(cfg);
        }

        return buildProjectToolResponse(projectId, tool, cfg);
    }

    // ========================
    // Response builders
    // ========================

    private ToolCatalogResponse toCatalogResponse(ToolCatalogEntity entity) {
        ToolCatalogResponse resp = new ToolCatalogResponse();
        resp.setId(entity.getId().toString());
        resp.setToolKey(entity.getToolKey());
        resp.setName(entity.getName());
        resp.setDescription(entity.getDescription());
        resp.setToolType(entity.getToolType());
        resp.setRiskLevel(entity.getRiskLevel());
        resp.setExecutionMode(entity.getExecutionMode());
        resp.setEnabled(entity.getEnabled() != null && entity.getEnabled() == 1);
        resp.setBuiltIn(entity.getBuiltIn() != null && entity.getBuiltIn() == 1);
        resp.setPolicyJson(entity.getPolicyJson());
        resp.setParameterSchemaJson(entity.getParameterSchemaJson());
        resp.setCreateTime(entity.getCreateTime() != null ? entity.getCreateTime().toString() : null);
        resp.setUpdateTime(entity.getUpdateTime() != null ? entity.getUpdateTime().toString() : null);
        return resp;
    }

    private ProjectToolConfigResponse buildProjectToolResponse(Long projectId,
                                                                ToolCatalogEntity tool,
                                                                ProjectToolConfigEntity cfg) {
        ProjectToolConfigResponse resp = new ProjectToolConfigResponse();
        resp.setId(cfg.getId() != null ? cfg.getId().toString() : null);
        resp.setProjectId(projectId.toString());
        resp.setToolId(tool.getId().toString());
        resp.setToolKey(tool.getToolKey());
        resp.setName(tool.getName());
        resp.setDescription(tool.getDescription());
        resp.setToolType(tool.getToolType());
        resp.setRiskLevel(tool.getRiskLevel());
        resp.setExecutionMode(tool.getExecutionMode());
        resp.setGlobalEnabled(tool.getEnabled() != null && tool.getEnabled() == 1);
        resp.setProjectEnabled(cfg.getEnabled() != null && cfg.getEnabled() == 1);
        resp.setConfigJson(cfg.getConfigJson());
        resp.setParameterSchemaJson(tool.getParameterSchemaJson());
        resp.setParametersJson(cfg.getParametersJson());
        resp.setCreateTime(cfg.getCreateTime() != null ? cfg.getCreateTime().toString() : null);
        resp.setUpdateTime(cfg.getUpdateTime() != null ? cfg.getUpdateTime().toString() : null);
        return resp;
    }

    private String serializeDefaultParameters(String schemaJson) {
        if (schemaJson == null || schemaJson.isBlank()) return null;
        try {
            Map<String, Object> defaults = toolParameterSchemaService.getDefaultParameters(schemaJson);
            if (defaults.isEmpty()) return null;
            return new ObjectMapper().writeValueAsString(defaults);
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}
