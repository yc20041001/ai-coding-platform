package com.aicoding.platform.orchestration.application;

import com.aicoding.platform.common.exception.BizException;
import com.aicoding.platform.common.exception.ErrorCode;
import com.aicoding.platform.common.pagination.PageQuery;
import com.aicoding.platform.common.pagination.PageResult;
import com.aicoding.platform.member.application.ProjectPermissionService;
import com.aicoding.platform.member.domain.ProjectRole;
import com.aicoding.platform.orchestration.domain.IncidentRegressionRisk;
import com.aicoding.platform.orchestration.domain.IncidentRetrospectiveStatus;
import com.aicoding.platform.orchestration.domain.ToolEscalationEventEntity;
import com.aicoding.platform.orchestration.domain.ToolIncidentEntity;
import com.aicoding.platform.orchestration.domain.ToolIncidentRetrospectiveEntity;
import com.aicoding.platform.orchestration.domain.ToolIncidentRootCauseNoteEntity;
import com.aicoding.platform.orchestration.domain.ToolOperatorReviewEntity;
import com.aicoding.platform.orchestration.dto.CreateIncidentRetrospectiveRequest;
import com.aicoding.platform.orchestration.dto.IncidentRetrospectiveResponse;
import com.aicoding.platform.orchestration.dto.IncidentRetrospectiveSummaryResponse;
import com.aicoding.platform.orchestration.dto.SimilarIncidentRegressionCheckResponse;
import com.aicoding.platform.orchestration.dto.SimilarIncidentResponse;
import com.aicoding.platform.orchestration.dto.UpdateIncidentRetrospectiveRequest;
import com.aicoding.platform.orchestration.infrastructure.ToolEscalationEventMapper;
import com.aicoding.platform.orchestration.infrastructure.ToolIncidentMapper;
import com.aicoding.platform.orchestration.infrastructure.ToolIncidentRetrospectiveMapper;
import com.aicoding.platform.orchestration.infrastructure.ToolIncidentRootCauseNoteMapper;
import com.aicoding.platform.orchestration.infrastructure.ToolOperatorReviewMapper;
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
public class IncidentRetrospectiveService {

    private static final Logger log = LoggerFactory.getLogger(IncidentRetrospectiveService.class);

    private final ToolIncidentRetrospectiveMapper retrospectiveMapper;
    private final ToolIncidentMapper incidentMapper;
    private final ToolIncidentRootCauseNoteMapper noteMapper;
    private final ToolOperatorReviewMapper operatorReviewMapper;
    private final ToolEscalationEventMapper escalationEventMapper;
    private final ProjectPermissionService projectPermissionService;
    private final SimilarIncidentSearchService similarSearchService;

    public IncidentRetrospectiveService(ToolIncidentRetrospectiveMapper retrospectiveMapper,
                                        ToolIncidentMapper incidentMapper,
                                        ToolIncidentRootCauseNoteMapper noteMapper,
                                        ToolOperatorReviewMapper operatorReviewMapper,
                                        ToolEscalationEventMapper escalationEventMapper,
                                        ProjectPermissionService projectPermissionService,
                                        SimilarIncidentSearchService similarSearchService) {
        this.retrospectiveMapper = retrospectiveMapper;
        this.incidentMapper = incidentMapper;
        this.noteMapper = noteMapper;
        this.operatorReviewMapper = operatorReviewMapper;
        this.escalationEventMapper = escalationEventMapper;
        this.projectPermissionService = projectPermissionService;
        this.similarSearchService = similarSearchService;
    }

