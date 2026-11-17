package com.example.aiknowledgeassistant;

import com.example.aiknowledgeassistant.dto.response.CitationDto;
import com.example.aiknowledgeassistant.service.ai.rag.CitationService;
import com.example.aiknowledgeassistant.service.ai.vector.SearchResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class CitationServiceTest {

    private CitationService citationService;

    @BeforeEach
    public void setUp() {
        citationService = new CitationService();
    }

    @Test
    public void testValidCitationsExtracted() {
        SearchResult res1 = new SearchResult(10L, 1L, "Content 1", 0.92, 0, "doc1.pdf", 100L, 2);
        SearchResult res2 = new SearchResult(11L, 1L, "Content 2", 0.88, 1, "doc1.pdf", 100L, 2);

        Map<String, SearchResult> sourceMap = Map.of(
                "S1", res1,
                "S2", res2
        );

        String answer = "Spring Boot enables rapid Java development [S1]. PostgreSQL pgvector provides vector search [S2].";
        List<CitationDto> citations = citationService.extractAndValidateCitations(answer, sourceMap);

        assertEquals(2, citations.size());
        assertEquals(10L, citations.get(0).getChunkId());
        assertEquals(11L, citations.get(1).getChunkId());
        assertEquals("doc1.pdf", citations.get(0).getDocumentName());
    }

    @Test
    public void testFakeCitationsIgnored() {
        SearchResult res1 = new SearchResult(10L, 1L, "Content 1", 0.92, 0, "doc1.pdf", 100L, 2);
        Map<String, SearchResult> sourceMap = Map.of("S1", res1);

        String answer = "Grounded claim [S1], but model hallucinated fake citation [S99].";
        List<CitationDto> citations = citationService.extractAndValidateCitations(answer, sourceMap);

        assertEquals(1, citations.size());
        assertEquals(10L, citations.get(0).getChunkId());
    }
}
