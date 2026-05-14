package com.aicoding.platform.modelgateway.application;

import com.aicoding.platform.modelgateway.dto.ModelRequest;
import com.aicoding.platform.modelgateway.dto.ModelResponse;
import com.aicoding.platform.modelgateway.dto.ModelStreamChunk;

public interface ModelProvider {
    String providerType();
    boolean supports(String provider);
    ModelResponse generate(ModelRequest request);

    default boolean supportsStream() {
        return false;
    }

    default void stream(ModelRequest request, ModelStreamCallback callback) {
        ModelResponse response = generate(request);
        if (Boolean.TRUE.equals(response.getSuccess())) {
            ModelStreamChunk chunk = new ModelStreamChunk();
            chunk.setContent(response.getContent());
            chunk.setDone(false);
            chunk.setProvider(response.getProvider());
            chunk.setModelName(response.getModelName());
            callback.onToken(chunk);
            callback.onComplete(response);
        } else {
            callback.onError(response);
        }
    }
}
