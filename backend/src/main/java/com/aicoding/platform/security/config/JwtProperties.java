package com.aicoding.platform.security.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

    private static final Logger log = LoggerFactory.getLogger(JwtProperties.class);
    private static final String DEV_DEFAULT_SECRET = "ai-coding-platform-dev-secret-do-not-use-in-production";

    private String secret;
    private long accessTokenExpireSeconds = 7200;
    private long refreshTokenExpireSeconds = 604800;

    @PostConstruct
    public void validate() {
        if (secret == null || secret.isBlank()) {
            log.warn("============================================");
            log.warn("JWT_SECRET is not set. Using dev default secret.");
            log.warn("This is NOT safe for production environments.");
            log.warn("Set JWT_SECRET environment variable before deploying.");
            log.warn("============================================");
            this.secret = DEV_DEFAULT_SECRET;
        }
    }

    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }

    public long getAccessTokenExpireSeconds() { return accessTokenExpireSeconds; }
    public void setAccessTokenExpireSeconds(long accessTokenExpireSeconds) { this.accessTokenExpireSeconds = accessTokenExpireSeconds; }

    public long getRefreshTokenExpireSeconds() { return refreshTokenExpireSeconds; }
    public void setRefreshTokenExpireSeconds(long refreshTokenExpireSeconds) { this.refreshTokenExpireSeconds = refreshTokenExpireSeconds; }
}
