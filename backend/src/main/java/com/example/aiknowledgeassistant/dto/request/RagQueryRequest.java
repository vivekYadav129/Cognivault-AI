package com.example.aiknowledgeassistant.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public class RagQueryRequest {

    @NotBlank(message = "Query cannot be blank")
    private String query;

    @Min(value = 1, message = "topK must be at least 1")
    @Max(value = 20, message = "topK cannot exceed 20")
    private Integer topK;

    private Long documentId;

    public RagQueryRequest() {
    }

    public RagQueryRequest(String query, Integer topK, Long documentId) {
        this.query = query;
        this.topK = topK;
        this.documentId = documentId;
    }

    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }

    public Integer getTopK() { return topK; }
    public void setTopK(Integer topK) { this.topK = topK; }

    public Long getDocumentId() { return documentId; }
    public void setDocumentId(Long documentId) { this.documentId = documentId; }
}
