package com.aicoding.platform.orchestration.dto;

import java.math.BigDecimal;

public class ReleaseComparisonResponse {

    private String projectId;
    private String currentReleaseLabel;
    private String baselineReleaseLabel;
    private BigDecimal confidenceScoreDelta;
    private Integer blockingIssueDelta;
    private Integer warningIssueDelta;
    private Integer failedVerificationDelta;
    private Boolean rollbackReadyChanged;
    private BigDecimal signoffCompletionDelta;
    private String trendSummary;

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }
    public String getCurrentReleaseLabel() { return currentReleaseLabel; }
    public void setCurrentReleaseLabel(String currentReleaseLabel) { this.currentReleaseLabel = currentReleaseLabel; }
    public String getBaselineReleaseLabel() { return baselineReleaseLabel; }
    public void setBaselineReleaseLabel(String baselineReleaseLabel) { this.baselineReleaseLabel = baselineReleaseLabel; }
    public BigDecimal getConfidenceScoreDelta() { return confidenceScoreDelta; }
    public void setConfidenceScoreDelta(BigDecimal confidenceScoreDelta) { this.confidenceScoreDelta = confidenceScoreDelta; }
    public Integer getBlockingIssueDelta() { return blockingIssueDelta; }
    public void setBlockingIssueDelta(Integer blockingIssueDelta) { this.blockingIssueDelta = blockingIssueDelta; }
    public Integer getWarningIssueDelta() { return warningIssueDelta; }
    public void setWarningIssueDelta(Integer warningIssueDelta) { this.warningIssueDelta = warningIssueDelta; }
    public Integer getFailedVerificationDelta() { return failedVerificationDelta; }
    public void setFailedVerificationDelta(Integer failedVerificationDelta) { this.failedVerificationDelta = failedVerificationDelta; }
    public Boolean getRollbackReadyChanged() { return rollbackReadyChanged; }
    public void setRollbackReadyChanged(Boolean rollbackReadyChanged) { this.rollbackReadyChanged = rollbackReadyChanged; }
    public BigDecimal getSignoffCompletionDelta() { return signoffCompletionDelta; }
    public void setSignoffCompletionDelta(BigDecimal signoffCompletionDelta) { this.signoffCompletionDelta = signoffCompletionDelta; }
    public String getTrendSummary() { return trendSummary; }
    public void setTrendSummary(String trendSummary) { this.trendSummary = trendSummary; }
}
