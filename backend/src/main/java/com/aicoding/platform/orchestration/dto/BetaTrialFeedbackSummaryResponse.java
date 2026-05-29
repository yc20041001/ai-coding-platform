package com.aicoding.platform.orchestration.dto;

import java.time.LocalDateTime;

public class BetaTrialFeedbackSummaryResponse {

    private String id;
    private String sessionId;
    private String category;
    private String severity;
    private String title;
    private String triageStatus;
    private Boolean releaseBlocking;
    private LocalDateTime createTime;

    public BetaTrialFeedbackSummaryResponse() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getTriageStatus() { return triageStatus; }
    public void setTriageStatus(String triageStatus) { this.triageStatus = triageStatus; }

    public Boolean getReleaseBlocking() { return releaseBlocking; }
    public void setReleaseBlocking(Boolean releaseBlocking) { this.releaseBlocking = releaseBlocking; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
