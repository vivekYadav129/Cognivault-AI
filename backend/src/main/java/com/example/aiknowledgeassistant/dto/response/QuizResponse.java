package com.example.aiknowledgeassistant.dto.response;

import com.example.aiknowledgeassistant.entity.QuizDifficulty;
import com.example.aiknowledgeassistant.entity.QuizMode;

import java.time.LocalDateTime;
import java.util.List;

public class QuizResponse {

    private Long id;
    private String title;
    private String description;
    private QuizMode mode;
    private QuizDifficulty difficulty;
    private String topicFocus;
    private Long documentId;
    private String documentName;
    private List<QuizQuestionDto> questions;
    private LocalDateTime createdAt;

    public QuizResponse() {
    }

    public QuizResponse(Long id, String title, String description, QuizMode mode,
                        QuizDifficulty difficulty, String topicFocus, Long documentId,
                        String documentName, List<QuizQuestionDto> questions, LocalDateTime createdAt) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.mode = mode;
        this.difficulty = difficulty;
        this.topicFocus = topicFocus;
        this.documentId = documentId;
        this.documentName = documentName;
        this.questions = questions;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public QuizMode getMode() { return mode; }
    public void setMode(QuizMode mode) { this.mode = mode; }

    public QuizDifficulty getDifficulty() { return difficulty; }
    public void setDifficulty(QuizDifficulty difficulty) { this.difficulty = difficulty; }

    public String getTopicFocus() { return topicFocus; }
    public void setTopicFocus(String topicFocus) { this.topicFocus = topicFocus; }

    public Long getDocumentId() { return documentId; }
    public void setDocumentId(Long documentId) { this.documentId = documentId; }

    public String getDocumentName() { return documentName; }
    public void setDocumentName(String documentName) { this.documentName = documentName; }

    public List<QuizQuestionDto> getQuestions() { return questions; }
    public void setQuestions(List<QuizQuestionDto> questions) { this.questions = questions; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

