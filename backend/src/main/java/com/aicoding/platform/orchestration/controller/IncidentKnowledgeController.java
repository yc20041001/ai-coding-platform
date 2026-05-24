package com.aicoding.platform.orchestration.controller;

import com.aicoding.platform.common.pagination.PageQuery;
import com.aicoding.platform.common.pagination.PageResult;
import com.aicoding.platform.common.response.ApiResponse;
import com.aicoding.platform.orchestration.application.IncidentKnowledgeService;
import com.aicoding.platform.orchestration.application.IncidentRootCauseService;
import com.aicoding.platform.orchestration.application.KnownIssueTemplateService;
import com.aicoding.platform.orchestration.application.SimilarIncidentSearchService;
import com.aicoding.platform.orchestration.dto.CreateIncidentRootCauseNoteRequest;
import com.aicoding.platform.orchestration.dto.CreateKnownIssueTemplateRequest;
import com.aicoding.platform.orchestration.dto.GenerateIncidentKnowledgeDocumentRequest;
import com.aicoding.platform.orchestration.dto.IncidentKnowledgeDocumentDraftResponse;
import com.aicoding.platform.orchestration.dto.IncidentKnowledgeLinkResponse;
import com.aicoding.platform.orchestration.dto.IncidentRootCauseNoteResponse;
import com.aicoding.platform.orchestration.dto.KnownIssueTemplateResponse;
import com.aicoding.platform.orchestration.dto.SimilarIncidentResponse;
import com.aicoding.platform.orchestration.dto.UpdateIncidentRootCauseNoteRequest;
import com.aicoding.platform.orchestration.dto.UpdateKnownIssueTemplateRequest;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class IncidentKnowledgeController {

    private final IncidentRootCauseService rootCauseService;
    private final KnownIssueTemplateService templateService;
    private final IncidentKnowledgeService knowledgeService;
    private final SimilarIncidentSearchService similarSearchService;

    public IncidentKnowledgeController(IncidentRootCauseService rootCauseService,
                                       KnownIssueTemplateService templateService,
                                       IncidentKnowledgeService knowledgeService,
                                       SimilarIncidentSearchService similarSearchService) {
        this.rootCauseService = rootCauseService;
        this.templateService = templateService;
        this.knowledgeService = knowledgeService;
        this.similarSearchService = similarSearchService;
    }

    // --- Root Cause Notes ---

    @PostMapping("/api/orchestration/incidents/{incidentId}/root-cause-note")
    public ApiResponse<IncidentRootCauseNoteResponse> createRootCauseNote(
            @PathVariable Long incidentId,
            @RequestBody CreateIncidentRootCauseNoteRequest request) {
        return ApiResponse.ok(rootCauseService.createNote(incidentId, request));
    }

    @PutMapping("/api/orchestration/incident-root-cause-notes/{noteId}")
    public ApiResponse<IncidentRootCauseNoteResponse> updateRootCauseNote(
            @PathVariable Long noteId,
            @RequestBody UpdateIncidentRootCauseNoteRequest request) {
        return ApiResponse.ok(rootCauseService.updateNote(noteId, request));
    }

    @GetMapping("/api/orchestration/incidents/{incidentId}/root-cause-note")
    public ApiResponse<IncidentRootCauseNoteResponse> getIncidentRootCauseNote(
            @PathVariable Long incidentId) {
        return ApiResponse.ok(rootCauseService.getIncidentNote(incidentId));
    }

    @GetMapping("/api/projects/{projectId}/incident-root-cause-notes")
    public ApiResponse<PageResult<IncidentRootCauseNoteResponse>> listProjectRootCauseNotes(
            @PathVariable Long projectId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        PageQuery pageQuery = new PageQuery();
        pageQuery.setPage(page);
        pageQuery.setPageSize(pageSize);
        return ApiResponse.ok(rootCauseService.listProjectNotes(projectId, status, pageQuery));
    }

    @GetMapping("/api/orchestration/incident-root-cause-notes/{noteId}/markdown")
    public ApiResponse<String> exportRootCauseNoteMarkdown(@PathVariable Long noteId) {
        return ApiResponse.ok(rootCauseService.exportNoteMarkdown(noteId));
    }

    // --- Known Issue Templates ---

    @PostMapping("/api/projects/{projectId}/known-issue-templates")
    public ApiResponse<KnownIssueTemplateResponse> createKnownIssueTemplate(
            @PathVariable Long projectId,
            @RequestBody CreateKnownIssueTemplateRequest request) {
        request.setProjectId(projectId.toString());
        return ApiResponse.ok(templateService.createTemplate(request));
    }

    @PutMapping("/api/orchestration/known-issue-templates/{templateId}")
    public ApiResponse<KnownIssueTemplateResponse> updateKnownIssueTemplate(
            @PathVariable Long templateId,
            @RequestBody UpdateKnownIssueTemplateRequest request) {
        return ApiResponse.ok(templateService.updateTemplate(templateId, request));
    }

    @GetMapping("/api/projects/{projectId}/known-issue-templates")
    public ApiResponse<List<KnownIssueTemplateResponse>> listKnownIssueTemplates(
            @PathVariable Long projectId,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Boolean enabled) {
        return ApiResponse.ok(templateService.listProjectTemplates(projectId, category, enabled));
    }

    @PostMapping("/api/orchestration/incidents/{incidentId}/apply-known-issue-template/{templateId}")
    public ApiResponse<IncidentRootCauseNoteResponse> applyKnownIssueTemplate(
            @PathVariable Long incidentId,
            @PathVariable Long templateId) {
        return ApiResponse.ok(rootCauseService.applyTemplate(incidentId, templateId));
    }

    // --- Knowledge Links ---

    @PostMapping("/api/orchestration/incidents/{incidentId}/knowledge-document")
    public ApiResponse<IncidentKnowledgeDocumentDraftResponse> generateKnowledgeDocument(
            @PathVariable Long incidentId,
            @RequestBody GenerateIncidentKnowledgeDocumentRequest request) {
        return ApiResponse.ok(knowledgeService.generateKnowledgeDocument(incidentId, request));
    }

    @GetMapping("/api/orchestration/incidents/{incidentId}/knowledge-links")
    public ApiResponse<List<IncidentKnowledgeLinkResponse>> listKnowledgeLinks(
            @PathVariable Long incidentId) {
        return ApiResponse.ok(knowledgeService.listIncidentKnowledgeLinks(incidentId));
    }

    @DeleteMapping("/api/orchestration/incident-knowledge-links/{linkId}")
    public ApiResponse<Void> deleteKnowledgeLink(@PathVariable Long linkId) {
        knowledgeService.deleteKnowledgeLink(linkId);
        return ApiResponse.ok();
    }

    // --- Similar Incident Search ---

    @GetMapping("/api/orchestration/incidents/{incidentId}/similar")
    public ApiResponse<List<SimilarIncidentResponse>> searchSimilarIncidents(
            @PathVariable Long incidentId,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Integer limit) {
        if (query != null && !query.isBlank()) {
            return ApiResponse.ok(similarSearchService.searchSimilar(incidentId, query, limit));
        }
        return ApiResponse.ok(similarSearchService.searchByIncident(incidentId, limit));
    }
}
