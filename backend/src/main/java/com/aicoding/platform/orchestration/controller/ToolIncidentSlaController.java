package com.aicoding.platform.orchestration.controller;

import com.aicoding.platform.common.response.ApiResponse;
import com.aicoding.platform.orchestration.application.ToolEscalationPolicyService;
import com.aicoding.platform.orchestration.application.ToolEscalationService;
import com.aicoding.platform.orchestration.application.ToolIncidentSlaService;
import com.aicoding.platform.orchestration.application.ToolIncidentTimelineService;
import com.aicoding.platform.orchestration.dto.CreateToolEscalationPolicyRequest;
import com.aicoding.platform.orchestration.dto.EscalateIncidentRequest;
import com.aicoding.platform.orchestration.dto.ToolEscalationEventResponse;
import com.aicoding.platform.orchestration.dto.ToolEscalationPolicyResponse;
import com.aicoding.platform.orchestration.dto.ToolIncidentEscalationScanResponse;
import com.aicoding.platform.orchestration.dto.ToolIncidentSlaScanResponse;
import com.aicoding.platform.orchestration.dto.ToolIncidentTimelineResponse;
import com.aicoding.platform.orchestration.dto.UpdateToolEscalationPolicyRequest;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ToolIncidentSlaController {

    private final ToolIncidentSlaService slaService;
    private final ToolEscalationService escalationService;
    private final ToolEscalationPolicyService escalationPolicyService;
    private final ToolIncidentTimelineService timelineService;

    public ToolIncidentSlaController(ToolIncidentSlaService slaService,
                                      ToolEscalationService escalationService,
                                      ToolEscalationPolicyService escalationPolicyService,
                                      ToolIncidentTimelineService timelineService) {
        this.slaService = slaService;
        this.escalationService = escalationService;
        this.escalationPolicyService = escalationPolicyService;
        this.timelineService = timelineService;
    }

    // --- SLA ---

    @PostMapping("/api/projects/{projectId}/incident-sla/scan")
    public ApiResponse<ToolIncidentSlaScanResponse> scanProjectSla(@PathVariable Long projectId) {
        return ApiResponse.ok(slaService.scanProjectSla(projectId));
    }

    // --- Escalation ---

    @PostMapping("/api/projects/{projectId}/incident-escalation/scan")
    public ApiResponse<ToolIncidentEscalationScanResponse> scanEscalation(@PathVariable Long projectId) {
        return ApiResponse.ok(escalationService.scanEscalation(projectId));
    }

    @PostMapping("/api/orchestration/incidents/{incidentId}/escalate")
    public ApiResponse<ToolEscalationEventResponse> escalateIncident(
            @PathVariable Long incidentId,
            @RequestBody(required = false) EscalateIncidentRequest request) {
        if (request == null) {
            request = new EscalateIncidentRequest();
        }
        return ApiResponse.ok(escalationService.escalateIncident(incidentId, request));
    }

    @GetMapping("/api/orchestration/incidents/{incidentId}/escalation-events")
    public ApiResponse<List<ToolEscalationEventResponse>> listEscalationEvents(@PathVariable Long incidentId) {
        return ApiResponse.ok(escalationService.listIncidentEscalationEvents(incidentId));
    }

    // --- Timeline ---

    @GetMapping("/api/orchestration/incidents/{incidentId}/timeline")
    public ApiResponse<ToolIncidentTimelineResponse> getIncidentTimeline(@PathVariable Long incidentId) {
        return ApiResponse.ok(timelineService.getIncidentTimeline(incidentId));
    }

    // --- Escalation Policies ---

    @GetMapping("/api/projects/{projectId}/escalation-policies")
    public ApiResponse<List<ToolEscalationPolicyResponse>> listProjectPolicies(@PathVariable Long projectId) {
        return ApiResponse.ok(escalationPolicyService.listProjectPolicies(projectId));
    }

    @GetMapping("/api/orchestration/escalation-policies/{policyId}")
    public ApiResponse<ToolEscalationPolicyResponse> getPolicy(@PathVariable Long policyId) {
        return ApiResponse.ok(escalationPolicyService.getPolicy(policyId));
    }

    @PostMapping("/api/orchestration/escalation-policies")
    public ApiResponse<ToolEscalationPolicyResponse> createPolicy(
            @RequestBody CreateToolEscalationPolicyRequest request) {
        return ApiResponse.ok(escalationPolicyService.createPolicy(request));
    }

    @PutMapping("/api/orchestration/escalation-policies/{policyId}")
    public ApiResponse<ToolEscalationPolicyResponse> updatePolicy(
            @PathVariable Long policyId,
            @RequestBody UpdateToolEscalationPolicyRequest request) {
        return ApiResponse.ok(escalationPolicyService.updatePolicy(policyId, request));
    }

    @DeleteMapping("/api/orchestration/escalation-policies/{policyId}")
    public ApiResponse<Void> deletePolicy(@PathVariable Long policyId) {
        escalationPolicyService.deletePolicy(policyId);
        return ApiResponse.ok(null);
    }
}
