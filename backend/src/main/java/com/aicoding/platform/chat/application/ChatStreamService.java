package com.aicoding.platform.chat.application;

import com.aicoding.platform.chat.domain.ChatMessageEntity;
import com.aicoding.platform.chat.domain.ChatMessageStatus;
import com.aicoding.platform.chat.dto.ChatMessageReferenceResponse;
import com.aicoding.platform.chat.dto.ChatMessageResponse;
import com.aicoding.platform.chat.dto.ChatStreamEvent;
import com.aicoding.platform.common.exception.BizException;
import com.aicoding.platform.common.exception.ErrorCode;
import com.aicoding.platform.member.application.ProjectPermissionService;
import com.aicoding.platform.member.domain.ProjectRole;
import com.aicoding.platform.modelgateway.application.ModelGateway;
import com.aicoding.platform.modelgateway.application.ModelRequestLogService;
import com.aicoding.platform.modelgateway.application.ModelStreamCallback;
import com.aicoding.platform.modelgateway.dto.ModelRequest;
import com.aicoding.platform.modelgateway.dto.ModelResponse;
import com.aicoding.platform.modelgateway.dto.ModelStreamChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Service
public class ChatStreamService {

    private static final Logger log = LoggerFactory.getLogger(ChatStreamService.class);

    private final ChatApplicationService chatApplicationService;
    private final ProjectPermissionService projectPermissionService;
    private final ModelGateway modelGateway;
    private final ModelRequestLogService modelRequestLogService;

    private static final String SYSTEM_PROMPT = "你是 AI Coding Platform 的智能助手。请根据用户的提问提供有帮助的回答。";

    public ChatStreamService(ChatApplicationService chatApplicationService,
                              ProjectPermissionService projectPermissionService,
                              ModelGateway modelGateway,
                              ModelRequestLogService modelRequestLogService) {
        this.chatApplicationService = chatApplicationService;
        this.projectPermissionService = projectPermissionService;
        this.modelGateway = modelGateway;
        this.modelRequestLogService = modelRequestLogService;
    }

    public SseEmitter streamMessage(Long sessionId, Long messageId) {
        var session = chatApplicationService.getSessionOrThrow(sessionId);
        projectPermissionService.checkProjectRole(session.getProjectId(), ProjectRole.OWNER, ProjectRole.MAINTAINER,
                ProjectRole.DEVELOPER, ProjectRole.VIEWER);

        ChatMessageEntity message = chatApplicationService.getMessageOrThrow(messageId);
        if (!message.getSessionId().equals(sessionId)) {
            throw new BizException(ErrorCode.NOT_FOUND, "消息不属于该会话");
        }

        SseEmitter emitter = new SseEmitter(60_000L);

        List<ChatMessageReferenceResponse> loadedReferences = chatApplicationService.getMessageReferences(messageId);
        final List<ChatMessageReferenceResponse> references = loadedReferences != null
                ? loadedReferences : Collections.emptyList();
        boolean ragUsed = !references.isEmpty();

        if (!ChatMessageStatus.STREAMING.name().equals(message.getStatus())) {
            completeEmitter(emitter, messageId.toString(), message.getStatus(), message.getTokenUsage(),
                    ragUsed, references);
            return emitter;
        }

        // Get the user message content as the prompt
        String userPrompt = getPreviousUserMessage(sessionId, messageId);

        // Build ModelRequest
        ModelRequest modelRequest = new ModelRequest();
        modelRequest.setRequestType("CHAT");
        modelRequest.setSystemPrompt(SYSTEM_PROMPT);
        modelRequest.setUserPrompt(userPrompt);
        modelRequest.setFallbackEnabled(true);

        // Add RAG context if available
        if (ragUsed && !references.isEmpty()) {
            StringBuilder ctx = new StringBuilder();
            for (int i = 0; i < references.size(); i++) {
                ChatMessageReferenceResponse ref = references.get(i);
                ctx.append("[Reference ").append(i + 1).append("] ");
                if (ref.getTitle() != null) ctx.append(ref.getTitle());
                if (ref.getSnippet() != null) ctx.append("\n").append(ref.getSnippet());
                ctx.append("\n");
            }
            modelRequest.setContext(ctx.toString());
        }

        // Prepare tracking state
        StringBuilder fullContent = new StringBuilder();
        final Long[] tokenUsage = {0L};
        final Boolean[] isComplete = {false};

        emitter.onTimeout(() -> {
            log.info("SSE timeout for messageId={}, partial content length={}", messageId, fullContent.length());
            if (fullContent.length() > 0 && !isComplete[0]) {
                chatApplicationService.updateMessageContent(messageId, fullContent.toString(),
                        ChatMessageStatus.COMPLETED.name(), tokenUsage[0]);
            }
        });

        emitter.onError(throwable -> {
            log.warn("SSE emitter error for messageId={}", messageId, throwable);
            if (!isComplete[0]) {
                chatApplicationService.updateMessageContent(messageId, fullContent.toString(),
                        ChatMessageStatus.FAILED.name(), tokenUsage[0]);
            }
        });

        // Call model gateway stream
        modelGateway.stream(modelRequest, new ModelStreamCallback() {
            @Override
            public void onToken(ModelStreamChunk chunk) {
                fullContent.append(chunk.getContent() != null ? chunk.getContent() : "");

                ChatStreamEvent tokenEvent = new ChatStreamEvent();
                tokenEvent.setMessageId(messageId.toString());
                tokenEvent.setContent(chunk.getContent());

                try {
                    emitter.send(SseEmitter.event()
                            .name("token")
                            .data(tokenEvent));
                } catch (IOException e) {
                    log.warn("SSE send failed for messageId={}, stopping stream", messageId, e);
                    completeWithError(emitter, messageId.toString(), "SSE send failed");
                }
            }

            @Override
            public void onComplete(ModelResponse response) {
                isComplete[0] = true;
                tokenUsage[0] = defaultLong(response.getTotalTokens());

                // Update message in DB
                chatApplicationService.updateMessageContent(messageId, fullContent.toString(),
                        ChatMessageStatus.COMPLETED.name(), tokenUsage[0]);

                // Record model log
                try {
                    modelRequestLogService.record(session.getProjectId(), null, modelRequest, response);
                } catch (Exception e) {
                    log.warn("Failed to record model log for messageId={}", messageId, e);
                }

                // Send done event
                ChatStreamEvent doneEvent = new ChatStreamEvent();
                doneEvent.setMessageId(messageId.toString());
                doneEvent.setStatus(ChatMessageStatus.COMPLETED.name());
                doneEvent.setTokenUsage(tokenUsage[0]);
                doneEvent.setRagUsed(ragUsed);
                doneEvent.setReferences(references);

                try {
                    emitter.send(SseEmitter.event().name("done").data(doneEvent));
                    emitter.complete();
                } catch (IOException e) {
                    emitter.completeWithError(e);
                }
            }

            @Override
            public void onError(ModelResponse errorResponse) {
                isComplete[0] = true;

                // Update message as FAILED with partial content
                chatApplicationService.updateMessageContent(messageId, fullContent.toString(),
                        ChatMessageStatus.FAILED.name(), 0L);

                // Record model log
                try {
                    modelRequestLogService.record(session.getProjectId(), null, modelRequest, errorResponse);
                } catch (Exception e) {
                    log.warn("Failed to record model log for messageId={}", messageId, e);
                }

                // Send error event
                String code = resolveErrorCode(errorResponse.getErrorType());
                ChatStreamEvent errorEvent = new ChatStreamEvent();
                errorEvent.setMessageId(messageId.toString());
                errorEvent.setCode(code);
                errorEvent.setMessage(errorResponse.getErrorMessage());

                try {
                    emitter.send(SseEmitter.event().name("error").data(errorEvent));
                } catch (IOException ignored) {
                }
                emitter.complete();
            }
        });

        return emitter;
    }

