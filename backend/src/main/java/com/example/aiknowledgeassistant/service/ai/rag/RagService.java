package com.example.aiknowledgeassistant.service.ai.rag;

import com.example.aiknowledgeassistant.dto.request.RagQueryRequest;
import com.example.aiknowledgeassistant.dto.response.CitationDto;
import com.example.aiknowledgeassistant.dto.response.RagQueryResponse;
import com.example.aiknowledgeassistant.entity.Document;
import com.example.aiknowledgeassistant.entity.User;
import com.example.aiknowledgeassistant.exception.BadRequestException;
import com.example.aiknowledgeassistant.exception.ResourceNotFoundException;
import com.example.aiknowledgeassistant.exception.UnauthorizedException;
import com.example.aiknowledgeassistant.repository.DocumentRepository;
import com.example.aiknowledgeassistant.repository.UserRepository;
import com.example.aiknowledgeassistant.service.ai.vector.SearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RagService {

    private static final Logger logger = LoggerFactory.getLogger(RagService.class);

    private final UserRepository userRepository;
    private final DocumentRepository documentRepository;
    private final RetrievalService retrievalService;
    private final ContextBuilder contextBuilder;
    private final PromptBuilder promptBuilder;
    private final LlmService llmService;
    private final CitationService citationService;

    public RagService(
            UserRepository userRepository,
            DocumentRepository documentRepository,
            RetrievalService retrievalService,
            ContextBuilder contextBuilder,
            PromptBuilder promptBuilder,
            LlmService llmService,
            CitationService citationService) {
        this.userRepository = userRepository;
        this.documentRepository = documentRepository;
        this.retrievalService = retrievalService;
        this.contextBuilder = contextBuilder;
        this.promptBuilder = promptBuilder;
        this.llmService = llmService;
        this.citationService = citationService;
    }

    public RagQueryResponse queryRag(RagQueryRequest request, String userEmail) {
        if (request == null || request.getQuery() == null || request.getQuery().isBlank()) {
            throw new BadRequestException("Query cannot be blank.");
        }

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + userEmail));

        // 1. Verify document ownership if documentId is specified
        if (request.getDocumentId() != null) {
            Document doc = documentRepository.findById(request.getDocumentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Document not found with ID: " + request.getDocumentId()));
            if (!doc.getUser().getId().equals(user.getId())) {
                throw new UnauthorizedException("You are not authorized to query this document.");
            }
        }

        logger.info("Executing RAG query for user ID {}, query: '{}'", user.getId(), request.getQuery());

        // 2. Perform Retrieval
        List<SearchResult> chunks = retrievalService.retrieveRelevantChunks(
                request.getQuery(),
                request.getTopK(),
                user.getId(),
                request.getDocumentId()
        );

        // 3. No-Context Guard: If no relevant chunks meet threshold, return default answer without calling LLM
        if (chunks.isEmpty()) {
            logger.info("No chunks met similarity threshold ({}). Returning no-context response without LLM call.", retrievalService.getSimilarityThreshold());
            return RagQueryResponse.noContext();
        }

        // 4. Build Context
        ContextBuilder.ContextResult contextResult = contextBuilder.buildContext(chunks);

        // 5. Build System and User Prompts
        String systemPrompt = promptBuilder.buildSystemPrompt();
        String userPrompt = promptBuilder.buildUserPrompt(request.getQuery(), contextResult.getFormattedContext());

        // 6. Generate LLM Answer
        String rawAnswer = llmService.generateAnswer(systemPrompt, userPrompt);

        // 7. Extract & Validate Citations
        List<CitationDto> citations = citationService.extractAndValidateCitations(rawAnswer, contextResult.getSourceMap());

        logger.info("RAG query completed successfully with {} valid citations.", citations.size());

        return new RagQueryResponse(rawAnswer, citations, citations.size());
    }
}
