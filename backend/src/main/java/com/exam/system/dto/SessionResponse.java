package com.exam.system.dto;

import com.exam.system.entity.SessionStatus;

import java.time.LocalDateTime;

public record SessionResponse(
        Long id,
        Long examId,
        String examTitle,
        LocalDateTime startTime,
        Integer durationMinutes,
        Integer capacity,
        Integer bookedCount,
        Integer remainingCapacity,
        Integer startedCount,
        Integer lateGraceMinutes,
        Integer tabSwitchLimit,
        SessionStatus status,
        LocalDateTime createdAt
) {
}
