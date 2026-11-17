package com.example.aiknowledgeassistant.service.ai.embedding;

import com.example.aiknowledgeassistant.exception.BadRequestException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

@Service
public class OpenAIEmbeddingService implements EmbeddingService {

    private static final Logger logger = LoggerFactory.getLogger(OpenAIEmbeddingService.class);

    private final String apiKey;
    private final String model;
    private final int dimension;
    private final int batchSize;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public OpenAIEmbeddingService(
            @Value("${cognivault.embedding.api-key:${OPENAI_API_KEY:}}") String apiKey,
            @Value("${cognivault.embedding.model:text-embedding-3-small}") String model,
            @Value("${cognivault.embedding.dimension:1536}") int dimension,
            @Value("${cognivault.embedding.batch-size:50}") int batchSize) {
        this.apiKey = apiKey != null ? apiKey.trim() : "";
        this.model = model;
        this.dimension = dimension;
        this.batchSize = batchSize;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
        this.objectMapper = new ObjectMapper();
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

        List<float[]> allEmbeddings = new ArrayList<>(texts.size());

        // Process in batches
        for (int i = 0; i < texts.size(); i += batchSize) {
            int end = Math.min(i + batchSize, texts.size());
            List<String> batch = texts.subList(i, end);

            if (apiKey.isBlank()) {
                logger.info("OpenAI API key not provided. Using term-hashing embedding generator for batch size {}", batch.size());
                for (String text : batch) {
                    allEmbeddings.add(generateDeterministicEmbedding(text, dimension));
                }
            } else {
                logger.info("Requesting OpenAI embeddings for batch of {} texts using model {}", batch.size(), model);
                List<float[]> batchEmbeddings = fetchOpenAIEmbeddings(batch);
                allEmbeddings.addAll(batchEmbeddings);
            }
        }

        return allEmbeddings;
    }

    private List<float[]> fetchOpenAIEmbeddings(List<String> batch) {
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("input", batch);
            requestBody.put("dimensions", dimension);

            String jsonPayload = objectMapper.writeValueAsString(requestBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.openai.com/v1/embeddings"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload, StandardCharsets.UTF_8))
                    .timeout(Duration.ofSeconds(30))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                logger.error("OpenAI Embedding API error, Status Code: {}", response.statusCode());
                throw new BadRequestException("Embedding API call failed with HTTP status: " + response.statusCode());
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode dataNode = root.get("data");

            if (dataNode == null || !dataNode.isArray()) {
                throw new BadRequestException("Invalid response format from OpenAI Embedding API.");
            }

            List<float[]> embeddings = new ArrayList<>(batch.size());
            for (JsonNode item : dataNode) {
                JsonNode embeddingNode = item.get("embedding");
                if (embeddingNode == null || !embeddingNode.isArray()) {
                    throw new BadRequestException("Missing embedding vector array in OpenAI response.");
                }

                if (embeddingNode.size() != dimension) {
                    throw new BadRequestException("Embedding dimension mismatch. Expected: " + dimension + ", received: " + embeddingNode.size());
                }

                float[] vector = new float[dimension];
                for (int d = 0; d < dimension; d++) {
                    vector[d] = (float) embeddingNode.get(d).asDouble();
                }
                embeddings.add(vector);
            }

            return embeddings;
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Failed to generate OpenAI embeddings", e);
            throw new BadRequestException("Failed to generate embeddings: " + e.getMessage());
        }
    }

    private static final Set<String> STOPWORDS = Set.of(
            "a", "about", "above", "after", "again", "against", "all", "am", "an", "and", "any", "are", "aren't",
            "as", "at", "be", "because", "been", "before", "being", "below", "between", "both", "but", "by",
            "can", "could", "did", "do", "does", "doing", "don't", "down", "during", "each", "few", "for", "from",
            "further", "had", "has", "have", "having", "he", "her", "here", "hers", "herself", "him", "himself",
            "his", "how", "i", "if", "in", "into", "is", "it", "its", "itself", "me", "more", "most", "my",
            "myself", "no", "nor", "not", "of", "off", "on", "once", "only", "or", "other", "ought", "our",
            "ours", "ourselves", "out", "over", "own", "same", "she", "should", "so", "some", "such", "than",
            "that", "the", "their", "theirs", "them", "themselves", "then", "there", "these", "they", "this",
            "those", "through", "to", "too", "under", "until", "up", "very", "was", "we", "were", "what",
            "when", "where", "which", "while", "who", "whom", "why", "with", "would", "you", "your", "yours"
    );

    /**
     * Term-hashing pseudo-embedding generator for offline/test mode.
     * Uses stopword suppression and 2-way hash projection so relevant document terms produce high cosine similarity.
     */
    public static float[] generateDeterministicEmbedding(String text, int dim) {
        float[] vector = new float[dim];
        if (text == null || text.isBlank()) {
            return vector;
        }

        String[] words = text.toLowerCase().split("\\W+");
        for (String word : words) {
            if (word.isBlank() || word.length() < 2 || STOPWORDS.contains(word)) continue;
            int hash = word.hashCode();
            int idx1 = Math.floorMod(hash, dim);
            int idx2 = Math.floorMod(hash * 31 + 17, dim);

            vector[idx1] += 1.0f;
            vector[idx2] += 0.5f;
        }

        // Normalize vector to unit length (L2 norm = 1.0)
        double norm = 0.0;
        for (float v : vector) {
            norm += v * v;
        }
        norm = Math.sqrt(norm);
        if (norm > 0) {
            for (int i = 0; i < dim; i++) {
                vector[i] /= norm;
            }
        }
        return vector;
    }
}
