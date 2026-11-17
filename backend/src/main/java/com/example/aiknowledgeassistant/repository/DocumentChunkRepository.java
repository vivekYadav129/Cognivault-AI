package com.example.aiknowledgeassistant.repository;

import com.example.aiknowledgeassistant.entity.DocumentChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, Long> {

    List<DocumentChunk> findAllByDocumentIdOrderByChunkIndexAsc(Long documentId);

    void deleteAllByDocumentId(Long documentId);

    int countByDocumentId(Long documentId);

    @Query("SELECT c FROM DocumentChunk c JOIN FETCH c.document d JOIN FETCH d.user u WHERE u.id = :userId")
    List<DocumentChunk> findAllByUserIdWithDocument(@Param("userId") Long userId);

    @Query("SELECT c FROM DocumentChunk c JOIN FETCH c.document d JOIN FETCH d.user u WHERE u.id = :userId AND d.id = :documentId")
    List<DocumentChunk> findAllByUserIdAndDocumentIdWithDocument(@Param("userId") Long userId, @Param("documentId") Long documentId);
}
