package com.aicoding.platform.modelgateway.application;

import com.aicoding.platform.modelgateway.dto.ModelRequest;
import com.aicoding.platform.modelgateway.dto.ModelResponse;

public interface ModelGateway {
    ModelResponse generate(ModelRequest request);

    void stream(ModelRequest request, ModelStreamCallback callback);
}
