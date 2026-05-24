package com.aicoding.platform.orchestration.application;

import com.aicoding.platform.common.exception.BizException;
import com.aicoding.platform.common.exception.ErrorCode;
import com.aicoding.platform.member.application.ProjectPermissionService;
import com.aicoding.platform.member.domain.ProjectRole;
import com.aicoding.platform.orchestration.domain.IncidentKnowledgeLinkType;
import com.aicoding.platform.orchestration.domain.ToolIncidentEntity;
import com.aicoding.platform.orchestration.domain.ToolIncidentKnowledgeLinkEntity;
import com.aicoding.platform.orchestration.domain.ToolIncidentRootCauseNoteEntity;
import com.aicoding.platform.orchestration.dto.GenerateIncidentKnowledgeDocumentRequest;
import com.aicoding.platform.orchestration.dto.IncidentKnowledgeDocumentDraftResponse;
import com.aicoding.platform.orchestration.dto.IncidentKnowledgeLinkResponse;
import com.aicoding.platform.orchestration.dto.IncidentKnowledgeSummaryResponse;
import com.aicoding.platform.orchestration.infrastructure.ToolIncidentKnowledgeLinkMapper;
import com.aicoding.platform.orchestration.infrastructure.ToolIncidentMapper;
import com.aicoding.platform.orchestration.infrastructure.ToolIncidentRootCauseNoteMapper;
import com.aicoding.platform.rag.application.KnowledgeDocumentApplicationService;
import com.aicoding.platform.rag.dto.KnowledgeDocumentResponse;
import com.aicoding.platform.rag.dto.UploadKnowledgeDocumentRequest;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class IncidentKnowledgeService {

    private static final Logger log = LoggerFactory.getLogger(IncidentKnowledgeService.class);

    private final ToolIncidentKnowledgeLinkMapper linkMapper;
    private final ToolIncidentMapper incidentMapper;
    private final ToolIncidentRootCauseNoteMapper noteMapper;
    private final KnowledgeDocumentApplicationService knowledgeDocumentService;
    private final ProjectPermissionService projectPermissionService;

    public IncidentKnowledgeService(ToolIncidentKnowledgeLinkMapper linkMapper,
                                    ToolIncidentMapper incidentMapper,
                                    ToolIncidentRootCauseNoteMapper noteMapper,
                                    KnowledgeDocumentApplicationService knowledgeDocumentService,
                                    ProjectPermissionService projectPermissionService) {
        this.linkMapper = linkMapper;
        this.incidentMapper = incidentMapper;
        this.noteMapper = noteMapper;
        this.knowledgeDocumentService = knowledgeDocumentService;
        this.projectPermissionService = projectPermissionService;
    }

    @Transactional
    public IncidentKnowledgeDocumentDraftResponse generateKnowledgeDocument(Long incidentId,
                                                                             GenerateIncidentKnowledgeDocumentRequest request) {
        if (request.getKnowledgeBaseId() == null || request.getKnowledgeBaseId().isBlank()) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "knowledgeBaseId 不能为空");
        }

        ToolIncidentEntity incident = getIncidentOrThrow(incidentId);
        projectPermissionService.checkProjectRole(incident.getProjectId(), ProjectRole.OWNER, ProjectRole.MAINTAINER);

        ToolIncidentRootCauseNoteEntity note = noteMapper.selectOne(
                new LambdaQueryWrapper<ToolIncidentRootCauseNoteEntity>()
                        .eq(ToolIncidentRootCauseNoteEntity::getIncidentId, incidentId)
                        .ne(ToolIncidentRootCauseNoteEntity::getStatus, "ARCHIVED")
                        .last("LIMIT 1"));

        String title = request.getTitle() != null ? request.getTitle()
                : "Incident Knowledge: " + incident.getTitle();

        String content = buildKnowledgeDocumentContent(incident, note, request);

        UploadKnowledgeDocumentRequest uploadReq = new UploadKnowledgeDocumentRequest();
        uploadReq.setKnowledgeBaseId(request.getKnowledgeBaseId());
        uploadReq.setTitle(title);
        uploadReq.setDocumentType("MARKDOWN");
        uploadReq.setSourceType("INCIDENT");
        uploadReq.setFileName(title.replaceAll("[^a-zA-Z0-9\\u4e00-\\u9fa5]", "_") + ".md");
        uploadReq.setContent(content);

        KnowledgeDocumentResponse docResponse = knowledgeDocumentService.uploadDocument(
                incident.getProjectId(), uploadReq);

        ToolIncidentKnowledgeLinkEntity link = new ToolIncidentKnowledgeLinkEntity();
        link.setProjectId(incident.getProjectId());
        link.setIncidentId(incidentId);
        link.setKnowledgeBaseId(Long.valueOf(request.getKnowledgeBaseId()));
        link.setKnowledgeDocumentId(Long.valueOf(docResponse.getId()));
        link.setLinkType(IncidentKnowledgeLinkType.GENERATED_FROM_INCIDENT.name());
        link.setTitle(title);
        if (note != null) {
            link.setRootCauseNoteId(note.getId());
        }
        linkMapper.insert(link);

        log.info("Generated knowledge document for incident {}: docId={}, linkId={}",
                incidentId, docResponse.getId(), link.getId());

        IncidentKnowledgeDocumentDraftResponse resp = new IncidentKnowledgeDocumentDraftResponse();
        resp.setDocumentId(docResponse.getId());
        resp.setTitle(docResponse.getTitle());
        resp.setStatus(docResponse.getStatus());
        resp.setKnowledgeBaseId(request.getKnowledgeBaseId());
        resp.setCreateTime(docResponse.getCreateTime());
        return resp;
    }

    @Transactional(readOnly = true)
    public List<IncidentKnowledgeLinkResponse> listIncidentKnowledgeLinks(Long incidentId) {
        ToolIncidentEntity incident = getIncidentOrThrow(incidentId);
        projectPermissionService.checkProjectRole(incident.getProjectId(), ProjectRole.OWNER, ProjectRole.MAINTAINER,
                ProjectRole.DEVELOPER, ProjectRole.VIEWER);

        List<ToolIncidentKnowledgeLinkEntity> entities = linkMapper.selectList(
                new LambdaQueryWrapper<ToolIncidentKnowledgeLinkEntity>()
                        .eq(ToolIncidentKnowledgeLinkEntity::getIncidentId, incidentId)
                        .orderByDesc(ToolIncidentKnowledgeLinkEntity::getCreateTime));

        return entities.stream().map(this::toLinkResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public IncidentKnowledgeSummaryResponse getIncidentKnowledgeSummary(Long incidentId) {
        ToolIncidentEntity incident = getIncidentOrThrow(incidentId);
        projectPermissionService.checkProjectRole(incident.getProjectId(), ProjectRole.OWNER, ProjectRole.MAINTAINER,
                ProjectRole.DEVELOPER, ProjectRole.VIEWER);

        List<ToolIncidentKnowledgeLinkEntity> links = linkMapper.selectList(
                new LambdaQueryWrapper<ToolIncidentKnowledgeLinkEntity>()
                        .eq(ToolIncidentKnowledgeLinkEntity::getIncidentId, incidentId));

        IncidentKnowledgeSummaryResponse resp = new IncidentKnowledgeSummaryResponse();
        resp.setTotalLinks((long) links.size());
        resp.setDocumentCount(links.stream()
                .filter(l -> l.getKnowledgeDocumentId() != null)
                .count());
        resp.setTemplateCount(links.stream()
                .filter(l -> IncidentKnowledgeLinkType.KNOWN_ISSUE_TEMPLATE.name().equals(l.getLinkType()))
                .count());
        resp.setRootCauseNoteCount(links.stream()
                .filter(l -> l.getRootCauseNoteId() != null)
                .count());
        resp.setLatestLinkTime(links.isEmpty() ? null : links.get(0).getCreateTime());
        return resp;
    }

    @Transactional
    public void deleteKnowledgeLink(Long linkId) {
        ToolIncidentKnowledgeLinkEntity entity = linkMapper.selectById(linkId);
        if (entity == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "知识关联不存在");
        }
        projectPermissionService.checkProjectRole(entity.getProjectId(), ProjectRole.OWNER, ProjectRole.MAINTAINER);
        linkMapper.deleteById(linkId);
        log.info("Deleted knowledge link: id={}", linkId);
    }

    private String buildKnowledgeDocumentContent(ToolIncidentEntity incident,
                                                  ToolIncidentRootCauseNoteEntity note,
                                                  GenerateIncidentKnowledgeDocumentRequest request) {
        boolean includeTimeline = request.getIncludeTimeline() == null || request.getIncludeTimeline();
        boolean includeTraceSummary = request.getIncludeTraceSummary() == null || request.getIncludeTraceSummary();
        boolean includeOperatorReview = request.getIncludeOperatorReview() == null || request.getIncludeOperatorReview();
        boolean includeEscalation = request.getIncludeEscalation() == null || request.getIncludeEscalation();

        StringBuilder md = new StringBuilder();
        md.append("# Incident Knowledge: ").append(incident.getTitle()).append("\n\n");
        md.append("## Summary\n");
        md.append("- Incident: ").append(incident.getTitle()).append("\n");
        md.append("- Severity: ").append(incident.getSeverity()).append("\n");
        md.append("- Status: ").append(incident.getStatus()).append("\n");
        md.append("- SLA: ").append(incident.getSlaStatus() != null ? incident.getSlaStatus() : "N/A").append("\n");
        md.append("- Created: ").append(incident.getCreateTime()).append("\n");
        if (incident.getResolvedAt() != null) {
            md.append("- Resolved: ").append(incident.getResolvedAt()).append("\n");
        }
        md.append("\n");

        String placeholder = "未填写。";

        if (note != null) {
            md.append("## Root Cause\n").append(note.getRootCause() != null ? note.getRootCause() : placeholder).append("\n\n");
            md.append("## Impact\n").append(note.getImpact() != null ? note.getImpact() : placeholder).append("\n\n");
            md.append("## Resolution\n").append(note.getResolution() != null ? note.getResolution() : placeholder).append("\n\n");
            md.append("## Prevention\n").append(note.getPrevention() != null ? note.getPrevention() : placeholder).append("\n\n");
            if (note.getFollowUpActions() != null) {
                md.append("## Follow-up Actions\n").append(note.getFollowUpActions()).append("\n\n");
            }
        } else {
            md.append("## Root Cause\n").append(placeholder).append("\n\n");
            md.append("## Impact\n").append(placeholder).append("\n\n");
            md.append("## Resolution\n").append(placeholder).append("\n\n");
            md.append("## Prevention\n").append(placeholder).append("\n\n");
        }

        if (includeTimeline) {
            md.append("## Timeline Summary\n").append(placeholder).append("\n\n");
        }
        if (includeTraceSummary) {
            md.append("## Trace / Tool Evidence Summary\n").append(placeholder).append("\n\n");
        }
        if (includeOperatorReview) {
            md.append("## Operator Review\n").append(placeholder).append("\n\n");
        }
        if (includeEscalation) {
            md.append("## Escalation Summary\n").append(placeholder).append("\n\n");
        }

        return md.toString();
    }

    private ToolIncidentEntity getIncidentOrThrow(Long incidentId) {
        ToolIncidentEntity incident = incidentMapper.selectById(incidentId);
        if (incident == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "事件不存在");
        }
        return incident;
    }

    private IncidentKnowledgeLinkResponse toLinkResponse(ToolIncidentKnowledgeLinkEntity entity) {
        IncidentKnowledgeLinkResponse resp = new IncidentKnowledgeLinkResponse();
        resp.setId(entity.getId().toString());
        resp.setProjectId(entity.getProjectId().toString());
        resp.setIncidentId(entity.getIncidentId().toString());
        resp.setRootCauseNoteId(entity.getRootCauseNoteId() != null ? entity.getRootCauseNoteId().toString() : null);
        resp.setKnowledgeBaseId(entity.getKnowledgeBaseId() != null ? entity.getKnowledgeBaseId().toString() : null);
        resp.setKnowledgeDocumentId(entity.getKnowledgeDocumentId() != null ? entity.getKnowledgeDocumentId().toString() : null);
        resp.setLinkType(entity.getLinkType());
        resp.setTitle(entity.getTitle());
        resp.setCreateTime(entity.getCreateTime());
        return resp;
    }
}
