package com.aicoding.platform.orchestration.dto;

import java.time.LocalDateTime;
import java.util.Map;

public class ToolExecutionTraceEventResponse {

    private String eventType;
    private String title;
    private String description;
    private String status;
    private LocalDateTime eventTime;
    private Map<String, Object> metadata;

    public ToolExecutionTraceEventResponse() {}

    public ToolExecutionTraceEventResponse(String eventType, String title, String description,
                                           String status, LocalDateTime eventTime) {
        this.eventType = eventType;
        this.title = title;
        this.description = description;
        this.status = status;
        this.eventTime = eventTime;
    }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getEventTime() { return eventTime; }
    public void setEventTime(LocalDateTime eventTime) { this.eventTime = eventTime; }

    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
}
