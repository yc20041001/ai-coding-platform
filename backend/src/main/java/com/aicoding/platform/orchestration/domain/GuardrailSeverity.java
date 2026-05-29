package com.aicoding.platform.orchestration.domain;

public enum GuardrailSeverity {
    INFO("INFO"),
    LOW("LOW"),
    MEDIUM("MEDIUM"),
    HIGH("HIGH"),
    CRITICAL("CRITICAL");

    private final String value;

    GuardrailSeverity(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
