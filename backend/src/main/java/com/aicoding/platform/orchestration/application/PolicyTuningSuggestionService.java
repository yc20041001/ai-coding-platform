package com.aicoding.platform.orchestration.application;

import com.aicoding.platform.orchestration.domain.*;
import com.aicoding.platform.orchestration.dto.PolicyTuningSuggestionResponse;
import com.aicoding.platform.orchestration.infrastructure.GovernanceCapacityForecastMapper;
import com.aicoding.platform.orchestration.infrastructure.GovernanceWaiverRequestMapper;
import com.aicoding.platform.orchestration.infrastructure.PolicyTuningSuggestionMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PolicyTuningSuggestionService {

    private final PolicyTuningSuggestionMapper policyTuningSuggestionMapper;
    private final GovernanceCapacityForecastMapper capacityForecastMapper;
    private final GovernanceWaiverRequestMapper governanceWaiverRequestMapper;

    public PolicyTuningSuggestionService(PolicyTuningSuggestionMapper policyTuningSuggestionMapper,
                                          GovernanceCapacityForecastMapper capacityForecastMapper,
                                          GovernanceWaiverRequestMapper governanceWaiverRequestMapper) {
        this.policyTuningSuggestionMapper = policyTuningSuggestionMapper;
        this.capacityForecastMapper = capacityForecastMapper;
        this.governanceWaiverRequestMapper = governanceWaiverRequestMapper;
    }

    @Transactional
    public void refreshSuggestions() {
        LocalDate today = LocalDate.now();
        LambdaQueryWrapper<PolicyTuningSuggestionEntity> delW = new LambdaQueryWrapper<>();
        delW.eq(PolicyTuningSuggestionEntity::getSnapshotDate, today);
        policyTuningSuggestionMapper.delete(delW);

        List<PolicyTuningSuggestionEntity> suggestions = new ArrayList<>();

        // 1. Check if any owner has high overdue
        LambdaQueryWrapper<GovernanceCapacityForecastEntity> fw = new LambdaQueryWrapper<>();
        fw.eq(GovernanceCapacityForecastEntity::getSnapshotDate, today);
        fw.eq(GovernanceCapacityForecastEntity::getForecastHorizonDays, 7);
        List<GovernanceCapacityForecastEntity> forecasts = capacityForecastMapper.selectList(fw);

        if (!forecasts.isEmpty()) {
            boolean hasHighOverdue = forecasts.stream().anyMatch(f -> f.getProjectedOverdueCount() != null && f.getProjectedOverdueCount() >= 5);
            if (hasHighOverdue) {
                PolicyTuningSuggestionEntity s = new PolicyTuningSuggestionEntity();
                s.setSnapshotDate(today);
                s.setSuggestionType("ADJUST_SLA");
                s.setPriority("P1");
                s.setTargetScope("OWNER");
                s.setCurrentValue("SLA 72h for P1");
                s.setSuggestedValue("SLA 96h for P1");
                s.setExpectedImpactText("Reduce overdue pressure on overloaded owners");
                s.setRationaleText("Multiple owners have projected overdue >= 5. Relaxing P1 SLA may reduce overdue count.");
                suggestions.add(s);
            }

            boolean hasCriticalOverdue = forecasts.stream().anyMatch(f -> f.getProjectedOverdueCount() != null && f.getProjectedOverdueCount() >= 10);
            if (hasCriticalOverdue) {
                PolicyTuningSuggestionEntity s = new PolicyTuningSuggestionEntity();
                s.setSnapshotDate(today);
                s.setSuggestionType("REBALANCE_OWNER_LOAD");
                s.setPriority("P0");
                s.setTargetScope("PORTFOLIO");
                s.setCurrentValue("Current owner distribution");
                s.setSuggestedValue("Redistribute items from overloaded to healthier owners");
                s.setExpectedImpactText("Reduce max projected overdue below 8");
                s.setRationaleText("Critical owner overload detected. Rebalancing 3-5 items per overloaded owner can reduce risk.");
                suggestions.add(s);
            }
        }

        // 2. Check waiver expiry cluster
        LambdaQueryWrapper<GovernanceWaiverRequestEntity> ww = new LambdaQueryWrapper<>();
        ww.eq(GovernanceWaiverRequestEntity::getWaiverStatus, "APPROVED");
        ww.isNotNull(GovernanceWaiverRequestEntity::getExpiresAt);
        List<GovernanceWaiverRequestEntity> waivers = governanceWaiverRequestMapper.selectList(ww);
        long expiringSoon = waivers.stream().filter(w -> w.getExpiresAt().isBefore(LocalDateTime.now().plusDays(7))
                && w.getExpiresAt().isAfter(LocalDateTime.now())).count();

        if (expiringSoon >= 3) {
            PolicyTuningSuggestionEntity s = new PolicyTuningSuggestionEntity();
            s.setSnapshotDate(today);
            s.setSuggestionType("REDUCE_WAIVER_CLUSTER");
            s.setPriority("P1");
            s.setTargetScope("WAIVER_GROUP");
            s.setCurrentValue(expiringSoon + " waivers expiring in 7 days");
            s.setSuggestedValue("Process " + (expiringSoon / 2) + " waivers before expiry");
            s.setExpectedImpactText("Reduce waiver expiry risk by 50%");
            s.setRationaleText("Waiver expiry cluster detected. Early processing reduces risk of expired waivers.");
            suggestions.add(s);
        }

        // 3. Check if blocking issues persist
        boolean hasBlocked = forecasts.stream().anyMatch(f -> f.getCapacityRiskLevel() != null
                && ("HIGH".equals(f.getCapacityRiskLevel()) || "CRITICAL".equals(f.getCapacityRiskLevel())));
        if (hasBlocked) {
            PolicyTuningSuggestionEntity s = new PolicyTuningSuggestionEntity();
            s.setSnapshotDate(today);
            s.setSuggestionType("ADJUST_GUARDRAIL_THRESHOLD");
            s.setPriority("P2");
            s.setTargetScope("PROJECT");
            s.setCurrentValue("Current guardrail thresholds");
            s.setSuggestedValue("Strengthen guardrail for high-risk projects");
            s.setExpectedImpactText("Reduce high-risk project count");
            s.setRationaleText("Multiple owners at HIGH/CRITICAL capacity risk. Consider stricter guardrails for early detection.");
            suggestions.add(s);
        }

        for (var s : suggestions) {
            policyTuningSuggestionMapper.insert(s);
        }
    }

    @Transactional(readOnly = true)
    public List<PolicyTuningSuggestionResponse> listSuggestions() {
        LocalDate today = LocalDate.now();
        LambdaQueryWrapper<PolicyTuningSuggestionEntity> w = new LambdaQueryWrapper<>();
        w.eq(PolicyTuningSuggestionEntity::getSnapshotDate, today);
        w.orderByDesc(PolicyTuningSuggestionEntity::getPriority);
        List<PolicyTuningSuggestionEntity> list = policyTuningSuggestionMapper.selectList(w);
        if (list.isEmpty()) {
            w = new LambdaQueryWrapper<>();
            w.orderByDesc(PolicyTuningSuggestionEntity::getCreateTime);
            w.last("LIMIT 50");
            list = policyTuningSuggestionMapper.selectList(w);
        }
        return list.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public int getSuggestionCount() {
        return listSuggestions().size();
    }

    private PolicyTuningSuggestionResponse toResponse(PolicyTuningSuggestionEntity e) {
        PolicyTuningSuggestionResponse r = new PolicyTuningSuggestionResponse();
        r.setId(e.getId() != null ? e.getId().toString() : null);
        r.setSnapshotDate(e.getSnapshotDate()); r.setSuggestionType(e.getSuggestionType());
        r.setPriority(e.getPriority()); r.setTargetScope(e.getTargetScope()); r.setTargetKey(e.getTargetKey());
        r.setCurrentValue(e.getCurrentValue()); r.setSuggestedValue(e.getSuggestedValue());
        r.setExpectedImpactText(e.getExpectedImpactText()); r.setRationaleText(e.getRationaleText());
        return r;
    }
}
