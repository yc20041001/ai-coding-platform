package com.aicoding.platform.orchestration.domain;

public enum ReleaseSignoffRole {

    TECH_OWNER("TECH_OWNER"),
    PRODUCT_OWNER("PRODUCT_OWNER"),
    OPS_OWNER("OPS_OWNER"),
    SECURITY_REVIEWER("SECURITY_REVIEWER"),
    QA_REVIEWER("QA_REVIEWER");

    private final String value;

    ReleaseSignoffRole(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
