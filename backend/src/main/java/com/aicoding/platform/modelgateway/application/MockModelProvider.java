package com.aicoding.platform.modelgateway.application;

import com.aicoding.platform.modelgateway.dto.ModelRequest;
import com.aicoding.platform.modelgateway.dto.ModelResponse;
import com.aicoding.platform.modelgateway.dto.ModelStreamChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class MockModelProvider implements ModelProvider {

    private static final Logger log = LoggerFactory.getLogger(MockModelProvider.class);

    private static final Map<String, String> MOCK_RESPONSES = Map.of(
            "TASK_EXECUTION", "任务已由 Mock Agent 执行完成。当前阶段未调用真实大模型，也未修改代码。系统已完成任务分析、执行日志记录与产物生成。",
            "CHAT", "我是 Mock Agent，已收到你的消息。当前阶段为 Mock 流式回复，真实模型流式输出将在模型网关 Provider 接入后实现。",
            "CODE_REVIEW", "Mock Review 完成：当前未发现阻塞性问题。后续接入真实模型后将输出详细代码审查意见。",
            "SUMMARY", "Mock Summary 完成：当前内容已被概括，后续将接入真实摘要模型。",
            "MOCK", "Mock Model Gateway 已处理请求。"
    );

    @Override
    public String providerType() {
        return "MOCK";
    }

    @Override
    public boolean supports(String provider) {
        return provider == null || provider.isBlank() || providerType().equalsIgnoreCase(provider);
    }

    @Override
    public boolean supportsStream() {
        return true;
    }

    @Override
    public ModelResponse generate(ModelRequest request) {
        String requestType = request.getRequestType() != null ? request.getRequestType() : "MOCK";
        String content = MOCK_RESPONSES.getOrDefault(requestType, MOCK_RESPONSES.get("MOCK"));

        if (request.getContext() != null && !request.getContext().isBlank()) {
            content = "本次 Mock 执行已接收项目知识库上下文。\n\n" + content;
        }

        ModelResponse response = new ModelResponse();
        response.setContent(content);
        response.setPromptTokens(15L);
        response.setCompletionTokens((long) (content.length() / 3));
        response.setTotalTokens(response.getPromptTokens() + response.getCompletionTokens());
        response.setLatencyMs(200L);
        response.setSuccess(true);
        response.setProvider(providerType());
        response.setModelName("mock-agent-model");
        response.setFallbackUsed(false);
        return response;
    }

    @Override
    public void stream(ModelRequest request, ModelStreamCallback callback) {
        String requestType = request.getRequestType() != null ? request.getRequestType() : "MOCK";
        String content = MOCK_RESPONSES.getOrDefault(requestType, MOCK_RESPONSES.get("MOCK"));

        if (request.getContext() != null && !request.getContext().isBlank()) {
            content = "本次 Mock 执行已接收项目知识库上下文。\n\n" + content;
        }

        log.debug("MockModelProvider streaming {} chars for requestType={}", content.length(), requestType);

        List<String> tokens = tokenize(content);
        long startTime = System.currentTimeMillis();

        try {
            for (String token : tokens) {
                ModelStreamChunk chunk = new ModelStreamChunk();
                chunk.setContent(token);
                chunk.setDone(false);
                chunk.setProvider("MOCK");
                chunk.setModelName("mock-agent-model");

                try {
                    callback.onToken(chunk);
                } catch (Exception e) {
                    log.warn("Callback onToken failed, stopping mock stream", e);
                    return;
                }

                try {
                    pauseBetweenTokens();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }

            long latencyMs = System.currentTimeMillis() - startTime;
            ModelResponse response = new ModelResponse();
            response.setContent(content);
            response.setSuccess(true);
            response.setProvider("MOCK");
            response.setModelName("mock-agent-model");
            response.setFallbackUsed(false);
            response.setPromptTokens(15L);
            response.setCompletionTokens((long) (content.length() / 3));
            response.setTotalTokens(response.getPromptTokens() + response.getCompletionTokens());
            response.setLatencyMs(latencyMs);

            callback.onComplete(response);
        } catch (Exception e) {
            log.error("Mock stream error", e);
            ModelResponse errorResponse = new ModelResponse();
            errorResponse.setSuccess(false);
            errorResponse.setProvider("MOCK");
            errorResponse.setModelName("mock-agent-model");
            errorResponse.setErrorMessage("Mock stream error: " + e.getMessage());
            callback.onError(errorResponse);
        }
    }

    private List<String> tokenize(String text) {
        List<String> tokens = new ArrayList<>();
        for (int i = 0; i < text.length(); ) {
            int len = Math.min(3, text.length() - i);
            if (len == 3 && Character.UnicodeScript.of(text.charAt(i)) == Character.UnicodeScript.HAN) {
                len = 2;
            }
            tokens.add(text.substring(i, i + len));
            i += len;
        }
        return tokens;
    }

    private void pauseBetweenTokens() throws InterruptedException {
        Thread.sleep(150);
    }
}
