package com.aicoding.platform.orchestration.dto;

import java.time.LocalDateTime;

public class ToolExecutionApprovalEvidenceResponse {

    private String approvalId;
    private String status;
    private String approverId;
    private String approverName;
    private String comment;
    private LocalDateTime createTime;
    private LocalDateTime decidedAt;

    public ToolExecutionApprovalEvidenceResponse() {}

    public String getApprovalId() { return approvalId; }
    public void setApprovalId(String approvalId) { this.approvalId = approvalId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getApproverId() { return approverId; }
    public void setApproverId(String approverId) { this.approverId = approverId; }

    public String getApproverName() { return approverName; }
    public void setApproverName(String approverName) { this.approverName = approverName; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getDecidedAt() { return decidedAt; }
    public void setDecidedAt(LocalDateTime decidedAt) { this.decidedAt = decidedAt; }
}
