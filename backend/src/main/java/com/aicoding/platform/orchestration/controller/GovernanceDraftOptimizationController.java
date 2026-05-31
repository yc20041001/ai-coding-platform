package com.aicoding.platform.orchestration.controller;

import com.aicoding.platform.common.response.ApiResponse;
import com.aicoding.platform.orchestration.application.*;
import com.aicoding.platform.orchestration.dto.*;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
public class GovernanceDraftOptimizationController {

    private final GovernanceDraftOptimizationService draftOptimizationService;
    private final GovernanceAssistiveOrderingService assistiveOrderingService;
    private final GovernancePackageCompositionService packageCompositionService;

    public GovernanceDraftOptimizationController(GovernanceDraftOptimizationService draftOptimizationService,
                                                  GovernanceAssistiveOrderingService assistiveOrderingService,
                                                  GovernancePackageCompositionService packageCompositionService) {
        this.draftOptimizationService = draftOptimizationService;
        this.assistiveOrderingService = assistiveOrderingService;
        this.packageCompositionService = packageCompositionService;
    }

    @PostMapping("/api/governance-draft-optimization/signals/refresh")
    public ApiResponse<String> refreshSignals() { draftOptimizationService.refreshSignals(); return ApiResponse.ok("Signals refreshed"); }
    @GetMapping("/api/governance-draft-optimization/signals")
    public ApiResponse<List<GovernanceDraftOptimizationSignalResponse>> listSignals() { return ApiResponse.ok(draftOptimizationService.listSignals()); }

    @PostMapping("/api/governance-draft-optimization/assistive-ordering/refresh")
    public ApiResponse<String> refreshAssistiveOrdering() { assistiveOrderingService.refreshOrdering(); return ApiResponse.ok("Ordering refreshed"); }
    @GetMapping("/api/governance-draft-optimization/assistive-ordering")
    public ApiResponse<List<GovernanceAssistiveOrderingOptimizationResponse>> listAssistiveOrdering() { return ApiResponse.ok(assistiveOrderingService.listOrdering()); }

    @PostMapping("/api/governance-draft-optimization/package-composition/refresh")
    public ApiResponse<String> refreshPackageComposition() { packageCompositionService.refreshComposition(); return ApiResponse.ok("Composition refreshed"); }
    @GetMapping("/api/governance-draft-optimization/package-composition")
    public ApiResponse<List<GovernancePackageCompositionTuningResponse>> listPackageComposition() { return ApiResponse.ok(packageCompositionService.listComposition()); }

    @GetMapping("/api/governance-draft-optimization/dashboard")
    public ApiResponse<Map<String, Object>> getDashboard() {
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("signalCount", draftOptimizationService.listSignals().size());
        resp.put("orderingCount", assistiveOrderingService.listOrdering().size());
        resp.put("compositionCount", packageCompositionService.listComposition().size());
        resp.put("signals", draftOptimizationService.listSignals());
        resp.put("ordering", assistiveOrderingService.listOrdering());
        resp.put("composition", packageCompositionService.listComposition());
        return ApiResponse.ok(resp);
    }

    @GetMapping("/api/governance-draft-optimization/report")
    public ApiResponse<String> getReport() {
        StringBuilder md = new StringBuilder();
        md.append("# Draft Optimization Report\n\n");
        md.append("- Signals: ").append(draftOptimizationService.listSignals().size()).append("\n");
        md.append("- Ordering: ").append(assistiveOrderingService.listOrdering().size()).append("\n");
        md.append("- Composition: ").append(packageCompositionService.listComposition().size()).append("\n");
        return ApiResponse.ok(md.toString());
    }
}
