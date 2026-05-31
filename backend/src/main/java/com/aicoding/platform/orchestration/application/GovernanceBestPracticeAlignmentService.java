package com.aicoding.platform.orchestration.application;

import com.aicoding.platform.orchestration.domain.GovernanceBestPracticeAlignmentItemEntity;
import com.aicoding.platform.orchestration.dto.GovernanceBestPracticeAlignmentItemResponse;
import com.aicoding.platform.orchestration.infrastructure.GovernanceBestPracticeAlignmentItemMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class GovernanceBestPracticeAlignmentService {

    private final GovernanceBestPracticeAlignmentItemMapper mapper;

    public GovernanceBestPracticeAlignmentService(GovernanceBestPracticeAlignmentItemMapper mapper) { this.mapper = mapper; }

    @Transactional
    public void refreshAlignments() {
        mapper.delete(new LambdaQueryWrapper<>());
        LocalDate today = LocalDate.now();
        String[][] items = {{"1", "Project-A", "DRAFT_ADOPTION", "75.0", "85.0"},
                            {"2", "Project-B", "ASSISTIVE_USE", "60.0", "80.0"},
                            {"3", "Project-C", "PACKAGE_QUALITY", "70.0", "75.0"}};
        for (String[] d : items) {
            GovernanceBestPracticeAlignmentItemEntity e = new GovernanceBestPracticeAlignmentItemEntity();
            e.setSnapshotDate(today); e.setProjectId(Long.parseLong(d[0])); e.setProjectName(d[1]);
            e.setPracticeType(d[2]); e.setCurrentScore(BigDecimal.valueOf(Double.parseDouble(d[3])));
            e.setTargetScore(BigDecimal.valueOf(Double.parseDouble(d[4])));
            e.setGap(BigDecimal.valueOf(Double.parseDouble(d[4]) - Double.parseDouble(d[3])));
            e.setAlignmentLevel(Double.parseDouble(d[3]) >= Double.parseDouble(d[4]) * 0.9 ? "ALIGNED" : "DEVIATED");
            e.setSuggestionText("Improve " + d[2] + " from " + d[3] + " to target " + d[4]);
            e.setCreateTime(LocalDateTime.now());
            mapper.insert(e);
        }
    }

    @Transactional(readOnly = true)
    public List<GovernanceBestPracticeAlignmentItemResponse> listAlignments() {
        return mapper.selectList(new LambdaQueryWrapper<GovernanceBestPracticeAlignmentItemEntity>()
                .orderByDesc(GovernanceBestPracticeAlignmentItemEntity::getGap))
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    private GovernanceBestPracticeAlignmentItemResponse toResponse(GovernanceBestPracticeAlignmentItemEntity e) {
        GovernanceBestPracticeAlignmentItemResponse r = new GovernanceBestPracticeAlignmentItemResponse();
        r.setId(e.getId() != null ? e.getId().toString() : null);
        r.setSnapshotDate(e.getSnapshotDate());
        r.setProjectId(e.getProjectId() != null ? e.getProjectId().toString() : null);
        r.setProjectName(e.getProjectName()); r.setPracticeType(e.getPracticeType());
        r.setAlignmentLevel(e.getAlignmentLevel()); r.setCurrentScore(e.getCurrentScore());
        r.setTargetScore(e.getTargetScore()); r.setGap(e.getGap()); r.setSuggestionText(e.getSuggestionText());
        return r;
    }
}
