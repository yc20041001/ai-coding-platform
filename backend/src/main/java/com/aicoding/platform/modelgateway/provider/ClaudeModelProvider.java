package com.aicoding.platform.modelgateway.provider;

import com.aicoding.platform.modelgateway.application.ModelProvider;
import com.aicoding.platform.modelgateway.config.ModelGatewayProperties;
import com.aicoding.platform.modelgateway.domain.ModelGatewayErrorType;
import com.aicoding.platform.modelgateway.dto.ModelRequest;
import com.aicoding.platform.modelgateway.dto.ModelResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ClaudeModelProvider implements ModelProvider {

    private static final Logger log = LoggerFactory.getLogger(ClaudeModelProvider.class);

    private final ModelGatewayProperties properties;

    public ClaudeModelProvider(ModelGatewayProperties properties) {
        this.properties = properties;
    }

    @Override
    public String providerType() {
        return "CLAUDE";
    }

    @Override
    public boolean supports(String provider) {
        return "CLAUDE".equalsIgnoreCase(provider);
    }

    @Override
    public ModelResponse generate(ModelRequest request) {
        ModelGatewayProperties.ProviderProperties config = properties.getProviderConfig("claude");

        if (!config.isEnabled()) {
            log.warn("Claude provider is not enabled");
            return configError("Claude provider is not enabled");
        }

        if (config.getApiKey() == null || config.getApiKey().isBlank()) {
            log.warn("Claude API key is not configured");
            return configError("Claude API key is not configured");
        }

        log.warn("Claude provider is a stub - real implementation not yet available");
        return configError("Claude provider is a stub - real implementation pending in future milestone");
    }

    private ModelResponse configError(String message) {
        ModelResponse response = new ModelResponse();
        response.setSuccess(false);
        response.setContent("");
        response.setPromptTokens(0L);
        response.setCompletionTokens(0L);
        response.setTotalTokens(0L);
        response.setLatencyMs(0L);
        response.setProvider("CLAUDE");
        response.setErrorType(ModelGatewayErrorType.CONFIG_ERROR.name());
        response.setErrorMessage(message);
        return response;
    }
}
