package com.aicoding.platform.orchestration.application;

import com.aicoding.platform.orchestration.domain.ReleaseConfidenceSnapshotEntity;
import com.aicoding.platform.orchestration.domain.ReleasePortfolioSnapshotEntity;
import com.aicoding.platform.orchestration.domain.ReleaseRolloutPlanEntity;
import com.aicoding.platform.orchestration.dto.MultiProjectGovernanceSummaryResponse;
import com.aicoding.platform.orchestration.dto.ReleasePortfolioDashboardResponse;
import com.aicoding.platform.orchestration.dto.ReleasePortfolioRankingResponse;
import com.aicoding.platform.orchestration.infrastructure.ReleaseConfidenceSnapshotMapper;
import com.aicoding.platform.orchestration.infrastructure.ReleasePortfolioSnapshotMapper;
import com.aicoding.platform.orchestration.infrastructure.ReleaseRolloutPlanMapper;
import com.aicoding.platform.project.infrastructure.ProjectMapper;
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
public class ReleasePortfolioGovernanceService {

    private final ReleasePortfolioSnapshotMapper releasePortfolioSnapshotMapper;
    private final ReleaseRolloutPlanMapper releaseRolloutPlanMapper;
    private final ReleaseConfidenceSnapshotMapper releaseConfidenceSnapshotMapper;
    private final ProjectMapper projectMapper;

    public ReleasePortfolioGovernanceService(ReleasePortfolioSnapshotMapper releasePortfolioSnapshotMapper,
                                              ReleaseRolloutPlanMapper releaseRolloutPlanMapper,
                                              ReleaseConfidenceSnapshotMapper releaseConfidenceSnapshotMapper,
                                              ProjectMapper projectMapper) {
        this.releasePortfolioSnapshotMapper = releasePortfolioSnapshotMapper;
        this.releaseRolloutPlanMapper = releaseRolloutPlanMapper;
        this.releaseConfidenceSnapshotMapper = releaseConfidenceSnapshotMapper;
        this.projectMapper = projectMapper;
    }

