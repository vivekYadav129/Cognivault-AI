package com.example.aiknowledgeassistant.service.quiz;

import com.example.aiknowledgeassistant.entity.*;
import com.example.aiknowledgeassistant.service.ai.rag.LlmService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class QuizGenerationService {

    private static final Logger logger = LoggerFactory.getLogger(QuizGenerationService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final LlmService llmService;
    private final SkillExtractionService skillExtractionService;

    public QuizGenerationService(LlmService llmService, SkillExtractionService skillExtractionService) {
        this.llmService = llmService;
        this.skillExtractionService = skillExtractionService;
    }

    public static class RawGeneratedQuestion {
        public String question;
        public List<String> options;
        public int correctAnswer;
        public String explanation;
        public QuestionCategory category;
        public QuizDifficulty difficulty;
        public GroundingType groundingType;
        public String skillTag;
        public String codeSnippet;
        public String sourceCitation;
        public String signature;
    }

    public static class GenerationResult {
        public String title;
        public String description;
        public List<RawGeneratedQuestion> questions = new ArrayList<>();
    }

    public GenerationResult generateQuiz(
            QuizMode mode,
            QuizDifficulty difficulty,
            int questionCount,
            List<QuestionCategory> categories,
            String topicFocus,
            List<String> extractedSkills,
            String documentContext,
            String documentTitle,
            Set<String> previousSignatures) {

        if (questionCount <= 0) questionCount = 5;
        if (categories == null || categories.isEmpty()) {
            categories = List.of(
                    QuestionCategory.CONCEPTUAL,
                    QuestionCategory.SCENARIO,
                    QuestionCategory.CODING,
                    QuestionCategory.DEBUGGING,
                    QuestionCategory.OUTPUT_PREDICTION,
                    QuestionCategory.REAL_WORLD
            );
        }

        // Determine target skills
        List<String> targetSkills = new ArrayList<>();
        if (topicFocus != null && !topicFocus.isBlank()) {
            targetSkills.add(topicFocus.trim());
        }
        if (extractedSkills != null && !extractedSkills.isEmpty()) {
            targetSkills.addAll(extractedSkills);
        }
        if (targetSkills.isEmpty()) {
            targetSkills.addAll(List.of("Java", "Spring Boot", "React", "PostgreSQL", "REST APIs", "RAG"));
        }

        logger.info("Generating quiz in mode {}, difficulty {}, target question count {}, skills: {}",
                mode, difficulty, questionCount, targetSkills);

        GenerationResult result = null;

        // Try LLM Generation (with bounded retry)
        int attempts = 0;
        int maxAttempts = 2;
        while (attempts < maxAttempts && (result == null || result.questions.size() < questionCount)) {
            attempts++;
            try {
                String systemPrompt = buildSystemPrompt(mode, difficulty, categories);
                String userPrompt = buildUserPrompt(mode, difficulty, questionCount, categories,
                        topicFocus, targetSkills, documentContext, documentTitle, previousSignatures);

                String rawLlmResponse = llmService.generateAnswer(systemPrompt, userPrompt);
                GenerationResult parsed = parseAndValidateLlmResponse(
                        rawLlmResponse, mode, difficulty, targetSkills, previousSignatures);

                if (parsed != null && !parsed.questions.isEmpty()) {
                    result = parsed;
                    logger.info("Successfully generated {} questions via LLM on attempt {}",
                            result.questions.size(), attempts);
                    break;
                }
            } catch (Exception e) {
                logger.warn("LLM quiz generation attempt {} encountered: {}", attempts, e.getMessage());
            }
        }

        // Deterministic Fallback if LLM unavailable or returned insufficient questions
        if (result == null || result.questions.size() < questionCount) {
            logger.info("Using authentic deterministic technical generator to fulfill requested question count of {}", questionCount);
            result = generateDeterministicQuiz(
                    mode, difficulty, questionCount, categories, topicFocus,
                    targetSkills, documentContext, documentTitle, previousSignatures);
        }

        // Ensure title and description are set
        if (result.title == null || result.title.isBlank()) {
            result.title = formatQuizTitle(mode, difficulty, topicFocus, documentTitle);
        }
        if (result.description == null || result.description.isBlank()) {
            result.description = formatQuizDescription(mode, difficulty, targetSkills);
        }

        return result;
    }

    private String buildSystemPrompt(QuizMode mode, QuizDifficulty difficulty, List<QuestionCategory> categories) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are the Cognivault AI Senior Technical Assessment Engine.\n");
        sb.append("Your mission is to generate deep, rigorous, multiple-choice questions assessing REAL technical competence and domain knowledge.\n\n");

        sb.append("STRICT RULES:\n");
        sb.append("1. NEVER ask superficial resume questions (e.g., 'Which skill is mentioned on page 1?').\n");
        sb.append("2. When a skill like Java, Spring Boot, React, MongoDB, etc. is provided, ask substantive questions testing concepts, trade-offs, internal mechanics, code outputs, and debugging.\n");
        sb.append("3. Match difficulty level strictly: ").append(difficulty.name()).append(".\n");
        sb.append("4. Every question MUST have exactly 4 plausible options, with exactly one unambiguously correct answer (indexed 0, 1, 2, or 3).\n");
        sb.append("5. Distractors (wrong options) must represent common developer misconceptions, off-by-one errors, or inverted logic—never obvious nonsense.\n");
        sb.append("6. Provide an in-depth explanation detailing WHY the correct option is right and WHY the key distractors are incorrect.\n");

        if (mode == QuizMode.DOCUMENT_BASED) {
            sb.append("7. MODE = DOCUMENT_BASED: Questions must test facts, architectures, and concepts described in the retrieved document chunks. Every question must include a citation tag [S1], [S2] etc.\n");
        } else if (mode == QuizMode.SKILL_BASED) {
            sb.append("7. MODE = SKILL_BASED: Test the candidate's actual mastery of the technical skills using your comprehensive computer science knowledge.\n");
        } else {
            sb.append("7. MODE = MIXED: Provide a blend of document-grounded questions and deep technical skill questions. Set groundingType accordingly ('DOCUMENT_GROUNDED' or 'TECHNICAL_ASSESSMENT').\n");
        }

        sb.append("8. Output MUST be ONLY a single JSON object with no introductory or trailing text. Schema:\n");
        sb.append("{\n");
        sb.append("  \"title\": \"Assessment Title\",\n");
        sb.append("  \"description\": \"Assessment Description\",\n");
        sb.append("  \"questions\": [\n");
        sb.append("    {\n");
        sb.append("      \"question\": \"Question text?\",\n");
        sb.append("      \"options\": [\"Option A\", \"Option B\", \"Option C\", \"Option D\"],\n");
        sb.append("      \"correctAnswer\": 0,\n");
        sb.append("      \"explanation\": \"Detailed explanation...\",\n");
        sb.append("      \"category\": \"CONCEPTUAL\",\n");
        sb.append("      \"difficulty\": \"").append(difficulty.name()).append("\",\n");
        sb.append("      \"groundingType\": \"TECHNICAL_ASSESSMENT\",\n");
        sb.append("      \"skillTag\": \"Java\",\n");
        sb.append("      \"codeSnippet\": \"optional code snippet\",\n");
        sb.append("      \"sourceCitation\": \"[S1] Section 2.1 (only for document-grounded)\"\n");
        sb.append("    }\n");
        sb.append("  ]\n");
        sb.append("}\n");

        return sb.toString();
    }

    private String buildUserPrompt(
            QuizMode mode,
            QuizDifficulty difficulty,
            int questionCount,
            List<QuestionCategory> categories,
            String topicFocus,
            List<String> targetSkills,
            String documentContext,
            String documentTitle,
            Set<String> previousSignatures) {

        StringBuilder sb = new StringBuilder();
        sb.append("GENERATE EXACTLY ").append(questionCount).append(" MULTIPLE CHOICE QUESTIONS.\n\n");
        sb.append("QUIZ MODE: ").append(mode.name()).append("\n");
        sb.append("DIFFICULTY: ").append(difficulty.name()).append("\n");
        sb.append("TARGET SKILLS & TOPICS: ").append(String.join(", ", targetSkills)).append("\n");
        if (topicFocus != null && !topicFocus.isBlank()) {
            sb.append("PRIMARY FOCUS TOPIC: ").append(topicFocus.trim()).append("\n");
        }
        sb.append("CATEGORIES TO COVER: ").append(categories).append("\n\n");

        if (documentContext != null && !documentContext.isBlank()) {
            sb.append("DOCUMENT CONTEXT (Document: ").append(documentTitle != null ? documentTitle : "Uploaded Document").append("):\n");
            sb.append(documentContext).append("\n\n");
        }

        if (previousSignatures != null && !previousSignatures.isEmpty()) {
            sb.append("IMPORTANT: Do NOT repeat previously asked questions. Generate completely fresh questions and scenarios.\n");
        }

        return sb.toString();
    }

    private GenerationResult parseAndValidateLlmResponse(
            String rawLlmResponse,
            QuizMode mode,
            QuizDifficulty difficulty,
            List<String> targetSkills,
            Set<String> previousSignatures) {

        if (rawLlmResponse == null || rawLlmResponse.isBlank()) {
            return null;
        }

        // Extract JSON string
        String jsonStr = null;
        Matcher m = Pattern.compile("```json\\s*([\\s\\S]*?)\\s*```").matcher(rawLlmResponse);
        if (m.find()) {
            jsonStr = m.group(1).trim();
        } else {
            int openBrace = rawLlmResponse.indexOf("{");
            int closeBrace = rawLlmResponse.lastIndexOf("}");
            if (openBrace != -1 && closeBrace != -1 && closeBrace > openBrace) {
                jsonStr = rawLlmResponse.substring(openBrace, closeBrace + 1).trim();
            }
        }

        if (jsonStr == null) {
            return null;
        }

        try {
            JsonNode root = MAPPER.readTree(jsonStr);
            JsonNode questionsNode = root.get("questions");
            if (questionsNode == null || !questionsNode.isArray() || questionsNode.isEmpty()) {
                return null;
            }

            GenerationResult result = new GenerationResult();
            result.title = root.has("title") ? root.get("title").asText() : null;
            result.description = root.has("description") ? root.get("description").asText() : null;

            Set<String> currentQuizSignatures = new HashSet<>();

            for (JsonNode qNode : questionsNode) {
                String questionText = qNode.has("question") ? qNode.get("question").asText().trim() : "";
                if (questionText.length() < 15) continue;

                // Options validation
                JsonNode optsNode = qNode.get("options");
                if (optsNode == null || !optsNode.isArray() || optsNode.size() != 4) {
                    continue;
                }

                List<String> options = new ArrayList<>();
                for (JsonNode opt : optsNode) {
                    String optText = opt.asText().trim();
                    if (!optText.isEmpty() && !options.contains(optText)) {
                        options.add(optText);
                    }
                }
                if (options.size() != 4) continue; // Must have 4 distinct options

                int correctAns = qNode.has("correctAnswer") ? qNode.get("correctAnswer").asInt(0) : 0;
                if (correctAns < 0 || correctAns >= 4) correctAns = 0;

                String explanation = qNode.has("explanation") ? qNode.get("explanation").asText().trim() : "";
                if (explanation.length() < 15) {
                    explanation = "Option " + (char)('A' + correctAns) + " is correct based on core engineering principles.";
                }

                String categoryStr = qNode.has("category") ? qNode.get("category").asText() : "CONCEPTUAL";
                QuestionCategory category = parseCategory(categoryStr);

                String diffStr = qNode.has("difficulty") ? qNode.get("difficulty").asText() : difficulty.name();
                QuizDifficulty diff = parseDifficulty(diffStr, difficulty);

                String gTypeStr = qNode.has("groundingType") ? qNode.get("groundingType").asText() : null;
                GroundingType gType = parseGroundingType(gTypeStr, mode);

                String skillTag = qNode.has("skillTag") ? qNode.get("skillTag").asText() : null;
                if (skillTag == null || skillTag.isBlank()) {
                    skillTag = targetSkills.isEmpty() ? "Engineering" : targetSkills.get(0);
                }

                String codeSnippet = qNode.has("codeSnippet") && !qNode.get("codeSnippet").isNull()
                        ? qNode.get("codeSnippet").asText().trim()
                        : null;

                String sourceCitation = qNode.has("sourceCitation") && !qNode.get("sourceCitation").isNull()
                        ? qNode.get("sourceCitation").asText().trim()
                        : null;

                // Duplicate checking
                String signature = computeQuestionSignature(questionText);
                if (currentQuizSignatures.contains(signature)) continue;
                if (previousSignatures != null && previousSignatures.contains(signature)) continue;

                currentQuizSignatures.add(signature);

                RawGeneratedQuestion q = new RawGeneratedQuestion();
                q.question = questionText;
                q.options = options;
                q.correctAnswer = correctAns;
                q.explanation = explanation;
                q.category = category;
                q.difficulty = diff;
                q.groundingType = gType;
                q.skillTag = skillTag;
                q.codeSnippet = (codeSnippet != null && !codeSnippet.isBlank()) ? codeSnippet : null;
                q.sourceCitation = sourceCitation;
                q.signature = signature;

                result.questions.add(q);
            }

            return result;

        } catch (Exception e) {
            logger.warn("Failed to parse LLM JSON into questions: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Authentic deterministic technical assessment generator.
     * Provides comprehensive question pools for Java, Spring Boot, React, MongoDB, PostgreSQL, REST APIs, RAG, etc.
     * Ensures offline and test environments receive genuinely deep technical questions with zero Cognivault trivia.
     */
    public GenerationResult generateDeterministicQuiz(
            QuizMode mode,
            QuizDifficulty difficulty,
            int questionCount,
            List<QuestionCategory> categories,
            String topicFocus,
            List<String> targetSkills,
            String documentContext,
            String documentTitle,
            Set<String> previousSignatures) {

        GenerationResult result = new GenerationResult();
        result.title = formatQuizTitle(mode, difficulty, topicFocus, documentTitle);
        result.description = formatQuizDescription(mode, difficulty, targetSkills);

        List<RawGeneratedQuestion> pool = new ArrayList<>();

        // 1. If DOCUMENT_BASED or MIXED, generate document-grounded questions from chunks
        if ((mode == QuizMode.DOCUMENT_BASED || mode == QuizMode.MIXED) && documentContext != null && !documentContext.isBlank()) {
            pool.addAll(generateDocumentGroundedQuestions(documentContext, documentTitle, difficulty));
        }

        // 2. If SKILL_BASED or MIXED, generate deep skill assessment questions
        if (mode == QuizMode.SKILL_BASED || mode == QuizMode.MIXED || pool.isEmpty()) {
            for (String skill : targetSkills) {
                pool.addAll(getTechnicalQuestionsForSkill(skill, difficulty));
            }
        }

        // 3. Fallback generic technical questions if pool is still small
        if (pool.size() < questionCount) {
            pool.addAll(getTechnicalQuestionsForSkill("Java", difficulty));
            pool.addAll(getTechnicalQuestionsForSkill("Spring Boot", difficulty));
            pool.addAll(getTechnicalQuestionsForSkill("React", difficulty));
            pool.addAll(getTechnicalQuestionsForSkill("PostgreSQL", difficulty));
        }

        // Filter out previously seen questions and deduplicate
        Set<String> selectedSignatures = new HashSet<>();
        for (RawGeneratedQuestion q : pool) {
            if (result.questions.size() >= questionCount) break;

            if (selectedSignatures.contains(q.signature)) continue;
            if (previousSignatures != null && previousSignatures.contains(q.signature)) continue;

            selectedSignatures.add(q.signature);
            result.questions.add(q);
        }

        // If strict duplicate filtering left us under count, allow pool items to satisfy question count
        if (result.questions.size() < questionCount) {
            for (RawGeneratedQuestion q : pool) {
                if (result.questions.size() >= questionCount) break;
                if (!result.questions.contains(q)) {
                    result.questions.add(q);
                }
            }
        }

        return result;
    }

    private List<RawGeneratedQuestion> getTechnicalQuestionsForSkill(String skill, QuizDifficulty difficulty) {
        String normalized = skillExtractionService.normalizeSkill(skill);
        List<RawGeneratedQuestion> list = new ArrayList<>();

        if ("Java".equalsIgnoreCase(normalized)) {
            list.add(createQuestion(
                    "In Java's memory model, how does ConcurrentHashMap achieve thread-safe reads without acquiring a lock on the entire map?",
                    List.of(
                            "By reading directly from volatile Node table pointers and volatile value fields without locking",
                            "By duplicating the entire hash table into a thread-local buffer on every write",
                            "By locking the entire table with a global ReentrantReadWriteLock",
                            "By converting all read operations into synchronized blocks across all buckets"
                    ),
                    0,
                    "ConcurrentHashMap achieves non-blocking concurrent reads by defining bucket table pointers and node value fields as volatile, enabling memory visibility without acquiring segment locks.",
                    QuestionCategory.CONCEPTUAL,
                    QuizDifficulty.ADVANCED,
                    GroundingType.TECHNICAL_ASSESSMENT,
                    "Java",
                    null,
                    null
            ));

            list.add(createQuestion(
                    "Consider the following Java code snippet. What is the output?",
                    List.of("false, true", "true, false", "true, true", "false, false"),
                    1,
                    "Integer.valueOf caches values between -128 and 127, so a and b reference the identical cached object (true). Numbers above 127 (like 200) instantiate separate heap objects, making == compare distinct memory references (false).",
                    QuestionCategory.OUTPUT_PREDICTION,
                    QuizDifficulty.INTERMEDIATE,
                    GroundingType.TECHNICAL_ASSESSMENT,
                    "Java",
                    "Integer a = 100;\nInteger b = 100;\nInteger c = 200;\nInteger d = 200;\nSystem.out.println((a == b) + \", \" + (c == d));",
                    null
            ));

            list.add(createQuestion(
                    "Why must both equals() and hashCode() be overridden together when creating custom keys for a HashMap?",
                    List.of(
                            "Because unequal objects might produce unequal hash codes and cause OutOfMemoryError",
                            "Because two objects considered equal by equals() MUST return the exact same hashCode() to locate the correct hash bucket",
                            "Because HashMap requires hashCode() to return an absolute positive integer",
                            "Because Java compiler throws a compilation error if only one is overridden"
                    ),
                    1,
                    "The contract between equals() and hashCode() mandates that if obj1.equals(obj2), then obj1.hashCode() == obj2.hashCode(). If violated, HashMap will look in the wrong bucket and fail to retrieve the entry.",
                    QuestionCategory.CONCEPTUAL,
                    QuizDifficulty.BEGINNER,
                    GroundingType.TECHNICAL_ASSESSMENT,
                    "Java",
                    null,
                    null
            ));

            list.add(createQuestion(
                    "What happens when an unhandled RuntimeException is thrown inside a Java CompletableFuture supplyAsync pipeline?",
                    List.of(
                            "The exception is swallowed silently and join() returns null",
                            "The future completes exceptionally and calling join() or get() throws CompletionException wrapping the original cause",
                            "The JVM terminates the entire application immediately",
                            "The thread pool automatically restarts the task up to 3 times"
                    ),
                    1,
                    "When an exception occurs inside CompletableFuture stages, the future transitions to an exceptionally completed state. Attempting to retrieve the result via get() or join() raises an ExecutionException or CompletionException.",
                    QuestionCategory.DEBUGGING,
                    QuizDifficulty.ADVANCED,
                    GroundingType.TECHNICAL_ASSESSMENT,
                    "Java",
                    null,
                    null
            ));
        } else if ("Spring Boot".equalsIgnoreCase(normalized) || "Spring Security".equalsIgnoreCase(normalized)) {
            list.add(createQuestion(
                    "Why is constructor-based dependency injection preferred over field injection with @Autowired in modern Spring Boot applications?",
                    List.of(
                            "It enables immutability with final fields and allows easy instantiation in unit tests without reflection or a Spring context",
                            "Field injection is completely deprecated and removed in Spring Boot 3",
                            "Constructor injection executes asynchronously whereas field injection blocks the main thread",
                            "Constructor injection disables singleton caching and increases throughput"
                    ),
                    0,
                    "Constructor injection promotes class immutability (fields can be marked final), ensures all mandatory dependencies are satisfied at object creation, and simplifies unit testing without mocking reflection utilities.",
                    QuestionCategory.CONCEPTUAL,
                    QuizDifficulty.INTERMEDIATE,
                    GroundingType.TECHNICAL_ASSESSMENT,
                    "Spring Boot",
                    null,
                    null
            ));

            list.add(createQuestion(
                    "In Spring declarative transaction management (@Transactional), which type of exception triggers an automatic rollback by default?",
                    List.of(
                            "Any checked Exception (inheriting from java.lang.Exception)",
                            "Only java.sql.SQLException and IOException",
                            "Unchecked exceptions (RuntimeException and Error)",
                            "All exceptions including checked exceptions unless explicitly marked with noRollbackFor"
                    ),
                    2,
                    "By default, Spring's transaction interceptor only marks a transaction for rollback on unchecked exceptions (instances of RuntimeException or Error). Checked exceptions do not cause rollback unless configured with rollbackFor = Exception.class.",
                    QuestionCategory.SCENARIO,
                    QuizDifficulty.INTERMEDIATE,
                    GroundingType.TECHNICAL_ASSESSMENT,
                    "Spring Boot",
                    null,
                    null
            ));

            list.add(createQuestion(
                    "How does Spring Security validate a stateless JWT token without querying a centralized session database on every request?",
                    List.of(
                            "By cryptographically verifying the HMAC or RSA signature of the token using the secret/public key",
                            "By verifying the IP address of the incoming client socket against a local cookie cache",
                            "By calling an external OAuth provider for every single request",
                            "By storing the full user record in a temporary browser local storage index"
                    ),
                    0,
                    "Stateless JWT authentication verifies authenticity by recalculating the cryptographic signature over the Base64URL-encoded header and payload using the server's secret key. If the signature matches and expiration is valid, the identity is authenticated.",
                    QuestionCategory.CONCEPTUAL,
                    QuizDifficulty.INTERMEDIATE,
                    GroundingType.TECHNICAL_ASSESSMENT,
                    "Spring Boot",
                    null,
                    null
            ));
        } else if ("React".equalsIgnoreCase(normalized)) {
            list.add(createQuestion(
                    "In React, what problem occurs when a state updater function depends on a previous state value but does NOT use the callback form (e.g. setCount(count + 1) vs setCount(prev => prev + 1)) during batched events?",
                    List.of(
                            "Stale state closure: consecutive calls within the same event loop tick operate on the same initial captured state and overwrite each other",
                            "Memory leak: React allocates a new DOM node for every updater invocation",
                            "Hydration error: The server render does not match the client bundle",
                            "Infinite re-render loop: React re-triggers useEffect continuously"
                    ),
                    0,
                    "When state updates are batched, setCount(count + 1) reads count from the current render's closure. Calling it twice will not increment by 2; both read the same count. The functional updater setCount(prev => prev + 1) guarantees access to the latest queued state.",
                    QuestionCategory.DEBUGGING,
                    QuizDifficulty.INTERMEDIATE,
                    GroundingType.TECHNICAL_ASSESSMENT,
                    "React",
                    null,
                    null
            ));

            list.add(createQuestion(
                    "What is the key performance difference between useMemo and useCallback in React?",
                    List.of(
                            "useMemo caches the result of invoking a calculation function, while useCallback caches the function definition itself between renders",
                            "useCallback runs before render while useMemo runs asynchronously after DOM mutation",
                            "useMemo is exclusively for server components while useCallback is for client components",
                            "There is no difference; useCallback is simply an alias for useMemo"
                    ),
                    0,
                    "useMemo invokes its factory function and caches the returned computed value. useCallback caches the function reference itself so it can be passed to memoized child components without triggering unnecessary re-renders.",
                    QuestionCategory.CONCEPTUAL,
                    QuizDifficulty.BEGINNER,
                    GroundingType.TECHNICAL_ASSESSMENT,
                    "React",
                    null,
                    null
            ));

            list.add(createQuestion(
                    "Why should array indices NOT be used as the 'key' prop for dynamically reordered or filtered lists in React?",
                    List.of(
                            "Because React throws an uncaught JavaScript TypeError when a key is a number",
                            "Because index keys disrupt component identity during reordering, leading to incorrect state binding in child components and inefficient DOM updates",
                            "Because index keys bypass the Virtual DOM and write directly to document.body",
                            "Because index keys disable Tailwind CSS stylesheet matching"
                    ),
                    1,
                    "Keys serve as stable identifiers for React's reconciliation algorithm. When items are reordered or deleted, index-based keys cause React to preserve the wrong child component internal state and perform unnecessary DOM mutations.",
                    QuestionCategory.REAL_WORLD,
                    QuizDifficulty.INTERMEDIATE,
                    GroundingType.TECHNICAL_ASSESSMENT,
                    "React",
                    null,
                    null
            ));
        } else if ("MongoDB".equalsIgnoreCase(normalized)) {
            list.add(createQuestion(
                    "In MongoDB aggregation pipelines, why is placing a $match stage as early as possible considered a critical performance best practice?",
                    List.of(
                            "Because an initial $match can utilize database indexes and drastically reduce the number of documents passed to downstream pipeline stages",
                            "Because $match converts BSON documents into relational SQL tables",
                            "Because MongoDB automatically cancels any aggregation where $match is not the first stage",
                            "Because subsequent stages like $sort and $group cannot process unindexed documents"
                    ),
                    0,
                    "When $match occurs at the start of an aggregation pipeline, the query planner can leverage indexes to filter out non-matching documents immediately, saving CPU and memory for subsequent stages like $group or $unwind.",
                    QuestionCategory.REAL_WORLD,
                    QuizDifficulty.INTERMEDIATE,
                    GroundingType.TECHNICAL_ASSESSMENT,
                    "MongoDB",
                    null,
                    null
            ));

            list.add(createQuestion(
                    "What is a 'covered query' in MongoDB and why does it offer maximum retrieval throughput?",
                    List.of(
                            "A query encrypted with SSL/TLS in transit",
                            "A query where all fields requested in the projection are part of an index, allowing MongoDB to return results directly from the index without inspecting disk documents",
                            "A query that automatically retries across all replica set nodes",
                            "A query executed inside an isolated transaction"
                    ),
                    1,
                    "A covered query is satisfied entirely by index keys. Because all projected fields exist in the index and the _id field is excluded (or included in the index), MongoDB never needs to load the actual document from RAM or disk.",
                    QuestionCategory.CONCEPTUAL,
                    QuizDifficulty.ADVANCED,
                    GroundingType.TECHNICAL_ASSESSMENT,
                    "MongoDB",
                    null,
                    null
            ));
        } else if ("PostgreSQL".equalsIgnoreCase(normalized)) {
            list.add(createQuestion(
                    "In PostgreSQL with the pgvector extension, what is the primary benefit of creating an HNSW (Hierarchical Navigable Small World) index over an IVFFlat index?",
                    List.of(
                            "HNSW provides significantly higher recall at low query latencies and does not require pre-training or a rebuild when adding new vectors",
                            "HNSW uses zero disk space because vectors are stored in memory only",
                            "HNSW limits dimensions to exactly 32 dimensions",
                            "IVFFlat is deprecated in PostgreSQL 16"
                    ),
                    0,
                    "HNSW constructs a multi-layer graph that delivers sub-millisecond approximate nearest neighbor search with high recall without requiring index re-training as the database grows, unlike IVFFlat which clusters into static centroids.",
                    QuestionCategory.CONCEPTUAL,
                    QuizDifficulty.ADVANCED,
                    GroundingType.TECHNICAL_ASSESSMENT,
                    "PostgreSQL",
                    null,
                    null
            ));

            list.add(createQuestion(
                    "What type of anomaly does the PostgreSQL default 'Read Committed' transaction isolation level prevent, and which anomaly can still occur?",
                    List.of(
                            "Prevents dirty reads; non-repeatable reads and phantom reads can still occur",
                            "Prevents phantom reads; dirty reads can still occur",
                            "Prevents all anomalies including serialization failures",
                            "Prevents non-repeatable reads; dirty reads can still occur"
                    ),
                    0,
                    "In Read Committed, transactions only see data committed before the query began (preventing dirty reads). However, if another transaction commits an update between two queries within the same transaction, non-repeatable reads can occur.",
                    QuestionCategory.CONCEPTUAL,
                    QuizDifficulty.ADVANCED,
                    GroundingType.TECHNICAL_ASSESSMENT,
                    "PostgreSQL",
                    null,
                    null
            ));
        } else if ("REST APIs".equalsIgnoreCase(normalized)) {
            list.add(createQuestion(
                    "According to HTTP/1.1 specifications (RFC 9110), which HTTP methods MUST be idempotent?",
                    List.of(
                            "GET, HEAD, PUT, DELETE, OPTIONS, TRACE",
                            "POST, PUT, PATCH, DELETE",
                            "Only GET and POST",
                            "Only POST and PUT"
                    ),
                    0,
                    "Idempotence means multiple identical requests have the exact same effect on the server as a single request. GET, HEAD, PUT, DELETE, OPTIONS, and TRACE are idempotent, while POST and PATCH are generally not.",
                    QuestionCategory.CONCEPTUAL,
                    QuizDifficulty.BEGINNER,
                    GroundingType.TECHNICAL_ASSESSMENT,
                    "REST APIs",
                    null,
                    null
            ));
        } else if ("RAG".equalsIgnoreCase(normalized)) {
            list.add(createQuestion(
                    "In a Retrieval-Augmented Generation (RAG) pipeline, why is overlapping text chunks (e.g. chunk size 1000 with 150 overlap) recommended?",
                    List.of(
                            "To prevent semantic context and sentences from being fractured across arbitrary chunk boundaries",
                            "To double the size of the database index artificially",
                            "To force the LLM to generate duplicate answers",
                            "To reduce the number of tokens sent to the embedding model"
                    ),
                    0,
                    "Fixed-size chunking can split sentences or concepts across boundaries. A chunk overlap ensures boundary context is preserved in both adjacent chunks, maximizing semantic similarity during vector search.",
                    QuestionCategory.CONCEPTUAL,
                    QuizDifficulty.INTERMEDIATE,
                    GroundingType.TECHNICAL_ASSESSMENT,
                    "RAG",
                    null,
                    null
            ));
        } else {
            // General engineering question for other skills
            list.add(createQuestion(
                    "When designing high-throughput distributed systems with " + normalized + ", what is the primary role of a Circuit Breaker pattern?",
                    List.of(
                            "To immediately fail fast and prevent cascading system outages when an upstream dependency becomes unresponsive",
                            "To encrypt all payload packets with AES-256",
                            "To automatically increase the CPU clock speed of the host server",
                            "To compress JSON messages into binary Protobuf"
                    ),
                    0,
                    "A Circuit Breaker detects downstream failures and trips open to prevent resource exhaustion (thread starvation, socket exhaustion) across caller microservices, allowing the failing service time to recover.",
                    QuestionCategory.SCENARIO,
                    QuizDifficulty.INTERMEDIATE,
                    GroundingType.TECHNICAL_ASSESSMENT,
                    normalized,
                    null,
                    null
            ));
        }

        return list;
    }

    private List<RawGeneratedQuestion> generateDocumentGroundedQuestions(String context, String docTitle, QuizDifficulty difficulty) {
        List<RawGeneratedQuestion> list = new ArrayList<>();
        if (context == null || context.isBlank()) return list;

        // Parse sentences / paragraphs from context
        String cleanContext = context.replaceAll("\\[S\\d+\\]", "").trim();
        String[] lines = cleanContext.split("\\n+");

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.length() > 60 && !trimmed.contains("==") && !trimmed.contains("--")) {
                list.add(createQuestion(
                        "Based on the uploaded document (" + (docTitle != null ? docTitle : "Document") + "), which statement is accurate?",
                        List.of(
                                trimmed,
                                "The document states that the architecture strictly disallows external database persistence.",
                                "All functionality is performed client-side without any backend REST API integration.",
                                "The system operates as an unverified standalone batch script."
                        ),
                        0,
                        "This statement is directly verified in the source text [S1]: \"" + (trimmed.length() > 100 ? trimmed.substring(0, 100) + "..." : trimmed) + "\"",
                        QuestionCategory.CONCEPTUAL,
                        difficulty,
                        GroundingType.DOCUMENT_GROUNDED,
                        "Document Knowledge",
                        null,
                        "[S1]"
                ));
                if (list.size() >= 3) break;
            }
        }
        return list;
    }

    private RawGeneratedQuestion createQuestion(
            String text, List<String> options, int correctIdx, String explanation,
            QuestionCategory category, QuizDifficulty diff, GroundingType gType,
            String skillTag, String codeSnippet, String citation) {

        RawGeneratedQuestion q = new RawGeneratedQuestion();
        q.question = text;
        q.options = options;
        q.correctAnswer = correctIdx;
        q.explanation = explanation;
        q.category = category;
        q.difficulty = diff;
        q.groundingType = gType;
        q.skillTag = skillTag;
        q.codeSnippet = codeSnippet;
        q.sourceCitation = citation;
        q.signature = computeQuestionSignature(text);
        return q;
    }

    public static String computeQuestionSignature(String text) {
        if (text == null) return "";
        String normalized = text.toLowerCase().replaceAll("[^a-z0-9]", "").trim();
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(normalized.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return String.valueOf(normalized.hashCode());
        }
    }

    private String formatQuizTitle(QuizMode mode, QuizDifficulty difficulty, String topicFocus, String docTitle) {
        if (topicFocus != null && !topicFocus.isBlank()) {
            return topicFocus.trim() + " " + capitalize(difficulty.name()) + " Technical Assessment";
        }
        if (mode == QuizMode.DOCUMENT_BASED) {
            return (docTitle != null ? docTitle : "Document") + " Grounded Assessment";
        }
        if (mode == QuizMode.MIXED) {
            return "Mixed Technical & Document Assessment (" + capitalize(difficulty.name()) + ")";
        }
        return "Skill-Based Technical Assessment (" + capitalize(difficulty.name()) + ")";
    }

    private String formatQuizDescription(QuizMode mode, QuizDifficulty difficulty, List<String> skills) {
        String skillsStr = skills.isEmpty() ? "Core Engineering" : String.join(", ", skills.subList(0, Math.min(4, skills.size())));
        if (mode == QuizMode.DOCUMENT_BASED) {
            return "Assess your understanding of the architecture, features, and facts described in the uploaded document.";
        }
        return "Comprehensive " + difficulty.name().toLowerCase() + "-level evaluation covering: " + skillsStr + ".";
    }

    private QuestionCategory parseCategory(String str) {
        if (str == null) return QuestionCategory.CONCEPTUAL;
        try {
            return QuestionCategory.valueOf(str.trim().toUpperCase());
        } catch (Exception e) {
            return QuestionCategory.CONCEPTUAL;
        }
    }

    private QuizDifficulty parseDifficulty(String str, QuizDifficulty defaultDiff) {
        if (str == null) return defaultDiff;
        try {
            return QuizDifficulty.valueOf(str.trim().toUpperCase());
        } catch (Exception e) {
            return defaultDiff;
        }
    }

    private GroundingType parseGroundingType(String str, QuizMode mode) {
        if (str != null) {
            try {
                return GroundingType.valueOf(str.trim().toUpperCase());
            } catch (Exception ignored) {}
        }
        return mode == QuizMode.DOCUMENT_BASED ? GroundingType.DOCUMENT_GROUNDED : GroundingType.TECHNICAL_ASSESSMENT;
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return "";
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }
}

