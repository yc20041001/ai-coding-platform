package com.aicoding.platform.github.application;

import com.aicoding.platform.audit.application.AuditLogApplicationService;
import com.aicoding.platform.audit.domain.AuditActionType;
import com.aicoding.platform.common.exception.BizException;
import com.aicoding.platform.common.exception.ErrorCode;
import com.aicoding.platform.github.domain.PrReviewFindingEntity;
import com.aicoding.platform.github.domain.PrReviewJobEntity;
import com.aicoding.platform.github.domain.PrReviewJobStatus;
import com.aicoding.platform.github.dto.CreatePrReviewRequest;
import com.aicoding.platform.github.dto.GithubPullRequestFileResponse;
import com.aicoding.platform.github.dto.GithubPullRequestResponse;
import com.aicoding.platform.github.dto.PrReviewFindingResponse;
import com.aicoding.platform.github.dto.PrReviewJobResponse;
import com.aicoding.platform.github.infrastructure.PrReviewFindingMapper;
import com.aicoding.platform.github.infrastructure.PrReviewJobMapper;
import com.aicoding.platform.member.application.ProjectPermissionService;
import com.aicoding.platform.member.domain.ProjectRole;
import com.aicoding.platform.modelgateway.application.ModelGateway;
import com.aicoding.platform.modelgateway.dto.ModelRequest;
import com.aicoding.platform.modelgateway.dto.ModelResponse;
import com.aicoding.platform.security.context.LoginUser;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class PrReviewApplicationService {

    private static final Logger log = LoggerFactory.getLogger(PrReviewApplicationService.class);

    private final PrReviewJobMapper reviewJobMapper;
    private final PrReviewFindingMapper findingMapper;
    private final GithubPullRequestService prService;
    private final ModelGateway modelGateway;
    private final ProjectPermissionService permissionService;
    private final AuditLogApplicationService auditLogApplicationService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PrReviewApplicationService(PrReviewJobMapper reviewJobMapper,
                                      PrReviewFindingMapper findingMapper,
                                      GithubPullRequestService prService,
                                      ModelGateway modelGateway,
                                      ProjectPermissionService permissionService,
                                      AuditLogApplicationService auditLogApplicationService) {
        this.reviewJobMapper = reviewJobMapper;
        this.findingMapper = findingMapper;
        this.prService = prService;
        this.modelGateway = modelGateway;
        this.permissionService = permissionService;
        this.auditLogApplicationService = auditLogApplicationService;
    }

    @Transactional
    public PrReviewJobResponse create(Long projectId, CreatePrReviewRequest request) {
        LoginUser user = permissionService.requireCurrentUser();
        permissionService.checkProjectRole(projectId, ProjectRole.MAINTAINER, ProjectRole.OWNER);

        // Fetch PR detail and cache
        GithubPullRequestResponse pr = prService.getDetail(
                request.getOwner(), request.getRepo(), request.getPullRequestNumber());

        // Create review job
        PrReviewJobEntity job = new PrReviewJobEntity();
        job.setProjectId(projectId);
        job.setPullRequestId(Long.valueOf(pr.getId()));
        job.setAgentId(request.getAgentId());
        job.setStatus(PrReviewJobStatus.PENDING.name());
        job.setReviewMode(request.getReviewMode() != null ? request.getReviewMode().toUpperCase() : "FULL");
        job.setCreatorId(user.getUserId());
        job.setCreateTime(LocalDateTime.now());
        job.setUpdateTime(LocalDateTime.now());
        reviewJobMapper.insert(job);

        auditLogApplicationService.recordSuccess(projectId, job.getId(),
                AuditActionType.PR_REVIEW_START.name(), "PR_REVIEW",
                "PR Review 已创建: " + request.getOwner() + "/" + request.getRepo() + " #" + request.getPullRequestNumber());

        // Execute review synchronously
        try {
            executeReview(job, pr, request);
        } catch (Exception e) {
            log.error("PR Review execution failed: reviewJobId={} message={}", job.getId(), e.getMessage());
            job.setStatus(PrReviewJobStatus.FAILED.name());
            job.setErrorMessage(e.getMessage() != null && e.getMessage().length() > 2000
                    ? e.getMessage().substring(0, 2000) : e.getMessage());
            job.setFinishedAt(LocalDateTime.now());
            job.setUpdateTime(LocalDateTime.now());
            reviewJobMapper.updateById(job);

            auditLogApplicationService.recordFailure(projectId, job.getId(),
                    AuditActionType.PR_REVIEW_FAILED.name(), "PR_REVIEW",
                    "PR Review 失败: " + job.getErrorMessage(), job.getErrorMessage());
        }

        return toJobResponse(reviewJobMapper.selectById(job.getId()));
    }

    private void executeReview(PrReviewJobEntity job, GithubPullRequestResponse pr, CreatePrReviewRequest request) {
        job.setStatus(PrReviewJobStatus.RUNNING.name());
        job.setStartedAt(LocalDateTime.now());
        job.setUpdateTime(LocalDateTime.now());
        reviewJobMapper.updateById(job);

        // Get changed files and patch
        List<GithubPullRequestFileResponse> files = prService.getFiles(
                request.getOwner(), request.getRepo(), request.getPullRequestNumber());
        String patch = prService.getPatch(request.getOwner(), request.getRepo(), request.getPullRequestNumber());

        // Build prompt
        String systemPrompt = buildSystemPrompt(job.getReviewMode());
        String userPrompt = buildUserPrompt(pr, files, patch, job.getReviewMode());

        // Call Model Gateway
        ModelRequest modelRequest = new ModelRequest();
        modelRequest.setRequestType("CODE_REVIEW");
        modelRequest.setSystemPrompt(systemPrompt);
        modelRequest.setUserPrompt(userPrompt);
        modelRequest.setMaxTokens(4096);

        ModelResponse response = modelGateway.generate(modelRequest);

        if (!response.getSuccess()) {
            throw new BizException(ErrorCode.AI_PROVIDER_ERROR,
                    "Model call failed: " + response.getErrorMessage());
        }

        // Update job with results
        job.setModelProvider(response.getProvider());
        job.setModelName(response.getModelName());
        job.setTokenUsage(response.getTotalTokens());
        job.setUpdateTime(LocalDateTime.now());

        // Parse JSON from response
        String content = response.getContent() != null ? response.getContent() : "";
        JsonNode parsed = parseReviewJson(content);

        if (parsed != null) {
            job.setSummary(parsed.has("summary") ? parsed.get("summary").asText() : content);
            String risk = parsed.has("riskLevel") ? parsed.get("riskLevel").asText().toUpperCase() : null;
            job.setRiskLevel(validateRiskLevel(risk));
            job.setStatus(PrReviewJobStatus.COMPLETED.name());
        } else {
            // Non-JSON fallback: save raw content as summary, no findings
            job.setSummary(content);
            job.setRiskLevel("MEDIUM");
            job.setStatus(PrReviewJobStatus.COMPLETED.name());
            log.info("PR Review model returned non-JSON, using raw summary: reviewJobId={}", job.getId());
        }

        job.setFinishedAt(LocalDateTime.now());
        job.setUpdateTime(LocalDateTime.now());
        reviewJobMapper.updateById(job);

        // Save findings
        if (parsed != null && parsed.has("findings") && parsed.get("findings").isArray()) {
            for (JsonNode f : parsed.get("findings")) {
                PrReviewFindingEntity finding = new PrReviewFindingEntity();
                finding.setReviewJobId(job.getId());
                finding.setProjectId(job.getProjectId());
                finding.setSeverity(f.has("severity") ? f.get("severity").asText().toUpperCase() : "INFO");
                finding.setCategory(f.has("category") ? f.get("category").asText().toUpperCase() : "BUG");
                finding.setFilePath(f.has("filePath") ? f.get("filePath").asText() : null);
                finding.setLineNumber(f.has("lineNumber") ? f.get("lineNumber").asInt() : null);
                finding.setTitle(f.has("title") ? f.get("title").asText() : "Untitled");
                finding.setDescription(f.has("description") ? f.get("description").asText() : null);
                finding.setSuggestion(f.has("suggestion") ? f.get("suggestion").asText() : null);
                finding.setCodeSnippet(f.has("codeSnippet") ? f.get("codeSnippet").asText() : null);
                finding.setCreateTime(LocalDateTime.now());
                findingMapper.insert(finding);
            }
        }

        auditLogApplicationService.recordSuccess(job.getProjectId(), job.getId(),
                AuditActionType.PR_REVIEW_COMPLETE.name(), "PR_REVIEW",
                "PR Review 完成: " + (parsed != null && parsed.has("findings")
                        ? parsed.get("findings").size() + " findings" : "summary only"));
    }

    String buildSystemPrompt(String reviewMode) {
        return """
                You are an expert code reviewer. Analyze the pull request changes and provide a structured review.
                Review mode: %s
                Return a JSON object with: summary (string), riskLevel (LOW/MEDIUM/HIGH/CRITICAL), and findings (array of objects with: severity (INFO/WARNING/ERROR/CRITICAL), category (BUG/SECURITY/PERFORMANCE/STYLE/MAINTAINABILITY/TEST/DOCUMENTATION), filePath, lineNumber (int), title, description, suggestion, codeSnippet).
                Respond ONLY with valid JSON, no markdown or extra text.""".formatted(reviewMode);
    }

    String buildUserPrompt(GithubPullRequestResponse pr,
                                    List<GithubPullRequestFileResponse> files,
                                    String patch, String reviewMode) {
        StringBuilder sb = new StringBuilder();
        sb.append("## Pull Request Review Request\n\n");
        sb.append("**Title:** ").append(pr.getTitle()).append("\n");
        sb.append("**Author:** ").append(pr.getAuthorLogin() != null ? pr.getAuthorLogin() : "unknown").append("\n");
        sb.append("**Base:** ").append(pr.getBaseBranch()).append(" **Head:** ").append(pr.getHeadBranch()).append("\n");
        sb.append("**Changes:** +").append(pr.getAdditions()).append(" -").append(pr.getDeletions())
                .append(" (").append(pr.getChangedFiles()).append(" files)\n\n");

        sb.append("### Changed Files\n");
        for (GithubPullRequestFileResponse f : files) {
            sb.append("- ").append(f.getFilename()).append(" (+").append(f.getAdditions())
                    .append(" -").append(f.getDeletions()).append(")\n");
        }

        sb.append("\n### Diff/Patch\n```diff\n");
        sb.append(patch != null ? patch : "(no patch available)");
        sb.append("\n```\n");

        return sb.toString();
    }

    JsonNode parseReviewJson(String content) {
        if (content == null || content.isBlank()) return null;
        // Try to extract JSON from markdown code blocks or raw text
        String jsonStr = content.trim();
        int jsonStart = jsonStr.indexOf("{");
        if (jsonStart > 0) jsonStr = jsonStr.substring(jsonStart);
        int jsonEnd = jsonStr.lastIndexOf("}");
        if (jsonEnd > 0) jsonStr = jsonStr.substring(0, jsonEnd + 1);
        try {
            return objectMapper.readTree(jsonStr);
        } catch (JsonProcessingException e) {
            log.info("Failed to parse review response as JSON, using raw: {}", e.getMessage());
            return null;
        }
    }

    String validateRiskLevel(String risk) {
        if (risk == null) return "MEDIUM";
        return switch (risk.toUpperCase()) {
            case "LOW", "MEDIUM", "HIGH", "CRITICAL" -> risk.toUpperCase();
            default -> "MEDIUM";
        };
    }

    @Transactional(readOnly = true)
    public List<PrReviewJobResponse> listByProject(Long projectId, int page, int pageSize) {
        permissionService.checkProjectMember(projectId);

        IPage<PrReviewJobEntity> result = reviewJobMapper.selectPage(
                new Page<>(page, pageSize),
                new LambdaQueryWrapper<PrReviewJobEntity>()
                        .eq(PrReviewJobEntity::getProjectId, projectId)
                        .orderByDesc(PrReviewJobEntity::getCreateTime));

        List<PrReviewJobResponse> list = new ArrayList<>();
        for (PrReviewJobEntity job : result.getRecords()) {
            list.add(toJobResponse(job));
        }
        return list;
    }

    @Transactional(readOnly = true)
    public PrReviewJobResponse getDetail(Long reviewJobId) {
        PrReviewJobEntity job = reviewJobMapper.selectById(reviewJobId);
        if (job == null) throw new BizException(ErrorCode.NOT_FOUND, "Review job 不存在");
        permissionService.checkProjectMember(job.getProjectId());
        return toJobResponse(job);
    }

    @Transactional(readOnly = true)
    public List<PrReviewFindingResponse> getFindings(Long reviewJobId) {
        PrReviewJobEntity job = reviewJobMapper.selectById(reviewJobId);
        if (job == null) throw new BizException(ErrorCode.NOT_FOUND, "Review job 不存在");
        permissionService.checkProjectMember(job.getProjectId());

        List<PrReviewFindingEntity> entities = findingMapper.selectList(
                new LambdaQueryWrapper<PrReviewFindingEntity>()
                        .eq(PrReviewFindingEntity::getReviewJobId, reviewJobId)
                        .orderByAsc(PrReviewFindingEntity::getSeverity));

        List<PrReviewFindingResponse> list = new ArrayList<>();
        for (PrReviewFindingEntity f : entities) {
            list.add(toFindingResponse(f));
        }
        return list;
    }

    private PrReviewJobResponse toJobResponse(PrReviewJobEntity job) {
        PrReviewJobResponse r = new PrReviewJobResponse();
        r.setId(job.getId() != null ? job.getId().toString() : null);
        r.setProjectId(job.getProjectId() != null ? job.getProjectId().toString() : null);
        r.setPullRequestId(job.getPullRequestId() != null ? job.getPullRequestId().toString() : null);
        r.setStatus(job.getStatus());
        r.setReviewMode(job.getReviewMode());
        r.setSummary(job.getSummary());
        r.setRiskLevel(job.getRiskLevel());
        r.setModelProvider(job.getModelProvider());
        r.setModelName(job.getModelName());
        r.setTokenUsage(job.getTokenUsage());
        r.setErrorMessage(job.getErrorMessage());
        r.setStartedAt(job.getStartedAt() != null ? job.getStartedAt().toString() : null);
        r.setFinishedAt(job.getFinishedAt() != null ? job.getFinishedAt().toString() : null);
        r.setCreateTime(job.getCreateTime() != null ? job.getCreateTime().toString() : null);
        return r;
    }

    private PrReviewFindingResponse toFindingResponse(PrReviewFindingEntity f) {
        PrReviewFindingResponse r = new PrReviewFindingResponse();
        r.setId(f.getId() != null ? f.getId().toString() : null);
        r.setReviewJobId(f.getReviewJobId() != null ? f.getReviewJobId().toString() : null);
        r.setSeverity(f.getSeverity());
        r.setCategory(f.getCategory());
        r.setFilePath(f.getFilePath());
        r.setLineNumber(f.getLineNumber());
        r.setTitle(f.getTitle());
        r.setDescription(f.getDescription());
        r.setSuggestion(f.getSuggestion());
        r.setCodeSnippet(f.getCodeSnippet());
        return r;
    }
}