    @Transactional
    public void refreshPortfolio() {
        LocalDate today = LocalDate.now();

        // Delete existing snapshot for today
        LambdaQueryWrapper<ReleasePortfolioSnapshotEntity> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(ReleasePortfolioSnapshotEntity::getSnapshotDate, today);
        releasePortfolioSnapshotMapper.delete(deleteWrapper);

        // Get all plans with project_id, group by project in Java
        LambdaQueryWrapper<ReleaseRolloutPlanEntity> allPlansWrapper = new LambdaQueryWrapper<>();
        allPlansWrapper.isNotNull(ReleaseRolloutPlanEntity::getProjectId);
        allPlansWrapper.orderByDesc(ReleaseRolloutPlanEntity::getCreateTime);
        List<ReleaseRolloutPlanEntity> allPlans = releaseRolloutPlanMapper.selectList(allPlansWrapper);

        Map<Long, ReleaseRolloutPlanEntity> latestPlanByProject = new HashMap<>();
        for (ReleaseRolloutPlanEntity plan : allPlans) {
            Long pid = plan.getProjectId();
            if (pid != null && !latestPlanByProject.containsKey(pid)) {
                latestPlanByProject.put(pid, plan);
            }
        }

        if (latestPlanByProject.isEmpty()) return;

        List<ReleasePortfolioSnapshotEntity> snapshots = new ArrayList<>();

        for (Map.Entry<Long, ReleaseRolloutPlanEntity> entry : latestPlanByProject.entrySet()) {
            Long projectId = entry.getKey();
            ReleaseRolloutPlanEntity latestPlan = entry.getValue();

            // Get project name
            String projectName = "Project-" + projectId;
            try {
                com.aicoding.platform.project.domain.ProjectEntity project = projectMapper.selectById(projectId);
                if (project != null && project.getName() != null) {
                    projectName = project.getName();
                }
            } catch (Exception e) {
                // ignore
            }

            // Get latest confidence snapshot for this project
            LambdaQueryWrapper<ReleaseConfidenceSnapshotEntity> csWrapper = new LambdaQueryWrapper<>();
            csWrapper.eq(ReleaseConfidenceSnapshotEntity::getProjectId, projectId);
            csWrapper.orderByDesc(ReleaseConfidenceSnapshotEntity::getSnapshotTime);
            csWrapper.last("LIMIT 1");
            ReleaseConfidenceSnapshotEntity latestCs = releaseConfidenceSnapshotMapper.selectOne(csWrapper);

            ReleasePortfolioSnapshotEntity snap = new ReleasePortfolioSnapshotEntity();
            snap.setSnapshotDate(today);
            snap.setProjectId(projectId);
            snap.setProjectName(projectName);
            snap.setLatestReleaseLabel(latestPlan.getReleaseLabel());
            snap.setRolloutStatus(latestPlan.getRolloutStatus());

            if (latestCs != null) {
                snap.setConfidenceScore(latestCs.getConfidenceScore());
                snap.setConfidenceLevel(latestCs.getConfidenceLevel());
                snap.setBlockingIssueCount(latestCs.getBlockingIssueCount());
                snap.setWarningIssueCount(latestCs.getWarningIssueCount());
                snap.setOpenIncidentCount(latestCs.getOpenIncidentCount());
                snap.setActiveAlertCount(latestCs.getActiveAlertCount());
                snap.setFailedVerificationCount(latestCs.getFailedVerificationCount());
                snap.setRollbackReady(latestCs.getRollbackReady());
                snap.setSignoffCompletionRate(latestCs.getSignoffCompletionRate());
            } else {
                snap.setConfidenceScore(BigDecimal.ZERO);
                snap.setConfidenceLevel("NONE");
                snap.setBlockingIssueCount(0);
                snap.setWarningIssueCount(0);
                snap.setOpenIncidentCount(0);
                snap.setActiveAlertCount(0);
                snap.setFailedVerificationCount(0);
                snap.setRollbackReady(0);
                snap.setSignoffCompletionRate(BigDecimal.ZERO);
            }

            snap.setExpansionRecommendation(calculateRecommendation(snap));
            snap.setSummaryText(buildSummaryText(snap));
            snap.setCreateTime(LocalDateTime.now());
            snapshots.add(snap);
        }

        // Sort by confidence score desc, assign rank
        snapshots.sort(Comparator.comparing(ReleasePortfolioSnapshotEntity::getConfidenceScore,
                Comparator.nullsFirst(BigDecimal::compareTo)).reversed());
        for (int i = 0; i < snapshots.size(); i++) {
            snapshots.get(i).setPortfolioRank(i + 1);
        }

        for (ReleasePortfolioSnapshotEntity snap : snapshots) {
            releasePortfolioSnapshotMapper.insert(snap);
        }
    }

    @Transactional(readOnly = true)
    public ReleasePortfolioDashboardResponse getDashboard() {
        LocalDate today = LocalDate.now();
        List<ReleasePortfolioSnapshotEntity> snapshots = getTodaySnapshots(today);

        ReleasePortfolioDashboardResponse resp = new ReleasePortfolioDashboardResponse();
        resp.setSnapshotDate(today);
        resp.setProjectCount(snapshots.size());

        int high = 0, medium = 0, low = 0, critical = 0;
        int expandNow = 0, expandGuardrails = 0, hold = 0, block = 0;
        BigDecimal totalScore = BigDecimal.ZERO;

        for (ReleasePortfolioSnapshotEntity s : snapshots) {
            switch (s.getConfidenceLevel()) {
                case "HIGH" -> high++;
                case "MEDIUM" -> medium++;
                case "LOW" -> low++;
                case "CRITICAL" -> critical++;
            }
            switch (s.getExpansionRecommendation()) {
                case "EXPAND_NOW" -> expandNow++;
                case "EXPAND_WITH_GUARDRAILS" -> expandGuardrails++;
                case "HOLD" -> hold++;
                case "BLOCK" -> block++;
            }
            if (s.getConfidenceScore() != null) {
                totalScore = totalScore.add(s.getConfidenceScore());
            }
        }

        resp.setHighConfidenceCount(high);
        resp.setMediumConfidenceCount(medium);
        resp.setLowConfidenceCount(low);
        resp.setCriticalConfidenceCount(critical);
        resp.setExpandNowCount(expandNow);
        resp.setExpandWithGuardrailsCount(expandGuardrails);
        resp.setHoldCount(hold);
        resp.setBlockCount(block);

        if (!snapshots.isEmpty()) {
            resp.setAverageConfidenceScore(totalScore.divide(BigDecimal.valueOf(snapshots.size()), 2, RoundingMode.HALF_UP));
        } else {
            resp.setAverageConfidenceScore(BigDecimal.ZERO);
        }

        List<ReleasePortfolioRankingResponse> ranking = toRankingList(snapshots);
        resp.setTopProjects(ranking.stream().limit(3).collect(Collectors.toList()));
        List<ReleasePortfolioRankingResponse> reversed = new ArrayList<>(ranking);
        java.util.Collections.reverse(reversed);
        resp.setBottomProjects(reversed.stream().limit(3).collect(Collectors.toList()));

        return resp;
    }

