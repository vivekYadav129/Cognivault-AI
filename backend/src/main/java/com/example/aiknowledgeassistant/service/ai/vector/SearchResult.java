package com.example.aiknowledgeassistant.service.ai.vector;

import java.util.HashMap;
import java.util.Map;

public class SearchResult {
    private final Long chunkId;
    private final Long documentId;
    private final String content;
    private final double similarityScore;
    private final Integer chunkIndex;
    private final String fileName;
    private final Long userId;
    private final Integer pageNumber;

    public SearchResult(Long chunkId, Long documentId, String content, double similarityScore,
                        Integer chunkIndex, String fileName, Long userId, Integer pageNumber) {
        this.chunkId = chunkId;
        this.documentId = documentId;
        this.content = content;
        this.similarityScore = similarityScore;
        this.chunkIndex = chunkIndex;
        this.fileName = fileName;
        this.userId = userId;
        this.pageNumber = pageNumber;
    }

    public Map<String, Object> getMetadata() {
        Map<String, Object> meta = new HashMap<>();
        meta.put("fileName", fileName);
        meta.put("chunkIndex", chunkIndex);
        if (pageNumber != null) {
            meta.put("pageNumber", pageNumber);
        }
        meta.put("userId", userId);
        return meta;
    }

    public Long getChunkId() { return chunkId; }
    public Long getDocumentId() { return documentId; }
    public String getContent() { return content; }
    public double getSimilarityScore() { return similarityScore; }
    public Integer getChunkIndex() { return chunkIndex; }
    public String getFileName() { return fileName; }
    public Long getUserId() { return userId; }
    public Integer getPageNumber() { return pageNumber; }
}
