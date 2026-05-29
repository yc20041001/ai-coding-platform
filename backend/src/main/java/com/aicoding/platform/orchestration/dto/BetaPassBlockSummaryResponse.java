package com.aicoding.platform.orchestration.dto;

public class BetaPassBlockSummaryResponse {

    private Long totalFeedback;
    private Long releaseBlockingCount;
    private Long p0Count;
    private Long p1Count;
    private Long newCount;
    private Long triagedCount;
    private Long scheduledCount;
    private Long doneCount;
    private Long wontFixCount;

    public BetaPassBlockSummaryResponse() {}

    public Long getTotalFeedback() { return totalFeedback; }
    public void setTotalFeedback(Long totalFeedback) { this.totalFeedback = totalFeedback; }

    public Long getReleaseBlockingCount() { return releaseBlockingCount; }
    public void setReleaseBlockingCount(Long releaseBlockingCount) { this.releaseBlockingCount = releaseBlockingCount; }

    public Long getP0Count() { return p0Count; }
    public void setP0Count(Long p0Count) { this.p0Count = p0Count; }

    public Long getP1Count() { return p1Count; }
    public void setP1Count(Long p1Count) { this.p1Count = p1Count; }

    public Long getNewCount() { return newCount; }
    public void setNewCount(Long newCount) { this.newCount = newCount; }

    public Long getTriagedCount() { return triagedCount; }
    public void setTriagedCount(Long triagedCount) { this.triagedCount = triagedCount; }

    public Long getScheduledCount() { return scheduledCount; }
    public void setScheduledCount(Long scheduledCount) { this.scheduledCount = scheduledCount; }

    public Long getDoneCount() { return doneCount; }
    public void setDoneCount(Long doneCount) { this.doneCount = doneCount; }

    public Long getWontFixCount() { return wontFixCount; }
    public void setWontFixCount(Long wontFixCount) { this.wontFixCount = wontFixCount; }
}
