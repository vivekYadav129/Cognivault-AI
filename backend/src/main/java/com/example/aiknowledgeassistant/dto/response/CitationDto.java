package com.example.aiknowledgeassistant.dto.response;

public class CitationDto {
    private Long documentId;
    private Long chunkId;
    private String documentName;
    private Integer chunkIndex;
    private Integer pageNumber;
    private double similarityScore;

    public CitationDto() {
    }

    public CitationDto(Long documentId, Long chunkId, String documentName, Integer chunkIndex, Integer pageNumber, double similarityScore) {
        this.documentId = documentId;
        this.chunkId = chunkId;
        this.documentName = documentName;
        this.chunkIndex = chunkIndex;
        this.pageNumber = pageNumber;
        this.similarityScore = similarityScore;
    }

    public static CitationDtoBuilder builder() {
        return new CitationDtoBuilder();
    }

    public static class CitationDtoBuilder {
        private Long documentId;
        private Long chunkId;
        private String documentName;
        private Integer chunkIndex;
        private Integer pageNumber;
        private double similarityScore;

        public CitationDtoBuilder documentId(Long documentId) { this.documentId = documentId; return this; }
        public CitationDtoBuilder chunkId(Long chunkId) { this.chunkId = chunkId; return this; }
        public CitationDtoBuilder documentName(String documentName) { this.documentName = documentName; return this; }
        public CitationDtoBuilder chunkIndex(Integer chunkIndex) { this.chunkIndex = chunkIndex; return this; }
        public CitationDtoBuilder pageNumber(Integer pageNumber) { this.pageNumber = pageNumber; return this; }
        public CitationDtoBuilder similarityScore(double similarityScore) { this.similarityScore = similarityScore; return this; }

        public CitationDto build() {
            return new CitationDto(documentId, chunkId, documentName, chunkIndex, pageNumber, similarityScore);
        }
    }

    public Long getDocumentId() { return documentId; }
    public void setDocumentId(Long documentId) { this.documentId = documentId; }

    public Long getChunkId() { return chunkId; }
    public void setChunkId(Long chunkId) { this.chunkId = chunkId; }

    public String getDocumentName() { return documentName; }
    public void setDocumentName(String documentName) { this.documentName = documentName; }

    public Integer getChunkIndex() { return chunkIndex; }
    public void setChunkIndex(Integer chunkIndex) { this.chunkIndex = chunkIndex; }

    public Integer getPageNumber() { return pageNumber; }
    public void setPageNumber(Integer pageNumber) { this.pageNumber = pageNumber; }

    public double getSimilarityScore() { return similarityScore; }
    public void setSimilarityScore(double similarityScore) { this.similarityScore = similarityScore; }
}
