package com.example.aiknowledgeassistant.dto.response;

import com.example.aiknowledgeassistant.entity.QuizDifficulty;
import com.example.aiknowledgeassistant.entity.QuizMode;

import java.time.LocalDateTime;

public class QuizSummaryResponse {

    private Long id;
    private String title;
    private QuizMode mode;
    private QuizDifficulty difficulty;
    private String topicFocus;
    private Integer questionCount;
    private Long documentId;
    private String documentName;
    private LocalDateTime createdAt;
    private Integer latestScore;
    private Double latestPercentage;
    private boolean completed;

    public QuizSummaryResponse() {
    }

    public QuizSummaryResponse(Long id, String title, QuizMode mode, QuizDifficulty difficulty,
                               String topicFocus, Integer questionCount, Long documentId,
                               String documentName, LocalDateTime createdAt, Integer latestScore,
                               Double latestPercentage, boolean completed) {
        this.id = id;
        this.title = title;
        this.mode = mode;
        this.difficulty = difficulty;
        this.topicFocus = topicFocus;
        this.questionCount = questionCount;
        this.documentId = documentId;
        this.documentName = documentName;
        this.createdAt = createdAt;
        this.latestScore = latestScore;
        this.latestPercentage = latestPercentage;
        this.completed = completed;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public QuizMode getMode() { return mode; }
    public void setMode(QuizMode mode) { this.mode = mode; }

    public QuizDifficulty getDifficulty() { return difficulty; }
    public void setDifficulty(QuizDifficulty difficulty) { this.difficulty = difficulty; }

    public String getTopicFocus() { return topicFocus; }
    public void setTopicFocus(String topicFocus) { this.topicFocus = topicFocus; }

    public Integer getQuestionCount() { return questionCount; }
    public void setQuestionCount(Integer questionCount) { this.questionCount = questionCount; }

    public Long getDocumentId() { return documentId; }
    public void setDocumentId(Long documentId) { this.documentId = documentId; }

    public String getDocumentName() { return documentName; }
    public void setDocumentName(String documentName) { this.documentName = documentName; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public Integer getLatestScore() { return latestScore; }
    public void setLatestScore(Integer latestScore) { this.latestScore = latestScore; }

    public Double getLatestPercentage() { return latestPercentage; }
    public void setLatestPercentage(Double latestPercentage) { this.latestPercentage = latestPercentage; }

    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }
}

