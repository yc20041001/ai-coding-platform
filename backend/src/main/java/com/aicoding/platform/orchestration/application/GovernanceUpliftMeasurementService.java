package com.aicoding.platform.orchestration.application;

import com.aicoding.platform.orchestration.domain.GovernanceUpliftMeasurementSnapshotEntity;
import com.aicoding.platform.orchestration.dto.GovernanceUpliftMeasurementSnapshotResponse;
import com.aicoding.platform.orchestration.infrastructure.GovernanceUpliftMeasurementSnapshotMapper;
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
public class GovernanceUpliftMeasurementService {

    private final GovernanceUpliftMeasurementSnapshotMapper mapper;

    public GovernanceUpliftMeasurementService(GovernanceUpliftMeasurementSnapshotMapper mapper) { this.mapper = mapper; }

    @Transactional
    public void refreshUplift() {
        mapper.delete(new LambdaQueryWrapper<>());
        LocalDate today = LocalDate.now();
        String[][] data = {{"1", "Project-A", "campaign-draft", "draft_adoption_rate", "60", "78"},
                           {"2", "Project-B", "campaign-assistive", "assistive_quality_score", "45", "65"}};
        for (String[] d : data) {
            GovernanceUpliftMeasurementSnapshotEntity e = new GovernanceUpliftMeasurementSnapshotEntity();
            e.setSnapshotDate(today); e.setProjectId(Long.parseLong(d[0])); e.setProjectName(d[1]);
            e.setCampaignKey(d[2]); e.setMetricKey(d[3]);
            e.setBeforeScore(BigDecimal.valueOf(Double.parseDouble(d[4])));
            e.setAfterScore(BigDecimal.valueOf(Double.parseDouble(d[5])));
            double uplift = Double.parseDouble(d[5]) - Double.parseDouble(d[4]);
            e.setUplift(BigDecimal.valueOf(uplift).setScale(2, RoundingMode.HALF_UP));
            e.setUpliftLevel(uplift >= 15 ? "SIGNIFICANT" : uplift >= 8 ? "MODERATE" : uplift > 0 ? "MINIMAL" : "NONE");
            e.setSummaryText(d[1] + " " + d[3] + ": " + d[4] + " → " + d[5] + " (uplift: " + uplift + ")");
            e.setCreateTime(LocalDateTime.now());
            mapper.insert(e);
        }
    }

    @Transactional(readOnly = true)
    public List<GovernanceUpliftMeasurementSnapshotResponse> listUplift() {
        return mapper.selectList(new LambdaQueryWrapper<GovernanceUpliftMeasurementSnapshotEntity>()
                .orderByDesc(GovernanceUpliftMeasurementSnapshotEntity::getUplift))
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    private GovernanceUpliftMeasurementSnapshotResponse toResponse(GovernanceUpliftMeasurementSnapshotEntity e) {
        GovernanceUpliftMeasurementSnapshotResponse r = new GovernanceUpliftMeasurementSnapshotResponse();
        r.setId(e.getId() != null ? e.getId().toString() : null);
        r.setSnapshotDate(e.getSnapshotDate());
        r.setProjectId(e.getProjectId() != null ? e.getProjectId().toString() : null);
        r.setProjectName(e.getProjectName()); r.setCampaignKey(e.getCampaignKey());
        r.setMetricKey(e.getMetricKey()); r.setBeforeScore(e.getBeforeScore());
        r.setAfterScore(e.getAfterScore()); r.setUplift(e.getUplift());
        r.setUpliftLevel(e.getUpliftLevel()); r.setSummaryText(e.getSummaryText());
        return r;
    }
}
