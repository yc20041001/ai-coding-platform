package com.aicoding.platform.orchestration.dto;

public class CreateReleaseSignoffRecordRequest {

    private String projectId;
    private String signoffRole;
    private String signoffStatus;
    private String signerId;
    private String signerName;
    private String commentText;

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }
    public String getSignoffRole() { return signoffRole; }
    public void setSignoffRole(String signoffRole) { this.signoffRole = signoffRole; }
    public String getSignoffStatus() { return signoffStatus; }
    public void setSignoffStatus(String signoffStatus) { this.signoffStatus = signoffStatus; }
    public String getSignerId() { return signerId; }
    public void setSignerId(String signerId) { this.signerId = signerId; }
    public String getSignerName() { return signerName; }
    public void setSignerName(String signerName) { this.signerName = signerName; }
    public String getCommentText() { return commentText; }
    public void setCommentText(String commentText) { this.commentText = commentText; }
}
