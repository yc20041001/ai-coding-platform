package com.aicoding.platform.orchestration.application;

import com.aicoding.platform.common.exception.BizException;
import com.aicoding.platform.common.exception.ErrorCode;
import com.aicoding.platform.orchestration.domain.WorkflowTemplateEntity;
import com.aicoding.platform.orchestration.dto.WorkflowPhaseTemplateResponse;
import com.aicoding.platform.orchestration.dto.WorkflowStepTemplateResponse;
import com.aicoding.platform.orchestration.dto.WorkflowStrategyResponse;
import com.aicoding.platform.orchestration.dto.WorkflowTemplateResponse;
import com.aicoding.platform.orchestration.infrastructure.WorkflowTemplateMapper;
import com.aicoding.platform.security.context.LoginUser;
import com.aicoding.platform.security.context.LoginUserContext;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class WorkflowTemplateApplicationService {

    private final WorkflowTemplateMapper workflowTemplateMapper;
    private final ObjectMapper objectMapper;

    public WorkflowTemplateApplicationService(WorkflowTemplateMapper workflowTemplateMapper,
                                               ObjectMapper objectMapper) {
        this.workflowTemplateMapper = workflowTemplateMapper;
        this.objectMapper = objectMapper;
    }

    public List<WorkflowTemplateResponse> listTemplates(String status) {
        requireAdmin();

        LambdaQueryWrapper<WorkflowTemplateEntity> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isBlank()) {
            wrapper.eq(WorkflowTemplateEntity::getStatus, status.trim().toUpperCase());
        }
        wrapper.orderByAsc(WorkflowTemplateEntity::getTemplateKey);

        List<WorkflowTemplateEntity> entities = workflowTemplateMapper.selectList(wrapper);
        List<WorkflowTemplateResponse> result = new ArrayList<>();
        for (WorkflowTemplateEntity entity : entities) {
            result.add(toResponse(entity));
        }
        return result;
    }

    public WorkflowTemplateResponse getTemplate(Long templateId) {
        requireAdmin();

        WorkflowTemplateEntity entity = workflowTemplateMapper.selectById(templateId);
        if (entity == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "模板不存在: " + templateId);
        }
        return toResponse(entity);
    }

    public WorkflowTemplateResponse updateStatus(Long templateId, String status) {
        requireAdmin();

        String upper = status != null ? status.trim().toUpperCase() : null;
        if (!"ENABLED".equals(upper) && !"DISABLED".equals(upper)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "无效的状态值: " + status + "，仅支持 ENABLED / DISABLED");
        }

        WorkflowTemplateEntity entity = workflowTemplateMapper.selectById(templateId);
        if (entity == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "模板不存在: " + templateId);
        }

        entity.setStatus(upper);
        workflowTemplateMapper.updateById(entity);

        return toResponse(entity);
    }

    // ========================
    // JSON parsing
    // ========================

    public WorkflowStrategyResponse parseTemplateJson(WorkflowTemplateEntity entity) {
        if (entity.getTemplateJson() == null || entity.getTemplateJson().isBlank()) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "模板JSON为空: " + entity.getTemplateKey());
        }

        try {
            JsonNode root = objectMapper.readTree(entity.getTemplateJson());
            WorkflowStrategyResponse resp = new WorkflowStrategyResponse();

            resp.setStrategyKey(pathText(root, "strategyKey", entity.getTemplateKey()));
            resp.setName(entity.getName());
            resp.setDescription(entity.getDescription());

            List<WorkflowPhaseTemplateResponse> phases = new ArrayList<>();
            JsonNode phasesNode = root.get("phases");
            int stepCount = 0;
            if (phasesNode != null && phasesNode.isArray()) {
                for (JsonNode phaseNode : phasesNode) {
                    WorkflowPhaseTemplateResponse pr = new WorkflowPhaseTemplateResponse();
                    pr.setPhaseOrder(phaseNode.get("phaseOrder").asInt());
                    pr.setPhaseKey(pathText(phaseNode, "phaseKey", ""));
                    pr.setTitle(pathText(phaseNode, "title", ""));

                    List<WorkflowStepTemplateResponse> steps = new ArrayList<>();
                    JsonNode stepsNode = phaseNode.get("steps");
                    if (stepsNode != null && stepsNode.isArray()) {
                        int stepOrder = 1;
                        for (JsonNode stepNode : stepsNode) {
                            WorkflowStepTemplateResponse sr = new WorkflowStepTemplateResponse();
                            sr.setStepOrder(stepOrder++);
                            sr.setStepType(pathText(stepNode, "stepType", ""));
                            sr.setAgentCode(pathText(stepNode, "agentCode", ""));
                            sr.setLaneKey(pathText(stepNode, "laneKey", ""));
                            sr.setTitle(pathText(stepNode, "title", ""));
                            steps.add(sr);
                        }
                    }
                    pr.setSteps(steps);
                    phases.add(pr);
                    stepCount += steps.size();
                }
            }
            resp.setPhases(phases);
            resp.setPhaseCount(phases.size());
            resp.setStepCount(stepCount);

            return resp;
        } catch (JsonProcessingException e) {
            throw new BizException(ErrorCode.INTERNAL_ERROR,
                    "模板JSON解析失败 (" + entity.getTemplateKey() + "): " + e.getMessage());
        }
    }

    // ========================
    // Private helpers
    // ========================

    private WorkflowTemplateResponse toResponse(WorkflowTemplateEntity entity) {
        WorkflowTemplateResponse resp = new WorkflowTemplateResponse();
        resp.setId(String.valueOf(entity.getId()));
        resp.setTemplateKey(entity.getTemplateKey());
        resp.setName(entity.getName());
        resp.setDescription(entity.getDescription());
        resp.setCategory(entity.getCategory());
        resp.setStatus(entity.getStatus());
        resp.setBuiltIn(entity.getBuiltIn() != null && entity.getBuiltIn() == 1);
        resp.setTemplateJson(entity.getTemplateJson());
        resp.setCreateTime(entity.getCreateTime());
        resp.setUpdateTime(entity.getUpdateTime());

        // Parse template JSON into strategy details
        try {
            WorkflowStrategyResponse strategy = parseTemplateJson(entity);
            resp.setStrategy(strategy);
            resp.setPhaseCount(strategy.getPhaseCount());
            resp.setStepCount(strategy.getStepCount());
        } catch (Exception e) {
            // If parsing fails, set counts to 0, strategy remains null
            resp.setPhaseCount(0);
            resp.setStepCount(0);
        }

        return resp;
    }

    private String pathText(JsonNode node, String field, String defaultValue) {
        JsonNode child = node.get(field);
        if (child == null || child.isNull()) {
            return defaultValue;
        }
        return child.asText();
    }

    private void requireAdmin() {
        LoginUser currentUser = LoginUserContext.currentUser().orElse(null);
        if (currentUser == null || currentUser.getRoles() == null || !currentUser.getRoles().contains("ADMIN")) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }
    }
}
