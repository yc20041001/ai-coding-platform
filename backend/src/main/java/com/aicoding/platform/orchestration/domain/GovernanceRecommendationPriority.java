package com.aicoding.platform.orchestration.domain;

public enum GovernanceRecommendationPriority {
    P3("P3"),
    P2("P2"),
    P1("P1"),
    P0("P0");

    private final String value;

    GovernanceRecommendationPriority(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
