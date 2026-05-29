package com.aicoding.platform.orchestration.controller;

import com.aicoding.platform.common.response.ApiResponse;
import com.aicoding.platform.orchestration.application.OrganizationTrialPolicyService;
import com.aicoding.platform.orchestration.application.PortfolioDriftDetectionService;
import com.aicoding.platform.orchestration.application.ReleaseGuardrailAutomationService;
import com.aicoding.platform.orchestration.dto.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class OrganizationGovernanceController {

    private final OrganizationTrialPolicyService organizationTrialPolicyService;
    private final ReleaseGuardrailAutomationService releaseGuardrailAutomationService;
    private final PortfolioDriftDetectionService portfolioDriftDetectionService;

    public OrganizationGovernanceController(OrganizationTrialPolicyService organizationTrialPolicyService,
                                             ReleaseGuardrailAutomationService releaseGuardrailAutomationService,
                                             PortfolioDriftDetectionService portfolioDriftDetectionService) {
        this.organizationTrialPolicyService = organizationTrialPolicyService;
        this.releaseGuardrailAutomationService = releaseGuardrailAutomationService;
        this.portfolioDriftDetectionService = portfolioDriftDetectionService;
    }

    // ========== Organization Policy ==========

    @PostMapping("/api/organization-governance/policies")
    public ApiResponse<OrganizationTrialPolicyResponse> createPolicy(@RequestBody CreateOrganizationTrialPolicyRequest request) {
        return ApiResponse.ok(organizationTrialPolicyService.createPolicy(request));
    }

    @GetMapping("/api/organization-governance/policies")
    public ApiResponse<List<OrganizationTrialPolicyResponse>> listPolicies(@RequestParam(required = false) String scope) {
        return ApiResponse.ok(organizationTrialPolicyService.listPolicies(scope));
    }

    @GetMapping("/api/organization-governance/policies/{policyId}")
    public ApiResponse<OrganizationTrialPolicyResponse> getPolicy(@PathVariable String policyId) {
        return ApiResponse.ok(organizationTrialPolicyService.getPolicy(policyId));
    }

    @PutMapping("/api/organization-governance/policies/{policyId}")
    public ApiResponse<OrganizationTrialPolicyResponse> updatePolicy(@PathVariable String policyId,
                                                                      @RequestBody UpdateOrganizationTrialPolicyRequest request) {
        return ApiResponse.ok(organizationTrialPolicyService.updatePolicy(policyId, request));
    }

    @PostMapping("/api/organization-governance/policies/{policyId}/status")
    public ApiResponse<OrganizationTrialPolicyResponse> updatePolicyStatus(@PathVariable String policyId,
                                                                           @RequestParam Boolean enabled) {
        return ApiResponse.ok(organizationTrialPolicyService.updatePolicyStatus(policyId, enabled));
    }

    // ========== Guardrail Evaluation ==========

    @PostMapping("/api/organization-governance/guardrails/refresh")
    public ApiResponse<String> refreshGuardrails() {
        releaseGuardrailAutomationService.refreshGuardrails();
        return ApiResponse.ok("Guardrail evaluation refreshed");
    }

    @GetMapping("/api/organization-governance/guardrails")
    public ApiResponse<List<ReleaseGuardrailEvaluationResponse>> getGuardrails() {
        return ApiResponse.ok(releaseGuardrailAutomationService.getGuardrails());
    }

    @GetMapping("/api/organization-governance/guardrails/dashboard")
    public ApiResponse<ReleaseGuardrailDashboardResponse> getGuardrailDashboard() {
        return ApiResponse.ok(releaseGuardrailAutomationService.getDashboard());
    }

    @GetMapping("/api/organization-governance/recommendations")
    public ApiResponse<List<GovernanceRecommendationResponse>> getRecommendations() {
        return ApiResponse.ok(releaseGuardrailAutomationService.getRecommendations());
    }

    // ========== Drift ==========

    @PostMapping("/api/organization-governance/drift/refresh")
    public ApiResponse<String> refreshDrift() {
        portfolioDriftDetectionService.refreshDrift();
        return ApiResponse.ok("Drift snapshot refreshed");
    }

    @GetMapping("/api/organization-governance/drift")
    public ApiResponse<List<PortfolioDriftSnapshotResponse>> getDriftList() {
        return ApiResponse.ok(portfolioDriftDetectionService.getDriftList());
    }

    @GetMapping("/api/organization-governance/drift/dashboard")
    public ApiResponse<PortfolioDriftDashboardResponse> getDriftDashboard() {
        return ApiResponse.ok(portfolioDriftDetectionService.getDriftDashboard());
    }

    // ========== Summary & Report ==========

    @GetMapping("/api/organization-governance/summary")
    public ApiResponse<OrganizationGovernanceSummaryResponse> getSummary() {
        // Refresh guardrails and drift first
        releaseGuardrailAutomationService.refreshGuardrails();
        portfolioDriftDetectionService.refreshDrift();

        OrganizationGovernanceSummaryResponse resp = new OrganizationGovernanceSummaryResponse();
        resp.setSnapshotDate(java.time.LocalDate.now());

        // Get guardrail dashboard for counts
        ReleaseGuardrailDashboardResponse guardrailDash = releaseGuardrailAutomationService.getDashboard();
        resp.setTotalProjectCount(guardrailDash.getProjectCount());
        resp.setBlockCount(guardrailDash.getBlockCount());
        resp.setWarnCount(guardrailDash.getWarnCount());

        // Top risk projects from blocked
        resp.setTopRiskProjects(guardrailDash.getTopBlockedProjects().stream()
                .map(ReleaseGuardrailEvaluationResponse::getProjectName)
                .distinct()
                .collect(java.util.stream.Collectors.toList()));

        // Top drift projects
        PortfolioDriftDashboardResponse driftDash = portfolioDriftDetectionService.getDriftDashboard();
        resp.setTopDriftProjects(driftDash.getTopDriftProjects().stream()
                .map(PortfolioDriftSnapshotResponse::getProjectName)
                .collect(java.util.stream.Collectors.toList()));

        // Top recommendations
        resp.setTopRecommendations(releaseGuardrailAutomationService.getRecommendations().stream()
                .limit(5)
                .collect(java.util.stream.Collectors.toList()));

        // Build summary markdown
        StringBuilder md = new StringBuilder();
        md.append("# Organization Governance Summary\n\n");
        md.append("**Snapshot Date**: ").append(resp.getSnapshotDate()).append("\n\n");
        md.append("**Total Projects**: ").append(resp.getTotalProjectCount()).append("\n\n");
        md.append("---\n\n");
        md.append("## Guardrail Overview\n\n");
        md.append("- BLOCK: ").append(resp.getBlockCount()).append("\n");
        md.append("- WARN: ").append(resp.getWarnCount()).append("\n\n");
        md.append("## Risk Projects\n\n");
        if (!resp.getTopRiskProjects().isEmpty()) {
            md.append("- ").append(String.join("\n- ", resp.getTopRiskProjects())).append("\n\n");
        }
        md.append("## Drift Overview\n\n");
        md.append("- Stable: ").append(driftDash.getStableCount()).append("\n");
        md.append("- Watch: ").append(driftDash.getWatchCount()).append("\n");
        md.append("- High: ").append(driftDash.getHighCount()).append("\n");
        md.append("- Critical: ").append(driftDash.getCriticalCount()).append("\n\n");
        md.append("## Top Recommendations\n\n");
        for (GovernanceRecommendationResponse rec : resp.getTopRecommendations()) {
            md.append("- [").append(rec.getPriority()).append("] ")
              .append(rec.getProjectName()).append(": ")
              .append(rec.getSummary()).append("\n");
        }
        resp.setSummaryMarkdown(md.toString());

        return ApiResponse.ok(resp);
    }

    @GetMapping("/api/organization-governance/report")
    public ApiResponse<OrganizationGovernanceSummaryResponse> getReport() {
        // Same as summary but also refreshes data
        return getSummary();
    }
}
