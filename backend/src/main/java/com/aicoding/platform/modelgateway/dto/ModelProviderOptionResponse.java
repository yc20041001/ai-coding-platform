package com.aicoding.platform.modelgateway.dto;

public class ModelProviderOptionResponse {

    private String provider;
    private String displayName;
    private boolean supportsStream;
    private boolean supportsNonStream;
    private boolean requiresApiKey;
    private boolean requiresBaseUrl;
    private String defaultBaseUrl;
    private String[] knownModels;

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public boolean isSupportsStream() { return supportsStream; }
    public void setSupportsStream(boolean supportsStream) { this.supportsStream = supportsStream; }

    public boolean isSupportsNonStream() { return supportsNonStream; }
    public void setSupportsNonStream(boolean supportsNonStream) { this.supportsNonStream = supportsNonStream; }

    public boolean isRequiresApiKey() { return requiresApiKey; }
    public void setRequiresApiKey(boolean requiresApiKey) { this.requiresApiKey = requiresApiKey; }

    public boolean isRequiresBaseUrl() { return requiresBaseUrl; }
    public void setRequiresBaseUrl(boolean requiresBaseUrl) { this.requiresBaseUrl = requiresBaseUrl; }

    public String getDefaultBaseUrl() { return defaultBaseUrl; }
    public void setDefaultBaseUrl(String defaultBaseUrl) { this.defaultBaseUrl = defaultBaseUrl; }

    public String[] getKnownModels() { return knownModels; }
    public void setKnownModels(String[] knownModels) { this.knownModels = knownModels; }
}
