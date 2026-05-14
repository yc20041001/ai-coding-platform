package com.aicoding.platform.rag.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.rag")
public class RagProperties {

    private boolean enabled = true;
    private boolean chatEnabled = true;
    private boolean agentEnabled = true;
    private int defaultLimit = 5;
    private int maxContextChars = 4000;
    private int maxChunkChars = 800;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public boolean isChatEnabled() { return chatEnabled; }
    public void setChatEnabled(boolean chatEnabled) { this.chatEnabled = chatEnabled; }

    public boolean isAgentEnabled() { return agentEnabled; }
    public void setAgentEnabled(boolean agentEnabled) { this.agentEnabled = agentEnabled; }

    public int getDefaultLimit() { return defaultLimit; }
    public void setDefaultLimit(int defaultLimit) { this.defaultLimit = defaultLimit; }

    public int getMaxContextChars() { return maxContextChars; }
    public void setMaxContextChars(int maxContextChars) { this.maxContextChars = maxContextChars; }

    public int getMaxChunkChars() { return maxChunkChars; }
    public void setMaxChunkChars(int maxChunkChars) { this.maxChunkChars = maxChunkChars; }
}
