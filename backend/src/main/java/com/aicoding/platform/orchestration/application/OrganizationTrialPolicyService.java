package com.aicoding.platform.orchestration.application;

import com.aicoding.platform.common.exception.BizException;
import com.aicoding.platform.common.exception.ErrorCode;
import com.aicoding.platform.orchestration.domain.OrganizationTrialPolicyEntity;
import com.aicoding.platform.orchestration.dto.CreateOrganizationTrialPolicyRequest;
import com.aicoding.platform.orchestration.dto.OrganizationTrialPolicyResponse;
import com.aicoding.platform.orchestration.dto.UpdateOrganizationTrialPolicyRequest;
import com.aicoding.platform.orchestration.infrastructure.OrganizationTrialPolicyMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrganizationTrialPolicyService {

    private final OrganizationTrialPolicyMapper organizationTrialPolicyMapper;

    public OrganizationTrialPolicyService(OrganizationTrialPolicyMapper organizationTrialPolicyMapper) {
        this.organizationTrialPolicyMapper = organizationTrialPolicyMapper;
    }

    @Transactional
    public OrganizationTrialPolicyResponse createPolicy(CreateOrganizationTrialPolicyRequest request) {
        LambdaQueryWrapper<OrganizationTrialPolicyEntity> dupCheck = new LambdaQueryWrapper<>();
        dupCheck.eq(OrganizationTrialPolicyEntity::getPolicyKey, request.getPolicyKey());
        if (organizationTrialPolicyMapper.selectCount(dupCheck) > 0) {
            throw new BizException(ErrorCode.CONFLICT, "Policy key " + request.getPolicyKey() + " 已存在");
        }

        OrganizationTrialPolicyEntity entity = new OrganizationTrialPolicyEntity();
        entity.setPolicyKey(request.getPolicyKey());
        entity.setDisplayName(request.getDisplayName());
        entity.setPolicyScope(request.getPolicyScope() != null ? request.getPolicyScope() : "GLOBAL");
        entity.setEnabled(1);
        entity.setThresholdJson(request.getThresholdJson());
        entity.setSignoffPolicyJson(request.getSignoffPolicyJson());
        entity.setRollbackPolicyJson(request.getRollbackPolicyJson());
        entity.setVerificationPolicyJson(request.getVerificationPolicyJson());
        entity.setRecommendationPolicyJson(request.getRecommendationPolicyJson());
        entity.setNotes(request.getNotes());

        organizationTrialPolicyMapper.insert(entity);
        return toResponse(entity);
    }

    @Transactional(readOnly = true)
    public List<OrganizationTrialPolicyResponse> listPolicies(String scope) {
        LambdaQueryWrapper<OrganizationTrialPolicyEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(OrganizationTrialPolicyEntity::getCreateTime);
        if (scope != null && !scope.isEmpty()) {
            wrapper.eq(OrganizationTrialPolicyEntity::getPolicyScope, scope);
        }
        return organizationTrialPolicyMapper.selectList(wrapper).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public OrganizationTrialPolicyResponse getPolicy(String policyIdStr) {
        Long policyId = parseLong(policyIdStr, "policyId");
        OrganizationTrialPolicyEntity entity = organizationTrialPolicyMapper.selectById(policyId);
        if (entity == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "Organization policy 不存在");
        }
        return toResponse(entity);
    }

    @Transactional
    public OrganizationTrialPolicyResponse updatePolicy(String policyIdStr, UpdateOrganizationTrialPolicyRequest request) {
        Long policyId = parseLong(policyIdStr, "policyId");
        OrganizationTrialPolicyEntity entity = organizationTrialPolicyMapper.selectById(policyId);
        if (entity == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "Organization policy 不存在");
        }

        if (request.getDisplayName() != null) entity.setDisplayName(request.getDisplayName());
        if (request.getPolicyScope() != null) entity.setPolicyScope(request.getPolicyScope());
        if (request.getThresholdJson() != null) entity.setThresholdJson(request.getThresholdJson());
        if (request.getSignoffPolicyJson() != null) entity.setSignoffPolicyJson(request.getSignoffPolicyJson());
        if (request.getRollbackPolicyJson() != null) entity.setRollbackPolicyJson(request.getRollbackPolicyJson());
        if (request.getVerificationPolicyJson() != null) entity.setVerificationPolicyJson(request.getVerificationPolicyJson());
        if (request.getRecommendationPolicyJson() != null) entity.setRecommendationPolicyJson(request.getRecommendationPolicyJson());
        if (request.getNotes() != null) entity.setNotes(request.getNotes());
        entity.setUpdateTime(LocalDateTime.now());

        organizationTrialPolicyMapper.updateById(entity);
        return toResponse(entity);
    }

    @Transactional
    public OrganizationTrialPolicyResponse updatePolicyStatus(String policyIdStr, Boolean enabled) {
        Long policyId = parseLong(policyIdStr, "policyId");
        OrganizationTrialPolicyEntity entity = organizationTrialPolicyMapper.selectById(policyId);
        if (entity == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "Organization policy 不存在");
        }

        entity.setEnabled(Boolean.TRUE.equals(enabled) ? 1 : 0);
        entity.setUpdateTime(LocalDateTime.now());
        organizationTrialPolicyMapper.updateById(entity);
        return toResponse(entity);
    }

    @Transactional(readOnly = true)
    public List<OrganizationTrialPolicyEntity> getEnabledPolicies() {
        LambdaQueryWrapper<OrganizationTrialPolicyEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrganizationTrialPolicyEntity::getEnabled, 1);
        return organizationTrialPolicyMapper.selectList(wrapper);
    }

    private OrganizationTrialPolicyResponse toResponse(OrganizationTrialPolicyEntity entity) {
        OrganizationTrialPolicyResponse resp = new OrganizationTrialPolicyResponse();
        resp.setId(entity.getId().toString());
        resp.setPolicyKey(entity.getPolicyKey());
        resp.setDisplayName(entity.getDisplayName());
        resp.setPolicyScope(entity.getPolicyScope());
        resp.setEnabled(entity.getEnabled() != null && entity.getEnabled() == 1);
        resp.setThresholdJson(entity.getThresholdJson());
        resp.setSignoffPolicyJson(entity.getSignoffPolicyJson());
        resp.setRollbackPolicyJson(entity.getRollbackPolicyJson());
        resp.setVerificationPolicyJson(entity.getVerificationPolicyJson());
        resp.setRecommendationPolicyJson(entity.getRecommendationPolicyJson());
        resp.setNotes(entity.getNotes());
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
}
