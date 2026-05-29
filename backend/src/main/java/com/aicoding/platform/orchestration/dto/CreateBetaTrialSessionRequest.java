package com.aicoding.platform.orchestration.dto;

public class CreateBetaTrialSessionRequest {

    private String projectId;
    private String title;
    private String participantRole;
    private String environmentType;
    private String providerMode;
    private String githubOauthStatus;

    public CreateBetaTrialSessionRequest() {}

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
}
