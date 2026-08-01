package com.exam.system.controller;

import com.exam.system.dto.ExamSubmission;
import com.exam.system.entity.Exam;
import com.exam.system.entity.ExamResult;
import com.exam.system.payload.response.ApiResponse;
import com.exam.system.service.ExamService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
    private ExamService examService;

    @GetMapping
    public ApiResponse<List<Exam>> getAllExams() {
        return ApiResponse.success(examService.getAllExams());
    }

    @GetMapping("/{id}")
    public ApiResponse<Exam> getExam(@PathVariable Long id) {
        return ApiResponse.success(examService.getExamById(id));
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ApiResponse<ExamResult> submitExam(@PathVariable Long id, @Valid @RequestBody ExamSubmission submission) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        log.info("User [{}] is submitting exam [ID: {}] (sessionId: {}, reservationId: {})",
                username, id, submission.getSessionId(), submission.getReservationId());

        ExamResult savedResult = examService.submitExam(id, submission, username);
        return ApiResponse.success(savedResult);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Exam> createExam(@Valid @RequestBody Exam exam) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        log.info("Admin [{}] is creating a new exam: {}", authentication.getName(), exam.getTitle());

        Exam savedExam = examService.createExam(exam);
        log.info("Exam [{}] created successfully with ID: {}", savedExam.getTitle(), savedExam.getId());
        return ApiResponse.success(savedExam);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Exam> updateExam(@PathVariable Long id, @Valid @RequestBody Exam examRequest) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        log.info("Admin [{}] is updating exam [ID: {}]", authentication.getName(), id);

        Exam updatedExam = examService.updateExam(id, examRequest);
        log.info("Exam [ID: {}] updated successfully", id);
        return ApiResponse.success(updatedExam);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> deleteExam(@PathVariable Long id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        log.info("Admin [{}] is deleting exam [ID: {}]", authentication.getName(), id);
        examService.deleteExam(id);
        log.info("Exam [ID: {}] deleted successfully", id);
        return ApiResponse.success("考试删除成功", null);
    }
}
