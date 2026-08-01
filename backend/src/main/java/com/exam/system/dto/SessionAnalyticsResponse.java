package com.exam.system.dto;

import java.time.LocalDateTime;

public record SessionAnalyticsResponse(
        Long sessionId,
        String examTitle,
        LocalDateTime startTime,
        Integer durationMinutes,
        String status,
        Integer reservationCount,
        Integer startedCount,
        Integer completedOnTimeCount,
        Integer timedOutCount,
        Integer forcedByTabSwitchCount,
        Integer averageDurationMinutes
) {
}
