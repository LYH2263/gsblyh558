package com.exam.system.dto;

import com.exam.system.entity.ReservationStatus;
import com.exam.system.entity.SessionReservation;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReservationResponse {

    private Long id;
    private Long sessionId;
    private LocalDateTime sessionStartTime;
    private Integer sessionDurationMinutes;
    private Integer sessionLateGraceMinutes;
    private String sessionStatus;
    private Long examId;
    private String examTitle;
    private Long userId;
    private String username;
    private LocalDateTime reservedAt;
    private ReservationStatus status;

    public static ReservationResponse from(SessionReservation reservation) {
        ReservationResponse response = new ReservationResponse();
        response.setId(reservation.getId());
        response.setSessionId(reservation.getSession().getId());
        response.setSessionStartTime(reservation.getSession().getStartTime());
        response.setSessionDurationMinutes(reservation.getSession().getDurationMinutes());
        response.setSessionLateGraceMinutes(reservation.getSession().getLateGraceMinutes());
        response.setSessionStatus(reservation.getSession().getStatus() != null
                ? reservation.getSession().getStatus().name() : null);
        response.setExamId(reservation.getSession().getExam().getId());
        response.setExamTitle(reservation.getSession().getExam().getTitle());
        response.setUserId(reservation.getUser().getId());
        response.setUsername(reservation.getUser().getUsername());
        response.setReservedAt(reservation.getReservedAt());
        response.setStatus(reservation.getStatus());
        return response;
    }
}
