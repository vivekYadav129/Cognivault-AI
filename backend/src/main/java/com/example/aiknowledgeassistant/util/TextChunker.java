package com.example.aiknowledgeassistant.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class TextChunker {

    private final int defaultChunkSize;
    private final int defaultChunkOverlap;

    public TextChunker(
            @Value("${cognivault.document.chunk-size:1000}") int chunkSize,
            @Value("${cognivault.document.chunk-overlap:150}") int chunkOverlap) {
        this.defaultChunkSize = chunkSize;
        this.defaultChunkOverlap = chunkOverlap;
    }

    public List<Chunk> chunkText(String text) {
        return chunkText(text, this.defaultChunkSize, this.defaultChunkOverlap);
    }

    public List<Chunk> chunkText(String text, int chunkSize, int chunkOverlap) {
        List<Chunk> chunks = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return chunks;
        }

        int textLength = text.length();
        if (textLength <= chunkSize) {
            chunks.add(new Chunk(0, text.trim(), text.trim().length()));
            return chunks;
        }

        int start = 0;
        int chunkIndex = 0;

        while (start < textLength) {
            int end = Math.min(start + chunkSize, textLength);

            // If not at the end of text, find a natural boundary to avoid word splitting
            if (end < textLength) {
                int boundary = findNaturalBoundary(text, start, end);
                if (boundary > start) {
                    end = boundary;
                }
            }

            String chunkText = text.substring(start, end).trim();
            if (!chunkText.isEmpty()) {
                chunks.add(new Chunk(chunkIndex++, chunkText, chunkText.length()));
            }

            if (end >= textLength) {
                break;
            }

            // Calculate next start with overlap, ensuring forward movement
            int nextStart = end - chunkOverlap;
            if (nextStart <= start) {
                nextStart = start + Math.max(1, chunkSize / 2);
            }
            start = nextStart;
        }

        return chunks;
    }

    private int findNaturalBoundary(String text, int start, int end) {
        // Look backwards from end for sentence/paragraph/space boundaries
        int minLookback = Math.max(start + (end - start) / 2, end - 200);

        // 1. Paragraph boundary (\n\n)
        int idx = text.lastIndexOf("\n\n", end);
        if (idx > minLookback) {
            return idx + 2;
        }

        // 2. Sentence boundary (. , ? , ! followed by space or newline)
        for (int i = end - 1; i >= minLookback; i--) {
            char c = text.charAt(i);
            if ((c == '.' || c == '?' || c == '!') && i + 1 < text.length() && Character.isWhitespace(text.charAt(i + 1))) {
                return i + 1;
            }
        }

        // 3. Newline boundary (\n)
        idx = text.lastIndexOf('\n', end);
        if (idx > minLookback) {
            return idx + 1;
        }

        // 4. Space boundary (' ')
        idx = text.lastIndexOf(' ', end);
        if (idx > minLookback) {
            return idx + 1;
        }

        return end;
    }

    public static class Chunk {
        private final int index;
        private final String content;
        private final int characterCount;

        public Chunk(int index, String content, int characterCount) {
            this.index = index;
            this.content = content;
            this.characterCount = characterCount;
        }

        public int getIndex() { return index; }
        public String getContent() { return content; }
        public int getCharacterCount() { return characterCount; }
    }
}
