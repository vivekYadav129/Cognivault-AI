package com.example.aiknowledgeassistant.service.processor;

import com.example.aiknowledgeassistant.exception.BadRequestException;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Component;

import java.io.InputStream;

@Component
public class DocxTextExtractor {

    public ExtractedDocumentData extract(InputStream inputStream) {
        try (XWPFDocument doc = new XWPFDocument(inputStream);
             XWPFWordExtractor extractor = new XWPFWordExtractor(doc)) {

            String text = extractor.getText();
            int estimatedPageCount = Math.max(1, doc.getParagraphs().size() / 15);

            return new ExtractedDocumentData(text, estimatedPageCount);
        } catch (Exception e) {
            throw new BadRequestException("Failed to extract text from DOCX document: " + e.getMessage());
        }
    }
}
