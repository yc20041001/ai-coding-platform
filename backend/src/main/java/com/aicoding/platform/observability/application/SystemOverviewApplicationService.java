package com.aicoding.platform.observability.application;

import com.aicoding.platform.agent.infrastructure.AiAgentMapper;
import com.aicoding.platform.auth.infrastructure.UserMapper;
import com.aicoding.platform.chat.infrastructure.ChatMessageMapper;
import com.aicoding.platform.observability.dto.SystemOverviewResponse;
import com.aicoding.platform.orchestrator.domain.ModelRequestLogEntity;
import com.aicoding.platform.orchestrator.infrastructure.ModelRequestLogMapper;
import com.aicoding.platform.project.infrastructure.ProjectMapper;
import com.aicoding.platform.rag.infrastructure.KnowledgeBaseMapper;
import com.aicoding.platform.rag.infrastructure.KnowledgeDocumentMapper;
import com.aicoding.platform.task.domain.TaskStatus;
import com.aicoding.platform.task.infrastructure.AiTaskMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class SystemOverviewApplicationService {

    private final ProjectMapper projectMapper;
    private final UserMapper userMapper;
    private final AiTaskMapper aiTaskMapper;
    private final AiAgentMapper aiAgentMapper;
    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final KnowledgeDocumentMapper knowledgeDocumentMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final ModelRequestLogMapper modelRequestLogMapper;

    public SystemOverviewApplicationService(ProjectMapper projectMapper,
                                            UserMapper userMapper,
                                            AiTaskMapper aiTaskMapper,
                                            AiAgentMapper aiAgentMapper,
                                            KnowledgeBaseMapper knowledgeBaseMapper,
                                            KnowledgeDocumentMapper knowledgeDocumentMapper,
                                            ChatMessageMapper chatMessageMapper,
                                            ModelRequestLogMapper modelRequestLogMapper) {
        this.projectMapper = projectMapper;
        this.userMapper = userMapper;
        this.aiTaskMapper = aiTaskMapper;
        this.aiAgentMapper = aiAgentMapper;
        this.knowledgeBaseMapper = knowledgeBaseMapper;
        this.knowledgeDocumentMapper = knowledgeDocumentMapper;
        this.chatMessageMapper = chatMessageMapper;
        this.modelRequestLogMapper = modelRequestLogMapper;
    }

    @Transactional(readOnly = true)
    public SystemOverviewResponse getGlobalOverview() {
        return buildOverview(null);
    }

    @Transactional(readOnly = true)
    public SystemOverviewResponse getProjectOverview(Long projectId) {
        return buildOverview(projectId);
    }

    private SystemOverviewResponse buildOverview(Long projectId) {
        SystemOverviewResponse resp = new SystemOverviewResponse();

        resp.setProjectCount(projectMapper.selectCount(null));
        resp.setUserCount(userMapper.selectCount(null));
        resp.setAgentCount(aiAgentMapper.selectCount(null));

        LambdaQueryWrapper<com.aicoding.platform.task.domain.AiTaskEntity> taskWrapper =
                new LambdaQueryWrapper<>();
        if (projectId != null) {
            taskWrapper.eq(com.aicoding.platform.task.domain.AiTaskEntity::getProjectId, projectId);
        }
        resp.setTaskCount(aiTaskMapper.selectCount(taskWrapper));
        resp.setRunningTaskCount(aiTaskMapper.selectCount(
                taskWrapper.clone().eq(com.aicoding.platform.task.domain.AiTaskEntity::getStatus,
                        TaskStatus.RUNNING.name())));
        resp.setCompletedTaskCount(aiTaskMapper.selectCount(
                taskWrapper.clone().eq(com.aicoding.platform.task.domain.AiTaskEntity::getStatus,
                        TaskStatus.COMPLETED.name())));

        LambdaQueryWrapper<com.aicoding.platform.rag.domain.KnowledgeBaseEntity> kbWrapper =
                new LambdaQueryWrapper<>();
        if (projectId != null) {
            kbWrapper.eq(com.aicoding.platform.rag.domain.KnowledgeBaseEntity::getProjectId, projectId);
        }
        resp.setKnowledgeBaseCount(knowledgeBaseMapper.selectCount(kbWrapper));

        LambdaQueryWrapper<com.aicoding.platform.rag.domain.KnowledgeDocumentEntity> docWrapper =
                new LambdaQueryWrapper<>();
        if (projectId != null) {
            docWrapper.eq(com.aicoding.platform.rag.domain.KnowledgeDocumentEntity::getProjectId, projectId);
        }
        resp.setDocumentCount(knowledgeDocumentMapper.selectCount(docWrapper));

        LambdaQueryWrapper<com.aicoding.platform.chat.domain.ChatMessageEntity> msgWrapper =
                new LambdaQueryWrapper<>();
        if (projectId != null) {
            msgWrapper.eq(com.aicoding.platform.chat.domain.ChatMessageEntity::getProjectId, projectId);
        }
        resp.setChatMessageCount(chatMessageMapper.selectCount(msgWrapper));

        LambdaQueryWrapper<ModelRequestLogEntity> modelWrapper = new LambdaQueryWrapper<>();
        if (projectId != null) {
            modelWrapper.eq(ModelRequestLogEntity::getProjectId, projectId);
        }
        resp.setModelRequestCount(modelRequestLogMapper.selectCount(modelWrapper));

        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        resp.setTodayModelRequestCount(modelRequestLogMapper.selectCount(
                modelWrapper.clone().ge(ModelRequestLogEntity::getCreateTime, todayStart)));
        resp.setTodayTokenUsage(modelRequestLogMapper.selectList(
                        modelWrapper.clone().ge(ModelRequestLogEntity::getCreateTime, todayStart)).stream()
                .mapToLong(e -> { Long t = e.getTotalTokens(); return t != null ? t : 0L; })
                .sum());

        return resp;
    }
}
