package com.example.aiknowledgeassistant.dto.request;

import com.example.aiknowledgeassistant.entity.QuestionCategory;
import com.example.aiknowledgeassistant.entity.QuizDifficulty;
import com.example.aiknowledgeassistant.entity.QuizMode;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class QuizGenerateRequest {

    private Long documentId;

    @NotNull(message = "Quiz mode is required")
    private QuizMode mode = QuizMode.SKILL_BASED;

    @NotNull(message = "Quiz difficulty is required")
    private QuizDifficulty difficulty = QuizDifficulty.INTERMEDIATE;

    @Min(value = 1, message = "Question count must be at least 1")
    @Max(value = 30, message = "Question count cannot exceed 30")
    private int questionCount = 5;

    private List<QuestionCategory> categories;

    private String topicFocus;

    public QuizGenerateRequest() {
    }

    public QuizGenerateRequest(Long documentId, QuizMode mode, QuizDifficulty difficulty,
                               int questionCount, List<QuestionCategory> categories, String topicFocus) {
        this.documentId = documentId;
        this.mode = mode != null ? mode : QuizMode.SKILL_BASED;
        this.difficulty = difficulty != null ? difficulty : QuizDifficulty.INTERMEDIATE;
        this.questionCount = questionCount > 0 ? questionCount : 5;
        this.categories = categories;
        this.topicFocus = topicFocus;
    }

    public Long getDocumentId() { return documentId; }
    public void setDocumentId(Long documentId) { this.documentId = documentId; }

    public QuizMode getMode() { return mode; }
    public void setMode(QuizMode mode) { this.mode = mode; }

    public QuizDifficulty getDifficulty() { return difficulty; }
    public void setDifficulty(QuizDifficulty difficulty) { this.difficulty = difficulty; }

    public int getQuestionCount() { return questionCount; }
    public void setQuestionCount(int questionCount) { this.questionCount = questionCount; }

    public List<QuestionCategory> getCategories() { return categories; }
    public void setCategories(List<QuestionCategory> categories) { this.categories = categories; }

    public String getTopicFocus() { return topicFocus; }
    public void setTopicFocus(String topicFocus) { this.topicFocus = topicFocus; }
}

