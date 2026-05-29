package com.aicoding.platform.orchestration.application;

import com.aicoding.platform.orchestration.domain.*;
import com.aicoding.platform.orchestration.dto.*;
import com.aicoding.platform.orchestration.infrastructure.GovernanceCapacityForecastMapper;
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
public class GovernanceCapacityForecastService {

    private final GovernanceCapacityForecastMapper governanceCapacityForecastMapper;
    private final GovernanceRecommendationWorkflowService governanceRecommendationWorkflowService;

    public GovernanceCapacityForecastService(GovernanceCapacityForecastMapper governanceCapacityForecastMapper,
                                              GovernanceRecommendationWorkflowService governanceRecommendationWorkflowService) {
        this.governanceCapacityForecastMapper = governanceCapacityForecastMapper;
        this.governanceRecommendationWorkflowService = governanceRecommendationWorkflowService;
    }

    @Transactional
    public void refreshForecast(int horizonDays) {
        LocalDate today = LocalDate.now();
        LambdaQueryWrapper<GovernanceCapacityForecastEntity> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(GovernanceCapacityForecastEntity::getSnapshotDate, today);
        deleteWrapper.eq(GovernanceCapacityForecastEntity::getForecastHorizonDays, horizonDays);
        governanceCapacityForecastMapper.delete(deleteWrapper);

        List<GovernanceRecommendationItemEntity> allItems = governanceRecommendationWorkflowService.getAllItems();
        LocalDateTime now = LocalDateTime.now();

        Map<Long, List<GovernanceRecommendationItemEntity>> byOwner = new HashMap<>();
        for (var item : allItems) {
            if (item.getOwnerId() != null) {
                byOwner.computeIfAbsent(item.getOwnerId(), k -> new ArrayList<>()).add(item);
            }
        }

        List<GovernanceCapacityForecastEntity> forecasts = new ArrayList<>();

        for (var entry : byOwner.entrySet()) {
            Long ownerId = entry.getKey();
            List<GovernanceRecommendationItemEntity> items = entry.getValue();
            String ownerName = items.get(0).getOwnerName() != null ? items.get(0).getOwnerName() : "Owner-" + ownerId;

            int open = 0, overdue = 0, completed7d = 0, incoming7d = 0;
            for (var item : items) {
                String ws = item.getWorkflowStatus();
                if ("OPEN".equals(ws) || "ACKNOWLEDGED".equals(ws)) open++;
                if (item.getDueAt() != null && item.getDueAt().isBefore(now)
                        && !"COMPLETED".equals(ws) && !"REJECTED".equals(ws)) overdue++;
                if (item.getResolvedAt() != null && item.getResolvedAt().isAfter(now.minusDays(7))) completed7d++;
                if (item.getCreateTime() != null && item.getCreateTime().isAfter(now.minusDays(7))) incoming7d++;
            }

            BigDecimal avgCompletedPerDay = BigDecimal.valueOf(completed7d).divide(BigDecimal.valueOf(7), 2, RoundingMode.HALF_UP);
            int projectedNewItems = Math.max(1, (int) Math.ceil((double) incoming7d / 7 * horizonDays));
            int projectedCompleted = (int) Math.floor(avgCompletedPerDay.doubleValue() * horizonDays);
            int projectedBacklog = Math.max(0, open + projectedNewItems - projectedCompleted);
            double overdueFactor = Math.min(1.0, Math.max(0.1, (double) overdue / Math.max(1, open)));
            int projectedOverdue = (int) Math.round(overdue + Math.max(0, projectedBacklog - open) * overdueFactor);

            String riskLevel;
            if (projectedOverdue >= 8) riskLevel = "CRITICAL";
            else if (projectedOverdue >= 3 || projectedBacklog > open * 1.5) riskLevel = "HIGH";
            else if (projectedOverdue > 0) riskLevel = "WATCH";
            else riskLevel = "LOW";

            GovernanceCapacityForecastEntity f = new GovernanceCapacityForecastEntity();
            f.setSnapshotDate(today);
            f.setForecastHorizonDays(horizonDays);
            f.setOwnerId(ownerId);
            f.setOwnerName(ownerName);
            f.setCurrentOpenCount(open);
            f.setCurrentOverdueCount(overdue);
            f.setAvgCompletedPerDay(avgCompletedPerDay);
            f.setProjectedNewItems(projectedNewItems);
            f.setProjectedCompletedItems(projectedCompleted);
            f.setProjectedBacklogCount(projectedBacklog);
            f.setProjectedOverdueCount(projectedOverdue);
            f.setCapacityRiskLevel(riskLevel);
            f.setSummaryText("Owner " + ownerName + " — " + horizonDays + "d forecast: backlog " + projectedBacklog + ", overdue " + projectedOverdue + " (" + riskLevel + ")");
            f.setCreateTime(LocalDateTime.now());
            forecasts.add(f);
        }

        for (var f : forecasts) {
            governanceCapacityForecastMapper.insert(f);
        }
    }

