package com.aicoding.platform.agent.dto;

import java.math.BigDecimal;

public class ProjectAgentRuntimeConfig {

    private BigDecimal temperature;
    private Integer maxTokens;
    private Integer timeoutSeconds;
    private Boolean useRag;
    private String knowledgeBaseId;
    private String customInstruction;

    public String toSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("Temp ");
        sb.append(temperature != null ? temperature.stripTrailingZeros().toPlainString() : "0.2");
        sb.append(" / ");
        sb.append(maxTokens != null ? String.valueOf(maxTokens) : "4096");
        sb.append(" tokens");
        if (Boolean.TRUE.equals(useRag)) {
            sb.append(" / RAG 开");
        }
        return sb.toString();
    }

    public BigDecimal getTemperature() { return temperature; }
    public void setTemperature(BigDecimal temperature) { this.temperature = temperature; }

    public Integer getMaxTokens() { return maxTokens; }
    public void setMaxTokens(Integer maxTokens) { this.maxTokens = maxTokens; }

    public Integer getTimeoutSeconds() { return timeoutSeconds; }
    public void setTimeoutSeconds(Integer timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }

    public Boolean getUseRag() { return useRag; }
    public void setUseRag(Boolean useRag) { this.useRag = useRag; }

    public String getKnowledgeBaseId() { return knowledgeBaseId; }
    public void setKnowledgeBaseId(String knowledgeBaseId) { this.knowledgeBaseId = knowledgeBaseId; }

    public String getCustomInstruction() { return customInstruction; }
    public void setCustomInstruction(String customInstruction) { this.customInstruction = customInstruction; }
}
