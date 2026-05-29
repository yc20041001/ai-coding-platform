package com.aicoding.platform.orchestration.application;

import com.aicoding.platform.orchestration.domain.*;
import com.aicoding.platform.orchestration.dto.*;
import com.aicoding.platform.orchestration.infrastructure.GovernanceBacklogSnapshotMapper;
import com.aicoding.platform.orchestration.infrastructure.GovernanceCapacityForecastMapper;
import com.aicoding.platform.orchestration.infrastructure.PredictiveRiskSignalMapper;
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
public class GovernanceBacklogHealthService {

    private final GovernanceBacklogSnapshotMapper governanceBacklogSnapshotMapper;
    private final GovernanceCapacityForecastMapper governanceCapacityForecastMapper;
    private final PredictiveRiskSignalMapper predictiveRiskSignalMapper;
    private final GovernanceRecommendationWorkflowService governanceRecommendationWorkflowService;

    public GovernanceBacklogHealthService(GovernanceBacklogSnapshotMapper governanceBacklogSnapshotMapper,
                                           GovernanceCapacityForecastMapper governanceCapacityForecastMapper,
                                           PredictiveRiskSignalMapper predictiveRiskSignalMapper,
                                           GovernanceRecommendationWorkflowService governanceRecommendationWorkflowService) {
        this.governanceBacklogSnapshotMapper = governanceBacklogSnapshotMapper;
        this.governanceCapacityForecastMapper = governanceCapacityForecastMapper;
        this.predictiveRiskSignalMapper = predictiveRiskSignalMapper;
        this.governanceRecommendationWorkflowService = governanceRecommendationWorkflowService;
    }

    @Transactional
    public void refreshBacklog() {
        LocalDate today = LocalDate.now();
        LambdaQueryWrapper<GovernanceBacklogSnapshotEntity> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(GovernanceBacklogSnapshotEntity::getSnapshotDate, today);
        governanceBacklogSnapshotMapper.delete(deleteWrapper);

        List<GovernanceRecommendationItemEntity> allItems = governanceRecommendationWorkflowService.getAllItems();
        LocalDateTime now = LocalDateTime.now();

        Map<Long, List<GovernanceRecommendationItemEntity>> byProject = new HashMap<>();
        for (var item : allItems) {
            if (item.getProjectId() != null) {
                byProject.computeIfAbsent(item.getProjectId(), k -> new ArrayList<>()).add(item);
            }
        }

        List<GovernanceBacklogSnapshotEntity> snapshots = new ArrayList<>();

        for (var entry : byProject.entrySet()) {
            Long projectId = entry.getKey();
            List<GovernanceRecommendationItemEntity> items = entry.getValue();
            String projectName = items.get(0).getProjectName() != null ? items.get(0).getProjectName() : "Project-" + projectId;

            int open = 0, inProgress = 0, blocked = 0, overdue = 0, completed7d = 0, incoming7d = 0;
            for (var item : items) {
                String ws = item.getWorkflowStatus();
                switch (item.getWorkflowStatus()) {
                    case "OPEN", "ACKNOWLEDGED" -> open++;
                    case "IN_PROGRESS" -> inProgress++;
                    case "BLOCKED" -> blocked++;
                }
                if (item.getDueAt() != null && item.getDueAt().isBefore(now)
                        && !"COMPLETED".equals(ws) && !"REJECTED".equals(ws)) overdue++;
                if (item.getResolvedAt() != null && item.getResolvedAt().isAfter(now.minusDays(7))) completed7d++;
                if (item.getCreateTime() != null && item.getCreateTime().isAfter(now.minusDays(7))) incoming7d++;
            }

            BigDecimal growthRate = completed7d > 0
                    ? BigDecimal.valueOf(incoming7d - completed7d).divide(BigDecimal.valueOf(completed7d), 2, RoundingMode.HALF_UP)
                    : incoming7d > 0 ? BigDecimal.valueOf(incoming7d) : BigDecimal.ZERO;

            String healthLevel;
            if (growthRate.compareTo(BigDecimal.valueOf(1.0)) > 0 && (blocked > 0 || overdue > 3)) healthLevel = "CRITICAL";
            else if (growthRate.compareTo(BigDecimal.valueOf(0.5)) > 0 || overdue > 3) healthLevel = "RISK";
            else if (growthRate.compareTo(BigDecimal.ZERO) > 0) healthLevel = "WATCH";
            else healthLevel = "HEALTHY";

            GovernanceBacklogSnapshotEntity snap = new GovernanceBacklogSnapshotEntity();
            snap.setSnapshotDate(today);
            snap.setProjectId(projectId);
            snap.setProjectName(projectName);
            snap.setOpenCount(open);
            snap.setInProgressCount(inProgress);
            snap.setBlockedCount(blocked);
            snap.setOverdueCount(overdue);
            snap.setCompleted7dCount(completed7d);
            snap.setIncoming7dCount(incoming7d);
            snap.setBacklogGrowthRate(growthRate);
            snap.setBacklogHealthLevel(healthLevel);
            snap.setSummaryText("Project " + projectName + " — backlog growth " + growthRate + " (" + healthLevel + "), open: " + open + ", overdue: " + overdue);
            snap.setCreateTime(LocalDateTime.now());
            snapshots.add(snap);
        }

        for (var snap : snapshots) {
            governanceBacklogSnapshotMapper.insert(snap);
        }
    }

