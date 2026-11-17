package com.example.aiknowledgeassistant.service.quiz;

import com.example.aiknowledgeassistant.dto.response.QuizEvaluationReport;
import com.example.aiknowledgeassistant.entity.GroundingType;
import com.example.aiknowledgeassistant.entity.Quiz;
import com.example.aiknowledgeassistant.entity.QuizQuestion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class QuizEvaluationService {

    private static final Logger logger = LoggerFactory.getLogger(QuizEvaluationService.class);

    public QuizEvaluationReport evaluateQuiz(Quiz quiz, List<String> targetSkills, String sourceDocumentText) {
        if (quiz == null || quiz.getQuestions() == null || quiz.getQuestions().isEmpty()) {
            return new QuizEvaluationReport(0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                    Map.of("status", "EMPTY_QUIZ"), "No questions available to evaluate.");
        }

        List<QuizQuestion> questions = quiz.getQuestions();
        int total = questions.size();

        int relevantSkillsCount = 0;
        int technicallyAccurateCount = 0;
        int validDistractorsCount = 0;
        int groundedCount = 0;
        int totalGroundedExpected = 0;

        Set<String> seenSignatures = new HashSet<>();
        int duplicates = 0;

        for (QuizQuestion q : questions) {
            // 1. Uniqueness check
            String sig = q.getQuestionSignature();
            if (sig == null || sig.isBlank()) {
                sig = QuizGenerationService.computeQuestionSignature(q.getQuestionText());
            }
            if (seenSignatures.contains(sig)) {
                duplicates++;
            } else {
                seenSignatures.add(sig);
            }

            // 2. Skill Relevance check
            boolean isSkillRelevant = false;
            if (targetSkills == null || targetSkills.isEmpty()) {
                isSkillRelevant = true;
            } else {
                for (String skill : targetSkills) {
                    if ((q.getSkillTag() != null && q.getSkillTag().toLowerCase().contains(skill.toLowerCase())) ||
                        q.getQuestionText().toLowerCase().contains(skill.toLowerCase())) {
                        isSkillRelevant = true;
                        break;
                    }
                }
            }
            if (isSkillRelevant) relevantSkillsCount++;

            // 3. Technical Accuracy (Structural validity)
            List<String> options = q.getOptions();
            boolean hasValidOptions = options != null && options.size() == 4;
            boolean hasDistinctOptions = hasValidOptions && new HashSet<>(options).size() == 4;
            boolean validCorrectIndex = q.getCorrectAnswerIndex() != null &&
                    q.getCorrectAnswerIndex() >= 0 && q.getCorrectAnswerIndex() < 4;
            boolean validExplanation = q.getExplanation() != null && q.getExplanation().trim().length() >= 20;
            boolean validQuestionLength = q.getQuestionText() != null && q.getQuestionText().trim().length() >= 15;

            if (hasDistinctOptions && validCorrectIndex && validExplanation && validQuestionLength) {
                technicallyAccurateCount++;
            }

            // 4. Distractor Quality
            if (hasDistinctOptions) {
                boolean allDistractorsSubstantive = true;
                for (String opt : options) {
                    if (opt.trim().length() < 2 || opt.equalsIgnoreCase("none of the above") || opt.equalsIgnoreCase("all of the above")) {
                        allDistractorsSubstantive = false;
                        break;
                    }
                }
                if (allDistractorsSubstantive) {
                    validDistractorsCount++;
                }
            }

            // 5. Document Grounding
            if (q.getGroundingType() == GroundingType.DOCUMENT_GROUNDED) {
                totalGroundedExpected++;
                boolean hasCitation = q.getSourceCitation() != null && !q.getSourceCitation().isBlank();
                boolean contextSupported = true;
                if (sourceDocumentText != null && !sourceDocumentText.isBlank()) {
                    // Check if question key phrases exist in document
                    String[] words = q.getQuestionText().toLowerCase().split("\\W+");
                    int matches = 0;
                    for (String w : words) {
                        if (w.length() >= 4 && sourceDocumentText.toLowerCase().contains(w)) {
                            matches++;
                        }
                    }
                    contextSupported = matches >= 2;
                }
                if (hasCitation && contextSupported) {
                    groundedCount++;
                }
            }
        }

        double skillRelevanceRate = roundRate((double) relevantSkillsCount / total);
        double technicalAccuracyRate = roundRate((double) technicallyAccurateCount / total);
        double duplicateRate = roundRate((double) duplicates / total);
        double distractorQualityRate = roundRate((double) validDistractorsCount / total);
        double groundingRate = totalGroundedExpected > 0 ? roundRate((double) groundedCount / totalGroundedExpected) : 100.0;
        double validJsonRate = 100.0; // Successfully parsed into entities

        Map<String, Object> automatedMetrics = new LinkedHashMap<>();
        automatedMetrics.put("totalQuestions", total);
        automatedMetrics.put("uniqueQuestionsCount", seenSignatures.size());
        automatedMetrics.put("duplicateCount", duplicates);
        automatedMetrics.put("technicallyAccurateCount", technicallyAccurateCount);
        automatedMetrics.put("relevantSkillsCount", relevantSkillsCount);
        automatedMetrics.put("validDistractorsCount", validDistractorsCount);
        automatedMetrics.put("groundedQuestionsEvaluated", totalGroundedExpected);
        automatedMetrics.put("groundedQuestionsVerified", groundedCount);

        String notes = String.format("Automated evaluation on %d questions: Skill Relevance %.1f%%, Technical Accuracy %.1f%%, Duplicate Rate %.1f%%, Grounding Faithfulness %.1f%%.",
                total, skillRelevanceRate, technicalAccuracyRate, duplicateRate, groundingRate);

        logger.info(notes);

        return new QuizEvaluationReport(
                total,
                skillRelevanceRate,
                technicalAccuracyRate,
                duplicateRate,
                validJsonRate,
                distractorQualityRate,
                groundingRate,
                automatedMetrics,
                notes
        );
    }

    private double roundRate(double fraction) {
        return Math.round(fraction * 1000.0) / 10.0;
    }
}

