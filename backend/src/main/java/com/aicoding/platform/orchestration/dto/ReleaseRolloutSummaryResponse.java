package com.aicoding.platform.orchestration.dto;

import java.time.LocalDateTime;

public class ReleaseRolloutSummaryResponse {

    private String planId;
    private String releaseLabel;
    private String rolloutStatus;
    private String rolloutStrategy;
    private String targetEnvironment;
    private Integer totalSteps;
    private Integer passedSteps;
    private Integer failedSteps;
    private Integer skippedSteps;
    private Integer blockedSteps;
    private Integer totalVerifications;
    private Integer failedVerifications;
    private Integer blockingVerifications;
    private Boolean rollbackRecommended;
    private String overallResult;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime createTime;

    public String getPlanId() { return planId; }
    public void setPlanId(String planId) { this.planId = planId; }
    public String getReleaseLabel() { return releaseLabel; }
    public void setReleaseLabel(String releaseLabel) { this.releaseLabel = releaseLabel; }
    public String getRolloutStatus() { return rolloutStatus; }
    public void setRolloutStatus(String rolloutStatus) { this.rolloutStatus = rolloutStatus; }
    public String getRolloutStrategy() { return rolloutStrategy; }
    public void setRolloutStrategy(String rolloutStrategy) { this.rolloutStrategy = rolloutStrategy; }
    public String getTargetEnvironment() { return targetEnvironment; }
    public void setTargetEnvironment(String targetEnvironment) { this.targetEnvironment = targetEnvironment; }
    public Integer getTotalSteps() { return totalSteps; }
    public void setTotalSteps(Integer totalSteps) { this.totalSteps = totalSteps; }
    public Integer getPassedSteps() { return passedSteps; }
    public void setPassedSteps(Integer passedSteps) { this.passedSteps = passedSteps; }
    public Integer getFailedSteps() { return failedSteps; }
    public void setFailedSteps(Integer failedSteps) { this.failedSteps = failedSteps; }
    public Integer getSkippedSteps() { return skippedSteps; }
    public void setSkippedSteps(Integer skippedSteps) { this.skippedSteps = skippedSteps; }
    public Integer getBlockedSteps() { return blockedSteps; }
    public void setBlockedSteps(Integer blockedSteps) { this.blockedSteps = blockedSteps; }
    public Integer getTotalVerifications() { return totalVerifications; }
    public void setTotalVerifications(Integer totalVerifications) { this.totalVerifications = totalVerifications; }
    public Integer getFailedVerifications() { return failedVerifications; }
    public void setFailedVerifications(Integer failedVerifications) { this.failedVerifications = failedVerifications; }
    public Integer getBlockingVerifications() { return blockingVerifications; }
    public void setBlockingVerifications(Integer blockingVerifications) { this.blockingVerifications = blockingVerifications; }
    public Boolean getRollbackRecommended() { return rollbackRecommended; }
    public void setRollbackRecommended(Boolean rollbackRecommended) { this.rollbackRecommended = rollbackRecommended; }
    public String getOverallResult() { return overallResult; }
    public void setOverallResult(String overallResult) { this.overallResult = overallResult; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
