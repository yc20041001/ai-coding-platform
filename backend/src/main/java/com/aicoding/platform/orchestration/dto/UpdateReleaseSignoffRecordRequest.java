package com.aicoding.platform.orchestration.dto;

public class UpdateReleaseSignoffRecordRequest {

    private String signoffStatus;
    private String signerId;
    private String signerName;
    private String commentText;

    public String getSignoffStatus() { return signoffStatus; }
    public void setSignoffStatus(String signoffStatus) { this.signoffStatus = signoffStatus; }
    public String getSignerId() { return signerId; }
    public void setSignerId(String signerId) { this.signerId = signerId; }
    public String getSignerName() { return signerName; }
    public void setSignerName(String signerName) { this.signerName = signerName; }
    public String getCommentText() { return commentText; }
    public void setCommentText(String commentText) { this.commentText = commentText; }
}
