package com.example.aiknowledgeassistant.dto.response;

import java.util.List;

public class DocumentListResponse {
    private List<DocumentResponse> documents;
    private int count;

    public DocumentListResponse() {
    }

    public DocumentListResponse(List<DocumentResponse> documents) {
        this.documents = documents;
        this.count = documents != null ? documents.size() : 0;
    }

    public List<DocumentResponse> getDocuments() { return documents; }
    public void setDocuments(List<DocumentResponse> documents) {
        this.documents = documents;
        this.count = documents != null ? documents.size() : 0;
    }

    public int getCount() { return count; }
    public void setCount(int count) { this.count = count; }
}
