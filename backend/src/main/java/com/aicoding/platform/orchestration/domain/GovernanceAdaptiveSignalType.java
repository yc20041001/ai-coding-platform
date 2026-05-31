package com.aicoding.platform.orchestration.domain;

public enum GovernanceAdaptiveSignalType {
    SUGGESTION_TYPE_WEIGHT, FOCUS_MODE_WEIGHT, CATEGORY_WEIGHT, BUNDLE_REUSE_SIGNAL, DISMISSAL_RISK_SIGNAL;
    private final String value;
    GovernanceAdaptiveSignalType() { this.value = name(); }
    public String getValue() { return value; }
}
