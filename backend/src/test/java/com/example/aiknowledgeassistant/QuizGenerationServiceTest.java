package com.example.aiknowledgeassistant;

import com.example.aiknowledgeassistant.entity.*;
import com.example.aiknowledgeassistant.service.ai.rag.LlmService;
import com.example.aiknowledgeassistant.service.quiz.QuizGenerationService;
import com.example.aiknowledgeassistant.service.quiz.SkillExtractionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

public class QuizGenerationServiceTest {

    private LlmService mockLlmService;
    private SkillExtractionService skillExtractionService;
    private QuizGenerationService quizGenerationService;

    @BeforeEach
    public void setup() {
        mockLlmService = Mockito.mock(LlmService.class);
        skillExtractionService = new SkillExtractionService();
        quizGenerationService = new QuizGenerationService(mockLlmService, skillExtractionService);
    }

    @Test
    public void testSkillBasedQuizGenerationProducesDeepTechnicalQuestions() {
        // When LLM returns empty/invalid (simulating offline or test mode), deterministic generator triggers
        when(mockLlmService.generateAnswer(anyString(), anyString())).thenReturn("");

        List<String> resumeSkills = List.of("Java", "Spring Boot", "React", "MongoDB");

        QuizGenerationService.GenerationResult result = quizGenerationService.generateQuiz(
                QuizMode.SKILL_BASED,
                QuizDifficulty.INTERMEDIATE,
                5,
                List.of(QuestionCategory.CONCEPTUAL, QuestionCategory.CODING, QuestionCategory.DEBUGGING),
                null,
                resumeSkills,
                null,
                "resume.pdf",
                Collections.emptySet()
        );

        assertNotNull(result);
        assertNotNull(result.questions);
        assertEquals(5, result.questions.size(), "Should generate requested question count");

        for (QuizGenerationService.RawGeneratedQuestion q : result.questions) {
            assertNotNull(q.question);
            assertTrue(q.question.length() >= 15);
            assertEquals(4, q.options.size(), "Must have exactly 4 options");
            // Check that all 4 options are distinct
            assertEquals(4, new HashSet<>(q.options).size(), "Options must be distinct");
            assertTrue(q.correctAnswer >= 0 && q.correctAnswer < 4, "Correct answer must be 0, 1, 2, or 3");
            assertNotNull(q.explanation);
            assertTrue(q.explanation.length() >= 20, "Explanation must be substantive");
            assertEquals(GroundingType.TECHNICAL_ASSESSMENT, q.groundingType);

            // Crucial assertion: Ensure questions test real technical concepts and NOT superficial resume trivia!
            assertFalse(q.question.toLowerCase().contains("which skill is mentioned"), "Must not ask which skill is mentioned in resume");
            assertFalse(q.question.toLowerCase().contains("what programming language is mentioned"), "Must not ask what language is in resume");
        }
    }

    @Test
    public void testDocumentBasedQuizGrounding() {
        when(mockLlmService.generateAnswer(anyString(), anyString())).thenReturn("");

        String docContext = """
                [S1] Cognivault AI utilizes an enterprise PostgreSQL 16 database with the pgvector extension for sub-millisecond similarity search.
                [S2] All document chunks are partitioned by authenticated user ID at the database SQL query level to enforce multi-tenant isolation.
                """;

        QuizGenerationService.GenerationResult result = quizGenerationService.generateQuiz(
                QuizMode.DOCUMENT_BASED,
                QuizDifficulty.INTERMEDIATE,
                2,
                List.of(QuestionCategory.CONCEPTUAL),
                null,
                Collections.emptyList(),
                docContext,
                "system-architecture.pdf",
                Collections.emptySet()
        );

        assertNotNull(result);
        assertFalse(result.questions.isEmpty());

        for (QuizGenerationService.RawGeneratedQuestion q : result.questions) {
            assertEquals(GroundingType.DOCUMENT_GROUNDED, q.groundingType);
            assertNotNull(q.sourceCitation);
            assertTrue(q.sourceCitation.startsWith("[S"), "Must contain source citation tag");
        }
    }

    @Test
    public void testDuplicatePreventionSignatureCalculation() {
        String q1 = "What is the difference between HashMap and ConcurrentHashMap in Java?";
        String q2 = "what is the difference between hashmap and concurrenthashmap in java?";
        String q3 = "How does Spring Boot dependency injection work?";

        String sig1 = QuizGenerationService.computeQuestionSignature(q1);
        String sig2 = QuizGenerationService.computeQuestionSignature(q2);
        String sig3 = QuizGenerationService.computeQuestionSignature(q3);

        assertEquals(sig1, sig2, "Signatures for identical questions with different case should match");
        assertNotEquals(sig1, sig3, "Signatures for different questions must not match");
    }

    @Test
    public void testPreviousSignaturesAreExcluded() {
        when(mockLlmService.generateAnswer(anyString(), anyString())).thenReturn("");

        // Pre-populate previous signatures with Java question signature
        String javaQuestion = "In Java's memory model, how does ConcurrentHashMap achieve thread-safe reads without acquiring a lock on the entire map?";
        String sig = QuizGenerationService.computeQuestionSignature(javaQuestion);
        Set<String> previousSignatures = Set.of(sig);

        QuizGenerationService.GenerationResult result = quizGenerationService.generateQuiz(
                QuizMode.SKILL_BASED,
                QuizDifficulty.ADVANCED,
                3,
                List.of(QuestionCategory.CONCEPTUAL),
                "Java",
                List.of("Java"),
                null,
                null,
                previousSignatures
        );

        assertNotNull(result);
        for (QuizGenerationService.RawGeneratedQuestion q : result.questions) {
            assertNotEquals(sig, q.signature, "Previously asked question must not be repeated");
        }
    }
}

