package com.aicoding.platform.orchestration.application;

import com.aicoding.platform.orchestration.domain.GovernanceAssistiveOrderingOptimizationEntity;
import com.aicoding.platform.orchestration.dto.GovernanceAssistiveOrderingOptimizationResponse;
import com.aicoding.platform.orchestration.infrastructure.GovernanceAssistiveOrderingOptimizationMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class GovernanceAssistiveOrderingService {

    private final GovernanceAssistiveOrderingOptimizationMapper mapper;

    public GovernanceAssistiveOrderingService(GovernanceAssistiveOrderingOptimizationMapper mapper) { this.mapper = mapper; }

    @Transactional
    public void refreshOrdering() {
        mapper.delete(new LambdaQueryWrapper<>());
        List<GovernanceAssistiveOrderingOptimizationEntity> items = new ArrayList<>();
        String[][] types = {{"OPEN_PLAYBOOK_DRAFT", "4.5", "PROMOTE"}, {"OPEN_RECIPE_DRAFT", "3.8", "KEEP"},
                            {"PREPARE_HANDOFF_NOTE", "2.5", "DEMOTE"}, {"PREPARE_WAIVER_REVIEW", "3.0", "KEEP"}};
        for (String[] t : types) {
            GovernanceAssistiveOrderingOptimizationEntity e = new GovernanceAssistiveOrderingOptimizationEntity();
            e.setActionType(t[0]); e.setAvgUsefulnessRating(BigDecimal.valueOf(Double.parseDouble(t[1])));
            e.setUsefulnessCount(10); e.setNotUsefulCount(2);
            e.setOptimizationLevel(t[2]);
            e.setSuggestedNewOrder("PROMOTE".equals(t[2]) ? 1 : "DEMOTE".equals(t[2]) ? 6 : 3);
            e.setRationaleText("Based on usefulness ratings and feedback");
            e.setCapturedAt(LocalDateTime.now());
            items.add(e);
        }
        for (var e : items) mapper.insert(e);
    }

    @Transactional(readOnly = true)
    public List<GovernanceAssistiveOrderingOptimizationResponse> listOrdering() {
        return mapper.selectList(new LambdaQueryWrapper<GovernanceAssistiveOrderingOptimizationEntity>()
                .orderByAsc(GovernanceAssistiveOrderingOptimizationEntity::getSuggestedNewOrder))
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    private GovernanceAssistiveOrderingOptimizationResponse toResponse(GovernanceAssistiveOrderingOptimizationEntity e) {
        GovernanceAssistiveOrderingOptimizationResponse r = new GovernanceAssistiveOrderingOptimizationResponse();
        r.setId(e.getId() != null ? e.getId().toString() : null);
        r.setActionType(e.getActionType()); r.setAvgUsefulnessRating(e.getAvgUsefulnessRating());
        r.setAvgActionOrder(e.getAvgActionOrder()); r.setUsefulnessCount(e.getUsefulnessCount());
        r.setNotUsefulCount(e.getNotUsefulCount()); r.setOptimizationLevel(e.getOptimizationLevel());
        r.setSuggestedNewOrder(e.getSuggestedNewOrder()); r.setRationaleText(e.getRationaleText());
        return r;
    }
}
