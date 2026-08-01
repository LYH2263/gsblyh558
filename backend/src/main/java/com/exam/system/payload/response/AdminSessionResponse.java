package com.exam.system.payload.response;

import com.exam.system.entity.SessionStatus;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理端场次视图，附带已预约人数，便于下线时给出人数提示。
 */
@Data
public class AdminSessionResponse {

    private Long id;
    private Long examId;
    private String examTitle;
    private LocalDateTime startTime;
    private Integer durationMinutes;
    private Integer lateEntryMinutes;
    private Integer capacity;
    private SessionStatus status;
    /** 当前有效预约人数（BOOKED） */
    private long bookedCount;
    /** 剩余名额 */
    private long remainingSeats;
    /** 已开考人数（BOOKED 且已记录 startedAt） */
    private long startedCount;
}
