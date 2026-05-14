package com.aicoding.platform.common.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    // 4xx 客户端错误
    BAD_REQUEST("BAD_REQUEST", "请求参数错误", HttpStatus.BAD_REQUEST),
    UNAUTHORIZED("UNAUTHORIZED", "未登录或 Token 无效", HttpStatus.UNAUTHORIZED),
    FORBIDDEN("FORBIDDEN", "无操作权限", HttpStatus.FORBIDDEN),
    PROJECT_ACCESS_DENIED("PROJECT_ACCESS_DENIED", "无项目访问权限", HttpStatus.FORBIDDEN),
    NOT_FOUND("NOT_FOUND", "资源不存在", HttpStatus.NOT_FOUND),
    CONFLICT("CONFLICT", "资源冲突", HttpStatus.CONFLICT),
    VALIDATION_ERROR("VALIDATION_ERROR", "参数校验失败", HttpStatus.UNPROCESSABLE_ENTITY),
    RATE_LIMITED("RATE_LIMITED", "请求过于频繁", HttpStatus.TOO_MANY_REQUESTS),

    // 5xx 服务端错误
    INTERNAL_ERROR("INTERNAL_ERROR", "系统内部错误", HttpStatus.INTERNAL_SERVER_ERROR),
    AI_PROVIDER_ERROR("AI_PROVIDER_ERROR", "模型供应商调用失败", HttpStatus.BAD_GATEWAY),
    AI_PROVIDER_TIMEOUT("AI_PROVIDER_TIMEOUT", "模型供应商调用超时", HttpStatus.GATEWAY_TIMEOUT);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;

    ErrorCode(String code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
