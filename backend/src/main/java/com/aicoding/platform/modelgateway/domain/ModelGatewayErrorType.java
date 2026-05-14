package com.aicoding.platform.modelgateway.domain;

public enum ModelGatewayErrorType {
    CONFIG_ERROR,
    AUTH_ERROR,
    RATE_LIMIT,
    TIMEOUT,
    PROVIDER_ERROR,
    NETWORK_ERROR,
    SAFETY_REJECTED,
    UNKNOWN
}
