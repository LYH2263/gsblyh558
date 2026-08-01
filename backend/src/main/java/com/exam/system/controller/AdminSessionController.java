package com.exam.system.controller;

import com.exam.system.dto.ExamSessionRequest;
import com.exam.system.dto.ExamSessionResponse;
import com.exam.system.dto.SessionStatsResponse;
import com.exam.system.entity.SessionStatus;
import com.exam.system.payload.response.ApiResponse;
import com.exam.system.service.ExamSessionService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/admin/sessions")
@PreAuthorize("hasRole('ADMIN')")
@Slf4j
public class AdminSessionController {

    @Autowired
    private ExamSessionService sessionService;

    @GetMapping
    public ApiResponse<List<ExamSessionResponse>> listSessions(@RequestParam(required = false) Long examId,
                                                               @RequestParam(required = false) SessionStatus status) {
        return ApiResponse.success(sessionService.listSessions(examId, status));
    }

    @GetMapping("/stats")
    public ApiResponse<List<SessionStatsResponse>> sessionStats(@RequestParam(required = false) Long examId,
                                                                @RequestParam(required = false) SessionStatus status) {
        return ApiResponse.success(sessionService.sessionStats(examId, status));
    }

    @PostMapping
    public ApiResponse<ExamSessionResponse> createSession(@Valid @RequestBody ExamSessionRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        log.info("Admin [{}] is creating a session for exam [ID: {}]", authentication.getName(), request.getExamId());
        return ApiResponse.success("场次创建成功", sessionService.createSession(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<ExamSessionResponse> updateSession(@PathVariable Long id,
                                                          @Valid @RequestBody ExamSessionRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        log.info("Admin [{}] is updating session [ID: {}]", authentication.getName(), id);
        return ApiResponse.success("场次修改成功", sessionService.updateSession(id, request));
    }

    @PutMapping("/{id}/close")
    public ApiResponse<Map<String, Long>> closeSession(@PathVariable Long id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        log.info("Admin [{}] is closing session [ID: {}]", authentication.getName(), id);
        long bookedCount = sessionService.closeSession(id);
        return ApiResponse.success("场次已下线，共有 " + bookedCount + " 人已预约", Map.of("bookedCount", bookedCount));
    }
}
