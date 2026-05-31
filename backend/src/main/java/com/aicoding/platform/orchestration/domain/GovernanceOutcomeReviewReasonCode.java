package com.aicoding.platform.orchestration.domain;

public enum GovernanceOutcomeReviewReasonCode {
    HIGH_QUALITY_DRAFT, NEEDS_MORE_CONTEXT, INCORRECT_ASSUMPTION,
    MISSING_STEPS, GOOD_BUT_INCOMPLETE, LOW_QUALITY_PACKAGE,
    ACCURATE_RECOMMENDATION, TIMELY_ASSISTANCE, TOO_LATE,
    ALREADY_RESOLVED, OTHER;
    private final String value;
    GovernanceOutcomeReviewReasonCode() { this.value = name(); }
    public String getValue() { return value; }
}
