package com.aicoding.platform.modelgateway.application;

import com.aicoding.platform.modelgateway.config.ModelGatewayProperties;
import com.aicoding.platform.modelgateway.dto.ModelRequest;
import org.springframework.stereotype.Component;

@Component
public class ModelConfigResolver {

    private final ModelGatewayProperties properties;

    public ModelConfigResolver(ModelGatewayProperties properties) {
        this.properties = properties;
    }

    public ResolvedModelConfig resolve(ModelRequest request) {
        ResolvedModelConfig config = new ResolvedModelConfig();

        // 1. Determine provider: request.provider > properties.defaultProvider
        String providerName = request.getProvider();
        if (providerName == null || providerName.isBlank()) {
            providerName = properties.getDefaultProvider();
        }
        config.setProvider(providerName);

        // 2. Determine modelName: request.modelName > provider config modelName > mock default
        String modelName = request.getModelName();
        ModelGatewayProperties.ProviderProperties providerProps = properties.getProviderConfig(providerName);
        if (modelName == null || modelName.isBlank()) {
            modelName = providerProps.getModelName();
        }
        if (modelName == null || modelName.isBlank()) {
            modelName = "mock-agent-model";
        }
        config.setModelName(modelName);

        // 3. Load provider config
        config.setBaseUrl(providerProps.getBaseUrl());
        config.setApiKey(providerProps.getApiKey());
        config.setEnabled(providerProps.isEnabled() || "MOCK".equalsIgnoreCase(providerName));

        return config;
    }
}
