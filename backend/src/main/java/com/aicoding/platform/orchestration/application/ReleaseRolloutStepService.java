package com.aicoding.platform.orchestration.application;

import com.aicoding.platform.common.exception.BizException;
import com.aicoding.platform.common.exception.ErrorCode;
import com.aicoding.platform.member.application.ProjectPermissionService;
import com.aicoding.platform.orchestration.domain.ReleaseRolloutStepEntity;
import com.aicoding.platform.orchestration.dto.CreateReleaseRolloutStepRequest;
import com.aicoding.platform.orchestration.dto.ReleaseRolloutStepResponse;
import com.aicoding.platform.orchestration.dto.UpdateReleaseRolloutStepRequest;
import com.aicoding.platform.orchestration.infrastructure.ReleaseRolloutStepMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class ReleaseRolloutStepService {

    private final ReleaseRolloutStepMapper releaseRolloutStepMapper;
    private final ProjectPermissionService projectPermissionService;

    public ReleaseRolloutStepService(ReleaseRolloutStepMapper releaseRolloutStepMapper,
                                     ProjectPermissionService projectPermissionService) {
        this.releaseRolloutStepMapper = releaseRolloutStepMapper;
        this.projectPermissionService = projectPermissionService;
    }

    @Transactional
    public void initDefaultSteps(Long planId, Long projectId, String releaseLabel) {
        String[][] defaults = {
            {"CODE_REVIEW", "代码审查", "检查本次发布代码审查是否已完成，是否有未解决的 CR 评论"},
            {"STATIC_ANALYSIS", "静态分析", "确认静态代码分析工具（SonarQube 等）未报告新增 blocker/critical 问题"},
            {"UNIT_TEST", "单元测试", "运行单元测试套件，确保通过率 >= 90%，无新增失败用例"},
            {"INTEGRATION_TEST", "集成测试", "执行集成测试套件，确保核心链路通过率 >= 85%"},
            {"STAGING_DEPLOY", "预发部署验证", "在预发环境完成部署，执行冒烟测试确保服务正常启动并响应请求"}
        };

        for (int i = 0; i < defaults.length; i++) {
            ReleaseRolloutStepEntity step = new ReleaseRolloutStepEntity();
            step.setPlanId(planId);
            step.setProjectId(projectId);
            step.setStepOrder(i + 1);
            step.setStepKey(defaults[i][0]);
            step.setDisplayName(defaults[i][1]);
            step.setStepStatus("PENDING");
            step.setVerificationScope("PRE_RELEASE");
            step.setRequired(i < 4 ? 1 : 0);
            step.setBlocking(1);
            step.setInstructions(defaults[i][2]);
            step.setExpectedResult("通过");
            releaseRolloutStepMapper.insert(step);
        }
    }

    @Transactional(readOnly = true)
    public List<ReleaseRolloutStepResponse> listSteps(String planIdStr) {
        Long planId = parseLong(planIdStr, "planId");
        LambdaQueryWrapper<ReleaseRolloutStepEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReleaseRolloutStepEntity::getPlanId, planId);
        wrapper.orderByAsc(ReleaseRolloutStepEntity::getStepOrder);
        return releaseRolloutStepMapper.selectList(wrapper).stream()
                .map(this::toStepResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ReleaseRolloutStepResponse createStep(CreateReleaseRolloutStepRequest request) {
        Long planId = parseLong(request.getPlanId(), "planId");
        Long projectId = parseLong(request.getProjectId(), "projectId");
        projectPermissionService.checkProjectMember(projectId);

        ReleaseRolloutStepEntity entity = new ReleaseRolloutStepEntity();
        entity.setPlanId(planId);
        entity.setProjectId(projectId);
        entity.setStepOrder(Objects.requireNonNullElse(request.getStepOrder(), 99));
        entity.setStepKey(request.getStepKey());
        entity.setDisplayName(request.getDisplayName());
        entity.setStepStatus("PENDING");
        entity.setVerificationScope(request.getVerificationScope() != null ? request.getVerificationScope() : "PRE_RELEASE");
        entity.setRequired(Objects.requireNonNullElse(request.getRequired(), 0));
        entity.setBlocking(Objects.requireNonNullElse(request.getBlocking(), 0));
        entity.setInstructions(request.getInstructions());
        entity.setExpectedResult(request.getExpectedResult());

        releaseRolloutStepMapper.insert(entity);
        return toStepResponse(entity);
    }

    @Transactional
    public ReleaseRolloutStepResponse updateStep(String stepIdStr, UpdateReleaseRolloutStepRequest request) {
        Long stepId = parseLong(stepIdStr, "stepId");
        ReleaseRolloutStepEntity entity = releaseRolloutStepMapper.selectById(stepId);
        if (entity == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "Rollout step 不存在");
        }
        projectPermissionService.checkProjectMember(entity.getProjectId());

        if (request.getStepStatus() != null) entity.setStepStatus(request.getStepStatus());
        if (request.getActualResult() != null) entity.setActualResult(request.getActualResult());
        if (request.getEvidenceJson() != null) entity.setEvidenceJson(request.getEvidenceJson());
        if (request.getOperatorId() != null) entity.setOperatorId(parseLong(request.getOperatorId(), "operatorId"));
        if (request.getStartedAt() != null) entity.setStartedAt(request.getStartedAt());
        if (request.getFinishedAt() != null) entity.setFinishedAt(request.getFinishedAt());

        releaseRolloutStepMapper.updateById(entity);
        return toStepResponse(entity);
    }

    @Transactional
    public ReleaseRolloutStepResponse updateStepStatus(String stepIdStr, String stepStatus,
                                                        String actualResult, String evidenceJson, String operatorId) {
        Long stepId = parseLong(stepIdStr, "stepId");
        ReleaseRolloutStepEntity entity = releaseRolloutStepMapper.selectById(stepId);
        if (entity == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "Rollout step 不存在");
        }
        projectPermissionService.checkProjectMember(entity.getProjectId());

        entity.setStepStatus(stepStatus);
        if (actualResult != null) entity.setActualResult(actualResult);
        if (evidenceJson != null) entity.setEvidenceJson(evidenceJson);
        if (operatorId != null) entity.setOperatorId(parseLong(operatorId, "operatorId"));

        if ("IN_PROGRESS".equals(stepStatus) && entity.getStartedAt() == null) {
            entity.setStartedAt(LocalDateTime.now());
        }
        if (isTerminalStatus(stepStatus) && entity.getFinishedAt() == null) {
            entity.setFinishedAt(LocalDateTime.now());
        }

        releaseRolloutStepMapper.updateById(entity);
        return toStepResponse(entity);
    }

    private boolean isTerminalStatus(String status) {
        return "PASSED".equals(status) || "FAILED".equals(status) || "SKIPPED".equals(status) || "BLOCKED".equals(status);
    }

    private ReleaseRolloutStepResponse toStepResponse(ReleaseRolloutStepEntity entity) {
        ReleaseRolloutStepResponse resp = new ReleaseRolloutStepResponse();
        resp.setId(entity.getId().toString());
        resp.setPlanId(entity.getPlanId() != null ? entity.getPlanId().toString() : null);
        resp.setProjectId(entity.getProjectId() != null ? entity.getProjectId().toString() : null);
        resp.setStepOrder(entity.getStepOrder());
        resp.setStepKey(entity.getStepKey());
        resp.setDisplayName(entity.getDisplayName());
        resp.setStepStatus(entity.getStepStatus());
        resp.setVerificationScope(entity.getVerificationScope());
        resp.setRequired(entity.getRequired());
        resp.setBlocking(entity.getBlocking());
        resp.setInstructions(entity.getInstructions());
        resp.setExpectedResult(entity.getExpectedResult());
        resp.setActualResult(entity.getActualResult());
        resp.setEvidenceJson(entity.getEvidenceJson());
        resp.setOperatorId(entity.getOperatorId() != null ? entity.getOperatorId().toString() : null);
        resp.setStartedAt(entity.getStartedAt());
        resp.setFinishedAt(entity.getFinishedAt());
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
