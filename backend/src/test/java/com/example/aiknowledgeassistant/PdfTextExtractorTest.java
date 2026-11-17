package com.example.aiknowledgeassistant;

import com.example.aiknowledgeassistant.service.processor.ExtractedDocumentData;
import com.example.aiknowledgeassistant.service.processor.PdfTextExtractor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.*;

public class PdfTextExtractorTest {

    private PdfTextExtractor pdfTextExtractor;

    @BeforeEach
    public void setUp() {
        pdfTextExtractor = new PdfTextExtractor();
    }

    @Test
    public void testExtractTextFromPdf() throws Exception {
        // Create a sample 1-page PDF document using Apache PDFBox
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page);

            try (PDPageContentStream contentStream = new PDPageContentStream(doc, page)) {
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 12);
                contentStream.beginText();
                contentStream.newLineAtOffset(100, 700);
                contentStream.showText("Hello Cognivault AI PDF Extraction Test");
                contentStream.endText();
            }

            doc.save(baos);
        }

        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        ExtractedDocumentData data = pdfTextExtractor.extract(bais);

        assertNotNull(data);
        assertEquals(1, data.getPageCount());
        assertTrue(data.getText().contains("Hello Cognivault AI PDF Extraction Test"));
    }
}
