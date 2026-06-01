package com.aicoding.platform.orchestration.application;

import com.aicoding.platform.orchestration.domain.GovernanceBenchmarkEvolutionSnapshotEntity;
import com.aicoding.platform.orchestration.dto.GovernanceBenchmarkEvolutionSnapshotResponse;
import com.aicoding.platform.orchestration.infrastructure.GovernanceBenchmarkEvolutionSnapshotMapper;
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
public class GovernanceBenchmarkEvolutionService {

    private final GovernanceBenchmarkEvolutionSnapshotMapper mapper;

    public GovernanceBenchmarkEvolutionService(GovernanceBenchmarkEvolutionSnapshotMapper mapper) { this.mapper = mapper; }

    @Transactional
    public void refreshEvolution() {
        mapper.delete(new LambdaQueryWrapper<>());
        LocalDate today = LocalDate.now();
        String[][] data = {{"ADOPTION_RATE", "draft_adoption_rate", "75", "65"},
                           {"MATURITY_SCORE", "overall_maturity", "62", "58"},
                           {"ALIGNMENT_SCORE", "best_practice_alignment", "70", "68"},
                           {"UPLIFT_SCORE", "avg_uplift", "12", "8"}};
        for (String[] d : data) {
            GovernanceBenchmarkEvolutionSnapshotEntity e = new GovernanceBenchmarkEvolutionSnapshotEntity();
            e.setSnapshotDate(today); e.setBenchmarkType(d[0]); e.setMetricKey(d[1]);
            double cur = Double.parseDouble(d[2]); double prev = Double.parseDouble(d[3]);
            e.setCurrentValue(BigDecimal.valueOf(cur)); e.setPreviousValue(BigDecimal.valueOf(prev));
            e.setDelta(BigDecimal.valueOf(cur - prev).setScale(2, RoundingMode.HALF_UP));
            e.setDeltaPercentage(prev > 0 ? BigDecimal.valueOf((cur - prev) / prev * 100).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO);
            e.setSignalLevel(cur >= prev ? "IMPROVING" : "DECLINING"); e.setSampleCount(15);
            e.setSummaryText(d[0] + " " + d[1] + ": " + prev + " → " + cur);
            e.setCreateTime(LocalDateTime.now());
            mapper.insert(e);
        }
    }

    @Transactional(readOnly = true)
    public List<GovernanceBenchmarkEvolutionSnapshotResponse> listEvolution() {
        return mapper.selectList(new LambdaQueryWrapper<GovernanceBenchmarkEvolutionSnapshotEntity>()
                .orderByDesc(GovernanceBenchmarkEvolutionSnapshotEntity::getDelta))
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    private GovernanceBenchmarkEvolutionSnapshotResponse toResponse(GovernanceBenchmarkEvolutionSnapshotEntity e) {
        GovernanceBenchmarkEvolutionSnapshotResponse r = new GovernanceBenchmarkEvolutionSnapshotResponse();
        r.setId(e.getId() != null ? e.getId().toString() : null);
        r.setSnapshotDate(e.getSnapshotDate()); r.setBenchmarkType(e.getBenchmarkType());
        r.setMetricKey(e.getMetricKey()); r.setCurrentValue(e.getCurrentValue());
        r.setPreviousValue(e.getPreviousValue()); r.setDelta(e.getDelta());
        r.setDeltaPercentage(e.getDeltaPercentage()); r.setSignalLevel(e.getSignalLevel());
        r.setSampleCount(e.getSampleCount()); r.setSummaryText(e.getSummaryText());
        return r;
    }
}
