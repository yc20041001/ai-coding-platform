package com.aicoding.platform.orchestration.dto;

public class CreateReleaseVerificationRecordRequest {

    private String planId;
    private String projectId;
    private String verificationPhase;
    private String verificationKey;
    private String displayName;
    private String verificationStatus;
    private String severity;
    private String summary;
    private String detail;
    private String evidenceJson;
    private String relatedIncidentId;
    private String relatedAlertId;
    private String recordedBy;

    public String getPlanId() { return planId; }
    public void setPlanId(String planId) { this.planId = planId; }
    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }
    public String getVerificationPhase() { return verificationPhase; }
    public void setVerificationPhase(String verificationPhase) { this.verificationPhase = verificationPhase; }
    public String getVerificationKey() { return verificationKey; }
    public void setVerificationKey(String verificationKey) { this.verificationKey = verificationKey; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getVerificationStatus() { return verificationStatus; }
    public void setVerificationStatus(String verificationStatus) { this.verificationStatus = verificationStatus; }
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
    public String getEvidenceJson() { return evidenceJson; }
    public void setEvidenceJson(String evidenceJson) { this.evidenceJson = evidenceJson; }
    public String getRelatedIncidentId() { return relatedIncidentId; }
    public void setRelatedIncidentId(String relatedIncidentId) { this.relatedIncidentId = relatedIncidentId; }
    public String getRelatedAlertId() { return relatedAlertId; }
    public void setRelatedAlertId(String relatedAlertId) { this.relatedAlertId = relatedAlertId; }
    public String getRecordedBy() { return recordedBy; }
    public void setRecordedBy(String recordedBy) { this.recordedBy = recordedBy; }
}
