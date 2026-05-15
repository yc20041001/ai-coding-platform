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
    AI_PROVIDER_TIMEOUT("AI_PROVIDER_TIMEOUT", "模型供应商调用超时", HttpStatus.GATEWAY_TIMEOUT),

    // GitHub 集成错误
    GITHUB_OAUTH_NOT_CONFIGURED("GITHUB_OAUTH_NOT_CONFIGURED", "GitHub OAuth 未配置", HttpStatus.SERVICE_UNAVAILABLE),
    GITHUB_STATE_INVALID("GITHUB_STATE_INVALID", "OAuth state 无效或已过期", HttpStatus.BAD_REQUEST),
    GITHUB_TOKEN_EXCHANGE_FAILED("GITHUB_TOKEN_EXCHANGE_FAILED", "GitHub Token 交换失败", HttpStatus.BAD_GATEWAY),
    GITHUB_TOKEN_MISSING("GITHUB_TOKEN_MISSING", "用户未绑定 GitHub 账号", HttpStatus.UNAUTHORIZED),
    GITHUB_AUTH_FAILED("GITHUB_AUTH_FAILED", "GitHub API 认证失败", HttpStatus.BAD_GATEWAY),
    GITHUB_RATE_LIMITED("GITHUB_RATE_LIMITED", "GitHub API 频率限制", HttpStatus.TOO_MANY_REQUESTS),
    GITHUB_REPO_NOT_FOUND("GITHUB_REPO_NOT_FOUND", "GitHub 仓库不存在", HttpStatus.NOT_FOUND),
    GITHUB_PR_NOT_FOUND("GITHUB_PR_NOT_FOUND", "Pull Request 不存在", HttpStatus.NOT_FOUND),
    GITHUB_API_ERROR("GITHUB_API_ERROR", "GitHub API 调用失败", HttpStatus.BAD_GATEWAY),
    GITHUB_BAD_RESPONSE("GITHUB_BAD_RESPONSE", "GitHub API 返回异常", HttpStatus.BAD_GATEWAY);

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
