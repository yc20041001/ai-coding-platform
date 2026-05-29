package com.aicoding.platform.orchestration.dto;

import java.time.LocalDateTime;

public class ReleaseAuditEventResponse {

    private String id;
    private String projectId;
    private String planId;
    private String releaseLabel;
    private String eventType;
    private String actorId;
    private String actorName;
    private String summary;
    private String detail;
    private String relatedStepId;
    private String relatedVerificationId;
    private String relatedIncidentId;
    private String relatedAlertId;
    private String evidenceJson;
    private LocalDateTime eventTime;
    private LocalDateTime createTime;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }
    public String getPlanId() { return planId; }
    public void setPlanId(String planId) { this.planId = planId; }
    public String getReleaseLabel() { return releaseLabel; }
    public void setReleaseLabel(String releaseLabel) { this.releaseLabel = releaseLabel; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public String getActorId() { return actorId; }
    public void setActorId(String actorId) { this.actorId = actorId; }
    public String getActorName() { return actorName; }
    public void setActorName(String actorName) { this.actorName = actorName; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
    public String getRelatedStepId() { return relatedStepId; }
    public void setRelatedStepId(String relatedStepId) { this.relatedStepId = relatedStepId; }
    public String getRelatedVerificationId() { return relatedVerificationId; }
    public void setRelatedVerificationId(String relatedVerificationId) { this.relatedVerificationId = relatedVerificationId; }
    public String getRelatedIncidentId() { return relatedIncidentId; }
    public void setRelatedIncidentId(String relatedIncidentId) { this.relatedIncidentId = relatedIncidentId; }
    public String getRelatedAlertId() { return relatedAlertId; }
    public void setRelatedAlertId(String relatedAlertId) { this.relatedAlertId = relatedAlertId; }
    public String getEvidenceJson() { return evidenceJson; }
    public void setEvidenceJson(String evidenceJson) { this.evidenceJson = evidenceJson; }
    public LocalDateTime getEventTime() { return eventTime; }
    public void setEventTime(LocalDateTime eventTime) { this.eventTime = eventTime; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
