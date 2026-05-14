package com.aicoding.platform.rag.dto;

import jakarta.validation.constraints.NotBlank;

public class UploadKnowledgeDocumentRequest {

    @NotBlank
    private String knowledgeBaseId;
    @NotBlank
    private String title;
    @NotBlank
    private String documentType;
    private String sourceType;
    private String fileName;
    private String filePath;
    @NotBlank
    private String content;

    public String getKnowledgeBaseId() { return knowledgeBaseId; }
    public void setKnowledgeBaseId(String knowledgeBaseId) { this.knowledgeBaseId = knowledgeBaseId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDocumentType() { return documentType; }
    public void setDocumentType(String documentType) { this.documentType = documentType; }

    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
