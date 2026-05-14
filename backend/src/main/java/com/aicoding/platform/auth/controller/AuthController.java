package com.aicoding.platform.auth.controller;

import com.aicoding.platform.auth.application.AuthApplicationService;
import com.aicoding.platform.auth.dto.CurrentUserResponse;
import com.aicoding.platform.auth.dto.LoginRequest;
import com.aicoding.platform.auth.dto.LoginResponse;
import com.aicoding.platform.auth.dto.RefreshTokenRequest;
import com.aicoding.platform.common.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthApplicationService authApplicationService;

    public AuthController(AuthApplicationService authApplicationService) {
        this.authApplicationService = authApplicationService;
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authApplicationService.login(request));
    }

    @PostMapping("/refresh")
    public ApiResponse<LoginResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ApiResponse.ok(authApplicationService.refresh(request));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        authApplicationService.logout();
        return ApiResponse.ok();
    }

    @GetMapping("/me")
    public ApiResponse<CurrentUserResponse> me() {
        return ApiResponse.ok(authApplicationService.me());
    }
}
