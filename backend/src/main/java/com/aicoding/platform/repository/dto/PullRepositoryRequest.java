package com.aicoding.platform.repository.dto;

public class PullRepositoryRequest {

    private String branch = "main";

    public String getBranch() { return branch; }
    public void setBranch(String branch) { this.branch = branch; }
}
