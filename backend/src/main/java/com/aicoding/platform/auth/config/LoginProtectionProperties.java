package com.aicoding.platform.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.auth.login-protection")
public class LoginProtectionProperties {

    private boolean enabled = true;
    private int maxEmailFailures = 5;
    private int maxIpFailures = 20;
    private int failureWindowSeconds = 300;
    private int lockSeconds = 600;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getMaxEmailFailures() {
        return maxEmailFailures;
    }

    public void setMaxEmailFailures(int maxEmailFailures) {
        this.maxEmailFailures = maxEmailFailures;
    }

    public int getMaxIpFailures() {
        return maxIpFailures;
    }

    public void setMaxIpFailures(int maxIpFailures) {
        this.maxIpFailures = maxIpFailures;
    }

    public int getFailureWindowSeconds() {
        return failureWindowSeconds;
    }

    public void setFailureWindowSeconds(int failureWindowSeconds) {
        this.failureWindowSeconds = failureWindowSeconds;
    }

    public int getLockSeconds() {
        return lockSeconds;
    }

    public void setLockSeconds(int lockSeconds) {
        this.lockSeconds = lockSeconds;
    }
}