package com.example.aiknowledgeassistant.service.ai.rag;

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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class OpenAiLlmService implements LlmService {

    private static final Logger logger = LoggerFactory.getLogger(OpenAiLlmService.class);

    private final String apiKey;
    private final String model;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public OpenAiLlmService(
            @Value("${cognivault.embedding.api-key:${OPENAI_API_KEY:}}") String apiKey,
            @Value("${cognivault.rag.model:gpt-4o-mini}") String model) {
        this.apiKey = apiKey != null ? apiKey.trim() : "";
        this.model = model;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public String getModel() {
        return model;
    }

    @Override
    public String generateAnswer(String systemPrompt, String userPrompt) {
        if (apiKey.isBlank()) {
            logger.info("OpenAI API key not provided. Using offline grounded LLM answer generator for testing.");
            return generateOfflineGroundedAnswer(userPrompt);
        }

        try {
            logger.info("Calling OpenAI Chat Completions API with model {}", model);

            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content", systemPrompt));
            messages.add(Map.of("role", "user", "content", userPrompt));

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("messages", messages);
            requestBody.put("temperature", 0.1);

            String jsonPayload = objectMapper.writeValueAsString(requestBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.openai.com/v1/chat/completions"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload, StandardCharsets.UTF_8))
                    .timeout(Duration.ofSeconds(45))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                logger.error("OpenAI Chat Completions API error, Status Code: {}", response.statusCode());
                throw new BadRequestException("LLM API call failed with HTTP status: " + response.statusCode());
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode choices = root.get("choices");
            if (choices != null && choices.isArray() && !choices.isEmpty()) {
                JsonNode messageNode = choices.get(0).get("message");
                if (messageNode != null && messageNode.has("content")) {
                    return messageNode.get("content").asText().trim();
                }
            }

            throw new BadRequestException("Empty or invalid response from LLM provider.");

        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error communicating with LLM service", e);
            throw new BadRequestException("LLM processing error: " + e.getMessage());
        }
    }

    /**
     * Offline answer generator for unit/integration tests and local offline mode when no live API key is set.
     * Extracts source tags [S1], [S2] present in the userPrompt, locates matching sections and facts from retrieved context,
     * and produces a grounded, factual response with valid citations.
     */
    private String generateOfflineGroundedAnswer(String userPrompt) {
        Matcher matcher = Pattern.compile("\\[S\\d+\\]").matcher(userPrompt);
        Set<String> foundSources = new LinkedHashSet<>();
        while (matcher.find()) {
            foundSources.add(matcher.group());
        }

        if (foundSources.isEmpty()) {
            return "I couldn't find enough information in your uploaded documents to answer this question.";
        }

        // 1. Extract user question
        String userQuestion = "";
        int qIdx = userPrompt.indexOf("USER QUESTION:");
        int cIdx = userPrompt.indexOf("RETRIEVED CONTEXT SOURCES:");
        if (qIdx != -1 && cIdx != -1 && cIdx > qIdx) {
            userQuestion = userPrompt.substring(qIdx + "USER QUESTION:".length(), cIdx).trim();
        }

        String questionLower = userQuestion.toLowerCase();

        // 2. Extract query terms (lowercased, length >= 3, non-stopword)
        Set<String> queryTerms = new LinkedHashSet<>();
        if (!userQuestion.isBlank()) {
            for (String term : userQuestion.toLowerCase().split("\\W+")) {
                if (term.length() >= 3 && !isQueryStopword(term)) {
                    queryTerms.add(term);
                }
            }
        }

        // 3. Extract source blocks [S1], [S2] and their content
        Pattern sourceContentPattern = Pattern.compile("\\[(S\\d+)\\][\\s\\S]*?Content:\\s*([\\s\\S]*?)(?=\\[S\\d+\\]|$)", Pattern.CASE_INSENSITIVE);
        Matcher scMatcher = sourceContentPattern.matcher(userPrompt);

        Map<String, String> sourcesContentMap = new LinkedHashMap<>();
        while (scMatcher.find()) {
            String sourceTag = "[" + scMatcher.group(1) + "]";
            String chunkContent = scMatcher.group(2).trim();
            sourcesContentMap.put(sourceTag, chunkContent);
        }

        if (sourcesContentMap.isEmpty()) {
            return "I couldn't find enough information in your uploaded documents to answer this question.";
        }

        String firstSourceTag = sourcesContentMap.keySet().iterator().next();

        // 4. Honest fallback for absent skills/technologies
        // If the query asks for a specific skill/tech that does NOT exist anywhere in the context
        Set<String> specificTechs = Set.of("rust", "swift", "golang", "ruby", "c#", "csharp", "scala", "kotlin", "vue", "angular", "flutter", "aws", "gcp", "azure", "graphql");
        for (String tech : specificTechs) {
            if (questionLower.contains(tech)) {
                boolean foundInContext = false;
                for (String content : sourcesContentMap.values()) {
                    if (content.toLowerCase().contains(tech)) {
                        foundInContext = true;
                        break;
                    }
                }
                if (!foundInContext) {
                    return "Based on your uploaded document " + firstSourceTag + ", there is no mention of experience or skills with " + capitalizeWord(tech) + ".";
                }
            }
        }

        // 5. Detect Query Intent
        boolean isSkillsQuery = questionLower.contains("skill") || questionLower.contains("technical") || questionLower.contains("language") || questionLower.contains("stack") || questionLower.contains("tools") || questionLower.contains("framework");
        boolean isExperienceQuery = questionLower.contains("experience") || questionLower.contains("work") || questionLower.contains("intern") || questionLower.contains("job") || questionLower.contains("role");
        boolean isProjectsQuery = questionLower.contains("project") || questionLower.contains("built") || questionLower.contains("portfolio") || questionLower.contains("platform");
        boolean isEducationQuery = questionLower.contains("education") || questionLower.contains("degree") || questionLower.contains("college") || questionLower.contains("university") || questionLower.contains("school");

        // 6. Section-aware extraction for matching category
        for (Map.Entry<String, String> entry : sourcesContentMap.entrySet()) {
            String sourceTag = entry.getKey();
            String chunkContent = entry.getValue();

            // A. Technical Skills Section
            if (isSkillsQuery) {
                List<String> skillLines = extractSectionContent(chunkContent, 
                        List.of("technical skills", "skills", "technologies"), 
                        List.of("experience", "projects", "education", "achievements", "certifications"));
                if (!skillLines.isEmpty()) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("Based on your uploaded document ").append(sourceTag).append(", here are the technical skills mentioned:\n\n");
                    for (String line : skillLines) {
                        sb.append("- ").append(formatCategorizedLine(line)).append(" ").append(sourceTag).append("\n");
                    }
                    return sb.toString().trim();
                }
            }

            // B. Work Experience Section
            if (isExperienceQuery) {
                List<String> expLines = extractSectionContent(chunkContent, 
                        List.of("experience", "work experience", "employment"), 
                        List.of("projects", "education", "technical skills", "skills", "achievements"));
                if (!expLines.isEmpty()) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("Based on your uploaded document ").append(sourceTag).append(", here is the relevant work experience:\n\n");
                    for (String line : expLines) {
                        sb.append("- ").append(formatBulletPoint(line)).append(" ").append(sourceTag).append("\n");
                    }
                    return sb.toString().trim();
                }
            }

            // C. Projects Section
            if (isProjectsQuery) {
                List<String> projectLines = extractSectionContent(chunkContent, 
                        List.of("projects", "personal projects", "key projects"), 
                        List.of("education", "experience", "achievements", "certifications", "skills"));
                if (!projectLines.isEmpty()) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("Based on your uploaded document ").append(sourceTag).append(", here are the project details:\n\n");
                    for (String line : projectLines) {
                        sb.append("- ").append(formatBulletPoint(line)).append(" ").append(sourceTag).append("\n");
                    }
                    return sb.toString().trim();
                }
            }

            // D. Education Section
            if (isEducationQuery) {
                List<String> eduLines = extractSectionContent(chunkContent, 
                        List.of("education", "academic background"), 
                        List.of("achievements", "certifications", "experience", "projects"));
                if (!eduLines.isEmpty()) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("Based on your uploaded document ").append(sourceTag).append(", here is the education background:\n\n");
                    for (String line : eduLines) {
                        sb.append("- ").append(formatBulletPoint(line)).append(" ").append(sourceTag).append("\n");
                    }
                    return sb.toString().trim();
                }
            }
        }

        // 7. General Keyword Matching Fallback across all chunks
        List<String> generalMatches = new ArrayList<>();
        for (Map.Entry<String, String> entry : sourcesContentMap.entrySet()) {
            String sourceTag = entry.getKey();
            String chunkContent = entry.getValue();

            String[] lines = chunkContent.split("\n+");
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i].trim();
                if (line.length() < 5) continue;

                String lower = line.toLowerCase();
                boolean matches = false;
                for (String term : queryTerms) {
                    if (lower.contains(term)) {
                        matches = true;
                        break;
                    }
                }

                if (matches && generalMatches.size() < 6) {
                    generalMatches.add(formatBulletPoint(line) + " " + sourceTag);
                    if (i + 1 < lines.length) {
                        String nextLine = lines[i + 1].trim();
                        if (nextLine.startsWith("•") || nextLine.startsWith("-") || nextLine.contains(":")) {
                            generalMatches.add(formatBulletPoint(nextLine) + " " + sourceTag);
                            i++;
                        }
                    }
                }
            }
        }

        if (!generalMatches.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append("Based on your uploaded document ").append(firstSourceTag).append(", here are the key details:\n\n");
            for (String match : generalMatches) {
                sb.append("- ").append(match).append("\n");
            }
            return sb.toString().trim();
        }

        return "I couldn't find enough information in your uploaded documents to answer this question.";
    }

    private List<String> extractSectionContent(String chunkContent, List<String> startHeaders, List<String> stopHeaders) {
        List<String> contentLines = new ArrayList<>();
        String[] lines = chunkContent.split("\n+");
        boolean inSection = false;

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;
            String lower = trimmed.toLowerCase();

            // Check if line starts the section
            boolean isStart = false;
            for (String sh : startHeaders) {
                if (lower.equals(sh) || lower.startsWith(sh + ":") || lower.startsWith(sh + " -") || lower.equals("### " + sh) || lower.equals("## " + sh)) {
                    isStart = true;
                    break;
                }
            }

            if (isStart) {
                inSection = true;
                if (trimmed.contains(":")) {
                    String afterColon = trimmed.substring(trimmed.indexOf(":") + 1).trim();
                    if (!afterColon.isEmpty()) {
                        contentLines.add(trimmed);
                    }
                }
                continue;
            }

            if (inSection) {
                // Check if line is a subsequent section header
                boolean isStop = false;
                for (String st : stopHeaders) {
                    if (lower.equals(st) || lower.startsWith(st + ":") || lower.startsWith(st + " -") || lower.equals("### " + st) || lower.equals("## " + st)) {
                        isStop = true;
                        break;
                    }
                }
                if (isStop) {
                    break;
                }

                if (trimmed.length() > 2) {
                    contentLines.add(trimmed);
                }
                if (contentLines.size() >= 8) {
                    break;
                }
            }
        }

        return contentLines;
    }

    private String formatCategorizedLine(String line) {
        String trimmed = line.trim().replaceAll("^[•\\-*\\s]+", "");
        if (trimmed.contains(":")) {
            int colonIdx = trimmed.indexOf(":");
            String category = trimmed.substring(0, colonIdx).trim();
            String details = trimmed.substring(colonIdx + 1).trim();
            return "**" + category + "**: " + details;
        }
        return trimmed;
    }

    private String formatBulletPoint(String line) {
        return line.trim().replaceAll("^[•\\-*\\s]+", "");
    }

    private boolean isQueryStopword(String word) {
        return Set.of(
                "what", "where", "which", "who", "when", "how", "are", "is", "the", "in", "my",
                "your", "this", "that", "these", "those", "does", "did", "tell", "show", "give",
                "mentioned", "resume", "document", "candidate", "about", "have", "has", "had"
        ).contains(word);
    }

    private String capitalizeWord(String str) {
        if (str == null || str.isEmpty()) return str;
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }
}
