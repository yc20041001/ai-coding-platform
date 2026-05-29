package com.aicoding.platform.orchestration.dto;

import java.time.LocalDateTime;
import java.util.List;

public class ReleaseReadinessReportResponse {

    private String releaseLabel;
    private String decisionStatus;
    private String rolloutStatus;
    private String overallReadinessStatus;
    private String rolloutStrategy;
    private String targetEnvironment;
    private List<ReleaseVerificationRecordResponse> verifications;
    private List<ReleaseRolloutStepResponse> steps;
    private String reportMarkdown;
    private LocalDateTime generatedAt;

    public String getReleaseLabel() { return releaseLabel; }
    public void setReleaseLabel(String releaseLabel) { this.releaseLabel = releaseLabel; }
    public String getDecisionStatus() { return decisionStatus; }
    public void setDecisionStatus(String decisionStatus) { this.decisionStatus = decisionStatus; }
    public String getRolloutStatus() { return rolloutStatus; }
    public void setRolloutStatus(String rolloutStatus) { this.rolloutStatus = rolloutStatus; }
    public String getOverallReadinessStatus() { return overallReadinessStatus; }
    public void setOverallReadinessStatus(String overallReadinessStatus) { this.overallReadinessStatus = overallReadinessStatus; }
    public String getRolloutStrategy() { return rolloutStrategy; }
    public void setRolloutStrategy(String rolloutStrategy) { this.rolloutStrategy = rolloutStrategy; }
    public String getTargetEnvironment() { return targetEnvironment; }
    public void setTargetEnvironment(String targetEnvironment) { this.targetEnvironment = targetEnvironment; }
    public List<ReleaseVerificationRecordResponse> getVerifications() { return verifications; }
    public void setVerifications(List<ReleaseVerificationRecordResponse> verifications) { this.verifications = verifications; }
    public List<ReleaseRolloutStepResponse> getSteps() { return steps; }
    public void setSteps(List<ReleaseRolloutStepResponse> steps) { this.steps = steps; }
    public String getReportMarkdown() { return reportMarkdown; }
    public void setReportMarkdown(String reportMarkdown) { this.reportMarkdown = reportMarkdown; }
    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }
}
