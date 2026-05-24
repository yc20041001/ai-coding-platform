package com.aicoding.platform.orchestration.application;

import com.aicoding.platform.common.exception.BizException;
import com.aicoding.platform.common.exception.ErrorCode;
import com.aicoding.platform.member.application.ProjectPermissionService;
import com.aicoding.platform.member.domain.ProjectRole;
import com.aicoding.platform.orchestration.domain.ToolKnownIssueTemplateEntity;
import com.aicoding.platform.orchestration.dto.CreateKnownIssueTemplateRequest;
import com.aicoding.platform.orchestration.dto.KnownIssueTemplateResponse;
import com.aicoding.platform.orchestration.dto.UpdateKnownIssueTemplateRequest;
import com.aicoding.platform.orchestration.infrastructure.ToolKnownIssueTemplateMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class KnownIssueTemplateService {

    private static final Logger log = LoggerFactory.getLogger(KnownIssueTemplateService.class);

    private final ToolKnownIssueTemplateMapper templateMapper;
    private final ProjectPermissionService projectPermissionService;

    public KnownIssueTemplateService(ToolKnownIssueTemplateMapper templateMapper,
                                     ProjectPermissionService projectPermissionService) {
        this.templateMapper = templateMapper;
        this.projectPermissionService = projectPermissionService;
    }

    @Transactional
    public KnownIssueTemplateResponse createTemplate(CreateKnownIssueTemplateRequest request) {
        if (request.getProjectId() == null || request.getProjectId().isBlank()) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "projectId 不能为空");
        }
        Long projectId = parseLong(request.getProjectId(), "projectId");
        projectPermissionService.checkProjectRole(projectId, ProjectRole.OWNER, ProjectRole.MAINTAINER);

        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "标题不能为空");
        }

        ToolKnownIssueTemplateEntity entity = new ToolKnownIssueTemplateEntity();
        entity.setProjectId(projectId);
        entity.setTitle(request.getTitle().trim());
        entity.setCategory(request.getCategory());
        entity.setSeverity(request.getSeverity());
        entity.setRootCauseTemplate(request.getRootCauseTemplate());
        entity.setImpactTemplate(request.getImpactTemplate());
        entity.setResolutionTemplate(request.getResolutionTemplate());
        entity.setPreventionTemplate(request.getPreventionTemplate());
        entity.setTags(request.getTags());
        entity.setEnabled(true);

        templateMapper.insert(entity);
        log.info("Created known issue template: id={}, title={}, projectId={}",
                entity.getId(), entity.getTitle(), projectId);

        return toResponse(entity);
    }

    @Transactional
    public KnownIssueTemplateResponse updateTemplate(Long templateId, UpdateKnownIssueTemplateRequest request) {
        ToolKnownIssueTemplateEntity entity = templateMapper.selectById(templateId);
        if (entity == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "已知问题模板不存在");
        }
        projectPermissionService.checkProjectRole(entity.getProjectId(), ProjectRole.OWNER, ProjectRole.MAINTAINER);

        if (request.getTitle() != null) entity.setTitle(request.getTitle().trim());
        if (request.getCategory() != null) entity.setCategory(request.getCategory());
        if (request.getSeverity() != null) entity.setSeverity(request.getSeverity());
        if (request.getRootCauseTemplate() != null) entity.setRootCauseTemplate(request.getRootCauseTemplate());
        if (request.getImpactTemplate() != null) entity.setImpactTemplate(request.getImpactTemplate());
        if (request.getResolutionTemplate() != null) entity.setResolutionTemplate(request.getResolutionTemplate());
        if (request.getPreventionTemplate() != null) entity.setPreventionTemplate(request.getPreventionTemplate());
        if (request.getTags() != null) entity.setTags(request.getTags());
        if (request.getEnabled() != null) entity.setEnabled(request.getEnabled());

        templateMapper.updateById(entity);
        log.info("Updated known issue template: id={}", templateId);

        return toResponse(entity);
    }

    @Transactional(readOnly = true)
    public KnownIssueTemplateResponse getTemplate(Long templateId) {
        ToolKnownIssueTemplateEntity entity = templateMapper.selectById(templateId);
        if (entity == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "已知问题模板不存在");
        }
        return toResponse(entity);
    }

    @Transactional(readOnly = true)
    public List<KnownIssueTemplateResponse> listProjectTemplates(Long projectId, String category, Boolean enabled) {
        LambdaQueryWrapper<ToolKnownIssueTemplateEntity> wrapper = new LambdaQueryWrapper<ToolKnownIssueTemplateEntity>()
                .eq(ToolKnownIssueTemplateEntity::getProjectId, projectId);
        if (category != null && !category.isBlank()) {
            wrapper.eq(ToolKnownIssueTemplateEntity::getCategory, category);
        }
        if (enabled != null) {
            wrapper.eq(ToolKnownIssueTemplateEntity::getEnabled, enabled);
        }
        wrapper.orderByDesc(ToolKnownIssueTemplateEntity::getCreateTime);

        List<ToolKnownIssueTemplateEntity> entities = templateMapper.selectList(wrapper);
        return entities.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public void deleteTemplate(Long templateId) {
        ToolKnownIssueTemplateEntity entity = templateMapper.selectById(templateId);
        if (entity == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "已知问题模板不存在");
        }
        projectPermissionService.checkProjectRole(entity.getProjectId(), ProjectRole.OWNER, ProjectRole.MAINTAINER);
        templateMapper.deleteById(templateId);
        log.info("Deleted known issue template: id={}", templateId);
    }

    private KnownIssueTemplateResponse toResponse(ToolKnownIssueTemplateEntity entity) {
        KnownIssueTemplateResponse resp = new KnownIssueTemplateResponse();
        resp.setId(entity.getId().toString());
        resp.setProjectId(entity.getProjectId().toString());
        resp.setTitle(entity.getTitle());
        resp.setCategory(entity.getCategory());
        resp.setSeverity(entity.getSeverity());
        resp.setRootCauseTemplate(entity.getRootCauseTemplate());
        resp.setImpactTemplate(entity.getImpactTemplate());
        resp.setResolutionTemplate(entity.getResolutionTemplate());
        resp.setPreventionTemplate(entity.getPreventionTemplate());
        resp.setTags(entity.getTags());
        resp.setEnabled(entity.getEnabled());
        resp.setCreateTime(entity.getCreateTime());
        resp.setUpdateTime(entity.getUpdateTime());
        return resp;
    }

    private static Long parseLong(String value, String field) {
        try { return Long.valueOf(value); }
        catch (NumberFormatException e) { throw new BizException(ErrorCode.VALIDATION_ERROR, field + " 格式无效"); }
    }
}
