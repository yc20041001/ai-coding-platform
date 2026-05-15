package com.aicoding.platform.modelgateway.dto;

import jakarta.validation.constraints.NotBlank;

public class ModelConnectionTestRequest {

    @NotBlank
    private String provider;

    private String baseUrl;

    private String modelName;

    private String apiKey;

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
}
