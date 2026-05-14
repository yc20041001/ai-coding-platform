package com.aicoding.platform.task.controller;

import com.aicoding.platform.common.pagination.PageQuery;
import com.aicoding.platform.common.pagination.PageResult;
import com.aicoding.platform.common.response.ApiResponse;
import com.aicoding.platform.task.application.TaskApplicationService;
import com.aicoding.platform.task.dto.CancelTaskRequest;
import com.aicoding.platform.task.dto.CreateTaskRequest;
import com.aicoding.platform.task.dto.TaskArtifactResponse;
import com.aicoding.platform.task.dto.TaskDetailResponse;
import com.aicoding.platform.task.dto.TaskLogResponse;
import com.aicoding.platform.task.dto.TaskResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class TaskController {

    private final TaskApplicationService taskApplicationService;

    public TaskController(TaskApplicationService taskApplicationService) {
        this.taskApplicationService = taskApplicationService;
    }

    @PostMapping("/api/projects/{projectId}/tasks")
    public ApiResponse<TaskResponse> createTask(@PathVariable Long projectId,
                                                 @Valid @RequestBody CreateTaskRequest request) {
        return ApiResponse.ok(taskApplicationService.createTask(projectId, request));
    }

    @GetMapping("/api/projects/{projectId}/tasks")
    public ApiResponse<PageResult<TaskResponse>> listTasks(
            @PathVariable Long projectId,
            @Valid PageQuery pageQuery,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String taskType) {
        return ApiResponse.ok(taskApplicationService.listTasks(projectId, pageQuery, status, taskType));
    }

    @GetMapping("/api/tasks/{taskId}")
    public ApiResponse<TaskDetailResponse> getTask(@PathVariable Long taskId) {
        return ApiResponse.ok(taskApplicationService.getTaskDetail(taskId));
    }

    @PostMapping("/api/tasks/{taskId}/start")
    public ApiResponse<TaskResponse> startTask(@PathVariable Long taskId) {
        return ApiResponse.ok(taskApplicationService.startTask(taskId));
    }

    @PostMapping("/api/tasks/{taskId}/cancel")
    public ApiResponse<TaskResponse> cancelTask(@PathVariable Long taskId,
                                                 @RequestBody CancelTaskRequest request) {
        return ApiResponse.ok(taskApplicationService.cancelTask(taskId, request));
    }

    @PostMapping("/api/tasks/{taskId}/retry")
    public ApiResponse<TaskResponse> retryTask(@PathVariable Long taskId) {
        return ApiResponse.ok(taskApplicationService.retryTask(taskId));
    }

    @GetMapping("/api/tasks/{taskId}/logs")
    public ApiResponse<List<TaskLogResponse>> getTaskLogs(@PathVariable Long taskId) {
        return ApiResponse.ok(taskApplicationService.getTaskLogs(taskId));
    }

    @GetMapping("/api/tasks/{taskId}/artifacts")
    public ApiResponse<List<TaskArtifactResponse>> getTaskArtifacts(@PathVariable Long taskId) {
        return ApiResponse.ok(taskApplicationService.getTaskArtifacts(taskId));
    }
}
