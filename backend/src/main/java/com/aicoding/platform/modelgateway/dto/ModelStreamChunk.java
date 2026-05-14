package com.aicoding.platform.modelgateway.dto;

public class ModelStreamChunk {

    private String content;
    private boolean done;
    private String provider;
    private String modelName;
    private Long promptTokens;
    private Long completionTokens;
    private Long totalTokens;
    private String errorType;
    private String errorMessage;
    private Boolean fallbackUsed;

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public boolean isDone() { return done; }
    public void setDone(boolean done) { this.done = done; }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }

    public Long getPromptTokens() { return promptTokens; }
    public void setPromptTokens(Long promptTokens) { this.promptTokens = promptTokens; }

    public Long getCompletionTokens() { return completionTokens; }
    public void setCompletionTokens(Long completionTokens) { this.completionTokens = completionTokens; }

    public Long getTotalTokens() { return totalTokens; }
    public void setTotalTokens(Long totalTokens) { this.totalTokens = totalTokens; }

    public String getErrorType() { return errorType; }
    public void setErrorType(String errorType) { this.errorType = errorType; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public Boolean getFallbackUsed() { return fallbackUsed; }
    public void setFallbackUsed(Boolean fallbackUsed) { this.fallbackUsed = fallbackUsed; }
}
