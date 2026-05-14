package com.aicoding.platform.chat.controller;

import com.aicoding.platform.chat.application.ChatApplicationService;
import com.aicoding.platform.chat.application.ChatStreamService;
import com.aicoding.platform.chat.dto.ChatMessageResponse;
import com.aicoding.platform.chat.dto.ChatSessionResponse;
import com.aicoding.platform.chat.dto.CreateChatSessionRequest;
import com.aicoding.platform.chat.dto.SendChatMessageRequest;
import com.aicoding.platform.chat.dto.SendChatMessageResponse;
import com.aicoding.platform.common.pagination.PageQuery;
import com.aicoding.platform.common.pagination.PageResult;
import com.aicoding.platform.common.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
public class ChatController {

    private final ChatApplicationService chatApplicationService;
    private final ChatStreamService chatStreamService;

    public ChatController(ChatApplicationService chatApplicationService,
                           ChatStreamService chatStreamService) {
        this.chatApplicationService = chatApplicationService;
        this.chatStreamService = chatStreamService;
    }

    @PostMapping("/api/projects/{projectId}/chat/sessions")
    public ApiResponse<ChatSessionResponse> createSession(@PathVariable Long projectId,
                                                           @Valid @RequestBody CreateChatSessionRequest request) {
        return ApiResponse.ok(chatApplicationService.createSession(projectId, request));
    }

    @GetMapping("/api/projects/{projectId}/chat/sessions")
    public ApiResponse<PageResult<ChatSessionResponse>> listSessions(
            @PathVariable Long projectId,
            @Valid PageQuery pageQuery) {
        return ApiResponse.ok(chatApplicationService.listSessions(projectId, pageQuery));
    }

    @GetMapping("/api/chat/sessions/{sessionId}/messages")
    public ApiResponse<List<ChatMessageResponse>> getMessages(
            @PathVariable Long sessionId,
            @RequestParam(required = false) Integer limit) {
        return ApiResponse.ok(chatApplicationService.getMessages(sessionId, limit));
    }

    @PostMapping("/api/chat/sessions/{sessionId}/messages")
    public ApiResponse<SendChatMessageResponse> sendMessage(@PathVariable Long sessionId,
                                                             @Valid @RequestBody SendChatMessageRequest request) {
        return ApiResponse.ok(chatApplicationService.sendMessage(sessionId, request));
    }

    @GetMapping("/api/chat/sessions/{sessionId}/stream")
    public SseEmitter streamMessage(@PathVariable Long sessionId,
                                     @RequestParam Long messageId) {
        return chatStreamService.streamMessage(sessionId, messageId);
    }
}
