package com.aicoding.platform.orchestration.dto;

public class UpdateBetaTrialFeedbackRequest {

    private String category;
    private String subcategory;
    private String severity;
    private String title;
    private String detail;
    private String expectedBehavior;
    private String actualBehavior;
    private String suggestedAction;
    private String triageStatus;
    private String mappedIncidentId;
    private String mappedKnownIssueId;
    private Boolean releaseBlocking;

    public UpdateBetaTrialFeedbackRequest() {}

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getSubcategory() { return subcategory; }
    public void setSubcategory(String subcategory) { this.subcategory = subcategory; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

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
}
