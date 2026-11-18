package com.example.aiknowledgeassistant.service;

import com.example.aiknowledgeassistant.dto.response.DocumentChunkResponse;
import com.example.aiknowledgeassistant.dto.response.DocumentListResponse;
import com.example.aiknowledgeassistant.dto.response.DocumentResponse;
import com.example.aiknowledgeassistant.entity.Document;
import com.example.aiknowledgeassistant.entity.DocumentChunk;
import com.example.aiknowledgeassistant.entity.DocumentStatus;
import com.example.aiknowledgeassistant.entity.User;
import com.example.aiknowledgeassistant.exception.BadRequestException;
import com.example.aiknowledgeassistant.exception.ResourceNotFoundException;
import com.example.aiknowledgeassistant.exception.UnauthorizedException;
import com.example.aiknowledgeassistant.repository.DocumentChunkRepository;
import com.example.aiknowledgeassistant.repository.DocumentRepository;
import com.example.aiknowledgeassistant.repository.QuizRepository;
import com.example.aiknowledgeassistant.repository.UserRepository;
import com.example.aiknowledgeassistant.service.ai.embedding.EmbeddingService;
import com.example.aiknowledgeassistant.service.ai.vector.VectorStore;
import com.example.aiknowledgeassistant.service.processor.DocumentProcessor;
import com.example.aiknowledgeassistant.service.storage.DocumentStorageService;
import com.example.aiknowledgeassistant.util.TextChunker;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DocumentService {

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10 MB
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "txt", "docx");

    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final UserRepository userRepository;
    private final DocumentStorageService storageService;
    private final DocumentProcessor documentProcessor;
    private final EmbeddingService embeddingService;
    private final VectorStore vectorStore;
    private final QuizRepository quizRepository;

    public DocumentService(DocumentRepository documentRepository,
                           DocumentChunkRepository documentChunkRepository,
                           UserRepository userRepository,
                           DocumentStorageService storageService,
                           DocumentProcessor documentProcessor,
                           EmbeddingService embeddingService,
                           VectorStore vectorStore,
                           QuizRepository quizRepository) {
        this.documentRepository = documentRepository;
        this.documentChunkRepository = documentChunkRepository;
        this.userRepository = userRepository;
        this.storageService = storageService;
        this.documentProcessor = documentProcessor;
        this.embeddingService = embeddingService;
        this.vectorStore = vectorStore;
        this.quizRepository = quizRepository;
    }

    @Transactional
    public DocumentResponse uploadAndProcessDocument(MultipartFile file, String userEmail) {
        User user = getUserByEmail(userEmail);
        validateFile(file);

        String originalFileName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "document";
        String fileExtension = getFileExtension(originalFileName).toLowerCase();
        String storedFileName = UUID.randomUUID() + "." + fileExtension;

        // 1. Store file
        String storagePath = storageService.store(file, storedFileName);

        // 2. Create initial document record with status UPLOADED
        Document document = Document.builder()
                .user(user)
                .originalFileName(originalFileName)
                .storedFileName(storedFileName)
                .fileType(fileExtension.toUpperCase())
                .fileSize(file.getSize())
                .storagePath(storagePath)
                .status(DocumentStatus.UPLOADED)
                .build();

        Document savedDoc = documentRepository.save(document);

        // 3. Process document pipeline
        try {
            savedDoc.setStatus(DocumentStatus.PROCESSING);
            documentRepository.save(savedDoc);

            List<DocumentChunk> chunks;
            try (InputStream is = storageService.loadAsStream(storagePath)) {
                DocumentProcessor.ProcessedDocumentResult result = documentProcessor.process(is, fileExtension);

                savedDoc.setExtractedText(result.getCleanedText());
                savedDoc.setPageCount(result.getPageCount());

                // Save Chunks
                chunks = new ArrayList<>();
                for (TextChunker.Chunk chunk : result.getChunks()) {
                    DocumentChunk chunkEntity = DocumentChunk.builder()
                            .document(savedDoc)
                            .chunkIndex(chunk.getIndex())
                            .content(chunk.getContent())
                            .characterCount(chunk.getCharacterCount())
                            .build();
                    chunks.add(chunkEntity);
                }
                chunks = documentChunkRepository.saveAll(chunks);
            }

            // 4. Embedding Generation Stage
            savedDoc.setStatus(DocumentStatus.EMBEDDING);
            documentRepository.save(savedDoc);

            List<String> chunkTexts = chunks.stream()
                    .map(DocumentChunk::getContent)
                    .collect(Collectors.toList());

            List<float[]> embeddings = embeddingService.embed(chunkTexts);
            vectorStore.saveAll(chunks, embeddings);

            // 5. Completion
            savedDoc.setStatus(DocumentStatus.PROCESSED);
            savedDoc.setErrorMessage(null);
            documentRepository.save(savedDoc);

            return mapToDocumentResponse(savedDoc, chunks.size());

        } catch (Exception e) {
            savedDoc.setStatus(DocumentStatus.FAILED);
            savedDoc.setErrorMessage(e.getMessage());
            documentRepository.save(savedDoc);
            throw new BadRequestException("Document processing failed: " + e.getMessage());
        }
    }
    @Transactional