    @Transactional
    public IncidentRetrospectiveResponse createDraft(Long incidentId) {
        ToolIncidentEntity incident = getIncidentOrThrow(incidentId);
        projectPermissionService.checkProjectRole(incident.getProjectId(), ProjectRole.OWNER, ProjectRole.MAINTAINER);

        List<ToolIncidentRetrospectiveEntity> existing = retrospectiveMapper.selectList(
                new LambdaQueryWrapper<ToolIncidentRetrospectiveEntity>()
                        .eq(ToolIncidentRetrospectiveEntity::getIncidentId, incidentId)
                        .ne(ToolIncidentRetrospectiveEntity::getStatus, IncidentRetrospectiveStatus.ARCHIVED.name()));
        if (!existing.isEmpty()) {
            throw new BizException(ErrorCode.CONFLICT, "该事件已存在活跃的事后回顾报告");
        }

        ToolIncidentRootCauseNoteEntity note = noteMapper.selectOne(
                new LambdaQueryWrapper<ToolIncidentRootCauseNoteEntity>()
                        .eq(ToolIncidentRootCauseNoteEntity::getIncidentId, incidentId)
                        .ne(ToolIncidentRootCauseNoteEntity::getStatus, "ARCHIVED")
                        .last("LIMIT 1"));

        List<ToolOperatorReviewEntity> reviews = operatorReviewMapper.selectList(
                new LambdaQueryWrapper<ToolOperatorReviewEntity>()
                        .eq(ToolOperatorReviewEntity::getReviewTargetId, incidentId)
                        .last("LIMIT 5"));

        List<ToolEscalationEventEntity> escalations = escalationEventMapper.selectList(
                new LambdaQueryWrapper<ToolEscalationEventEntity>()
                        .eq(ToolEscalationEventEntity::getIncidentId, incidentId)
                        .orderByDesc(ToolEscalationEventEntity::getCreateTime)
                        .last("LIMIT 10"));

        String placeholder = "待补充。";

        String whatHappened = incident.getSummary() != null
                ? incident.getSummary() : "事件: " + incident.getTitle() + "\n" + placeholder;

        StringBuilder impactSb = new StringBuilder();
        impactSb.append("严重级别: ").append(incident.getSeverity()).append("\n");
        impactSb.append("状态: ").append(incident.getStatus()).append("\n");
        if (note != null && note.getImpact() != null) {
            impactSb.append("影响分析: ").append(note.getImpact()).append("\n");
        }
        if (incident.getSlaStatus() != null) {
            impactSb.append("SLA 状态: ").append(incident.getSlaStatus()).append("\n");
        }
        impactSb.append(placeholder);
        String impactSummary = impactSb.toString();

        StringBuilder responseSb = new StringBuilder();
        if (!reviews.isEmpty()) {
            for (ToolOperatorReviewEntity review : reviews) {
                responseSb.append("- 审查: ").append(review.getTitle())
                        .append(" [").append(review.getStatus()).append("]");
                if (review.getResolution() != null) {
                    responseSb.append(" - ").append(review.getResolution());
                }
                responseSb.append("\n");
            }
        }
        responseSb.append(placeholder);
        String responseSummary = responseSb.toString();

        String lessonsLearned = note != null && note.getRootCause() != null
                ? "根因: " + note.getRootCause() + "\n" + placeholder
                : placeholder;

        String preventionPlan = note != null && note.getPrevention() != null
                ? note.getPrevention()
                : placeholder;

        String actionItems = note != null && note.getFollowUpActions() != null
                ? note.getFollowUpActions()
                : placeholder;

        String title = "事后回顾: " + incident.getTitle();

        ToolIncidentRetrospectiveEntity entity = new ToolIncidentRetrospectiveEntity();
        entity.setProjectId(incident.getProjectId());
        entity.setIncidentId(incidentId);
        entity.setRootCauseNoteId(note != null ? note.getId() : null);
        entity.setTitle(title);
        entity.setWhatHappened(whatHappened);
        entity.setImpactSummary(impactSummary);
        entity.setResponseSummary(responseSummary);
        entity.setLessonsLearned(lessonsLearned);
        entity.setPreventionPlan(preventionPlan);
        entity.setActionItems(actionItems);
        entity.setRegressionRisk(IncidentRegressionRisk.LOW.name());
        entity.setRepeatedIncident(false);
        entity.setStatus(IncidentRetrospectiveStatus.DRAFT.name());
        entity.setOwnerId(300002L);

        retrospectiveMapper.insert(entity);
        log.info("Created retrospective draft: id={}, incidentId={}", entity.getId(), incidentId);

        return toResponse(entity);
    }

    @Transactional
    public IncidentRetrospectiveResponse updateRetrospective(Long retrospectiveId, UpdateIncidentRetrospectiveRequest request) {
        ToolIncidentRetrospectiveEntity entity = retrospectiveMapper.selectById(retrospectiveId);
        if (entity == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "事后回顾报告不存在");
        }
        projectPermissionService.checkProjectRole(entity.getProjectId(), ProjectRole.OWNER, ProjectRole.MAINTAINER);

