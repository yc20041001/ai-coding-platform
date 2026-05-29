package com.aicoding.platform.orchestration.application;

import com.aicoding.platform.common.exception.BizException;
import com.aicoding.platform.common.exception.ErrorCode;
import com.aicoding.platform.orchestration.domain.GovernanceSlaPolicyEntity;
import com.aicoding.platform.orchestration.dto.CreateGovernanceSlaPolicyRequest;
import com.aicoding.platform.orchestration.dto.GovernanceSlaPolicyResponse;
import com.aicoding.platform.orchestration.dto.UpdateGovernanceSlaPolicyRequest;
import com.aicoding.platform.orchestration.infrastructure.GovernanceSlaPolicyMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class GovernanceSlaPolicyService {

    private final GovernanceSlaPolicyMapper governanceSlaPolicyMapper;

    public GovernanceSlaPolicyService(GovernanceSlaPolicyMapper governanceSlaPolicyMapper) {
        this.governanceSlaPolicyMapper = governanceSlaPolicyMapper;
    }

    @Transactional
    public GovernanceSlaPolicyResponse createPolicy(CreateGovernanceSlaPolicyRequest request) {
        LambdaQueryWrapper<GovernanceSlaPolicyEntity> dupCheck = new LambdaQueryWrapper<>();
        dupCheck.eq(GovernanceSlaPolicyEntity::getPolicyKey, request.getPolicyKey());
        if (governanceSlaPolicyMapper.selectCount(dupCheck) > 0) {
            throw new BizException(ErrorCode.CONFLICT, "SLA policy key " + request.getPolicyKey() + " 已存在");
        }
        GovernanceSlaPolicyEntity entity = new GovernanceSlaPolicyEntity();
        entity.setPolicyKey(request.getPolicyKey());
        entity.setDisplayName(request.getDisplayName());
        entity.setPriority(request.getPriority());
        entity.setCategory(request.getCategory());
        entity.setSlaHours(request.getSlaHours() != null ? request.getSlaHours() : 72);
        entity.setWarningHours(request.getWarningHours() != null ? request.getWarningHours() : 48);
        entity.setEnabled(1);
        entity.setNotes(request.getNotes());
        governanceSlaPolicyMapper.insert(entity);
        return toResponse(entity);
    }

    @Transactional(readOnly = true)
    public List<GovernanceSlaPolicyResponse> listPolicies() {
        LambdaQueryWrapper<GovernanceSlaPolicyEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(GovernanceSlaPolicyEntity::getCreateTime);
        return governanceSlaPolicyMapper.selectList(wrapper).stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public GovernanceSlaPolicyResponse getPolicy(String policyIdStr) {
        GovernanceSlaPolicyEntity entity = findEntity(policyIdStr);
        return toResponse(entity);
    }

    @Transactional
    public GovernanceSlaPolicyResponse updatePolicy(String policyIdStr, UpdateGovernanceSlaPolicyRequest request) {
        GovernanceSlaPolicyEntity entity = findEntity(policyIdStr);
        if (request.getDisplayName() != null) entity.setDisplayName(request.getDisplayName());
        if (request.getPriority() != null) entity.setPriority(request.getPriority());
        if (request.getCategory() != null) entity.setCategory(request.getCategory());
        if (request.getSlaHours() != null) entity.setSlaHours(request.getSlaHours());
        if (request.getWarningHours() != null) entity.setWarningHours(request.getWarningHours());
        if (request.getNotes() != null) entity.setNotes(request.getNotes());
        entity.setUpdateTime(LocalDateTime.now());
        governanceSlaPolicyMapper.updateById(entity);
        return toResponse(entity);
    }

    @Transactional
    public GovernanceSlaPolicyResponse updatePolicyStatus(String policyIdStr, Boolean enabled) {
        GovernanceSlaPolicyEntity entity = findEntity(policyIdStr);
        entity.setEnabled(Boolean.TRUE.equals(enabled) ? 1 : 0);
        entity.setUpdateTime(LocalDateTime.now());
        governanceSlaPolicyMapper.updateById(entity);
        return toResponse(entity);
    }

    @Transactional(readOnly = true)
    public List<GovernanceSlaPolicyEntity> getEnabledPolicies() {
        LambdaQueryWrapper<GovernanceSlaPolicyEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GovernanceSlaPolicyEntity::getEnabled, 1);
        return governanceSlaPolicyMapper.selectList(wrapper);
    }

    private GovernanceSlaPolicyEntity findEntity(String idStr) {
        Long id = parseLong(idStr, "policyId");
        GovernanceSlaPolicyEntity entity = governanceSlaPolicyMapper.selectById(id);
        if (entity == null) throw new BizException(ErrorCode.NOT_FOUND, "SLA policy 不存在");
        return entity;
    }

    private GovernanceSlaPolicyResponse toResponse(GovernanceSlaPolicyEntity entity) {
        GovernanceSlaPolicyResponse resp = new GovernanceSlaPolicyResponse();
        resp.setId(entity.getId().toString());
        resp.setPolicyKey(entity.getPolicyKey());
        resp.setDisplayName(entity.getDisplayName());
        resp.setPriority(entity.getPriority());
        resp.setCategory(entity.getCategory());
        resp.setSlaHours(entity.getSlaHours());
        resp.setWarningHours(entity.getWarningHours());
        resp.setEnabled(entity.getEnabled() != null && entity.getEnabled() == 1);
        resp.setNotes(entity.getNotes());
        resp.setCreateTime(entity.getCreateTime());
        resp.setUpdateTime(entity.getUpdateTime());
        return resp;
    }

    private static Long parseLong(String value, String fieldName) {
        try { return Long.parseLong(value); }
        catch (NumberFormatException e) { throw new BizException(ErrorCode.BAD_REQUEST, fieldName + " 格式无效"); }
    }
}
