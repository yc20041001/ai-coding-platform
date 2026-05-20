package com.aicoding.platform.orchestration.dto;

public class StartMultiAgentRunRequest {

    private String strategy;
    private String instruction;
    private Boolean useRag;
    private String knowledgeBaseId;

    public String getStrategy() { return strategy; }
    public void setStrategy(String strategy) { this.strategy = strategy; }

    public String getInstruction() { return instruction; }
    public void setInstruction(String instruction) { this.instruction = instruction; }

    public Boolean getUseRag() { return useRag; }
    public void setUseRag(Boolean useRag) { this.useRag = useRag; }

    public String getKnowledgeBaseId() { return knowledgeBaseId; }
    public void setKnowledgeBaseId(String knowledgeBaseId) { this.knowledgeBaseId = knowledgeBaseId; }
}
