package com.aicoding.platform.orchestration.domain;

public enum GovernancePlaybookTemplateMatchMode {
    EXACT("EXACT"), CATEGORY_PRIORITY("CATEGORY_PRIORITY"), CATEGORY_ONLY("CATEGORY_ONLY"), DEFAULT("DEFAULT");
    private final String value;
    GovernancePlaybookTemplateMatchMode(String v) { this.value = v; }
    public String getValue() { return value; }
}
