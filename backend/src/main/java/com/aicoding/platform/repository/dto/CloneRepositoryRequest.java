package com.aicoding.platform.repository.dto;

public class CloneRepositoryRequest {

    private String branch = "main";
    private boolean force;

    public String getBranch() { return branch; }
    public void setBranch(String branch) { this.branch = branch; }

    public boolean isForce() { return force; }
    public void setForce(boolean force) { this.force = force; }
}
