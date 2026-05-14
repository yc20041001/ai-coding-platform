package com.aicoding.platform.chat.application;

import com.aicoding.platform.agent.domain.AiAgentEntity;
import com.aicoding.platform.agent.infrastructure.AiAgentMapper;
import com.aicoding.platform.audit.application.AuditLogApplicationService;
import com.aicoding.platform.audit.domain.AuditActionType;
import com.aicoding.platform.auth.domain.UserEntity;
import com.aicoding.platform.auth.infrastructure.UserMapper;
import com.aicoding.platform.chat.domain.ChatMessageReferenceEntity;
import com.aicoding.platform.chat.domain.ChatMessageEntity;
import com.aicoding.platform.chat.domain.ChatMessageStatus;
import com.aicoding.platform.chat.domain.ChatMessageType;
import com.aicoding.platform.chat.domain.ChatSessionEntity;
import com.aicoding.platform.chat.domain.ChatSessionStatus;
import com.aicoding.platform.chat.domain.ChatSessionType;
import com.aicoding.platform.chat.domain.MessageSenderType;
import com.aicoding.platform.chat.domain.MessageReferenceType;
import com.aicoding.platform.chat.dto.ChatMessageReferenceResponse;
import com.aicoding.platform.chat.dto.ChatMessageResponse;
import com.aicoding.platform.chat.dto.ChatSessionResponse;
import com.aicoding.platform.chat.dto.CreateChatSessionRequest;
import com.aicoding.platform.chat.dto.SendChatMessageRequest;
import com.aicoding.platform.chat.dto.SendChatMessageResponse;
import com.aicoding.platform.chat.infrastructure.ChatMessageMapper;
import com.aicoding.platform.chat.infrastructure.ChatMessageReferenceMapper;
import com.aicoding.platform.chat.infrastructure.ChatSessionMapper;
import com.aicoding.platform.common.exception.BizException;
import com.aicoding.platform.common.exception.ErrorCode;
import com.aicoding.platform.common.pagination.PageQuery;
import com.aicoding.platform.common.pagination.PageResult;
import com.aicoding.platform.member.application.ProjectPermissionService;
import com.aicoding.platform.member.domain.ProjectRole;
import com.aicoding.platform.rag.application.RagContextService;
import com.aicoding.platform.rag.dto.RagContext;
import com.aicoding.platform.rag.dto.RagReference;
import com.aicoding.platform.security.context.LoginUser;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ChatApplicationService {

    private final ChatSessionMapper chatSessionMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final ChatMessageReferenceMapper chatMessageReferenceMapper;
    private final ProjectPermissionService projectPermissionService;
    private final UserMapper userMapper;
    private final AiAgentMapper aiAgentMapper;
    private final RagContextService ragContextService;
    private final AuditLogApplicationService auditLogApplicationService;

    public ChatApplicationService(ChatSessionMapper chatSessionMapper,
                                   ChatMessageMapper chatMessageMapper,
                                   ChatMessageReferenceMapper chatMessageReferenceMapper,
                                   ProjectPermissionService projectPermissionService,
                                   UserMapper userMapper,
                                   AiAgentMapper aiAgentMapper,
                                   RagContextService ragContextService,
                                   AuditLogApplicationService auditLogApplicationService) {
        this.chatSessionMapper = chatSessionMapper;
        this.chatMessageMapper = chatMessageMapper;
        this.chatMessageReferenceMapper = chatMessageReferenceMapper;
        this.projectPermissionService = projectPermissionService;
        this.userMapper = userMapper;
        this.aiAgentMapper = aiAgentMapper;
        this.ragContextService = ragContextService;
        this.auditLogApplicationService = auditLogApplicationService;
    }

    @Transactional
    public ChatSessionResponse createSession(Long projectId, CreateChatSessionRequest request) {
        LoginUser currentUser = projectPermissionService.requireCurrentUser();
        projectPermissionService.checkProjectRole(projectId, ProjectRole.OWNER, ProjectRole.MAINTAINER,
                ProjectRole.DEVELOPER);

        ChatSessionEntity session = new ChatSessionEntity();
        session.setProjectId(projectId);
        session.setTitle(request.getTitle());
        session.setSessionType(request.getSessionType() != null ? request.getSessionType()
                : ChatSessionType.PROJECT.name());
        session.setCreatorId(currentUser.getUserId());
        session.setStatus(ChatSessionStatus.ACTIVE.name());

        chatSessionMapper.insert(session);
        return toSessionResponse(session);
    }

    @Transactional(readOnly = true)
    public PageResult<ChatSessionResponse> listSessions(Long projectId, PageQuery pageQuery) {
        projectPermissionService.checkProjectRole(projectId, ProjectRole.OWNER, ProjectRole.MAINTAINER,
                ProjectRole.DEVELOPER, ProjectRole.VIEWER);

        LambdaQueryWrapper<ChatSessionEntity> wrapper = new LambdaQueryWrapper<ChatSessionEntity>()
                .eq(ChatSessionEntity::getProjectId, projectId)
                .orderByDesc(ChatSessionEntity::getLastMessageTime)
                .orderByDesc(ChatSessionEntity::getCreateTime);

        Page<ChatSessionEntity> page = new Page<>(pageQuery.getPage(), pageQuery.getPageSize());
        Page<ChatSessionEntity> result = chatSessionMapper.selectPage(page, wrapper);

        List<ChatSessionResponse> records = result.getRecords().stream()
                .map(this::toSessionResponse)
                .collect(Collectors.toList());

        return PageResult.of(records, pageQuery.getPage(), pageQuery.getPageSize(), result.getTotal());
    }

    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getMessages(Long sessionId, Integer limit) {
        ChatSessionEntity session = getSessionOrThrow(sessionId);
        projectPermissionService.checkProjectRole(session.getProjectId(), ProjectRole.OWNER, ProjectRole.MAINTAINER,
                ProjectRole.DEVELOPER, ProjectRole.VIEWER);

        int fetchLimit = limit != null ? Math.min(limit, 100) : 50;

        LambdaQueryWrapper<ChatMessageEntity> wrapper = new LambdaQueryWrapper<ChatMessageEntity>()
                .eq(ChatMessageEntity::getSessionId, sessionId)
                .orderByAsc(ChatMessageEntity::getCreateTime)
                .last("LIMIT " + fetchLimit);

        List<ChatMessageEntity> messages = chatMessageMapper.selectList(wrapper);
        return messages.stream().map(this::toMessageResponse).collect(Collectors.toList());
    }

    @Transactional
    public SendChatMessageResponse sendMessage(Long sessionId, SendChatMessageRequest request) {
        LoginUser currentUser = projectPermissionService.requireCurrentUser();
        ChatSessionEntity session = getSessionOrThrow(sessionId);
        projectPermissionService.checkProjectRole(session.getProjectId(), ProjectRole.OWNER, ProjectRole.MAINTAINER,
                ProjectRole.DEVELOPER);

        // Save user message (COMPLETED)
        ChatMessageEntity userMessage = new ChatMessageEntity();
        userMessage.setProjectId(session.getProjectId());
        userMessage.setSessionId(sessionId);
        userMessage.setSenderId(currentUser.getUserId());
        userMessage.setSenderType(MessageSenderType.USER.name());
        userMessage.setMessageType(ChatMessageType.TEXT.name());
        userMessage.setContent(request.getContent());
        userMessage.setStatus(ChatMessageStatus.COMPLETED.name());
        userMessage.setTokenUsage(0L);

        if (request.getContext() != null && request.getContext().getTaskId() != null
                && !request.getContext().getTaskId().isBlank()) {
            userMessage.setTaskId(toLongId(request.getContext().getTaskId()));
        }

        chatMessageMapper.insert(userMessage);

        // Execute RAG search
        boolean ragUsed = false;
        List<ChatMessageReferenceResponse> refResponses = new ArrayList<>();

        RagContext ragContext = ragContextService.buildContextForChat(
                session.getProjectId(),
                request.getContent(),
                request.getKnowledgeBaseId(),
                request.getRagLimit(),
                request.getUseRag());

        if (ragContext.getTotal() > 0 && !ragContext.getReferences().isEmpty()) {
            ragUsed = true;
        }

        // Create assistant mock message (STREAMING)
        ChatMessageEntity assistantMessage = new ChatMessageEntity();
        assistantMessage.setProjectId(session.getProjectId());
        assistantMessage.setSessionId(sessionId);
        assistantMessage.setSenderType(MessageSenderType.AGENT.name());
        assistantMessage.setMessageType(ChatMessageType.TEXT.name());
        assistantMessage.setContent("");
        assistantMessage.setStatus(ChatMessageStatus.STREAMING.name());
        assistantMessage.setTokenUsage(0L);

        if (request.getAgentIds() != null && !request.getAgentIds().isEmpty()) {
            assistantMessage.setAgentId(toLongId(request.getAgentIds().get(0)));
        }

        chatMessageMapper.insert(assistantMessage);

        // Save RAG references to assistant message
        if (ragUsed) {
            for (RagReference ragRef : ragContext.getReferences()) {
                ChatMessageReferenceEntity refEntity = new ChatMessageReferenceEntity();
                refEntity.setMessageId(assistantMessage.getId());
                refEntity.setProjectId(session.getProjectId());
                refEntity.setReferenceType(ragRef.getReferenceType() != null
                        ? ragRef.getReferenceType() : MessageReferenceType.DOCUMENT.name());
                refEntity.setReferenceId(toLongId(ragRef.getChunkId()));
                refEntity.setTitle(ragRef.getTitle());
                refEntity.setFilePath(ragRef.getFilePath());
                refEntity.setScore(ragRef.getScore());
                refEntity.setSnippet(ragRef.getSnippet());
                refEntity.setStartLine(ragRef.getStartLine());
                refEntity.setEndLine(ragRef.getEndLine());
                chatMessageReferenceMapper.insert(refEntity);

                // Convert to ChatMessageReferenceResponse for response
                ChatMessageReferenceResponse refResp = new ChatMessageReferenceResponse();
                refResp.setId(refEntity.getId().toString());
                refResp.setReferenceType(refEntity.getReferenceType());
                refResp.setReferenceId(refEntity.getReferenceId() != null ? refEntity.getReferenceId().toString() : null);
                refResp.setTitle(refEntity.getTitle());
                refResp.setFilePath(refEntity.getFilePath());
                refResp.setScore(refEntity.getScore());
                refResp.setSnippet(refEntity.getSnippet());
                refResp.setStartLine(refEntity.getStartLine());
                refResp.setEndLine(refEntity.getEndLine());
                refResponses.add(refResp);
            }
        }

        // Update session last message time
        session.setLastMessageTime(LocalDateTime.now());
        chatSessionMapper.updateById(session);

        auditLogApplicationService.recordSuccess(session.getProjectId(), assistantMessage.getId(),
                AuditActionType.CHAT_SEND.name(), "CHAT_MESSAGE",
                "Send chat message in session #" + sessionId);

        SendChatMessageResponse resp = new SendChatMessageResponse();
        resp.setUserMessageId(userMessage.getId().toString());
        resp.setAssistantMessageId(assistantMessage.getId().toString());
        resp.setStreamUrl("/api/chat/sessions/" + sessionId + "/stream?messageId=" + assistantMessage.getId());
        resp.setRagUsed(ragUsed);
        resp.setReferences(refResponses);
        return resp;
    }

    ChatSessionEntity getSessionOrThrow(Long sessionId) {
        ChatSessionEntity session = chatSessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "会话不存在");
        }
        return session;
    }

    ChatMessageEntity getMessageOrThrow(Long messageId) {
        ChatMessageEntity message = chatMessageMapper.selectById(messageId);
        if (message == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "消息不存在");
        }
        return message;
    }

    List<ChatMessageReferenceResponse> getMessageReferences(Long messageId) {
        return chatMessageReferenceMapper.selectList(
                new LambdaQueryWrapper<ChatMessageReferenceEntity>()
                        .eq(ChatMessageReferenceEntity::getMessageId, messageId)
                        .orderByAsc(ChatMessageReferenceEntity::getCreateTime))
                .stream().map(this::toReferenceResponse).collect(Collectors.toList());
    }

    void updateMessageContent(Long messageId, String content, String status, Long tokenUsage) {
        ChatMessageEntity message = new ChatMessageEntity();
        message.setId(messageId);
        message.setContent(content);
        message.setStatus(status);
        message.setTokenUsage(tokenUsage);
        chatMessageMapper.updateById(message);
    }

    // ---- DTO Mapping ----

    private ChatSessionResponse toSessionResponse(ChatSessionEntity session) {
        ChatSessionResponse resp = new ChatSessionResponse();
        resp.setId(session.getId().toString());
        resp.setProjectId(session.getProjectId().toString());
        resp.setTitle(session.getTitle());
        resp.setSessionType(session.getSessionType());
        resp.setStatus(session.getStatus());
        resp.setCreateTime(session.getCreateTime() != null ? session.getCreateTime().toString() : null);
        resp.setLastMessageTime(session.getLastMessageTime() != null ? session.getLastMessageTime().toString() : null);

        // Fetch last message preview
        if (session.getLastMessageTime() != null) {
            LambdaQueryWrapper<ChatMessageEntity> wrapper = new LambdaQueryWrapper<ChatMessageEntity>()
                    .eq(ChatMessageEntity::getSessionId, session.getId())
                    .orderByDesc(ChatMessageEntity::getCreateTime)
                    .last("LIMIT 1");
            List<ChatMessageEntity> lastMessages = chatMessageMapper.selectList(wrapper);
            if (!lastMessages.isEmpty()) {
                String content = lastMessages.get(0).getContent();
                resp.setLastMessage(content != null && content.length() > 50
                        ? content.substring(0, 50) + "..." : content);
            }
        }

        return resp;
    }

    private ChatMessageResponse toMessageResponse(ChatMessageEntity message) {
        ChatMessageResponse resp = new ChatMessageResponse();
        resp.setId(message.getId().toString());
        resp.setSessionId(message.getSessionId().toString());
        resp.setSenderType(message.getSenderType());
        resp.setSenderId(message.getSenderId() != null ? message.getSenderId().toString() : null);
        resp.setAgentId(message.getAgentId() != null ? message.getAgentId().toString() : null);
        resp.setTaskId(message.getTaskId() != null ? message.getTaskId().toString() : null);
        resp.setMessageType(message.getMessageType());
        resp.setContent(message.getContent());
        resp.setStatus(message.getStatus());
        resp.setTokenUsage(message.getTokenUsage());
        resp.setCreateTime(message.getCreateTime() != null ? message.getCreateTime().toString() : null);

        // Resolve sender name
        if (MessageSenderType.USER.name().equals(message.getSenderType()) && message.getSenderId() != null) {
            UserEntity user = userMapper.selectById(message.getSenderId());
            resp.setSenderName(user != null ? user.getUsername() : null);
        } else if (MessageSenderType.AGENT.name().equals(message.getSenderType()) && message.getAgentId() != null) {
            AiAgentEntity agent = aiAgentMapper.selectById(message.getAgentId());
            resp.setSenderName(agent != null ? agent.getName() : null);
        }

        // Load references
        List<ChatMessageReferenceResponse> refs = chatMessageReferenceMapper.selectList(
                new LambdaQueryWrapper<com.aicoding.platform.chat.domain.ChatMessageReferenceEntity>()
                        .eq(com.aicoding.platform.chat.domain.ChatMessageReferenceEntity::getMessageId, message.getId())
                        .orderByAsc(com.aicoding.platform.chat.domain.ChatMessageReferenceEntity::getCreateTime))
                .stream().map(this::toReferenceResponse).collect(Collectors.toList());
        resp.setReferences(refs);

        return resp;
    }

    private ChatMessageReferenceResponse toReferenceResponse(
            com.aicoding.platform.chat.domain.ChatMessageReferenceEntity entity) {
        ChatMessageReferenceResponse resp = new ChatMessageReferenceResponse();
        resp.setId(entity.getId().toString());
        resp.setReferenceType(entity.getReferenceType());
        resp.setReferenceId(entity.getReferenceId() != null ? entity.getReferenceId().toString() : null);
        resp.setTitle(entity.getTitle());
        resp.setUrl(entity.getUrl());
        resp.setFilePath(entity.getFilePath());
        resp.setStartLine(entity.getStartLine());
        resp.setEndLine(entity.getEndLine());
        resp.setScore(entity.getScore());
        resp.setSnippet(entity.getSnippet());
        return resp;
    }

    private Long toLongId(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Long.decode(value);
    }
}
