package com.example.aiknowledgeassistant.repository;

import com.example.aiknowledgeassistant.entity.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuizRepository extends JpaRepository<Quiz, Long> {
    List<Quiz> findAllByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<Quiz> findByIdAndUserId(Long id, Long userId);

    @Modifying
    @Query("UPDATE Quiz q SET q.document = null WHERE q.document.id = :documentId")
    void detachDocument(@Param("documentId") Long documentId);
}
