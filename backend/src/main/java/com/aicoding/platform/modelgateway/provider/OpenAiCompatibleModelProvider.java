package com.aicoding.platform.modelgateway.provider;

import com.aicoding.platform.modelgateway.application.ModelProvider;
import com.aicoding.platform.modelgateway.application.ModelStreamCallback;
import com.aicoding.platform.modelgateway.config.ModelGatewayProperties;
import com.aicoding.platform.modelgateway.domain.ModelGatewayErrorType;
import com.aicoding.platform.modelgateway.dto.ModelRequest;
import com.aicoding.platform.modelgateway.dto.ModelResponse;
import com.aicoding.platform.modelgateway.dto.ModelStreamChunk;
import com.aicoding.platform.modelgateway.dto.OpenAiChatCompletionRequest;
import com.aicoding.platform.modelgateway.dto.OpenAiChatCompletionResponse;
import com.aicoding.platform.modelgateway.dto.OpenAiChatMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component
public class OpenAiCompatibleModelProvider implements ModelProvider {

    private static final Logger log = LoggerFactory.getLogger(OpenAiCompatibleModelProvider.class);

    private final ModelGatewayProperties properties;

    public OpenAiCompatibleModelProvider(ModelGatewayProperties properties) {
        this.properties = properties;
    }

    @Override
    public String providerType() {
        return "OPENAI_COMPATIBLE";
    }

    @Override
    public boolean supportsStream() {
        return true;
    }

    @Override
    public boolean supports(String provider) {
        if (provider == null) return false;
        String upper = provider.toUpperCase();
        return "OPENAI".equals(upper) || "DEEPSEEK".equals(upper) || "QWEN".equals(upper)
                || "GEMINI".equals(upper) || "OPENAI_COMPATIBLE".equals(upper);
    }

    @Override
    public ModelResponse generate(ModelRequest request) {
        long startTime = System.currentTimeMillis();

        String providerName = request.getProvider() != null ? request.getProvider().toUpperCase() : "OPENAI";
        ModelGatewayProperties.ProviderProperties config = properties.getProviderConfig(providerName);

        if (!config.isEnabled()) {
            log.warn("{} provider is not enabled", providerName);
            return error(providerName, ModelGatewayErrorType.CONFIG_ERROR,
                    providerName + " provider is not enabled", 0L);
        }

        if (config.getApiKey() == null || config.getApiKey().isBlank()) {
            log.warn("{} API key is not configured", providerName);
            return error(providerName, ModelGatewayErrorType.CONFIG_ERROR,
                    providerName + " API key is not configured", 0L);
        }

        if (config.getBaseUrl() == null || config.getBaseUrl().isBlank()) {
            log.warn("{} baseUrl is not configured", providerName);
            return error(providerName, ModelGatewayErrorType.CONFIG_ERROR,
                    providerName + " baseUrl is not configured", 0L);
        }

        String modelName = request.getModelName() != null ? request.getModelName() : config.getModelName();

        RestClient restClient = buildRestClient(config);

        OpenAiChatCompletionRequest body = buildRequestBody(request, modelName);
        String apiKeyMasked = config.maskApiKey();
        log.info("Calling {} model={} baseUrl={} apiKey={}", providerName, modelName, config.getBaseUrl(), apiKeyMasked);

        try {
            OpenAiChatCompletionResponse apiResponse = restClient.post()
                    .uri("/chat/completions")
                    .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                    .header("Authorization", "Bearer " + config.getApiKey())
                    .body(Objects.requireNonNull(body))
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                        byte[] bytes = res.getBody() != null ? res.getBody().readAllBytes() : new byte[0];
                        String bodyStr = new String(bytes);
                        log.error("{} HTTP {} from {}: {}", providerName, res.getStatusCode().value(), config.getBaseUrl(), bodyStr);
                        throw OpenAiHttpException.fromStatus(res.getStatusCode().value(), bodyStr);
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                        byte[] bytes = res.getBody() != null ? res.getBody().readAllBytes() : new byte[0];
                        String bodyStr = new String(bytes);
                        log.error("{} HTTP {} from {}: {}", providerName, res.getStatusCode().value(), config.getBaseUrl(), bodyStr);
                        throw OpenAiHttpException.fromStatus(res.getStatusCode().value(), bodyStr);
                    })
                    .body(OpenAiChatCompletionResponse.class);

