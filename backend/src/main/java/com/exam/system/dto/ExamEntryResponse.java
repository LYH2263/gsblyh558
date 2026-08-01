package com.exam.system.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ExamEntryResponse {
    private Long reservationId;
    private Long sessionId;
    private Long examId;
    private String examTitle;
    private LocalDateTime sessionStartTime;
    private Integer durationMinutes;
    private Integer lateGraceMinutes;
    private Integer screenSwitchLimit;
    private Integer screenSwitchCount;
    private LocalDateTime deadline;
    private Long serverTime;
}
