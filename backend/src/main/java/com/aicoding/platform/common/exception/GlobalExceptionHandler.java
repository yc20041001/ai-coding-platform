package com.aicoding.platform.common.exception;

import com.aicoding.platform.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BizException.class)
    public ResponseEntity<ApiResponse<Void>> handleBizException(BizException ex, HttpServletRequest request) {
        log.warn("request failed, traceId={}, path={}, code={}, message={}",
                MDC.get("traceId"), request.getRequestURI(), ex.getErrorCode().getCode(), ex.getMessage());
        ApiResponse<Void> body = ex.getDetails() != null
                ? ApiResponse.error(ex.getErrorCode(), ex.getMessage(), ex.getDetails())
                : ApiResponse.error(ex.getErrorCode(), ex.getMessage());
        return status(ex.getErrorCode()).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException ex,
                                                                         HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("参数校验失败");
        log.warn("request failed, traceId={}, path={}, code={}, message={}",
                MDC.get("traceId"), request.getRequestURI(), ErrorCode.VALIDATION_ERROR.getCode(), message);
        return status(ErrorCode.VALIDATION_ERROR)
                .body(ApiResponse.error(ErrorCode.VALIDATION_ERROR, message));
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(Exception ex, HttpServletRequest request) {
        log.warn("request failed, traceId={}, path={}, code={}, message={}",
                MDC.get("traceId"), request.getRequestURI(), ErrorCode.FORBIDDEN.getCode(), ex.getMessage());
        return status(ErrorCode.FORBIDDEN)
                .body(ApiResponse.error(ErrorCode.FORBIDDEN));
    }

    @ExceptionHandler(org.springframework.security.core.AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthenticationException(Exception ex, HttpServletRequest request) {
        log.debug("request failed, traceId={}, path={}, code={}, message={}",
                MDC.get("traceId"), request.getRequestURI(), ErrorCode.UNAUTHORIZED.getCode(), ex.getMessage());
        return status(ErrorCode.UNAUTHORIZED)
                .body(ApiResponse.error(ErrorCode.UNAUTHORIZED));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception ex, HttpServletRequest request) {
        log.error("request failed, traceId={}, path={}, code={}, message={}",
                MDC.get("traceId"), request.getRequestURI(), ErrorCode.INTERNAL_ERROR.getCode(), ex.getMessage(), ex);
        return status(ErrorCode.INTERNAL_ERROR)
                .body(ApiResponse.error(ErrorCode.INTERNAL_ERROR, "系统内部错误"));
    }

    private ResponseEntity.BodyBuilder status(ErrorCode errorCode) {
        return ResponseEntity.status(errorCode.getHttpStatus().value());
    }
}
