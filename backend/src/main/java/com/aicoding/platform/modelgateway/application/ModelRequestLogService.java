package com.aicoding.platform.modelgateway.application;

import com.aicoding.platform.audit.application.AuditLogApplicationService;
import com.aicoding.platform.audit.domain.AuditActionType;
import com.aicoding.platform.modelgateway.domain.ModelGatewayErrorCode;
import com.aicoding.platform.modelgateway.dto.ModelRequest;
import com.aicoding.platform.modelgateway.dto.ModelResponse;
import com.aicoding.platform.orchestrator.domain.ModelRequestLogEntity;
import com.aicoding.platform.orchestrator.infrastructure.ModelRequestLogMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class ModelRequestLogService {

    private static final Logger log = LoggerFactory.getLogger(ModelRequestLogService.class);

    private final ModelRequestLogMapper modelRequestLogMapper;
    private final AuditLogApplicationService auditLogApplicationService;
    private final ModelPricingService pricingService;

    public ModelRequestLogService(ModelRequestLogMapper modelRequestLogMapper,
                                   AuditLogApplicationService auditLogApplicationService,
                                   ModelPricingService pricingService) {
        this.modelRequestLogMapper = modelRequestLogMapper;
        this.auditLogApplicationService = auditLogApplicationService;
        this.pricingService = pricingService;
    }

    @Transactional
    public void record(Long projectId, Long executionId, ModelRequest request, ModelResponse response) {
        ModelRequestLogEntity entity = new ModelRequestLogEntity();
        entity.setProjectId(projectId);
        entity.setExecutionId(executionId);
        entity.setProvider(response.getProvider() != null ? response.getProvider() : "MOCK");
        entity.setModelName(response.getModelName() != null ? response.getModelName() : "mock-agent-model");
        entity.setRequestType(request.getRequestType());
        entity.setPromptTokens(defaultLong(response.getPromptTokens()));
        entity.setCompletionTokens(defaultLong(response.getCompletionTokens()));
        entity.setTotalTokens(defaultLong(response.getTotalTokens()));
        entity.setLatencyMs(defaultLong(response.getLatencyMs()));
        entity.setSuccess(Boolean.TRUE.equals(response.getSuccess()));
        entity.setErrorMessage(truncate(response.getErrorMessage(), 2000));
        entity.setFallbackUsed(Boolean.TRUE.equals(response.getFallbackUsed()));

        // Map error type to granular error code
        if (response.getErrorType() != null) {
            entity.setErrorCode(ModelGatewayErrorCode.fromErrorType(response.getErrorType()).name());
        }

        // Estimate cost
        BigDecimal cost = pricingService.estimateCost(
                entity.getModelName(), entity.getPromptTokens(), entity.getCompletionTokens());
        entity.setEstimatedCost(cost);

        modelRequestLogMapper.insert(entity);

        if (Boolean.TRUE.equals(response.getSuccess())) {
            String msg = "Model call: " + entity.getProvider() + "/" + entity.getModelName()
                    + ", tokens=" + entity.getTotalTokens()
                    + ", cost=$" + cost.toPlainString();
            if (Boolean.TRUE.equals(entity.getFallbackUsed())) {
                msg += " [FALLBACK]";
            }
            auditLogApplicationService.recordSuccess(projectId, entity.getId(),
                    AuditActionType.MODEL_CALL.name(), "MODEL_REQUEST", msg);
        } else {
            auditLogApplicationService.recordFailure(projectId, entity.getId(),
                    AuditActionType.MODEL_CALL.name(), "MODEL_REQUEST",
                    "Model call: " + entity.getProvider() + "/" + entity.getModelName()
                            + ", error=" + response.getErrorType(),
                    response.getErrorMessage());
        }

        log.info("Model request logged: provider={} model={} success={} fallback={} tokens={} latencyMs={} cost={}",
                entity.getProvider(), entity.getModelName(), entity.getSuccess(),
                entity.getFallbackUsed(), entity.getTotalTokens(), entity.getLatencyMs(), cost);
    }

    private Long defaultLong(Long value) {
        return value != null ? value : 0L;
    }

    private String truncate(String value, int maxLen) {
        if (value == null) return null;
        return value.length() > maxLen ? value.substring(0, maxLen) + "..." : value;
    }
}
