package com.aicoding.platform.orchestration.dto;

public class UpdateBetaReleaseDecisionRequest {

    private String decisionStatus;
    private String decisionReason;
    private String approverId;

    public String getDecisionStatus() { return decisionStatus; }
    public void setDecisionStatus(String decisionStatus) { this.decisionStatus = decisionStatus; }
    public String getDecisionReason() { return decisionReason; }
    public void setDecisionReason(String decisionReason) { this.decisionReason = decisionReason; }
    public String getApproverId() { return approverId; }
    public void setApproverId(String approverId) { this.approverId = approverId; }
}
