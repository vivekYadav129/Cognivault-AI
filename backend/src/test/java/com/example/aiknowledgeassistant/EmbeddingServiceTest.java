package com.example.aiknowledgeassistant;

import com.example.aiknowledgeassistant.service.ai.embedding.OpenAIEmbeddingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class EmbeddingServiceTest {

    private OpenAIEmbeddingService embeddingService;

    @BeforeEach
    public void setUp() {
        // Instantiate offline deterministic mode (blank API key) with 1536 dimensions
        embeddingService = new OpenAIEmbeddingService("", "text-embedding-3-small", 1536, 50);
    }

    @Test
    public void testGetDimension() {
        assertEquals(1536, embeddingService.getDimension());
    }

    @Test
    public void testEmbedSingleText() {
        float[] vector = embeddingService.embed("Java Spring Boot RAG System");

        assertNotNull(vector);
        assertEquals(1536, vector.length);
    }

    @Test
    public void testEmbedBatchTexts() {
        List<String> texts = List.of("TCP/IP Protocol", "PostgreSQL pgvector", "React Vite Tailwind");
        List<float[]> vectors = embeddingService.embed(texts);

        assertEquals(3, vectors.size());
        for (float[] vec : vectors) {
            assertEquals(1536, vec.length);
        }
    }

    @Test
    public void testDeterministicFeatureVectorConsistency() {
        float[] v1 = OpenAIEmbeddingService.generateDeterministicEmbedding("Semantic Search Test", 1536);
        float[] v2 = OpenAIEmbeddingService.generateDeterministicEmbedding("Semantic Search Test", 1536);

        assertArrayEquals(v1, v2);
    }
}
