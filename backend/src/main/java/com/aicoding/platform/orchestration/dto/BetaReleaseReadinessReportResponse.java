package com.aicoding.platform.orchestration.dto;

import java.util.List;

public class BetaReleaseReadinessReportResponse {

    private String releaseLabel;
    private String overallStatus;
    private String reportMarkdown;
    private List<BetaReleaseGateEvaluationResponse> evaluations;
    private BetaReleaseDecisionResponse decision;

    public String getReleaseLabel() { return releaseLabel; }
    public void setReleaseLabel(String releaseLabel) { this.releaseLabel = releaseLabel; }
    public String getOverallStatus() { return overallStatus; }
    public void setOverallStatus(String overallStatus) { this.overallStatus = overallStatus; }
    public String getReportMarkdown() { return reportMarkdown; }
    public void setReportMarkdown(String reportMarkdown) { this.reportMarkdown = reportMarkdown; }
    public List<BetaReleaseGateEvaluationResponse> getEvaluations() { return evaluations; }
    public void setEvaluations(List<BetaReleaseGateEvaluationResponse> evaluations) { this.evaluations = evaluations; }
    public BetaReleaseDecisionResponse getDecision() { return decision; }
    public void setDecision(BetaReleaseDecisionResponse decision) { this.decision = decision; }
}
