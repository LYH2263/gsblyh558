package com.exam.system.dto;

import com.exam.system.entity.ReservationStatus;
import com.exam.system.entity.SessionStatus;

import java.time.LocalDateTime;

public record ReservationResponse(
        Long id,
        Long sessionId,
        Long examId,
        String examTitle,
        LocalDateTime sessionStartTime,
        Integer durationMinutes,
        Integer capacity,
        SessionStatus sessionStatus,
        ReservationStatus status,
        LocalDateTime reservedAt
) {
}
