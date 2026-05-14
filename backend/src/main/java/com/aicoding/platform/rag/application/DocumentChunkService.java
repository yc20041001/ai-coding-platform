package com.aicoding.platform.rag.application;

import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

@Service
public class DocumentChunkService {

    public List<String> splitIntoChunks(String content, int chunkSize, int chunkOverlap) {
        if (content == null || content.isEmpty()) {
            return new ArrayList<>();
        }
        if (chunkOverlap >= chunkSize) {
            chunkOverlap = chunkSize / 2;
        }
        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < content.length()) {
            int end = Math.min(start + chunkSize, content.length());
            chunks.add(content.substring(start, end));
            if (end >= content.length()) {
                break;
            }
            start = end - chunkOverlap;
        }
        return chunks;
    }

    public long estimateTokens(String content) {
        if (content == null || content.isEmpty()) {
            return 0L;
        }
        return Math.max(1, content.length() / 3);
    }

    public String mockEmbedding(String content) {
        String contentHash = hashContent(content);
        long tokenCount = estimateTokens(content);
        return "mock-embedding:" + contentHash + ":" + tokenCount;
    }

    public String hashContent(String content) {
        if (content == null || content.isEmpty()) {
            return "empty";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            return Integer.toHexString(content.hashCode());
        }
    }
}
