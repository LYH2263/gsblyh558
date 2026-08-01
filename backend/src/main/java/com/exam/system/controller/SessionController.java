package com.exam.system.controller;

import com.exam.system.dto.ExamSessionResponse;
import com.exam.system.dto.SessionAccessResponse;
import com.exam.system.payload.response.ApiResponse;
import com.exam.system.service.ExamSessionService;
import com.exam.system.service.ReservationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/sessions")
@PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
public class SessionController {

    @Autowired
    private ExamSessionService sessionService;

    @Autowired
    private ReservationService reservationService;

    /** 可预约场次列表（仅 OPEN），含剩余名额与可预约标记 */
    @GetMapping
    public ApiResponse<List<ExamSessionResponse>> listBookableSessions(@RequestParam(required = false) Long examId) {
        return ApiResponse.success(sessionService.listBookableSessions(examId));
    }

    /** 开考准入校验：已预约 + 当前时间落在作答窗口内才可进入答题页 */
    @GetMapping("/{sessionId}/access")
    public ApiResponse<SessionAccessResponse> checkAccess(@PathVariable Long sessionId) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return ApiResponse.success(reservationService.checkAccess(sessionId, username));
    }
}
