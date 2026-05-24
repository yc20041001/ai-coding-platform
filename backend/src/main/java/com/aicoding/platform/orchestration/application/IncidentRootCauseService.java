package com.aicoding.platform.orchestration.application;

import com.aicoding.platform.common.exception.BizException;
import com.aicoding.platform.common.exception.ErrorCode;
import com.aicoding.platform.common.pagination.PageQuery;
import com.aicoding.platform.common.pagination.PageResult;
import com.aicoding.platform.member.application.ProjectPermissionService;
import com.aicoding.platform.member.domain.ProjectRole;
import com.aicoding.platform.orchestration.domain.IncidentRootCauseNoteStatus;
import com.aicoding.platform.orchestration.domain.ToolIncidentEntity;
import com.aicoding.platform.orchestration.domain.ToolIncidentRootCauseNoteEntity;
import com.aicoding.platform.orchestration.domain.ToolKnownIssueTemplateEntity;
import com.aicoding.platform.orchestration.dto.CreateIncidentRootCauseNoteRequest;
import com.aicoding.platform.orchestration.dto.IncidentRootCauseNoteResponse;
import com.aicoding.platform.orchestration.dto.UpdateIncidentRootCauseNoteRequest;
import com.aicoding.platform.orchestration.infrastructure.ToolIncidentMapper;
import com.aicoding.platform.orchestration.infrastructure.ToolIncidentRootCauseNoteMapper;
import com.aicoding.platform.orchestration.infrastructure.ToolKnownIssueTemplateMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class IncidentRootCauseService {

    private static final Logger log = LoggerFactory.getLogger(IncidentRootCauseService.class);

    private final ToolIncidentRootCauseNoteMapper noteMapper;
    private final ToolIncidentMapper incidentMapper;
    private final ToolKnownIssueTemplateMapper templateMapper;
    private final ProjectPermissionService projectPermissionService;

    public IncidentRootCauseService(ToolIncidentRootCauseNoteMapper noteMapper,
                                    ToolIncidentMapper incidentMapper,
                                    ToolKnownIssueTemplateMapper templateMapper,
                                    ProjectPermissionService projectPermissionService) {
        this.noteMapper = noteMapper;
        this.incidentMapper = incidentMapper;
        this.templateMapper = templateMapper;
        this.projectPermissionService = projectPermissionService;
    }

    @Transactional
    public IncidentRootCauseNoteResponse createNote(Long incidentId, CreateIncidentRootCauseNoteRequest request) {
        ToolIncidentEntity incident = getIncidentOrThrow(incidentId);
        projectPermissionService.checkProjectRole(incident.getProjectId(), ProjectRole.OWNER, ProjectRole.MAINTAINER);

        List<ToolIncidentRootCauseNoteEntity> existing = noteMapper.selectList(
                new LambdaQueryWrapper<ToolIncidentRootCauseNoteEntity>()
                        .eq(ToolIncidentRootCauseNoteEntity::getIncidentId, incidentId)
                        .ne(ToolIncidentRootCauseNoteEntity::getStatus, IncidentRootCauseNoteStatus.ARCHIVED.name()));
        if (!existing.isEmpty()) {
            throw new BizException(ErrorCode.CONFLICT, "该事件已存在活跃的根因分析记录");
        }

        String status = IncidentRootCauseNoteStatus.DRAFT.name();
        String confidence = request.getConfidence() != null ? request.getConfidence() : "MEDIUM";

        ToolIncidentRootCauseNoteEntity entity = new ToolIncidentRootCauseNoteEntity();
        entity.setProjectId(incident.getProjectId());
        entity.setIncidentId(incidentId);
        entity.setRootCause(request.getRootCause());
        entity.setImpact(request.getImpact());
        entity.setResolution(request.getResolution());
        entity.setPrevention(request.getPrevention());
        entity.setFollowUpActions(request.getFollowUpActions());
        entity.setTags(request.getTags());
        entity.setConfidence(confidence);
        entity.setStatus(status);
        entity.setAuthorId(300002L);
        entity.setLastEditorId(300002L);

        noteMapper.insert(entity);
        log.info("Created root cause note: id={}, incidentId={}, status={}", entity.getId(), incidentId, status);

        return toResponse(entity);
    }

    @Transactional
    public IncidentRootCauseNoteResponse updateNote(Long noteId, UpdateIncidentRootCauseNoteRequest request) {
        ToolIncidentRootCauseNoteEntity entity = noteMapper.selectById(noteId);
        if (entity == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "根因分析记录不存在");
        }
        projectPermissionService.checkProjectRole(entity.getProjectId(), ProjectRole.OWNER, ProjectRole.MAINTAINER);

        if (IncidentRootCauseNoteStatus.ARCHIVED.name().equals(entity.getStatus())) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "已归档的记录不可修改");
        }

        if (request.getRootCause() != null) entity.setRootCause(request.getRootCause());
        if (request.getImpact() != null) entity.setImpact(request.getImpact());
        if (request.getResolution() != null) entity.setResolution(request.getResolution());
        if (request.getPrevention() != null) entity.setPrevention(request.getPrevention());
        if (request.getFollowUpActions() != null) entity.setFollowUpActions(request.getFollowUpActions());
        if (request.getTags() != null) entity.setTags(request.getTags());
        if (request.getConfidence() != null) entity.setConfidence(request.getConfidence());

        if (request.getStatus() != null) {
            String newStatus = request.getStatus();
            String oldStatus = entity.getStatus();
            if (isValidTransition(oldStatus, newStatus)) {
                entity.setStatus(newStatus);
                if (IncidentRootCauseNoteStatus.PUBLISHED.name().equals(newStatus)) {
                    entity.setPublishedAt(LocalDateTime.now());
                }
            } else {
                throw new BizException(ErrorCode.VALIDATION_ERROR,
                        "状态不允许从 " + oldStatus + " 转换到 " + newStatus);
            }
        }

        entity.setLastEditorId(300002L);
        noteMapper.updateById(entity);
        log.info("Updated root cause note: id={}, status={}", noteId, entity.getStatus());

        return toResponse(entity);
    }

    @Transactional(readOnly = true)
    public IncidentRootCauseNoteResponse getIncidentNote(Long incidentId) {
        ToolIncidentEntity incident = getIncidentOrThrow(incidentId);
        projectPermissionService.checkProjectRole(incident.getProjectId(), ProjectRole.OWNER, ProjectRole.MAINTAINER,
                ProjectRole.DEVELOPER, ProjectRole.VIEWER);

        ToolIncidentRootCauseNoteEntity entity = noteMapper.selectOne(
                new LambdaQueryWrapper<ToolIncidentRootCauseNoteEntity>()
                        .eq(ToolIncidentRootCauseNoteEntity::getIncidentId, incidentId)
                        .ne(ToolIncidentRootCauseNoteEntity::getStatus, IncidentRootCauseNoteStatus.ARCHIVED.name())
                        .last("LIMIT 1"));
        if (entity == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "该事件没有根因分析记录");
        }
        return toResponse(entity);
    }

    @Transactional(readOnly = true)
    public PageResult<IncidentRootCauseNoteResponse> listProjectNotes(Long projectId, String status, PageQuery pageQuery) {
        projectPermissionService.checkProjectRole(projectId, ProjectRole.OWNER, ProjectRole.MAINTAINER,
                ProjectRole.DEVELOPER, ProjectRole.VIEWER);

        LambdaQueryWrapper<ToolIncidentRootCauseNoteEntity> wrapper = new LambdaQueryWrapper<ToolIncidentRootCauseNoteEntity>()
                .eq(ToolIncidentRootCauseNoteEntity::getProjectId, projectId);
        if (status != null && !status.isBlank()) {
            wrapper.eq(ToolIncidentRootCauseNoteEntity::getStatus, status);
        }
        wrapper.orderByDesc(ToolIncidentRootCauseNoteEntity::getCreateTime);

        Page<ToolIncidentRootCauseNoteEntity> page = new Page<>(pageQuery.getPage(), pageQuery.getPageSize());
        Page<ToolIncidentRootCauseNoteEntity> result = noteMapper.selectPage(page, wrapper);

        List<IncidentRootCauseNoteResponse> records = result.getRecords().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return PageResult.of(records, pageQuery.getPage(), pageQuery.getPageSize(), result.getTotal());
    }

    @Transactional
    public IncidentRootCauseNoteResponse applyTemplate(Long incidentId, Long templateId) {
        ToolIncidentEntity incident = getIncidentOrThrow(incidentId);
        projectPermissionService.checkProjectRole(incident.getProjectId(), ProjectRole.OWNER, ProjectRole.MAINTAINER);

        ToolKnownIssueTemplateEntity template = templateMapper.selectById(templateId);
        if (template == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "已知问题模板不存在");
        }

        List<ToolIncidentRootCauseNoteEntity> existing = noteMapper.selectList(
                new LambdaQueryWrapper<ToolIncidentRootCauseNoteEntity>()
                        .eq(ToolIncidentRootCauseNoteEntity::getIncidentId, incidentId)
                        .ne(ToolIncidentRootCauseNoteEntity::getStatus, IncidentRootCauseNoteStatus.ARCHIVED.name()));
        if (!existing.isEmpty()) {
            throw new BizException(ErrorCode.CONFLICT, "该事件已存在活跃的根因分析记录");
        }

        ToolIncidentRootCauseNoteEntity entity = new ToolIncidentRootCauseNoteEntity();
        entity.setProjectId(incident.getProjectId());
        entity.setIncidentId(incidentId);
        entity.setRootCause(template.getRootCauseTemplate());
        entity.setImpact(template.getImpactTemplate());
        entity.setResolution(template.getResolutionTemplate());
        entity.setPrevention(template.getPreventionTemplate());
        entity.setTags(template.getTags());
        entity.setConfidence("MEDIUM");
        entity.setStatus(IncidentRootCauseNoteStatus.DRAFT.name());
        entity.setAuthorId(300002L);
        entity.setLastEditorId(300002L);

        noteMapper.insert(entity);
        log.info("Applied template {} to incident {}, note id={}", templateId, incidentId, entity.getId());

        return toResponse(entity);
    }

    @Transactional(readOnly = true)
    public String exportNoteMarkdown(Long noteId) {
        ToolIncidentRootCauseNoteEntity entity = noteMapper.selectById(noteId);
        if (entity == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "根因分析记录不存在");
        }

        ToolIncidentEntity incident = getIncidentOrThrow(entity.getIncidentId());

        StringBuilder md = new StringBuilder();
        md.append("# Root Cause Analysis: ").append(incident.getTitle()).append("\n\n");
        md.append("## Incident\n");
        md.append("- ID: ").append(entity.getIncidentId()).append("\n");
        md.append("- Severity: ").append(incident.getSeverity()).append("\n");
        md.append("- Status: ").append(incident.getStatus()).append("\n");
        md.append("- Confidence: ").append(entity.getConfidence()).append("\n\n");

        md.append("## Root Cause\n").append(entity.getRootCause() != null ? entity.getRootCause() : "未填写。").append("\n\n");
        md.append("## Impact\n").append(entity.getImpact() != null ? entity.getImpact() : "未填写。").append("\n\n");
        md.append("## Resolution\n").append(entity.getResolution() != null ? entity.getResolution() : "未填写。").append("\n\n");
        md.append("## Prevention\n").append(entity.getPrevention() != null ? entity.getPrevention() : "未填写。").append("\n\n");

        if (entity.getFollowUpActions() != null) {
            md.append("## Follow-up Actions\n").append(entity.getFollowUpActions()).append("\n\n");
        }
        if (entity.getTags() != null) {
            md.append("## Tags\n").append(entity.getTags()).append("\n\n");
        }
        md.append("---\n");
        md.append("*Created: ").append(entity.getCreateTime()).append(" | ");
        md.append("Status: ").append(entity.getStatus()).append("*\n");

        return md.toString();
    }

    private ToolIncidentEntity getIncidentOrThrow(Long incidentId) {
        ToolIncidentEntity incident = incidentMapper.selectById(incidentId);
        if (incident == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "事件不存在");
        }
        return incident;
    }

    private boolean isValidTransition(String oldStatus, String newStatus) {
        if (oldStatus.equals(newStatus)) return true;
        return switch (oldStatus) {
            case "DRAFT" -> "REVIEWED".equals(newStatus) || "ARCHIVED".equals(newStatus);
            case "REVIEWED" -> "PUBLISHED".equals(newStatus) || "DRAFT".equals(newStatus);
            case "PUBLISHED" -> "ARCHIVED".equals(newStatus);
            case "ARCHIVED" -> false;
            default -> false;
        };
    }

    private IncidentRootCauseNoteResponse toResponse(ToolIncidentRootCauseNoteEntity entity) {
        IncidentRootCauseNoteResponse resp = new IncidentRootCauseNoteResponse();
        resp.setId(entity.getId().toString());
        resp.setProjectId(entity.getProjectId().toString());
        resp.setIncidentId(entity.getIncidentId().toString());
        resp.setRootCause(entity.getRootCause());
        resp.setImpact(entity.getImpact());
        resp.setResolution(entity.getResolution());
        resp.setPrevention(entity.getPrevention());
        resp.setFollowUpActions(entity.getFollowUpActions());
        resp.setTags(entity.getTags());
        resp.setConfidence(entity.getConfidence());
        resp.setStatus(entity.getStatus());
        resp.setAuthorId(entity.getAuthorId() != null ? entity.getAuthorId().toString() : null);
        resp.setLastEditorId(entity.getLastEditorId() != null ? entity.getLastEditorId().toString() : null);
        resp.setPublishedAt(entity.getPublishedAt());
        resp.setCreateTime(entity.getCreateTime());
        resp.setUpdateTime(entity.getUpdateTime());
        return resp;
    }
}
