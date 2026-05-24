package com.aicoding.platform.orchestration.domain;

public enum ToolIncidentSlaStatus {
    NOT_STARTED,
    WITHIN_SLA,
    AT_RISK,
    BREACHED,
    RESOLVED,
    WAIVED
}
