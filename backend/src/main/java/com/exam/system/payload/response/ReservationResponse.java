package com.exam.system.payload.response;

import com.exam.system.entity.ReservationStatus;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 「我的预约」列表视图。
 */
@Data
public class ReservationResponse {

    private Long id;
    private Long sessionId;
    private Long examId;
    private String examTitle;
    private LocalDateTime startTime;
    private Integer durationMinutes;
    private LocalDateTime reservedAt;
    private ReservationStatus status;
}
