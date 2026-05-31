package com.aicoding.platform.orchestration.application;

import com.aicoding.platform.orchestration.domain.*;
import com.aicoding.platform.orchestration.dto.GovernanceCopilotTuningSnapshotResponse;
import com.aicoding.platform.orchestration.infrastructure.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class GovernanceCopilotTuningService {

    private final GovernanceCopilotTuningSnapshotMapper snapshotMapper;
    private final GovernanceOperatorFeedbackMapper feedbackMapper;
    private final GovernanceAdaptiveGuidanceSignalMapper signalMapper;

    public GovernanceCopilotTuningService(GovernanceCopilotTuningSnapshotMapper snapshotMapper,
                                           GovernanceOperatorFeedbackMapper feedbackMapper,
                                           GovernanceAdaptiveGuidanceSignalMapper signalMapper) {
        this.snapshotMapper = snapshotMapper;
        this.feedbackMapper = feedbackMapper;
        this.signalMapper = signalMapper;
    }

    @Transactional
    public void refreshSnapshot() {
        List<GovernanceOperatorFeedbackEntity> allFeedback = feedbackMapper.selectList(null);
        List<GovernanceAdaptiveGuidanceSignalEntity> allSignals = signalMapper.selectList(null);

        int totalFeedback = allFeedback.size();
        long accepted = allFeedback.stream().filter(f -> f.getAcceptedFlag() != null && f.getAcceptedFlag() == 1).count();
        long dismissed = allFeedback.stream().filter(f -> f.getAcceptedFlag() == null || f.getAcceptedFlag() == 0).count();
        double acceptRate = totalFeedback > 0 ? (double) accepted / totalFeedback * 100 : 0;
        double dismissRate = totalFeedback > 0 ? (double) dismissed / totalFeedback * 100 : 0;
        double avgRating = allFeedback.stream().filter(f -> f.getFeedbackRating() != null)
                .mapToInt(GovernanceOperatorFeedbackEntity::getFeedbackRating).average().orElse(0);

        Optional<String> topType = allSignals.stream().filter(s -> "SUGGESTION_TYPE_WEIGHT".equals(s.getSignalType()))
                .max(Comparator.comparing(GovernanceAdaptiveGuidanceSignalEntity::getWeightScore))
                .map(GovernanceAdaptiveGuidanceSignalEntity::getSuggestionType);
        Optional<String> weakestType = allSignals.stream().filter(s -> "SUGGESTION_TYPE_WEIGHT".equals(s.getSignalType()))
                .min(Comparator.comparing(GovernanceAdaptiveGuidanceSignalEntity::getWeightScore))
                .map(GovernanceAdaptiveGuidanceSignalEntity::getSuggestionType);

        double confidence = Math.min(totalFeedback, 50) * 1.2 + acceptRate * 0.25 + avgRating * 8;

        String window = "DAY_14";
        LambdaQueryWrapper<GovernanceCopilotTuningSnapshotEntity> delW = new LambdaQueryWrapper<>();
        delW.eq(GovernanceCopilotTuningSnapshotEntity::getSnapshotWindow, window);
        snapshotMapper.delete(delW);

        GovernanceCopilotTuningSnapshotEntity snap = new GovernanceCopilotTuningSnapshotEntity();
        snap.setSnapshotWindow(window); snap.setTotalFeedbackCount(totalFeedback);
        snap.setAcceptanceRate(BigDecimal.valueOf(acceptRate).setScale(2, RoundingMode.HALF_UP));
        snap.setDismissalRate(BigDecimal.valueOf(dismissRate).setScale(2, RoundingMode.HALF_UP));
        snap.setAvgFeedbackRating(BigDecimal.valueOf(avgRating).setScale(2, RoundingMode.HALF_UP));
        topType.ifPresent(snap::setTopSuggestionType);
        weakestType.ifPresent(snap::setWeakestSuggestionType);
        snap.setTuningConfidenceScore(BigDecimal.valueOf(Math.min(100, confidence)).setScale(2, RoundingMode.HALF_UP));
        snap.setCapturedAt(LocalDateTime.now());

        StringBuilder md = new StringBuilder();
        md.append("# Copilot Tuning Snapshot\n\n");
        md.append("Window: ").append(window).append("\n\n");
        md.append("- Feedback: ").append(totalFeedback).append("\n");
        md.append("- Acceptance: ").append(String.format("%.1f", acceptRate)).append("%\n");
        md.append("- Avg Rating: ").append(String.format("%.1f", avgRating)).append("/5\n");
        md.append("- Confidence: ").append(String.format("%.0f", Math.min(100, confidence))).append("\n");
        snap.setSummaryMarkdown(md.toString());
        snapshotMapper.insert(snap);
    }

    @Transactional(readOnly = true)
    public List<GovernanceCopilotTuningSnapshotResponse> listSnapshots() {
        return snapshotMapper.selectList(new LambdaQueryWrapper<GovernanceCopilotTuningSnapshotEntity>()
                .orderByDesc(GovernanceCopilotTuningSnapshotEntity::getCapturedAt).last("LIMIT 20"))
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getDashboard() {
        var snapshots = listSnapshots();
        var latest = snapshots.isEmpty() ? null : snapshots.get(0);
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("snapshotCount", snapshots.size());
        resp.put("latestSnapshot", latest);
        return resp;
    }

    @Transactional(readOnly = true)
    public String getReport() {
        var dash = getDashboard();
        StringBuilder md = new StringBuilder();
        md.append("# Copilot Tuning Report\n\n");
        @SuppressWarnings("unchecked")
        Map<String, Object> dashMap = (Map<String, Object>) dash;
        md.append("Snapshots: ").append(dashMap.get("snapshotCount")).append("\n\n");
        if (dashMap.get("latestSnapshot") != null) {
            var snap = (GovernanceCopilotTuningSnapshotResponse) dashMap.get("latestSnapshot");
            md.append("Latest Snapshot:\n");
            md.append("- Feedback: ").append(snap.getTotalFeedbackCount()).append("\n");
            md.append("- Acceptance: ").append(snap.getAcceptanceRate()).append("%\n");
            md.append("- Dismissal: ").append(snap.getDismissalRate()).append("%\n");
            md.append("- Avg Rating: ").append(snap.getAvgFeedbackRating()).append("/5\n");
            md.append("- Confidence: ").append(snap.getTuningConfidenceScore()).append("\n");
        }
        return md.toString();
    }

    private GovernanceCopilotTuningSnapshotResponse toResponse(GovernanceCopilotTuningSnapshotEntity e) {
        GovernanceCopilotTuningSnapshotResponse r = new GovernanceCopilotTuningSnapshotResponse();
        r.setId(e.getId() != null ? e.getId().toString() : null);
        r.setSnapshotWindow(e.getSnapshotWindow()); r.setTotalFeedbackCount(e.getTotalFeedbackCount());
        r.setAcceptanceRate(e.getAcceptanceRate()); r.setDismissalRate(e.getDismissalRate());
        r.setAvgFeedbackRating(e.getAvgFeedbackRating()); r.setTopSuggestionType(e.getTopSuggestionType());
        r.setWeakestSuggestionType(e.getWeakestSuggestionType()); r.setTopFocusMode(e.getTopFocusMode());
        r.setWeakestFocusMode(e.getWeakestFocusMode());
        r.setTuningConfidenceScore(e.getTuningConfidenceScore()); r.setSummaryMarkdown(e.getSummaryMarkdown());
        return r;
    }
}
