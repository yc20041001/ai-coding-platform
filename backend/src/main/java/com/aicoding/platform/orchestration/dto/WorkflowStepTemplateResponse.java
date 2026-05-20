package com.aicoding.platform.orchestration.dto;

public class WorkflowStepTemplateResponse {

    private Integer stepOrder;
    private String stepType;
    private String agentCode;
    private String laneKey;
    private String title;

    public Integer getStepOrder() { return stepOrder; }
    public void setStepOrder(Integer stepOrder) { this.stepOrder = stepOrder; }

    public String getStepType() { return stepType; }
    public void setStepType(String stepType) { this.stepType = stepType; }

    public String getAgentCode() { return agentCode; }
    public void setAgentCode(String agentCode) { this.agentCode = agentCode; }

    public String getLaneKey() { return laneKey; }
    public void setLaneKey(String laneKey) { this.laneKey = laneKey; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
}
