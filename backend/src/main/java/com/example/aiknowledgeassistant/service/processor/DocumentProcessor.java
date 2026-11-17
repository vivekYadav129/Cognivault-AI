package com.example.aiknowledgeassistant.service.processor;

import com.example.aiknowledgeassistant.exception.BadRequestException;
import com.example.aiknowledgeassistant.util.TextCleaner;
import com.example.aiknowledgeassistant.util.TextChunker;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;

@Component
public class DocumentProcessor {

    private final PdfTextExtractor pdfTextExtractor;
    private final DocxTextExtractor docxTextExtractor;
    private final TxtTextExtractor txtTextExtractor;
    private final TextCleaner textCleaner;
    private final TextChunker textChunker;

    public DocumentProcessor(PdfTextExtractor pdfTextExtractor,
                             DocxTextExtractor docxTextExtractor,
                             TxtTextExtractor txtTextExtractor,
                             TextCleaner textCleaner,
                             TextChunker textChunker) {
        this.pdfTextExtractor = pdfTextExtractor;
        this.docxTextExtractor = docxTextExtractor;
        this.txtTextExtractor = txtTextExtractor;
        this.textCleaner = textCleaner;
        this.textChunker = textChunker;
    }

    public ProcessedDocumentResult process(InputStream inputStream, String fileType) {
        ExtractedDocumentData rawData;
        String normalizedFileType = fileType != null ? fileType.toUpperCase().trim() : "";

        if (normalizedFileType.endsWith("PDF")) {
            rawData = pdfTextExtractor.extract(inputStream);
        } else if (normalizedFileType.endsWith("DOCX") || normalizedFileType.contains("WORD")) {
            rawData = docxTextExtractor.extract(inputStream);
        } else if (normalizedFileType.endsWith("TXT") || normalizedFileType.contains("TEXT") || normalizedFileType.contains("PLAIN")) {
            rawData = txtTextExtractor.extract(inputStream);
        } else {
            throw new BadRequestException("Unsupported file type: " + fileType + ". Allowed types are PDF, DOCX, TXT.");
        }

        String cleanedText = textCleaner.clean(rawData.getText());
        if (cleanedText.isBlank()) {
            throw new BadRequestException("Document contains no extractable text.");
        }

        List<TextChunker.Chunk> chunks = textChunker.chunkText(cleanedText);

        return new ProcessedDocumentResult(cleanedText, rawData.getPageCount(), chunks);
    }

    public static class ProcessedDocumentResult {
        private final String cleanedText;
        private final Integer pageCount;
        private final List<TextChunker.Chunk> chunks;

        public ProcessedDocumentResult(String cleanedText, Integer pageCount, List<TextChunker.Chunk> chunks) {
            this.cleanedText = cleanedText;
            this.pageCount = pageCount;
            this.chunks = chunks;
        }

        public String getCleanedText() { return cleanedText; }
        public Integer getPageCount() { return pageCount; }
        public List<TextChunker.Chunk> getChunks() { return chunks; }
    }
}
