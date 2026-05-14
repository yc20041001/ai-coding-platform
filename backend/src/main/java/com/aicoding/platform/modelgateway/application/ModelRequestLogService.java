package com.aicoding.platform.modelgateway.application;

import com.aicoding.platform.audit.application.AuditLogApplicationService;
import com.aicoding.platform.audit.domain.AuditActionType;
import com.aicoding.platform.modelgateway.dto.ModelRequest;
import com.aicoding.platform.modelgateway.dto.ModelResponse;
import com.aicoding.platform.orchestrator.domain.ModelRequestLogEntity;
import com.aicoding.platform.orchestrator.infrastructure.ModelRequestLogMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ModelRequestLogService {

    private final ModelRequestLogMapper modelRequestLogMapper;
    private final AuditLogApplicationService auditLogApplicationService;

    public ModelRequestLogService(ModelRequestLogMapper modelRequestLogMapper,
                                   AuditLogApplicationService auditLogApplicationService) {
        this.modelRequestLogMapper = modelRequestLogMapper;
        this.auditLogApplicationService = auditLogApplicationService;
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
        entity.setErrorMessage(response.getErrorMessage());
        modelRequestLogMapper.insert(entity);

        if (Boolean.TRUE.equals(response.getSuccess())) {
            auditLogApplicationService.recordSuccess(projectId, entity.getId(),
                    AuditActionType.MODEL_CALL.name(), "MODEL_REQUEST",
                    "Model call: " + entity.getProvider() + "/" + entity.getModelName()
                            + ", tokens=" + entity.getTotalTokens());
        } else {
            auditLogApplicationService.recordFailure(projectId, entity.getId(),
                    AuditActionType.MODEL_CALL.name(), "MODEL_REQUEST",
                    "Model call: " + entity.getProvider() + "/" + entity.getModelName(),
                    response.getErrorMessage());
        }
    }

    private Long defaultLong(Long value) {
        if (value == null) {
            return 0L;
        }
        return value;
    }
}
