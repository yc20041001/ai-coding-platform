package com.aicoding.platform.orchestration.controller;

import com.aicoding.platform.common.response.ApiResponse;
import com.aicoding.platform.orchestration.application.GovernanceEscalationService;
import com.aicoding.platform.orchestration.application.GovernanceOwnershipHealthService;
import com.aicoding.platform.orchestration.application.GovernanceSlaPolicyService;
import com.aicoding.platform.orchestration.dto.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class GovernanceOperationsController {

    private final GovernanceSlaPolicyService governanceSlaPolicyService;
    private final GovernanceEscalationService governanceEscalationService;
    private final GovernanceOwnershipHealthService governanceOwnershipHealthService;

    public GovernanceOperationsController(GovernanceSlaPolicyService governanceSlaPolicyService,
                                           GovernanceEscalationService governanceEscalationService,
                                           GovernanceOwnershipHealthService governanceOwnershipHealthService) {
        this.governanceSlaPolicyService = governanceSlaPolicyService;
        this.governanceEscalationService = governanceEscalationService;
        this.governanceOwnershipHealthService = governanceOwnershipHealthService;
    }

    // ========== SLA Policy ==========
    @PostMapping("/api/governance-operations/sla-policies")
    public ApiResponse<GovernanceSlaPolicyResponse> createSlaPolicy(@RequestBody CreateGovernanceSlaPolicyRequest req) {
        return ApiResponse.ok(governanceSlaPolicyService.createPolicy(req));
    }

    @GetMapping("/api/governance-operations/sla-policies")
    public ApiResponse<List<GovernanceSlaPolicyResponse>> listSlaPolicies() {
        return ApiResponse.ok(governanceSlaPolicyService.listPolicies());
    }

    @GetMapping("/api/governance-operations/sla-policies/{policyId}")
    public ApiResponse<GovernanceSlaPolicyResponse> getSlaPolicy(@PathVariable String policyId) {
        return ApiResponse.ok(governanceSlaPolicyService.getPolicy(policyId));
    }

    @PutMapping("/api/governance-operations/sla-policies/{policyId}")
    public ApiResponse<GovernanceSlaPolicyResponse> updateSlaPolicy(@PathVariable String policyId,
                                                                      @RequestBody UpdateGovernanceSlaPolicyRequest req) {
        return ApiResponse.ok(governanceSlaPolicyService.updatePolicy(policyId, req));
    }

    @PostMapping("/api/governance-operations/sla-policies/{policyId}/status")
    public ApiResponse<GovernanceSlaPolicyResponse> updateSlaPolicyStatus(@PathVariable String policyId,
                                                                           @RequestParam Boolean enabled) {
        return ApiResponse.ok(governanceSlaPolicyService.updatePolicyStatus(policyId, enabled));
    }

    // ========== Escalation ==========
    @PostMapping("/api/governance-operations/escalations/scan")
    public ApiResponse<String> scanEscalations() {
        int count = governanceEscalationService.scanEscalations();
        return ApiResponse.ok("Created " + count + " escalation events");
    }

    @GetMapping("/api/governance-operations/escalations")
    public ApiResponse<List<GovernanceEscalationEventResponse>> listEscalations() {
        return ApiResponse.ok(governanceEscalationService.listEscalations());
    }

    @GetMapping("/api/governance-operations/escalations/dashboard")
    public ApiResponse<GovernanceEscalationDashboardResponse> getEscalationDashboard() {
        return ApiResponse.ok(governanceEscalationService.getDashboard());
    }

    @PostMapping("/api/governance-operations/escalations/{eventId}/status")
    public ApiResponse<GovernanceEscalationEventResponse> updateEscalationStatus(@PathVariable String eventId,
                                                                                  @RequestParam String status) {
        return ApiResponse.ok(governanceEscalationService.updateEventStatus(eventId, status));
    }

    // ========== Ownership Health ==========
    @PostMapping("/api/governance-operations/ownership/refresh")
    public ApiResponse<String> refreshOwnership() {
        governanceOwnershipHealthService.refreshOwnership();
        return ApiResponse.ok("Ownership snapshot refreshed");
    }

    @GetMapping("/api/governance-operations/ownership")
    public ApiResponse<List<GovernanceOwnershipSnapshotResponse>> getOwnershipList() {
        return ApiResponse.ok(governanceOwnershipHealthService.getOwnershipList());
    }

    @GetMapping("/api/governance-operations/ownership/dashboard")
    public ApiResponse<GovernanceOwnershipDashboardResponse> getOwnershipDashboard() {
        return ApiResponse.ok(governanceOwnershipHealthService.getDashboard());
    }

    // ========== Summary & Report ==========
    @GetMapping("/api/governance-operations/summary")
    public ApiResponse<GovernanceOperationsSummaryResponse> getSummary() {
        return ApiResponse.ok(governanceOwnershipHealthService.getSummary());
    }

    @GetMapping("/api/governance-operations/report")
    public ApiResponse<GovernanceOperationsSummaryResponse> getReport() {
        return ApiResponse.ok(governanceOwnershipHealthService.getSummary());
    }
}
