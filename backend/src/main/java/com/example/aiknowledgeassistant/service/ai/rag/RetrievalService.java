package com.example.aiknowledgeassistant.service.ai.rag;

import com.example.aiknowledgeassistant.service.ai.embedding.EmbeddingService;
import com.example.aiknowledgeassistant.service.ai.vector.SearchResult;
import com.example.aiknowledgeassistant.service.ai.vector.VectorStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RetrievalService {

    private static final Logger logger = LoggerFactory.getLogger(RetrievalService.class);

    private final EmbeddingService embeddingService;
    private final VectorStore vectorStore;
    private final int defaultTopK;
    private final double similarityThreshold;

    public RetrievalService(
            EmbeddingService embeddingService,
            VectorStore vectorStore,
            @Value("${cognivault.rag.top-k:5}") int defaultTopK,
            @Value("${cognivault.rag.similarity-threshold:0.05}") double similarityThreshold) {
        this.embeddingService = embeddingService;
        this.vectorStore = vectorStore;
        this.defaultTopK = defaultTopK;
        this.similarityThreshold = similarityThreshold;
    }

    public List<SearchResult> retrieveRelevantChunks(String query, Integer topK, Long userId, Long documentId) {
        int targetK = (topK != null && topK > 0) ? topK : defaultTopK;

        logger.info("RAG Retrieval: userId={}, targetK={}, similarityThreshold={}", userId, targetK, similarityThreshold);

        // 1. Generate query vector
        float[] queryVector = embeddingService.embed(query);

        // 2. Similarity search from vector store with user ownership filtering
        List<SearchResult> rawResults = vectorStore.similaritySearch(queryVector, targetK, userId, documentId);

        if (!rawResults.isEmpty()) {
            double topScore = rawResults.get(0).getSimilarityScore();
            logger.info("Raw vector search returned {} candidates. Top similarity score: {}", rawResults.size(), String.format("%.4f", topScore));
        } else {
            logger.info("Raw vector search returned 0 candidates for userId={}", userId);
        }

        // 3. Filter by similarity threshold
        List<SearchResult> filteredResults = rawResults.stream()
                .filter(res -> res.getSimilarityScore() >= similarityThreshold)
                .collect(Collectors.toList());

        logger.info("RAG Retrieval complete: {} of {} candidates met similarity threshold {}", filteredResults.size(), rawResults.size(), similarityThreshold);

        return filteredResults;
    }

    public double getSimilarityThreshold() {
        return similarityThreshold;
    }
}
