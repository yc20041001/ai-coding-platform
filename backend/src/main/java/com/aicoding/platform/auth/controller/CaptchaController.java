package com.aicoding.platform.auth.controller;

import com.aicoding.platform.auth.application.CaptchaService;
import com.aicoding.platform.auth.dto.CaptchaResponse;
import com.aicoding.platform.common.response.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class CaptchaController {

    private final CaptchaService captchaService;

    public CaptchaController(CaptchaService captchaService) {
        this.captchaService = captchaService;
    }

    @GetMapping("/captcha")
    public ApiResponse<CaptchaResponse> captcha() {
        CaptchaResponse response = captchaService.generate();
        return ApiResponse.ok(response);
    }
}