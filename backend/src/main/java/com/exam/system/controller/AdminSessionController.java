package com.exam.system.controller;

import com.exam.system.dto.OfflineSessionResponse;
import com.exam.system.dto.ReservationResponse;
import com.exam.system.dto.SessionCreateRequest;
import com.exam.system.dto.SessionDashboardRow;
import com.exam.system.dto.SessionResponse;
import com.exam.system.dto.SessionUpdateRequest;
import com.exam.system.entity.SessionStatus;
import com.exam.system.payload.response.ApiResponse;
import com.exam.system.service.ExamSessionService;
import com.exam.system.service.SessionDashboardService;
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
    private ExamSessionService sessionService;

    @Autowired
    private SessionDashboardService dashboardService;

    @GetMapping
    public ApiResponse<List<SessionResponse>> listSessions(
            @RequestParam(value = "examId", required = false) Long examId,
            @RequestParam(value = "status", required = false) SessionStatus status) {
        return ApiResponse.success(sessionService.listAdminSessions(examId, status));
    }

    @GetMapping("/dashboard")
    public ApiResponse<List<SessionDashboardRow>> dashboard(
            @RequestParam(value = "status", required = false) SessionStatus status) {
        return ApiResponse.success(dashboardService.getDashboard(status));
    }

    @PostMapping
    public ApiResponse<SessionResponse> createSession(@Valid @RequestBody SessionCreateRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        log.info("Admin [{}] is creating a session for exam [{}]", auth.getName(), request.getExamId());
        return ApiResponse.success("场次创建成功", sessionService.createSession(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<SessionResponse> updateSession(
            @PathVariable Long id,
            @Valid @RequestBody SessionUpdateRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        log.info("Admin [{}] is updating session [ID: {}]", auth.getName(), id);
        return ApiResponse.success("场次更新成功", sessionService.updateSession(id, request));
    }

    @PostMapping("/{id}/offline")
    public ApiResponse<OfflineSessionResponse> offlineSession(@PathVariable Long id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        log.info("Admin [{}] is offlining session [ID: {}]", auth.getName(), id);
        OfflineSessionResponse response = sessionService.offlineSession(id);
        return ApiResponse.success(
                "场次已下线，已取消 " + response.getCancelledReservations() + " 个预约",
                response);
    }

    @GetMapping("/{id}/reservations")
    public ApiResponse<List<ReservationResponse>> listReservations(@PathVariable Long id) {
        return ApiResponse.success(sessionService.listSessionReservations(id));
    }
}
