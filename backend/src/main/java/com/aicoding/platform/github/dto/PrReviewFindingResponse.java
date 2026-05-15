package com.aicoding.platform.github.dto;

public class PrReviewFindingResponse {
    private String id;
    private String reviewJobId;
    private String severity;
    private String category;
    private String filePath;
    private Integer lineNumber;
    private String title;
    private String description;
    private String suggestion;
    private String codeSnippet;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getReviewJobId() { return reviewJobId; }
    public void setReviewJobId(String reviewJobId) { this.reviewJobId = reviewJobId; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public Integer getLineNumber() { return lineNumber; }
    public void setLineNumber(Integer lineNumber) { this.lineNumber = lineNumber; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getSuggestion() { return suggestion; }
    public void setSuggestion(String suggestion) { this.suggestion = suggestion; }

    public String getCodeSnippet() { return codeSnippet; }
    public void setCodeSnippet(String codeSnippet) { this.codeSnippet = codeSnippet; }
}
