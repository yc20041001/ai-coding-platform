package com.aicoding.platform.orchestration.application;

import com.aicoding.platform.orchestration.domain.GovernanceProgressMapSnapshotEntity;
import com.aicoding.platform.orchestration.dto.GovernanceProgressMapSnapshotResponse;
import com.aicoding.platform.orchestration.infrastructure.GovernanceProgressMapSnapshotMapper;
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
public class GovernanceProgressMapService {

    private final GovernanceProgressMapSnapshotMapper mapper;

    public GovernanceProgressMapService(GovernanceProgressMapSnapshotMapper mapper) { this.mapper = mapper; }

    @Transactional
    public void refreshProgress() {
        mapper.delete(new LambdaQueryWrapper<>());
        LocalDate today = LocalDate.now();
        String[][] data = {{"1", "Project-A", "draft_adoption_rate", "50", "75", "85"},
                           {"2", "Project-B", "assistive_quality_score", "40", "65", "80"},
                           {"3", "Project-C", "package_quality_score", "55", "70", "75"}};
        for (String[] d : data) {
            GovernanceProgressMapSnapshotEntity e = new GovernanceProgressMapSnapshotEntity();
            e.setSnapshotDate(today); e.setProjectId(Long.parseLong(d[0])); e.setProjectName(d[1]);
            e.setMetricKey(d[2]);
            double baseline = Double.parseDouble(d[3]); double current = Double.parseDouble(d[4]); double target = Double.parseDouble(d[5]);
            e.setBaselineScore(BigDecimal.valueOf(baseline)); e.setCurrentScore(BigDecimal.valueOf(current));
            e.setTargetScore(BigDecimal.valueOf(target));
            e.setProgressPercentage(target > baseline ? BigDecimal.valueOf((current - baseline) / (target - baseline) * 100).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO);
            e.setSignalLevel(e.getProgressPercentage().compareTo(BigDecimal.valueOf(80)) >= 0 ? "ON_TRACK"
                    : e.getProgressPercentage().compareTo(BigDecimal.valueOf(50)) >= 0 ? "AT_RISK" : "BEHIND");
            e.setSummaryText(d[1] + " " + d[2] + ": " + baseline + " → " + current + "/" + target);
            e.setCreateTime(LocalDateTime.now());
            mapper.insert(e);
        }
    }

    @Transactional(readOnly = true)
    public List<GovernanceProgressMapSnapshotResponse> listProgress() {
        return mapper.selectList(new LambdaQueryWrapper<GovernanceProgressMapSnapshotEntity>()
                .orderByDesc(GovernanceProgressMapSnapshotEntity::getProgressPercentage))
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    private GovernanceProgressMapSnapshotResponse toResponse(GovernanceProgressMapSnapshotEntity e) {
        GovernanceProgressMapSnapshotResponse r = new GovernanceProgressMapSnapshotResponse();
        r.setId(e.getId() != null ? e.getId().toString() : null);
        r.setSnapshotDate(e.getSnapshotDate());
        r.setProjectId(e.getProjectId() != null ? e.getProjectId().toString() : null);
        r.setProjectName(e.getProjectName()); r.setMetricKey(e.getMetricKey());
        r.setBaselineScore(e.getBaselineScore()); r.setCurrentScore(e.getCurrentScore());
        r.setTargetScore(e.getTargetScore()); r.setProgressPercentage(e.getProgressPercentage());
        r.setSignalLevel(e.getSignalLevel()); r.setSummaryText(e.getSummaryText());
        return r;
    }
}
