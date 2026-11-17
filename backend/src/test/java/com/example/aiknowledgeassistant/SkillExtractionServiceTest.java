package com.example.aiknowledgeassistant;

import com.example.aiknowledgeassistant.service.quiz.SkillExtractionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class SkillExtractionServiceTest {

    private SkillExtractionService skillExtractionService;

    @BeforeEach
    public void setup() {
        skillExtractionService = new SkillExtractionService();
    }

    @Test
    public void testExtractSkillsFromResumeText() {
        String resumeText = """
                ALEX MORGAN - Senior Full Stack Engineer
                SUMMARY:
                Experienced software engineer specializing in Java, Spring Boot, React.js, and PostgreSQL.
                
                TECHNICAL SKILLS:
                - Languages: Java, Python, TypeScript, SQL
                - Frameworks: Spring Boot, Spring Security, React, Next.js, FastAPI
                - Databases: MongoDB, PostgreSQL, Redis
                - Cloud/DevOps: Docker, Kubernetes, AWS, Git, CI/CD
                - AI & ML: RAG, pgvector, Vector Embeddings, LLMs
                - Architecture: Microservices, REST APIs, Kafka, JWT
                """;

        SkillExtractionService.SkillExtractionResult result = skillExtractionService.extractSkills(resumeText);

        assertNotNull(result);
        List<String> allSkills = result.getAllSkills();
        assertTrue(allSkills.contains("Java"), "Should extract Java");
        assertTrue(allSkills.contains("Spring Boot"), "Should extract Spring Boot");
        assertTrue(allSkills.contains("React"), "Should normalize React.js to React");
        assertTrue(allSkills.contains("MongoDB"), "Should extract MongoDB");
        assertTrue(allSkills.contains("PostgreSQL"), "Should extract PostgreSQL");
        assertTrue(allSkills.contains("REST APIs"), "Should extract REST APIs");
        assertTrue(allSkills.contains("RAG"), "Should extract RAG");
        assertTrue(allSkills.contains("Docker"), "Should extract Docker");

        // Verify categorization
        Map<String, List<String>> categorized = result.getCategorizedSkills();
        assertFalse(categorized.isEmpty());
        assertTrue(categorized.containsKey("Programming Languages"));
        assertTrue(categorized.containsKey("Frameworks & Libraries"));
        assertTrue(categorized.containsKey("Databases & Storage"));

        // Verify suggested topics
        List<String> topics = result.getSuggestedTopics();
        assertFalse(topics.isEmpty());
        assertTrue(topics.stream().anyMatch(t -> t.contains("Collections") || t.contains("OOP") || t.contains("JVM")));
    }

    @Test
    public void testNormalizeSkillAliases() {
        assertEquals("React", skillExtractionService.normalizeSkill("react.js"));
        assertEquals("React", skillExtractionService.normalizeSkill("reactjs"));
        assertEquals("Spring Boot", skillExtractionService.normalizeSkill("springboot"));
        assertEquals("PostgreSQL", skillExtractionService.normalizeSkill("postgres"));
        assertEquals("Kubernetes", skillExtractionService.normalizeSkill("k8s"));
        assertEquals("MongoDB", skillExtractionService.normalizeSkill("mongo"));
        assertEquals("Go", skillExtractionService.normalizeSkill("golang"));
    }

    @Test
    public void testEmptyOrNullResumeText() {
        SkillExtractionService.SkillExtractionResult emptyResult = skillExtractionService.extractSkills("");
        assertNotNull(emptyResult);
        assertTrue(emptyResult.getAllSkills().isEmpty());

        SkillExtractionService.SkillExtractionResult nullResult = skillExtractionService.extractSkills(null);
        assertNotNull(nullResult);
        assertTrue(nullResult.getAllSkills().isEmpty());
    }

    @Test
    public void testGetTopicsForKnownSkill() {
        List<String> javaTopics = skillExtractionService.getTopicsForSkill("Java");
        assertNotNull(javaTopics);
        assertTrue(javaTopics.size() >= 4);
        assertTrue(javaTopics.stream().anyMatch(t -> t.toLowerCase().contains("concurrency") || t.toLowerCase().contains("multithreading")));

        List<String> reactTopics = skillExtractionService.getTopicsForSkill("React");
        assertNotNull(reactTopics);
        assertTrue(reactTopics.stream().anyMatch(t -> t.toLowerCase().contains("hooks") || t.toLowerCase().contains("state")));
    }
}

