package com.aicoding.platform.orchestration.domain;

public enum GovernanceFeedbackReasonCode {
    HELPFUL, TOO_GENERIC, NOT_RELEVANT, TOO_COMPLEX, MISSING_CONTEXT, LOW_IMPACT, GOOD_BUNDLE, BAD_ORDERING;
    private final String value;
    GovernanceFeedbackReasonCode() { this.value = name(); }
    public String getValue() { return value; }
}
