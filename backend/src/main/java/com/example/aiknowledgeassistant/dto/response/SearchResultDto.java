package com.example.aiknowledgeassistant.dto.response;

import java.util.Map;

public class SearchResultDto {
    private Long chunkId;
    private Long documentId;
    private String content;
    private double similarityScore;
    private Map<String, Object> metadata;

    public SearchResultDto() {
    }

    public SearchResultDto(Long chunkId, Long documentId, String content, double similarityScore, Map<String, Object> metadata) {
        this.chunkId = chunkId;
        this.documentId = documentId;
        this.content = content;
        this.similarityScore = similarityScore;
        this.metadata = metadata;
    }

    public Long getChunkId() { return chunkId; }
    public void setChunkId(Long chunkId) { this.chunkId = chunkId; }

    public Long getDocumentId() { return documentId; }
    public void setDocumentId(Long documentId) { this.documentId = documentId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public double getSimilarityScore() { return similarityScore; }
    public void setSimilarityScore(double similarityScore) { this.similarityScore = similarityScore; }

    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
}
