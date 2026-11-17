package com.example.aiknowledgeassistant.dto.response;

import java.util.Map;

public class QuizEvaluationReport {

    private int totalQuestionsEvaluated;
    private double skillRelevanceRate;
    private double technicalAccuracyRate;
    private double duplicateRate;
    private double validJsonRate;
    private double distractorQualityRate;
    private double groundingFaithfulnessRate;
    private Map<String, Object> automatedMetrics;
    private String notes;

    public QuizEvaluationReport() {
    }

    public QuizEvaluationReport(int totalQuestionsEvaluated, double skillRelevanceRate,
                                double technicalAccuracyRate, double duplicateRate,
                                double validJsonRate, double distractorQualityRate,
                                double groundingFaithfulnessRate, Map<String, Object> automatedMetrics,
                                String notes) {
        this.totalQuestionsEvaluated = totalQuestionsEvaluated;
        this.skillRelevanceRate = skillRelevanceRate;
        this.technicalAccuracyRate = technicalAccuracyRate;
        this.duplicateRate = duplicateRate;
        this.validJsonRate = validJsonRate;
        this.distractorQualityRate = distractorQualityRate;
        this.groundingFaithfulnessRate = groundingFaithfulnessRate;
        this.automatedMetrics = automatedMetrics;
        this.notes = notes;
    }

    public int getTotalQuestionsEvaluated() { return totalQuestionsEvaluated; }
    public void setTotalQuestionsEvaluated(int totalQuestionsEvaluated) { this.totalQuestionsEvaluated = totalQuestionsEvaluated; }

    public double getSkillRelevanceRate() { return skillRelevanceRate; }
    public void setSkillRelevanceRate(double skillRelevanceRate) { this.skillRelevanceRate = skillRelevanceRate; }

    public double getTechnicalAccuracyRate() { return technicalAccuracyRate; }
    public void setTechnicalAccuracyRate(double technicalAccuracyRate) { this.technicalAccuracyRate = technicalAccuracyRate; }

    public double getDuplicateRate() { return duplicateRate; }
    public void setDuplicateRate(double duplicateRate) { this.duplicateRate = duplicateRate; }

    public double getValidJsonRate() { return validJsonRate; }
    public void setValidJsonRate(double validJsonRate) { this.validJsonRate = validJsonRate; }

    public double getDistractorQualityRate() { return distractorQualityRate; }
    public void setDistractorQualityRate(double distractorQualityRate) { this.distractorQualityRate = distractorQualityRate; }

    public double getGroundingFaithfulnessRate() { return groundingFaithfulnessRate; }
    public void setGroundingFaithfulnessRate(double groundingFaithfulnessRate) { this.groundingFaithfulnessRate = groundingFaithfulnessRate; }

    public Map<String, Object> getAutomatedMetrics() { return automatedMetrics; }
    public void setAutomatedMetrics(Map<String, Object> automatedMetrics) { this.automatedMetrics = automatedMetrics; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}

