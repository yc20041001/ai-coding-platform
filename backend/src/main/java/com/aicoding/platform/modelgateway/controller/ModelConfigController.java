package com.aicoding.platform.modelgateway.controller;

import com.aicoding.platform.common.exception.BizException;
import com.aicoding.platform.common.exception.ErrorCode;
import com.aicoding.platform.common.response.ApiResponse;
import com.aicoding.platform.modelgateway.application.ModelConfigApplicationService;
import com.aicoding.platform.modelgateway.application.ModelConnectionTestService;
import com.aicoding.platform.modelgateway.dto.ModelConfigRequest;
import com.aicoding.platform.modelgateway.dto.ModelConfigResponse;
import com.aicoding.platform.modelgateway.dto.ModelConnectionTestRequest;
import com.aicoding.platform.modelgateway.dto.ModelConnectionTestResponse;
import com.aicoding.platform.modelgateway.dto.ModelProviderOptionResponse;
import com.aicoding.platform.modelgateway.application.ModelPricingService;
import com.aicoding.platform.modelgateway.dto.ModelUsageCostResponse;
import com.aicoding.platform.orchestrator.domain.ModelRequestLogEntity;
import com.aicoding.platform.orchestrator.infrastructure.ModelRequestLogMapper;
import com.aicoding.platform.security.context.LoginUser;
import com.aicoding.platform.security.context.LoginUserContext;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
public class ModelConfigController {

    private final ModelConfigApplicationService configService;
    private final ModelConnectionTestService connectionTestService;
    private final ModelPricingService pricingService;
    private final ModelRequestLogMapper modelRequestLogMapper;

    public ModelConfigController(ModelConfigApplicationService configService,
                                  ModelConnectionTestService connectionTestService,
                                  ModelPricingService pricingService,
                                  ModelRequestLogMapper modelRequestLogMapper) {
        this.configService = configService;
        this.connectionTestService = connectionTestService;
        this.pricingService = pricingService;
        this.modelRequestLogMapper = modelRequestLogMapper;
    }

    @GetMapping("/api/model-gateway/providers")
    public ApiResponse<List<ModelProviderOptionResponse>> getProviders() {
        requireAdmin();
        return ApiResponse.ok(configService.getProviderOptions());
    }

    @GetMapping("/api/model-gateway/configs")
    public ApiResponse<List<ModelConfigResponse>> listConfigs() {
        requireAdmin();
        return ApiResponse.ok(configService.listConfigs());
    }

    @PostMapping("/api/model-gateway/configs")
    public ApiResponse<ModelConfigResponse> createConfig(@Valid @RequestBody ModelConfigRequest request) {
        requireAdmin();
        return ApiResponse.ok(configService.createOrUpdate(request));
    }

    @PutMapping("/api/model-gateway/configs/{configId}")
    public ApiResponse<ModelConfigResponse> updateConfig(@PathVariable Long configId,
                                                          @Valid @RequestBody ModelConfigRequest request) {
        requireAdmin();
        return ApiResponse.ok(configService.createOrUpdate(request));
    }

    @DeleteMapping("/api/model-gateway/configs/{configId}")
    public ApiResponse<Void> deleteConfig(@PathVariable Long configId) {
        requireAdmin();
        configService.deleteConfig(configId);
        return ApiResponse.ok(null);
    }

    @PostMapping("/api/model-gateway/test-connection")
    public ApiResponse<ModelConnectionTestResponse> testConnection(@Valid @RequestBody ModelConnectionTestRequest request) {
        requireAdmin();
        return ApiResponse.ok(connectionTestService.test(request));
    }

    @GetMapping("/api/observability/model-usage/cost-summary")
    public ApiResponse<ModelUsageCostResponse> getGlobalCostSummary() {
        requireAdmin();
        return ApiResponse.ok(buildCostSummary(new LambdaQueryWrapper<>()));
    }

    @GetMapping("/api/projects/{projectId}/observability/model-usage/cost-summary")
    public ApiResponse<ModelUsageCostResponse> getProjectCostSummary(@PathVariable Long projectId) {
        requireAdmin();
        return ApiResponse.ok(buildCostSummary(new LambdaQueryWrapper<ModelRequestLogEntity>()
                .eq(ModelRequestLogEntity::getProjectId, projectId)));
    }

