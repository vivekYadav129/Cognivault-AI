package com.example.aiknowledgeassistant.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "quizzes", indexes = {
    @Index(name = "idx_quizzes_user_id", columnList = "user_id"),
    @Index(name = "idx_quizzes_doc_id", columnList = "document_id")
})
public class Quiz {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id")
    private Document document;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private QuizMode mode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private QuizDifficulty difficulty;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "topic_focus", length = 255)
    private String topicFocus;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "quiz", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("questionIndex ASC")
    private List<QuizQuestion> questions = new ArrayList<>();

    @OneToMany(mappedBy = "quiz", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<QuizAttempt> attempts = new ArrayList<>();

    public Quiz() {
    }

    public Quiz(Long id, User user, Document document, QuizMode mode, QuizDifficulty difficulty,
                String title, String description, String topicFocus, LocalDateTime createdAt) {
        this.id = id;
        this.user = user;
        this.document = document;
        this.mode = mode;
        this.difficulty = difficulty;
        this.title = title;
        this.description = description;
        this.topicFocus = topicFocus;
        this.createdAt = createdAt;
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    public static QuizBuilder builder() {
        return new QuizBuilder();
    }

    public static class QuizBuilder {
        private Long id;
        private User user;
        private Document document;
        private QuizMode mode;
        private QuizDifficulty difficulty;
        private String title;
        private String description;
        private String topicFocus;
        private LocalDateTime createdAt;

        public QuizBuilder id(Long id) { this.id = id; return this; }
        public QuizBuilder user(User user) { this.user = user; return this; }
        public QuizBuilder document(Document document) { this.document = document; return this; }
        public QuizBuilder mode(QuizMode mode) { this.mode = mode; return this; }
        public QuizBuilder difficulty(QuizDifficulty difficulty) { this.difficulty = difficulty; return this; }
        public QuizBuilder title(String title) { this.title = title; return this; }
        public QuizBuilder description(String description) { this.description = description; return this; }
        public QuizBuilder topicFocus(String topicFocus) { this.topicFocus = topicFocus; return this; }
        public QuizBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public Quiz build() {
            return new Quiz(id, user, document, mode, difficulty, title, description, topicFocus, createdAt);
        }
    }

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Document getDocument() { return document; }
    public void setDocument(Document document) { this.document = document; }

    public QuizMode getMode() { return mode; }
    public void setMode(QuizMode mode) { this.mode = mode; }

    public QuizDifficulty getDifficulty() { return difficulty; }
    public void setDifficulty(QuizDifficulty difficulty) { this.difficulty = difficulty; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getTopicFocus() { return topicFocus; }
    public void setTopicFocus(String topicFocus) { this.topicFocus = topicFocus; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public List<QuizQuestion> getQuestions() { return questions; }
    public void setQuestions(List<QuizQuestion> questions) { this.questions = questions; }

    public List<QuizAttempt> getAttempts() { return attempts; }
    public void setAttempts(List<QuizAttempt> attempts) { this.attempts = attempts; }
}

