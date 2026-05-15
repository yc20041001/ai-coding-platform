package com.aicoding.platform.modelgateway.domain;

/**
 * Enhanced model gateway error codes for production hardening.
 * Extends the concepts from {@link ModelGatewayErrorType} with more granular codes.
 */
public enum ModelGatewayErrorCode {
    CONFIG_MISSING,
    API_KEY_MISSING,
    PROVIDER_NOT_FOUND,
    MODEL_NOT_FOUND,
    PROMPT_BLOCKED,
    RATE_LIMITED,
    TIMEOUT,
    NETWORK_ERROR,
    AUTH_FAILED,
    BAD_RESPONSE,
    STREAM_INTERRUPTED,
    FALLBACK_USED,
    UNKNOWN;

    /**
     * Map from existing ModelGatewayErrorType to the new error code.
     */
    public static ModelGatewayErrorCode fromErrorType(String errorType) {
        if (errorType == null) return UNKNOWN;
        return switch (errorType.toUpperCase()) {
            case "CONFIG_ERROR" -> CONFIG_MISSING;
            case "AUTH_ERROR" -> AUTH_FAILED;
            case "RATE_LIMIT" -> RATE_LIMITED;
            case "TIMEOUT" -> TIMEOUT;
            case "PROVIDER_ERROR" -> BAD_RESPONSE;
            case "NETWORK_ERROR" -> NETWORK_ERROR;
            case "SAFETY_REJECTED" -> PROMPT_BLOCKED;
            case "UNKNOWN" -> UNKNOWN;
            default -> {
                try {
                    yield ModelGatewayErrorCode.valueOf(errorType.toUpperCase());
                } catch (IllegalArgumentException e) {
                    yield UNKNOWN;
                }
            }
        };
    }
}
