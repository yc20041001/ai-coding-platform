package com.aicoding.platform.repository.dto;

import java.util.List;

public class RepositoryDiffResponse {

    private String base;
    private String head;
    private List<DiffFile> files;

    public String getBase() { return base; }
    public void setBase(String base) { this.base = base; }

    public String getHead() { return head; }
    public void setHead(String head) { this.head = head; }

    public List<DiffFile> getFiles() { return files; }
    public void setFiles(List<DiffFile> files) { this.files = files; }

    public static class DiffFile {
        private String path;
        private String changeType;
        private int additions;
        private int deletions;
        private String patch;

        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }

        public String getChangeType() { return changeType; }
        public void setChangeType(String changeType) { this.changeType = changeType; }

        public int getAdditions() { return additions; }
        public void setAdditions(int additions) { this.additions = additions; }

        public int getDeletions() { return deletions; }
        public void setDeletions(int deletions) { this.deletions = deletions; }

        public String getPatch() { return patch; }
        public void setPatch(String patch) { this.patch = patch; }
    }
}
