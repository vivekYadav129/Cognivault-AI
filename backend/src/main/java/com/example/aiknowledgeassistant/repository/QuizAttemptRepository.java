package com.example.aiknowledgeassistant.repository;

import com.example.aiknowledgeassistant.entity.QuizAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Long> {
    List<QuizAttempt> findAllByUserIdOrderByCompletedAtDesc(Long userId);
    List<QuizAttempt> findAllByQuizIdOrderByCompletedAtDesc(Long quizId);
    Optional<QuizAttempt> findFirstByQuizIdOrderByCompletedAtDesc(Long quizId);
}

