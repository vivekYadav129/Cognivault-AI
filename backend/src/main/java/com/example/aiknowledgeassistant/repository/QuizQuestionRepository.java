package com.example.aiknowledgeassistant.repository;

import com.example.aiknowledgeassistant.entity.QuizQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface QuizQuestionRepository extends JpaRepository<QuizQuestion, Long> {

    List<QuizQuestion> findAllByQuizIdOrderByQuestionIndexAsc(Long quizId);

    @Query("SELECT q.questionSignature FROM QuizQuestion q WHERE q.quiz.user.id = :userId AND q.questionSignature IS NOT NULL")
    Set<String> findQuestionSignaturesByUserId(@Param("userId") Long userId);

    @Query("SELECT q.questionSignature FROM QuizQuestion q WHERE q.quiz.user.id = :userId AND q.skillTag = :skillTag AND q.questionSignature IS NOT NULL")
    Set<String> findQuestionSignaturesByUserIdAndSkillTag(@Param("userId") Long userId, @Param("skillTag") String skillTag);
}

