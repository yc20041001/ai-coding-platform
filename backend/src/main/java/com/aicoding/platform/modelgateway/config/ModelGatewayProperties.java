package com.aicoding.platform.modelgateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

@ConfigurationProperties(prefix = "app.model-gateway")
public class ModelGatewayProperties {

    private String defaultProvider = "MOCK";
    private String fallbackProvider = "MOCK";
    private boolean fallbackEnabled = true;
    private long timeoutMs = 30000;
    private int retryTimes = 1;
    private boolean promptSafetyEnabled = true;
    private Map<String, ProviderProperties> providers = new HashMap<>();

    public String getDefaultProvider() { return defaultProvider; }
    public void setDefaultProvider(String defaultProvider) { this.defaultProvider = defaultProvider; }

    public String getFallbackProvider() { return fallbackProvider; }
    public void setFallbackProvider(String fallbackProvider) { this.fallbackProvider = fallbackProvider; }

    public boolean isFallbackEnabled() { return fallbackEnabled; }
    public void setFallbackEnabled(boolean fallbackEnabled) { this.fallbackEnabled = fallbackEnabled; }

    public long getTimeoutMs() { return timeoutMs; }
    public void setTimeoutMs(long timeoutMs) { this.timeoutMs = timeoutMs; }

    public int getRetryTimes() { return retryTimes; }
    public void setRetryTimes(int retryTimes) { this.retryTimes = retryTimes; }

    public boolean isPromptSafetyEnabled() { return promptSafetyEnabled; }
    public void setPromptSafetyEnabled(boolean promptSafetyEnabled) { this.promptSafetyEnabled = promptSafetyEnabled; }

    public Map<String, ProviderProperties> getProviders() { return providers; }
    public void setProviders(Map<String, ProviderProperties> providers) { this.providers = providers; }

    public ProviderProperties getProviderConfig(String name) {
        if (name == null || name.isBlank()) {
            return new ProviderProperties();
        }
        ProviderProperties config = providers.get(name.toLowerCase());
        return config != null ? config : new ProviderProperties();
    }

    public static class ProviderProperties {
        private boolean enabled = false;
        private String baseUrl;
        private String apiKey;
        private String modelName;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }

        public String getModelName() { return modelName; }
        public void setModelName(String modelName) { this.modelName = modelName; }

        public String maskApiKey() {
            if (apiKey == null || apiKey.isEmpty()) {
                return "<empty>";
            }
            if (apiKey.length() <= 8) {
                return "****";
            }
            return apiKey.substring(0, 4) + "****" + apiKey.substring(apiKey.length() - 4);
        }
    }
}
