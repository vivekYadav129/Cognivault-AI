package com.example.aiknowledgeassistant.dto.response;

import com.example.aiknowledgeassistant.entity.DocumentStatus;
import java.time.LocalDateTime;

public class DocumentResponse {
    private Long id;
    private Long userId;
    private String fileName;
    private String originalFileName;
    private String fileType;
    private Long fileSize;
    private DocumentStatus status;
    private Integer pageCount;
    private Integer chunkCount;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public DocumentResponse() {
    }

    public DocumentResponse(Long id, Long userId, String fileName, String originalFileName, String fileType,
                            Long fileSize, DocumentStatus status, Integer pageCount, Integer chunkCount,
                            String errorMessage, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.fileName = fileName;
        this.originalFileName = originalFileName;
        this.fileType = fileType;
        this.fileSize = fileSize;
        this.status = status;
        this.pageCount = pageCount;
        this.chunkCount = chunkCount;
        this.errorMessage = errorMessage;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static DocumentResponseBuilder builder() {
        return new DocumentResponseBuilder();
    }

    public static class DocumentResponseBuilder {
        private Long id;
        private Long userId;
        private String fileName;
        private String originalFileName;
        private String fileType;
        private Long fileSize;
        private DocumentStatus status;
        private Integer pageCount;
        private Integer chunkCount;
        private String errorMessage;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public DocumentResponseBuilder id(Long id) { this.id = id; return this; }
        public DocumentResponseBuilder userId(Long userId) { this.userId = userId; return this; }
        public DocumentResponseBuilder fileName(String fileName) { this.fileName = fileName; return this; }
        public DocumentResponseBuilder originalFileName(String originalFileName) { this.originalFileName = originalFileName; return this; }
        public DocumentResponseBuilder fileType(String fileType) { this.fileType = fileType; return this; }
        public DocumentResponseBuilder fileSize(Long fileSize) { this.fileSize = fileSize; return this; }
        public DocumentResponseBuilder status(DocumentStatus status) { this.status = status; return this; }
        public DocumentResponseBuilder pageCount(Integer pageCount) { this.pageCount = pageCount; return this; }
        public DocumentResponseBuilder chunkCount(Integer chunkCount) { this.chunkCount = chunkCount; return this; }
        public DocumentResponseBuilder errorMessage(String errorMessage) { this.errorMessage = errorMessage; return this; }
        public DocumentResponseBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public DocumentResponseBuilder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public DocumentResponse build() {
            return new DocumentResponse(id, userId, fileName, originalFileName, fileType, fileSize, status, pageCount, chunkCount, errorMessage, createdAt, updatedAt);
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getOriginalFileName() { return originalFileName; }
    public void setOriginalFileName(String originalFileName) { this.originalFileName = originalFileName; }

    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }

    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }

    public DocumentStatus getStatus() { return status; }
    public void setStatus(DocumentStatus status) { this.status = status; }

    public Integer getPageCount() { return pageCount; }
    public void setPageCount(Integer pageCount) { this.pageCount = pageCount; }

    public Integer getChunkCount() { return chunkCount; }
    public void setChunkCount(Integer chunkCount) { this.chunkCount = chunkCount; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
