package com.example.aiknowledgeassistant.service.processor;

import com.example.aiknowledgeassistant.exception.BadRequestException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.InputStream;

@Component
public class PdfTextExtractor {

    public ExtractedDocumentData extract(InputStream inputStream) {
        try (PDDocument document = Loader.loadPDF(inputStream.readAllBytes())) {
            if (document.isEncrypted()) {
                throw new BadRequestException("PDF document is encrypted and cannot be processed.");
            }

            int pageCount = document.getNumberOfPages();
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            String text = stripper.getText(document);

            return new ExtractedDocumentData(text, pageCount);
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestException("Failed to extract text from PDF document: " + e.getMessage());
        }
    }
}
