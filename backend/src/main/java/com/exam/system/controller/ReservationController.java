package com.exam.system.controller;

import com.exam.system.dto.ReservationRequest;
import com.exam.system.dto.ReservationResponse;
import com.exam.system.dto.SwitchScreenResponse;
import com.exam.system.payload.response.ApiResponse;
import com.exam.system.service.ReservationService;
import jakarta.validation.Valid;
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
public class ReservationController {

    @Autowired
    private ReservationService reservationService;

    @PostMapping
    public ApiResponse<ReservationResponse> book(@Valid @RequestBody ReservationRequest request) {
        String username = currentUsername();
        return ApiResponse.success("预约成功", reservationService.book(request.getSessionId(), username));
    }

    @GetMapping("/my")
    public ApiResponse<List<ReservationResponse>> myReservations() {
        return ApiResponse.success(reservationService.myReservations(currentUsername()));
    }

    @PutMapping("/{id}/cancel")
    public ApiResponse<Void> cancel(@PathVariable Long id) {
        reservationService.cancel(id, currentUsername());
        return ApiResponse.success("预约已取消", null);
    }

    /** 切屏上报：累计次数与预约关联落库，达上限返回 forceSubmit 要求强制交卷 */
    @PostMapping("/{id}/switch-screen")
    public ApiResponse<SwitchScreenResponse> switchScreen(@PathVariable Long id) {
        return ApiResponse.success(reservationService.recordSwitchScreen(id, currentUsername()));
    }

    private String currentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName();
    }
}