public int reembedExistingDocument(Long documentId) {
    List<DocumentChunk> chunks =
            documentChunkRepository.findAllByDocumentIdOrderByChunkIndexAsc(documentId);

    if (chunks.isEmpty()) {
        throw new IllegalStateException("No chunks found for document ID: " + documentId);
    }

    List<String> chunkTexts = chunks.stream()
            .map(DocumentChunk::getContent)
            .collect(Collectors.toList());

    List<float[]> embeddings = embeddingService.embed(chunkTexts);

    vectorStore.saveAll(chunks, embeddings);

    return chunks.size();
}

    public DocumentListResponse getUserDocuments(String userEmail) {
        User user = getUserByEmail(userEmail);
        List<Document> documents = documentRepository.findAllByUserIdOrderByCreatedAtDesc(user.getId());
        List<DocumentResponse> responseList = documents.stream()
                .map(doc -> {
                    int chunkCount = documentChunkRepository.countByDocumentId(doc.getId());
                    return mapToDocumentResponse(doc, chunkCount);
                })
                .collect(Collectors.toList());

        return new DocumentListResponse(responseList);
    }

    public DocumentResponse getUserDocumentById(Long documentId, String userEmail) {
        User user = getUserByEmail(userEmail);
        Document document = findDocumentAndVerifyOwnership(documentId, user.getId());
        int chunkCount = documentChunkRepository.countByDocumentId(document.getId());
        return mapToDocumentResponse(document, chunkCount);
    }

    public List<DocumentChunkResponse> getUserDocumentChunks(Long documentId, String userEmail) {
        User user = getUserByEmail(userEmail);
        Document document = findDocumentAndVerifyOwnership(documentId, user.getId());
        List<DocumentChunk> chunks = documentChunkRepository.findAllByDocumentIdOrderByChunkIndexAsc(document.getId());

        return chunks.stream()
                .map(chunk -> DocumentChunkResponse.builder()
                        .id(chunk.getId())
                        .documentId(document.getId())
                        .chunkIndex(chunk.getChunkIndex())
                        .content(chunk.getContent())
                        .characterCount(chunk.getCharacterCount())
                        .createdAt(chunk.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteUserDocument(Long documentId, String userEmail) {
        User user = getUserByEmail(userEmail);
        Document document = findDocumentAndVerifyOwnership(documentId, user.getId());

        // 1. Detach quizzes referencing this document
        quizRepository.detachDocument(documentId);

        // 2. Delete vectors from VectorStore
        vectorStore.deleteByDocumentId(documentId);

        // 3. Delete physical stored file
        storageService.delete(document.getStoragePath());

        // 4. Delete database records (chunks auto-deleted via JPA cascade)
        documentRepository.delete(document);
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }

    private Document findDocumentAndVerifyOwnership(Long documentId, Long userId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with ID: " + documentId));

        if (!document.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("You are not authorized to access this document.");
        }
        return document;
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Uploaded file cannot be empty.");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BadRequestException("File size exceeds maximum allowed limit of 10 MB.");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || !filename.contains(".")) {
            throw new BadRequestException("Invalid filename. File must have a valid extension.");
        }

        // Path traversal defense
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            throw new BadRequestException("Filename contains illegal path traversal characters.");
        }

        String extension = getFileExtension(filename).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BadRequestException("Unsupported file format: ." + extension + ". Allowed formats are: " + String.join(", ", ALLOWED_EXTENSIONS));
        }

        // Validate content type against malicious executable types
        String contentType = file.getContentType();
        if (contentType != null) {
            String lowerType = contentType.toLowerCase();
            if (lowerType.contains("html") || lowerType.contains("javascript") || lowerType.contains("dosexec") || lowerType.contains("x-sh")) {
                throw new BadRequestException("Disallowed executable or script MIME type: " + contentType);
            }
        }

        // Validate PDF magic bytes
        if ("pdf".equals(extension)) {
            try (InputStream is = file.getInputStream()) {
                byte[] header = new byte[4];
                int bytesRead = is.read(header);
                if (bytesRead < 4 || header[0] != '%' || header[1] != 'P' || header[2] != 'D' || header[3] != 'F') {
                    throw new BadRequestException("Invalid PDF header. File is not a valid PDF document.");
                }
            } catch (java.io.IOException e) {
                throw new BadRequestException("Failed to read file header for verification: " + e.getMessage());
            }
        }
    }

    private String getFileExtension(String filename) {
        int lastIndexOf = filename.lastIndexOf(".");
        if (lastIndexOf == -1) {
            return "";
        }
        return filename.substring(lastIndexOf + 1);
    }

    public DocumentResponse mapToDocumentResponse(Document doc, int chunkCount) {
        return DocumentResponse.builder()
                .id(doc.getId())
                .userId(doc.getUser().getId())
                .fileName(doc.getStoredFileName())
                .originalFileName(doc.getOriginalFileName())
                .fileType(doc.getFileType())
                .fileSize(doc.getFileSize())
                .status(doc.getStatus())
                .pageCount(doc.getPageCount())
                .chunkCount(chunkCount)
                .errorMessage(doc.getErrorMessage())
                .createdAt(doc.getCreatedAt())
                .updatedAt(doc.getUpdatedAt())
                .build();
    }
}
