package com.aicoding.platform.orchestration.domain;

public enum ReleaseRiskLevel {
    LOW("LOW"),
    MEDIUM("MEDIUM"),
    HIGH("HIGH"),
    CRITICAL("CRITICAL");

    private final String value;

    ReleaseRiskLevel(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
