package com.aicoding.platform.orchestration.application;

import com.aicoding.platform.common.exception.BizException;
import com.aicoding.platform.common.exception.ErrorCode;
import com.aicoding.platform.member.application.ProjectPermissionService;
import com.aicoding.platform.member.domain.ProjectRole;
import com.aicoding.platform.orchestration.dto.ToolAuditExportResponse;
import com.aicoding.platform.orchestration.dto.ToolExecutionTraceEventResponse;
import com.aicoding.platform.orchestration.dto.ToolExecutionTraceResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ToolExecutionAuditExportService {

    private static final Logger log = LoggerFactory.getLogger(ToolExecutionAuditExportService.class);

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String CONTENT_TYPE_MARKDOWN = "text/markdown";

    private final ToolExecutionTraceService traceService;
    private final ProjectPermissionService projectPermissionService;
    private final ToolTracePayloadSanitizer payloadSanitizer;

    public ToolExecutionAuditExportService(ToolExecutionTraceService traceService,
                                            ProjectPermissionService projectPermissionService,
                                            ToolTracePayloadSanitizer payloadSanitizer) {
        this.traceService = traceService;
        this.projectPermissionService = projectPermissionService;
        this.payloadSanitizer = payloadSanitizer;
    }

    @Transactional(readOnly = true)
    public ToolAuditExportResponse exportExecutionTrace(Long executionId) {
        ToolExecutionTraceResponse trace = traceService.getTrace(executionId);

        String md = buildExecutionMarkdown(trace);
        md = sanitizeMarkdown(md);

        long projectId = Long.parseLong(trace.getProjectId());
        boolean redacted = trace.getEvidence() != null && Boolean.TRUE.equals(trace.getEvidence().getRedacted());
        boolean truncated = trace.getEvidence() != null && Boolean.TRUE.equals(trace.getEvidence().getTruncated());

        ToolAuditExportResponse resp = new ToolAuditExportResponse();
        resp.setTargetType("TOOL_EXECUTION");
        resp.setTargetId(executionId.toString());
        resp.setFileName("tool-execution-" + executionId + "-audit.md");
        resp.setContentType(CONTENT_TYPE_MARKDOWN);
        resp.setMarkdown(md);
        resp.setTraceCount(1);
        resp.setRedacted(redacted);
        resp.setTruncated(truncated);
        resp.setGeneratedAt(LocalDateTime.now());
        return resp;
    }

    @Transactional(readOnly = true)
    public ToolAuditExportResponse exportRunEvidence(Long runId) {
        List<ToolExecutionTraceResponse> traces = traceService.listRunTraces(runId);
        if (traces.isEmpty()) {
            throw new BizException(ErrorCode.NOT_FOUND, "该 run 下无工具执行记录");
        }

        String md = buildRunMarkdown(runId, traces);
        md = sanitizeMarkdown(md);

        boolean anyRedacted = traces.stream()
                .anyMatch(t -> t.getEvidence() != null && Boolean.TRUE.equals(t.getEvidence().getRedacted()));
        boolean anyTruncated = traces.stream()
                .anyMatch(t -> t.getEvidence() != null && Boolean.TRUE.equals(t.getEvidence().getTruncated()));

        ToolAuditExportResponse resp = new ToolAuditExportResponse();
        resp.setTargetType("MULTI_AGENT_RUN");
        resp.setTargetId(runId.toString());
        resp.setFileName("multi-agent-run-" + runId + "-evidence.md");
        resp.setContentType(CONTENT_TYPE_MARKDOWN);
        resp.setMarkdown(md);
        resp.setTraceCount(traces.size());
        resp.setRedacted(anyRedacted);
        resp.setTruncated(anyTruncated);
        resp.setGeneratedAt(LocalDateTime.now());
        return resp;
    }

    @Transactional(readOnly = true)
    public ToolAuditExportResponse exportTaskToolAudit(Long taskId) {
        List<ToolExecutionTraceResponse> traces = traceService.listTaskTraces(taskId);
        if (traces.isEmpty()) {
            throw new BizException(ErrorCode.NOT_FOUND, "该 task 下无工具执行记录");
        }

        String md = buildTaskMarkdown(taskId, traces);
        md = sanitizeMarkdown(md);

        boolean anyRedacted = traces.stream()
                .anyMatch(t -> t.getEvidence() != null && Boolean.TRUE.equals(t.getEvidence().getRedacted()));
        boolean anyTruncated = traces.stream()
                .anyMatch(t -> t.getEvidence() != null && Boolean.TRUE.equals(t.getEvidence().getTruncated()));

        ToolAuditExportResponse resp = new ToolAuditExportResponse();
        resp.setTargetType("TASK");
        resp.setTargetId(taskId.toString());
        resp.setFileName("task-" + taskId + "-tool-audit.md");
        resp.setContentType(CONTENT_TYPE_MARKDOWN);
        resp.setMarkdown(md);
        resp.setTraceCount(traces.size());
        resp.setRedacted(anyRedacted);
        resp.setTruncated(anyTruncated);
        resp.setGeneratedAt(LocalDateTime.now());
        return resp;
    }

    private String buildExecutionMarkdown(ToolExecutionTraceResponse trace) {
        StringBuilder sb = new StringBuilder();
        sb.append("# Tool Execution Audit Report\n\n");

        // Summary
        sb.append("## Summary\n");
        sb.append("- **Target**: Tool Execution\n");
        sb.append("- **Execution ID**: ").append(trace.getExecutionId()).append("\n");
        sb.append("- **Project ID**: ").append(trace.getProjectId()).append("\n");
        sb.append("- **Task ID**: ").append(nullSafe(trace.getTaskId())).append("\n");
        sb.append("- **Run ID**: ").append(nullSafe(trace.getRunId())).append("\n");
        sb.append("- **Generated At**: ").append(LocalDateTime.now().format(DT_FMT)).append("\n");
        sb.append("- **Trace Count**: 1\n\n");

        // Tool Execution
        sb.append("## Tool Execution\n");
        sb.append("- **Tool**: ").append(nullSafe(trace.getToolKey())).append("\n");
        sb.append("- **Tool Name**: ").append(nullSafe(trace.getToolName())).append("\n");
        sb.append("- **Status**: ").append(nullSafe(trace.getStatus())).append("\n");
        sb.append("- **Risk**: ").append(nullSafe(trace.getRiskLevel())).append("\n");
        sb.append("- **Mode**: ").append(nullSafe(trace.getMode())).append("\n");
        sb.append("- **Read-only**: ").append(trace.getReadOnly() != null && trace.getReadOnly()).append("\n");
        sb.append("- **Policy Allowed**: ").append(trace.getPolicyAllowed() != null && trace.getPolicyAllowed()).append("\n");
        if (trace.getPolicyReason() != null && !trace.getPolicyReason().isBlank()) {
            sb.append("- **Policy Reason**: ").append(trace.getPolicyReason()).append("\n");
        }
        sb.append("\n");

        // Timeline
        appendTimeline(sb, trace.getEvents());

        // Evidence
        appendEvidence(sb, trace);

        // Approval
        if (trace.getApproval() != null) {
            sb.append("## Approval\n");
            sb.append("- **Approval ID**: ").append(nullSafe(trace.getApproval().getApprovalId())).append("\n");
            sb.append("- **Status**: ").append(nullSafe(trace.getApproval().getStatus())).append("\n");
            if (trace.getApproval().getComment() != null) {
                sb.append("- **Comment**: ").append(trace.getApproval().getComment()).append("\n");
            }
            if (trace.getApproval().getApproverId() != null) {
                sb.append("- **Approver ID**: ").append(trace.getApproval().getApproverId()).append("\n");
            }
            sb.append("- **Requested At**: ").append(nullSafe(trace.getApproval().getCreateTime())).append("\n");
            sb.append("- **Decided At**: ").append(nullSafe(trace.getApproval().getDecidedAt())).append("\n\n");
        }

        // Job
        if (trace.getJob() != null) {
            sb.append("## Job\n");
            sb.append("- **Job ID**: ").append(nullSafe(trace.getJob().getJobId())).append("\n");
            sb.append("- **Status**: ").append(nullSafe(trace.getJob().getStatus())).append("\n");
            sb.append("- **Priority**: ").append(nullSafe(trace.getJob().getPriority())).append("\n");
            sb.append("- **Attempts**: ").append(trace.getJob().getAttemptCount() != null ? trace.getJob().getAttemptCount() : 1).append("\n");
            if (trace.getJob().getErrorCode() != null) {
                sb.append("- **Error Code**: ").append(trace.getJob().getErrorCode()).append("\n");
            }
            if (trace.getJob().getFailureStage() != null) {
                sb.append("- **Failure Stage**: ").append(trace.getJob().getFailureStage()).append("\n");
            }
            if (trace.getJob().getNextRetryAt() != null) {
                sb.append("- **Next Retry At**: ").append(trace.getJob().getNextRetryAt()).append("\n");
            }
            if (trace.getJob().getDeadLetteredAt() != null) {
                sb.append("- **Dead Lettered At**: ").append(trace.getJob().getDeadLetteredAt()).append("\n");
            }
            if (trace.getJob().getDeadLetterReason() != null) {
                sb.append("- **Dead Letter Reason**: ").append(trace.getJob().getDeadLetterReason()).append("\n");
            }
            if (trace.getJob().getSourceJobId() != null) {
                sb.append("- **Source Job ID**: ").append(trace.getJob().getSourceJobId()).append("\n");
            }
            sb.append("- **Started At**: ").append(nullSafe(trace.getJob().getStartedAt())).append("\n");
            sb.append("- **Finished At**: ").append(nullSafe(trace.getJob().getFinishedAt())).append("\n\n");
        }

        // Artifact / Patch Review
        if (trace.getEvidence() != null && trace.getEvidence().getArtifacts() != null
                && !trace.getEvidence().getArtifacts().isEmpty()) {
            sb.append("## Artifact / Patch Review\n");
            trace.getEvidence().getArtifacts().forEach(art -> {
                sb.append("- **Artifact ID**: ").append(nullSafe(art.getArtifactId())).append("\n");
                sb.append("  - **Type**: ").append(nullSafe(art.getArtifactType())).append("\n");
                sb.append("  - **Title**: ").append(nullSafe(art.getTitle())).append("\n");
                if (art.getPatchReviewStatus() != null) {
                    sb.append("  - **Review Status**: ").append(art.getPatchReviewStatus()).append("\n");
                }
                if (art.getPatchReviewDecision() != null) {
                    sb.append("  - **Review Decision**: ").append(art.getPatchReviewDecision()).append("\n");
                }
            });
            sb.append("\n");
        }

        // Payload
        sb.append("## Sanitized Payload\n");
        sb.append("```json\n");
        sb.append(nullSafe(trace.getOutputPayload())).append("\n");
        sb.append("```\n\n");

        // Notes
        sb.append("## Notes\n");
        sb.append("- This report is generated from stored trace data.\n");
        sb.append("- No tool was re-executed.\n");
        sb.append("- No repository files were re-read.\n");

        return sb.toString();
    }

    private String buildRunMarkdown(Long runId, List<ToolExecutionTraceResponse> traces) {
        StringBuilder sb = new StringBuilder();
        sb.append("# Multi-Agent Run Tool Evidence Report\n\n");
        sb.append("## Run Summary\n");
        sb.append("- **Run ID**: ").append(runId).append("\n");
        sb.append("- **Trace Count**: ").append(traces.size()).append("\n");
        sb.append("- **Generated At**: ").append(LocalDateTime.now().format(DT_FMT)).append("\n\n");

        int idx = 1;
        for (ToolExecutionTraceResponse trace : traces) {
            sb.append("---\n\n");
            sb.append("## Trace ").append(idx).append(": ").append(nullSafe(trace.getToolKey())).append("\n\n");
            sb.append(buildExecutionMarkdown(trace));
            idx++;
        }

        return sb.toString();
    }

    private String buildTaskMarkdown(Long taskId, List<ToolExecutionTraceResponse> traces) {
        StringBuilder sb = new StringBuilder();
        sb.append("# Task Tool Audit Report\n\n");
        sb.append("## Task Summary\n");
        sb.append("- **Task ID**: ").append(taskId).append("\n");
        sb.append("- **Trace Count**: ").append(traces.size()).append("\n");
        sb.append("- **Generated At**: ").append(LocalDateTime.now().format(DT_FMT)).append("\n\n");

        int idx = 1;
        for (ToolExecutionTraceResponse trace : traces) {
            sb.append("---\n\n");
            sb.append("## Tool Execution ").append(idx).append(": ").append(nullSafe(trace.getToolKey())).append("\n\n");
            sb.append(buildExecutionMarkdown(trace));
            idx++;
        }

        return sb.toString();
    }

    private void appendTimeline(StringBuilder sb, List<ToolExecutionTraceEventResponse> events) {
        sb.append("## Timeline\n");
        if (events == null || events.isEmpty()) {
            sb.append("No timeline events.\n\n");
            return;
        }
        sb.append("| Time | Event | Status | Description |\n");
        sb.append("|------|-------|--------|-------------|\n");
        for (ToolExecutionTraceEventResponse event : events) {
            String time = event.getEventTime() != null ? event.getEventTime().format(DT_FMT) : "-";
            String eventType = nullSafe(event.getEventType());
            String status = nullSafe(event.getStatus());
            String desc = nullSafe(event.getDescription()).replace("\n", " ");
            sb.append("| ").append(time).append(" | ").append(eventType).append(" | ").append(status).append(" | ").append(desc).append(" |\n");
        }
        sb.append("\n");
    }

    private void appendEvidence(StringBuilder sb, ToolExecutionTraceResponse trace) {
        sb.append("## Evidence\n");
        if (trace.getEvidence() == null) {
            sb.append("No evidence recorded.\n\n");
            return;
        }

        // Files Read
        sb.append("### Files Read\n");
        if (trace.getEvidence().getFilesReadCount() != null && trace.getEvidence().getFilesReadCount() > 0
                && trace.getEvidence().getFilesRead() != null) {
            trace.getEvidence().getFilesRead().forEach(f -> {
                sb.append("- ").append(nullSafe(f.getPath()));
                if (f.getLineStart() != null) sb.append(" (line ").append(f.getLineStart());
                if (f.getLineEnd() != null) sb.append("-").append(f.getLineEnd());
                if (f.getLineStart() != null) sb.append(")");
                sb.append("\n");
            });
        } else {
            sb.append("No files read.\n");
        }
        sb.append("\n");

        // Skipped Files
        sb.append("### Skipped Files\n");
        if (trace.getEvidence().getSkippedFilesCount() != null && trace.getEvidence().getSkippedFilesCount() > 0
                && trace.getEvidence().getSkippedFiles() != null) {
            trace.getEvidence().getSkippedFiles().forEach(f -> {
                sb.append("- ").append(nullSafe(f.getPath()));
                if (f.getReason() != null) sb.append(" (reason: ").append(f.getReason()).append(")");
                sb.append("\n");
            });
        } else {
            sb.append("No skipped files.\n");
        }
        sb.append("\n");

        // Redaction / Truncation
        sb.append("### Redaction / Truncation\n");
        sb.append("- **Redacted**: ").append(Boolean.TRUE.equals(trace.getEvidence().getRedacted())).append("\n");
        sb.append("- **Truncated**: ").append(Boolean.TRUE.equals(trace.getEvidence().getTruncated())).append("\n");
        if (Boolean.TRUE.equals(trace.getEvidence().getRedacted())) {
            sb.append("> Sensitive values were redacted.\n");
        }
        if (Boolean.TRUE.equals(trace.getEvidence().getTruncated())) {
            sb.append("> Payload was truncated for safety.\n");
        }
        sb.append("\n");

        // Read-only Contract
        sb.append("### Read-only Contract\n");
        sb.append("- **Files Touched**: none\n");
        sb.append("- **Git Operations**: none\n\n");
    }

    private String sanitizeMarkdown(String markdown) {
        return payloadSanitizer.sanitize(markdown);
    }

    private static String nullSafe(Object obj) {
        return obj != null ? obj.toString() : "-";
    }

    private static String nullSafe(String str) {
        return str != null && !str.isBlank() ? str : "-";
    }
}
