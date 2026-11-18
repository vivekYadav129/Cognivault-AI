
package com.example.aiknowledgeassistant.service.ai.rag;

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
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@ConditionalOnProperty(
        name = "cognivault.rag.provider",
        havingValue = "gemini"
)
public class GeminiLlmService implements LlmService {

    private static final Logger logger =
            LoggerFactory.getLogger(GeminiLlmService.class);

    private final String apiKey;
    private final String model;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public GeminiLlmService(
            @Value("${GEMINI_API_KEY:}") String apiKey,
            @Value("${cognivault.rag.model:gemini-2.5-flash}") String model) {

        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = model;

        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();

        this.objectMapper = new ObjectMapper();

        if (this.apiKey.isBlank()) {
            throw new IllegalStateException(
                    "GEMINI_API_KEY is not configured."
            );
        }
    }

    @Override
    public String getModel() {
        return model;
    }

    @Override
    public String generateAnswer(String systemPrompt, String userPrompt) {

        if (systemPrompt == null || userPrompt == null
                || userPrompt.isBlank()) {
            throw new BadRequestException(
                    "System prompt and user prompt cannot be empty."
            );
        }

        try {
            logger.info("Calling Gemini API with model {}", model);

            Map<String, Object> requestBody = new HashMap<>();

            requestBody.put(
                    "systemInstruction",
                    Map.of(
                            "parts",
                            List.of(Map.of("text", systemPrompt))
                    )
            );

            requestBody.put(
                    "contents",
                    List.of(
                            Map.of(
                                    "role", "user",
                                    "parts",
                                    List.of(Map.of("text", userPrompt))
                            )
                    )
            );

            requestBody.put(
                    "generationConfig",
                    Map.of(
                            "temperature", 0.1,
                            "maxOutputTokens", 4096
                    )
            );

            String jsonPayload =
                    objectMapper.writeValueAsString(requestBody);

            String endpoint =
                    "https://generativelanguage.googleapis.com/v1beta/models/"
                            + model + ":generateContent";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .header("Content-Type", "application/json")
                    .header("x-goog-api-key", apiKey)
                    .POST(
                            HttpRequest.BodyPublishers.ofString(
                                    jsonPayload,
                                    StandardCharsets.UTF_8
                            )
                    )
                    .timeout(Duration.ofSeconds(60))
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() != 200) {
                logger.error(
                        "Gemini API returned HTTP status {}",
                        response.statusCode()
                );

                throw new BadRequestException(
                        "Gemini API request failed with HTTP status: "
                                + response.statusCode()
                );
            }

            JsonNode root = objectMapper.readTree(response.body());

            JsonNode parts = root.path("candidates")
                    .path(0)
                    .path("content")
                    .path("parts");

            if (!parts.isArray() || parts.isEmpty()) {
                throw new BadRequestException(
                        "Gemini returned an empty response."
                );
            }

            StringBuilder answer = new StringBuilder();

            for (JsonNode part : parts) {
                JsonNode textNode = part.get("text");

                if (textNode != null && textNode.isTextual()) {
                    answer.append(textNode.asText());
                }
            }

            String result = answer.toString().trim();

            if (result.isBlank()) {
                throw new BadRequestException(
                        "Gemini returned no text answer."
                );
            }

            return result;

        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Gemini LLM processing failed", e);

            throw new BadRequestException(
                    "Gemini LLM processing error: " + e.getMessage()
            );
        }
    }
}
