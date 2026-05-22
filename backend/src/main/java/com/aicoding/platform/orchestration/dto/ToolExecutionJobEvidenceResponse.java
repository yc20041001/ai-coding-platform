package com.aicoding.platform.orchestration.dto;

import java.time.LocalDateTime;

public class ToolExecutionJobEvidenceResponse {

    private String jobId;
    private String status;
    private String priority;
    private Integer attemptCount;
    private String errorCode;
    private String failureStage;
    private LocalDateTime nextRetryAt;
    private LocalDateTime deadLetteredAt;
    private String deadLetterReason;
    private String sourceJobId;
    private LocalDateTime createTime;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;

    public ToolExecutionJobEvidenceResponse() {}

    public String getJobId() { return jobId; }
    public void setJobId(String jobId) { this.jobId = jobId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public Integer getAttemptCount() { return attemptCount; }
    public void setAttemptCount(Integer attemptCount) { this.attemptCount = attemptCount; }

    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }

    public String getFailureStage() { return failureStage; }
    public void setFailureStage(String failureStage) { this.failureStage = failureStage; }

    public LocalDateTime getNextRetryAt() { return nextRetryAt; }
    public void setNextRetryAt(LocalDateTime nextRetryAt) { this.nextRetryAt = nextRetryAt; }

    public LocalDateTime getDeadLetteredAt() { return deadLetteredAt; }
    public void setDeadLetteredAt(LocalDateTime deadLetteredAt) { this.deadLetteredAt = deadLetteredAt; }

    public String getDeadLetterReason() { return deadLetterReason; }
    public void setDeadLetterReason(String deadLetterReason) { this.deadLetterReason = deadLetterReason; }

    public String getSourceJobId() { return sourceJobId; }
    public void setSourceJobId(String sourceJobId) { this.sourceJobId = sourceJobId; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }

    public LocalDateTime getFinishedAt() { return finishedAt; }
    public void setFinishedAt(LocalDateTime finishedAt) { this.finishedAt = finishedAt; }
}