    @Transactional(readOnly = true)
    public List<ReleasePortfolioRankingResponse> getRanking() {
        LocalDate today = LocalDate.now();
        List<ReleasePortfolioSnapshotEntity> snapshots = getTodaySnapshots(today);
        return toRankingList(snapshots);
    }

    @Transactional
    public MultiProjectGovernanceSummaryResponse getSummary() {
        refreshPortfolio();

        LocalDate today = LocalDate.now();
        List<ReleasePortfolioSnapshotEntity> snapshots = getTodaySnapshots(today);

        MultiProjectGovernanceSummaryResponse resp = new MultiProjectGovernanceSummaryResponse();
        resp.setSnapshotDate(today);
        resp.setTotalProjectCount(snapshots.size());

        int expandNow = 0, hold = 0, block = 0, guardrails = 0;
        BigDecimal totalScore = BigDecimal.ZERO;
        String improvingProject = null;
        String decliningProject = null;

        for (ReleasePortfolioSnapshotEntity s : snapshots) {
            switch (s.getExpansionRecommendation()) {
                case "EXPAND_NOW" -> expandNow++;
                case "EXPAND_WITH_GUARDRAILS" -> guardrails++;
                case "HOLD" -> hold++;
                case "BLOCK" -> block++;
            }
            if (s.getConfidenceScore() != null) {
                totalScore = totalScore.add(s.getConfidenceScore());
            }
        }

        resp.setExpandNowCount(expandNow);
        resp.setExpandWithGuardrailsCount(guardrails);
        resp.setHoldCount(hold);
        resp.setBlockCount(block);

        if (!snapshots.isEmpty()) {
            resp.setAverageConfidenceScore(totalScore.divide(BigDecimal.valueOf(snapshots.size()), 2, RoundingMode.HALF_UP));
        } else {
            resp.setAverageConfidenceScore(BigDecimal.ZERO);
        }

        // Riskiest projects (bottom 3 by confidence)
        List<ReleasePortfolioSnapshotEntity> sortedByScore = snapshots.stream()
                .filter(s -> s.getConfidenceScore() != null)
                .sorted(Comparator.comparing(ReleasePortfolioSnapshotEntity::getConfidenceScore))
                .collect(Collectors.toList());
        List<String> riskiest = sortedByScore.stream()
                .limit(3)
                .map(ReleasePortfolioSnapshotEntity::getProjectName)
                .collect(Collectors.toList());
        resp.setRiskiestProjects(riskiest);

        // improving/declining from previous snapshot
        LocalDate yesterday = today.minusDays(1);
        List<ReleasePortfolioSnapshotEntity> prevSnapshots = getTodaySnapshots(yesterday);
        if (!prevSnapshots.isEmpty()) {
            Map<Long, BigDecimal> prevScores = new HashMap<>();
            for (ReleasePortfolioSnapshotEntity ps : prevSnapshots) {
                if (ps.getConfidenceScore() != null) {
                    prevScores.put(ps.getProjectId(), ps.getConfidenceScore());
                }
            }
            BigDecimal maxImprove = null;
            BigDecimal maxDecline = null;
            for (ReleasePortfolioSnapshotEntity s : snapshots) {
                BigDecimal prev = prevScores.get(s.getProjectId());
                if (prev != null && s.getConfidenceScore() != null) {
                    BigDecimal delta = s.getConfidenceScore().subtract(prev);
                    if (maxImprove == null || delta.compareTo(maxImprove) > 0) {
                        maxImprove = delta;
                        improvingProject = s.getProjectName();
                    }
                    if (maxDecline == null || delta.compareTo(maxDecline) < 0) {
                        maxDecline = delta;
                        decliningProject = s.getProjectName();
                    }
                }
            }
        }
        resp.setImprovingProject(improvingProject);
        resp.setDecliningProject(decliningProject);

        // Summary markdown
        StringBuilder md = new StringBuilder();
        md.append("# Multi-Project Release Governance Summary\n\n");
        md.append("**Snapshot Date**: ").append(today).append("\n\n");
        md.append("**Total Projects**: ").append(snapshots.size()).append("\n\n");
        md.append("---\n\n");
        md.append("## Expansion Recommendation\n\n");
        md.append("- EXPAND_NOW: ").append(expandNow).append("\n");
        md.append("- EXPAND_WITH_GUARDRAILS: ").append(guardrails).append("\n");
        md.append("- HOLD: ").append(hold).append("\n");
        md.append("- BLOCK: ").append(block).append("\n\n");
        md.append("## Risk Overview\n\n");
        md.append("- Average Confidence: ").append(resp.getAverageConfidenceScore()).append("\n");
        md.append("- Riskiest Projects: ").append(String.join(", ", riskiest)).append("\n");
        if (improvingProject != null) md.append("- Most Improved: ").append(improvingProject).append("\n");
        if (decliningProject != null) md.append("- Most Declined: ").append(decliningProject).append("\n");
        resp.setSummaryMarkdown(md.toString());

        return resp;
    }

