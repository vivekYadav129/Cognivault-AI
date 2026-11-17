package com.example.aiknowledgeassistant;

import com.example.aiknowledgeassistant.dto.request.LoginRequest;
import com.example.aiknowledgeassistant.dto.request.RegisterRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
public class DocumentIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String user1Token;
    private String user2Token;

    @BeforeEach
    public void setupUsers() throws Exception {
        // Register User 1
        RegisterRequest user1Req = RegisterRequest.builder()
                .name("Alice Knowledge")
                .email("alice.doc@example.com")
                .password("Password123")
                .build();

        MvcResult res1 = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user1Req)))
                .andReturn();

        if (res1.getResponse().getStatus() == 201) {
            user1Token = objectMapper.readTree(res1.getResponse().getContentAsString()).get("token").asText();
        } else {
            // If already registered, login
            LoginRequest login1 = LoginRequest.builder().email("alice.doc@example.com").password("Password123").build();
            MvcResult l1 = mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(login1)))
                    .andReturn();
            user1Token = objectMapper.readTree(l1.getResponse().getContentAsString()).get("token").asText();
        }

        // Register User 2
        RegisterRequest user2Req = RegisterRequest.builder()
                .name("Bob Intruder")
                .email("bob.doc@example.com")
                .password("Password123")
                .build();

        MvcResult res2 = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user2Req)))
                .andReturn();

        if (res2.getResponse().getStatus() == 201) {
            user2Token = objectMapper.readTree(res2.getResponse().getContentAsString()).get("token").asText();
        } else {
            LoginRequest login2 = LoginRequest.builder().email("bob.doc@example.com").password("Password123").build();
            MvcResult l2 = mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(login2)))
                    .andReturn();
            user2Token = objectMapper.readTree(l2.getResponse().getContentAsString()).get("token").asText();
        }
    }

    @Test
    public void testDocumentUploadFetchChunksAndSecurityIsolation() throws Exception {
        // 1. Upload TXT file as User 1
        String fileContent = "TCP/IP Protocol Suite Overview.\n\n" +
                "Transmission Control Protocol (TCP) is a core protocol of the Internet protocol suite. " +
                "It originated in the initial network implementation in which it complemented the Internet Protocol (IP).\n\n" +
                "TCP provides reliable, ordered, and error-checked delivery of a stream of octets between applications running on hosts communicating via an IP network.";

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "tcp-notes.txt",
                "text/plain",
                fileContent.getBytes()
        );

        MvcResult uploadResult = mockMvc.perform(multipart("/api/documents/upload")
                        .file(file)
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.originalFileName").value("tcp-notes.txt"))
                .andExpect(jsonPath("$.status").value("PROCESSED"))
                .andExpect(jsonPath("$.chunkCount").value(1))
                .andReturn();

        Long docId = objectMapper.readTree(uploadResult.getResponse().getContentAsString()).get("id").asLong();

        // 2. Fetch Document List for User 1
        mockMvc.perform(get("/api/documents")
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1))
                .andExpect(jsonPath("$.documents[0].id").value(docId));

        // 3. Fetch Document Chunks for User 1
        mockMvc.perform(get("/api/documents/" + docId + "/chunks")
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].chunkIndex").value(0))
                .andExpect(jsonPath("$[0].content").value(org.hamcrest.Matchers.containsString("TCP/IP Protocol Suite")));

        // 4. Security Check: User 2 attempt to access User 1's document should return 401/403 (UnauthorizedException)
        mockMvc.perform(get("/api/documents/" + docId)
                        .header("Authorization", "Bearer " + user2Token))
                .andExpect(status().isUnauthorized());

        // 5. Security Check: User 2 attempt to delete User 1's document should fail
        mockMvc.perform(delete("/api/documents/" + docId)
                        .header("Authorization", "Bearer " + user2Token))
                .andExpect(status().isUnauthorized());

        // 6. Delete Document as User 1
        mockMvc.perform(delete("/api/documents/" + docId)
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isNoContent());

        // Verify document is deleted
        mockMvc.perform(get("/api/documents/" + docId)
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testUnsupportedFileFormatRejection() throws Exception {
        MockMultipartFile invalidFile = new MockMultipartFile(
                "file",
                "malicious.exe",
                "application/octet-stream",
                "binary content".getBytes()
        );

        mockMvc.perform(multipart("/api/documents/upload")
                        .file(invalidFile)
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Unsupported file format")));
    }
}
