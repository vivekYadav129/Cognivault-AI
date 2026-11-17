package com.example.aiknowledgeassistant;

import com.example.aiknowledgeassistant.util.TextCleaner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TextCleanerTest {

    private TextCleaner textCleaner;

    @BeforeEach
    public void setUp() {
        textCleaner = new TextCleaner();
    }

    @Test
    public void testCleanWhitespaceAndNewlines() {
        String rawText = "   Hello    World!  \r\n\r\n\r\nThis   is   a   test.  \t  \n";
        String cleaned = textCleaner.clean(rawText);

        assertNotNull(cleaned);
        assertTrue(cleaned.contains("Hello World!"));
        assertTrue(cleaned.contains("This is a test."));
        assertFalse(cleaned.contains("\r"));
        assertFalse(cleaned.contains("    "));
    }

    @Test
    public void testControlCharacterRemoval() {
        String rawText = "Valid Text\u0000\u0007\u001B with hidden control characters";
        String cleaned = textCleaner.clean(rawText);

        assertEquals("Valid Text with hidden control characters", cleaned);
    }

    @Test
    public void testEmptyOrNullText() {
        assertEquals("", textCleaner.clean(null));
        assertEquals("", textCleaner.clean("   "));
    }
}
