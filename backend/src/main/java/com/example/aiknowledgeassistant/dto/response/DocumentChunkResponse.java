package com.example.aiknowledgeassistant.dto.response;

import java.time.LocalDateTime;

public class DocumentChunkResponse {
    private Long id;
    private Long documentId;
    private Integer chunkIndex;
    private String content;
    private Integer characterCount;
    private LocalDateTime createdAt;

    public DocumentChunkResponse() {
    }

    public DocumentChunkResponse(Long id, Long documentId, Integer chunkIndex, String content, Integer characterCount, LocalDateTime createdAt) {
        this.id = id;
        this.documentId = documentId;
        this.chunkIndex = chunkIndex;
        this.content = content;
        this.characterCount = characterCount;
        this.createdAt = createdAt;
    }

    public static DocumentChunkResponseBuilder builder() {
        return new DocumentChunkResponseBuilder();
    }

    public static class DocumentChunkResponseBuilder {
        private Long id;
        private Long documentId;
        private Integer chunkIndex;
        private String content;
        private Integer characterCount;
        private LocalDateTime createdAt;

        public DocumentChunkResponseBuilder id(Long id) { this.id = id; return this; }
        public DocumentChunkResponseBuilder documentId(Long documentId) { this.documentId = documentId; return this; }
        public DocumentChunkResponseBuilder chunkIndex(Integer chunkIndex) { this.chunkIndex = chunkIndex; return this; }
        public DocumentChunkResponseBuilder content(String content) { this.content = content; return this; }
        public DocumentChunkResponseBuilder characterCount(Integer characterCount) { this.characterCount = characterCount; return this; }
        public DocumentChunkResponseBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public DocumentChunkResponse build() {
            return new DocumentChunkResponse(id, documentId, chunkIndex, content, characterCount, createdAt);
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getDocumentId() { return documentId; }
    public void setDocumentId(Long documentId) { this.documentId = documentId; }

    public Integer getChunkIndex() { return chunkIndex; }
    public void setChunkIndex(Integer chunkIndex) { this.chunkIndex = chunkIndex; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Integer getCharacterCount() { return characterCount; }
    public void setCharacterCount(Integer characterCount) { this.characterCount = characterCount; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
