package com.aicoding.platform.modelgateway.application;

import com.aicoding.platform.modelgateway.dto.ModelResponse;
import com.aicoding.platform.modelgateway.dto.ModelStreamChunk;

public interface ModelStreamCallback {

    void onToken(ModelStreamChunk chunk);

    void onComplete(ModelResponse response);

    void onError(ModelResponse errorResponse);
}
