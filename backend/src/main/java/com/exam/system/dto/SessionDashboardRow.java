package com.exam.system.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SessionDashboardRow {

    private Long sessionId;
    private String examTitle;
    private LocalDateTime sessionStartTime;
    private Integer durationMinutes;
    private String status;
    private Integer capacity;
    private Integer reservedCount;
    private Integer startedCount;
    private Integer completedOnTimeCount;
    private Integer timeoutSubmissionCount;
    private Integer screenSwitchForcedCount;
    private Integer averageDurationMinutes;
}
