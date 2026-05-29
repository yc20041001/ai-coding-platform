package com.aicoding.platform.orchestration.dto;

import java.time.LocalDateTime;

public class BetaTrialSessionResponse {

    private String id;
    private String projectId;
    private String title;
    private String participantRole;
    private String environmentType;
    private String providerMode;
    private String githubOauthStatus;
    private String sessionStatus;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private String completedPathJson;
    private String blockedAtStep;
    private String blockerSummary;
    private Integer satisfactionScore;
    private String continueIntent;
    private String summary;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public BetaTrialSessionResponse() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getParticipantRole() { return participantRole; }
    public void setParticipantRole(String participantRole) { this.participantRole = participantRole; }

    public String getEnvironmentType() { return environmentType; }
    public void setEnvironmentType(String environmentType) { this.environmentType = environmentType; }

    public String getProviderMode() { return providerMode; }
    public void setProviderMode(String providerMode) { this.providerMode = providerMode; }

    public String getGithubOauthStatus() { return githubOauthStatus; }
    public void setGithubOauthStatus(String githubOauthStatus) { this.githubOauthStatus = githubOauthStatus; }

    public String getSessionStatus() { return sessionStatus; }
    public void setSessionStatus(String sessionStatus) { this.sessionStatus = sessionStatus; }

    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }

    public LocalDateTime getEndedAt() { return endedAt; }
    public void setEndedAt(LocalDateTime endedAt) { this.endedAt = endedAt; }

    public String getCompletedPathJson() { return completedPathJson; }
    public void setCompletedPathJson(String completedPathJson) { this.completedPathJson = completedPathJson; }

    public String getBlockedAtStep() { return blockedAtStep; }
    public void setBlockedAtStep(String blockedAtStep) { this.blockedAtStep = blockedAtStep; }

    public String getBlockerSummary() { return blockerSummary; }
    public void setBlockerSummary(String blockerSummary) { this.blockerSummary = blockerSummary; }

    public Integer getSatisfactionScore() { return satisfactionScore; }
    public void setSatisfactionScore(Integer satisfactionScore) { this.satisfactionScore = satisfactionScore; }

    public String getContinueIntent() { return continueIntent; }
    public void setContinueIntent(String continueIntent) { this.continueIntent = continueIntent; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
