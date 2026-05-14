package com.aicoding.platform.modelgateway.application;

import com.aicoding.platform.modelgateway.dto.ModelRequest;
import com.aicoding.platform.modelgateway.dto.ModelResponse;

import java.util.Map;

/**
 * @deprecated Replaced by {@link MockModelProvider}
 * and {@link DefaultModelGateway}. Kept for backward compatibility during transition.
 */
@Deprecated
public class MockModelGateway {

    private static final Map<String, String> MOCK_RESPONSES = Map.of(
            "TASK_EXECUTION", "任务已由 Mock Agent 执行完成。当前阶段未调用真实大模型，也未修改代码。系统已完成任务分析、执行日志记录与产物生成。",
            "CHAT", "我是 Mock Agent，已收到你的消息。真实模型网关将在后续里程碑接入。",
            "CODE_REVIEW", "Mock Review 完成：当前未发现阻塞性问题。后续接入真实模型后将输出详细代码审查意见。",
            "SUMMARY", "Mock Summary 完成：当前内容已被概括，后续将接入真实摘要模型。",
            "MOCK", "Mock Model Gateway 已处理请求。"
    );

    @Deprecated
    public ModelResponse generate(ModelRequest request) {
        String requestType = request.getRequestType() != null ? request.getRequestType() : "MOCK";
        String content = MOCK_RESPONSES.getOrDefault(requestType, MOCK_RESPONSES.get("MOCK"));

        // Context-aware enhancement
        if (request.getContext() != null && !request.getContext().isBlank()) {
            content = "本次 Mock 执行已接收项目知识库上下文。\n\n" + content;
        }

        // Simulate latency
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        ModelResponse response = new ModelResponse();
        response.setContent(content);
        response.setPromptTokens(15L);
        response.setCompletionTokens((long) (content.length() / 3));
        response.setTotalTokens(response.getPromptTokens() + response.getCompletionTokens());
        response.setLatencyMs(200L);
        response.setSuccess(true);
        return response;
    }
}
