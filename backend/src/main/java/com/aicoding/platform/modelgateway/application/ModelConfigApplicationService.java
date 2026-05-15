package com.aicoding.platform.modelgateway.application;

import com.aicoding.platform.agent.domain.ModelConfigEntity;
import com.aicoding.platform.agent.infrastructure.ModelConfigMapper;
import com.aicoding.platform.common.exception.BizException;
import com.aicoding.platform.common.exception.ErrorCode;
import com.aicoding.platform.modelgateway.domain.ModelProviderCapability;
import com.aicoding.platform.modelgateway.dto.ModelConfigRequest;
import com.aicoding.platform.modelgateway.dto.ModelConfigResponse;
import com.aicoding.platform.modelgateway.dto.ModelProviderOptionResponse;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class ModelConfigApplicationService {

    private static final Logger log = LoggerFactory.getLogger(ModelConfigApplicationService.class);

    private final ModelConfigMapper modelConfigMapper;
    private final ModelSecretMaskingService maskingService;

    // Provider capability registry
    private static final List<ModelProviderCapability> BUILTIN_PROVIDERS = List.of(
            new ModelProviderCapability("MOCK", "Mock Provider", true, true, false, false, null,
                    new String[]{"mock-agent-model"}),
            new ModelProviderCapability("OPENAI_COMPATIBLE", "OpenAI Compatible", true, true, true, true,
                    "https://api.openai.com/v1",
                    new String[]{"gpt-4.1-mini", "gpt-4.1", "gpt-4o", "gpt-4o-mini"}),
            new ModelProviderCapability("CLAUDE", "Anthropic Claude", true, true, true, true,
                    "https://api.anthropic.com",
                    new String[]{"claude-3-5-sonnet-latest", "claude-3-opus-latest", "claude-3-haiku-latest"}),
            new ModelProviderCapability("DEEPSEEK", "DeepSeek", true, true, true, true,
                    "https://api.deepseek.com/v1",
                    new String[]{"deepseek-chat", "deepseek-reasoner"}),
            new ModelProviderCapability("QWEN", "Qwen (Tongyi)", true, true, true, true,
                    "https://dashscope.aliyuncs.com/compatible-mode/v1",
                    new String[]{"qwen-plus", "qwen-max", "qwen-turbo"}),
            new ModelProviderCapability("GEMINI", "Google Gemini", true, true, true, true,
                    "https://generativelanguage.googleapis.com/v1beta",
                    new String[]{"gemini-2.5-flash", "gemini-2.5-pro"})
    );

    public ModelConfigApplicationService(ModelConfigMapper modelConfigMapper,
                                          ModelSecretMaskingService maskingService) {
        this.modelConfigMapper = modelConfigMapper;
        this.maskingService = maskingService;
    }

    @Transactional(readOnly = true)
    public List<ModelProviderOptionResponse> getProviderOptions() {
        List<ModelProviderOptionResponse> result = new ArrayList<>();
        for (ModelProviderCapability cap : BUILTIN_PROVIDERS) {
            ModelProviderOptionResponse opt = new ModelProviderOptionResponse();
            opt.setProvider(cap.getProvider());
            opt.setDisplayName(cap.getDisplayName());
            opt.setSupportsStream(cap.isSupportsStream());
            opt.setSupportsNonStream(cap.isSupportsNonStream());
            opt.setRequiresApiKey(cap.isRequiresApiKey());
            opt.setRequiresBaseUrl(cap.isRequiresBaseUrl());
            opt.setDefaultBaseUrl(cap.getDefaultBaseUrl());
            opt.setKnownModels(cap.getKnownModels());
            result.add(opt);
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<ModelConfigResponse> listConfigs() {
        List<ModelConfigEntity> entities = modelConfigMapper.selectList(
                new LambdaQueryWrapper<ModelConfigEntity>()
                        .orderByDesc(ModelConfigEntity::getUpdateTime));

        List<ModelConfigResponse> result = new ArrayList<>();
        for (ModelConfigEntity e : entities) {
            result.add(toResponse(e));
        }
        return result;
    }

    @Transactional
    public ModelConfigResponse createOrUpdate(ModelConfigRequest request) {
        // Find existing by provider + modelName
        ModelConfigEntity existing = modelConfigMapper.selectOne(
                new LambdaQueryWrapper<ModelConfigEntity>()
                        .eq(ModelConfigEntity::getProvider, request.getProvider())
                        .eq(ModelConfigEntity::getModelName, request.getModelName()));

        if (existing != null) {
            // Update
            applyRequest(existing, request);
            existing.setUpdateTime(java.time.LocalDateTime.now());
            modelConfigMapper.updateById(existing);
            log.info("Updated model config: provider={} model={}", existing.getProvider(), existing.getModelName());
            return toResponse(existing);
        } else {
            // Create
            ModelConfigEntity entity = new ModelConfigEntity();
            applyRequest(entity, request);
            modelConfigMapper.insert(entity);
            log.info("Created model config: provider={} model={}", entity.getProvider(), entity.getModelName());
            return toResponse(entity);
        }
    }

    @Transactional
    public void deleteConfig(Long configId) {
        ModelConfigEntity entity = modelConfigMapper.selectById(configId);
        if (entity == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "模型配置不存在");
        }
        modelConfigMapper.deleteById(configId);
        log.info("Deleted model config: id={} provider={} model={}", configId, entity.getProvider(), entity.getModelName());
    }

    private void applyRequest(ModelConfigEntity entity, ModelConfigRequest request) {
        entity.setProvider(request.getProvider());
        entity.setModelName(request.getModelName());
        entity.setModelType(request.getModelType() != null ? request.getModelType() : "CHAT");
        entity.setApiBase(request.getApiBase());
        entity.setStatus(request.getStatus() != null ? request.getStatus() : "ENABLED");

        // API key: only store if provided. Never overwrite with empty.
        if (request.getApiKey() != null && !request.getApiKey().isBlank()) {
            entity.setApiKeyEnc(request.getApiKey());
        }

        // Default params stored as JSON for timeout/retry/fallback/stream
        if (request.getTimeoutMs() != null || request.getMaxRetries() != null
                || request.getFallbackEnabled() != null || request.getStreamEnabled() != null) {
            StringBuilder params = new StringBuilder("{");
            if (request.getTimeoutMs() != null) params.append("\"timeoutMs\":").append(request.getTimeoutMs()).append(",");
            if (request.getMaxRetries() != null) params.append("\"maxRetries\":").append(request.getMaxRetries()).append(",");
            if (request.getFallbackEnabled() != null) params.append("\"fallbackEnabled\":").append(request.getFallbackEnabled()).append(",");
            if (request.getStreamEnabled() != null) params.append("\"streamEnabled\":").append(request.getStreamEnabled()).append(",");
            if (params.charAt(params.length() - 1) == ',') params.setLength(params.length() - 1);
            params.append("}");
            entity.setDefaultParams(params.toString());
        }
    }

    private ModelConfigResponse toResponse(ModelConfigEntity e) {
        ModelConfigResponse r = new ModelConfigResponse();
        r.setId(e.getId());
        r.setProvider(e.getProvider());
        r.setModelName(e.getModelName());
        r.setModelType(e.getModelType());
        r.setApiBase(e.getApiBase());
        r.setMaskedApiKey(maskingService.mask(e.getApiKeyEnc()));
        r.setStatus(e.getStatus());
        r.setCreateTime(e.getCreateTime());
        r.setUpdateTime(e.getUpdateTime());

        // Parse default params JSON for display
        if (e.getDefaultParams() != null && !e.getDefaultParams().isBlank()) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                com.fasterxml.jackson.databind.JsonNode node = mapper.readTree(e.getDefaultParams());
                if (node.has("timeoutMs")) r.setTimeoutMs(node.get("timeoutMs").asLong());
                if (node.has("maxRetries")) r.setMaxRetries(node.get("maxRetries").asInt());
                if (node.has("fallbackEnabled")) r.setFallbackEnabled(node.get("fallbackEnabled").asBoolean());
                if (node.has("streamEnabled")) r.setStreamEnabled(node.get("streamEnabled").asBoolean());
            } catch (Exception ex) {
                log.debug("Failed to parse default params JSON for model config {}", e.getId());
            }
        }
        return r;
    }
}
