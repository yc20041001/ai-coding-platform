package com.aicoding.platform.orchestration.dto;

public class ToolExecutionFileEvidenceResponse {

    private String path;
    private String reason;
    private Long sizeBytes;
    private Integer lineStart;
    private Integer lineEnd;
    private Boolean redacted;
    private Boolean truncated;

    public ToolExecutionFileEvidenceResponse() {}

    public ToolExecutionFileEvidenceResponse(String path) {
        this.path = path;
    }

    public ToolExecutionFileEvidenceResponse(String path, String reason) {
        this.path = path;
        this.reason = reason;
    }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public Long getSizeBytes() { return sizeBytes; }
    public void setSizeBytes(Long sizeBytes) { this.sizeBytes = sizeBytes; }

    public Integer getLineStart() { return lineStart; }
    public void setLineStart(Integer lineStart) { this.lineStart = lineStart; }

    public Integer getLineEnd() { return lineEnd; }
    public void setLineEnd(Integer lineEnd) { this.lineEnd = lineEnd; }

    public Boolean getRedacted() { return redacted; }
    public void setRedacted(Boolean redacted) { this.redacted = redacted; }

    public Boolean getTruncated() { return truncated; }
    public void setTruncated(Boolean truncated) { this.truncated = truncated; }
}
