package com.aicoding.platform.orchestration.dto;

import java.time.LocalDateTime;

public class IncidentRetrospectiveResponse {

    private String id;
    private String projectId;
    private String incidentId;
    private String rootCauseNoteId;
    private String title;
    private String summary;
    private String whatHappened;
    private String impactSummary;
    private String responseSummary;
    private String lessonsLearned;
    private String preventionPlan;
    private String actionItems;
    private String ownerId;
    private LocalDateTime dueAt;
    private String regressionRisk;
    private Boolean repeatedIncident;
    private String status;
    private LocalDateTime publishedAt;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public IncidentRetrospectiveResponse() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    public String getIncidentId() { return incidentId; }
    public void setIncidentId(String incidentId) { this.incidentId = incidentId; }

    public String getRootCauseNoteId() { return rootCauseNoteId; }
    public void setRootCauseNoteId(String rootCauseNoteId) { this.rootCauseNoteId = rootCauseNoteId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public String getWhatHappened() { return whatHappened; }
    public void setWhatHappened(String whatHappened) { this.whatHappened = whatHappened; }

    public String getImpactSummary() { return impactSummary; }
    public void setImpactSummary(String impactSummary) { this.impactSummary = impactSummary; }

    public String getResponseSummary() { return responseSummary; }
    public void setResponseSummary(String responseSummary) { this.responseSummary = responseSummary; }

    public String getLessonsLearned() { return lessonsLearned; }
    public void setLessonsLearned(String lessonsLearned) { this.lessonsLearned = lessonsLearned; }

    public String getPreventionPlan() { return preventionPlan; }
    public void setPreventionPlan(String preventionPlan) { this.preventionPlan = preventionPlan; }

    public String getActionItems() { return actionItems; }
    public void setActionItems(String actionItems) { this.actionItems = actionItems; }

    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }

    public LocalDateTime getDueAt() { return dueAt; }
    public void setDueAt(LocalDateTime dueAt) { this.dueAt = dueAt; }

    public String getRegressionRisk() { return regressionRisk; }
    public void setRegressionRisk(String regressionRisk) { this.regressionRisk = regressionRisk; }

    public Boolean getRepeatedIncident() { return repeatedIncident; }
    public void setRepeatedIncident(Boolean repeatedIncident) { this.repeatedIncident = repeatedIncident; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getPublishedAt() { return publishedAt; }
    public void setPublishedAt(LocalDateTime publishedAt) { this.publishedAt = publishedAt; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