    @Transactional(readOnly = true)
    public List<GovernanceCapacityForecastResponse> getForecasts(Integer horizonDays) {
        LocalDate today = LocalDate.now();
        LambdaQueryWrapper<GovernanceCapacityForecastEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GovernanceCapacityForecastEntity::getSnapshotDate, today);
        if (horizonDays != null) wrapper.eq(GovernanceCapacityForecastEntity::getForecastHorizonDays, horizonDays);
        wrapper.orderByDesc(GovernanceCapacityForecastEntity::getProjectedOverdueCount).last("LIMIT 50");
        List<GovernanceCapacityForecastEntity> list = governanceCapacityForecastMapper.selectList(wrapper);
        if (list.isEmpty()) {
            wrapper = new LambdaQueryWrapper<>();
            wrapper.orderByDesc(GovernanceCapacityForecastEntity::getCreateTime);
            wrapper.last("LIMIT 50");
            list = governanceCapacityForecastMapper.selectList(wrapper);
        }
        return list.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public GovernanceCapacityDashboardResponse getDashboard(Integer horizonDays) {
        List<GovernanceCapacityForecastResponse> forecasts = getForecasts(horizonDays);
        int low = 0, watch = 0, high = 0, critical = 0, totalBacklog = 0, totalOverdue = 0;
        for (var f : forecasts) {
            switch (f.getCapacityRiskLevel()) {
                case "LOW" -> low++;
                case "WATCH" -> watch++;
                case "HIGH" -> high++;
                case "CRITICAL" -> critical++;
            }
            totalBacklog += f.getProjectedBacklogCount() != null ? f.getProjectedBacklogCount() : 0;
            totalOverdue += f.getProjectedOverdueCount() != null ? f.getProjectedOverdueCount() : 0;
        }

        List<GovernanceCapacityForecastResponse> sorted = new ArrayList<>(forecasts);
        sorted.sort(Comparator.comparingInt(f -> riskWeight(f.getCapacityRiskLevel())));
        sorted = sorted.stream().limit(5).collect(Collectors.toList());

        GovernanceCapacityDashboardResponse resp = new GovernanceCapacityDashboardResponse();
        resp.setSnapshotDate(forecasts.isEmpty() ? LocalDate.now() : forecasts.get(0).getSnapshotDate());
        resp.setOwnerCount(forecasts.size());
        resp.setLowRiskCount(low);
        resp.setWatchCount(watch);
        resp.setHighCount(high);
        resp.setCriticalCount(critical);
        resp.setTopRiskOwners(sorted);
        resp.setAverageProjectedBacklog(forecasts.isEmpty() ? 0 : totalBacklog / forecasts.size());
        resp.setAverageProjectedOverdue(forecasts.isEmpty() ? 0 : totalOverdue / forecasts.size());
        return resp;
    }

    private int riskWeight(String level) {
        if ("CRITICAL".equals(level)) return 0;
        if ("HIGH".equals(level)) return 1;
        if ("WATCH".equals(level)) return 2;
        return 3;
    }

    private GovernanceCapacityForecastResponse toResponse(GovernanceCapacityForecastEntity entity) {
        GovernanceCapacityForecastResponse r = new GovernanceCapacityForecastResponse();
        r.setId(entity.getId() != null ? entity.getId().toString() : null);
        r.setSnapshotDate(entity.getSnapshotDate());
        r.setForecastHorizonDays(entity.getForecastHorizonDays());
        r.setOwnerId(entity.getOwnerId() != null ? entity.getOwnerId().toString() : null);
        r.setOwnerName(entity.getOwnerName());
        r.setCurrentOpenCount(entity.getCurrentOpenCount());
        r.setCurrentOverdueCount(entity.getCurrentOverdueCount());
        r.setAvgCompletedPerDay(entity.getAvgCompletedPerDay());
        r.setProjectedNewItems(entity.getProjectedNewItems());
        r.setProjectedCompletedItems(entity.getProjectedCompletedItems());
        r.setProjectedBacklogCount(entity.getProjectedBacklogCount());
        r.setProjectedOverdueCount(entity.getProjectedOverdueCount());
        r.setCapacityRiskLevel(entity.getCapacityRiskLevel());
        r.setSummaryText(entity.getSummaryText());
        return r;
    }
}