    private String getPreviousUserMessage(Long sessionId, Long currentMessageId) {
        try {
            List<ChatMessageResponse> messages = chatApplicationService.getMessages(sessionId, 20);
            for (int i = messages.size() - 1; i >= 0; i--) {
                ChatMessageResponse msg = messages.get(i);
                if ("USER".equals(msg.getSenderType())) {
                    long msgId = Long.parseLong(msg.getId());
                    if (msgId < currentMessageId) {
                        return msg.getContent() != null ? msg.getContent() : "";
                    }
                }
            }
        } catch (BizException | NumberFormatException e) {
            log.warn("Failed to get previous user message for sessionId={}", sessionId, e);
        }
        return "";
    }

    private Long defaultLong(Long value) {
        if (value == null) {
            return 0L;
        }
        return value;
    }

    private String resolveErrorCode(String errorType) {
        if (errorType == null) return "INTERNAL_ERROR";
        return switch (errorType) {
            case "SAFETY_REJECTED" -> "SAFETY_REJECTED";
            case "AUTH_ERROR", "CONFIG_ERROR" -> "AI_PROVIDER_ERROR";
            case "TIMEOUT" -> "AI_PROVIDER_TIMEOUT";
            default -> "INTERNAL_ERROR";
        };
    }

    private void completeEmitter(SseEmitter emitter, String messageId, String status, Long tokenUsage,
                                  boolean ragUsed, List<ChatMessageReferenceResponse> references) {
        ChatStreamEvent doneEvent = new ChatStreamEvent();
        doneEvent.setMessageId(messageId);
        doneEvent.setStatus(status);
        doneEvent.setTokenUsage(tokenUsage != null ? tokenUsage : 0L);
        doneEvent.setRagUsed(ragUsed);
        doneEvent.setReferences(references);
        try {
            emitter.send(SseEmitter.event().name("done").data(doneEvent));
            emitter.complete();
        } catch (IOException e) {
            emitter.completeWithError(e);
        }
    }

    private void completeWithError(SseEmitter emitter, String messageId, String errorMsg) {
        ChatStreamEvent errorEvent = new ChatStreamEvent();
        errorEvent.setMessageId(messageId);
        errorEvent.setCode("INTERNAL_ERROR");
        errorEvent.setMessage(errorMsg);
        try {
            emitter.send(SseEmitter.event().name("error").data(errorEvent));
        } catch (IOException ignored) {
        }
        emitter.complete();
    }
}
