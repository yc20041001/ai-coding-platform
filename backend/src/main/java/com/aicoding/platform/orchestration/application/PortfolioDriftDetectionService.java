package com.aicoding.platform.orchestration.application;

import com.aicoding.platform.orchestration.domain.PortfolioDriftSnapshotEntity;
import com.aicoding.platform.orchestration.domain.ReleasePortfolioSnapshotEntity;
import com.aicoding.platform.orchestration.dto.PortfolioDriftDashboardResponse;
import com.aicoding.platform.orchestration.dto.PortfolioDriftSnapshotResponse;
import com.aicoding.platform.orchestration.infrastructure.PortfolioDriftSnapshotMapper;
import com.aicoding.platform.orchestration.infrastructure.ReleasePortfolioSnapshotMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PortfolioDriftDetectionService {

    private final PortfolioDriftSnapshotMapper portfolioDriftSnapshotMapper;
    private final ReleasePortfolioSnapshotMapper releasePortfolioSnapshotMapper;

    public PortfolioDriftDetectionService(PortfolioDriftSnapshotMapper portfolioDriftSnapshotMapper,
                                           ReleasePortfolioSnapshotMapper releasePortfolioSnapshotMapper) {
        this.portfolioDriftSnapshotMapper = portfolioDriftSnapshotMapper;
        this.releasePortfolioSnapshotMapper = releasePortfolioSnapshotMapper;
    }

    @Transactional
    public void refreshDrift() {
        LocalDate today = LocalDate.now();

        // Delete existing drift for today
        LambdaQueryWrapper<PortfolioDriftSnapshotEntity> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(PortfolioDriftSnapshotEntity::getSnapshotDate, today);
        portfolioDriftSnapshotMapper.delete(deleteWrapper);

        // Get today's portfolio snapshots
        List<ReleasePortfolioSnapshotEntity> currentSnapshots = getSnapshotsByDate(today);
        if (currentSnapshots.isEmpty()) return;

        // Get yesterday's snapshots for comparison
        LocalDate yesterday = today.minusDays(1);
        List<ReleasePortfolioSnapshotEntity> prevSnapshots = getSnapshotsByDate(yesterday);

        // Index previous snapshots by project ID
        Map<Long, ReleasePortfolioSnapshotEntity> prevMap = new HashMap<>();
        for (ReleasePortfolioSnapshotEntity ps : prevSnapshots) {
            if (ps.getProjectId() != null) {
                prevMap.put(ps.getProjectId(), ps);
            }
        }

        List<PortfolioDriftSnapshotEntity> drifts = new ArrayList<>();

        for (ReleasePortfolioSnapshotEntity current : currentSnapshots) {
            Long projectId = current.getProjectId();
            if (projectId == null) continue;

            ReleasePortfolioSnapshotEntity previous = prevMap.get(projectId);

            BigDecimal confidenceDelta;
            BigDecimal signoffDelta;
            BigDecimal verificationDelta;
            int rollbackReadinessChanged;

            if (previous != null) {
                BigDecimal currConf = current.getConfidenceScore() != null ? current.getConfidenceScore() : BigDecimal.ZERO;
                BigDecimal prevConf = previous.getConfidenceScore() != null ? previous.getConfidenceScore() : BigDecimal.ZERO;
                confidenceDelta = currConf.subtract(prevConf);

                BigDecimal currSign = current.getSignoffCompletionRate() != null ? current.getSignoffCompletionRate() : BigDecimal.ZERO;
                BigDecimal prevSign = previous.getSignoffCompletionRate() != null ? previous.getSignoffCompletionRate() : BigDecimal.ZERO;
                signoffDelta = currSign.subtract(prevSign);

                int currVer = intOrZero(current.getFailedVerificationCount());
                int prevVer = intOrZero(previous.getFailedVerificationCount());
                verificationDelta = BigDecimal.valueOf(currVer - prevVer);

                int currRb = intOrZero(current.getRollbackReady());
                int prevRb = intOrZero(previous.getRollbackReady());
                rollbackReadinessChanged = (currRb != prevRb) ? 1 : 0;
            } else {
                confidenceDelta = BigDecimal.ZERO;
                signoffDelta = BigDecimal.ZERO;
                verificationDelta = BigDecimal.ZERO;
                rollbackReadinessChanged = 0;
            }

            // Calculate drift score
            BigDecimal driftScore = calculateDriftScore(confidenceDelta, signoffDelta, verificationDelta, rollbackReadinessChanged);
            String driftLevel = toDriftLevel(driftScore);

            PortfolioDriftSnapshotEntity drift = new PortfolioDriftSnapshotEntity();
            drift.setSnapshotDate(today);
            drift.setProjectId(projectId);
            drift.setProjectName(current.getProjectName());
            drift.setDriftScore(driftScore);
            drift.setDriftLevel(driftLevel);
            drift.setConfidenceDelta(confidenceDelta);
            drift.setSignoffDelta(signoffDelta);
            drift.setVerificationDelta(verificationDelta);
            drift.setRollbackReadinessChanged(rollbackReadinessChanged);
            drift.setSummaryText(buildDriftSummary(projectId, current.getProjectName(), driftLevel, driftScore, confidenceDelta));
            drift.setCreateTime(LocalDateTime.now());
            drifts.add(drift);
        }

        for (PortfolioDriftSnapshotEntity drift : drifts) {
            portfolioDriftSnapshotMapper.insert(drift);
        }
    }

    private BigDecimal calculateDriftScore(BigDecimal confidenceDelta, BigDecimal signoffDelta,
                                            BigDecimal verificationDelta, int rollbackReadinessChanged) {
        BigDecimal score = BigDecimal.ZERO;
        score = score.add(confidenceDelta.abs().multiply(BigDecimal.valueOf(0.6)));
        score = score.add(signoffDelta.abs().multiply(BigDecimal.valueOf(0.2)));
        score = score.add(verificationDelta.abs().multiply(BigDecimal.valueOf(0.15)));
        if (rollbackReadinessChanged == 1) {
            score = score.add(BigDecimal.valueOf(15));
        }
        return score.setScale(2, RoundingMode.HALF_UP);
    }

    private String toDriftLevel(BigDecimal score) {
        if (score.compareTo(BigDecimal.valueOf(40)) >= 0) return "CRITICAL";
        if (score.compareTo(BigDecimal.valueOf(20)) >= 0) return "HIGH";
        if (score.compareTo(BigDecimal.valueOf(5)) >= 0) return "WATCH";
        return "STABLE";
    }

    private String buildDriftSummary(Long projectId, String projectName, String level, BigDecimal score, BigDecimal confidenceDelta) {
        return "Project " + projectName + " — drift " + score + "/100 (" + level + ")"
                + ", confidence delta: " + confidenceDelta;
    }

    @Transactional(readOnly = true)
    public List<PortfolioDriftSnapshotResponse> getDriftList() {
        LocalDate today = LocalDate.now();
        LambdaQueryWrapper<PortfolioDriftSnapshotEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PortfolioDriftSnapshotEntity::getSnapshotDate, today);
        wrapper.orderByDesc(PortfolioDriftSnapshotEntity::getDriftScore);
        List<PortfolioDriftSnapshotEntity> list = portfolioDriftSnapshotMapper.selectList(wrapper);

        if (list.isEmpty()) {
            wrapper = new LambdaQueryWrapper<>();
            wrapper.orderByDesc(PortfolioDriftSnapshotEntity::getCreateTime);
            wrapper.last("LIMIT 50");
            list = portfolioDriftSnapshotMapper.selectList(wrapper);
        }

        return list.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PortfolioDriftDashboardResponse getDriftDashboard() {
        LocalDate today = LocalDate.now();
        List<PortfolioDriftSnapshotEntity> list;
        LambdaQueryWrapper<PortfolioDriftSnapshotEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PortfolioDriftSnapshotEntity::getSnapshotDate, today);
        list = portfolioDriftSnapshotMapper.selectList(wrapper);

        if (list.isEmpty()) {
            wrapper = new LambdaQueryWrapper<>();
            wrapper.orderByDesc(PortfolioDriftSnapshotEntity::getCreateTime);
            wrapper.last("LIMIT 50");
            list = portfolioDriftSnapshotMapper.selectList(wrapper);
        }

        int stable = 0, watch = 0, high = 0, critical = 0;
        BigDecimal maxDrift = BigDecimal.ZERO;
        String trendSummary = "No significant drift detected";

        for (PortfolioDriftSnapshotEntity d : list) {
            switch (d.getDriftLevel()) {
                case "STABLE" -> stable++;
                case "WATCH" -> watch++;
                case "HIGH" -> high++;
                case "CRITICAL" -> critical++;
            }
            if (d.getDriftScore() != null && d.getDriftScore().compareTo(maxDrift) > 0) {
                maxDrift = d.getDriftScore();
            }
        }

        if (!list.isEmpty()) {
            if (critical > 0 || high > 0) {
                trendSummary = "Detected " + critical + " critical and " + high + " high drifts, max drift score: " + maxDrift;
            } else if (watch > 0) {
                trendSummary = watch + " projects under watch, max drift score: " + maxDrift;
            }
        }

        List<PortfolioDriftSnapshotResponse> topDrifts = list.stream()
                .sorted(Comparator.comparing(PortfolioDriftSnapshotEntity::getDriftScore,
                        Comparator.nullsFirst(BigDecimal::compareTo)).reversed())
                .limit(5)
                .map(this::toResponse)
                .collect(Collectors.toList());

        PortfolioDriftDashboardResponse resp = new PortfolioDriftDashboardResponse();
        resp.setSnapshotDate(list.isEmpty() ? today : list.get(0).getSnapshotDate());
        resp.setStableCount(stable);
        resp.setWatchCount(watch);
        resp.setHighCount(high);
        resp.setCriticalCount(critical);
        resp.setTopDriftProjects(topDrifts);
        resp.setDriftTrendSummary(trendSummary);
        return resp;
    }

    private List<ReleasePortfolioSnapshotEntity> getSnapshotsByDate(LocalDate date) {
        LambdaQueryWrapper<ReleasePortfolioSnapshotEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReleasePortfolioSnapshotEntity::getSnapshotDate, date);
        return releasePortfolioSnapshotMapper.selectList(wrapper);
    }

    private PortfolioDriftSnapshotResponse toResponse(PortfolioDriftSnapshotEntity entity) {
        PortfolioDriftSnapshotResponse resp = new PortfolioDriftSnapshotResponse();
        resp.setId(entity.getId() != null ? entity.getId().toString() : null);
        resp.setSnapshotDate(entity.getSnapshotDate());
        resp.setProjectId(entity.getProjectId() != null ? entity.getProjectId().toString() : null);
        resp.setProjectName(entity.getProjectName());
        resp.setDriftScore(entity.getDriftScore());
        resp.setDriftLevel(entity.getDriftLevel());
        resp.setBaselineTemplateKey(entity.getBaselineTemplateKey());
        resp.setConfidenceDelta(entity.getConfidenceDelta());
        resp.setSignoffDelta(entity.getSignoffDelta());
        resp.setVerificationDelta(entity.getVerificationDelta());
        resp.setRollbackReadinessChanged(entity.getRollbackReadinessChanged());
        resp.setSummaryText(entity.getSummaryText());
        resp.setDetailJson(entity.getDetailJson());
        return resp;
    }

    private static int intOrZero(Integer value) {
        return value != null ? value : 0;
    }
}
