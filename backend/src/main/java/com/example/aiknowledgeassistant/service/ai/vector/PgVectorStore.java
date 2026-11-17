package com.example.aiknowledgeassistant.service.ai.vector;

import com.example.aiknowledgeassistant.entity.Document;
import com.example.aiknowledgeassistant.entity.DocumentChunk;
import com.example.aiknowledgeassistant.exception.BadRequestException;
import com.example.aiknowledgeassistant.repository.DocumentChunkRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Repository
public class PgVectorStore implements VectorStore {

    private static final Logger logger = LoggerFactory.getLogger(PgVectorStore.class);

    private final DocumentChunkRepository chunkRepository;
    private final JdbcTemplate jdbcTemplate;
    private volatile Boolean isPostgres;

    public PgVectorStore(DocumentChunkRepository chunkRepository, JdbcTemplate jdbcTemplate) {
        this.chunkRepository = chunkRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public void save(DocumentChunk chunk, float[] embedding) {
        if (chunk == null || embedding == null) {
            throw new BadRequestException("Chunk and embedding cannot be null.");
        }
        chunk.setEmbedding(embedding);
        chunkRepository.save(chunk);
    }

    @Override
    @Transactional
    public void saveAll(List<DocumentChunk> chunks, List<float[]> embeddings) {
        if (chunks == null || embeddings == null || chunks.size() != embeddings.size()) {
            throw new BadRequestException("Chunks list size must match embeddings list size.");
        }
        for (int i = 0; i < chunks.size(); i++) {
            chunks.get(i).setEmbedding(embeddings.get(i));
        }
        chunkRepository.saveAll(chunks);
    }

    @Override
    public List<SearchResult> similaritySearch(float[] queryVector, int topK, Long userId, Long documentId) {
        if (queryVector == null || queryVector.length == 0) {
            throw new BadRequestException("Query vector cannot be empty.");
        }
        if (userId == null) {
            throw new BadRequestException("User ID is required for similarity search.");
        }

        logger.info("Performing vector similarity search for user ID {}, topK {}, documentId filter {}", userId, topK, documentId);

        if (isPostgresDatabase()) {
            try {
                return searchPostgresNative(queryVector, topK, userId, documentId);
            } catch (Exception ex) {
                logger.warn("Native PostgreSQL pgvector search failed ({}), falling back to in-memory search.", ex.getMessage());
            }
        }

        return searchInMemory(queryVector, topK, userId, documentId);
    }

    private boolean isPostgresDatabase() {
        if (isPostgres != null) {
            return isPostgres;
        }
        if (jdbcTemplate == null || jdbcTemplate.getDataSource() == null) {
            isPostgres = false;
            return false;
        }
        try (var conn = jdbcTemplate.getDataSource().getConnection()) {
            String prodName = conn.getMetaData().getDatabaseProductName();
            isPostgres = prodName != null && prodName.toLowerCase().contains("postgres");
            logger.info("PgVectorStore database detected: {} (isPostgres: {})", prodName, isPostgres);
            return isPostgres;
        } catch (Exception e) {
            logger.warn("Could not inspect database metadata: {}. Defaulting to in-memory cosine search.", e.getMessage());
            isPostgres = false;
            return false;
        }
    }

    private List<SearchResult> searchPostgresNative(float[] queryVector, int topK, Long userId, Long documentId) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < queryVector.length; i++) {
            sb.append(queryVector[i]);
            if (i < queryVector.length - 1) {
                sb.append(",");
            }
        }
        sb.append("]");
        String vectorLiteral = sb.toString();

        String sql;
        List<Object> params = new ArrayList<>();

