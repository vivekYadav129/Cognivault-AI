package com.example.aiknowledgeassistant.service.ai.rag;

import com.example.aiknowledgeassistant.service.ai.vector.SearchResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class ContextBuilder {

    private final int maxContextChars;

    public ContextBuilder(@Value("${cognivault.rag.max-context-chars:12000}") int maxContextChars) {
        this.maxContextChars = maxContextChars;
    }

    public ContextResult buildContext(List<SearchResult> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return new ContextResult("", Map.of());
        }

        StringBuilder contextBuilder = new StringBuilder();
        Map<String, SearchResult> sourceMap = new LinkedHashMap<>();
        Set<String> seenContent = new HashSet<>();

        int sourceCounter = 1;

        for (SearchResult chunk : chunks) {
            String rawContent = chunk.getContent() != null ? chunk.getContent().trim() : "";
            if (rawContent.isBlank() || seenContent.contains(rawContent)) {
                continue; // Skip duplicates or empty chunks
            }
            seenContent.add(rawContent);

            String sourceId = "S" + sourceCounter++;
            sourceMap.put(sourceId, chunk);

            StringBuilder sourceBlock = new StringBuilder();
            sourceBlock.append("[").append(sourceId).append("]\n");
            sourceBlock.append("Document: ").append(chunk.getFileName()).append("\n");
            sourceBlock.append("Chunk Index: ").append(chunk.getChunkIndex()).append("\n");
            if (chunk.getPageNumber() != null) {
                sourceBlock.append("Page: ").append(chunk.getPageNumber()).append("\n");
            }
            sourceBlock.append("Content:\n").append(rawContent).append("\n\n");

            if (contextBuilder.length() + sourceBlock.length() > maxContextChars) {
                break; // Stop if max context length is reached
            }

            contextBuilder.append(sourceBlock);
        }

        return new ContextResult(contextBuilder.toString().trim(), sourceMap);
    }

    public static class ContextResult {
        private final String formattedContext;
        private final Map<String, SearchResult> sourceMap; // Maps "S1" -> SearchResult

        public ContextResult(String formattedContext, Map<String, SearchResult> sourceMap) {
            this.formattedContext = formattedContext;
            this.sourceMap = sourceMap;
        }

        public String getFormattedContext() { return formattedContext; }
        public Map<String, SearchResult> getSourceMap() { return sourceMap; }
    }
}
