package com.aicoding.platform.orchestration.dto;

public class CreateReleaseRolloutStepRequest {

    private String planId;
    private String projectId;
    private Integer stepOrder;
    private String stepKey;
    private String displayName;
    private String verificationScope;
    private Integer required;
    private Integer blocking;
    private String instructions;
    private String expectedResult;

    public String getPlanId() { return planId; }
    public void setPlanId(String planId) { this.planId = planId; }
    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }
    public Integer getStepOrder() { return stepOrder; }
    public void setStepOrder(Integer stepOrder) { this.stepOrder = stepOrder; }
    public String getStepKey() { return stepKey; }
    public void setStepKey(String stepKey) { this.stepKey = stepKey; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getVerificationScope() { return verificationScope; }
    public void setVerificationScope(String verificationScope) { this.verificationScope = verificationScope; }
    public Integer getRequired() { return required; }
    public void setRequired(Integer required) { this.required = required; }
    public Integer getBlocking() { return blocking; }
    public void setBlocking(Integer blocking) { this.blocking = blocking; }
    public String getInstructions() { return instructions; }
    public void setInstructions(String instructions) { this.instructions = instructions; }
    public String getExpectedResult() { return expectedResult; }
    public void setExpectedResult(String expectedResult) { this.expectedResult = expectedResult; }
}
