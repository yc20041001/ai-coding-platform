package com.aicoding.platform.orchestration.dto;

import java.time.LocalDateTime;

public class ReleaseSignoffRecordResponse {

    private String id;
    private String projectId;
    private String planId;
    private String releaseLabel;
    private String signoffRole;
    private String signoffStatus;
    private String signerId;
    private String signerName;
    private String commentText;
    private LocalDateTime signedAt;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }
    public String getPlanId() { return planId; }
    public void setPlanId(String planId) { this.planId = planId; }
    public String getReleaseLabel() { return releaseLabel; }
    public void setReleaseLabel(String releaseLabel) { this.releaseLabel = releaseLabel; }
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
    public LocalDateTime getSignedAt() { return signedAt; }
    public void setSignedAt(LocalDateTime signedAt) { this.signedAt = signedAt; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
