package com.aicoding.platform.orchestration.domain;

public enum GovernanceEscalationType {
    OVERDUE_RECOMMENDATION("OVERDUE_RECOMMENDATION"),
    WAIVER_EXPIRING_SOON("WAIVER_EXPIRING_SOON"),
    WAIVER_EXPIRED("WAIVER_EXPIRED"),
    OWNER_OVERLOADED("OWNER_OVERLOADED"),
    OWNER_MISSING("OWNER_MISSING");

    private final String value;
    GovernanceEscalationType(String value) { this.value = value; }
    public String getValue() { return value; }
}