    private String calculateRecommendation(ReleasePortfolioSnapshotEntity snap) {
        BigDecimal score = snap.getConfidenceScore() != null ? snap.getConfidenceScore() : BigDecimal.ZERO;
        int blockingIssues = intOrZero(snap.getBlockingIssueCount());
        int rollbackReady = intOrZero(snap.getRollbackReady());

        if (score.compareTo(BigDecimal.valueOf(85)) >= 0 && blockingIssues == 0 && rollbackReady == 1) {
            return "EXPAND_NOW";
        } else if (score.compareTo(BigDecimal.valueOf(65)) >= 0) {
            return "EXPAND_WITH_GUARDRAILS";
        } else if (score.compareTo(BigDecimal.valueOf(40)) >= 0) {
            return "HOLD";
        } else {
            return "BLOCK";
        }
    }

    private String buildSummaryText(ReleasePortfolioSnapshotEntity snap) {
        return "Project " + snap.getProjectName() + " — confidence " + snap.getConfidenceScore()
                + "/100 (" + snap.getConfidenceLevel() + "), "
                + "blocking issues: " + snap.getBlockingIssueCount() + ", "
                + "rollback ready: " + (snap.getRollbackReady() != null && snap.getRollbackReady() == 1 ? "yes" : "no");
    }

    private List<ReleasePortfolioSnapshotEntity> getTodaySnapshots(LocalDate date) {
        LambdaQueryWrapper<ReleasePortfolioSnapshotEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReleasePortfolioSnapshotEntity::getSnapshotDate, date);
        wrapper.orderByDesc(ReleasePortfolioSnapshotEntity::getConfidenceScore);
        return releasePortfolioSnapshotMapper.selectList(wrapper);
    }

    private List<ReleasePortfolioRankingResponse> toRankingList(List<ReleasePortfolioSnapshotEntity> snapshots) {
        return snapshots.stream().map(s -> {
            ReleasePortfolioRankingResponse r = new ReleasePortfolioRankingResponse();
            r.setProjectId(s.getProjectId() != null ? s.getProjectId().toString() : null);
            r.setProjectName(s.getProjectName());
            r.setLatestReleaseLabel(s.getLatestReleaseLabel());
            r.setConfidenceScore(s.getConfidenceScore());
            r.setConfidenceLevel(s.getConfidenceLevel());
            r.setPortfolioRank(s.getPortfolioRank());
            r.setExpansionRecommendation(s.getExpansionRecommendation());
            r.setBlockingIssueCount(s.getBlockingIssueCount());
            r.setWarningIssueCount(s.getWarningIssueCount());
            r.setRollbackReady(s.getRollbackReady() != null && s.getRollbackReady() == 1);
            r.setSignoffCompletionRate(s.getSignoffCompletionRate());
            r.setSummaryText(s.getSummaryText());
            return r;
        }).collect(Collectors.toList());
    }

    private static int intOrZero(Integer value) {
        if (value == null) {
            return 0;
        }
        return value;
    }
}
