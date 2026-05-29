package com.aicoding.platform.orchestration.domain;

public enum ReleaseVerificationSeverity {

    INFO("INFO"),
    LOW("LOW"),
    MEDIUM("MEDIUM"),
    HIGH("HIGH"),
    CRITICAL("CRITICAL");

    private final String value;

    ReleaseVerificationSeverity(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
