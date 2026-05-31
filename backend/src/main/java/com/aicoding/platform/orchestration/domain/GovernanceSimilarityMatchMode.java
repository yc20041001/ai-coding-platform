package com.aicoding.platform.orchestration.domain;

public enum GovernanceSimilarityMatchMode {
    EXACT("EXACT"), CATEGORY_GUARDRAIL("CATEGORY_GUARDRAIL"),
    CATEGORY_PRIORITY("CATEGORY_PRIORITY"), TAG_OVERLAP("TAG_OVERLAP"), DEFAULT("DEFAULT");
    private final String value;
    GovernanceSimilarityMatchMode(String v) { this.value = v; }
    public String getValue() { return value; }
}
