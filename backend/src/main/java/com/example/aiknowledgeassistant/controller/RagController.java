package com.example.aiknowledgeassistant.controller;

import com.example.aiknowledgeassistant.dto.request.RagQueryRequest;
import com.example.aiknowledgeassistant.dto.response.RagQueryResponse;
import com.example.aiknowledgeassistant.service.ai.rag.RagService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rag")
public class RagController {

    private final RagService ragService;

    public RagController(RagService ragService) {
        this.ragService = ragService;
    }

    @PostMapping("/query")
    public ResponseEntity<RagQueryResponse> query(
            @Valid @RequestBody RagQueryRequest request,
            Authentication authentication) {
        RagQueryResponse response = ragService.queryRag(request, authentication.getName());
        return ResponseEntity.ok(response);
    }
}
