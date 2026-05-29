package com.aicoding.platform.orchestration.domain;

public enum GovernanceTemplateScope {
    GLOBAL("GLOBAL"),
    PROJECT_TYPE("PROJECT_TYPE"),
    PROJECT_OVERRIDE("PROJECT_OVERRIDE");

    private final String value;

    GovernanceTemplateScope(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
