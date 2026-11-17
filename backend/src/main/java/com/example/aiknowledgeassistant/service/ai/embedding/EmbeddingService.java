package com.example.aiknowledgeassistant.service.ai.embedding;

import java.util.List;

public interface EmbeddingService {
    List<float[]> embed(List<String> texts);
    float[] embed(String text);
    int getDimension();
}