            long latencyMs = System.currentTimeMillis() - startTime;

            if (apiResponse == null) {
                log.error("{} returned empty response body", providerName);
                return error(providerName, ModelGatewayErrorType.PROVIDER_ERROR,
                        "Provider returned empty response body", latencyMs);
            }

            String content = extractContent(apiResponse);
            ModelResponse response = new ModelResponse();
            response.setContent(content);
            response.setSuccess(true);
            response.setProvider(providerName);
            response.setModelName(modelName);
            response.setFallbackUsed(false);

            OpenAiChatCompletionResponse.Usage usage = apiResponse.getUsage();
            if (usage != null) {
                response.setPromptTokens(usage.getPromptTokens());
                response.setCompletionTokens(usage.getCompletionTokens());
                response.setTotalTokens(usage.getTotalTokens());
            } else {
                response.setPromptTokens(0L);
                response.setCompletionTokens(0L);
                response.setTotalTokens(0L);
            }
            response.setLatencyMs(latencyMs);

            log.info("{} call success: model={} totalTokens={} latencyMs={}",
                    providerName, modelName, response.getTotalTokens(), latencyMs);
            return response;

        } catch (OpenAiHttpException e) {
            long latencyMs = System.currentTimeMillis() - startTime;
            log.error("{} HTTP error: status={} message={}", providerName, e.getStatusCode(), e.getMessage());
            return error(providerName, e.getErrorType(), e.getMessage(), latencyMs);
        } catch (ResourceAccessException e) {
            long latencyMs = System.currentTimeMillis() - startTime;
            Throwable cause = e.getCause();
            if (cause instanceof SocketTimeoutException) {
                log.error("{} timeout after {}ms", providerName, properties.getTimeoutMs());
                return error(providerName, ModelGatewayErrorType.TIMEOUT,
                        "Request timed out after " + properties.getTimeoutMs() + "ms", latencyMs);
            }
            log.error("{} network error: {}", providerName, e.getMessage());
            return error(providerName, ModelGatewayErrorType.NETWORK_ERROR,
                    "Network error: " + e.getMessage(), latencyMs);
        } catch (Exception e) {
            long latencyMs = System.currentTimeMillis() - startTime;
            log.error("{} unexpected error: {}", providerName, e.getMessage(), e);
            return error(providerName, ModelGatewayErrorType.PROVIDER_ERROR,
                    "Unexpected error: " + e.getMessage(), latencyMs);
        }
    }

    @Override
    public void stream(ModelRequest request, ModelStreamCallback callback) {
        long startTime = System.currentTimeMillis();

        String providerName = request.getProvider() != null ? request.getProvider().toUpperCase() : "OPENAI";
        ModelGatewayProperties.ProviderProperties config = properties.getProviderConfig(providerName);

        if (!config.isEnabled()) {
            callback.onError(error(providerName, ModelGatewayErrorType.CONFIG_ERROR,
                    providerName + " provider is not enabled", 0L));
            return;
        }

        if (config.getApiKey() == null || config.getApiKey().isBlank()) {
            callback.onError(error(providerName, ModelGatewayErrorType.CONFIG_ERROR,
                    providerName + " API key is not configured", 0L));
            return;
        }

        if (config.getBaseUrl() == null || config.getBaseUrl().isBlank()) {
            callback.onError(error(providerName, ModelGatewayErrorType.CONFIG_ERROR,
                    providerName + " baseUrl is not configured", 0L));
            return;
        }

        String modelName = request.getModelName() != null ? request.getModelName() : config.getModelName();

        OpenAiChatCompletionRequest body = buildRequestBody(request, modelName);
        body.setStream(true);

        String apiKeyMasked = config.maskApiKey();
        log.info("Streaming {} model={} baseUrl={} apiKey={}", providerName, modelName, config.getBaseUrl(), apiKeyMasked);

        RestClient restClient = buildRestClient(config);

        try {
            StringBuilder fullContent = new StringBuilder();
            long[] promptTokensHolder = {0L};
            long[] completionTokensHolder = {0L};

            restClient.post()
                    .uri("/chat/completions")
                    .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                    .header("Authorization", "Bearer " + config.getApiKey())
                    .accept(MediaType.TEXT_EVENT_STREAM)
                    .body(Objects.requireNonNull(body))
                    .exchange((req, res) -> {
                        if (res.getStatusCode().is4xxClientError() || res.getStatusCode().is5xxServerError()) {
                            byte[] bytes = res.getBody() != null ? res.getBody().readAllBytes() : new byte[0];
                            String bodyStr = new String(bytes);
                            log.error("{} stream HTTP {} from {}: {}", providerName, res.getStatusCode().value(), config.getBaseUrl(), bodyStr);
                            OpenAiHttpException ex = OpenAiHttpException.fromStatus(res.getStatusCode().value(), bodyStr);
                            long latency = System.currentTimeMillis() - startTime;
                            callback.onError(error(providerName, ex.getErrorType(), ex.getMessage(), latency));
                            return "error";
                        }

                        try (java.io.BufferedReader reader = new java.io.BufferedReader(
                                new java.io.InputStreamReader(res.getBody(), java.nio.charset.StandardCharsets.UTF_8))) {
                            String line;
                            while ((line = reader.readLine()) != null) {
                                if (line.isBlank()) continue;
                                if (!line.startsWith("data:")) continue;

                                String data = line.substring(5).strip();
                                if ("[DONE]".equals(data)) break;

                                try {
                                    com.fasterxml.jackson.databind.ObjectMapper mapper =
                                            new com.fasterxml.jackson.databind.ObjectMapper();
                                    com.fasterxml.jackson.databind.JsonNode node = mapper.readTree(data);
                                    com.fasterxml.jackson.databind.JsonNode choices = node.get("choices");
                                    if (choices != null && choices.isArray() && !choices.isEmpty()) {
                                        com.fasterxml.jackson.databind.JsonNode delta = choices.get(0).get("delta");
                                        if (delta != null) {
                                            com.fasterxml.jackson.databind.JsonNode contentNode = delta.get("content");
                                            if (contentNode != null && !contentNode.isNull()) {
                                                String token = contentNode.asText();
                                                if (!token.isEmpty()) {
                                                    fullContent.append(token);
                                                    ModelStreamChunk chunk = new ModelStreamChunk();
                                                    chunk.setContent(token);
                                                    chunk.setDone(false);
                                                    chunk.setProvider(providerName);
                                                    chunk.setModelName(modelName);
                                                    callback.onToken(chunk);
                                                }
                                            }
                                        }

                                        // Check for usage in final chunk
                                        com.fasterxml.jackson.databind.JsonNode usageNode = node.get("usage");
                                        if (usageNode != null) {
                                            promptTokensHolder[0] = usageNode.has("prompt_tokens") ? usageNode.get("prompt_tokens").asLong() : 0L;
                                            completionTokensHolder[0] = usageNode.has("completion_tokens") ? usageNode.get("completion_tokens").asLong() : 0L;
                                        }
                                    }
                                } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                                    log.debug("Failed to parse SSE data line: {}", data, e);
                                }
                            }
                        }

                        long latencyMs = System.currentTimeMillis() - startTime;
                        ModelResponse response = new ModelResponse();
                        response.setContent(fullContent.toString());
                        response.setSuccess(true);
                        response.setProvider(providerName);
                        response.setModelName(modelName);
                        response.setFallbackUsed(false);
                        long completionTokens = completionTokensHolder[0] > 0L
                                ? completionTokensHolder[0] : fullContent.length() / 3;
                        response.setPromptTokens(promptTokensHolder[0]);
                        response.setCompletionTokens(completionTokens);
                        response.setTotalTokens(promptTokensHolder[0] + completionTokens);
                        response.setLatencyMs(latencyMs);

                        log.info("{} stream success: model={} totalChars={} latencyMs={}",
                                providerName, modelName, fullContent.length(), latencyMs);
                        callback.onComplete(response);
                        return "ok";
                    });
        } catch (Exception e) {
            long latencyMs = System.currentTimeMillis() - startTime;
            log.error("{} stream error: {}", providerName, e.getMessage(), e);
            callback.onError(error(providerName, ModelGatewayErrorType.PROVIDER_ERROR,
                    "Stream error: " + e.getMessage(), latencyMs));
        }
    }

    private RestClient buildRestClient(ModelGatewayProperties.ProviderProperties config) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        int timeout = (int) Math.min(properties.getTimeoutMs(), Integer.MAX_VALUE);
        factory.setConnectTimeout(timeout);
        factory.setReadTimeout(timeout);
        String baseUrl = config.getBaseUrl();
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException("OpenAI-compatible provider baseUrl must be configured");
        }
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .build();
    }

    private OpenAiChatCompletionRequest buildRequestBody(ModelRequest request, String modelName) {
        OpenAiChatCompletionRequest body = new OpenAiChatCompletionRequest();
        body.setModel(modelName);
        body.setStream(false);

        List<OpenAiChatMessage> messages = new ArrayList<>();

        StringBuilder systemContent = new StringBuilder();
        if (request.getSystemPrompt() != null && !request.getSystemPrompt().isBlank()) {
            systemContent.append(request.getSystemPrompt());
        }
        if (request.getContext() != null && !request.getContext().isBlank()) {
            if (systemContent.length() > 0) {
                systemContent.append("\n\n");
            }
            systemContent.append("Context:\n").append(request.getContext());
        }
        if (systemContent.length() > 0) {
            messages.add(new OpenAiChatMessage("system", systemContent.toString()));
        }

        String userContent = request.getUserPrompt() != null ? request.getUserPrompt() : "";
        messages.add(new OpenAiChatMessage("user", userContent));

        body.setMessages(messages);

        if (request.getTemperature() != null) {
            body.setTemperature(request.getTemperature());
        } else {
            body.setTemperature(new BigDecimal("0.2"));
        }
        if (request.getMaxTokens() != null) {
            body.setMaxTokens(request.getMaxTokens());
        } else {
            body.setMaxTokens(2048);
        }

        return body;
    }

    private String extractContent(OpenAiChatCompletionResponse apiResponse) {
        if (apiResponse.getChoices() != null && !apiResponse.getChoices().isEmpty()) {
            OpenAiChatCompletionResponse.Choice choice = apiResponse.getChoices().get(0);
            if (choice.getMessage() != null && choice.getMessage().getContent() != null) {
                return choice.getMessage().getContent();
            }
        }
        return "";
    }

    private ModelResponse error(String providerName, ModelGatewayErrorType errorType,
                                 String message, long latencyMs) {
        ModelResponse response = new ModelResponse();
        response.setSuccess(false);
        response.setContent("");
        response.setPromptTokens(0L);
        response.setCompletionTokens(0L);
        response.setTotalTokens(0L);
        response.setLatencyMs(latencyMs);
        response.setProvider(providerName);
        response.setErrorType(errorType.name());
        response.setErrorMessage(message);
        return response;
    }

    private static class OpenAiHttpException extends RuntimeException {
        private final int statusCode;
        private final ModelGatewayErrorType errorType;

        OpenAiHttpException(int statusCode, ModelGatewayErrorType errorType, String message) {
            super(message);
            this.statusCode = statusCode;
            this.errorType = errorType;
        }

        int getStatusCode() { return statusCode; }
        ModelGatewayErrorType getErrorType() { return errorType; }

        static OpenAiHttpException fromStatus(int status, String body) {
            ModelGatewayErrorType type = switch (status) {
                case 401, 403 -> ModelGatewayErrorType.AUTH_ERROR;
                case 429 -> ModelGatewayErrorType.RATE_LIMIT;
                case 408 -> ModelGatewayErrorType.TIMEOUT;
                default -> ModelGatewayErrorType.PROVIDER_ERROR;
            };
            return new OpenAiHttpException(status, type, "HTTP " + status + ": " + body);
        }
    }
}
