package com.aicoding.platform.modelgateway.application;

import com.aicoding.platform.modelgateway.config.ModelGatewayProperties;
import com.aicoding.platform.modelgateway.domain.ModelGatewayErrorType;
import com.aicoding.platform.modelgateway.dto.ModelRequest;
import com.aicoding.platform.modelgateway.dto.ModelResponse;
import com.aicoding.platform.modelgateway.dto.ModelStreamChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicReference;

@Service
public class DefaultModelGateway implements ModelGateway {

    private static final Logger log = LoggerFactory.getLogger(DefaultModelGateway.class);

    private final ModelGatewayProperties properties;
    private final ModelProviderRegistry providerRegistry;
    private final ModelConfigResolver configResolver;
    private final PromptSafetyService promptSafetyService;
    private final MockModelProvider mockModelProvider;

    public DefaultModelGateway(ModelGatewayProperties properties,
                               ModelProviderRegistry providerRegistry,
                               ModelConfigResolver configResolver,
                               PromptSafetyService promptSafetyService,
                               MockModelProvider mockModelProvider) {
        this.properties = properties;
        this.providerRegistry = providerRegistry;
        this.configResolver = configResolver;
        this.promptSafetyService = promptSafetyService;
        this.mockModelProvider = mockModelProvider;
    }

    @Override
    public ModelResponse generate(ModelRequest request) {
        // 1. Prompt safety check
        PromptSafetyService.SafetyResult safetyResult = promptSafetyService.check(request);
        if (!safetyResult.isPassed()) {
            log.warn("Prompt safety rejected request, no fallback");
            return safetyResult.getRejectedResponse();
        }

        // 2. Resolve provider config
        ResolvedModelConfig config = configResolver.resolve(request);
        log.info("Resolved provider={} model={} enabled={}", config.getProvider(), config.getModelName(), config.isEnabled());

        if (!config.isEnabled()) {
            log.warn("Provider {} is not enabled, attempting fallback", config.getProvider());
            return fallbackIfEnabled(request, ModelGatewayErrorType.CONFIG_ERROR,
                    "Provider " + config.getProvider() + " is not enabled", 0L);
        }

        // 3. Get provider from registry
        ModelProvider provider = providerRegistry.getProvider(config.getProvider());
        if (provider == null) {
            log.warn("No provider found for {}, attempting fallback", config.getProvider());
            return fallbackIfEnabled(request, ModelGatewayErrorType.CONFIG_ERROR,
                    "No provider found for " + config.getProvider(), 0L);
        }

        // 4. Call provider with retry
        ModelResponse response = callWithRetry(request, provider, config);
        if (response.getSuccess()) {
            return response;
        }

        // 5. Fallback on failure
        if (safetyResult.isWarning()) {
            log.info("Prompt safety warning flagged but request proceeds, response will reflect actual provider");
        }

        boolean requestAllowsFallback = request.getFallbackEnabled() == null || request.getFallbackEnabled();
        if (properties.isFallbackEnabled() && requestAllowsFallback) {
            log.warn("Provider {} failed ({}), falling back to Mock. Original error: {}",
                    config.getProvider(), response.getErrorType(), response.getErrorMessage());
            return fallbackToMock(request, response.getErrorMessage());
        }

        return response;
    }

    private ModelResponse callWithRetry(ModelRequest request, ModelProvider provider, ResolvedModelConfig config) {
        int maxAttempts = properties.getRetryTimes() + 1;
        ModelResponse lastResponse = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            if (attempt > 1) {
                log.info("Retry attempt {}/{} for provider {}", attempt, maxAttempts, config.getProvider());
            }

            ModelResponse response = provider.generate(request);

            if (response.getSuccess()) {
                response.setProvider(config.getProvider());
                response.setModelName(config.getModelName());
                return response;
            }

            lastResponse = response;

            // Only retry on timeout and network errors
            String errorType = response.getErrorType();
            if (!ModelGatewayErrorType.TIMEOUT.name().equals(errorType)
                    && !ModelGatewayErrorType.NETWORK_ERROR.name().equals(errorType)
                    && !ModelGatewayErrorType.RATE_LIMIT.name().equals(errorType)) {
                log.info("Error type {} is not retryable, stopping after attempt {}", errorType, attempt);
                break;
            }
        }

