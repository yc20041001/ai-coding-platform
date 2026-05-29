package com.aicoding.platform.orchestration.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ReleaseConfidenceSnapshotResponse {

    private String id;
    private String projectId;
    private String planId;
    private String releaseLabel;
    private BigDecimal confidenceScore;
    private String confidenceLevel;
    private Integer blockingIssueCount;
    private Integer warningIssueCount;
    private Integer openIncidentCount;
    private Integer activeAlertCount;
    private Integer failedVerificationCount;
    private Boolean rollbackReady;
    private BigDecimal signoffCompletionRate;
    private String snapshotSummary;
    private LocalDateTime snapshotTime;
    private LocalDateTime createTime;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }
    public String getPlanId() { return planId; }
    public void setPlanId(String planId) { this.planId = planId; }
    public String getReleaseLabel() { return releaseLabel; }
    public void setReleaseLabel(String releaseLabel) { this.releaseLabel = releaseLabel; }
    public BigDecimal getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(BigDecimal confidenceScore) { this.confidenceScore = confidenceScore; }
    public String getConfidenceLevel() { return confidenceLevel; }
    public void setConfidenceLevel(String confidenceLevel) { this.confidenceLevel = confidenceLevel; }
    public Integer getBlockingIssueCount() { return blockingIssueCount; }
    public void setBlockingIssueCount(Integer blockingIssueCount) { this.blockingIssueCount = blockingIssueCount; }
    public Integer getWarningIssueCount() { return warningIssueCount; }
    public void setWarningIssueCount(Integer warningIssueCount) { this.warningIssueCount = warningIssueCount; }
    public Integer getOpenIncidentCount() { return openIncidentCount; }
    public void setOpenIncidentCount(Integer openIncidentCount) { this.openIncidentCount = openIncidentCount; }
    public Integer getActiveAlertCount() { return activeAlertCount; }
    public void setActiveAlertCount(Integer activeAlertCount) { this.activeAlertCount = activeAlertCount; }
    public Integer getFailedVerificationCount() { return failedVerificationCount; }
    public void setFailedVerificationCount(Integer failedVerificationCount) { this.failedVerificationCount = failedVerificationCount; }
    public Boolean getRollbackReady() { return rollbackReady; }
    public void setRollbackReady(Boolean rollbackReady) { this.rollbackReady = rollbackReady; }
    public BigDecimal getSignoffCompletionRate() { return signoffCompletionRate; }
    public void setSignoffCompletionRate(BigDecimal signoffCompletionRate) { this.signoffCompletionRate = signoffCompletionRate; }
    public String getSnapshotSummary() { return snapshotSummary; }
    public void setSnapshotSummary(String snapshotSummary) { this.snapshotSummary = snapshotSummary; }
    public LocalDateTime getSnapshotTime() { return snapshotTime; }
    public void setSnapshotTime(LocalDateTime snapshotTime) { this.snapshotTime = snapshotTime; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
