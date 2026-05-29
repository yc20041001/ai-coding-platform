package com.aicoding.platform.orchestration.application;

import com.aicoding.platform.common.exception.BizException;
import com.aicoding.platform.common.exception.ErrorCode;
import com.aicoding.platform.member.application.ProjectPermissionService;
import com.aicoding.platform.member.domain.ProjectRole;
import com.aicoding.platform.orchestration.domain.ModelCostAlertEntity;
import com.aicoding.platform.orchestration.domain.ModelCostSummaryEntity;
import com.aicoding.platform.orchestration.dto.ModelCostAlertResponse;
import com.aicoding.platform.orchestration.dto.ModelCostDashboardResponse;
import com.aicoding.platform.orchestration.dto.ModelCostSummaryResponse;
import com.aicoding.platform.orchestration.dto.ModelCostTrendResponse;
import com.aicoding.platform.orchestration.infrastructure.ModelCostAlertMapper;
import com.aicoding.platform.orchestration.infrastructure.ModelCostSummaryMapper;
import com.aicoding.platform.orchestrator.domain.ModelRequestLogEntity;
import com.aicoding.platform.orchestrator.infrastructure.ModelRequestLogMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ModelCostAnalyticsService {

    private final ModelCostSummaryMapper modelCostSummaryMapper;
    private final ModelCostAlertMapper modelCostAlertMapper;
    private final ModelRequestLogMapper modelRequestLogMapper;
    private final ProjectPermissionService projectPermissionService;

    public ModelCostAnalyticsService(ModelCostSummaryMapper modelCostSummaryMapper,
                                     ModelCostAlertMapper modelCostAlertMapper,
                                     ModelRequestLogMapper modelRequestLogMapper,
                                     ProjectPermissionService projectPermissionService) {
        this.modelCostSummaryMapper = modelCostSummaryMapper;
        this.modelCostAlertMapper = modelCostAlertMapper;
        this.modelRequestLogMapper = modelRequestLogMapper;
        this.projectPermissionService = projectPermissionService;
    }

    @Transactional
    public void refreshDailySummaries(String projectIdStr) {
        Long projectId = parseLong(projectIdStr, "projectId");
        projectPermissionService.checkProjectRole(projectId, ProjectRole.OWNER, ProjectRole.MAINTAINER);

        LocalDate yesterday = LocalDate.now().minusDays(1);
        LocalDateTime startOfDay = yesterday.atStartOfDay();
        LocalDateTime endOfDay = yesterday.atTime(LocalTime.MAX);

        QueryWrapper<ModelRequestLogEntity> query = new QueryWrapper<>();
        query.select("project_id", "provider", "model_name", "request_type",
                        "COUNT(*) as request_count",
                        "SUM(CASE WHEN success = 1 THEN 1 ELSE 0 END) as success_count",
                        "SUM(CASE WHEN success = 0 THEN 1 ELSE 0 END) as failure_count",
                        "SUM(CASE WHEN fallback_used = 1 THEN 1 ELSE 0 END) as fallback_count",
                        "COALESCE(SUM(prompt_tokens), 0) as prompt_tokens",
                        "COALESCE(SUM(completion_tokens), 0) as completion_tokens",
                        "COALESCE(SUM(total_tokens), 0) as total_tokens",
                        "COALESCE(SUM(estimated_cost), 0) as estimated_cost",
                        "COALESCE(ROUND(AVG(latency_ms)), 0) as avg_latency_ms")
                .eq(projectId != null, "project_id", projectId)
                .ge("create_time", startOfDay)
                .lt("create_time", endOfDay)
                .groupBy("project_id", "provider", "model_name", "request_type");

        List<Map<String, Object>> results = modelRequestLogMapper.selectMaps(query);

        for (Map<String, Object> row : results) {
            Long rowProjectId = toLong(row.get("project_id"));
            String provider = (String) row.get("provider");
            String modelName = (String) row.get("model_name");
            String requestType = (String) row.get("request_type");

            ModelCostSummaryEntity entity = new ModelCostSummaryEntity();
            entity.setProjectId(rowProjectId);
            entity.setProvider(provider);
            entity.setModelName(modelName);
            entity.setRequestType(requestType);
            entity.setStatDate(yesterday);
            entity.setRequestCount(toLong(row.get("request_count")));
            entity.setSuccessCount(toLong(row.get("success_count")));
            entity.setFailureCount(toLong(row.get("failure_count")));
            entity.setFallbackCount(toLong(row.get("fallback_count")));
            entity.setPromptTokens(toLong(row.get("prompt_tokens")));
            entity.setCompletionTokens(toLong(row.get("completion_tokens")));
            entity.setTotalTokens(toLong(row.get("total_tokens")));
            entity.setEstimatedCost(toBigDecimal(row.get("estimated_cost")));
            entity.setAvgLatencyMs(toLong(row.get("avg_latency_ms")));
            modelCostSummaryMapper.insert(entity);
        }
    }

    @Transactional(readOnly = true)
    public List<ModelCostSummaryResponse> listCostSummaries(String projectIdStr, String provider,
                                                             String modelName, LocalDate startDate,
                                                             LocalDate endDate, int page, int size) {
        Long projectId = parseLong(projectIdStr, "projectId");
        projectPermissionService.checkProjectMember(projectId);

        LambdaQueryWrapper<ModelCostSummaryEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ModelCostSummaryEntity::getProjectId, projectId);
        if (provider != null && !provider.isBlank()) {
            wrapper.eq(ModelCostSummaryEntity::getProvider, provider);
        }
        if (modelName != null && !modelName.isBlank()) {
            wrapper.eq(ModelCostSummaryEntity::getModelName, modelName);
        }
        if (startDate != null) {
            wrapper.ge(ModelCostSummaryEntity::getStatDate, startDate);
        }
        if (endDate != null) {
            wrapper.le(ModelCostSummaryEntity::getStatDate, endDate);
        }
        wrapper.orderByDesc(ModelCostSummaryEntity::getStatDate);
        wrapper.last("LIMIT " + size + " OFFSET " + (page - 1) * size);

        List<ModelCostSummaryEntity> entities = modelCostSummaryMapper.selectList(wrapper);
        return entities.stream().map(this::toSummaryResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ModelCostTrendResponse> getCostTrend(String projectIdStr, LocalDate startDate, LocalDate endDate) {
        Long projectId = parseLong(projectIdStr, "projectId");
        projectPermissionService.checkProjectMember(projectId);

        QueryWrapper<ModelCostSummaryEntity> wrapper = new QueryWrapper<>();
        wrapper.select("stat_date", "provider", "model_name",
                        "SUM(estimated_cost) as total_cost",
                        "SUM(total_tokens) as total_tokens",
                        "SUM(request_count) as request_count")
                .eq("project_id", projectId)
                .ge("stat_date", startDate != null ? startDate : LocalDate.now().minusDays(30))
                .le("stat_date", endDate != null ? endDate : LocalDate.now())
                .groupBy("stat_date", "provider", "model_name")
                .orderByAsc("stat_date");

        List<Map<String, Object>> results = modelCostSummaryMapper.selectMaps(wrapper);
        List<ModelCostTrendResponse> trends = new ArrayList<>();
        for (Map<String, Object> row : results) {
            ModelCostTrendResponse resp = new ModelCostTrendResponse();
            resp.setStatDate(row.get("stat_date") != null ? LocalDate.parse(row.get("stat_date").toString()) : null);
            resp.setProvider((String) row.get("provider"));
            resp.setModelName((String) row.get("model_name"));
            resp.setTotalCost(toBigDecimal(row.get("total_cost")));
            resp.setTotalTokens(toLong(row.get("total_tokens")));
            resp.setRequestCount(toLong(row.get("request_count")));
            trends.add(resp);
        }
        return trends;
    }

    @Transactional(readOnly = true)
    public ModelCostDashboardResponse getCostDashboard(String projectIdStr) {
        Long projectId = parseLong(projectIdStr, "projectId");
        projectPermissionService.checkProjectMember(projectId);

        ModelCostDashboardResponse dashboard = new ModelCostDashboardResponse();
        LocalDate today = LocalDate.now();
        LocalDate weekAgo = today.minusDays(7);
        LocalDate monthAgo = today.minusDays(30);
        LocalDate yesterday = today.minusDays(1);

        // Today's cost
        QueryWrapper<ModelCostSummaryEntity> todayQuery = new QueryWrapper<>();
        todayQuery.select("COALESCE(SUM(estimated_cost), 0) as total_cost",
                        "COALESCE(SUM(request_count), 0) as request_count")
                .eq("project_id", projectId)
                .eq("stat_date", yesterday);
        List<Map<String, Object>> todayResult = modelCostSummaryMapper.selectMaps(todayQuery);
        if (!todayResult.isEmpty()) {
            dashboard.setTotalCostToday(toBigDecimal(todayResult.get(0).get("total_cost")));
            dashboard.setTotalRequestsToday(toLong(todayResult.get(0).get("request_count")));
        }

        // Weekly cost
        QueryWrapper<ModelCostSummaryEntity> weekQuery = new QueryWrapper<>();
        weekQuery.select("COALESCE(SUM(estimated_cost), 0) as total_cost")
                .eq("project_id", projectId)
                .ge("stat_date", weekAgo)
                .le("stat_date", yesterday);
        List<Map<String, Object>> weekResult = modelCostSummaryMapper.selectMaps(weekQuery);
        if (!weekResult.isEmpty()) {
            dashboard.setTotalCostThisWeek(toBigDecimal(weekResult.get(0).get("total_cost")));
        }

        // Monthly cost
        QueryWrapper<ModelCostSummaryEntity> monthQuery = new QueryWrapper<>();
        monthQuery.select("COALESCE(SUM(estimated_cost), 0) as total_cost")
                .eq("project_id", projectId)
                .ge("stat_date", monthAgo)
                .le("stat_date", yesterday);
        List<Map<String, Object>> monthResult = modelCostSummaryMapper.selectMaps(monthQuery);
        if (!monthResult.isEmpty()) {
            dashboard.setTotalCostThisMonth(toBigDecimal(monthResult.get(0).get("total_cost")));
        }

        // Average cost per request
        if (dashboard.getTotalRequestsToday() != null && dashboard.getTotalRequestsToday() > 0
                && dashboard.getTotalCostToday() != null) {
            BigDecimal avg = dashboard.getTotalCostToday()
                    .divide(BigDecimal.valueOf(dashboard.getTotalRequestsToday()), 6, RoundingMode.HALF_UP);
            dashboard.setAverageCostPerRequest(avg);
        }

        // Cost change vs previous week
        QueryWrapper<ModelCostSummaryEntity> prevWeekQuery = new QueryWrapper<>();
        prevWeekQuery.select("COALESCE(SUM(estimated_cost), 0) as total_cost")
                .eq("project_id", projectId)
                .ge("stat_date", weekAgo.minusDays(7))
                .lt("stat_date", weekAgo);
        List<Map<String, Object>> prevWeekResult = modelCostSummaryMapper.selectMaps(prevWeekQuery);
        BigDecimal prevWeekCost = BigDecimal.ZERO;
        if (!prevWeekResult.isEmpty()) {
            prevWeekCost = toBigDecimal(prevWeekResult.get(0).get("total_cost"));
        }
        if (prevWeekCost.compareTo(BigDecimal.ZERO) > 0 && dashboard.getTotalCostThisWeek() != null) {
            BigDecimal change = dashboard.getTotalCostThisWeek()
                    .subtract(prevWeekCost)
                    .divide(prevWeekCost, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            dashboard.setCostChangePercent(change);
        }

        // Top models by cost
        QueryWrapper<ModelCostSummaryEntity> topModelsQuery = new QueryWrapper<>();
        topModelsQuery.select("provider", "model_name",
                        "SUM(estimated_cost) as estimated_cost",
                        "SUM(request_count) as request_count",
                        "SUM(success_count) as success_count",
                        "SUM(failure_count) as failure_count",
                        "SUM(fallback_count) as fallback_count",
                        "SUM(prompt_tokens) as prompt_tokens",
                        "SUM(completion_tokens) as completion_tokens",
                        "SUM(total_tokens) as total_tokens",
                        "COALESCE(ROUND(AVG(avg_latency_ms)), 0) as avg_latency_ms")
                .eq("project_id", projectId)
                .ge("stat_date", monthAgo)
                .groupBy("provider", "model_name")
                .orderByDesc("estimated_cost")
                .last("LIMIT 5");
        List<Map<String, Object>> topModels = modelCostSummaryMapper.selectMaps(topModelsQuery);
        List<ModelCostSummaryResponse> topModelResponses = new ArrayList<>();
        for (Map<String, Object> row : topModels) {
            ModelCostSummaryResponse resp = new ModelCostSummaryResponse();
            resp.setProvider((String) row.get("provider"));
            resp.setModelName((String) row.get("model_name"));
            resp.setEstimatedCost(toBigDecimal(row.get("estimated_cost")));
            resp.setRequestCount(toLong(row.get("request_count")));
            resp.setSuccessCount(toLong(row.get("success_count")));
            resp.setFailureCount(toLong(row.get("failure_count")));
            resp.setFallbackCount(toLong(row.get("fallback_count")));
            resp.setPromptTokens(toLong(row.get("prompt_tokens")));
            resp.setCompletionTokens(toLong(row.get("completion_tokens")));
            resp.setTotalTokens(toLong(row.get("total_tokens")));
            resp.setAvgLatencyMs(toLong(row.get("avg_latency_ms")));
            topModelResponses.add(resp);
        }
        dashboard.setTopModelsByCost(topModelResponses);

        // Recent alerts
        List<ModelCostAlertEntity> recentAlerts = modelCostAlertMapper.selectList(
                new LambdaQueryWrapper<ModelCostAlertEntity>()
                        .eq(ModelCostAlertEntity::getProjectId, projectId)
                        .orderByDesc(ModelCostAlertEntity::getCreateTime)
                        .last("LIMIT 10"));
        dashboard.setRecentAlerts(recentAlerts.stream().map(this::toAlertResponse).collect(Collectors.toList()));

        return dashboard;
    }

    private ModelCostSummaryResponse toSummaryResponse(ModelCostSummaryEntity entity) {
        ModelCostSummaryResponse resp = new ModelCostSummaryResponse();
        resp.setId(entity.getId().toString());
        resp.setProjectId(entity.getProjectId().toString());
        resp.setProvider(entity.getProvider());
        resp.setModelName(entity.getModelName());
        resp.setRequestType(entity.getRequestType());
        resp.setStatDate(entity.getStatDate());
        resp.setRequestCount(entity.getRequestCount());
        resp.setSuccessCount(entity.getSuccessCount());
        resp.setFailureCount(entity.getFailureCount());
        resp.setFallbackCount(entity.getFallbackCount());
        resp.setPromptTokens(entity.getPromptTokens());
        resp.setCompletionTokens(entity.getCompletionTokens());
        resp.setTotalTokens(entity.getTotalTokens());
        resp.setEstimatedCost(entity.getEstimatedCost());
        resp.setAvgLatencyMs(entity.getAvgLatencyMs());
        resp.setCreateTime(entity.getCreateTime());
        resp.setUpdateTime(entity.getUpdateTime());
        return resp;
    }

    private ModelCostAlertResponse toAlertResponse(ModelCostAlertEntity entity) {
        ModelCostAlertResponse resp = new ModelCostAlertResponse();
        resp.setId(entity.getId().toString());
        resp.setProjectId(entity.getProjectId().toString());
        resp.setProvider(entity.getProvider());
        resp.setModelName(entity.getModelName());
        resp.setAlertType(entity.getAlertType());
        resp.setSeverity(entity.getSeverity());
        resp.setStatus(entity.getStatus());
        resp.setSummary(entity.getSummary());
        resp.setDetail(entity.getDetail());
        resp.setStatDate(entity.getStatDate());
        resp.setThresholdValue(entity.getThresholdValue());
        resp.setActualValue(entity.getActualValue());
        resp.setCreateTime(entity.getCreateTime());
        resp.setUpdateTime(entity.getUpdateTime());
        return resp;
    }

    private static Long parseLong(String value, String fieldName) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new BizException(ErrorCode.BAD_REQUEST, fieldName + " 格式无效");
        }
    }

    private static Long toLong(Object value) {
        if (value == null) return 0L;
        if (value instanceof BigDecimal) return ((BigDecimal) value).longValue();
        if (value instanceof Long) return (Long) value;
        if (value instanceof Integer) return ((Integer) value).longValue();
        return Long.parseLong(value.toString());
    }

    private static BigDecimal toBigDecimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal) return (BigDecimal) value;
        return new BigDecimal(value.toString());
    }
}
