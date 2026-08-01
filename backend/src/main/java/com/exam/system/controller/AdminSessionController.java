package com.exam.system.controller;

import com.exam.system.entity.SessionStatus;
import com.exam.system.payload.request.ExamSessionRequest;
import com.exam.system.payload.response.AdminSessionResponse;
import com.exam.system.payload.response.ApiResponse;
import com.exam.system.payload.response.SessionStatsResponse;
import com.exam.system.service.ExamSessionService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理端场次管理。Controller 仅做鉴权上下文读取、入参校验、调用 Service、包装响应。
 */
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/admin/sessions")
@PreAuthorize("hasRole('ADMIN')")
@Slf4j
public class AdminSessionController {

    @Autowired
    private ExamSessionService examSessionService;

    @GetMapping
    public ApiResponse<List<AdminSessionResponse>> listSessions(
            @RequestParam(required = false) SessionStatus status) {
        return ApiResponse.success(examSessionService.listAll(status));
    }

    /**
     * 场次数据看板统计。可按 SessionStatus 过滤。
     */
    @GetMapping("/stats")
    public ApiResponse<List<SessionStatsResponse>> sessionStats(
            @RequestParam(required = false) SessionStatus status) {
        return ApiResponse.success(examSessionService.listStats(status));
    }

    @PostMapping
    public ApiResponse<AdminSessionResponse> createSession(@Valid @RequestBody ExamSessionRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        log.info("Admin [{}] is creating a session for exam [ID: {}]", authentication.getName(), request.getExamId());
        return ApiResponse.success("场次创建成功", examSessionService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<AdminSessionResponse> updateSession(@PathVariable Long id,
                                                           @Valid @RequestBody ExamSessionRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        log.info("Admin [{}] is updating session [ID: {}]", authentication.getName(), id);
        return ApiResponse.success("场次更新成功", examSessionService.update(id, request));
    }

    /**
     * 下线场次。confirmed=false 且已有人预约时，返回体携带 bookedCount 供前端人数提示。
     */
    @PostMapping("/{id}/close")
    public ApiResponse<AdminSessionResponse> closeSession(@PathVariable Long id,
                                                          @RequestParam(defaultValue = "false") boolean confirmed) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        log.info("Admin [{}] is closing session [ID: {}], confirmed={}", authentication.getName(), id, confirmed);
        return ApiResponse.success(examSessionService.close(id, confirmed));
    }
}
