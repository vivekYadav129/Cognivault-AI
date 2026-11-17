package com.example.aiknowledgeassistant.dto.response;

import java.util.List;
import java.util.Map;

public class SkillExtractionResponse {

    private Long documentId;
    private String documentName;
    private Map<String, List<String>> categorizedSkills;
    private List<String> allSkills;
    private List<String> suggestedTopics;

    public SkillExtractionResponse() {
    }

    public SkillExtractionResponse(Long documentId, String documentName,
                                   Map<String, List<String>> categorizedSkills,
                                   List<String> allSkills, List<String> suggestedTopics) {
        this.documentId = documentId;
        this.documentName = documentName;
        this.categorizedSkills = categorizedSkills;
        this.allSkills = allSkills;
        this.suggestedTopics = suggestedTopics;
    }

    public Long getDocumentId() { return documentId; }
    public void setDocumentId(Long documentId) { this.documentId = documentId; }

    public String getDocumentName() { return documentName; }
    public void setDocumentName(String documentName) { this.documentName = documentName; }

    public Map<String, List<String>> getCategorizedSkills() { return categorizedSkills; }
    public void setCategorizedSkills(Map<String, List<String>> categorizedSkills) { this.categorizedSkills = categorizedSkills; }

    public List<String> getAllSkills() { return allSkills; }
    public void setAllSkills(List<String> allSkills) { this.allSkills = allSkills; }

    public List<String> getSuggestedTopics() { return suggestedTopics; }
    public void setSuggestedTopics(List<String> suggestedTopics) { this.suggestedTopics = suggestedTopics; }
}

