package com.aicoding.platform.orchestration.dto;

public class CodeIndexSummaryResponse {

    private String projectId;
    private int fileCount;
    private int symbolCount;
    private int chunkCount;
    private String indexedAt;
    private boolean mock;

    public CodeIndexSummaryResponse() {}

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    public int getFileCount() { return fileCount; }
    public void setFileCount(int fileCount) { this.fileCount = fileCount; }

    public int getSymbolCount() { return symbolCount; }
    public void setSymbolCount(int symbolCount) { this.symbolCount = symbolCount; }

    public int getChunkCount() { return chunkCount; }
    public void setChunkCount(int chunkCount) { this.chunkCount = chunkCount; }

    public String getIndexedAt() { return indexedAt; }
    public void setIndexedAt(String indexedAt) { this.indexedAt = indexedAt; }

    public boolean isMock() { return mock; }
    public void setMock(boolean mock) { this.mock = mock; }
}
