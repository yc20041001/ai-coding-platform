package com.aicoding.platform.orchestration.domain;

public enum GovernanceDraftOptimizationScopeType {
    DRAFT_TYPE, SCOPE, PRIORITY, CATEGORY, RISK_LEVEL;
    private final String value;
    GovernanceDraftOptimizationScopeType() { this.value = name(); }
    public String getValue() { return value; }
}
