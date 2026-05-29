package com.aicoding.platform.orchestration.dto;

public class BetaTrialDashboardResponse {

    private Long totalSessions;
    private Long completedSessions;
    private Long blockedSessions;
    private Long inProgressSessions;
    private Double averageSatisfactionScore;
    private Long continueYesCount;
    private Long p0Count;
    private Long p1Count;
    private Long releaseBlockingCount;
    private Long readinessPassCount;
    private Long readinessWarnCount;
    private Long readinessFailCount;

    public BetaTrialDashboardResponse() {}

    public Long getTotalSessions() { return totalSessions; }
    public void setTotalSessions(Long totalSessions) { this.totalSessions = totalSessions; }

    public Long getCompletedSessions() { return completedSessions; }
    public void setCompletedSessions(Long completedSessions) { this.completedSessions = completedSessions; }

    public Long getBlockedSessions() { return blockedSessions; }
    public void setBlockedSessions(Long blockedSessions) { this.blockedSessions = blockedSessions; }

    public Long getInProgressSessions() { return inProgressSessions; }
    public void setInProgressSessions(Long inProgressSessions) { this.inProgressSessions = inProgressSessions; }

    public Double getAverageSatisfactionScore() { return averageSatisfactionScore; }
    public void setAverageSatisfactionScore(Double averageSatisfactionScore) { this.averageSatisfactionScore = averageSatisfactionScore; }

    public Long getContinueYesCount() { return continueYesCount; }
    public void setContinueYesCount(Long continueYesCount) { this.continueYesCount = continueYesCount; }

    public Long getP0Count() { return p0Count; }
    public void setP0Count(Long p0Count) { this.p0Count = p0Count; }

    public Long getP1Count() { return p1Count; }
    public void setP1Count(Long p1Count) { this.p1Count = p1Count; }

    public Long getReleaseBlockingCount() { return releaseBlockingCount; }
    public void setReleaseBlockingCount(Long releaseBlockingCount) { this.releaseBlockingCount = releaseBlockingCount; }

    public Long getReadinessPassCount() { return readinessPassCount; }
    public void setReadinessPassCount(Long readinessPassCount) { this.readinessPassCount = readinessPassCount; }

    public Long getReadinessWarnCount() { return readinessWarnCount; }
    public void setReadinessWarnCount(Long readinessWarnCount) { this.readinessWarnCount = readinessWarnCount; }

    public Long getReadinessFailCount() { return readinessFailCount; }
    public void setReadinessFailCount(Long readinessFailCount) { this.readinessFailCount = readinessFailCount; }
}
