package com.aicoding.platform.orchestration.application;

import com.aicoding.platform.orchestration.domain.*;
import com.aicoding.platform.orchestration.dto.GovernanceOptimizationSuggestionResponse;
import com.aicoding.platform.orchestration.infrastructure.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class GovernanceRecipeOptimizationService {

    private final GovernanceOptimizationSuggestionMapper suggestionMapper;
    private final GovernanceRecipeEffectivenessSnapshotMapper effectivenessMapper;
    private final GovernanceRemediationRecipeMapper recipeMapper;

    public GovernanceRecipeOptimizationService(GovernanceOptimizationSuggestionMapper suggestionMapper,
                                                GovernanceRecipeEffectivenessSnapshotMapper effectivenessMapper,
                                                GovernanceRemediationRecipeMapper recipeMapper) {
        this.suggestionMapper = suggestionMapper;
        this.effectivenessMapper = effectivenessMapper;
        this.recipeMapper = recipeMapper;
    }

    @Transactional
    public void refreshSuggestions() {
        LocalDate today = LocalDate.now();
        LambdaQueryWrapper<GovernanceOptimizationSuggestionEntity> delW = new LambdaQueryWrapper<>();
        delW.eq(GovernanceOptimizationSuggestionEntity::getSnapshotDate, today);
        suggestionMapper.delete(delW);

        List<GovernanceOptimizationSuggestionEntity> suggestions = new ArrayList<>();

        // Get today's effectiveness snapshots
        LambdaQueryWrapper<GovernanceRecipeEffectivenessSnapshotEntity> w = new LambdaQueryWrapper<>();
        w.eq(GovernanceRecipeEffectivenessSnapshotEntity::getSnapshotDate, today);
        List<GovernanceRecipeEffectivenessSnapshotEntity> snapshots = effectivenessMapper.selectList(w);

        if (snapshots.isEmpty()) {
            w = new LambdaQueryWrapper<>();
            w.orderByDesc(GovernanceRecipeEffectivenessSnapshotEntity::getCreateTime).last("LIMIT 50");
            snapshots = effectivenessMapper.selectList(w);
        }

        for (var snap : snapshots) {
            int usage = snap.getUsageCount() != null ? snap.getUsageCount() : 0;
            double score = snap.getEffectivenessScore() != null ? snap.getEffectivenessScore().doubleValue() : 0;
            double success = snap.getSuccessRate() != null ? snap.getSuccessRate().doubleValue() : 0;

            // High value - promote
            if (usage >= 3 && success >= 70 && score >= 60) {
                GovernanceOptimizationSuggestionEntity s = new GovernanceOptimizationSuggestionEntity();
                s.setSnapshotDate(today);
                s.setSuggestionType("PROMOTE_RECIPE");
                s.setPriority("P1");
                s.setTargetType("RECIPE");
                s.setTargetKey(snap.getRecipeKey());
                s.setCurrentMetricValue("Score: " + String.format("%.0f", score) + ", Usage: " + usage);
                s.setSuggestedAction("Promote recipe '" + snap.getRecipeName() + "' to higher visibility");
                s.setExpectedImpactText("Increase usage and effectiveness of high-value recipe");
                s.setRationaleText("High effectiveness score (" + String.format("%.0f", score) + ") and usage (" + usage + ").");
                suggestions.add(s);
            }

            // Low value - prune
            if (usage >= 2 && (score < 35 || success < 40)) {
                GovernanceOptimizationSuggestionEntity s = new GovernanceOptimizationSuggestionEntity();
                s.setSnapshotDate(today);
                s.setSuggestionType("PRUNE_RECIPE");
                s.setPriority("P2");
                s.setTargetType("RECIPE");
                s.setTargetKey(snap.getRecipeKey());
                s.setCurrentMetricValue("Score: " + String.format("%.0f", score) + ", Success: " + String.format("%.0f", success) + "%");
                s.setSuggestedAction("Consider deprecating or merging recipe '" + snap.getRecipeName() + "'");
                s.setExpectedImpactText("Reduce maintenance overhead and clean up recipe library");
                s.setRationaleText("Low effectiveness score or success rate indicates poor value.");
                suggestions.add(s);
            }

            // High usage but moderate score means refine playbook
            if (usage >= 4 && score < 50) {
                GovernanceOptimizationSuggestionEntity s = new GovernanceOptimizationSuggestionEntity();
                s.setSnapshotDate(today);
                s.setSuggestionType("REFINE_PLAYBOOK");
                s.setPriority("P1");
                s.setTargetType("PLAYBOOK");
                s.setTargetKey(snap.getRecipeKey());
                s.setCurrentMetricValue("Score: " + String.format("%.0f", score) + ", Usage: " + usage);
                s.setSuggestedAction("Refine playbook steps for recipe '" + snap.getRecipeName() + "'");
                s.setExpectedImpactText("Improve completion rate and reduce failure rate");
                s.setRationaleText("High usage but moderate score indicates room for playbook improvement.");
                suggestions.add(s);
            }
        }

        // Check for duplicate recipes (by category overlap)
        List<GovernanceRemediationRecipeEntity> recipes = recipeMapper.selectList(null);
        Map<String, List<GovernanceRemediationRecipeEntity>> byCategory = new HashMap<>();
        for (var r : recipes) {
            if (r.getRecommendationCategory() != null) {
                byCategory.computeIfAbsent(r.getRecommendationCategory(), k -> new ArrayList<>()).add(r);
            }
        }
        for (var entry : byCategory.entrySet()) {
            if (entry.getValue().size() >= 3) {
                GovernanceOptimizationSuggestionEntity s = new GovernanceOptimizationSuggestionEntity();
                s.setSnapshotDate(today);
                s.setSuggestionType("MERGE_DUPLICATE_RECIPES");
                s.setPriority("P3");
                s.setTargetType("RECIPE");
                s.setTargetKey("category:" + entry.getKey());
                s.setCurrentMetricValue(entry.getValue().size() + " recipes for category '" + entry.getKey() + "'");
                s.setSuggestedAction("Merge " + entry.getValue().size() + " recipes in category '" + entry.getKey() + "'");
                s.setExpectedImpactText("Reduce recipe duplication and improve maintainability");
                s.setRationaleText(entry.getValue().size() + " recipes share the same recommendation category. Consider merging.");
                suggestions.add(s);
            }
        }

        for (var s : suggestions) suggestionMapper.insert(s);
    }

    @Transactional(readOnly = true)
    public List<GovernanceOptimizationSuggestionResponse> listSuggestions() {
        LocalDate today = LocalDate.now();
        LambdaQueryWrapper<GovernanceOptimizationSuggestionEntity> w = new LambdaQueryWrapper<>();
        w.eq(GovernanceOptimizationSuggestionEntity::getSnapshotDate, today);
        w.orderByDesc(GovernanceOptimizationSuggestionEntity::getPriority);
        List<GovernanceOptimizationSuggestionEntity> list = suggestionMapper.selectList(w);
        if (list.isEmpty()) {
            w = new LambdaQueryWrapper<>();
            w.orderByDesc(GovernanceOptimizationSuggestionEntity::getCreateTime).last("LIMIT 50");
            list = suggestionMapper.selectList(w);
        }
        return list.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getDashboard() {
        List<GovernanceOptimizationSuggestionResponse> all = listSuggestions();
        int high = 0, promote = 0, prune = 0, refine = 0;
        for (var s : all) {
            if ("P1".equals(s.getPriority()) || "P0".equals(s.getPriority())) high++;
            switch (s.getSuggestionType()) {
                case "PROMOTE_RECIPE" -> promote++;
                case "PRUNE_RECIPE" -> prune++;
                case "REFINE_PLAYBOOK" -> refine++;
            }
        }
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("snapshotDate", LocalDate.now().toString());
        resp.put("suggestionCount", all.size());
        resp.put("highPrioritySuggestionCount", high);
        resp.put("promoteSuggestionCount", promote);
        resp.put("pruneSuggestionCount", prune);
        resp.put("refinePlaybookCount", refine);
        resp.put("topSuggestions", all.stream().limit(10).collect(Collectors.toList()));
        return resp;
    }

    @Transactional(readOnly = true)
    public String getReport() {
        var suggestions = listSuggestions();
        StringBuilder md = new StringBuilder();
        md.append("# Governance Optimization Report\n\n");
        md.append("**Date**: ").append(LocalDate.now()).append("\n\n");
        md.append("## Suggestions (").append(suggestions.size()).append(")\n\n");
        for (var s : suggestions.stream().limit(20).collect(Collectors.toList())) {
            md.append("- [").append(s.getPriority()).append("] ")
              .append(s.getSuggestionType()).append(": ")
              .append(s.getExpectedImpactText()).append("\n");
        }
        return md.toString();
    }

    private GovernanceOptimizationSuggestionResponse toResponse(GovernanceOptimizationSuggestionEntity e) {
        GovernanceOptimizationSuggestionResponse r = new GovernanceOptimizationSuggestionResponse();
        r.setId(e.getId() != null ? e.getId().toString() : null);
        r.setSnapshotDate(e.getSnapshotDate()); r.setSuggestionType(e.getSuggestionType());
        r.setPriority(e.getPriority()); r.setTargetType(e.getTargetType()); r.setTargetKey(e.getTargetKey());
        r.setCurrentMetricValue(e.getCurrentMetricValue()); r.setSuggestedAction(e.getSuggestedAction());
        r.setExpectedImpactText(e.getExpectedImpactText()); r.setRationaleText(e.getRationaleText());
        return r;
    }
}
