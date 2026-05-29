package com.aicoding.platform.orchestration.application;

import com.aicoding.platform.common.exception.BizException;
import com.aicoding.platform.common.exception.ErrorCode;
import com.aicoding.platform.orchestration.domain.*;
import com.aicoding.platform.orchestration.dto.*;
import com.aicoding.platform.orchestration.infrastructure.*;
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
public class GovernanceSimulationService {

    private final GovernanceSimulationScenarioMapper scenarioMapper;
    private final GovernanceSimulationResultMapper resultMapper;
    private final GovernanceCapacityForecastMapper capacityForecastMapper;

    public GovernanceSimulationService(GovernanceSimulationScenarioMapper scenarioMapper,
                                        GovernanceSimulationResultMapper resultMapper,
                                        GovernanceCapacityForecastMapper capacityForecastMapper) {
        this.scenarioMapper = scenarioMapper;
        this.resultMapper = resultMapper;
        this.capacityForecastMapper = capacityForecastMapper;
    }

    @Transactional
    public GovernanceSimulationScenarioResponse createScenario(CreateGovernanceSimulationScenarioRequest request) {
        GovernanceSimulationScenarioEntity entity = new GovernanceSimulationScenarioEntity();
        entity.setScenarioName(request.getScenarioName());
        entity.setScenarioType(request.getScenarioType());
        entity.setScenarioStatus(DRAFT);
        entity.setInputJson(request.getInputJson() != null ? request.getInputJson() : "{}");
        entity.setNotes(request.getNotes());
        if (request.getBaselineSnapshotDate() != null) {
            entity.setBaselineSnapshotDate(LocalDate.parse(request.getBaselineSnapshotDate()));
        }
        entity.setCreatedByName("Admin");
        scenarioMapper.insert(entity);
        return toScenarioResponse(entity);
    }

    private static final String DRAFT = "DRAFT";
    private static final String READY = "READY";
    private static final String SIMULATED = "SIMULATED";
    private static final String ARCHIVED = "ARCHIVED";

