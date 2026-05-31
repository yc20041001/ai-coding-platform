package com.aicoding.platform.orchestration.application;

import com.aicoding.platform.common.exception.BizException;
import com.aicoding.platform.common.exception.ErrorCode;
import com.aicoding.platform.orchestration.domain.*;
import com.aicoding.platform.orchestration.dto.*;
import com.aicoding.platform.orchestration.infrastructure.GovernanceRecommendationExecutionPlanMapper;
import com.aicoding.platform.orchestration.infrastructure.GovernanceHandoffChecklistMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class GovernanceExecutionPlanService {

    private final GovernanceRecommendationExecutionPlanMapper planMapper;
    private final GovernanceHandoffChecklistMapper handoffMapper;
    private final GovernancePlaybookTemplateService templateService;

    public GovernanceExecutionPlanService(GovernanceRecommendationExecutionPlanMapper planMapper,
                                           GovernanceHandoffChecklistMapper handoffMapper,
                                           GovernancePlaybookTemplateService templateService) {
        this.planMapper = planMapper;
        this.handoffMapper = handoffMapper;
        this.templateService = templateService;
    }

    @Transactional
    public GovernanceExecutionPlanResponse createPlan(String recommendationIdStr, String templateKey) {
        Long recId = parseLong(recommendationIdStr);

        String stepsJson = "[]";
        if (templateKey != null) {
            var templates = templateService.listTemplates().stream()
                    .filter(t -> templateKey.equals(t.getTemplateKey()))
                    .collect(Collectors.toList());
            if (!templates.isEmpty()) {
                stepsJson = templates.get(0).getTemplateStepsJson();
            }
        }
        if (stepsJson == null || "[]".equals(stepsJson) || "null".equals(stepsJson)) {
            stepsJson = "[{\"stepKey\":\"s1\",\"title\":\"确认问题\",\"status\":\"TODO\",\"required\":true},{\"stepKey\":\"s2\",\"title\":\"制定方案\",\"status\":\"TODO\",\"required\":true},{\"stepKey\":\"s3\",\"title\":\"执行修复\",\"status\":\"TODO\",\"required\":true}]";
        }

        GovernanceRecommendationExecutionPlanEntity entity = new GovernanceRecommendationExecutionPlanEntity();
        entity.setRecommendationId(recId);
        entity.setProjectId(0L);
        entity.setPlanStatus("DRAFT");
        entity.setTemplateKey(templateKey);
        entity.setStepsJson(stepsJson);
        entity.setCompletionRate(BigDecimal.ZERO);
        entity.setSummaryText("Execution plan for recommendation " + recId);
        planMapper.insert(entity);
        return toResponse(entity);
    }

    @Transactional(readOnly = true)
    public List<GovernanceExecutionPlanResponse> listPlans() {
        return planMapper.selectList(new LambdaQueryWrapper<GovernanceRecommendationExecutionPlanEntity>()
                .orderByDesc(GovernanceRecommendationExecutionPlanEntity::getCreateTime))
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public GovernanceExecutionPlanResponse getPlan(String planIdStr) {
        return toResponse(findPlan(planIdStr));
    }

    @Transactional
    public GovernanceExecutionPlanResponse updatePlan(String planIdStr, String ownerName, String dueAt, String summaryText) {
        GovernanceRecommendationExecutionPlanEntity entity = findPlan(planIdStr);
        if (ownerName != null) entity.setOwnerName(ownerName);
        if (dueAt != null) entity.setDueAt(LocalDateTime.parse(dueAt));
        if (summaryText != null) entity.setSummaryText(summaryText);
        entity.setUpdateTime(LocalDateTime.now());
        planMapper.updateById(entity);
        return toResponse(entity);
    }

    @Transactional
    public GovernanceExecutionPlanResponse updatePlanStatus(String planIdStr, String newStatus) {
        GovernanceRecommendationExecutionPlanEntity entity = findPlan(planIdStr);
        String current = entity.getPlanStatus();
        if (!isValidPlanTransition(current, newStatus)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "Invalid plan transition from " + current + " to " + newStatus);
        }
        entity.setPlanStatus(newStatus);
        entity.setUpdateTime(LocalDateTime.now());
        planMapper.updateById(entity);
        return toResponse(entity);
    }

    @Transactional
    public GovernanceExecutionPlanResponse updateStepStatus(String planIdStr, String stepKey, String newStatus) {
        GovernanceRecommendationExecutionPlanEntity entity = findPlan(planIdStr);
        String stepsJson = entity.getStepsJson();

        // Parse steps, update status, recalculate completion
        String updatedStepsJson = updateStepStatusInJson(stepsJson, stepKey, newStatus);
        entity.setStepsJson(updatedStepsJson);

        // Recalculate completion rate
        double rate = calculateCompletionRate(updatedStepsJson);
        entity.setCompletionRate(BigDecimal.valueOf(rate).setScale(2, RoundingMode.HALF_UP));

        // Auto-complete plan if all required steps done
        if (rate >= 100.0) {
            entity.setPlanStatus("COMPLETED");
        }

        entity.setUpdateTime(LocalDateTime.now());
        planMapper.updateById(entity);
        return toResponse(entity);
    }

    private String updateStepStatusInJson(String stepsJson, String stepKey, String newStatus) {
        // Simple JSON manipulation
        StringBuilder sb = new StringBuilder();
        String[] parts = stepsJson.split("\"stepKey\":\"");

        sb.append(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            String part = parts[i];
            String currentStepKey = part.substring(0, part.indexOf("\""));
            if (i > 1) sb.append("\"stepKey\":\"");
            sb.append("\"stepKey\":\"").append(currentStepKey).append("\"");

            // Replace status in this step
            String afterKey = part.substring(part.indexOf("\"") + 1);
            if (currentStepKey.equals(stepKey)) {
                afterKey = afterKey.replaceAll("\"status\":\"[^\"]*\"", "\"status\":\"" + newStatus + "\"");
            }
            sb.append(afterKey);
        }
        return sb.toString();
    }

    private double calculateCompletionRate(String stepsJson) {
        // Count DONE vs required steps
        int required = 0, done = 0;
        String[] stepParts = stepsJson.split("\\{");
        for (String part : stepParts) {
            if (part.contains("\"required\"")) {
                boolean isRequired = part.contains("\"required\":true") || part.contains("\"required\": true");
                if (isRequired) {
                    required++;
                    if (part.contains("\"status\":\"DONE\"")) done++;
                }
            }
        }
        if (required == 0) return 100.0;
        return (double) done / required * 100;
    }

    @Transactional(readOnly = true)
    public GovernanceExecutionDashboardResponse getDashboard() {
        List<GovernanceExecutionPlanResponse> plans = listPlans();
        int ready = 0, inProgress = 0, blocked = 0, completed = 0;
        BigDecimal totalRate = BigDecimal.ZERO;
        for (var p : plans) {
            switch (p.getPlanStatus()) {
                case "READY" -> ready++;
                case "IN_PROGRESS" -> inProgress++;
                case "BLOCKED" -> blocked++;
                case "COMPLETED" -> completed++;
            }
            if (p.getCompletionRate() != null) totalRate = totalRate.add(p.getCompletionRate());
        }

        List<GovernanceExecutionPlanResponse> blockedPlans = plans.stream()
                .filter(p -> "BLOCKED".equals(p.getPlanStatus())).limit(5).collect(Collectors.toList());
        List<GovernanceExecutionPlanResponse> nearDuePlans = plans.stream()
                .filter(p -> p.getDueAt() != null && p.getDueAt().isBefore(LocalDateTime.now().plusDays(3))
                        && !"COMPLETED".equals(p.getPlanStatus()) && !"ARCHIVED".equals(p.getPlanStatus()))
                .limit(5).collect(Collectors.toList());

        GovernanceExecutionDashboardResponse resp = new GovernanceExecutionDashboardResponse();
        resp.setTotalPlanCount(plans.size()); resp.setReadyPlanCount(ready);
        resp.setInProgressPlanCount(inProgress); resp.setBlockedPlanCount(blocked);
        resp.setCompletedPlanCount(completed);
        resp.setAverageCompletionRate(plans.isEmpty() ? BigDecimal.ZERO
                : totalRate.divide(BigDecimal.valueOf(plans.size()), 2, RoundingMode.HALF_UP));

        long openHandoffs = handoffMapper.selectCount(new LambdaQueryWrapper<GovernanceHandoffChecklistEntity>()
                .eq(GovernanceHandoffChecklistEntity::getChecklistStatus, "OPEN"));
        resp.setHandoffOpenCount((int) openHandoffs);
        resp.setTopBlockedPlans(blockedPlans);
        resp.setTopNearDuePlans(nearDuePlans);
        return resp;
    }

    @Transactional(readOnly = true)
    public String getReport() {
        List<GovernanceExecutionPlanResponse> plans = listPlans();
        StringBuilder md = new StringBuilder();
        md.append("# Recommendation Execution Summary\n\n");
        md.append("## Overview\n\n");
        md.append("- Total Plans: ").append(plans.size()).append("\n");
        long completed = plans.stream().filter(p -> "COMPLETED".equals(p.getPlanStatus())).count();
        long blocked = plans.stream().filter(p -> "BLOCKED".equals(p.getPlanStatus())).count();
        md.append("- Completed: ").append(completed).append("\n");
        md.append("- Blocked: ").append(blocked).append("\n\n");
        md.append("## Blocked Plans\n\n");
        for (var p : plans) {
            if ("BLOCKED".equals(p.getPlanStatus())) {
                md.append("- ").append(p.getSummaryText()).append(" (completion: ").append(p.getCompletionRate()).append("%)\n");
            }
        }
        return md.toString();
    }

    private boolean isValidPlanTransition(String current, String next) {
        Map<String, List<String>> transitions = new HashMap<>();
        transitions.put("DRAFT", List.of("READY"));
        transitions.put("READY", List.of("IN_PROGRESS", "ARCHIVED"));
        transitions.put("IN_PROGRESS", List.of("COMPLETED", "BLOCKED"));
        transitions.put("BLOCKED", List.of("IN_PROGRESS"));
        transitions.put("COMPLETED", List.of("ARCHIVED"));
        List<String> allowed = transitions.get(current);
        return allowed != null && allowed.contains(next);
    }

    private GovernanceRecommendationExecutionPlanEntity findPlan(String idStr) {
        Long id = parseLong(idStr);
        GovernanceRecommendationExecutionPlanEntity entity = planMapper.selectById(id);
        if (entity == null) throw new BizException(ErrorCode.NOT_FOUND, "Execution plan 不存在");
        return entity;
    }

    private GovernanceExecutionPlanResponse toResponse(GovernanceRecommendationExecutionPlanEntity e) {
        GovernanceExecutionPlanResponse r = new GovernanceExecutionPlanResponse();
        r.setId(e.getId() != null ? e.getId().toString() : null);
        r.setRecommendationId(e.getRecommendationId() != null ? e.getRecommendationId().toString() : null);
        r.setProjectId(e.getProjectId() != null ? e.getProjectId().toString() : null);
        r.setPlanStatus(e.getPlanStatus()); r.setTemplateKey(e.getTemplateKey());
        r.setOwnerId(e.getOwnerId() != null ? e.getOwnerId().toString() : null);
        r.setOwnerName(e.getOwnerName()); r.setDueAt(e.getDueAt());
        r.setStepsJson(e.getStepsJson()); r.setCompletionRate(e.getCompletionRate());
        r.setSummaryText(e.getSummaryText()); r.setCreateTime(e.getCreateTime()); r.setUpdateTime(e.getUpdateTime());
        return r;
    }

    private static Long parseLong(String v) {
        try { return Long.valueOf(v); }
        catch (NumberFormatException e) { throw new BizException(ErrorCode.BAD_REQUEST, "ID 格式无效"); }
    }
}
