package com.aicoding.platform.common.response;

import com.aicoding.platform.common.exception.ErrorCode;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.OffsetDateTime;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private final String code;
    private final String message;
    private final T data;
    private final String traceId;
    private final String timestamp;
    private final Object details;

    private ApiResponse(String code, String message, T data, String traceId, Object details) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.traceId = traceId;
        this.timestamp = OffsetDateTime.now().toString();
        this.details = details;
    }

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>("OK", "success", data, currentTraceId(), null);
    }

    public static <T> ApiResponse<T> ok() {
        return ok(null);
    }

    public static <T> ApiResponse<T> error(ErrorCode errorCode) {
        return new ApiResponse<>(errorCode.getCode(), errorCode.getMessage(), null, currentTraceId(), null);
    }

    public static <T> ApiResponse<T> error(ErrorCode errorCode, String message) {
        return new ApiResponse<>(errorCode.getCode(), message, null, currentTraceId(), null);
    }

    public static <T> ApiResponse<T> error(ErrorCode errorCode, String message, Object details) {
        return new ApiResponse<>(errorCode.getCode(), message, null, currentTraceId(), details);
    }

    private static String currentTraceId() {
        String traceId = org.slf4j.MDC.get("traceId");
        return traceId != null ? traceId : UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }

    public String getTraceId() {
        return traceId;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public Object getDetails() {
        return details;
    }
}
