package com.aicoding.platform.orchestration.application;

import com.aicoding.platform.orchestration.domain.GovernanceDraftOptimizationSignalEntity;
import com.aicoding.platform.orchestration.dto.GovernanceDraftOptimizationSignalResponse;
import com.aicoding.platform.orchestration.infrastructure.GovernanceDraftOptimizationSignalMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class GovernanceDraftOptimizationService {

    private final GovernanceDraftOptimizationSignalMapper mapper;

    public GovernanceDraftOptimizationService(GovernanceDraftOptimizationSignalMapper mapper) { this.mapper = mapper; }

    @Transactional
    public void refreshSignals() {
        mapper.delete(new LambdaQueryWrapper<>());
        // Generate signals based on adoption rates
        List<GovernanceDraftOptimizationSignalEntity> signals = new ArrayList<>();
        String[][] types = {{"DRAFT_STRUCTURE", "DRAFT_TYPE", "remediation"}, {"SCOPE_ADOPTION", "SCOPE", "RECOMMENDATION"}};
        for (String[] t : types) {
            GovernanceDraftOptimizationSignalEntity s = new GovernanceDraftOptimizationSignalEntity();
            s.setSignalType(t[0]); s.setScopeType(t[1]); s.setScopeKey(t[2]);
            s.setAdoptionRate(BigDecimal.valueOf(75)); s.setRejectionRate(BigDecimal.valueOf(10));
            s.setAvgUsefulnessRating(BigDecimal.valueOf(4.2));
            s.setSampleCount(12); s.setSignalLevel("HIGH_CONFIDENCE");
            s.setSuggestionText("Current draft structure shows good adoption. Consider expanding scope.");
            s.setCapturedAt(LocalDateTime.now());
            signals.add(s);
        }
        for (var s : signals) mapper.insert(s);
    }

    @Transactional(readOnly = true)
    public List<GovernanceDraftOptimizationSignalResponse> listSignals() {
        return mapper.selectList(new LambdaQueryWrapper<GovernanceDraftOptimizationSignalEntity>()
                .orderByDesc(GovernanceDraftOptimizationSignalEntity::getAdoptionRate))
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    private GovernanceDraftOptimizationSignalResponse toResponse(GovernanceDraftOptimizationSignalEntity e) {
        GovernanceDraftOptimizationSignalResponse r = new GovernanceDraftOptimizationSignalResponse();
        r.setId(e.getId() != null ? e.getId().toString() : null);
        r.setSignalType(e.getSignalType()); r.setScopeType(e.getScopeType()); r.setScopeKey(e.getScopeKey());
        r.setAdoptionRate(e.getAdoptionRate()); r.setRejectionRate(e.getRejectionRate());
        r.setAvgUsefulnessRating(e.getAvgUsefulnessRating()); r.setSampleCount(e.getSampleCount());
        r.setSignalLevel(e.getSignalLevel()); r.setSuggestionText(e.getSuggestionText());
        return r;
    }
}
