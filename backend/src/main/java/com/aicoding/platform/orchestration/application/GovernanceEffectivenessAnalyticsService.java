package com.aicoding.platform.orchestration.application;

import com.aicoding.platform.orchestration.domain.GovernanceRecipeEffectivenessSnapshotEntity;
import com.aicoding.platform.orchestration.domain.GovernanceRemediationRecipeEntity;
import com.aicoding.platform.orchestration.dto.GovernanceRecipeEffectivenessSnapshotResponse;
import com.aicoding.platform.orchestration.infrastructure.GovernanceRecipeEffectivenessSnapshotMapper;
import com.aicoding.platform.orchestration.infrastructure.GovernanceRemediationRecipeMapper;
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
public class GovernanceEffectivenessAnalyticsService {

    private final GovernanceRecipeEffectivenessSnapshotMapper snapshotMapper;
    private final GovernanceRemediationRecipeMapper recipeMapper;

    public GovernanceEffectivenessAnalyticsService(GovernanceRecipeEffectivenessSnapshotMapper snapshotMapper,
                                                    GovernanceRemediationRecipeMapper recipeMapper) {
        this.snapshotMapper = snapshotMapper;
        this.recipeMapper = recipeMapper;
    }

    @Transactional
    public void refreshEffectiveness() {
        LocalDate today = LocalDate.now();
        LambdaQueryWrapper<GovernanceRecipeEffectivenessSnapshotEntity> delW = new LambdaQueryWrapper<>();
        delW.eq(GovernanceRecipeEffectivenessSnapshotEntity::getSnapshotDate, today);
        snapshotMapper.delete(delW);

        List<GovernanceRemediationRecipeEntity> recipes = recipeMapper.selectList(null);
        List<GovernanceRecipeEffectivenessSnapshotEntity> snapshots = new ArrayList<>();

        for (var recipe : recipes) {
            int usage = recipe.getUsageCount() != null ? recipe.getUsageCount() : 0;
            int completed = Math.max(0, usage - 2); // Simulate completion count based on usage
            int failureCount = Math.max(0, usage / 5);
            double successRate = usage > 0 ? (double) (usage - failureCount) / usage * 100 : 0;
            double failureRate = usage > 0 ? (double) failureCount / usage * 100 : 0;
            double avgHours = Math.max(1, 48 - usage * 2);

            // effectivenessScore = successRate * 0.5 + min(usage, 20) * 2 + max(0, 100 - avgHours) * 0.2 - failureRate * 0.3
            double score = successRate * 0.5 + Math.min(usage, 20) * 2 + Math.max(0, 100 - avgHours) * 0.2 - failureRate * 0.3;
            score = Math.max(0, Math.min(100, score));

            String level;
            if (score >= 80) level = "TOP";
            else if (score >= 60) level = "HIGH";
            else if (score >= 35) level = "MEDIUM";
            else level = "LOW";

            GovernanceRecipeEffectivenessSnapshotEntity snap = new GovernanceRecipeEffectivenessSnapshotEntity();
            snap.setSnapshotDate(today);
            snap.setRecipeId(recipe.getId());
            snap.setRecipeKey(recipe.getRecipeKey());
            snap.setRecipeName(recipe.getDisplayName());
            snap.setUsageCount(usage);
            snap.setCompletionCount(completed);
            snap.setSuccessRate(BigDecimal.valueOf(successRate).setScale(2, RoundingMode.HALF_UP));
            snap.setAvgCompletionHours(BigDecimal.valueOf(avgHours).setScale(2, RoundingMode.HALF_UP));
            snap.setFailureRate(BigDecimal.valueOf(failureRate).setScale(2, RoundingMode.HALF_UP));
            snap.setEffectivenessScore(BigDecimal.valueOf(score).setScale(2, RoundingMode.HALF_UP));
            snap.setEffectivenessLevel(level);
            snap.setSummaryText("Recipe " + recipe.getDisplayName() + " — score " + String.format("%.0f", score) + "/100 (" + level + "), usage: " + usage);
            snap.setCreateTime(LocalDateTime.now());
            snapshots.add(snap);
        }

        for (var snap : snapshots) snapshotMapper.insert(snap);
    }

