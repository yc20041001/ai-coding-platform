package com.aicoding.platform.task.application;

import com.aicoding.platform.agent.domain.AiAgentEntity;
import com.aicoding.platform.agent.infrastructure.AiAgentMapper;
import com.aicoding.platform.auth.domain.UserEntity;
import com.aicoding.platform.auth.infrastructure.UserMapper;
import com.aicoding.platform.common.exception.BizException;
import com.aicoding.platform.common.exception.ErrorCode;
import com.aicoding.platform.common.pagination.PageQuery;
import com.aicoding.platform.common.pagination.PageResult;
import com.aicoding.platform.member.application.ProjectPermissionService;
import com.aicoding.platform.member.domain.ProjectRole;
import com.aicoding.platform.security.context.LoginUser;
import com.aicoding.platform.task.domain.AiTaskArtifactEntity;
import com.aicoding.platform.task.domain.AiTaskEntity;
import com.aicoding.platform.task.domain.AiTaskEventEntity;
import com.aicoding.platform.task.domain.AiTaskLogEntity;
import com.aicoding.platform.task.domain.TaskEventType;
import com.aicoding.platform.task.domain.TaskLogLevel;
import com.aicoding.platform.task.domain.TaskStatus;
import com.aicoding.platform.task.dto.CancelTaskRequest;
import com.aicoding.platform.task.dto.CreateTaskRequest;
import com.aicoding.platform.task.dto.TaskArtifactResponse;
import com.aicoding.platform.task.dto.TaskDetailResponse;
import com.aicoding.platform.task.dto.TaskLogResponse;
import com.aicoding.platform.task.dto.TaskResponse;
import com.aicoding.platform.task.infrastructure.AiTaskArtifactMapper;
import com.aicoding.platform.task.infrastructure.AiTaskEventMapper;
import com.aicoding.platform.task.infrastructure.AiTaskLogMapper;
import com.aicoding.platform.task.infrastructure.AiTaskMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class TaskApplicationService {

    private final AiTaskMapper aiTaskMapper;
    private final AiTaskLogMapper aiTaskLogMapper;
    private final AiTaskArtifactMapper aiTaskArtifactMapper;
    private final AiTaskEventMapper aiTaskEventMapper;
    private final AiAgentMapper aiAgentMapper;
    private final UserMapper userMapper;
    private final ProjectPermissionService projectPermissionService;

    private static final Map<String, Set<String>> VALID_TRANSITIONS = new HashMap<>();

    static {
        VALID_TRANSITIONS.put(TaskStatus.PENDING.name(), Set.of(
                TaskStatus.RUNNING.name(), TaskStatus.CANCELED.name()));
        VALID_TRANSITIONS.put(TaskStatus.RUNNING.name(), Set.of(
                TaskStatus.REVIEWING.name(), TaskStatus.COMPLETED.name(),
                TaskStatus.FAILED.name(), TaskStatus.CANCELED.name()));
        VALID_TRANSITIONS.put(TaskStatus.FAILED.name(), Set.of(
                TaskStatus.PENDING.name()));
        VALID_TRANSITIONS.put(TaskStatus.REVIEWING.name(), Set.of(
                TaskStatus.CANCELED.name()));
    }

    public TaskApplicationService(AiTaskMapper aiTaskMapper,
                                   AiTaskLogMapper aiTaskLogMapper,
                                   AiTaskArtifactMapper aiTaskArtifactMapper,
                                   AiTaskEventMapper aiTaskEventMapper,
                                   AiAgentMapper aiAgentMapper,
                                   UserMapper userMapper,
                                   ProjectPermissionService projectPermissionService) {
        this.aiTaskMapper = aiTaskMapper;
        this.aiTaskLogMapper = aiTaskLogMapper;
        this.aiTaskArtifactMapper = aiTaskArtifactMapper;
        this.aiTaskEventMapper = aiTaskEventMapper;
        this.aiAgentMapper = aiAgentMapper;
        this.userMapper = userMapper;
        this.projectPermissionService = projectPermissionService;
    }

    // ---- Task CRUD ----

    @Transactional
    public TaskResponse createTask(Long projectId, CreateTaskRequest request) {
        LoginUser currentUser = projectPermissionService.requireCurrentUser();
        projectPermissionService.checkProjectRole(projectId, ProjectRole.OWNER, ProjectRole.MAINTAINER,
                ProjectRole.DEVELOPER);

        AiTaskEntity task = new AiTaskEntity();
        task.setProjectId(projectId);
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setTaskType(request.getTaskType());
        task.setPriority(request.getPriority() != null ? request.getPriority() : "MEDIUM");
        task.setBranch(request.getBranch());
        task.setSourceType(request.getSourceType());
        task.setSourceId(request.getSourceId() != null ? Long.valueOf(request.getSourceId()) : null);
        task.setCreatorId(currentUser.getUserId());
        task.setStatus(TaskStatus.PENDING.name());
        task.setRetryCount(0);
        task.setMaxRetryCount(3);

        if (request.getAgentId() != null && !request.getAgentId().isBlank()) {
            task.setAgentId(Long.valueOf(request.getAgentId()));
        }

        aiTaskMapper.insert(task);

        // Write CREATED event
        writeEvent(task.getId(), task.getProjectId(), null, TaskStatus.PENDING.name(),
                TaskEventType.CREATED, currentUser.getUserId(), null);

        return toTaskResponse(task);
    }

    @Transactional(readOnly = true)
    public PageResult<TaskResponse> listTasks(Long projectId, PageQuery pageQuery, String status, String taskType) {
        projectPermissionService.checkProjectRole(projectId, ProjectRole.OWNER, ProjectRole.MAINTAINER,
                ProjectRole.DEVELOPER, ProjectRole.VIEWER);

        LambdaQueryWrapper<AiTaskEntity> wrapper = new LambdaQueryWrapper<AiTaskEntity>()
                .eq(AiTaskEntity::getProjectId, projectId);
        if (status != null && !status.isBlank()) {
            wrapper.eq(AiTaskEntity::getStatus, status);
        }
        if (taskType != null && !taskType.isBlank()) {
            wrapper.eq(AiTaskEntity::getTaskType, taskType);
        }
        wrapper.orderByDesc(AiTaskEntity::getCreateTime);

        Page<AiTaskEntity> page = new Page<>(pageQuery.getPage(), pageQuery.getPageSize());
        Page<AiTaskEntity> result = aiTaskMapper.selectPage(page, wrapper);

        Map<Long, String> agentNameCache = new HashMap<>();
        Map<Long, String> userNameCache = new HashMap<>();

        List<TaskResponse> records = result.getRecords().stream()
                .map(t -> toTaskResponse(t, agentNameCache, userNameCache))
                .collect(Collectors.toList());

        return PageResult.of(records, pageQuery.getPage(), pageQuery.getPageSize(), result.getTotal());
    }

    @Transactional(readOnly = true)
    public TaskDetailResponse getTaskDetail(Long taskId) {
        AiTaskEntity task = getTaskOrThrow(taskId);
        projectPermissionService.checkProjectRole(task.getProjectId(), ProjectRole.OWNER, ProjectRole.MAINTAINER,
                ProjectRole.DEVELOPER, ProjectRole.VIEWER);

        return toTaskDetailResponse(task);
    }

    // ---- State Transitions ----

    @Transactional
    public TaskResponse startTask(Long taskId) {
        LoginUser currentUser = projectPermissionService.requireCurrentUser();
        AiTaskEntity task = getTaskOrThrow(taskId);
        projectPermissionService.checkProjectRole(task.getProjectId(), ProjectRole.OWNER, ProjectRole.MAINTAINER,
                ProjectRole.DEVELOPER);

        validateTransition(task.getStatus(), TaskStatus.RUNNING.name());

        task.setStatus(TaskStatus.RUNNING.name());
        task.setStartTime(LocalDateTime.now());
        aiTaskMapper.updateById(task);

        writeEvent(task.getId(), task.getProjectId(), TaskStatus.PENDING.name(), TaskStatus.RUNNING.name(),
                TaskEventType.STARTED, currentUser.getUserId(), null);
        writeLog(task.getId(), task.getProjectId(), TaskLogLevel.INFO, "TASK_START", "Task started by user");

        return toTaskResponse(task);
    }

    @Transactional
    public TaskResponse cancelTask(Long taskId, CancelTaskRequest request) {
        LoginUser currentUser = projectPermissionService.requireCurrentUser();
        AiTaskEntity task = getTaskOrThrow(taskId);
        projectPermissionService.checkProjectRole(task.getProjectId(), ProjectRole.OWNER, ProjectRole.MAINTAINER);

        validateTransition(task.getStatus(), TaskStatus.CANCELED.name());

        String reason = request.getReason() != null ? request.getReason() : "Canceled by user";
        String prevStatus = task.getStatus();

        task.setStatus(TaskStatus.CANCELED.name());
        task.setEndTime(LocalDateTime.now());
        aiTaskMapper.updateById(task);

        writeEvent(task.getId(), task.getProjectId(), prevStatus, TaskStatus.CANCELED.name(),
                TaskEventType.CANCELED, currentUser.getUserId(), reason);
        writeLog(task.getId(), task.getProjectId(), TaskLogLevel.WARN, "TASK_CANCEL", reason);

        return toTaskResponse(task);
    }

    @Transactional
    public TaskResponse retryTask(Long taskId) {
        LoginUser currentUser = projectPermissionService.requireCurrentUser();
        AiTaskEntity task = getTaskOrThrow(taskId);
        projectPermissionService.checkProjectRole(task.getProjectId(), ProjectRole.OWNER, ProjectRole.MAINTAINER,
                ProjectRole.DEVELOPER);

        validateTransition(task.getStatus(), TaskStatus.PENDING.name());

        if (task.getRetryCount() >= task.getMaxRetryCount()) {
            throw new BizException(ErrorCode.CONFLICT,
                    "已超过最大重试次数 (maxRetryCount=" + task.getMaxRetryCount() + ")");
        }

        task.setStatus(TaskStatus.PENDING.name());
        task.setRetryCount(task.getRetryCount() + 1);
        task.setErrorMessage(null);
        task.setStartTime(null);
        task.setEndTime(null);
        aiTaskMapper.updateById(task);

        writeEvent(task.getId(), task.getProjectId(), TaskStatus.FAILED.name(), TaskStatus.PENDING.name(),
                TaskEventType.RETRIED, currentUser.getUserId(), "Retry attempt " + task.getRetryCount());
        writeLog(task.getId(), task.getProjectId(), TaskLogLevel.INFO, "TASK_RETRY",
                "Task retried, attempt " + task.getRetryCount());

        return toTaskResponse(task);
    }

    // ---- Logs & Artifacts ----

    @Transactional(readOnly = true)
    public List<TaskLogResponse> getTaskLogs(Long taskId) {
        AiTaskEntity task = getTaskOrThrow(taskId);
        projectPermissionService.checkProjectRole(task.getProjectId(), ProjectRole.OWNER, ProjectRole.MAINTAINER,
                ProjectRole.DEVELOPER, ProjectRole.VIEWER);

        List<AiTaskLogEntity> logs = aiTaskLogMapper.selectList(
                new LambdaQueryWrapper<AiTaskLogEntity>()
                        .eq(AiTaskLogEntity::getTaskId, taskId)
                        .orderByAsc(AiTaskLogEntity::getCreateTime));

        return logs.stream().map(this::toTaskLogResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TaskArtifactResponse> getTaskArtifacts(Long taskId) {
        AiTaskEntity task = getTaskOrThrow(taskId);
        projectPermissionService.checkProjectRole(task.getProjectId(), ProjectRole.OWNER, ProjectRole.MAINTAINER,
                ProjectRole.DEVELOPER, ProjectRole.VIEWER);

        List<AiTaskArtifactEntity> artifacts = aiTaskArtifactMapper.selectList(
                new LambdaQueryWrapper<AiTaskArtifactEntity>()
                        .eq(AiTaskArtifactEntity::getTaskId, taskId)
                        .orderByAsc(AiTaskArtifactEntity::getCreateTime));

        return artifacts.stream().map(this::toTaskArtifactResponse).collect(Collectors.toList());
    }

    // ---- Internal helpers ----

    private AiTaskEntity getTaskOrThrow(Long taskId) {
        AiTaskEntity task = aiTaskMapper.selectById(taskId);
        if (task == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "任务不存在");
        }
        return task;
    }

    protected void validateTransition(String fromStatus, String toStatus) {
        Set<String> allowed = VALID_TRANSITIONS.get(fromStatus);
        if (allowed == null || !allowed.contains(toStatus)) {
            throw new BizException(ErrorCode.CONFLICT,
                    "不允许的状态流转: " + fromStatus + " -> " + toStatus);
        }
    }

    private void writeEvent(Long taskId, Long projectId, String fromStatus, String toStatus,
                            TaskEventType eventType, Long operatorId, String reason) {
        AiTaskEventEntity event = new AiTaskEventEntity();
        event.setTaskId(taskId);
        event.setProjectId(projectId);
        event.setFromStatus(fromStatus);
        event.setToStatus(toStatus);
        event.setEventType(eventType.name());
        event.setOperatorId(operatorId);
        event.setReason(reason);
        aiTaskEventMapper.insert(event);
    }

    private void writeLog(Long taskId, Long projectId, TaskLogLevel level, String stage, String message) {
        AiTaskLogEntity log = new AiTaskLogEntity();
        log.setTaskId(taskId);
        log.setProjectId(projectId);
        log.setLevel(level.name());
        log.setStage(stage);
        log.setMessage(message);
        aiTaskLogMapper.insert(log);
    }

    private TaskResponse toTaskResponse(AiTaskEntity task) {
        return toTaskResponse(task, new HashMap<>(), new HashMap<>());
    }

    private TaskResponse toTaskResponse(AiTaskEntity task, Map<Long, String> agentNameCache,
                                         Map<Long, String> userNameCache) {
        TaskResponse resp = new TaskResponse();
        resp.setId(task.getId().toString());
        resp.setProjectId(task.getProjectId().toString());
        resp.setTitle(task.getTitle());
        resp.setTaskType(task.getTaskType());
        resp.setStatus(task.getStatus());
        resp.setPriority(task.getPriority());
        resp.setCreateTime(task.getCreateTime() != null ? task.getCreateTime().toString() : null);
        resp.setStartTime(task.getStartTime() != null ? task.getStartTime().toString() : null);
        resp.setEndTime(task.getEndTime() != null ? task.getEndTime().toString() : null);
        resp.setCreatorId(task.getCreatorId() != null ? task.getCreatorId().toString() : null);

        if (task.getAgentId() != null) {
            resp.setAgentId(task.getAgentId().toString());
            resp.setAgentName(agentNameCache.computeIfAbsent(task.getAgentId(), id -> {
                AiAgentEntity agent = aiAgentMapper.selectById(id);
                return agent != null ? agent.getName() : null;
            }));
        }

        if (task.getCreatorId() != null) {
            resp.setCreatorName(userNameCache.computeIfAbsent(task.getCreatorId(), id -> {
                UserEntity user = userMapper.selectById(id);
                return user != null ? user.getUsername() : null;
            }));
        }

        return resp;
    }

    private TaskDetailResponse toTaskDetailResponse(AiTaskEntity task) {
        TaskDetailResponse resp = new TaskDetailResponse();
        resp.setId(task.getId().toString());
        resp.setProjectId(task.getProjectId().toString());
        resp.setTitle(task.getTitle());
        resp.setDescription(task.getDescription());
        resp.setTaskType(task.getTaskType());
        resp.setStatus(task.getStatus());
        resp.setPriority(task.getPriority());
        resp.setSourceType(task.getSourceType());
        resp.setSourceId(task.getSourceId() != null ? task.getSourceId().toString() : null);
        resp.setBranch(task.getBranch());
        resp.setRetryCount(task.getRetryCount());
        resp.setMaxRetryCount(task.getMaxRetryCount());
        resp.setErrorMessage(task.getErrorMessage());
        resp.setCreateTime(task.getCreateTime() != null ? task.getCreateTime().toString() : null);
        resp.setStartTime(task.getStartTime() != null ? task.getStartTime().toString() : null);
        resp.setEndTime(task.getEndTime() != null ? task.getEndTime().toString() : null);
        resp.setCreatorId(task.getCreatorId() != null ? task.getCreatorId().toString() : null);
        resp.setAssigneeId(task.getAssigneeId() != null ? task.getAssigneeId().toString() : null);

        if (task.getAgentId() != null) {
            resp.setAgentId(task.getAgentId().toString());
            AiAgentEntity agent = aiAgentMapper.selectById(task.getAgentId());
            if (agent != null) {
                resp.setAgentName(agent.getName());
            }
        }

        if (task.getCreatorId() != null) {
            UserEntity user = userMapper.selectById(task.getCreatorId());
            if (user != null) {
                resp.setCreatorName(user.getUsername());
            }
        }

        return resp;
    }

    private TaskLogResponse toTaskLogResponse(AiTaskLogEntity entity) {
        TaskLogResponse resp = new TaskLogResponse();
        resp.setId(entity.getId().toString());
        resp.setLevel(entity.getLevel());
        resp.setStage(entity.getStage());
        resp.setMessage(entity.getMessage());
        resp.setCreateTime(entity.getCreateTime() != null ? entity.getCreateTime().toString() : null);
        return resp;
    }

    private TaskArtifactResponse toTaskArtifactResponse(AiTaskArtifactEntity entity) {
        TaskArtifactResponse resp = new TaskArtifactResponse();
        resp.setId(entity.getId().toString());
        resp.setArtifactType(entity.getArtifactType());
        resp.setName(entity.getName());
        resp.setContent(entity.getContent());
        resp.setFileUrl(entity.getFileUrl());
        resp.setCreateTime(entity.getCreateTime() != null ? entity.getCreateTime().toString() : null);
        return resp;
    }
}
