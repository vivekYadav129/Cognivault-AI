package com.example.aiknowledgeassistant;

import com.example.aiknowledgeassistant.dto.request.LoginRequest;
import com.example.aiknowledgeassistant.dto.request.RegisterRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
public class SecurityAndValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String userAToken;
    private String userBToken;

    @BeforeEach
    public void setupUsers() throws Exception {
        userAToken = obtainToken("User A", "user.a.sec@example.com", "Password123!");
        userBToken = obtainToken("User B", "user.b.sec@example.com", "Password123!");
    }

    private String obtainToken(String name, String email, String password) throws Exception {
        RegisterRequest registerReq = RegisterRequest.builder()
                .name(name)
                .email(email)
                .password(password)
                .build();

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andReturn();

        if (result.getResponse().getStatus() == 201) {
            return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
        }

        LoginRequest loginReq = LoginRequest.builder().email(email).password(password).build();
        MvcResult loginRes = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andReturn();
        return objectMapper.readTree(loginRes.getResponse().getContentAsString()).get("token").asText();
    }

    @Test
    @DisplayName("Should reject unauthenticated requests to protected endpoints")
    public void testUnauthenticatedAccessRejected() throws Exception {
        mockMvc.perform(get("/api/documents"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should reject file upload containing directory traversal in filename")
    public void testRejectPathTraversalUpload() throws Exception {
        MockMultipartFile maliciousFile = new MockMultipartFile(
                "file",
                "../../etc/passwd.txt",
                "text/plain",
                "malicious text content".getBytes()
        );

        mockMvc.perform(multipart("/api/documents/upload")
                        .file(maliciousFile)
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Filename contains illegal path traversal characters."));
    }

    @Test
    @DisplayName("Should reject disallowed executable file extensions")
    public void testRejectDisallowedExtensions() throws Exception {
        MockMultipartFile exeFile = new MockMultipartFile(
                "file",
                "malware.exe",
                "application/octet-stream",
                "MZ9000...".getBytes()
        );

        mockMvc.perform(multipart("/api/documents/upload")
                        .file(exeFile)
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Unsupported file format")));
    }

    @Test
    @DisplayName("Should reject empty file uploads")
    public void testRejectEmptyFileUpload() throws Exception {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "empty.txt",
                "text/plain",
                new byte[0]
        );

        mockMvc.perform(multipart("/api/documents/upload")
                        .file(emptyFile)
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Uploaded file cannot be empty."));
    }

    @Test
    @DisplayName("Should reject fake PDF missing standard %PDF magic header")
    public void testRejectFakePdfMagicBytes() throws Exception {
        MockMultipartFile fakePdf = new MockMultipartFile(
                "file",
                "fake.pdf",
                "application/pdf",
                "This is not a real PDF file".getBytes()
        );

        mockMvc.perform(multipart("/api/documents/upload")
                        .file(fakePdf)
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid PDF header. File is not a valid PDF document."));
    }

    @Test
    @DisplayName("Should strictly forbid User B from accessing or deleting User A's uploaded documents")
    public void testCrossUserDocumentAccessForbidden() throws Exception {
        // 1. User A uploads a valid text document
        MockMultipartFile userAFile = new MockMultipartFile(
                "file",
                "userA-classified-notes.txt",
                "text/plain",
                "Cognivault AI production security guidelines and enterprise vault isolation.".getBytes()
        );

        MvcResult uploadRes = mockMvc.perform(multipart("/api/documents/upload")
                        .file(userAFile)
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isCreated())
                .andReturn();

        long documentId = objectMapper.readTree(uploadRes.getResponse().getContentAsString()).get("id").asLong();

        // 2. User B attempts to view User A's document details -> Expect 401 Unauthorized
        mockMvc.perform(get("/api/documents/" + documentId)
                        .header("Authorization", "Bearer " + userBToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("You are not authorized to access this document."));

        // 3. User B attempts to view User A's document chunks -> Expect 401 Unauthorized
        mockMvc.perform(get("/api/documents/" + documentId + "/chunks")
                        .header("Authorization", "Bearer " + userBToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("You are not authorized to access this document."));

        // 4. User B attempts to delete User A's document -> Expect 401 Unauthorized
        mockMvc.perform(delete("/api/documents/" + documentId)
                        .header("Authorization", "Bearer " + userBToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("You are not authorized to access this document."));

        // 5. User A can view and delete their own document -> Expect 200 OK and 204 No Content
        mockMvc.perform(get("/api/documents/" + documentId)
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/documents/" + documentId)
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isNoContent());
    }
}
