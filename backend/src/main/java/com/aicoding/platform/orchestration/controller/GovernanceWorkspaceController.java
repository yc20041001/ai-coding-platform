package com.aicoding.platform.orchestration.controller;

import com.aicoding.platform.common.response.ApiResponse;
import com.aicoding.platform.orchestration.application.*;
import com.aicoding.platform.orchestration.dto.*;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
public class GovernanceWorkspaceController {

    private final GovernanceWorkspaceService workspaceService;
    private final GovernanceGuidedOperationsService guidedOperationsService;
    private final GovernanceNextStepRecommendationService nextStepService;

    public GovernanceWorkspaceController(GovernanceWorkspaceService workspaceService,
                                          GovernanceGuidedOperationsService guidedOperationsService,
                                          GovernanceNextStepRecommendationService nextStepService) {
        this.workspaceService = workspaceService;
        this.guidedOperationsService = guidedOperationsService;
        this.nextStepService = nextStepService;
    }

    // ========== Workspace Session ==========
    @PostMapping("/api/governance-workspace/sessions")
    public ApiResponse<GovernanceWorkspaceSessionResponse> createSession(@RequestParam(required = false) String operatorName,
                                                                          @RequestParam(required = false) String focusMode) {
        return ApiResponse.ok(workspaceService.createSession(operatorName, focusMode));
    }

    @GetMapping("/api/governance-workspace/sessions")
    public ApiResponse<List<GovernanceWorkspaceSessionResponse>> listSessions() {
        return ApiResponse.ok(workspaceService.listSessions());
    }

    @GetMapping("/api/governance-workspace/sessions/{sessionId}")
    public ApiResponse<GovernanceWorkspaceSessionResponse> getSession(@PathVariable String sessionId) {
        return ApiResponse.ok(workspaceService.getSession(sessionId));
    }

    @PutMapping("/api/governance-workspace/sessions/{sessionId}")
    public ApiResponse<GovernanceWorkspaceSessionResponse> updateSession(@PathVariable String sessionId,
                                                                          @RequestParam(required = false) String focusMode,
                                                                          @RequestParam(required = false) String selectedProjectId,
                                                                          @RequestParam(required = false) String selectedRecommendationId,
                                                                          @RequestParam(required = false) String selectedOwnerId) {
        return ApiResponse.ok(workspaceService.updateSession(sessionId, focusMode, selectedProjectId, selectedRecommendationId, selectedOwnerId));
    }

    @PostMapping("/api/governance-workspace/sessions/{sessionId}/status")
    public ApiResponse<GovernanceWorkspaceSessionResponse> updateSessionStatus(@PathVariable String sessionId,
                                                                                @RequestParam String status) {
        return ApiResponse.ok(workspaceService.updateSessionStatus(sessionId, status));
    }

    // ========== Guided Tasks ==========
    @GetMapping("/api/governance-workspace/sessions/{sessionId}/tasks")
    public ApiResponse<List<GovernanceGuidedTaskResponse>> getTasks(@PathVariable String sessionId) {
        return ApiResponse.ok(guidedOperationsService.getTasks(sessionId));
    }

    @PostMapping("/api/governance-workspace/tasks/{taskId}/status")
    public ApiResponse<GovernanceGuidedTaskResponse> updateTaskStatus(@PathVariable String taskId,
                                                                       @RequestParam String status) {
        return ApiResponse.ok(guidedOperationsService.updateTaskStatus(taskId, status));
    }

    // ========== Workspace Refresh & Next Steps ==========
    @PostMapping("/api/governance-workspace/sessions/{sessionId}/refresh")
    public ApiResponse<Map<String, Object>> refreshWorkspace(@PathVariable String sessionId) {
        var tasks = guidedOperationsService.refreshTasks(sessionId);
        var nextSteps = nextStepService.refreshNextSteps(sessionId);
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("sessionId", sessionId);
        resp.put("taskCount", tasks.size());
        resp.put("nextStepCount", nextSteps.size());
        return ApiResponse.ok(resp);
    }

    @GetMapping("/api/governance-workspace/sessions/{sessionId}/next-steps")
    public ApiResponse<List<GovernanceNextStepRecommendationResponse>> getNextSteps(@PathVariable String sessionId) {
        return ApiResponse.ok(nextStepService.getNextSteps(sessionId));
    }

    @SuppressWarnings("unchecked")
    @GetMapping("/api/governance-workspace/dashboard")
    public ApiResponse<Map<String, Object>> getDashboard() {
        var activeSession = workspaceService.getActiveSession();
        var tasks = guidedOperationsService.getTasks(activeSession.getId());
        var nextSteps = nextStepService.getNextSteps(activeSession.getId());

        int open = 0, inProgress = 0, blocked = 0;
        for (var t : tasks) {
            switch (t.getTaskStatus()) {
                case "OPEN" -> open++;
                case "IN_PROGRESS" -> inProgress++;
                case "BLOCKED" -> blocked++;
            }
        }

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("activeSession", activeSession);
        resp.put("focusMode", activeSession.getFocusMode());
        resp.put("openTaskCount", open);
        resp.put("inProgressTaskCount", inProgress);
        resp.put("blockedTaskCount", blocked);
        resp.put("topGuidedTasks", tasks.stream().limit(10).collect(java.util.stream.Collectors.toList()));
        resp.put("topNextStepRecommendations", nextSteps.stream().limit(5).collect(java.util.stream.Collectors.toList()));
        return ApiResponse.ok(resp);
    }

    @GetMapping("/api/governance-workspace/report")
    public ApiResponse<String> getReport() {
        var activeSession = workspaceService.getActiveSession();
        var tasks = guidedOperationsService.getTasks(activeSession.getId());
        var nextSteps = nextStepService.getNextSteps(activeSession.getId());

        StringBuilder md = new StringBuilder();
        md.append("# Governance Workspace Summary\n\n");
        md.append("**Focus Mode**: ").append(activeSession.getFocusMode()).append("\n\n");
        md.append("## Guided Tasks (").append(tasks.size()).append(")\n\n");
        for (var t : tasks.stream().limit(10).collect(java.util.stream.Collectors.toList())) {
            md.append("- [").append(t.getPriority()).append("] ").append(t.getTitle()).append(" (").append(t.getTaskStatus()).append(")\n");
        }
        md.append("\n## Next Steps\n\n");
        for (var s : nextSteps) {
            md.append("- ").append(s.getTitle()).append("\n");
        }
        return ApiResponse.ok(md.toString());
    }
}
