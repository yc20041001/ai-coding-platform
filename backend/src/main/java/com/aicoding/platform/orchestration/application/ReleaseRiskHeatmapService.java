package com.aicoding.platform.orchestration.application;

import com.aicoding.platform.orchestration.domain.ModelCostAlertEntity;
import com.aicoding.platform.orchestration.domain.PrReviewQualityRecordEntity;
import com.aicoding.platform.orchestration.domain.ReleasePortfolioSnapshotEntity;
import com.aicoding.platform.orchestration.domain.ReleaseRiskHeatmapSnapshotEntity;
import com.aicoding.platform.orchestration.dto.ReleaseRiskHeatmapCellResponse;
import com.aicoding.platform.orchestration.dto.ReleaseRiskHeatmapResponse;
import com.aicoding.platform.orchestration.infrastructure.ModelCostAlertMapper;
import com.aicoding.platform.orchestration.infrastructure.PrReviewQualityRecordMapper;
import com.aicoding.platform.orchestration.infrastructure.ReleasePortfolioSnapshotMapper;
import com.aicoding.platform.orchestration.infrastructure.ReleaseRiskHeatmapSnapshotMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ReleaseRiskHeatmapService {

    private final ReleaseRiskHeatmapSnapshotMapper releaseRiskHeatmapSnapshotMapper;
    private final ReleasePortfolioSnapshotMapper releasePortfolioSnapshotMapper;
    private final ModelCostAlertMapper modelCostAlertMapper;
    private final PrReviewQualityRecordMapper prReviewQualityRecordMapper;

    private static final List<String> ALL_CATEGORIES = Arrays.asList(
            "INCIDENT", "ALERT", "VERIFICATION", "ROLLOUT", "SIGNOFF", "COST", "PR_QUALITY"
    );

    public ReleaseRiskHeatmapService(ReleaseRiskHeatmapSnapshotMapper releaseRiskHeatmapSnapshotMapper,
                                      ReleasePortfolioSnapshotMapper releasePortfolioSnapshotMapper,
                                      ModelCostAlertMapper modelCostAlertMapper,
                                      PrReviewQualityRecordMapper prReviewQualityRecordMapper) {
        this.releaseRiskHeatmapSnapshotMapper = releaseRiskHeatmapSnapshotMapper;
        this.releasePortfolioSnapshotMapper = releasePortfolioSnapshotMapper;
        this.modelCostAlertMapper = modelCostAlertMapper;
        this.prReviewQualityRecordMapper = prReviewQualityRecordMapper;
    }

    @Transactional
    public void refreshHeatmap() {
        LocalDate today = LocalDate.now();

        // Delete existing heatmap for today
        LambdaQueryWrapper<ReleaseRiskHeatmapSnapshotEntity> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(ReleaseRiskHeatmapSnapshotEntity::getSnapshotDate, today);
        releaseRiskHeatmapSnapshotMapper.delete(deleteWrapper);

        // Get today's portfolio snapshots
        LambdaQueryWrapper<ReleasePortfolioSnapshotEntity> portfolioWrapper = new LambdaQueryWrapper<>();
        portfolioWrapper.eq(ReleasePortfolioSnapshotEntity::getSnapshotDate, today);
        List<ReleasePortfolioSnapshotEntity> snapshots = releasePortfolioSnapshotMapper.selectList(portfolioWrapper);

        // If no snapshots for today, get latest snapshot per project
        if (snapshots.isEmpty()) {
            LambdaQueryWrapper<ReleasePortfolioSnapshotEntity> latestWrapper = new LambdaQueryWrapper<>();
            latestWrapper.orderByDesc(ReleasePortfolioSnapshotEntity::getSnapshotDate);
            snapshots = releasePortfolioSnapshotMapper.selectList(latestWrapper);

            // Keep only latest per project
            Map<Long, ReleasePortfolioSnapshotEntity> latestPerProject = new HashMap<>();
            for (ReleasePortfolioSnapshotEntity s : snapshots) {
                if (!latestPerProject.containsKey(s.getProjectId())) {
                    latestPerProject.put(s.getProjectId(), s);
                }
            }
            snapshots = new ArrayList<>(latestPerProject.values());
        }

        if (snapshots.isEmpty()) return;

        // For COST and PR_QUALITY, collect counts per project
        Map<Long, Integer> costAlertCounts = new HashMap<>();
        List<ModelCostAlertEntity> costAlerts = modelCostAlertMapper.selectList(
                new LambdaQueryWrapper<ModelCostAlertEntity>()
                        .eq(ModelCostAlertEntity::getStatus, "OPEN"));
        for (ModelCostAlertEntity ca : costAlerts) {
            if (ca.getProjectId() != null) {
                costAlertCounts.merge(ca.getProjectId(), 1, (a, b) -> a + b);
            }
        }

        Map<Long, Integer> qualityWarnCounts = new HashMap<>();
        List<PrReviewQualityRecordEntity> prRecords = prReviewQualityRecordMapper.selectList(
                new LambdaQueryWrapper<PrReviewQualityRecordEntity>()
                        .eq(PrReviewQualityRecordEntity::getHumanFeedbackStatus, "DISPUTED"));
        for (PrReviewQualityRecordEntity pr : prRecords) {
            if (pr.getProjectId() != null) {
                qualityWarnCounts.merge(pr.getProjectId(), 1, (a, b) -> a + b);
            }
        }

        List<ReleaseRiskHeatmapSnapshotEntity> cells = new ArrayList<>();

        for (ReleasePortfolioSnapshotEntity snap : snapshots) {
            Long projectId = snap.getProjectId();
            if (projectId == null) continue;

            BigDecimal signoffRate = snap.getSignoffCompletionRate() != null
                    ? snap.getSignoffCompletionRate() : BigDecimal.ZERO;
            int costCount = costAlertCounts.getOrDefault(projectId, 0);
            int qualityCount = qualityWarnCounts.getOrDefault(projectId, 0);

            addCell(cells, today, projectId, "INCIDENT",
                    intOrZero(snap.getOpenIncidentCount()) * 20);
            addCell(cells, today, projectId, "ALERT",
                    intOrZero(snap.getActiveAlertCount()) * 10);
            addCell(cells, today, projectId, "VERIFICATION",
                    intOrZero(snap.getFailedVerificationCount()) * 15);
            addCell(cells, today, projectId, "ROLLOUT",
                    snap.getRollbackReady() != null && snap.getRollbackReady() == 1 ? 0 : 25, snap.getRollbackReady() != null && snap.getRollbackReady() == 1 ? 0 : 1);
            addCell(cells, today, projectId, "SIGNOFF",
                    BigDecimal.valueOf(100).subtract(signoffRate)
                            .multiply(BigDecimal.valueOf(0.2)).doubleValue());
            addCell(cells, today, projectId, "COST", costCount * 12, costCount);
            addCell(cells, today, projectId, "PR_QUALITY", qualityCount * 8, qualityCount);
        }

        for (ReleaseRiskHeatmapSnapshotEntity cell : cells) {
            releaseRiskHeatmapSnapshotMapper.insert(cell);
        }
    }

    @Transactional(readOnly = true)
    public ReleaseRiskHeatmapResponse getHeatmap() {
        LocalDate today = LocalDate.now();

        LambdaQueryWrapper<ReleaseRiskHeatmapSnapshotEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReleaseRiskHeatmapSnapshotEntity::getSnapshotDate, today);
        List<ReleaseRiskHeatmapSnapshotEntity> cells = releaseRiskHeatmapSnapshotMapper.selectList(wrapper);

        if (cells.isEmpty()) {
            // Try latest available date
            LambdaQueryWrapper<ReleaseRiskHeatmapSnapshotEntity> latestWrapper = new LambdaQueryWrapper<>();
            latestWrapper.orderByDesc(ReleaseRiskHeatmapSnapshotEntity::getSnapshotDate);
            latestWrapper.last("LIMIT 1");
            ReleaseRiskHeatmapSnapshotEntity latest = releaseRiskHeatmapSnapshotMapper.selectOne(latestWrapper);
            if (latest != null) {
                LambdaQueryWrapper<ReleaseRiskHeatmapSnapshotEntity> reQuery = new LambdaQueryWrapper<>();
                reQuery.eq(ReleaseRiskHeatmapSnapshotEntity::getSnapshotDate, latest.getSnapshotDate());
                cells = releaseRiskHeatmapSnapshotMapper.selectList(reQuery);
            }
        }

        // Get project names from portfolio
        Map<Long, String> projectNames = new HashMap<>();
        if (!cells.isEmpty()) {
            LambdaQueryWrapper<ReleasePortfolioSnapshotEntity> pWrapper = new LambdaQueryWrapper<>();
            pWrapper.eq(ReleasePortfolioSnapshotEntity::getSnapshotDate, cells.get(0).getSnapshotDate());
            List<ReleasePortfolioSnapshotEntity> portfolios = releasePortfolioSnapshotMapper.selectList(pWrapper);
            for (ReleasePortfolioSnapshotEntity p : portfolios) {
                projectNames.put(p.getProjectId(), p.getProjectName());
            }
        }

        ReleaseRiskHeatmapResponse resp = new ReleaseRiskHeatmapResponse();
        resp.setSnapshotDate(cells.isEmpty() ? today : cells.get(0).getSnapshotDate());
        resp.setCategories(ALL_CATEGORIES);

        List<ReleaseRiskHeatmapCellResponse> cellResponses = cells.stream().map(c -> {
            ReleaseRiskHeatmapCellResponse cr = new ReleaseRiskHeatmapCellResponse();
            cr.setProjectId(c.getProjectId() != null ? c.getProjectId().toString() : null);
            cr.setProjectName(projectNames.getOrDefault(c.getProjectId(), "Project-" + c.getProjectId()));
            cr.setRiskCategory(c.getRiskCategory());
            cr.setRiskScore(c.getRiskScore());
            cr.setRiskLevel(c.getRiskLevel());
            cr.setSourceCount(c.getSourceCount());
            return cr;
        }).collect(Collectors.toList());
        resp.setCells(cellResponses);

        return resp;
    }

    private void addCell(List<ReleaseRiskHeatmapSnapshotEntity> cells, LocalDate date,
                          Long projectId, String category, double rawScore) {
        addCell(cells, date, projectId, category, rawScore, 1);
    }

    private void addCell(List<ReleaseRiskHeatmapSnapshotEntity> cells, LocalDate date,
                          Long projectId, String category, double rawScore, int sourceCount) {
        double normalized = Math.max(0, Math.min(100, rawScore));

        ReleaseRiskHeatmapSnapshotEntity cell = new ReleaseRiskHeatmapSnapshotEntity();
        cell.setSnapshotDate(date);
        cell.setProjectId(projectId);
        cell.setRiskCategory(category);
        cell.setRiskScore(BigDecimal.valueOf(normalized).setScale(2, java.math.RoundingMode.HALF_UP));
        cell.setRiskLevel(toRiskLevel(normalized));
        cell.setSourceCount(sourceCount);
        cell.setCreateTime(LocalDateTime.now());
        cells.add(cell);
    }

    private String toRiskLevel(double score) {
        if (score >= 80) return "CRITICAL";
        if (score >= 50) return "HIGH";
        if (score >= 20) return "MEDIUM";
        return "LOW";
    }

    private static int intOrZero(Integer value) {
        if (value == null) {
            return 0;
        }
        return value;
    }
}
