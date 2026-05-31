package com.aicoding.platform.orchestration.domain;

public enum GovernanceBestPracticeType {
    DRAFT_ADOPTION, ASSISTIVE_USE, PACKAGE_QUALITY, OUTCOME_REVIEW, OPERATOR_PRODUCTIVITY;
    private final String value;
    GovernanceBestPracticeType() { this.value = name(); }
    public String getValue() { return value; }
}
