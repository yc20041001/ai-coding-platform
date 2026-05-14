package com.aicoding.platform.orchestrator.dto;

public class ExecuteTaskRequest {

    private String agentId;
    private String instruction;
    private Boolean useRag;
    private String knowledgeBaseId;
    private Integer ragLimit;

    public String getAgentId() { return agentId; }
    public void setAgentId(String agentId) { this.agentId = agentId; }

    public String getInstruction() { return instruction; }
    public void setInstruction(String instruction) { this.instruction = instruction; }

    public Boolean getUseRag() { return useRag; }
    public void setUseRag(Boolean useRag) { this.useRag = useRag; }

    public String getKnowledgeBaseId() { return knowledgeBaseId; }
    public void setKnowledgeBaseId(String knowledgeBaseId) { this.knowledgeBaseId = knowledgeBaseId; }

    public Integer getRagLimit() { return ragLimit; }
    public void setRagLimit(Integer ragLimit) { this.ragLimit = ragLimit; }
}
