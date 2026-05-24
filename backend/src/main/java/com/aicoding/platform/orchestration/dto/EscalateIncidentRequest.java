package com.aicoding.platform.orchestration.dto;

public class EscalateIncidentRequest {

    private String reason;

    public EscalateIncidentRequest() {}

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
