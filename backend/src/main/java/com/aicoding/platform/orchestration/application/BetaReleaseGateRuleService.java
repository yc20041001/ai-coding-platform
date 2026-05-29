package com.aicoding.platform.orchestration.application;

import com.aicoding.platform.common.exception.BizException;
import com.aicoding.platform.common.exception.ErrorCode;
import com.aicoding.platform.member.application.ProjectPermissionService;
import com.aicoding.platform.member.domain.ProjectRole;
import com.aicoding.platform.orchestration.domain.BetaReleaseGateRuleEntity;
import com.aicoding.platform.orchestration.dto.BetaReleaseGateRuleResponse;
import com.aicoding.platform.orchestration.infrastructure.BetaReleaseGateRuleMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BetaReleaseGateRuleService {

    private final BetaReleaseGateRuleMapper betaReleaseGateRuleMapper;
    private final ProjectPermissionService projectPermissionService;

    public BetaReleaseGateRuleService(BetaReleaseGateRuleMapper betaReleaseGateRuleMapper,
                                      ProjectPermissionService projectPermissionService) {
        this.betaReleaseGateRuleMapper = betaReleaseGateRuleMapper;
        this.projectPermissionService = projectPermissionService;
    }

    @Transactional(readOnly = true)
    public List<BetaReleaseGateRuleResponse> listRules(String projectIdStr) {
        Long projectId = parseLong(projectIdStr, "projectId");
        projectPermissionService.checkProjectMember(projectId);

        LambdaQueryWrapper<BetaReleaseGateRuleEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w.eq(BetaReleaseGateRuleEntity::getProjectId, projectId)
                .or().isNull(BetaReleaseGateRuleEntity::getProjectId));
        wrapper.eq(BetaReleaseGateRuleEntity::getEnabled, 1);
        wrapper.orderByAsc(BetaReleaseGateRuleEntity::getSortOrder);
        List<BetaReleaseGateRuleEntity> entities = betaReleaseGateRuleMapper.selectList(wrapper);
        return entities.stream().map(this::toRuleResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BetaReleaseGateRuleResponse> listAllRules(String projectIdStr) {
        Long projectId = parseLong(projectIdStr, "projectId");
        projectPermissionService.checkProjectMember(projectId);

        LambdaQueryWrapper<BetaReleaseGateRuleEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w.eq(BetaReleaseGateRuleEntity::getProjectId, projectId)
                .or().isNull(BetaReleaseGateRuleEntity::getProjectId));
        wrapper.orderByAsc(BetaReleaseGateRuleEntity::getSortOrder);
        List<BetaReleaseGateRuleEntity> entities = betaReleaseGateRuleMapper.selectList(wrapper);
        return entities.stream().map(this::toRuleResponse).collect(Collectors.toList());
    }

    @Transactional
    public BetaReleaseGateRuleResponse updateRule(String id, String enabled, String blocking,
                                                   BigDecimal thresholdValue) {
        Long ruleId = parseLong(id, "id");
        BetaReleaseGateRuleEntity entity = betaReleaseGateRuleMapper.selectById(ruleId);
        if (entity == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "发布门禁规则不存在");
        }
        if (entity.getProjectId() != null) {
            projectPermissionService.checkProjectRole(entity.getProjectId(), ProjectRole.OWNER, ProjectRole.MAINTAINER);
        }

        if (enabled != null) {
            entity.setEnabled("true".equals(enabled) || "1".equals(enabled) ? 1 : 0);
        }
        if (blocking != null) {
            entity.setBlocking("true".equals(blocking) || "1".equals(blocking) ? 1 : 0);
        }
        if (thresholdValue != null) {
            entity.setThresholdValue(thresholdValue);
        }
        betaReleaseGateRuleMapper.updateById(entity);
        return toRuleResponse(entity);
    }

    private BetaReleaseGateRuleResponse toRuleResponse(BetaReleaseGateRuleEntity entity) {
        BetaReleaseGateRuleResponse resp = new BetaReleaseGateRuleResponse();
        resp.setId(entity.getId().toString());
        resp.setProjectId(entity.getProjectId() != null ? entity.getProjectId().toString() : null);
        resp.setRuleKey(entity.getRuleKey());
        resp.setCategory(entity.getCategory());
        resp.setDisplayName(entity.getDisplayName());
        resp.setThresholdOperator(entity.getThresholdOperator());
        resp.setThresholdValue(entity.getThresholdValue());
        resp.setEnabled(entity.getEnabled());
        resp.setBlocking(entity.getBlocking());
        resp.setSortOrder(entity.getSortOrder());
        resp.setDescription(entity.getDescription());
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
