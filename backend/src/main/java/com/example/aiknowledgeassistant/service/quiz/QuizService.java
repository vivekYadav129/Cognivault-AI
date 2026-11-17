package com.example.aiknowledgeassistant.service.quiz;

import com.example.aiknowledgeassistant.dto.request.QuizGenerateRequest;
import com.example.aiknowledgeassistant.dto.request.QuizSubmitRequest;
import com.example.aiknowledgeassistant.dto.response.*;
import com.example.aiknowledgeassistant.entity.*;
import com.example.aiknowledgeassistant.exception.BadRequestException;
import com.example.aiknowledgeassistant.exception.ResourceNotFoundException;
import com.example.aiknowledgeassistant.repository.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class QuizService {

    private static final Logger logger = LoggerFactory.getLogger(QuizService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final QuizRepository quizRepository;
    private final QuizQuestionRepository quizQuestionRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final UserRepository userRepository;
    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final SkillExtractionService skillExtractionService;
    private final QuizGenerationService quizGenerationService;
    private final QuizEvaluationService quizEvaluationService;

    public QuizService(
            QuizRepository quizRepository,
            QuizQuestionRepository quizQuestionRepository,
            QuizAttemptRepository quizAttemptRepository,
            UserRepository userRepository,
            DocumentRepository documentRepository,
            DocumentChunkRepository documentChunkRepository,
            SkillExtractionService skillExtractionService,
            QuizGenerationService quizGenerationService,
            QuizEvaluationService quizEvaluationService) {
        this.quizRepository = quizRepository;
        this.quizQuestionRepository = quizQuestionRepository;
        this.quizAttemptRepository = quizAttemptRepository;
        this.userRepository = userRepository;
        this.documentRepository = documentRepository;
        this.documentChunkRepository = documentChunkRepository;
        this.skillExtractionService = skillExtractionService;
        this.quizGenerationService = quizGenerationService;
        this.quizEvaluationService = quizEvaluationService;
    }

    @Transactional
    public QuizResponse generateQuiz(QuizGenerateRequest request, String userEmail) {
        User user = getUserByEmail(userEmail);

        Document document = null;
        List<String> extractedSkills = new ArrayList<>();
        String documentContext = "";

        if (request.getDocumentId() != null) {
            document = documentRepository.findByIdAndUserId(request.getDocumentId(), user.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Document not found with ID: " + request.getDocumentId()));

            // 1. Extract technical skills from document text
            String extractedText = document.getExtractedText();
            if (extractedText != null && !extractedText.isBlank()) {
                SkillExtractionService.SkillExtractionResult extractionResult =
                        skillExtractionService.extractSkills(extractedText);
                extractedSkills.addAll(extractionResult.getAllSkills());
            }

            // 2. Load document chunks for document-based or mixed grounding
            if (request.getMode() == QuizMode.DOCUMENT_BASED || request.getMode() == QuizMode.MIXED) {
                List<DocumentChunk> chunks = documentChunkRepository
                        .findAllByDocumentIdOrderByChunkIndexAsc(document.getId());

                StringBuilder contextBuilder = new StringBuilder();
                int chunkCount = Math.min(chunks.size(), 8);
                for (int i = 0; i < chunkCount; i++) {
                    DocumentChunk chunk = chunks.get(i);
                    contextBuilder.append("[S").append(i + 1).append("] ")
                            .append(chunk.getContent())
                            .append("\n\n");
                }
                documentContext = contextBuilder.toString();
            }
        }

        // 3. Fetch previous question signatures by this user to guarantee novelty and avoid duplicates
        Set<String> previousSignatures = quizQuestionRepository.findQuestionSignaturesByUserId(user.getId());

        // 4. Generate quiz questions
        QuizGenerationService.GenerationResult generation = quizGenerationService.generateQuiz(
                request.getMode(),
                request.getDifficulty(),
                request.getQuestionCount(),
                request.getCategories(),
                request.getTopicFocus(),
                extractedSkills,
                documentContext,
                document != null ? document.getOriginalFileName() : null,
                previousSignatures
        );

        if (generation.questions.isEmpty()) {
            throw new BadRequestException("Could not generate questions matching the specified configuration. Please try with different skills or topic focus.");
        }

        // 5. Persist Quiz entity
        Quiz quiz = Quiz.builder()
                .user(user)
                .document(document)
                .mode(request.getMode())
                .difficulty(request.getDifficulty())
                .title(generation.title)
                .description(generation.description)
                .topicFocus(request.getTopicFocus())
                .createdAt(LocalDateTime.now())
                .build();

        Quiz savedQuiz = quizRepository.save(quiz);

        // 6. Persist QuizQuestion entities
        List<QuizQuestion> questionEntities = new ArrayList<>();
        for (int i = 0; i < generation.questions.size(); i++) {
            QuizGenerationService.RawGeneratedQuestion raw = generation.questions.get(i);

            QuizQuestion question = QuizQuestion.builder()
                    .quiz(savedQuiz)
                    .questionIndex(i)
                    .questionText(raw.question)
                    .options(raw.options)
                    .correctAnswerIndex(raw.correctAnswer)
                    .explanation(raw.explanation)
                    .category(raw.category)
                    .difficulty(raw.difficulty)
                    .groundingType(raw.groundingType)
                    .skillTag(raw.skillTag)
                    .codeSnippet(raw.codeSnippet)
                    .questionSignature(raw.signature)
                    .sourceCitation(raw.sourceCitation)
                    .build();

            questionEntities.add(question);
        }

        List<QuizQuestion> savedQuestions = quizQuestionRepository.saveAll(questionEntities);
        savedQuiz.setQuestions(savedQuestions);

        logger.info("Saved new quiz ID {} with {} questions for user {}", savedQuiz.getId(), savedQuestions.size(), userEmail);

        return mapToQuizResponse(savedQuiz, false);
    }

    @Transactional
    public QuizResultResponse submitQuiz(Long quizId, QuizSubmitRequest request, String userEmail) {
        User user = getUserByEmail(userEmail);
        Quiz quiz = quizRepository.findByIdAndUserId(quizId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Quiz not found with ID: " + quizId));

        List<QuizQuestion> questions = quizQuestionRepository.findAllByQuizIdOrderByQuestionIndexAsc(quizId);
        if (questions.isEmpty()) {
            throw new BadRequestException("This quiz has no questions to evaluate.");
        }

        Map<Long, Integer> answers = request.getAnswers() != null ? request.getAnswers() : Collections.emptyMap();

        int score = 0;
        int total = questions.size();

        Map<String, int[]> topicStats = new HashMap<>(); // topic -> [correct, total]
        Map<String, int[]> diffStats = new HashMap<>();  // difficulty -> [correct, total]

        List<QuestionResultDto> resultList = new ArrayList<>();

        for (QuizQuestion q : questions) {
            Integer selectedOption = answers.get(q.getId());
            boolean isCorrect = selectedOption != null && selectedOption.equals(q.getCorrectAnswerIndex());

            if (isCorrect) {
                score++;
            }

            // Track topic performance
            String topicKey = q.getSkillTag() != null && !q.getSkillTag().isBlank() ? q.getSkillTag() : "General";
            topicStats.putIfAbsent(topicKey, new int[]{0, 0});
            topicStats.get(topicKey)[1]++;
            if (isCorrect) topicStats.get(topicKey)[0]++;

            // Track difficulty performance
            String diffKey = q.getDifficulty() != null ? q.getDifficulty().name() : "INTERMEDIATE";
            diffStats.putIfAbsent(diffKey, new int[]{0, 0});
            diffStats.get(diffKey)[1]++;
            if (isCorrect) diffStats.get(diffKey)[0]++;

            resultList.add(new QuestionResultDto(
                    q.getId(),
                    q.getQuestionIndex(),
                    q.getQuestionText(),
                    q.getOptions(),
                    selectedOption,
                    q.getCorrectAnswerIndex(),
                    isCorrect,
                    q.getExplanation(),
                    q.getCategory(),
                    q.getDifficulty(),
                    q.getGroundingType(),
                    q.getSkillTag(),
                    q.getCodeSnippet(),
                    q.getSourceCitation()
            ));
        }

        double percentage = total > 0 ? Math.round(((double) score / total) * 1000.0) / 10.0 : 0.0;

        // Build Breakdown maps
        Map<String, QuizResultResponse.TopicScore> topicBreakdown = new LinkedHashMap<>();
        for (Map.Entry<String, int[]> entry : topicStats.entrySet()) {
            topicBreakdown.put(entry.getKey(), new QuizResultResponse.TopicScore(entry.getValue()[0], entry.getValue()[1]));
        }

        Map<String, QuizResultResponse.TopicScore> diffBreakdown = new LinkedHashMap<>();
        for (Map.Entry<String, int[]> entry : diffStats.entrySet()) {
            diffBreakdown.put(entry.getKey(), new QuizResultResponse.TopicScore(entry.getValue()[0], entry.getValue()[1]));
        }

        // Persist QuizAttempt
        String userAnswersJson;
        String topicBreakdownJson;
        String diffBreakdownJson;
        try {
            userAnswersJson = MAPPER.writeValueAsString(answers);
            topicBreakdownJson = MAPPER.writeValueAsString(topicBreakdown);
            diffBreakdownJson = MAPPER.writeValueAsString(diffBreakdown);
        } catch (Exception e) {
            userAnswersJson = "{}";
            topicBreakdownJson = "{}";
            diffBreakdownJson = "{}";
        }

        QuizAttempt attempt = QuizAttempt.builder()
                .quiz(quiz)
                .user(user)
                .score(score)
                .totalQuestions(total)
                .percentage(percentage)
                .userAnswersJson(userAnswersJson)
                .topicBreakdownJson(topicBreakdownJson)
                .difficultyBreakdownJson(diffBreakdownJson)
                .completedAt(LocalDateTime.now())
                .build();

        QuizAttempt savedAttempt = quizAttemptRepository.save(attempt);

        logger.info("Evaluated quiz ID {}, attempt ID {}: score {}/{} ({}%) for user {}",
                quizId, savedAttempt.getId(), score, total, percentage, userEmail);

        return new QuizResultResponse(
                savedAttempt.getId(),
                quiz.getId(),
                score,
                total,
                percentage,
                topicBreakdown,
                diffBreakdown,
                resultList,
                savedAttempt.getCompletedAt()
        );
    }

    public List<QuizSummaryResponse> getUserQuizzes(String userEmail) {
        User user = getUserByEmail(userEmail);
        List<Quiz> quizzes = quizRepository.findAllByUserIdOrderByCreatedAtDesc(user.getId());

        return quizzes.stream().map(q -> {
            Optional<QuizAttempt> latestAttempt = quizAttemptRepository.findFirstByQuizIdOrderByCompletedAtDesc(q.getId());
            Integer latestScore = latestAttempt.map(QuizAttempt::getScore).orElse(null);
            Double latestPercentage = latestAttempt.map(QuizAttempt::getPercentage).orElse(null);
            boolean isCompleted = latestAttempt.isPresent();

            int count = q.getQuestions() != null ? q.getQuestions().size() : 0;
            String docName = q.getDocument() != null ? q.getDocument().getOriginalFileName() : null;
            Long docId = q.getDocument() != null ? q.getDocument().getId() : null;

            return new QuizSummaryResponse(
                    q.getId(),
                    q.getTitle(),
                    q.getMode(),
                    q.getDifficulty(),
                    q.getTopicFocus(),
                    count,
                    docId,
                    docName,
                    q.getCreatedAt(),
                    latestScore,
                    latestPercentage,
                    isCompleted
            );
        }).collect(Collectors.toList());
    }

    public QuizResponse getQuizById(Long quizId, String userEmail) {
        User user = getUserByEmail(userEmail);
        Quiz quiz = quizRepository.findByIdAndUserId(quizId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Quiz not found with ID: " + quizId));

        return mapToQuizResponse(quiz, false);
    }

    public SkillExtractionResponse extractSkillsFromDocument(Long documentId, String userEmail) {
        User user = getUserByEmail(userEmail);
        Document document = documentRepository.findByIdAndUserId(documentId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with ID: " + documentId));

        String text = document.getExtractedText();
        SkillExtractionService.SkillExtractionResult result = skillExtractionService.extractSkills(text);

        return new SkillExtractionResponse(
                document.getId(),
                document.getOriginalFileName(),
                result.getCategorizedSkills(),
                result.getAllSkills(),
                result.getSuggestedTopics()
        );
    }

    public QuizEvaluationReport evaluateQuizById(Long quizId, String userEmail) {
        User user = getUserByEmail(userEmail);
        Quiz quiz = quizRepository.findByIdAndUserId(quizId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Quiz not found with ID: " + quizId));

        String docText = quiz.getDocument() != null ? quiz.getDocument().getExtractedText() : null;
        List<String> targetSkills = new ArrayList<>();
        if (quiz.getTopicFocus() != null && !quiz.getTopicFocus().isBlank()) {
            targetSkills.add(quiz.getTopicFocus());
        }
        if (quiz.getDocument() != null && docText != null) {
            targetSkills.addAll(skillExtractionService.extractSkills(docText).getAllSkills());
        }

        return quizEvaluationService.evaluateQuiz(quiz, targetSkills, docText);
    }

    private QuizResponse mapToQuizResponse(Quiz quiz, boolean includeAnswers) {
        List<QuizQuestionDto> questionDtos = new ArrayList<>();

        if (quiz.getQuestions() != null) {
            for (QuizQuestion q : quiz.getQuestions()) {
                questionDtos.add(new QuizQuestionDto(
                        q.getId(),
                        q.getQuestionIndex(),
                        q.getQuestionText(),
                        q.getOptions(),
                        q.getCategory(),
                        q.getDifficulty(),
                        q.getGroundingType(),
                        q.getSkillTag(),
                        q.getCodeSnippet(),
                        q.getSourceCitation()
                ));
            }
        }

        Long docId = quiz.getDocument() != null ? quiz.getDocument().getId() : null;
        String docName = quiz.getDocument() != null ? quiz.getDocument().getOriginalFileName() : null;

        return new QuizResponse(
                quiz.getId(),
                quiz.getTitle(),
                quiz.getDescription(),
                quiz.getMode(),
                quiz.getDifficulty(),
                quiz.getTopicFocus(),
                docId,
                docName,
                questionDtos,
                quiz.getCreatedAt()
        );
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }
}

