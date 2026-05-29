package com.aicoding.platform.orchestration.domain;

public enum ReleaseExpansionRecommendation {
    EXPAND_NOW("EXPAND_NOW"),
    EXPAND_WITH_GUARDRAILS("EXPAND_WITH_GUARDRAILS"),
    HOLD("HOLD"),
    BLOCK("BLOCK");

    private final String value;

    ReleaseExpansionRecommendation(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
