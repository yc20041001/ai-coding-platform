package com.aicoding.platform.agent.controller;

import com.aicoding.platform.agent.application.AgentApplicationService;
import com.aicoding.platform.agent.dto.AgentDetailResponse;
import com.aicoding.platform.agent.dto.AgentResponse;
import com.aicoding.platform.agent.dto.AgentVersionResponse;
import com.aicoding.platform.agent.dto.CreateAgentRequest;
import com.aicoding.platform.agent.dto.EnableProjectAgentRequest;
import com.aicoding.platform.agent.dto.ProjectAgentConfigResponse;
import com.aicoding.platform.agent.dto.UpdateAgentRequest;
import com.aicoding.platform.common.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class AgentController {

    private final AgentApplicationService agentApplicationService;

    public AgentController(AgentApplicationService agentApplicationService) {
        this.agentApplicationService = agentApplicationService;
    }

    @GetMapping("/api/agents")
    public ApiResponse<List<AgentResponse>> listAgents(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status) {
        return ApiResponse.ok(agentApplicationService.listAgents(type, status));
    }

    @PostMapping("/api/agents")
    public ApiResponse<AgentDetailResponse> createAgent(@Valid @RequestBody CreateAgentRequest request) {
        return ApiResponse.ok(agentApplicationService.createAgent(request));
    }

    @GetMapping("/api/agents/{agentId}")
    public ApiResponse<AgentDetailResponse> getAgent(@PathVariable Long agentId) {
        return ApiResponse.ok(agentApplicationService.getAgentDetail(agentId));
    }

    @GetMapping("/api/agents/{agentId}/versions")
    public ApiResponse<List<AgentVersionResponse>> listVersions(@PathVariable Long agentId) {
        return ApiResponse.ok(agentApplicationService.listAgentVersions(agentId));
    }

    @GetMapping("/api/agents/{agentId}/versions/{versionId}")
    public ApiResponse<AgentVersionResponse> getVersion(@PathVariable Long agentId,
                                                         @PathVariable Long versionId) {
        return ApiResponse.ok(agentApplicationService.getAgentVersion(agentId, versionId));
    }

    @PutMapping("/api/agents/{agentId}")
    public ApiResponse<Boolean> updateAgent(@PathVariable Long agentId,
                                            @RequestBody UpdateAgentRequest request) {
        return ApiResponse.ok(agentApplicationService.updateAgent(agentId, request));
    }

    @GetMapping("/api/projects/{projectId}/agents")
    public ApiResponse<List<ProjectAgentConfigResponse>> listProjectAgents(@PathVariable Long projectId) {
        return ApiResponse.ok(agentApplicationService.listProjectAgents(projectId));
    }

    @PostMapping("/api/projects/{projectId}/agents/{agentId}/enable")
    public ApiResponse<Boolean> enableAgent(@PathVariable Long projectId,
                                            @PathVariable Long agentId,
                                            @RequestBody EnableProjectAgentRequest request) {
        return ApiResponse.ok(agentApplicationService.enableProjectAgent(projectId, agentId, request));
    }

    @PostMapping("/api/projects/{projectId}/agents/{agentId}/disable")
    public ApiResponse<Boolean> disableAgent(@PathVariable Long projectId,
                                             @PathVariable Long agentId) {
        return ApiResponse.ok(agentApplicationService.disableProjectAgent(projectId, agentId));
    }
}
