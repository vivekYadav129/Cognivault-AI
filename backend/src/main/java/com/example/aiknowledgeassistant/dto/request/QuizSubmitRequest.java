package com.example.aiknowledgeassistant.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.Map;

public class QuizSubmitRequest {

    @NotNull(message = "Answers map cannot be null")
    private Map<Long, Integer> answers; // questionId -> selectedOptionIndex

    public QuizSubmitRequest() {
    }

    public QuizSubmitRequest(Map<Long, Integer> answers) {
        this.answers = answers;
    }

    public Map<Long, Integer> getAnswers() {
        return answers;
    }

    public void setAnswers(Map<Long, Integer> answers) {
        this.answers = answers;
    }
}

