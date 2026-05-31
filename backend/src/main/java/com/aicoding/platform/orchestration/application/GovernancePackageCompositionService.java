package com.aicoding.platform.orchestration.application;

import com.aicoding.platform.orchestration.domain.GovernancePackageCompositionTuningEntity;
import com.aicoding.platform.orchestration.dto.GovernancePackageCompositionTuningResponse;
import com.aicoding.platform.orchestration.infrastructure.GovernancePackageCompositionTuningMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class GovernancePackageCompositionService {

    private final GovernancePackageCompositionTuningMapper mapper;

    public GovernancePackageCompositionService(GovernancePackageCompositionTuningMapper mapper) { this.mapper = mapper; }

    @Transactional
    public void refreshComposition() {
        mapper.delete(new LambdaQueryWrapper<>());
        List<GovernancePackageCompositionTuningEntity> items = new ArrayList<>();
        for (String[] d : new String[][]{{"HIGH", "85", "90", "ADD_SECTION"}, {"MEDIUM", "65", "60", "REORDER"},
                                         {"LOW", "35", "30", "REMOVE_SECTION"}}) {
            GovernancePackageCompositionTuningEntity e = new GovernancePackageCompositionTuningEntity();
            e.setScoreRange(d[0]); e.setAvgCompleteness(BigDecimal.valueOf(Double.parseDouble(d[1])));
            e.setAvgAccuracy(BigDecimal.valueOf(Double.parseDouble(d[2])));
            e.setAvgOverall(BigDecimal.valueOf((Double.parseDouble(d[1]) + Double.parseDouble(d[2])) / 2));
            e.setSampleCount(5); e.setTuningLevel(d[3]);
            e.setSuggestionText("Package composition tuning for " + d[0] + " score range");
            e.setCapturedAt(LocalDateTime.now());
            items.add(e);
        }
        for (var e : items) mapper.insert(e);
    }

    @Transactional(readOnly = true)
    public List<GovernancePackageCompositionTuningResponse> listComposition() {
        return mapper.selectList(new LambdaQueryWrapper<GovernancePackageCompositionTuningEntity>()
                .orderByDesc(GovernancePackageCompositionTuningEntity::getAvgOverall))
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    private GovernancePackageCompositionTuningResponse toResponse(GovernancePackageCompositionTuningEntity e) {
        GovernancePackageCompositionTuningResponse r = new GovernancePackageCompositionTuningResponse();
        r.setId(e.getId() != null ? e.getId().toString() : null);
        r.setScoreRange(e.getScoreRange()); r.setAvgCompleteness(e.getAvgCompleteness());
        r.setAvgAccuracy(e.getAvgAccuracy()); r.setAvgOverall(e.getAvgOverall());
        r.setSampleCount(e.getSampleCount()); r.setTuningLevel(e.getTuningLevel());
        r.setSuggestionText(e.getSuggestionText());
        return r;
    }
}