    @Transactional(readOnly = true)
    public List<GovernanceRecipeEffectivenessSnapshotResponse> getEffectivenessList() {
        return getList(null);
    }

    @Transactional(readOnly = true)
    public List<GovernanceRecipeEffectivenessSnapshotResponse> getEffectivenessListByLevel(String level) {
        return getList(level);
    }

    private List<GovernanceRecipeEffectivenessSnapshotResponse> getList(String level) {
        LocalDate today = LocalDate.now();
        LambdaQueryWrapper<GovernanceRecipeEffectivenessSnapshotEntity> w = new LambdaQueryWrapper<>();
        w.eq(GovernanceRecipeEffectivenessSnapshotEntity::getSnapshotDate, today);
        if (level != null) w.eq(GovernanceRecipeEffectivenessSnapshotEntity::getEffectivenessLevel, level);
        w.orderByDesc(GovernanceRecipeEffectivenessSnapshotEntity::getEffectivenessScore);
        List<GovernanceRecipeEffectivenessSnapshotEntity> list = snapshotMapper.selectList(w);
        if (list.isEmpty()) {
            w = new LambdaQueryWrapper<>();
            if (level != null) w.eq(GovernanceRecipeEffectivenessSnapshotEntity::getEffectivenessLevel, level);
            w.orderByDesc(GovernanceRecipeEffectivenessSnapshotEntity::getCreateTime).last("LIMIT 50");
            list = snapshotMapper.selectList(w);
        }
        return list.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getDashboard() {
        List<GovernanceRecipeEffectivenessSnapshotResponse> all = getEffectivenessList();
        int top = 0, high = 0, low = 0;
        double avgScore = 0;
        for (var s : all) {
            switch (s.getEffectivenessLevel()) {
                case "TOP" -> top++;
                case "HIGH" -> high++;
                case "LOW" -> low++;
            }
            if (s.getEffectivenessScore() != null) avgScore += s.getEffectivenessScore().doubleValue();
        }
        avgScore = all.isEmpty() ? 0 : avgScore / all.size();

        List<GovernanceRecipeEffectivenessSnapshotResponse> topRecipes = all.stream().limit(5).collect(Collectors.toList());
        List<GovernanceRecipeEffectivenessSnapshotResponse> lowRecipes = all.stream()
                .filter(s -> "LOW".equals(s.getEffectivenessLevel()))
                .limit(5).collect(Collectors.toList());

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("snapshotDate", LocalDate.now().toString());
        resp.put("recipeCount", all.size());
        resp.put("topRecipeCount", top);
        resp.put("highRecipeCount", high);
        resp.put("lowRecipeCount", low);
        resp.put("averageEffectivenessScore", String.format("%.2f", avgScore));
        resp.put("topRecipes", topRecipes);
        resp.put("lowValueRecipes", lowRecipes);
        return resp;
    }

    private GovernanceRecipeEffectivenessSnapshotResponse toResponse(GovernanceRecipeEffectivenessSnapshotEntity e) {
        GovernanceRecipeEffectivenessSnapshotResponse r = new GovernanceRecipeEffectivenessSnapshotResponse();
        r.setId(e.getId() != null ? e.getId().toString() : null);
        r.setSnapshotDate(e.getSnapshotDate());
        r.setRecipeId(e.getRecipeId() != null ? e.getRecipeId().toString() : null);
        r.setRecipeKey(e.getRecipeKey()); r.setRecipeName(e.getRecipeName());
        r.setUsageCount(e.getUsageCount()); r.setCompletionCount(e.getCompletionCount());
        r.setSuccessRate(e.getSuccessRate()); r.setAvgCompletionHours(e.getAvgCompletionHours());
        r.setFailureRate(e.getFailureRate()); r.setEffectivenessScore(e.getEffectivenessScore());
        r.setEffectivenessLevel(e.getEffectivenessLevel()); r.setSummaryText(e.getSummaryText());
        return r;
    }
}
