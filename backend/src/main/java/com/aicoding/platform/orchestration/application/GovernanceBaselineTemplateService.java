package com.aicoding.platform.orchestration.application;

import com.aicoding.platform.common.exception.BizException;
import com.aicoding.platform.common.exception.ErrorCode;
import com.aicoding.platform.orchestration.domain.GovernanceBaselineTemplateEntity;
import com.aicoding.platform.orchestration.dto.CreateGovernanceBaselineTemplateRequest;
import com.aicoding.platform.orchestration.dto.GovernanceBaselineTemplateResponse;
import com.aicoding.platform.orchestration.dto.UpdateGovernanceBaselineTemplateRequest;
import com.aicoding.platform.orchestration.infrastructure.GovernanceBaselineTemplateMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class GovernanceBaselineTemplateService {

    private final GovernanceBaselineTemplateMapper governanceBaselineTemplateMapper;

    public GovernanceBaselineTemplateService(GovernanceBaselineTemplateMapper governanceBaselineTemplateMapper) {
        this.governanceBaselineTemplateMapper = governanceBaselineTemplateMapper;
    }

    @Transactional
    public GovernanceBaselineTemplateResponse createTemplate(CreateGovernanceBaselineTemplateRequest request) {
        // Check duplicate templateKey
        LambdaQueryWrapper<GovernanceBaselineTemplateEntity> dupCheck = new LambdaQueryWrapper<>();
        dupCheck.eq(GovernanceBaselineTemplateEntity::getTemplateKey, request.getTemplateKey());
        if (governanceBaselineTemplateMapper.selectCount(dupCheck) > 0) {
            throw new BizException(ErrorCode.CONFLICT, "Template key " + request.getTemplateKey() + " 已存在");
        }

        GovernanceBaselineTemplateEntity entity = new GovernanceBaselineTemplateEntity();
        entity.setTemplateKey(request.getTemplateKey());
        entity.setDisplayName(request.getDisplayName());
        entity.setTemplateScope(request.getTemplateScope() != null ? request.getTemplateScope() : "GLOBAL");
        entity.setEnabled(1);
        entity.setDefaultSignoffRolesJson(request.getDefaultSignoffRolesJson());
        entity.setDefaultVerificationRulesJson(request.getDefaultVerificationRulesJson());
        entity.setDefaultRollbackRequirementsJson(request.getDefaultRollbackRequirementsJson());
        entity.setDefaultConfidencePolicyJson(request.getDefaultConfidencePolicyJson());
        entity.setNotes(request.getNotes());

        governanceBaselineTemplateMapper.insert(entity);
        return toResponse(entity);
    }

    @Transactional(readOnly = true)
    public List<GovernanceBaselineTemplateResponse> listTemplates(String scope) {
        LambdaQueryWrapper<GovernanceBaselineTemplateEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(GovernanceBaselineTemplateEntity::getCreateTime);
        if (scope != null && !scope.isEmpty()) {
            wrapper.eq(GovernanceBaselineTemplateEntity::getTemplateScope, scope);
        }
        return governanceBaselineTemplateMapper.selectList(wrapper).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public GovernanceBaselineTemplateResponse getTemplate(String templateIdStr) {
        Long templateId = parseLong(templateIdStr, "templateId");
        GovernanceBaselineTemplateEntity entity = governanceBaselineTemplateMapper.selectById(templateId);
        if (entity == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "Baseline template 不存在");
        }
        return toResponse(entity);
    }

    @Transactional
    public GovernanceBaselineTemplateResponse updateTemplate(String templateIdStr, UpdateGovernanceBaselineTemplateRequest request) {
        Long templateId = parseLong(templateIdStr, "templateId");
        GovernanceBaselineTemplateEntity entity = governanceBaselineTemplateMapper.selectById(templateId);
        if (entity == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "Baseline template 不存在");
        }

        if (request.getDisplayName() != null) entity.setDisplayName(request.getDisplayName());
        if (request.getTemplateScope() != null) entity.setTemplateScope(request.getTemplateScope());
        if (request.getDefaultSignoffRolesJson() != null) entity.setDefaultSignoffRolesJson(request.getDefaultSignoffRolesJson());
        if (request.getDefaultVerificationRulesJson() != null) entity.setDefaultVerificationRulesJson(request.getDefaultVerificationRulesJson());
        if (request.getDefaultRollbackRequirementsJson() != null) entity.setDefaultRollbackRequirementsJson(request.getDefaultRollbackRequirementsJson());
        if (request.getDefaultConfidencePolicyJson() != null) entity.setDefaultConfidencePolicyJson(request.getDefaultConfidencePolicyJson());
        if (request.getNotes() != null) entity.setNotes(request.getNotes());
        entity.setUpdateTime(LocalDateTime.now());

        governanceBaselineTemplateMapper.updateById(entity);
        return toResponse(entity);
    }

    @Transactional
    public GovernanceBaselineTemplateResponse updateTemplateStatus(String templateIdStr, Boolean enabled) {
        Long templateId = parseLong(templateIdStr, "templateId");
        GovernanceBaselineTemplateEntity entity = governanceBaselineTemplateMapper.selectById(templateId);
        if (entity == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "Baseline template 不存在");
        }

        entity.setEnabled(Boolean.TRUE.equals(enabled) ? 1 : 0);
        entity.setUpdateTime(LocalDateTime.now());
        governanceBaselineTemplateMapper.updateById(entity);
        return toResponse(entity);
    }

    private GovernanceBaselineTemplateResponse toResponse(GovernanceBaselineTemplateEntity entity) {
        GovernanceBaselineTemplateResponse resp = new GovernanceBaselineTemplateResponse();
        resp.setId(entity.getId().toString());
        resp.setTemplateKey(entity.getTemplateKey());
        resp.setDisplayName(entity.getDisplayName());
        resp.setTemplateScope(entity.getTemplateScope());
        resp.setEnabled(entity.getEnabled() != null && entity.getEnabled() == 1);
        resp.setDefaultSignoffRolesJson(entity.getDefaultSignoffRolesJson());
        resp.setDefaultVerificationRulesJson(entity.getDefaultVerificationRulesJson());
        resp.setDefaultRollbackRequirementsJson(entity.getDefaultRollbackRequirementsJson());
        resp.setDefaultConfidencePolicyJson(entity.getDefaultConfidencePolicyJson());
        resp.setNotes(entity.getNotes());
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
}
