package com.aicoding.platform.orchestration.dto;

public class CodeSearchRequest {

    private String keyword;
    private String searchType;
    private String branch;
    private String language;
    private String pathPrefix;
    private int limit = 10;

    public CodeSearchRequest() {}

    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }

    public String getSearchType() { return searchType; }
    public void setSearchType(String searchType) { this.searchType = searchType; }

    public String getBranch() { return branch; }
    public void setBranch(String branch) { this.branch = branch; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public String getPathPrefix() { return pathPrefix; }
    public void setPathPrefix(String pathPrefix) { this.pathPrefix = pathPrefix; }

    public int getLimit() { return limit; }
    public void setLimit(int limit) { this.limit = limit; }
}
