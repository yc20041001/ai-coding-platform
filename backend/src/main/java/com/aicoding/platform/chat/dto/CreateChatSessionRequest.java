package com.aicoding.platform.chat.dto;

import jakarta.validation.constraints.NotBlank;

public class CreateChatSessionRequest {

    @NotBlank(message = "会话标题不能为空")
    private String title;

    @NotBlank(message = "会话类型不能为空")
    private String sessionType;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getSessionType() { return sessionType; }
    public void setSessionType(String sessionType) { this.sessionType = sessionType; }
}
