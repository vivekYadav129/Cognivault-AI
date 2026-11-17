package com.example.aiknowledgeassistant.service.processor;

public class ExtractedDocumentData {
    private final String text;
    private final Integer pageCount;

    public ExtractedDocumentData(String text, Integer pageCount) {
        this.text = text;
        this.pageCount = pageCount;
    }

    public String getText() { return text; }
    public Integer getPageCount() { return pageCount; }
}
