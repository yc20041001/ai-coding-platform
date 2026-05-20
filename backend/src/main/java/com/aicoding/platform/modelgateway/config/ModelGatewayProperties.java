package com.aicoding.platform.modelgateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.lang.NonNull;

import java.util.HashMap;
import java.util.Map;

@ConfigurationProperties(prefix = "app.model-gateway")
public class ModelGatewayProperties implements EnvironmentAware {

    private String defaultProvider = "MOCK";
    private String fallbackProvider = "MOCK";
    private boolean fallbackEnabled = true;
    private long timeoutMs = 30000;
    private int retryTimes = 1;
    private boolean promptSafetyEnabled = true;
    private Map<String, ProviderProperties> providers = new HashMap<>();
    private Environment environment;

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
        ProviderProperties resolved = config != null ? copy(config) : new ProviderProperties();
        applyEnvironmentOverrides(name, resolved);
        return resolved;
    }

    @Override
    public void setEnvironment(@NonNull Environment environment) {
        this.environment = environment;
    }

    private ProviderProperties copy(ProviderProperties source) {
        ProviderProperties copy = new ProviderProperties();
        copy.setEnabled(source.isEnabled());
        copy.setBaseUrl(source.getBaseUrl());
        copy.setApiKey(source.getApiKey());
        copy.setModelName(source.getModelName());
        return copy;
    }

    private void applyEnvironmentOverrides(String providerName, ProviderProperties config) {
        if (environment == null || providerName == null || providerName.isBlank()) {
            return;
        }

        String prefix = providerName.toUpperCase().replace('-', '_');
        String enabled = environment.getProperty(prefix + "_ENABLED");
        if (enabled != null && !enabled.isBlank()) {
            config.setEnabled(Boolean.parseBoolean(enabled));
        }

        String baseUrl = environment.getProperty(prefix + "_BASE_URL");
        if (baseUrl != null && !baseUrl.isBlank()) {
            config.setBaseUrl(baseUrl);
        }

        String apiKey = environment.getProperty(prefix + "_API_KEY");
        if (apiKey != null && !apiKey.isBlank()) {
            config.setApiKey(apiKey);
        }

        String modelName = environment.getProperty(prefix + "_MODEL");
        if (modelName != null && !modelName.isBlank()) {
            config.setModelName(modelName);
        }
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
