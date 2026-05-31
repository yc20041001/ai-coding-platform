package com.aicoding.platform.orchestration.domain;

public enum GovernanceDraftOptimizationSignalLevel {
    HIGH_CONFIDENCE("HIGH_CONFIDENCE"), MEDIUM_CONFIDENCE("MEDIUM_CONFIDENCE"),
    LOW_CONFIDENCE("LOW_CONFIDENCE"), INCONCLUSIVE("INCONCLUSIVE");
    private final String value;
    GovernanceDraftOptimizationSignalLevel(String v) { this.value = v; }
    public String getValue() { return value; }
}
