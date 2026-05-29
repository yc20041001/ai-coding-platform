package com.aicoding.platform.orchestration.dto;

import java.util.List;

public class BetaReleaseGateDashboardResponse {

    private GateSummary summary;
    private List<BetaReleaseGateEvaluationResponse> evaluations;
    private List<BetaReleaseDecisionResponse> recentDecisions;

    public GateSummary getSummary() { return summary; }
    public void setSummary(GateSummary summary) { this.summary = summary; }
    public List<BetaReleaseGateEvaluationResponse> getEvaluations() { return evaluations; }
    public void setEvaluations(List<BetaReleaseGateEvaluationResponse> evaluations) { this.evaluations = evaluations; }
    public List<BetaReleaseDecisionResponse> getRecentDecisions() { return recentDecisions; }
    public void setRecentDecisions(List<BetaReleaseDecisionResponse> recentDecisions) { this.recentDecisions = recentDecisions; }

    public static class GateSummary {
        private long totalRules;
        private long blockingFailures;
        private long warningCount;
        private long passCount;
        private String overallStatus;

        public long getTotalRules() { return totalRules; }
        public void setTotalRules(long totalRules) { this.totalRules = totalRules; }
        public long getBlockingFailures() { return blockingFailures; }
        public void setBlockingFailures(long blockingFailures) { this.blockingFailures = blockingFailures; }
        public long getWarningCount() { return warningCount; }
        public void setWarningCount(long warningCount) { this.warningCount = warningCount; }
        public long getPassCount() { return passCount; }
        public void setPassCount(long passCount) { this.passCount = passCount; }
        public String getOverallStatus() { return overallStatus; }
        public void setOverallStatus(String overallStatus) { this.overallStatus = overallStatus; }
    }
}
