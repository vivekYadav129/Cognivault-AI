package com.example.aiknowledgeassistant.util;

import org.springframework.stereotype.Component;

@Component
public class TextCleaner {

    /**
     * Cleans raw extracted text:
     * - Removes null/control characters
     * - Normalizes carriage returns (\r\n -> \n)
     * - Replaces tabs with spaces
     * - Reduces multiple consecutive spaces to a single space per line
     * - Reduces 3+ consecutive newlines to double newlines (\n\n) to preserve paragraphs
     * - Trims leading and trailing whitespace
     */
    public String clean(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }

        // 1. Remove non-printable control characters except \t, \n, \r
        String cleaned = text.replaceAll("[\\p{C}&&[^\r\n\t]]", "");

        // 2. Normalize Windows/Mac line endings to standard \n
        cleaned = cleaned.replace("\r\n", "\n").replace("\r", "\n");

        // 3. Convert tabs to spaces
        cleaned = cleaned.replace("\t", " ");

        // 4. Reduce multiple spaces on a single line to a single space
        cleaned = cleaned.replaceAll("[ ]{2,}", " ");

        // 5. Trim whitespace around each line
        String[] lines = cleaned.split("\n");
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            String trimmedLine = line.trim();
            sb.append(trimmedLine).append("\n");
        }
        cleaned = sb.toString();

        // 6. Reduce 3 or more consecutive newlines to 2 newlines (paragraph separation)
        cleaned = cleaned.replaceAll("\n{3,}", "\n\n");

        return cleaned.trim();
    }
}
