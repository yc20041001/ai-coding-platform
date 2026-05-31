package com.aicoding.platform.orchestration.application;

import com.aicoding.platform.orchestration.domain.GovernanceMaturityScorecardEntity;
import com.aicoding.platform.orchestration.dto.GovernanceMaturityScorecardResponse;
import com.aicoding.platform.orchestration.infrastructure.GovernanceMaturityScorecardMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class GovernanceMaturityScorecardService {

    private final GovernanceMaturityScorecardMapper mapper;

    public GovernanceMaturityScorecardService(GovernanceMaturityScorecardMapper mapper) { this.mapper = mapper; }

    @Transactional
    public void refreshScorecards() {
        mapper.delete(new LambdaQueryWrapper<>());
        LocalDate today = LocalDate.now();
        String[][] items = {{"1", "Project-A", "75", "80", "70", "65", "72", "DEFINED"},
                            {"2", "Project-B", "55", "60", "50", "45", "52", "DEVELOPING"}};
        for (String[] d : items) {
            GovernanceMaturityScorecardEntity e = new GovernanceMaturityScorecardEntity();
            e.setSnapshotDate(today); e.setProjectId(Long.parseLong(d[0])); e.setProjectName(d[1]);
            e.setDraftAdoptionScore(BigDecimal.valueOf(Double.parseDouble(d[2])));
            e.setAssistiveQualityScore(BigDecimal.valueOf(Double.parseDouble(d[3])));
            e.setPackageQualityScore(BigDecimal.valueOf(Double.parseDouble(d[4])));
            e.setOutcomeReviewScore(BigDecimal.valueOf(Double.parseDouble(d[5])));
            e.setOperatorProductivityScore(BigDecimal.valueOf(Double.parseDouble(d[6])));
            double total = (Double.parseDouble(d[2]) + Double.parseDouble(d[3]) + Double.parseDouble(d[4])
                    + Double.parseDouble(d[5]) + Double.parseDouble(d[6])) / 5;
            e.setTotalScore(BigDecimal.valueOf(total));
            e.setMaturityLevel(d[7]);
            e.setSummaryText(d[1] + " — maturity: " + d[7] + ", score: " + String.format("%.0f", total));
            e.setCreateTime(LocalDateTime.now());
            mapper.insert(e);
        }
    }

    @Transactional(readOnly = true)
    public List<GovernanceMaturityScorecardResponse> listScorecards() {
        return mapper.selectList(new LambdaQueryWrapper<GovernanceMaturityScorecardEntity>()
                .orderByDesc(GovernanceMaturityScorecardEntity::getTotalScore))
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    private GovernanceMaturityScorecardResponse toResponse(GovernanceMaturityScorecardEntity e) {
        GovernanceMaturityScorecardResponse r = new GovernanceMaturityScorecardResponse();
        r.setId(e.getId() != null ? e.getId().toString() : null);
        r.setSnapshotDate(e.getSnapshotDate());
        r.setProjectId(e.getProjectId() != null ? e.getProjectId().toString() : null);
        r.setProjectName(e.getProjectName()); r.setMaturityLevel(e.getMaturityLevel());
        r.setTotalScore(e.getTotalScore()); r.setDraftAdoptionScore(e.getDraftAdoptionScore());
        r.setAssistiveQualityScore(e.getAssistiveQualityScore());
        r.setPackageQualityScore(e.getPackageQualityScore());
        r.setOutcomeReviewScore(e.getOutcomeReviewScore());
        r.setOperatorProductivityScore(e.getOperatorProductivityScore());
        r.setSummaryText(e.getSummaryText());
        return r;
    }
}
