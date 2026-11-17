package com.example.aiknowledgeassistant.service.processor;

import com.example.aiknowledgeassistant.exception.BadRequestException;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Component
public class TxtTextExtractor {

    public ExtractedDocumentData extract(InputStream inputStream) {
        try {
            byte[] bytes = inputStream.readAllBytes();
            String text = new String(bytes, StandardCharsets.UTF_8);
            int lineCount = text.split("\n").length;
            int pageCount = Math.max(1, (lineCount + 49) / 50);

            return new ExtractedDocumentData(text, pageCount);
        } catch (Exception e) {
            throw new BadRequestException("Failed to read text file: " + e.getMessage());
        }
    }
}
