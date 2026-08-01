package com.exam.system.controller;

import com.exam.system.dto.ExamSubmission;
import com.exam.system.entity.*;
import com.exam.system.exception.ResourceNotFoundException;
import com.exam.system.repository.*;
import com.exam.system.payload.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/exams")
@Slf4j
public class ExamController {

    @Autowired
    ExamRepository examRepository;

    @Autowired
    ExamResultRepository examResultRepository;

    @Autowired
    com.exam.system.service.QuestionService questionService;

    @Autowired
    UserRepository userRepository;

    @Autowired
    WrongQuestionRecordRepository wrongQuestionRecordRepository;

    @GetMapping
    public ApiResponse<List<Exam>> getAllExams() {
        return ApiResponse.success(examRepository.findAll());
    }

    @GetMapping("/{id}")
    public ApiResponse<Exam> getExam(@PathVariable Long id) {
        return ApiResponse.success(examRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("未找到 ID 为 " + id + " 的考试")));
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ApiResponse<ExamResult> submitExam(@PathVariable Long id, @Valid @RequestBody ExamSubmission submission) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        
        log.info("User [{}] is submitting exam [ID: {}]", username, id);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("未找到该用户"));

        Exam exam = examRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("未找到 ID 为 " + id + " 的考试"));

        int totalScore = 0;
        Map<Long, String> answers = submission.getAnswers();

        for (ExamQuestion examQuestion : exam.getQuestions()) {
            Question question = examQuestion.getQuestion();
            String userAnswer = answers.get(question.getId());
            
            boolean isCorrect = questionService.isAnswerCorrect(question, userAnswer);

            if (isCorrect) {
                totalScore += examQuestion.getScore();
                // Remove from wrong question book if it exists
                wrongQuestionRecordRepository
                        .findByUserIdAndQuestionIdAndIsRemovedFalse(user.getId(), question.getId())
                        .ifPresent(wr -> {
                            wr.setIsRemoved(true);
                            wr.setRemoveTime(LocalDateTime.now());
                            wrongQuestionRecordRepository.save(wr);
                            log.info("Question [ID: {}] removed from user [{}] wrong question book because it was answered correctly in exam [ID: {}].", question.getId(), username, id);
                        });
            } else {
                // Add to wrong question book
                log.info("User [{}] answered question [ID: {}] incorrectly in exam [ID: {}]. Adding/Updating wrong question book.", username, question.getId(), id);
                WrongQuestionRecord wrongRecord = wrongQuestionRecordRepository
                        .findByUserIdAndQuestionIdAndIsRemovedFalse(user.getId(), question.getId())
                        .orElse(new WrongQuestionRecord());
                
                wrongRecord.setUser(user);
                wrongRecord.setQuestion(question);
                wrongRecord.setIsRemoved(false);
                wrongRecord.setLastWrongTime(LocalDateTime.now());
                wrongQuestionRecordRepository.save(wrongRecord);
            }
        }

        ExamResult result = new ExamResult();
        result.setUser(user);
        result.setExam(exam);
        result.setScore(totalScore);
        
        ExamResult savedResult = examResultRepository.save(result);
        log.info("User [{}] finished exam [ID: {}] with score: {}", username, id, totalScore);
        
        return ApiResponse.success(savedResult);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Exam> createExam(@Valid @RequestBody Exam exam) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        log.info("Admin [{}] is creating a new exam: {}", authentication.getName(), exam.getTitle());
        
        validateExamScore(exam);
        if (exam.getQuestions() != null) {
            for (ExamQuestion eq : exam.getQuestions()) {
                eq.setExam(exam);
            }
        }
        Exam savedExam = examRepository.save(exam);
        log.info("Exam [{}] created successfully with ID: {}", savedExam.getTitle(), savedExam.getId());
        return ApiResponse.success(savedExam);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Exam> updateExam(@PathVariable Long id, @Valid @RequestBody Exam examRequest) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        log.info("Admin [{}] is updating exam [ID: {}]", authentication.getName(), id);
        
        validateExamScore(examRequest);
        return ApiResponse.success(examRepository.findById(id).map(exam -> {
            exam.setTitle(examRequest.getTitle());
            exam.setDescription(examRequest.getDescription());
            exam.setTotalScore(examRequest.getTotalScore());
            exam.setDuration(examRequest.getDuration());
            
            // Clear existing questions and add new ones
            if (exam.getQuestions() != null) {
                exam.getQuestions().clear();
            }
            
            if (examRequest.getQuestions() != null) {
                exam.getQuestions().addAll(examRequest.getQuestions());
                for (ExamQuestion eq : exam.getQuestions()) {
                    eq.setExam(exam);
                }
            }
            
            Exam updatedExam = examRepository.save(exam);
            log.info("Exam [ID: {}] updated successfully", id);
            return updatedExam;
        }).orElseThrow(() -> new ResourceNotFoundException("Exam not found with id " + id)));
    }

    private void validateExamScore(Exam exam) {
        if (exam.getQuestions() == null || exam.getQuestions().isEmpty()) {
            log.warn("Attempt to save exam with no questions");
            throw new RuntimeException("考试至少需要包含一道题目");
        }
        int calculatedTotalScore = exam.getQuestions().stream()
                .mapToInt(eq -> eq.getScore() != null ? eq.getScore() : 0)
                .sum();
        if (calculatedTotalScore != exam.getTotalScore()) {
            log.warn("Exam score mismatch: calculated={}, declared={}", calculatedTotalScore, exam.getTotalScore());
            throw new RuntimeException("已选题目总分 (" + calculatedTotalScore + ") 与设定的考试总分 (" + exam.getTotalScore() + ") 不一致");
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> deleteExam(@PathVariable Long id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        log.info("Admin [{}] is deleting exam [ID: {}]", authentication.getName(), id);
        examRepository.deleteById(id);
        log.info("Exam [ID: {}] deleted successfully", id);
        return ApiResponse.success("考试删除成功", null);
    }


}
