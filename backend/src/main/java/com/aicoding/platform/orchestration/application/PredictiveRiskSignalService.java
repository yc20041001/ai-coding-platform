package com.aicoding.platform.orchestration.application;

import com.aicoding.platform.orchestration.domain.*;
import com.aicoding.platform.orchestration.dto.*;
import com.aicoding.platform.orchestration.infrastructure.GovernanceCapacityForecastMapper;
import com.aicoding.platform.orchestration.infrastructure.GovernanceWaiverRequestMapper;
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
public class PredictiveRiskSignalService {

    private final PredictiveRiskSignalMapper predictiveRiskSignalMapper;
    private final GovernanceCapacityForecastMapper governanceCapacityForecastMapper;
    private final GovernanceWaiverRequestMapper governanceWaiverRequestMapper;

    public PredictiveRiskSignalService(PredictiveRiskSignalMapper predictiveRiskSignalMapper,
                                        GovernanceCapacityForecastMapper governanceCapacityForecastMapper,
                                        GovernanceWaiverRequestMapper governanceWaiverRequestMapper) {
        this.predictiveRiskSignalMapper = predictiveRiskSignalMapper;
        this.governanceCapacityForecastMapper = governanceCapacityForecastMapper;
        this.governanceWaiverRequestMapper = governanceWaiverRequestMapper;
    }

    @Transactional
    public void refreshSignals() {
        LocalDate today = LocalDate.now();
        LambdaQueryWrapper<PredictiveRiskSignalEntity> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(PredictiveRiskSignalEntity::getSnapshotDate, today);
        predictiveRiskSignalMapper.delete(deleteWrapper);

        List<PredictiveRiskSignalEntity> signals = new ArrayList<>();

        // 1. Owner overload forecast signals from capacity forecasts
        LambdaQueryWrapper<GovernanceCapacityForecastEntity> forecastQuery = new LambdaQueryWrapper<>();
        forecastQuery.eq(GovernanceCapacityForecastEntity::getSnapshotDate, today);
        forecastQuery.eq(GovernanceCapacityForecastEntity::getForecastHorizonDays, 7);
        List<GovernanceCapacityForecastEntity> forecasts = governanceCapacityForecastMapper.selectList(forecastQuery);

        for (var f : forecasts) {
            if ("HIGH".equals(f.getCapacityRiskLevel()) || "CRITICAL".equals(f.getCapacityRiskLevel())) {
                double riskScore = Math.min(100, f.getProjectedOverdueCount() * 12 + f.getProjectedBacklogCount() * 3);
                double prob = Math.min(95, 50 + f.getProjectedOverdueCount() * 5);

                PredictiveRiskSignalEntity s = new PredictiveRiskSignalEntity();
                s.setSnapshotDate(today);
                s.setTargetType("OWNER");
                s.setTargetId(f.getOwnerId());
                s.setTargetName(f.getOwnerName());
                s.setSignalType("OWNER_OVERLOAD_FORECAST");
                s.setRiskLevel(f.getCapacityRiskLevel());
                s.setRiskScore(BigDecimal.valueOf(Math.min(100, riskScore)).setScale(2, RoundingMode.HALF_UP));
                s.setProbabilityScore(BigDecimal.valueOf(Math.min(100, prob)).setScale(2, RoundingMode.HALF_UP));
                s.setTimeHorizonDays(7);
                s.setSummary("Owner overload forecast: projected backlog " + f.getProjectedBacklogCount() + ", overdue " + f.getProjectedOverdueCount());
                s.setCreateTime(LocalDateTime.now());
                signals.add(s);
            }
        }

        // 2. Waiver expiry cluster signals
        LambdaQueryWrapper<GovernanceWaiverRequestEntity> waiverQuery = new LambdaQueryWrapper<>();
        waiverQuery.eq(GovernanceWaiverRequestEntity::getWaiverStatus, "APPROVED");
        waiverQuery.isNotNull(GovernanceWaiverRequestEntity::getExpiresAt);
        List<GovernanceWaiverRequestEntity> activeWaivers = governanceWaiverRequestMapper.selectList(waiverQuery);

        long expiringSoon7d = activeWaivers.stream()
                .filter(w -> w.getExpiresAt().isBefore(LocalDateTime.now().plusDays(7))
                        && w.getExpiresAt().isAfter(LocalDateTime.now()))
                .count();

        if (expiringSoon7d >= 2) {
            PredictiveRiskSignalEntity s = new PredictiveRiskSignalEntity();
            s.setSnapshotDate(today);
            s.setTargetType("WAIVER_GROUP");
            s.setTargetName("Waiver Group");
            s.setSignalType("WAIVER_EXPIRY_CLUSTER");
            s.setRiskLevel(expiringSoon7d >= 5 ? "CRITICAL" : "HIGH");
            double riskScore = Math.min(100, expiringSoon7d * 15);
            s.setRiskScore(BigDecimal.valueOf(riskScore).setScale(2, RoundingMode.HALF_UP));
            s.setProbabilityScore(BigDecimal.valueOf(Math.min(90, 40 + expiringSoon7d * 8)).setScale(2, RoundingMode.HALF_UP));
            s.setTimeHorizonDays(7);
            s.setSummary(expiringSoon7d + " waivers expiring within 7 days");
            s.setCreateTime(LocalDateTime.now());
            signals.add(s);
        }

        // 3. Overdue trend and throughput deficit signals based on forecasts
        OptionalDouble avgBacklog = forecasts.stream().mapToInt(f -> f.getProjectedBacklogCount() != null ? f.getProjectedBacklogCount() : 0).average();
        OptionalDouble avgOverdue = forecasts.stream().mapToInt(f -> f.getProjectedOverdueCount() != null ? f.getProjectedOverdueCount() : 0).average();

        if (avgOverdue.isPresent() && avgOverdue.getAsDouble() > 2) {
            PredictiveRiskSignalEntity s = new PredictiveRiskSignalEntity();
            s.setSnapshotDate(today);
            s.setTargetType("PORTFOLIO");
            s.setTargetName("Portfolio");
            s.setSignalType("OVERDUE_TREND_FORECAST");
            s.setRiskLevel(avgOverdue.getAsDouble() >= 5 ? "CRITICAL" : "HIGH");
            s.setRiskScore(BigDecimal.valueOf(Math.min(100, avgOverdue.getAsDouble() * 10)).setScale(2, RoundingMode.HALF_UP));
            s.setProbabilityScore(BigDecimal.valueOf(Math.min(90, 30 + avgOverdue.getAsDouble() * 5)).setScale(2, RoundingMode.HALF_UP));
            s.setTimeHorizonDays(7);
            s.setSummary("Overdue trend: avg projected overdue " + String.format("%.1f", avgOverdue.getAsDouble()) + " per owner");
            s.setCreateTime(LocalDateTime.now());
            signals.add(s);
        }

        if (avgBacklog.isPresent() && avgBacklog.getAsDouble() > 5) {
            PredictiveRiskSignalEntity s = new PredictiveRiskSignalEntity();
            s.setSnapshotDate(today);
            s.setTargetType("PORTFOLIO");
            s.setTargetName("Portfolio");
            s.setSignalType("THROUGHPUT_DEFICIT");
            s.setRiskLevel(avgBacklog.getAsDouble() >= 10 ? "CRITICAL" : "HIGH");
            s.setRiskScore(BigDecimal.valueOf(Math.min(100, avgBacklog.getAsDouble() * 5)).setScale(2, RoundingMode.HALF_UP));
            s.setProbabilityScore(BigDecimal.valueOf(Math.min(90, 30 + avgBacklog.getAsDouble() * 3)).setScale(2, RoundingMode.HALF_UP));
            s.setTimeHorizonDays(7);
            s.setSummary("Throughput deficit: avg projected backlog " + String.format("%.1f", avgBacklog.getAsDouble()) + " per owner");
            s.setCreateTime(LocalDateTime.now());
            signals.add(s);
        }

        for (var s : signals) {
            predictiveRiskSignalMapper.insert(s);
        }
    }

