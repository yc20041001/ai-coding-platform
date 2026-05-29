package com.aicoding.platform.orchestration.domain;

public enum ReleaseConfidenceLevel {

    HIGH("HIGH"),
    MEDIUM("MEDIUM"),
    LOW("LOW"),
    CRITICAL("CRITICAL");

    private final String value;

    ReleaseConfidenceLevel(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
