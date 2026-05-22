package com.aicoding.platform.orchestration.dto;

import java.util.List;

public class RepositoryBranchResult {

    private List<String> branches;
    private boolean includeRemote;
    private boolean noCheckout;
    private boolean noPull;

    public List<String> getBranches() { return branches; }
    public void setBranches(List<String> branches) { this.branches = branches; }

    public boolean isIncludeRemote() { return includeRemote; }
    public void setIncludeRemote(boolean includeRemote) { this.includeRemote = includeRemote; }

    public boolean isNoCheckout() { return noCheckout; }
    public void setNoCheckout(boolean noCheckout) { this.noCheckout = noCheckout; }

    public boolean isNoPull() { return noPull; }
    public void setNoPull(boolean noPull) { this.noPull = noPull; }
}