    @Transactional(readOnly = true)
    public List<GovernanceBacklogSnapshotResponse> getBacklogList() {
        LocalDate today = LocalDate.now();
        LambdaQueryWrapper<GovernanceBacklogSnapshotEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GovernanceBacklogSnapshotEntity::getSnapshotDate, today);
        wrapper.orderByDesc(GovernanceBacklogSnapshotEntity::getBacklogGrowthRate);
        List<GovernanceBacklogSnapshotEntity> list = governanceBacklogSnapshotMapper.selectList(wrapper);
        if (list.isEmpty()) {
            wrapper = new LambdaQueryWrapper<>();
            wrapper.orderByDesc(GovernanceBacklogSnapshotEntity::getCreateTime);
            wrapper.last("LIMIT 50");
            list = governanceBacklogSnapshotMapper.selectList(wrapper);
        }
        return list.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public GovernanceBacklogDashboardResponse getDashboard() {
        List<GovernanceBacklogSnapshotResponse> list = getBacklogList();
        int healthy = 0, watch = 0, risk = 0, critical = 0;
        for (var s : list) {
            switch (s.getBacklogHealthLevel()) {
                case "HEALTHY" -> healthy++;
                case "WATCH" -> watch++;
                case "RISK" -> risk++;
                case "CRITICAL" -> critical++;
            }
        }

        List<GovernanceBacklogSnapshotResponse> byGrowth = new ArrayList<>(list);
        byGrowth.sort(Comparator.comparing(GovernanceBacklogSnapshotResponse::getBacklogGrowthRate, Comparator.nullsFirst(Comparator.reverseOrder())));

        List<GovernanceBacklogSnapshotResponse> byOverdue = new ArrayList<>(list);
        byOverdue.sort(Comparator.comparing(GovernanceBacklogSnapshotResponse::getOverdueCount, Comparator.nullsFirst(Comparator.reverseOrder())));

        GovernanceBacklogDashboardResponse resp = new GovernanceBacklogDashboardResponse();
        resp.setSnapshotDate(list.isEmpty() ? LocalDate.now() : list.get(0).getSnapshotDate());
        resp.setProjectCount(list.size());
        resp.setHealthyCount(healthy);
        resp.setWatchCount(watch);
        resp.setRiskCount(risk);
        resp.setCriticalCount(critical);
        resp.setTopGrowingBacklogs(byGrowth.stream().limit(5).collect(Collectors.toList()));
        resp.setTopOverdueProjects(byOverdue.stream().limit(5).collect(Collectors.toList()));
        return resp;
    }

    @Transactional(readOnly = true)
    public GovernanceForecastSummaryResponse getSummary() {
        var capDashboard = getFromCapacityService();
        var riskDashboard = getFromRiskService();
        var backlogDash = getDashboard();

        GovernanceForecastSummaryResponse resp = new GovernanceForecastSummaryResponse();
        resp.setSnapshotDate(LocalDate.now());
        resp.setOwnerForecastCount(capDashboard.getOwnerCount());
        resp.setCriticalOwnerCount(capDashboard.getCriticalCount());
        resp.setHighOwnerCount(capDashboard.getHighCount());
        resp.setSignalCount(riskDashboard.getSignalCount());
        resp.setCriticalSignalCount(riskDashboard.getCriticalSignalCount());
        resp.setProjectCount(backlogDash.getProjectCount());
        resp.setCriticalBacklogCount(backlogDash.getCriticalCount());
        resp.setRiskBacklogCount(backlogDash.getRiskCount());
        resp.setTotalProjectedBacklog(capDashboard.getAverageProjectedBacklog() * Math.max(1, capDashboard.getOwnerCount()));
        resp.setTotalProjectedOverdue(capDashboard.getAverageProjectedOverdue() * Math.max(1, capDashboard.getOwnerCount()));

        StringBuilder md = new StringBuilder();
        md.append("# Governance Forecast Summary\n\n");
        md.append("**Snapshot Date**: ").append(resp.getSnapshotDate()).append("\n\n");
        md.append("## Capacity Forecast\n\n");
        md.append("- Owners: ").append(resp.getOwnerForecastCount()).append("\n");
        md.append("- Critical: ").append(resp.getCriticalOwnerCount()).append("\n");
        md.append("- High: ").append(resp.getHighOwnerCount()).append("\n");
        md.append("- Total Projected Backlog: ").append(resp.getTotalProjectedBacklog()).append("\n");
        md.append("- Total Projected Overdue: ").append(resp.getTotalProjectedOverdue()).append("\n\n");
        md.append("## Risk Signals\n\n");
        md.append("- Signals: ").append(resp.getSignalCount()).append("\n");
        md.append("- Critical: ").append(resp.getCriticalSignalCount()).append("\n\n");
        md.append("## Backlog Health\n\n");
        md.append("- Projects: ").append(resp.getProjectCount()).append("\n");
        md.append("- Critical: ").append(resp.getCriticalBacklogCount()).append("\n");
        md.append("- Risk: ").append(resp.getRiskBacklogCount()).append("\n");
        resp.setSummaryMarkdown(md.toString());

        return resp;
    }

