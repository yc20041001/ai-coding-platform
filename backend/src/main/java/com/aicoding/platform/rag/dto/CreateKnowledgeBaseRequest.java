package com.aicoding.platform.rag.dto;

import jakarta.validation.constraints.NotBlank;

public class CreateKnowledgeBaseRequest {

    @NotBlank
    private String name;
    private String description;
    private Integer chunkSize;
    private Integer chunkOverlap;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Integer getChunkSize() { return chunkSize; }
    public void setChunkSize(Integer chunkSize) { this.chunkSize = chunkSize; }

    public Integer getChunkOverlap() { return chunkOverlap; }
    public void setChunkOverlap(Integer chunkOverlap) { this.chunkOverlap = chunkOverlap; }
}