    private ModelUsageCostResponse buildCostSummary(LambdaQueryWrapper<ModelRequestLogEntity> wrapper) {
        List<ModelRequestLogEntity> logs = modelRequestLogMapper.selectList(wrapper);

        long totalRequests = logs.size();
        long successCount = logs.stream().filter(e -> Boolean.TRUE.equals(e.getSuccess())).count();
        long failureCount = totalRequests - successCount;
        long fallbackCount = logs.stream().filter(e -> "MOCK".equalsIgnoreCase(e.getProvider())).count();

        long promptTokens = logs.stream().mapToLong(e -> e.getPromptTokens() != null ? e.getPromptTokens() : 0L).sum();
        long completionTokens = logs.stream().mapToLong(e -> e.getCompletionTokens() != null ? e.getCompletionTokens() : 0L).sum();
        long totalTokens = promptTokens + completionTokens;

        double successRate = totalRequests > 0 ? (double) successCount / totalRequests : 0.0;

        BigDecimal estimatedCost = BigDecimal.ZERO;
        for (ModelRequestLogEntity log : logs) {
            estimatedCost = estimatedCost.add(
                    pricingService.estimateCost(log.getModelName(), log.getPromptTokens(), log.getCompletionTokens()));
        }

        // Provider breakdown
        Map<String, List<ModelRequestLogEntity>> byProvider = logs.stream()
                .collect(Collectors.groupingBy(e -> e.getProvider() != null ? e.getProvider() : "UNKNOWN"));
        List<ModelUsageCostResponse.ProviderBreakdown> providerBreakdowns = new ArrayList<>();
        for (var entry : byProvider.entrySet()) {
            ModelUsageCostResponse.ProviderBreakdown pb = new ModelUsageCostResponse.ProviderBreakdown();
            pb.setProvider(entry.getKey());
            pb.setRequestCount((long) entry.getValue().size());
            pb.setSuccessCount(entry.getValue().stream().filter(e -> Boolean.TRUE.equals(e.getSuccess())).count());
            pb.setTokenCount(entry.getValue().stream().mapToLong(e -> e.getTotalTokens() != null ? e.getTotalTokens() : 0L).sum());
            BigDecimal cost = BigDecimal.ZERO;
            for (ModelRequestLogEntity log : entry.getValue()) {
                cost = cost.add(pricingService.estimateCost(log.getModelName(), log.getPromptTokens(), log.getCompletionTokens()));
            }
            pb.setCost(cost);
            providerBreakdowns.add(pb);
        }

        // Model breakdown
        Map<String, List<ModelRequestLogEntity>> byModel = logs.stream()
                .collect(Collectors.groupingBy(e -> e.getModelName() != null ? e.getModelName() : "UNKNOWN"));
        List<ModelUsageCostResponse.ModelBreakdown> modelBreakdowns = new ArrayList<>();
        for (var entry : byModel.entrySet()) {
            ModelUsageCostResponse.ModelBreakdown mb = new ModelUsageCostResponse.ModelBreakdown();
            mb.setModelName(entry.getKey());
            mb.setRequestCount((long) entry.getValue().size());
            mb.setTokenCount(entry.getValue().stream().mapToLong(e -> e.getTotalTokens() != null ? e.getTotalTokens() : 0L).sum());
            BigDecimal cost = BigDecimal.ZERO;
            for (ModelRequestLogEntity log : entry.getValue()) {
                cost = cost.add(pricingService.estimateCost(log.getModelName(), log.getPromptTokens(), log.getCompletionTokens()));
            }
            mb.setCost(cost);
            modelBreakdowns.add(mb);
        }

        ModelUsageCostResponse resp = new ModelUsageCostResponse();
        resp.setTotalRequests(totalRequests);
        resp.setSuccessCount(successCount);
        resp.setFailureCount(failureCount);
        resp.setFallbackCount(fallbackCount);
        resp.setSuccessRate(Math.round(successRate * 10000.0) / 100.0);
        resp.setPromptTokens(promptTokens);
        resp.setCompletionTokens(completionTokens);
        resp.setTotalTokens(totalTokens);
        resp.setEstimatedCost(estimatedCost);
        resp.setProviderBreakdowns(providerBreakdowns);
        resp.setModelBreakdowns(modelBreakdowns);
        return resp;
    }

    private void requireAdmin() {
        LoginUser currentUser = LoginUserContext.currentUser()
                .orElseThrow(() -> new BizException(ErrorCode.UNAUTHORIZED));
        if (currentUser.getRoles() == null || !currentUser.getRoles().contains("ADMIN")) {
            throw new BizException(ErrorCode.FORBIDDEN, "需要平台管理员权限");
        }
    }
}
