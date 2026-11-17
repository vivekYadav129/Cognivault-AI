package com.example.aiknowledgeassistant;

import com.example.aiknowledgeassistant.dto.response.QuizEvaluationReport;
import com.example.aiknowledgeassistant.entity.*;
import com.example.aiknowledgeassistant.service.quiz.QuizEvaluationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class QuizEvaluationServiceTest {

    private QuizEvaluationService evaluationService;

    @BeforeEach
    public void setup() {
        evaluationService = new QuizEvaluationService();
    }

    @Test
    public void testEvaluateHighQualityQuiz() {
        Quiz quiz = Quiz.builder()
                .title("Full Stack Java Assessment")
                .mode(QuizMode.SKILL_BASED)
                .difficulty(QuizDifficulty.INTERMEDIATE)
                .build();

        List<QuizQuestion> questions = new ArrayList<>();

        // Valid Question 1
        questions.add(QuizQuestion.builder()
                .quiz(quiz)
                .questionIndex(0)
                .questionText("How does Spring Boot handle dependency injection using constructor injection?")
                .options(List.of(
                        "Dependencies are injected automatically without requiring @Autowired on a single constructor",
                        "Spring Boot instantiates dependencies via native C++ DLLs",
                        "Constructor injection disables the Spring ApplicationContext",
                        "All constructor arguments are initialized as null until runtime"
                ))
                .correctAnswerIndex(0)
                .explanation("In Spring 4.3+, single constructors do not require @Autowired. Spring automatically injects managed beans into the constructor arguments.")
                .category(QuestionCategory.CONCEPTUAL)
                .difficulty(QuizDifficulty.INTERMEDIATE)
                .groundingType(GroundingType.TECHNICAL_ASSESSMENT)
                .skillTag("Spring Boot")
                .build());

        // Valid Question 2
        questions.add(QuizQuestion.builder()
                .quiz(quiz)
                .questionIndex(1)
                .questionText("What is the difference between useState and useEffect hooks in React applications?")
                .options(List.of(
                        "useState manages local component state while useEffect handles lifecycle side effects",
                        "useEffect is only for Redux while useState is for CSS modules",
                        "useState runs on every frame while useEffect runs once per session",
                        "They are interchangeable aliases in React 19"
                ))
                .correctAnswerIndex(0)
                .explanation("useState declares a state variable preserved across renders. useEffect synchronizes external systems or performs side effects after render commits.")
                .category(QuestionCategory.CONCEPTUAL)
                .difficulty(QuizDifficulty.INTERMEDIATE)
                .groundingType(GroundingType.TECHNICAL_ASSESSMENT)
                .skillTag("React")
                .build());

        quiz.setQuestions(questions);

        QuizEvaluationReport report = evaluationService.evaluateQuiz(quiz, List.of("Spring Boot", "React"), null);

        assertNotNull(report);
        assertEquals(2, report.getTotalQuestionsEvaluated());
        assertEquals(100.0, report.getSkillRelevanceRate(), "Both questions must be skill-relevant");
        assertEquals(100.0, report.getTechnicalAccuracyRate(), "Both questions must be technically accurate");
        assertEquals(0.0, report.getDuplicateRate(), "Duplicate rate should be 0%");
        assertEquals(100.0, report.getDistractorQualityRate(), "Distractor quality should be 100%");
        assertNotNull(report.getAutomatedMetrics());
        assertNotNull(report.getNotes());
    }

    @Test
    public void testDetectDuplicateQuestionsInEvaluation() {
        Quiz quiz = Quiz.builder()
                .title("Duplicate Test Quiz")
                .mode(QuizMode.SKILL_BASED)
                .difficulty(QuizDifficulty.BEGINNER)
                .build();

        String questionText = "What is the primary role of indexing in database systems?";

        QuizQuestion q1 = QuizQuestion.builder()
                .quiz(quiz)
                .questionIndex(0)
                .questionText(questionText)
                .options(List.of("Speed up data retrieval", "Encrypt passwords", "Delete tables", "Format dates"))
                .correctAnswerIndex(0)
                .explanation("Indexes speed up data retrieval operations at the cost of slower writes.")
                .category(QuestionCategory.CONCEPTUAL)
                .difficulty(QuizDifficulty.BEGINNER)
                .groundingType(GroundingType.TECHNICAL_ASSESSMENT)
                .skillTag("SQL")
                .build();

        QuizQuestion q2 = QuizQuestion.builder()
                .quiz(quiz)
                .questionIndex(1)
                .questionText(questionText) // Duplicate!
                .options(List.of("Speed up data retrieval", "Encrypt passwords", "Delete tables", "Format dates"))
                .correctAnswerIndex(0)
                .explanation("Indexes speed up data retrieval operations at the cost of slower writes.")
                .category(QuestionCategory.CONCEPTUAL)
                .difficulty(QuizDifficulty.BEGINNER)
                .groundingType(GroundingType.TECHNICAL_ASSESSMENT)
                .skillTag("SQL")
                .build();

        quiz.setQuestions(List.of(q1, q2));

        QuizEvaluationReport report = evaluationService.evaluateQuiz(quiz, List.of("SQL"), null);

        assertNotNull(report);
        assertEquals(2, report.getTotalQuestionsEvaluated());
        assertEquals(50.0, report.getDuplicateRate(), "Duplicate rate must be 50% when 1 of 2 questions is a duplicate");
    }
}

