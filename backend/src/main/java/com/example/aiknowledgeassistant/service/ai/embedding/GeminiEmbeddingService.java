
package com.example.aiknowledgeassistant.service.ai.embedding;

import com.example.aiknowledgeassistant.exception.BadRequestException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@ConditionalOnProperty(
        name = "cognivault.embedding.provider",
        havingValue = "gemini"
)
public class GeminiEmbeddingService implements EmbeddingService {

    private static final Logger logger =
            LoggerFactory.getLogger(GeminiEmbeddingService.class);

    private final String apiKey;
    private final String model;
    private final int dimension;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public GeminiEmbeddingService(
            @Value("${GEMINI_API_KEY:}") String apiKey,
            @Value("${cognivault.embedding.model:gemini-embedding-001}") String model,
            @Value("${cognivault.embedding.dimension:1536}") int dimension) {

        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = model;
        this.dimension = dimension;

        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();

        this.objectMapper = new ObjectMapper();

        if (this.apiKey.isBlank()) {
            throw new IllegalStateException("GEMINI_API_KEY is not configured.");
        }
    }

    @Override
    public int getDimension() {
        return dimension;
    }

    @Override
    public float[] embed(String text) {
        if (text == null || text.isBlank()) {
            throw new BadRequestException("Text for embedding cannot be empty.");
        }

        List<float[]> results = embed(Collections.singletonList(text));
        return results.get(0);
    }

    @Override
    public List<float[]> embed(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return Collections.emptyList();
        }

        List<float[]> results = new ArrayList<>();

        for (String text : texts) {
            results.add(fetchEmbedding(text));
        }

        return results;
    }

    private float[] fetchEmbedding(String text) {
        try {
            Map<String, Object> requestBody = Map.of(
                    "model", "models/" + model,
                    "content", Map.of(
                            "parts", List.of(Map.of("text", text))
                    ),
                    "outputDimensionality", dimension
            );

            String payload = objectMapper.writeValueAsString(requestBody);

            String endpoint =
                    "https://generativelanguage.googleapis.com/v1beta/models/"
                            + model + ":embedContent";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .header("Content-Type", "application/json")
                    .header("x-goog-api-key", apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .timeout(Duration.ofSeconds(45))
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() != 200) {
                logger.error("Gemini Embedding API returned status {}",
                        response.statusCode());
                throw new BadRequestException(
                        "Gemini embedding request failed with HTTP status: "
                                + response.statusCode()
                );
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode values = root.path("embedding").path("values");

            if (!values.isArray() || values.size() != dimension) {
                throw new BadRequestException(
                        "Gemini embedding dimension mismatch. Expected "
                                + dimension + ", received " + values.size()
                );
            }

            float[] vector = new float[dimension];

            for (int i = 0; i < dimension; i++) {
                vector[i] = (float) values.get(i).asDouble();
            }

            return vector;

        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Gemini embedding generation failed", e);
            throw new BadRequestException(
                    "Failed to generate Gemini embedding: " + e.getMessage()
            );
        }
    }
}