    @Transactional(readOnly = true)
    public List<GovernanceSimulationScenarioResponse> listScenarios() {
        LambdaQueryWrapper<GovernanceSimulationScenarioEntity> w = new LambdaQueryWrapper<>();
        w.orderByDesc(GovernanceSimulationScenarioEntity::getCreateTime);
        return scenarioMapper.selectList(w).stream().map(this::toScenarioResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public GovernanceSimulationScenarioResponse getScenario(String idStr) {
        return toScenarioResponse(findScenario(idStr));
    }

    @Transactional
    public GovernanceSimulationScenarioResponse updateScenario(String idStr, UpdateGovernanceSimulationScenarioRequest req) {
        GovernanceSimulationScenarioEntity entity = findScenario(idStr);
        if (req.getScenarioName() != null) entity.setScenarioName(req.getScenarioName());
        if (req.getScenarioType() != null) entity.setScenarioType(req.getScenarioType());
        if (req.getInputJson() != null) entity.setInputJson(req.getInputJson());
        if (req.getNotes() != null) entity.setNotes(req.getNotes());
        entity.setUpdateTime(LocalDateTime.now());
        scenarioMapper.updateById(entity);
        return toScenarioResponse(entity);
    }

    @Transactional
    public GovernanceSimulationScenarioResponse updateScenarioStatus(String idStr, String newStatus) {
        GovernanceSimulationScenarioEntity entity = findScenario(idStr);
        String current = entity.getScenarioStatus();
        if (!isValidTransition(current, newStatus)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "Invalid transition from " + current + " to " + newStatus);
        }
        entity.setScenarioStatus(newStatus);
        entity.setUpdateTime(LocalDateTime.now());
        scenarioMapper.updateById(entity);
        return toScenarioResponse(entity);
    }

    @Transactional
    public GovernanceSimulationResultResponse runScenario(String idStr) {
        GovernanceSimulationScenarioEntity entity = findScenario(idStr);
        if (SIMULATED.equals(entity.getScenarioStatus())) {
            // Allow re-run — update existing result
        } else if (!READY.equals(entity.getScenarioStatus()) && !SIMULATED.equals(entity.getScenarioStatus())) {
            // Auto-transition to READY if DRAFT
            entity.setScenarioStatus(READY);
            entity.setUpdateTime(LocalDateTime.now());
            scenarioMapper.updateById(entity);
        }

        String type = entity.getScenarioType();
        String inputJson = entity.getInputJson();
        LocalDate today = LocalDate.now();

        // Calculate baseline from capacity forecasts
        LambdaQueryWrapper<GovernanceCapacityForecastEntity> fw = new LambdaQueryWrapper<>();
        fw.eq(GovernanceCapacityForecastEntity::getSnapshotDate, today);
        fw.eq(GovernanceCapacityForecastEntity::getForecastHorizonDays, 7);
        List<GovernanceCapacityForecastEntity> forecasts = capacityForecastMapper.selectList(fw);

        if (forecasts.isEmpty()) {
            GovernanceSimulationResultEntity invalid = new GovernanceSimulationResultEntity();
            invalid.setScenarioId(entity.getId());
            invalid.setResultStatus("INVALID");
            invalid.setSummaryText("No baseline data available");
            invalid.setCalculatedAt(LocalDateTime.now());
            invalid.setImpactedOwnerCount(0); invalid.setImpactedProjectCount(0);
            invalid.setProjectedBacklogDelta(BigDecimal.ZERO); invalid.setProjectedOverdueDelta(BigDecimal.ZERO);
            invalid.setProjectedRiskDelta(BigDecimal.ZERO); invalid.setProjectedCapacityDelta(BigDecimal.ZERO);
            invalid.setReportMarkdown("# Simulation Report\n\n**Status**: INVALID — No baseline data\n");
            resultMapper.insert(invalid);
            entity.setScenarioStatus(SIMULATED); entity.setUpdateTime(LocalDateTime.now());
            scenarioMapper.updateById(entity);
            return toResultResponse(invalid);
        }

        double totalBaselineBacklog = forecasts.stream().mapToInt(f -> f.getProjectedBacklogCount() != null ? f.getProjectedBacklogCount() : 0).sum();
        double totalBaselineOverdue = forecasts.stream().mapToInt(f -> f.getProjectedOverdueCount() != null ? f.getProjectedOverdueCount() : 0).sum();
        double totalBaselineRisk = totalBaselineOverdue * 12 + totalBaselineBacklog * 3;

        // Simulate based on type
        double backlogDelta = 0, overdueDelta = 0, riskDelta = 0, capacityDelta = 0;
        int impactedOwners = 0, impactedProjects = 0;

        switch (type) {
            case "SLA_TUNING": {
                // Assume SLA relaxation reduces overdue pressure
                double factor = 0.85;
                backlogDelta = totalBaselineBacklog * (1 - factor) * (-1);
                overdueDelta = totalBaselineOverdue * (1 - factor) * (-1);
                riskDelta = totalBaselineRisk * (1 - factor) * (-1);
                capacityDelta = totalBaselineOverdue * 0.1;
                impactedOwners = (int) forecasts.stream().filter(f -> f.getProjectedOverdueCount() > 0).count();
                break;
            }
            case "OWNER_REBALANCING": {
                backlogDelta = -Math.min(5, totalBaselineBacklog * 0.15);
                overdueDelta = -Math.min(3, totalBaselineOverdue * 0.2);
                riskDelta = totalBaselineRisk * (-0.12);
                capacityDelta = 5;
                impactedOwners = 2;
                break;
            }
            case "WAIVER_REDUCTION": {
                overdueDelta = -Math.min(4, Math.max(1, totalBaselineOverdue * 0.15));
                riskDelta = totalBaselineRisk * (-0.08);
                backlogDelta = -Math.min(3, totalBaselineBacklog * 0.05);
                capacityDelta = 2;
                impactedOwners = 1;
                impactedProjects = 1;
                break;
            }
            case "POLICY_THRESHOLD_TUNING": {
                backlogDelta = -Math.min(8, totalBaselineBacklog * 0.2);
                overdueDelta = -Math.min(5, totalBaselineOverdue * 0.25);
                riskDelta = totalBaselineRisk * (-0.15);
                capacityDelta = 3;
                impactedOwners = (int) forecasts.size();
                impactedProjects = 1;
                break;
            }
        }

        String resultStatus;
        if (backlogDelta < 0 && overdueDelta < 0 && riskDelta < 0) resultStatus = "SUCCESS";
        else if (backlogDelta <= 0 || overdueDelta <= 0) resultStatus = "WARNING";
        else resultStatus = "NO_IMPROVEMENT";

        // Delete previous result for this scenario
        LambdaQueryWrapper<GovernanceSimulationResultEntity> delW = new LambdaQueryWrapper<>();
        delW.eq(GovernanceSimulationResultEntity::getScenarioId, entity.getId());
        resultMapper.delete(delW);

        GovernanceSimulationResultEntity result = new GovernanceSimulationResultEntity();
        result.setScenarioId(entity.getId());
        result.setResultStatus(resultStatus);
        result.setImpactedOwnerCount(Math.max(1, impactedOwners));
        result.setImpactedProjectCount(Math.max(0, impactedProjects));
        result.setProjectedBacklogDelta(BigDecimal.valueOf(backlogDelta).setScale(2, RoundingMode.HALF_UP));
        result.setProjectedOverdueDelta(BigDecimal.valueOf(overdueDelta).setScale(2, RoundingMode.HALF_UP));
        result.setProjectedRiskDelta(BigDecimal.valueOf(riskDelta).setScale(2, RoundingMode.HALF_UP));
        result.setProjectedCapacityDelta(BigDecimal.valueOf(capacityDelta).setScale(2, RoundingMode.HALF_UP));
        result.setSummaryText("Simulation " + resultStatus + ": backlog " + String.format("%.0f", backlogDelta)
                + ", overdue " + String.format("%.0f", overdueDelta));
        result.setCalculatedAt(LocalDateTime.now());
        result.setReportMarkdown(buildReport(entity.getScenarioName(), type, resultStatus, backlogDelta, overdueDelta, riskDelta, capacityDelta));
        resultMapper.insert(result);

        entity.setScenarioStatus(SIMULATED);
        entity.setUpdateTime(LocalDateTime.now());
        scenarioMapper.updateById(entity);

        return toResultResponse(result);
    }

    private String buildReport(String name, String type, String status, double bl, double od, double rk, double cp) {
        StringBuilder md = new StringBuilder();
        md.append("# Simulation Report: ").append(name).append("\n\n");
        md.append("**Type**: ").append(type).append("\n\n");
        md.append("**Result**: ").append(status).append("\n\n");
        md.append("## Delta Summary\n\n");
        md.append("- Backlog Delta: ").append(String.format("%.2f", bl)).append("\n");
        md.append("- Overdue Delta: ").append(String.format("%.2f", od)).append("\n");
        md.append("- Risk Score Delta: ").append(String.format("%.2f", rk)).append("\n");
        md.append("- Capacity Delta: ").append(String.format("%.2f", cp)).append("\n\n");
        md.append("## Interpretation\n\n");
        if (bl < 0 && od < 0) md.append("Positive improvement across all metrics.");
        else if (bl < 0 || od < 0) md.append("Partial improvement — some metrics improved, others unchanged.");
        else md.append("No significant improvement detected.");
        return md.toString();
    }

    @Transactional(readOnly = true)
    public GovernanceSimulationResultResponse getResult(String scenarioIdStr) {
        Long scenarioId = parseLong(scenarioIdStr);
        LambdaQueryWrapper<GovernanceSimulationResultEntity> w = new LambdaQueryWrapper<>();
        w.eq(GovernanceSimulationResultEntity::getScenarioId, scenarioId);
        w.orderByDesc(GovernanceSimulationResultEntity::getCalculatedAt).last("LIMIT 1");
        GovernanceSimulationResultEntity entity = resultMapper.selectOne(w);
        if (entity == null) throw new BizException(ErrorCode.NOT_FOUND, "No result for this scenario");
        return toResultResponse(entity);
    }

    @Transactional(readOnly = true)
    public GovernanceSimulationComparisonResponse getComparison(String scenarioIdStr) {
        GovernanceSimulationScenarioEntity scenario = findScenario(scenarioIdStr);
        GovernanceSimulationResultResponse result = getResult(scenarioIdStr);

        GovernanceSimulationComparisonResponse cmp = new GovernanceSimulationComparisonResponse();
        cmp.setScenarioId(scenario.getId().toString());
        cmp.setScenarioName(scenario.getScenarioName());
        cmp.setScenarioType(scenario.getScenarioType());
        cmp.setBaselineProjectedBacklog(BigDecimal.ZERO);
        cmp.setSimulatedProjectedBacklog(result.getProjectedBacklogDelta());
        cmp.setBaselineProjectedOverdue(BigDecimal.ZERO);
        cmp.setSimulatedProjectedOverdue(result.getProjectedOverdueDelta());
        cmp.setBaselineRiskScore(BigDecimal.valueOf(50));
        cmp.setSimulatedRiskScore(result.getProjectedRiskDelta());
        cmp.setDeltaSummary(result.getSummaryText());
        return cmp;
    }

    @Transactional(readOnly = true)
    public GovernanceSimulationDashboardResponse getDashboard() {
        String snapshot = "DRAFT,READY,SIMULATED,ARCHIVED";
        List<GovernanceSimulationScenarioEntity> scenarios = scenarioMapper.selectList(
                new LambdaQueryWrapper<GovernanceSimulationScenarioEntity>().orderByDesc(GovernanceSimulationScenarioEntity::getCreateTime));

        int total = 0, success = 0, warning = 0, noImp = 0;
        for (var s : scenarios) {
            total++;
            if (SIMULATED.equals(s.getScenarioStatus())) {
                try {
                    var result = getResult(s.getId().toString());
                    switch (result.getResultStatus()) {
                        case "SUCCESS" -> success++;
                        case "WARNING" -> warning++;
                        case "NO_IMPROVEMENT" -> noImp++;
                    }
                } catch (Exception e) { /* ignore */ }
            }
        }

        GovernanceSimulationDashboardResponse resp = new GovernanceSimulationDashboardResponse();
        resp.setSnapshotDate(LocalDate.now());
        resp.setScenarioCount(total);
        resp.setSuccessfulScenarioCount(success);
        resp.setWarningScenarioCount(warning);
        resp.setNoImprovementCount(noImp);
        resp.setTopScenarios(scenarios.stream().limit(5).map(this::toScenarioResponse).collect(Collectors.toList()));
        resp.setTopSuggestions(new ArrayList<>());
        return resp;
    }

    @Transactional(readOnly = true)
    public String getReport() {
        StringBuilder md = new StringBuilder();
        md.append("# Governance Simulation Report\n\n");
        md.append("**Date**: ").append(LocalDate.now()).append("\n\n");
        List<GovernanceSimulationScenarioEntity> scenarios = scenarioMapper.selectList(
                new LambdaQueryWrapper<GovernanceSimulationScenarioEntity>().orderByDesc(GovernanceSimulationScenarioEntity::getCreateTime));
        for (var s : scenarios) {
            md.append("- **").append(s.getScenarioName()).append("** (").append(s.getScenarioType()).append(") — ").append(s.getScenarioStatus()).append("\n");
            if (SIMULATED.equals(s.getScenarioStatus())) {
                try {
                    var result = getResult(s.getId().toString());
                    md.append("  - Result: ").append(result.getResultStatus()).append(", delta: ").append(result.getSummaryText()).append("\n");
                } catch (Exception e) { /* ignore */ }
            }
        }
        return md.toString();
    }

    private boolean isValidTransition(String current, String next) {
        Map<String, List<String>> transitions = new HashMap<>();
        transitions.put(DRAFT, List.of(READY));
        transitions.put(READY, List.of(SIMULATED, ARCHIVED));
        transitions.put(SIMULATED, List.of(ARCHIVED, READY));
        List<String> allowed = transitions.get(current);
        return allowed != null && allowed.contains(next);
    }

    private GovernanceSimulationScenarioEntity findScenario(String idStr) {
        Long id = parseLong(idStr);
        GovernanceSimulationScenarioEntity entity = scenarioMapper.selectById(id);
        if (entity == null) throw new BizException(ErrorCode.NOT_FOUND, "Scenario 不存在");
        return entity;
    }

    private GovernanceSimulationScenarioResponse toScenarioResponse(GovernanceSimulationScenarioEntity e) {
        GovernanceSimulationScenarioResponse r = new GovernanceSimulationScenarioResponse();
        r.setId(e.getId() != null ? e.getId().toString() : null);
        r.setScenarioName(e.getScenarioName()); r.setScenarioType(e.getScenarioType());
        r.setBaselineSnapshotDate(e.getBaselineSnapshotDate()); r.setScenarioStatus(e.getScenarioStatus());
        r.setInputJson(e.getInputJson()); r.setNotes(e.getNotes());
        r.setCreatedBy(e.getCreatedBy() != null ? e.getCreatedBy().toString() : null);
        r.setCreatedByName(e.getCreatedByName());
        r.setCreateTime(e.getCreateTime()); r.setUpdateTime(e.getUpdateTime());
        return r;
    }

    private GovernanceSimulationResultResponse toResultResponse(GovernanceSimulationResultEntity e) {
        GovernanceSimulationResultResponse r = new GovernanceSimulationResultResponse();
        r.setId(e.getId() != null ? e.getId().toString() : null);
        r.setScenarioId(e.getScenarioId() != null ? e.getScenarioId().toString() : null);
        r.setResultStatus(e.getResultStatus()); r.setImpactedOwnerCount(e.getImpactedOwnerCount());
        r.setImpactedProjectCount(e.getImpactedProjectCount());
        r.setProjectedBacklogDelta(e.getProjectedBacklogDelta()); r.setProjectedOverdueDelta(e.getProjectedOverdueDelta());
        r.setProjectedRiskDelta(e.getProjectedRiskDelta()); r.setProjectedCapacityDelta(e.getProjectedCapacityDelta());
        r.setSummaryText(e.getSummaryText()); r.setDetailJson(e.getDetailJson());
        r.setReportMarkdown(e.getReportMarkdown()); r.setCalculatedAt(e.getCalculatedAt());
        r.setCreateTime(e.getCreateTime()); r.setUpdateTime(e.getUpdateTime());
        return r;
    }

    private static Long parseLong(String v) {
        try { return Long.parseLong(v); }
        catch (NumberFormatException e) { throw new BizException(ErrorCode.BAD_REQUEST, "ID 格式无效"); }
    }
}
