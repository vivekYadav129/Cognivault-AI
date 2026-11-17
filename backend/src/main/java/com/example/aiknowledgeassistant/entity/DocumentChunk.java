package com.example.aiknowledgeassistant.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Arrays;

@Entity
@Table(name = "document_chunks", indexes = {
    @Index(name = "idx_document_chunks_doc_id", columnList = "document_id"),
    @Index(name = "idx_document_chunks_index", columnList = "document_id, chunk_index")
})
public class DocumentChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @Column(name = "chunk_index", nullable = false)
    private Integer chunkIndex;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "character_count", nullable = false)
    private Integer characterCount;

    @Column(name = "embedding", columnDefinition = "TEXT")
    private String embeddingData; // Stores JSON/CSV vector representation

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public DocumentChunk() {
    }

    public DocumentChunk(Long id, Document document, Integer chunkIndex, String content, Integer characterCount, String embeddingData, LocalDateTime createdAt) {
        this.id = id;
        this.document = document;
        this.chunkIndex = chunkIndex;
        this.content = content;
        this.characterCount = characterCount;
        this.embeddingData = embeddingData;
        this.createdAt = createdAt;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.characterCount == null && this.content != null) {
            this.characterCount = this.content.length();
        }
    }

    public float[] getEmbedding() {
        if (embeddingData == null || embeddingData.isBlank()) {
            return null;
        }
        String[] parts = embeddingData.split(",");
        float[] vector = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            vector[i] = Float.parseFloat(parts[i].trim());
        }
        return vector;
    }

    public void setEmbedding(float[] vector) {
        if (vector == null || vector.length == 0) {
            this.embeddingData = null;
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < vector.length; i++) {
            sb.append(vector[i]);
            if (i < vector.length - 1) {
                sb.append(",");
            }
        }
        this.embeddingData = sb.toString();
    }

    public static DocumentChunkBuilder builder() {
        return new DocumentChunkBuilder();
    }

    public static class DocumentChunkBuilder {
        private Long id;
        private Document document;
        private Integer chunkIndex;
        private String content;
        private Integer characterCount;
        private String embeddingData;
        private LocalDateTime createdAt;

        public DocumentChunkBuilder id(Long id) { this.id = id; return this; }
        public DocumentChunkBuilder document(Document document) { this.document = document; return this; }
        public DocumentChunkBuilder chunkIndex(Integer chunkIndex) { this.chunkIndex = chunkIndex; return this; }
        public DocumentChunkBuilder content(String content) { this.content = content; return this; }
        public DocumentChunkBuilder characterCount(Integer characterCount) { this.characterCount = characterCount; return this; }
        public DocumentChunkBuilder embeddingData(String embeddingData) { this.embeddingData = embeddingData; return this; }
        public DocumentChunkBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public DocumentChunk build() {
            return new DocumentChunk(id, document, chunkIndex, content, characterCount, embeddingData, createdAt);
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Document getDocument() { return document; }
    public void setDocument(Document document) { this.document = document; }

    public Integer getChunkIndex() { return chunkIndex; }
    public void setChunkIndex(Integer chunkIndex) { this.chunkIndex = chunkIndex; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Integer getCharacterCount() { return characterCount; }
    public void setCharacterCount(Integer characterCount) { this.characterCount = characterCount; }

    public String getEmbeddingData() { return embeddingData; }
    public void setEmbeddingData(String embeddingData) { this.embeddingData = embeddingData; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
