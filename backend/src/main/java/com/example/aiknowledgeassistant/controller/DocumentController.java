package com.example.aiknowledgeassistant.controller;

import com.example.aiknowledgeassistant.dto.response.DocumentChunkResponse;
import com.example.aiknowledgeassistant.dto.response.DocumentListResponse;
import com.example.aiknowledgeassistant.dto.response.DocumentResponse;
import com.example.aiknowledgeassistant.service.DocumentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentResponse> uploadDocument(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        DocumentResponse response = documentService.uploadAndProcessDocument(file, authentication.getName());
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<DocumentListResponse> getDocuments(Authentication authentication) {
        DocumentListResponse response = documentService.getUserDocuments(authentication.getName());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DocumentResponse> getDocumentById(
            @PathVariable("id") Long id,
            Authentication authentication) {
        DocumentResponse response = documentService.getUserDocumentById(id, authentication.getName());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/chunks")
    public ResponseEntity<List<DocumentChunkResponse>> getDocumentChunks(
            @PathVariable("id") Long id,
            Authentication authentication) {
        List<DocumentChunkResponse> chunks = documentService.getUserDocumentChunks(id, authentication.getName());
        return ResponseEntity.ok(chunks);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDocument(
            @PathVariable("id") Long id,
            Authentication authentication) {
        documentService.deleteUserDocument(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
