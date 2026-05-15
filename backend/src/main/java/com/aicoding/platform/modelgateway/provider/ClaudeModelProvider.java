package com.aicoding.platform.modelgateway.provider;

import com.aicoding.platform.modelgateway.application.ModelProvider;
import com.aicoding.platform.modelgateway.application.ModelStreamCallback;
import com.aicoding.platform.modelgateway.config.ModelGatewayProperties;
import com.aicoding.platform.modelgateway.domain.ModelGatewayErrorType;
import com.aicoding.platform.modelgateway.dto.ModelRequest;
import com.aicoding.platform.modelgateway.dto.ModelResponse;
import com.aicoding.platform.modelgateway.dto.ModelStreamChunk;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public class ClaudeModelProvider implements ModelProvider {

    private static final Logger log = LoggerFactory.getLogger(ClaudeModelProvider.class);
    private static final @NonNull MediaType APPLICATION_JSON = Objects.requireNonNull(MediaType.APPLICATION_JSON);
    private static final @NonNull MediaType TEXT_EVENT_STREAM = Objects.requireNonNull(MediaType.TEXT_EVENT_STREAM);

    private final ModelGatewayProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ClaudeModelProvider(ModelGatewayProperties properties) {
        this.properties = properties;
    }

    @Override
    public String providerType() {
        return "CLAUDE";
    }

    @Override
    public boolean supportsStream() {
        return true;
    }

    @Override
    public boolean supports(String provider) {
        return "CLAUDE".equalsIgnoreCase(provider);
    }

    @Override
    public ModelResponse generate(ModelRequest request) {
        long startTime = System.currentTimeMillis();
        ModelGatewayProperties.ProviderProperties config = properties.getProviderConfig("claude");

        if (!config.isEnabled()) {
            return error("CLAUDE", ModelGatewayErrorType.CONFIG_ERROR, "Claude provider is not enabled", 0L);
        }
        if (config.getApiKey() == null || config.getApiKey().isBlank()) {
            return error("CLAUDE", ModelGatewayErrorType.CONFIG_ERROR, "Claude API key is not configured", 0L);
        }
        if (config.getBaseUrl() == null || config.getBaseUrl().isBlank()) {
            return error("CLAUDE", ModelGatewayErrorType.CONFIG_ERROR, "Claude baseUrl is not configured", 0L);
        }

        String modelName = resolveModelName(request, config);

        RestClient restClient = buildRestClient(config);
        Map<String, Object> body = buildRequestBody(request, modelName);

        String apiKeyMasked = config.maskApiKey();
        log.info("Calling Claude model={} baseUrl={} apiKey={}", modelName, config.getBaseUrl(), apiKeyMasked);

        try {
            String responseJson = restClient.post()
                    .uri("/v1/messages")
                    .contentType(APPLICATION_JSON)
                    .header("x-api-key", config.getApiKey())
                    .header("anthropic-version", "2023-06-01")
                    .body(Objects.requireNonNull(body))
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                        byte[] bytes = res.getBody() != null ? res.getBody().readAllBytes() : new byte[0];
                        String bodyStr = new String(bytes);
                        log.error("Claude HTTP {} from {}: {}", res.getStatusCode().value(), config.getBaseUrl(), bodyStr);
                        throw ClaudeHttpException.fromStatus(res.getStatusCode().value(), bodyStr);
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                        byte[] bytes = res.getBody() != null ? res.getBody().readAllBytes() : new byte[0];
                        String bodyStr = new String(bytes);
                        log.error("Claude HTTP {} from {}: {}", res.getStatusCode().value(), config.getBaseUrl(), bodyStr);
                        throw ClaudeHttpException.fromStatus(res.getStatusCode().value(), bodyStr);
                    })
                    .body(String.class);

            long latencyMs = System.currentTimeMillis() - startTime;

            JsonNode root = objectMapper.readTree(responseJson);
            String content = extractContent(root);
            JsonNode usageNode = root.get("usage");

            ModelResponse response = new ModelResponse();
            response.setContent(content);
            response.setSuccess(true);
            response.setProvider("CLAUDE");
            response.setModelName(modelName);
            response.setFallbackUsed(false);
            response.setLatencyMs(latencyMs);

            if (usageNode != null) {
                response.setPromptTokens(usageNode.has("input_tokens") ? usageNode.get("input_tokens").asLong() : 0L);
                response.setCompletionTokens(usageNode.has("output_tokens") ? usageNode.get("output_tokens").asLong() : 0L);
                response.setTotalTokens(response.getPromptTokens() + response.getCompletionTokens());
            } else {
                response.setPromptTokens(0L);
                response.setCompletionTokens((long) (content.length() / 3));
                response.setTotalTokens(response.getPromptTokens() + response.getCompletionTokens());
            }

            log.info("Claude call success: model={} totalTokens={} latencyMs={}",
                    modelName, response.getTotalTokens(), latencyMs);
            return response;

        } catch (ClaudeHttpException e) {
            long latencyMs = System.currentTimeMillis() - startTime;
            log.error("Claude HTTP error: status={} message={}", e.getStatusCode(), e.getMessage());
            return error("CLAUDE", e.getErrorType(), e.getMessage(), latencyMs);
        } catch (ResourceAccessException e) {
            long latencyMs = System.currentTimeMillis() - startTime;
            if (e.getCause() instanceof SocketTimeoutException) {
                return error("CLAUDE", ModelGatewayErrorType.TIMEOUT,
                        "Request timed out after " + properties.getTimeoutMs() + "ms", latencyMs);
            }
            return error("CLAUDE", ModelGatewayErrorType.NETWORK_ERROR,
                    "Network error: " + e.getMessage(), latencyMs);
        } catch (JsonProcessingException e) {
            long latencyMs = System.currentTimeMillis() - startTime;
            log.error("Claude response parse error: {}", e.getMessage(), e);
            return error("CLAUDE", ModelGatewayErrorType.PROVIDER_ERROR,
                    "Response parse error: " + e.getMessage(), latencyMs);
        } catch (RestClientException e) {
            long latencyMs = System.currentTimeMillis() - startTime;
            log.error("Claude client error: {}", e.getMessage(), e);
            return error("CLAUDE", ModelGatewayErrorType.NETWORK_ERROR,
                    "Client error: " + e.getMessage(), latencyMs);
        }
    }

    @Override
    public void stream(ModelRequest request, ModelStreamCallback callback) {
        long startTime = System.currentTimeMillis();
        ModelGatewayProperties.ProviderProperties config = properties.getProviderConfig("claude");

        if (!config.isEnabled()) {
            callback.onError(error("CLAUDE", ModelGatewayErrorType.CONFIG_ERROR,
                    "Claude provider is not enabled", 0L));
            return;
        }
        if (config.getApiKey() == null || config.getApiKey().isBlank()) {
            callback.onError(error("CLAUDE", ModelGatewayErrorType.CONFIG_ERROR,
                    "Claude API key is not configured", 0L));
            return;
        }
        if (config.getBaseUrl() == null || config.getBaseUrl().isBlank()) {
            callback.onError(error("CLAUDE", ModelGatewayErrorType.CONFIG_ERROR,
                    "Claude baseUrl is not configured", 0L));
            return;
        }

        String modelName = resolveModelName(request, config);

        Map<String, Object> body = buildRequestBody(request, modelName);
        body.put("stream", true);

        String apiKeyMasked = config.maskApiKey();
        log.info("Streaming Claude model={} baseUrl={} apiKey={}", modelName, config.getBaseUrl(), apiKeyMasked);

        RestClient restClient = buildRestClient(config);

        try {
            StringBuilder fullContent = new StringBuilder();
            long[] inputTokensHolder = {0L};
            long[] outputTokensHolder = {0L};

            restClient.post()
                    .uri("/v1/messages")
                    .contentType(APPLICATION_JSON)
                    .header("x-api-key", config.getApiKey())
                    .header("anthropic-version", "2023-06-01")
                    .accept(TEXT_EVENT_STREAM)
                    .body(Objects.requireNonNull(body))
                    .exchange((req, res) -> {
                        if (res.getStatusCode().is4xxClientError() || res.getStatusCode().is5xxServerError()) {
                            byte[] bytes = res.getBody() != null ? res.getBody().readAllBytes() : new byte[0];
                            String bodyStr = new String(bytes);
                            log.error("Claude stream HTTP {} from {}: {}", res.getStatusCode().value(), config.getBaseUrl(), bodyStr);
                            ClaudeHttpException ex = ClaudeHttpException.fromStatus(res.getStatusCode().value(), bodyStr);
                            callback.onError(error("CLAUDE", ex.getErrorType(), ex.getMessage(),
                                    System.currentTimeMillis() - startTime));
                            return "error";
                        }

                        try (BufferedReader reader = new BufferedReader(
                                new InputStreamReader(res.getBody(), StandardCharsets.UTF_8))) {
                            String line;
                            while ((line = reader.readLine()) != null) {
                                if (line.isBlank()) continue;
                                if (!line.startsWith("data:")) continue;

                                String data = line.substring(5).strip();
                                if ("[DONE]".equals(data)) break;

                                try {
                                    JsonNode node = objectMapper.readTree(data);
                                    String eventType = node.has("type") ? node.get("type").asText() : "";

                                    switch (eventType) {
                                        case "content_block_delta" -> {
                                            JsonNode delta = node.get("delta");
                                            if (delta != null && delta.has("text")) {
                                                String text = delta.get("text").asText();
                                                if (!text.isEmpty()) {
                                                    fullContent.append(text);
                                                    ModelStreamChunk chunk = new ModelStreamChunk();
                                                    chunk.setContent(text);
                                                    chunk.setDone(false);
                                                    chunk.setProvider("CLAUDE");
                                                    chunk.setModelName(modelName);
                                                    callback.onToken(chunk);
                                                }
                                            }
                                        }
                                        case "message_start" -> {
                                            JsonNode message = node.get("message");
                                            if (message != null && message.has("usage")) {
                                                JsonNode usage = message.get("usage");
                                                inputTokensHolder[0] = usage.has("input_tokens") ? usage.get("input_tokens").asLong() : 0L;
                                            }
                                        }
                                        case "message_delta" -> {
                                            JsonNode usage = node.get("usage");
                                            if (usage != null && usage.has("output_tokens")) {
                                                outputTokensHolder[0] = usage.get("output_tokens").asLong();
                                            }
                                        }
                                    }
                                } catch (JsonProcessingException e) {
                                    log.debug("Failed to parse Claude SSE data line: {}", data, e);
                                }
                            }
                        }

                        long latencyMs = System.currentTimeMillis() - startTime;
                        ModelResponse response = new ModelResponse();
                        response.setContent(fullContent.toString());
                        response.setSuccess(true);
                        response.setProvider("CLAUDE");
                        response.setModelName(modelName);
                        response.setFallbackUsed(false);
                        long promptTokens = inputTokensHolder[0];
                        long completionTokens = outputTokensHolder[0] > 0 ? outputTokensHolder[0] : fullContent.length() / 3;
                        response.setPromptTokens(promptTokens);
                        response.setCompletionTokens(completionTokens);
                        response.setTotalTokens(promptTokens + completionTokens);
                        response.setLatencyMs(latencyMs);

                        log.info("Claude stream success: model={} totalChars={} latencyMs={}",
                                modelName, fullContent.length(), latencyMs);
                        callback.onComplete(response);
                        return "ok";
                    });
        } catch (RestClientException e) {
            long latencyMs = System.currentTimeMillis() - startTime;
            log.error("Claude stream error: {}", e.getMessage(), e);
            callback.onError(error("CLAUDE", ModelGatewayErrorType.PROVIDER_ERROR,
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
            baseUrl = "https://api.anthropic.com";
        }
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .build();
    }

    private Map<String, Object> buildRequestBody(ModelRequest request, String modelName) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", modelName);

        Integer requestedMaxTokens = request.getMaxTokens();
        int maxTokens = requestedMaxTokens != null ? requestedMaxTokens : 2048;
        body.put("max_tokens", maxTokens);

        List<Map<String, Object>> messages = new ArrayList<>();

        StringBuilder systemContent = new StringBuilder();
        if (request.getSystemPrompt() != null && !request.getSystemPrompt().isBlank()) {
            systemContent.append(request.getSystemPrompt());
        }
        if (request.getContext() != null && !request.getContext().isBlank()) {
            if (systemContent.length() > 0) systemContent.append("\n\n");
            systemContent.append("Context:\n").append(request.getContext());
        }
        if (systemContent.length() > 0) {
            body.put("system", systemContent.toString());
        }

        String userContent = request.getUserPrompt() != null ? request.getUserPrompt() : "";
        messages.add(Map.of("role", "user", "content", userContent));
        body.put("messages", messages);

        return body;
    }

    private String resolveModelName(ModelRequest request, ModelGatewayProperties.ProviderProperties config) {
        String name = request.getModelName() != null ? request.getModelName() : config.getModelName();
        return (name != null && !name.isBlank()) ? name : "claude-3-5-sonnet-latest";
    }

    private String extractContent(JsonNode root) {
        JsonNode content = root.get("content");
        if (content != null && content.isArray()) {
            StringBuilder sb = new StringBuilder();
            for (JsonNode block : content) {
                if (block.has("text")) {
                    sb.append(block.get("text").asText());
                }
            }
            return sb.toString();
        }
        return "";
    }

    private ModelResponse error(String provider, ModelGatewayErrorType errorType,
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

    private static class ClaudeHttpException extends RuntimeException {
        private final int statusCode;
        private final ModelGatewayErrorType errorType;

        ClaudeHttpException(int statusCode, ModelGatewayErrorType errorType, String message) {
            super(message);
            this.statusCode = statusCode;
            this.errorType = errorType;
        }

        int getStatusCode() { return statusCode; }
        ModelGatewayErrorType getErrorType() { return errorType; }

        static ClaudeHttpException fromStatus(int status, String body) {
            ModelGatewayErrorType type = switch (status) {
                case 401, 403 -> ModelGatewayErrorType.AUTH_ERROR;
                case 429 -> ModelGatewayErrorType.RATE_LIMIT;
                case 408 -> ModelGatewayErrorType.TIMEOUT;
                default -> ModelGatewayErrorType.PROVIDER_ERROR;
            };
            return new ClaudeHttpException(status, type, "HTTP " + status + ": " + body);
        }
    }
}
