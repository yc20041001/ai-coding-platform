package com.aicoding.platform.orchestration.dto;

public class CreateIncidentRootCauseNoteRequest {

    private String rootCause;
    private String impact;
    private String resolution;
    private String prevention;
    private String followUpActions;
    private String tags;
    private String confidence;

    public CreateIncidentRootCauseNoteRequest() {}

    public String getRootCause() { return rootCause; }
    public void setRootCause(String rootCause) { this.rootCause = rootCause; }

    public String getImpact() { return impact; }
    public void setImpact(String impact) { this.impact = impact; }

    public String getResolution() { return resolution; }
    public void setResolution(String resolution) { this.resolution = resolution; }

    public String getPrevention() { return prevention; }
    public void setPrevention(String prevention) { this.prevention = prevention; }

    public String getFollowUpActions() { return followUpActions; }
    public void setFollowUpActions(String followUpActions) { this.followUpActions = followUpActions; }

    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }

    public String getConfidence() { return confidence; }
    public void setConfidence(String confidence) { this.confidence = confidence; }
}
