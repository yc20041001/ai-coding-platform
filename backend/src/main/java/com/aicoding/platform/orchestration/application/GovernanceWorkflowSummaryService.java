package com.aicoding.platform.orchestration.application;

import com.aicoding.platform.orchestration.domain.GovernanceRecommendationItemEntity;
import com.aicoding.platform.orchestration.domain.GovernanceWorkflowSnapshotEntity;
import com.aicoding.platform.orchestration.dto.GovernanceRecommendationItemResponse;
import com.aicoding.platform.orchestration.dto.GovernanceWorkflowDashboardResponse;
import com.aicoding.platform.orchestration.dto.GovernanceWorkflowSummaryResponse;
import com.aicoding.platform.orchestration.infrastructure.GovernanceWorkflowSnapshotMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class GovernanceWorkflowSummaryService {

    private final GovernanceWorkflowSnapshotMapper governanceWorkflowSnapshotMapper;
    private final GovernanceRecommendationWorkflowService governanceRecommendationWorkflowService;
    private final GovernanceWaiverManagementService governanceWaiverManagementService;

    public GovernanceWorkflowSummaryService(GovernanceWorkflowSnapshotMapper governanceWorkflowSnapshotMapper,
                                             GovernanceRecommendationWorkflowService governanceRecommendationWorkflowService,
                                             GovernanceWaiverManagementService governanceWaiverManagementService) {
        this.governanceWorkflowSnapshotMapper = governanceWorkflowSnapshotMapper;
        this.governanceRecommendationWorkflowService = governanceRecommendationWorkflowService;
        this.governanceWaiverManagementService = governanceWaiverManagementService;
    }

    @Transactional
    public void refreshSnapshot() {
        LocalDate today = LocalDate.now();

        // Delete existing snapshot for today
        LambdaQueryWrapper<GovernanceWorkflowSnapshotEntity> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(GovernanceWorkflowSnapshotEntity::getSnapshotDate, today);
        governanceWorkflowSnapshotMapper.delete(deleteWrapper);

        List<GovernanceRecommendationItemEntity> allItems = governanceRecommendationWorkflowService.getAllItems();
        int total = allItems.size();

        int open = 0, inProgress = 0, completed = 0, blocked = 0;
        int overdue = 0;

        for (GovernanceRecommendationItemEntity item : allItems) {
            switch (item.getWorkflowStatus()) {
                case "OPEN" -> open++;
                case "ACKNOWLEDGED" -> open++;
                case "IN_PROGRESS" -> inProgress++;
                case "COMPLETED" -> completed++;
                case "BLOCKED" -> blocked++;
            }
            // Overdue detection
            String ws = item.getWorkflowStatus();
            if (item.getDueAt() != null && item.getDueAt().isBefore(LocalDateTime.now())
                    && ("OPEN".equals(ws) || "ACKNOWLEDGED".equals(ws) || "IN_PROGRESS".equals(ws) || "BLOCKED".equals(ws))) {
                overdue++;
            }
        }

        int activeWaiverCount = governanceWaiverManagementService.getActiveWaivers().size();
        int expiredWaiverCount = governanceWaiverManagementService.getExpiredWaivers().size();

        BigDecimal completionRate = total > 0
                ? BigDecimal.valueOf(completed).multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        BigDecimal overdueRate = total > 0
                ? BigDecimal.valueOf(overdue).multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        GovernanceWorkflowSnapshotEntity snapshot = new GovernanceWorkflowSnapshotEntity();
        snapshot.setSnapshotDate(today);
        snapshot.setTotalRecommendationCount(total);
        snapshot.setOpenRecommendationCount(open);
        snapshot.setInProgressCount(inProgress);
        snapshot.setCompletedCount(completed);
        snapshot.setBlockedCount(blocked);
        snapshot.setOverdueCount(overdue);
        snapshot.setActiveWaiverCount(activeWaiverCount);
        snapshot.setExpiredWaiverCount(expiredWaiverCount);
        snapshot.setCompletionRate(completionRate);
        snapshot.setOverdueRate(overdueRate);
        snapshot.setSummaryText(buildSummary(total, open + inProgress, completed, overdue, completionRate));
        snapshot.setCreateTime(LocalDateTime.now());

        governanceWorkflowSnapshotMapper.insert(snapshot);
    }

    private String buildSummary(int total, int open, int completed, int overdue, BigDecimal completionRate) {
        return "Total: " + total + ", Open: " + open + ", Completed: " + completed
                + ", Overdue: " + overdue + ", Completion: " + completionRate + "%";
    }

    @Transactional(readOnly = true)
    public GovernanceWorkflowDashboardResponse getDashboard() {
        LocalDate today = LocalDate.now();

        // Get today's snapshot or latest
        LambdaQueryWrapper<GovernanceWorkflowSnapshotEntity> snapWrapper = new LambdaQueryWrapper<>();
        snapWrapper.eq(GovernanceWorkflowSnapshotEntity::getSnapshotDate, today);
        snapWrapper.last("LIMIT 1");
        GovernanceWorkflowSnapshotEntity snap = governanceWorkflowSnapshotMapper.selectOne(snapWrapper);

        if (snap == null) {
            snapWrapper = new LambdaQueryWrapper<>();
            snapWrapper.orderByDesc(GovernanceWorkflowSnapshotEntity::getCreateTime);
            snapWrapper.last("LIMIT 1");
            snap = governanceWorkflowSnapshotMapper.selectOne(snapWrapper);
        }

        GovernanceWorkflowDashboardResponse resp = new GovernanceWorkflowDashboardResponse();

        if (snap != null) {
            resp.setSnapshotDate(snap.getSnapshotDate());
            resp.setTotalRecommendationCount(snap.getTotalRecommendationCount());
            resp.setOpenRecommendationCount(snap.getOpenRecommendationCount());
            resp.setInProgressCount(snap.getInProgressCount());
            resp.setCompletedCount(snap.getCompletedCount());
            resp.setBlockedCount(snap.getBlockedCount());
            resp.setOverdueCount(snap.getOverdueCount());
            resp.setActiveWaiverCount(snap.getActiveWaiverCount());
            resp.setExpiredWaiverCount(snap.getExpiredWaiverCount());
            resp.setCompletionRate(snap.getCompletionRate());
            resp.setOverdueRate(snap.getOverdueRate());
        } else {
            resp.setSnapshotDate(today);
            resp.setTotalRecommendationCount(0);
            resp.setCompletionRate(BigDecimal.ZERO);
            resp.setOverdueRate(BigDecimal.ZERO);
        }

        // Get top priority and overdue items
        List<GovernanceRecommendationItemResponse> items = governanceRecommendationWorkflowService.listItems(null, null);
        List<GovernanceRecommendationItemResponse> sortedByPriority = items.stream()
                .sorted(Comparator.comparingInt(i -> priorityWeight(i.getPriority())))
                .limit(5)
                .collect(Collectors.toList());
        resp.setTopPriorityItems(sortedByPriority);

        List<GovernanceRecommendationItemResponse> overdueItems = items.stream()
                .filter(i -> i.getDueAt() != null && i.getDueAt().isBefore(LocalDateTime.now())
                        && !"COMPLETED".equals(i.getWorkflowStatus()) && !"REJECTED".equals(i.getWorkflowStatus()))
                .sorted(Comparator.comparing(GovernanceRecommendationItemResponse::getDueAt))
                .limit(5)
                .collect(Collectors.toList());
        resp.setTopOverdueItems(overdueItems);

        return resp;
    }

    @Transactional(readOnly = true)
    public GovernanceWorkflowSummaryResponse getSummary() {
        GovernanceWorkflowDashboardResponse dash = getDashboard();
        List<GovernanceRecommendationItemResponse> items = governanceRecommendationWorkflowService.listItems(null, null);

        GovernanceWorkflowSummaryResponse resp = new GovernanceWorkflowSummaryResponse();
        resp.setSnapshotDate(dash.getSnapshotDate());
        resp.setTotalRecommendationCount(dash.getTotalRecommendationCount());
        resp.setOpenCount(dash.getOpenRecommendationCount());
        resp.setInProgressCount(dash.getInProgressCount());
        resp.setCompletedCount(dash.getCompletedCount());
        resp.setBlockedCount(dash.getBlockedCount());
        resp.setOverdueCount(dash.getOverdueCount());
        resp.setActiveWaiverCount(dash.getActiveWaiverCount());
        resp.setCompletionRate(dash.getCompletionRate());
        resp.setOverdueRate(dash.getOverdueRate());
        resp.setTopPriorityItems(dash.getTopPriorityItems());
        resp.setTopOverdueItems(dash.getTopOverdueItems());

        // Build markdown
        StringBuilder md = new StringBuilder();
        md.append("# Governance Workflow Summary\n\n");
        md.append("**Snapshot Date**: ").append(resp.getSnapshotDate()).append("\n\n");
        md.append("## Overview\n\n");
        md.append("- Total Recommendations: ").append(resp.getTotalRecommendationCount()).append("\n");
        md.append("- Open: ").append(resp.getOpenCount()).append("\n");
        md.append("- In Progress: ").append(resp.getInProgressCount()).append("\n");
        md.append("- Completed: ").append(resp.getCompletedCount()).append("\n");
        md.append("- Blocked: ").append(resp.getBlockedCount()).append("\n");
        md.append("- Overdue: ").append(resp.getOverdueCount()).append("\n\n");
        md.append("## Rates\n\n");
        md.append("- Completion Rate: ").append(resp.getCompletionRate()).append("%\n");
        md.append("- Overdue Rate: ").append(resp.getOverdueRate()).append("%\n\n");
        md.append("## Waivers\n\n");
        md.append("- Active Waivers: ").append(resp.getActiveWaiverCount()).append("\n\n");
        md.append("## Top Priority Items\n\n");
        for (GovernanceRecommendationItemResponse item : resp.getTopPriorityItems()) {
            md.append("- [").append(item.getPriority()).append("] ")
              .append(item.getProjectName()).append(": ")
              .append(item.getTitle()).append("\n");
        }
        resp.setSummaryMarkdown(md.toString());

        return resp;
    }

    private int priorityWeight(String priority) {
        if (priority == null) return 999;
        switch (priority) {
            case "P0": return 0;
            case "P1": return 1;
            case "P2": return 2;
            case "P3": return 3;
            default: return 999;
        }
    }
}
