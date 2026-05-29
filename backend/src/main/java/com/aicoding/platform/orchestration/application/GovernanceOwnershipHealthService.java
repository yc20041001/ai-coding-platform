package com.aicoding.platform.orchestration.application;

import com.aicoding.platform.orchestration.domain.GovernanceOwnershipSnapshotEntity;
import com.aicoding.platform.orchestration.domain.GovernanceRecommendationItemEntity;
import com.aicoding.platform.orchestration.dto.*;
import com.aicoding.platform.orchestration.infrastructure.GovernanceOwnershipSnapshotMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class GovernanceOwnershipHealthService {

    private final GovernanceOwnershipSnapshotMapper governanceOwnershipSnapshotMapper;
    private final GovernanceRecommendationWorkflowService governanceRecommendationWorkflowService;
    private final GovernanceEscalationService governanceEscalationService;
    private final GovernanceSlaPolicyService governanceSlaPolicyService;

    public GovernanceOwnershipHealthService(GovernanceOwnershipSnapshotMapper governanceOwnershipSnapshotMapper,
                                             GovernanceRecommendationWorkflowService governanceRecommendationWorkflowService,
                                             GovernanceEscalationService governanceEscalationService,
                                             GovernanceSlaPolicyService governanceSlaPolicyService) {
        this.governanceOwnershipSnapshotMapper = governanceOwnershipSnapshotMapper;
        this.governanceRecommendationWorkflowService = governanceRecommendationWorkflowService;
        this.governanceEscalationService = governanceEscalationService;
        this.governanceSlaPolicyService = governanceSlaPolicyService;
    }

    @Transactional
    public void refreshOwnership() {
        LocalDate today = LocalDate.now();
        LambdaQueryWrapper<GovernanceOwnershipSnapshotEntity> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(GovernanceOwnershipSnapshotEntity::getSnapshotDate, today);
        governanceOwnershipSnapshotMapper.delete(deleteWrapper);

        List<GovernanceRecommendationItemEntity> allItems = governanceRecommendationWorkflowService.getAllItems();
        LocalDateTime now = LocalDateTime.now();

        // Group by owner
        Map<Long, List<GovernanceRecommendationItemEntity>> byOwner = new HashMap<>();
        for (var item : allItems) {
            if (item.getOwnerId() != null) {
                byOwner.computeIfAbsent(item.getOwnerId(), k -> new ArrayList<>()).add(item);
            }
        }

        // Also add items with null owner as "unassigned" group for stats, but skip for snapshot
        List<GovernanceOwnershipSnapshotEntity> snapshots = new ArrayList<>();

        for (var entry : byOwner.entrySet()) {
            Long ownerId = entry.getKey();
            List<GovernanceRecommendationItemEntity> items = entry.getValue();
            String ownerName = items.get(0).getOwnerName() != null ? items.get(0).getOwnerName() : "Owner-" + ownerId;

            int open = 0, inProgress = 0, overdue = 0, completed7d = 0, activeWaiver = 0;

            for (var item : items) {
                switch (item.getWorkflowStatus()) {
                    case "OPEN", "ACKNOWLEDGED" -> open++;
                    case "IN_PROGRESS", "BLOCKED" -> inProgress++;
                }
                if (item.getDueAt() != null && item.getDueAt().isBefore(now)
                        && !"COMPLETED".equals(item.getWorkflowStatus())
                        && !"REJECTED".equals(item.getWorkflowStatus())) {
                    overdue++;
                }
                if (item.getResolvedAt() != null && item.getResolvedAt().isAfter(now.minusDays(7))) {
                    completed7d++;
                }
            }

            // Health score: base 100 - overdue*12 - open*3 - activeWaiver*4 + completed7d*2
            BigDecimal score = BigDecimal.valueOf(100)
                    .subtract(BigDecimal.valueOf(overdue * 12))
                    .subtract(BigDecimal.valueOf(open * 3))
                    .subtract(BigDecimal.valueOf(activeWaiver * 4))
                    .add(BigDecimal.valueOf(completed7d * 2));
            score = score.max(BigDecimal.ZERO).min(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP);

            String level = score.compareTo(BigDecimal.valueOf(85)) >= 0 ? "HEALTHY"
                    : score.compareTo(BigDecimal.valueOf(60)) >= 0 ? "WATCH"
                    : score.compareTo(BigDecimal.valueOf(35)) >= 0 ? "RISK" : "CRITICAL";

            GovernanceOwnershipSnapshotEntity snap = new GovernanceOwnershipSnapshotEntity();
            snap.setSnapshotDate(today);
            snap.setOwnerId(ownerId);
            snap.setOwnerName(ownerName);
            snap.setTotalAssignedCount(items.size());
            snap.setOpenCount(open);
            snap.setInProgressCount(inProgress);
            snap.setOverdueCount(overdue);
            snap.setCompleted7dCount(completed7d);
            snap.setActiveWaiverCount(activeWaiver);
            snap.setOwnerHealthScore(score);
            snap.setOwnerHealthLevel(level);
            snap.setSummaryText("Owner " + ownerName + " — score " + score + "/100 (" + level + "), open: " + open + ", overdue: " + overdue);
            snap.setCreateTime(LocalDateTime.now());
            snapshots.add(snap);
        }

        for (var snap : snapshots) {
            governanceOwnershipSnapshotMapper.insert(snap);
        }
    }

    @Transactional(readOnly = true)
    public List<GovernanceOwnershipSnapshotResponse> getOwnershipList() {
        LocalDate today = LocalDate.now();
        LambdaQueryWrapper<GovernanceOwnershipSnapshotEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GovernanceOwnershipSnapshotEntity::getSnapshotDate, today);
        wrapper.orderByDesc(GovernanceOwnershipSnapshotEntity::getOwnerHealthScore);
        List<GovernanceOwnershipSnapshotEntity> list = governanceOwnershipSnapshotMapper.selectList(wrapper);
        if (list.isEmpty()) {
            wrapper = new LambdaQueryWrapper<>();
            wrapper.orderByDesc(GovernanceOwnershipSnapshotEntity::getCreateTime);
            wrapper.last("LIMIT 50");
            list = governanceOwnershipSnapshotMapper.selectList(wrapper);
        }
        return list.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public GovernanceOwnershipDashboardResponse getDashboard() {
        List<GovernanceOwnershipSnapshotResponse> list = getOwnershipList();
        int healthy = 0, watch = 0, risk = 0, critical = 0, throughput = 0;
        for (var s : list) {
            switch (s.getOwnerHealthLevel()) {
                case "HEALTHY" -> healthy++;
                case "WATCH" -> watch++;
                case "RISK" -> risk++;
                case "CRITICAL" -> critical++;
            }
            throughput += intOrZero(s.getCompleted7dCount());
        }

        List<GovernanceOwnershipSnapshotResponse> sortedByScore = new ArrayList<>(list);
        sortedByScore.sort(Comparator.comparing(GovernanceOwnershipSnapshotResponse::getOwnerHealthScore));

        GovernanceOwnershipDashboardResponse resp = new GovernanceOwnershipDashboardResponse();
        resp.setSnapshotDate(list.isEmpty() ? LocalDate.now() : list.get(0).getSnapshotDate());
        resp.setOwnerCount(list.size());
        resp.setHealthyCount(healthy);
        resp.setWatchCount(watch);
        resp.setRiskCount(risk);
        resp.setCriticalCount(critical);
        resp.setTopOverloadedOwners(sortedByScore.stream().limit(5).collect(Collectors.toList()));
        resp.setTopHealthyOwners(list.stream().limit(5).collect(Collectors.toList()));
        resp.setOverallThroughput7d(throughput);
        return resp;
    }

    @Transactional(readOnly = true)
    public GovernanceOperationsSummaryResponse getSummary() {
        var slaPoliciesCount = 0;
        try {
            slaPoliciesCount = governanceSlaPolicyService.listPolicies().size();
        } catch (Exception e) { /* ignore */ }

        var escDash = governanceEscalationService.getDashboard();
        var ownerDash = getDashboard();

        GovernanceOperationsSummaryResponse resp = new GovernanceOperationsSummaryResponse();
        resp.setSnapshotDate(LocalDate.now());
        resp.setSlaPolicyCount(slaPoliciesCount);
        resp.setOpenEscalationCount(escDash.getOpenEscalationCount());
        resp.setHighEscalationCount(escDash.getHighEscalationCount());
        resp.setCriticalEscalationCount(escDash.getCriticalEscalationCount());
        resp.setHealthyOwnerCount(ownerDash.getHealthyCount());
        resp.setWatchOwnerCount(ownerDash.getWatchCount());
        resp.setRiskOwnerCount(ownerDash.getRiskCount());
        resp.setCriticalOwnerCount(ownerDash.getCriticalCount());
        resp.setWaiverExpiringSoonCount(escDash.getWaiverExpiringSoonCount());
        resp.setOverallThroughput7d(ownerDash.getOverallThroughput7d());

        // Count overdue recommendations from escalation events
        resp.setOverdueRecommendationCount(escDash.getOpenEscalationCount());

        StringBuilder md = new StringBuilder();
        md.append("# Governance Operations Summary\n\n");
        md.append("**Snapshot Date**: ").append(resp.getSnapshotDate()).append("\n\n");
        md.append("## SLA & Escalation\n\n");
        md.append("- SLA Policies: ").append(resp.getSlaPolicyCount()).append("\n");
        md.append("- Open Escalations: ").append(resp.getOpenEscalationCount()).append("\n");
        md.append("- High Escalations: ").append(resp.getHighEscalationCount()).append("\n");
        md.append("- Critical Escalations: ").append(resp.getCriticalEscalationCount()).append("\n");
        md.append("- Waiver Expiring Soon: ").append(resp.getWaiverExpiringSoonCount()).append("\n\n");
        md.append("## Ownership Health\n\n");
        md.append("- Owners: ").append(ownerDash.getOwnerCount()).append("\n");
        md.append("- Healthy: ").append(resp.getHealthyOwnerCount()).append("\n");
        md.append("- Watch: ").append(resp.getWatchOwnerCount()).append("\n");
        md.append("- Risk: ").append(resp.getRiskOwnerCount()).append("\n");
        md.append("- Critical: ").append(resp.getCriticalOwnerCount()).append("\n\n");
        md.append("## Throughput\n\n");
        md.append("- 7d Completed: ").append(resp.getOverallThroughput7d()).append("\n");
        resp.setSummaryMarkdown(md.toString());

        return resp;
    }

    private GovernanceOwnershipSnapshotResponse toResponse(GovernanceOwnershipSnapshotEntity entity) {
        GovernanceOwnershipSnapshotResponse resp = new GovernanceOwnershipSnapshotResponse();
        resp.setId(entity.getId() != null ? entity.getId().toString() : null);
        resp.setSnapshotDate(entity.getSnapshotDate());
        resp.setOwnerId(entity.getOwnerId() != null ? entity.getOwnerId().toString() : null);
        resp.setOwnerName(entity.getOwnerName());
        resp.setTotalAssignedCount(entity.getTotalAssignedCount());
        resp.setOpenCount(entity.getOpenCount());
        resp.setInProgressCount(entity.getInProgressCount());
        resp.setOverdueCount(entity.getOverdueCount());
        resp.setCompleted7dCount(entity.getCompleted7dCount());
        resp.setActiveWaiverCount(entity.getActiveWaiverCount());
        resp.setOwnerHealthScore(entity.getOwnerHealthScore());
        resp.setOwnerHealthLevel(entity.getOwnerHealthLevel());
        resp.setSummaryText(entity.getSummaryText());
        return resp;
    }

    private static int intOrZero(Integer value) {
        if (value == null) {
            return 0;
        }
        return value;
    }
}
