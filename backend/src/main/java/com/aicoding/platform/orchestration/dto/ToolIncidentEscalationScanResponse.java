package com.aicoding.platform.orchestration.dto;

public class ToolIncidentEscalationScanResponse {

    private int scanned;
    private int escalated;
    private int skipped;
    private int maxLevelReached;

    public ToolIncidentEscalationScanResponse() {}

    public int getScanned() { return scanned; }
    public void setScanned(int scanned) { this.scanned = scanned; }

    public int getEscalated() { return escalated; }
    public void setEscalated(int escalated) { this.escalated = escalated; }

    public int getSkipped() { return skipped; }
    public void setSkipped(int skipped) { this.skipped = skipped; }

    public int getMaxLevelReached() { return maxLevelReached; }
    public void setMaxLevelReached(int maxLevelReached) { this.maxLevelReached = maxLevelReached; }
}
