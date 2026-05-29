package com.aicoding.platform.orchestration.application;

import com.aicoding.platform.common.exception.BizException;
import com.aicoding.platform.common.exception.ErrorCode;
import com.aicoding.platform.orchestration.domain.*;
import com.aicoding.platform.orchestration.dto.GovernanceEscalationDashboardResponse;
import com.aicoding.platform.orchestration.dto.GovernanceEscalationEventResponse;
import com.aicoding.platform.orchestration.infrastructure.GovernanceEscalationEventMapper;
import com.aicoding.platform.orchestration.infrastructure.GovernanceWaiverRequestMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class GovernanceEscalationService {

    private final GovernanceEscalationEventMapper governanceEscalationEventMapper;
    private final GovernanceRecommendationWorkflowService governanceRecommendationWorkflowService;
    private final GovernanceWaiverRequestMapper governanceWaiverRequestMapper;
    private final GovernanceSlaPolicyService governanceSlaPolicyService;

    public GovernanceEscalationService(GovernanceEscalationEventMapper governanceEscalationEventMapper,
                                        GovernanceRecommendationWorkflowService governanceRecommendationWorkflowService,
                                        GovernanceWaiverRequestMapper governanceWaiverRequestMapper,
                                        GovernanceSlaPolicyService governanceSlaPolicyService) {
        this.governanceEscalationEventMapper = governanceEscalationEventMapper;
        this.governanceRecommendationWorkflowService = governanceRecommendationWorkflowService;
        this.governanceWaiverRequestMapper = governanceWaiverRequestMapper;
        this.governanceSlaPolicyService = governanceSlaPolicyService;
    }

    @Transactional
    public int scanEscalations() {
        int created = 0;
        LocalDateTime now = LocalDateTime.now();

        // Get default SLA hours per priority
        var slaPolicies = governanceSlaPolicyService.getEnabledPolicies();
        Map<String, Integer> slaMap = new HashMap<>();
        Map<String, Integer> warnMap = new HashMap<>();
        for (var p : slaPolicies) {
            slaMap.put(p.getPriority(), p.getSlaHours());
            warnMap.put(p.getPriority(), p.getWarningHours());
        }

        // 1. Scan overdue recommendations
        List<GovernanceRecommendationItemEntity> items = governanceRecommendationWorkflowService.getOpenItems();
        for (var item : items) {
            if (item.getDueAt() != null && item.getDueAt().isBefore(now)) {
                if (!eventExists(item.getId(), "OVERDUE_RECOMMENDATION")) {
                    createEscalation(item.getId(), item.getProjectId(), "OVERDUE_RECOMMENDATION",
                            "HIGH", "Recommendation overdue", "Due at " + item.getDueAt(),
                            item.getOwnerId(), item.getOwnerName());
                    created++;
                }
            }
            // Check SLA warning
            if (item.getDueAt() != null && item.getCreateTime() != null && eventExists(item.getId(), "OVERDUE_RECOMMENDATION")) {
                // Already has overdue event
            }
        }

        // 2. Scan waiver expiry
        LambdaQueryWrapper<GovernanceWaiverRequestEntity> waiverQuery = new LambdaQueryWrapper<>();
        waiverQuery.eq(GovernanceWaiverRequestEntity::getWaiverStatus, "APPROVED");
        waiverQuery.isNotNull(GovernanceWaiverRequestEntity::getExpiresAt);
        List<GovernanceWaiverRequestEntity> activeWaivers = governanceWaiverRequestMapper.selectList(waiverQuery);

        for (var w : activeWaivers) {
            if (w.getExpiresAt().isBefore(now)) {
                if (!eventExists(w.getRecommendationId(), "WAIVER_EXPIRED")) {
                    createEscalation(w.getRecommendationId(), w.getProjectId(), "WAIVER_EXPIRED",
                            "CRITICAL", "Waiver expired", "Waiver expired at " + w.getExpiresAt(),
                            null, null);
                    created++;
                }
            } else if (w.getExpiresAt().isBefore(now.plusHours(24))) {
                if (!eventExists(w.getRecommendationId(), "WAIVER_EXPIRING_SOON")) {
                    createEscalation(w.getRecommendationId(), w.getProjectId(), "WAIVER_EXPIRING_SOON",
                            "MEDIUM", "Waiver expiring soon", "Waiver expires at " + w.getExpiresAt(),
                            null, null);
                    created++;
                }
            }
        }

        // 3. Scan owner missing
        for (var item : items) {
            if (item.getOwnerId() == null && !eventExists(item.getId(), "OWNER_MISSING")) {
                createEscalation(item.getId(), item.getProjectId(), "OWNER_MISSING",
                        "HIGH", "Owner not assigned", "Recommendation has no owner",
                        null, null);
                created++;
            }
        }

        // 4. Scan owner overloaded
        Map<Long, List<GovernanceRecommendationItemEntity>> byOwner = new HashMap<>();
        for (var item : items) {
            if (item.getOwnerId() != null) {
                byOwner.computeIfAbsent(item.getOwnerId(), k -> new ArrayList<>()).add(item);
            }
        }
        for (var entry : byOwner.entrySet()) {
            long overdueCount = entry.getValue().stream()
                    .filter(i -> i.getDueAt() != null && i.getDueAt().isBefore(now)).count();
            if (overdueCount >= 5) {
                // Check if we already have an overload event for this owner
                boolean hasEvent = governanceEscalationEventMapper.selectList(new LambdaQueryWrapper<GovernanceEscalationEventEntity>()
                        .eq(GovernanceEscalationEventEntity::getEscalationType, "OWNER_OVERLOADED")
                        .eq(GovernanceEscalationEventEntity::getOwnerId, entry.getKey())
                        .eq(GovernanceEscalationEventEntity::getEventStatus, "OPEN")).stream().findAny().isPresent();
                if (!hasEvent) {
                    String name = entry.getValue().get(0).getOwnerName();
                    createEscalation(entry.getValue().get(0).getId(), entry.getValue().get(0).getProjectId(),
                            "OWNER_OVERLOADED", overdueCount >= 10 ? "CRITICAL" : "HIGH",
                            "Owner overloaded", overdueCount + " overdue items for " + name,
                            entry.getKey(), name);
                    created++;
                }
            }
        }

        return created;
    }

    private boolean eventExists(Long recommendationId, String type) {
        LambdaQueryWrapper<GovernanceEscalationEventEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GovernanceEscalationEventEntity::getRecommendationId, recommendationId);
        wrapper.eq(GovernanceEscalationEventEntity::getEscalationType, type);
        wrapper.in(GovernanceEscalationEventEntity::getEventStatus, "OPEN", "ACKNOWLEDGED");
        return governanceEscalationEventMapper.selectCount(wrapper) > 0;
    }

    private void createEscalation(Long recId, Long projectId, String type, String level,
                                   String summary, String detail, Long ownerId, String ownerName) {
        GovernanceEscalationEventEntity event = new GovernanceEscalationEventEntity();
        event.setRecommendationId(recId);
        event.setProjectId(projectId);
        event.setEscalationType(type);
        event.setEscalationLevel(level);
        event.setEventStatus("OPEN");
        event.setSummary(summary);
        event.setDetail(detail);
        event.setOwnerId(ownerId);
        event.setOwnerName(ownerName);
        event.setTriggeredAt(LocalDateTime.now());
        governanceEscalationEventMapper.insert(event);
    }

    @Transactional(readOnly = true)
    public List<GovernanceEscalationEventResponse> listEscalations() {
        LambdaQueryWrapper<GovernanceEscalationEventEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(GovernanceEscalationEventEntity::getTriggeredAt);
        return governanceEscalationEventMapper.selectList(wrapper).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public GovernanceEscalationDashboardResponse getDashboard() {
        List<GovernanceEscalationEventResponse> all = listEscalations();
        GovernanceEscalationDashboardResponse resp = new GovernanceEscalationDashboardResponse();
        resp.setSnapshotDate(java.time.LocalDate.now());
        resp.setOpenEscalationCount((int) all.stream().filter(e -> "OPEN".equals(e.getEventStatus())).count());
        resp.setHighEscalationCount((int) all.stream().filter(e -> "HIGH".equals(e.getEscalationLevel())).count());
        resp.setCriticalEscalationCount((int) all.stream().filter(e -> "CRITICAL".equals(e.getEscalationLevel())).count());
        resp.setWaiverExpiringSoonCount((int) all.stream().filter(e -> "WAIVER_EXPIRING_SOON".equals(e.getEscalationType())).count());
        resp.setWaiverExpiredCount((int) all.stream().filter(e -> "WAIVER_EXPIRED".equals(e.getEscalationType())).count());
        resp.setOwnerMissingCount((int) all.stream().filter(e -> "OWNER_MISSING".equals(e.getEscalationType())).count());
        resp.setTopEscalations(all.stream().filter(e -> "OPEN".equals(e.getEventStatus())).limit(10).collect(Collectors.toList()));
        return resp;
    }

    @Transactional
    public GovernanceEscalationEventResponse updateEventStatus(String eventIdStr, String newStatus) {
        Long eventId = parseLong(eventIdStr, "eventId");
        GovernanceEscalationEventEntity entity = governanceEscalationEventMapper.selectById(eventId);
        if (entity == null) throw new BizException(ErrorCode.NOT_FOUND, "Escalation event 不存在");

        String current = entity.getEventStatus();
        if (!isValidTransition(current, newStatus)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "Invalid transition from " + current + " to " + newStatus);
        }

        entity.setEventStatus(newStatus);
        if ("ACKNOWLEDGED".equals(newStatus)) entity.setAcknowledgedAt(LocalDateTime.now());
        if ("RESOLVED".equals(newStatus)) entity.setResolvedAt(LocalDateTime.now());
        governanceEscalationEventMapper.updateById(entity);
        return toResponse(entity);
    }

    private boolean isValidTransition(String current, String next) {
        if ("OPEN".equals(current) && ("ACKNOWLEDGED".equals(next) || "IGNORED".equals(next))) return true;
        if ("ACKNOWLEDGED".equals(current) && "RESOLVED".equals(next)) return true;
        return false;
    }

    private GovernanceEscalationEventResponse toResponse(GovernanceEscalationEventEntity entity) {
        GovernanceEscalationEventResponse resp = new GovernanceEscalationEventResponse();
        resp.setId(entity.getId() != null ? entity.getId().toString() : null);
        resp.setRecommendationId(entity.getRecommendationId() != null ? entity.getRecommendationId().toString() : null);
        resp.setProjectId(entity.getProjectId() != null ? entity.getProjectId().toString() : null);
        resp.setEscalationType(entity.getEscalationType());
        resp.setEscalationLevel(entity.getEscalationLevel());
        resp.setEventStatus(entity.getEventStatus());
        resp.setSummary(entity.getSummary());
        resp.setDetail(entity.getDetail());
        resp.setOwnerId(entity.getOwnerId() != null ? entity.getOwnerId().toString() : null);
        resp.setOwnerName(entity.getOwnerName());
        resp.setTriggeredAt(entity.getTriggeredAt());
        resp.setAcknowledgedAt(entity.getAcknowledgedAt());
        resp.setResolvedAt(entity.getResolvedAt());
        resp.setCreateTime(entity.getCreateTime());
        return resp;
    }

    private static Long parseLong(String value, String fieldName) {
        try { return Long.parseLong(value); }
        catch (NumberFormatException e) { throw new BizException(ErrorCode.BAD_REQUEST, fieldName + " 格式无效"); }
    }
}
