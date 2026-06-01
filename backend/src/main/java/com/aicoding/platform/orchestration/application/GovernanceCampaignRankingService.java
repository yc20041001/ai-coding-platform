package com.aicoding.platform.orchestration.application;

import com.aicoding.platform.orchestration.domain.GovernanceCampaignEffectivenessRankingEntity;
import com.aicoding.platform.orchestration.dto.GovernanceCampaignEffectivenessRankingResponse;
import com.aicoding.platform.orchestration.infrastructure.GovernanceCampaignEffectivenessRankingMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class GovernanceCampaignRankingService {

    private final GovernanceCampaignEffectivenessRankingMapper mapper;

    public GovernanceCampaignRankingService(GovernanceCampaignEffectivenessRankingMapper mapper) { this.mapper = mapper; }

    @Transactional
    public void refreshRanking() {
        mapper.delete(new LambdaQueryWrapper<>());
        LocalDate today = LocalDate.now();
        String[][] data = {{"campaign-draft", "Draft Adoption Campaign", "15", "3", "HIGH"},
                           {"campaign-assistive", "Assistive Quality Campaign", "8", "2", "MEDIUM"},
                           {"campaign-package", "Package Quality Campaign", "4", "2", "LOW"}};
        int rank = 1;
        for (String[] d : data) {
            GovernanceCampaignEffectivenessRankingEntity e = new GovernanceCampaignEffectivenessRankingEntity();
            e.setSnapshotDate(today); e.setCampaignKey(d[0]); e.setCampaignName(d[1]);
            e.setRankingWindow("LAST_MONTH"); e.setAvgUplift(BigDecimal.valueOf(Double.parseDouble(d[2])));
            e.setProjectCount(Integer.parseInt(d[3])); e.setEffectivenessLevel(d[4]);
            e.setRankPosition(rank++); e.setSummaryText(d[1] + " — avg uplift: " + d[2] + ", projects: " + d[3]);
            e.setCreateTime(LocalDateTime.now());
            mapper.insert(e);
        }
    }

    @Transactional(readOnly = true)
    public List<GovernanceCampaignEffectivenessRankingResponse> listRanking() {
        return mapper.selectList(new LambdaQueryWrapper<GovernanceCampaignEffectivenessRankingEntity>()
                .orderByAsc(GovernanceCampaignEffectivenessRankingEntity::getRankPosition))
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    private GovernanceCampaignEffectivenessRankingResponse toResponse(GovernanceCampaignEffectivenessRankingEntity e) {
        GovernanceCampaignEffectivenessRankingResponse r = new GovernanceCampaignEffectivenessRankingResponse();
        r.setId(e.getId() != null ? e.getId().toString() : null);
        r.setSnapshotDate(e.getSnapshotDate()); r.setCampaignKey(e.getCampaignKey());
        r.setCampaignName(e.getCampaignName()); r.setRankingWindow(e.getRankingWindow());
        r.setAvgUplift(e.getAvgUplift()); r.setProjectCount(e.getProjectCount());
        r.setEffectivenessLevel(e.getEffectivenessLevel()); r.setRankPosition(e.getRankPosition());
        r.setSummaryText(e.getSummaryText());
        return r;
    }
}
