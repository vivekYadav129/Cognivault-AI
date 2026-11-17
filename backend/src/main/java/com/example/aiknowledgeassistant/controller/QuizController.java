package com.example.aiknowledgeassistant.controller;

import com.example.aiknowledgeassistant.dto.request.QuizGenerateRequest;
import com.example.aiknowledgeassistant.dto.request.QuizSubmitRequest;
import com.example.aiknowledgeassistant.dto.response.*;
import com.example.aiknowledgeassistant.service.quiz.QuizService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/quiz")
public class QuizController {

    private final QuizService quizService;

    public QuizController(QuizService quizService) {
        this.quizService = quizService;
    }

    @PostMapping("/generate")
    public ResponseEntity<QuizResponse> generateQuiz(
            @Valid @RequestBody QuizGenerateRequest request,
            Authentication authentication) {
        QuizResponse response = quizService.generateQuiz(request, authentication.getName());
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/{id}/submit")
    public ResponseEntity<QuizResultResponse> submitQuiz(
            @PathVariable Long id,
            @Valid @RequestBody QuizSubmitRequest request,
            Authentication authentication) {
        QuizResultResponse response = quizService.submitQuiz(id, request, authentication.getName());
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<QuizSummaryResponse>> getUserQuizzes(Authentication authentication) {
        List<QuizSummaryResponse> response = quizService.getUserQuizzes(authentication.getName());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<QuizResponse> getQuizById(
            @PathVariable Long id,
            Authentication authentication) {
        QuizResponse response = quizService.getQuizById(id, authentication.getName());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/skills/{documentId}")
    public ResponseEntity<SkillExtractionResponse> extractSkills(
            @PathVariable Long documentId,
            Authentication authentication) {
        SkillExtractionResponse response = quizService.extractSkillsFromDocument(documentId, authentication.getName());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/evaluate")
    public ResponseEntity<QuizEvaluationReport> evaluateQuiz(
            @PathVariable Long id,
            Authentication authentication) {
        QuizEvaluationReport response = quizService.evaluateQuizById(id, authentication.getName());
        return ResponseEntity.ok(response);
    }
}

