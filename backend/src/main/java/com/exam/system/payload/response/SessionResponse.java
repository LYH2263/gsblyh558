package com.exam.system.payload.response;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户端可预约场次视图，含剩余名额与当前用户是否已预约。
 */
@Data
public class SessionResponse {

    private Long id;
    private Long examId;
    private String examTitle;
    private String examDescription;
    private LocalDateTime startTime;
    private Integer durationMinutes;
    private Integer capacity;
    private long remainingSeats;
    /** 当前登录用户是否已预约该场次 */
    private boolean reservedByMe;
}
