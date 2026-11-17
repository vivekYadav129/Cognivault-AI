package com.example.aiknowledgeassistant.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class QuizResultResponse {

    private Long attemptId;
    private Long quizId;
    private Integer score;
    private Integer totalQuestions;
    private Double percentage;
    private Map<String, TopicScore> topicBreakdown;
    private Map<String, TopicScore> difficultyBreakdown;
    private List<QuestionResultDto> questionResults;
    private LocalDateTime completedAt;

    public static class TopicScore {
        private int correct;
        private int total;
        private double percentage;

        public TopicScore() {}

        public TopicScore(int correct, int total) {
            this.correct = correct;
            this.total = total;
            this.percentage = total > 0 ? Math.round(((double) correct / total) * 1000.0) / 10.0 : 0.0;
        }

        public int getCorrect() { return correct; }
        public void setCorrect(int correct) { this.correct = correct; }

        public int getTotal() { return total; }
        public void setTotal(int total) { this.total = total; }

        public double getPercentage() { return percentage; }
        public void setPercentage(double percentage) { this.percentage = percentage; }
    }

    public QuizResultResponse() {
    }

    public QuizResultResponse(Long attemptId, Long quizId, Integer score, Integer totalQuestions,
                              Double percentage, Map<String, TopicScore> topicBreakdown,
                              Map<String, TopicScore> difficultyBreakdown,
                              List<QuestionResultDto> questionResults, LocalDateTime completedAt) {
        this.attemptId = attemptId;
        this.quizId = quizId;
        this.score = score;
        this.totalQuestions = totalQuestions;
        this.percentage = percentage;
        this.topicBreakdown = topicBreakdown;
        this.difficultyBreakdown = difficultyBreakdown;
        this.questionResults = questionResults;
        this.completedAt = completedAt;
    }

    public Long getAttemptId() { return attemptId; }
    public void setAttemptId(Long attemptId) { this.attemptId = attemptId; }

    public Long getQuizId() { return quizId; }
    public void setQuizId(Long quizId) { this.quizId = quizId; }

    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }

    public Integer getTotalQuestions() { return totalQuestions; }
    public void setTotalQuestions(Integer totalQuestions) { this.totalQuestions = totalQuestions; }

    public Double getPercentage() { return percentage; }
    public void setPercentage(Double percentage) { this.percentage = percentage; }

    public Map<String, TopicScore> getTopicBreakdown() { return topicBreakdown; }
    public void setTopicBreakdown(Map<String, TopicScore> topicBreakdown) { this.topicBreakdown = topicBreakdown; }

    public Map<String, TopicScore> getDifficultyBreakdown() { return difficultyBreakdown; }
    public void setDifficultyBreakdown(Map<String, TopicScore> difficultyBreakdown) { this.difficultyBreakdown = difficultyBreakdown; }

    public List<QuestionResultDto> getQuestionResults() { return questionResults; }
    public void setQuestionResults(List<QuestionResultDto> questionResults) { this.questionResults = questionResults; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
}

