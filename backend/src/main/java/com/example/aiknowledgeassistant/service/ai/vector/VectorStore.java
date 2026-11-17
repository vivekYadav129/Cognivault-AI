package com.example.aiknowledgeassistant.service.ai.vector;

import com.example.aiknowledgeassistant.entity.DocumentChunk;

import java.util.List;

public interface VectorStore {
    void save(DocumentChunk chunk, float[] embedding);
    void saveAll(List<DocumentChunk> chunks, List<float[]> embeddings);
    List<SearchResult> similaritySearch(float[] queryVector, int topK, Long userId, Long documentId);
    void deleteByDocumentId(Long documentId);
}
