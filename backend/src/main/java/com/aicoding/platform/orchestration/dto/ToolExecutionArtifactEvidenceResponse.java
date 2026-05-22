package com.aicoding.platform.orchestration.dto;

import java.time.LocalDateTime;

public class ToolExecutionArtifactEvidenceResponse {

    private String artifactId;
    private String artifactType;
    private String title;
    private String patchReviewStatus;
    private String patchReviewDecision;
    private LocalDateTime createTime;

    public ToolExecutionArtifactEvidenceResponse() {}

    public String getArtifactId() { return artifactId; }
    public void setArtifactId(String artifactId) { this.artifactId = artifactId; }

    public String getArtifactType() { return artifactType; }
    public void setArtifactType(String artifactType) { this.artifactType = artifactType; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getPatchReviewStatus() { return patchReviewStatus; }
    public void setPatchReviewStatus(String patchReviewStatus) { this.patchReviewStatus = patchReviewStatus; }

    public String getPatchReviewDecision() { return patchReviewDecision; }
    public void setPatchReviewDecision(String patchReviewDecision) { this.patchReviewDecision = patchReviewDecision; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
