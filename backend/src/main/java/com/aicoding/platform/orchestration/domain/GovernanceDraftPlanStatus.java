package com.aicoding.platform.orchestration.domain;

public enum GovernanceDraftPlanStatus {
    DRAFT("DRAFT"), READY_FOR_REVIEW("READY_FOR_REVIEW"), REVIEWED("REVIEWED"), ARCHIVED("ARCHIVED");
    private final String value;
    GovernanceDraftPlanStatus(String v) { this.value = v; }
    public String getValue() { return value; }
}
