package com.example.aiknowledgeassistant;

import com.example.aiknowledgeassistant.dto.response.CitationDto;
import com.example.aiknowledgeassistant.service.ai.rag.GroundingValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class GroundingValidatorTest {

    private GroundingValidator validator;

    @BeforeEach
    public void setUp() {
        validator = new GroundingValidator();
    }

    @Test
    public void testGroundedAnswerValidation() {
        CitationDto citation = CitationDto.builder().chunkId(1L).documentId(10L).build();
        assertTrue(validator.isGrounded("Answer based on document [S1].", List.of(citation), true));
    }

    @Test
    public void testNoContextIsNotGrounded() {
        assertFalse(validator.isGrounded("Answer without context.", List.of(), false));
    }

    @Test
    public void testExplicitNoInformationFoundIsGrounded() {
        assertTrue(validator.isGrounded("I couldn't find enough information in your uploaded documents to answer this question.", List.of(), true));
    }
}
