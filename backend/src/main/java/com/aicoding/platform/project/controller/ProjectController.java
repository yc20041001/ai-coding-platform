package com.aicoding.platform.project.controller;

import com.aicoding.platform.common.pagination.PageQuery;
import com.aicoding.platform.common.pagination.PageResult;
import com.aicoding.platform.common.response.ApiResponse;
import com.aicoding.platform.project.application.ProjectApplicationService;
import com.aicoding.platform.project.dto.CreateProjectRequest;
import com.aicoding.platform.project.dto.ProjectDetailResponse;
import com.aicoding.platform.project.dto.ProjectOverviewResponse;
import com.aicoding.platform.project.dto.ProjectResponse;
import java.util.Map;
import com.aicoding.platform.project.dto.UpdateProjectRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectApplicationService projectApplicationService;

    public ProjectController(ProjectApplicationService projectApplicationService) {
        this.projectApplicationService = projectApplicationService;
    }

    @PostMapping
    public ApiResponse<ProjectResponse> create(@Valid @RequestBody CreateProjectRequest request) {
        return ApiResponse.ok(projectApplicationService.createProject(request));
    }

    @GetMapping
    public ApiResponse<PageResult<ProjectResponse>> list(
            @Valid PageQuery pageQuery,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        return ApiResponse.ok(projectApplicationService.listProjects(pageQuery, keyword, status));
    }

    @GetMapping("/{projectId}")
    public ApiResponse<ProjectDetailResponse> detail(@PathVariable Long projectId) {
        return ApiResponse.ok(projectApplicationService.getProjectDetail(projectId));
    }

    @PutMapping("/{projectId}")
    public ApiResponse<Boolean> update(@PathVariable Long projectId,
                                        @RequestBody UpdateProjectRequest request) {
        return ApiResponse.ok(projectApplicationService.updateProject(projectId, request));
    }

    @DeleteMapping("/{projectId}")
    public ApiResponse<Boolean> archive(@PathVariable Long projectId) {
        return ApiResponse.ok(projectApplicationService.archiveProject(projectId));
    }

    @GetMapping("/{projectId}/overview")
    public ApiResponse<ProjectOverviewResponse> overview(@PathVariable Long projectId) {
        return ApiResponse.ok(projectApplicationService.getProjectOverview(projectId));
    }

    @PutMapping("/{projectId}/config")
    public ApiResponse<Boolean> updateConfig(@PathVariable Long projectId,
                                              @RequestBody Map<String, Object> request) {
        return ApiResponse.ok(projectApplicationService.updateProjectConfig(projectId, request));
    }
}
