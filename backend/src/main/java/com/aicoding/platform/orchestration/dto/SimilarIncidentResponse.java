package com.aicoding.platform.orchestration.dto;

public class SimilarIncidentResponse {

    private String incidentId;
    private String title;
    private String status;
    private String severity;
    private double score;
    private String matchedField;
    private String snippet;
    private String createTime;

    public SimilarIncidentResponse() {}

    public String getIncidentId() { return incidentId; }
    public void setIncidentId(String incidentId) { this.incidentId = incidentId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public double getScore() { return score; }
    public void setScore(double score) { this.score = score; }

    public String getMatchedField() { return matchedField; }
    public void setMatchedField(String matchedField) { this.matchedField = matchedField; }

    public String getSnippet() { return snippet; }
    public void setSnippet(String snippet) { this.snippet = snippet; }

    public String getCreateTime() { return createTime; }
    public void setCreateTime(String createTime) { this.createTime = createTime; }
}
