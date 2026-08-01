package com.exam.system.controller;

import com.exam.system.dto.SwitchScreenRequest;
import com.exam.system.payload.response.ApiResponse;
import com.exam.system.payload.response.ReservationResponse;
import com.exam.system.payload.response.SwitchScreenResponse;
import com.exam.system.service.ReservationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户端预约操作与「我的预约」。
 */
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/reservations")
@PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
@Slf4j
public class ReservationController {

    @Autowired
    private ReservationService reservationService;

    @GetMapping("/mine")
    public ApiResponse<List<ReservationResponse>> myReservations() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return ApiResponse.success(reservationService.listMyReservations(authentication.getName()));
    }

    @PostMapping
    public ApiResponse<ReservationResponse> book(@RequestParam Long sessionId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        log.info("User [{}] is booking session [ID: {}]", authentication.getName(), sessionId);
        return ApiResponse.success("预约成功", reservationService.book(sessionId, authentication.getName()));
    }

    @PostMapping("/{id}/cancel")
    public ApiResponse<ReservationResponse> cancel(@PathVariable Long id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        log.info("User [{}] is cancelling reservation [ID: {}]", authentication.getName(), id);
        return ApiResponse.success("已取消预约", reservationService.cancel(id, authentication.getName()));
    }

    /**
     * 上报一次切屏。累计次数落库；达到可配置阈值时后端强制交卷并以错误码返回。
     */
    @PostMapping("/{id}/switch-screen")
    public ApiResponse<SwitchScreenResponse> reportSwitchScreen(@PathVariable Long id,
                                                                @RequestBody(required = false) SwitchScreenRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        java.util.Map<Long, String> answers = request == null ? null : request.getAnswers();
        return ApiResponse.success(
                reservationService.reportSwitchScreen(id, authentication.getName(), answers));
    }
}
