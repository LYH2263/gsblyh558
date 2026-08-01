package com.exam.system.controller;

import com.exam.system.dto.ExamSubmission;
import com.exam.system.entity.*;
import com.exam.system.exception.BusinessException;
import com.exam.system.exception.ResourceNotFoundException;
import com.exam.system.repository.ExamRepository;
import com.exam.system.repository.UserRepository;
import com.exam.system.payload.response.ApiResponse;
import com.exam.system.service.ExamSubmissionService;
import com.exam.system.service.SessionService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/exams")
@Slf4j
public class ExamController {

    @Autowired
    ExamRepository examRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    ExamSubmissionService examSubmissionService;

    @Autowired
    SessionService sessionService;

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
    public ResponseEntity<ApiResponse<ExamResult>> submitExam(@PathVariable Long id, @Valid @RequestBody ExamSubmission submission) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        log.info("User [{}] is submitting exam [ID: {}]", username, id);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("未找到该用户"));

        if (submission.getReservationId() != null) {
            if (submission.getSessionId() == null) {
                throw new BusinessException("RSV_NO_RESERVATION", "缺少场次信息，无法提交预约考试");
            }
            ExamResult result = sessionService.submitReservationExam(
                    username,
                    id,
                    submission.getReservationId(),
                    submission.getSessionId(),
                    submission.getForcedSubmitReason(),
                    submission.getAnswers()
            );
            if (Boolean.TRUE.equals(result.getTimedOut())) {
                log.warn("User [{}] submitted exam [ID: {}] after timeout", username, id);
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error(400, "考试已超时，系统已按超时交卷处理", "RSV_SESSION_ENDED", result));
            }
            log.info("User [{}] finished reserved exam [ID: {}] with score: {}", username, id, result.getScore());
            return ResponseEntity.ok(ApiResponse.success(result));
        }

        Exam exam = examRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("未找到 ID 为 " + id + " 的考试"));
        ExamResult savedResult = examSubmissionService.submitDirect(user, exam, submission.getAnswers());
        log.info("User [{}] finished exam [ID: {}] with score: {}", username, id, savedResult.getScore());

        return ResponseEntity.ok(ApiResponse.success(savedResult));
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