    private GovernanceCapacityDashboardResponse getFromCapacityService() {
        GovernanceCapacityDashboardResponse resp = new GovernanceCapacityDashboardResponse();
        resp.setOwnerCount(0); resp.setCriticalCount(0); resp.setHighCount(0);
        resp.setAverageProjectedBacklog(0); resp.setAverageProjectedOverdue(0);
        try {
            List<GovernanceCapacityForecastEntity> list = governanceCapacityForecastMapper.selectList(
                    new LambdaQueryWrapper<GovernanceCapacityForecastEntity>()
                            .eq(GovernanceCapacityForecastEntity::getSnapshotDate, LocalDate.now())
                            .eq(GovernanceCapacityForecastEntity::getForecastHorizonDays, 7));
            if (list.isEmpty()) {
                list = governanceCapacityForecastMapper.selectList(
                        new LambdaQueryWrapper<GovernanceCapacityForecastEntity>().last("LIMIT 50"));
            }
            int low = 0, watch = 0, high = 0, critical = 0, totalB = 0, totalO = 0;
            for (var f : list) {
                switch (f.getCapacityRiskLevel()) {
                    case "LOW" -> low++; case "WATCH" -> watch++;
                    case "HIGH" -> high++; case "CRITICAL" -> critical++;
                }
                totalB += intOrZero(f.getProjectedBacklogCount());
                totalO += intOrZero(f.getProjectedOverdueCount());
            }
            resp.setOwnerCount(list.size()); resp.setLowRiskCount(low); resp.setWatchCount(watch);
            resp.setHighCount(high); resp.setCriticalCount(critical);
            resp.setAverageProjectedBacklog(list.isEmpty() ? 0 : totalB / list.size());
            resp.setAverageProjectedOverdue(list.isEmpty() ? 0 : totalO / list.size());
        } catch (Exception e) { /* ignore */ }
        return resp;
    }

    private PredictiveRiskDashboardResponse getFromRiskService() {
        PredictiveRiskDashboardResponse resp = new PredictiveRiskDashboardResponse();
        resp.setSignalCount(0); resp.setCriticalSignalCount(0);
        try {
            List<PredictiveRiskSignalEntity> list = predictiveRiskSignalMapper.selectList(
                    new LambdaQueryWrapper<PredictiveRiskSignalEntity>()
                            .eq(PredictiveRiskSignalEntity::getSnapshotDate, LocalDate.now()));
            if (list.isEmpty()) {
                list = predictiveRiskSignalMapper.selectList(
                        new LambdaQueryWrapper<PredictiveRiskSignalEntity>().last("LIMIT 50"));
            }
            resp.setSignalCount(list.size());
            resp.setCriticalSignalCount((int) list.stream().filter(s -> "CRITICAL".equals(s.getRiskLevel())).count());
        } catch (Exception e) { /* ignore */ }
        return resp;
    }

    private GovernanceBacklogSnapshotResponse toResponse(GovernanceBacklogSnapshotEntity entity) {
        GovernanceBacklogSnapshotResponse r = new GovernanceBacklogSnapshotResponse();
        r.setId(entity.getId() != null ? entity.getId().toString() : null);
        r.setSnapshotDate(entity.getSnapshotDate());
        r.setProjectId(entity.getProjectId() != null ? entity.getProjectId().toString() : null);
        r.setProjectName(entity.getProjectName());
        r.setOpenCount(entity.getOpenCount());
        r.setInProgressCount(entity.getInProgressCount());
        r.setBlockedCount(entity.getBlockedCount());
        r.setOverdueCount(entity.getOverdueCount());
        r.setWaiverActiveCount(entity.getWaiverActiveCount());
        r.setIncoming7dCount(entity.getIncoming7dCount());
        r.setCompleted7dCount(entity.getCompleted7dCount());
        r.setBacklogGrowthRate(entity.getBacklogGrowthRate());
        r.setBacklogHealthLevel(entity.getBacklogHealthLevel());
        r.setSummaryText(entity.getSummaryText());
        return r;
    }

    private static int intOrZero(Integer value) {
        if (value == null) {
            return 0;
        }
        return value;
    }
}
