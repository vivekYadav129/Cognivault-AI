package com.example.aiknowledgeassistant.controller;

import com.example.aiknowledgeassistant.dto.request.SearchRequest;
import com.example.aiknowledgeassistant.dto.response.SearchResponse;
import com.example.aiknowledgeassistant.dto.response.SearchResultDto;
import com.example.aiknowledgeassistant.entity.User;
import com.example.aiknowledgeassistant.exception.ResourceNotFoundException;
import com.example.aiknowledgeassistant.repository.UserRepository;
import com.example.aiknowledgeassistant.service.ai.embedding.EmbeddingService;
import com.example.aiknowledgeassistant.service.ai.vector.SearchResult;
import com.example.aiknowledgeassistant.service.ai.vector.VectorStore;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/search")
public class SearchController {

    private final EmbeddingService embeddingService;
    private final VectorStore vectorStore;
    private final UserRepository userRepository;

    public SearchController(EmbeddingService embeddingService, VectorStore vectorStore, UserRepository userRepository) {
        this.embeddingService = embeddingService;
        this.vectorStore = vectorStore;
        this.userRepository = userRepository;
    }

    @PostMapping
    public ResponseEntity<SearchResponse> search(
            @Valid @RequestBody SearchRequest request,
            Authentication authentication) {

        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // 1. Embed query
        float[] queryVector = embeddingService.embed(request.getQuery());

        // 2. Perform similarity search enforcing authenticated userId isolation
        List<SearchResult> rawResults = vectorStore.similaritySearch(
                queryVector,
                request.getTopK() != null ? request.getTopK() : 5,
                user.getId(),
                request.getDocumentId()
        );

        // 3. Map to DTOs
        List<SearchResultDto> dtos = rawResults.stream()
                .map(r -> new SearchResultDto(
                        r.getChunkId(),
                        r.getDocumentId(),
                        r.getContent(),
                        r.getSimilarityScore(),
                        r.getMetadata()
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(new SearchResponse(request.getQuery(), dtos));
    }
}