        if (documentId != null) {
            sql = "SELECT c.id AS chunk_id, c.document_id AS document_id, c.content AS content, " +
                  "c.chunk_index AS chunk_index, d.original_file_name AS original_file_name, " +
                  "d.user_id AS user_id, d.page_count AS page_count, " +
                  "(1 - (c.embedding_vec <=> CAST(? AS vector))) AS similarity_score " +
                  "FROM document_chunks c " +
                  "JOIN documents d ON c.document_id = d.id " +
                  "WHERE d.user_id = ? AND d.id = ? AND c.embedding_vec IS NOT NULL " +
                  "ORDER BY (c.embedding_vec <=> CAST(? AS vector)) ASC " +
                  "LIMIT ?";
            params.add(vectorLiteral);
            params.add(userId);
            params.add(documentId);
            params.add(vectorLiteral);
            params.add(topK);
        } else {
            sql = "SELECT c.id AS chunk_id, c.document_id AS document_id, c.content AS content, " +
                  "c.chunk_index AS chunk_index, d.original_file_name AS original_file_name, " +
                  "d.user_id AS user_id, d.page_count AS page_count, " +
                  "(1 - (c.embedding_vec <=> CAST(? AS vector))) AS similarity_score " +
                  "FROM document_chunks c " +
                  "JOIN documents d ON c.document_id = d.id " +
                  "WHERE d.user_id = ? AND c.embedding_vec IS NOT NULL " +
                  "ORDER BY (c.embedding_vec <=> CAST(? AS vector)) ASC " +
                  "LIMIT ?";
            params.add(vectorLiteral);
            params.add(userId);
            params.add(vectorLiteral);
            params.add(topK);
        }

        return jdbcTemplate.query(sql, (rs, rowNum) -> new SearchResult(
                rs.getLong("chunk_id"),
                rs.getLong("document_id"),
                rs.getString("content"),
                rs.getDouble("similarity_score"),
                rs.getInt("chunk_index"),
                rs.getString("original_file_name"),
                rs.getLong("user_id"),
                rs.getObject("page_count") != null ? rs.getInt("page_count") : null
        ), params.toArray());
    }

    private List<SearchResult> searchInMemory(float[] queryVector, int topK, Long userId, Long documentId) {
        // 1. Fetch only this user's chunks directly from DB with JOIN FETCH to prevent N+1 queries
        List<DocumentChunk> candidateChunks;
        if (documentId != null) {
            candidateChunks = chunkRepository.findAllByUserIdAndDocumentIdWithDocument(userId, documentId);
        } else {
            candidateChunks = chunkRepository.findAllByUserIdWithDocument(userId);
        }

        List<SearchResult> results = new ArrayList<>();

        // 2. Compute Cosine Similarity for candidates
        for (DocumentChunk chunk : candidateChunks) {
            float[] chunkVector = chunk.getEmbedding();
            if (chunkVector == null || chunkVector.length != queryVector.length) {
                continue;
            }

            double similarity = calculateCosineSimilarity(queryVector, chunkVector);
            Document doc = chunk.getDocument();

            results.add(new SearchResult(
                    chunk.getId(),
                    doc.getId(),
                    chunk.getContent(),
                    similarity,
                    chunk.getChunkIndex(),
                    doc.getOriginalFileName(),
                    doc.getUser().getId(),
                    doc.getPageCount()
            ));
        }

        // 3. Sort by similarity score descending and limit to topK
        return results.stream()
                .sorted(Comparator.comparingDouble(SearchResult::getSimilarityScore).reversed())
                .limit(topK)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteByDocumentId(Long documentId) {
        if (documentId != null) {
            chunkRepository.deleteAllByDocumentId(documentId);
        }
    }

    /**
     * Calculates Cosine Similarity between two N-dimensional vectors.
     * Cosine Similarity = (A . B) / (||A|| * ||B||)
     */
    public static double calculateCosineSimilarity(float[] vectorA, float[] vectorB) {
        if (vectorA.length != vectorB.length) {
            return 0.0;
        }

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < vectorA.length; i++) {
            dotProduct += vectorA[i] * vectorB[i];
            normA += vectorA[i] * vectorA[i];
            normB += vectorB[i] * vectorB[i];
        }

        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
