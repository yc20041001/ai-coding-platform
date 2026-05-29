package com.aicoding.platform.orchestration.domain;

public enum GuardrailEvaluationStatus {
    PASS("PASS"),
    WARN("WARN"),
    BLOCK("BLOCK"),
    SKIP("SKIP");

    private final String value;

    GuardrailEvaluationStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
