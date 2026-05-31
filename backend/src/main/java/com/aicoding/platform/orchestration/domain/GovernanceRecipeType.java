package com.aicoding.platform.orchestration.domain;

public enum GovernanceRecipeType {
    REMEDIATION("REMEDIATION"), WAIVER_MITIGATION("WAIVER_MITIGATION"),
    HANDOFF_SUPPORT("HANDOFF_SUPPORT"), ESCALATION_RESPONSE("ESCALATION_RESPONSE");
    private final String value;
    GovernanceRecipeType(String v) { this.value = v; }
    public String getValue() { return value; }
}
