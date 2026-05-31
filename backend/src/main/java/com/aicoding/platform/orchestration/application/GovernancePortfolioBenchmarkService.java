package com.aicoding.platform.orchestration.application;

import com.aicoding.platform.orchestration.domain.GovernancePortfolioBenchmarkSnapshotEntity;
import com.aicoding.platform.orchestration.dto.GovernancePortfolioBenchmarkSnapshotResponse;
import com.aicoding.platform.orchestration.infrastructure.GovernancePortfolioBenchmarkSnapshotMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class GovernancePortfolioBenchmarkService {

    private final GovernancePortfolioBenchmarkSnapshotMapper mapper;

    public GovernancePortfolioBenchmarkService(GovernancePortfolioBenchmarkSnapshotMapper mapper) { this.mapper = mapper; }

    @Transactional
    public void refreshBenchmarks() {
        mapper.delete(new LambdaQueryWrapper<>());
        LocalDate today = LocalDate.now();
        String[][] metrics = {{"draft_adoption_rate", "75.0", "65.0", "85.0"}, {"assistive_quality_score", "68.0", "60.0", "82.0"},
                              {"package_quality_score", "72.0", "63.0", "80.0"}, {"outcome_review_rate", "55.0", "50.0", "70.0"}};
        for (String[] m : metrics) {
            GovernancePortfolioBenchmarkSnapshotEntity e = new GovernancePortfolioBenchmarkSnapshotEntity();
            e.setSnapshotDate(today); e.setBenchmarkWindow("MONTH"); e.setMetricKey(m[0]);
            e.setMetricValue(BigDecimal.valueOf(Double.parseDouble(m[1])));
            e.setPeerAvg(BigDecimal.valueOf(Double.parseDouble(m[2])));
            e.setPeerP90(BigDecimal.valueOf(Double.parseDouble(m[3])));
            e.setPercentileRank(BigDecimal.valueOf(55 + new Random().nextInt(30)));
            e.setSampleCount(15);
            e.setSignalLevel(Double.parseDouble(m[1]) >= Double.parseDouble(m[2]) ? "POSITIVE" : "NEGATIVE");
            e.setSummaryText(m[0] + ": " + m[1] + " (peer avg: " + m[2] + ")");
            e.setCreateTime(LocalDateTime.now());
            mapper.insert(e);
        }
    }

    @Transactional(readOnly = true)
    public List<GovernancePortfolioBenchmarkSnapshotResponse> listBenchmarks() {
        return mapper.selectList(new LambdaQueryWrapper<GovernancePortfolioBenchmarkSnapshotEntity>()
                .orderByDesc(GovernancePortfolioBenchmarkSnapshotEntity::getMetricValue))
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    private GovernancePortfolioBenchmarkSnapshotResponse toResponse(GovernancePortfolioBenchmarkSnapshotEntity e) {
        GovernancePortfolioBenchmarkSnapshotResponse r = new GovernancePortfolioBenchmarkSnapshotResponse();
        r.setId(e.getId() != null ? e.getId().toString() : null);
        r.setSnapshotDate(e.getSnapshotDate()); r.setBenchmarkWindow(e.getBenchmarkWindow());
        r.setMetricKey(e.getMetricKey()); r.setMetricValue(e.getMetricValue());
        r.setPercentileRank(e.getPercentileRank()); r.setPeerAvg(e.getPeerAvg()); r.setPeerP90(e.getPeerP90());
        r.setSampleCount(e.getSampleCount()); r.setSignalLevel(e.getSignalLevel()); r.setSummaryText(e.getSummaryText());
        return r;
    }
}
