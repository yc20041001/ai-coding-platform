package com.aicoding.platform.orchestration.application;

import com.aicoding.platform.common.exception.BizException;
import com.aicoding.platform.common.exception.ErrorCode;
import com.aicoding.platform.orchestration.domain.GovernanceRecommendationItemEntity;
import com.aicoding.platform.orchestration.domain.GovernanceWaiverRequestEntity;
import com.aicoding.platform.orchestration.dto.GovernanceRecommendationItemResponse;
import com.aicoding.platform.orchestration.dto.UpdateGovernanceRecommendationItemRequest;
import com.aicoding.platform.orchestration.infrastructure.GovernanceRecommendationItemMapper;
import com.aicoding.platform.orchestration.infrastructure.GovernanceWaiverRequestMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class GovernanceRecommendationWorkflowService {

    private final GovernanceRecommendationItemMapper governanceRecommendationItemMapper;
    private final GovernanceWaiverRequestMapper governanceWaiverRequestMapper;
    private final ReleaseGuardrailAutomationService releaseGuardrailAutomationService;

    public GovernanceRecommendationWorkflowService(GovernanceRecommendationItemMapper governanceRecommendationItemMapper,
                                                    GovernanceWaiverRequestMapper governanceWaiverRequestMapper,
                                                    ReleaseGuardrailAutomationService releaseGuardrailAutomationService) {
        this.governanceRecommendationItemMapper = governanceRecommendationItemMapper;
        this.governanceWaiverRequestMapper = governanceWaiverRequestMapper;
        this.releaseGuardrailAutomationService = releaseGuardrailAutomationService;
    }

    @Transactional
    public int syncRecommendations() {
        // Get recommendations from 40B guardrail service
        var recommendations = releaseGuardrailAutomationService.getRecommendations();

        int synced = 0;
        LocalDate today = LocalDate.now();

        for (var rec : recommendations) {
            // Only sync HIGH/CRITICAL severity -> P0/P1 priority
            String priority = rec.getPriority();
            if (!"P0".equals(priority) && !"P1".equals(priority)) continue;

            // Check for existing item by unique key
            Long projectId = parseLong(rec.getProjectId(), "projectId");
            LambdaQueryWrapper<GovernanceRecommendationItemEntity> dupCheck = new LambdaQueryWrapper<>();
            dupCheck.eq(GovernanceRecommendationItemEntity::getProjectId, projectId);
            dupCheck.eq(GovernanceRecommendationItemEntity::getSourceSnapshotDate, today);
            dupCheck.eq(GovernanceRecommendationItemEntity::getPolicyKey, rec.getPolicyKey());
            dupCheck.eq(GovernanceRecommendationItemEntity::getGuardrailKey, rec.getGuardrailKey());

            if (governanceRecommendationItemMapper.selectCount(dupCheck) > 0) continue;

            GovernanceRecommendationItemEntity item = new GovernanceRecommendationItemEntity();
            item.setProjectId(projectId);
            item.setProjectName(rec.getProjectName());
            item.setSourceSnapshotDate(today);
            item.setPolicyKey(rec.getPolicyKey());
            item.setGuardrailKey(rec.getGuardrailKey());
            item.setCategory(rec.getCategory());
            item.setPriority(priority);
            item.setWorkflowStatus("OPEN");
            item.setTitle(rec.getTitle());
            item.setSummary(rec.getSummary());

            // Set due date to 7 days from now
            item.setDueAt(LocalDateTime.now().plusDays(7));

            governanceRecommendationItemMapper.insert(item);
            synced++;
        }

        return synced;
    }

    @Transactional(readOnly = true)
    public List<GovernanceRecommendationItemResponse> listItems(String status, String priority) {
        LambdaQueryWrapper<GovernanceRecommendationItemEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(GovernanceRecommendationItemEntity::getCreateTime);
        if (status != null && !status.isEmpty()) {
            wrapper.eq(GovernanceRecommendationItemEntity::getWorkflowStatus, status);
        }
        if (priority != null && !priority.isEmpty()) {
            wrapper.eq(GovernanceRecommendationItemEntity::getPriority, priority);
        }
        List<GovernanceRecommendationItemEntity> items = governanceRecommendationItemMapper.selectList(wrapper);
        return items.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public GovernanceRecommendationItemResponse getItem(String itemIdStr) {
        Long itemId = parseLong(itemIdStr, "itemId");
        GovernanceRecommendationItemEntity entity = governanceRecommendationItemMapper.selectById(itemId);
        if (entity == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "Recommendation item 不存在");
        }
        return toResponse(entity);
    }

    @Transactional
    public GovernanceRecommendationItemResponse updateItem(String itemIdStr, UpdateGovernanceRecommendationItemRequest request) {
        Long itemId = parseLong(itemIdStr, "itemId");
        GovernanceRecommendationItemEntity entity = governanceRecommendationItemMapper.selectById(itemId);
        if (entity == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "Recommendation item 不存在");
        }

        if (request.getTitle() != null) entity.setTitle(request.getTitle());
        if (request.getSummary() != null) entity.setSummary(request.getSummary());
        if (request.getPriority() != null) entity.setPriority(request.getPriority());
        if (request.getOwnerId() != null) entity.setOwnerId(parseLong(request.getOwnerId(), "ownerId"));
        if (request.getOwnerName() != null) entity.setOwnerName(request.getOwnerName());
        if (request.getDueAt() != null) entity.setDueAt(LocalDateTime.parse(request.getDueAt(), DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        if (request.getResolutionNote() != null) entity.setResolutionNote(request.getResolutionNote());
        entity.setUpdateTime(LocalDateTime.now());

        governanceRecommendationItemMapper.updateById(entity);
        return toResponse(entity);
    }

    @Transactional
    public GovernanceRecommendationItemResponse updateItemStatus(String itemIdStr, String newStatus) {
        Long itemId = parseLong(itemIdStr, "itemId");
        GovernanceRecommendationItemEntity entity = governanceRecommendationItemMapper.selectById(itemId);
        if (entity == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "Recommendation item 不存在");
        }

        String currentStatus = entity.getWorkflowStatus();
        if (!isValidTransition(currentStatus, newStatus)) {
            throw new BizException(ErrorCode.BAD_REQUEST,
                    "Invalid status transition from " + currentStatus + " to " + newStatus);
        }

        entity.setWorkflowStatus(newStatus);
        if ("COMPLETED".equals(newStatus) || "REJECTED".equals(newStatus)) {
            entity.setResolvedAt(LocalDateTime.now());
        }
        entity.setUpdateTime(LocalDateTime.now());

        governanceRecommendationItemMapper.updateById(entity);
        return toResponse(entity);
    }

    @Transactional(readOnly = true)
    public List<GovernanceRecommendationItemEntity> getOpenItems() {
        LambdaQueryWrapper<GovernanceRecommendationItemEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(GovernanceRecommendationItemEntity::getWorkflowStatus, "OPEN", "ACKNOWLEDGED", "IN_PROGRESS", "BLOCKED");
        return governanceRecommendationItemMapper.selectList(wrapper);
    }

    @Transactional(readOnly = true)
    public List<GovernanceRecommendationItemEntity> getAllItems() {
        return governanceRecommendationItemMapper.selectList(new LambdaQueryWrapper<>());
    }

    private boolean isValidTransition(String current, String next) {
        Map<String, List<String>> validTransitions = new HashMap<>();
        validTransitions.put("OPEN", List.of("ACKNOWLEDGED", "REJECTED"));
        validTransitions.put("ACKNOWLEDGED", List.of("IN_PROGRESS", "BLOCKED", "REJECTED"));
        validTransitions.put("IN_PROGRESS", List.of("COMPLETED", "BLOCKED"));
        validTransitions.put("BLOCKED", List.of("IN_PROGRESS"));

        List<String> allowed = validTransitions.get(current);
        return allowed != null && allowed.contains(next);
    }

    public GovernanceRecommendationItemResponse toResponse(GovernanceRecommendationItemEntity entity) {
        GovernanceRecommendationItemResponse resp = new GovernanceRecommendationItemResponse();
        resp.setId(entity.getId() != null ? entity.getId().toString() : null);
        resp.setProjectId(entity.getProjectId() != null ? entity.getProjectId().toString() : null);
        resp.setProjectName(entity.getProjectName());
        resp.setSourceSnapshotDate(entity.getSourceSnapshotDate());
        resp.setPolicyKey(entity.getPolicyKey());
        resp.setGuardrailKey(entity.getGuardrailKey());
        resp.setCategory(entity.getCategory());
        resp.setPriority(entity.getPriority());
        resp.setWorkflowStatus(entity.getWorkflowStatus());
        resp.setTitle(entity.getTitle());
        resp.setSummary(entity.getSummary());
        resp.setOwnerId(entity.getOwnerId() != null ? entity.getOwnerId().toString() : null);
        resp.setOwnerName(entity.getOwnerName());
        resp.setDueAt(entity.getDueAt());
        resp.setResolvedAt(entity.getResolvedAt());
        resp.setResolutionNote(entity.getResolutionNote());

        // Check for active waiver status
        if (entity.getId() != null) {
            LambdaQueryWrapper<GovernanceWaiverRequestEntity> waiverQuery = new LambdaQueryWrapper<>();
            waiverQuery.eq(GovernanceWaiverRequestEntity::getRecommendationId, entity.getId());
            waiverQuery.orderByDesc(GovernanceWaiverRequestEntity::getCreateTime);
            waiverQuery.last("LIMIT 1");
            GovernanceWaiverRequestEntity latestWaiver = governanceWaiverRequestMapper.selectOne(waiverQuery);
            if (latestWaiver != null) {
                resp.setWaiverStatus(latestWaiver.getWaiverStatus());
            }
        }

        resp.setCreateTime(entity.getCreateTime());
        resp.setUpdateTime(entity.getUpdateTime());
        return resp;
    }

    private static Long parseLong(String value, String fieldName) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new BizException(ErrorCode.BAD_REQUEST, fieldName + " 格式无效");
        }
    }
}
