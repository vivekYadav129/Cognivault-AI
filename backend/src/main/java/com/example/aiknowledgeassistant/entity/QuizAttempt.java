package com.example.aiknowledgeassistant.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "quiz_attempts", indexes = {
    @Index(name = "idx_quiz_attempts_user_id", columnList = "user_id"),
    @Index(name = "idx_quiz_attempts_quiz_id", columnList = "quiz_id")
})
public class QuizAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private Integer score;

    @Column(name = "total_questions", nullable = false)
    private Integer totalQuestions;

    @Column(nullable = false)
    private Double percentage;

    @Column(name = "user_answers_json", columnDefinition = "TEXT")
    private String userAnswersJson;

    @Column(name = "topic_breakdown_json", columnDefinition = "TEXT")
    private String topicBreakdownJson;

    @Column(name = "difficulty_breakdown_json", columnDefinition = "TEXT")
    private String difficultyBreakdownJson;

    @Column(name = "completed_at", nullable = false)
    private LocalDateTime completedAt;

    public QuizAttempt() {
    }

    public QuizAttempt(Long id, Quiz quiz, User user, Integer score, Integer totalQuestions,
                       Double percentage, String userAnswersJson, String topicBreakdownJson,
                       String difficultyBreakdownJson, LocalDateTime completedAt) {
        this.id = id;
        this.quiz = quiz;
        this.user = user;
        this.score = score;
        this.totalQuestions = totalQuestions;
        this.percentage = percentage;
        this.userAnswersJson = userAnswersJson;
        this.topicBreakdownJson = topicBreakdownJson;
        this.difficultyBreakdownJson = difficultyBreakdownJson;
        this.completedAt = completedAt;
    }

    @PrePersist
    protected void onCreate() {
        if (this.completedAt == null) {
            this.completedAt = LocalDateTime.now();
        }
    }

    public static QuizAttemptBuilder builder() {
        return new QuizAttemptBuilder();
    }

    public static class QuizAttemptBuilder {
        private Long id;
        private Quiz quiz;
        private User user;
        private Integer score;
        private Integer totalQuestions;
        private Double percentage;
        private String userAnswersJson;
        private String topicBreakdownJson;
        private String difficultyBreakdownJson;
        private LocalDateTime completedAt;

        public QuizAttemptBuilder id(Long id) { this.id = id; return this; }
        public QuizAttemptBuilder quiz(Quiz quiz) { this.quiz = quiz; return this; }
        public QuizAttemptBuilder user(User user) { this.user = user; return this; }
        public QuizAttemptBuilder score(Integer score) { this.score = score; return this; }
        public QuizAttemptBuilder totalQuestions(Integer totalQuestions) { this.totalQuestions = totalQuestions; return this; }
        public QuizAttemptBuilder percentage(Double percentage) { this.percentage = percentage; return this; }
        public QuizAttemptBuilder userAnswersJson(String userAnswersJson) { this.userAnswersJson = userAnswersJson; return this; }
        public QuizAttemptBuilder topicBreakdownJson(String topicBreakdownJson) { this.topicBreakdownJson = topicBreakdownJson; return this; }
        public QuizAttemptBuilder difficultyBreakdownJson(String difficultyBreakdownJson) { this.difficultyBreakdownJson = difficultyBreakdownJson; return this; }
        public QuizAttemptBuilder completedAt(LocalDateTime completedAt) { this.completedAt = completedAt; return this; }

        public QuizAttempt build() {
            return new QuizAttempt(id, quiz, user, score, totalQuestions, percentage,
                    userAnswersJson, topicBreakdownJson, difficultyBreakdownJson, completedAt);
        }
    }

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Quiz getQuiz() { return quiz; }
    public void setQuiz(Quiz quiz) { this.quiz = quiz; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }

    public Integer getTotalQuestions() { return totalQuestions; }
    public void setTotalQuestions(Integer totalQuestions) { this.totalQuestions = totalQuestions; }

    public Double getPercentage() { return percentage; }
    public void setPercentage(Double percentage) { this.percentage = percentage; }

    public String getUserAnswersJson() { return userAnswersJson; }
    public void setUserAnswersJson(String userAnswersJson) { this.userAnswersJson = userAnswersJson; }

    public String getTopicBreakdownJson() { return topicBreakdownJson; }
    public void setTopicBreakdownJson(String topicBreakdownJson) { this.topicBreakdownJson = topicBreakdownJson; }

    public String getDifficultyBreakdownJson() { return difficultyBreakdownJson; }
    public void setDifficultyBreakdownJson(String difficultyBreakdownJson) { this.difficultyBreakdownJson = difficultyBreakdownJson; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
}

