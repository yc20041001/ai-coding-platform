package com.aicoding.platform.member.dto;

public class ProjectMemberResponse {

    private String userId;
    private String username;
    private String email;
    private String avatar;
    private String role;
    private String status;
    private String joinedTime;

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getJoinedTime() { return joinedTime; }
    public void setJoinedTime(String joinedTime) { this.joinedTime = joinedTime; }
}
