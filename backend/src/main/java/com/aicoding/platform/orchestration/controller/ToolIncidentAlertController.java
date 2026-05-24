package com.aicoding.platform.orchestration.controller;

import com.aicoding.platform.common.pagination.PageQuery;
import com.aicoding.platform.common.pagination.PageResult;
import com.aicoding.platform.common.response.ApiResponse;
import com.aicoding.platform.orchestration.application.ToolAlertDeliveryService;
import com.aicoding.platform.orchestration.application.ToolAlertRuleService;
import com.aicoding.platform.orchestration.application.ToolIncidentService;
import com.aicoding.platform.orchestration.dto.CreateToolAlertRuleRequest;
import com.aicoding.platform.orchestration.dto.CreateToolIncidentRequest;
import com.aicoding.platform.orchestration.dto.ToolAlertDeliveryResponse;
import com.aicoding.platform.orchestration.dto.ToolAlertRuleResponse;
import com.aicoding.platform.orchestration.dto.ToolIncidentResponse;
import com.aicoding.platform.orchestration.dto.ToolIncidentSummaryResponse;
import com.aicoding.platform.orchestration.dto.UpdateToolAlertRuleRequest;
import com.aicoding.platform.orchestration.dto.UpdateToolIncidentRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class ToolIncidentAlertController {

    private final ToolIncidentService incidentService;
    private final ToolAlertRuleService alertRuleService;
    private final ToolAlertDeliveryService alertDeliveryService;

    public ToolIncidentAlertController(ToolIncidentService incidentService,
                                       ToolAlertRuleService alertRuleService,
                                       ToolAlertDeliveryService alertDeliveryService) {
        this.incidentService = incidentService;
        this.alertRuleService = alertRuleService;
        this.alertDeliveryService = alertDeliveryService;
    }

    // --- Incident endpoints ---

    @PostMapping("/api/orchestration/incidents")
    public ApiResponse<ToolIncidentResponse> createIncident(@RequestBody CreateToolIncidentRequest request) {
        return ApiResponse.ok(incidentService.createIncident(request));
    }

    @PutMapping("/api/orchestration/incidents/{id}")
    public ApiResponse<ToolIncidentResponse> updateIncident(@PathVariable Long id,
                                                            @RequestBody UpdateToolIncidentRequest request) {
        return ApiResponse.ok(incidentService.updateIncident(id, request));
    }

    @GetMapping("/api/orchestration/incidents/{id}")
    public ApiResponse<ToolIncidentResponse> getIncident(@PathVariable Long id) {
        return ApiResponse.ok(incidentService.getIncident(id));
    }

    @GetMapping("/api/projects/{projectId}/incidents")
    public ApiResponse<PageResult<ToolIncidentResponse>> listProjectIncidents(
            @PathVariable Long projectId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String severity,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        PageQuery pageQuery = new PageQuery();
        pageQuery.setPage(page);
        pageQuery.setPageSize(pageSize);
        return ApiResponse.ok(incidentService.listProjectIncidents(projectId, status, severity, pageQuery));
    }

    @GetMapping("/api/projects/{projectId}/incidents/summary")
    public ApiResponse<ToolIncidentSummaryResponse> getProjectIncidentSummary(@PathVariable Long projectId) {
        return ApiResponse.ok(incidentService.getProjectIncidentSummary(projectId));
    }

    @PostMapping("/api/projects/{projectId}/incidents/sync-problem-jobs")
    public ApiResponse<Map<String, Integer>> syncProblemJobs(@PathVariable Long projectId) {
        return ApiResponse.ok(incidentService.syncProblemJobs(projectId));
    }

    // --- Alert rule endpoints ---

    @PostMapping("/api/orchestration/alert-rules")
    public ApiResponse<ToolAlertRuleResponse> createAlertRule(@RequestBody CreateToolAlertRuleRequest request) {
        return ApiResponse.ok(alertRuleService.createRule(request));
    }

    @PutMapping("/api/orchestration/alert-rules/{id}")
    public ApiResponse<ToolAlertRuleResponse> updateAlertRule(@PathVariable Long id,
                                                              @RequestBody UpdateToolAlertRuleRequest request) {
        return ApiResponse.ok(alertRuleService.updateRule(id, request));
    }

    @GetMapping("/api/projects/{projectId}/alert-rules")
    public ApiResponse<List<ToolAlertRuleResponse>> listProjectAlertRules(@PathVariable Long projectId) {
        return ApiResponse.ok(alertRuleService.listProjectRules(projectId));
    }

    // --- Alert delivery endpoints ---

    @GetMapping("/api/projects/{projectId}/alert-deliveries")
    public ApiResponse<List<ToolAlertDeliveryResponse>> listProjectAlertDeliveries(@PathVariable Long projectId) {
        return ApiResponse.ok(alertDeliveryService.listProjectDeliveries(projectId));
    }

    @GetMapping("/api/orchestration/incidents/{incidentId}/alert-deliveries")
    public ApiResponse<List<ToolAlertDeliveryResponse>> listIncidentAlertDeliveries(@PathVariable Long incidentId) {
        return ApiResponse.ok(alertDeliveryService.listIncidentDeliveries(incidentId));
    }

    @PostMapping("/api/orchestration/alert-deliveries/{id}/retry")
    public ApiResponse<ToolAlertDeliveryResponse> retryAlertDelivery(@PathVariable Long id) {
        return ApiResponse.ok(alertDeliveryService.retryDelivery(id));
    }
}
