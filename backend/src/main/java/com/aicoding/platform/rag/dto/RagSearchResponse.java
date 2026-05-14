package com.aicoding.platform.rag.dto;

import java.util.List;

public class RagSearchResponse {

    private String query;
    private List<RagSearchResultResponse> results;
    private Long total;
    private Long elapsedMs;

    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }

    public List<RagSearchResultResponse> getResults() { return results; }
    public void setResults(List<RagSearchResultResponse> results) { this.results = results; }

    public Long getTotal() { return total; }
    public void setTotal(Long total) { this.total = total; }

    public Long getElapsedMs() { return elapsedMs; }
    public void setElapsedMs(Long elapsedMs) { this.elapsedMs = elapsedMs; }
}
