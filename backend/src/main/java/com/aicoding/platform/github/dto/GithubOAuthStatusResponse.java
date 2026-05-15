package com.aicoding.platform.github.dto;

public class GithubOAuthStatusResponse {
    private boolean configured;
    private boolean bound;
    private String githubLogin;
    private Long githubUserId;

    public boolean isConfigured() { return configured; }
    public void setConfigured(boolean configured) { this.configured = configured; }

    public boolean isBound() { return bound; }
    public void setBound(boolean bound) { this.bound = bound; }

    public String getGithubLogin() { return githubLogin; }
    public void setGithubLogin(String githubLogin) { this.githubLogin = githubLogin; }

    public Long getGithubUserId() { return githubUserId; }
    public void setGithubUserId(Long githubUserId) { this.githubUserId = githubUserId; }
}
