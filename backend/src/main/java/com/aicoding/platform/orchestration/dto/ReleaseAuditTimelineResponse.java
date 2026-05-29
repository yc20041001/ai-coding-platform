package com.aicoding.platform.orchestration.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class ReleaseAuditTimelineResponse {

    private String planId;
    private String releaseLabel;
    private Integer totalEvents;
    private LocalDateTime latestEventTime;
    private Map<String, Integer> eventCountsByType;
    private List<ReleaseAuditEventResponse> events;

    public String getPlanId() { return planId; }
    public void setPlanId(String planId) { this.planId = planId; }
    public String getReleaseLabel() { return releaseLabel; }
    public void setReleaseLabel(String releaseLabel) { this.releaseLabel = releaseLabel; }
    public Integer getTotalEvents() { return totalEvents; }
    public void setTotalEvents(Integer totalEvents) { this.totalEvents = totalEvents; }
    public LocalDateTime getLatestEventTime() { return latestEventTime; }
    public void setLatestEventTime(LocalDateTime latestEventTime) { this.latestEventTime = latestEventTime; }
    public Map<String, Integer> getEventCountsByType() { return eventCountsByType; }
    public void setEventCountsByType(Map<String, Integer> eventCountsByType) { this.eventCountsByType = eventCountsByType; }
    public List<ReleaseAuditEventResponse> getEvents() { return events; }
    public void setEvents(List<ReleaseAuditEventResponse> events) { this.events = events; }
}
