package com.aicoding.platform.orchestration.dto;

import java.util.List;

public class CodeSearchResponse {

    private List<CodeSearchResultResponse> results;
    private int totalCount;
    private String keyword;
    private String searchType;

    public CodeSearchResponse() {}

    public CodeSearchResponse(List<CodeSearchResultResponse> results, int totalCount, String keyword, String searchType) {
        this.results = results;
        this.totalCount = totalCount;
        this.keyword = keyword;
        this.searchType = searchType;
    }

    public List<CodeSearchResultResponse> getResults() { return results; }
    public void setResults(List<CodeSearchResultResponse> results) { this.results = results; }

    public int getTotalCount() { return totalCount; }
    public void setTotalCount(int totalCount) { this.totalCount = totalCount; }

    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }

    public String getSearchType() { return searchType; }
    public void setSearchType(String searchType) { this.searchType = searchType; }
}
