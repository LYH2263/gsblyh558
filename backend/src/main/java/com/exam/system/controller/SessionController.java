package com.exam.system.controller;

import com.exam.system.dto.ExamEntryResponse;
import com.exam.system.dto.ReservationResponse;
import com.exam.system.dto.ScreenSwitchReportRequest;
import com.exam.system.dto.ScreenSwitchReportResponse;
import com.exam.system.dto.SessionExamSubmission;
import com.exam.system.dto.SessionResponse;
import com.exam.system.entity.ExamResult;
import com.exam.system.payload.response.ApiResponse;
import com.exam.system.service.ExamSessionService;
import com.exam.system.service.ExamTakingService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/sessions")
@Slf4j
public class SessionController {

    @Autowired
    private ExamSessionService sessionService;

    @Autowired
    private ExamTakingService examTakingService;

    @GetMapping
    public ApiResponse<List<SessionResponse>> listOpenSessions() {
        return ApiResponse.success(sessionService.listOpenSessionsForUser());
    }

    @PostMapping("/{id}/book")
    public ApiResponse<ReservationResponse> bookSession(@PathVariable Long id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        log.info("User [{}] is booking session [ID: {}]", auth.getName(), id);
        return ApiResponse.success("预约成功", sessionService.bookSession(id, auth.getName()));
    }

    @PostMapping("/{id}/enter")
    public ApiResponse<ExamEntryResponse> enterSession(@PathVariable Long id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        log.info("User [{}] is entering session [ID: {}]", auth.getName(), id);
        return ApiResponse.success(examTakingService.enterSession(id, auth.getName()));
    }

    @PostMapping("/{id}/submit")
    public ApiResponse<ExamResult> submitSessionExam(
            @PathVariable Long id,
            @Valid @RequestBody SessionExamSubmission submission) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        log.info("User [{}] is submitting session [ID: {}]", auth.getName(), id);
        submission.setSessionId(id);
        ExamResult result = examTakingService.submitSessionExam(submission, auth.getName());
        String message = Boolean.TRUE.equals(result.getTimeout()) ? "已超时，系统按超时记录本次作答" : "交卷成功";
        return ApiResponse.success(message, result);
    }

    @PostMapping("/{id}/screen-switch")
    public ApiResponse<ScreenSwitchReportResponse> reportScreenSwitch(
            @PathVariable Long id,
            @Valid @RequestBody ScreenSwitchReportRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        log.info("User [{}] reports screen switch for session [ID: {}]", auth.getName(), id);
        request.setSessionId(id);
        ScreenSwitchReportResponse response = examTakingService.reportScreenSwitch(request, auth.getName());
        if (response.isLimitReached()) {
            return ApiResponse.success("切屏次数已达上限，系统已强制交卷", response);
        }
        return ApiResponse.success(response);
    }
}
