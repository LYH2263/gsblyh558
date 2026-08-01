package com.exam.system.controller;

import com.exam.system.payload.response.ApiResponse;
import com.exam.system.payload.response.SessionAccessResponse;
import com.exam.system.payload.response.SessionResponse;
import com.exam.system.service.ReservationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户端浏览可预约场次。
 */
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/sessions")
@Slf4j
public class SessionController {

    @Autowired
    private ReservationService reservationService;

    @GetMapping
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ApiResponse<List<SessionResponse>> listOpenSessions() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return ApiResponse.success(reservationService.listOpenSessions(authentication.getName()));
    }

    /**
     * 开考准入校验。准入通过返回答题定位信息与剩余作答时间；失败以 RSV_ 错误码返回。
     */
    @GetMapping("/{sessionId}/access")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ApiResponse<SessionAccessResponse> checkAccess(@PathVariable Long sessionId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return ApiResponse.success(reservationService.checkAccess(sessionId, authentication.getName()));
    }
}
