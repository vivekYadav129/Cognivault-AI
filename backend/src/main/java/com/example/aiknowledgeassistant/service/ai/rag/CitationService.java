package com.example.aiknowledgeassistant.service.ai.rag;

import com.example.aiknowledgeassistant.dto.response.CitationDto;
import com.example.aiknowledgeassistant.service.ai.vector.SearchResult;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class CitationService {

    private static final Pattern CITATION_PATTERN = Pattern.compile("\\[S(\\d+)\\]");

    public List<CitationDto> extractAndValidateCitations(String answer, Map<String, SearchResult> sourceMap) {
        if (answer == null || answer.isBlank() || sourceMap == null || sourceMap.isEmpty()) {
            return Collections.emptyList();
        }

        Matcher matcher = CITATION_PATTERN.matcher(answer);
        Set<String> citedKeys = new LinkedHashSet<>();

        while (matcher.find()) {
            String fullKey = "S" + matcher.group(1);
            if (sourceMap.containsKey(fullKey)) {
                citedKeys.add(fullKey);
            }
        }

        // If LLM didn't explicitly include [S1] tags in text but generated grounded answer, include all provided sources up to sourceMap limit
        if (citedKeys.isEmpty()) {
            citedKeys.addAll(sourceMap.keySet());
        }

        List<CitationDto> citations = new ArrayList<>();
        for (String key : citedKeys) {
            SearchResult res = sourceMap.get(key);
            if (res != null) {
                CitationDto dto = CitationDto.builder()
                        .documentId(res.getDocumentId())
                        .chunkId(res.getChunkId())
                        .documentName(res.getFileName())
                        .chunkIndex(res.getChunkIndex())
                        .pageNumber(res.getPageNumber())
                        .similarityScore(res.getSimilarityScore())
                        .build();
                citations.add(dto);
            }
        }

        return citations;
    }
}