    @Transactional(readOnly = true)
    public List<PredictiveRiskSignalResponse> getSignals() {
        LocalDate today = LocalDate.now();
        LambdaQueryWrapper<PredictiveRiskSignalEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PredictiveRiskSignalEntity::getSnapshotDate, today);
        wrapper.orderByDesc(PredictiveRiskSignalEntity::getRiskScore);
        List<PredictiveRiskSignalEntity> list = predictiveRiskSignalMapper.selectList(wrapper);
        if (list.isEmpty()) {
            wrapper = new LambdaQueryWrapper<>();
            wrapper.orderByDesc(PredictiveRiskSignalEntity::getCreateTime);
            wrapper.last("LIMIT 50");
            list = predictiveRiskSignalMapper.selectList(wrapper);
        }
        return list.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PredictiveRiskDashboardResponse getDashboard() {
        List<PredictiveRiskSignalResponse> all = getSignals();
        PredictiveRiskDashboardResponse resp = new PredictiveRiskDashboardResponse();
        resp.setSnapshotDate(all.isEmpty() ? LocalDate.now() : all.get(0).getSnapshotDate());
        resp.setSignalCount(all.size());
        resp.setHighSignalCount((int) all.stream().filter(s -> "HIGH".equals(s.getRiskLevel())).count());
        resp.setCriticalSignalCount((int) all.stream().filter(s -> "CRITICAL".equals(s.getRiskLevel())).count());
        resp.setOwnerRiskSignals((int) all.stream().filter(s -> "OWNER".equals(s.getTargetType())).count());
        resp.setProjectRiskSignals((int) all.stream().filter(s -> "PROJECT".equals(s.getTargetType())).count());
        resp.setPortfolioRiskSignals((int) all.stream().filter(s -> "PORTFOLIO".equals(s.getTargetType())).count());
        resp.setTopSignals(all.stream().limit(10).collect(Collectors.toList()));
        return resp;
    }

    private PredictiveRiskSignalResponse toResponse(PredictiveRiskSignalEntity entity) {
        PredictiveRiskSignalResponse r = new PredictiveRiskSignalResponse();
        r.setId(entity.getId() != null ? entity.getId().toString() : null);
        r.setSnapshotDate(entity.getSnapshotDate());
        r.setTargetType(entity.getTargetType());
        r.setTargetId(entity.getTargetId() != null ? entity.getTargetId().toString() : null);
        r.setTargetName(entity.getTargetName());
        r.setSignalType(entity.getSignalType());
        r.setRiskLevel(entity.getRiskLevel());
        r.setRiskScore(entity.getRiskScore());
        r.setProbabilityScore(entity.getProbabilityScore());
        r.setTimeHorizonDays(entity.getTimeHorizonDays());
        r.setSummary(entity.getSummary());
        r.setDetail(entity.getDetail());
        return r;
    }
}
