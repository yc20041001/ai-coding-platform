package com.aicoding.platform.common.controller;

import com.aicoding.platform.common.exception.ErrorCode;
import com.aicoding.platform.common.response.ApiResponse;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CustomErrorController implements ErrorController {

    @RequestMapping("/error")
    public ResponseEntity<ApiResponse<Void>> handleError(HttpServletRequest request) {
        Integer statusCode = (Integer) request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        String requestUri = (String) request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);

        if (statusCode == null) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error(ErrorCode.INTERNAL_ERROR));
        }

        return switch (statusCode) {
            case 404 -> ResponseEntity.status(404)
                    .body(ApiResponse.error(ErrorCode.NOT_FOUND,
                            "No handler found for " + requestUri));
            case 405 -> ResponseEntity.status(405)
                    .body(ApiResponse.error(ErrorCode.BAD_REQUEST, "Method not allowed"));
            case 401 -> ResponseEntity.status(401)
                    .body(ApiResponse.error(ErrorCode.UNAUTHORIZED));
            case 403 -> ResponseEntity.status(403)
                    .body(ApiResponse.error(ErrorCode.FORBIDDEN));
            default -> ResponseEntity.status(statusCode)
                    .body(ApiResponse.error(ErrorCode.INTERNAL_ERROR, "HTTP " + statusCode));
        };
    }
}
