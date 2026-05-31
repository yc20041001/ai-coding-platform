package com.aicoding.platform.orchestration.application;

import com.aicoding.platform.orchestration.domain.*;
import com.aicoding.platform.orchestration.dto.GovernanceOperatorActionMemoryResponse;
import com.aicoding.platform.orchestration.dto.GovernanceWorkspaceSessionInsightResponse;
import com.aicoding.platform.orchestration.infrastructure.GovernanceWorkspaceSessionInsightMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class GovernanceSessionLearningService {

    private final GovernanceWorkspaceSessionInsightMapper insightMapper;
    private final GovernanceOperatorMemoryService memoryService;

    public GovernanceSessionLearningService(GovernanceWorkspaceSessionInsightMapper insightMapper,
                                             GovernanceOperatorMemoryService memoryService) {
        this.insightMapper = insightMapper;
        this.memoryService = memoryService;
    }

    @Transactional
    public void refreshInsight(String sessionIdStr) {
        Long sessionId = parseLong(sessionIdStr);
        var actions = memoryService.listActions(sessionIdStr);

        int total = actions.size();
        int accepted = (int) actions.stream().filter(a -> Boolean.TRUE.equals(a.getAcceptedFlag())).count();
        int dismissed = (int) actions.stream().filter(a -> !Boolean.TRUE.equals(a.getAcceptedFlag())
                && "DISMISS_NEXT_STEP".equals(a.getActionType())).count();
        int completed = (int) actions.stream().filter(a -> Boolean.TRUE.equals(a.getSuccessFlag())
                && "COMPLETE_GUIDED_TASK".equals(a.getActionType())).count();
        int blocked = (int) actions.stream().filter(a -> "UPDATE_GUIDED_TASK".equals(a.getActionType())
                && !Boolean.TRUE.equals(a.getSuccessFlag())).count();

        OptionalDouble avgDur = actions.stream()
                .filter(a -> a.getDurationSeconds() != null)
                .mapToInt(GovernanceOperatorActionMemoryResponse::getDurationSeconds).average();
        int avgDuration = (int) avgDur.orElse(0);

        double acceptanceRate = total > 0 ? (double) accepted / total * 100 : 0;
        double completionRate = total > 0 ? (double) completed / Math.max(1, accepted) * 100 : 0;
        double successRate = total > 0 ? (double) accepted / total * 100 : 0;

        double score = acceptanceRate * 0.35 + completionRate * 0.35
                + Math.max(0, 100 - avgDuration / 60.0) * 0.15 + successRate * 0.15;
        score = Math.max(0, Math.min(100, score));
        BigDecimal prodScore = BigDecimal.valueOf(score).setScale(2, RoundingMode.HALF_UP);

        // Dominant pattern
        String dominantPattern = extractDominantPattern(actions.stream().map(GovernanceOperatorActionMemoryResponse::getActionType)
                .collect(Collectors.toList()));

        deleteInsight(sessionId, "SESSION");
        GovernanceWorkspaceSessionInsightEntity insight = new GovernanceWorkspaceSessionInsightEntity();
        insight.setSessionId(sessionId); insight.setInsightWindow("SESSION");
        insight.setTotalActions(total); insight.setAcceptedRecommendationCount(accepted);
        insight.setDismissedRecommendationCount(dismissed); insight.setCompletedGuidedTaskCount(completed);
        insight.setBlockedGuidedTaskCount(blocked); insight.setAvgActionDurationSeconds(avgDuration);
        insight.setProductivityScore(prodScore); insight.setDominantActionPattern(dominantPattern);
        insight.setCapturedAt(LocalDateTime.now());
        insight.setSummaryMarkdown(buildSummary(sessionIdStr, total, accepted, completed, score, dominantPattern));
        insightMapper.insert(insight);
    }

    private void deleteInsight(Long sessionId, String window) {
        LambdaQueryWrapper<GovernanceWorkspaceSessionInsightEntity> d = new LambdaQueryWrapper<>();
        d.eq(GovernanceWorkspaceSessionInsightEntity::getSessionId, sessionId);
        d.eq(GovernanceWorkspaceSessionInsightEntity::getInsightWindow, window);
        insightMapper.delete(d);
    }

    private String extractDominantPattern(List<String> actions) {
        if (actions.size() < 2) return "INSUFFICIENT_DATA";
        // Simple pattern detection
        Map<String, Integer> patterns = new HashMap<>();
        for (int i = 0; i < actions.size() - 1; i++) {
            String pair = actions.get(i) + " -> " + actions.get(i + 1);
            patterns.merge(pair, 1, (a, b) -> a + b);
        }
        return patterns.entrySet().stream().max(Map.Entry.comparingByValue())
                .map(e -> e.getKey() + " (x" + e.getValue() + ")")
                .orElse("NO_PATTERN");
    }

    private String buildSummary(String sessionId, int total, int accepted, int completed, double score, String pattern) {
        return "Session " + sessionId + " — actions: " + total + ", accepted: " + accepted
                + ", completed: " + completed + ", productivity: " + String.format("%.0f", score)
                + ", dominant pattern: " + pattern;
    }

    @Transactional(readOnly = true)
    public List<GovernanceWorkspaceSessionInsightResponse> listInsights() {
        return insightMapper.selectList(new LambdaQueryWrapper<GovernanceWorkspaceSessionInsightEntity>()
                .orderByDesc(GovernanceWorkspaceSessionInsightEntity::getCapturedAt).last("LIMIT 50"))
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getDashboard() {
        var insights = listInsights();
        int totalSess = (int) insights.stream().map(GovernanceWorkspaceSessionInsightResponse::getSessionId).distinct().count();
        int totalActions = insights.stream().mapToInt(i -> intOrZero(i.getTotalActions())).sum();
        int totalAccepted = insights.stream().mapToInt(i -> intOrZero(i.getAcceptedRecommendationCount())).sum();
        double acceptanceRate = totalActions > 0 ? (double) totalAccepted / totalActions * 100 : 0;
        int totalCompleted = insights.stream().mapToInt(i -> intOrZero(i.getCompletedGuidedTaskCount())).sum();
        int totalTasks = totalAccepted;
        double completionRate = totalTasks > 0 ? (double) totalCompleted / totalTasks * 100 : 0;
        OptionalDouble avgDur = insights.stream()
                .filter(i -> i.getAvgActionDurationSeconds() != null)
                .mapToInt(GovernanceWorkspaceSessionInsightResponse::getAvgActionDurationSeconds).average();

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("totalSessions", totalSess); resp.put("totalActions", totalActions);
        resp.put("acceptanceRate", String.format("%.1f", acceptanceRate) + "%");
        resp.put("guidedTaskCompletionRate", String.format("%.1f", completionRate) + "%");
        resp.put("avgActionDurationSeconds", (int) avgDur.orElse(0));
        resp.put("latestSessionInsights", insights.stream().limit(5).collect(Collectors.toList()));
        resp.put("topActionPatterns", insights.stream()
                .filter(i -> i.getDominantActionPattern() != null)
                .map(GovernanceWorkspaceSessionInsightResponse::getDominantActionPattern)
                .distinct().limit(5).collect(Collectors.toList()));
        return resp;
    }

    @Transactional(readOnly = true)
    public String getReport() {
        var dash = getDashboard();
        StringBuilder md = new StringBuilder();
        md.append("# Governance Operator Learning Report\n\n");
        md.append("## Overview\n\n");
        md.append("- Total Sessions: ").append(dash.get("totalSessions")).append("\n");
        md.append("- Total Actions: ").append(dash.get("totalActions")).append("\n");
        md.append("- Acceptance Rate: ").append(dash.get("acceptanceRate")).append("\n");
        md.append("- Guided Task Completion Rate: ").append(dash.get("guidedTaskCompletionRate")).append("\n");
        md.append("- Avg Action Duration: ").append(dash.get("avgActionDurationSeconds")).append("s\n\n");
        md.append("## Top Action Patterns\n\n");
        @SuppressWarnings("unchecked")
        List<String> patterns = (List<String>) dash.get("topActionPatterns");
        for (String p : patterns) md.append("- ").append(p).append("\n");
        return md.toString();
    }

    private GovernanceWorkspaceSessionInsightResponse toResponse(GovernanceWorkspaceSessionInsightEntity e) {
        GovernanceWorkspaceSessionInsightResponse r = new GovernanceWorkspaceSessionInsightResponse();
        r.setId(e.getId() != null ? e.getId().toString() : null);
        r.setSessionId(e.getSessionId() != null ? e.getSessionId().toString() : null);
        r.setOperatorId(e.getOperatorId() != null ? e.getOperatorId().toString() : null);
        r.setOperatorName(e.getOperatorName()); r.setInsightWindow(e.getInsightWindow());
        r.setTotalActions(e.getTotalActions()); r.setAcceptedRecommendationCount(e.getAcceptedRecommendationCount());
        r.setDismissedRecommendationCount(e.getDismissedRecommendationCount());
        r.setCompletedGuidedTaskCount(e.getCompletedGuidedTaskCount());
        r.setBlockedGuidedTaskCount(e.getBlockedGuidedTaskCount());
        r.setAvgActionDurationSeconds(e.getAvgActionDurationSeconds());
        r.setProductivityScore(e.getProductivityScore());
        r.setDominantActionPattern(e.getDominantActionPattern());
        r.setSummaryMarkdown(e.getSummaryMarkdown());
        return r;
    }

    private static Long parseLong(String v) { try { return Long.valueOf(v); } catch (NumberFormatException e) { return 0L; } }

    private static int intOrZero(Integer value) { return value != null ? value : 0; }
}
