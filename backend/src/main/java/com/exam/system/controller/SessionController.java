package com.exam.system.controller;

import com.exam.system.dto.SessionResponse;
import com.exam.system.payload.response.ApiResponse;
import com.exam.system.service.SessionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/sessions")
@PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
@Slf4j
public class SessionController {
    @Autowired
    private SessionService sessionService;

    @GetMapping
    public ApiResponse<List<SessionResponse>> getAvailableSessions() {
        return ApiResponse.success(sessionService.getAvailableSessions());
    }
}
