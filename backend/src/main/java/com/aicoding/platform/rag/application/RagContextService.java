package com.aicoding.platform.rag.application;

import com.aicoding.platform.rag.config.RagProperties;
import com.aicoding.platform.rag.dto.RagContext;
import com.aicoding.platform.rag.dto.RagReference;
import com.aicoding.platform.rag.dto.RagSearchRequest;
import com.aicoding.platform.rag.dto.RagSearchResponse;
import com.aicoding.platform.rag.dto.RagSearchResultResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class RagContextService {

    private static final Logger log = LoggerFactory.getLogger(RagContextService.class);

    private final RagSearchApplicationService ragSearchApplicationService;
    private final RagProperties ragProperties;

    public RagContextService(RagSearchApplicationService ragSearchApplicationService,
                            RagProperties ragProperties) {
        this.ragSearchApplicationService = ragSearchApplicationService;
        this.ragProperties = ragProperties;
    }

    public RagContext buildContextForChat(Long projectId, String query, String knowledgeBaseId,
                                          Integer limit, Boolean useRag) {
        if (!ragProperties.isEnabled() || !ragProperties.isChatEnabled()) {
            log.debug("RAG disabled for chat: globalEnabled={}, chatEnabled={}",
                    ragProperties.isEnabled(), ragProperties.isChatEnabled());
            return RagContext.empty(query);
        }
        if (Boolean.FALSE.equals(useRag)) {
            log.debug("RAG explicitly disabled for this chat message");
            return RagContext.empty(query);
        }
        return doSearch(projectId, query, knowledgeBaseId, limit);
    }

    public RagContext buildContextForTask(Long projectId, String query, String knowledgeBaseId,
                                          Integer limit, Boolean useRag) {
        if (!ragProperties.isEnabled() || !ragProperties.isAgentEnabled()) {
            log.debug("RAG disabled for agent: globalEnabled={}, agentEnabled={}",
                    ragProperties.isEnabled(), ragProperties.isAgentEnabled());
            return RagContext.empty(query);
        }
        if (Boolean.FALSE.equals(useRag)) {
            log.debug("RAG explicitly disabled for this task execution");
            return RagContext.empty(query);
        }
        return doSearch(projectId, query, knowledgeBaseId, limit);
    }

    private RagContext doSearch(Long projectId, String query, String knowledgeBaseId, Integer limit) {
        if (query == null || query.isBlank()) {
            return RagContext.empty(query);
        }

        int effectiveLimit = limit != null ? limit : ragProperties.getDefaultLimit();

        try {
            RagSearchRequest searchRequest = new RagSearchRequest();
            searchRequest.setQuery(query);
            searchRequest.setKnowledgeBaseId(knowledgeBaseId);
            searchRequest.setLimit(effectiveLimit);
            searchRequest.setIncludeContent(true);

            RagSearchResponse response = ragSearchApplicationService.search(projectId, searchRequest);

            List<RagReference> references = toRagReferences(response.getResults());
            String contextText = buildContextText(references);

            RagContext ctx = new RagContext();
            ctx.setQuery(query);
            ctx.setContextText(contextText);
            ctx.setReferences(references);
            ctx.setTotal(response.getTotal());
            ctx.setElapsedMs(response.getElapsedMs());
            return ctx;
        } catch (Exception e) {
            log.warn("RAG search failed for projectId={}, query={}, keeping chat/task alive",
                    projectId, query, e);
            return RagContext.empty(query);
        }
    }

    private List<RagReference> toRagReferences(List<RagSearchResultResponse> results) {
        if (results == null || results.isEmpty()) {
            return new ArrayList<>();
        }
        List<RagReference> refs = new ArrayList<>();
        for (RagSearchResultResponse r : results) {
            RagReference ref = new RagReference();
            ref.setChunkId(r.getChunkId());
            ref.setDocumentId(r.getDocumentId());
            ref.setKnowledgeBaseId(r.getKnowledgeBaseId());
            ref.setTitle(r.getTitle());
            ref.setFilePath(r.getFilePath());
            ref.setScore(r.getScore());
            ref.setSnippet(truncateContent(r.getContent(), ragProperties.getMaxChunkChars()));
            ref.setReferenceType(r.getReferenceType());
            ref.setStartLine(r.getStartLine());
            ref.setEndLine(r.getEndLine());
            refs.add(ref);
        }
        return refs;
    }

    private String buildContextText(List<RagReference> references) {
        if (references.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("以下是从项目知识库检索到的相关上下文，仅供参考：\n");

        int totalChars = sb.length();
        int maxChars = ragProperties.getMaxContextChars();
        int count = 0;

        for (RagReference ref : references) {
            count++;
            String header = "\n[Reference " + count + "]\n"
                    + "Title: " + (ref.getTitle() != null ? ref.getTitle() : "") + "\n"
                    + "File: " + (ref.getFilePath() != null ? ref.getFilePath() : "") + "\n"
                    + "Score: " + (ref.getScore() != null ? ref.getScore().toString() : "") + "\n"
                    + "Content:\n";
            String body = ref.getSnippet() != null ? ref.getSnippet() : "";

            int blockLen = header.length() + body.length();
            if (totalChars + blockLen > maxChars) {
                sb.append("\n... [context truncated]");
                break;
            }
            sb.append(header).append(body).append("\n");
            totalChars += blockLen;
        }

        return sb.toString();
    }

    private String truncateContent(String content, int maxLen) {
        if (content == null) {
            return "";
        }
        if (content.length() <= maxLen) {
            return content;
        }
        return content.substring(0, maxLen) + "...";
    }
}
