package com.example.aiknowledgeassistant.service.ai.rag;

public interface LlmService {
    String generateAnswer(String systemPrompt, String userPrompt);
    String getModel();
}
