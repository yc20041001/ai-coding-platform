package com.aicoding.platform.orchestration.application;

import com.aicoding.platform.common.exception.BizException;
import com.aicoding.platform.common.exception.ErrorCode;
import com.aicoding.platform.orchestration.domain.GovernanceGuidedTaskEntity;
import com.aicoding.platform.orchestration.domain.GovernanceRecommendationItemEntity;
import com.aicoding.platform.orchestration.dto.GovernanceGuidedTaskResponse;
import com.aicoding.platform.orchestration.infrastructure.GovernanceGuidedTaskMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class GovernanceGuidedOperationsService {

    private final GovernanceGuidedTaskMapper guidedTaskMapper;
    private final GovernanceRecommendationWorkflowService workflowService;

    public GovernanceGuidedOperationsService(GovernanceGuidedTaskMapper guidedTaskMapper,
                                              GovernanceRecommendationWorkflowService workflowService) {
        this.guidedTaskMapper = guidedTaskMapper;
        this.workflowService = workflowService;
    }

    @Transactional
    public List<GovernanceGuidedTaskResponse> refreshTasks(String sessionIdStr) {
        Long sessionId = parseLong(sessionIdStr);
        LambdaQueryWrapper<GovernanceGuidedTaskEntity> delW = new LambdaQueryWrapper<>();
        delW.eq(GovernanceGuidedTaskEntity::getSessionId, sessionId);
        guidedTaskMapper.delete(delW);

        List<GovernanceGuidedTaskEntity> tasks = new ArrayList<>();
        List<GovernanceRecommendationItemEntity> items = workflowService.getOpenItems();

        for (var item : items) {
            String taskType;
            String priority = item.getPriority() != null ? item.getPriority() : "P3";
            String title = item.getTitle();
            String summary = item.getSummary();

            boolean isOverdue = item.getDueAt() != null && item.getDueAt().isBefore(LocalDateTime.now());
            boolean isBlocked = "BLOCKED".equals(item.getWorkflowStatus());

            if (isBlocked) taskType = "TRIAGE_RECOMMENDATION";
            else if (isOverdue) taskType = "RUN_PLAYBOOK";
            else taskType = "APPLY_RECIPE_GUIDANCE";

            GovernanceGuidedTaskEntity task = new GovernanceGuidedTaskEntity();
            task.setSessionId(sessionId);
            task.setRecommendationId(item.getId());
            task.setTaskType(taskType);
            task.setPriority(priority);
            task.setTaskStatus("OPEN");
            task.setTitle(title);
            task.setSummary(summary != null ? summary : title);
            task.setSourceType("RECOMMENDATION");
            task.setSourceId(item.getId());
            task.setDueAt(item.getDueAt());
            // Link playbook/recipe
            if ("BLOCKED".equals(item.getWorkflowStatus())) {
                task.setLinkedPlaybookKey("blocked-resolution");
            } else if (item.getGuardrailKey() != null) {
                task.setLinkedRecipeKey(item.getGuardrailKey());
            }
            tasks.add(task);
        }

        // Sort by priority then overdue
        tasks.sort((a, b) -> {
            int pa = priorityWeight(a.getPriority());
            int pb = priorityWeight(b.getPriority());
            if (pa != pb) return Integer.compare(pa, pb);
            boolean oa = a.getDueAt() != null && a.getDueAt().isBefore(LocalDateTime.now());
            boolean ob = b.getDueAt() != null && b.getDueAt().isBefore(LocalDateTime.now());
            return Boolean.compare(ob, oa);
        });

        for (var task : tasks.stream().limit(20).collect(Collectors.toList())) {
            guidedTaskMapper.insert(task);
        }

        return getTasks(sessionIdStr);
    }

    @Transactional(readOnly = true)
    public List<GovernanceGuidedTaskResponse> getTasks(String sessionIdStr) {
        Long sessionId = parseLong(sessionIdStr);
        LambdaQueryWrapper<GovernanceGuidedTaskEntity> w = new LambdaQueryWrapper<>();
        w.eq(GovernanceGuidedTaskEntity::getSessionId, sessionId);
        w.orderByDesc(GovernanceGuidedTaskEntity::getPriority);
        return guidedTaskMapper.selectList(w).stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public GovernanceGuidedTaskResponse updateTaskStatus(String taskIdStr, String newStatus) {
        GovernanceGuidedTaskEntity entity = findEntity(taskIdStr);
        String current = entity.getTaskStatus();
        if (!isValidTransition(current, newStatus)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "Invalid task transition from " + current + " to " + newStatus);
        }
        entity.setTaskStatus(newStatus);
        entity.setUpdateTime(LocalDateTime.now());
        guidedTaskMapper.updateById(entity);
        return toResponse(entity);
    }

    private boolean isValidTransition(String current, String next) {
        if ("OPEN".equals(current) && ("IN_PROGRESS".equals(next) || "SKIPPED".equals(next) || "BLOCKED".equals(next))) return true;
        if ("IN_PROGRESS".equals(current) && ("DONE".equals(next) || "BLOCKED".equals(next))) return true;
        if ("BLOCKED".equals(current) && "IN_PROGRESS".equals(next)) return true;
        return false;
    }

    private int priorityWeight(String p) {
        if ("P0".equals(p)) return 0; if ("P1".equals(p)) return 1;
        if ("P2".equals(p)) return 2; return 3;
    }

    private GovernanceGuidedTaskEntity findEntity(String idStr) {
        Long id = parseLong(idStr);
        GovernanceGuidedTaskEntity entity = guidedTaskMapper.selectById(id);
        if (entity == null) throw new BizException(ErrorCode.NOT_FOUND, "Guided task 不存在");
        return entity;
    }

    private GovernanceGuidedTaskResponse toResponse(GovernanceGuidedTaskEntity e) {
        GovernanceGuidedTaskResponse r = new GovernanceGuidedTaskResponse();
        r.setId(e.getId() != null ? e.getId().toString() : null);
        r.setSessionId(e.getSessionId() != null ? e.getSessionId().toString() : null);
        r.setRecommendationId(e.getRecommendationId() != null ? e.getRecommendationId().toString() : null);
        r.setTaskType(e.getTaskType()); r.setPriority(e.getPriority()); r.setTaskStatus(e.getTaskStatus());
        r.setTitle(e.getTitle()); r.setSummary(e.getSummary()); r.setSourceType(e.getSourceType());
        r.setSourceId(e.getSourceId() != null ? e.getSourceId().toString() : null);
        r.setLinkedPlaybookKey(e.getLinkedPlaybookKey()); r.setLinkedRecipeKey(e.getLinkedRecipeKey());
        r.setLinkedKnowledgeEntryId(e.getLinkedKnowledgeEntryId() != null ? e.getLinkedKnowledgeEntryId().toString() : null);
        r.setDueAt(e.getDueAt()); r.setCreateTime(e.getCreateTime()); r.setUpdateTime(e.getUpdateTime());
        return r;
    }

    private static Long parseLong(String v) { try { return Long.parseLong(v); } catch (NumberFormatException e) { throw new BizException(ErrorCode.BAD_REQUEST, "ID 格式无效"); } }
}
