package com.aicoding.platform.orchestration.dto;

import java.time.LocalDateTime;

public class IncidentRootCauseNoteResponse {

    private String id;
    private String projectId;
    private String incidentId;
    private String rootCause;
    private String impact;
    private String resolution;
    private String prevention;
    private String followUpActions;
    private String tags;
    private String confidence;
    private String status;
    private String authorId;
    private String lastEditorId;
    private LocalDateTime publishedAt;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public IncidentRootCauseNoteResponse() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    public String getIncidentId() { return incidentId; }
    public void setIncidentId(String incidentId) { this.incidentId = incidentId; }

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

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getAuthorId() { return authorId; }
    public void setAuthorId(String authorId) { this.authorId = authorId; }

    public String getLastEditorId() { return lastEditorId; }
    public void setLastEditorId(String lastEditorId) { this.lastEditorId = lastEditorId; }

    public LocalDateTime getPublishedAt() { return publishedAt; }
    public void setPublishedAt(LocalDateTime publishedAt) { this.publishedAt = publishedAt; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
