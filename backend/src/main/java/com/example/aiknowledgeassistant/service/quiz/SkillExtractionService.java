package com.example.aiknowledgeassistant.service.quiz;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;

@Service
public class SkillExtractionService {

    private static final Logger logger = LoggerFactory.getLogger(SkillExtractionService.class);

    // Canonical Skills Taxonomy
    private static final Map<String, List<String>> CANONICAL_CATEGORIES = new LinkedHashMap<>();
    private static final Map<String, String> ALIAS_MAP = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    private static final Map<String, List<String>> SKILL_TOPIC_MAP = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    static {
        // 1. Languages
        CANONICAL_CATEGORIES.put("Programming Languages", List.of(
                "Java", "Python", "JavaScript", "TypeScript", "C++", "C#", "Go", "Rust", "Ruby", "PHP", "SQL", "Kotlin", "Swift", "Scala"
        ));

        // 2. Frameworks & Libraries
        CANONICAL_CATEGORIES.put("Frameworks & Libraries", List.of(
                "Spring Boot", "Spring Security", "Hibernate", "JPA", "React", "Angular", "Vue", "Node.js", "Express", "Next.js", "Django", "FastAPI", "Redux", "Tailwind CSS"
        ));

        // 3. Databases & Caching
        CANONICAL_CATEGORIES.put("Databases & Storage", List.of(
                "PostgreSQL", "MongoDB", "MySQL", "Redis", "Oracle", "SQLite", "Cassandra", "Elasticsearch", "DynamoDB"
        ));

        // 4. Cloud & DevOps
        CANONICAL_CATEGORIES.put("Cloud & DevOps", List.of(
                "Docker", "Kubernetes", "AWS", "GCP", "Azure", "Git", "GitHub", "CI/CD", "Jenkins", "Terraform", "Linux", "Nginx"
        ));

        // 5. AI, Search & Vectors
        CANONICAL_CATEGORIES.put("AI, Search & Vectors", List.of(
                "RAG", "pgvector", "Vector Embeddings", "LLMs", "LangChain", "OpenAI", "PyTorch", "TensorFlow", "NLP"
        ));

        // 6. Architecture & Practices
        CANONICAL_CATEGORIES.put("Architecture & Practices", List.of(
                "Microservices", "REST APIs", "GraphQL", "Kafka", "RabbitMQ", "JWT", "OAuth", "OOP", "Data Structures & Algorithms"
        ));

        // Alias Normalization
        ALIAS_MAP.put("react.js", "React");
        ALIAS_MAP.put("reactjs", "React");
        ALIAS_MAP.put("react js", "React");
        ALIAS_MAP.put("node.js", "Node.js");
        ALIAS_MAP.put("nodejs", "Node.js");
        ALIAS_MAP.put("node js", "Node.js");
        ALIAS_MAP.put("springboot", "Spring Boot");
        ALIAS_MAP.put("spring boot 3", "Spring Boot");
        ALIAS_MAP.put("spring framework", "Spring Boot");
        ALIAS_MAP.put("postgres", "PostgreSQL");
        ALIAS_MAP.put("pgsql", "PostgreSQL");
        ALIAS_MAP.put("mongo", "MongoDB");
        ALIAS_MAP.put("k8s", "Kubernetes");
        ALIAS_MAP.put("golang", "Go");
        ALIAS_MAP.put("vue.js", "Vue");
        ALIAS_MAP.put("vuejs", "Vue");
        ALIAS_MAP.put("aws cloud", "AWS");
        ALIAS_MAP.put("restful apis", "REST APIs");
        ALIAS_MAP.put("rest api", "REST APIs");
        ALIAS_MAP.put("rest apis", "REST APIs");
        ALIAS_MAP.put("restful", "REST APIs");
        ALIAS_MAP.put("ci/cd", "CI/CD");
        ALIAS_MAP.put("ci-cd", "CI/CD");
        ALIAS_MAP.put("jwt token", "JWT");
        ALIAS_MAP.put("dsa", "Data Structures & Algorithms");
        ALIAS_MAP.put("vector search", "Vector Embeddings");
        ALIAS_MAP.put("embeddings", "Vector Embeddings");
        ALIAS_MAP.put("llm", "LLMs");
        ALIAS_MAP.put("rag system", "RAG");
        ALIAS_MAP.put("nextjs", "Next.js");
        ALIAS_MAP.put("next.js", "Next.js");
        ALIAS_MAP.put("tailwind", "Tailwind CSS");

        // Skill to Technical Topic Mapping
        SKILL_TOPIC_MAP.put("Java", List.of(
                "OOP Principles (Polymorphism, Abstraction, Encapsulation)",
                "Collections Framework (HashMap vs ConcurrentHashMap, ArrayList vs LinkedList)",
                "JVM Architecture & Memory Management (Heap, Stack, Metaspace, GC)",
                "Multithreading, Concurrency & Thread Synchronization",
                "Java Streams API, Lambdas & Functional Interfaces",
                "Exception Handling (Checked vs Unchecked, Try-With-Resources)"
        ));

        SKILL_TOPIC_MAP.put("Spring Boot", List.of(
                "Dependency Injection, Inversion of Control & Bean Lifecycles",
                "RESTful Controller Architecture & @ControllerAdvice Error Handling",
                "Spring Security Filter Chains & Stateless JWT Authentication",
                "Spring Data JPA, Entity Relationships & N+1 Query Elimination",
                "Spring Boot Auto-Configuration & Application Profiles"
        ));

        SKILL_TOPIC_MAP.put("React", List.of(
                "Component Architecture & State vs Props",
                "React Hooks Lifecycle: useState, useEffect, useMemo, useCallback",
                "Virtual DOM Diffing, Reconciliation & Render Optimization",
                "Client-Side Routing & Protected Route Guards",
                "Context API & Global State Management"
        ));

        SKILL_TOPIC_MAP.put("MongoDB", List.of(
                "Document-Oriented Data Modeling & Embedding vs Referencing",
                "B-Tree Indexing, Compound Indexes & Query Optimization",
                "Aggregation Pipeline ($match, $group, $lookup, $project)",
                "Replica Sets, High Availability & Write Concerns"
        ));

        SKILL_TOPIC_MAP.put("PostgreSQL", List.of(
                "ACID Guarantees, Transaction Isolation Levels & Locking",
                "Advanced Indexing: B-Tree, GIN, GiST, and HNSW",
                "pgvector Extension & High-Performance Cosine Distance Queries",
                "Query Performance, EXPLAIN ANALYZE & Connection Pooling (HikariCP)"
        ));

        SKILL_TOPIC_MAP.put("REST APIs", List.of(
                "HTTP Verbs, Idempotency & Safe Methods",
                "HTTP Status Code Conventions (200, 201, 204, 400, 401, 403, 404, 422)",
                "Stateless JWT Authentication & Refresh Token Rotation",
                "Input Validation, DTO Sanitization & Defensive Design"
        ));

        SKILL_TOPIC_MAP.put("RAG", List.of(
                "Vector Embeddings Generation & High-Dimensional Semantic Spaces",
                "Chunking Strategies: Chunk Size, Overlap & Text Boundary Cleaning",
                "Cosine Similarity vs Euclidean Distance vs Dot Product",
                "Retrieval Precision, Context Window Budgets & Hallucination Mitigation"
        ));

        SKILL_TOPIC_MAP.put("Docker", List.of(
                "Multi-Stage Container Builds & Image Optimization",
                "Docker Compose Orchestration, Networks & Service Dependencies",
                "Persistent Named Volumes vs Host Bind Mounts",
                "Container Healthchecks, Least-Privilege Users & Security"
        ));

        SKILL_TOPIC_MAP.put("Kubernetes", List.of(
                "Pods, Deployments, ReplicaSets & Horizontal Pod Autoscaling",
                "Services (ClusterIP, NodePort, LoadBalancer) & Ingress Controllers",
                "ConfigMaps, Secrets & Environment Injection",
                "Readiness and Liveness Probes"
        ));

        SKILL_TOPIC_MAP.put("Git", List.of(
                "Git Branching Strategies, Rebasing vs Merging",
                "Git Reset (Soft, Mixed, Hard) & Commit Rewriting",
                "Merge Conflict Resolution & Pull Request Workflows"
        ));

        SKILL_TOPIC_MAP.put("JavaScript", List.of(
                "Event Loop, Microtasks vs Macrotasks, Call Stack",
                "Closures, Lexical Scope & Hoisting",
                "Promises, Async/Await & Asynchronous Error Handling",
                "Prototypes, Inheritance & ES6+ Modules"
        ));

        SKILL_TOPIC_MAP.put("TypeScript", List.of(
                "Generics, Type Constraints & Type Inference",
                "Interfaces vs Type Aliases & Structural Subtyping",
                "Union, Intersection & Discriminated Union Types",
                "Utility Types: Partial, Pick, Omit, Record, Readonly"
        ));

        SKILL_TOPIC_MAP.put("Python", List.of(
                "GIL (Global Interpreter Lock) & Concurrency (Multiprocessing vs Threading)",
                "Generators, Iterators & Memory-Efficient Streaming",
                "Decorators, Context Managers & Dunder Methods",
                "Virtual Environments, Package Management & Typing"
        ));

        SKILL_TOPIC_MAP.put("SQL", List.of(
                "Complex JOIN Operations (INNER, LEFT, RIGHT, FULL, CROSS)",
                "Window Functions (ROW_NUMBER, RANK, DENSE_RANK, OVER)",
                "Subqueries, CTEs (Common Table Expressions) & Recursive Queries",
                "Database Indexing Strategies, Index Scans vs Seq Scans"
        ));

        SKILL_TOPIC_MAP.put("Microservices", List.of(
                "Service Discovery, API Gateways & Reverse Proxies",
                "Inter-Service Communication (Synchronous REST vs Asynchronous Messaging)",
                "Distributed Transactions, Saga Pattern & Eventual Consistency",
                "Resilience Patterns: Circuit Breakers, Retries & Fallbacks"
        ));

        SKILL_TOPIC_MAP.put("Kafka", List.of(
                "Topics, Partitions, Consumer Groups & Offsets",
                "Message Ordering, Delivery Guarantees (At-Least-Once, Exactly-Once)",
                "Log Compaction, Retention Policies & Broker Architecture"
        ));
    }

