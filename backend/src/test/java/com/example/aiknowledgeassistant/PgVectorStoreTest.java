package com.example.aiknowledgeassistant;

import com.example.aiknowledgeassistant.service.ai.vector.PgVectorStore;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PgVectorStoreTest {

    @Test
    public void testIdenticalVectorsCosineSimilarityIsOne() {
        float[] v1 = new float[]{1.0f, 2.0f, 3.0f};
        float[] v2 = new float[]{1.0f, 2.0f, 3.0f};

        double similarity = PgVectorStore.calculateCosineSimilarity(v1, v2);
        assertEquals(1.0, similarity, 0.0001);
    }

    @Test
    public void testOrthogonalVectorsCosineSimilarityIsZero() {
        float[] v1 = new float[]{1.0f, 0.0f};
        float[] v2 = new float[]{0.0f, 1.0f};

        double similarity = PgVectorStore.calculateCosineSimilarity(v1, v2);
        assertEquals(0.0, similarity, 0.0001);
    }

    @Test
    public void testOppositeVectorsCosineSimilarityIsNegativeOne() {
        float[] v1 = new float[]{1.0f, 2.0f};
        float[] v2 = new float[]{-1.0f, -2.0f};

        double similarity = PgVectorStore.calculateCosineSimilarity(v1, v2);
        assertEquals(-1.0, similarity, 0.0001);
    }
}
