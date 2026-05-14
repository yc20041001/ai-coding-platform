package com.aicoding.platform.modelgateway.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ModelProviderRegistry {

    private static final Logger log = LoggerFactory.getLogger(ModelProviderRegistry.class);

    private final Map<String, ModelProvider> providerMap = new ConcurrentHashMap<>();
    private final MockModelProvider mockModelProvider;

    public ModelProviderRegistry(List<ModelProvider> providers, MockModelProvider mockModelProvider) {
        this.mockModelProvider = mockModelProvider;
        for (ModelProvider provider : providers) {
            String type = provider.providerType().toUpperCase();
            providerMap.put(type, provider);
            log.info("Registered ModelProvider: {}", type);
        }
    }

    public ModelProvider getProvider(String provider) {
        if (provider == null || provider.isBlank()) {
            log.debug("No provider specified, using mock fallback");
            return mockModelProvider;
        }

        String key = provider.toUpperCase();
        ModelProvider resolved = providerMap.get(key);
        if (resolved != null) {
            return resolved;
        }

        // Fallback: check supports() for multi-provider implementations (e.g. OpenAiCompatibleProvider)
        for (ModelProvider p : providerMap.values()) {
            if (p.supports(provider)) {
                log.debug("Provider '{}' resolved via supports() to {}", provider, p.providerType());
                return p;
            }
        }

        log.warn("Provider '{}' not found in registry, falling back to mock", provider);
        return mockModelProvider;
    }
}
