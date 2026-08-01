package com.exam.system.dto;

import com.exam.system.entity.SessionStatus;

import java.time.LocalDateTime;

public record ReservationExamAccessResponse(
        Long reservationId,
        Long sessionId,
        Long examId,
        String examTitle,
        LocalDateTime startTime,
        Integer durationMinutes,
        Integer lateGraceMinutes,
        Integer tabSwitchLimit,
        Integer tabSwitchCount,
        Integer remainingMinutes,
        LocalDateTime serverTime,
        SessionStatus sessionStatus
) {
}
