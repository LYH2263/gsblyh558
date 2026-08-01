package com.exam.system.controller;

import com.exam.system.dto.ReservationExamAccessResponse;
import com.exam.system.dto.ReservationResponse;
import com.exam.system.dto.TabSwitchResponse;
import com.exam.system.payload.response.ApiResponse;
import com.exam.system.service.SessionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/reservations")
@PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
@Slf4j
public class ReservationController {
    @Autowired
    private SessionService sessionService;

    @GetMapping("/me")
    public ApiResponse<List<ReservationResponse>> getMyReservations() {
        return ApiResponse.success(sessionService.getMyReservations(currentUsername()));
    }

    @GetMapping("/{id}/access")
    public ApiResponse<ReservationExamAccessResponse> getExamAccess(@PathVariable Long id) {
        return ApiResponse.success(sessionService.getExamAccess(currentUsername(), id));
    }

    @PostMapping("/{id}/tab-switch")
    public ApiResponse<TabSwitchResponse> reportTabSwitch(@PathVariable Long id) {
        return ApiResponse.success(sessionService.reportTabSwitch(currentUsername(), id));
    }

    @PostMapping("/{sessionId}")
    public ApiResponse<ReservationResponse> bookSession(@PathVariable Long sessionId) {
        String username = currentUsername();
        log.info("User [{}] is booking session [ID: {}]", username, sessionId);
        return ApiResponse.success("预约成功", sessionService.bookSession(username, sessionId));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> cancelReservation(@PathVariable Long id) {
        String username = currentUsername();
        log.info("User [{}] is cancelling reservation [ID: {}]", username, id);
        sessionService.cancelReservation(username, id);
        return ApiResponse.success("预约已取消", null);
    }

    private String currentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName();
    }
}
