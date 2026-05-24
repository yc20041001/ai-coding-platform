package com.aicoding.platform.orchestration.dto;

public class CreateIncidentRetrospectiveRequest {

    private String title;
    private String summary;
    private String whatHappened;
    private String impactSummary;
    private String responseSummary;
    private String lessonsLearned;
    private String preventionPlan;
    private String actionItems;
    private Long ownerId;
    private String dueAt;
    private String regressionRisk;
    private Boolean repeatedIncident;

    public CreateIncidentRetrospectiveRequest() {}

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

    public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }

    public String getDueAt() { return dueAt; }
    public void setDueAt(String dueAt) { this.dueAt = dueAt; }

    public String getRegressionRisk() { return regressionRisk; }
    public void setRegressionRisk(String regressionRisk) { this.regressionRisk = regressionRisk; }

    public Boolean getRepeatedIncident() { return repeatedIncident; }
    public void setRepeatedIncident(Boolean repeatedIncident) { this.repeatedIncident = repeatedIncident; }
}
