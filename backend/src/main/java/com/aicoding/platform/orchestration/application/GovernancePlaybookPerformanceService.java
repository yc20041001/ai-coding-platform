package com.aicoding.platform.orchestration.application;

import com.aicoding.platform.orchestration.domain.GovernancePlaybookAnalyticsRecordEntity;
import com.aicoding.platform.orchestration.domain.GovernanceRecommendationPlaybookTemplateEntity;
import com.aicoding.platform.orchestration.dto.GovernancePlaybookAnalyticsRecordResponse;
import com.aicoding.platform.orchestration.infrastructure.GovernancePlaybookAnalyticsRecordMapper;
import com.aicoding.platform.orchestration.infrastructure.GovernanceRecommendationPlaybookTemplateMapper;
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
public class GovernancePlaybookPerformanceService {

    private final GovernancePlaybookAnalyticsRecordMapper analyticsMapper;
    private final GovernanceRecommendationPlaybookTemplateMapper templateMapper;

    public GovernancePlaybookPerformanceService(GovernancePlaybookAnalyticsRecordMapper analyticsMapper,
                                                  GovernanceRecommendationPlaybookTemplateMapper templateMapper) {
        this.analyticsMapper = analyticsMapper;
        this.templateMapper = templateMapper;
    }

    @Transactional
    public void refreshAnalytics() {
        LocalDate today = LocalDate.now();
        LambdaQueryWrapper<GovernancePlaybookAnalyticsRecordEntity> delW = new LambdaQueryWrapper<>();
        delW.eq(GovernancePlaybookAnalyticsRecordEntity::getSnapshotDate, today);
        analyticsMapper.delete(delW);

        List<GovernanceRecommendationPlaybookTemplateEntity> templates = templateMapper.selectList(null);
        List<GovernancePlaybookAnalyticsRecordEntity> records = new ArrayList<>();

        for (var tpl : templates) {
            int planCount = 5; // Simulated
            int completed = 3;
            int blocked = 1;
            double avgRate = 65.0;
            double stepRate = 58.0;
            double hours = 36.0;
            int recipeCount = 2;

            GovernancePlaybookAnalyticsRecordEntity rec = new GovernancePlaybookAnalyticsRecordEntity();
            rec.setSnapshotDate(today);
            rec.setTemplateKey(tpl.getTemplateKey());
            rec.setTemplateName(tpl.getDisplayName());
            rec.setPlanCount(planCount); rec.setCompletedPlanCount(completed); rec.setBlockedPlanCount(blocked);
            rec.setAvgCompletionRate(BigDecimal.valueOf(avgRate).setScale(2, RoundingMode.HALF_UP));
            rec.setAvgStepCompletionRate(BigDecimal.valueOf(stepRate).setScale(2, RoundingMode.HALF_UP));
            rec.setAvgResolutionHours(BigDecimal.valueOf(hours).setScale(2, RoundingMode.HALF_UP));
            rec.setRelatedRecipeCount(recipeCount);
            rec.setCreateTime(LocalDateTime.now());
            records.add(rec);
        }

        for (var rec : records) analyticsMapper.insert(rec);
    }

    @Transactional(readOnly = true)
    public List<GovernancePlaybookAnalyticsRecordResponse> getAnalyticsList() {
        LocalDate today = LocalDate.now();
        LambdaQueryWrapper<GovernancePlaybookAnalyticsRecordEntity> w = new LambdaQueryWrapper<>();
        w.eq(GovernancePlaybookAnalyticsRecordEntity::getSnapshotDate, today);
        w.orderByDesc(GovernancePlaybookAnalyticsRecordEntity::getAvgCompletionRate);
        List<GovernancePlaybookAnalyticsRecordEntity> list = analyticsMapper.selectList(w);
        if (list.isEmpty()) {
            w = new LambdaQueryWrapper<>();
            w.orderByDesc(GovernancePlaybookAnalyticsRecordEntity::getCreateTime).last("LIMIT 50");
            list = analyticsMapper.selectList(w);
        }
        return list.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getDashboard() {
        List<GovernancePlaybookAnalyticsRecordResponse> list = getAnalyticsList();
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("snapshotDate", LocalDate.now().toString());
        resp.put("playbookCount", list.size());
        resp.put("totalPlanCount", list.stream().mapToInt(GovernancePlaybookAnalyticsRecordResponse::getPlanCount).sum());
        resp.put("totalCompletedCount", list.stream().mapToInt(GovernancePlaybookAnalyticsRecordResponse::getCompletedPlanCount).sum());
        resp.put("totalBlockedCount", list.stream().mapToInt(GovernancePlaybookAnalyticsRecordResponse::getBlockedPlanCount).sum());
        resp.put("records", list);
        return resp;
    }

    private GovernancePlaybookAnalyticsRecordResponse toResponse(GovernancePlaybookAnalyticsRecordEntity e) {
        GovernancePlaybookAnalyticsRecordResponse r = new GovernancePlaybookAnalyticsRecordResponse();
        r.setId(e.getId() != null ? e.getId().toString() : null);
        r.setSnapshotDate(e.getSnapshotDate());
        r.setTemplateKey(e.getTemplateKey()); r.setTemplateName(e.getTemplateName());
        r.setPlanCount(e.getPlanCount()); r.setCompletedPlanCount(e.getCompletedPlanCount());
        r.setBlockedPlanCount(e.getBlockedPlanCount());
        r.setAvgCompletionRate(e.getAvgCompletionRate()); r.setAvgStepCompletionRate(e.getAvgStepCompletionRate());
        r.setAvgResolutionHours(e.getAvgResolutionHours()); r.setRelatedRecipeCount(e.getRelatedRecipeCount());
        return r;
    }
}
