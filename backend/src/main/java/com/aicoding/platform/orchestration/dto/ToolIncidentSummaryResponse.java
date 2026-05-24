package com.aicoding.platform.orchestration.dto;

public class ToolIncidentSummaryResponse {

    private long openCount;
    private long acknowledgedCount;
    private long resolvedCount;
    private long criticalCount;
    private long highCount;
    private long deadLetteredCount;
    private long retryPendingCount;

    public ToolIncidentSummaryResponse() {}

    public long getOpenCount() { return openCount; }
    public void setOpenCount(long openCount) { this.openCount = openCount; }

    public long getAcknowledgedCount() { return acknowledgedCount; }
    public void setAcknowledgedCount(long acknowledgedCount) { this.acknowledgedCount = acknowledgedCount; }

    public long getResolvedCount() { return resolvedCount; }
    public void setResolvedCount(long resolvedCount) { this.resolvedCount = resolvedCount; }

    public long getCriticalCount() { return criticalCount; }
    public void setCriticalCount(long criticalCount) { this.criticalCount = criticalCount; }

    public long getHighCount() { return highCount; }
    public void setHighCount(long highCount) { this.highCount = highCount; }

    public long getDeadLetteredCount() { return deadLetteredCount; }
    public void setDeadLetteredCount(long deadLetteredCount) { this.deadLetteredCount = deadLetteredCount; }

    public long getRetryPendingCount() { return retryPendingCount; }
    public void setRetryPendingCount(long retryPendingCount) { this.retryPendingCount = retryPendingCount; }
}