        return lastResponse != null ? lastResponse : errorResponse(config.getProvider(),
                ModelGatewayErrorType.UNKNOWN, "All retry attempts exhausted", 0L);
    }

    private ModelResponse fallbackIfEnabled(ModelRequest request, ModelGatewayErrorType errorType,
                                             String errorMessage, long latencyMs) {
        boolean requestAllowsFallback = request.getFallbackEnabled() == null || request.getFallbackEnabled();
        if (properties.isFallbackEnabled() && requestAllowsFallback) {
            log.info("Falling back to Mock provider. Reason: {}", errorMessage);
            return fallbackToMock(request, errorMessage);
        }

        log.warn("Fallback disabled, returning error: {}", errorMessage);
        return errorResponse(request.getProvider() != null ? request.getProvider() : properties.getDefaultProvider(),
                errorType, errorMessage, latencyMs);
    }

    private ModelResponse fallbackToMock(ModelRequest request, String originalError) {
        ModelResponse mockResponse = mockModelProvider.generate(request);
        mockResponse.setFallbackUsed(true);
        mockResponse.setProvider("MOCK");
        mockResponse.setModelName("mock-agent-model");
        if (originalError != null && !originalError.isBlank()) {
            String combined = "[Fallback from: " + originalError + "]";
            if (mockResponse.getErrorMessage() != null) {
                combined = mockResponse.getErrorMessage() + " " + combined;
            }
            mockResponse.setErrorMessage(combined);
        }
        log.info("Fallback to Mock completed, success={}", mockResponse.getSuccess());
        return mockResponse;
    }

    @Override
    public void stream(ModelRequest request, ModelStreamCallback callback) {
        long startTime = System.currentTimeMillis();

        // 1. Prompt safety check
        PromptSafetyService.SafetyResult safetyResult = promptSafetyService.check(request);
        if (!safetyResult.isPassed()) {
            log.warn("Prompt safety rejected stream request, no fallback");
            callback.onError(safetyResult.getRejectedResponse());
            return;
        }

        // 2. Resolve provider config
        ResolvedModelConfig config = configResolver.resolve(request);
        log.info("Stream resolved provider={} model={} enabled={}", config.getProvider(), config.getModelName(), config.isEnabled());

        if (!config.isEnabled()) {
            log.warn("Provider {} is not enabled for stream, attempting fallback", config.getProvider());
            streamFallbackToMock(request, callback, startTime,
                    ModelGatewayErrorType.CONFIG_ERROR, "Provider " + config.getProvider() + " is not enabled");
            return;
        }

        // 3. Get provider from registry
        ModelProvider provider = providerRegistry.getProvider(config.getProvider());
        if (provider == null) {
            log.warn("No provider found for {} stream, attempting fallback", config.getProvider());
            streamFallbackToMock(request, callback, startTime,
                    ModelGatewayErrorType.CONFIG_ERROR, "No provider found for " + config.getProvider());
            return;
        }

        // 4. Wrap callback for error handling and fallback
        AtomicReference<String> fullContent = new AtomicReference<>("");
        AtomicReference<ModelResponse> finalResponse = new AtomicReference<>();
        AtomicReference<Boolean> streamError = new AtomicReference<>(false);

        ModelStreamCallback wrappedCallback = new ModelStreamCallback() {
            @Override
            public void onToken(ModelStreamChunk chunk) {
                if (chunk.getProvider() == null) chunk.setProvider(config.getProvider());
                if (chunk.getModelName() == null) chunk.setModelName(config.getModelName());
                String content = chunk.getContent() != null ? chunk.getContent() : "";
                fullContent.updateAndGet(v -> v + content);
                callback.onToken(chunk);
            }

            @Override
            public void onComplete(ModelResponse response) {
                response.setProvider(config.getProvider());
                response.setModelName(config.getModelName());
                finalResponse.set(response);
                callback.onComplete(response);
            }

            @Override
            public void onError(ModelResponse errorResponse) {
                streamError.set(true);
                finalResponse.set(errorResponse);

                // Only retry/fallback for retryable errors
                String errorType = errorResponse.getErrorType();
                boolean retryable = ModelGatewayErrorType.TIMEOUT.name().equals(errorType)
                        || ModelGatewayErrorType.NETWORK_ERROR.name().equals(errorType)
                        || ModelGatewayErrorType.RATE_LIMIT.name().equals(errorType);

                if (retryable && properties.getRetryTimes() > 0) {
                    log.info("Stream error is retryable ({}), starting retry/fallback", errorType);
                    streamWithRetry(request, callback, config, 0);
                    return;
                }

                boolean requestAllowsFallback = request.getFallbackEnabled() == null || request.getFallbackEnabled();
                if (properties.isFallbackEnabled() && requestAllowsFallback) {
                    log.warn("Stream failed ({}), falling back to Mock. Original error: {}",
                            errorType, errorResponse.getErrorMessage());
                    String originalError = errorResponse.getErrorMessage();
                    doStreamFallbackToMock(request, callback, startTime, originalError);
                    return;
                }

                callback.onError(errorResponse);
            }
        };

        // 5. Call provider stream
        if (provider.supportsStream()) {
            provider.stream(request, wrappedCallback);
        } else {
            provider.stream(request, wrappedCallback); // Uses default one-shot fallback
        }
    }

    private void streamWithRetry(ModelRequest request, ModelStreamCallback callback,
                                  ResolvedModelConfig config, int attempt) {
        if (attempt >= properties.getRetryTimes()) {
            log.warn("Stream retry attempts exhausted, falling back to Mock");
            streamFallbackToMock(request, callback, System.currentTimeMillis(),
                    ModelGatewayErrorType.UNKNOWN, "All stream retry attempts exhausted");
            return;
        }

        ModelProvider provider = providerRegistry.getProvider(config.getProvider());
        if (provider == null) {
            streamFallbackToMock(request, callback, System.currentTimeMillis(),
                    ModelGatewayErrorType.CONFIG_ERROR, "No provider found for stream retry");
            return;
        }

        log.info("Stream retry attempt {}/{} for provider {}", attempt + 1, properties.getRetryTimes() + 1, config.getProvider());

        provider.stream(request, new ModelStreamCallback() {
            @Override
            public void onToken(ModelStreamChunk chunk) {
                if (chunk.getProvider() == null) chunk.setProvider(config.getProvider());
                if (chunk.getModelName() == null) chunk.setModelName(config.getModelName());
                callback.onToken(chunk);
            }

            @Override
            public void onComplete(ModelResponse response) {
                response.setProvider(config.getProvider());
                response.setModelName(config.getModelName());
                callback.onComplete(response);
            }

            @Override
            public void onError(ModelResponse errorResponse) {
                String errorType = errorResponse.getErrorType();
                boolean retryable = ModelGatewayErrorType.TIMEOUT.name().equals(errorType)
                        || ModelGatewayErrorType.NETWORK_ERROR.name().equals(errorType)
                        || ModelGatewayErrorType.RATE_LIMIT.name().equals(errorType);
                if (retryable) {
                    streamWithRetry(request, callback, config, attempt + 1);
                } else {
                    boolean allowsFallback = request.getFallbackEnabled() == null || request.getFallbackEnabled();
                    if (properties.isFallbackEnabled() && allowsFallback) {
                        doStreamFallbackToMock(request, callback, System.currentTimeMillis(), errorResponse.getErrorMessage());
                    } else {
                        callback.onError(errorResponse);
                    }
                }
            }
        });
    }

    private void streamFallbackToMock(ModelRequest request, ModelStreamCallback callback,
                                       long startTime, ModelGatewayErrorType errorType, String message) {
        boolean requestAllowsFallback = request.getFallbackEnabled() == null || request.getFallbackEnabled();
        if (properties.isFallbackEnabled() && requestAllowsFallback) {
            doStreamFallbackToMock(request, callback, startTime, message);
        } else {
            ModelResponse errorResponse = errorResponse(
                    request.getProvider() != null ? request.getProvider() : properties.getDefaultProvider(),
                    errorType, message, System.currentTimeMillis() - startTime);
            callback.onError(errorResponse);
        }
    }

    private void doStreamFallbackToMock(ModelRequest request, ModelStreamCallback callback,
                                         long startTime, String originalError) {
        log.info("Stream falling back to Mock. Reason: {}", originalError);

        StringBuilder fullContent = new StringBuilder();
        mockModelProvider.stream(request, new ModelStreamCallback() {
            @Override
            public void onToken(ModelStreamChunk chunk) {
                fullContent.append(chunk.getContent() != null ? chunk.getContent() : "");
                chunk.setFallbackUsed(true);
                callback.onToken(chunk);
            }

            @Override
            public void onComplete(ModelResponse response) {
                response.setFallbackUsed(true);
                response.setProvider("MOCK");
                response.setModelName("mock-agent-model");
                if (response.getLatencyMs() == null) {
                    response.setLatencyMs(System.currentTimeMillis() - startTime);
                }
                if (originalError != null && !originalError.isBlank()) {
                    String combined = "[Fallback from: " + originalError + "]";
                    if (response.getErrorMessage() != null && !response.getErrorMessage().isBlank()) {
                        combined = response.getErrorMessage() + " " + combined;
                    }
                    response.setErrorMessage(combined);
                }
                log.info("Stream fallback to Mock completed, success={}", response.getSuccess());
                callback.onComplete(response);
            }

            @Override
            public void onError(ModelResponse errorResponse) {
                log.error("Stream fallback to Mock also failed: {}", errorResponse.getErrorMessage());
                callback.onError(errorResponse);
            }
        });
    }
    private ModelResponse errorResponse(String provider, ModelGatewayErrorType errorType,
                                         String message, long latencyMs) {
        ModelResponse response = new ModelResponse();
        response.setSuccess(false);
        response.setContent("");
        response.setPromptTokens(0L);
        response.setCompletionTokens(0L);
        response.setTotalTokens(0L);
        response.setLatencyMs(latencyMs);
        response.setProvider(provider);
        response.setErrorType(errorType.name());
        response.setErrorMessage(message);
        return response;
    }
}
