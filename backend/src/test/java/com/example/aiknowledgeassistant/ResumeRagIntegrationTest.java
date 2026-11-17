package com.example.aiknowledgeassistant;

import com.example.aiknowledgeassistant.dto.request.RagQueryRequest;
import com.example.aiknowledgeassistant.dto.request.RegisterRequest;
import com.example.aiknowledgeassistant.repository.DocumentRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
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
import java.io.File;
import java.nio.file.Files;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
public class ResumeRagIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DocumentRepository documentRepository;

    private String userToken;
    private Long resumeDocId;

    @BeforeEach
    public void setup() throws Exception {
        documentRepository.deleteAll();

        // 1. Register test user
        RegisterRequest registerReq = RegisterRequest.builder()
                .name("Resume Candidate")
                .email("candidate_test@example.com")
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

        // 2. Obtain resume PDF bytes (from existing uploaded resume file if available, or generate realistic resume PDF)
        byte[] pdfBytes;
        File existingPdf = new File("uploads/26d98a8e-fbaa-4174-9c59-9ab248e1710a.pdf");
        if (existingPdf.exists()) {
            pdfBytes = Files.readAllBytes(existingPdf.toPath());
        } else {
            try (PDDocument doc = new PDDocument(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                PDPage page = new PDPage();
                doc.addPage(page);
                try (PDPageContentStream cos = new PDPageContentStream(doc, page)) {
                    cos.beginText();
                    cos.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 12);
                    cos.newLineAtOffset(50, 750);
                    cos.showText("Alex Morgan - Software Engineer Resume");
                    cos.newLineAtOffset(0, -20);
                    cos.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);
                    cos.showText("Technical Skills: Java, Spring Boot, React, PostgreSQL, Docker, Kubernetes");
                    cos.newLineAtOffset(0, -20);
                    cos.showText("Experience: Full Stack Engineer at TechCorp (2022-2024)");
                    cos.newLineAtOffset(0, -20);
                    cos.showText("Developed microservices, REST APIs, and vector search systems with 99.9% uptime.");
                    cos.endText();
                }
                doc.save(baos);
                pdfBytes = baos.toByteArray();
            }
        }

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "candidate-resume.pdf",
                "application/pdf",
                pdfBytes
        );

        // 3. Upload resume document
        MvcResult uploadResult = mockMvc.perform(multipart("/api/documents/upload")
                        .file(file)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.chunkCount", greaterThan(0)))
                .andExpect(jsonPath("$.status").value("PROCESSED"))
                .andReturn();

        JsonNode uploadNode = objectMapper.readTree(uploadResult.getResponse().getContentAsString());
        resumeDocId = uploadNode.get("id").asLong();
        assertNotNull(resumeDocId);
    }

    @Test
    public void testResumeSkillsQueryReturnsGroundedAnswerAndCitations() throws Exception {
        RagQueryRequest queryReq = new RagQueryRequest("What technical skills are mentioned in my resume?", 5, resumeDocId);

        MvcResult result = mockMvc.perform(post("/api/rag/query")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(queryReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sourcesUsed", greaterThan(0)))
                .andExpect(jsonPath("$.citations", not(empty())))
                .andExpect(jsonPath("$.citations[0].documentId").value(resumeDocId))
                .andExpect(jsonPath("$.citations[0].documentName").value("candidate-resume.pdf"))
                .andReturn();

        String responseContent = result.getResponse().getContentAsString();
        JsonNode jsonNode = objectMapper.readTree(responseContent);
        String answer = jsonNode.get("answer").asText();

        assertNotNull(answer);
        assertTrue(answer.contains("[S"), "Answer must contain source citation tag [S...]");
        // Verify that the answer includes actual technical skill content and not merely a heading
        assertTrue(answer.toLowerCase().contains("java") || answer.toLowerCase().contains("react") || answer.toLowerCase().contains("spring"),
                "Answer must contain specific technical skills (e.g. Java, React, Spring), got: " + answer);
    }

    @Test
    public void testResumeExperienceQueryReturnsGroundedAnswer() throws Exception {
        RagQueryRequest queryReq = new RagQueryRequest("What is the candidate's work experience?", 5, resumeDocId);

        MvcResult result = mockMvc.perform(post("/api/rag/query")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(queryReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sourcesUsed", greaterThan(0)))
                .andExpect(jsonPath("$.citations", not(empty())))
                .andExpect(jsonPath("$.answer", not(containsString("couldn't find enough information"))))
                .andReturn();

        String answer = objectMapper.readTree(result.getResponse().getContentAsString()).get("answer").asText();
        assertTrue(answer.toLowerCase().contains("intern") || answer.toLowerCase().contains("software") || answer.toLowerCase().contains("development"),
                "Answer must contain work experience details, got: " + answer);
    }

    @Test
    public void testAbsentSkillQueryReturnsHonestFallback() throws Exception {
        RagQueryRequest queryReq = new RagQueryRequest("What experience does the candidate have in Rust programming?", 5, resumeDocId);

        MvcResult result = mockMvc.perform(post("/api/rag/query")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(queryReq)))
                .andExpect(status().isOk())
                .andReturn();

        String answer = objectMapper.readTree(result.getResponse().getContentAsString()).get("answer").asText();
        assertTrue(answer.toLowerCase().contains("no mention") || answer.toLowerCase().contains("couldn't find enough information"),
                "Expected honest fallback for unmentioned skill, got: " + answer);
    }

    @Test
    public void testUnrelatedQueryTriggersNoContextGuard() throws Exception {
        RagQueryRequest queryReq = new RagQueryRequest("How do I bake a chocolate cake?", 5, resumeDocId);

        mockMvc.perform(post("/api/rag/query")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(queryReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sourcesUsed").value(0))
                .andExpect(jsonPath("$.citations", empty()))
                .andExpect(jsonPath("$.answer", containsString("couldn't find enough information")));
    }
}
