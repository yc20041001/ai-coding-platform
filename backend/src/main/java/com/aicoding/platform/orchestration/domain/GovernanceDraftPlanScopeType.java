package com.aicoding.platform.orchestration.domain;

public enum GovernanceDraftPlanScopeType {
    RECOMMENDATION("RECOMMENDATION"), PROJECT("PROJECT"), OWNER("OWNER"), PORTFOLIO("PORTFOLIO");
    private final String value;
    GovernanceDraftPlanScopeType(String v) { this.value = v; }
    public String getValue() { return value; }
}
