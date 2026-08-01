package com.exam.system.controller;

import com.exam.system.dto.SessionAnalyticsResponse;
import com.exam.system.dto.SessionRequest;
import com.exam.system.dto.SessionResponse;
import com.exam.system.entity.SessionStatus;
import com.exam.system.payload.response.ApiResponse;
import com.exam.system.service.SessionService;
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
@RequestMapping("/api/admin/sessions")
@PreAuthorize("hasRole('ADMIN')")
@Slf4j
public class AdminSessionController {
    @Autowired
    private SessionService sessionService;

    @GetMapping
    public ApiResponse<List<SessionResponse>> getSessions(@RequestParam(required = false) Long examId,
                                                           @RequestParam(required = false) SessionStatus status) {
        return ApiResponse.success(sessionService.getAdminSessions(examId, status));
    }

    @PostMapping
    public ApiResponse<SessionResponse> createSession(@Valid @RequestBody SessionRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        log.info("Admin [{}] is creating exam session for exam [ID: {}]", authentication.getName(), request.examId());
        return ApiResponse.success(sessionService.createSession(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<SessionResponse> updateSession(@PathVariable Long id, @Valid @RequestBody SessionRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        log.info("Admin [{}] is updating session [ID: {}]", authentication.getName(), id);
        return ApiResponse.success(sessionService.updateSession(id, request));
    }

    @PostMapping("/{id}/close")
    public ApiResponse<SessionResponse> closeSession(@PathVariable Long id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        log.info("Admin [{}] is closing session [ID: {}]", authentication.getName(), id);
        return ApiResponse.success("场次已下线", sessionService.closeSession(id));
    }

    @GetMapping("/analytics")
    public ApiResponse<List<SessionAnalyticsResponse>> getAnalytics(@RequestParam(required = false) SessionStatus status) {
        return ApiResponse.success(sessionService.getSessionAnalytics(status));
    }
}
