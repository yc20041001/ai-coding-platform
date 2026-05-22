package com.aicoding.platform.orchestration.dto;

import java.time.LocalDateTime;

public class ToolAuditExportResponse {

    private String targetType;
    private String targetId;
    private String fileName;
    private String contentType;
    private String markdown;
    private Integer traceCount;
    private Boolean redacted;
    private Boolean truncated;
    private LocalDateTime generatedAt;

    public ToolAuditExportResponse() {}

    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }

    public String getTargetId() { return targetId; }
    public void setTargetId(String targetId) { this.targetId = targetId; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public String getMarkdown() { return markdown; }
    public void setMarkdown(String markdown) { this.markdown = markdown; }

    public Integer getTraceCount() { return traceCount; }
    public void setTraceCount(Integer traceCount) { this.traceCount = traceCount; }

    public Boolean getRedacted() { return redacted; }
    public void setRedacted(Boolean redacted) { this.redacted = redacted; }

    public Boolean getTruncated() { return truncated; }
    public void setTruncated(Boolean truncated) { this.truncated = truncated; }

    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }
}
