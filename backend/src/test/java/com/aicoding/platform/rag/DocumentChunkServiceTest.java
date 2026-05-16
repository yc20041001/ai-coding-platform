package com.aicoding.platform.rag;

import com.aicoding.platform.rag.application.DocumentChunkService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentChunkServiceTest {

    private final DocumentChunkService service = new DocumentChunkService();

    // === splitIntoChunks tests ===

    @Test
    void shouldReturnEmptyListForNullContent() {
        List<String> chunks = service.splitIntoChunks(null, 100, 20);
        assertThat(chunks).isEmpty();
    }

    @Test
    void shouldReturnEmptyListForEmptyContent() {
        List<String> chunks = service.splitIntoChunks("", 100, 20);
        assertThat(chunks).isEmpty();
    }

    @Test
    void shouldReturnSingleChunkForShortText() {
        List<String> chunks = service.splitIntoChunks("Hello, world!", 100, 20);
        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0)).isEqualTo("Hello, world!");
    }

    @Test
    void shouldSplitLongTextByChunkSize() {
        String content = "a".repeat(250);
        List<String> chunks = service.splitIntoChunks(content, 100, 20);
        // 250 chars / (100-20) step = 4 chunks (0-99, 80-179, 160-249, 240-250)
        // Actually: 0-100, 80-180, 160-250 → 3 chunks
        assertThat(chunks.size()).isGreaterThanOrEqualTo(2);
        // Each chunk should be <= chunkSize
        for (String chunk : chunks) {
            assertThat(chunk.length()).isLessThanOrEqualTo(100);
        }
    }

    @Test
    void shouldApplyChunkOverlap() {
        String content = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        List<String> chunks = service.splitIntoChunks(content, 10, 4);
        assertThat(chunks.size()).isGreaterThan(1);
        // Second chunk should start with overlap from first
        String first = chunks.get(0);
        String second = chunks.get(1);
        // The overlap means some chars appear in both
        assertThat(first).endsWith(second.substring(0, Math.min(4, second.length())));
    }

    @Test
    void shouldHandleOverlapGreaterThanChunkSize() {
        // Overlap >= chunkSize should be capped to chunkSize/2
        String content = "Hello World This Is A Test Of Long Overlap Content";
        List<String> chunks = service.splitIntoChunks(content, 10, 20);
        // Should not throw, overlap capped to 5
        assertThat(chunks).isNotEmpty();
    }

    @Test
    void shouldHandleContentExactlyAtChunkSize() {
        String content = "1234567890"; // exactly 10 chars
        List<String> chunks = service.splitIntoChunks(content, 10, 2);
        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0)).isEqualTo("1234567890");
    }

    // === estimateTokens tests ===

    @Test
    void shouldReturnZeroForNullContent() {
        assertThat(service.estimateTokens(null)).isEqualTo(0L);
    }

    @Test
    void shouldReturnZeroForEmptyContent() {
        assertThat(service.estimateTokens("")).isEqualTo(0L);
    }

    @Test
    void shouldReturnAtLeastOneForNonEmptyContent() {
        // 1 char / 3 = 0 → max(1,0) = 1
        assertThat(service.estimateTokens("a")).isEqualTo(1L);
    }

    @Test
    void shouldEstimateTokensByLength() {
        // 300 chars / 3 = 100 tokens
        assertThat(service.estimateTokens("a".repeat(300))).isEqualTo(100L);
    }

    // === hashContent tests ===

    @Test
    void shouldReturnEmptyForNullContent() {
        assertThat(service.hashContent(null)).isEqualTo("empty");
    }

    @Test
    void shouldReturnEmptyForBlankContent() {
        assertThat(service.hashContent("")).isEqualTo("empty");
    }

    @Test
    void shouldProduceStableHash() {
        String hash1 = service.hashContent("Hello, World!");
        String hash2 = service.hashContent("Hello, World!");
        assertThat(hash1).isEqualTo(hash2);
        assertThat(hash1).isNotEmpty();
        assertThat(hash1).isNotEqualTo("empty");
    }

    @Test
    void shouldProduceDifferentHashForDifferentContent() {
        String hash1 = service.hashContent("Hello");
        String hash2 = service.hashContent("World");
        assertThat(hash1).isNotEqualTo(hash2);
    }

    // === mockEmbedding tests ===

    @Test
    void shouldProduceMockEmbedding() {
        String embedding = service.mockEmbedding("Test content for embedding");
        assertThat(embedding).startsWith("mock-embedding:");
        assertThat(embedding).contains(":");
    }
}
