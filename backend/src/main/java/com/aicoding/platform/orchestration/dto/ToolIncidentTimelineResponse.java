package com.aicoding.platform.orchestration.dto;

import java.util.List;

public class ToolIncidentTimelineResponse {

    private String incidentId;
    private List<ToolIncidentTimelineEventResponse> events;

    public ToolIncidentTimelineResponse() {}

    public String getIncidentId() { return incidentId; }
    public void setIncidentId(String incidentId) { this.incidentId = incidentId; }

    public List<ToolIncidentTimelineEventResponse> getEvents() { return events; }
    public void setEvents(List<ToolIncidentTimelineEventResponse> events) { this.events = events; }
}
