package com.exam.system.dto;

import com.exam.system.entity.ExamSession;
import com.exam.system.entity.SessionStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SessionResponse {

    private Long id;
    private Long examId;
    private String examTitle;
    private LocalDateTime startTime;
    private Integer durationMinutes;
    private Integer lateGraceMinutes;
    private Integer screenSwitchLimit;
    private Integer capacity;
    private Integer bookedCount;
    private Integer startedCount;
    private Integer remainingSlots;
    private SessionStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static SessionResponse from(ExamSession session, int bookedCount, int startedCount) {
        SessionResponse response = new SessionResponse();
        response.setId(session.getId());
        response.setExamId(session.getExam().getId());
        response.setExamTitle(session.getExam().getTitle());
        response.setStartTime(session.getStartTime());
        response.setDurationMinutes(session.getDurationMinutes());
        response.setLateGraceMinutes(session.getLateGraceMinutes());
        response.setScreenSwitchLimit(session.getScreenSwitchLimit());
        response.setCapacity(session.getCapacity());
        response.setBookedCount(bookedCount);
        response.setStartedCount(startedCount);
        int remaining = session.getCapacity() - bookedCount;
        response.setRemainingSlots(Math.max(remaining, 0));
        response.setStatus(session.getStatus());
        response.setCreatedAt(session.getCreatedAt());
        response.setUpdatedAt(session.getUpdatedAt());
        return response;
    }
}