    public SkillExtractionResult extractSkills(String text) {
        if (text == null || text.isBlank()) {
            return new SkillExtractionResult(Collections.emptyMap(), Collections.emptyList(), Collections.emptyList());
        }

        String lowerText = " " + text.toLowerCase() + " ";
        Set<String> matchedSkills = new LinkedHashSet<>();

        // 1. Check aliases first
        for (Map.Entry<String, String> aliasEntry : ALIAS_MAP.entrySet()) {
            String alias = aliasEntry.getKey().toLowerCase();
            if (containsWordOrPhrase(lowerText, alias)) {
                matchedSkills.add(aliasEntry.getValue());
            }
        }

        // 2. Check all canonical skills
        for (List<String> categorySkills : CANONICAL_CATEGORIES.values()) {
            for (String skill : categorySkills) {
                if (containsWordOrPhrase(lowerText, skill.toLowerCase())) {
                    matchedSkills.add(skill);
                }
            }
        }

        // 3. Categorize matched skills
        Map<String, List<String>> categorized = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> catEntry : CANONICAL_CATEGORIES.entrySet()) {
            String category = catEntry.getKey();
            List<String> canonicalList = catEntry.getValue();

            List<String> inCategory = new ArrayList<>();
            for (String skill : matchedSkills) {
                if (canonicalList.contains(skill)) {
                    inCategory.add(skill);
                }
            }
            if (!inCategory.isEmpty()) {
                categorized.put(category, inCategory);
            }
        }

