package com.example.aiknowledgeassistant.entity;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "quiz_questions", indexes = {
    @Index(name = "idx_quiz_questions_quiz_id", columnList = "quiz_id"),
    @Index(name = "idx_quiz_questions_sig", columnList = "question_signature")
})
public class QuizQuestion {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    @Column(name = "question_index", nullable = false)
    private Integer questionIndex;

    @Column(name = "question_text", nullable = false, columnDefinition = "TEXT")
    private String questionText;

    @Column(name = "options_json", nullable = false, columnDefinition = "TEXT")
    private String optionsJson;

    @Column(name = "correct_answer_index", nullable = false)
    private Integer correctAnswerIndex;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String explanation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private QuestionCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private QuizDifficulty difficulty;

    @Enumerated(EnumType.STRING)
    @Column(name = "grounding_type", nullable = false, length = 30)
    private GroundingType groundingType;

    @Column(name = "skill_tag", length = 100)
    private String skillTag;

    @Column(name = "code_snippet", columnDefinition = "TEXT")
    private String codeSnippet;

    @Column(name = "question_signature", length = 64)
    private String questionSignature;

    @Column(name = "source_citation", length = 255)
    private String sourceCitation;

    public QuizQuestion() {
    }

    public QuizQuestion(Long id, Quiz quiz, Integer questionIndex, String questionText,
                        String optionsJson, Integer correctAnswerIndex, String explanation,
                        QuestionCategory category, QuizDifficulty difficulty,
                        GroundingType groundingType, String skillTag, String codeSnippet,
                        String questionSignature, String sourceCitation) {
        this.id = id;
        this.quiz = quiz;
        this.questionIndex = questionIndex;
        this.questionText = questionText;
        this.optionsJson = optionsJson;
        this.correctAnswerIndex = correctAnswerIndex;
        this.explanation = explanation;
        this.category = category;
        this.difficulty = difficulty;
        this.groundingType = groundingType;
        this.skillTag = skillTag;
        this.codeSnippet = codeSnippet;
        this.questionSignature = questionSignature;
        this.sourceCitation = sourceCitation;
    }

    public List<String> getOptions() {
        if (optionsJson == null || optionsJson.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return MAPPER.readValue(optionsJson, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return List.of(optionsJson.split(";"));
        }
    }

    public void setOptions(List<String> options) {
        if (options == null) {
            this.optionsJson = "[]";
            return;
        }
        try {
            this.optionsJson = MAPPER.writeValueAsString(options);
        } catch (Exception e) {
            this.optionsJson = String.join(";", options);
        }
    }

    public static QuizQuestionBuilder builder() {
        return new QuizQuestionBuilder();
    }

    public static class QuizQuestionBuilder {
        private Long id;
        private Quiz quiz;
        private Integer questionIndex;
        private String questionText;
        private String optionsJson;
        private Integer correctAnswerIndex;
        private String explanation;
        private QuestionCategory category;
        private QuizDifficulty difficulty;
        private GroundingType groundingType;
        private String skillTag;
        private String codeSnippet;
        private String questionSignature;
        private String sourceCitation;

        public QuizQuestionBuilder id(Long id) { this.id = id; return this; }
        public QuizQuestionBuilder quiz(Quiz quiz) { this.quiz = quiz; return this; }
        public QuizQuestionBuilder questionIndex(Integer questionIndex) { this.questionIndex = questionIndex; return this; }
        public QuizQuestionBuilder questionText(String questionText) { this.questionText = questionText; return this; }
        public QuizQuestionBuilder optionsJson(String optionsJson) { this.optionsJson = optionsJson; return this; }
        public QuizQuestionBuilder correctAnswerIndex(Integer correctAnswerIndex) { this.correctAnswerIndex = correctAnswerIndex; return this; }
        public QuizQuestionBuilder explanation(String explanation) { this.explanation = explanation; return this; }
        public QuizQuestionBuilder category(QuestionCategory category) { this.category = category; return this; }
        public QuizQuestionBuilder difficulty(QuizDifficulty difficulty) { this.difficulty = difficulty; return this; }
        public QuizQuestionBuilder groundingType(GroundingType groundingType) { this.groundingType = groundingType; return this; }
        public QuizQuestionBuilder skillTag(String skillTag) { this.skillTag = skillTag; return this; }
        public QuizQuestionBuilder codeSnippet(String codeSnippet) { this.codeSnippet = codeSnippet; return this; }
        public QuizQuestionBuilder questionSignature(String questionSignature) { this.questionSignature = questionSignature; return this; }
        public QuizQuestionBuilder sourceCitation(String sourceCitation) { this.sourceCitation = sourceCitation; return this; }

        public QuizQuestionBuilder options(List<String> options) {
            if (options != null) {
                try {
                    this.optionsJson = MAPPER.writeValueAsString(options);
                } catch (Exception e) {
                    this.optionsJson = String.join(";", options);
                }
            }
            return this;
        }

        public QuizQuestion build() {
            return new QuizQuestion(id, quiz, questionIndex, questionText, optionsJson, correctAnswerIndex,
                    explanation, category, difficulty, groundingType, skillTag, codeSnippet,
                    questionSignature, sourceCitation);
        }
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Quiz getQuiz() { return quiz; }
    public void setQuiz(Quiz quiz) { this.quiz = quiz; }

    public Integer getQuestionIndex() { return questionIndex; }
    public void setQuestionIndex(Integer questionIndex) { this.questionIndex = questionIndex; }

    public String getQuestionText() { return questionText; }
    public void setQuestionText(String questionText) { this.questionText = questionText; }

    public String getOptionsJson() { return optionsJson; }
    public void setOptionsJson(String optionsJson) { this.optionsJson = optionsJson; }

    public Integer getCorrectAnswerIndex() { return correctAnswerIndex; }
    public void setCorrectAnswerIndex(Integer correctAnswerIndex) { this.correctAnswerIndex = correctAnswerIndex; }

    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }

    public QuestionCategory getCategory() { return category; }
    public void setCategory(QuestionCategory category) { this.category = category; }

    public QuizDifficulty getDifficulty() { return difficulty; }
    public void setDifficulty(QuizDifficulty difficulty) { this.difficulty = difficulty; }

    public GroundingType getGroundingType() { return groundingType; }
    public void setGroundingType(GroundingType groundingType) { this.groundingType = groundingType; }

    public String getSkillTag() { return skillTag; }
    public void setSkillTag(String skillTag) { this.skillTag = skillTag; }

    public String getCodeSnippet() { return codeSnippet; }
    public void setCodeSnippet(String codeSnippet) { this.codeSnippet = codeSnippet; }

    public String getQuestionSignature() { return questionSignature; }
    public void setQuestionSignature(String questionSignature) { this.questionSignature = questionSignature; }

    public String getSourceCitation() { return sourceCitation; }
    public void setSourceCitation(String sourceCitation) { this.sourceCitation = sourceCitation; }
}

