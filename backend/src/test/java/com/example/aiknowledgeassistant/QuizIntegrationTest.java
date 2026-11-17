package com.example.aiknowledgeassistant;

import com.example.aiknowledgeassistant.dto.request.QuizGenerateRequest;
import com.example.aiknowledgeassistant.dto.request.QuizSubmitRequest;
import com.example.aiknowledgeassistant.dto.request.RegisterRequest;
import com.example.aiknowledgeassistant.entity.QuestionCategory;
import com.example.aiknowledgeassistant.entity.QuizDifficulty;
import com.example.aiknowledgeassistant.entity.QuizMode;
import com.example.aiknowledgeassistant.repository.DocumentRepository;
import com.example.aiknowledgeassistant.repository.QuizRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
public class QuizIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private QuizRepository quizRepository;

    @Autowired
    private DocumentRepository documentRepository;

    private String userToken;
    private Long resumeDocId;

    @BeforeEach
    public void setup() throws Exception {
        quizRepository.deleteAll();
        documentRepository.deleteAll();

        // 1. Register test user
        RegisterRequest registerReq = RegisterRequest.builder()
                .name("Quiz Tester")
                .email("quiz_tester@example.com")
                .password("Password123!")
                .build();

        MvcResult authResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andReturn();

        if (authResult.getResponse().getStatus() == 201) {
            userToken = objectMapper.readTree(authResult.getResponse().getContentAsString()).get("token").asText();
        } else {
            MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(registerReq)))
                    .andReturn();
            userToken = objectMapper.readTree(loginResult.getResponse().getContentAsString()).get("token").asText();
        }

        // 2. Create resume PDF with technical skills
        byte[] pdfBytes;
        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            doc.addPage(page);
            try (PDPageContentStream cos = new PDPageContentStream(doc, page)) {
                cos.beginText();
                cos.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 12);
                cos.newLineAtOffset(50, 750);
                cos.showText("Alex Morgan - Full Stack Engineer Resume");
                cos.newLineAtOffset(0, -20);
                cos.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);
                cos.showText("Technical Skills: Java, Spring Boot, React.js, MongoDB, PostgreSQL, Docker, REST APIs");
                cos.newLineAtOffset(0, -20);
                cos.showText("Experience: Developed enterprise backend services with Spring Boot and React frontend.");
                cos.endText();
            }
            doc.save(baos);
            pdfBytes = baos.toByteArray();
        }

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "alex-morgan-resume.pdf",
                "application/pdf",
                pdfBytes
        );

        MvcResult uploadResult = mockMvc.perform(multipart("/api/documents/upload")
                        .file(file)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode uploadNode = objectMapper.readTree(uploadResult.getResponse().getContentAsString());
        resumeDocId = uploadNode.get("id").asLong();
        assertNotNull(resumeDocId);
    }

    @AfterEach
    public void tearDown() {
        quizRepository.deleteAll();
        documentRepository.deleteAll();
    }

    @Test
    public void testExtractSkillsFromDocumentEndpoint() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/quiz/skills/" + resumeDocId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentId").value(resumeDocId))
                .andExpect(jsonPath("$.allSkills", hasItem("Java")))
                .andExpect(jsonPath("$.allSkills", hasItem("Spring Boot")))
                .andExpect(jsonPath("$.allSkills", hasItem("React")))
                .andExpect(jsonPath("$.allSkills", hasItem("MongoDB")))
                .andExpect(jsonPath("$.categorizedSkills").isMap())
                .andExpect(jsonPath("$.suggestedTopics", not(empty())))
                .andReturn();

        String content = result.getResponse().getContentAsString();
        assertTrue(content.contains("Programming Languages"));
    }

    @Test
    public void testGenerateSkillBasedQuizAndVerifyAnswerSecurity() throws Exception {
        QuizGenerateRequest genReq = new QuizGenerateRequest(
                resumeDocId,
                QuizMode.SKILL_BASED,
                QuizDifficulty.INTERMEDIATE,
                5,
                List.of(QuestionCategory.CONCEPTUAL, QuestionCategory.CODING, QuestionCategory.DEBUGGING),
                null
        );

        MvcResult genResult = mockMvc.perform(post("/api/quiz/generate")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(genReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.mode").value("SKILL_BASED"))
                .andExpect(jsonPath("$.questions", hasSize(5)))
                // CRITICAL SECURITY ASSERTION: Correct answers and explanations MUST NOT be in the client DTO!
                .andExpect(jsonPath("$.questions[0].correctAnswerIndex").doesNotExist())
                .andExpect(jsonPath("$.questions[0].explanation").doesNotExist())
                .andExpect(jsonPath("$.questions[0].options", hasSize(4)))
                .andReturn();

        JsonNode quizNode = objectMapper.readTree(genResult.getResponse().getContentAsString());
        Long quizId = quizNode.get("id").asLong();

        // Verify that questions test deep technical concepts rather than resume superficialities
        String question1 = quizNode.get("questions").get(0).get("questionText").asText();
        assertFalse(question1.toLowerCase().contains("which programming language is mentioned in your resume"),
                "Generated questions must test technical concepts, not resume mentions!");

        // Test Submit Answers
        Map<Long, Integer> answers = new HashMap<>();
        for (JsonNode qNode : quizNode.get("questions")) {
            Long qId = qNode.get("id").asLong();
            answers.put(qId, 0); // choose option 0
        }

        QuizSubmitRequest submitReq = new QuizSubmitRequest(answers);

        MvcResult submitResult = mockMvc.perform(post("/api/quiz/" + quizId + "/submit")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(submitReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quizId").value(quizId))
                .andExpect(jsonPath("$.totalQuestions").value(5))
                .andExpect(jsonPath("$.score").isNumber())
                .andExpect(jsonPath("$.percentage").isNumber())
                .andExpect(jsonPath("$.questionResults", hasSize(5)))
                // Upon submission, explanations and correct answers are now revealed for review
                .andExpect(jsonPath("$.questionResults[0].correctAnswerIndex").exists())
                .andExpect(jsonPath("$.questionResults[0].explanation").exists())
                .andExpect(jsonPath("$.topicBreakdown").exists())
                .andExpect(jsonPath("$.difficultyBreakdown").exists())
                .andReturn();

        // Test User Quiz History Endpoint
        mockMvc.perform(get("/api/quiz")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].id").value(quizId))
                .andExpect(jsonPath("$[0].completed").value(true))
                .andExpect(jsonPath("$[0].latestScore").exists());

        // Test Quiz Evaluation Endpoint
        mockMvc.perform(get("/api/quiz/" + quizId + "/evaluate")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalQuestionsEvaluated").value(5))
                .andExpect(jsonPath("$.skillRelevanceRate").value(greaterThan(0.0)))
                .andExpect(jsonPath("$.technicalAccuracyRate").value(greaterThan(0.0)))
                .andExpect(jsonPath("$.duplicateRate").value(0.0));
    }

    @Test
    public void testDocumentBasedQuizGeneration() throws Exception {
        QuizGenerateRequest genReq = new QuizGenerateRequest(
                resumeDocId,
                QuizMode.DOCUMENT_BASED,
                QuizDifficulty.INTERMEDIATE,
                2,
                List.of(QuestionCategory.CONCEPTUAL),
                null
        );

        mockMvc.perform(post("/api/quiz/generate")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(genReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.mode").value("DOCUMENT_BASED"))
                .andExpect(jsonPath("$.questions", not(empty())))
                .andExpect(jsonPath("$.questions[0].groundingType").value("DOCUMENT_GROUNDED"));
    }

    @Test
    public void testUnauthorizedUserCannotAccessAnotherUsersQuiz() throws Exception {
        // 1. Generate quiz for User A
        QuizGenerateRequest genReq = new QuizGenerateRequest(
                resumeDocId,
                QuizMode.SKILL_BASED,
                QuizDifficulty.BEGINNER,
                3,
                null,
                "Java"
        );

        MvcResult genResult = mockMvc.perform(post("/api/quiz/generate")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(genReq)))
                .andExpect(status().isCreated())
                .andReturn();

        Long quizId = objectMapper.readTree(genResult.getResponse().getContentAsString()).get("id").asLong();

        // 2. Register User B
        RegisterRequest userBReq = RegisterRequest.builder()
                .name("User B")
                .email("user_b_quiz@example.com")
                .password("Password123!")
                .build();

        MvcResult userBAuth = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userBReq)))
                .andReturn();
        String userBToken = objectMapper.readTree(userBAuth.getResponse().getContentAsString()).get("token").asText();

        // 3. User B attempts to access User A's quiz -> Should return 404 (Not Found for that user)
        mockMvc.perform(get("/api/quiz/" + quizId)
                        .header("Authorization", "Bearer " + userBToken))
                .andExpect(status().isNotFound());

        // 4. User B attempts to submit User A's quiz -> Should return 404
        QuizSubmitRequest submitReq = new QuizSubmitRequest(Map.of(1L, 0));
        mockMvc.perform(post("/api/quiz/" + quizId + "/submit")
                        .header("Authorization", "Bearer " + userBToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(submitReq)))
                .andExpect(status().isNotFound());
    }
}
