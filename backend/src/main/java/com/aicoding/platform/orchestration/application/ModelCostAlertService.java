package com.aicoding.platform.orchestration.application;

import com.aicoding.platform.common.exception.BizException;
import com.aicoding.platform.common.exception.ErrorCode;
import com.aicoding.platform.member.application.ProjectPermissionService;
import com.aicoding.platform.member.domain.ProjectRole;
import com.aicoding.platform.orchestration.domain.ModelCostAlertEntity;
import com.aicoding.platform.orchestration.domain.ModelCostSummaryEntity;
import com.aicoding.platform.orchestration.dto.ModelCostAlertResponse;
import com.aicoding.platform.orchestration.infrastructure.ModelCostAlertMapper;
import com.aicoding.platform.orchestration.infrastructure.ModelCostSummaryMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ModelCostAlertService {

    private final ModelCostAlertMapper modelCostAlertMapper;
    private final ModelCostSummaryMapper modelCostSummaryMapper;
    private final ProjectPermissionService projectPermissionService;

    public ModelCostAlertService(ModelCostAlertMapper modelCostAlertMapper,
                                 ModelCostSummaryMapper modelCostSummaryMapper,
                                 ProjectPermissionService projectPermissionService) {
        this.modelCostAlertMapper = modelCostAlertMapper;
        this.modelCostSummaryMapper = modelCostSummaryMapper;
        this.projectPermissionService = projectPermissionService;
    }

    @Transactional
    public List<ModelCostAlertResponse> scanAlerts(String projectIdStr) {
        Long projectId = parseLong(projectIdStr, "projectId");
        projectPermissionService.checkProjectRole(projectId, ProjectRole.OWNER, ProjectRole.MAINTAINER);

        List<ModelCostAlertEntity> newAlerts = new ArrayList<>();
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        // Rule 1: DAILY_COST_SPIKE - daily cost > 7d average * 2.0
        QueryWrapper<ModelCostSummaryEntity> dailyCostQuery = new QueryWrapper<>();
        dailyCostQuery.select("provider", "model_name", "request_type",
                        "SUM(estimated_cost) as daily_cost")
                .eq("project_id", projectId)
                .eq("stat_date", yesterday)
                .groupBy("provider", "model_name", "request_type");
        List<Map<String, Object>> dailyCosts = modelCostSummaryMapper.selectMaps(dailyCostQuery);

        // Get 7-day averages
        LocalDate weekAgo = yesterday.minusDays(7);
        for (Map<String, Object> row : dailyCosts) {
            String provider = (String) row.get("provider");
            String modelName = (String) row.get("model_name");
            String requestType = (String) row.get("request_type");
            BigDecimal dailyCost = toBigDecimal(row.get("daily_cost"));

            QueryWrapper<ModelCostSummaryEntity> avgQuery = new QueryWrapper<>();
            avgQuery.select("COALESCE(AVG(estimated_cost), 0) as avg_cost")
                    .eq("project_id", projectId)
                    .eq("provider", provider)
                    .eq("model_name", modelName)
                    .eq("request_type", requestType)
                    .ge("stat_date", weekAgo)
                    .lt("stat_date", yesterday);
            List<Map<String, Object>> avgResult = modelCostSummaryMapper.selectMaps(avgQuery);
            BigDecimal avgCost = BigDecimal.ZERO;
            if (!avgResult.isEmpty()) {
                avgCost = toBigDecimal(avgResult.get(0).get("avg_cost"));
            }

            if (avgCost.compareTo(BigDecimal.ZERO) > 0
                    && dailyCost.compareTo(avgCost.multiply(BigDecimal.valueOf(2.0))) > 0) {
                ModelCostAlertEntity alert = createAlert(projectId, provider, modelName,
                        "DAILY_COST_SPIKE", "HIGH", yesterday,
                        avgCost.multiply(BigDecimal.valueOf(2.0)), dailyCost,
                        provider + "/" + modelName + " 日成本异常: $" + dailyCost
                                + " 超过阈值 $" + avgCost.multiply(BigDecimal.valueOf(2.0)));
                newAlerts.add(alert);
            }
        }

        // Rule 2: HIGH_FALLBACK_RATE - fallback rate > 30%
        QueryWrapper<ModelCostSummaryEntity> fallbackQuery = new QueryWrapper<>();
        fallbackQuery.select("provider", "model_name",
                        "SUM(request_count) as total_requests",
                        "SUM(fallback_count) as total_fallbacks")
                .eq("project_id", projectId)
                .eq("stat_date", yesterday)
                .groupBy("provider", "model_name");
        List<Map<String, Object>> fallbackRates = modelCostSummaryMapper.selectMaps(fallbackQuery);

        for (Map<String, Object> row : fallbackRates) {
            String provider = (String) row.get("provider");
            String modelName = (String) row.get("model_name");
            long totalRequests = toLong(row.get("total_requests"));
            long totalFallbacks = toLong(row.get("total_fallbacks"));

            if (totalRequests > 0) {
                double fallbackRate = (double) totalFallbacks / totalRequests;
                if (fallbackRate > 0.30) {
                    ModelCostAlertEntity alert = createAlert(projectId, provider, modelName,
                            "HIGH_FALLBACK_RATE", "MEDIUM", yesterday,
                            BigDecimal.valueOf(30), BigDecimal.valueOf(fallbackRate * 100),
                            provider + "/" + modelName + " 回退率 " + String.format("%.1f", fallbackRate * 100)
                                    + "% 超过阈值 30%");
                    newAlerts.add(alert);
                }
            }
        }

        // Rule 3: HIGH_FAILURE_COST - failure cost > $5 in a day
        QueryWrapper<ModelCostSummaryEntity> failureCostQuery = new QueryWrapper<>();
        failureCostQuery.select("provider", "model_name",
                        "SUM(estimated_cost) as failure_cost")
                .eq("project_id", projectId)
                .eq("stat_date", yesterday)
                .gt("failure_count", 0)
                .groupBy("provider", "model_name");
        List<Map<String, Object>> failureCosts = modelCostSummaryMapper.selectMaps(failureCostQuery);

        for (Map<String, Object> row : failureCosts) {
            String provider = (String) row.get("provider");
            String modelName = (String) row.get("model_name");
            BigDecimal failureCost = toBigDecimal(row.get("failure_cost"));

            if (failureCost.compareTo(BigDecimal.valueOf(5)) > 0) {
                ModelCostAlertEntity alert = createAlert(projectId, provider, modelName,
                        "HIGH_FAILURE_COST", "LOW", yesterday,
                        BigDecimal.valueOf(5), failureCost,
                        provider + "/" + modelName + " 失败成本 $" + failureCost + " 超过阈值 $5");
                newAlerts.add(alert);
            }
        }

        // Save all new alerts
        for (ModelCostAlertEntity alert : newAlerts) {
            modelCostAlertMapper.insert(alert);
        }

        return newAlerts.stream().map(this::toAlertResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ModelCostAlertResponse> listAlerts(String projectIdStr, String status, String severity, int page, int size) {
        Long projectId = parseLong(projectIdStr, "projectId");
        projectPermissionService.checkProjectMember(projectId);

        LambdaQueryWrapper<ModelCostAlertEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ModelCostAlertEntity::getProjectId, projectId);
        if (status != null && !status.isBlank()) {
            wrapper.eq(ModelCostAlertEntity::getStatus, status);
        }
        if (severity != null && !severity.isBlank()) {
            wrapper.eq(ModelCostAlertEntity::getSeverity, severity);
        }
        wrapper.orderByDesc(ModelCostAlertEntity::getCreateTime);
        wrapper.last("LIMIT " + size + " OFFSET " + (page - 1) * size);

        List<ModelCostAlertEntity> entities = modelCostAlertMapper.selectList(wrapper);
        return entities.stream().map(this::toAlertResponse).collect(Collectors.toList());
    }

    @Transactional
    public ModelCostAlertResponse updateAlertStatus(String id, String newStatus) {
        Long alertId = parseLong(id, "id");
        ModelCostAlertEntity entity = modelCostAlertMapper.selectById(alertId);
        if (entity == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "成本告警不存在");
        }
        projectPermissionService.checkProjectRole(entity.getProjectId(), ProjectRole.OWNER, ProjectRole.MAINTAINER);

        entity.setStatus(newStatus);
        modelCostAlertMapper.updateById(entity);
        return toAlertResponse(entity);
    }

    private ModelCostAlertEntity createAlert(Long projectId, String provider, String modelName,
                                              String alertType, String severity, LocalDate statDate,
                                              BigDecimal threshold, BigDecimal actual, String summary) {
        ModelCostAlertEntity alert = new ModelCostAlertEntity();
        alert.setProjectId(projectId);
        alert.setProvider(provider);
        alert.setModelName(modelName);
        alert.setAlertType(alertType);
        alert.setSeverity(severity);
        alert.setStatus("OPEN");
        alert.setStatDate(statDate);
        alert.setThresholdValue(threshold);
        alert.setActualValue(actual);
        alert.setSummary(summary);
        return alert;
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
            return Long.valueOf(value);
        } catch (NumberFormatException e) {
            throw new BizException(ErrorCode.BAD_REQUEST, fieldName + " 格式无效");
        }
    }

    private static Long toLong(Object value) {
        if (value == null) return 0L;
        if (value instanceof BigDecimal bd) return bd.longValue();
        if (value instanceof Long l) return l;
        if (value instanceof Integer i) return i.longValue();
        return Long.valueOf(value.toString());
    }

    private static BigDecimal toBigDecimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal bd) return bd;
        return new BigDecimal(value.toString());
    }
}
