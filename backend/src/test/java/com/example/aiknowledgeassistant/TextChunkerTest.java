package com.example.aiknowledgeassistant;

import com.example.aiknowledgeassistant.util.TextChunker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TextChunkerTest {

    private TextChunker textChunker;

    @BeforeEach
    public void setUp() {
        textChunker = new TextChunker(100, 20);
    }

    @Test
    public void testShortTextReturnsSingleChunk() {
        String text = "This is a short text under 100 characters.";
        List<TextChunker.Chunk> chunks = textChunker.chunkText(text);

        assertEquals(1, chunks.size());
        assertEquals(0, chunks.get(0).getIndex());
        assertEquals(text, chunks.get(0).getContent());
    }

    @Test
    public void testLongTextSplitsWithOverlap() {
        String text = "Paragraph one contains information about Java programming. " +
                "Paragraph two discusses Spring Boot framework and microservices architecture. " +
                "Paragraph three covers PostgreSQL database management and vector embeddings for RAG systems.";

        List<TextChunker.Chunk> chunks = textChunker.chunkText(text, 80, 15);

        assertTrue(chunks.size() > 1);
        for (int i = 0; i < chunks.size(); i++) {
            assertEquals(i, chunks.get(i).getIndex());
            assertNotNull(chunks.get(i).getContent());
            assertTrue(chunks.get(i).getContent().length() <= 85);
        }
    }

    @Test
    public void testEmptyTextReturnsEmptyList() {
        List<TextChunker.Chunk> chunks = textChunker.chunkText(null);
        assertTrue(chunks.isEmpty());
    }
}
