package com.aicoding.platform.github.dto;

public class GithubOAuthAuthorizeResponse {
    private String authorizeUrl;
    private String state;
    private boolean configured;

    public String getAuthorizeUrl() { return authorizeUrl; }
    public void setAuthorizeUrl(String authorizeUrl) { this.authorizeUrl = authorizeUrl; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public boolean isConfigured() { return configured; }
    public void setConfigured(boolean configured) { this.configured = configured; }
}
