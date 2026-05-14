package com.aicoding.platform.rag.dto;

import jakarta.validation.constraints.NotBlank;

public class RagSearchRequest {

    @NotBlank
    private String query;
    private String knowledgeBaseId;
    private Integer limit;
    private Boolean includeContent;

    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }

    public String getKnowledgeBaseId() { return knowledgeBaseId; }
    public void setKnowledgeBaseId(String knowledgeBaseId) { this.knowledgeBaseId = knowledgeBaseId; }

    public Integer getLimit() { return limit; }
    public void setLimit(Integer limit) { this.limit = limit; }

    public Boolean getIncludeContent() { return includeContent; }
    public void setIncludeContent(Boolean includeContent) { this.includeContent = includeContent; }
}
