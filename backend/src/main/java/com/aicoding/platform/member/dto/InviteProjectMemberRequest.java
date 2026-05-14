package com.aicoding.platform.member.dto;

import jakarta.validation.constraints.NotBlank;

public class InviteProjectMemberRequest {

    @NotBlank(message = "邮箱不能为空")
    private String email;

    @NotBlank(message = "角色不能为空")
    private String role;

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
