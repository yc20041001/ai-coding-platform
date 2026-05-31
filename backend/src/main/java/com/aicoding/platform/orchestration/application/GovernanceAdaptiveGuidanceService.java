package com.aicoding.platform.orchestration.application;

import com.aicoding.platform.orchestration.domain.GovernanceAdaptiveGuidanceSignalEntity;
import com.aicoding.platform.orchestration.domain.GovernanceOperatorFeedbackEntity;
import com.aicoding.platform.orchestration.dto.GovernanceAdaptiveGuidanceSignalResponse;
import com.aicoding.platform.orchestration.infrastructure.GovernanceAdaptiveGuidanceSignalMapper;
import com.aicoding.platform.orchestration.infrastructure.GovernanceOperatorFeedbackMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class GovernanceAdaptiveGuidanceService {

    private final GovernanceAdaptiveGuidanceSignalMapper signalMapper;
    private final GovernanceOperatorFeedbackMapper feedbackMapper;

    public GovernanceAdaptiveGuidanceService(GovernanceAdaptiveGuidanceSignalMapper signalMapper,
                                              GovernanceOperatorFeedbackMapper feedbackMapper) {
        this.signalMapper = signalMapper;
        this.feedbackMapper = feedbackMapper;
    }

    @Transactional
    public void refreshSignals() {
        signalMapper.delete(new LambdaQueryWrapper<>());

        List<GovernanceOperatorFeedbackEntity> allFeedback = feedbackMapper.selectList(null);
        if (allFeedback.isEmpty()) return;

        List<GovernanceAdaptiveGuidanceSignalEntity> signals = new ArrayList<>();

        // By suggestion type
        Map<String, List<GovernanceOperatorFeedbackEntity>> byType = allFeedback.stream()
                .filter(f -> f.getSuggestionType() != null)
                .collect(Collectors.groupingBy(GovernanceOperatorFeedbackEntity::getSuggestionType));
        for (var entry : byType.entrySet()) {
            signals.add(computeSignal("SUGGESTION_TYPE_WEIGHT", entry.getKey(), entry.getValue(), null, null));
        }

        // By focus mode (using suggestion_type as proxy since we don't have direct focus mode in feedback)
        Map<String, List<GovernanceOperatorFeedbackEntity>> byTarget = allFeedback.stream()
                .collect(Collectors.groupingBy(GovernanceOperatorFeedbackEntity::getFeedbackTargetType));
        for (var entry : byTarget.entrySet()) {
            signals.add(computeSignal("FOCUS_MODE_WEIGHT", entry.getKey(), entry.getValue(), null, null));
        }

        // Dismissal risk signal
        long dismissed = allFeedback.stream().filter(f -> f.getAcceptedFlag() == null || f.getAcceptedFlag() == 0).count();
        if (allFeedback.size() >= 3 && (double) dismissed / allFeedback.size() >= 0.5) {
            GovernanceAdaptiveGuidanceSignalEntity s = new GovernanceAdaptiveGuidanceSignalEntity();
            s.setSignalType("DISMISSAL_RISK_SIGNAL"); s.setSignalLevel("DOWNRANK");
            s.setWeightScore(BigDecimal.valueOf(20));
            s.setAcceptanceRate(BigDecimal.ZERO); s.setCompletionRate(BigDecimal.ZERO);
            s.setAvgFeedbackRating(BigDecimal.ZERO);
            s.setRationaleText("Dismissal rate >= 50% across " + allFeedback.size() + " feedback records");
            s.setCapturedAt(LocalDateTime.now());
            signals.add(s);
        }

        for (var s : signals) signalMapper.insert(s);
    }

    private GovernanceAdaptiveGuidanceSignalEntity computeSignal(String type, String key, List<GovernanceOperatorFeedbackEntity> items,
                                                                   String focusMode, String category) {
        int total = items.size();
        long accepted = items.stream().filter(f -> f.getAcceptedFlag() != null && f.getAcceptedFlag() == 1).count();
        long helpful = items.stream().filter(f -> f.getHelpfulFlag() != null && f.getHelpfulFlag() == 1).count();
        double acceptRate = total > 0 ? (double) accepted / total * 100 : 0;
        double helpRate = total > 0 ? (double) helpful / total * 100 : 0;
        double avgRating = items.stream().filter(f -> f.getFeedbackRating() != null)
                .mapToInt(GovernanceOperatorFeedbackEntity::getFeedbackRating).average().orElse(0);

        double weight = acceptRate * 0.4 + helpRate * 0.3 + avgRating * 12;
        long dismissed = items.stream().filter(f -> f.getAcceptedFlag() == null || f.getAcceptedFlag() == 0).count();
        double dismissRate = total > 0 ? (double) dismissed / total * 100 : 0;
        weight -= dismissRate * 0.25;
        weight = Math.max(0, Math.min(100, weight));

        String level;
        if (weight >= 80) level = "BOOST";
        else if (weight >= 55) level = "KEEP";
        else if (weight >= 30) level = "WATCH";
        else level = "DOWNRANK";

        GovernanceAdaptiveGuidanceSignalEntity s = new GovernanceAdaptiveGuidanceSignalEntity();
        s.setSignalType(type);
        if ("SUGGESTION_TYPE_WEIGHT".equals(type)) s.setSuggestionType(key);
        else s.setFocusMode(key);
        s.setAcceptanceRate(BigDecimal.valueOf(acceptRate).setScale(2, RoundingMode.HALF_UP));
        s.setCompletionRate(BigDecimal.valueOf(helpRate).setScale(2, RoundingMode.HALF_UP));
        s.setAvgFeedbackRating(BigDecimal.valueOf(avgRating).setScale(2, RoundingMode.HALF_UP));
        s.setWeightScore(BigDecimal.valueOf(weight).setScale(2, RoundingMode.HALF_UP));
        s.setSignalLevel(level);
        s.setRationaleText("Type: " + key + ", acceptance: " + String.format("%.0f", acceptRate)
                + "%, rating: " + String.format("%.1f", avgRating) + "/5, weight: " + String.format("%.0f", weight));
        s.setCapturedAt(LocalDateTime.now());
        return s;
    }

    @Transactional(readOnly = true)
    public List<GovernanceAdaptiveGuidanceSignalResponse> listSignals() {
        return signalMapper.selectList(new LambdaQueryWrapper<GovernanceAdaptiveGuidanceSignalEntity>()
                .orderByDesc(GovernanceAdaptiveGuidanceSignalEntity::getWeightScore))
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getDashboard() {
        var signals = listSignals();
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("signalCount", signals.size());
        resp.put("boostCount", signals.stream().filter(s -> "BOOST".equals(s.getSignalLevel())).count());
        resp.put("downrankCount", signals.stream().filter(s -> "DOWNRANK".equals(s.getSignalLevel())).count());
        resp.put("signals", signals);
        return resp;
    }

    private GovernanceAdaptiveGuidanceSignalResponse toResponse(GovernanceAdaptiveGuidanceSignalEntity e) {
        GovernanceAdaptiveGuidanceSignalResponse r = new GovernanceAdaptiveGuidanceSignalResponse();
        r.setId(e.getId() != null ? e.getId().toString() : null);
        r.setSignalType(e.getSignalType()); r.setFocusMode(e.getFocusMode()); r.setCategory(e.getCategory());
        r.setSuggestionType(e.getSuggestionType()); r.setRecommendationPriority(e.getRecommendationPriority());
        r.setAcceptanceRate(e.getAcceptanceRate()); r.setCompletionRate(e.getCompletionRate());
        r.setAvgFeedbackRating(e.getAvgFeedbackRating()); r.setWeightScore(e.getWeightScore());
        r.setSignalLevel(e.getSignalLevel()); r.setRationaleText(e.getRationaleText());
        return r;
    }
}
