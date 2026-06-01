package com.aicoding.platform.orchestration.domain;

public enum GovernanceBenchmarkType {
    ADOPTION_RATE("ADOPTION_RATE"), MATURITY_SCORE("MATURITY_SCORE"),
    ALIGNMENT_SCORE("ALIGNMENT_SCORE"), UPLIFT_SCORE("UPLIFT_SCORE");
    private final String value;
    GovernanceBenchmarkType(String v) { this.value = v; }
    public String getValue() { return value; }
}
