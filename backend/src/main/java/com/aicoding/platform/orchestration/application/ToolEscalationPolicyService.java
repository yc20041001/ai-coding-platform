package com.aicoding.platform.orchestration.application;

import com.aicoding.platform.common.exception.BizException;
import com.aicoding.platform.common.exception.ErrorCode;
import com.aicoding.platform.orchestration.domain.ToolEscalationPolicyEntity;
import com.aicoding.platform.orchestration.dto.CreateToolEscalationPolicyRequest;
import com.aicoding.platform.orchestration.dto.ToolEscalationPolicyResponse;
import com.aicoding.platform.orchestration.dto.UpdateToolEscalationPolicyRequest;
import com.aicoding.platform.orchestration.infrastructure.ToolEscalationPolicyMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class ToolEscalationPolicyService {

    private static final Logger log = LoggerFactory.getLogger(ToolEscalationPolicyService.class);

    private final ToolEscalationPolicyMapper policyMapper;

    public ToolEscalationPolicyService(ToolEscalationPolicyMapper policyMapper) {
        this.policyMapper = policyMapper;
    }

    @Transactional
    public ToolEscalationPolicyResponse createPolicy(CreateToolEscalationPolicyRequest request) {
        if (request.getProjectId() == null || request.getProjectId().isBlank()) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "projectId 不能为空");
        }
        if (request.getName() == null || request.getName().isBlank()) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "名称不能为空");
        }
        if (request.getSeverity() == null || request.getSeverity().isBlank()) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "severity 不能为空");
        }

        ToolEscalationPolicyEntity entity = new ToolEscalationPolicyEntity();
        entity.setProjectId(parseLong(request.getProjectId(), "projectId"));
        entity.setName(request.getName().trim());
        entity.setEnabled(true);
        entity.setSeverity(request.getSeverity());
        entity.setSlaMinutes(Objects.requireNonNullElse(request.getSlaMinutes(), 30));
        entity.setEscalationAfterMinutes(Objects.requireNonNullElse(request.getEscalationAfterMinutes(), 5));
        entity.setMaxEscalationLevel(Objects.requireNonNullElse(request.getMaxEscalationLevel(), 3));
        entity.setChannel(request.getChannel() != null ? request.getChannel() : "IN_APP");
        entity.setRouteTarget(request.getRouteTarget());

        policyMapper.insert(entity);
        log.info("Created escalation policy: id={}, name={}, projectId={}, severity={}",
                entity.getId(), entity.getName(), entity.getProjectId(), entity.getSeverity());

        return toResponse(entity);
    }

    @Transactional
    public ToolEscalationPolicyResponse updatePolicy(Long policyId, UpdateToolEscalationPolicyRequest request) {
        ToolEscalationPolicyEntity entity = policyMapper.selectById(policyId);
        if (entity == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "Escalation policy 不存在");
        }

        if (request.getName() != null) entity.setName(request.getName().trim());
        if (request.getEnabled() != null) entity.setEnabled(request.getEnabled());
        if (request.getSeverity() != null) entity.setSeverity(request.getSeverity());
        if (request.getSlaMinutes() != null) entity.setSlaMinutes(request.getSlaMinutes());
        if (request.getEscalationAfterMinutes() != null) entity.setEscalationAfterMinutes(request.getEscalationAfterMinutes());
        if (request.getMaxEscalationLevel() != null) entity.setMaxEscalationLevel(request.getMaxEscalationLevel());
        if (request.getChannel() != null) entity.setChannel(request.getChannel());
        if (request.getRouteTarget() != null) entity.setRouteTarget(request.getRouteTarget());

        policyMapper.updateById(entity);
        log.info("Updated escalation policy: id={}", policyId);

        return toResponse(entity);
    }

    @Transactional(readOnly = true)
    public ToolEscalationPolicyResponse getPolicy(Long policyId) {
        ToolEscalationPolicyEntity entity = policyMapper.selectById(policyId);
        if (entity == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "Escalation policy 不存在");
        }
        return toResponse(entity);
    }

    @Transactional(readOnly = true)
    public List<ToolEscalationPolicyResponse> listProjectPolicies(Long projectId) {
        List<ToolEscalationPolicyEntity> entities = policyMapper.selectList(
                new LambdaQueryWrapper<ToolEscalationPolicyEntity>()
                        .eq(ToolEscalationPolicyEntity::getProjectId, projectId)
                        .orderByAsc(ToolEscalationPolicyEntity::getCreateTime));

        return entities.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public void deletePolicy(Long policyId) {
        ToolEscalationPolicyEntity entity = policyMapper.selectById(policyId);
        if (entity == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "Escalation policy 不存在");
        }
        policyMapper.deleteById(policyId);
        log.info("Deleted escalation policy: id={}", policyId);
    }

    public ToolEscalationPolicyEntity findMatchingPolicy(Long projectId, String severity) {
        List<ToolEscalationPolicyEntity> policies = policyMapper.selectList(
                new LambdaQueryWrapper<ToolEscalationPolicyEntity>()
                        .eq(ToolEscalationPolicyEntity::getProjectId, projectId)
                        .eq(ToolEscalationPolicyEntity::getEnabled, true)
                        .eq(ToolEscalationPolicyEntity::getSeverity, severity)
                        .orderByAsc(ToolEscalationPolicyEntity::getEscalationAfterMinutes)
                        .last("LIMIT 1"));
        return policies.isEmpty() ? null : policies.get(0);
    }

    private ToolEscalationPolicyResponse toResponse(ToolEscalationPolicyEntity entity) {
        ToolEscalationPolicyResponse resp = new ToolEscalationPolicyResponse();
        resp.setId(entity.getId().toString());
        resp.setProjectId(entity.getProjectId().toString());
        resp.setName(entity.getName());
        resp.setEnabled(entity.getEnabled());
        resp.setSeverity(entity.getSeverity());
        resp.setSlaMinutes(entity.getSlaMinutes());
        resp.setEscalationAfterMinutes(entity.getEscalationAfterMinutes());
        resp.setMaxEscalationLevel(entity.getMaxEscalationLevel());
        resp.setChannel(entity.getChannel());
        resp.setRouteTarget(entity.getRouteTarget());
        resp.setCreateTime(entity.getCreateTime());
        resp.setUpdateTime(entity.getUpdateTime());
        return resp;
    }

    private static Long parseLong(String value, String field) {
        try { return Long.valueOf(value); }
        catch (NumberFormatException e) { throw new BizException(ErrorCode.VALIDATION_ERROR, field + " 格式无效"); }
    }
}
