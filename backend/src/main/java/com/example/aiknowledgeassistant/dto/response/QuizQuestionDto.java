package com.example.aiknowledgeassistant.dto.response;

import com.example.aiknowledgeassistant.entity.GroundingType;
import com.example.aiknowledgeassistant.entity.QuestionCategory;
import com.example.aiknowledgeassistant.entity.QuizDifficulty;

import java.util.List;

public class QuizQuestionDto {

    private Long id;
    private Integer questionIndex;
    private String questionText;
    private List<String> options;
    private QuestionCategory category;
    private QuizDifficulty difficulty;
    private GroundingType groundingType;
    private String skillTag;
    private String codeSnippet;
    private String sourceCitation;

    public QuizQuestionDto() {
    }

    public QuizQuestionDto(Long id, Integer questionIndex, String questionText, List<String> options,
                           QuestionCategory category, QuizDifficulty difficulty, GroundingType groundingType,
                           String skillTag, String codeSnippet, String sourceCitation) {
        this.id = id;
        this.questionIndex = questionIndex;
        this.questionText = questionText;
        this.options = options;
        this.category = category;
        this.difficulty = difficulty;
        this.groundingType = groundingType;
        this.skillTag = skillTag;
        this.codeSnippet = codeSnippet;
        this.sourceCitation = sourceCitation;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Integer getQuestionIndex() { return questionIndex; }
    public void setQuestionIndex(Integer questionIndex) { this.questionIndex = questionIndex; }

    public String getQuestionText() { return questionText; }
    public void setQuestionText(String questionText) { this.questionText = questionText; }

    public List<String> getOptions() { return options; }
    public void setOptions(List<String> options) { this.options = options; }

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

    public String getSourceCitation() { return sourceCitation; }
    public void setSourceCitation(String sourceCitation) { this.sourceCitation = sourceCitation; }
}

