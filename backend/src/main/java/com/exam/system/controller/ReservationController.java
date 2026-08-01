package com.exam.system.controller;

import com.exam.system.dto.ReservationResponse;
import com.exam.system.payload.response.ApiResponse;
import com.exam.system.service.ExamSessionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/reservations")
@Slf4j
public class ReservationController {

    @Autowired
    private ExamSessionService sessionService;

    @GetMapping("/my")
    public ApiResponse<List<ReservationResponse>> myReservations() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return ApiResponse.success(sessionService.listMyReservations(auth.getName()));
    }

    @PostMapping("/{id}/cancel")
    public ApiResponse<Void> cancelReservation(@PathVariable Long id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        log.info("User [{}] is cancelling reservation [ID: {}]", auth.getName(), id);
        sessionService.cancelReservation(id, auth.getName());
        return ApiResponse.success("预约已取消", null);
    }
}
