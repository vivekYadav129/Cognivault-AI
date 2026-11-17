package com.example.aiknowledgeassistant;

import com.example.aiknowledgeassistant.service.ai.rag.ContextBuilder;
import com.example.aiknowledgeassistant.service.ai.vector.SearchResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ContextBuilderTest {

    private ContextBuilder contextBuilder;

    @BeforeEach
    public void setUp() {
        contextBuilder = new ContextBuilder(500);
    }

    @Test
    public void testBuildContextWithSources() {
        SearchResult res1 = new SearchResult(1L, 10L, "Java Spring Boot Framework", 0.95, 0, "spring-notes.pdf", 1L, 1);
        SearchResult res2 = new SearchResult(2L, 10L, "PostgreSQL Vector Search pgvector", 0.90, 1, "spring-notes.pdf", 1L, 1);

        ContextBuilder.ContextResult result = contextBuilder.buildContext(List.of(res1, res2));

        assertNotNull(result);
        assertTrue(result.getFormattedContext().contains("[S1]"));
        assertTrue(result.getFormattedContext().contains("[S2]"));
        assertTrue(result.getFormattedContext().contains("Java Spring Boot Framework"));
        assertTrue(result.getFormattedContext().contains("PostgreSQL Vector Search pgvector"));

        assertEquals(2, result.getSourceMap().size());
        assertEquals(res1, result.getSourceMap().get("S1"));
        assertEquals(res2, result.getSourceMap().get("S2"));
    }

    @Test
    public void testDeduplicationOfChunks() {
        SearchResult res1 = new SearchResult(1L, 10L, "Duplicate Content", 0.95, 0, "doc1.pdf", 1L, 1);
        SearchResult res2 = new SearchResult(2L, 10L, "Duplicate Content", 0.90, 1, "doc1.pdf", 1L, 1);

        ContextBuilder.ContextResult result = contextBuilder.buildContext(List.of(res1, res2));

        assertEquals(1, result.getSourceMap().size());
        assertTrue(result.getSourceMap().containsKey("S1"));
    }

    @Test
    public void testEmptyChunksList() {
        ContextBuilder.ContextResult result = contextBuilder.buildContext(List.of());
        assertEquals("", result.getFormattedContext());
        assertTrue(result.getSourceMap().isEmpty());
    }
}