        // 4. Generate suggested assessment topics
        List<String> suggestedTopics = new ArrayList<>();
        for (String skill : matchedSkills) {
            List<String> topics = SKILL_TOPIC_MAP.get(skill);
            if (topics != null) {
                suggestedTopics.addAll(topics);
            } else {
                suggestedTopics.add(skill + " Core Concepts and Best Practices");
            }
        }

        logger.info("Extracted {} technical skills from text: {}", matchedSkills.size(), matchedSkills);

        return new SkillExtractionResult(categorized, new ArrayList<>(matchedSkills), suggestedTopics);
    }

    public List<String> getTopicsForSkill(String skill) {
        String normalized = normalizeSkill(skill);
        List<String> topics = SKILL_TOPIC_MAP.get(normalized);
        if (topics != null) {
            return topics;
        }
        return List.of(
                normalized + " Core Architecture and Concepts",
                normalized + " Performance, Scaling and Edge Cases",
                normalized + " Real-World Troubleshooting and Best Practices"
        );
    }

    public String normalizeSkill(String skill) {
        if (skill == null || skill.isBlank()) return "General Software Engineering";
        String trimmed = skill.trim();
        String alias = ALIAS_MAP.get(trimmed);
        if (alias != null) return alias;

        for (List<String> skills : CANONICAL_CATEGORIES.values()) {
            for (String s : skills) {
                if (s.equalsIgnoreCase(trimmed)) {
                    return s;
                }
            }
        }
        return trimmed;
    }

    private boolean containsWordOrPhrase(String text, String term) {
        if (term.length() <= 2) {
            // For short names like C, Go, R, AI, use strict word boundary regex
            String pattern = "(?i)\\b" + Pattern.quote(term) + "\\b";
            return Pattern.compile(pattern).matcher(text).find();
        }
        // General term search with boundary checks
        String pattern = "(?i)(?<=^|[^a-zA-Z0-9_#+])" + Pattern.quote(term) + "(?=[^a-zA-Z0-9_#+]|$)";
        return Pattern.compile(pattern).matcher(text).find();
    }

    public static class SkillExtractionResult {
        private final Map<String, List<String>> categorizedSkills;
        private final List<String> allSkills;
        private final List<String> suggestedTopics;

        public SkillExtractionResult(Map<String, List<String>> categorizedSkills,
                                     List<String> allSkills,
                                     List<String> suggestedTopics) {
            this.categorizedSkills = categorizedSkills;
            this.allSkills = allSkills;
            this.suggestedTopics = suggestedTopics;
        }

        public Map<String, List<String>> getCategorizedSkills() { return categorizedSkills; }
        public List<String> getAllSkills() { return allSkills; }
        public List<String> getSuggestedTopics() { return suggestedTopics; }
    }
}
