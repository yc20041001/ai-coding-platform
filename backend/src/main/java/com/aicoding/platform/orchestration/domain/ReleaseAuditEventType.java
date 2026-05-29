package com.aicoding.platform.orchestration.domain;

public enum ReleaseAuditEventType {

    PLAN_CREATED("PLAN_CREATED"),
    PLAN_STATUS_CHANGED("PLAN_STATUS_CHANGED"),
    STEP_STATUS_CHANGED("STEP_STATUS_CHANGED"),
    VERIFICATION_RECORDED("VERIFICATION_RECORDED"),
    ROLLBACK_DRILL_UPDATED("ROLLBACK_DRILL_UPDATED"),
    DECISION_LINKED("DECISION_LINKED"),
    INCIDENT_LINKED("INCIDENT_LINKED"),
    POSTMORTEM_UPDATED("POSTMORTEM_UPDATED"),
    REPORT_EXPORTED("REPORT_EXPORTED");

    private final String value;

    ReleaseAuditEventType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
