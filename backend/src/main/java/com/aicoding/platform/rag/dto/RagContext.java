package com.aicoding.platform.rag.dto;

import java.util.ArrayList;
import java.util.List;

public class RagContext {

    private String query;
    private String contextText;
    private List<RagReference> references;
    private long total;
    private long elapsedMs;

    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }

    public String getContextText() { return contextText; }
    public void setContextText(String contextText) { this.contextText = contextText; }

    public List<RagReference> getReferences() { return references; }
    public void setReferences(List<RagReference> references) { this.references = references; }

    public long getTotal() { return total; }
    public void setTotal(long total) { this.total = total; }

    public long getElapsedMs() { return elapsedMs; }
    public void setElapsedMs(long elapsedMs) { this.elapsedMs = elapsedMs; }

    public static RagContext empty(String query) {
        RagContext ctx = new RagContext();
        ctx.setQuery(query);
        ctx.setContextText("");
        ctx.setReferences(new ArrayList<>());
        ctx.setTotal(0);
        ctx.setElapsedMs(0);
        return ctx;
    }
}