        if (IncidentRetrospectiveStatus.ARCHIVED.name().equals(entity.getStatus())) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "已归档的报告不可修改");
        }

        if (request.getTitle() != null) entity.setTitle(request.getTitle());
        if (request.getSummary() != null) entity.setSummary(request.getSummary());
        if (request.getWhatHappened() != null) entity.setWhatHappened(request.getWhatHappened());
        if (request.getImpactSummary() != null) entity.setImpactSummary(request.getImpactSummary());
        if (request.getResponseSummary() != null) entity.setResponseSummary(request.getResponseSummary());
        if (request.getLessonsLearned() != null) entity.setLessonsLearned(request.getLessonsLearned());
        if (request.getPreventionPlan() != null) entity.setPreventionPlan(request.getPreventionPlan());
        if (request.getActionItems() != null) entity.setActionItems(request.getActionItems());
        if (request.getOwnerId() != null) entity.setOwnerId(request.getOwnerId());
        if (request.getDueAt() != null) entity.setDueAt(LocalDateTime.parse(request.getDueAt()));
        if (request.getRegressionRisk() != null) entity.setRegressionRisk(request.getRegressionRisk());
        if (request.getRepeatedIncident() != null) entity.setRepeatedIncident(request.getRepeatedIncident());

        if (request.getStatus() != null) {
            String newStatus = request.getStatus();
            String oldStatus = entity.getStatus();
            if (isValidTransition(oldStatus, newStatus)) {
                entity.setStatus(newStatus);
                if (IncidentRetrospectiveStatus.PUBLISHED.name().equals(newStatus)) {
                    entity.setPublishedAt(LocalDateTime.now());
                }
            } else {
                throw new BizException(ErrorCode.VALIDATION_ERROR,
                        "状态不允许从 " + oldStatus + " 转换到 " + newStatus);
            }
        }

        retrospectiveMapper.updateById(entity);
        log.info("Updated retrospective: id={}, status={}", retrospectiveId, entity.getStatus());

        return toResponse(entity);
    }

    @Transactional(readOnly = true)
    public IncidentRetrospectiveResponse getRetrospective(Long retrospectiveId) {
        ToolIncidentRetrospectiveEntity entity = retrospectiveMapper.selectById(retrospectiveId);
        if (entity == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "事后回顾报告不存在");
        }
        projectPermissionService.checkProjectRole(entity.getProjectId(), ProjectRole.OWNER, ProjectRole.MAINTAINER,
                ProjectRole.DEVELOPER, ProjectRole.VIEWER);
        return toResponse(entity);
    }

    @Transactional(readOnly = true)
    public IncidentRetrospectiveResponse getIncidentRetrospective(Long incidentId) {
        ToolIncidentEntity incident = getIncidentOrThrow(incidentId);
        projectPermissionService.checkProjectRole(incident.getProjectId(), ProjectRole.OWNER, ProjectRole.MAINTAINER,
                ProjectRole.DEVELOPER, ProjectRole.VIEWER);

        ToolIncidentRetrospectiveEntity entity = retrospectiveMapper.selectOne(
                new LambdaQueryWrapper<ToolIncidentRetrospectiveEntity>()
                        .eq(ToolIncidentRetrospectiveEntity::getIncidentId, incidentId)
                        .ne(ToolIncidentRetrospectiveEntity::getStatus, IncidentRetrospectiveStatus.ARCHIVED.name())
                        .last("LIMIT 1"));
        if (entity == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "该事件没有事后回顾报告");
        }
        return toResponse(entity);
    }

    @Transactional(readOnly = true)
    public PageResult<IncidentRetrospectiveSummaryResponse> listProjectRetrospectives(Long projectId, String status,
                                                                                      PageQuery pageQuery) {
        projectPermissionService.checkProjectRole(projectId, ProjectRole.OWNER, ProjectRole.MAINTAINER,
                ProjectRole.DEVELOPER, ProjectRole.VIEWER);

        LambdaQueryWrapper<ToolIncidentRetrospectiveEntity> wrapper = new LambdaQueryWrapper<ToolIncidentRetrospectiveEntity>()
                .eq(ToolIncidentRetrospectiveEntity::getProjectId, projectId);
        if (status != null && !status.isBlank()) {
            wrapper.eq(ToolIncidentRetrospectiveEntity::getStatus, status);
        }
        wrapper.orderByDesc(ToolIncidentRetrospectiveEntity::getCreateTime);

        Page<ToolIncidentRetrospectiveEntity> page = new Page<>(pageQuery.getPage(), pageQuery.getPageSize());
        Page<ToolIncidentRetrospectiveEntity> result = retrospectiveMapper.selectPage(page, wrapper);

        List<IncidentRetrospectiveSummaryResponse> records = result.getRecords().stream()
                .map(this::toSummaryResponse)
                .collect(Collectors.toList());

        return PageResult.of(records, pageQuery.getPage(), pageQuery.getPageSize(), result.getTotal());
    }

    @Transactional(readOnly = true)
    public SimilarIncidentRegressionCheckResponse checkRegression(Long incidentId) {
        ToolIncidentEntity incident = getIncidentOrThrow(incidentId);
        projectPermissionService.checkProjectRole(incident.getProjectId(), ProjectRole.OWNER, ProjectRole.MAINTAINER,
                ProjectRole.DEVELOPER, ProjectRole.VIEWER);

        List<SimilarIncidentResponse> similar = similarSearchService.searchByIncident(incidentId, 20);

        SimilarIncidentRegressionCheckResponse resp = new SimilarIncidentRegressionCheckResponse();

        if (similar.isEmpty()) {
            resp.setRepeatedIncident(false);
            resp.setRegressionRisk(IncidentRegressionRisk.LOW.name());
            resp.setHighestScore(0.0);
            resp.setSimilarCount(0);
            resp.setSimilarIncidents(similar);
            return resp;
        }

        double highestScore = similar.stream().mapToDouble(SimilarIncidentResponse::getScore).max().orElse(0);
        long highScoreCount = similar.stream().filter(s -> s.getScore() >= 0.90).count();

        boolean repeated = highScoreCount >= 1;
        String risk;

        if (repeated) {
            risk = IncidentRegressionRisk.MEDIUM.name();
            boolean hasPublishedRca = similar.stream()
                    .filter(s -> s.getScore() >= 0.95)
                    .count() >= 1;
            if (hasPublishedRca) {
                risk = IncidentRegressionRisk.HIGH.name();
            }
        } else {
            risk = IncidentRegressionRisk.LOW.name();
        }

        resp.setRepeatedIncident(repeated);
        resp.setRegressionRisk(risk);
        resp.setHighestScore(highestScore);
        resp.setSimilarCount(similar.size());
        resp.setSimilarIncidents(similar);

        return resp;
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

    private ToolIncidentEntity getIncidentOrThrow(Long incidentId) {
        ToolIncidentEntity incident = incidentMapper.selectById(incidentId);
        if (incident == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "事件不存在");
        }
        return incident;
    }

    private IncidentRetrospectiveResponse toResponse(ToolIncidentRetrospectiveEntity entity) {
        IncidentRetrospectiveResponse resp = new IncidentRetrospectiveResponse();
        resp.setId(entity.getId().toString());
        resp.setProjectId(entity.getProjectId().toString());
        resp.setIncidentId(entity.getIncidentId().toString());
        resp.setRootCauseNoteId(entity.getRootCauseNoteId() != null ? entity.getRootCauseNoteId().toString() : null);
        resp.setTitle(entity.getTitle());
        resp.setSummary(entity.getSummary());
        resp.setWhatHappened(entity.getWhatHappened());
        resp.setImpactSummary(entity.getImpactSummary());
        resp.setResponseSummary(entity.getResponseSummary());
        resp.setLessonsLearned(entity.getLessonsLearned());
        resp.setPreventionPlan(entity.getPreventionPlan());
        resp.setActionItems(entity.getActionItems());
        resp.setOwnerId(entity.getOwnerId() != null ? entity.getOwnerId().toString() : null);
        resp.setDueAt(entity.getDueAt());
        resp.setRegressionRisk(entity.getRegressionRisk());
        resp.setRepeatedIncident(entity.getRepeatedIncident());
        resp.setStatus(entity.getStatus());
        resp.setPublishedAt(entity.getPublishedAt());
        resp.setCreateTime(entity.getCreateTime());
        resp.setUpdateTime(entity.getUpdateTime());
        return resp;
    }

    private IncidentRetrospectiveSummaryResponse toSummaryResponse(ToolIncidentRetrospectiveEntity entity) {
        IncidentRetrospectiveSummaryResponse resp = new IncidentRetrospectiveSummaryResponse();
        resp.setId(entity.getId().toString());
        resp.setProjectId(entity.getProjectId().toString());
        resp.setIncidentId(entity.getIncidentId().toString());
        resp.setTitle(entity.getTitle());
        resp.setSummary(entity.getSummary());
        resp.setOwnerId(entity.getOwnerId() != null ? entity.getOwnerId().toString() : null);
        resp.setDueAt(entity.getDueAt());
        resp.setRegressionRisk(entity.getRegressionRisk());
        resp.setRepeatedIncident(entity.getRepeatedIncident());
        resp.setStatus(entity.getStatus());
        resp.setPublishedAt(entity.getPublishedAt());
        resp.setCreateTime(entity.getCreateTime());
        resp.setUpdateTime(entity.getUpdateTime());
        return resp;
    }
}
