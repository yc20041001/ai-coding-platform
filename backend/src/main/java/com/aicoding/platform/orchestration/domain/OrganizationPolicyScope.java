package com.aicoding.platform.orchestration.domain;

public enum OrganizationPolicyScope {
    GLOBAL("GLOBAL"),
    PROJECT_GROUP("PROJECT_GROUP"),
    PROJECT_OVERRIDE("PROJECT_OVERRIDE");

    private final String value;

    OrganizationPolicyScope(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
