package com.aicoding.platform.orchestration.dto;

import java.time.LocalDateTime;

public class BetaTrialFeedbackResponse {

    private String id;
    private String sessionId;
    private String projectId;
    private String category;
    private String subcategory;
    private String severity;
    private String sourceType;
    private String title;
    private String detail;
    private String expectedBehavior;
    private String actualBehavior;
    private String suggestedAction;
    private String triageStatus;
    private String mappedIncidentId;
    private String mappedKnownIssueId;
    private Boolean releaseBlocking;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public BetaTrialFeedbackResponse() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getSubcategory() { return subcategory; }
    public void setSubcategory(String subcategory) { this.subcategory = subcategory; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }

    public String getExpectedBehavior() { return expectedBehavior; }
    public void setExpectedBehavior(String expectedBehavior) { this.expectedBehavior = expectedBehavior; }

    public String getActualBehavior() { return actualBehavior; }
    public void setActualBehavior(String actualBehavior) { this.actualBehavior = actualBehavior; }

    public String getSuggestedAction() { return suggestedAction; }
    public void setSuggestedAction(String suggestedAction) { this.suggestedAction = suggestedAction; }

    public String getTriageStatus() { return triageStatus; }
    public void setTriageStatus(String triageStatus) { this.triageStatus = triageStatus; }

    public String getMappedIncidentId() { return mappedIncidentId; }
    public void setMappedIncidentId(String mappedIncidentId) { this.mappedIncidentId = mappedIncidentId; }

    public String getMappedKnownIssueId() { return mappedKnownIssueId; }
    public void setMappedKnownIssueId(String mappedKnownIssueId) { this.mappedKnownIssueId = mappedKnownIssueId; }

    public Boolean getReleaseBlocking() { return releaseBlocking; }
    public void setReleaseBlocking(Boolean releaseBlocking) { this.releaseBlocking = releaseBlocking; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
