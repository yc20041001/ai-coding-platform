package com.aicoding.platform.orchestration.application;

import com.aicoding.platform.common.exception.BizException;
import com.aicoding.platform.common.exception.ErrorCode;
import com.aicoding.platform.orchestration.domain.GovernanceRemediationReuseBundleEntity;
import com.aicoding.platform.orchestration.dto.GovernanceRemediationReuseBundleResponse;
import com.aicoding.platform.orchestration.infrastructure.GovernanceRemediationReuseBundleMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class GovernanceRemediationReuseService {

    private final GovernanceRemediationReuseBundleMapper mapper;

    public GovernanceRemediationReuseService(GovernanceRemediationReuseBundleMapper mapper) { this.mapper = mapper; }

    @Transactional
    public GovernanceRemediationReuseBundleResponse createBundle(String bundleKey, String title, String category,
                                                                   String guardrailKey, String priority, String actionSequenceJson) {
        LambdaQueryWrapper<GovernanceRemediationReuseBundleEntity> dup = new LambdaQueryWrapper<>();
        dup.eq(GovernanceRemediationReuseBundleEntity::getBundleKey, bundleKey);
        if (mapper.selectCount(dup) > 0) throw new BizException(ErrorCode.CONFLICT, "Bundle key " + bundleKey + " 已存在");
        GovernanceRemediationReuseBundleEntity entity = new GovernanceRemediationReuseBundleEntity();
        entity.setBundleKey(bundleKey); entity.setTitle(title); entity.setCategory(category);
        entity.setGuardrailKey(guardrailKey); entity.setPriority(priority);
        entity.setEffectivenessLevel("USEFUL"); entity.setReuseCount(0);
        entity.setSuccessRate(BigDecimal.valueOf(80));
        entity.setActionSequenceJson(actionSequenceJson != null ? actionSequenceJson : "[]");
        entity.setEnabled(1);
        mapper.insert(entity);
        return toResponse(entity);
    }

    @Transactional(readOnly = true)
    public List<GovernanceRemediationReuseBundleResponse> listBundles() {
        return mapper.selectList(new LambdaQueryWrapper<GovernanceRemediationReuseBundleEntity>()
                .orderByDesc(GovernanceRemediationReuseBundleEntity::getReuseCount))
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public GovernanceRemediationReuseBundleResponse getBundle(String idStr) {
        return toResponse(findEntity(idStr));
    }

    @Transactional
    public GovernanceRemediationReuseBundleResponse updateBundle(String idStr, String title, String actionSequenceJson) {
        GovernanceRemediationReuseBundleEntity entity = findEntity(idStr);
        if (title != null) entity.setTitle(title);
        if (actionSequenceJson != null) entity.setActionSequenceJson(actionSequenceJson);
        entity.setUpdateTime(LocalDateTime.now());
        mapper.updateById(entity);
        return toResponse(entity);
    }

    @Transactional
    public GovernanceRemediationReuseBundleResponse updateBundleStatus(String idStr, Boolean enabled) {
        GovernanceRemediationReuseBundleEntity entity = findEntity(idStr);
        entity.setEnabled(Boolean.TRUE.equals(enabled) ? 1 : 0);
        entity.setUpdateTime(LocalDateTime.now());
        mapper.updateById(entity);
        return toResponse(entity);
    }

    @Transactional
    public void refreshBundles() {
        // Generate from known high-success patterns (simplified: create default bundles)
        List<GovernanceRemediationReuseBundleEntity> existing = mapper.selectList(null);
        if (existing.isEmpty()) {
            createBundle("confidence-boost", "Boost Confidence Score", "CONFIDENCE", "MIN_CONFIDENCE_SCORE", "P1",
                    "[{\"step\":\"OPEN_PLAYBOOK\"},{\"step\":\"OPEN_RECIPE\"},{\"step\":\"COMPLETE_GUIDED_TASK\"}]");
        }
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getDashboard() {
        var bundles = listBundles();
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("bundleCount", bundles.size());
        resp.put("topBundles", bundles.stream().limit(5).collect(Collectors.toList()));
        return resp;
    }

    private GovernanceRemediationReuseBundleEntity findEntity(String idStr) {
        Long id = parseLong(idStr);
        GovernanceRemediationReuseBundleEntity entity = mapper.selectById(id);
        if (entity == null) throw new BizException(ErrorCode.NOT_FOUND, "Bundle 不存在");
        return entity;
    }

    private GovernanceRemediationReuseBundleResponse toResponse(GovernanceRemediationReuseBundleEntity e) {
        GovernanceRemediationReuseBundleResponse r = new GovernanceRemediationReuseBundleResponse();
        r.setId(e.getId() != null ? e.getId().toString() : null);
        r.setBundleKey(e.getBundleKey()); r.setTitle(e.getTitle()); r.setCategory(e.getCategory());
        r.setGuardrailKey(e.getGuardrailKey()); r.setPriority(e.getPriority());
        r.setEffectivenessLevel(e.getEffectivenessLevel()); r.setReuseCount(e.getReuseCount());
        r.setSuccessRate(e.getSuccessRate()); r.setActionSequenceJson(e.getActionSequenceJson());
        r.setSourceSessionId(e.getSourceSessionId() != null ? e.getSourceSessionId().toString() : null);
        r.setSourceOperatorId(e.getSourceOperatorId() != null ? e.getSourceOperatorId().toString() : null);
        r.setSourceOperatorName(e.getSourceOperatorName());
        r.setEnabled(e.getEnabled() != null && e.getEnabled() == 1); r.setSummaryText(e.getSummaryText());
        return r;
    }

    private static Long parseLong(String v) { try { return Long.valueOf(v); } catch (NumberFormatException e) { throw new BizException(ErrorCode.BAD_REQUEST, "ID 格式无效"); } }
}
