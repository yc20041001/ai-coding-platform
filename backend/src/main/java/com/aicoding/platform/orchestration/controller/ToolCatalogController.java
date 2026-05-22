package com.aicoding.platform.orchestration.controller;

import com.aicoding.platform.common.response.ApiResponse;
import com.aicoding.platform.orchestration.application.ToolCatalogApplicationService;
import com.aicoding.platform.orchestration.dto.ProjectToolConfigResponse;
import com.aicoding.platform.orchestration.dto.ToolCatalogResponse;
import com.aicoding.platform.orchestration.dto.UpdateProjectToolConfigRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ToolCatalogController {

    private final ToolCatalogApplicationService toolCatalogApplicationService;

    public ToolCatalogController(ToolCatalogApplicationService toolCatalogApplicationService) {
        this.toolCatalogApplicationService = toolCatalogApplicationService;
    }

    @GetMapping("/api/tool-catalog")
    public ApiResponse<List<ToolCatalogResponse>> listTools(
            @RequestParam(required = false) String toolType,
            @RequestParam(required = false) String riskLevel,
            @RequestParam(required = false) Boolean enabled) {
        return ApiResponse.ok(toolCatalogApplicationService.listTools(toolType, riskLevel, enabled));
    }

    @GetMapping("/api/projects/{projectId}/tools")
    public ApiResponse<List<ProjectToolConfigResponse>> listProjectTools(
            @PathVariable String projectId) {
        return ApiResponse.ok(
                toolCatalogApplicationService.listProjectTools(Long.valueOf(projectId)));
    }

    @PostMapping("/api/projects/{projectId}/tools/{toolId}/enable")
    public ApiResponse<ProjectToolConfigResponse> enableProjectTool(
            @PathVariable String projectId,
            @PathVariable String toolId,
            @RequestBody(required = false) UpdateProjectToolConfigRequest request) {
        return ApiResponse.ok(
                toolCatalogApplicationService.enableProjectTool(
                        Long.valueOf(projectId), Long.valueOf(toolId), request));
    }

    @PostMapping("/api/projects/{projectId}/tools/{toolId}/disable")
    public ApiResponse<ProjectToolConfigResponse> disableProjectTool(
            @PathVariable String projectId,
            @PathVariable String toolId) {
        return ApiResponse.ok(
                toolCatalogApplicationService.disableProjectTool(
                        Long.valueOf(projectId), Long.valueOf(toolId)));
    }
}
