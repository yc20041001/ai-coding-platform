package com.aicoding.platform.orchestration.dto;

public class UpdateGovernanceWaiverRequestRequest {

    private String reasonText;
    private String approvalNote;
    private String expiresAt;

    public String getReasonText() { return reasonText; }
    public void setReasonText(String reasonText) { this.reasonText = reasonText; }
    public String getApprovalNote() { return approvalNote; }
    public void setApprovalNote(String approvalNote) { this.approvalNote = approvalNote; }
    public String getExpiresAt() { return expiresAt; }
    public void setExpiresAt(String expiresAt) { this.expiresAt = expiresAt; }
}
